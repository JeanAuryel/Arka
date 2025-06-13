package controllers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import repositories.*
import ktorm.*
import java.time.LocalDateTime

/**
 * Main Controller - Application orchestrator and entry point
 *
 * Responsibilities:
 * - Application initialization and setup
 * - Navigation state management
 * - Global application state
 * - Inter-controller communication
 * - Session management
 * - Application-wide error handling
 * - Dashboard and overview data
 *
 * Design Pattern: Facade + Mediator
 * This controller acts as a facade for the UI and mediates between other controllers
 */
class MainController(
    private val authController: AuthController,
    private val familyMemberController: FamilyMemberController,
    private val categoryController: CategoryController,
    private val folderController: FolderController,
    private val fileController: FileController,
    private val delegationController: DelegationController,
    private val permissionController: PermissionController,
    private val familyRepository: FamilyRepository
) {

    /**
     * Application states using StateFlow for reactive UI
     * This is a modern approach for state management in Kotlin/Compose
     */
    private val _applicationState = MutableStateFlow(ApplicationState())
    val applicationState: StateFlow<ApplicationState> = _applicationState.asStateFlow()

    private val _navigationState = MutableStateFlow(NavigationState.DASHBOARD)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _dashboardData = MutableStateFlow<DashboardData?>(null)
    val dashboardData: StateFlow<DashboardData?> = _dashboardData.asStateFlow()

    /**
     * Application-wide result wrapper
     */
    sealed class MainResult {
        data class Success<T>(val data: T) : MainResult()
        data class Error(val message: String, val code: MainErrorCode) : MainResult()
    }

    enum class MainErrorCode {
        INITIALIZATION_FAILED,
        SESSION_EXPIRED,
        NETWORK_ERROR,
        PERMISSION_DENIED,
        DATA_CORRUPTION,
        INTERNAL_ERROR
    }

    /**
     * Initialize the application
     * This should be called when the app starts
     */
    suspend fun initializeApplication(): MainResult = withContext(Dispatchers.IO) {
        try {
            updateApplicationState { it.copy(isLoading = true, isInitialized = false) }

            // Initialize database connections and verify schema
            val dbInitResult = initializeDatabase()
            if (dbInitResult is MainResult.Error) {
                return@withContext dbInitResult
            }

            // Load application configuration
            loadApplicationConfiguration()

            // Check for existing session
            checkExistingSession()

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
                "Échec de l'initialisation de l'application: ${e.message}",
                MainErrorCode.INITIALIZATION_FAILED
            )
        }
    }

    /**
     * Handle user login through AuthController
     */
    suspend fun login(email: String, password: String): MainResult = withContext(Dispatchers.IO) {
        try {
            updateApplicationState { it.copy(isLoading = true) }

            val loginRequest = LoginRequest(email, password)
            val authResult = authController.login(loginRequest)

            when (authResult) {
                is AuthController.AuthResult.Success -> {
                    // Update application state with logged user
                    updateApplicationState {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            currentUser = authResult.member,
                            lastLogin = LocalDateTime.now()
                        )
                    }

                    // Load dashboard data
                    loadDashboardData()

                    // Navigate to dashboard
                    navigateTo(NavigationState.DASHBOARD)

                    return@withContext MainResult.Success(authResult.member)
                }

                is AuthController.AuthResult.Error -> {
                    updateApplicationState { it.copy(isLoading = false) }
                    return@withContext MainResult.Error(
                        authResult.message,
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
     * Handle user logout
     */
    fun logout(): MainResult {
        try {
            authController.logout()

            // Reset application state
            updateApplicationState {
                ApplicationState().copy(isInitialized = true)
            }
            _dashboardData.value = null

            // Navigate to login
            navigateTo(NavigationState.LOGIN)

            return MainResult.Success(Unit)

        } catch (e: Exception) {
            return MainResult.Error(
                "Erreur lors de la déconnexion: ${e.message}",
                MainErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Load and refresh dashboard data
     */
    suspend fun loadDashboardData(): MainResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext MainResult.Error(
                    "Utilisateur non connecté",
                    MainErrorCode.SESSION_EXPIRED
                )

            updateApplicationState { it.copy(isLoading = true) }

            // Gather dashboard data from various controllers
            val familyMembers = familyMemberController.getFamilyMembers()
            val accessibleCategories = categoryController.getAllAccessibleCategories()
            val recentFiles = fileController.getRecentFiles(10)
            val pendingDelegations = delegationController.getPendingDelegationRequests()

            // Calculate statistics
            val stats = calculateDashboardStatistics()

            val dashboardData = DashboardData(
                currentUser = currentUser,
                familyMemberCount = when (familyMembers) {
                    is FamilyMemberController.FamilyMemberResult.Success -> familyMembers.data.size
                    else -> 0
                },
                categoryCount = when (accessibleCategories) {
                    is CategoryController.CategoryResult.Success -> accessibleCategories.data.size
                    else -> 0
                },
                recentFilesCount = when (recentFiles) {
                    is FileController.FileResult.Success -> recentFiles.data.size
                    else -> 0
                },
                pendingDelegationsCount = when (pendingDelegations) {
                    is DelegationController.DelegationResult.Success -> pendingDelegations.data.size
                    else -> 0
                },
                totalFileSize = stats.totalFileSize,
                lastActivity = stats.lastActivity,
                systemHealth = checkSystemHealth()
            )

            _dashboardData.value = dashboardData
            updateApplicationState { it.copy(isLoading = false) }

            return@withContext MainResult.Success(dashboardData)

        } catch (e: Exception) {
            updateApplicationState {
                it.copy(
                    isLoading = false,
                    lastError = "Erreur de chargement du tableau de bord: ${e.message}"
                )
            }
            return@withContext MainResult.Error(
                "Erreur lors du chargement du tableau de bord: ${e.message}",
                MainErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Navigate to a specific screen
     */
    fun navigateTo(destination: NavigationState) {
        _navigationState.value = destination
        updateApplicationState {
            it.copy(lastNavigation = LocalDateTime.now())
        }
    }

    /**
     * Handle global search across the application
     */
    suspend fun globalSearch(query: String): MainResult = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext MainResult.Error(
                    "Critère de recherche requis",
                    MainErrorCode.INTERNAL_ERROR
                )
            }

            val currentUser = authController.getCurrentUser()
                ?: return@withContext MainResult.Error(
                    "Utilisateur non connecté",
                    MainErrorCode.SESSION_EXPIRED
                )

            // Search across different entities
            val fileResults = fileController.searchFiles(query)
            val folderResults = folderController.searchFolders(query)

            val searchResults = GlobalSearchResults(
                query = query,
                files = when (fileResults) {
                    is FileController.FileResult.Success -> fileResults.data
                    else -> emptyList()
                },
                folders = when (folderResults) {
                    is FolderController.FolderResult.Success -> folderResults.data
                    else -> emptyList()
                },
                searchTime = LocalDateTime.now()
            )

            return@withContext MainResult.Success(searchResults)

        } catch (e: Exception) {
            return@withContext MainResult.Error(
                "Erreur lors de la recherche: ${e.message}",
                MainErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Get application health status
     */
    suspend fun getApplicationHealth(): MainResult = withContext(Dispatchers.IO) {
        try {
            val health = ApplicationHealth(
                databaseConnected = checkDatabaseConnection(),
                userSessionValid = authController.isAuthenticated(),
                systemHealth = checkSystemHealth(),
                lastHealthCheck = LocalDateTime.now()
            )

            return@withContext MainResult.Success(health)

        } catch (e: Exception) {
            return@withContext MainResult.Error(
                "Erreur lors de la vérification de l'état: ${e.message}",
                MainErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Handle application shutdown
     */
    suspend fun shutdownApplication(): MainResult = withContext(Dispatchers.IO) {
        try {
            // Save any pending data
            // Close database connections
            // Clean up resources

            updateApplicationState {
                it.copy(isShuttingDown = true)
            }

            return@withContext MainResult.Success(Unit)

        } catch (e: Exception) {
            return@withContext MainResult.Error(
                "Erreur lors de l'arrêt de l'application: ${e.message}",
                MainErrorCode.INTERNAL_ERROR
            )
        }
    }

    // Private helper methods

    private fun updateApplicationState(update: (ApplicationState) -> ApplicationState) {
        _applicationState.value = update(_applicationState.value)
    }

    private suspend fun initializeDatabase(): MainResult {
        return try {
            // Verify database connection and schema
            // Run any necessary migrations
            // Check table integrity
            MainResult.Success(Unit)
        } catch (e: Exception) {
            MainResult.Error(
                "Échec de l'initialisation de la base de données: ${e.message}",
                MainErrorCode.INITIALIZATION_FAILED
            )
        }
    }

    private fun loadApplicationConfiguration() {
        // Load app settings, preferences, etc.
        updateApplicationState {
            it.copy(
                applicationVersion = "2.0",
                databaseVersion = "1.0"
            )
        }
    }

    private fun checkExistingSession() {
        // Check for saved session or auto-login
        val isAuthenticated = authController.isAuthenticated()
        updateApplicationState {
            it.copy(
                isAuthenticated = isAuthenticated,
                currentUser = if (isAuthenticated) authController.getCurrentUser() else null
            )
        }
    }

    private suspend fun calculateDashboardStatistics(): DashboardStatistics {
        return try {
            val currentUser = authController.getCurrentUser()
            if (currentUser != null) {
                val stats = familyMemberController.getMemberStatistics(currentUser.membreFamilleId)
                when (stats) {
                    is FamilyMemberController.FamilyMemberResult.Success -> {
                        DashboardStatistics(
                            totalFileSize = stats.data.totalFileSize,
                            lastActivity = LocalDateTime.now() // This should come from actual data
                        )
                    }
                    else -> DashboardStatistics()
                }
            } else {
                DashboardStatistics()
            }
        } catch (e: Exception) {
            DashboardStatistics()
        }
    }

    private fun checkSystemHealth(): SystemHealth {
        return SystemHealth(
            cpuUsage = 0.0, // Would implement actual monitoring
            memoryUsage = 0.0,
            diskSpace = 0.0,
            status = "HEALTHY"
        )
    }

    private suspend fun checkDatabaseConnection(): Boolean {
        return try {
            // Perform a simple database query
            familyRepository.healthCheck()
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Application state data classes
 */
data class ApplicationState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isShuttingDown: Boolean = false,
    val currentUser: MembreFamille? = null,
    val lastError: String? = null,
    val lastInitialized: LocalDateTime? = null,
    val lastLogin: LocalDateTime? = null,
    val lastNavigation: LocalDateTime? = null,
    val applicationVersion: String = "2.0",
    val databaseVersion: String = "1.0"
)

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

data class DashboardData(
    val currentUser: MembreFamille,
    val familyMemberCount: Int,
    val categoryCount: Int,
    val recentFilesCount: Int,
    val pendingDelegationsCount: Int,
    val totalFileSize: Long,
    val lastActivity: LocalDateTime,
    val systemHealth: SystemHealth
)

data class DashboardStatistics(
    val totalFileSize: Long = 0L,
    val lastActivity: LocalDateTime = LocalDateTime.now()
)

data class GlobalSearchResults(
    val query: String,
    val files: List<Fichier>,
    val folders: List<Dossier>,
    val searchTime: LocalDateTime
)

data class ApplicationHealth(
    val databaseConnected: Boolean,
    val userSessionValid: Boolean,
    val systemHealth: SystemHealth,
    val lastHealthCheck: LocalDateTime
)

data class SystemHealth(
    val cpuUsage: Double,
    val memoryUsage: Double,
    val diskSpace: Double,
    val status: String
)