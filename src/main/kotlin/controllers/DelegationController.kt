package controllers

import repositories.DelegationRequestRepository
import repositories.PermissionRepository
import repositories.FamilyMemberRepository
import org.ktorm.schema.Column
import ktorm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Controller pour la gestion des délégations
 *
 * Responsabilités:
 * - CRUD des demandes de délégation
 * - Workflow complet : création → approbation/rejet → révocation
 * - Gestion des permissions déléguées
 * - Validation des demandes et permissions
 * - Notifications et audit des délégations
 * - Statistiques et rapports de délégation
 * - Gestion des expirations automatiques
 *
 * Utilisé par: UI Components, DashboardService, PermissionController
 * Utilise: DelegationRequestRepository, PermissionRepository, FamilyMemberRepository
 *
 * Pattern: Controller Layer + Result Pattern (standardisé avec AuthController)
 */
class DelegationController(
    private val delegationRequestRepository: DelegationRequestRepository,
    private val permissionRepository: PermissionRepository,
    private val familyMemberRepository: FamilyMemberRepository
) {

    /**
     * Résultats des opérations de délégation - PATTERN STANDARDISÉ
     */
    sealed class DelegationResult<out T> {
        data class Success<T>(val data: T) : DelegationResult<T>()
        data class Error(val message: String, val code: DelegationErrorCode) : DelegationResult<Nothing>()
    }

    enum class DelegationErrorCode {
        REQUEST_NOT_FOUND,
        PERMISSION_NOT_FOUND,
        MEMBER_NOT_FOUND,
        INVALID_INPUT,
        PERMISSION_DENIED,
        ALREADY_EXISTS,
        ALREADY_PROCESSED,
        EXPIRED,
        INTERNAL_ERROR
    }

    // ================================================================
    // MÉTHODES DE CRÉATION DE DEMANDES
    // ================================================================

    /**
     * Crée une nouvelle demande de délégation
     *
     * @param requestData Données de la demande
     * @return Résultat de la création
     */
    suspend fun createDelegationRequest(requestData: CreateDelegationRequest): DelegationResult<DemandeDelegation> = withContext(Dispatchers.IO) {
        try {
            // Validation des données
            val validationError = validateDelegationRequest(requestData)
            if (validationError != null) {
                return@withContext DelegationResult.Error(validationError, DelegationErrorCode.INVALID_INPUT)
            }

            // Vérifier que le propriétaire existe
            val owner = familyMemberRepository.findById(requestData.ownerId)
                ?: return@withContext DelegationResult.Error(
                    "Propriétaire non trouvé",
                    DelegationErrorCode.MEMBER_NOT_FOUND
                )

            // Vérifier que le bénéficiaire existe
            val beneficiary = familyMemberRepository.findById(requestData.beneficiaryId)
                ?: return@withContext DelegationResult.Error(
                    "Bénéficiaire non trouvé",
                    DelegationErrorCode.MEMBER_NOT_FOUND
                )

            // Vérifier qu'ils sont de la même famille
            if (owner.familleId != beneficiary.familleId) {
                return@withContext DelegationResult.Error(
                    "Le propriétaire et le bénéficiaire doivent être de la même famille",
                    DelegationErrorCode.PERMISSION_DENIED
                )
            }

            // Vérifier qu'une permission similaire n'existe pas déjà (implémentation simplifiée)
            val existingPermissions = permissionRepository.findAll()
                .filter {
                    it.beneficiaireId == requestData.beneficiaryId &&
                            it.portee == requestData.scope &&
                            it.cibleId == requestData.targetId &&
                            it.estActive == true
                }
            if (existingPermissions.isNotEmpty()) {
                return@withContext DelegationResult.Error(
                    "Une permission similaire existe déjà",
                    DelegationErrorCode.ALREADY_EXISTS
                )
            }

            // Vérifier qu'une demande similaire en attente n'existe pas (implémentation simplifiée)
            val pendingRequests = delegationRequestRepository.getDemandesEnAttente()
                .filter {
                    it.proprietaireId == requestData.ownerId &&
                            it.beneficiaireId == requestData.beneficiaryId &&
                            it.portee == requestData.scope &&
                            it.cibleId == requestData.targetId
                }
            if (pendingRequests.isNotEmpty()) {
                return@withContext DelegationResult.Error(
                    "Une demande similaire est déjà en attente",
                    DelegationErrorCode.ALREADY_EXISTS
                )
            }

            // Créer l'entité demande
            val requestEntity = DemandeDelegationEntity {
                proprietaireId = requestData.ownerId
                beneficiaireId = requestData.beneficiaryId
                portee = requestData.scope
                cibleId = requestData.targetId
                typePermission = requestData.permissionType
                raisonDemande = requestData.reason
                dateDemande = LocalDateTime.now()
                dateExpiration = requestData.expirationDate
                statut = StatutDemande.EN_ATTENTE.name
                valideeParId = null
                dateValidation = null
                commentaireAdmin = null
            }

            // Sauvegarder
            val savedRequest = delegationRequestRepository.create(requestEntity)

            return@withContext DelegationResult.Success(savedRequest.toModel())

        } catch (e: Exception) {
            return@withContext DelegationResult.Error(
                "Erreur lors de la création de la demande: ${e.message}",
                DelegationErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES D'APPROBATION ET REJET
    // ================================================================

    /**
     * Approuve une demande de délégation et crée la permission
     *
     * @param requestId ID de la demande
     * @param approverId ID de l'approbateur
     * @param comment Commentaire d'approbation (optionnel)
     * @return Résultat de l'approbation
     */
    suspend fun approveDelegationRequest(
        requestId: Int,
        approverId: Int,
        comment: String? = null
    ): DelegationResult<DelegationApprovalResult> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la demande existe
            val request = delegationRequestRepository.findById(requestId)
                ?: return@withContext DelegationResult.Error(
                    "Demande non trouvée",
                    DelegationErrorCode.REQUEST_NOT_FOUND
                )

            // Vérifier que la demande est en attente
            if (request.statut != StatutDemande.EN_ATTENTE.name) {
                return@withContext DelegationResult.Error(
                    "Cette demande a déjà été traitée",
                    DelegationErrorCode.ALREADY_PROCESSED
                )
            }

            // Vérifier les permissions de l'approbateur
            if (!canApproveRequest(approverId, request.proprietaireId ?: 0)) {
                return@withContext DelegationResult.Error(
                    "Permissions insuffisantes pour approuver cette demande",
                    DelegationErrorCode.PERMISSION_DENIED
                )
            }

            // Mettre à jour la demande
            request.statut = StatutDemande.APPROUVEE.name
            request.valideeParId = approverId
            request.dateValidation = LocalDateTime.now()
            request.commentaireAdmin = comment

            delegationRequestRepository.update(request)

            // Créer la permission correspondante
            val permissionData = CreatePermissionRequest(
                ownerId = request.proprietaireId ?: 0,
                beneficiaryId = request.beneficiaireId ?: 0,
                scope = request.portee ?: "",
                targetId = request.cibleId ?: 0,
                permissionType = request.typePermission ?: "",
                expirationDate = request.dateExpiration,
                delegationRequestId = requestId
            )

            val permissionEntity = PermissionActiveEntity {
                proprietaireId = permissionData.ownerId
                beneficiaireId = permissionData.beneficiaryId
                portee = permissionData.scope
                cibleId = permissionData.targetId
                typePermission = permissionData.permissionType
                dateOctroi = LocalDateTime.now()
                dateExpiration = permissionData.expirationDate
                estActive = true
                // Note: demandeId n'existe pas dans l'entité - géré séparément
            }

            val savedPermission = permissionRepository.create(permissionEntity)

            val result = DelegationApprovalResult(
                request = request.toModel(),
                permission = savedPermission.toModel(),
                approver = familyMemberRepository.findById(approverId)?.toModel()
            )

            return@withContext DelegationResult.Success(result)

        } catch (e: Exception) {
            return@withContext DelegationResult.Error(
                "Erreur lors de l'approbation: ${e.message}",
                DelegationErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Rejette une demande de délégation
     *
     * @param requestId ID de la demande
     * @param rejecterId ID du rejeteur
     * @param reason Raison du rejet
     * @return Résultat du rejet
     */
    suspend fun rejectDelegationRequest(
        requestId: Int,
        rejecterId: Int,
        reason: String
    ): DelegationResult<DemandeDelegation> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la demande existe
            val request = delegationRequestRepository.findById(requestId)
                ?: return@withContext DelegationResult.Error(
                    "Demande non trouvée",
                    DelegationErrorCode.REQUEST_NOT_FOUND
                )

            // Vérifier que la demande est en attente
            if (request.statut != StatutDemande.EN_ATTENTE.name) {
                return@withContext DelegationResult.Error(
                    "Cette demande a déjà été traitée",
                    DelegationErrorCode.ALREADY_PROCESSED
                )
            }

            // Vérifier les permissions du rejeteur
            if (!canRejectRequest(rejecterId, request.proprietaireId ?: 0)) {
                return@withContext DelegationResult.Error(
                    "Permissions insuffisantes pour rejeter cette demande",
                    DelegationErrorCode.PERMISSION_DENIED
                )
            }

            // Mettre à jour la demande
            request.statut = StatutDemande.REJETEE.name
            request.valideeParId = rejecterId
            request.dateValidation = LocalDateTime.now()
            request.commentaireAdmin = reason

            delegationRequestRepository.update(request)

            return@withContext DelegationResult.Success(request.toModel())

        } catch (e: Exception) {
            return@withContext DelegationResult.Error(
                "Erreur lors du rejet: ${e.message}",
                DelegationErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE CONSULTATION
    // ================================================================

    /**
     * Récupère une demande de délégation par son ID
     *
     * @param requestId ID de la demande
     * @return Résultat avec la demande trouvée
     */
    suspend fun getDelegationRequestById(requestId: Int): DelegationResult<DemandeDelegation> = withContext(Dispatchers.IO) {
        try {
            val request = delegationRequestRepository.findById(requestId)
                ?: return@withContext DelegationResult.Error(
                    "Demande non trouvée",
                    DelegationErrorCode.REQUEST_NOT_FOUND
                )

            return@withContext DelegationResult.Success(request.toModel())

        } catch (e: Exception) {
            return@withContext DelegationResult.Error(
                "Erreur lors de la récupération: ${e.message}",
                DelegationErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère les demandes en attente d'approbation
     *
     * @param approverId ID de l'approbateur potentiel (pour filtrer les permissions)
     * @return Résultat avec les demandes en attente
     */
    suspend fun getPendingDelegationRequests(approverId: Int? = null): DelegationResult<List<DemandeDelegation>> = withContext(Dispatchers.IO) {
        try {
            val pendingRequests = delegationRequestRepository.getDemandesEnAttente()

            // Filtrer selon les permissions si un approbateur est spécifié
            val filteredRequests = if (approverId != null) {
                pendingRequests.filter { canApproveRequest(approverId, it.proprietaireId ?: 0) }
            } else {
                pendingRequests
            }

            val requestModels = filteredRequests.map { it.toModel() }

            return@withContext DelegationResult.Success(requestModels)

        } catch (e: Exception) {
            return@withContext DelegationResult.Error(
                "Erreur lors de la récupération des demandes en attente: ${e.message}",
                DelegationErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère les demandes par propriétaire
     *
     * @param ownerId ID du propriétaire
     * @param status Statut des demandes (optionnel)
     * @return Résultat avec les demandes du propriétaire
     */
    suspend fun getDelegationRequestsByOwner(
        ownerId: Int,
        status: StatutDemande? = null
    ): DelegationResult<List<DemandeDelegation>> = withContext(Dispatchers.IO) {
        try {
            val requests = if (status != null) {
                delegationRequestRepository.getDemandesByStatut(status)
                    .filter { it.proprietaireId == ownerId }
            } else {
                delegationRequestRepository.findAll()
                    .filter { it.proprietaireId == ownerId }
            }

            val requestModels = requests.map { it.toModel() }

            return@withContext DelegationResult.Success(requestModels)

        } catch (e: Exception) {
            return@withContext DelegationResult.Error(
                "Erreur lors de la récupération des demandes du propriétaire: ${e.message}",
                DelegationErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère les demandes par bénéficiaire
     *
     * @param beneficiaryId ID du bénéficiaire
     * @param status Statut des demandes (optionnel)
     * @return Résultat avec les demandes du bénéficiaire
     */
    suspend fun getDelegationRequestsByBeneficiary(
        beneficiaryId: Int,
        status: StatutDemande? = null
    ): DelegationResult<List<DemandeDelegation>> = withContext(Dispatchers.IO) {
        try {
            val requests = if (status != null) {
                delegationRequestRepository.getDemandesByStatut(status)
                    .filter { it.beneficiaireId == beneficiaryId }
            } else {
                delegationRequestRepository.findAll()
                    .filter { it.beneficiaireId == beneficiaryId }
            }

            val requestModels = requests.map { it.toModel() }

            return@withContext DelegationResult.Success(requestModels)

        } catch (e: Exception) {
            return@withContext DelegationResult.Error(
                "Erreur lors de la récupération des demandes du bénéficiaire: ${e.message}",
                DelegationErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE RÉVOCATION DE PERMISSIONS
    // ================================================================

    /**
     * Révoque une permission déléguée
     *
     * @param permissionId ID de la permission
     * @param revokerId ID de celui qui révoque
     * @param reason Raison de la révocation
     * @return Résultat de la révocation
     */
    suspend fun revokePermission(
        permissionId: Int,
        revokerId: Int,
        reason: String
    ): DelegationResult<PermissionActive> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la permission existe
            val permission = permissionRepository.findById(permissionId)
                ?: return@withContext DelegationResult.Error(
                    "Permission non trouvée",
                    DelegationErrorCode.PERMISSION_NOT_FOUND
                )

            // Vérifier les droits de révocation
            if (!canRevokePermission(revokerId, permission.proprietaireId ?: 0, permission.beneficiaireId ?: 0)) {
                return@withContext DelegationResult.Error(
                    "Permissions insuffisantes pour révoquer cette permission",
                    DelegationErrorCode.PERMISSION_DENIED
                )
            }

            // Révoquer la permission
            permission.estActive = false
            // Note: dateRevocation n'existe pas dans l'entité - on utilise un autre moyen de traçabilité

            permissionRepository.update(permission)

            return@withContext DelegationResult.Success(permission.toModel())

        } catch (e: Exception) {
            return@withContext DelegationResult.Error(
                "Erreur lors de la révocation: ${e.message}",
                DelegationErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques de délégation pour un utilisateur
     *
     * @param userId ID de l'utilisateur
     * @return Résultat avec les statistiques
     */
    suspend fun getDelegationStatistics(userId: Int): DelegationResult<DelegationStatistics> = withContext(Dispatchers.IO) {
        try {
            val allRequests = delegationRequestRepository.findAll()
            val allPermissions = permissionRepository.findAll()

            val ownedRequests = allRequests.filter { it.proprietaireId == userId }
            val beneficiaryRequests = allRequests.filter { it.beneficiaireId == userId }

            val ownedPermissions = allPermissions.filter { it.proprietaireId == userId && it.estActive == true }
            val beneficiaryPermissions = allPermissions.filter { it.beneficiaireId == userId && it.estActive == true }

            val pendingOwnedRequests = ownedRequests.count { it.statut == StatutDemande.EN_ATTENTE.name }
            val pendingBeneficiaryRequests = beneficiaryRequests.count { it.statut == StatutDemande.EN_ATTENTE.name }

            val stats = DelegationStatistics(
                totalOwnedRequests = ownedRequests.size,
                totalBeneficiaryRequests = beneficiaryRequests.size,
                pendingOwnedRequests = pendingOwnedRequests,
                pendingBeneficiaryRequests = pendingBeneficiaryRequests,
                activeOwnedPermissions = ownedPermissions.size,
                activeBeneficiaryPermissions = beneficiaryPermissions.size
            )

            return@withContext DelegationResult.Success(stats)

        } catch (e: Exception) {
            return@withContext DelegationResult.Error(
                "Erreur lors du calcul des statistiques: ${e.message}",
                DelegationErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES DE VALIDATION
    // ================================================================

    private fun validateDelegationRequest(requestData: CreateDelegationRequest): String? {
        return when {
            requestData.ownerId <= 0 -> "ID de propriétaire invalide"
            requestData.beneficiaryId <= 0 -> "ID de bénéficiaire invalide"
            requestData.ownerId == requestData.beneficiaryId ->
                "Le propriétaire et le bénéficiaire ne peuvent pas être la même personne"
            requestData.scope.isBlank() -> "La portée ne peut pas être vide"
            requestData.permissionType.isBlank() -> "Le type de permission ne peut pas être vide"
            requestData.reason.isBlank() -> "La raison de la demande ne peut pas être vide"
            requestData.reason.length > 500 -> "La raison ne peut pas dépasser 500 caractères"
            requestData.expirationDate != null && requestData.expirationDate.isBefore(LocalDateTime.now()) ->
                "La date d'expiration ne peut pas être dans le passé"
            else -> null
        }
    }

    private fun canApproveRequest(approverId: Int, ownerId: Int): Boolean {
        val approver = familyMemberRepository.findById(approverId)
        val owner = familyMemberRepository.findById(ownerId)

        // Propriétaire peut approuver ses propres demandes
        if (approverId == ownerId) return true

        // Admins peuvent approuver toutes les demandes de leur famille
        if (approver?.estAdmin == true && approver.familleId == owner?.familleId) return true

        // Responsables peuvent approuver dans leur famille
        if (approver?.estResponsable == true && approver.familleId == owner?.familleId) return true

        return false
    }

    private fun canRejectRequest(rejecterId: Int, ownerId: Int): Boolean {
        // Mêmes règles que pour l'approbation
        return canApproveRequest(rejecterId, ownerId)
    }

    private fun canRevokePermission(revokerId: Int, ownerId: Int, beneficiaryId: Int): Boolean {
        val revoker = familyMemberRepository.findById(revokerId)

        // Propriétaire peut révoquer ses permissions
        if (revokerId == ownerId) return true

        // Bénéficiaire peut renoncer à ses permissions
        if (revokerId == beneficiaryId) return true

        // Admins peuvent révoquer dans leur famille
        if (revoker?.estAdmin == true) {
            val owner = familyMemberRepository.findById(ownerId)
            return revoker.familleId == owner?.familleId
        }

        return false
    }
}

// ================================================================
// DATA CLASSES POUR LES REQUÊTES ET RÉSULTATS
// ================================================================

/**
 * Données pour créer une nouvelle demande de délégation
 */
data class CreateDelegationRequest(
    val ownerId: Int,
    val beneficiaryId: Int,
    val scope: String,
    val targetId: Int,
    val permissionType: String,
    val reason: String,
    val expirationDate: LocalDateTime? = null
)

/**
 * Données pour créer une nouvelle permission
 */
data class CreatePermissionRequest(
    val ownerId: Int,
    val beneficiaryId: Int,
    val scope: String,
    val targetId: Int,
    val permissionType: String,
    val expirationDate: LocalDateTime? = null,
    val delegationRequestId: Int? = null
)

/**
 * Résultat d'approbation d'une demande
 */
data class DelegationApprovalResult(
    val request: DemandeDelegation,
    val permission: PermissionActive,
    val approver: MembreFamille?
)

/**
 * Statistiques de délégation pour un utilisateur
 */
data class DelegationStatistics(
    val totalOwnedRequests: Int,
    val totalBeneficiaryRequests: Int,
    val pendingOwnedRequests: Int,
    val pendingBeneficiaryRequests: Int,
    val activeOwnedPermissions: Int,
    val activeBeneficiaryPermissions: Int
)