// ================================================================
// FAMILYMEMBERSSCREEN.KT - GESTION DES MEMBRES DE FAMILLE (DESKTOP) - VERSION CORRIGÉE
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
import ktorm.*

/**
 * Écran de gestion des membres de famille pour application desktop
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

    // ✅ CORRIGÉ: Chargement des données avec méthodes existantes
    fun loadFamilyData() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                // ✅ CORRIGÉ: Utilise getFamilyMembers avec familyId au lieu de getAllFamilyMembers
                val membersResult = familyMemberController.getFamilyMembers(1) // TODO: Utiliser le bon familyId
                if (membersResult is FamilyMemberController.FamilyMemberResult.Success<List<MembreFamille>>) {
                    members = membersResult.data.map { FamilyMemberInfo.fromEntity(it) }
                }

                // ✅ CORRIGÉ: Utilise getFamilyMemberStatistics avec familyId au lieu de getFamilyStatistics
                val statsResult = familyMemberController.getFamilyMemberStatistics(1) // TODO: Utiliser le bon familyId
                if (statsResult is FamilyMemberController.FamilyMemberResult.Success<FamilyMemberStatistics>) {
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
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colors.primary)
                                    Text("Chargement des membres...")
                                }
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

    // Autres dialogues...
}

/**
 * ✅ CORRIGÉ: Panneau latéral avec statistiques familiales
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
                    style = MaterialTheme.typography.h6,
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
        }
    }
}

/**
 * ✅ CORRIGÉ: Composant StatsCard
 */
@Composable
private fun StatsCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// Reste des composants avec corrections mineures...

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
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (memberCount == totalCount) {
                        "$totalCount membre${if (totalCount > 1) "s" else ""}"
                    } else {
                        "$memberCount sur $totalCount membre${if (totalCount > 1) "s" else ""}"
                    },
                    style = MaterialTheme.typography.caption,
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
            // Avatar et informations
            Row(
                modifier = Modifier.weight(1f),
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
                            style = MaterialTheme.typography.body1,
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
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Actions
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

// Composants utilitaires...

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
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Détails du membre",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
            }

            item {
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
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = member.role.displayName,
                        style = MaterialTheme.typography.body2,
                        color = member.role.color
                    )
                }
            }

            item {
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
                }
            }
        }
    }
}

@Composable
private fun InviteMemberDialog(
    onConfirm: (String, UserRole) -> Unit,
    onDismiss: () -> Unit,
    currentUserRole: UserRole
) {
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.MEMBER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inviter un nouveau membre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Adresse email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Rôle du nouveau membre", style = MaterialTheme.typography.body2)

                UserRole.values().forEach { role ->
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

                        Text(role.displayName)
                    }
                }
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Envoyer invitation",
                onClick = { onConfirm(email, selectedRole) },
                enabled = email.isNotBlank()
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

// ================================================================
// MODÈLES DE DONNÉES ET UTILITAIRES
// ================================================================

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

enum class MemberStatus(val displayName: String, val color: Color) {
    ACTIVE("Actif", Color(0xFF4CAF50)),
    INACTIVE("Inactif", Color(0xFF9E9E9E)),
    PENDING("En attente", Color(0xFFFF9800)),
    SUSPENDED("Suspendu", Color(0xFFF44336))
}

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
        fun fromEntity(entity: MembreFamille): FamilyMemberInfo {
            return FamilyMemberInfo(
                id = entity.membreFamilleId,
                firstName = entity.prenomMembre,
                lastName = "Membre", // TODO: Ajouter nom de famille dans l'entité
                email = entity.mailMembre,
                phone = null,
                birthDate = entity.dateNaissanceMembre,
                gender = entity.genreMembre.name,
                role = when {
                    entity.estAdmin -> UserRole.ADMIN
                    entity.estResponsable -> UserRole.RESPONSIBLE
                    else -> UserRole.MEMBER
                },
                status = MemberStatus.ACTIVE, // TODO: Ajouter statut dans l'entité
                joinDate = entity.dateAjoutMembre?.toLocalDate() ?: LocalDate.now(),
                lastActivity = LocalDateTime.now(), // TODO: Ajouter dans l'entité
                fileCount = 0, // TODO: Calculer
                activeDelegations = 0, // TODO: Calculer
                permissions = emptyList() // TODO: Charger
            )
        }
    }
}

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
        fun fromEntity(entity: FamilyMemberStatistics): FamilyStats {
            return FamilyStats(
                totalMembers = entity.totalMembers,
                activeMembers = entity.totalMembers, // TODO: Calculer les actifs
                adminCount = entity.adminCount,
                responsibleCount = entity.responsibleCount,
                memberCount = entity.totalMembers - entity.adminCount - entity.responsibleCount,
                pendingInvitations = 0, // TODO: Ajouter
                lastActivity = entity.lastActivity ?: LocalDateTime.now()
            )
        }
    }
}

// Fonctions de permissions
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

private fun canDeleteMember(currentUserRole: UserRole, targetRole: UserRole, isCurrentUser: Boolean): Boolean {
    if (isCurrentUser) return false
    return when (currentUserRole) {
        UserRole.ADMIN -> targetRole != UserRole.ADMIN
        UserRole.RESPONSIBLE -> targetRole == UserRole.MEMBER
        UserRole.MEMBER -> false
    }
}