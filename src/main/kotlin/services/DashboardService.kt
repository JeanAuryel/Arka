package services

import controllers.*
import ktorm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.time.LocalDateTime

/**
 * Service de gestion du tableau de bord
 *
 * Responsabilités:
 * - Collecte et agrégation des données du dashboard
 * - Mise en cache des informations fréquemment consultées
 * - Calcul des statistiques en temps réel
 * - Gestion de l'état réactif du dashboard
 * - Optimisation des requêtes de données
 *
 * Utilisé par: MainController, HomeScreen
 * Utilise: Tous les controllers métier
 *
 * Pattern: Service Layer + Observer (StateFlow)
 *
 * Note: Version temporaire avec des placeholders en attendant les controllers complets
 */
class DashboardService(
    private val sessionService: SessionService
    // TODO: Ajouter d'autres controllers quand ils seront créés
    // private val familyMemberController: FamilyMemberController,
    // private val categoryController: CategoryController,
    // private val fileController: FileController,
    // private val delegationController: DelegationController,
) {

    /**
     * État réactif du dashboard
     */
    private val _dashboardData = MutableStateFlow<DashboardData?>(null)
    val dashboardData: StateFlow<DashboardData?> = _dashboardData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Cache des données pour éviter les requêtes répétées
     */
    private var lastDataLoad: LocalDateTime? = null
    private val cacheValidityMinutes = 5L // Cache valide 5 minutes

    /**
     * Résultats des opérations du dashboard
     */
    sealed class DashboardResult {
        data class Success(val data: DashboardData) : DashboardResult()
        data class Error(val message: String) : DashboardResult()
    }

    // ================================================================
    // MÉTHODES PRINCIPALES DE CHARGEMENT
    // ================================================================

    /**
     * Charge toutes les données du dashboard
     * Utilise des coroutines parallèles pour optimiser les performances
     *
     * @param forceRefresh Force le rechargement même si le cache est valide
     * @return Les données du dashboard ou null en cas d'erreur
     */
    suspend fun loadDashboardData(forceRefresh: Boolean = false): DashboardData? = withContext(Dispatchers.IO) {
        try {
            // Vérifier le cache si pas de forçage
            if (!forceRefresh && isCacheValid()) {
                return@withContext _dashboardData.value
            }

            _isLoading.value = true
            _lastError.value = null

            val currentUser = sessionService.getCurrentUser()
                ?: return@withContext null.also {
                    setError("Utilisateur non connecté")
                }

            // Exécution parallèle des requêtes pour optimiser les performances
            val dashboardData = supervisorScope {
                try {
                    // Lancer toutes les requêtes en parallèle
                    val familyMembersDeferred = async { loadFamilyMembersData() }
                    val categoriesDeferred = async { loadCategoriesData() }
                    val filesDeferred = async { loadFilesData() }
                    val delegationsDeferred = async { loadDelegationsData() }
                    val statisticsDeferred = async { loadStatisticsData(currentUser.membreFamilleId) }

                    // Attendre tous les résultats
                    val familyMembersData = familyMembersDeferred.await()
                    val categoriesData = categoriesDeferred.await()
                    val filesData = filesDeferred.await()
                    val delegationsData = delegationsDeferred.await()
                    val statisticsData = statisticsDeferred.await()

                    // Construire l'objet DashboardData
                    DashboardData(
                        currentUser = currentUser,
                        familyMemberCount = familyMembersData.memberCount,
                        categoryCount = categoriesData.categoryCount,
                        recentFilesCount = filesData.recentFilesCount,
                        pendingDelegationsCount = delegationsData.pendingCount,
                        totalFileSize = statisticsData.totalFileSize,
                        lastActivity = statisticsData.lastActivity,
                        systemHealth = SystemHealth(0.0, 0.0, 0.0, "HEALTHY"), // À implémenter selon besoins
                        // Données étendues pour un dashboard plus riche
                        familyMembers = familyMembersData.members.take(5), // Top 5 pour preview
                        recentFiles = filesData.recentFiles.take(10),
                        categories = categoriesData.categories,
                        pendingDelegations = delegationsData.pendingDelegations.take(5),
                        quickStats = statisticsData.quickStats,
                        loadTime = LocalDateTime.now()
                    )

                } catch (e: Exception) {
                    println("❌ Erreur lors du chargement parallèle: ${e.message}")
                    null
                }
            }

            if (dashboardData != null) {
                _dashboardData.value = dashboardData
                lastDataLoad = LocalDateTime.now()
                println("✅ Données dashboard chargées avec succès")
            } else {
                setError("Erreur lors du chargement des données")
            }

            return@withContext dashboardData

        } catch (e: Exception) {
            setError("Erreur lors du chargement du dashboard: ${e.message}")
            return@withContext null
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Rafraîchit les données du dashboard
     */
    suspend fun refreshDashboardData(): DashboardResult = withContext(Dispatchers.IO) {
        val data = loadDashboardData(forceRefresh = true)
        return@withContext if (data != null) {
            DashboardResult.Success(data)
        } else {
            DashboardResult.Error(_lastError.value ?: "Erreur inconnue")
        }
    }

    // ================================================================
    // MÉTHODES DE CHARGEMENT DE DONNÉES SPÉCIFIQUES
    // ================================================================

    /**
     * Charge les données des membres de famille
     */
    private suspend fun loadFamilyMembersData(): FamilyMembersData {
        return try {
            // TODO: Remplacer par l'appel correct quand FamilyMemberController sera complété
            // val result = familyMemberController.getFamilyMembers()
            // Pour l'instant, retourner des données vides
            FamilyMembersData(
                memberCount = 0,
                members = emptyList(),
                activeMembers = 0
            )
        } catch (e: Exception) {
            println("❌ Erreur chargement membres famille: ${e.message}")
            FamilyMembersData()
        }
    }

    /**
     * Charge les données des catégories (placeholder)
     */
    private suspend fun loadCategoriesData(): CategoriesData {
        return try {
            // TODO: Remplacer par categoryController.getAllAccessibleCategories() quand disponible
            CategoriesData(
                categoryCount = 0,
                categories = emptyList(),
                categoriesWithFiles = 0
            )
        } catch (e: Exception) {
            println("❌ Erreur chargement catégories: ${e.message}")
            CategoriesData()
        }
    }

    /**
     * Charge les données des fichiers (placeholder)
     */
    private suspend fun loadFilesData(): FilesData {
        return try {
            // TODO: Remplacer par fileController.getRecentFiles(10) quand disponible
            FilesData(
                recentFilesCount = 0,
                recentFiles = emptyList(),
                totalFilesCount = 0
            )
        } catch (e: Exception) {
            println("❌ Erreur chargement fichiers: ${e.message}")
            FilesData()
        }
    }

    /**
     * Charge les données des délégations (placeholder)
     */
    private suspend fun loadDelegationsData(): DelegationsData {
        return try {
            // TODO: Remplacer par delegationController.getPendingDelegationRequests() quand disponible
            DelegationsData(
                pendingCount = 0,
                pendingDelegations = emptyList(),
                totalDelegationsCount = 0
            )
        } catch (e: Exception) {
            println("❌ Erreur chargement délégations: ${e.message}")
            DelegationsData()
        }
    }

    /**
     * Charge les statistiques générales
     */
    private suspend fun loadStatisticsData(userId: Int): StatisticsData {
        return try {
            // TODO: Remplacer par l'appel correct quand FamilyMemberController sera complété
            // val memberStats = familyMemberController.getMemberStatistics(userId)
            // Pour l'instant, retourner des statistiques vides

            val quickStats = QuickStats(
                filesUploaded = 0, // TODO: À récupérer depuis les vraies données
                foldersCreated = 0, // TODO: À récupérer depuis les vraies données
                delegationsReceived = 0, // À implémenter
                delegationsGiven = 0 // À implémenter
            )

            StatisticsData(
                totalFileSize = 0L, // TODO: À récupérer depuis les vraies données
                lastActivity = LocalDateTime.now(), // À récupérer depuis les données réelles
                quickStats = quickStats
            )
        } catch (e: Exception) {
            println("❌ Erreur chargement statistiques: ${e.message}")
            StatisticsData()
        }
    }

    // ================================================================
    // MÉTHODES DE GESTION DU CACHE
    // ================================================================

    /**
     * Vérifie si le cache est encore valide
     */
    private fun isCacheValid(): Boolean {
        val lastLoad = lastDataLoad ?: return false
        val now = LocalDateTime.now()
        val duration = java.time.Duration.between(lastLoad, now)
        return duration.toMinutes() < cacheValidityMinutes
    }

    /**
     * Invalide le cache et force le rechargement
     */
    fun invalidateCache() {
        lastDataLoad = null
        println("🔄 Cache dashboard invalidé")
    }

    // ================================================================
    // MÉTHODES DE NOTIFICATION ET MISE À JOUR
    // ================================================================

    /**
     * Notifie d'un changement dans les données (pour invalider le cache)
     */
    fun notifyDataChanged(dataType: DataChangeType) {
        when (dataType) {
            DataChangeType.FILES -> {
                // Invalider seulement la partie fichiers si possible
                invalidateCache()
            }
            DataChangeType.DELEGATIONS -> {
                // Recharger seulement les délégations
                invalidateCache()
            }
            DataChangeType.FAMILY_MEMBERS -> {
                // Recharger les données famille
                invalidateCache()
            }
            DataChangeType.CATEGORIES -> {
                // Recharger les catégories
                invalidateCache()
            }
            DataChangeType.ALL -> {
                // Recharger complètement
                invalidateCache()
            }
        }
    }

    /**
     * Met à jour une partie spécifique du dashboard
     */
    suspend fun updatePartialData(dataType: DataChangeType) {
        val currentData = _dashboardData.value ?: return

        when (dataType) {
            DataChangeType.FILES -> {
                val filesData = loadFilesData()
                _dashboardData.value = currentData.copy(
                    recentFilesCount = filesData.recentFilesCount,
                    recentFiles = filesData.recentFiles
                )
            }
            DataChangeType.DELEGATIONS -> {
                val delegationsData = loadDelegationsData()
                _dashboardData.value = currentData.copy(
                    pendingDelegationsCount = delegationsData.pendingCount,
                    pendingDelegations = delegationsData.pendingDelegations
                )
            }
            DataChangeType.CATEGORIES -> {
                val categoriesData = loadCategoriesData()
                _dashboardData.value = currentData.copy(
                    categoryCount = categoriesData.categoryCount,
                    categories = categoriesData.categories
                )
            }
            DataChangeType.FAMILY_MEMBERS -> {
                val familyData = loadFamilyMembersData()
                _dashboardData.value = currentData.copy(
                    familyMemberCount = familyData.memberCount,
                    familyMembers = familyData.members.take(5) // Limiter pour l'affichage
                )
            }
            DataChangeType.ALL -> {
                loadDashboardData(forceRefresh = true)
            }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE ET FILTRAGE
    // ================================================================

    /**
     * Filtre les données du dashboard selon des critères
     */
    fun filterDashboardData(filter: DashboardFilter): DashboardData? {
        val data = _dashboardData.value ?: return null

        return data.copy(
            recentFiles = data.recentFiles.filter { file ->
                when (filter.fileType) {
                    null -> true
                    else -> file.typeFichier == filter.fileType
                }
            },
            categories = data.categories.filter { category ->
                when (filter.categoryName) {
                    null -> true
                    else -> category.nomCategorie.contains(filter.categoryName, ignoreCase = true)
                }
            }
        )
    }

    // ================================================================
    // MÉTHODES AUXILIAIRES
    // ================================================================

    /**
     * Obtient le nombre total de fichiers (méthode auxiliaire)
     */
    private suspend fun getTotalFilesCount(): Int {
        return try {
            // Implémentation selon vos besoins
            // fileController.getTotalFileCount() ou requête directe
            0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Obtient le nombre total de délégations
     */
    private suspend fun getTotalDelegationsCount(): Int {
        return try {
            // delegationController.getTotalDelegationsCount()
            0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Définit une erreur et la propage
     */
    private fun setError(message: String) {
        _lastError.value = message
        println("❌ DashboardService Error: $message")
    }

    // ================================================================
    // MÉTHODES DE NETTOYAGE
    // ================================================================

    /**
     * Vide le cache et les données du dashboard
     */
    fun clearDashboardData() {
        _dashboardData.value = null
        lastDataLoad = null
        _lastError.value = null
        println("🧹 Données dashboard nettoyées")
    }

    /**
     * Nettoyage des ressources
     */
    fun cleanup() {
        clearDashboardData()
        _isLoading.value = false
    }

    // ================================================================
    // MÉTHODES D'INFORMATION
    // ================================================================

    /**
     * Obtient un résumé de l'état du service
     */
    fun getServiceStatus(): Map<String, Any?> {
        return mapOf(
            "hasData" to (_dashboardData.value != null),
            "isLoading" to _isLoading.value,
            "lastError" to _lastError.value,
            "lastDataLoad" to lastDataLoad,
            "cacheValid" to isCacheValid(),
            "dataItemsCount" to (_dashboardData.value?.let {
                it.familyMemberCount + it.categoryCount + it.recentFilesCount + it.pendingDelegationsCount
            } ?: 0)
        )
    }
}

// ================================================================
// DATA CLASSES AUXILIAIRES
// ================================================================

/**
 * Structure pour les données des membres de famille
 */
data class FamilyMembersData(
    val memberCount: Int = 0,
    val members: List<MembreFamille> = emptyList(),
    val activeMembers: Int = 0
)

/**
 * Structure pour les données des catégories
 */
data class CategoriesData(
    val categoryCount: Int = 0,
    val categories: List<Categorie> = emptyList(),
    val categoriesWithFiles: Int = 0
)

/**
 * Structure pour les données des fichiers
 */
data class FilesData(
    val recentFilesCount: Int = 0,
    val recentFiles: List<Fichier> = emptyList(),
    val totalFilesCount: Int = 0
)

/**
 * Structure pour les données des délégations
 */
data class DelegationsData(
    val pendingCount: Int = 0,
    val pendingDelegations: List<DemandeDelegation> = emptyList(),
    val totalDelegationsCount: Int = 0
)

/**
 * Structure pour les statistiques
 */
data class StatisticsData(
    val totalFileSize: Long = 0L,
    val lastActivity: LocalDateTime = LocalDateTime.now(),
    val quickStats: QuickStats = QuickStats()
)

/**
 * Statistiques rapides pour le dashboard
 */
data class QuickStats(
    val filesUploaded: Int = 0,
    val foldersCreated: Int = 0,
    val delegationsReceived: Int = 0,
    val delegationsGiven: Int = 0
)

/**
 * DashboardData étendu avec plus d'informations
 */
data class DashboardData(
    val currentUser: MembreFamille,
    val familyMemberCount: Int,
    val categoryCount: Int,
    val recentFilesCount: Int,
    val pendingDelegationsCount: Int,
    val totalFileSize: Long,
    val lastActivity: LocalDateTime,
    val systemHealth: SystemHealth,
    // Données étendues
    val familyMembers: List<MembreFamille> = emptyList(),
    val recentFiles: List<Fichier> = emptyList(),
    val categories: List<Categorie> = emptyList(),
    val pendingDelegations: List<DemandeDelegation> = emptyList(),
    val quickStats: QuickStats = QuickStats(),
    val loadTime: LocalDateTime = LocalDateTime.now()
)

/**
 * Types de changements de données
 */
enum class DataChangeType {
    FILES,
    DELEGATIONS,
    FAMILY_MEMBERS,
    CATEGORIES,
    ALL
}

/**
 * Filtre pour les données du dashboard
 */
data class DashboardFilter(
    val fileType: String? = null,
    val categoryName: String? = null,
    val dateRange: Pair<LocalDateTime, LocalDateTime>? = null
)

/**
 * Santé du système (peut être étendu)
 */
data class SystemHealth(
    val cpuUsage: Double,
    val memoryUsage: Double,
    val diskSpace: Double,
    val status: String
)