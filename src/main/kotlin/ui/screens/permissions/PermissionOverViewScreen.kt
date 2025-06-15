// ================================================================
// PERMISSIONSOVERVIEWSCREEN.KT - ÉCRAN GESTION PERMISSIONS ARKA
// ================================================================

package ui.screens.permissions

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
import ui.screens.folders.GrantedPermission
import ui.screens.folders.PermissionAudit
import ui.screens.folders.PermissionRequest
import ui.screens.folders.PermissionsStatistics
import ui.screens.folders.UserPermission
import ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran de gestion des permissions Arka
 *
 * Fonctionnalités:
 * - Vue d'ensemble de toutes les permissions utilisateur
 * - Gestion des demandes de permissions (envoyées et reçues)
 * - Permissions accordées et actives
 * - Historique et audit des permissions
 * - Création de nouvelles demandes
 * - Actions admin (approuver/refuser/révoquer)
 */
@Composable
fun PermissionsOverviewScreen(
    permissionController: PermissionController,
    delegationController: DelegationController,
    authController: AuthController,
    onNavigateToRequestPermission: () -> Unit,
    onNavigateToSpacePermissions: (Int) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var myPermissions by remember { mutableStateOf<List<UserPermission>>(emptyList()) }
    var sentRequests by remember { mutableStateOf<List<PermissionRequest>>(emptyList()) }
    var receivedRequests by remember { mutableStateOf<List<PermissionRequest>>(emptyList()) }
    var grantedPermissions by remember { mutableStateOf<List<GrantedPermission>>(emptyList()) }
    var auditLog by remember { mutableStateOf<List<PermissionAudit>>(emptyList()) }
    var permissionsStats by remember { mutableStateOf<PermissionsStatistics?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    // Charger les données des permissions
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            scope.launch {
                isLoading = true
                try {
                    // Charger MES permissions (ce que j'ai)
                    val myPermsResult = permissionController.getUserPermissions(currentUser.membreFamilleId)
                    if (myPermsResult is PermissionController.PermissionResult.Success) {
                        myPermissions = myPermsResult.data
                    }

                    // Charger les demandes que j'ai envoyées
                    val sentResult = permissionController.getSentRequests(currentUser.membreFamilleId)
                    if (sentResult is PermissionController.PermissionResult.Success) {
                        sentRequests = sentResult.data
                    }

                    // Charger les demandes que j'ai reçues (si admin/responsable)
                    if (currentUser.estAdmin || currentUser.estResponsable) {
                        val receivedResult = permissionController.getReceivedRequests(currentUser.membreFamilleId)
                        if (receivedResult is PermissionController.PermissionResult.Success) {
                            receivedRequests = receivedResult.data
                        }
                    }

                    // Charger les permissions que j'ai accordées
                    val grantedResult = permissionController.getGrantedPermissions(currentUser.membreFamilleId)
                    if (grantedResult is PermissionController.PermissionResult.Success) {
                        grantedPermissions = grantedResult.data
                    }

                    // Charger l'historique d'audit
                    val auditResult = permissionController.getPermissionAudit(currentUser.membreFamilleId, 20)
                    if (auditResult is PermissionController.PermissionResult.Success) {
                        auditLog = auditResult.data
                    }

                    // Charger les statistiques
                    val statsResult = permissionController.getPermissionsStatistics(currentUser.membreFamilleId)
                    if (statsResult is PermissionController.PermissionResult.Success) {
                        permissionsStats = statsResult.data
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
        "Mes permissions" to Icons.Default.Lock,
        "Demandes envoyées" to Icons.Default.Send,
        if (currentUser?.estAdmin == true || currentUser?.estResponsable == true)
            "Demandes reçues" to Icons.Default.Inbox else null,
        "Accordées" to Icons.Default.Share,
        "Historique" to Icons.Default.History
    ).filterNotNull()

    Column(modifier = Modifier.fillMaxSize()) {
        // Barre de navigation supérieure
        ArkaTopAppBar(
            title = "Gestion des permissions",
            subtitle = "Contrôlez l'accès à vos ressources",
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
            actions = {
                // Notifications pour les demandes en attente
                if (receivedRequests.isNotEmpty()) {
                    BadgedBox(
                        badge = {
                            Badge { Text("${receivedRequests.size}") }
                        }
                    ) {
                        ArkaIconButton(
                            icon = Icons.Default.Notifications,
                            onClick = { selectedTab = tabs.indexOfFirst { it.first == "Demandes reçues" } },
                            tooltip = "Demandes en attente"
                        )
                    }
                }

                // Nouvelle demande
                ArkaIconButton(
                    icon = Icons.Default.Add,
                    onClick = { showRequestDialog = true },
                    tooltip = "Nouvelle demande"
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
                            onNavigateToRequestPermission()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Demander un accès")
                        }

                        if (currentUser?.estAdmin == true) {
                            DropdownMenuItem(onClick = {
                                showMenu = false
                                // TODO: Paramètres globaux permissions
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Paramètres globaux")
                            }

                            DropdownMenuItem(onClick = {
                                showMenu = false
                                // TODO: Exporter audit
                            }) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Exporter l'audit")
                            }
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
                        message = "Chargement des permissions..."
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
                    // Vue d'ensemble des statistiques
                    PermissionsStatsCard(
                        stats = permissionsStats,
                        myPermissions = myPermissions,
                        sentRequests = sentRequests,
                        receivedRequests = receivedRequests,
                        grantedPermissions = grantedPermissions
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
                                icon = {
                                    // Badge pour les demandes reçues
                                    if (title == "Demandes reçues" && receivedRequests.isNotEmpty()) {
                                        BadgedBox(
                                            badge = { Badge { Text("${receivedRequests.size}") } }
                                        ) {
                                            Icon(icon, contentDescription = title)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = title)
                                    }
                                }
                            )
                        }
                    }

                    // Contenu des onglets
                    when (selectedTab) {
                        0 -> MyPermissionsTab(
                            permissions = myPermissions,
                            onNavigateToSpace = onNavigateToSpacePermissions
                        )
                        1 -> SentRequestsTab(
                            requests = sentRequests,
                            onCancelRequest = { requestId ->
                                // TODO: Annuler la demande
                            }
                        )
                        2 -> if (currentUser?.estAdmin == true || currentUser?.estResponsable == true) {
                            ReceivedRequestsTab(
                                requests = receivedRequests,
                                onApprove = { requestId ->
                                    // TODO: Approuver la demande
                                },
                                onReject = { requestId ->
                                    // TODO: Refuser la demande
                                }
                            )
                        }
                        3 -> GrantedPermissionsTab(
                            grantedPermissions = grantedPermissions,
                            onRevokePermission = { permissionId ->
                                // TODO: Révoquer la permission
                            }
                        )
                        4 -> AuditTab(auditLog = auditLog)
                    }
                }
            }
        }
    }

    // Dialogue de nouvelle demande rapide
    if (showRequestDialog) {
        QuickRequestDialog(
            onDismiss = { showRequestDialog = false },
            onCreateDetailed = {
                showRequestDialog = false
                onNavigateToRequestPermission()
            },
            onQuickRequest = { target, permission ->
                showRequestDialog = false
                // TODO: Créer demande rapide
            }
        )
    }

    // Dialogue de gestion de demande
    selectedRequest?.let { request ->
        RequestActionDialog(
            request = request,
            canApprove = currentUser?.estAdmin == true || currentUser?.estResponsable == true,
            onDismiss = { selectedRequest = null },
            onAction = { action ->
                selectedRequest = null
                // TODO: Traiter l'action
            }
        )
    }
}

/**
 * Carte des statistiques des permissions
 */
@Composable
private fun PermissionsStatsCard(
    stats: PermissionsStatistics?,
    myPermissions: List<UserPermission>,
    sentRequests: List<PermissionRequest>,
    receivedRequests: List<PermissionRequest>,
    grantedPermissions: List<GrantedPermission>
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
            Text(
                text = "Vue d'ensemble des permissions",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PermissionStatCard(
                        icon = Icons.Default.Lock,
                        label = "Mes accès",
                        value = "${myPermissions.size}",
                        color = MaterialTheme.colors.primary
                    )
                }

                item {
                    PermissionStatCard(
                        icon = Icons.Default.Send,
                        label = "Demandes envoyées",
                        value = "${sentRequests.size}",
                        color = MaterialTheme.colors.secondary
                    )
                }

                if (receivedRequests.isNotEmpty()) {
                    item {
                        PermissionStatCard(
                            icon = Icons.Default.Inbox,
                            label = "En attente",
                            value = "${receivedRequests.size}",
                            color = MaterialTheme.colors.arka.warning
                        )
                    }
                }

                item {
                    PermissionStatCard(
                        icon = Icons.Default.Share,
                        label = "Accordées",
                        value = "${grantedPermissions.size}",
                        color = MaterialTheme.colors.arka.success
                    )
                }

                stats?.let { s ->
                    item {
                        PermissionStatCard(
                            icon = Icons.Default.Security,
                            label = "Niveau moyen",
                            value = s.averagePermissionLevel,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Onglet "Mes permissions"
 */
@Composable
private fun MyPermissionsTab(
    permissions: List<UserPermission>,
    onNavigateToSpace: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (permissions.isEmpty()) {
            item {
                EmptyPermissionsState(
                    icon = Icons.Default.Lock,
                    title = "Aucune permission",
                    description = "Vous n'avez pas encore d'accès spéciaux",
                    actionText = "Demander un accès",
                    onAction = { /* TODO */ }
                )
            }
        } else {
            // Grouper par niveau de permission
            val groupedPermissions = permissions.groupBy { it.level }

            groupedPermissions.forEach { (level, perms) ->
                item {
                    PermissionGroupHeader(
                        level = level,
                        count = perms.size
                    )
                }

                items(perms) { permission ->
                    PermissionCard(
                        permission = permission,
                        onClick = { onNavigateToSpace(permission.resourceId) }
                    )
                }
            }
        }
    }
}

/**
 * Onglet "Demandes envoyées"
 */
@Composable
private fun SentRequestsTab(
    requests: List<PermissionRequest>,
    onCancelRequest: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (requests.isEmpty()) {
            item {
                EmptyPermissionsState(
                    icon = Icons.Default.Send,
                    title = "Aucune demande envoyée",
                    description = "Vous n'avez pas encore fait de demande d'accès",
                    actionText = "Nouvelle demande",
                    onAction = { /* TODO */ }
                )
            }
        } else {
            items(requests) { request ->
                RequestCard(
                    request = request,
                    showActions = true,
                    onCancel = { onCancelRequest(request.id) }
                )
            }
        }
    }
}

/**
 * Onglet "Demandes reçues"
 */
@Composable
private fun ReceivedRequestsTab(
    requests: List<PermissionRequest>,
    onApprove: (Int) -> Unit,
    onReject: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (requests.isEmpty()) {
            item {
                EmptyPermissionsState(
                    icon = Icons.Default.Inbox,
                    title = "Aucune demande reçue",
                    description = "Vous n'avez pas de demandes en attente",
                    actionText = null,
                    onAction = { }
                )
            }
        } else {
            items(requests) { request ->
                ReceivedRequestCard(
                    request = request,
                    onApprove = { onApprove(request.id) },
                    onReject = { onReject(request.id) }
                )
            }
        }
    }
}

/**
 * Onglet "Permissions accordées"
 */
@Composable
private fun GrantedPermissionsTab(
    grantedPermissions: List<GrantedPermission>,
    onRevokePermission: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (grantedPermissions.isEmpty()) {
            item {
                EmptyPermissionsState(
                    icon = Icons.Default.Share,
                    title = "Aucune permission accordée",
                    description = "Vous n'avez accordé d'accès à personne",
                    actionText = null,
                    onAction = { }
                )
            }
        } else {
            items(grantedPermissions) { granted ->
                GrantedPermissionCard(
                    granted = granted,
                    onRevoke = { onRevokePermission(granted.id) }
                )
            }
        }
    }
}

/**
 * Onglet "Historique"
 */
@Composable
private fun AuditTab(auditLog: List<PermissionAudit>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (auditLog.isEmpty()) {
            item {
                EmptyPermissionsState(
                    icon = Icons.Default.History,
                    title = "Aucun historique",
                    description = "L'historique des permissions apparaîtra ici",
                    actionText = null,
                    onAction = { }
                )
            }
        } else {
            items(auditLog) { audit ->
                AuditLogItem(audit = audit)
            }
        }
    }
}

/**
 * Carte de permission
 */
@Composable
private fun PermissionCard(
    permission: UserPermission,
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
            // Icône de la ressource
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = getPermissionLevelColor(permission.level).copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = getResourceIcon(permission.resourceType),
                    contentDescription = null,
                    tint = getPermissionLevelColor(permission.level),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }

            // Informations de la permission
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = permission.resourceName,
                    style = ArkaTextStyles.permissionTitle,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface
                )

                Text(
                    text = permission.resourcePath,
                    style = ArkaTextStyles.permissionPath,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PermissionLevelBadge(permission.level)

                    Text(
                        text = "Accordée le ${permission.grantedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                        style = ArkaTextStyles.metadata,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Actions
            ArkaIconButton(
                icon = Icons.Default.ChevronRight,
                onClick = onClick,
                size = 20.dp
            )
        }
    }
}

/**
 * Carte de demande
 */
@Composable
private fun RequestCard(
    request: PermissionRequest,
    showActions: Boolean = false,
    onCancel: (() -> Unit)? = null
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = request.resourceName,
                        style = ArkaTextStyles.requestTitle,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface
                    )

                    Text(
                        text = "Demande à ${request.targetUserName}",
                        style = ArkaTextStyles.requestTarget,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PermissionLevelBadge(request.requestedLevel)

                        RequestStatusBadge(request.status)
                    }
                }

                Text(
                    text = request.timeAgo,
                    style = ArkaTextStyles.timeAgo,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            request.message?.let { message ->
                Text(
                    text = "\"$message\"",
                    style = ArkaTextStyles.requestMessage,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )
            }

            if (showActions && request.status == "PENDING" && onCancel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colors.error
                        )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Annuler")
                    }
                }
            }
        }
    }
}

/**
 * Carte de demande reçue
 */
@Composable
private fun ReceivedRequestCard(
    request: PermissionRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.arka.warning.copy(alpha = 0.02f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${request.requesterName} demande un accès",
                        style = ArkaTextStyles.requestTitle,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface
                    )

                    Text(
                        text = request.resourceName,
                        style = ArkaTextStyles.requestResource,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )

                    PermissionLevelBadge(request.requestedLevel)
                }

                Text(
                    text = request.timeAgo,
                    style = ArkaTextStyles.timeAgo,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            request.message?.let { message ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colors.background
                ) {
                    Text(
                        text = "\"$message\"",
                        modifier = Modifier.padding(12.dp),
                        style = ArkaTextStyles.requestMessage,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArkaOutlinedButton(
                    text = "Refuser",
                    icon = Icons.Default.Close,
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colors.error
                )

                ArkaButton(
                    text = "Approuver",
                    icon = Icons.Default.Check,
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Carte de permission accordée
 */
@Composable
private fun GrantedPermissionCard(
    granted: GrantedPermission,
    onRevoke: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar du bénéficiaire
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }

            // Informations
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = granted.beneficiaryName,
                    style = ArkaTextStyles.grantedTitle,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onSurface
                )

                Text(
                    text = granted.resourceName,
                    style = ArkaTextStyles.grantedResource,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PermissionLevelBadge(granted.level)

                    Text(
                        text = "Accordée le ${granted.grantedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                        style = ArkaTextStyles.metadata,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Actions
            ArkaIconButton(
                icon = Icons.Default.RemoveCircleOutline,
                onClick = onRevoke,
                size = 20.dp,
                color = MaterialTheme.colors.error
            )
        }
    }
}

/**
 * Item d'audit
 */
@Composable
private fun AuditLogItem(audit: PermissionAudit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getAuditIcon(audit.action),
            contentDescription = null,
            tint = getAuditColor(audit.action),
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audit.description,
                style = ArkaTextStyles.auditDescription,
                color = MaterialTheme.colors.onSurface
            )

            Text(
                text = "${audit.actor} • ${audit.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))}",
                style = ArkaTextStyles.auditMeta,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ================================================================
// DIALOGUES
// ================================================================

/**
 * Dialogue de demande rapide
 */
@Composable
private fun QuickRequestDialog(
    onDismiss: () -> Unit,
    onCreateDetailed: () -> Unit,
    onQuickRequest: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle demande d'accès") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Comment souhaitez-vous procéder ?")

                // Option demande rapide
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuickRequest("QUICK", "READ") },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Demande rapide",
                                style = ArkaTextStyles.optionTitle,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Accès en lecture à un espace ou dossier",
                                style = ArkaTextStyles.optionDescription,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Option demande détaillée
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreateDetailed() },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colors.secondary
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Demande personnalisée",
                                style = ArkaTextStyles.optionTitle,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Configurez précisément les permissions souhaitées",
                                style = ArkaTextStyles.optionDescription,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
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
 * Dialogue d'action sur une demande
 */
@Composable
private fun RequestActionDialog(
    request: PermissionRequest,
    canApprove: Boolean,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${request.resourceName}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Demande de ${request.requesterName}")

                request.message?.let { message ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colors.background
                    ) {
                        Text(
                            text = "\"$message\"",
                            modifier = Modifier.padding(12.dp),
                            style = ArkaTextStyles.requestMessage
                        )
                    }
                }

                if (canApprove && request.status == "PENDING") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ArkaOutlinedButton(
                            text = "Refuser",
                            icon = Icons.Default.Close,
                            onClick = { onAction("REJECT") },
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colors.error
                        )

                        ArkaButton(
                            text = "Approuver",
                            icon = Icons.Default.Check,
                            onClick = { onAction("APPROVE") },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
private fun PermissionStatCard(
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
private fun PermissionGroupHeader(
    level: String,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = getPermissionLevelIcon(level),
            contentDescription = null,
            tint = getPermissionLevelColor(level),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = "${getPermissionLevelDisplay(level)} ($count)",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
private fun PermissionLevelBadge(level: String) {
    val color = getPermissionLevelColor(level)

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = getPermissionLevelDisplay(level),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = ArkaTextStyles.permissionBadge,
            color = color
        )
    }
}

@Composable
private fun RequestStatusBadge(status: String) {
    val (color, text) = when (status) {
        "PENDING" -> MaterialTheme.colors.arka.warning to "En attente"
        "APPROVED" -> MaterialTheme.colors.arka.success to "Approuvée"
        "REJECTED" -> MaterialTheme.colors.error to "Refusée"
        "CANCELLED" -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f) to "Annulée"
        else -> MaterialTheme.colors.onSurface to status
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = ArkaTextStyles.statusBadge,
            color = color
        )
    }
}

@Composable
private fun EmptyPermissionsState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String?,
    onAction: () -> Unit
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
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = description,
                style = ArkaTextStyles.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )

            actionText?.let { text ->
                ArkaButton(
                    text = text,
                    icon = Icons.Default.Add,
                    onClick = onAction
                )
            }
        }
    }
}

// ================================================================
// DATA CLASSES
// ================================================================

data class UserPermission(
    val id: Int,
    val resourceName: String,
    val resourcePath: String,
    val resourceType: String,
    val resourceId: Int,
    val level: String,
    val grantedDate: LocalDateTime,
    val grantedBy: String
)

data class PermissionRequest(
    val id: Int,
    val requesterName: String,
    val targetUserName: String,
    val resourceName: String,
    val requestedLevel: String,
    val status: String,
    val message: String?,
    val timeAgo: String
)

data class GrantedPermission(
    val id: Int,
    val beneficiaryName: String,
    val resourceName: String,
    val level: String,
    val grantedDate: LocalDateTime
)

data class PermissionAudit(
    val action: String,
    val description: String,
    val actor: String,
    val timestamp: LocalDateTime
)

data class PermissionsStatistics(
    val totalPermissions: Int,
    val averagePermissionLevel: String,
    val pendingRequests: Int,
    val grantedPermissions: Int
)

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun getPermissionLevelColor(level: String): Color {
    return when (level) {
        "READ" -> MaterialTheme.colors.arka.success
        "WRITE" -> MaterialTheme.colors.arka.warning
        "ADMIN" -> MaterialTheme.colors.error
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    }
}

private fun getPermissionLevelIcon(level: String): ImageVector {
    return when (level) {
        "READ" -> Icons.Default.Visibility
        "WRITE" -> Icons.Default.Edit
        "ADMIN" -> Icons.Default.AdminPanelSettings
        else -> Icons.Default.Lock
    }
}

private fun getPermissionLevelDisplay(level: String): String {
    return when (level) {
        "READ" -> "Lecture"
        "WRITE" -> "Écriture"
        "ADMIN" -> "Administration"
        else -> level
    }
}

private fun getResourceIcon(type: String): ImageVector {
    return when (type) {
        "SPACE" -> Icons.Default.Storage
        "CATEGORY" -> Icons.Default.Category
        "FOLDER" -> Icons.Default.Folder
        "FILE" -> Icons.Default.InsertDriveFile
        else -> Icons.Default.Lock
    }
}

private fun getAuditIcon(action: String): ImageVector {
    return when (action) {
        "GRANTED" -> Icons.Default.Check
        "REVOKED" -> Icons.Default.RemoveCircle
        "REQUESTED" -> Icons.Default.Send
        "APPROVED" -> Icons.Default.CheckCircle
        "REJECTED" -> Icons.Default.Cancel
        else -> Icons.Default.Info
    }
}

private fun getAuditColor(action: String): Color {
    return when (action) {
        "GRANTED", "APPROVED" -> MaterialTheme.colors.arka.success
        "REVOKED", "REJECTED" -> MaterialTheme.colors.error
        "REQUESTED" -> MaterialTheme.colors.primary
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    }
}