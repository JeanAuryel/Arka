// ================================================================
// DELEGATIONSSCREEN.KT - GESTION DES DÉLÉGATIONS ARKA (DESKTOP)
// ================================================================

package ui.screens.delegations

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import controllers.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.screens.familymembers.FamilyMemberInfo
import ui.screens.familymembers.UserRole
import ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran de gestion des délégations et permissions pour application desktop
 *
 * Fonctionnalités:
 * - Vue des délégations envoyées et reçues
 * - Création de nouvelles délégations
 * - Approbation/rejet de demandes
 * - Gestion des permissions par utilisateur
 * - Historique des délégations
 * - Délégations temporaires avec expiration
 * - Audit trail des permissions
 */
@Composable
fun DelegationsScreen(
    delegationController: DelegationController,
    familyMemberController: FamilyMemberController,
    currentUserId: Int,
    currentUserRole: UserRole,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // États principaux
    var sentDelegations by remember { mutableStateOf<List<DelegationInfo>>(emptyList()) }
    var receivedDelegations by remember { mutableStateOf<List<DelegationInfo>>(emptyList()) }
    var activeDelegations by remember { mutableStateOf<List<DelegationInfo>>(emptyList()) }
    var familyMembers by remember { mutableStateOf<List<FamilyMemberInfo>>(emptyList()) }
    var selectedDelegation by remember { mutableStateOf<DelegationInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // États UI
    var selectedTab by remember { mutableStateOf(DelegationTab.RECEIVED) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<DelegationStatus?>(null) }
    var filterType by remember { mutableStateOf<DelegationType?>(null) }
    var showDelegationDetails by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(DelegationSortOption.DATE_DESC) }

    // Dialogues
    var showCreateDelegationDialog by remember { mutableStateOf(false) }
    var showApproveDelegationDialog by remember { mutableStateOf(false) }
    var showRevokeDelegationDialog by remember { mutableStateOf(false) }
    var delegationToProcess by remember { mutableStateOf<DelegationInfo?>(null) }

    // Statistiques
    var delegationStats by remember { mutableStateOf<DelegationStats?>(null) }

    // Chargement des données
    fun loadDelegations() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                // Charger les délégations envoyées
                val sentResult = delegationController.getSentDelegations(currentUserId)
                if (sentResult is DelegationController.DelegationResult.Success) {
                    sentDelegations = sentResult.data.map { DelegationInfo.fromEntity(it) }
                }

                // Charger les délégations reçues
                val receivedResult = delegationController.getReceivedDelegations(currentUserId)
                if (receivedResult is DelegationController.DelegationResult.Success) {
                    receivedDelegations = receivedResult.data.map { DelegationInfo.fromEntity(it) }
                }

                // Charger les délégations actives
                val activeResult = delegationController.getActiveDelegations(currentUserId)
                if (activeResult is DelegationController.DelegationResult.Success) {
                    activeDelegations = activeResult.data.map { DelegationInfo.fromEntity(it) }
                }

                // Charger les membres de famille
                val membersResult = familyMemberController.getAllFamilyMembers()
                if (membersResult is FamilyMemberController.FamilyMemberResult.Success) {
                    familyMembers = membersResult.data.map { FamilyMemberInfo.Companion.fromEntity(it) }
                }

                // Charger les statistiques
                val statsResult = delegationController.getDelegationStatistics(currentUserId)
                if (statsResult is DelegationController.DelegationResult.Success) {
                    delegationStats = DelegationStats.fromEntity(statsResult.data)
                }

            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Obtenir les délégations selon l'onglet sélectionné
    val currentDelegations = remember(selectedTab, sentDelegations, receivedDelegations, activeDelegations) {
        when (selectedTab) {
            DelegationTab.SENT -> sentDelegations
            DelegationTab.RECEIVED -> receivedDelegations
            DelegationTab.ACTIVE -> activeDelegations
            DelegationTab.ALL -> (sentDelegations + receivedDelegations + activeDelegations).distinctBy { it.id }
        }
    }

    // Filtrage et tri
    val filteredAndSortedDelegations = remember(currentDelegations, searchQuery, filterStatus, filterType, sortOption) {
        currentDelegations
            .filter { delegation ->
                if (searchQuery.isNotBlank()) {
                    delegation.fromUser.contains(searchQuery, ignoreCase = true) ||
                            delegation.toUser.contains(searchQuery, ignoreCase = true) ||
                            delegation.permissions.any { it.contains(searchQuery, ignoreCase = true) }
                } else true
            }
            .filter { delegation ->
                filterStatus?.let { it == delegation.status } ?: true
            }
            .filter { delegation ->
                filterType?.let { it == delegation.type } ?: true
            }
            .sortedWith { a, b ->
                when (sortOption) {
                    DelegationSortOption.DATE_ASC -> a.createdDate.compareTo(b.createdDate)
                    DelegationSortOption.DATE_DESC -> b.createdDate.compareTo(a.createdDate)
                    DelegationSortOption.STATUS_ASC -> a.status.ordinal.compareTo(b.status.ordinal)
                    DelegationSortOption.STATUS_DESC -> b.status.ordinal.compareTo(a.status.ordinal)
                    DelegationSortOption.USER_ASC -> a.fromUser.compareTo(b.fromUser, ignoreCase = true)
                    DelegationSortOption.USER_DESC -> b.fromUser.compareTo(a.fromUser, ignoreCase = true)
                    DelegationSortOption.EXPIRY_ASC -> (a.expiryDate ?: LocalDateTime.MAX).compareTo(b.expiryDate ?: LocalDateTime.MAX)
                    DelegationSortOption.EXPIRY_DESC -> (b.expiryDate ?: LocalDateTime.MIN).compareTo(a.expiryDate ?: LocalDateTime.MIN)
                }
            }
    }

    // Chargement initial
    LaunchedEffect(Unit) {
        loadDelegations()
    }

    // Layout principal
    Row(modifier = Modifier.fillMaxSize()) {
        // Panneau latéral avec statistiques (300dp)
        DelegationStatsPanel(
            stats = delegationStats,
            currentTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onCreateDelegation = if (canCreateDelegation(currentUserRole)) {
                { showCreateDelegationDialog = true }
            } else null,
            modifier = Modifier.width(300.dp)
        )

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

        // Panneau principal
        Column(modifier = Modifier.weight(1f)) {
            // Barre d'outils
            DelegationsToolbar(
                selectedTab = selectedTab,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                filterStatus = filterStatus,
                onFilterStatusChange = { filterStatus = it },
                filterType = filterType,
                onFilterTypeChange = { filterType = it },
                sortOption = sortOption,
                onSortChange = { sortOption = it },
                onCreateDelegation = if (canCreateDelegation(currentUserRole)) {
                    { showCreateDelegationDialog = true }
                } else null,
                onRefresh = { loadDelegations() },
                delegationCount = filteredAndSortedDelegations.size,
                totalCount = currentDelegations.size
            )

            Row(modifier = Modifier.weight(1f)) {
                // Zone principale des délégations
                Box(
                    modifier = Modifier
                        .weight(if (showDelegationDetails) 0.65f else 1f)
                        .fillMaxHeight()
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ArkaLoadingIndicator(message = "Chargement des délégations...")
                            }
                        }

                        errorMessage != null -> {
                            ErrorDisplay(
                                message = errorMessage!!,
                                onRetry = { loadDelegations() }
                            )
                        }

                        filteredAndSortedDelegations.isEmpty() -> {
                            EmptyDelegationsView(
                                selectedTab = selectedTab,
                                hasDelegations = currentDelegations.isNotEmpty(),
                                searchQuery = searchQuery,
                                onClearSearch = { searchQuery = "" },
                                onCreateDelegation = if (canCreateDelegation(currentUserRole)) {
                                    { showCreateDelegationDialog = true }
                                } else null
                            )
                        }

                        else -> {
                            DelegationsList(
                                delegations = filteredAndSortedDelegations,
                                selectedDelegation = selectedDelegation,
                                currentUserId = currentUserId,
                                currentUserRole = currentUserRole,
                                onDelegationClick = { delegation ->
                                    selectedDelegation = delegation
                                    showDelegationDetails = true
                                },
                                onApproveDelegation = { delegation ->
                                    delegationToProcess = delegation
                                    showApproveDelegationDialog = true
                                },
                                onRejectDelegation = { delegation ->
                                    scope.launch {
                                        try {
                                            // TODO: Rejeter délégation
                                            loadDelegations()
                                        } catch (e: Exception) {
                                            errorMessage = "Erreur lors du rejet: ${e.message}"
                                        }
                                    }
                                },
                                onRevokeDelegation = { delegation ->
                                    delegationToProcess = delegation
                                    showRevokeDelegationDialog = true
                                }
                            )
                        }
                    }
                }

                // Panneau de détails (si activé)
                if (showDelegationDetails && selectedDelegation != null) {
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

                    DelegationDetailsPanel(
                        delegation = selectedDelegation!!,
                        currentUserId = currentUserId,
                        currentUserRole = currentUserRole,
                        onClose = { showDelegationDetails = false },
                        onApprove = {
                            delegationToProcess = selectedDelegation
                            showApproveDelegationDialog = true
                        },
                        onReject = {
                            scope.launch {
                                try {
                                    // TODO: Rejeter délégation
                                    loadDelegations()
                                } catch (e: Exception) {
                                    errorMessage = "Erreur lors du rejet: ${e.message}"
                                }
                            }
                        },
                        onRevoke = {
                            delegationToProcess = selectedDelegation
                            showRevokeDelegationDialog = true
                        },
                        modifier = Modifier.width(350.dp)
                    )
                }
            }
        }
    }

    // Dialogues
    if (showCreateDelegationDialog) {
        CreateDelegationDialog(
            familyMembers = familyMembers.filter { it.id != currentUserId },
            onConfirm = { toUserId, permissions, expiryDate, message ->
                scope.launch {
                    try {
                        // TODO: Créer délégation
                        loadDelegations()
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de la création: ${e.message}"
                    }
                }
                showCreateDelegationDialog = false
            },
            onDismiss = { showCreateDelegationDialog = false }
        )
    }

    if (showApproveDelegationDialog && delegationToProcess != null) {
        ApproveDelegationDialog(
            delegation = delegationToProcess!!,
            onConfirm = { conditions ->
                scope.launch {
                    try {
                        // TODO: Approuver délégation avec conditions
                        loadDelegations()
                        if (selectedDelegation?.id == delegationToProcess?.id) {
                            selectedDelegation = null
                            showDelegationDetails = false
                        }
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de l'approbation: ${e.message}"
                    }
                }
                showApproveDelegationDialog = false
                delegationToProcess = null
            },
            onDismiss = {
                showApproveDelegationDialog = false
                delegationToProcess = null
            }
        )
    }

    if (showRevokeDelegationDialog && delegationToProcess != null) {
        RevokeDelegationDialog(
            delegation = delegationToProcess!!,
            onConfirm = { reason ->
                scope.launch {
                    try {
                        // TODO: Révoquer délégation
                        loadDelegations()
                        if (selectedDelegation?.id == delegationToProcess?.id) {
                            selectedDelegation = null
                            showDelegationDetails = false
                        }
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de la révocation: ${e.message}"
                    }
                }
                showRevokeDelegationDialog = false
                delegationToProcess = null
            },
            onDismiss = {
                showRevokeDelegationDialog = false
                delegationToProcess = null
            }
        )
    }
}

/**
 * Panneau latéral avec statistiques et navigation
 */
@Composable
private fun DelegationStatsPanel(
    stats: DelegationStats?,
    currentTab: DelegationTab,
    onTabSelected: (DelegationTab) -> Unit,
    onCreateDelegation: (() -> Unit)?,
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
                    text = "Délégations",
                    style = ArkaTextStyles.cardTitle,
                    fontWeight = FontWeight.Bold
                )
            }

            // Navigation par onglets
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DelegationTab.values().forEach { tab ->
                        DelegationTabItem(
                            tab = tab,
                            isSelected = currentTab == tab,
                            count = when (tab) {
                                DelegationTab.RECEIVED -> stats?.pendingReceived ?: 0
                                DelegationTab.SENT -> stats?.pendingSent ?: 0
                                DelegationTab.ACTIVE -> stats?.activeDelegations ?: 0
                                DelegationTab.ALL -> stats?.totalDelegations ?: 0
                            },
                            onClick = { onTabSelected(tab) }
                        )
                    }
                }
            }

            if (stats != null) {
                item {
                    Divider()
                }

                item {
                    Text(
                        text = "Statistiques",
                        style = ArkaTextStyles.cardDescription,
                        fontWeight = FontWeight.Medium
                    )
                }

                item {
                    StatsCard(
                        title = "En attente",
                        value = "${stats.pendingReceived}",
                        subtitle = "Demandes à traiter",
                        icon = Icons.Default.PendingActions,
                        color = Color(0xFFFF9800)
                    )
                }

                item {
                    StatsCard(
                        title = "Actives",
                        value = "${stats.activeDelegations}",
                        subtitle = "Délégations en cours",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50)
                    )
                }

                item {
                    StatsCard(
                        title = "Expirent bientôt",
                        value = "${stats.expiringSoon}",
                        subtitle = "Dans les 7 prochains jours",
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFF44336)
                    )
                }

                item {
                    StatsCard(
                        title = "Révoquées",
                        value = "${stats.revokedDelegations}",
                        subtitle = "Ce mois",
                        icon = Icons.Default.Cancel,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }

            if (onCreateDelegation != null) {
                item {
                    Divider()
                }

                item {
                    ArkaButton(
                        text = "Nouvelle délégation",
                        onClick = onCreateDelegation,
                        icon = Icons.Default.Add,
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
                        text = "Historique complet",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.History,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ArkaOutlinedButton(
                        text = "Modèles de délégation",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.Template,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ArkaOutlinedButton(
                        text = "Rapport d'audit",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.Assessment,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Item d'onglet de délégation
 */
@Composable
private fun DelegationTabItem(
    tab: DelegationTab,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent,
        shape = ArkaComponentShapes.cardSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = tab.displayName,
                    style = ArkaTextStyles.navigation,
                    color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                )
            }

            if (count > 0) {
                StatusBadge(
                    text = count.toString(),
                    backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                    textColor = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

/**
 * Barre d'outils de gestion des délégations
 */
@Composable
private fun DelegationsToolbar(
    selectedTab: DelegationTab,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: DelegationStatus?,
    onFilterStatusChange: (DelegationStatus?) -> Unit,
    filterType: DelegationType?,
    onFilterTypeChange: (DelegationType?) -> Unit,
    sortOption: DelegationSortOption,
    onSortChange: (DelegationSortOption) -> Unit,
    onCreateDelegation: (() -> Unit)?,
    onRefresh: () -> Unit,
    delegationCount: Int,
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
                    text = selectedTab.displayName,
                    style = ArkaTextStyles.cardTitle,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (delegationCount == totalCount) {
                        "$totalCount délégation${if (totalCount > 1) "s" else ""}"
                    } else {
                        "$delegationCount sur $totalCount délégation${if (totalCount > 1) "s" else ""}"
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
                placeholder = { Text("Rechercher...") },
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
                modifier = Modifier.width(250.dp)
            )

            // Filtres
            var showFilters by remember { mutableStateOf(false) }
            IconButton(onClick = { showFilters = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtres",
                    tint = if (filterStatus != null || filterType != null) MaterialTheme.colors.primary
                    else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            // Menu des filtres
            DropdownMenu(
                expanded = showFilters,
                onDismissRequest = { showFilters = false }
            ) {
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

                DelegationStatus.values().forEach { status ->
                    DropdownMenuItem(onClick = {
                        onFilterStatusChange(status)
                        showFilters = false
                    }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DelegationStatusIndicator(status = status, size = 8.dp)
                            Text(status.displayName)
                            if (filterStatus == status) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Divider()

                Text(
                    text = "Filtrer par type",
                    style = ArkaTextStyles.metadata,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                DropdownMenuItem(onClick = {
                    onFilterTypeChange(null)
                    showFilters = false
                }) {
                    Text("Tous les types")
                    if (filterType == null) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                DelegationType.values().forEach { type ->
                    DropdownMenuItem(onClick = {
                        onFilterTypeChange(type)
                        showFilters = false
                    }) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = type.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(type.displayName)
                            if (filterType == type) {
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
                DelegationSortOption.values().forEach { sort ->
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
            if (onCreateDelegation != null) {
                ArkaButton(
                    text = "Nouvelle",
                    onClick = onCreateDelegation,
                    icon = Icons.Default.Add
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
            }
        }
    }
}

/**
 * Liste des délégations
 */
@Composable
private fun DelegationsList(
    delegations: List<DelegationInfo>,
    selectedDelegation: DelegationInfo?,
    currentUserId: Int,
    currentUserRole: UserRole,
    onDelegationClick: (DelegationInfo) -> Unit,
    onApproveDelegation: (DelegationInfo) -> Unit,
    onRejectDelegation: (DelegationInfo) -> Unit,
    onRevokeDelegation: (DelegationInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // En-tête des colonnes
        item {
            DelegationsListHeader()
        }

        // Liste des délégations
        items(delegations) { delegation ->
            DelegationListItem(
                delegation = delegation,
                isSelected = selectedDelegation?.id == delegation.id,
                currentUserId = currentUserId,
                currentUserRole = currentUserRole,
                onClick = { onDelegationClick(delegation) },
                onApprove = { onApproveDelegation(delegation) },
                onReject = { onRejectDelegation(delegation) },
                onRevoke = { onRevokeDelegation(delegation) }
            )
        }
    }
}

/**
 * En-tête de la liste des délégations
 */
@Composable
private fun DelegationsListHeader() {
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
                text = "De/Vers",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.2f)
            )

            Text(
                text = "Permissions",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.25f)
            )

            Text(
                text = "Type",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.1f)
            )

            Text(
                text = "Statut",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.1f)
            )

            Text(
                text = "Créée",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.15f)
            )

            Text(
                text = "Expire",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.1f)
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
 * Item de délégation dans la liste
 */
@Composable
private fun DelegationListItem(
    delegation: DelegationInfo,
    isSelected: Boolean,
    currentUserId: Int,
    currentUserRole: UserRole,
    onClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRevoke: () -> Unit
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
            // De/Vers utilisateur
            Column(
                modifier = Modifier.weight(0.2f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (delegation.fromUserId == currentUserId) Icons.Default.CallMade else Icons.Default.CallReceived,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (delegation.fromUserId == currentUserId) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    )

                    Text(
                        text = if (delegation.fromUserId == currentUserId) delegation.toUser else delegation.fromUser,
                        style = ArkaTextStyles.cardDescription,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = if (delegation.fromUserId == currentUserId) "Vers" else "De",
                    style = ArkaTextStyles.helpText,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            // Permissions
            Column(
                modifier = Modifier.weight(0.25f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = delegation.permissions.take(2).joinToString(", "),
                    style = ArkaTextStyles.metadata,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (delegation.permissions.size > 2) {
                    Text(
                        text = "+${delegation.permissions.size - 2} autres",
                        style = ArkaTextStyles.helpText,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Type
            Row(
                modifier = Modifier.weight(0.1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = delegation.type.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = delegation.type.color
                )

                Text(
                    text = delegation.type.shortName,
                    style = ArkaTextStyles.metadata,
                    color = delegation.type.color
                )
            }

            // Statut
            Row(
                modifier = Modifier.weight(0.1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DelegationStatusIndicator(
                    status = delegation.status,
                    size = 8.dp
                )

                Text(
                    text = delegation.status.shortName,
                    style = ArkaTextStyles.metadata,
                    color = delegation.status.color
                )
            }

            // Date de création
            Text(
                text = formatDate(delegation.createdDate),
                style = ArkaTextStyles.date,
                modifier = Modifier.weight(0.15f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Date d'expiration
            Text(
                text = delegation.expiryDate?.let { formatDate(it) } ?: "Permanente",
                style = ArkaTextStyles.date,
                modifier = Modifier.weight(0.1f),
                color = if (delegation.isExpiringSoon()) Color(0xFFF44336)
                else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Actions
            Row(
                modifier = Modifier.weight(0.1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when {
                    delegation.status == DelegationStatus.PENDING && delegation.toUserId == currentUserId -> {
                        // Actions pour approuver/rejeter
                        IconButton(
                            onClick = onApprove,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Approuver",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4CAF50)
                            )
                        }

                        IconButton(
                            onClick = onReject,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Rejeter",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colors.error
                            )
                        }
                    }

                    delegation.status == DelegationStatus.APPROVED &&
                            (delegation.fromUserId == currentUserId || canRevokeDelegation(currentUserRole)) -> {
                        // Action pour révoquer
                        IconButton(
                            onClick = onRevoke,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Révoquer",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colors.error
                            )
                        }
                    }

                    else -> {
                        // Pas d'actions disponibles
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Vue vide quand aucune délégation ne correspond aux critères
 */
@Composable
private fun EmptyDelegationsView(
    selectedTab: DelegationTab,
    hasDelegations: Boolean,
    searchQuery: String,
    onClearSearch: () -> Unit,
    onCreateDelegation: (() -> Unit)?
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
                imageVector = if (hasDelegations) Icons.Default.SearchOff else selectedTab.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                text = when {
                    !hasDelegations -> "Aucune délégation ${selectedTab.displayName.lowercase()}"
                    searchQuery.isNotEmpty() -> "Aucune délégation trouvée pour \"$searchQuery\""
                    else -> "Aucune délégation dans cette catégorie"
                },
                style = ArkaTextStyles.cardTitle,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = when {
                    !hasDelegations && selectedTab == DelegationTab.RECEIVED -> "Vous n'avez pas encore reçu de demandes de délégation"
                    !hasDelegations && selectedTab == DelegationTab.SENT -> "Vous n'avez pas encore envoyé de demandes de délégation"
                    !hasDelegations && selectedTab == DelegationTab.ACTIVE -> "Aucune délégation n'est actuellement active"
                    searchQuery.isNotEmpty() -> "Essayez de modifier votre recherche"
                    else -> "Aucune délégation ne correspond aux filtres sélectionnés"
                },
                style = ArkaTextStyles.cardDescription,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )

            when {
                searchQuery.isNotEmpty() -> {
                    ArkaOutlinedButton(
                        text = "Effacer la recherche",
                        onClick = onClearSearch,
                        icon = Icons.Default.Clear
                    )
                }
                !hasDelegations && onCreateDelegation != null -> {
                    ArkaButton(
                        text = "Créer ma première délégation",
                        onClick = onCreateDelegation,
                        icon = Icons.Default.Add
                    )
                }
            }
        }
    }
}

/**
 * Panneau de détails d'une délégation
 */
@Composable
private fun DelegationDetailsPanel(
    delegation: DelegationInfo,
    currentUserId: Int,
    currentUserRole: UserRole,
    onClose: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRevoke: () -> Unit,
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
                        text = "Détails de la délégation",
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
                // Informations principales
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Statut et type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DelegationStatusIndicator(
                                status = delegation.status,
                                size = 12.dp
                            )

                            Text(
                                text = delegation.status.displayName,
                                style = ArkaTextStyles.cardDescription,
                                color = delegation.status.color,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = delegation.type.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = delegation.type.color
                            )

                            Text(
                                text = delegation.type.displayName,
                                style = ArkaTextStyles.cardDescription,
                                color = delegation.type.color
                            )
                        }
                    }

                    // Direction de la délégation
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = delegation.fromUser,
                            size = 32.dp
                        )

                        Icon(
                            imageVector = Icons.Default.ArrowRightAlt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )

                        UserAvatar(
                            name = delegation.toUser,
                            size = 32.dp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = delegation.fromUser,
                            style = ArkaTextStyles.cardDescription,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = delegation.toUser,
                            style = ArkaTextStyles.cardDescription,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                DelegationDetailSection(
                    title = "Informations",
                    items = listOf(
                        "Demande créée" to formatDateTime(delegation.createdDate),
                        "Expire le" to (delegation.expiryDate?.let { formatDateTime(it) } ?: "Jamais"),
                        "Dernière activité" to formatDateTime(delegation.lastActivity),
                        "Raison" to (delegation.reason ?: "Non spécifiée")
                    )
                )
            }

            item {
                // Permissions accordées
                Text(
                    text = "Permissions accordées",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    delegation.permissions.forEach { permission ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getPermissionIcon(permission),
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

            if (delegation.conditions.isNotEmpty()) {
                item {
                    DelegationDetailSection(
                        title = "Conditions",
                        items = delegation.conditions.map { it to "" }
                    )
                }
            }

            item {
                Divider()
            }

            item {
                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        delegation.status == DelegationStatus.PENDING && delegation.toUserId == currentUserId -> {
                            ArkaButton(
                                text = "Approuver",
                                onClick = onApprove,
                                icon = Icons.Default.Check,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                            )

                            ArkaOutlinedButton(
                                text = "Rejeter",
                                onClick = onReject,
                                icon = Icons.Default.Close,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        delegation.status == DelegationStatus.APPROVED &&
                                (delegation.fromUserId == currentUserId || canRevokeDelegation(currentUserRole)) -> {
                            ArkaOutlinedButton(
                                text = "Révoquer délégation",
                                onClick = onRevoke,
                                icon = Icons.Default.Cancel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    ArkaOutlinedButton(
                        text = "Voir l'historique",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.History,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Section de détails d'une délégation
 */
@Composable
private fun DelegationDetailSection(
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
            if (value.isNotEmpty()) {
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
            } else {
                Text(
                    text = label,
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

/**
 * Indicateur de statut de délégation
 */
@Composable
private fun DelegationStatusIndicator(
    status: DelegationStatus,
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
// DIALOGUES
// ================================================================

/**
 * Dialogue de création de délégation
 */
@Composable
private fun CreateDelegationDialog(
    familyMembers: List<FamilyMemberInfo>,
    onConfirm: (Int, List<String>, LocalDateTime?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUserId by remember { mutableStateOf<Int?>(null) }
    var selectedPermissions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasExpiry by remember { mutableStateOf(false) }
    var expiryDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var message by remember { mutableStateOf("") }

    val availablePermissions = listOf(
        "READ_FILES" to "Lire les fichiers",
        "WRITE_FILES" to "Modifier les fichiers",
        "DELETE_FILES" to "Supprimer les fichiers",
        "UPLOAD_FILES" to "Téléverser des fichiers",
        "MANAGE_FOLDERS" to "Gérer les dossiers",
        "SHARE_FILES" to "Partager les fichiers",
        "VIEW_ACTIVITY" to "Voir l'activité"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une nouvelle délégation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Sélection du destinataire
                Text(
                    text = "Déléguer à",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                familyMembers.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedUserId = member.id }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedUserId == member.id,
                            onClick = { selectedUserId = member.id }
                        )

                        UserAvatar(
                            name = member.fullName,
                            size = 24.dp
                        )

                        Column {
                            Text(
                                text = member.fullName,
                                style = ArkaTextStyles.cardDescription,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = member.role.displayName,
                                style = ArkaTextStyles.helpText,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Divider()

                // Sélection des permissions
                Text(
                    text = "Permissions à déléguer",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
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

                Divider()

                // Expiration
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hasExpiry = !hasExpiry }
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasExpiry,
                        onCheckedChange = { hasExpiry = it }
                    )

                    Text(
                        text = "Délégation temporaire",
                        style = ArkaTextStyles.cardDescription
                    )
                }

                if (hasExpiry) {
                    // TODO: Sélecteur de date d'expiration
                    Text(
                        text = "Expire le: ${LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                        style = ArkaTextStyles.helpText
                    )
                }

                // Message optionnel
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message (optionnel)") },
                    placeholder = { Text("Expliquez pourquoi vous demandez cette délégation") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Créer délégation",
                onClick = {
                    onConfirm(
                        selectedUserId!!,
                        selectedPermissions.toList(),
                        if (hasExpiry) LocalDateTime.now().plusDays(7) else null,
                        message.takeIf { it.isNotBlank() }
                    )
                },
                enabled = selectedUserId != null && selectedPermissions.isNotEmpty(),
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
 * Dialogue d'approbation de délégation
 */
@Composable
private fun ApproveDelegationDialog(
    delegation: DelegationInfo,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var conditions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approuver la délégation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Approuver la demande de délégation de ${delegation.fromUser} ?"
                )

                Text(
                    text = "Permissions demandées :",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                delegation.permissions.forEach { permission ->
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

                OutlinedTextField(
                    value = conditions,
                    onValueChange = { conditions = it },
                    label = { Text("Conditions (optionnelles)") },
                    placeholder = { Text("Ajoutez des conditions à cette délégation") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Approuver",
                onClick = {
                    onConfirm(if (conditions.isNotBlank()) listOf(conditions) else emptyList())
                },
                icon = Icons.Default.Check,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
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
 * Dialogue de révocation de délégation
 */
@Composable
private fun RevokeDelegationDialog(
    delegation: DelegationInfo,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Révoquer la délégation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Êtes-vous sûr de vouloir révoquer cette délégation ?"
                )

                Text(
                    text = "Cette action supprimera immédiatement les permissions accordées à ${delegation.toUser}.",
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Raison de la révocation (optionnelle)") },
                    placeholder = { Text("Expliquez pourquoi vous révoquez cette délégation") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Révoquer",
                onClick = {
                    onConfirm(reason.takeIf { it.isNotBlank() })
                },
                icon = Icons.Default.Cancel,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
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

// ================================================================
// MODÈLES DE DONNÉES ET UTILITAIRES
// ================================================================

/**
 * Onglets de délégation
 */
enum class DelegationTab(
    val displayName: String,
    val icon: ImageVector
) {
    RECEIVED("Demandes reçues", Icons.Default.Inbox),
    SENT("Demandes envoyées", Icons.Default.Send),
    ACTIVE("Délégations actives", Icons.Default.CheckCircle),
    ALL("Toutes", Icons.Default.List)
}

/**
 * Statuts de délégation
 */
enum class DelegationStatus(
    val displayName: String,
    val shortName: String,
    val color: Color
) {
    PENDING("En attente", "Attente", Color(0xFFFF9800)),
    APPROVED("Approuvée", "Active", Color(0xFF4CAF50)),
    REJECTED("Rejetée", "Rejetée", Color(0xFFF44336)),
    REVOKED("Révoquée", "Révoquée", Color(0xFF9E9E9E)),
    EXPIRED("Expirée", "Expirée", Color(0xFF795548))
}

/**
 * Types de délégation
 */
enum class DelegationType(
    val displayName: String,
    val shortName: String,
    val icon: ImageVector,
    val color: Color
) {
    TEMPORARY("Temporaire", "Temp", Icons.Default.Schedule, Color(0xFFFF9800)),
    PERMANENT("Permanente", "Perm", Icons.Default.Security, Color(0xFF2196F3)),
    CONDITIONAL("Conditionnelle", "Cond", Icons.Default.Rule, Color(0xFF9C27B0)),
    EMERGENCY("Urgence", "Urg", Icons.Default.Warning, Color(0xFFF44336))
}

/**
 * Options de tri pour les délégations
 */
enum class DelegationSortOption(
    val displayName: String,
    val icon: ImageVector
) {
    DATE_ASC("Plus anciennes", Icons.Default.DateRange),
    DATE_DESC("Plus récentes", Icons.Default.DateRange),
    STATUS_ASC("Statut (A-Z)", Icons.Default.Sort),
    STATUS_DESC("Statut (Z-A)", Icons.Default.Sort),
    USER_ASC("Utilisateur (A-Z)", Icons.Default.Person),
    USER_DESC("Utilisateur (Z-A)", Icons.Default.Person),
    EXPIRY_ASC("Expire bientôt", Icons.Default.Schedule),
    EXPIRY_DESC("Expire tardivement", Icons.Default.Schedule)
}

/**
 * Informations d'une délégation
 */
data class DelegationInfo(
    val id: Int,
    val fromUserId: Int,
    val fromUser: String,
    val toUserId: Int,
    val toUser: String,
    val permissions: List<String>,
    val type: DelegationType,
    val status: DelegationStatus,
    val createdDate: LocalDateTime,
    val expiryDate: LocalDateTime?,
    val lastActivity: LocalDateTime,
    val reason: String?,
    val conditions: List<String>
) {
    fun isExpiringSoon(): Boolean {
        return expiryDate?.let {
            it.isBefore(LocalDateTime.now().plusDays(7))
        } ?: false
    }

    companion object {
        fun fromEntity(entity: Any): DelegationInfo {
            // TODO: Mapper depuis les entités réelles
            return DelegationInfo(
                id = 1,
                fromUserId = 2,
                fromUser = "John Doe",
                toUserId = 3,
                toUser = "Jane Smith",
                permissions = listOf("READ_FILES", "WRITE_FILES"),
                type = DelegationType.TEMPORARY,
                status = DelegationStatus.PENDING,
                createdDate = LocalDateTime.now().minusDays(1),
                expiryDate = LocalDateTime.now().plusDays(7),
                lastActivity = LocalDateTime.now().minusHours(2),
                reason = "Besoin d'accéder aux documents pendant les vacances",
                conditions = emptyList()
            )
        }
    }
}

/**
 * Statistiques des délégations
 */
data class DelegationStats(
    val totalDelegations: Int,
    val pendingReceived: Int,
    val pendingSent: Int,
    val activeDelegations: Int,
    val expiringSoon: Int,
    val revokedDelegations: Int
) {
    companion object {
        fun fromEntity(entity: Any): DelegationStats {
            // TODO: Mapper depuis les entités réelles
            return DelegationStats(
                totalDelegations = 15,
                pendingReceived = 3,
                pendingSent = 1,
                activeDelegations = 8,
                expiringSoon = 2,
                revokedDelegations = 1
            )
        }
    }
}

// ================================================================
// FONCTIONS DE VÉRIFICATION DES PERMISSIONS
// ================================================================

private fun canCreateDelegation(userRole: UserRole): Boolean {
    return true // Tous les utilisateurs peuvent créer des délégations
}

private fun canRevokeDelegation(userRole: UserRole): Boolean {
    return userRole == UserRole.ADMIN || userRole == UserRole.RESPONSIBLE
}

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun getPermissionIcon(permission: String): ImageVector {
    return when (permission) {
        "READ_FILES" -> Icons.Default.Visibility
        "WRITE_FILES" -> Icons.Default.Edit
        "DELETE_FILES" -> Icons.Default.Delete
        "UPLOAD_FILES" -> Icons.Default.CloudUpload
        "MANAGE_FOLDERS" -> Icons.Default.Folder
        "SHARE_FILES" -> Icons.Default.Share
        "VIEW_ACTIVITY" -> Icons.Default.History
        else -> Icons.Default.Security
    }
}

private fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")
    return dateTime.format(formatter)
}

private fun formatDate(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return dateTime.format(formatter)
}