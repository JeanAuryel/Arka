package services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

/**
 * Service de gestion de la navigation
 *
 * Responsabilités:
 * - Gestion de l'état de navigation global
 * - Historique de navigation
 * - Validation des transitions de navigation
 * - Gestion des paramètres de navigation
 * - Navigation contextuelle selon les permissions
 *
 * Utilisé par: MainController, UI Components
 * Utilise: SessionService (pour validation des permissions)
 *
 * Pattern: State Management + Observer
 */
class NavigationService {

    /**
     * État de navigation réactif
     */
    private val _navigationState = MutableStateFlow(NavigationState.LOGIN)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _navigationParams = MutableStateFlow<Map<String, Any>>(emptyMap())
    val navigationParams: StateFlow<Map<String, Any>> = _navigationParams.asStateFlow()

    /**
     * Historique de navigation
     */
    private val navigationHistory = mutableListOf<NavigationHistoryItem>()
    private val maxHistorySize = 50

    /**
     * Stack de navigation pour gestion back/forward
     */
    private val navigationStack = mutableListOf<NavigationState>()

    /**
     * Callbacks de navigation
     */
    private val navigationCallbacks = mutableMapOf<NavigationState, () -> Unit>()

    // ================================================================
    // MÉTHODES DE NAVIGATION PRINCIPALES
    // ================================================================

    /**
     * Navigue vers une destination
     *
     * @param destination État de navigation cible
     * @param params Paramètres optionnels
     * @param addToHistory Ajouter à l'historique (défaut: true)
     * @return true si la navigation a réussi
     */
    fun navigateTo(
        destination: NavigationState,
        params: Map<String, Any> = emptyMap(),
        addToHistory: Boolean = true
    ): Boolean {
        return try {
            // Validation de la transition
            if (!isNavigationAllowed(getCurrentDestination(), destination)) {
                println("❌ Navigation refusée: ${getCurrentDestination()} -> $destination")
                return false
            }

            val previousState = _navigationState.value

            // Mettre à jour l'état
            _navigationState.value = destination
            _navigationParams.value = params

            // Ajouter à l'historique
            if (addToHistory) {
                addToNavigationHistory(previousState, destination, params)
            }

            // Ajouter à la stack si différent du précédent
            if (navigationStack.isEmpty() || navigationStack.last() != destination) {
                navigationStack.add(destination)
            }

            // Exécuter les callbacks
            navigationCallbacks[destination]?.invoke()

            println("✅ Navigation: $previousState -> $destination ${if (params.isNotEmpty()) "avec paramètres: $params" else ""}")
            true

        } catch (e: Exception) {
            println("❌ Erreur lors de la navigation: ${e.message}")
            false
        }
    }

    /**
     * Navigation avec remplacement (ne garde pas l'état précédent)
     *
     * @param destination État de navigation cible
     * @param params Paramètres optionnels
     * @return true si la navigation a réussi
     */
    fun navigateReplace(
        destination: NavigationState,
        params: Map<String, Any> = emptyMap()
    ): Boolean {
        // Supprimer le dernier élément de la stack
        if (navigationStack.isNotEmpty()) {
            navigationStack.removeAt(navigationStack.size - 1)
        }

        return navigateTo(destination, params, addToHistory = false)
    }

    /**
     * Navigation arrière
     *
     * @return true si la navigation arrière était possible
     */
    fun navigateBack(): Boolean {
        return if (canNavigateBack()) {
            // Supprimer l'état actuel
            navigationStack.removeAt(navigationStack.size - 1)

            // Aller au précédent
            val previousDestination = navigationStack.lastOrNull() ?: NavigationState.DASHBOARD
            navigateReplace(previousDestination)
        } else {
            false
        }
    }

    /**
     * Navigation vers la destination par défaut selon le contexte
     *
     * @param userRole Rôle de l'utilisateur pour déterminer la destination
     * @return true si la navigation a réussi
     */
    fun navigateToDefault(userRole: UserRole = UserRole.MEMBER): Boolean {
        val defaultDestination = when (userRole) {
            UserRole.ADMIN -> NavigationState.DASHBOARD
            UserRole.RESPONSIBLE -> NavigationState.DASHBOARD
            UserRole.MEMBER -> NavigationState.FOLDERS
        }

        return navigateTo(defaultDestination)
    }

    // ================================================================
    // MÉTHODES DE NAVIGATION CONTEXTUELLE
    // ================================================================

    /**
     * Navigation avec contexte (pour les écrans qui nécessitent des données)
     *
     * @param destination Destination
     * @param contextId ID du contexte (ex: dossier ID, fichier ID)
     * @param contextType Type de contexte
     * @return true si la navigation a réussi
     */
    fun navigateWithContext(
        destination: NavigationState,
        contextId: Int,
        contextType: ContextType
    ): Boolean {
        val params = mapOf(
            "contextId" to contextId,
            "contextType" to contextType.name,
            "timestamp" to LocalDateTime.now()
        )

        return navigateTo(destination, params)
    }

    /**
     * Navigation vers un dossier spécifique
     *
     * @param folderId ID du dossier
     * @param categoryId ID de la catégorie (optionnel)
     * @return true si la navigation a réussi
     */
    fun navigateToFolder(folderId: Int, categoryId: Int? = null): Boolean {
        val params = mutableMapOf<String, Any>("folderId" to folderId)
        categoryId?.let { params["categoryId"] = it }

        return navigateTo(NavigationState.FOLDERS, params)
    }

    /**
     * Navigation vers les détails d'un fichier
     *
     * @param fileId ID du fichier
     * @param folderId ID du dossier parent (optionnel)
     * @return true si la navigation a réussi
     */
    fun navigateToFile(fileId: Int, folderId: Int? = null): Boolean {
        val params = mutableMapOf<String, Any>("fileId" to fileId)
        folderId?.let { params["folderId"] = it }

        return navigateTo(NavigationState.FILES, params)
    }

    /**
     * Navigation vers les délégations avec un filtre
     *
     * @param delegationType Type de délégation à afficher
     * @return true si la navigation a réussi
     */
    fun navigateToDelegations(delegationType: DelegationType = DelegationType.ALL): Boolean {
        val params = mapOf("delegationType" to delegationType.name)
        return navigateTo(NavigationState.DELEGATIONS, params)
    }

    // ================================================================
    // MÉTHODES D'ÉTAT ET VALIDATION
    // ================================================================

    /**
     * Obtient la destination actuelle
     *
     * @return État de navigation actuel
     */
    fun getCurrentDestination(): NavigationState = _navigationState.value

    /**
     * Obtient les paramètres de navigation actuels
     *
     * @return Map des paramètres
     */
    fun getCurrentParams(): Map<String, Any> = _navigationParams.value

    /**
     * Vérifie si on peut naviguer en arrière
     *
     * @return true si navigation arrière possible
     */
    fun canNavigateBack(): Boolean = navigationStack.size > 1

    /**
     * Valide si une transition de navigation est autorisée
     *
     * @param from État de départ
     * @param to État d'arrivée
     * @return true si la transition est autorisée
     */
    private fun isNavigationAllowed(from: NavigationState, to: NavigationState): Boolean {
        // Règles de validation de navigation
        return when {
            // On peut toujours aller au login
            to == NavigationState.LOGIN -> true

            // Depuis login, on peut aller partout
            from == NavigationState.LOGIN -> true

            // Navigation entre écrans principaux toujours autorisée
            to in listOf(
                NavigationState.DASHBOARD,
                NavigationState.FOLDERS,
                NavigationState.CATEGORIES,
                NavigationState.FAMILY_MEMBERS
            ) -> true

            // Navigation vers les paramètres depuis n'importe où
            to == NavigationState.SETTINGS -> true

            // Navigation vers profil depuis n'importe où
            to == NavigationState.PROFILE -> true

            // Par défaut, autoriser toutes les autres transitions
            else -> true
        }
    }

    // ================================================================
    // MÉTHODES D'HISTORIQUE
    // ================================================================

    /**
     * Ajoute une entrée à l'historique de navigation
     */
    private fun addToNavigationHistory(
        from: NavigationState,
        to: NavigationState,
        params: Map<String, Any>
    ) {
        val historyItem = NavigationHistoryItem(
            from = from,
            to = to,
            params = params,
            timestamp = LocalDateTime.now()
        )

        navigationHistory.add(historyItem)

        // Limiter la taille de l'historique
        if (navigationHistory.size > maxHistorySize) {
            navigationHistory.removeAt(0)
        }
    }

    /**
     * Obtient l'historique de navigation
     *
     * @param limit Nombre d'entrées max à retourner
     * @return Liste des entrées d'historique
     */
    fun getNavigationHistory(limit: Int = 20): List<NavigationHistoryItem> {
        return navigationHistory.takeLast(limit)
    }

    /**
     * Efface l'historique de navigation
     */
    fun clearHistory() {
        navigationHistory.clear()
        println("🧹 Historique de navigation effacé")
    }

    // ================================================================
    // MÉTHODES DE CALLBACKS ET ÉVÉNEMENTS
    // ================================================================

    /**
     * Enregistre un callback pour une destination
     *
     * @param destination Destination à surveiller
     * @param callback Fonction à exécuter lors de la navigation
     */
    fun registerNavigationCallback(destination: NavigationState, callback: () -> Unit) {
        navigationCallbacks[destination] = callback
    }

    /**
     * Supprime un callback
     *
     * @param destination Destination dont supprimer le callback
     */
    fun unregisterNavigationCallback(destination: NavigationState) {
        navigationCallbacks.remove(destination)
    }

    // ================================================================
    // MÉTHODES D'INITIALISATION ET NETTOYAGE
    // ================================================================

    /**
     * Initialise la navigation
     *
     * @param initialState État initial (défaut: LOGIN)
     */
    fun initializeNavigation(initialState: NavigationState = NavigationState.LOGIN) {
        _navigationState.value = initialState
        _navigationParams.value = emptyMap()
        navigationStack.clear()
        navigationStack.add(initialState)

        println("✅ Navigation initialisée: $initialState")
    }

    /**
     * Réinitialise complètement la navigation
     */
    fun resetNavigation() {
        _navigationState.value = NavigationState.LOGIN
        _navigationParams.value = emptyMap()
        navigationStack.clear()
        navigationHistory.clear()
        navigationCallbacks.clear()

        println("🔄 Navigation réinitialisée")
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Obtient un paramètre spécifique de navigation
     *
     * @param key Clé du paramètre
     * @return Valeur du paramètre ou null
     */
    fun getParam(key: String): Any? = _navigationParams.value[key]

    /**
     * Obtient un paramètre typé
     *
     * @param key Clé du paramètre
     * @return Valeur typée ou null
     */
    inline fun <reified T> getTypedParam(key: String): T? {
        return getCurrentParams()[key] as? T
    }

    /**
     * Vérifie si on est sur une destination spécifique
     *
     * @param destination Destination à vérifier
     * @return true si on est sur cette destination
     */
    fun isCurrentDestination(destination: NavigationState): Boolean {
        return _navigationState.value == destination
    }

    /**
     * Obtient les statistiques de navigation
     *
     * @return Map avec les statistiques
     */
    fun getNavigationStatistics(): Map<String, Any> {
        val destinationCounts = navigationHistory
            .groupBy { it.to }
            .mapValues { it.value.size }

        val mostVisited = destinationCounts.maxByOrNull { it.value }

        return mapOf<String, Any>(
            "totalNavigations" to navigationHistory.size,
            "currentDestination" to getCurrentDestination().name,
            "canGoBack" to canNavigateBack(),
            "stackSize" to navigationStack.size,
            "mostVisitedDestination" to (mostVisited?.key?.name ?: "Aucune"),
            "visitCounts" to destinationCounts
        )
    }

    /**
     * Obtient le breadcrumb de navigation actuel
     *
     * @return Liste des destinations dans l'ordre
     */
    fun getBreadcrumb(): List<NavigationState> {
        return navigationStack.toList()
    }
}

// ================================================================
// DATA CLASSES ET ENUMS
// ================================================================

/**
 * États de navigation de l'application
 */
enum class NavigationState {
    LOGIN,
    DASHBOARD,
    FAMILY_MEMBERS,
    CATEGORIES,
    FOLDERS,
    FILES,
    DELEGATIONS,
    PERMISSIONS,
    SETTINGS,
    PROFILE
}

/**
 * Rôles utilisateur pour navigation contextuelle
 */
enum class UserRole {
    ADMIN,
    RESPONSIBLE,
    MEMBER
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
    PERMISSION
}

/**
 * Types de délégation pour filtrage
 */
enum class DelegationType {
    ALL,
    GRANTED,
    RECEIVED,
    PENDING,
    EXPIRED
}

/**
 * Entrée d'historique de navigation
 */
data class NavigationHistoryItem(
    val from: NavigationState,
    val to: NavigationState,
    val params: Map<String, Any>,
    val timestamp: LocalDateTime
)