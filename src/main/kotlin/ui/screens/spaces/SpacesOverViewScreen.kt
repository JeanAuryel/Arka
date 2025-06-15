// ================================================================
// SPACESOVERVIEWSCREEN.KT - ÉCRAN NAVIGATION DES ESPACES ARKA
// ================================================================

package ui.screens.spaces

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
import ui.screens.files.ViewMode
import ui.theme.*

/**
 * Écran de navigation des espaces Arka
 *
 * Fonctionnalités:
 * - Vue d'ensemble de tous les espaces accessibles
 * - Espaces personnels, familiaux et partagés
 * - Création de nouveaux espaces
 * - Recherche et filtrage des espaces
 * - Gestion des permissions par espace
 * - Statistiques d'utilisation
 * - Demandes d'accès en attente
 */
@Composable
fun SpacesOverviewScreen(
    spaceController: SpaceController,
    permissionController: PermissionController,
    authController: AuthController,
    onNavigateToSpace: (Int) -> Unit,
    onNavigateToCreateSpace: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var personalSpaces by remember { mutableStateOf<List<EspaceOverview>>(emptyList()) }
    var familySpaces by remember { mutableStateOf<List<EspaceOverview>>(emptyList()) }
    var sharedSpaces by remember { mutableStateOf<List<EspaceOverview>>(emptyList()) }
    var recentActivity by remember { mutableStateOf<List<SpaceActivity>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<AccessRequest>>(emptyList()) }
    var spacesStats by remember { mutableStateOf<SpacesStatistics?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SpaceFilter.ALL) }
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Charger les données des espaces
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            scope.launch {
                isLoading = true
                try {
                    // Charger les espaces personnels
                    val personalResult = spaceController.getPersonalSpaces(currentUser.membreFamilleId)
                    if (personalResult is SpaceController.SpaceResult.Success) {
                        personalSpaces = personalResult.data
                    }

                    // Charger les espaces familiaux
                    val familyResult = spaceController.getFamilySpaces(currentUser.familleId)
                    if (familyResult is SpaceController.SpaceResult.Success) {
                        familySpaces = familyResult.data
                    }

                    // Charger les espaces partagés
                    val sharedResult = spaceController.getSharedSpaces(currentUser.membreFamilleId)
                    if (sharedResult is SpaceController.SpaceResult.Success) {
                        sharedSpaces = sharedResult.data
                    }

                    // Charger l'activité récente
                    val activityResult = spaceController.getRecentSpaceActivity(currentUser.membreFamilleId, 10)
                    if (activityResult is SpaceController.SpaceResult.Success) {
                        recentActivity = activityResult.data
                    }

                    // Charger les demandes en attente
                    val requestsResult = permissionController.getPendingAccessRequests(currentUser.membreFamilleId)
                    if (requestsResult is PermissionController.PermissionResult.Success) {
                        pendingRequests = requestsResult.data
                    }

                    // Charger les statistiques
                    val statsResult = spaceController.getSpacesStatistics(currentUser.membreFamilleId)
                    if (statsResult is SpaceController.SpaceResult.Success) {
                        spacesStats = statsResult.data
                    }

                } catch (e: Exception) {
                    errorMessage = "Erreur lors du chargement: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Filtrer les espaces
    val allSpaces = personalSpaces + familySpaces + sharedSpaces
    val filteredSpaces = remember(allSpaces, searchQuery, selectedFilter) {
        allSpaces.filter { space ->
            val matchesSearch = searchQuery.isEmpty() ||
                    space.nom.contains(searchQuery, ignoreCase = true) ||
                    space.description?.contains(searchQuery, ignoreCase = true) == true

            val matchesFilter = when (selectedFilter) {
                SpaceFilter.ALL -> true
                SpaceFilter.PERSONAL -> space.type == "PERSONNEL"
                SpaceFilter.FAMILY -> space.type == "FAMILIAL"
                SpaceFilter.SHARED -> space.type == "PARTAGE"
                SpaceFilter.ACCESSIBLE -> space.hasAccess
                SpaceFilter.MANAGED -> space.canManage
            }

            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barre de navigation supérieure
        ArkaTopAppBar(
            title = "Mes espaces",
            subtitle = "${allSpaces.size} espace${if (allSpaces.size > 1) "s" else ""} disponible${if (allSpaces.size > 1) "s" else ""}",
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
            actions = {
                // Recherche
                var showSearch by remember { mutableStateOf(false) }

                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher un espace...") },
                        modifier = Modifier.width(250.dp),
                        singleLine = true,
                        trailingIcon = {
                            ArkaIconButton(
                                icon = Icons.Default.Close,
                                onClick = {
                                    showSearch = false
                                    searchQuery = ""
                                }
                            )
                        }
                    )
                } else {
                    ArkaIconButton(
                        icon = Icons.Default.Search,
                        onClick = { showSearch = true },
                        tooltip = "Rechercher"
                    )
                }

                // Notifications de demandes en attente
                if (pendingRequests.isNotEmpty()) {
                    BadgedBox(
                        badge = {
                            Badge { Text("${pendingRequests.size}") }
                        }
                    ) {
                        ArkaIconButton(
                            icon = Icons.Default.Notifications,
                            onClick = onNavigateToPermissions,
                            tooltip = "Demandes en attente"
                        )
                    }
                }

                // Mode d'affichage
                ArkaIconButton(
                    icon = if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                    onClick = {
                        viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                    },
                    tooltip = if (viewMode == ViewMode.GRID) "Vue liste" else "Vue grille"
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
                        DropdownMenuItem(onClick = {
                            showMenu = false
                            showCreateDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Créer un espace")
                        }

                        DropdownMenuItem(onClick = {
                            showMenu = false
                            onNavigateToPermissions()
                        }) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Gérer les permissions")
                        }

                        DropdownMenuItem(onClick = {
                            showMenu = false
                            // TODO: Exporter la liste
                        }) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Exporter la liste")
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
                        message = "Chargement des espaces..."
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Statistiques rapides
                    item {
                        SpacesStatsCard(
                            stats = spacesStats,
                            personalCount = personalSpaces.size,
                            familyCount = familySpaces.size,
                            sharedCount = sharedSpaces.size
                        )
                    }

                    // Filtres
                    item {
                        SpaceFiltersRow(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { selectedFilter = it },
                            spaceCounts = SpaceCounts(
                                all = allSpaces.size,
                                personal = personalSpaces.size,
                                family = familySpaces.size,
                                shared = sharedSpaces.size,
                                accessible = allSpaces.count { it.hasAccess },
                                managed = allSpaces.count { it.canManage }
                            )
                        )
                    }

                    // Demandes en attente (si il y en a)
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            PendingRequestsCard(
                                requests = pendingRequests,
                                onViewAll = onNavigateToPermissions
                            )
                        }
                    }

                    // Liste des espaces
                    if (filteredSpaces.isEmpty()) {
                        item {
                            EmptySpacesState(
                                hasSpaces = allSpaces.isNotEmpty(),
                                filter = selectedFilter,
                                searchQuery = searchQuery,
                                onCreateSpace = { showCreateDialog = true },
                                onClearFilters = {
                                    selectedFilter = SpaceFilter.ALL
                                    searchQuery = ""
                                }
                            )
                        }
                    } else {
                        // Affichage selon le mode sélectionné
                        when (viewMode) {
                            ViewMode.GRID -> {
                                item {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 280.dp),
                                        modifier = height((filteredSpaces.size / 2 + 1) * 200.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(filteredSpaces) { space ->
                                            SpaceGridCard(
                                                space = space,
                                                onClick = { onNavigateToSpace(space.id) }
                                            )
                                        }
                                    }
                                }
                            }

                            ViewMode.LIST -> {
                                items(filteredSpaces) { space ->
                                    SpaceListItem(
                                        space = space,
                                        onClick = { onNavigateToSpace(space.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Activité récente
                    if (recentActivity.isNotEmpty()) {
                        item {
                            RecentActivityCard(
                                activities = recentActivity.take(5)
                            )
                        }
                    }

                    // Espace pour le FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // FAB pour créer un nouvel espace
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.padding(16.dp),
            backgroundColor = MaterialTheme.colors.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Créer un espace")
        }
    }

    // Dialogue de création d'espace
    if (showCreateDialog) {
        CreateSpaceDialog(
            onDismiss = { showCreateDialog = false },
            onSuccess = { spaceType ->
                showCreateDialog = false
                if (spaceType == "CUSTOM") {
                    onNavigateToCreateSpace()
                } else {
                    // TODO: Créer un espace prédéfini
                }
            }
        )
    }
}

/**
 * Carte des statistiques des espaces
 */
@Composable
private fun SpacesStatsCard(
    stats: SpacesStatistics?,
    personalCount: Int,
    familyCount: Int,
    sharedCount: Int
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Vue d'ensemble",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    QuickStatCard(
                        icon = Icons.Default.PersonalVideo,
                        label = "Personnels",
                        value = "$personalCount",
                        color = MaterialTheme.colors.primary
                    )
                }

                item {
                    QuickStatCard(
                        icon = Icons.Default.Group,
                        label = "Familiaux",
                        value = "$familyCount",
                        color = MaterialTheme.colors.secondary
                    )
                }

                item {
                    QuickStatCard(
                        icon = Icons.Default.Share,
                        label = "Partagés",
                        value = "$sharedCount",
                        color = MaterialTheme.colors.arka.success
                    )
                }

                stats?.let { s ->
                    item {
                        QuickStatCard(
                            icon = Icons.Default.Storage,
                            label = "Stockage",
                            value = formatFileSize(s.totalStorageUsed),
                            color = MaterialTheme.colors.arka.warning
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rangée de filtres
 */
@Composable
private fun SpaceFiltersRow(
    selectedFilter: SpaceFilter,
    onFilterSelected: (SpaceFilter) -> Unit,
    spaceCounts: SpaceCounts
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SpaceFilter.values()) { filter ->
            val count = when (filter) {
                SpaceFilter.ALL -> spaceCounts.all
                SpaceFilter.PERSONAL -> spaceCounts.personal
                SpaceFilter.FAMILY -> spaceCounts.family
                SpaceFilter.SHARED -> spaceCounts.shared
                SpaceFilter.ACCESSIBLE -> spaceCounts.accessible
                SpaceFilter.MANAGED -> spaceCounts.managed
            }

            FilterChip(
                filter = filter,
                count = count,
                isSelected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

/**
 * Chip de filtre
 */
@Composable
private fun FilterChip(
    filter: SpaceFilter,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.surface
        },
        border = if (!isSelected) {
            BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = filter.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) {
                    MaterialTheme.colors.onPrimary
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                }
            )

            Text(
                text = "${filter.displayName} ($count)",
                style = ArkaTextStyles.filterChip,
                color = if (isSelected) {
                    MaterialTheme.colors.onPrimary
                } else {
                    MaterialTheme.colors.onSurface
                }
            )
        }
    }
}

/**
 * Carte des demandes en attente
 */
@Composable
private fun PendingRequestsCard(
    requests: List<AccessRequest>,
    onViewAll: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.arka.warning.copy(alpha = 0.05f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colors.arka.warning,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "Demandes en attente (${requests.size})",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.arka.warning
                    )
                }

                TextButton(onClick = onViewAll) {
                    Text("Voir tout", color = MaterialTheme.colors.arka.warning)
                }
            }

            requests.take(3).forEach { request ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = MaterialTheme.colors.arka.warning,
                        modifier = Modifier.size(6.dp)
                    )

                    Text(
                        text = "${request.requesterName} demande l'accès à \"${request.spaceName}\"",
                        style = ArkaTextStyles.notificationText,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = request.timeAgo,
                        style = ArkaTextStyles.timeAgo,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * État vide des espaces
 */
@Composable
private fun EmptySpacesState(
    hasSpaces: Boolean,
    filter: SpaceFilter,
    searchQuery: String,
    onCreateSpace: () -> Unit,
    onClearFilters: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (hasSpaces) Icons.Default.FilterList else Icons.Default.Storage,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )

            Text(
                text = if (hasSpaces) {
                    if (searchQuery.isNotEmpty()) {
                        "Aucun résultat pour \"$searchQuery\""
                    } else {
                        "Aucun espace pour ce filtre"
                    }
                } else {
                    "Aucun espace disponible"
                },
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = if (hasSpaces) {
                    "Essayez de modifier vos critères de recherche"
                } else {
                    "Créez votre premier espace pour commencer"
                },
                style = ArkaTextStyles.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasSpaces) {
                    ArkaOutlinedButton(
                        text = "Effacer les filtres",
                        icon = Icons.Default.Clear,
                        onClick = onClearFilters
                    )
                }

                ArkaButton(
                    text = "Créer un espace",
                    icon = Icons.Default.Add,
                    onClick = onCreateSpace
                )
            }
        }
    }
}

/**
 * Carte d'espace en mode grille
 */
@Composable
private fun SpaceGridCard(
    space: EspaceOverview,
    onClick: () -> Unit
) {
    ArkaCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = getSpaceIcon(space.type),
                    contentDescription = null,
                    tint = getSpaceColor(space.type),
                    modifier = Modifier.size(32.dp)
                )

                // Badge de permission
                Surface(
                    color = if (space.canManage) {
                        MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colors.surface
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (space.canManage) "Admin" else if (space.hasAccess) "Accès" else "Limité",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = ArkaTextStyles.permissionBadge,
                        color = if (space.canManage) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        }
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = space.nom,
                    style = ArkaTextStyles.spaceTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1
                )

                space.description?.let { description ->
                    Text(
                        text = description,
                        style = ArkaTextStyles.spaceDescription,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoItem(
                        icon = Icons.Default.InsertDriveFile,
                        text = "${space.filesCount}"
                    )

                    InfoItem(
                        icon = Icons.Default.Group,
                        text = "${space.membersCount}"
                    )
                }

                Text(
                    text = space.lastActivity?.let { "Actif ${it}" } ?: "Inactif",
                    style = ArkaTextStyles.lastActivity,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Item d'espace en mode liste
 */
@Composable
private fun SpaceListItem(
    space: EspaceOverview,
    onClick: () -> Unit
) {
    ArkaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône de l'espace
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = getSpaceColor(space.type).copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = getSpaceIcon(space.type),
                    contentDescription = null,
                    tint = getSpaceColor(space.type),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }

            // Informations de l'espace
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = space.nom,
                        style = ArkaTextStyles.spaceTitle,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface
                    )

                    TypeBadge(space.type)
                }

                space.description?.let { description ->
                    Text(
                        text = description,
                        style = ArkaTextStyles.spaceDescription,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoItem(
                        icon = Icons.Default.InsertDriveFile,
                        text = "${space.filesCount} fichier${if (space.filesCount > 1) "s" else ""}"
                    )

                    InfoItem(
                        icon = Icons.Default.Group,
                        text = "${space.membersCount} membre${if (space.membersCount > 1) "s" else ""}"
                    )

                    space.lastActivity?.let { lastActivity ->
                        Text(
                            text = "Actif $lastActivity",
                            style = ArkaTextStyles.lastActivity,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Badge de permission et actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (space.canManage) {
                        MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colors.surface
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (space.canManage) "Admin" else if (space.hasAccess) "Accès" else "Limité",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = ArkaTextStyles.permissionBadge,
                        color = if (space.canManage) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        }
                    )
                }

                ArkaIconButton(
                    icon = Icons.Default.ChevronRight,
                    onClick = onClick,
                    size = 20.dp
                )
            }
        }
    }
}

/**
 * Carte d'activité récente
 */
@Composable
private fun RecentActivityCard(
    activities: List<SpaceActivity>
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Activité récente",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            activities.forEach { activity ->
                ActivityItem(activity = activity)
            }
        }
    }
}

/**
 * Item d'activité
 */
@Composable
private fun ActivityItem(activity: SpaceActivity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getActivityIcon(activity.type),
            contentDescription = null,
            tint = getActivityColor(activity.type),
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.description,
                style = ArkaTextStyles.activityDescription,
                color = MaterialTheme.colors.onSurface
            )

            Text(
                text = "${activity.spaceName} • ${activity.timeAgo}",
                style = ArkaTextStyles.activityMeta,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Dialogue de création d'espace
 */
@Composable
private fun CreateSpaceDialog(
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un nouvel espace") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Choisissez le type d'espace à créer :")

                SpaceTypeOption(
                    icon = Icons.Default.PersonalVideo,
                    title = "Espace personnel",
                    description = "Privé, accessible uniquement par vous",
                    onClick = { onSuccess("PERSONNEL") }
                )

                SpaceTypeOption(
                    icon = Icons.Default.Group,
                    title = "Espace familial",
                    description = "Partagé avec tous les membres de la famille",
                    onClick = { onSuccess("FAMILIAL") }
                )

                SpaceTypeOption(
                    icon = Icons.Default.Share,
                    title = "Espace partagé",
                    description = "Partagé avec des membres spécifiques",
                    onClick = { onSuccess("PARTAGE") }
                )

                SpaceTypeOption(
                    icon = Icons.Default.Settings,
                    title = "Configuration personnalisée",
                    description = "Créer avec des paramètres avancés",
                    onClick = { onSuccess("CUSTOM") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Option de type d'espace
 */
@Composable
private fun SpaceTypeOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ArkaTextStyles.optionTitle,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = description,
                    style = ArkaTextStyles.optionDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ================================================================
// COMPOSANTS UTILITAIRES
// ================================================================

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
private fun TypeBadge(type: String) {
    val (color, text) = when (type) {
        "PERSONNEL" -> MaterialTheme.colors.primary to "Personnel"
        "FAMILIAL" -> MaterialTheme.colors.secondary to "Familial"
        "PARTAGE" -> MaterialTheme.colors.arka.success to "Partagé"
        else -> MaterialTheme.colors.onSurface to type
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = ArkaTextStyles.typeBadge.copy(fontSize = 10.sp),
            color = color
        )
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )

        Text(
            text = text,
            style = ArkaTextStyles.infoText,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )
    }
}

// ================================================================
// ENUMS ET DATA CLASSES
// ================================================================

enum class SpaceFilter(val displayName: String, val icon: ImageVector) {
    ALL("Tous", Icons.Default.Storage),
    PERSONAL("Personnels", Icons.Default.PersonalVideo),
    FAMILY("Familiaux", Icons.Default.Group),
    SHARED("Partagés", Icons.Default.Share),
    ACCESSIBLE("Accessibles", Icons.Default.CheckCircle),
    MANAGED("Gérés", Icons.Default.AdminPanelSettings)
}

enum class ViewMode {
    GRID, LIST
}

data class EspaceOverview(
    val id: Int,
    val nom: String,
    val description: String?,
    val type: String,
    val filesCount: Int,
    val membersCount: Int,
    val hasAccess: Boolean,
    val canManage: Boolean,
    val lastActivity: String?
)

data class SpacesStatistics(
    val totalSpaces: Int,
    val personalSpaces: Int,
    val familySpaces: Int,
    val sharedSpaces: Int,
    val totalStorageUsed: Long,
    val totalFiles: Int
)

data class SpaceActivity(
    val description: String,
    val spaceName: String,
    val type: String,
    val timeAgo: String
)

data class AccessRequest(
    val id: Int,
    val requesterName: String,
    val spaceName: String,
    val timeAgo: String
)

data class SpaceCounts(
    val all: Int,
    val personal: Int,
    val family: Int,
    val shared: Int,
    val accessible: Int,
    val managed: Int
)

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun getSpaceIcon(type: String): ImageVector {
    return when (type) {
        "PERSONNEL" -> Icons.Default.PersonalVideo
        "FAMILIAL" -> Icons.Default.Group
        "PARTAGE" -> Icons.Default.Share
        else -> Icons.Default.Storage
    }
}

private fun getSpaceColor(type: String): Color {
    return when (type) {
        "PERSONNEL" -> MaterialTheme.colors.primary
        "FAMILIAL" -> MaterialTheme.colors.secondary
        "PARTAGE" -> MaterialTheme.colors.arka.success
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    }
}

private fun getActivityIcon(type: String): ImageVector {
    return when (type) {
        "FILE_ADDED" -> Icons.Default.CloudUpload
        "MEMBER_JOINED" -> Icons.Default.PersonAdd
        "PERMISSION_GRANTED" -> Icons.Default.Security
        "FOLDER_CREATED" -> Icons.Default.CreateNewFolder
        else -> Icons.Default.Notifications
    }
}

private fun getActivityColor(type: String): Color {
    return when (type) {
        "FILE_ADDED" -> MaterialTheme.colors.arka.success
        "MEMBER_JOINED" -> MaterialTheme.colors.primary
        "PERMISSION_GRANTED" -> MaterialTheme.colors.secondary
        "FOLDER_CREATED" -> MaterialTheme.colors.arka.warning
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