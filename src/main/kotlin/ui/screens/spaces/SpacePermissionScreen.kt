// ================================================================
// SPACEPERMISSIONSSCREEN.KT - GESTION DES PERMISSIONS PAR ESPACE
// ================================================================

package ui.screens.spaces

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import controllers.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*

/**
 * Écran de gestion des permissions par espace
 *
 * Fonctionnalités:
 * - Visualisation des permissions par espace familial
 * - Modification des droits d'accès (lecture, écriture, admin)
 * - Gestion des membres autorisés
 * - Historique des modifications de permissions
 */
@Composable
fun SpacePermissionsScreen(
    permissionController: PermissionController,
    familyMemberController: FamilyMemberController,
    auditController: JournalAuditPermissionController,
    spaceId: Int,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var permissions by remember { mutableStateOf<List<Permission>>(emptyList()) }
    var familyMembers by remember { mutableStateOf<List<MembreFamille>>(emptyList()) }
    var auditEntries by remember { mutableStateOf<List<JournalAuditPermission>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddPermissionDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<MembreFamille?>(null) }

    // Charger les données
    LaunchedEffect(spaceId) {
        scope.launch {
            isLoading = true
            try {
                // Charger les permissions existantes
                val permissionsResult = permissionController.getPermissionsBySpace(spaceId)
                if (permissionsResult is PermissionController.PermissionResult.Success) {
                    permissions = permissionsResult.data
                }

                // Charger les membres de la famille
                val membersResult = familyMemberController.getAllFamilyMembers()
                if (membersResult is FamilyMemberController.FamilyMemberResult.Success) {
                    familyMembers = membersResult.data
                }

                // Charger l'historique d'audit
                val auditResult = auditController.getAuditEntriesBySpace(spaceId)
                if (auditResult is JournalAuditPermissionController.AuditResult.Success) {
                    auditEntries = auditResult.data.take(10) // 10 dernières entrées
                }

            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement des permissions: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête avec navigation retour
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Permissions de l'espace",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showAddPermissionDialog = true },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter permission")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section permissions actuelles
                item {
                    PermissionsSection(
                        permissions = permissions,
                        familyMembers = familyMembers,
                        onUpdatePermission = { permission, newType ->
                            scope.launch {
                                val result = permissionController.updatePermission(
                                    permission.copy(typePermission = newType)
                                )
                                if (result is PermissionController.PermissionResult.Success) {
                                    // Recharger les permissions
                                    val updatedResult = permissionController.getPermissionsBySpace(spaceId)
                                    if (updatedResult is PermissionController.PermissionResult.Success) {
                                        permissions = updatedResult.data
                                    }
                                }
                            }
                        },
                        onDeletePermission = { permission ->
                            scope.launch {
                                val result = permissionController.deletePermission(permission.id)
                                if (result is PermissionController.PermissionResult.Success) {
                                    permissions = permissions.filter { it.id != permission.id }
                                }
                            }
                        }
                    )
                }

                // Section historique d'audit
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    AuditHistorySection(auditEntries = auditEntries)
                }
            }
        }

        // Gestion des erreurs
        errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Snackbar ou dialog d'erreur
            }
        }

        // Dialog d'ajout de permission
        if (showAddPermissionDialog) {
            AddPermissionDialog(
                familyMembers = familyMembers.filter { member ->
                    permissions.none { it.membreFamilleId == member.id }
                },
                onDismiss = { showAddPermissionDialog = false },
                onConfirm = { memberId, permissionType ->
                    scope.launch {
                        val newPermission = Permission(
                            id = 0, // Auto-généré
                            membreFamilleId = memberId,
                            espaceId = spaceId,
                            typePermission = permissionType
                        )

                        val result = permissionController.createPermission(newPermission)
                        if (result is PermissionController.PermissionResult.Success) {
                            // Recharger les permissions
                            val updatedResult = permissionController.getPermissionsBySpace(spaceId)
                            if (updatedResult is PermissionController.PermissionResult.Success) {
                                permissions = updatedResult.data
                            }
                        }
                    }
                    showAddPermissionDialog = false
                }
            )
        }
    }
}

/**
 * Section affichant les permissions actuelles
 */
@Composable
private fun PermissionsSection(
    permissions: List<Permission>,
    familyMembers: List<MembreFamille>,
    onUpdatePermission: (Permission, TypePermission) -> Unit,
    onDeletePermission: (Permission) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Permissions actuelles",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (permissions.isEmpty()) {
                Text(
                    text = "Aucune permission définie",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            } else {
                permissions.forEach { permission ->
                    val member = familyMembers.find { it.id == permission.membreFamilleId }

                    PermissionItem(
                        permission = permission,
                        memberName = member?.prenom ?: "Membre inconnu",
                        memberRole = member?.role ?: RoleFamille.ENFANT,
                        onUpdatePermission = onUpdatePermission,
                        onDeletePermission = onDeletePermission
                    )

                    if (permission != permissions.last()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Item individuel de permission
 */
@Composable
private fun PermissionItem(
    permission: Permission,
    memberName: String,
    memberRole: RoleFamille,
    onUpdatePermission: (Permission, TypePermission) -> Unit,
    onDeletePermission: (Permission) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icône et nom du membre
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (memberRole) {
                    RoleFamille.PARENT -> Icons.Default.AdminPanelSettings
                    RoleFamille.ENFANT -> Icons.Default.Child
                },
                contentDescription = "Rôle",
                tint = when (memberRole) {
                    RoleFamille.PARENT -> Color(0xFFF57C00)
                    RoleFamille.ENFANT -> Color(0xFF4CAF50)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = memberName,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = memberRole.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Sélecteur de permission
        Box {
            OutlinedButton(
                onClick = { showDropdown = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = getPermissionColor(permission.typePermission).copy(alpha = 0.1f),
                    contentColor = getPermissionColor(permission.typePermission)
                )
            ) {
                Text(getPermissionLabel(permission.typePermission))
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Modifier")
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                TypePermission.values().forEach { permissionType ->
                    DropdownMenuItem(
                        onClick = {
                            onUpdatePermission(permission, permissionType)
                            showDropdown = false
                        }
                    ) {
                        Text(
                            text = getPermissionLabel(permissionType),
                            color = getPermissionColor(permissionType)
                        )
                    }
                }
            }
        }

        // Bouton de suppression
        IconButton(
            onClick = { onDeletePermission(permission) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Supprimer",
                tint = Color.Red.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Section de l'historique d'audit
 */
@Composable
private fun AuditHistorySection(
    auditEntries: List<JournalAuditPermission>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = "Historique")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Historique des modifications",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (auditEntries.isEmpty()) {
                Text(
                    text = "Aucune modification récente",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            } else {
                auditEntries.forEach { entry ->
                    AuditEntryItem(entry = entry)

                    if (entry != auditEntries.last()) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

/**
 * Item d'entrée d'audit
 */
@Composable
private fun AuditEntryItem(
    entry: JournalAuditPermission
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (entry.action) {
                "CREATE" -> Icons.Default.Add
                "UPDATE" -> Icons.Default.Edit
                "DELETE" -> Icons.Default.Delete
                else -> Icons.Default.Info
            },
            contentDescription = "Action",
            tint = when (entry.action) {
                "CREATE" -> Color.Green
                "UPDATE" -> Color.Orange
                "DELETE" -> Color.Red
                else -> Color.Gray
            },
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = entry.description,
                style = MaterialTheme.typography.body2
            )
            Text(
                text = "Par ${entry.membreFamilleId} • ${entry.dateHeure}",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Dialog d'ajout de permission
 */
@Composable
private fun AddPermissionDialog(
    familyMembers: List<MembreFamille>,
    onDismiss: () -> Unit,
    onConfirm: (Int, TypePermission) -> Unit
) {
    var selectedMemberId by remember { mutableStateOf<Int?>(null) }
    var selectedPermissionType by remember { mutableStateOf(TypePermission.LECTURE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ajouter une permission")
        },
        text = {
            Column {
                Text("Sélectionnez un membre et définissez ses permissions :")

                Spacer(modifier = Modifier.height(16.dp))

                // Sélection du membre
                familyMembers.forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMemberId == member.id,
                            onClick = { selectedMemberId = member.id }
                        )
                        Text(
                            text = "${member.prenom} (${member.role.name.lowercase()})",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sélection du type de permission
                Text("Type de permission :")
                TypePermission.values().forEach { permissionType ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPermissionType == permissionType,
                            onClick = { selectedPermissionType = permissionType }
                        )
                        Text(
                            text = getPermissionLabel(permissionType),
                            modifier = Modifier.padding(start = 8.dp),
                            color = getPermissionColor(permissionType)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedMemberId?.let { memberId ->
                        onConfirm(memberId, selectedPermissionType)
                    }
                },
                enabled = selectedMemberId != null
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// Fonctions utilitaires
private fun getPermissionLabel(type: TypePermission): String = when (type) {
    TypePermission.LECTURE -> "Lecture seule"
    TypePermission.ECRITURE -> "Lecture/Écriture"
    TypePermission.ADMIN -> "Administration"
}

private fun getPermissionColor(type: TypePermission): Color = when (type) {
    TypePermission.LECTURE -> Color(0xFF4CAF50)
    TypePermission.ECRITURE -> Color(0xFFF57C00)
    TypePermission.ADMIN -> Color(0xFFF44336)
}