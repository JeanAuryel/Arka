package services

import repositories.FamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.time.LocalDateTime
import java.time.Duration

/**
 * Service de gestion de la santé du système
 *
 * Responsabilités:
 * - Vérification de la santé des composants système
 * - Diagnostic des problèmes de performance
 * - Monitoring des ressources système
 * - Initialisation et validation des services
 * - Génération de rapports de santé
 * - Détection d'anomalies et alertes
 *
 * Utilisé par: MainController, Tâches de monitoring
 * Utilise: Repositories, Controllers système
 *
 * Pattern: Health Check Pattern + Monitoring
 */
class HealthService(
    private val familyRepository: FamilyRepository
) {

    /**
     * Résultats des vérifications de santé
     */
    sealed class HealthResult {
        data class Success(val health: ApplicationHealth) : HealthResult()
        data class Error(val message: String, val severity: HealthSeverity) : HealthResult()
    }

    enum class HealthSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * Cache des dernières vérifications pour éviter les tests répétés
     */
    private var lastHealthCheck: HealthCheckCache? = null
    private val healthCacheValidityMinutes = 2L // Cache valide 2 minutes

    /**
     * Historique des vérifications de santé
     */
    private val healthHistory = mutableListOf<HealthCheckResult>()
    private val maxHistorySize = 100

    // ================================================================
    // MÉTHODES D'INITIALISATION DU SYSTÈME
    // ================================================================

    /**
     * Initialise et vérifie tous les composants du système
     *
     * @return Résultat de l'initialisation
     */
    suspend fun initializeSystem(): HealthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            println("🚀 Début de l'initialisation du système...")
            val startTime = System.currentTimeMillis()

            // Vérifications parallèles pour optimiser le temps d'initialisation
            val initResults = supervisorScope {
                val databaseCheck = async { checkDatabaseHealth() }
                val systemResourcesCheck = async { checkSystemResources() }
                val configurationCheck = async { checkConfiguration() }

                InitializationResults(
                    database = databaseCheck.await(),
                    systemResources = systemResourcesCheck.await(),
                    configuration = configurationCheck.await()
                )
            }

            val initDuration = System.currentTimeMillis() - startTime

            // Analyser les résultats
            val health = analyzeInitializationResults(initResults, initDuration)

            // Enregistrer dans l'historique
            recordHealthCheck(health, "SYSTEM_INITIALIZATION")

            if (health.overallStatus == HealthStatus.HEALTHY || health.overallStatus == HealthStatus.WARNING) {
                println("✅ Initialisation système terminée en ${initDuration}ms - Statut: ${health.overallStatus}")
                HealthResult.Success(health)
            } else {
                val errorMessage = "Échec de l'initialisation: ${health.issues.joinToString(", ")}"
                println("❌ $errorMessage")
                HealthResult.Error(errorMessage, HealthSeverity.CRITICAL)
            }

        } catch (e: Exception) {
            val errorMessage = "Erreur critique lors de l'initialisation: ${e.message}"
            println("💥 $errorMessage")
            HealthResult.Error(errorMessage, HealthSeverity.CRITICAL)
        }
    }

    /**
     * Obtient l'état de santé général du système
     *
     * @param forceRefresh Force une nouvelle vérification même si le cache est valide
     * @return État de santé du système
     */
    suspend fun getSystemHealth(forceRefresh: Boolean = false): HealthResult = withContext(Dispatchers.IO) {
        return@withContext try {
            // Vérifier le cache si pas de forçage
            if (!forceRefresh && isHealthCacheValid()) {
                val cachedHealth = lastHealthCheck?.health
                if (cachedHealth != null) {
                    return@withContext HealthResult.Success(cachedHealth)
                }
            }

            println("🔍 Vérification de santé du système...")
            val startTime = System.currentTimeMillis()

            // Vérifications parallèles pour optimiser les performances
            val healthChecks = supervisorScope {
                val databaseHealth = async { checkDatabaseHealth() }
                val performanceHealth = async { checkPerformanceMetrics() }
                val resourceHealth = async { checkSystemResources() }
                val dataIntegrityHealth = async { checkDataIntegrity() }

                SystemHealthChecks(
                    database = databaseHealth.await(),
                    performance = performanceHealth.await(),
                    resources = resourceHealth.await(),
                    dataIntegrity = dataIntegrityHealth.await()
                )
            }

            val checkDuration = System.currentTimeMillis() - startTime

            // Construire le rapport de santé
            val health = buildHealthReport(healthChecks, checkDuration)

            // Mettre à jour le cache
            updateHealthCache(health)

            // Enregistrer dans l'historique
            recordHealthCheck(health, "ROUTINE_CHECK")

            println("✅ Vérification santé terminée en ${checkDuration}ms - Statut: ${health.overallStatus}")
            HealthResult.Success(health)

        } catch (e: Exception) {
            val errorMessage = "Erreur lors de la vérification de santé: ${e.message}"
            println("❌ $errorMessage")
            HealthResult.Error(errorMessage, HealthSeverity.ERROR)
        }
    }

    // ================================================================
    // VÉRIFICATIONS SPÉCIFIQUES DE SANTÉ
    // ================================================================

    /**
     * Vérifie la santé de la base de données
     */
    private suspend fun checkDatabaseHealth(): DatabaseHealthCheck {
        return try {
            val startTime = System.currentTimeMillis()

            // Test de connexion basique
            val isConnected = familyRepository.healthCheck()
            val responseTime = System.currentTimeMillis() - startTime

            if (!isConnected) {
                return DatabaseHealthCheck(
                    isConnected = false,
                    responseTimeMs = responseTime,
                    status = HealthStatus.CRITICAL,
                    details = "Impossible de se connecter à la base de données"
                )
            }

            // Tests approfondis si connexion OK
            val detailedChecks = performDetailedDatabaseChecks()

            DatabaseHealthCheck(
                isConnected = true,
                responseTimeMs = responseTime,
                status = when {
                    responseTime > 5000 -> HealthStatus.WARNING
                    responseTime > 1000 -> HealthStatus.DEGRADED
                    else -> HealthStatus.HEALTHY
                },
                details = "Base de données accessible",
                tablesAccessible = detailedChecks.tablesAccessible,
                lastError = detailedChecks.lastError
            )

        } catch (e: Exception) {
            DatabaseHealthCheck(
                isConnected = false,
                responseTimeMs = -1,
                status = HealthStatus.CRITICAL,
                details = "Erreur lors de la vérification DB: ${e.message}"
            )
        }
    }

    /**
     * Vérifie les métriques de performance
     */
    private suspend fun checkPerformanceMetrics(): PerformanceHealthCheck {
        return try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val memoryUsagePercent = (usedMemory.toDouble() / totalMemory.toDouble()) * 100

            // Simulation de vérification CPU (à adapter selon les besoins)
            val cpuUsage = estimateCpuUsage()

            val status = when {
                memoryUsagePercent > 90 || cpuUsage > 90 -> HealthStatus.CRITICAL
                memoryUsagePercent > 75 || cpuUsage > 75 -> HealthStatus.WARNING
                memoryUsagePercent > 60 || cpuUsage > 60 -> HealthStatus.DEGRADED
                else -> HealthStatus.HEALTHY
            }

            PerformanceHealthCheck(
                memoryUsagePercent = memoryUsagePercent,
                cpuUsagePercent = cpuUsage,
                totalMemoryMB = totalMemory / (1024 * 1024),
                freeMemoryMB = freeMemory / (1024 * 1024),
                status = status,
                details = "Mémoire: ${String.format("%.1f", memoryUsagePercent)}%, CPU: ${String.format("%.1f", cpuUsage)}%"
            )

        } catch (e: Exception) {
            PerformanceHealthCheck(
                status = HealthStatus.ERROR,
                details = "Erreur lors de la vérification performance: ${e.message}"
            )
        }
    }

    /**
     * Vérifie les ressources système
     */
    private suspend fun checkSystemResources(): SystemResourcesCheck {
        return try {
            // Vérification de l'espace disque (simulation)
            val diskUsage = checkDiskUsage()

            // Vérification des connexions réseau (si applicable)
            val networkStatus = checkNetworkConnectivity()

            val status = when {
                diskUsage > 95 -> HealthStatus.CRITICAL
                diskUsage > 85 -> HealthStatus.WARNING
                diskUsage > 75 -> HealthStatus.DEGRADED
                else -> HealthStatus.HEALTHY
            }

            SystemResourcesCheck(
                diskUsagePercent = diskUsage,
                networkConnected = networkStatus,
                status = status,
                details = "Disque: ${String.format("%.1f", diskUsage)}%, Réseau: ${if (networkStatus) "OK" else "NOK"}"
            )

        } catch (e: Exception) {
            SystemResourcesCheck(
                status = HealthStatus.ERROR,
                details = "Erreur lors de la vérification des ressources: ${e.message}"
            )
        }
    }

    /**
     * Vérifie l'intégrité des données
     */
    private suspend fun checkDataIntegrity(): DataIntegrityCheck {
        return try {
            // Ici on peut ajouter des vérifications spécifiques aux données Arka
            // Par exemple: cohérence des relations, orphelins, etc.

            var issues = 0
            val details = mutableListOf<String>()

            // Exemple de vérifications (à adapter selon vos besoins)
            // val orphanFiles = fileRepository.findOrphanFiles()
            // if (orphanFiles.isNotEmpty()) {
            //     issues++
            //     details.add("${orphanFiles.size} fichiers orphelins détectés")
            // }

            val status = when {
                issues > 10 -> HealthStatus.WARNING
                issues > 0 -> HealthStatus.DEGRADED
                else -> HealthStatus.HEALTHY
            }

            DataIntegrityCheck(
                issuesFound = issues,
                status = status,
                details = if (details.isEmpty()) "Intégrité des données OK" else details.joinToString(", ")
            )

        } catch (e: Exception) {
            DataIntegrityCheck(
                issuesFound = -1,
                status = HealthStatus.ERROR,
                details = "Erreur lors de la vérification d'intégrité: ${e.message}"
            )
        }
    }

    /**
     * Vérifie la configuration de l'application
     */
    private suspend fun checkConfiguration(): ConfigurationCheck {
        return try {
            val issues = mutableListOf<String>()

            // Vérifier les variables d'environnement critiques
            // if (System.getProperty("database.url").isNullOrBlank()) {
            //     issues.add("URL de base de données manquante")
            // }

            val status = when {
                issues.isNotEmpty() -> HealthStatus.WARNING
                else -> HealthStatus.HEALTHY
            }

            ConfigurationCheck(
                status = status,
                details = if (issues.isEmpty()) "Configuration OK" else issues.joinToString(", ")
            )

        } catch (e: Exception) {
            ConfigurationCheck(
                status = HealthStatus.ERROR,
                details = "Erreur lors de la vérification configuration: ${e.message}"
            )
        }
    }

    // ================================================================
    // MÉTHODES AUXILIAIRES DE VÉRIFICATION
    // ================================================================

    /**
     * Effectue des vérifications détaillées de base de données
     */
    private suspend fun performDetailedDatabaseChecks(): DetailedDatabaseCheck {
        val accessibleTables = mutableListOf<String>()
        var lastError: String? = null

        try {
            // Test d'accès aux tables principales
            val tablesToCheck = listOf("Familles", "MembresFamille", "Dossiers", "Fichiers")

            tablesToCheck.forEach { tableName ->
                try {
                    when (tableName) {
                        "Familles" -> familyRepository.findAll().take(1)
                        // Ajouter d'autres vérifications selon vos repositories
                    }
                    accessibleTables.add(tableName)
                } catch (e: Exception) {
                    lastError = "Erreur table $tableName: ${e.message}"
                }
            }

        } catch (e: Exception) {
            lastError = "Erreur lors des vérifications détaillées: ${e.message}"
        }

        return DetailedDatabaseCheck(accessibleTables, lastError)
    }

    /**
     * Estime l'utilisation CPU (simulation simple)
     */
    private fun estimateCpuUsage(): Double {
        // Implémentation simple - à remplacer par une vraie mesure si nécessaire
        val startTime = System.nanoTime()
        var counter = 0
        val endTime = startTime + 10_000_000 // 10ms de test

        while (System.nanoTime() < endTime) {
            counter++
        }

        // Convertir en pourcentage approximatif (très basique)
        return minOf(counter / 100000.0, 100.0)
    }

    /**
     * Vérifie l'usage disque (simulation)
     */
    private fun checkDiskUsage(): Double {
        return try {
            val file = java.io.File(".")
            val totalSpace = file.totalSpace
            val freeSpace = file.freeSpace
            val usedSpace = totalSpace - freeSpace

            (usedSpace.toDouble() / totalSpace.toDouble()) * 100
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Vérifie la connectivité réseau (basique)
     */
    private fun checkNetworkConnectivity(): Boolean {
        return try {
            // Test simple - peut être étendu selon les besoins
            java.net.InetAddress.getByName("google.com").isReachable(5000)
        } catch (e: Exception) {
            false
        }
    }

    // ================================================================
    // MÉTHODES DE CACHE ET HISTORIQUE
    // ================================================================

    /**
     * Vérifie si le cache de santé est valide
     */
    private fun isHealthCacheValid(): Boolean {
        val cache = lastHealthCheck ?: return false
        val now = LocalDateTime.now()
        val duration = Duration.between(cache.timestamp, now)
        return duration.toMinutes() < healthCacheValidityMinutes
    }

    /**
     * Met à jour le cache de santé
     */
    private fun updateHealthCache(health: ApplicationHealth) {
        lastHealthCheck = HealthCheckCache(health, LocalDateTime.now())
    }

    /**
     * Enregistre une vérification dans l'historique
     */
    private fun recordHealthCheck(health: ApplicationHealth, checkType: String) {
        val result = HealthCheckResult(
            timestamp = LocalDateTime.now(),
            checkType = checkType,
            status = health.overallStatus,
            duration = health.checkDurationMs,
            issueCount = health.issues.size
        )

        healthHistory.add(result)

        // Limiter la taille de l'historique
        if (healthHistory.size > maxHistorySize) {
            healthHistory.removeAt(0)
        }
    }

    /**
     * Obtient l'historique des vérifications
     */
    fun getHealthHistory(limit: Int = 20): List<HealthCheckResult> {
        return healthHistory.takeLast(limit)
    }

    // ================================================================
    // MÉTHODES DE CONSTRUCTION DE RAPPORTS
    // ================================================================

    /**
     * Analyse les résultats d'initialisation
     */
    private fun analyzeInitializationResults(
        results: InitializationResults,
        duration: Long
    ): ApplicationHealth {
        val issues = mutableListOf<String>()

        if (results.database.status != HealthStatus.HEALTHY) {
            issues.add("Base de données: ${results.database.details}")
        }
        if (results.systemResources.status == HealthStatus.CRITICAL) {
            issues.add("Ressources système: ${results.systemResources.details}")
        }
        if (results.configuration.status != HealthStatus.HEALTHY) {
            issues.add("Configuration: ${results.configuration.details}")
        }

        val overallStatus = when {
            issues.any { it.contains("Base de données") } -> HealthStatus.CRITICAL
            issues.isNotEmpty() -> HealthStatus.WARNING
            else -> HealthStatus.HEALTHY
        }

        return ApplicationHealth(
            overallStatus = overallStatus,
            databaseConnected = results.database.isConnected,
            userSessionValid = false, // Pas de session à l'initialisation
            systemHealth = SystemHealth(
                cpuUsage = results.systemResources.cpuUsage ?: 0.0,
                memoryUsage = results.systemResources.memoryUsage ?: 0.0,
                diskSpace = results.systemResources.diskUsagePercent,
                status = overallStatus.name
            ),
            lastHealthCheck = LocalDateTime.now(),
            checkDurationMs = duration,
            issues = issues,
            details = mapOf(
                "database" to results.database,
                "systemResources" to results.systemResources,
                "configuration" to results.configuration
            )
        )
    }

    /**
     * Construit un rapport de santé complet
     */
    private fun buildHealthReport(
        checks: SystemHealthChecks,
        duration: Long
    ): ApplicationHealth {
        val issues = mutableListOf<String>()

        // Analyser chaque vérification
        if (checks.database.status == HealthStatus.CRITICAL) {
            issues.add("Base de données critique: ${checks.database.details}")
        } else if (checks.database.status == HealthStatus.WARNING) {
            issues.add("Base de données dégradée: ${checks.database.details}")
        }

        if (checks.performance.status == HealthStatus.CRITICAL) {
            issues.add("Performance critique: ${checks.performance.details}")
        } else if (checks.performance.status == HealthStatus.WARNING) {
            issues.add("Performance dégradée: ${checks.performance.details}")
        }

        if (checks.resources.status == HealthStatus.CRITICAL) {
            issues.add("Ressources critiques: ${checks.resources.details}")
        }

        if (checks.dataIntegrity.status == HealthStatus.WARNING) {
            issues.add("Intégrité des données: ${checks.dataIntegrity.details}")
        }

        // Déterminer le statut global
        val overallStatus = when {
            listOf(checks.database.status, checks.performance.status, checks.resources.status, checks.dataIntegrity.status)
                .any { it == HealthStatus.CRITICAL } -> HealthStatus.CRITICAL
            listOf(checks.database.status, checks.performance.status, checks.resources.status, checks.dataIntegrity.status)
                .any { it == HealthStatus.WARNING } -> HealthStatus.WARNING
            listOf(checks.database.status, checks.performance.status, checks.resources.status, checks.dataIntegrity.status)
                .any { it == HealthStatus.DEGRADED } -> HealthStatus.DEGRADED
            else -> HealthStatus.HEALTHY
        }

        return ApplicationHealth(
            overallStatus = overallStatus,
            databaseConnected = checks.database.isConnected,
            userSessionValid = true, // Assumé valide pendant vérification routine
            systemHealth = SystemHealth(
                cpuUsage = checks.performance.cpuUsagePercent,
                memoryUsage = checks.performance.memoryUsagePercent,
                diskSpace = checks.resources.diskUsagePercent,
                status = overallStatus.name
            ),
            lastHealthCheck = LocalDateTime.now(),
            checkDurationMs = duration,
            issues = issues,
            details = mapOf(
                "database" to checks.database,
                "performance" to checks.performance,
                "resources" to checks.resources,
                "dataIntegrity" to checks.dataIntegrity
            )
        )
    }

    // ================================================================
    // MÉTHODES DE NETTOYAGE ET MAINTENANCE
    // ================================================================

    /**
     * Nettoyage des ressources du service
     */
    fun cleanup() {
        lastHealthCheck = null
        healthHistory.clear()
        println("🧹 HealthService nettoyé")
    }

    /**
     * Obtient un résumé du service de santé
     */
    fun getServiceSummary(): Map<String, Any?> {
        return mapOf(
            "lastCheckTime" to lastHealthCheck?.timestamp,
            "cacheValid" to isHealthCacheValid(),
            "historySize" to healthHistory.size,
            "lastStatus" to lastHealthCheck?.health?.overallStatus?.name
        )
    }
}

// ================================================================
// DATA CLASSES POUR HEALTH CHECKS
// ================================================================

data class HealthCheckCache(
    val health: ApplicationHealth,
    val timestamp: LocalDateTime
)

data class HealthCheckResult(
    val timestamp: LocalDateTime,
    val checkType: String,
    val status: HealthStatus,
    val duration: Long,
    val issueCount: Int
)

data class InitializationResults(
    val database: DatabaseHealthCheck,
    val systemResources: SystemResourcesCheck,
    val configuration: ConfigurationCheck
)

data class SystemHealthChecks(
    val database: DatabaseHealthCheck,
    val performance: PerformanceHealthCheck,
    val resources: SystemResourcesCheck,
    val dataIntegrity: DataIntegrityCheck
)

data class DatabaseHealthCheck(
    val isConnected: Boolean,
    val responseTimeMs: Long,
    val status: HealthStatus,
    val details: String,
    val tablesAccessible: List<String> = emptyList(),
    val lastError: String? = null
)

data class PerformanceHealthCheck(
    val memoryUsagePercent: Double = 0.0,
    val cpuUsagePercent: Double = 0.0,
    val totalMemoryMB: Long = 0,
    val freeMemoryMB: Long = 0,
    val status: HealthStatus,
    val details: String
)

data class SystemResourcesCheck(
    val diskUsagePercent: Double = 0.0,
    val networkConnected: Boolean = true,
    val status: HealthStatus,
    val details: String,
    val cpuUsage: Double? = null,
    val memoryUsage: Double? = null
)

data class DataIntegrityCheck(
    val issuesFound: Int,
    val status: HealthStatus,
    val details: String
)

data class ConfigurationCheck(
    val status: HealthStatus,
    val details: String
)

data class DetailedDatabaseCheck(
    val tablesAccessible: List<String>,
    val lastError: String?
)

enum class HealthStatus {
    HEALTHY,
    DEGRADED,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Santé globale de l'application (étendue)
 */
data class ApplicationHealth(
    val overallStatus: HealthStatus,
    val databaseConnected: Boolean,
    val userSessionValid: Boolean,
    val systemHealth: SystemHealth,
    val lastHealthCheck: LocalDateTime,
    val checkDurationMs: Long = 0,
    val issues: List<String> = emptyList(),
    val details: Map<String, Any> = emptyMap()
)