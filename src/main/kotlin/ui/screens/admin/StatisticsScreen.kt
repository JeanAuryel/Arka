// ================================================================
// STATISTICSSCREEN.KT - ANALYTICS ET STATISTIQUES
// ================================================================

package ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import controllers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.components.*
import ui.screens.FilterChip
import ui.theme.*

/**
 * Écran de statistiques et analytics
 *
 * Fonctionnalités:
 * - Tableau de bord des métriques
 * - Graphiques d'utilisation
 * - Statistiques par famille/utilisateur
 * - Analyses temporelles
 * - Export des données
 */
@Composable
fun StatisticsScreen(
    familyController: FamilyController,
    familyMemberController: FamilyMemberController,
    fileController: FileController,
    delegationController: DelegationRequestController,
    alertController: AlertController,
    authController: AuthController,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf(StatisticsPeriod.LAST_30_DAYS) }
    var selectedCategory by remember { mutableStateOf(StatisticsCategory.OVERVIEW) }
    var statisticsData by remember { mutableStateOf<StatisticsData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Charger les statistiques
    LaunchedEffect(selectedPeriod, selectedCategory) {
        scope.launch {
            isLoading = true
            try {
                // Simuler le chargement des données depuis les contrôleurs
                val families = familyController.getAllFamilies()
                val members = familyMemberController.getAllMembers()
                val delegations = delegationController.getAllDelegationRequests()
                val alerts = alertController.getAllAlerts()

                // Calculer les statistiques
                statisticsData = calculateStatistics(
                    families, members, delegations, alerts, selectedPeriod
                )

            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête avec navigation et contrôles
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Statistiques",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bouton d'export
            OutlinedButton(
                onClick = {
                    // Logique d'export des statistiques
                }
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = "Exporter")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exporter")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sélecteurs de période et catégorie
        StatisticsFilters(
            selectedPeriod = selectedPeriod,
            selectedCategory = selectedCategory,
            onPeriodChange = { selectedPeriod = it },
            onCategoryChange = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            statisticsData?.let { data ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedCategory) {
                        StatisticsCategory.OVERVIEW -> {
                            item { OverviewSection(data.overview) }
                            item { TrendsSection(data.trends) }
                            item { TopMetricsSection(data.topMetrics) }
                        }
                        StatisticsCategory.USERS -> {
                            item { UserStatisticsSection(data.userStats) }
                            item { ActivityHeatmapSection(data.activityHeatmap) }
                        }
                        StatisticsCategory.FILES -> {
                            item { FileStatisticsSection(data.fileStats) }
                            item { StorageAnalysisSection(data.storageAnalysis) }
                        }
                        StatisticsCategory.PERMISSIONS -> {
                            item { PermissionStatisticsSection(data.permissionStats) }
                            item { DelegationTrendsSection(data.delegationTrends) }
                        }
                        StatisticsCategory.PERFORMANCE -> {
                            item { PerformanceMetricsSection(data.performanceMetrics) }
                            item { SystemHealthSection(data.systemHealth) }
                        }
                    }
                }
            }
        }

        // Gestion des erreurs
        errorMessage?.let { error ->
            LaunchedEffect(error) {
                delay(5000)
                errorMessage = null
            }

            Snackbar(
                modifier = Modifier.padding(16.dp),
                backgroundColor = Color.Red.copy(alpha = 0.8f)
            ) {
                Text(text = error, color = Color.White)
            }
        }
    }
}

/**
 * Filtres de statistiques
 */
@Composable
private fun StatisticsFilters(
    selectedPeriod: StatisticsPeriod,
    selectedCategory: StatisticsCategory,
    onPeriodChange: (StatisticsPeriod) -> Unit,
    onCategoryChange: (StatisticsCategory) -> Unit
) {
    Column {
        // Sélecteur de période
        Text(
            text = "Période",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(StatisticsPeriod.values()) { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { onPeriodChange(period) },
                    label = { Text(period.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sélecteur de catégorie
        Text(
            text = "Catégorie",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(StatisticsCategory.values()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.label,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(category.label)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Section de vue d'ensemble
 */
@Composable
private fun OverviewSection(
    overview: OverviewStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Vue d'ensemble",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grille de métriques principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticCard(
                    title = "Familles",
                    value = overview.totalFamilies.toString(),
                    change = overview.familiesChange,
                    icon = Icons.Default.FamilyRestroom,
                    color = Color.Blue
                )

                StatisticCard(
                    title = "Utilisateurs",
                    value = overview.totalUsers.toString(),
                    change = overview.usersChange,
                    icon = Icons.Default.People,
                    color = Color.Green
                )

                StatisticCard(
                    title = "Fichiers",
                    value = overview.totalFiles.toString(),
                    change = overview.filesChange,
                    icon = Icons.Default.InsertDriveFile,
                    color = Color.Orange
                )

                StatisticCard(
                    title = "Permissions",
                    value = overview.totalPermissions.toString(),
                    change = overview.permissionsChange,
                    icon = Icons.Default.Security,
                    color = Color.Purple
                )
            }
        }
    }
}

/**
 * Carte de statistique individuelle
 */
@Composable
private fun StatisticCard(
    title: String,
    value: String,
    change: Double,
    icon: ImageVector,
    color: Color
) {
    Card(
        elevation = 2.dp,
        backgroundColor = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = title,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            // Indicateur de changement
            if (change != 0.0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (change > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = "Tendance",
                        tint = if (change > 0) Color.Green else Color.Red,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${if (change > 0) "+" else ""}${change.toInt()}%",
                        style = MaterialTheme.typography.caption,
                        color = if (change > 0) Color.Green else Color.Red,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Section des tendances
 */
@Composable
private fun TrendsSection(
    trends: TrendsData
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = "Tendances")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tendances",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graphique simple des tendances (mock)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📈 Graphique des tendances\n(${trends.dataPoints.size} points de données)",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Métriques de tendances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrendMetric("Croissance", "+${trends.growthRate}%", Color.Green)
                TrendMetric("Activité", "${trends.activityScore}/100", Color.Blue)
                TrendMetric("Engagement", "${trends.engagementRate}%", Color.Orange)
            }
        }
    }
}

/**
 * Métrique de tendance
 */
@Composable
private fun TrendMetric(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Section des métriques principales
 */
@Composable
private fun TopMetricsSection(
    topMetrics: TopMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = "Top métriques")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Métriques clés",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TopMetricItem("Famille la plus active", topMetrics.mostActiveFamily, Icons.Default.FamilyRestroom)
                TopMetricItem("Utilisateur le plus connecté", topMetrics.mostActiveUser, Icons.Default.Person)
                TopMetricItem("Type de fichier le plus utilisé", topMetrics.mostUsedFileType, Icons.Default.FilePresent)
                TopMetricItem("Permission la plus demandée", topMetrics.mostRequestedPermission, Icons.Default.Security)
            }
        }
    }
}

/**
 * Item de métrique principale
 */
@Composable
private fun TopMetricItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Section des statistiques utilisateurs
 */
@Composable
private fun UserStatisticsSection(
    userStats: UserStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.People, contentDescription = "Statistiques utilisateurs")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Statistiques utilisateurs",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticCard(
                    title = "Actifs",
                    value = userStats.activeUsers.toString(),
                    change = 0.0,
                    icon = Icons.Default.Person,
                    color = Color.Green
                )

                StatisticCard(
                    title = "Parents",
                    value = userStats.parentUsers.toString(),
                    change = 0.0,
                    icon = Icons.Default.AdminPanelSettings,
                    color = Color.Blue
                )

                StatisticCard(
                    title = "Enfants",
                    value = userStats.childUsers.toString(),
                    change = 0.0,
                    icon = Icons.Default.Child,
                    color = Color.Orange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Temps de connexion moyen: ${userStats.averageSessionTime} min",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// Autres sections similaires pour Files, Permissions, Performance...
// Pour économiser l'espace, je vais créer des sections simplifiées

/**
 * Section basique réutilisable
 */
@Composable
private fun BasicStatsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = title)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

// Utilisation des sections basiques pour les autres catégories
@Composable private fun FileStatisticsSection(fileStats: FileStats) =
    BasicStatsSection("Statistiques des fichiers", Icons.Default.FilePresent) {
        Text("Total: ${fileStats.totalFiles} fichiers")
        Text("Taille moyenne: ${fileStats.averageSize} MB")
    }

@Composable private fun StorageAnalysisSection(storageAnalysis: StorageAnalysis) =
    BasicStatsSection("Analyse du stockage", Icons.Default.Storage) {
        Text("Utilisé: ${storageAnalysis.usedSpace} GB")
        Text("Disponible: ${storageAnalysis.freeSpace} GB")
    }

@Composable private fun PermissionStatisticsSection(permissionStats: PermissionStats) =
    BasicStatsSection("Statistiques des permissions", Icons.Default.Security) {
        Text("Permissions actives: ${permissionStats.activePermissions}")
        Text("Demandes en attente: ${permissionStats.pendingRequests}")
    }

@Composable private fun DelegationTrendsSection(delegationTrends: DelegationTrends) =
    BasicStatsSection("Tendances des délégations", Icons.Default.TrendingUp) {
        Text("Taux d'approbation: ${delegationTrends.approvalRate}%")
        Text("Temps de réponse moyen: ${delegationTrends.averageResponseTime}h")
    }

@Composable private fun PerformanceMetricsSection(performanceMetrics: PerformanceMetrics) =
    BasicStatsSection("Métriques de performance", Icons.Default.Speed) {
        Text("Temps de réponse: ${performanceMetrics.responseTime}ms")
        Text("Disponibilité: ${performanceMetrics.uptime}%")
    }

@Composable private fun SystemHealthSection(systemHealth: SystemHealthStats) =
    BasicStatsSection("Santé du système", Icons.Default.MonitorHeart) {
        Text("CPU: ${systemHealth.cpuUsage}%")
        Text("Mémoire: ${systemHealth.memoryUsage}%")
    }

@Composable private fun ActivityHeatmapSection(activityHeatmap: ActivityHeatmap) =
    BasicStatsSection("Carte d'activité", Icons.Default.Heatmap) {
        Text("Pic d'activité: ${activityHeatmap.peakHour}h")
        Text("Jour le plus actif: ${activityHeatmap.busiestDay}")
    }

// ================================================================
// CLASSES ET ENUMS DE DONNÉES
// ================================================================

enum class StatisticsPeriod(val label: String) {
    LAST_7_DAYS("7 derniers jours"),
    LAST_30_DAYS("30 derniers jours"),
    LAST_3_MONTHS("3 derniers mois"),
    LAST_YEAR("Dernière année"),
    ALL_TIME("Depuis le début")
}

enum class StatisticsCategory(val label: String, val icon: ImageVector) {
    OVERVIEW("Vue d'ensemble", Icons.Default.Dashboard),
    USERS("Utilisateurs", Icons.Default.People),
    FILES("Fichiers", Icons.Default.FilePresent),
    PERMISSIONS("Permissions", Icons.Default.Security),
    PERFORMANCE("Performance", Icons.Default.Speed)
}

data class StatisticsData(
    val overview: OverviewStats,
    val trends: TrendsData,
    val topMetrics: TopMetrics,
    val userStats: UserStats,
    val fileStats: FileStats,
    val storageAnalysis: StorageAnalysis,
    val permissionStats: PermissionStats,
    val delegationTrends: DelegationTrends,
    val performanceMetrics: PerformanceMetrics,
    val systemHealth: SystemHealthStats,
    val activityHeatmap: ActivityHeatmap
)

data class OverviewStats(
    val totalFamilies: Int,
    val totalUsers: Int,
    val totalFiles: Int,
    val totalPermissions: Int,
    val familiesChange: Double,
    val usersChange: Double,
    val filesChange: Double,
    val permissionsChange: Double
)

data class TrendsData(
    val dataPoints: List<Double>,
    val growthRate: Double,
    val activityScore: Int,
    val engagementRate: Double
)

data class TopMetrics(
    val mostActiveFamily: String,
    val mostActiveUser: String,
    val mostUsedFileType: String,
    val mostRequestedPermission: String
)

data class UserStats(
    val activeUsers: Int,
    val parentUsers: Int,
    val childUsers: Int,
    val averageSessionTime: Int
)

data class FileStats(val totalFiles: Int, val averageSize: Double)
data class StorageAnalysis(val usedSpace: Double, val freeSpace: Double)
data class PermissionStats(val activePermissions: Int, val pendingRequests: Int)
data class DelegationTrends(val approvalRate: Double, val averageResponseTime: Int)
data class PerformanceMetrics(val responseTime: Int, val uptime: Double)
data class SystemHealthStats(val cpuUsage: Double, val memoryUsage: Double)
data class ActivityHeatmap(val peakHour: Int, val busiestDay: String)

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun calculateStatistics(
    families: Any,
    members: Any,
    delegations: Any,
    alerts: Any,
    period: StatisticsPeriod
): StatisticsData {
    // Mock implementation - en réalité, analyser les données réelles
    return StatisticsData(
        overview = OverviewStats(5, 12, 234, 18, 12.5, 8.3, 15.7, 5.2),
        trends = TrendsData(listOf(1.0, 2.0, 3.0), 12.5, 85, 76.3),
        topMetrics = TopMetrics("Famille Dupont", "Alice M.", "PDF", "Lecture"),
        userStats = UserStats(10, 4, 8, 45),
        fileStats = FileStats(234, 2.4),
        storageAnalysis = StorageAnalysis(2.4, 7.6),
        permissionStats = PermissionStats(18, 3),
        delegationTrends = DelegationTrends(78.5, 24),
        performanceMetrics = PerformanceMetrics(125, 99.8),
        systemHealth = SystemHealthStats(45.2, 62.1),
        activityHeatmap = ActivityHeatmap(14, "Mercredi")
    )
}