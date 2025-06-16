// ================================================================
// FAMILYTREESCREEN.KT - ÉCRAN DE GESTION FAMILLE ARKA (CORRIGÉ)
// ================================================================

package ui.screens.families

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import ktorm.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran de gestion de la famille Arka
 *
 * Fonctionnalités:
 * - Vue d'ensemble des membres de la famille
 * - Gestion des rôles et permissions
 * - Ajout/suppression de membres
 * - Statistiques familiales
 * - Historique des activités
 * - Configuration des espaces familiaux
 */
@Composable
fun FamilyTreeScreen(
    familyController: FamilyController,
    familyMemberController: FamilyMemberController,
    permissionController: PermissionController,
    authController: AuthController,
    onNavigateToMemberDetails: (Int) -> Unit,
    onNavigateToPermissions: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var familyInfo by remember { mutableStateOf<Famille?>(null) }
    var familyMembers by remember { mutableStateOf<List<MembreFamille>>(emptyList()) }
    var familyStats by remember { mutableStateOf<controllers.FamilyStatistics?>(null) }
    var recentActivity by remember { mutableStateOf<List<FamilyActivity>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showMemberActionsDialog by remember { mutableStateOf<MembreFamille?>(null) }

    // Charger les données de la famille
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            scope.launch {
                isLoading = true
                try {
                    // Charger les informations de la famille (adapter selon les méthodes disponibles)
                    // Note: getFamilyInfo n'existe pas, on utilise les données du currentUser
                    // Pour l'instant, on crée un objet Famille basique
                    familyInfo = Famille(
                        familleId = currentUser.familleId,
                        nomFamille = "Ma famille", // À adapter selon vos besoins
                        dateCreationFamille = LocalDateTime.now()
                    )

                    // Charger les membres
                    val membersResult = familyMemberController.getFamilyMembers(currentUser.familleId)
                    if (membersResult is FamilyMemberController.FamilyMemberResult.Success) {
                        familyMembers = membersResult.data
                    }

                    // Charger les statistiques
                    val statsResult = familyController.getFamilyStatistics(currentUser.familleId)
                    if (statsResult is FamilyController.FamilyResult.Success) {
                        familyStats = statsResult.data
                    }

                    // Pour l'activité récente, on simule car getRecentActivity n'existe pas
                    recentActivity = emptyList() // À implémenter selon vos besoins

                } catch (e: Exception) {
                    errorMessage = "Erreur lors du chargement: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val tabs = listOf(
        "Membres" to Icons.Default.Group,
        "Statistiques" to Icons.Default.Analytics,
        "Activité" to Icons.Default.History,
        "Configuration" to Icons.Default.Settings
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Barre de navigation supérieure
        ArkaTopAppBar(
            title = familyInfo?.nomFamille ?: "Ma famille",
            onNavigationClick = onBack,
            actions = {
                ArkaIconButton(
                    icon = Icons.Default.PersonAdd,
                    onClick = { showInviteDialog = true },
                    contentDescription = "Inviter un membre"
                )
                ArkaIconButton(
                    icon = Icons.Default.Security,
                    onClick = onNavigateToPermissions,
                    contentDescription = "Permissions"
                )
            }
        )

        // Gestion des états de chargement et d'erreur
        when {
            isLoading -> {
                LoadingState()
            }
            errorMessage != null -> {
                ErrorState(
                    message = errorMessage!!,
                    onRetry = {
                        errorMessage = null
                        // Relancer le chargement
                    }
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Informations de la famille
                    FamilyInfoCard(
                        familyInfo = familyInfo,
                        familyStats = familyStats,
                        currentUserRole = when {
                            currentUser?.estAdmin == true -> "Admin"
                            currentUser?.estResponsable == true -> "Responsable"
                            else -> "Membre"
                        }
                    )

                    // Onglets
                    TabRow(
                        selectedTabIndex = selectedTab,
                        backgroundColor = MaterialTheme.colors.surface
                    ) {
                        tabs.forEachIndexed { index, (title, icon) ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) },
                                icon = { Icon(icon, contentDescription = title) }
                            )
                        }
                    }

                    // Contenu des onglets
                    when (selectedTab) {
                        0 -> FamilyMembersTab(
                            members = familyMembers,
                            currentUser = currentUser,
                            onMemberClick = onNavigateToMemberDetails,
                            onMemberAction = { member -> showMemberActionsDialog = member }
                        )
                        1 -> FamilyStatsTab(familyStats, familyMembers)
                        2 -> FamilyActivityTab(recentActivity)
                        3 -> FamilyConfigTab(familyInfo, currentUser)
                    }
                }
            }
        }
    }

    // Dialogues
    if (showInviteDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteDialog = false },
            onSuccess = {
                showInviteDialog = false
                // Rafraîchir la liste des membres
            }
        )
    }

    showMemberActionsDialog?.let { member ->
        MemberActionsDialog(
            member = member,
            currentUser = currentUser,
            onDismiss = { showMemberActionsDialog = null },
            onAction = { action ->
                showMemberActionsDialog = null
                // TODO: Traiter l'action
            }
        )
    }
}

/**
 * État de chargement
 */
@Composable
private fun LoadingState() {
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
                text = "Chargement des données familiales...",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * État d'erreur
 */
@Composable
private fun ErrorState(
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
 * Carte d'informations de la famille
 */
@Composable
private fun FamilyInfoCard(
    familyInfo: Famille?,
    familyStats: controllers.FamilyStatistics?,
    currentUserRole: String
) {
    ArkaCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = familyInfo?.nomFamille ?: "Ma famille",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )

                    Text(
                        text = "Créée le ${familyInfo?.dateCreationFamille?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""}",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Badge du rôle
                Surface(
                    color = when (currentUserRole) {
                        "Admin" -> MaterialTheme.colors.primary
                        "Responsable" -> MaterialTheme.colors.secondary
                        else -> MaterialTheme.colors.surface
                    }.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = currentUserRole,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.caption,
                        color = when (currentUserRole) {
                            "Admin" -> MaterialTheme.colors.primary
                            "Responsable" -> MaterialTheme.colors.secondary
                            else -> MaterialTheme.colors.onSurface
                        }
                    )
                }
            }

            // Statistiques rapides
            familyStats?.let { stats ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuickStatItem(
                            icon = Icons.Default.Group,
                            label = "Membres",
                            value = "${stats.memberCount}",
                            color = MaterialTheme.colors.primary
                        )
                    }
                    item {
                        QuickStatItem(
                            icon = Icons.Default.AdminPanelSettings,
                            label = "Admins",
                            value = "${stats.adminCount}",
                            color = MaterialTheme.colors.secondary
                        )
                    }
                    item {
                        QuickStatItem(
                            icon = Icons.Default.Person,
                            label = "Responsables",
                            value = "${stats.responsibleCount}",
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Statistique rapide
 */
@Composable
private fun QuickStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Text(
            text = value,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Onglet des membres de la famille
 */
@Composable
private fun FamilyMembersTab(
    members: List<MembreFamille>,
    currentUser: MembreFamille?,
    onMemberClick: (Int) -> Unit,
    onMemberAction: (MembreFamille) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(members) { member ->
            MemberCard(
                member = member,
                isCurrentUser = member.membreFamilleId == currentUser?.membreFamilleId,
                onClick = { onMemberClick(member.membreFamilleId) },
                onAction = { onMemberAction(member) }
            )
        }
    }
}

/**
 * Carte de membre
 */
@Composable
private fun MemberCard(
    member: MembreFamille,
    isCurrentUser: Boolean,
    onClick: () -> Unit,
    onAction: () -> Unit
) {
    ArkaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = member.prenomMembre.take(1).uppercase(),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.primary
                    )
                }
            }

            // Informations
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = member.prenomMembre,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Medium
                    )

                    if (isCurrentUser) {
                        Surface(
                            color = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Vous",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                }

                Text(
                    text = member.mailMembre,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = "Ajouté le ${member.dateAjoutMembre?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            // Indicateur de statut (actif)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.size(8.dp)
            ) {}

            // Actions
            ArkaIconButton(
                icon = Icons.Default.MoreVert,
                onClick = onAction,
                contentDescription = "Actions"
            )
        }
    }
}

/**
 * Onglet des statistiques
 */
@Composable
private fun FamilyStatsTab(
    familyStats: controllers.FamilyStatistics?,
    members: List<MembreFamille>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistiques générales
        item {
            StatsCard(
                title = "Vue d'ensemble",
                stats = listOf(
                    StatItem("Membres total", "${members.size}", Icons.Default.Group),
                    StatItem("Admins", "${familyStats?.adminCount ?: 0}", Icons.Default.AdminPanelSettings),
                    StatItem("Responsables", "${familyStats?.responsibleCount ?: 0}", Icons.Default.Person),
                    StatItem("Enfants", "${familyStats?.childrenCount ?: 0}", Icons.Default.ChildCare)
                )
            )
        }
    }
}

/**
 * Carte de statistiques
 */
@Composable
private fun StatsCard(
    title: String,
    stats: List<StatItem>
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats) { stat ->
                    StatItemCard(
                        label = stat.label,
                        value = stat.value,
                        icon = stat.icon
                    )
                }
            }
        }
    }
}

/**
 * Élément de statistique (composant)
 */
@Composable
private fun StatItemCard(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colors.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.h5,
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

/**
 * Onglet de l'activité familiale
 */
@Composable
private fun FamilyActivityTab(
    activities: List<FamilyActivity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (activities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )

                        Text(
                            text = "Aucune activité récente",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )

                        Text(
                            text = "L'activité de la famille apparaîtra ici",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            items(activities) { activity ->
                ActivityItem(activity = activity)
            }
        }
    }
}

/**
 * Élément d'activité
 */
@Composable
private fun ActivityItem(activity: FamilyActivity) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getActivityIcon(activity.type),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = getActivityColor(activity.type)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = "par ${activity.author} • ${activity.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Onglet de configuration familiale
 */
@Composable
private fun FamilyConfigTab(
    familyInfo: Famille?,
    currentUser: MembreFamille?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Configuration générale
        item {
            ConfigCard(
                title = "Informations générales",
                items = listOf(
                    ConfigItem("Nom de la famille", familyInfo?.nomFamille ?: "", currentUser?.estAdmin == true),
                    ConfigItem("Date de création", familyInfo?.dateCreationFamille?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "", false)
                )
            )
        }
    }
}

/**
 * Carte de configuration
 */
@Composable
private fun ConfigCard(
    title: String,
    items: List<ConfigItem>
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.body1
                        )
                    }

                    if (item.canEdit) {
                        ArkaIconButton(
                            icon = Icons.Default.Edit,
                            onClick = { /* TODO: Éditer */ },
                            contentDescription = "Éditer"
                        )
                    }
                }
            }
        }
    }
}

// Composables de dialogue (à implémenter selon vos besoins)
@Composable
private fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    // TODO: Implémenter le dialogue d'invitation
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inviter un membre") },
        text = { Text("Fonctionnalité à implémenter") },
        confirmButton = {
            TextButton(onClick = onSuccess) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun MemberActionsDialog(
    member: MembreFamille,
    currentUser: MembreFamille?,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    // TODO: Implémenter le dialogue d'actions sur les membres
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actions pour ${member.prenomMembre}") },
        text = { Text("Fonctionnalité à implémenter") },
        confirmButton = {
            TextButton(onClick = { onAction("edit") }) {
                Text("Modifier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// ================================================================
// DATA CLASSES
// ================================================================

data class FamilyActivity(
    val description: String,
    val author: String,
    val date: LocalDateTime,
    val type: String
)

data class StatItem(
    val label: String,
    val value: String,
    val icon: ImageVector
)

data class ConfigItem(
    val label: String,
    val value: String,
    val canEdit: Boolean
)

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun getActivityIcon(type: String): ImageVector {
    return when (type) {
        "FILE_UPLOAD" -> Icons.Default.CloudUpload
        "MEMBER_JOIN" -> Icons.Default.PersonAdd
        "MEMBER_LEAVE" -> Icons.Default.PersonRemove
        "PERMISSION_CHANGE" -> Icons.Default.Security
        "FOLDER_CREATE" -> Icons.Default.CreateNewFolder
        else -> Icons.Default.Notifications
    }
}

private fun getActivityColor(type: String): Color {
    return when (type) {
        "FILE_UPLOAD" -> Color(0xFF4CAF50)
        "MEMBER_JOIN" -> Color(0xFF2196F3)
        "MEMBER_LEAVE" -> Color(0xFFFF9800)
        "PERMISSION_CHANGE" -> Color(0xFF9C27B0)
        else -> Color(0xFF757575)
    }
}