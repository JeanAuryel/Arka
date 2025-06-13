package controllers

import repositories.FamilyMemberRepository
import repositories.FamilyRepository
import utils.PasswordHasher
import ktorm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Controller pour la gestion des membres de famille
 *
 * Responsabilités:
 * - CRUD des membres de famille
 * - Gestion des rôles et permissions
 * - Validation des données membres
 * - Statistiques et rapports membres
 * - Opérations sur les profils utilisateur
 * - Gestion des relations famille-membres
 *
 * Utilisé par: UI Components, AuthController, MainController
 * Utilise: FamilyMemberRepository, FamilyRepository, PasswordHasher
 *
 * Pattern: Controller Layer + Result Pattern (standardisé avec AuthController)
 */
class FamilyMemberController(
    private val familyMemberRepository: FamilyMemberRepository,
    private val familyRepository: FamilyRepository,
    private val passwordHasher: PasswordHasher
) {

    /**
     * Résultats des opérations sur les membres de famille - PATTERN STANDARDISÉ
     */
    sealed class FamilyMemberResult<out T> {
        data class Success<T>(val data: T) : FamilyMemberResult<T>()
        data class Error(val message: String, val code: FamilyMemberErrorCode) : FamilyMemberResult<Nothing>()
    }

    enum class FamilyMemberErrorCode {
        MEMBER_NOT_FOUND,
        EMAIL_ALREADY_EXISTS,
        FAMILY_NOT_FOUND,
        INVALID_INPUT,
        PERMISSION_DENIED,
        CANNOT_DELETE_LAST_ADMIN,
        WEAK_PASSWORD,
        INTERNAL_ERROR
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET MODIFICATION
    // ================================================================

    /**
     * Crée un nouveau membre de famille
     *
     * @param memberData Données du nouveau membre
     * @return Résultat de la création
     */
    suspend fun createFamilyMember(memberData: CreateMemberRequest): FamilyMemberResult<MembreFamille> = withContext(Dispatchers.IO) {
        try {
            // Validation des données
            val validationError = validateMemberData(memberData)
            if (validationError != null) {
                return@withContext FamilyMemberResult.Error(validationError, FamilyMemberErrorCode.INVALID_INPUT)
            }

            // Vérifier que la famille existe
            val family = familyRepository.findById(memberData.familyId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Famille non trouvée",
                    FamilyMemberErrorCode.FAMILY_NOT_FOUND
                )

            // Vérifier que l'email n'existe pas déjà
            if (familyMemberRepository.existsByEmail(memberData.email)) {
                return@withContext FamilyMemberResult.Error(
                    "Cette adresse email est déjà utilisée",
                    FamilyMemberErrorCode.EMAIL_ALREADY_EXISTS
                )
            }

            // Valider le mot de passe
            if (!isValidPassword(memberData.password)) {
                return@withContext FamilyMemberResult.Error(
                    "Le mot de passe ne respecte pas les critères de sécurité",
                    FamilyMemberErrorCode.WEAK_PASSWORD
                )
            }

            // Créer l'entité membre
            val hashedPassword = passwordHasher.hashPassword(memberData.password)
            val memberEntity = MembreFamilleEntity {
                prenomMembre = memberData.firstName
                mailMembre = memberData.email.lowercase().trim()
                mdpMembre = hashedPassword
                // Gestion explicite de la nullabilité pour dateNaissanceMembre
                dateNaissanceMembre = memberData.birthDate ?: LocalDate.of(2000, 1, 1) // Date par défaut si null
                genreMembre = memberData.gender ?: "" // Valeur par défaut si null
                estResponsable = memberData.isResponsible
                estAdmin = memberData.isAdmin
                familleId = memberData.familyId
                dateAjoutMembre = LocalDateTime.now()
            }

            // Sauvegarder
            val savedMember = familyMemberRepository.create(memberEntity)
            return@withContext FamilyMemberResult.Success(savedMember.toModel())

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la création du membre: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Met à jour les informations d'un membre
     *
     * @param memberId ID du membre
     * @param updateData Nouvelles données
     * @return Résultat de la mise à jour
     */
    suspend fun updateFamilyMember(
        memberId: Int,
        updateData: UpdateMemberRequest
    ): FamilyMemberResult<MembreFamille> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le membre existe
            val member = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé",
                    FamilyMemberErrorCode.MEMBER_NOT_FOUND
                )

            // Validation des nouvelles données
            val validationError = validateUpdateData(updateData)
            if (validationError != null) {
                return@withContext FamilyMemberResult.Error(validationError, FamilyMemberErrorCode.INVALID_INPUT)
            }

            // Vérifier email si changé
            if (updateData.email != null && updateData.email != member.mailMembre) {
                if (familyMemberRepository.existsByEmail(updateData.email)) {
                    return@withContext FamilyMemberResult.Error(
                        "Cette adresse email est déjà utilisée",
                        FamilyMemberErrorCode.EMAIL_ALREADY_EXISTS
                    )
                }
            }

            // Appliquer les modifications avec gestion de nullabilité
            updateData.firstName?.let { member.prenomMembre = it }
            updateData.email?.let { member.mailMembre = it.lowercase().trim() }
            updateData.birthDate?.let { member.dateNaissanceMembre = it } // Seulement si non-null
            updateData.gender?.let { member.genreMembre = it }

            // Mettre à jour
            familyMemberRepository.update(member)

            return@withContext FamilyMemberResult.Success(member.toModel())

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la mise à jour: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE CONSULTATION
    // ================================================================

    /**
     * Récupère un membre par son ID
     *
     * @param memberId ID du membre
     * @return Résultat avec le membre trouvé
     */
    suspend fun getFamilyMemberById(memberId: Int): FamilyMemberResult<MembreFamille> = withContext(Dispatchers.IO) {
        try {
            val member = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé",
                    FamilyMemberErrorCode.MEMBER_NOT_FOUND
                )

            return@withContext FamilyMemberResult.Success(member.toModel())

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la récupération: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère tous les membres d'une famille
     *
     * @param familyId ID de la famille
     * @return Résultat avec la liste des membres
     */
    suspend fun getFamilyMembers(familyId: Int): FamilyMemberResult<List<MembreFamille>> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la famille existe
            val family = familyRepository.findById(familyId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Famille non trouvée",
                    FamilyMemberErrorCode.FAMILY_NOT_FOUND
                )

            val members = familyMemberRepository.findByFamilyId(familyId)
            val memberModels = members.map { it.toModel() }

            return@withContext FamilyMemberResult.Success(memberModels)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la récupération des membres: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère les administrateurs d'une famille
     *
     * @param familyId ID de la famille
     * @return Résultat avec la liste des administrateurs
     */
    suspend fun getFamilyAdmins(familyId: Int): FamilyMemberResult<List<MembreFamille>> = withContext(Dispatchers.IO) {
        try {
            val admins = familyMemberRepository.findAdminsByFamilyId(familyId)
            val adminModels = admins.map { it.toModel() }

            return@withContext FamilyMemberResult.Success(adminModels)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la récupération des administrateurs: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Recherche des membres par nom
     *
     * @param searchTerm Terme de recherche
     * @param familyId ID de la famille (optionnel)
     * @return Résultat avec les membres trouvés
     */
    suspend fun searchMembers(
        searchTerm: String,
        familyId: Int? = null
    ): FamilyMemberResult<List<MembreFamille>> = withContext(Dispatchers.IO) {
        try {
            if (searchTerm.isBlank()) {
                return@withContext FamilyMemberResult.Error(
                    "Terme de recherche requis",
                    FamilyMemberErrorCode.INVALID_INPUT
                )
            }

            val members = familyMemberRepository.searchByName(searchTerm, familyId)
            val memberModels = members.map { it.toModel() }

            return@withContext FamilyMemberResult.Success(memberModels)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la recherche: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE GESTION DES RÔLES
    // ================================================================

    /**
     * Met à jour les rôles d'un membre
     *
     * @param memberId ID du membre
     * @param isAdmin Nouveau statut admin
     * @param isResponsible Nouveau statut responsable
     * @return Résultat de la mise à jour
     */
    suspend fun updateMemberRoles(
        memberId: Int,
        isAdmin: Boolean,
        isResponsible: Boolean
    ): FamilyMemberResult<MembreFamille> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le membre existe
            val member = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé",
                    FamilyMemberErrorCode.MEMBER_NOT_FOUND
                )

            // Si on retire les droits admin, vérifier qu'il ne soit pas le dernier admin
            if (!isAdmin && member.estAdmin) {
                val adminCount = familyMemberRepository.countAdminsByFamilyId(member.familleId)
                if (adminCount <= 1) {
                    return@withContext FamilyMemberResult.Error(
                        "Impossible de retirer les droits au dernier administrateur",
                        FamilyMemberErrorCode.CANNOT_DELETE_LAST_ADMIN
                    )
                }
            }

            // Mettre à jour les rôles
            familyMemberRepository.updateRoles(memberId, isAdmin, isResponsible)

            // Récupérer le membre mis à jour
            val updatedMember = familyMemberRepository.findById(memberId)!!
            return@withContext FamilyMemberResult.Success(updatedMember.toModel())

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la mise à jour des rôles: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Promeut un membre au rang d'administrateur
     *
     * @param memberId ID du membre
     * @return Résultat de la promotion
     */
    suspend fun promoteToAdmin(memberId: Int): FamilyMemberResult<MembreFamille> = withContext(Dispatchers.IO) {
        try {
            familyMemberRepository.promoteToAdmin(memberId)
            val updatedMember = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé après promotion",
                    FamilyMemberErrorCode.MEMBER_NOT_FOUND
                )

            return@withContext FamilyMemberResult.Success(updatedMember.toModel())

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la promotion: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION
    // ================================================================

    /**
     * Supprime un membre de famille
     *
     * @param memberId ID du membre à supprimer
     * @return Résultat de la suppression
     */
    suspend fun deleteFamilyMember(memberId: Int): FamilyMemberResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le membre existe
            val member = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé",
                    FamilyMemberErrorCode.MEMBER_NOT_FOUND
                )

            // Vérifier qu'on ne supprime pas le dernier admin
            if (member.estAdmin) {
                val adminCount = familyMemberRepository.countAdminsByFamilyId(member.familleId)
                if (adminCount <= 1) {
                    return@withContext FamilyMemberResult.Error(
                        "Impossible de supprimer le dernier administrateur",
                        FamilyMemberErrorCode.CANNOT_DELETE_LAST_ADMIN
                    )
                }
            }

            // Supprimer
            val deleted = familyMemberRepository.delete(memberId)
            if (deleted == 0) {
                return@withContext FamilyMemberResult.Error(
                    "Aucun membre supprimé",
                    FamilyMemberErrorCode.INTERNAL_ERROR
                )
            }

            return@withContext FamilyMemberResult.Success(Unit)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la suppression: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques des membres d'une famille
     *
     * @param familyId ID de la famille
     * @return Résultat avec les statistiques
     */
    suspend fun getFamilyMemberStatistics(familyId: Int): FamilyMemberResult<FamilyMemberStatistics> = withContext(Dispatchers.IO) {
        try {
            val totalCount = familyMemberRepository.countByFamilyId(familyId)
            val adminCount = familyMemberRepository.countAdminsByFamilyId(familyId)
            val responsibleCount = familyMemberRepository.countResponsiblesByFamilyId(familyId)
            val childrenCount = familyMemberRepository.countChildrenByFamilyId(familyId)
            val lastActivity = familyMemberRepository.getLastActivityByFamilyId(familyId)

            val stats = FamilyMemberStatistics(
                familyId = familyId,
                totalMembers = totalCount,
                adminCount = adminCount,
                responsibleCount = responsibleCount,
                childrenCount = childrenCount,
                lastActivity = lastActivity
            )

            return@withContext FamilyMemberResult.Success(stats)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors du calcul des statistiques: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES DE VALIDATION
    // ================================================================

    private fun validateMemberData(memberData: CreateMemberRequest): String? {
        return when {
            memberData.firstName.isBlank() -> "Le prénom ne peut pas être vide"
            memberData.firstName.length < 2 -> "Le prénom doit contenir au moins 2 caractères"
            memberData.firstName.length > 50 -> "Le prénom ne peut pas dépasser 50 caractères"
            !isValidEmail(memberData.email) -> "Format d'email invalide"
            memberData.birthDate == null -> "La date de naissance est requise"
            memberData.birthDate.isAfter(LocalDate.now()) ->
                "La date de naissance ne peut pas être dans le futur"
            else -> null
        }
    }

    private fun validateUpdateData(updateData: UpdateMemberRequest): String? {
        return when {
            updateData.firstName != null && updateData.firstName.isBlank() ->
                "Le prénom ne peut pas être vide"
            updateData.firstName != null && updateData.firstName.length < 2 ->
                "Le prénom doit contenir au moins 2 caractères"
            updateData.firstName != null && updateData.firstName.length > 50 ->
                "Le prénom ne peut pas dépasser 50 caractères"
            updateData.email != null && !isValidEmail(updateData.email) ->
                "Format d'email invalide"
            updateData.birthDate != null && updateData.birthDate.isAfter(LocalDate.now()) ->
                "La date de naissance ne peut pas être dans le futur"
            else -> null
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isLetter() }
    }
}

// ================================================================
// DATA CLASSES POUR LES REQUÊTES ET RÉSULTATS
// ================================================================

/**
 * Données pour créer un nouveau membre
 */
data class CreateMemberRequest(
    val firstName: String,
    val email: String,
    val password: String,
    val birthDate: LocalDate?,
    val gender: String?,
    val isAdmin: Boolean = false,
    val isResponsible: Boolean = false,
    val familyId: Int
)

/**
 * Données pour mettre à jour un membre
 */
data class UpdateMemberRequest(
    val firstName: String? = null,
    val email: String? = null,
    val birthDate: LocalDate? = null,
    val gender: String? = null
)

/**
 * Statistiques des membres d'une famille
 */
data class FamilyMemberStatistics(
    val familyId: Int,
    val totalMembers: Int,
    val adminCount: Int,
    val responsibleCount: Int,
    val childrenCount: Int,
    val lastActivity: LocalDateTime?
)