// ================================================================
// AUDITSCREEN.KT - JOURNAL D'AUDIT
// ================================================================

package ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import controllers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.components.*
import ui.screens.FilterChip
import ui.theme.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran du journal d'audit
 *
 * Fonctionnalités:
 * - Affichage de toutes les activités système
 * - Filtrage par type d'action, utilisateur, date
 * - Recherche dans les logs
 * - Export des journaux d'audit
 * - Détails des actions sensibles
 */
@Composable
fun AuditScreen(
    auditController: JournalAuditPermissionController,
    familyMemberController: FamilyMemberController,
    authController: AuthController,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var auditEntries by remember { mutableStateOf<List<AuditEntry>>(emptyList()) }
    var filteredEntries by remember { mutableStateOf<List<AuditEntry>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // États de filtrage
    var selectedActionType by remember { mutableStateOf<AuditActionType?>(null) }
    var selectedSeverity by remember { mutableStateOf<AuditSeverity?>(null) }
    var selectedUserId by remember { mutableStateOf<Int?>(null) }
    var dateRange by remember { mutableStateOf<Pair<LocalDateTime?, LocalDateTime?>>(null to null) }
    var searchQuery by remember { mutableStateOf("") }

    // États UI
    var showFilters by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<AuditEntry?>(null) }
    var familyMembers by remember { mutableStateOf<List<MembreFamille>>(emptyList()) }

    // Charger les données
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                // Charger les membres de la famille pour les filtres
                val membersResult = familyMemberController.getAllFamilyMembers()
                if (membersResult is FamilyMemberController.FamilyMemberResult.Success) {
                    familyMembers = membersResult.data
                }

                // Charger les entrées d'audit
                val auditResult = auditController.getAllAuditEntries()
                if (auditResult is JournalAuditPermissionController.AuditResult.Success) {
                    // Convertir en format unifié
                    auditEntries = auditResult.data.map { entry ->
                        AuditEntry(
                            id = entry.id,
                            timestamp = entry.dateHeure,
                            userId = entry.membreFamilleId,
                            actionType = mapToActionType(entry.action),
                            description = entry.description,
                            ipAddress = "192.168.1.1", // Mock data
                            userAgent = "Arka Desktop v2.0", // Mock data
                            severity = mapToSeverity(entry.action),
                            details = mapOf(
                                "Entity ID" to entry.permissionId.toString(),
                                "Entity Type" to "Permission"
                            )
                        )
                    }.sortedByDescending { it.timestamp }

                    filteredEntries = auditEntries
                }

            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Appliquer les filtres
    LaunchedEffect(selectedActionType, selectedSeverity, selectedUserId, dateRange, searchQuery) {
        filteredEntries = auditEntries.filter { entry ->
            // Filtre par type d'action
            (selectedActionType == null || entry.actionType == selectedActionType) &&
                    // Filtre par sévérité
                    (selectedSeverity == null || entry.severity == selectedSeverity) &&
                    // Filtre par utilisateur
                    (selectedUserId == null || entry.userId == selectedUserId) &&
                    // Filtre par date
                    (dateRange.first == null || entry.timestamp.isAfter(dateRange.first)) &&
                    (dateRange.second == null || entry.timestamp.isBefore(dateRange.second)) &&
                    // Filtre par recherche
                    (searchQuery.isEmpty() || entry.description.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête avec contrôles
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Journal d'audit",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Compteur d'entrées
            Text(
                text = "${filteredEntries.size} entrées",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Bouton filtres
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filtres",
                    tint = if (hasActiveFilters(selectedActionType, selectedSeverity, selectedUserId, dateRange, searchQuery)) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    }
                )
            }

            // Bouton export
            IconButton(
                onClick = {
                    // Logique d'export du journal d'audit
                }
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = "Exporter")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Barre de recherche
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Rechercher dans les journaux...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filtres dépliables
        if (showFilters) {
            AuditFiltersSection(
                selectedActionType = selectedActionType,
                selectedSeverity = selectedSeverity,
                selectedUserId = selectedUserId,
                dateRange = dateRange,
                familyMembers = familyMembers,
                onActionTypeChange = { selectedActionType = it },
                onSeverityChange = { selectedSeverity = it },
                onUserIdChange = { selectedUserId = it },
                onDateRangeChange = { dateRange = it },
                onClearFilters = {
                    selectedActionType = null
                    selectedSeverity = null
                    selectedUserId = null
                    dateRange = null to null
                    searchQuery = ""
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredEntries.isEmpty()) {
            EmptyAuditState(hasFilters = hasActiveFilters(selectedActionType, selectedSeverity, selectedUserId, dateRange, searchQuery))
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredEntries) { entry ->
                    AuditEntryCard(
                        entry = entry,
                        familyMembers = familyMembers,
                        onClick = { selectedEntry = entry }
                    )
                }
            }
        }

        // Dialog de détails
        selectedEntry?.let { entry ->
            AuditEntryDetailDialog(
                entry = entry,
                familyMembers = familyMembers,
                onDismiss = { selectedEntry = null }
            )
        }

        // Gestion des erreurs
        errorMessage?.let { error ->
            LaunchedEffect(error) {
                delay(5000)
                errorMessage = null
            }

            Snackbar(
                modifier = Modifier.padding(16.dp),
                backgroundColor = Color.Red.copy(alpha = 0.8f)
            ) {
                Text(text = error, color = Color.White)
            }
        }
    }
}

/**
 * Section des filtres d'audit
 */
@Composable
private fun AuditFiltersSection(
    selectedActionType: AuditActionType?,
    selectedSeverity: AuditSeverity?,
    selectedUserId: Int?,
    dateRange: Pair<LocalDateTime?, LocalDateTime?>,
    familyMembers: List<MembreFamille>,
    onActionTypeChange: (AuditActionType?) -> Unit,
    onSeverityChange: (AuditSeverity?) -> Unit,
    onUserIdChange: (Int?) -> Unit,
    onDateRangeChange: (Pair<LocalDateTime?, LocalDateTime?>) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.8f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtres",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = onClearFilters) {
                    Text("Effacer tout")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filtre par type d'action
            Text(
                text = "Type d'action",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedActionType == null,
                        onClick = { onActionTypeChange(null) },
                        label = { Text("Toutes") }
                    )
                }

                items(AuditActionType.values()) { actionType ->
                    FilterChip(
                        selected = selectedActionType == actionType,
                        onClick = { onActionTypeChange(actionType) },
                        label = { Text(actionType.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filtre par sévérité
            Text(
                text = "Sévérité",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSeverity == null,
                        onClick = { onSeverityChange(null) },
                        label = { Text("Toutes") }
                    )
                }

                items(AuditSeverity.values()) { severity ->
                    FilterChip(
                        selected = selectedSeverity == severity,
                        onClick = { onSeverityChange(severity) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = severity.icon,
                                    contentDescription = severity.label,
                                    modifier = Modifier.size(14.dp),
                                    tint = severity.color
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(severity.label)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filtre par utilisateur
            Text(
                text = "Utilisateur",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedUserId == null,
                        onClick = { onUserIdChange(null) },
                        label = { Text("Tous") }
                    )
                }

                items(familyMembers) { member ->
                    FilterChip(
                        selected = selectedUserId == member.id,
                        onClick = { onUserIdChange(member.id) },
                        label = { Text(member.prenom) }
                    )
                }
            }
        }
    }
}

/**
 * Carte d'entrée d'audit
 */
@Composable
private fun AuditEntryCard(
    entry: AuditEntry,
    familyMembers: List<MembreFamille>,
    onClick: () -> Unit
) {
    val user = familyMembers.find { it.id == entry.userId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = false, onClick = onClick),
        elevation = 2.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Indicateur de sévérité
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            entry.severity.color,
                            shape = MaterialTheme.shapes.small
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Icône d'action
            Icon(
                imageVector = entry.actionType.icon,
                contentDescription = "Type d'action",
                tint = entry.actionType.color,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // En-tête avec timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatAuditTimestamp(entry.timestamp),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = entry.actionType.label,
                        style = MaterialTheme.typography.caption,
                        color = entry.actionType.color,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Description de l'action
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Utilisateur
                user?.let {
                    Text(
                        text = "Par ${it.prenom} ${it.nom}",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Flèche d'accès aux détails
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Voir détails",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * État vide pour les journaux d'audit
 */
@Composable
private fun EmptyAuditState(
    hasFilters: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Aucun journal",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (hasFilters) {
                    "Aucun journal ne correspond aux filtres"
                } else {
                    "Aucun journal d'audit"
                },
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Text(
                text = if (hasFilters) {
                    "Essayez de modifier vos critères de recherche"
                } else {
                    "Les activités système apparaîtront ici"
                },
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Dialog de détails d'une entrée d'audit
 */
@Composable
private fun AuditEntryDetailDialog(
    entry: AuditEntry,
    familyMembers: List<MembreFamille>,
    onDismiss: () -> Unit
) {
    val user = familyMembers.find { it.id == entry.userId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = entry.actionType.icon,
                    contentDescription = "Type",
                    tint = entry.actionType.color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Détails de l'audit")
            }
        },
        text = {
            Column {
                DetailRow("Horodatage", entry.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                DetailRow("Action", entry.actionType.label)
                DetailRow("Utilisateur", user?.let { "${it.prenom} ${it.nom}" } ?: "Utilisateur inconnu")
                DetailRow("Description", entry.description)
                DetailRow("Sévérité", entry.severity.label)
                DetailRow("Adresse IP", entry.ipAddress)
                DetailRow("User Agent", entry.userAgent)

                if (entry.details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Détails supplémentaires:",
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Medium
                    )
                    entry.details.forEach { (key, value) ->
                        DetailRow(key, value)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

/**
 * Ligne de détail
 */
@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.weight(1f)
        )
    }
}

// ================================================================
// CLASSES ET ENUMS DE DONNÉES
// ================================================================

data class AuditEntry(
    val id: Int,
    val timestamp: LocalDateTime,
    val userId: Int,
    val actionType: AuditActionType,
    val description: String,
    val ipAddress: String,
    val userAgent: String,
    val severity: AuditSeverity,
    val details: Map<String, String> = emptyMap()
)

enum class AuditActionType(val label: String, val icon: ImageVector, val color: Color) {
    LOGIN("Connexion", Icons.Default.Login, Color.Blue),
    LOGOUT("Déconnexion", Icons.Default.Logout, Color.Gray),
    CREATE("Création", Icons.Default.Add, Color.Green),
    UPDATE("Modification", Icons.Default.Edit, Color.Orange),
    DELETE("Suppression", Icons.Default.Delete, Color.Red),
    PERMISSION_GRANTED("Permission accordée", Icons.Default.Security, Color.Green),
    PERMISSION_REVOKED("Permission révoquée", Icons.Default.Block, Color.Red),
    FILE_UPLOADED("Fichier ajouté", Icons.Default.Upload, Color.Blue),
    FILE_DOWNLOADED("Fichier téléchargé", Icons.Default.Download, Color.Blue),
    SECURITY_ALERT("Alerte sécurité", Icons.Default.Warning, Color.Red)
}

enum class AuditSeverity(val label: String, val icon: ImageVector, val color: Color) {
    LOW("Faible", Icons.Default.Info, Color.Green),
    MEDIUM("Moyen", Icons.Default.Warning, Color.Orange),
    HIGH("Élevé", Icons.Default.Error, Color.Red),
    CRITICAL("Critique", Icons.Default.ReportProblem, Color(0xFF8B0000))
}

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun mapToActionType(action: String): AuditActionType = when (action.uppercase()) {
    "CREATE" -> AuditActionType.CREATE
    "UPDATE" -> AuditActionType.UPDATE
    "DELETE" -> AuditActionType.DELETE
    "LOGIN" -> AuditActionType.LOGIN
    "LOGOUT" -> AuditActionType.LOGOUT
    else -> AuditActionType.UPDATE
}

private fun mapToSeverity(action: String): AuditSeverity = when (action.uppercase()) {
    "DELETE" -> AuditSeverity.HIGH
    "CREATE", "UPDATE" -> AuditSeverity.MEDIUM
    else -> AuditSeverity.LOW
}

private fun formatAuditTimestamp(timestamp: LocalDateTime): String {
    val now = LocalDateTime.now()
    val diff = Duration.between(timestamp, now)

    return when {
        diff.toDays() > 0 -> timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        diff.toHours() > 0 -> "Il y a ${diff.toHours()}h"
        diff.toMinutes() > 0 -> "Il y a ${diff.toMinutes()}min"
        else -> "À l'instant"
    }
}

private fun hasActiveFilters(
    actionType: AuditActionType?,
    severity: AuditSeverity?,
    userId: Int?,
    dateRange: Pair<LocalDateTime?, LocalDateTime?>,
    searchQuery: String
): Boolean {
    return actionType != null ||
            severity != null ||
            userId != null ||
            dateRange.first != null ||
            dateRange.second != null ||
            searchQuery.isNotEmpty()
}