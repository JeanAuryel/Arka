// ================================================================
// ADMINPANELSCREEN.KT - PANEL D'ADMINISTRATION
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import controllers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*
import java.time.Duration
import java.time.LocalDateTime

/**
 * Panneau d'administration principal
 *
 * Fonctionnalités:
 * - Vue d'ensemble du système
 * - Gestion des utilisateurs et familles
 * - Monitoring des performances
 * - Actions administratives
 * - Accès rapide aux outils d'administration
 */
@Composable
fun AdminPanelScreen(
    authController: AuthController,
    familyController: FamilyController,
    familyMemberController: FamilyMemberController,
    fileController: FileController,
    delegationController: DelegationRequestController,
    alertController: AlertController,
    onNavigateBack: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAudit: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // Vérification des droits d'accès
    if (currentUser?.role != RoleFamille.PARENT) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                elevation = 8.dp,
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Accès refusé",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Accès refusé",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Seuls les parents peuvent accéder au panneau d'administration",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Retour")
                    }
                }
            }
        }
        return
    }

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var systemOverview by remember { mutableStateOf<SystemOverview?>(null) }
    var recentActivity by remember { mutableStateOf<List<AdminActivity>>(emptyList()) }
    var pendingActions by remember { mutableStateOf<List<PendingAction>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Charger les données d'administration
    LaunchedEffect(currentUser) {
        scope.launch {
            isLoading = true
            try {
                // Charger les statistiques système
                val familiesResult = familyController.getAllFamilies()
                val membersResult = familyMemberController.getAllMembers()
                val delegationsResult = delegationController.getAllDelegationRequests()
                val alertsResult = alertController.getAllAlerts()

                systemOverview = SystemOverview(
                    totalFamilies = if (familiesResult is FamilyController.FamilyResult.Success) familiesResult.data.size else 0,
                    totalMembers = if (membersResult is FamilyMemberController.FamilyMemberResult.Success) membersResult.data.size else 0,
                    activeAlerts = if (alertsResult is AlertController.AlertResult.Success) alertsResult.data.filter { !it.estLue }.size else 0,
                    pendingDelegations = if (delegationsResult is DelegationRequestController.DelegationResult.Success) delegationsResult.data.filter { it.statut == StatutDemande.EN_ATTENTE }.size else 0,
                    systemHealth = 95.0, // Mock data
                    storageUsed = 2.4, // GB - Mock data
                    storageTotal = 10.0 // GB - Mock data
                )

                // Charger l'activité récente
                recentActivity = listOf(
                    AdminActivity(
                        id = 1,
                        type = ActivityType.USER_CREATED,
                        description = "Nouveau membre ajouté: Marie Dupont",
                        timestamp = LocalDateTime.now().minusHours(2),
                        severity = ActivitySeverity.INFO
                    ),
                    AdminActivity(
                        id = 2,
                        type = ActivityType.PERMISSION_GRANTED,
                        description = "Permission accordée pour l'espace Documents",
                        timestamp = LocalDateTime.now().minusHours(4),
                        severity = ActivitySeverity.INFO
                    ),
                    AdminActivity(
                        id = 3,
                        type = ActivityType.ALERT_TRIGGERED,
                        description = "Alerte de sécurité: tentative d'accès non autorisé",
                        timestamp = LocalDateTime.now().minusHours(6),
                        severity = ActivitySeverity.WARNING
                    )
                )

                // Charger les actions en attente
                pendingActions = listOf(
                    PendingAction(
                        id = 1,
                        type = ActionType.DELEGATION_APPROVAL,
                        title = "Demandes de permissions",
                        description = "3 demandes en attente d'approbation",
                        priority = ActionPriority.MEDIUM,
                        count = 3
                    ),
                    PendingAction(
                        id = 2,
                        type = ActionType.ALERT_REVIEW,
                        title = "Alertes non résolues",
                        description = "2 alertes critiques nécessitent votre attention",
                        priority = ActionPriority.HIGH,
                        count = 2
                    )
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
        // En-tête avec navigation et titre
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Administration",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Indicateur de santé du système
            systemOverview?.let { overview ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            overview.systemHealth >= 90 -> Icons.Default.CheckCircle
                            overview.systemHealth >= 70 -> Icons.Default.Warning
                            else -> Icons.Default.Error
                        },
                        contentDescription = "Santé du système",
                        tint = when {
                            overview.systemHealth >= 90 -> Color.Green
                            overview.systemHealth >= 70 -> Color.Orange
                            else -> Color.Red
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${overview.systemHealth.toInt()}%",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vue d'ensemble du système
                item {
                    systemOverview?.let { overview ->
                        SystemOverviewSection(overview = overview)
                    }
                }

                // Actions rapides d'administration
                item {
                    QuickActionsSection(
                        onNavigateToUsers = onNavigateToUsers,
                        onNavigateToStatistics = onNavigateToStatistics,
                        onNavigateToAudit = onNavigateToAudit,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }

                // Actions en attente
                item {
                    PendingActionsSection(
                        actions = pendingActions,
                        onActionClick = { action ->
                            when (action.type) {
                                ActionType.DELEGATION_APPROVAL -> onNavigateToUsers()
                                ActionType.ALERT_REVIEW -> onNavigateToAudit()
                                ActionType.SYSTEM_MAINTENANCE -> onNavigateToSettings()
                            }
                        }
                    )
                }

                // Activité récente
                item {
                    RecentActivitySection(activities = recentActivity)
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
 * Section de vue d'ensemble du système
 */
@Composable
private fun SystemOverviewSection(
    overview: SystemOverview
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
                Icon(Icons.Default.Dashboard, contentDescription = "Vue d'ensemble")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vue d'ensemble du système",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Métriques principales en grille
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricCard(
                    title = "Familles",
                    value = overview.totalFamilies.toString(),
                    icon = Icons.Default.FamilyRestroom,
                    color = Color.Blue
                )

                MetricCard(
                    title = "Membres",
                    value = overview.totalMembers.toString(),
                    icon = Icons.Default.People,
                    color = Color.Green
                )

                MetricCard(
                    title = "Alertes",
                    value = overview.activeAlerts.toString(),
                    icon = Icons.Default.Warning,
                    color = if (overview.activeAlerts > 0) Color.Orange else Color.Green
                )

                MetricCard(
                    title = "Demandes",
                    value = overview.pendingDelegations.toString(),
                    icon = Icons.Default.Schedule,
                    color = if (overview.pendingDelegations > 0) Color.Orange else Color.Green
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Utilisation du stockage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Storage, contentDescription = "Stockage")
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Stockage utilisé",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    LinearProgressIndicator(
                        progress = (overview.storageUsed / overview.storageTotal).toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            overview.storageUsed / overview.storageTotal > 0.9 -> Color.Red
                            overview.storageUsed / overview.storageTotal > 0.7 -> Color.Orange
                            else -> Color.Green
                        }
                    )
                    Text(
                        text = "${overview.storageUsed} GB / ${overview.storageTotal} GB",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}

/**
 * Carte de métrique individuelle
 */
@Composable
private fun MetricCard(
    title: String,
    value: String,
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
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Section des actions rapides
 */
@Composable
private fun QuickActionsSection(
    onNavigateToUsers: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAudit: () -> Unit,
    onNavigateToSettings: () -> Unit
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
                Icon(Icons.Default.FlashOn, contentDescription = "Actions rapides")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Actions rapides",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    listOf(
                        QuickAction("Utilisateurs", Icons.Default.People, onNavigateToUsers),
                        QuickAction("Statistiques", Icons.Default.Analytics, onNavigateToStatistics),
                        QuickAction("Audit", Icons.Default.History, onNavigateToAudit),
                        QuickAction("Paramètres", Icons.Default.Settings, onNavigateToSettings)
                    )
                ) { action ->
                    QuickActionCard(action = action)
                }
            }
        }
    }
}

/**
 * Carte d'action rapide
 */
@Composable
private fun QuickActionCard(
    action: QuickAction
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(100.dp),
        elevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = action.onClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colors.primary
                )
            }
            Text(
                text = action.title,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Section des actions en attente
 */
@Composable
private fun PendingActionsSection(
    actions: List<PendingAction>,
    onActionClick: (PendingAction) -> Unit
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
                Icon(Icons.Default.Schedule, contentDescription = "Actions en attente")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Actions en attente",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )

                if (actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        backgroundColor = Color.Red,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = actions.size.toString(),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (actions.isEmpty()) {
                Text(
                    text = "Aucune action en attente",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            } else {
                actions.forEach { action ->
                    PendingActionItem(
                        action = action,
                        onClick = { onActionClick(action) }
                    )

                    if (action != actions.last()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Item d'action en attente
 */
@Composable
private fun PendingActionItem(
    action: PendingAction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (action.priority) {
                ActionPriority.HIGH -> Icons.Default.PriorityHigh
                ActionPriority.MEDIUM -> Icons.Default.Schedule
                ActionPriority.LOW -> Icons.Default.Info
            },
            contentDescription = "Priorité",
            tint = when (action.priority) {
                ActionPriority.HIGH -> Color.Red
                ActionPriority.MEDIUM -> Color.Orange
                ActionPriority.LOW -> Color.Gray
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = action.title,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = action.description,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }

        if (action.count > 0) {
            Badge(
                backgroundColor = when (action.priority) {
                    ActionPriority.HIGH -> Color.Red
                    ActionPriority.MEDIUM -> Color.Orange
                    ActionPriority.LOW -> Color.Gray
                },
                contentColor = Color.White
            ) {
                Text(
                    text = action.count.toString(),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Accéder"
            )
        }
    }
}

/**
 * Section de l'activité récente
 */
@Composable
private fun RecentActivitySection(
    activities: List<AdminActivity>
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
                Icon(Icons.Default.History, contentDescription = "Activité récente")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Activité récente",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activities.isEmpty()) {
                Text(
                    text = "Aucune activité récente",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            } else {
                activities.forEach { activity ->
                    ActivityItem(activity = activity)

                    if (activity != activities.last()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Item d'activité
 */
@Composable
private fun ActivityItem(
    activity: AdminActivity
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (activity.type) {
                ActivityType.USER_CREATED -> Icons.Default.PersonAdd
                ActivityType.PERMISSION_GRANTED -> Icons.Default.Security
                ActivityType.ALERT_TRIGGERED -> Icons.Default.Warning
                ActivityType.FILE_UPLOADED -> Icons.Default.Upload
                ActivityType.SYSTEM_UPDATE -> Icons.Default.SystemUpdate
            },
            contentDescription = "Type d'activité",
            tint = when (activity.severity) {
                ActivitySeverity.INFO -> Color.Blue
                ActivitySeverity.WARNING -> Color.Orange
                ActivitySeverity.ERROR -> Color.Red
            },
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activity.description,
                style = MaterialTheme.typography.body2
            )
            Text(
                text = formatTimestamp(activity.timestamp),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ================================================================
// CLASSES ET ENUMS DE DONNÉES
// ================================================================

data class SystemOverview(
    val totalFamilies: Int,
    val totalMembers: Int,
    val activeAlerts: Int,
    val pendingDelegations: Int,
    val systemHealth: Double,
    val storageUsed: Double,
    val storageTotal: Double
)

data class AdminActivity(
    val id: Int,
    val type: ActivityType,
    val description: String,
    val timestamp: LocalDateTime,
    val severity: ActivitySeverity
)

data class PendingAction(
    val id: Int,
    val type: ActionType,
    val title: String,
    val description: String,
    val priority: ActionPriority,
    val count: Int = 0
)

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

enum class ActivityType {
    USER_CREATED,
    PERMISSION_GRANTED,
    ALERT_TRIGGERED,
    FILE_UPLOADED,
    SYSTEM_UPDATE
}

enum class ActivitySeverity {
    INFO, WARNING, ERROR
}

enum class ActionType {
    DELEGATION_APPROVAL,
    ALERT_REVIEW,
    SYSTEM_MAINTENANCE
}

enum class ActionPriority {
    LOW, MEDIUM, HIGH
}

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun formatTimestamp(timestamp: LocalDateTime): String {
    val now = LocalDateTime.now()
    val diff = Duration.between(timestamp, now)

    return when {
        diff.toDays() > 0 -> "Il y a ${diff.toDays()}j"
        diff.toHours() > 0 -> "Il y a ${diff.toHours()}h"
        diff.toMinutes() > 0 -> "Il y a ${diff.toMinutes()}min"
        else -> "À l'instant"
    }
}