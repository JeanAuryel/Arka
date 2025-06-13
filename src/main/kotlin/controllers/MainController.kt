package controllers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import services.SessionService
import services.NavigationService
import services.HealthService
import services.NavigationState as ServiceNavigationState
import ktorm.*
import java.time.LocalDateTime

/**
 * Main Controller - VERSION TEMPORAIRE SIMPLIFIÉE
 *
 * Cette version utilise seulement les services de base disponibles :
 * - SessionService
 * - NavigationService
 * - HealthService
 *
 * DashboardService et SearchService seront ajoutés quand leurs dépendances seront prêtes.
 *
 * Responsabilités RÉDUITES:
 * - Authentification via SessionService
 * - Navigation de base
 * - Vérification de santé système
 * - Gestion d'état minimal
 */
class MainController(
    private val sessionService: SessionService,
    private val navigationService: NavigationService,
    private val healthService: HealthService
) {

    /**
     * État global de l'application (simplifié)
     */
    private val _applicationState = MutableStateFlow(ApplicationState())
    val applicationState: StateFlow<ApplicationState> = _applicationState.asStateFlow()

    /**
     * État de navigation délégué
     */
    val navigationState: StateFlow<ServiceNavigationState> = navigationService.navigationState

    /**
     * Résultats standardisés
     */
    sealed class MainResult<out T> {
        data class Success<T>(val data: T) : MainResult<T>()
        data class Error(val message: String, val code: MainErrorCode) : MainResult<Nothing>()
    }

    enum class MainErrorCode {
        INITIALIZATION_FAILED,
        SESSION_EXPIRED,
        PERMISSION_DENIED,
        INTERNAL_ERROR
    }

    // ================================================================
    // MÉTHODES PRINCIPALES SIMPLIFIÉES
    // ================================================================

    /**
     * Initialise l'application de base
     */
    suspend fun initializeApplication(): MainResult<Unit> = withContext(Dispatchers.IO) {
        try {
            updateApplicationState { it.copy(isLoading = true, isInitialized = false) }

            // 1. Vérifier la santé du système
            val healthResult = healthService.initializeSystem()
            when (healthResult) {
                is HealthService.HealthResult.Error -> {
                    return@withContext MainResult.Error(
                        healthResult.message,
                        MainErrorCode.INITIALIZATION_FAILED
                    )
                }
                is HealthService.HealthResult.Success -> {
                    // Continuer l'initialisation
                }
            }

            // 2. Vérifier session existante
            sessionService.checkExistingSession()

            // 3. Initialiser la navigation
            navigationService.initializeNavigation()

            updateApplicationState {
                it.copy(
                    isLoading = false,
                    isInitialized = true,
                    lastInitialized = LocalDateTime.now()
                )
            }

            return@withContext MainResult.Success(Unit)

        } catch (e: Exception) {
            updateApplicationState {
                it.copy(
                    isLoading = false,
                    isInitialized = false,
                    lastError = "Erreur d'initialisation: ${e.message}"
                )
            }
            return@withContext MainResult.Error(
                "Échec de l'initialisation: ${e.message}",
                MainErrorCode.INITIALIZATION_FAILED
            )
        }
    }

    /**
     * Connexion utilisateur
     */
    suspend fun login(email: String, password: String): MainResult<MembreFamille> = withContext(Dispatchers.IO) {
        try {
            updateApplicationState { it.copy(isLoading = true) }

            val sessionResult = sessionService.authenticate(email, password)

            when (sessionResult) {
                is SessionService.SessionResult.Success -> {
                    updateApplicationState {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            currentUser = sessionResult.user,
                            lastLogin = LocalDateTime.now()
                        )
                    }

                    // Naviguer vers le dashboard
                    navigationService.navigateTo(ServiceNavigationState.DASHBOARD)

                    return@withContext MainResult.Success(sessionResult.user)
                }

                is SessionService.SessionResult.Error -> {
                    updateApplicationState { it.copy(isLoading = false) }
                    return@withContext MainResult.Error(
                        sessionResult.message,
                        MainErrorCode.PERMISSION_DENIED
                    )
                }
            }

        } catch (e: Exception) {
            updateApplicationState {
                it.copy(
                    isLoading = false,
                    lastError = "Erreur de connexion: ${e.message}"
                )
            }
            return@withContext MainResult.Error(
                "Erreur lors de la connexion: ${e.message}",
                MainErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Déconnexion utilisateur
     */
    fun logout(): MainResult<Unit> {
        return try {
            sessionService.logout()
            navigationService.navigateTo(ServiceNavigationState.LOGIN)

            // Reset de l'état global
            updateApplicationState {
                ApplicationState().copy(isInitialized = true)
            }

            MainResult.Success(Unit)

        } catch (e: Exception) {
            MainResult.Error(
                "Erreur lors de la déconnexion: ${e.message}",
                MainErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Vérification de l'état de l'application
     */
    suspend fun getApplicationHealth(): MainResult<services.ApplicationHealth> = withContext(Dispatchers.IO) {
        try {
            val healthResult = healthService.getSystemHealth()

            return@withContext when (healthResult) {
                is HealthService.HealthResult.Success -> MainResult.Success(healthResult.health)
                is HealthService.HealthResult.Error -> MainResult.Error(
                    healthResult.message,
                    MainErrorCode.INTERNAL_ERROR
                )
            }

        } catch (e: Exception) {
            return@withContext MainResult.Error(
                "Erreur lors de la vérification: ${e.message}",
                MainErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE DÉLÉGATION
    // ================================================================

    /**
     * Navigation
     */
    fun navigateTo(destination: ServiceNavigationState) {
        navigationService.navigateTo(destination)
        updateApplicationState { it.copy(lastNavigation = LocalDateTime.now()) }
    }

    /**
     * Getters pour l'état de session
     */
    fun getCurrentUser() = sessionService.getCurrentUser()
    fun isAuthenticated() = sessionService.isAuthenticated()
    fun isCurrentUserAdmin() = sessionService.isCurrentUserAdmin()

    // ================================================================
    // MÉTHODES PRIVÉES
    // ================================================================

    private fun updateApplicationState(update: (ApplicationState) -> ApplicationState) {
        _applicationState.value = update(_applicationState.value)
    }
}

// ================================================================
// DATA CLASSES SIMPLIFIÉES
// ================================================================

/**
 * État global simplifié de l'application
 */
data class ApplicationState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: MembreFamille? = null,
    val lastError: String? = null,
    val lastInitialized: LocalDateTime? = null,
    val lastLogin: LocalDateTime? = null,
    val lastNavigation: LocalDateTime? = null,
    val applicationVersion: String = "2.0-minimal"
)