// ================================================================
// NAVIGATIONSTATE.KT - ÉTATS ET MODÈLES DE NAVIGATION ARKA
// ================================================================

package ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDateTime

/**
 * États de navigation de l'application Arka
 * Définit tous les écrans/pages possibles de l'application
 */
enum class NavigationState(
    val title: String,
    val icon: ImageVector,
    val isMainDestination: Boolean = false,
    val requiresAuth: Boolean = true,
    val allowedRoles: Set<UserRole> = setOf(UserRole.ADMIN, UserRole.RESPONSIBLE, UserRole.MEMBER)
) {
    // ================================================================
    // AUTHENTIFICATION
    // ================================================================
    LOGIN(
        title = "Connexion",
        icon = Icons.Default.Login,
        requiresAuth = false
    ),

    SETUP(
        title = "Configuration initiale",
        icon = Icons.Default.Settings,
        requiresAuth = false
    ),

    // ================================================================
    // PAGES PRINCIPALES
    // ================================================================
    DASHBOARD(
        title = "Tableau de bord",
        icon = Icons.Default.Dashboard,
        isMainDestination = true
    ),

    FILES(
        title = "Mes fichiers",
        icon = Icons.Default.Folder,
        isMainDestination = true
    ),

    FAMILY_MEMBERS(
        title = "Famille",
        icon = Icons.Default.Group,
        isMainDestination = true
    ),

    CATEGORIES(
        title = "Catégories",
        icon = Icons.Default.Category,
        isMainDestination = true
    ),

    DELEGATIONS(
        title = "Délégations",
        icon = Icons.Default.Share,
        isMainDestination = true
    ),

    // ================================================================
    // PAGES DE DÉTAIL
    // ================================================================
    FILE_DETAILS(
        title = "Détails du fichier",
        icon = Icons.Default.Description
    ),

    FOLDER_DETAILS(
        title = "Détails du dossier",
        icon = Icons.Default.FolderOpen
    ),

    CATEGORY_DETAILS(
        title = "Détails de la catégorie",
        icon = Icons.Default.Category
    ),

    MEMBER_DETAILS(
        title = "Profil membre",
        icon = Icons.Default.Person
    ),

    DELEGATION_DETAILS(
        title = "Détails de la délégation",
        icon = Icons.Default.Share
    ),

    // ================================================================
    // GESTION ET ADMINISTRATION
    // ================================================================
    PERMISSIONS(
        title = "Permissions",
        icon = Icons.Default.Security,
        allowedRoles = setOf(UserRole.ADMIN, UserRole.RESPONSIBLE)
    ),

    SETTINGS(
        title = "Paramètres",
        icon = Icons.Default.Settings,
        allowedRoles = setOf(UserRole.ADMIN, UserRole.RESPONSIBLE)
    ),

    USER_PROFILE(
        title = "Mon profil",
        icon = Icons.Default.AccountCircle
    ),

    SYSTEM_LOGS(
        title = "Journaux système",
        icon = Icons.Default.History,
        allowedRoles = setOf(UserRole.ADMIN)
    ),

    // ================================================================
    // ACTIONS SPÉCIFIQUES
    // ================================================================
    FILE_UPLOAD(
        title = "Téléverser des fichiers",
        icon = Icons.Default.CloudUpload
    ),

    CREATE_FOLDER(
        title = "Créer un dossier",
        icon = Icons.Default.CreateNewFolder
    ),

    CREATE_CATEGORY(
        title = "Créer une catégorie",
        icon = Icons.Default.Add,
        allowedRoles = setOf(UserRole.ADMIN, UserRole.RESPONSIBLE)
    ),

    INVITE_MEMBER(
        title = "Inviter un membre",
        icon = Icons.Default.PersonAdd,
        allowedRoles = setOf(UserRole.ADMIN, UserRole.RESPONSIBLE)
    ),

    // ================================================================
    // SYNCHRONISATION MOBILE
    // ================================================================
    MOBILE_SYNC(
        title = "Synchronisation mobile",
        icon = Icons.Default.Sync
    ),

    MOBILE_DEVICES(
        title = "Appareils mobiles",
        icon = Icons.Default.PhoneAndroid
    ),

    // ================================================================
    // ÉTATS D'ERREUR
    // ================================================================
    ERROR_404(
        title = "Page non trouvée",
        icon = Icons.Default.Error,
        requiresAuth = false
    ),

    ERROR_403(
        title = "Accès refusé",
        icon = Icons.Default.Block,
        requiresAuth = false
    ),

    ERROR_500(
        title = "Erreur serveur",
        icon = Icons.Default.ErrorOutline,
        requiresAuth = false
    );

    /**
     * Vérifie si l'utilisateur a accès à cette destination
     */
    fun isAccessibleBy(userRole: UserRole): Boolean {
        return allowedRoles.contains(userRole)
    }

    /**
     * Obtient le chemin de navigation
     */
    fun getRoute(): String = name.lowercase().replace("_", "/")

    companion object {
        /**
         * Obtient toutes les destinations principales
         */
        fun getMainDestinations(): List<NavigationState> {
            return values().filter { it.isMainDestination }
        }

        /**
         * Obtient les destinations accessibles pour un rôle
         */
        fun getAccessibleDestinations(userRole: UserRole): List<NavigationState> {
            return values().filter { it.isAccessibleBy(userRole) }
        }

        /**
         * Recherche une destination par route
         */
        fun fromRoute(route: String): NavigationState? {
            return values().find { it.getRoute() == route }
        }
    }
}

/**
 * Rôles utilisateur pour navigation contextuelle
 */
enum class UserRole(
    val displayName: String,
    val permissions: Set<String>
) {
    ADMIN(
        displayName = "Administrateur",
        permissions = setOf(
            "CREATE_CATEGORY", "DELETE_CATEGORY", "MODIFY_CATEGORY",
            "INVITE_MEMBER", "REMOVE_MEMBER", "MODIFY_MEMBER_ROLE",
            "VIEW_SYSTEM_LOGS", "MODIFY_SETTINGS",
            "GRANT_DELEGATION", "REVOKE_DELEGATION",
            "ACCESS_ALL_FILES", "DELETE_ANY_FILE"
        )
    ),

    RESPONSIBLE(
        displayName = "Responsable",
        permissions = setOf(
            "CREATE_CATEGORY", "MODIFY_CATEGORY",
            "INVITE_MEMBER", "MODIFY_MEMBER_ROLE",
            "MODIFY_SETTINGS",
            "GRANT_DELEGATION", "REVOKE_DELEGATION",
            "ACCESS_FAMILY_FILES"
        )
    ),

    MEMBER(
        displayName = "Membre",
        permissions = setOf(
            "ACCESS_OWN_FILES", "CREATE_FOLDER", "UPLOAD_FILE",
            "VIEW_SHARED_FILES", "REQUEST_DELEGATION"
        )
    );

    /**
     * Vérifie si le rôle a une permission spécifique
     */
    fun hasPermission(permission: String): Boolean {
        return permissions.contains(permission)
    }

    /**
     * Vérifie si le rôle peut accéder à une action
     */
    fun canAccess(action: String): Boolean {
        return hasPermission(action)
    }
}

/**
 * Types de contexte pour navigation
 */
enum class ContextType {
    FOLDER,
    FILE,
    CATEGORY,
    DELEGATION,
    FAMILY_MEMBER,
    PERMISSION,
    DEVICE,
    SYNC_SESSION
}

/**
 * Types de délégation pour filtrage
 */
enum class DelegationType(val displayName: String) {
    ALL("Toutes"),
    GRANTED("Accordées"),
    RECEIVED("Reçues"),
    PENDING("En attente"),
    EXPIRED("Expirées"),
    ACTIVE("Actives")
}

/**
 * Paramètres de navigation
 */
data class NavigationParams(
    val params: Map<String, Any> = emptyMap()
) {
    fun getString(key: String): String? = params[key] as? String
    fun getInt(key: String): Int? = params[key] as? Int
    fun getLong(key: String): Long? = params[key] as? Long
    fun getBoolean(key: String): Boolean? = params[key] as? Boolean

    inline fun <reified T> get(key: String): T? = params[key] as? T

    fun with(key: String, value: Any): NavigationParams {
        return copy(params = params + (key to value))
    }

    fun without(key: String): NavigationParams {
        return copy(params = params - key)
    }

    operator fun plus(other: NavigationParams): NavigationParams {
        return copy(params = params + other.params)
    }
}

/**
 * Entrée d'historique de navigation
 */
data class NavigationHistoryItem(
    val from: NavigationState,
    val to: NavigationState,
    val params: NavigationParams,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val userId: Int? = null
) {
    /**
     * Obtient une description lisible de la navigation
     */
    fun getDescription(): String {
        return "Navigation de ${from.title} vers ${to.title}"
    }

    /**
     * Vérifie si cette navigation est récente
     */
    fun isRecent(minutes: Long = 5): Boolean {
        return timestamp.isAfter(LocalDateTime.now().minusMinutes(minutes))
    }
}

/**
 * État de navigation avec contexte
 */
data class NavigationContext(
    val currentState: NavigationState,
    val params: NavigationParams = NavigationParams(),
    val userRole: UserRole,
    val breadcrumb: List<NavigationState> = emptyList(),
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
) {
    /**
     * Vérifie si l'utilisateur peut accéder à l'état actuel
     */
    fun hasAccess(): Boolean {
        return currentState.isAccessibleBy(userRole)
    }

    /**
     * Obtient le titre complet avec contexte
     */
    fun getFullTitle(): String {
        val baseTitle = currentState.title

        return when (currentState) {
            NavigationState.FILE_DETAILS -> {
                val fileName = params.getString("fileName") ?: "Fichier"
                "$baseTitle - $fileName"
            }
            NavigationState.FOLDER_DETAILS -> {
                val folderName = params.getString("folderName") ?: "Dossier"
                "$baseTitle - $folderName"
            }
            NavigationState.CATEGORY_DETAILS -> {
                val categoryName = params.getString("categoryName") ?: "Catégorie"
                "$baseTitle - $categoryName"
            }
            NavigationState.MEMBER_DETAILS -> {
                val memberName = params.getString("memberName") ?: "Membre"
                "$baseTitle - $memberName"
            }
            else -> baseTitle
        }
    }
}

/**
 * Résultat de navigation
 */
sealed class NavigationResult {
    object Success : NavigationResult()
    data class Error(val message: String, val code: NavigationErrorCode) : NavigationResult()
    object Denied : NavigationResult()
    object NotFound : NavigationResult()
}

/**
 * Codes d'erreur de navigation
 */
enum class NavigationErrorCode {
    ACCESS_DENIED,
    INVALID_PARAMS,
    STATE_NOT_FOUND,
    TRANSITION_NOT_ALLOWED,
    AUTHENTICATION_REQUIRED,
    INSUFFICIENT_PERMISSIONS
}

/**
 * Configuration de navigation
 */
data class NavigationConfig(
    val enableHistory: Boolean = true,
    val maxHistorySize: Int = 50,
    val enableBreadcrumb: Boolean = true,
    val maxBreadcrumbSize: Int = 5,
    val enableAnimations: Boolean = true,
    val defaultErrorHandler: ((NavigationErrorCode) -> Unit)? = null
)