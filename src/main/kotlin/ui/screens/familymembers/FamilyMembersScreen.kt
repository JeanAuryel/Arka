// ================================================================
// FAMILYMEMBERSSCREEN.KT - GESTION DES MEMBRES DE FAMILLE (DESKTOP)
// ================================================================

package ui.screens.familymembers

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import controllers.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran de gestion des membres de famille pour application desktop
 *
 * Fonctionnalités:
 * - Liste des membres avec rôles et statuts
 * - Invitation de nouveaux membres
 * - Gestion des permissions et rôles
 * - Historique des activités
 * - Panel détaillé pour chaque membre
 * - Actions d'administration (pour admins/responsables)
 * - Statistiques familiales
 */
@Composable
fun FamilyMembersScreen(
    familyMemberController: FamilyMemberController,
    delegationController: DelegationController,
    currentUserId: Int,
    currentUserRole: UserRole,
    onMemberClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // États principaux
    var members by remember { mutableStateOf<List<FamilyMemberInfo>>(emptyList()) }
    var familyStats by remember { mutableStateOf<FamilyStats?>(null) }
    var selectedMember by remember { mutableStateOf<FamilyMemberInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // États UI
    var searchQuery by remember { mutableStateOf("") }
    var filterRole by remember { mutableStateOf<UserRole?>(null) }
    var filterStatus by remember { mutableStateOf<MemberStatus?>(null) }
    var showMemberDetails by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(MemberSortOption.NAME_ASC) }

    // Dialogues
    var showInviteDialog by remember { mutableStateOf(false) }
    var showEditMemberDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<FamilyMemberInfo?>(null) }

    // Chargement des données
    fun loadFamilyData() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                // Charger les membres de famille
                val membersResult = familyMemberController.getAllFamilyMembers()
                if (membersResult is FamilyMemberController.FamilyMemberResult.Success) {
                    members = membersResult.data.map { FamilyMemberInfo.fromEntity(it) }
                }

                // Charger les statistiques familiales
                val statsResult = familyMemberController.getFamilyStatistics()
                if (statsResult is FamilyMemberController.FamilyMemberResult.Success) {
                    familyStats = FamilyStats.fromEntity(statsResult.data)
                }

            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Filtrage et tri des membres
    val filteredAndSortedMembers = remember(members, searchQuery, filterRole, filterStatus, sortOption) {
        members
            .filter { member ->
                if (searchQuery.isNotBlank()) {
                    member.fullName.contains(searchQuery, ignoreCase = true) ||
                            member.email.contains(searchQuery, ignoreCase = true)
                } else true
            }
            .filter { member ->
                filterRole?.let { it == member.role } ?: true
            }
            .filter { member ->
                filterStatus?.let { it == member.status } ?: true
            }
            .sortedWith { a, b ->
                when (sortOption) {
                    MemberSortOption.NAME_ASC -> a.fullName.compareTo(b.fullName, ignoreCase = true)
                    MemberSortOption.NAME_DESC -> b.fullName.compareTo(a.fullName, ignoreCase = true)
                    MemberSortOption.ROLE_ASC -> a.role.ordinal.compareTo(b.role.ordinal)
                    MemberSortOption.ROLE_DESC -> b.role.ordinal.compareTo(a.role.ordinal)
                    MemberSortOption.JOIN_DATE_ASC -> a.joinDate.compareTo(b.joinDate)
                    MemberSortOption.JOIN_DATE_DESC -> b.joinDate.compareTo(a.joinDate)
                    MemberSortOption.ACTIVITY_ASC -> a.lastActivity.compareTo(b.lastActivity)
                    MemberSortOption.ACTIVITY_DESC -> b.lastActivity.compareTo(a.lastActivity)
                }
            }
    }

    // Chargement initial
    LaunchedEffect(Unit) {
        loadFamilyData()
    }

    // Layout principal
    Row(modifier = Modifier.fillMaxSize()) {
        // Panneau latéral avec statistiques (300dp)
        FamilyStatsPanel(
            stats = familyStats,
            onInviteMember = if (canInviteMembers(currentUserRole)) {
                { showInviteDialog = true }
            } else null,
            modifier = Modifier.width(300.dp)
        )

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

        // Panneau principal
        Column(modifier = Modifier.weight(1f)) {
            // Barre d'outils
            FamilyMembersToolbar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                filterRole = filterRole,
                onFilterRoleChange = { filterRole = it },
                filterStatus = filterStatus,
                onFilterStatusChange = { filterStatus = it },
                sortOption = sortOption,
                onSortChange = { sortOption = it },
                onInviteMember = if (canInviteMembers(currentUserRole)) {
                    { showInviteDialog = true }
                } else null,
                onRefresh = { loadFamilyData() },
                memberCount = filteredAndSortedMembers.size,
                totalCount = members.size
            )

            Row(modifier = Modifier.weight(1f)) {
                // Liste des membres
                Box(
                    modifier = Modifier
                        .weight(if (showMemberDetails) 0.6f else 1f)
                        .fillMaxHeight()
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ArkaLoadingIndicator(message = "Chargement des membres...")
                            }
                        }

                        errorMessage != null -> {
                            ErrorDisplay(
                                message = errorMessage!!,
                                onRetry = { loadFamilyData() }
                            )
                        }

                        filteredAndSortedMembers.isEmpty() -> {
                            EmptyMembersView(
                                hasMembers = members.isNotEmpty(),
                                searchQuery = searchQuery,
                                onClearSearch = { searchQuery = "" },
                                onInviteMember = if (canInviteMembers(currentUserRole)) {
                                    { showInviteDialog = true }
                                } else null
                            )
                        }

                        else -> {
                            FamilyMembersList(
                                members = filteredAndSortedMembers,
                                currentUserId = currentUserId,
                                currentUserRole = currentUserRole,
                                selectedMember = selectedMember,
                                onMemberClick = { member ->
                                    selectedMember = member
                                    showMemberDetails = true
                                    onMemberClick(member.id)
                                },
                                onEditMember = { member ->
                                    memberToEdit = member
                                    showEditMemberDialog = true
                                },
                                onChangeRole = { member, newRole ->
                                    // TODO: Changer le rôle
                                },
                                onToggleStatus = { member ->
                                    // TODO: Activer/désactiver membre
                                },
                                onDeleteMember = { member ->
                                    memberToEdit = member
                                    showDeleteConfirmation = true
                                },
                                onManagePermissions = { member ->
                                    memberToEdit = member
                                    showPermissionsDialog = true
                                }
                            )
                        }
                    }
                }

                // Panneau de détails (si activé)
                if (showMemberDetails && selectedMember != null) {
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

                    MemberDetailsPanel(
                        member = selectedMember!!,
                        currentUserRole = currentUserRole,
                        onClose = { showMemberDetails = false },
                        onEditMember = {
                            memberToEdit = selectedMember
                            showEditMemberDialog = true
                        },
                        onManagePermissions = {
                            memberToEdit = selectedMember
                            showPermissionsDialog = true
                        },
                        onSendMessage = { /* TODO */ },
                        modifier = Modifier.width(350.dp)
                    )
                }
            }
        }
    }

    // Dialogues
    if (showInviteDialog) {
        InviteMemberDialog(
            onConfirm = { email, role ->
                scope.launch {
                    try {
                        // TODO: Inviter membre
                        loadFamilyData()
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de l'invitation: ${e.message}"
                    }
                }
                showInviteDialog = false
            },
            onDismiss = { showInviteDialog = false },
            currentUserRole = currentUserRole
        )
    }

    if (showEditMemberDialog && memberToEdit != null) {
        EditMemberDialog(
            member = memberToEdit!!,
            currentUserRole = currentUserRole,
            onConfirm = { updatedMember ->
                scope.launch {
                    try {
                        // TODO: Mettre à jour membre
                        loadFamilyData()
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de la mise à jour: ${e.message}"
                    }
                }
                showEditMemberDialog = false
                memberToEdit = null
            },
            onDismiss = {
                showEditMemberDialog = false
                memberToEdit = null
            }
        )
    }

    if (showDeleteConfirmation && memberToEdit != null) {
        DeleteMemberConfirmationDialog(
            member = memberToEdit!!,
            onConfirm = {
                scope.launch {
                    try {
                        // TODO: Supprimer membre
                        if (selectedMember?.id == memberToEdit?.id) {
                            selectedMember = null
                            showMemberDetails = false
                        }
                        loadFamilyData()
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de la suppression: ${e.message}"
                    }
                }
                showDeleteConfirmation = false
                memberToEdit = null
            },
            onDismiss = {
                showDeleteConfirmation = false
                memberToEdit = null
            }
        )
    }

    if (showPermissionsDialog && memberToEdit != null) {
        MemberPermissionsDialog(
            member = memberToEdit!!,
            onPermissionsChanged = { permissions ->
                scope.launch {
                    try {
                        // TODO: Mettre à jour permissions
                        loadFamilyData()
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de la mise à jour des permissions: ${e.message}"
                    }
                }
                showPermissionsDialog = false
                memberToEdit = null
            },
            onDismiss = {
                showPermissionsDialog = false
                memberToEdit = null
            }
        )
    }
}

/**
 * Panneau latéral avec statistiques familiales
 */
@Composable
private fun FamilyStatsPanel(
    stats: FamilyStats?,
    onInviteMember: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colors.surface,
        elevation = 1.dp
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Statistiques familiales",
                    style = ArkaTextStyles.cardTitle,
                    fontWeight = FontWeight.Bold
                )
            }

            if (stats != null) {
                item {
                    StatsCard(
                        title = "Membres actifs",
                        value = "${stats.activeMembers}",
                        subtitle = "sur ${stats.totalMembers} membres",
                        icon = Icons.Default.Group,
                        color = Color(0xFF4CAF50)
                    )
                }

                item {
                    StatsCard(
                        title = "Administrateurs",
                        value = "${stats.adminCount}",
                        subtitle = "Accès complet",
                        icon = Icons.Default.AdminPanelSettings,
                        color = Color(0xFF2196F3)
                    )
                }

                item {
                    StatsCard(
                        title = "Responsables",
                        value = "${stats.responsibleCount}",
                        subtitle = "Gestion limitée",
                        icon = Icons.Default.ManageAccounts,
                        color = Color(0xFFFF9800)
                    )
                }

                item {
                    StatsCard(
                        title = "Membres",
                        value = "${stats.memberCount}",
                        subtitle = "Accès basique",
                        icon = Icons.Default.Person,
                        color = Color(0xFF9C27B0)
                    )
                }

                item {
                    Divider()
                }

                item {
                    Text(
                        text = "Activité récente",
                        style = ArkaTextStyles.cardDescription,
                        fontWeight = FontWeight.Medium
                    )
                }

                item {
                    StatsCard(
                        title = "Dernière connexion",
                        value = formatDateTime(stats.lastActivity),
                        subtitle = "",
                        icon = Icons.Default.AccessTime,
                        color = MaterialTheme.colors.primary
                    )
                }

                item {
                    StatsCard(
                        title = "Invitations en cours",
                        value = "${stats.pendingInvitations}",
                        subtitle = if (stats.pendingInvitations > 0) "En attente" else "Aucune",
                        icon = Icons.Default.Mail,
                        color = if (stats.pendingInvitations > 0) Color(0xFFFF5722) else Color(0xFF607D8B)
                    )
                }
            }

            if (onInviteMember != null) {
                item {
                    Divider()
                }

                item {
                    ArkaButton(
                        text = "Inviter un membre",
                        onClick = onInviteMember,
                        icon = Icons.Default.PersonAdd,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Text(
                    text = "Actions rapides",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArkaOutlinedButton(
                        text = "Exporter liste",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.Download,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ArkaOutlinedButton(
                        text = "Paramètres famille",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.Settings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Barre d'outils de gestion des membres
 */
@Composable
private fun FamilyMembersToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterRole: UserRole?,
    onFilterRoleChange: (UserRole?) -> Unit,
    filterStatus: MemberStatus?,
    onFilterStatusChange: (MemberStatus?) -> Unit,
    sortOption: MemberSortOption,
    onSortChange: (MemberSortOption) -> Unit,
    onInviteMember: (() -> Unit)?,
    onRefresh: () -> Unit,
    memberCount: Int,
    totalCount: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Titre et compteur
            Column {
                Text(
                    text = "Membres de famille",
                    style = ArkaTextStyles.cardTitle,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (memberCount == totalCount) {
                        "$totalCount membre${if (totalCount > 1) "s" else ""}"
                    } else {
                        "$memberCount sur $totalCount membre${if (totalCount > 1) "s" else ""}"
                    },
                    style = ArkaTextStyles.helpText,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Rechercher un membre...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.width(300.dp)
            )

            // Filtres
            var showFilters by remember { mutableStateOf(false) }
            IconButton(onClick = { showFilters = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtres",
                    tint = if (filterRole != null || filterStatus != null) MaterialTheme.colors.primary
                    else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            // Menu des filtres
            DropdownMenu(
                expanded = showFilters,
                onDismissRequest = { showFilters = false }
            ) {
                Text(
                    text = "Filtrer par rôle",
                    style = ArkaTextStyles.metadata,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                DropdownMenuItem(onClick = {
                    onFilterRoleChange(null)
                    showFilters = false
                }) {
                    Text("Tous les rôles")
                    if (filterRole == null) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                UserRole.values().forEach { role ->
                    DropdownMenuItem(onClick = {
                        onFilterRoleChange(role)
                        showFilters = false
                    }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = role.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(role.displayName)
                            if (filterRole == role) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Divider()

                Text(
                    text = "Filtrer par statut",
                    style = ArkaTextStyles.metadata,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                DropdownMenuItem(onClick = {
                    onFilterStatusChange(null)
                    showFilters = false
                }) {
                    Text("Tous les statuts")
                    if (filterStatus == null) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                MemberStatus.values().forEach { status ->
                    DropdownMenuItem(onClick = {
                        onFilterStatusChange(status)
                        showFilters = false
                    }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusIndicator(status = status, size = 8.dp)
                            Text(status.displayName)
                            if (filterStatus == status) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Tri
            var showSort by remember { mutableStateOf(false) }
            IconButton(onClick = { showSort = true }) {
                Icon(Icons.Default.Sort, contentDescription = "Trier")
            }

            DropdownMenu(
                expanded = showSort,
                onDismissRequest = { showSort = false }
            ) {
                MemberSortOption.values().forEach { sort ->
                    DropdownMenuItem(onClick = {
                        onSortChange(sort)
                        showSort = false
                    }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = sort.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(sort.displayName)
                            if (sortOption == sort) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.height(24.dp).width(1.dp))

            // Actions
            if (onInviteMember != null) {
                ArkaButton(
                    text = "Inviter",
                    onClick = onInviteMember,
                    icon = Icons.Default.PersonAdd
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
            }
        }
    }
}

/**
 * Liste des membres de famille
 */
@Composable
private fun FamilyMembersList(
    members: List<FamilyMemberInfo>,
    currentUserId: Int,
    currentUserRole: UserRole,
    selectedMember: FamilyMemberInfo?,
    onMemberClick: (FamilyMemberInfo) -> Unit,
    onEditMember: (FamilyMemberInfo) -> Unit,
    onChangeRole: (FamilyMemberInfo, UserRole) -> Unit,
    onToggleStatus: (FamilyMemberInfo) -> Unit,
    onDeleteMember: (FamilyMemberInfo) -> Unit,
    onManagePermissions: (FamilyMemberInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // En-tête des colonnes
        item {
            FamilyMembersListHeader()
        }

        // Liste des membres
        items(members) { member ->
            val isSelected = selectedMember?.id == member.id
            val isCurrentUser = member.id == currentUserId

            FamilyMemberItem(
                member = member,
                isSelected = isSelected,
                isCurrentUser = isCurrentUser,
                currentUserRole = currentUserRole,
                onClick = { onMemberClick(member) },
                onEdit = { onEditMember(member) },
                onChangeRole = { newRole -> onChangeRole(member, newRole) },
                onToggleStatus = { onToggleStatus(member) },
                onDelete = { onDeleteMember(member) },
                onManagePermissions = { onManagePermissions(member) }
            )
        }
    }
}

/**
 * En-tête de la liste des membres
 */
@Composable
private fun FamilyMembersListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Membre",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.3f)
            )

            Text(
                text = "Rôle",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.15f)
            )

            Text(
                text = "Statut",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.1f)
            )

            Text(
                text = "Dernière activité",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.2f)
            )

            Text(
                text = "Rejoint le",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.15f)
            )

            Text(
                text = "Actions",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.1f)
            )
        }
    }
}

/**
 * Item de membre dans la liste
 */
@Composable
private fun FamilyMemberItem(
    member: FamilyMemberInfo,
    isSelected: Boolean,
    isCurrentUser: Boolean,
    currentUserRole: UserRole,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onChangeRole: (UserRole) -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onManagePermissions: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else Color.Transparent
            ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Informations du membre
            Row(
                modifier = Modifier.weight(0.3f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    name = member.fullName,
                    size = 40.dp,
                    backgroundColor = if (isCurrentUser) MaterialTheme.colors.primary
                    else MaterialTheme.colors.secondary
                )

                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member.fullName,
                            style = ArkaTextStyles.cardDescription,
                            fontWeight = FontWeight.Medium
                        )

                        if (isCurrentUser) {
                            StatusBadge(
                                text = "Vous",
                                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                                textColor = MaterialTheme.colors.primary
                            )
                        }
                    }

                    Text(
                        text = member.email,
                        style = ArkaTextStyles.helpText,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Rôle
            Row(
                modifier = Modifier.weight(0.15f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = member.role.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = member.role.color
                )

                Text(
                    text = member.role.displayName,
                    style = ArkaTextStyles.metadata,
                    color = member.role.color
                )
            }

            // Statut
            Row(
                modifier = Modifier.weight(0.1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIndicator(
                    status = member.status,
                    size = 8.dp
                )

                Text(
                    text = member.status.displayName,
                    style = ArkaTextStyles.metadata,
                    color = member.status.color
                )
            }

            // Dernière activité
            Text(
                text = formatRelativeTime(member.lastActivity),
                style = ArkaTextStyles.date,
                modifier = Modifier.weight(0.2f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Date d'adhésion
            Text(
                text = formatDate(member.joinDate),
                style = ArkaTextStyles.date,
                modifier = Modifier.weight(0.15f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Actions
            Row(
                modifier = Modifier.weight(0.1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                var showActionsMenu by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { showActionsMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Menu des actions
                DropdownMenu(
                    expanded = showActionsMenu,
                    onDismissRequest = { showActionsMenu = false }
                ) {
                    if (canEditMember(currentUserRole, member.role, isCurrentUser)) {
                        DropdownMenuItem(onClick = {
                            onEdit()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Modifier")
                            }
                        }
                    }

                    if (canManagePermissions(currentUserRole, member.role)) {
                        DropdownMenuItem(onClick = {
                            onManagePermissions()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Permissions")
                            }
                        }
                    }

                    if (canChangeRole(currentUserRole, member.role)) {
                        // Sous-menu pour changer le rôle
                        var showRoleMenu by remember { mutableStateOf(false) }
                        DropdownMenuItem(onClick = { showRoleMenu = true }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ChangeCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Changer rôle")
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (canToggleStatus(currentUserRole, member.role, isCurrentUser)) {
                        DropdownMenuItem(onClick = {
                            onToggleStatus()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = if (member.status == MemberStatus.ACTIVE) Icons.Default.Block else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(if (member.status == MemberStatus.ACTIVE) "Désactiver" else "Activer")
                            }
                        }
                    }

                    if (canDeleteMember(currentUserRole, member.role, isCurrentUser)) {
                        Divider()

                        DropdownMenuItem(onClick = {
                            onDelete()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colors.error
                                )
                                Text("Supprimer", color = MaterialTheme.colors.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vue vide quand aucun membre ne correspond aux critères
 */
@Composable
private fun EmptyMembersView(
    hasMembers: Boolean,
    searchQuery: String,
    onClearSearch: () -> Unit,
    onInviteMember: (() -> Unit)?
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
                imageVector = if (hasMembers) Icons.Default.SearchOff else Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                text = if (hasMembers) {
                    if (searchQuery.isNotEmpty()) "Aucun membre trouvé pour \"$searchQuery\""
                    else "Aucun membre ne correspond aux filtres"
                } else {
                    "Aucun membre dans la famille"
                },
                style = ArkaTextStyles.cardTitle,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = if (hasMembers) {
                    "Essayez de modifier vos critères de recherche"
                } else {
                    "Commencez par inviter des membres de votre famille"
                },
                style = ArkaTextStyles.cardDescription,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )

            if (hasMembers && searchQuery.isNotEmpty()) {
                ArkaOutlinedButton(
                    text = "Effacer la recherche",
                    onClick = onClearSearch,
                    icon = Icons.Default.Clear
                )
            } else if (!hasMembers && onInviteMember != null) {
                ArkaButton(
                    text = "Inviter le premier membre",
                    onClick = onInviteMember,
                    icon = Icons.Default.PersonAdd
                )
            }
        }
    }
}

/**
 * Panneau de détails d'un membre
 */
@Composable
private fun MemberDetailsPanel(
    member: FamilyMemberInfo,
    currentUserRole: UserRole,
    onClose: () -> Unit,
    onEditMember: () -> Unit,
    onManagePermissions: () -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colors.surface,
        elevation = 1.dp
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // En-tête
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Détails du membre",
                        style = ArkaTextStyles.cardTitle,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
            }

            item {
                Divider()
            }

            item {
                // Photo et informations principales
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserAvatar(
                        name = member.fullName,
                        size = 80.dp,
                        backgroundColor = MaterialTheme.colors.primary
                    )

                    Text(
                        text = member.fullName,
                        style = ArkaTextStyles.cardTitle,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = member.role.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = member.role.color
                        )

                        Text(
                            text = member.role.displayName,
                            style = ArkaTextStyles.cardDescription,
                            color = member.role.color
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusIndicator(
                            status = member.status,
                            size = 8.dp
                        )

                        Text(
                            text = member.status.displayName,
                            style = ArkaTextStyles.cardDescription,
                            color = member.status.color
                        )
                    }
                }
            }

            item {
                Divider()
            }

            item {
                // Informations détaillées
                MemberDetailSection(
                    title = "Informations personnelles",
                    items = listOf(
                        "Email" to member.email,
                        "Date de naissance" to formatDate(member.birthDate),
                        "Genre" to member.gender,
                        "Téléphone" to (member.phone ?: "Non renseigné")
                    )
                )
            }

            item {
                MemberDetailSection(
                    title = "Activité",
                    items = listOf(
                        "Membre depuis" to formatDate(member.joinDate),
                        "Dernière connexion" to formatRelativeTime(member.lastActivity),
                        "Nombre de fichiers" to "${member.fileCount}",
                        "Délégations actives" to "${member.activeDelegations}"
                    )
                )
            }

            if (member.permissions.isNotEmpty()) {
                item {
                    MemberPermissionsSection(
                        permissions = member.permissions
                    )
                }
            }

            item {
                Divider()
            }

            item {
                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArkaButton(
                        text = "Envoyer message",
                        onClick = onSendMessage,
                        icon = Icons.Default.Message,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (canEditMember(currentUserRole, member.role, false)) {
                        ArkaOutlinedButton(
                            text = "Modifier profil",
                            onClick = onEditMember,
                            icon = Icons.Default.Edit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (canManagePermissions(currentUserRole, member.role)) {
                        ArkaOutlinedButton(
                            text = "Gérer permissions",
                            onClick = onManagePermissions,
                            icon = Icons.Default.Security,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section de détails d'un membre
 */
@Composable
private fun MemberDetailSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = ArkaTextStyles.cardDescription,
            fontWeight = FontWeight.Medium
        )

        items.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = value,
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

/**
 * Section des permissions d'un membre
 */
@Composable
private fun MemberPermissionsSection(
    permissions: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Permissions spéciales",
            style = ArkaTextStyles.cardDescription,
            fontWeight = FontWeight.Medium
        )

        permissions.forEach { permission ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF4CAF50)
                )

                Text(
                    text = permission,
                    style = ArkaTextStyles.metadata
                )
            }
        }
    }
}

// ================================================================
// DIALOGUES
// ================================================================

/**
 * Dialogue d'invitation d'un nouveau membre
 */
@Composable
private fun InviteMemberDialog(
    onConfirm: (String, UserRole) -> Unit,
    onDismiss: () -> Unit,
    currentUserRole: UserRole
) {
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.MEMBER) }
    var isEmailValid by remember { mutableStateOf(false) }

    LaunchedEffect(email) {
        isEmailValid = email.contains("@") && email.contains(".")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inviter un nouveau membre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Adresse email") },
                    placeholder = { Text("exemple@email.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = email.isNotEmpty() && !isEmailValid,
                    modifier = Modifier.fillMaxWidth()
                )

                if (email.isNotEmpty() && !isEmailValid) {
                    Text(
                        text = "Adresse email invalide",
                        style = ArkaTextStyles.helpText,
                        color = MaterialTheme.colors.error
                    )
                }

                // Rôle
                Text(
                    text = "Rôle du nouveau membre",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                Column {
                    UserRole.values().filter { role ->
                        canAssignRole(currentUserRole, role)
                    }.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = role }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role }
                            )

                            Icon(
                                imageVector = role.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = role.color
                            )

                            Column {
                                Text(
                                    text = role.displayName,
                                    style = ArkaTextStyles.cardDescription,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = role.description,
                                    style = ArkaTextStyles.helpText,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Envoyer invitation",
                onClick = { onConfirm(email, selectedRole) },
                enabled = isEmailValid,
                icon = Icons.Default.Send
            )
        },
        dismissButton = {
            ArkaOutlinedButton(
                text = "Annuler",
                onClick = onDismiss
            )
        }
    )
}

/**
 * Dialogue d'édition d'un membre
 */
@Composable
private fun EditMemberDialog(
    member: FamilyMemberInfo,
    currentUserRole: UserRole,
    onConfirm: (FamilyMemberInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var firstName by remember { mutableStateOf(member.firstName) }
    var lastName by remember { mutableStateOf(member.lastName) }
    var email by remember { mutableStateOf(member.email) }
    var phone by remember { mutableStateOf(member.phone ?: "") }
    var selectedRole by remember { mutableStateOf(member.role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le membre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Prénom") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Nom") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone (optionnel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                if (canChangeRole(currentUserRole, member.role)) {
                    Text(
                        text = "Rôle",
                        style = ArkaTextStyles.cardDescription,
                        fontWeight = FontWeight.Medium
                    )

                    UserRole.values().filter { role ->
                        canAssignRole(currentUserRole, role)
                    }.forEach { role ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = role }
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role }
                            )

                            Icon(
                                imageVector = role.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = role.color
                            )

                            Text(
                                text = role.displayName,
                                style = ArkaTextStyles.cardDescription
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Enregistrer",
                onClick = {
                    onConfirm(
                        member.copy(
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            phone = phone.takeIf { it.isNotBlank() },
                            role = selectedRole
                        )
                    )
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank()
            )
        },
        dismissButton = {
            ArkaOutlinedButton(
                text = "Annuler",
                onClick = onDismiss
            )
        }
    )
}

/**
 * Dialogue de confirmation de suppression
 */
@Composable
private fun DeleteMemberConfirmationDialog(
    member: FamilyMemberInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer le membre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Êtes-vous sûr de vouloir supprimer ${member.fullName} de la famille ?")

                Text(
                    text = "Cette action est irréversible et supprimera :",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("• Accès à tous les fichiers familiaux")
                    Text("• Toutes les délégations accordées")
                    Text("• L'historique d'activité")
                }
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Supprimer",
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                icon = Icons.Default.Delete
            )
        },
        dismissButton = {
            ArkaOutlinedButton(
                text = "Annuler",
                onClick = onDismiss
            )
        }
    )
}

/**
 * Dialogue de gestion des permissions
 */
@Composable
private fun MemberPermissionsDialog(
    member: FamilyMemberInfo,
    onPermissionsChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPermissions by remember { mutableStateOf(member.permissions.toSet()) }

    val availablePermissions = listOf(
        "READ_ALL_FILES" to "Lire tous les fichiers",
        "WRITE_ALL_FILES" to "Modifier tous les fichiers",
        "DELETE_FILES" to "Supprimer des fichiers",
        "MANAGE_CATEGORIES" to "Gérer les catégories",
        "INVITE_MEMBERS" to "Inviter des membres",
        "MANAGE_DELEGATIONS" to "Gérer les délégations",
        "VIEW_AUDIT_LOG" to "Voir les journaux d'audit"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions de ${member.fullName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sélectionnez les permissions spéciales pour ce membre :",
                    style = ArkaTextStyles.cardDescription
                )

                availablePermissions.forEach { (permission, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPermissions = if (selectedPermissions.contains(permission)) {
                                    selectedPermissions - permission
                                } else {
                                    selectedPermissions + permission
                                }
                            }
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedPermissions.contains(permission),
                            onCheckedChange = { checked ->
                                selectedPermissions = if (checked) {
                                    selectedPermissions + permission
                                } else {
                                    selectedPermissions - permission
                                }
                            }
                        )

                        Column {
                            Text(
                                text = description,
                                style = ArkaTextStyles.cardDescription
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Enregistrer",
                onClick = { onPermissionsChanged(selectedPermissions.toList()) }
            )
        },
        dismissButton = {
            ArkaOutlinedButton(
                text = "Annuler",
                onClick = onDismiss
            )
        }
    )
}

/**
 * Affichage d'erreur
 */
@Composable
private fun ErrorDisplay(
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
                style = ArkaTextStyles.cardTitle
            )

            Text(
                text = message,
                style = ArkaTextStyles.cardDescription,
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
 * Indicateur de statut
 */
@Composable
private fun StatusIndicator(
    status: MemberStatus,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = status.color,
                shape = CircleShape
            )
    )
}

// ================================================================
// MODÈLES DE DONNÉES ET UTILITAIRES
// ================================================================

/**
 * Options de tri pour les membres
 */
enum class MemberSortOption(val displayName: String, val icon: ImageVector) {
    NAME_ASC("Nom (A-Z)", Icons.Default.SortByAlpha),
    NAME_DESC("Nom (Z-A)", Icons.Default.SortByAlpha),
    ROLE_ASC("Rôle (Admin en premier)", Icons.Default.AdminPanelSettings),
    ROLE_DESC("Rôle (Membre en premier)", Icons.Default.Person),
    JOIN_DATE_ASC("Ancienneté", Icons.Default.DateRange),
    JOIN_DATE_DESC("Nouveaux membres", Icons.Default.DateRange),
    ACTIVITY_ASC("Moins actifs", Icons.Default.AccessTime),
    ACTIVITY_DESC("Plus actifs", Icons.Default.AccessTime)
}

/**
 * Statuts des membres
 */
enum class MemberStatus(val displayName: String, val color: Color) {
    ACTIVE("Actif", Color(0xFF4CAF50)),
    INACTIVE("Inactif", Color(0xFF9E9E9E)),
    PENDING("En attente", Color(0xFFFF9800)),
    SUSPENDED("Suspendu", Color(0xFFF44336))
}

/**
 * Rôles utilisateur étendus avec icônes et couleurs
 */
enum class UserRole(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
) {
    ADMIN(
        "Administrateur",
        "Contrôle total de la famille et de tous les fichiers",
        Icons.Default.AdminPanelSettings,
        Color(0xFF2196F3)
    ),
    RESPONSIBLE(
        "Responsable",
        "Peut gérer les membres et certains paramètres",
        Icons.Default.ManageAccounts,
        Color(0xFFFF9800)
    ),
    MEMBER(
        "Membre",
        "Accès à ses propres fichiers et fichiers partagés",
        Icons.Default.Person,
        Color(0xFF9C27B0)
    )
}

/**
 * Informations d'un membre de famille
 */
data class FamilyMemberInfo(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val birthDate: LocalDate,
    val gender: String,
    val role: UserRole,
    val status: MemberStatus,
    val joinDate: LocalDate,
    val lastActivity: LocalDateTime,
    val fileCount: Int,
    val activeDelegations: Int,
    val permissions: List<String>
) {
    val fullName: String get() = "$firstName $lastName"

    companion object {
        fun fromEntity(entity: Any): FamilyMemberInfo {
            // TODO: Mapper depuis les entités réelles
            return FamilyMemberInfo(
                id = 1,
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                phone = "+33123456789",
                birthDate = LocalDate.of(1990, 1, 1),
                gender = "M",
                role = UserRole.MEMBER,
                status = MemberStatus.ACTIVE,
                joinDate = LocalDate.now().minusDays(30),
                lastActivity = LocalDateTime.now().minusHours(2),
                fileCount = 15,
                activeDelegations = 2,
                permissions = emptyList()
            )
        }
    }
}

/**
 * Statistiques familiales
 */
data class FamilyStats(
    val totalMembers: Int,
    val activeMembers: Int,
    val adminCount: Int,
    val responsibleCount: Int,
    val memberCount: Int,
    val pendingInvitations: Int,
    val lastActivity: LocalDateTime
) {
    companion object {
        fun fromEntity(entity: Any): FamilyStats {
            // TODO: Mapper depuis les entités réelles
            return FamilyStats(
                totalMembers = 5,
                activeMembers = 4,
                adminCount = 1,
                responsibleCount = 1,
                memberCount = 3,
                pendingInvitations = 1,
                lastActivity = LocalDateTime.now()
            )
        }
    }
}

// ================================================================
// FONCTIONS DE VÉRIFICATION DES PERMISSIONS
// ================================================================

private fun canInviteMembers(userRole: UserRole): Boolean {
    return userRole == UserRole.ADMIN || userRole == UserRole.RESPONSIBLE
}

private fun canEditMember(currentUserRole: UserRole, targetRole: UserRole, isCurrentUser: Boolean): Boolean {
    return when (currentUserRole) {
        UserRole.ADMIN -> true
        UserRole.RESPONSIBLE -> targetRole != UserRole.ADMIN
        UserRole.MEMBER -> isCurrentUser
    }
}

private fun canChangeRole(currentUserRole: UserRole, targetRole: UserRole): Boolean {
    return when (currentUserRole) {
        UserRole.ADMIN -> true
        UserRole.RESPONSIBLE -> targetRole == UserRole.MEMBER
        UserRole.MEMBER -> false
    }
}

private fun canAssignRole(currentUserRole: UserRole, roleToAssign: UserRole): Boolean {
    return when (currentUserRole) {
        UserRole.ADMIN -> true
        UserRole.RESPONSIBLE -> roleToAssign != UserRole.ADMIN
        UserRole.MEMBER -> false
    }
}

private fun canManagePermissions(currentUserRole: UserRole, targetRole: UserRole): Boolean {
    return when (currentUserRole) {
        UserRole.ADMIN -> true
        UserRole.RESPONSIBLE -> targetRole == UserRole.MEMBER
        UserRole.MEMBER -> false
    }
}

private fun canToggleStatus(currentUserRole: UserRole, targetRole: UserRole, isCurrentUser: Boolean): Boolean {
    if (isCurrentUser) return false
    return when (currentUserRole) {
        UserRole.ADMIN -> true
        UserRole.RESPONSIBLE -> targetRole == UserRole.MEMBER
        UserRole.MEMBER -> false
    }
}

private fun canDeleteMember(currentUserRole: UserRole, targetRole: UserRole, isCurrentUser: Boolean): Boolean {
    if (isCurrentUser) return false
    return when (currentUserRole) {
        UserRole.ADMIN -> targetRole != UserRole.ADMIN
        UserRole.RESPONSIBLE -> targetRole == UserRole.MEMBER
        UserRole.MEMBER -> false
    }
}

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")
    return dateTime.format(formatter)
}

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return date.format(formatter)
}

private fun formatRelativeTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val diff = Duration.between(dateTime, now)

    return when {
        diff.toDays() > 7 -> formatDateTime(dateTime)
        diff.toDays() > 0 -> "Il y a ${diff.toDays()} jour${if (diff.toDays() > 1) "s" else ""}"
        diff.toHours() > 0 -> "Il y a ${diff.toHours()} heure${if (diff.toHours() > 1) "s" else ""}"
        diff.toMinutes() > 0 -> "Il y a ${diff.toMinutes()} minute${if (diff.toMinutes() > 1) "s" else ""}"
        else -> "À l'instant"
    }
}