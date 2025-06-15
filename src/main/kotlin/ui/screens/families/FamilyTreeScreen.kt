// ================================================================
// FAMILYTREESCREEN.KT - ÉCRAN DE GESTION FAMILLE ARKA
// ================================================================

package ui.screens.families

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
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
    var familyInfo by remember { mutableStateOf<FamilyInfo?>(null) }
    var familyMembers by remember { mutableStateOf<List<MembreFamille>>(emptyList()) }
    var familyStats by remember { mutableStateOf<FamilyStatistics?>(null) }
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
                    // Charger les informations de la famille
                    val familyResult = familyController.getFamilyInfo(currentUser.familleId)
                    if (familyResult is FamilyController.FamilyResult.Success) {
                        familyInfo = familyResult.data
                    }

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

                    // Charger l'activité récente
                    val activityResult = familyController.getRecentActivity(currentUser.familleId, 10)
                    if (activityResult is FamilyController.FamilyResult.Success) {
                        recentActivity = activityResult.data
                    }

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
            title = familyInfo?.nom ?: "Ma famille",
            subtitle = "${familyMembers.size} membre${if (familyMembers.size > 1) "s" else ""}",
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
            actions = {
                // Inviter un membre
                if (currentUser?.estAdmin == true) {
                    ArkaIconButton(
                        icon = Icons.Default.PersonAdd,
                        onClick = { showInviteDialog = true },
                        tooltip = "Inviter un membre"
                    )
                }

                // Gérer les permissions
                ArkaIconButton(
                    icon = Icons.Default.Security,
                    onClick = onNavigateToPermissions,
                    tooltip = "Gérer les permissions"
                )

                // Menu plus d'actions
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    ArkaIconButton(
                        icon = Icons.Default.MoreVert,
                        onClick = { showMenu = true }
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (currentUser?.estAdmin == true) {
                            DropdownMenuItem(onClick = {
                                showMenu = false
                                // TODO: Configuration famille
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Configuration famille")
                            }

                            DropdownMenuItem(onClick = {
                                showMenu = false
                                // TODO: Exporter données
                            }) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Exporter les données")
                            }
                        }

                        DropdownMenuItem(onClick = {
                            showMenu = false
                            // TODO: Quitter la famille
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Quitter la famille")
                        }
                    }
                }
            }
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ArkaLoadingIndicator(
                        message = "Chargement de la famille..."
                    )
                }
            }

            errorMessage != null -> {
                ArkaErrorState(
                    message = errorMessage!!,
                    onRetry = {
                        errorMessage = null
                        // Relancer le chargement
                    }
                )
            }

            else -> {
                Column {
                    // Informations de la famille
                    FamilyInfoCard(
                        familyInfo = familyInfo,
                        familyStats = familyStats,
                        currentUserRole = if (currentUser?.estAdmin == true) "Admin" else if (currentUser?.estResponsable == true) "Responsable" else "Membre"
                    )

                    // Onglets de navigation
                    TabRow(
                        selectedTabIndex = selectedTab,
                        backgroundColor = MaterialTheme.colors.surface,
                        contentColor = MaterialTheme.colors.primary
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
 * Carte d'informations de la famille
 */
@Composable
private fun FamilyInfoCard(
    familyInfo: FamilyInfo?,
    familyStats: FamilyStatistics?,
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
                        text = familyInfo?.nom ?: "Ma famille",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )

                    Text(
                        text = "Créée le ${familyInfo?.dateCreation?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""}",
                        style = ArkaTextStyles.metadata,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    familyInfo?.description?.let { description ->
                        Text(
                            text = description,
                            style = ArkaTextStyles.cardDescription,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                        )
                    }
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
                        style = ArkaTextStyles.badge,
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
                            icon = Icons.Default.Storage,
                            label = "Espaces",
                            value = "${stats.spacesCount}",
                            color = MaterialTheme.colors.arka.success
                        )
                    }

                    item {
                        QuickStatItem(
                            icon = Icons.Default.InsertDriveFile,
                            label = "Fichiers",
                            value = "${stats.totalFiles}",
                            color = MaterialTheme.colors.arka.warning
                        )
                    }
                }
            }
        }
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Grouper les membres par rôle
        val admins = members.filter { it.estAdmin }
        val responsables = members.filter { it.estResponsable && !it.estAdmin }
        val membreStandard = members.filter { !it.estAdmin && !it.estResponsable }

        // Admins
        if (admins.isNotEmpty()) {
            item {
                MemberGroupHeader(
                    title = "Administrateurs",
                    count = admins.size,
                    icon = Icons.Default.AdminPanelSettings
                )
            }

            items(admins) { member ->
                MemberCard(
                    member = member,
                    isCurrentUser = member.membreFamilleId == currentUser?.membreFamilleId,
                    onClick = { onMemberClick(member.membreFamilleId) },
                    onAction = { onMemberAction(member) }
                )
            }
        }

        // Responsables
        if (responsables.isNotEmpty()) {
            item {
                MemberGroupHeader(
                    title = "Responsables",
                    count = responsables.size,
                    icon = Icons.Default.SupervisorAccount
                )
            }

            items(responsables) { member ->
                MemberCard(
                    member = member,
                    isCurrentUser = member.membreFamilleId == currentUser?.membreFamilleId,
                    onClick = { onMemberClick(member.membreFamilleId) },
                    onAction = { onMemberAction(member) }
                )
            }
        }

        // Membres
        if (membreStandard.isNotEmpty()) {
            item {
                MemberGroupHeader(
                    title = "Membres",
                    count = membreStandard.size,
                    icon = Icons.Default.Person
                )
            }

            items(membreStandard) { member ->
                MemberCard(
                    member = member,
                    isCurrentUser = member.membreFamilleId == currentUser?.membreFamilleId,
                    onClick = { onMemberClick(member.membreFamilleId) },
                    onAction = { onMemberAction(member) }
                )
            }
        }

        // Espace pour le FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * En-tête de groupe de membres
 */
@Composable
private fun MemberGroupHeader(
    title: String,
    count: Int,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface
        )
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
            .clickable { onClick() },
        backgroundColor = if (isCurrentUser) {
            MaterialTheme.colors.primary.copy(alpha = 0.05f)
        } else {
            MaterialTheme.colors.surface
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }

            // Informations du membre
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${member.prenom} ${member.nom}",
                        style = ArkaTextStyles.memberName,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface
                    )

                    if (isCurrentUser) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colors.primary
                        ) {
                            Text(
                                text = "VOUS",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = ArkaTextStyles.chip.copy(fontSize = 10.sp),
                                color = MaterialTheme.colors.onPrimary
                            )
                        }
                    }
                }

                Text(
                    text = member.email,
                    style = ArkaTextStyles.memberEmail,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badges de rôle
                    if (member.estAdmin) {
                        RoleBadge("Admin", MaterialTheme.colors.primary)
                    }
                    if (member.estResponsable) {
                        RoleBadge("Responsable", MaterialTheme.colors.secondary)
                    }
                }

                Text(
                    text = "Membre depuis ${member.dateInscription?.format(DateTimeFormatter.ofPattern("MM/yyyy")) ?: ""}",
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            // Indicateur de statut
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (member.estActif) MaterialTheme.colors.arka.success else MaterialTheme.colors.arka.warning,
                modifier = Modifier.size(8.dp)
            ) {}

            // Actions
            ArkaIconButton(
                icon = Icons.Default.MoreVert,
                onClick = onAction,
                size = 20.dp
            )
        }
    }
}

/**
 * Badge de rôle
 */
@Composable
private fun RoleBadge(
    role: String,
    color: Color
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = role,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = ArkaTextStyles.roleBadge,
            color = color
        )
    }
}

/**
 * Onglet des statistiques
 */
@Composable
private fun FamilyStatsTab(
    familyStats: FamilyStatistics?,
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
                    StatItem("Responsables", "${familyStats?.responsibleCount ?: 0}", Icons.Default.SupervisorAccount),
                    StatItem("Membres actifs", "${members.count { it.estActif }}", Icons.Default.CheckCircle)
                )
            )
        }

        // Statistiques de contenu
        familyStats?.let { stats ->
            item {
                StatsCard(
                    title = "Contenu familial",
                    stats = listOf(
                        StatItem("Espaces", "${stats.spacesCount}", Icons.Default.Storage),
                        StatItem("Catégories", "${stats.categoriesCount}", Icons.Default.Category),
                        StatItem("Dossiers", "${stats.foldersCount}", Icons.Default.Folder),
                        StatItem("Fichiers", "${stats.totalFiles}", Icons.Default.InsertDriveFile)
                    )
                )
            }

            item {
                StatsCard(
                    title = "Stockage",
                    stats = listOf(
                        StatItem("Espace utilisé", formatFileSize(stats.totalStorageUsed), Icons.Default.Storage),
                        StatItem("Limite", formatFileSize(stats.storageLimit), Icons.Default.CloudQueue),
                        StatItem("Pourcentage", "${(stats.totalStorageUsed * 100 / stats.storageLimit).toInt()}%", Icons.Default.PieChart),
                        StatItem("Disponible", formatFileSize(stats.storageLimit - stats.totalStorageUsed), Icons.Default.CloudDone)
                    )
                )
            }
        }

        // Graphique de répartition des membres
        item {
            MemberDistributionChart(members)
        }
    }
}

/**
 * Onglet d'activité familiale
 */
@Composable
private fun FamilyActivityTab(
    activities: List<FamilyActivity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (activities.isEmpty()) {
            item {
                ArkaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
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
                            style = ArkaTextStyles.caption,
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
 * Onglet de configuration familiale
 */
@Composable
private fun FamilyConfigTab(
    familyInfo: FamilyInfo?,
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
                    ConfigItem("Nom de la famille", familyInfo?.nom ?: "", currentUser?.estAdmin == true),
                    ConfigItem("Description", familyInfo?.description ?: "", currentUser?.estAdmin == true),
                    ConfigItem("Langue", "Français", currentUser?.estAdmin == true),
                    ConfigItem("Fuseau horaire", "Europe/Paris", currentUser?.estAdmin == true)
                )
            )
        }

        // Paramètres de sécurité
        if (currentUser?.estAdmin == true) {
            item {
                ConfigCard(
                    title = "Sécurité et confidentialité",
                    items = listOf(
                        ConfigItem("Validation des nouveaux membres", "Activée", true),
                        ConfigItem("Partage externe", "Autorisé", true),
                        ConfigItem("Sauvegarde automatique", "Activée", true),
                        ConfigItem("Chiffrement des données", "AES-256", false)
                    )
                )
            }
        }

        // Limites et quotas
        item {
            ConfigCard(
                title = "Limites et quotas",
                items = listOf(
                    ConfigItem("Membres maximum", "10", false),
                    ConfigItem("Stockage maximum", "100 GB", false),
                    ConfigItem("Taille fichier max", "50 MB", false),
                    ConfigItem("Espaces maximum", "5", false)
                )
            )
        }
    }
}

// ================================================================
// DIALOGUES
// ================================================================

/**
 * Dialogue d'invitation de membre
 */
@Composable
private fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBRE") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inviter un membre") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Adresse email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Sélection du rôle
                Column {
                    Text("Rôle :", style = ArkaTextStyles.label)
                    Spacer(Modifier.height(8.dp))

                    listOf("MEMBRE" to "Membre", "RESPONSABLE" to "Responsable", "ADMIN" to "Administrateur").forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = role == value,
                                onClick = { role = value }
                            )
                            Text(label)
                        }
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message d'invitation (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // TODO: Envoyer l'invitation
                    onSuccess()
                },
                enabled = email.isNotBlank()
            ) {
                Text("Envoyer l'invitation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialogue d'actions sur un membre
 */
@Composable
private fun MemberActionsDialog(
    member: MembreFamille,
    currentUser: MembreFamille?,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${member.prenom} ${member.nom}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentUser?.estAdmin == true && member.membreFamilleId != currentUser.membreFamilleId) {
                    TextButton(
                        onClick = { onAction("CHANGE_ROLE") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Modifier le rôle")
                    }

                    TextButton(
                        onClick = { onAction("PERMISSIONS") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gérer les permissions")
                    }

                    if (member.estActif) {
                        TextButton(
                            onClick = { onAction("SUSPEND") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Suspendre")
                        }
                    } else {
                        TextButton(
                            onClick = { onAction("ACTIVATE") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Réactiver")
                        }
                    }

                    TextButton(
                        onClick = { onAction("REMOVE") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RemoveCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retirer de la famille")
                    }
                }

                TextButton(
                    onClick = { onAction("CONTACT") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Contacter")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

// ================================================================
// COMPOSANTS UTILITAIRES
// ================================================================

@Composable
private fun QuickStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )

            Text(
                text = label,
                style = ArkaTextStyles.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

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
                fontWeight = FontWeight.SemiBold
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats) { stat ->
                    StatItemCard(stat)
                }
            }
        }
    }
}

@Composable
private fun StatItemCard(stat: StatItem) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = stat.value,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface
            )

            Text(
                text = stat.label,
                style = ArkaTextStyles.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MemberDistributionChart(members: List<MembreFamille>) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Répartition des rôles",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            val admins = members.count { it.estAdmin }
            val responsables = members.count { it.estResponsable && !it.estAdmin }
            val membresStandard = members.count { !it.estAdmin && !it.estResponsable }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartItem("Administrateurs", admins, members.size, MaterialTheme.colors.primary)
                ChartItem("Responsables", responsables, members.size, MaterialTheme.colors.secondary)
                ChartItem("Membres", membresStandard, members.size, MaterialTheme.colors.arka.success)
            }
        }
    }
}

@Composable
private fun ChartItem(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val percentage = if (total > 0) (count * 100 / total) else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color,
            modifier = Modifier.size(12.dp)
        ) {}

        Text(
            text = label,
            style = ArkaTextStyles.chartLabel,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "$count ($percentage%)",
            style = ArkaTextStyles.chartValue,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ActivityItem(activity: FamilyActivity) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getActivityIcon(activity.type),
                contentDescription = null,
                tint = getActivityColor(activity.type),
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.description,
                    style = ArkaTextStyles.activityDescription,
                    color = MaterialTheme.colors.onSurface
                )

                Text(
                    text = "${activity.author} • ${activity.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))}",
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = ArkaTextStyles.configLabel,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.value,
                            style = ArkaTextStyles.configValue,
                            color = MaterialTheme.colors.onSurface
                        )

                        if (item.canEdit) {
                            ArkaIconButton(
                                icon = Icons.Default.Edit,
                                onClick = { /* TODO: Éditer */ },
                                size = 16.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================================================================
// DATA CLASSES
// ================================================================

data class FamilyInfo(
    val nom: String,
    val description: String?,
    val dateCreation: LocalDate
)

data class FamilyStatistics(
    val familyName: String,
    val memberCount: Int,
    val adminCount: Int,
    val responsibleCount: Int,
    val spacesCount: Int,
    val categoriesCount: Int,
    val foldersCount: Int,
    val totalFiles: Int,
    val totalStorageUsed: Long,
    val storageLimit: Long
)

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
        "FILE_UPLOAD" -> MaterialTheme.colors.arka.success
        "MEMBER_JOIN" -> MaterialTheme.colors.primary
        "MEMBER_LEAVE" -> MaterialTheme.colors.arka.warning
        "PERMISSION_CHANGE" -> MaterialTheme.colors.secondary
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
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