package controllers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import repositories.SpaceRepository
import repositories.CategoryRepository
import repositories.MemberSpaceRepository
import ktorm.Espace
import ktorm.MembreEspace
import ktorm.MembreFamille
import java.time.LocalDateTime

/**
 * Controller responsible for space management operations.
 * Based on the Espace entity from ktorm models.
 *
 * Key Responsibilities:
 * - CRUD operations on spaces (Espace)
 * - Member access management to spaces (MembreEspace)
 * - Space organization and hierarchy
 * - Integration with categories within spaces
 *
 * In Arka Architecture:
 * Space -> Contains -> Categories -> Contains -> Folders -> Contains -> Files
 */
class SpaceController(
    private val spaceRepository: SpaceRepository,
    private val categoryRepository: CategoryRepository,
    private val memberSpaceRepository: MemberSpaceRepository,
    private val authController: AuthController,
    private val permissionController: PermissionController
) {

    /**
     * Result wrapper for space operations
     */
    sealed class SpaceResult {
        data class Success<T>(val data: T) : SpaceResult()
        data class Error(val message: String, val code: SpaceErrorCode) : SpaceResult()
    }

    enum class SpaceErrorCode {
        NOT_FOUND,
        ALREADY_EXISTS,
        INVALID_INPUT,
        PERMISSION_DENIED,
        HAS_CATEGORIES,
        INTERNAL_ERROR,
        ACCESS_DENIED
    }

    /**
     * Get all spaces accessible to the current user
     */
    suspend fun getAccessibleSpaces(): SpaceResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext SpaceResult.Error(
                    "Utilisateur non connecté",
                    SpaceErrorCode.PERMISSION_DENIED
                )

            val spaces = if (currentUser.estAdmin || currentUser.estResponsable) {
                // Admins and responsible users can see all spaces
                spaceRepository.findAll()
            } else {
                // Regular users see only spaces they have access to
                spaceRepository.findAccessibleByUserId(currentUser.membreFamilleId)
            }

            return@withContext SpaceResult.Success(spaces)

        } catch (e: Exception) {
            return@withContext SpaceResult.Error(
                "Erreur lors de la récupération des espaces: ${e.message}",
                SpaceErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Get a specific space by ID
     */
    suspend fun getSpaceById(spaceId: Int): SpaceResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext SpaceResult.Error(
                    "Utilisateur non connecté",
                    SpaceErrorCode.PERMISSION_DENIED
                )

            val space = spaceRepository.findById(spaceId)
                ?: return@withContext SpaceResult.Error(
                    "Espace non trouvé",
                    SpaceErrorCode.NOT_FOUND
                )

            // Check access permissions
            if (!hasSpaceAccess(currentUser.membreFamilleId, spaceId)) {
                return@withContext SpaceResult.Error(
                    "Accès refusé à cet espace",
                    SpaceErrorCode.ACCESS_DENIED
                )
            }

            return@withContext SpaceResult.Success(space)

        } catch (e: Exception) {
            return@withContext SpaceResult.Error(
                "Erreur lors de la récupération de l'espace: ${e.message}",
                SpaceErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Create a new space
     * Only admins and responsible users can create spaces
     */
    suspend fun createSpace(
        name: String,
        description: String? = null
    ): SpaceResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext SpaceResult.Error(
                    "Utilisateur non connecté",
                    SpaceErrorCode.PERMISSION_DENIED
                )

            // Permission check
            if (!canManageSpaces(currentUser)) {
                return@withContext SpaceResult.Error(
                    "Permissions insuffisantes pour créer un espace",
                    SpaceErrorCode.PERMISSION_DENIED
                )
            }

            // Validation based on ktorm Espace model
            if (name.isBlank()) {
                return@withContext SpaceResult.Error(
                    "Le nom de l'espace est requis",
                    SpaceErrorCode.INVALID_INPUT
                )
            }

            if (name.length > 100) {
                return@withContext SpaceResult.Error(
                    "Le nom de l'espace ne peut pas dépasser 100 caractères",
                    SpaceErrorCode.INVALID_INPUT
                )
            }

            // Check if space name already exists
            if (spaceRepository.existsByName(name)) {
                return@withContext SpaceResult.Error(
                    "Un espace avec ce nom existe déjà",
                    SpaceErrorCode.ALREADY_EXISTS
                )
            }

            // Create space based on ktorm Espace model
            val newSpace = Espace(
                espaceId = 0, // Auto-generated
                nomEspace = name.trim(),
                descriptionEspace = description?.trim(),
                dateCreationEspace = LocalDateTime.now()
            )

            val createdSpace = spaceRepository.create(newSpace)

            // Automatically grant access to the creator
            grantSpaceAccess(currentUser.membreFamilleId, createdSpace.espaceId)

            return@withContext SpaceResult.Success(createdSpace)

        } catch (e: Exception) {
            return@withContext SpaceResult.Error(
                "Erreur lors de la création de l'espace: ${e.message}",
                SpaceErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Update an existing space
     */
    suspend fun updateSpace(
        spaceId: Int,
        name: String,
        description: String? = null
    ): SpaceResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext SpaceResult.Error(
                    "Utilisateur non connecté",
                    SpaceErrorCode.PERMISSION_DENIED
                )

            // Permission check
            if (!canManageSpaces(currentUser)) {
                return@withContext SpaceResult.Error(
                    "Permissions insuffisantes pour modifier un espace",
                    SpaceErrorCode.PERMISSION_DENIED
                )
            }

            // Find existing space
            val existingSpace = spaceRepository.findById(spaceId)
                ?: return@withContext SpaceResult.Error(
                    "Espace non trouvé",
                    SpaceErrorCode.NOT_FOUND
                )

            // Validation
            if (name.isBlank()) {
                return@withContext SpaceResult.Error(
                    "Le nom de l'espace est requis",
                    SpaceErrorCode.INVALID_INPUT
                )
            }

            if (name.length > 100) {
                return@withContext SpaceResult.Error(
                    "Le nom de l'espace ne peut pas dépasser 100 caractères",
                    SpaceErrorCode.INVALID_INPUT
                )
            }

            // Check if new name conflicts (excluding current space)
            if (name != existingSpace.nomEspace &&
                spaceRepository.existsByName(name)) {
                return@withContext SpaceResult.Error(
                    "Un espace avec ce nom existe déjà",
                    SpaceErrorCode.ALREADY_EXISTS
                )
            }

            // Update space using ktorm copy
            val updatedSpace = existingSpace.copy(
                nomEspace = name.trim(),
                descriptionEspace = description?.trim()
            )

            spaceRepository.update(updatedSpace)

            return@withContext SpaceResult.Success(updatedSpace)

        } catch (e: Exception) {
            return@withContext SpaceResult.Error(
                "Erreur lors de la mise à jour de l'espace: ${e.message}",
                SpaceErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Delete a space
     * Only possible if no categories exist in the space
     */
    suspend fun deleteSpace(spaceId: Int): SpaceResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext SpaceResult.Error(
                    "Utilisateur non connecté",
                    SpaceErrorCode.PERMISSION_DENIED
                )

            // Permission check
            if (!canManageSpaces(currentUser)) {
                return@withContext SpaceResult.Error(
                    "Permissions insuffisantes pour supprimer un espace",
                    SpaceErrorCode.PERMISSION_DENIED
                )
            }

            // Check if space exists
            val space = spaceRepository.findById(spaceId)
                ?: return@withContext SpaceResult.Error(
                    "Espace non trouvé",
                    SpaceErrorCode.NOT_FOUND
                )

            // Check if space has categories
            if (categoryRepository.countBySpaceId(spaceId) > 0) {
                return@withContext SpaceResult.Error(
                    "Impossible de supprimer un espace contenant des catégories",
                    SpaceErrorCode.HAS_CATEGORIES
                )
            }

            // Delete member access records first
            memberSpaceRepository.deleteBySpaceId(spaceId)

            // Delete space
            spaceRepository.delete(spaceId)

            return@withContext SpaceResult.Success(space)

        } catch (e: Exception) {
            return@withContext SpaceResult.Error(
                "Erreur lors de la suppression de l'espace: ${e.message}",
                SpaceErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Grant access to a space for a specific member
     * Based on MembreEspace ktorm entity
     */
    suspend fun grantSpaceAccess(memberId: Int, spaceId: Int): SpaceResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext SpaceResult.Error(
                    "Utilisateur non connecté",
                    SpaceErrorCode.PERMISSION_DENIED
                )

            // Permission check
            if (!canManageSpaceAccess(currentUser)) {
                return@withContext SpaceResult.Error(
                    "Permissions insuffisantes pour gérer les accès",
                    SpaceErrorCode.PERMISSION_DENIED
                )
            }

            // Check if space exists
            if (!spaceRepository.exists(spaceId)) {
                return@withContext SpaceResult.Error(
                    "Espace non trouvé",
                    SpaceErrorCode.NOT_FOUND
                )
            }

            // Check if access already exists
            if (memberSpaceRepository.hasAccess(memberId, spaceId)) {
                return@withContext SpaceResult.Error(
                    "L'accès existe déjà",
                    SpaceErrorCode.ALREADY_EXISTS
                )
            }

            // Create MembreEspace record based on ktorm model
            val memberSpace = MembreEspace(
                membreFamilleId = memberId,
                espaceId = spaceId,
                dateAcces = LocalDateTime.now()
            )

            memberSpaceRepository.create(memberSpace)

            return@withContext SpaceResult.Success(memberSpace)

        } catch (e: Exception) {
            return@withContext SpaceResult.Error(
                "Erreur lors de l'octroi d'accès: ${e.message}",
                SpaceErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Revoke access to a space for a specific member
     */
    suspend fun revokeSpaceAccess(memberId: Int, spaceId: Int): SpaceResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext SpaceResult.Error(
                    "Utilisateur non connecté",
                    SpaceErrorCode.PERMISSION_DENIED
                )

            // Permission check
            if (!canManageSpaceAccess(currentUser)) {
                return@withContext SpaceResult.Error(
                    "Permissions insuffisantes pour gérer les accès",
                    SpaceErrorCode.PERMISSION_DENIED
                )
            }

            // Remove access
            val removed = memberSpaceRepository.removeAccess(memberId, spaceId)

            if (!removed) {
                return@withContext SpaceResult.Error(
                    "Accès non trouvé",
                    SpaceErrorCode.NOT_FOUND
                )
            }

            return@withContext SpaceResult.Success(Unit)

        } catch (e: Exception) {
            return@withContext SpaceResult.Error(
                "Erreur lors de la révocation d'accès: ${e.message}",
                SpaceErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Get all members who have access to a specific space
     */
    suspend fun getSpaceMembers(spaceId: Int): SpaceResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext SpaceResult.Error(
                    "Utilisateur non connecté",
                    SpaceErrorCode.PERMISSION_DENIED
                )

            // Check access to space
            if (!hasSpaceAccess(currentUser.membreFamilleId, spaceId)) {
                return@withContext SpaceResult.Error(
                    "Accès refusé à cet espace",
                    SpaceErrorCode.ACCESS_DENIED
                )
            }

            val spaceMembers = memberSpaceRepository.findMembersBySpaceId(spaceId)

            return@withContext SpaceResult.Success(spaceMembers)

        } catch (e: Exception) {
            return@withContext SpaceResult.Error(
                "Erreur lors de la récupération des membres: ${e.message}",
                SpaceErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Get all spaces accessible to a specific member
     */
    suspend fun getMemberSpaces(memberId: Int): SpaceResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext SpaceResult.Error(
                    "Utilisateur non connecté",
                    SpaceErrorCode.PERMISSION_DENIED
                )

            // Permission check: can view own spaces or admin can view others
            if (memberId != currentUser.membreFamilleId && !currentUser.estAdmin) {
                return@withContext SpaceResult.Error(
                    "Permissions insuffisantes",
                    SpaceErrorCode.PERMISSION_DENIED
                )
            }

            val memberSpaces = memberSpaceRepository.findSpacesByMemberId(memberId)

            return@withContext SpaceResult.Success(memberSpaces)

        } catch (e: Exception) {
            return@withContext SpaceResult.Error(
                "Erreur lors de la récupération des espaces: ${e.message}",
                SpaceErrorCode.INTERNAL_ERROR
            )
        }
    }

    // Private helper methods

    private suspend fun hasSpaceAccess(memberId: Int, spaceId: Int): Boolean {
        return try {
            permissionController.hasSpaceAccess(memberId, spaceId)
        } catch (e: Exception) {
            false
        }
    }

    private fun canManageSpaces(user: MembreFamille): Boolean {
        return user.estAdmin || user.estResponsable
    }

    private fun canManageSpaceAccess(user: MembreFamille): Boolean {
        return user.estAdmin || user.estResponsable
    }
}

/**
 * Data classes for space operations results
 * Based on ktorm Espace and MembreEspace models
 */
data class SpaceWithAccess(
    val space: Espace,
    val hasAccess: Boolean,
    val accessDate: LocalDateTime?
)

data class SpaceMemberInfo(
    val memberSpace: MembreEspace,
    val memberName: String,
    val memberEmail: String
)