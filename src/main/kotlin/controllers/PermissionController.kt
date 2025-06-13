package controllers

import ktorm.*
import repositories.*
import java.time.LocalDateTime

/**
 * Controller pour la gestion granulaire des permissions dans Arka
 * Fournit une API unifiée pour toutes les vérifications d'accès
 */
class PermissionController(
    private val permissionRepository: PermissionRepository,
    private val memberRepository: FamilyMemberRepository
) {

    /**
     * Vérifie si un utilisateur a accès à une ressource spécifique
     * Point d'entrée principal pour toutes les vérifications de permissions
     * @param accessRequest La demande d'accès
     * @return Le résultat de la vérification
     */
    fun checkAccess(accessRequest: AccessCheckRequest): AccessCheckResult {
        return try {
            // 1. Vérifications de base
            val baseCheck = performBaseChecks(accessRequest)
            if (!baseCheck.granted) {
                return baseCheck
            }

            // 2. Vérifications spécifiques selon la portée
            val specificCheck = when (accessRequest.portee) {
                PorteePermission.FICHIER -> checkFileAccess(accessRequest)
                PorteePermission.DOSSIER -> checkFolderAccess(accessRequest)
                PorteePermission.CATEGORIE -> checkCategoryAccess(accessRequest)
                PorteePermission.ESPACE_COMPLET -> checkSpaceAccess(accessRequest)
            }

            // 3. Combiner les résultats
            if (specificCheck.granted) {
                AccessCheckResult.Granted(
                    reason = specificCheck.reason,
                    permissionLevel = determinePermissionLevel(accessRequest),
                    expiresAt = getPermissionExpiration(accessRequest)
                )
            } else {
                specificCheck
            }
        } catch (e: Exception) {
            AccessCheckResult.Denied("Erreur lors de la vérification: ${e.message}")
        }
    }

    /**
     * Vérifie si un utilisateur peut effectuer une action spécifique
     * @param userId L'ID de l'utilisateur
     * @param action L'action à vérifier
     * @param context Le contexte de l'action
     * @return true si l'action est autorisée
     */
    fun canPerformAction(userId: Int, action: UserAction, context: ActionContext): Boolean {
        return when (action) {
            UserAction.CREATE_FILE -> canCreateFile(userId, context.dossierId!!)
            UserAction.READ_FILE -> canReadFile(userId, context.fichierId!!)
            UserAction.UPDATE_FILE -> canUpdateFile(userId, context.fichierId!!)
            UserAction.DELETE_FILE -> canDeleteFile(userId, context.fichierId!!)
            UserAction.CREATE_FOLDER -> canCreateFolder(userId, context.categorieId!!, context.dossierParentId)
            UserAction.READ_FOLDER -> canReadFolder(userId, context.dossierId!!)
            UserAction.UPDATE_FOLDER -> canUpdateFolder(userId, context.dossierId!!)
            UserAction.DELETE_FOLDER -> canDeleteFolder(userId, context.dossierId!!)
            UserAction.SHARE_RESOURCE -> canShareResource(userId, context)
            UserAction.MANAGE_PERMISSIONS -> canManagePermissions(userId, context)
        }
    }

    /**
     * Obtient toutes les permissions actives d'un utilisateur
     * @param userId L'ID de l'utilisateur
     * @return Le résumé des permissions
     */
    fun getUserPermissionsSummary(userId: Int): UserPermissionsSummary {
        return try {
            val activePermissions = permissionRepository.findActiveByBeneficiary(userId)
            val permissionsByScope = activePermissions.groupBy { PorteePermission.valueOf(it.portee) }

            val filePermissions = activePermissions
                .filter { it.portee == PorteePermission.FICHIER.name }
                .map { ResourcePermission(it.cibleId!!, TypePermission.valueOf(it.typePermission), it.dateExpiration) }

            val folderPermissions = activePermissions
                .filter { it.portee == PorteePermission.DOSSIER.name }
                .map { ResourcePermission(it.cibleId!!, TypePermission.valueOf(it.typePermission), it.dateExpiration) }

            val categoryPermissions = activePermissions
                .filter { it.portee == PorteePermission.CATEGORIE.name }
                .map { ResourcePermission(it.cibleId!!, TypePermission.valueOf(it.typePermission), it.dateExpiration) }

            val hasSpaceAccess = activePermissions.any { it.portee == PorteePermission.ESPACE_COMPLET.name }

            UserPermissionsSummary(
                userId = userId,
                filePermissions = filePermissions,
                folderPermissions = folderPermissions,
                categoryPermissions = categoryPermissions,
                hasSpaceAccess = hasSpaceAccess,
                totalActivePermissions = activePermissions.size,
                lastUpdated = LocalDateTime.now()
            )
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la récupération des permissions de l'utilisateur $userId: ${e.message}")
            UserPermissionsSummary(userId, emptyList(), emptyList(), emptyList(), false, 0, LocalDateTime.now())
        }
    }

    /**
     * Vérifie et nettoie les permissions expirées pour un utilisateur
     * @param userId L'ID de l'utilisateur
     * @return Le nombre de permissions nettoyées
     */
    fun cleanupExpiredPermissions(userId: Int): Int {
        return try {
            val userPermissions = permissionRepository.findActiveByBeneficiary(userId)
            var cleanedCount = 0

            for (permission in userPermissions) {
                if (permission.dateExpiration != null && permission.dateExpiration!!.isBefore(LocalDateTime.now())) {
                    permissionRepository.deactivatePermission(permission.permissionId)
                    cleanedCount++
                }
            }

            cleanedCount
        } catch (e: Exception) {
            println("⚠️ Erreur lors du nettoyage des permissions expirées: ${e.message}")
            0
        }
    }

    /**
     * Obtient les suggestions de permissions pour un utilisateur
     * @param userId L'ID de l'utilisateur
     * @param context Le contexte pour les suggestions
     * @return Les suggestions de permissions
     */
    fun getPermissionSuggestions(userId: Int, context: PermissionSuggestionContext): List<PermissionSuggestion> {
        return try {
            val suggestions = mutableListOf<PermissionSuggestion>()

            // Suggestions basées sur l'activité récente
            if (context.includeRecentActivity) {
                suggestions.addAll(getRecentActivitySuggestions(userId))
            }

            // Suggestions basées sur les permissions existantes
            if (context.includeRelatedPermissions) {
                suggestions.addAll(getRelatedPermissionSuggestions(userId))
            }

            // Suggestions basées sur les rôles familiaux
            if (context.includeFamilyRoles) {
                suggestions.addAll(getFamilyRoleSuggestions(userId))
            }

            suggestions.distinctBy { "${it.portee}-${it.cibleId}-${it.typePermission}" }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la génération des suggestions: ${e.message}")
            emptyList()
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES - VÉRIFICATIONS SPÉCIFIQUES
    // ================================================================

    /**
     * Effectue les vérifications de base communes à toutes les demandes
     */
    private fun performBaseChecks(request: AccessCheckRequest): AccessCheckResult {
        // 1. Vérifier que l'utilisateur existe
        val user = memberRepository.findById(request.userId)
        if (user == null) {
            return AccessCheckResult.Denied("Utilisateur non trouvé")
        }

        // 2. Vérifier si l'utilisateur est administrateur (accès complet)
        if (user.estAdmin) {
            return AccessCheckResult.Granted("Accès administrateur", TypePermission.ACCES_COMPLET, null)
        }

        // 3. Vérifications spécifiques selon le type de demande
        if (request.cibleId != null) {
            // Vérifier que la cible existe (implémentation simplifiée)
            // Dans un vrai projet, on vérifierait selon la portée
        }

        return AccessCheckResult.Granted("Vérifications de base passées", TypePermission.LECTURE, null)
    }

    /**
     * Vérifie l'accès à un fichier
     */
    private fun checkFileAccess(request: AccessCheckRequest): AccessCheckResult {
        if (request.cibleId == null) {
            return AccessCheckResult.Denied("ID du fichier requis")
        }

        val hasPermission = permissionRepository.hasPermission(
            beneficiaireId = request.userId,
            portee = PorteePermission.FICHIER,
            cibleId = request.cibleId,
            typePermission = request.typePermission
        )

        return if (hasPermission) {
            AccessCheckResult.Granted("Permission directe sur le fichier", request.typePermission, null)
        } else {
            AccessCheckResult.Denied("Aucune permission sur ce fichier")
        }
    }

    /**
     * Vérifie l'accès à un dossier
     */
    private fun checkFolderAccess(request: AccessCheckRequest): AccessCheckResult {
        if (request.cibleId == null) {
            return AccessCheckResult.Denied("ID du dossier requis")
        }

        val hasPermission = permissionRepository.hasPermission(
            beneficiaireId = request.userId,
            portee = PorteePermission.DOSSIER,
            cibleId = request.cibleId,
            typePermission = request.typePermission
        )

        return if (hasPermission) {
            AccessCheckResult.Granted("Permission directe sur le dossier", request.typePermission, null)
        } else {
            AccessCheckResult.Denied("Aucune permission sur ce dossier")
        }
    }

    /**
     * Vérifie l'accès à une catégorie
     */
    private fun checkCategoryAccess(request: AccessCheckRequest): AccessCheckResult {
        if (request.cibleId == null) {
            return AccessCheckResult.Denied("ID de la catégorie requis")
        }

        val hasPermission = permissionRepository.hasPermission(
            beneficiaireId = request.userId,
            portee = PorteePermission.CATEGORIE,
            cibleId = request.cibleId,
            typePermission = request.typePermission
        )

        return if (hasPermission) {
            AccessCheckResult.Granted("Permission sur la catégorie", request.typePermission, null)
        } else {
            AccessCheckResult.Denied("Aucune permission sur cette catégorie")
        }
    }

    /**
     * Vérifie l'accès à l'espace complet
     */
    private fun checkSpaceAccess(request: AccessCheckRequest): AccessCheckResult {
        val hasPermission = permissionRepository.hasPermission(
            beneficiaireId = request.userId,
            portee = PorteePermission.ESPACE_COMPLET,
            cibleId = null,
            typePermission = request.typePermission
        )

        return if (hasPermission) {
            AccessCheckResult.Granted("Permission sur l'espace complet", request.typePermission, null)
        } else {
            AccessCheckResult.Denied("Aucune permission sur l'espace")
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES - ACTIONS SPÉCIFIQUES
    // ================================================================

    private fun canCreateFile(userId: Int, dossierId: Int): Boolean {
        return permissionRepository.hasPermission(userId, PorteePermission.DOSSIER, dossierId, TypePermission.ECRITURE)
    }

    private fun canReadFile(userId: Int, fichierId: Int): Boolean {
        return permissionRepository.hasPermission(userId, PorteePermission.FICHIER, fichierId, TypePermission.LECTURE)
    }

    private fun canUpdateFile(userId: Int, fichierId: Int): Boolean {
        return permissionRepository.hasPermission(userId, PorteePermission.FICHIER, fichierId, TypePermission.ECRITURE)
    }

    private fun canDeleteFile(userId: Int, fichierId: Int): Boolean {
        return permissionRepository.hasPermission(userId, PorteePermission.FICHIER, fichierId, TypePermission.SUPPRESSION)
    }

    private fun canCreateFolder(userId: Int, categorieId: Int, parentFolderId: Int?): Boolean {
        return if (parentFolderId != null) {
            permissionRepository.hasPermission(userId, PorteePermission.DOSSIER, parentFolderId, TypePermission.ECRITURE)
        } else {
            permissionRepository.hasPermission(userId, PorteePermission.CATEGORIE, categorieId, TypePermission.ECRITURE)
        }
    }

    private fun canReadFolder(userId: Int, dossierId: Int): Boolean {
        return permissionRepository.hasPermission(userId, PorteePermission.DOSSIER, dossierId, TypePermission.LECTURE)
    }

    private fun canUpdateFolder(userId: Int, dossierId: Int): Boolean {
        return permissionRepository.hasPermission(userId, PorteePermission.DOSSIER, dossierId, TypePermission.ECRITURE)
    }

    private fun canDeleteFolder(userId: Int, dossierId: Int): Boolean {
        return permissionRepository.hasPermission(userId, PorteePermission.DOSSIER, dossierId, TypePermission.SUPPRESSION)
    }

    private fun canShareResource(userId: Int, context: ActionContext): Boolean {
        return when {
            context.fichierId != null ->
                permissionRepository.hasPermission(userId, PorteePermission.FICHIER, context.fichierId, TypePermission.ACCES_COMPLET)
            context.dossierId != null ->
                permissionRepository.hasPermission(userId, PorteePermission.DOSSIER, context.dossierId, TypePermission.ACCES_COMPLET)
            else -> false
        }
    }

    private fun canManagePermissions(userId: Int, context: ActionContext): Boolean {
        val user = memberRepository.findById(userId)
        return user?.estAdmin == true || user?.estResponsable == true
    }

    // ================================================================
    // MÉTHODES PRIVÉES - UTILITAIRES
    // ================================================================

    /**
     * Détermine le niveau de permission effectif
     */
    private fun determinePermissionLevel(request: AccessCheckRequest): TypePermission {
        return try {
            val permissions = permissionRepository.findActiveByBeneficiary(request.userId)
            val relevantPermissions = permissions.filter {
                it.portee == request.portee.name &&
                        (request.cibleId == null || it.cibleId == request.cibleId)
            }

            if (relevantPermissions.any { it.typePermission == TypePermission.ACCES_COMPLET.name }) {
                TypePermission.ACCES_COMPLET
            } else if (relevantPermissions.any { it.typePermission == TypePermission.SUPPRESSION.name }) {
                TypePermission.SUPPRESSION
            } else if (relevantPermissions.any { it.typePermission == TypePermission.ECRITURE.name }) {
                TypePermission.ECRITURE
            } else {
                TypePermission.LECTURE
            }
        } catch (e: Exception) {
            TypePermission.LECTURE
        }
    }

    /**
     * Obtient la date d'expiration d'une permission
     */
    private fun getPermissionExpiration(request: AccessCheckRequest): LocalDateTime? {
        return try {
            val permissions = permissionRepository.findActiveByBeneficiary(request.userId)
            permissions.find {
                it.portee == request.portee.name &&
                        (request.cibleId == null || it.cibleId == request.cibleId)
            }?.dateExpiration
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Génère des suggestions basées sur l'activité récente
     */
    private fun getRecentActivitySuggestions(userId: Int): List<PermissionSuggestion> {
        // Implémentation simplifiée - dans un vrai projet, on analyserait l'historique
        return emptyList()
    }

    /**
     * Génère des suggestions basées sur les permissions existantes
     */
    private fun getRelatedPermissionSuggestions(userId: Int): List<PermissionSuggestion> {
        // Implémentation simplifiée
        return emptyList()
    }

    /**
     * Génère des suggestions basées sur les rôles familiaux
     */
    private fun getFamilyRoleSuggestions(userId: Int): List<PermissionSuggestion> {
        return try {
            val user = memberRepository.findById(userId)
            val suggestions = mutableListOf<PermissionSuggestion>()

            if (user?.estResponsable == true) {
                suggestions.add(
                    PermissionSuggestion(
                        portee = PorteePermission.ESPACE_COMPLET,
                        cibleId = null,
                        typePermission = TypePermission.ECRITURE,
                        raison = "Accès étendu pour responsable de famille"
                    )
                )
            }

            suggestions
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// ================================================================
// CLASSES DE DONNÉES POUR LE CONTROLLER
// ================================================================

/**
 * Demande de vérification d'accès
 */
data class AccessCheckRequest(
    val userId: Int,
    val portee: PorteePermission,
    val cibleId: Int?,
    val typePermission: TypePermission
)

/**
 * Contexte d'une action
 */
data class ActionContext(
    val fichierId: Int? = null,
    val dossierId: Int? = null,
    val categorieId: Int? = null,
    val dossierParentId: Int? = null
)

/**
 * Actions possibles d'un utilisateur
 */
enum class UserAction {
    CREATE_FILE, READ_FILE, UPDATE_FILE, DELETE_FILE,
    CREATE_FOLDER, READ_FOLDER, UPDATE_FOLDER, DELETE_FOLDER,
    SHARE_RESOURCE, MANAGE_PERMISSIONS
}

/**
 * Permission sur une ressource
 */
data class ResourcePermission(
    val resourceId: Int,
    val typePermission: TypePermission,
    val expiresAt: LocalDateTime?
)

/**
 * Résumé des permissions d'un utilisateur
 */
data class UserPermissionsSummary(
    val userId: Int,
    val filePermissions: List<ResourcePermission>,
    val folderPermissions: List<ResourcePermission>,
    val categoryPermissions: List<ResourcePermission>,
    val hasSpaceAccess: Boolean,
    val totalActivePermissions: Int,
    val lastUpdated: LocalDateTime
)

/**
 * Contexte pour les suggestions de permissions
 */
data class PermissionSuggestionContext(
    val includeRecentActivity: Boolean = true,
    val includeRelatedPermissions: Boolean = true,
    val includeFamilyRoles: Boolean = true
)

/**
 * Suggestion de permission
 */
data class PermissionSuggestion(
    val portee: PorteePermission,
    val cibleId: Int?,
    val typePermission: TypePermission,
    val raison: String
)

// ================================================================
// RÉSULTATS D'OPÉRATIONS
// ================================================================

sealed class AccessCheckResult {
    data class Granted(
        val reason: String,
        val permissionLevel: TypePermission,
        val expiresAt: LocalDateTime?
    ) : AccessCheckResult() {
        val granted = true
    }

    data class Denied(val reason: String) : AccessCheckResult() {
        val granted = false
    }
}