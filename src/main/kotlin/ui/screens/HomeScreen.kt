// ================================================================
// HOMESCREEN.KT - ÉCRAN D'ACCUEIL ARKA
// ================================================================

package ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import controllers.*
import ktorm.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.screens.families.FamilyStatistics
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
    var familyStats by remember { mutableStateOf<FamilyStatistics?>(null) }
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
                    if (statsResult is FamilyController.FamilyResult.Success) {
                        familyStats = statsResult.data
                    }

                    // Charger les fichiers récents
                    val filesResult = fileController.getRecentFiles(currentUser.membreFamilleId, 5)
                    if (filesResult is FileController.FileResult.Success) {
                        recentFiles = filesResult.data
                    }

                    // Charger les alertes en attente
                    val alertsResult = alertController.getUserAlerts()
                    if (alertsResult is AlertController.AlertResult.Success) {
                        pendingAlerts = alertsResult.data.take(3)
                    }

                    // Charger les membres de la famille (si admin/responsable)
                    if (currentUser.estAdmin || currentUser.estResponsable) {
                        val membersResult = familyMemberController.getFamilyMembers(currentUser.familleId)
                        if (membersResult is FamilyMemberController.FamilyMemberResult.Success) {
                            familyMembers = membersResult.data
                        }
                    }

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
            subtitle = "Bienvenue dans Arka",
            navigationIcon = null,
            actions = {
                // Badge des alertes
                if (pendingAlerts.isNotEmpty()) {
                    BadgedBox(
                        badge = {
                            Badge { Text("${pendingAlerts.size}") }
                        }
                    ) {
                        ArkaIconButton(
                            icon = Icons.Default.Notifications,
                            onClick = onNavigateToAlerts,
                            contentDescription = "Alertes"
                        )
                    }
                } else {
                    ArkaIconButton(
                        icon = Icons.Default.Notifications,
                        onClick = onNavigateToAlerts,
                        contentDescription = "Alertes"
                    )
                }

                ArkaIconButton(
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSettings,
                    contentDescription = "Paramètres"
                )

                ArkaIconButton(
                    icon = Icons.Default.ExitToApp,
                    onClick = onLogout,
                    contentDescription = "Déconnexion"
                )
            }
        )

        // Contenu principal
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "Chargement de vos données...",
                        style = ArkaTextStyles.cardDescription,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ErrorMessage(
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
        } else {
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
                        FamilyManagementSection(
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserAvatar(
                name = user?.prenomMembre ?: "Utilisateur",
                size = AvatarSizes.large,
                backgroundColor = MaterialTheme.colors.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bonjour, ${user?.prenomMembre ?: "Utilisateur"} !",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )

                Text(
                    text = when {
                        user?.estAdmin == true -> "Administrateur de la famille"
                        user?.estResponsable == true -> "Responsable de la famille"
                        else -> "Membre de la famille"
                    },
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Dernière connexion aujourd'hui",
                        style = ArkaTextStyles.metadata,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Section des statistiques rapides
 */
@Composable
private fun QuickStatsSection(
    familyStats: FamilyStatistics?,
    recentFilesCount: Int,
    alertsCount: Int,
    membersCount: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Vue d'ensemble",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                StatCard(
                    title = "Membres",
                    value = "${familyStats?.memberCount ?: membersCount}",
                    icon = Icons.Default.Group,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.width(140.dp)
                )
            }

            item {
                StatCard(
                    title = "Fichiers récents",
                    value = "$recentFilesCount",
                    icon = Icons.Default.Description,
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier.width(140.dp)
                )
            }

            item {
                StatCard(
                    title = "Alertes",
                    value = "$alertsCount",
                    icon = Icons.Default.Notifications,
                    color = if (alertsCount > 0) MaterialTheme.colors.error else MaterialTheme.colors.arka.success,
                    modifier = Modifier.width(140.dp)
                )
            }

            item {
                StatCard(
                    title = "Admins",
                    value = "${familyStats?.adminCount ?: 0}",
                    icon = Icons.Default.AdminPanelSettings,
                    color = MaterialTheme.colors.arka.warning,
                    modifier = Modifier.width(140.dp)
                )
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
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Actions rapides",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    text = "Mes fichiers",
                    icon = Icons.Default.Folder,
                    onClick = onNavigateToFiles,
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    text = "Famille",
                    icon = Icons.Default.Group,
                    onClick = onNavigateToFamily,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    text = "Partage",
                    icon = Icons.Default.Share,
                    onClick = onNavigateToPermissions,
                    modifier = Modifier.weight(1f)
                )

                QuickActionButton(
                    text = "Alertes",
                    icon = Icons.Default.Notifications,
                    onClick = onNavigateToAlerts,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Bouton d'action rapide
 */
@Composable
private fun QuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ArkaOutlinedButton(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier
    )
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
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(onClick = onSeeAll) {
                    Text(
                        text = "Voir tout",
                        style = ArkaTextStyles.link
                    )
                }
            }

            files.forEach { file ->
                FileItem(file = file)
            }
        }
    }
}

/**
 * Item de fichier dans la liste
 */
@Composable
private fun FileItem(file: Fichier) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = getFileIcon(file.typeFichier),
            contentDescription = null,
            tint = ArkaColorUtils.getFileTypeColor(file.typeFichier),
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.nomFichier,
                style = ArkaTextStyles.fileName,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = "Modifié le ${file.dateDerniereModifFichier?.toLocalDate() ?: ""}",
                style = ArkaTextStyles.metadata,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        Text(
            text = formatFileSize(file.tailleFichier),
            style = ArkaTextStyles.fileSize,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
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
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.arka.warning.copy(alpha = 0.1f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colors.arka.warning,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = "Alertes en attente",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.arka.warning
                    )
                }

                TextButton(onClick = onManageAlerts) {
                    Text(
                        text = "Gérer",
                        style = ArkaTextStyles.link,
                        color = MaterialTheme.colors.arka.warning
                    )
                }
            }

            alerts.forEach { alert ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = MaterialTheme.colors.arka.warning,
                        modifier = Modifier.size(8.dp)
                    )

                    Text(
                        text = alert.typeAlerte,
                        style = ArkaTextStyles.cardDescription,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Section de gestion familiale (pour admins)
 */
@Composable
private fun FamilyManagementSection(
    familyStats: FamilyStatistics?,
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
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = "Gestion familiale",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            familyStats?.let { stats ->
                Text(
                    text = "Famille: ${stats.familyName}",
                    style = ArkaTextStyles.cardTitle,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = "${stats.memberCount} membres · ${stats.adminCount} admin(s) · ${stats.responsibleCount} responsable(s)",
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArkaOutlinedButton(
                    text = "Gérer famille",
                    icon = Icons.Default.Group,
                    onClick = onManageFamily,
                    modifier = Modifier.weight(1f)
                )

                ArkaOutlinedButton(
                    text = "Permissions",
                    icon = Icons.Default.Security,
                    onClick = onManagePermissions,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Utilitaires
 */
private fun getFileIcon(fileType: String?): ImageVector {
    return when (fileType?.lowercase()) {
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx" -> Icons.Default.TableChart
        "ppt", "pptx" -> Icons.Default.Slideshow
        "jpg", "jpeg", "png", "gif", "bmp" -> Icons.Default.Image
        "mp4", "avi", "mov", "wmv" -> Icons.Default.VideoFile
        "mp3", "wav" -> Icons.Default.AudioFile
        "zip", "rar", "7z" -> Icons.Default.Archive
        "txt" -> Icons.Default.TextSnippet
        else -> Icons.Default.InsertDriveFile
    }
}

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