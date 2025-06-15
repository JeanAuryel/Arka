// ================================================================
// NOTIFICATIONSSCREEN.KT - CENTRE DE NOTIFICATIONS
// ================================================================

package ui.screens.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import controllers.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran de centre de notifications
 *
 * Fonctionnalités:
 * - Affichage de toutes les notifications
 * - Filtrage par type et statut
 * - Marquer comme lu/non lu
 * - Actions rapides sur les notifications
 * - Suppression et archivage
 */
@Composable
fun NotificationsScreen(
    alertController: AlertController,
    delegationController: DelegationRequestController,
    authController: AuthController,
    onNavigateBack: () -> Unit,
    onNavigateToPermissionRequest: (Int) -> Unit,
    onNavigateToSpace: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // États de filtrage
    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    var showOnlyUnread by remember { mutableStateOf(false) }

    // Charger les notifications
    LaunchedEffect(currentUser, selectedFilter, showOnlyUnread) {
        if (currentUser != null) {
            scope.launch {
                isLoading = true
                try {
                    val allNotifications = mutableListOf<NotificationItem>()

                    // Charger les alertes
                    val alertsResult = alertController.getAlertsByMember(currentUser.id)
                    if (alertsResult is AlertController.AlertResult.Success) {
                        allNotifications.addAll(
                            alertsResult.data.map { alert ->
                                NotificationItem(
                                    id = "alert_${alert.id}",
                                    type = NotificationType.ALERT,
                                    title = "Alerte système",
                                    message = alert.message,
                                    timestamp = alert.dateHeure,
                                    isRead = alert.estLue,
                                    priority = when (alert.type) {
                                        TypeAlerte.CRITIQUE -> NotificationPriority.HIGH
                                        TypeAlerte.AVERTISSEMENT -> NotificationPriority.MEDIUM
                                        TypeAlerte.INFO -> NotificationPriority.LOW
                                    },
                                    relatedEntityId = alert.id,
                                    actionable = true
                                )
                            }
                        )
                    }

                    // Charger les demandes de délégation (pour les parents)
                    if (currentUser.role == RoleFamille.PARENT) {
                        val delegationResult = delegationController.getPendingDelegationRequests()
                        if (delegationResult is DelegationRequestController.DelegationResult.Success) {
                            allNotifications.addAll(
                                delegationResult.data.map { request ->
                                    NotificationItem(
                                        id = "delegation_${request.id}",
                                        type = NotificationType.PERMISSION_REQUEST,
                                        title = "Demande de permission",
                                        message = "Nouvelle demande de permission en attente",
                                        timestamp = request.dateCreation,
                                        isRead = false, // Les nouvelles demandes sont non lues
                                        priority = NotificationPriority.MEDIUM,
                                        relatedEntityId = request.id,
                                        actionable = true
                                    )
                                }
                            )
                        }
                    }

                    // Filtrer les notifications
                    notifications = allNotifications
                        .filter { notification ->
                            when (selectedFilter) {
                                NotificationFilter.ALL -> true
                                NotificationFilter.ALERTS -> notification.type == NotificationType.ALERT
                                NotificationFilter.PERMISSIONS -> notification.type == NotificationType.PERMISSION_REQUEST
                                NotificationFilter.FILES -> notification.type == NotificationType.FILE_SHARED
                                NotificationFilter.SYSTEM -> notification.type == NotificationType.SYSTEM
                            }
                        }
                        .filter { notification ->
                            if (showOnlyUnread) !notification.isRead else true
                        }
                        .sortedByDescending { it.timestamp }

                } catch (e: Exception) {
                    errorMessage = "Erreur lors du chargement: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête avec actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Notifications",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Badge avec nombre de notifications non lues
            val unreadCount = notifications.count { !it.isRead }
            if (unreadCount > 0) {
                Badge(
                    backgroundColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Text(
                        text = unreadCount.toString(),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Menu d'actions
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            // Marquer toutes comme lues
                            scope.launch {
                                notifications.forEach { notification ->
                                    if (!notification.isRead) {
                                        markNotificationAsRead(
                                            notification,
                                            alertController,
                                            delegationController
                                        )
                                    }
                                }
                                // Recharger les notifications
                                notifications = notifications.map { it.copy(isRead = true) }
                            }
                            showMenu = false
                        }
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Tout marquer comme lu")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tout marquer comme lu")
                    }

                    DropdownMenuItem(
                        onClick = {
                            // Supprimer les notifications lues
                            notifications = notifications.filter { !it.isRead }
                            showMenu = false
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer les lues")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Supprimer les lues")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filtres
        NotificationFilters(
            selectedFilter = selectedFilter,
            showOnlyUnread = showOnlyUnread,
            onFilterChange = { selectedFilter = it },
            onToggleUnread = { showOnlyUnread = it },
            notificationCounts = notifications.groupBy { it.type }.mapValues { it.value.size }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            EmptyNotificationsState(selectedFilter = selectedFilter)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onMarkAsRead = {
                            scope.launch {
                                markNotificationAsRead(notification, alertController, delegationController)
                                notifications = notifications.map {
                                    if (it.id == notification.id) it.copy(isRead = true) else it
                                }
                            }
                        },
                        onAction = { action ->
                            when (action) {
                                NotificationAction.VIEW_DETAILS -> {
                                    when (notification.type) {
                                        NotificationType.PERMISSION_REQUEST -> {
                                            onNavigateToPermissionRequest(notification.relatedEntityId)
                                        }
                                        NotificationType.FILE_SHARED -> {
                                            onNavigateToSpace(notification.relatedEntityId)
                                        }
                                        else -> { /* Autres actions */ }
                                    }
                                }
                                NotificationAction.APPROVE -> {
                                    if (notification.type == NotificationType.PERMISSION_REQUEST) {
                                        scope.launch {
                                            val result = delegationController.approveDelegationRequest(
                                                notification.relatedEntityId,
                                                currentUser?.id ?: 0
                                            )
                                            if (result is DelegationRequestController.DelegationResult.Success) {
                                                notifications = notifications.filter { it.id != notification.id }
                                            }
                                        }
                                    }
                                }
                                NotificationAction.REJECT -> {
                                    if (notification.type == NotificationType.PERMISSION_REQUEST) {
                                        scope.launch {
                                            val result = delegationController.rejectDelegationRequest(
                                                notification.relatedEntityId,
                                                currentUser?.id ?: 0
                                            )
                                            if (result is DelegationRequestController.DelegationResult.Success) {
                                                notifications = notifications.filter { it.id != notification.id }
                                            }
                                        }
                                    }
                                }
                                NotificationAction.DELETE -> {
                                    notifications = notifications.filter { it.id != notification.id }
                                }
                            }
                        }
                    )
                }
            }
        }

        // Gestion des erreurs
        errorMessage?.let { error ->
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(5000)
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
 * Filtres de notifications
 */
@Composable
private fun NotificationFilters(
    selectedFilter: NotificationFilter,
    showOnlyUnread: Boolean,
    onFilterChange: (NotificationFilter) -> Unit,
    onToggleUnread: (Boolean) -> Unit,
    notificationCounts: Map<NotificationType, Int>
) {
    Column {
        // Filtres par type
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(NotificationFilter.values()) { filter ->
                _root_ide_package_.ui.screens.FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(filter.label)
                            // Afficher le nombre si disponible
                            val count = when (filter) {
                                NotificationFilter.ALL -> notificationCounts.values.sum()
                                NotificationFilter.ALERTS -> notificationCounts[NotificationType.ALERT] ?: 0
                                NotificationFilter.PERMISSIONS -> notificationCounts[NotificationType.PERMISSION_REQUEST]
                                    ?: 0

                                NotificationFilter.FILES -> notificationCounts[NotificationType.FILE_SHARED] ?: 0
                                NotificationFilter.SYSTEM -> notificationCounts[NotificationType.SYSTEM] ?: 0
                            }
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($count)",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Toggle non lues uniquement
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showOnlyUnread,
                onCheckedChange = onToggleUnread
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Afficher uniquement les non lues",
                style = MaterialTheme.typography.body2
            )
        }
    }
}

/**
 * Carte de notification
 */
@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onMarkAsRead: () -> Unit,
    onAction: (NotificationAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = if (notification.isRead) 2.dp else 6.dp,
        backgroundColor = if (notification.isRead) {
            MaterialTheme.colors.surface
        } else {
            MaterialTheme.colors.primary.copy(alpha = 0.05f)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icône de type
                Icon(
                    imageVector = getNotificationIcon(notification.type),
                    contentDescription = "Type",
                    tint = getNotificationColor(notification.type),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Titre avec badge de priorité
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.body1,
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                        )

                        if (notification.priority == NotificationPriority.HIGH) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.PriorityHigh,
                                contentDescription = "Priorité élevée",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Message
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(
                            alpha = if (notification.isRead) 0.6f else 0.8f
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Timestamp
                    Text(
                        text = formatTimestamp(notification.timestamp),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Indicateur non lu
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    Color.Blue,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }
            }

            // Actions
            if (notification.actionable) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Actions spécifiques au type
                    when (notification.type) {
                        NotificationType.PERMISSION_REQUEST -> {
                            OutlinedButton(
                                onClick = { onAction(NotificationAction.REJECT) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Red
                                )
                            ) {
                                Text("Refuser")
                            }

                            Button(
                                onClick = { onAction(NotificationAction.APPROVE) }
                            ) {
                                Text("Approuver")
                            }
                        }
                        else -> {
                            TextButton(
                                onClick = { onAction(NotificationAction.VIEW_DETAILS) }
                            ) {
                                Text("Voir détails")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Actions communes
                    if (!notification.isRead) {
                        TextButton(onClick = onMarkAsRead) {
                            Text("Marquer comme lu")
                        }
                    }

                    IconButton(
                        onClick = { onAction(NotificationAction.DELETE) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * État vide pour les notifications
 */
@Composable
private fun EmptyNotificationsState(
    selectedFilter: NotificationFilter
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "Aucune notification",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (selectedFilter) {
                    NotificationFilter.ALL -> "Aucune notification"
                    NotificationFilter.ALERTS -> "Aucune alerte"
                    NotificationFilter.PERMISSIONS -> "Aucune demande de permission"
                    NotificationFilter.FILES -> "Aucun fichier partagé"
                    NotificationFilter.SYSTEM -> "Aucune notification système"
                },
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = "Vous êtes à jour !",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ================================================================
// CLASSES ET ENUMS DE DONNÉES
// ================================================================

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: LocalDateTime,
    val isRead: Boolean = false,
    val priority: NotificationPriority = NotificationPriority.MEDIUM,
    val relatedEntityId: Int,
    val actionable: Boolean = false
)

enum class NotificationType {
    ALERT,
    PERMISSION_REQUEST,
    FILE_SHARED,
    SYSTEM
}

enum class NotificationPriority {
    LOW, MEDIUM, HIGH
}

enum class NotificationFilter(val label: String) {
    ALL("Toutes"),
    ALERTS("Alertes"),
    PERMISSIONS("Permissions"),
    FILES("Fichiers"),
    SYSTEM("Système")
}

enum class NotificationAction {
    VIEW_DETAILS,
    APPROVE,
    REJECT,
    DELETE
}

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun getNotificationIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.ALERT -> Icons.Default.Warning
    NotificationType.PERMISSION_REQUEST -> Icons.Default.Security
    NotificationType.FILE_SHARED -> Icons.Default.FilePresent
    NotificationType.SYSTEM -> Icons.Default.Info
}

private fun getNotificationColor(type: NotificationType): Color = when (type) {
    NotificationType.ALERT -> Color.Orange
    NotificationType.PERMISSION_REQUEST -> Color.Blue
    NotificationType.FILE_SHARED -> Color.Green
    NotificationType.SYSTEM -> Color.Gray
}

private fun formatTimestamp(timestamp: LocalDateTime): String {
    val now = LocalDateTime.now()
    val diff = java.time.Duration.between(timestamp, now)

    return when {
        diff.toDays() > 0 -> "${diff.toDays()}j"
        diff.toHours() > 0 -> "${diff.toHours()}h"
        diff.toMinutes() > 0 -> "${diff.toMinutes()}min"
        else -> "maintenant"
    }
}

private suspend fun markNotificationAsRead(
    notification: NotificationItem,
    alertController: AlertController,
    delegationController: DelegationRequestController
) {
    when (notification.type) {
        NotificationType.ALERT -> {
            alertController.markAlertAsRead(notification.relatedEntityId)
        }
        NotificationType.PERMISSION_REQUEST -> {
            // Les demandes de délégation sont marquées comme "vues" différemment
            // Cela peut être géré dans le contrôleur selon les besoins
        }
        else -> {
            // Autres types de notifications
        }
    }
}