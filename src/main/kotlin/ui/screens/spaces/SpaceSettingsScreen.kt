// ================================================================
// SPACESETTINGSSCREEN.KT - CONFIGURATION DES ESPACES
// ================================================================

package ui.screens.spaces

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import controllers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*
import java.time.LocalDateTime

/**
 * Écran de configuration des espaces
 *
 * Fonctionnalités:
 * - Gestion des espaces familiaux
 * - Création et modification d'espaces
 * - Configuration des paramètres par espace
 * - Gestion des templates par défaut
 * - Paramètres de sécurité et confidentialité
 */
@Composable
fun SpaceSettingsScreen(
    spaceController: SpaceController, // À créer
    familyController: FamilyController,
    authController: AuthController,
    onNavigateBack: () -> Unit,
    onNavigateToPermissions: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // Vérification des droits d'accès (seuls les parents peuvent gérer les espaces)
    if (currentUser?.role != RoleFamille.PARENT) {
        AccessDeniedContent(onNavigateBack = onNavigateBack)
        return
    }

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var spaces by remember { mutableStateOf<List<SpaceInfo>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // États des dialogs
    var showCreateSpaceDialog by remember { mutableStateOf(false) }
    var editingSpace by remember { mutableStateOf<SpaceInfo?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<SpaceInfo?>(null) }
    var showTemplatesDialog by remember { mutableStateOf(false) }

    // Charger les espaces
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            scope.launch {
                isLoading = true
                try {
                    // Mock data - en réalité, charger depuis spaceController
                    spaces = listOf(
                        SpaceInfo(
                            id = 1,
                            nom = "Documents Personnels",
                            description = "Espace pour les documents personnels de chaque membre",
                            icone = "📄",
                            couleur = "#2196F3",
                            isPublic = false,
                            maxFileSize = 50,
                            allowedFileTypes = listOf("PDF", "DOC", "DOCX", "JPG", "PNG"),
                            autoCleanup = true,
                            cleanupDays = 365,
                            memberCount = 4,
                            fileCount = 23,
                            createdAt = LocalDateTime.now().minusDays(30)
                        ),
                        SpaceInfo(
                            id = 2,
                            nom = "Photos Famille",
                            description = "Albums photos partagés de la famille",
                            icone = "📸",
                            couleur = "#4CAF50",
                            isPublic = true,
                            maxFileSize = 20,
                            allowedFileTypes = listOf("JPG", "PNG", "GIF", "HEIC"),
                            autoCleanup = false,
                            cleanupDays = 0,
                            memberCount = 6,
                            fileCount = 156,
                            createdAt = LocalDateTime.now().minusDays(60)
                        ),
                        SpaceInfo(
                            id = 3,
                            nom = "Devoirs Enfants",
                            description = "Espace dédié aux devoirs et travaux scolaires",
                            icone = "📚",
                            couleur = "#FF9800",
                            isPublic = false,
                            maxFileSize = 30,
                            allowedFileTypes = listOf("PDF", "DOC", "DOCX", "TXT"),
                            autoCleanup = true,
                            cleanupDays = 180,
                            memberCount = 3,
                            fileCount = 45,
                            createdAt = LocalDateTime.now().minusDays(15)
                        )
                    )

                } catch (e: Exception) {
                    errorMessage = "Erreur lors du chargement: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête avec actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Configuration des espaces",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bouton templates
            OutlinedButton(
                onClick = { showTemplatesDialog = true }
            ) {
                Icon(Icons.Default.Template, contentDescription = "Templates")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Templates")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bouton créer un espace
            Button(
                onClick = { showCreateSpaceDialog = true },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Créer")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Créer un espace")
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
        } else if (spaces.isEmpty()) {
            EmptySpacesState(onCreateSpace = { showCreateSpaceDialog = true })
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(spaces) { space ->
                    SpaceConfigCard(
                        space = space,
                        onEdit = { editingSpace = space },
                        onDelete = { showDeleteConfirmation = space },
                        onManagePermissions = { onNavigateToPermissions(space.id) },
                        onTogglePublic = { isPublic ->
                            // Logique pour changer la visibilité
                            scope.launch {
                                try {
                                    // spaceController.updateSpaceVisibility(space.id, isPublic)
                                    spaces = spaces.map {
                                        if (it.id == space.id) it.copy(isPublic = isPublic) else it
                                    }
                                    successMessage = "Visibilité mise à jour"
                                } catch (e: Exception) {
                                    errorMessage = "Erreur: ${e.message}"
                                }
                            }
                        }
                    )
                }
            }
        }

        // Messages de feedback
        successMessage?.let { message ->
            LaunchedEffect(message) {
                delay(3000)
                successMessage = null
            }

            Snackbar(
                modifier = Modifier.padding(16.dp),
                backgroundColor = Color.Green.copy(alpha = 0.8f)
            ) {
                Text(text = message, color = Color.White)
            }
        }

        errorMessage?.let { message ->
            LaunchedEffect(message) {
                delay(5000)
                errorMessage = null
            }

            Snackbar(
                modifier = Modifier.padding(16.dp),
                backgroundColor = Color.Red.copy(alpha = 0.8f)
            ) {
                Text(text = message, color = Color.White)
            }
        }

        // Dialogs
        if (showCreateSpaceDialog) {
            CreateSpaceDialog(
                onDismiss = { showCreateSpaceDialog = false },
                onConfirm = { newSpace ->
                    scope.launch {
                        try {
                            // spaceController.createSpace(newSpace)
                            spaces = spaces + newSpace.copy(id = (spaces.maxOfOrNull { it.id } ?: 0) + 1)
                            successMessage = "Espace créé avec succès"
                        } catch (e: Exception) {
                            errorMessage = "Erreur lors de la création: ${e.message}"
                        }
                    }
                    showCreateSpaceDialog = false
                }
            )
        }

        editingSpace?.let { space ->
            EditSpaceDialog(
                space = space,
                onDismiss = { editingSpace = null },
                onConfirm = { updatedSpace ->
                    scope.launch {
                        try {
                            // spaceController.updateSpace(updatedSpace)
                            spaces = spaces.map {
                                if (it.id == updatedSpace.id) updatedSpace else it
                            }
                            successMessage = "Espace mis à jour"
                        } catch (e: Exception) {
                            errorMessage = "Erreur lors de la mise à jour: ${e.message}"
                        }
                    }
                    editingSpace = null
                }
            )
        }

        showDeleteConfirmation?.let { space ->
            DeleteSpaceConfirmationDialog(
                space = space,
                onDismiss = { showDeleteConfirmation = null },
                onConfirm = {
                    scope.launch {
                        try {
                            // spaceController.deleteSpace(space.id)
                            spaces = spaces.filter { it.id != space.id }
                            successMessage = "Espace supprimé"
                        } catch (e: Exception) {
                            errorMessage = "Erreur lors de la suppression: ${e.message}"
                        }
                    }
                    showDeleteConfirmation = null
                }
            )
        }

        if (showTemplatesDialog) {
            SpaceTemplatesDialog(
                onDismiss = { showTemplatesDialog = false },
                onSelectTemplate = { template ->
                    // Pré-remplir le dialog de création avec le template
                    showTemplatesDialog = false
                    showCreateSpaceDialog = true
                }
            )
        }
    }
}

/**
 * Carte de configuration d'espace
 */
@Composable
private fun SpaceConfigCard(
    space: SpaceInfo,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManagePermissions: () -> Unit,
    onTogglePublic: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // En-tête avec icône et nom
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icône colorée
                Card(
                    backgroundColor = Color(android.graphics.Color.parseColor(space.couleur)).copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = space.icone,
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = space.nom,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = space.description,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Menu d'actions
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(onClick = { onEdit(); showMenu = false }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Modifier")
                        }

                        DropdownMenuItem(onClick = { onManagePermissions(); showMenu = false }) {
                            Icon(Icons.Default.Security, contentDescription = "Permissions")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Permissions")
                        }

                        Divider()

                        DropdownMenuItem(onClick = { onDelete(); showMenu = false }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Supprimer", color = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistiques rapides
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpaceStatItem("Membres", space.memberCount.toString(), Icons.Default.People)
                SpaceStatItem("Fichiers", space.fileCount.toString(), Icons.Default.FilePresent)
                SpaceStatItem("Taille max", "${space.maxFileSize}MB", Icons.Default.Storage)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Paramètres rapides
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Switch(
                        checked = space.isPublic,
                        onCheckedChange = onTogglePublic
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (space.isPublic) "Public" else "Privé",
                        style = MaterialTheme.typography.body2
                    )
                }

                if (space.autoCleanup) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Nettoyage auto",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nettoyage ${space.cleanupDays}j",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Types de fichiers autorisés
            if (space.allowedFileTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Types autorisés: ${space.allowedFileTypes.joinToString(", ")}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Item de statistique d'espace
 */
@Composable
private fun SpaceStatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colors.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * État vide pour les espaces
 */
@Composable
private fun EmptySpacesState(
    onCreateSpace: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FolderSpecial,
                contentDescription = "Aucun espace",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Aucun espace configuré",
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = "Créez votre premier espace familial",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCreateSpace,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Créer")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Créer un espace")
            }
        }
    }
}

/**
 * Contenu d'accès refusé
 */
@Composable
private fun AccessDeniedContent(
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = 8.dp,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = "Accès refusé",
                    modifier = Modifier.size(64.dp),
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Accès refusé",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Seuls les parents peuvent configurer les espaces",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateBack) {
                    Text("Retour")
                }
            }
        }
    }
}

/**
 * Dialog de création d'espace
 */
@Composable
private fun CreateSpaceDialog(
    onDismiss: () -> Unit,
    onConfirm: (SpaceInfo) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var icone by remember { mutableStateOf("📁") }
    var couleur by remember { mutableStateOf("#2196F3") }
    var isPublic by remember { mutableStateOf(false) }
    var maxFileSize by remember { mutableStateOf(50) }
    var autoCleanup by remember { mutableStateOf(true) }
    var cleanupDays by remember { mutableStateOf(365) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un nouvel espace") },
        text = {
            Column {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom de l'espace") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    OutlinedTextField(
                        value = icone,
                        onValueChange = { icone = it },
                        label = { Text("Icône") },
                        modifier = Modifier.weight(0.3f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = maxFileSize.toString(),
                        onValueChange = { maxFileSize = it.toIntOrNull() ?: 50 },
                        label = { Text("Taille max (MB)") },
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it }
                    )
                    Text("Espace public")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoCleanup,
                        onCheckedChange = { autoCleanup = it }
                    )
                    Text("Nettoyage automatique")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        SpaceInfo(
                            id = 0, // Sera assigné par le serveur
                            nom = nom,
                            description = description,
                            icone = icone,
                            couleur = couleur,
                            isPublic = isPublic,
                            maxFileSize = maxFileSize,
                            allowedFileTypes = listOf("PDF", "DOC", "JPG", "PNG"),
                            autoCleanup = autoCleanup,
                            cleanupDays = if (autoCleanup) cleanupDays else 0,
                            memberCount = 0,
                            fileCount = 0,
                            createdAt = LocalDateTime.now()
                        )
                    )
                },
                enabled = nom.isNotBlank()
            ) {
                Text("Créer")
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
 * Dialog de modification d'espace
 */
@Composable
private fun EditSpaceDialog(
    space: SpaceInfo,
    onDismiss: () -> Unit,
    onConfirm: (SpaceInfo) -> Unit
) {
    var nom by remember { mutableStateOf(space.nom) }
    var description by remember { mutableStateOf(space.description) }
    var icone by remember { mutableStateOf(space.icone) }
    var maxFileSize by remember { mutableStateOf(space.maxFileSize) }
    var autoCleanup by remember { mutableStateOf(space.autoCleanup) }
    var cleanupDays by remember { mutableStateOf(space.cleanupDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier l'espace") },
        text = {
            Column {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom de l'espace") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    OutlinedTextField(
                        value = icone,
                        onValueChange = { icone = it },
                        label = { Text("Icône") },
                        modifier = Modifier.weight(0.3f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = maxFileSize.toString(),
                        onValueChange = { maxFileSize = it.toIntOrNull() ?: 50 },
                        label = { Text("Taille max (MB)") },
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = autoCleanup,
                        onCheckedChange = { autoCleanup = it }
                    )
                    Text("Nettoyage automatique")
                }

                if (autoCleanup) {
                    OutlinedTextField(
                        value = cleanupDays.toString(),
                        onValueChange = { cleanupDays = it.toIntOrNull() ?: 365 },
                        label = { Text("Nettoyage après (jours)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        space.copy(
                            nom = nom,
                            description = description,
                            icone = icone,
                            maxFileSize = maxFileSize,
                            autoCleanup = autoCleanup,
                            cleanupDays = if (autoCleanup) cleanupDays else 0
                        )
                    )
                },
                enabled = nom.isNotBlank()
            ) {
                Text("Sauvegarder")
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
 * Dialog de confirmation de suppression
 */
@Composable
private fun DeleteSpaceConfirmationDialog(
    space: SpaceInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Attention", tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Supprimer l'espace", color = Color.Red)
            }
        },
        text = {
            Column {
                Text("Êtes-vous sûr de vouloir supprimer l'espace \"${space.nom}\" ?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Cette action supprimera définitivement:",
                    fontWeight = FontWeight.Medium
                )
                Text("• ${space.fileCount} fichiers")
                Text("• Toutes les permissions associées")
                Text("• L'historique des activités")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Cette action est irréversible.",
                    color = Color.Red,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
            ) {
                Text("Supprimer définitivement", color = Color.White)
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
 * Dialog des templates d'espaces
 */
@Composable
private fun SpaceTemplatesDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (SpaceTemplate) -> Unit
) {
    val templates = listOf(
        SpaceTemplate("Documents Personnels", "📄", "Espace pour documents privés", false),
        SpaceTemplate("Photos Famille", "📸", "Albums photos partagés", true),
        SpaceTemplate("Devoirs École", "📚", "Travaux scolaires des enfants", false),
        SpaceTemplate("Projets Créatifs", "🎨", "Créations et projets artistiques", true),
        SpaceTemplate("Documents Administratifs", "📋", "Papiers officiels et administration", false)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir un template") },
        text = {
            LazyColumn {
                items(templates) { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = false,
                                onClick = { onSelectTemplate(template) }
                            ),
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(template.icone, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = template.nom,
                                    style = MaterialTheme.typography.body1,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            if (template.isPublic) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = "Public",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Green
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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

// ================================================================
// CLASSES DE DONNÉES
// ================================================================

data class SpaceInfo(
    val id: Int,
    val nom: String,
    val description: String,
    val icone: String,
    val couleur: String,
    val isPublic: Boolean,
    val maxFileSize: Int, // MB
    val allowedFileTypes: List<String>,
    val autoCleanup: Boolean,
    val cleanupDays: Int,
    val memberCount: Int,
    val fileCount: Int,
    val createdAt: LocalDateTime
)

data class SpaceTemplate(
    val nom: String,
    val icone: String,
    val description: String,
    val isPublic: Boolean
)

// Note: SpaceController sera à créer pour gérer les espaces