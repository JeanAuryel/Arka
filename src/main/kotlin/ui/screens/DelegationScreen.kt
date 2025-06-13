package ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import controllers.DelegationController
import controllers.PermissionController
import ktorm.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.format.DateTimeFormatter

@Composable
fun DelegationScreen(
    currentUserId: Int,
    delegationController: DelegationController = koinInject(),
    permissionController: PermissionController = koinInject()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    val tabs = listOf(
        "Mes demandes" to Icons.Default.Send,
        "Demandes reçues" to Icons.Default.Inbox,
        "Permissions actives" to Icons.Default.Lock,
        "Audit" to Icons.Default.History
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête avec titre et bouton d'action
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🔐 Gestion des Délégations",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = "Gérez les permissions et accès délégués",
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nouvelle demande")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Onglets
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
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

        Spacer(Modifier.height(16.dp))

        // Contenu des onglets
        when (selectedTab) {
            0 -> MyRequestsTab(currentUserId, delegationController, refreshTrigger)
            1 -> ReceivedRequestsTab(currentUserId, delegationController, refreshTrigger) { refreshTrigger++ }
            2 -> ActivePermissionsTab(currentUserId, permissionController, refreshTrigger)
            3 -> AuditTab(currentUserId, permissionController, refreshTrigger)
        }
    }

    // Dialogue de création de demande
    if (showCreateDialog) {
        CreateDelegationDialog(
            currentUserId = currentUserId,
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                refreshTrigger++
            }
        )
    }
}

fun koinInject() {
    TODO("Not yet implemented")
}

@Composable
private fun MyRequestsTab(
    currentUserId: Int,
    delegationController: DelegationController,
    refreshTrigger: Int
) {
    var demandes by remember { mutableStateOf<List<DemandeDelegation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        scope.launch {
            isLoading = true
            val result = delegationController.getMyDelegationRequests(currentUserId)
            if (result.isSuccess) {
                demandes = result.getOrNull() ?: emptyList()
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (demandes.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Send,
                        title = "Aucune demande envoyée",
                        description = "Vous n'avez encore fait aucune demande de délégation"
                    )
                }
            } else {
                items(demandes) { demande ->
                    DelegationRequestCard(
                        demande = demande,
                        isMyRequest = true,
                        onAction = { /* Actions pour mes demandes */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceivedRequestsTab(
    currentUserId: Int,
    delegationController: DelegationController,
    refreshTrigger: Int,
    onRefresh: () -> Unit
) {
    var demandes by remember { mutableStateOf<List<DemandeDelegation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        scope.launch {
            isLoading = true
            val result = delegationController.getReceivedDelegationRequests(currentUserId)
            if (result.isSuccess) {
                demandes = result.getOrNull() ?: emptyList()
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (demandes.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Inbox,
                        title = "Aucune demande reçue",
                        description = "Vous n'avez reçu aucune demande de délégation"
                    )
                }
            } else {
                items(demandes) { demande ->
                    DelegationRequestCard(
                        demande = demande,
                        isMyRequest = false,
                        onAction = { action, demandeId ->
                            scope.launch {
                                when (action) {
                                    "approve" -> delegationController.approveDelegationRequest(demandeId, currentUserId)
                                    "reject" -> delegationController.rejectDelegationRequest(demandeId, currentUserId, "Refusé par l'utilisateur")
                                }
                                onRefresh()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivePermissionsTab(
    currentUserId: Int,
    permissionController: PermissionController,
    refreshTrigger: Int
) {
    var permissions by remember { mutableStateOf<List<PermissionActive>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        scope.launch {
            isLoading = true
            val result = permissionController.getActivePermissions(currentUserId)
            if (result.isSuccess) {
                permissions = result.getOrNull() ?: emptyList()
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (permissions.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Lock,
                        title = "Aucune permission active",
                        description = "Vous n'avez aucune permission déléguée en cours"
                    )
                }
            } else {
                items(permissions) { permission ->
                    ActivePermissionCard(permission = permission)
                }
            }
        }
    }
}

@Composable
private fun AuditTab(
    currentUserId: Int,
    permissionController: PermissionController,
    refreshTrigger: Int
) {
    // TODO: Implémenter l'onglet audit avec l'historique des actions
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colors.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Journal d'audit",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fonctionnalité en cours de développement",
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun DelegationRequestCard(
    demande: DemandeDelegation,
    isMyRequest: Boolean,
    onAction: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (demande.portee) {
                            PorteePermission.FICHIER -> Icons.Default.Description
                            PorteePermission.DOSSIER -> Icons.Default.Folder
                            PorteePermission.CATEGORIE -> Icons.Default.Category
                            PorteePermission.ESPACE_COMPLET -> Icons.Default.Storage
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${demande.portee.name} - ${demande.typePermission.name}",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isMyRequest) "Demandé à: ${demande.proprietaireId}" else "Demandé par: ${demande.beneficiaireId}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                StatusChip(demande.statut)
            }

            if (demande.raisonDemande?.isNotBlank() == true) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Raison: ${demande.raisonDemande}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Demandé le: ${demande.dateDemande.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))}",
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            // Actions pour les demandes reçues en attente
            if (!isMyRequest && demande.statut == StatutDemande.EN_ATTENTE) {
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAction("approve", demande.demandeId) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Approuver")
                    }

                    OutlinedButton(
                        onClick = { onAction("reject", demande.demandeId) }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Refuser")
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePermissionCard(permission: PermissionActive) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${permission.portee.name} - ${permission.typePermission.name}",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Accordé par: ${permission.proprietaireId}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                if (permission.dateExpiration != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Expire le:",
                            fontSize = 10.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = permission.dateExpiration.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Accordé le: ${permission.dateOctroi.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))}",
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StatusChip(statut: StatutDemande) {
    val (color, text) = when (statut) {
        StatutDemande.EN_ATTENTE -> Color(0xFFFF9800) to "En attente"
        StatutDemande.APPROUVEE -> Color(0xFF4CAF50) to "Approuvée"
        StatutDemande.REJETEE -> Color(0xFFF44336) to "Rejetée"
        StatutDemande.REVOQUEE -> Color(0xFF9E9E9E) to "Révoquée"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}