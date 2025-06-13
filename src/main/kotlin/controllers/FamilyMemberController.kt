package controllers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import repositories.FamilyMemberRepository
import repositories.FolderRepository
import utils.PasswordHasher
import ktorm.MembreFamille
import ktorm.Genre
import java.time.LocalDateTime
import java.time.LocalDate

/**
 * Controller responsible for family member management operations.
 *
 * Key Features:
 * - CRUD operations on family members
 * - Role management (admin, responsible)
 * - Member profile updates
 * - Family hierarchy management
 * - Integration with permission system
 *
 * Business Rules:
 * - Only admins can manage other family members
 * - Users can update their own profile
 * - At least one admin must remain in each family
 * - Children have restricted permissions by default
 */
class FamilyMemberController(
    private val familyMemberRepository: FamilyMemberRepository,
    private val folderRepository: FolderRepository,
    private val passwordHasher: PasswordHasher,
    private val authController: AuthController,
    private val permissionController: PermissionController
) {

    /**
     * Result wrapper for family member operations
     */
    sealed class FamilyMemberResult {
        data class Success<T>(val data: T) : FamilyMemberResult()
        data class Error(val message: String, val code: FamilyMemberErrorCode) : FamilyMemberResult()
    }

    enum class FamilyMemberErrorCode {
        NOT_FOUND,
        ALREADY_EXISTS,
        INVALID_INPUT,
        PERMISSION_DENIED,
        LAST_ADMIN,
        EMAIL_IN_USE,
        INTERNAL_ERROR,
        CANNOT_DELETE_SELF,
        AGE_RESTRICTION
    }

    /**
     * Get all family members in the current user's family
     */
    suspend fun getFamilyMembers(): FamilyMemberResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext FamilyMemberResult.Error(
                    "Utilisateur non connecté",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )

            val members = familyMemberRepository.findByFamilyId(currentUser.familleId)

            return@withContext FamilyMemberResult.Success(members)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la récupération des membres: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Get a specific family member by ID
     */
    suspend fun getFamilyMemberById(memberId: Int): FamilyMemberResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext FamilyMemberResult.Error(
                    "Utilisateur non connecté",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )

            val member = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé",
                    FamilyMemberErrorCode.NOT_FOUND
                )

            // Check if user can view this member (same family or admin/responsible)
            if (!canViewMember(currentUser, member)) {
                return@withContext FamilyMemberResult.Error(
                    "Accès refusé à ce membre",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )
            }

            return@withContext FamilyMemberResult.Success(member)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la récupération du membre: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Create a new family member
     * Only admins and responsible users can add new members
     */
    suspend fun createFamilyMember(
        firstName: String,
        email: String,
        password: String,
        dateOfBirth: LocalDate,
        gender: String,
        isResponsible: Boolean = false,
        isAdmin: Boolean = false
    ): FamilyMemberResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext FamilyMemberResult.Error(
                    "Utilisateur non connecté",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )

            // Permission check
            if (!canManageFamilyMembers(currentUser)) {
                return@withContext FamilyMemberResult.Error(
                    "Permissions insuffisantes pour ajouter un membre",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )
            }

            // Validation
            val validationResult = validateMemberInput(firstName, email, password, dateOfBirth, gender)
            if (validationResult != null) {
                return@withContext validationResult
            }

            // Check if email already exists
            if (familyMemberRepository.existsByEmail(email)) {
                return@withContext FamilyMemberResult.Error(
                    "Cette adresse email est déjà utilisée",
                    FamilyMemberErrorCode.EMAIL_IN_USE
                )
            }

            // Hash password
            val hashedPassword = passwordHasher.hashPassword(password)

            // Determine age and set appropriate defaults
            val age = calculateAge(dateOfBirth)
            val actualIsAdmin = if (age < 18) false else isAdmin // Minors cannot be admin
            val actualIsResponsible = if (age < 18) false else isResponsible // Minors cannot be responsible

            // Create new member
            val newMember = MembreFamille(
                membreFamilleId = 0, // Auto-generated
                prenomMembre = firstName.trim(),
                mailMembre = email.lowercase().trim(),
                mdpMembre = hashedPassword,
                dateNaissanceMembre = dateOfBirth,
                genreMembre = Genre.valueOf(gender),
                estResponsable = actualIsResponsible,
                estAdmin = actualIsAdmin,
                dateAjoutMembre = LocalDateTime.now(),
                familleId = currentUser.familleId
            )

            val createdMember = familyMemberRepository.create(newMember)

            // Create default personal folders for the new member
            createDefaultPersonalFolders(createdMember.membreFamilleId)

            return@withContext FamilyMemberResult.Success(createdMember)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la création du membre: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Update a family member's profile
     */
    suspend fun updateFamilyMember(
        memberId: Int,
        firstName: String?,
        email: String?,
        dateOfBirth: LocalDate?,
        gender: String?
    ): FamilyMemberResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext FamilyMemberResult.Error(
                    "Utilisateur non connecté",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )

            val existingMember = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé",
                    FamilyMemberErrorCode.NOT_FOUND
                )

            // Permission check: can edit self or admin can edit others
            if (!canEditMember(currentUser, existingMember)) {
                return@withContext FamilyMemberResult.Error(
                    "Permissions insuffisantes pour modifier ce membre",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )
            }

            // Build updated member with only non-null values
            var updatedMember = existingMember

            firstName?.let {
                if (it.isBlank()) {
                    return@withContext FamilyMemberResult.Error(
                        "Le prénom ne peut pas être vide",
                        FamilyMemberErrorCode.INVALID_INPUT
                    )
                }
                updatedMember = updatedMember.copy(prenomMembre = it.trim())
            }

            email?.let {
                if (!isValidEmail(it)) {
                    return@withContext FamilyMemberResult.Error(
                        "Format d'email invalide",
                        FamilyMemberErrorCode.INVALID_INPUT
                    )
                }
                if (it != existingMember.mailMembre && familyMemberRepository.existsByEmail(it)) {
                    return@withContext FamilyMemberResult.Error(
                        "Cette adresse email est déjà utilisée",
                        FamilyMemberErrorCode.EMAIL_IN_USE
                    )
                }
                updatedMember = updatedMember.copy(mailMembre = it.lowercase().trim())
            }

            dateOfBirth?.let {
                if (it.isAfter(LocalDate.now())) {
                    return@withContext FamilyMemberResult.Error(
                        "La date de naissance ne peut pas être dans le futur",
                        FamilyMemberErrorCode.INVALID_INPUT
                    )
                }
                updatedMember = updatedMember.copy(dateNaissanceMembre = it)
            }

            gender?.let {
                if (!isValidGender(it)) {
                    return@withContext FamilyMemberResult.Error(
                        "Genre invalide",
                        FamilyMemberErrorCode.INVALID_INPUT
                    )
                }
                updatedMember = updatedMember.copy(genreMembre = it)
            }

            familyMemberRepository.update(updatedMember)

            return@withContext FamilyMemberResult.Success(updatedMember)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la mise à jour du membre: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Update member roles (admin, responsible)
     * Only admins can change roles
     */
    suspend fun updateMemberRoles(
        memberId: Int,
        isAdmin: Boolean,
        isResponsible: Boolean
    ): FamilyMemberResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext FamilyMemberResult.Error(
                    "Utilisateur non connecté",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )

            // Only admins can change roles
            if (!currentUser.estAdmin) {
                return@withContext FamilyMemberResult.Error(
                    "Seuls les administrateurs peuvent modifier les rôles",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )
            }

            val existingMember = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé",
                    FamilyMemberErrorCode.NOT_FOUND
                )

            // Check if removing admin role would leave no admins
            if (existingMember.estAdmin && !isAdmin) {
                val adminCount = familyMemberRepository.countAdminsByFamilyId(currentUser.familleId)
                if (adminCount <= 1) {
                    return@withContext FamilyMemberResult.Error(
                        "Il doit y avoir au moins un administrateur dans la famille",
                        FamilyMemberErrorCode.LAST_ADMIN
                    )
                }
            }

            // Check age restrictions for roles
            val age = calculateAge(existingMember.dateNaissanceMembre)
            if (age < 18 && (isAdmin || isResponsible)) {
                return@withContext FamilyMemberResult.Error(
                    "Les mineurs ne peuvent pas avoir de rôles administratifs",
                    FamilyMemberErrorCode.AGE_RESTRICTION
                )
            }

            val updatedMember = existingMember.copy(
                estAdmin = isAdmin,
                estResponsable = isResponsible
            )

            familyMemberRepository.update(updatedMember)

            return@withContext FamilyMemberResult.Success(updatedMember)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la mise à jour des rôles: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Delete a family member
     * Only admins can delete members, with restrictions
     */
    suspend fun deleteFamilyMember(memberId: Int): FamilyMemberResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext FamilyMemberResult.Error(
                    "Utilisateur non connecté",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )

            // Only admins can delete members
            if (!currentUser.estAdmin) {
                return@withContext FamilyMemberResult.Error(
                    "Seuls les administrateurs peuvent supprimer des membres",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )
            }

            // Cannot delete self
            if (memberId == currentUser.membreFamilleId) {
                return@withContext FamilyMemberResult.Error(
                    "Vous ne pouvez pas supprimer votre propre compte",
                    FamilyMemberErrorCode.CANNOT_DELETE_SELF
                )
            }

            val memberToDelete = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé",
                    FamilyMemberErrorCode.NOT_FOUND
                )

            // Check if deleting admin would leave no admins
            if (memberToDelete.estAdmin) {
                val adminCount = familyMemberRepository.countAdminsByFamilyId(currentUser.familleId)
                if (adminCount <= 1) {
                    return@withContext FamilyMemberResult.Error(
                        "Il doit y avoir au moins un administrateur dans la famille",
                        FamilyMemberErrorCode.LAST_ADMIN
                    )
                }
            }

            // Note: The database should handle cascade deletes for related data
            // (folders, files, permissions) as defined in the SQL schema
            familyMemberRepository.delete(memberId)

            return@withContext FamilyMemberResult.Success(memberToDelete)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la suppression du membre: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Get member statistics and activity summary
     */
    suspend fun getMemberStatistics(memberId: Int): FamilyMemberResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext FamilyMemberResult.Error(
                    "Utilisateur non connecté",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )

            val member = familyMemberRepository.findById(memberId)
                ?: return@withContext FamilyMemberResult.Error(
                    "Membre non trouvé",
                    FamilyMemberErrorCode.NOT_FOUND
                )

            // Check viewing permissions
            if (!canViewMember(currentUser, member)) {
                return@withContext FamilyMemberResult.Error(
                    "Accès refusé à ce membre",
                    FamilyMemberErrorCode.PERMISSION_DENIED
                )
            }

            val statistics = MemberStatistics(
                member = member,
                folderCount = folderRepository.countByMemberId(memberId),
                fileCount = folderRepository.countFilesByMemberId(memberId),
                totalFileSize = folderRepository.getTotalFileSizeByMemberId(memberId),
                age = calculateAge(member.dateNaissanceMembre)
            )

            return@withContext FamilyMemberResult.Success(statistics)

        } catch (e: Exception) {
            return@withContext FamilyMemberResult.Error(
                "Erreur lors de la récupération des statistiques: ${e.message}",
                FamilyMemberErrorCode.INTERNAL_ERROR
            )
        }
    }

    // Private helper methods

    private fun canViewMember(currentUser: FamilyMember, targetMember: FamilyMember): Boolean {
        return currentUser.familleId == targetMember.familleId &&
                (currentUser.estAdmin || currentUser.estResponsible ||
                        currentUser.membreFamilleId == targetMember.membreFamilleId)
    }

    private fun canEditMember(currentUser: FamilyMember, targetMember: FamilyMember): Boolean {
        return currentUser.familleId == targetMember.familleId &&
                (currentUser.estAdmin || currentUser.membreFamilleId == targetMember.membreFamilleId)
    }

    private fun canManageFamilyMembers(user: FamilyMember): Boolean {
        return user.estAdmin || user.estResponsable
    }

    private fun validateMemberInput(
        firstName: String,
        email: String,
        password: String,
        dateOfBirth: LocalDate,
        gender: String
    ): FamilyMemberResult.Error? {
        if (firstName.isBlank()) {
            return FamilyMemberResult.Error("Le prénom est requis", FamilyMemberErrorCode.INVALID_INPUT)
        }

        if (!isValidEmail(email)) {
            return FamilyMemberResult.Error("Format d'email invalide", FamilyMemberErrorCode.INVALID_INPUT)
        }

        if (!isValidPassword(password)) {
            return FamilyMemberResult.Error(
                "Le mot de passe doit contenir au moins 8 caractères, une lettre et un chiffre",
                FamilyMemberErrorCode.INVALID_INPUT
            )
        }

        if (dateOfBirth.isAfter(LocalDate.now())) {
            return FamilyMemberResult.Error(
                "La date de naissance ne peut pas être dans le futur",
                FamilyMemberErrorCode.INVALID_INPUT
            )
        }

        if (!isValidGender(gender)) {
            return FamilyMemberResult.Error("Genre invalide", FamilyMemberErrorCode.INVALID_INPUT)
        }

        return null
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        return emailRegex.matches(email)
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isLetter() }
    }

    private fun isValidGender(gender: String): Boolean {
        return gender in listOf("M", "F", "Autre")
    }

    private fun calculateAge(dateOfBirth: LocalDate): Int {
        return LocalDate.now().year - dateOfBirth.year -
                if (LocalDate.now().dayOfYear < dateOfBirth.dayOfYear) 1 else 0
    }

    /**
     * Create default personal folders for a new member
     * This should integrate with the CategoryController for default templates
     */
    private suspend fun createDefaultPersonalFolders(memberId: Int) {
        // This would typically create folders based on default templates
        // Implementation would depend on the specific requirements
        // For now, this is a placeholder for the functionality
        try {
            // TODO: Implement default folder creation based on category templates
            println("Creating default folders for member $memberId")
        } catch (e: Exception) {
            println("Warning: Could not create default folders for member $memberId: ${e.message}")
        }
    }
}

/**
 * Data class for member statistics
 */
data class MemberStatistics(
    val member: MembreFamille,
    val folderCount: Int,
    val fileCount: Int,
    val totalFileSize: Long,
    val age: Int
)