package controllers

import repositories.PermissionRepository
import repositories.FamilyMemberRepository
import repositories.FileRepository
import repositories.FolderRepository
import org.ktorm.schema.Column
import ktorm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Controller pour la gestion des permissions
 *
 * Responsabilités:
 * - Vérification des permissions d'accès aux ressources
 * - CRUD des permissions actives
 * - Validation des droits d'accès par contexte
 * - Gestion des expirations de permissions
 * - Audit et traçabilité des accès
 * - API unifiée pour toutes les vérifications de sécurité
 *
 * Utilisé par: Tous les controllers, UI Components, DelegationController
 * Utilise: PermissionRepository, FamilyMemberRepository, FileRepository, FolderRepository
 *
 * Pattern: Controller Layer + Result Pattern (standardisé avec AuthController)
 */
class PermissionController(
    private val permissionRepository: PermissionRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val fileRepository: FileRepository? = null,
    private val folderRepository: FolderRepository? = null
) {

    /**
     * Résultats des opérations de permission - PATTERN STANDARDISÉ
     */
    sealed class PermissionResult<out T> {
        data class Success<T>(val data: T) : PermissionResult<T>()
        data class Error(val message: String, val code: PermissionErrorCode) : PermissionResult<Nothing>()
    }

    enum class PermissionErrorCode {
        PERMISSION_NOT_FOUND,
        MEMBER_NOT_FOUND,
        RESOURCE_NOT_FOUND,
        ACCESS_DENIED,
        PERMISSION_EXPIRED,
        INVALID_INPUT,
        INTERNAL_ERROR
    }

    /**
     * Types de ressources supportées
     */
    enum class ResourceType {
        FILE,
        FOLDER,
        CATEGORY,
        SPACE
    }

    /**
     * Niveaux de permission
     */
    enum class PermissionLevel {
        READ,
        WRITE,
        DELETE,
        ADMIN
    }

    // ================================================================
    // MÉTHODES PRINCIPALES DE VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si un utilisateur a accès à une ressource
     *
     * @param userId ID de l'utilisateur
     * @param resourceType Type de ressource
     * @param resourceId ID de la ressource
     * @param requiredLevel Niveau de permission requis
     * @return Résultat de la vérification
     */
    suspend fun checkAccess(
        userId: Int,
        resourceType: ResourceType,
        resourceId: Int,
        requiredLevel: PermissionLevel = PermissionLevel.READ
    ): PermissionResult<AccessResult> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que l'utilisateur existe
            val user = familyMemberRepository.findById(userId)
                ?: return@withContext PermissionResult.Error(
                    "Utilisateur non trouvé",
                    PermissionErrorCode.MEMBER_NOT_FOUND
                )

            // Les admins ont accès à tout
            if (user.estAdmin) {
                return@withContext PermissionResult.Success(
                    AccessResult(
                        granted = true,
                        reason = "Accès administrateur",
                        level = PermissionLevel.ADMIN,
                        expiresAt = null
                    )
                )
            }

            // Vérifications spécifiques par type de ressource
            val accessResult = when (resourceType) {
                ResourceType.FILE -> checkFileAccess(userId, resourceId, requiredLevel)
                ResourceType.FOLDER -> checkFolderAccess(userId, resourceId, requiredLevel)
                ResourceType.CATEGORY -> checkCategoryAccess(userId, resourceId, requiredLevel)
                ResourceType.SPACE -> checkSpaceAccess(userId, resourceId, requiredLevel)
            }

            return@withContext PermissionResult.Success(accessResult)

        } catch (e: Exception) {
            return@withContext PermissionResult.Error(
                "Erreur lors de la vérification d'accès: ${e.message}",
                PermissionErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Vérifie si un utilisateur peut effectuer une action spécifique
     *
     * @param userId ID de l'utilisateur
     * @param action Action à vérifier
     * @param context Contexte de l'action
     * @return Résultat de la vérification
     */
    suspend fun canPerformAction(
        userId: Int,
        action: UserAction,
        context: ActionContext
    ): PermissionResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val canPerform = when (action) {
                UserAction.CREATE_FILE -> canCreateFile(userId, context.folderId)
                UserAction.READ_FILE -> canReadFile(userId, context.fileId)
                UserAction.UPDATE_FILE -> canUpdateFile(userId, context.fileId)
                UserAction.DELETE_FILE -> canDeleteFile(userId, context.fileId)
                UserAction.CREATE_FOLDER -> canCreateFolder(userId, context.categoryId, context.parentFolderId)
                UserAction.READ_FOLDER -> canReadFolder(userId, context.folderId)
                UserAction.UPDATE_FOLDER -> canUpdateFolder(userId, context.folderId)
                UserAction.DELETE_FOLDER -> canDeleteFolder(userId, context.folderId)
                UserAction.MANAGE_CATEGORY -> canManageCategory(userId, context.categoryId)
                UserAction.MANAGE_PERMISSIONS -> canManagePermissions(userId, context.familyId)
            }

            return@withContext PermissionResult.Success(canPerform)

        } catch (e: Exception) {
            return@withContext PermissionResult.Error(
                "Erreur lors de la vérification d'action: ${e.message}",
                PermissionErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE GESTION DES PERMISSIONS
    // ================================================================

    /**
     * Récupère toutes les permissions actives d'un utilisateur
     *
     * @param userId ID de l'utilisateur
     * @return Résultat avec les permissions
     */
    suspend fun getUserPermissions(userId: Int): PermissionResult<List<PermissionActive>> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que l'utilisateur existe
            val user = familyMemberRepository.findById(userId)
                ?: return@withContext PermissionResult.Error(
                    "Utilisateur non trouvé",
                    PermissionErrorCode.MEMBER_NOT_FOUND
                )

            val permissions = permissionRepository.findAll()
                .filter { it.beneficiaireId == userId && it.estActive == true }
                .filter { !isExpired(it) }
                .map { it.toModel() }

            return@withContext PermissionResult.Success(permissions)

        } catch (e: Exception) {
            return@withContext PermissionResult.Error(
                "Erreur lors de la récupération des permissions: ${e.message}",
                PermissionErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère les permissions accordées par un utilisateur
     *
     * @param ownerId ID du propriétaire
     * @return Résultat avec les permissions accordées
     */
    suspend fun getGrantedPermissions(ownerId: Int): PermissionResult<List<PermissionActive>> = withContext(Dispatchers.IO) {
        try {
            val permissions = permissionRepository.findAll()
                .filter { it.proprietaireId == ownerId && it.estActive == true }
                .filter { !isExpired(it) }
                .map { it.toModel() }

            return@withContext PermissionResult.Success(permissions)

        } catch (e: Exception) {
            return@withContext PermissionResult.Error(
                "Erreur lors de la récupération des permissions accordées: ${e.message}",
                PermissionErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Désactive une permission
     *
     * @param permissionId ID de la permission
     * @param userId ID de l'utilisateur qui désactive (doit être propriétaire ou admin)
     * @return Résultat de la désactivation
     */
    suspend fun revokePermission(
        permissionId: Int,
        userId: Int
    ): PermissionResult<PermissionActive> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la permission existe
            val permission = permissionRepository.findById(permissionId)
                ?: return@withContext PermissionResult.Error(
                    "Permission non trouvée",
                    PermissionErrorCode.PERMISSION_NOT_FOUND
                )

            // Vérifier les droits de révocation
            if (!canRevokePermission(userId, permission)) {
                return@withContext PermissionResult.Error(
                    "Droits insuffisants pour révoquer cette permission",
                    PermissionErrorCode.ACCESS_DENIED
                )
            }

            // Désactiver la permission
            permission.estActive = false
            permissionRepository.update(permission)

            return@withContext PermissionResult.Success(permission.toModel())

        } catch (e: Exception) {
            return@withContext PermissionResult.Error(
                "Erreur lors de la révocation: ${e.message}",
                PermissionErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES DE VÉRIFICATION SPÉCIFIQUES
    // ================================================================

    private suspend fun checkFileAccess(userId: Int, fileId: Int, requiredLevel: PermissionLevel): AccessResult {
        // TODO: Implémenter quand FileRepository sera complètement intégré
        val file = fileRepository?.findById(fileId)

        if (file == null) {
            return AccessResult(false, "Fichier non trouvé", PermissionLevel.READ, null)
        }

        // Propriétaire a tous les droits
        if (file.proprietaireId == userId || file.createurId == userId) {
            return AccessResult(true, "Propriétaire du fichier", PermissionLevel.ADMIN, null)
        }

        // Vérifier les permissions déléguées
        return checkDelegatedAccess(userId, "FICHIER", fileId, requiredLevel)
    }

    private suspend fun checkFolderAccess(userId: Int, folderId: Int, requiredLevel: PermissionLevel): AccessResult {
        // TODO: Implémenter quand FolderRepository sera complètement intégré
        val folder = folderRepository?.findById(folderId)

        if (folder == null) {
            return AccessResult(false, "Dossier non trouvé", PermissionLevel.READ, null)
        }

        // Propriétaire a tous les droits
        if (folder.membreFamilleId == userId) {
            return AccessResult(true, "Propriétaire du dossier", PermissionLevel.ADMIN, null)
        }

        // Vérifier les permissions déléguées
        return checkDelegatedAccess(userId, "DOSSIER", folderId, requiredLevel)
    }

    private suspend fun checkCategoryAccess(userId: Int, categoryId: Int, requiredLevel: PermissionLevel): AccessResult {
        // Vérifier les permissions déléguées sur la catégorie
        return checkDelegatedAccess(userId, "CATEGORIE", categoryId, requiredLevel)
    }

    private suspend fun checkSpaceAccess(userId: Int, spaceId: Int, requiredLevel: PermissionLevel): AccessResult {
        // Vérifier les permissions déléguées sur l'espace
        return checkDelegatedAccess(userId, "ESPACE", spaceId, requiredLevel)
    }

    private suspend fun checkDelegatedAccess(
        userId: Int,
        scope: String,
        targetId: Int,
        requiredLevel: PermissionLevel
    ): AccessResult {
        val activePermissions = permissionRepository.findAll()
            .filter {
                it.beneficiaireId == userId &&
                        it.estActive == true &&
                        it.portee == scope &&
                        it.cibleId == targetId
            }

        if (activePermissions.isEmpty()) {
            return AccessResult(false, "Aucune permission trouvée", PermissionLevel.READ, null)
        }

        // Prendre la permission la plus élevée
        val permission = activePermissions.first()

        // Vérifier l'expiration
        if (isExpired(permission)) {
            return AccessResult(false, "Permission expirée", PermissionLevel.READ, permission.dateExpiration)
        }

        // Vérifier le niveau requis
        val grantedLevel = mapPermissionType(permission.typePermission ?: "read")
        if (!hasRequiredLevel(grantedLevel, requiredLevel)) {
            return AccessResult(false, "Niveau de permission insuffisant", grantedLevel, permission.dateExpiration)
        }

        return AccessResult(true, "Permission déléguée", grantedLevel, permission.dateExpiration)
    }

    // ================================================================
    // MÉTHODES D'ACTION SPÉCIFIQUES
    // ================================================================

    private suspend fun canCreateFile(userId: Int, folderId: Int?): Boolean {
        if (folderId == null) return false
        val access = checkFolderAccess(userId, folderId, PermissionLevel.WRITE)
        return access.granted
    }

    private suspend fun canReadFile(userId: Int, fileId: Int?): Boolean {
        if (fileId == null) return false
        val access = checkFileAccess(userId, fileId, PermissionLevel.READ)
        return access.granted
    }

    private suspend fun canUpdateFile(userId: Int, fileId: Int?): Boolean {
        if (fileId == null) return false
        val access = checkFileAccess(userId, fileId, PermissionLevel.WRITE)
        return access.granted
    }

    private suspend fun canDeleteFile(userId: Int, fileId: Int?): Boolean {
        if (fileId == null) return false
        val access = checkFileAccess(userId, fileId, PermissionLevel.DELETE)
        return access.granted
    }

    private suspend fun canCreateFolder(userId: Int, categoryId: Int?, parentFolderId: Int?): Boolean {
        return if (parentFolderId != null) {
            val access = checkFolderAccess(userId, parentFolderId, PermissionLevel.WRITE)
            access.granted
        } else if (categoryId != null) {
            val access = checkCategoryAccess(userId, categoryId, PermissionLevel.WRITE)
            access.granted
        } else {
            false
        }
    }

    private suspend fun canReadFolder(userId: Int, folderId: Int?): Boolean {
        if (folderId == null) return false
        val access = checkFolderAccess(userId, folderId, PermissionLevel.READ)
        return access.granted
    }

    private suspend fun canUpdateFolder(userId: Int, folderId: Int?): Boolean {
        if (folderId == null) return false
        val access = checkFolderAccess(userId, folderId, PermissionLevel.WRITE)
        return access.granted
    }

    private suspend fun canDeleteFolder(userId: Int, folderId: Int?): Boolean {
        if (folderId == null) return false
        val access = checkFolderAccess(userId, folderId, PermissionLevel.DELETE)
        return access.granted
    }

    private suspend fun canManageCategory(userId: Int, categoryId: Int?): Boolean {
        if (categoryId == null) return false
        val access = checkCategoryAccess(userId, categoryId, PermissionLevel.ADMIN)
        return access.granted
    }

    private suspend fun canManagePermissions(userId: Int, familyId: Int?): Boolean {
        if (familyId == null) return false
        val user = familyMemberRepository.findById(userId)
        return user?.estAdmin == true || user?.estResponsable == true
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    private fun isExpired(permission: PermissionActiveEntity): Boolean {
        return permission.dateExpiration?.isBefore(LocalDateTime.now()) == true
    }

    private fun canRevokePermission(userId: Int, permission: PermissionActiveEntity): Boolean {
        val user = familyMemberRepository.findById(userId)

        // Propriétaire peut révoquer
        if (permission.proprietaireId == userId) return true

        // Bénéficiaire peut renoncer
        if (permission.beneficiaireId == userId) return true

        // Admin peut révoquer dans sa famille
        if (user?.estAdmin == true) {
            val owner = familyMemberRepository.findById(permission.proprietaireId ?: 0)
            return user.familleId == owner?.familleId
        }

        return false
    }

    private fun mapPermissionType(typePermission: String): PermissionLevel {
        return when (typePermission.lowercase()) {
            "read", "lecture" -> PermissionLevel.READ
            "write", "ecriture" -> PermissionLevel.WRITE
            "delete", "suppression" -> PermissionLevel.DELETE
            "admin", "administration" -> PermissionLevel.ADMIN
            else -> PermissionLevel.READ
        }
    }

    private fun hasRequiredLevel(granted: PermissionLevel, required: PermissionLevel): Boolean {
        val levels = listOf(PermissionLevel.READ, PermissionLevel.WRITE, PermissionLevel.DELETE, PermissionLevel.ADMIN)
        return levels.indexOf(granted) >= levels.indexOf(required)
    }
}

// ================================================================
// DATA CLASSES POUR LES REQUÊTES ET RÉSULTATS
// ================================================================

/**
 * Résultat d'une vérification d'accès
 */
data class AccessResult(
    val granted: Boolean,
    val reason: String,
    val level: PermissionLevel,
    val expiresAt: LocalDateTime?
)

/**
 * Actions utilisateur possibles
 */
enum class UserAction {
    CREATE_FILE,
    READ_FILE,
    UPDATE_FILE,
    DELETE_FILE,
    CREATE_FOLDER,
    READ_FOLDER,
    UPDATE_FOLDER,
    DELETE_FOLDER,
    MANAGE_CATEGORY,
    MANAGE_PERMISSIONS
}

/**
 * Contexte d'une action
 */
data class ActionContext(
    val fileId: Int? = null,
    val folderId: Int? = null,
    val categoryId: Int? = null,
    val spaceId: Int? = null,
    val familyId: Int? = null,
    val parentFolderId: Int? = null
)