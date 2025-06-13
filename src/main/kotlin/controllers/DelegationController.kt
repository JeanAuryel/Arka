package controllers

import ktorm.*
import repositories.*
import java.time.LocalDateTime

/**
 * Controller pour la gestion complète du workflow de délégation dans Arka
 * Orchestre les demandes, approbations, rejets et révocations de permissions
 */
class DelegationController(
    private val delegationRepository: DelegationRequestRepository,
    private val permissionRepository: PermissionRepository,
    private val memberRepository: FamilyMemberRepository
) {

    /**
     * Crée une nouvelle demande de délégation
     * @param requestData Les données de la demande
     * @param requesterUserId L'ID de l'utilisateur qui fait la demande
     * @return Le résultat de la création
     */
    fun createDelegationRequest(
        requestData: CreateDelegationData,
        requesterUserId: Int
    ): DelegationCreateResult {
        return try {
            // 1. Vérifications préliminaires
            val validationResult = validateDelegationRequest(requestData, requesterUserId)
            if (!validationResult.isValid) {
                return DelegationCreateResult.ValidationError(validationResult.errors)
            }

            // 2. Vérifier que le bénéficiaire n'a pas déjà cette permission
            if (hasExistingPermission(requestData)) {
                return DelegationCreateResult.Error("Le bénéficiaire a déjà cette permission")
            }

            // 3. Créer la demande
            val delegationRequestData = CreateDelegationRequestData(
                proprietaireId = requestData.proprietaireId,
                beneficiaireId = requestData.beneficiaireId,
                portee = requestData.portee,
                cibleId = requestData.cibleId,
                typePermission = requestData.typePermission,
                raisonDemande = requestData.raisonDemande,
                dateExpiration = requestData.dateExpiration
            )

            val result = delegationRepository.createDelegationRequest(delegationRequestData)

            when (result) {
                is RepositoryResult.Success -> {
                    // 4. Notifier les parties concernées (à implémenter)
                    notifyDelegationRequest(result.data)

                    DelegationCreateResult.Success(
                        demande = result.data,
                        message = "Demande de délégation créée avec succès"
                    )
                }
                is RepositoryResult.Error -> DelegationCreateResult.Error(result.message)
                is RepositoryResult.ValidationError -> DelegationCreateResult.ValidationError(result.errors)
            }
        } catch (e: Exception) {
            DelegationCreateResult.Error("Erreur lors de la création de la demande: ${e.message}")
        }
    }

    /**
     * Approuve une demande de délégation et crée la permission
     * @param demandeId L'ID de la demande
     * @param approverId L'ID de l'utilisateur qui approuve
     * @param commentaire Commentaire d'approbation (optionnel)
     * @return Le résultat de l'approbation
     */
    fun approveDelegationRequest(
        demandeId: Int,
        approverId: Int,
        commentaire: String? = null
    ): DelegationApprovalResult {
        return try {
            // 1. Vérifier que la demande existe et est en attente
            val demande = delegationRepository.findById(demandeId)
            if (demande == null) {
                return DelegationApprovalResult.Error("Demande non trouvée")
            }

            if (demande.statut != StatutDemande.EN_ATTENTE.name) {
                return DelegationApprovalResult.Error("Cette demande a déjà été traitée")
            }

            // 2. Vérifier les permissions de l'approbateur
            if (!canApproveRequest(approverId, demande.proprietaireId)) {
                return DelegationApprovalResult.Error("Permissions insuffisantes pour approuver cette demande")
            }

            // 3. Approuver la demande
            val approvalResult = delegationRepository.approveDelegationRequest(demandeId, approverId, commentaire)

            when (approvalResult) {
                is RepositoryResult.Success -> {
                    // 4. Créer la permission correspondante
                    val permissionResult = createPermissionFromRequest(approvalResult.data)

                    when (permissionResult) {
                        is RepositoryResult.Success -> {
                            // 5. Notifier les parties concernées
                            notifyDelegationApproval(approvalResult.data, permissionResult.data)

                            DelegationApprovalResult.Success(
                                demande = approvalResult.data,
                                permission = permissionResult.data,
                                message = "Demande approuvée et permission créée"
                            )
                        }
                        is RepositoryResult.Error -> {
                            // Rollback de l'approbation si la création de permission échoue
                            delegationRepository.rejectDelegationRequest(
                                demandeId,
                                approverId,
                                "Échec de la création de permission: ${permissionResult.message}"
                            )
                            DelegationApprovalResult.Error("Échec de la création de permission: ${permissionResult.message}")
                        }
                        is RepositoryResult.ValidationError -> {
                            DelegationApprovalResult.Error("Erreur de validation permission: ${permissionResult.errors.joinToString(", ")}")
                        }
                    }
                }
                is RepositoryResult.Error -> DelegationApprovalResult.Error(approvalResult.message)
                is RepositoryResult.ValidationError -> DelegationApprovalResult.Error(approvalResult.errors.joinToString(", "))
            }
        } catch (e: Exception) {
            DelegationApprovalResult.Error("Erreur lors de l'approbation: ${e.message}")
        }
    }

    /**
     * Rejette une demande de délégation
     * @param demandeId L'ID de la demande
     * @param rejecterId L'ID de l'utilisateur qui rejette
     * @param raisonRejet La raison du rejet
     * @return Le résultat du rejet
     */
    fun rejectDelegationRequest(
        demandeId: Int,
        rejecterId: Int,
        raisonRejet: String
    ): DelegationRejectionResult {
        return try {
            // 1. Vérifier les permissions
            val demande = delegationRepository.findById(demandeId)
            if (demande == null) {
                return DelegationRejectionResult.Error("Demande non trouvée")
            }

            if (!canRejectRequest(rejecterId, demande.proprietaireId)) {
                return DelegationRejectionResult.Error("Permissions insuffisantes pour rejeter cette demande")
            }

            // 2. Rejeter la demande
            val result = delegationRepository.rejectDelegationRequest(demandeId, rejecterId, raisonRejet)

            when (result) {
                is RepositoryResult.Success -> {
                    // 3. Notifier les parties concernées
                    notifyDelegationRejection(result.data, raisonRejet)

                    DelegationRejectionResult.Success(
                        demande = result.data,
                        message = "Demande rejetée avec succès"
                    )
                }
                is RepositoryResult.Error -> DelegationRejectionResult.Error(result.message)
                is RepositoryResult.ValidationError -> DelegationRejectionResult.Error(result.errors.joinToString(", "))
            }
        } catch (e: Exception) {
            DelegationRejectionResult.Error("Erreur lors du rejet: ${e.message}")
        }
    }

    /**
     * Révoque une permission active (et la demande associée)
     * @param permissionId L'ID de la permission
     * @param revokerId L'ID de l'utilisateur qui révoque
     * @param raisonRevocation La raison de la révocation
     * @return Le résultat de la révocation
     */
    fun revokePermission(
        permissionId: Int,
        revokerId: Int,
        raisonRevocation: String
    ): PermissionRevocationResult {
        return try {
            // 1. Vérifier que la permission existe
            val permission = permissionRepository.findById(permissionId)
            if (permission == null) {
                return PermissionRevocationResult.Error("Permission non trouvée")
            }

            // 2. Vérifier les permissions de révocation
            if (!canRevokePermission(revokerId, permission.proprietaireId)) {
                return PermissionRevocationResult.Error("Permissions insuffisantes pour révoquer cette permission")
            }

            // 3. Désactiver la permission
            val deactivationResult = permissionRepository.deactivatePermission(permissionId)

            when (deactivationResult) {
                is RepositoryResult.Success -> {
                    // 4. Marquer la demande comme révoquée (si elle existe)
                    markRequestAsRevoked(permission, revokerId, raisonRevocation)

                    // 5. Notifier les parties concernées
                    notifyPermissionRevocation(permission, raisonRevocation)

                    PermissionRevocationResult.Success(
                        permission = deactivationResult.data,
                        message = "Permission révoquée avec succès"
                    )
                }
                is RepositoryResult.Error -> PermissionRevocationResult.Error(deactivationResult.message)
                is RepositoryResult.ValidationError -> PermissionRevocationResult.Error(deactivationResult.errors.joinToString(", "))
            }
        } catch (e: Exception) {
            PermissionRevocationResult.Error("Erreur lors de la révocation: ${e.message}")
        }
    }

    /**
     * Obtient le tableau de bord des délégations pour un utilisateur
     * @param userId L'ID de l'utilisateur
     * @param userRole Le rôle de l'utilisateur (propriétaire, bénéficiaire, admin)
     * @return Le tableau de bord
     */
    fun getDelegationDashboard(userId: Int, userRole: DashboardRole): DelegationDashboard {
        return try {
            val dashboard = when (userRole) {
                DashboardRole.OWNER -> createOwnerDashboard(userId)
                DashboardRole.BENEFICIARY -> createBeneficiaryDashboard(userId)
                DashboardRole.ADMIN -> createAdminDashboard(userId)
            }

            dashboard
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la création du tableau de bord: ${e.message}")
            DelegationDashboard(
                userId = userId,
                role = userRole,
                pendingRequests = emptyList(),
                activePermissions = emptyList(),
                recentActivity = emptyList(),
                statistics = DelegationStatistics(0, 0, 0, 0, 0)
            )
        }
    }

    /**
     * Recherche des demandes de délégation avec filtres
     * @param searchCriteria Les critères de recherche
     * @param searcherId L'ID de l'utilisateur qui recherche
     * @return Les résultats de recherche
     */
    fun searchDelegationRequests(
        searchCriteria: DelegationSearchCriteria,
        searcherId: Int
    ): List<DelegationSearchResult> {
        return try {
            // 1. Appliquer les filtres de base
            val allRequests = when {
                searchCriteria.proprietaireId != null ->
                    delegationRepository.findByOwner(searchCriteria.proprietaireId, searchCriteria.statut)
                searchCriteria.beneficiaireId != null ->
                    delegationRepository.findByBeneficiary(searchCriteria.beneficiaireId, searchCriteria.statut)
                searchCriteria.statut != null ->
                    delegationRepository.findAll().filter { it.statut == searchCriteria.statut.name }
                else -> delegationRepository.findAll()
            }

            // 2. Filtrer selon les permissions de l'utilisateur qui recherche
            val filteredRequests = allRequests.filter { request ->
                canViewRequest(searcherId, request.proprietaireId, request.beneficiaireId)
            }

            // 3. Appliquer les filtres additionnels
            var results = filteredRequests

            searchCriteria.portee?.let { portee ->
                results = results.filter { it.portee == portee.name }
            }

            searchCriteria.typePermission?.let { type ->
                results = results.filter { it.typePermission == type.name }
            }

            searchCriteria.dateDebutCreation?.let { debut ->
                results = results.filter { it.dateDemande?.isAfter(debut) == true }
            }

            searchCriteria.dateFinCreation?.let { fin ->
                results = results.filter { it.dateDemande?.isBefore(fin) == true }
            }

            // 4. Convertir en résultats de recherche avec métadonnées
            results.map { createSearchResult(it) }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche: ${e.message}")
            emptyList()
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES - VALIDATIONS ET VÉRIFICATIONS
    // ================================================================

    /**
     * Valide une demande de délégation
     */
    private fun validateDelegationRequest(requestData: CreateDelegationData, requesterUserId: Int): ValidationResult {
        val errors = mutableListOf<String>()

        // Vérifier que le demandeur existe
        val requester = memberRepository.findById(requesterUserId)
        if (requester == null) {
            errors.add("Utilisateur demandeur non trouvé")
        }

        // Vérifier que le propriétaire existe
        val owner = memberRepository.findById(requestData.proprietaireId)
        if (owner == null) {
            errors.add("Propriétaire non trouvé")
        }

        // Vérifier que le bénéficiaire existe
        val beneficiary = memberRepository.findById(requestData.beneficiaireId)
        if (beneficiary == null) {
            errors.add("Bénéficiaire non trouvé")
        }

        // Vérifier que le demandeur et le bénéficiaire sont de la même famille
        if (requester != null && beneficiary != null && requester.familleId != beneficiary.familleId) {
            errors.add("Le demandeur et le bénéficiaire doivent être de la même famille")
        }

        // Vérifier la logique métier selon la portée
        when (requestData.portee) {
            PorteePermission.FICHIER, PorteePermission.DOSSIER -> {
                if (requestData.cibleId == null) {
                    errors.add("ID de la cible requis pour cette portée")
                }
            }
            PorteePermission.CATEGORIE -> {
                if (requestData.cibleId == null) {
                    errors.add("ID de la catégorie requis")
                }
            }
            PorteePermission.ESPACE_COMPLET -> {
                // Vérifier que seuls admin/responsable peuvent demander l'accès complet
                if (requester?.estAdmin != true && requester?.estResponsable != true) {
                    errors.add("Seuls les administrateurs et responsables peuvent demander l'accès à l'espace complet")
                }
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Vérifie si une permission similaire existe déjà
     */
    private fun hasExistingPermission(requestData: CreateDelegationData): Boolean {
        return permissionRepository.hasPermission(
            beneficiaireId = requestData.beneficiaireId,
            portee = requestData.portee,
            cibleId = requestData.cibleId,
            typePermission = requestData.typePermission
        )
    }

    /**
     * Vérifie si un utilisateur peut approuver une demande
     */
    private fun canApproveRequest(approverId: Int, ownerId: Int): Boolean {
        val approver = memberRepository.findById(approverId)

        // Le propriétaire peut toujours approuver
        if (approverId == ownerId) return true

        // Les admins peuvent approuver
        if (approver?.estAdmin == true) return true

        // Les responsables peuvent approuver dans leur famille
        if (approver?.estResponsable == true) {
            val owner = memberRepository.findById(ownerId)
            return approver.familleId == owner?.familleId
        }

        return false
    }

    /**
     * Vérifie si un utilisateur peut rejeter une demande
     */
    private fun canRejectRequest(rejecterId: Int, ownerId: Int): Boolean {
        return canApproveRequest(rejecterId, ownerId) // Mêmes règles que l'approbation
    }

    /**
     * Vérifie si un utilisateur peut révoquer une permission
     */
    private fun canRevokePermission(revokerId: Int, ownerId: Int): Boolean {
        return canApproveRequest(revokerId, ownerId) // Mêmes règles que l'approbation
    }

    /**
     * Vérifie si un utilisateur peut voir une demande
     */
    private fun canViewRequest(viewerId: Int, ownerId: Int, beneficiaryId: Int): Boolean {
        val viewer = memberRepository.findById(viewerId)

        // Propriétaire et bénéficiaire peuvent voir
        if (viewerId == ownerId || viewerId == beneficiaryId) return true

        // Admins peuvent voir toutes les demandes
        if (viewer?.estAdmin == true) return true

        // Responsables peuvent voir les demandes dans leur famille
        if (viewer?.estResponsable == true) {
            val owner = memberRepository.findById(ownerId)
            return viewer.familleId == owner?.familleId
        }

        return false
    }

    // ================================================================
    // MÉTHODES PRIVÉES - UTILITAIRES
    // ================================================================

    /**
     * Crée une permission à partir d'une demande approuvée
     */
    private fun createPermissionFromRequest(demande: DemandeDelegation): RepositoryResult<PermissionActive> {
        val permissionData = CreatePermissionData(
            proprietaireId = demande.proprietaireId,
            beneficiaireId = demande.beneficiaireId,
            portee = demande.portee,
            cibleId = demande.cibleId,
            typePermission = demande.typePermission,
            dateExpiration = demande.dateExpiration
        )

        return permissionRepository.createPermission(permissionData)
    }

    /**
     * Marque une demande comme révoquée
     */
    private fun markRequestAsRevoked(permission: PermissionActiveEntity, revokerId: Int, raison: String) {
        try {
            // Trouver la demande correspondante (implémentation simplifiée)
            // Dans un vrai projet, on aurait une relation directe
            val requests = delegationRepository.findByOwner(permission.proprietaireId, StatutDemande.APPROUVEE)
            val matchingRequest = requests.find {
                it.beneficiaireId == permission.beneficiaireId &&
                        it.portee == permission.portee &&
                        it.cibleId == permission.cibleId &&
                        it.typePermission == permission.typePermission
            }

            matchingRequest?.let {
                delegationRepository.revokeDelegationRequest(it.demandeId, revokerId, raison)
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors du marquage de la demande comme révoquée: ${e.message}")
        }
    }

    /**
     * Crée le tableau de bord pour un propriétaire
     */
    private fun createOwnerDashboard(userId: Int): DelegationDashboard {
        val pendingRequests = delegationRepository.findByOwner(userId, StatutDemande.EN_ATTENTE)
        val grantedPermissions = permissionRepository.findGrantedByOwner(userId)
        val stats = delegationRepository.getDelegationStats(userId)

        return DelegationDashboard(
            userId = userId,
            role = DashboardRole.OWNER,
            pendingRequests = pendingRequests.map { it.toModel() },
            activePermissions = grantedPermissions.map { it.toModel() },
            recentActivity = getRecentActivity(userId),
            statistics = DelegationStatistics(
                totalRequests = stats.total,
                pendingRequests = stats.enAttente,
                approvedRequests = stats.approuvees,
                rejectedRequests = stats.rejetees,
                activePermissions = grantedPermissions.size
            )
        )
    }

    /**
     * Crée le tableau de bord pour un bénéficiaire
     */
    private fun createBeneficiaryDashboard(userId: Int): DelegationDashboard {
        val myRequests = delegationRepository.findByBeneficiary(userId)
        val myPermissions = permissionRepository.findActiveByBeneficiary(userId)

        return DelegationDashboard(
            userId = userId,
            role = DashboardRole.BENEFICIARY,
            pendingRequests = myRequests.filter { it.statut == StatutDemande.EN_ATTENTE.name }.map { it.toModel() },
            activePermissions = myPermissions.map { it.toModel() },
            recentActivity = getRecentActivity(userId),
            statistics = DelegationStatistics(
                totalRequests = myRequests.size,
                pendingRequests = myRequests.count { it.statut == StatutDemande.EN_ATTENTE.name },
                approvedRequests = myRequests.count { it.statut == StatutDemande.APPROUVEE.name },
                rejectedRequests = myRequests.count { it.statut == StatutDemande.REJETEE.name },
                activePermissions = myPermissions.size
            )
        )
    }

    /**
     * Crée le tableau de bord pour un administrateur
     */
    private fun createAdminDashboard(userId: Int): DelegationDashboard {
        val allPendingRequests = delegationRepository.findPendingRequests()
        val allStats = delegationRepository.getDelegationStats()
        val allPermissions = permissionRepository.getPermissionStats()

        return DelegationDashboard(
            userId = userId,
            role = DashboardRole.ADMIN,
            pendingRequests = allPendingRequests.map { it.toModel() },
            activePermissions = emptyList(), // Les admins voient les stats globales
            recentActivity = getRecentActivity(userId),
            statistics = DelegationStatistics(
                totalRequests = allStats.total,
                pendingRequests = allStats.enAttente,
                approvedRequests = allStats.approuvees,
                rejectedRequests = allStats.rejetees,
                activePermissions = allPermissions.actives
            )
        )
    }

    /**
     * Crée un résultat de recherche avec métadonnées
     */
    private fun createSearchResult(request: DemandeDelegationEntity): DelegationSearchResult {
        val owner = memberRepository.findById(request.proprietaireId)
        val beneficiary = memberRepository.findById(request.beneficiaireId)

        return DelegationSearchResult(
            demande = request.toModel(),
            ownerName = owner?.prenomMembre ?: "Inconnu",
            beneficiaryName = beneficiary?.prenomMembre ?: "Inconnu",
            targetName = getTargetName(request.portee, request.cibleId),
            isExpired = request.dateExpiration?.isBefore(LocalDateTime.now()) == true
        )
    }

    /**
     * Obtient le nom de la cible selon la portée
     */
    private fun getTargetName(portee: String, cibleId: Int?): String {
        return when (PorteePermission.valueOf(portee)) {
            PorteePermission.FICHIER -> "Fichier #$cibleId"
            PorteePermission.DOSSIER -> "Dossier #$cibleId"
            PorteePermission.CATEGORIE -> "Catégorie #$cibleId"
            PorteePermission.ESPACE_COMPLET -> "Espace complet"
        }
    }

    /**
     * Obtient l'activité récente d'un utilisateur
     */
    private fun getRecentActivity(userId: Int): List<DelegationActivity> {
        // Implémentation simplifiée - dans un vrai projet, on aurait un système d'audit
        return emptyList()
    }

    // ================================================================
    // MÉTHODES PRIVÉES - NOTIFICATIONS (À IMPLÉMENTER)
    // ================================================================

    private fun notifyDelegationRequest(demande: DemandeDelegation) {
        // TODO: Implémenter le système de notifications
        println("📧 Notification: Nouvelle demande de délégation créée")
    }

    private fun notifyDelegationApproval(demande: DemandeDelegation, permission: PermissionActive) {
        println("📧 Notification: Demande de délégation approuvée")
    }

    private fun notifyDelegationRejection(demande: DemandeDelegation, raison: String) {
        println("📧 Notification: Demande de délégation rejetée")
    }

    private fun notifyPermissionRevocation(permission: PermissionActiveEntity, raison: String) {
        println("📧 Notification: Permission révoquée")
    }
}

// ================================================================
// CLASSES DE DONNÉES POUR LE CONTROLLER
// ================================================================

/**
 * Données pour créer une demande de délégation
 */
data class CreateDelegationData(
    val proprietaireId: Int,
    val beneficiaireId: Int,
    val portee: PorteePermission,
    val cibleId: Int?,
    val typePermission: TypePermission,
    val raisonDemande: String?,
    val dateExpiration: LocalDateTime?
)

/**
 * Rôles pour le tableau de bord
 */
enum class DashboardRole {
    OWNER, BENEFICIARY, ADMIN
}

/**
 * Critères de recherche pour les délégations
 */
data class DelegationSearchCriteria(
    val proprietaireId: Int? = null,
    val beneficiaireId: Int? = null,
    val statut: StatutDemande? = null,
    val portee: PorteePermission? = null,
    val typePermission: TypePermission? = null,
    val dateDebutCreation: LocalDateTime? = null,
    val dateFinCreation: LocalDateTime? = null
)

/**
 * Résultat de recherche de délégation
 */
data class DelegationSearchResult(
    val demande: DemandeDelegation,
    val ownerName: String,
    val beneficiaryName: String,
    val targetName: String,
    val isExpired: Boolean
)

/**
 * Tableau de bord des délégations
 */
data class DelegationDashboard(
    val userId: Int,
    val role: DashboardRole,
    val pendingRequests: List<DemandeDelegation>,
    val activePermissions: List<PermissionActive>,
    val recentActivity: List<DelegationActivity>,
    val statistics: DelegationStatistics
)

/**
 * Statistiques des délégations
 */
data class DelegationStatistics(
    val totalRequests: Int,
    val pendingRequests: Int,
    val approvedRequests: Int,
    val rejectedRequests: Int,
    val activePermissions: Int
)

/**
 * Activité de délégation
 */
data class DelegationActivity(
    val action: String,
    val timestamp: LocalDateTime,
    val description: String
)

/**
 * Résultat de validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

// ================================================================
// RÉSULTATS D'OPÉRATIONS
// ================================================================

sealed class DelegationCreateResult {
    data class Success(val demande: DemandeDelegation, val message: String) : DelegationCreateResult()
    data class Error(val message: String) : DelegationCreateResult()
    data class ValidationError(val errors: List<String>) : DelegationCreateResult()
}

sealed class DelegationApprovalResult {
    data class Success(val demande: DemandeDelegation, val permission: PermissionActive, val message: String) : DelegationApprovalResult()
    data class Error(val message: String) : DelegationApprovalResult()
}

sealed class DelegationRejectionResult {
    data class Success(val demande: DemandeDelegation, val message: String) : DelegationRejectionResult()
    data class Error(val message: String) : DelegationRejectionResult()
}

sealed class PermissionRevocationResult {
    data class Success(val permission: PermissionActive, val message: String) : PermissionRevocationResult()
    data class Error(val message: String) : PermissionRevocationResult()
}