// ================================================================
// HOMESCREEN.KT - ÉCRAN D'ACCUEIL ARKA (CORRIGÉ)
// ================================================================

package ui.screens

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
import controllers.*
import ktorm.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*

/**
 * Écran d'accueil principal d'Arka
 *
 * Fonctionnalités:
 * - Vue d'ensemble des statistiques familiales
 * - Accès rapide aux fonctionnalités principales
 * - Fichiers récents
 * - Alertes en attente
 * - Actions rapides contextuelles
 */
@Composable
fun HomeScreen(
    authController: AuthController,
    familyController: FamilyController,
    familyMemberController: FamilyMemberController,
    fileController: FileController,
    alertController: AlertController,
    onNavigateToFiles: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var familyStats by remember { mutableStateOf<controllers.FamilyStatistics?>(null) }
    var recentFiles by remember { mutableStateOf<List<Fichier>>(emptyList()) }
    var pendingAlerts by remember { mutableStateOf<List<Alerte>>(emptyList()) }
    var familyMembers by remember { mutableStateOf<List<MembreFamille>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Charger les données du dashboard
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            scope.launch {
                isLoading = true
                try {
                    // Charger les statistiques de la famille
                    val statsResult = familyController.getFamilyStatistics(currentUser.familleId)
                    if (statsResult is FamilyController.FamilyResult.Success<*>) {
                        familyStats = statsResult.data as? controllers.FamilyStatistics
                    }

                    // Charger les membres de la famille
                    val membersResult = familyMemberController.getFamilyMembers(currentUser.familleId)
                    if (membersResult is FamilyMemberController.FamilyMemberResult.Success<*>) {
                        familyMembers = membersResult.data as? List<MembreFamille> ?: emptyList()
                    }

                    // Charger les fichiers récents (simulé pour l'instant)
                    recentFiles = emptyList() // À implémenter avec fileController

                    // Charger les alertes en attente (simulé pour l'instant)
                    pendingAlerts = emptyList() // À implémenter avec alertController

                } catch (e: Exception) {
                    errorMessage = "Erreur lors du chargement: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barre de navigation supérieure
        ArkaTopAppBar(
            title = "Accueil",
            actions = {
                ArkaIconButton(
                    icon = Icons.Default.Notifications,
                    onClick = onNavigateToAlerts,
                    contentDescription = "Notifications"
                )
                ArkaIconButton(
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSettings,
                    contentDescription = "Paramètres"
                )
            }
        )

        // Gestion des états
        when {
            isLoading -> {
                HomeLoadingState()
            }
            errorMessage != null -> {
                HomeErrorState(
                    message = errorMessage!!,
                    onRetry = {
                        errorMessage = null
                        // Relancer le chargement
                        scope.launch {
                            // Code de rechargement ici
                        }
                    }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Message de bienvenue
                    item {
                        WelcomeCard(currentUser)
                    }

                    // Statistiques rapides
                    item {
                        QuickStatsSection(
                            familyStats = familyStats,
                            recentFilesCount = recentFiles.size,
                            alertsCount = pendingAlerts.size,
                            membersCount = familyMembers.size
                        )
                    }

                    // Actions rapides
                    item {
                        QuickActionsSection(
                            onNavigateToFiles = onNavigateToFiles,
                            onNavigateToFamily = onNavigateToFamily,
                            onNavigateToPermissions = onNavigateToPermissions,
                            onNavigateToAlerts = onNavigateToAlerts
                        )
                    }

                    // Fichiers récents
                    if (recentFiles.isNotEmpty()) {
                        item {
                            RecentFilesSection(
                                files = recentFiles,
                                onSeeAll = onNavigateToFiles
                            )
                        }
                    }

                    // Alertes en attente
                    if (pendingAlerts.isNotEmpty()) {
                        item {
                            PendingAlertsSection(
                                alerts = pendingAlerts,
                                onManageAlerts = onNavigateToAlerts
                            )
                        }
                    }

                    // Gestion familiale (pour admins/responsables)
                    if ((currentUser?.estAdmin == true || currentUser?.estResponsable == true) && familyMembers.isNotEmpty()) {
                        item {
                            HomeFamilyManagementSection(
                                familyStats = familyStats,
                                familyMembers = familyMembers,
                                onManageFamily = onNavigateToFamily,
                                onManagePermissions = onNavigateToPermissions
                            )
                        }
                    }

                    // Espace pour éviter que le FAB cache le contenu
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

/**
 * État de chargement pour la page d'accueil
 */
@Composable
private fun HomeLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colors.primary
            )
            Text(
                text = "Chargement du tableau de bord...",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * État d'erreur pour la page d'accueil
 */
@Composable
private fun HomeErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.error
            )

            Text(
                text = "Erreur de chargement",
                style = MaterialTheme.typography.h6
            )

            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            ArkaButton(
                text = "Réessayer",
                onClick = onRetry,
                icon = Icons.Default.Refresh
            )
        }
    }
}

/**
 * Carte de bienvenue personnalisée
 */
@Composable
private fun WelcomeCard(user: MembreFamille?) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.primary
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Bonjour ${user?.prenomMembre ?: ""}!",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )

                Text(
                    text = "Bienvenue dans votre espace familial Arka",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Section des statistiques rapides
 */
@Composable
private fun QuickStatsSection(
    familyStats: controllers.FamilyStatistics?,
    recentFilesCount: Int,
    alertsCount: Int,
    membersCount: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Vue d'ensemble",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatCard(
                    title = "Membres",
                    value = "${familyStats?.memberCount ?: membersCount}",
                    icon = Icons.Default.People,
                    color = MaterialTheme.colors.primary
                )
            }

            item {
                StatCard(
                    title = "Fichiers récents",
                    value = "$recentFilesCount",
                    icon = Icons.Default.InsertDriveFile,
                    color = MaterialTheme.colors.secondary
                )
            }

            item {
                StatCard(
                    title = "Alertes",
                    value = "$alertsCount",
                    icon = Icons.Default.Notifications,
                    color = if (alertsCount > 0) Color(0xFFFF9800) else MaterialTheme.colors.primary
                )
            }

            familyStats?.let { stats ->
                item {
                    StatCard(
                        title = "Administrateurs",
                        value = "${stats.adminCount}",
                        icon = Icons.Default.AdminPanelSettings,
                        color = MaterialTheme.colors.primary
                    )
                }
            }
        }
    }
}

/**
 * Section des actions rapides
 */
@Composable
private fun QuickActionsSection(
    onNavigateToFiles: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAlerts: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Actions rapides",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuickActionCard(
                    title = "Mes fichiers",
                    icon = Icons.Default.Folder,
                    onClick = onNavigateToFiles
                )
            }

            item {
                QuickActionCard(
                    title = "Ma famille",
                    icon = Icons.Default.Group,
                    onClick = onNavigateToFamily
                )
            }

            item {
                QuickActionCard(
                    title = "Permissions",
                    icon = Icons.Default.Security,
                    onClick = onNavigateToPermissions
                )
            }

            item {
                QuickActionCard(
                    title = "Alertes",
                    icon = Icons.Default.NotificationsActive,
                    onClick = onNavigateToAlerts
                )
            }
        }
    }
}

/**
 * Carte d'action rapide
 */
@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.width(120.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colors.primary
            )

            Text(
                text = title,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Section des fichiers récents
 */
@Composable
private fun RecentFilesSection(
    files: List<Fichier>,
    onSeeAll: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fichiers récents",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onSeeAll) {
                    Text(text = "Voir tout")
                }
            }

            files.take(3).forEach { file ->
                FileItem(file = file)
            }
        }
    }
}

/**
 * Élément de fichier
 */
@Composable
private fun FileItem(file: Fichier) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colors.primary
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = file.nomFichier,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = file.typeFichier ?: "Fichier",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }

        Text(
            text = formatFileSize(file.tailleFichier),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Section des alertes en attente
 */
@Composable
private fun PendingAlertsSection(
    alerts: List<Alerte>,
    onManageAlerts: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alertes en attente (${alerts.size})",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )

                TextButton(onClick = onManageAlerts) {
                    Text(
                        text = "Gérer",
                        color = Color(0xFFFF9800)
                    )
                }
            }

            alerts.take(3).forEach { alert ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(8.dp)
                    )

                    Text(
                        text = alert.typeAlerte,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Section de gestion familiale (pour admins) - renommée pour éviter les conflits
 */
@Composable
private fun HomeFamilyManagementSection(
    familyStats: controllers.FamilyStatistics?,
    familyMembers: List<MembreFamille>,
    onManageFamily: () -> Unit,
    onManagePermissions: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.primary
                )

                Text(
                    text = "Gestion familiale",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Vous avez des privilèges d'administration",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArkaOutlinedButton(
                    text = "Gérer la famille",
                    onClick = onManageFamily,
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f)
                )

                ArkaOutlinedButton(
                    text = "Permissions",
                    onClick = onManagePermissions,
                    icon = Icons.Default.Security,
                    modifier = Modifier.weight(1f)
                )
            }

            // Statistiques rapides famille
            familyStats?.let { stats ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickFamilyStat(
                        label = "Membres",
                        value = "${stats.memberCount}",
                        modifier = Modifier.weight(1f)
                    )
                    QuickFamilyStat(
                        label = "Admins",
                        value = "${stats.adminCount}",
                        modifier = Modifier.weight(1f)
                    )
                    QuickFamilyStat(
                        label = "Responsables",
                        value = "${stats.responsibleCount}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Statistique rapide famille
 */
@Composable
private fun QuickFamilyStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
    }
}

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

/**
 * Formate la taille d'un fichier
 */
private fun formatFileSize(size: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var fileSize = size.toDouble()
    var unitIndex = 0

    while (fileSize >= 1024 && unitIndex < units.size - 1) {
        fileSize /= 1024
        unitIndex++
    }

    return "%.1f %s".format(fileSize, units[unitIndex])
}
