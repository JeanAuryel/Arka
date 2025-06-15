// ================================================================
// SPACEDETAILSSCREEN.KT - ÉCRAN DÉTAILS D'UN ESPACE ARKA
// ================================================================

package ui.screens.spaces

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import controllers.*
import ktorm.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*

/**
 * Écran de détails d'un espace Arka
 *
 * Fonctionnalités:
 * - Vue détaillée du contenu d'un espace
 * - Navigation vers les catégories et dossiers
 * - Gestion des permissions d'accès
 * - Statistiques de l'espace
 * - Actions contextuelles selon les permissions
 */
@Composable
fun SpaceDetailsScreen(
    spaceId: Int,
    spaceName: String,
    spaceController: SpaceController,
    categoryController: CategoryController,
    fileController: FileController,
    permissionController: PermissionController,
    authController: AuthController,
    onNavigateToCategory: (Int) -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var spaceDetails by remember { mutableStateOf<EspaceDetails?>(null) }
    var categories by remember { mutableStateOf<List<Categorie>>(emptyList()) }
    var recentFiles by remember { mutableStateOf<List<Fichier>>(emptyList()) }
    var spaceMembers by remember { mutableStateOf<List<MembreFamille>>(emptyList()) }
    var userPermissions by remember { mutableStateOf<PermissionLevel?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Charger les données de l'espace
    LaunchedEffect(spaceId) {
        scope.launch {
            isLoading = true
            try {
                // Charger les détails de l'espace
                val spaceResult = spaceController.getSpaceDetails(spaceId)
                if (spaceResult is SpaceController.SpaceResult.Success) {
                    spaceDetails = spaceResult.data
                }

                // Charger les catégories de l'espace
                val categoriesResult = categoryController.getCategoriesBySpace(spaceId)
                if (categoriesResult is CategoryController.CategoryResult.Success) {
                    categories = categoriesResult.data
                }

                // Charger les fichiers récents
                val filesResult = fileController.getRecentFilesBySpace(spaceId, 8)
                if (filesResult is FileController.FileResult.Success) {
                    recentFiles = filesResult.data
                }

                // Charger les membres avec accès
                val membersResult = spaceController.getSpaceMembers(spaceId)
                if (membersResult is SpaceController.SpaceResult.Success) {
                    spaceMembers = membersResult.data
                }

                // Vérifier les permissions de l'utilisateur
                currentUser?.let { user ->
                    val permissionResult = permissionController.getUserPermissionLevel(user.membreFamilleId, spaceId)
                    if (permissionResult is PermissionController.PermissionResult.Success) {
                        userPermissions = permissionResult.data
                    }
                }

            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barre de navigation supérieure
        ArkaTopAppBar(
            title = spaceName,
            subtitle = spaceDetails?.description,
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
            actions = {
                // Permissions
                if (userPermissions?.canManagePermissions == true) {
                    ArkaIconButton(
                        icon = Icons.Default.Security,
                        onClick = onNavigateToPermissions,
                        tooltip = "Gérer les permissions"
                    )
                }

                // Paramètres
                if (userPermissions?.canManageSpace == true) {
                    ArkaIconButton(
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToSettings,
                        tooltip = "Paramètres de l'espace"
                    )
                }

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
                            showPermissionDialog = true
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Demander un accès")
                        }

                        if (userPermissions?.canCreateContent == true) {
                            DropdownMenuItem(onClick = {
                                showMenu = false
                                // TODO: Navigation vers création catégorie
                            }) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Nouvelle catégorie")
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
                        message = "Chargement de l'espace..."
                    )
                }
            }

            errorMessage != null -> {
                ArkaErrorState(
                    message = errorMessage!!,
                    onRetry = {
                        errorMessage = null
                        scope.launch {
                            // Relancer le chargement
                        }
                    }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Informations de l'espace
                    item {
                        SpaceInfoCard(
                            spaceDetails = spaceDetails,
                            userPermissions = userPermissions,
                            spaceMembers = spaceMembers
                        )
                    }

                    // Statistiques rapides
                    item {
                        SpaceStatsSection(
                            categories = categories,
                            recentFiles = recentFiles,
                            spaceDetails = spaceDetails
                        )
                    }

                    // Actions rapides
                    if (userPermissions?.canReadContent == true) {
                        item {
                            QuickActionsSection(
                                userPermissions = userPermissions,
                                onCreateCategory = { /* TODO */ },
                                onUploadFile = { /* TODO */ },
                                onInviteMember = { showPermissionDialog = true }
                            )
                        }
                    }

                    // Catégories de l'espace
                    if (categories.isNotEmpty()) {
                        item {
                            SpaceCategoriesSection(
                                categories = categories,
                                onCategoryClick = onNavigateToCategory,
                                userPermissions = userPermissions
                            )
                        }
                    }

                    // Fichiers récents
                    if (recentFiles.isNotEmpty()) {
                        item {
                            RecentFilesSection(
                                files = recentFiles,
                                userPermissions = userPermissions
                            )
                        }
                    }

                    // Membres de l'espace
                    if (spaceMembers.isNotEmpty()) {
                        item {
                            SpaceMembersSection(
                                members = spaceMembers,
                                userPermissions = userPermissions,
                                onManagePermissions = onNavigateToPermissions
                            )
                        }
                    }

                    // Espace pour éviter que le FAB cache le contenu
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialogue de demande de permission
    if (showPermissionDialog) {
        RequestPermissionDialog(
            spaceId = spaceId,
            spaceName = spaceName,
            onDismiss = { showPermissionDialog = false },
            onSuccess = {
                showPermissionDialog = false
                // Rafraîchir les données
            }
        )
    }
}

/**
 * Carte d'informations de l'espace
 */
@Composable
private fun SpaceInfoCard(
    spaceDetails: EspaceDetails?,
    userPermissions: PermissionLevel?,
    spaceMembers: List<MembreFamille>
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when (spaceDetails?.type) {
                        "PERSONNEL" -> Icons.Default.PersonalVideo
                        "FAMILIAL" -> Icons.Default.Group
                        "PARTAGE" -> Icons.Default.Share
                        else -> Icons.Default.Storage
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(32.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spaceDetails?.nom ?: "Espace",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                    )

                    spaceDetails?.description?.let { description ->
                        Text(
                            text = description,
                            style = ArkaTextStyles.cardDescription,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Badge de permission
                userPermissions?.let { permissions ->
                    Surface(
                        color = when {
                            permissions.canManageSpace -> MaterialTheme.colors.primary
                            permissions.canCreateContent -> MaterialTheme.colors.secondary
                            permissions.canReadContent -> MaterialTheme.colors.surface
                            else -> MaterialTheme.colors.error
                        }.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = when {
                                permissions.canManageSpace -> "Admin"
                                permissions.canCreateContent -> "Éditeur"
                                permissions.canReadContent -> "Lecteur"
                                else -> "Aucun accès"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = ArkaTextStyles.badge,
                            color = when {
                                permissions.canManageSpace -> MaterialTheme.colors.primary
                                permissions.canCreateContent -> MaterialTheme.colors.secondary
                                permissions.canReadContent -> MaterialTheme.colors.onSurface
                                else -> MaterialTheme.colors.error
                            }
                        )
                    }
                }
            }

            // Informations supplémentaires
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                InfoItem(
                    icon = Icons.Default.Group,
                    label = "Membres",
                    value = "${spaceMembers.size}"
                )

                spaceDetails?.let { details ->
                    InfoItem(
                        icon = Icons.Default.Category,
                        label = "Catégories",
                        value = "${details.categoriesCount}"
                    )

                    InfoItem(
                        icon = Icons.Default.InsertDriveFile,
                        label = "Fichiers",
                        value = "${details.filesCount}"
                    )

                    InfoItem(
                        icon = Icons.Default.Storage,
                        label = "Taille",
                        value = formatFileSize(details.totalSize)
                    )
                }
            }
        }
    }
}

/**
 * Section des statistiques de l'espace
 */
@Composable
private fun SpaceStatsSection(
    categories: List<Categorie>,
    recentFiles: List<Fichier>,
    spaceDetails: EspaceDetails?
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Statistiques",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StatCard(
                        title = "Catégories",
                        value = "${categories.size}",
                        icon = Icons.Default.Category,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.width(140.dp)
                    )
                }

                item {
                    StatCard(
                        title = "Fichiers récents",
                        value = "${recentFiles.size}",
                        icon = Icons.Default.Description,
                        color = MaterialTheme.colors.secondary,
                        modifier = Modifier.width(140.dp)
                    )
                }

                spaceDetails?.let { details ->
                    item {
                        StatCard(
                            title = "Total fichiers",
                            value = "${details.filesCount}",
                            icon = Icons.Default.InsertDriveFile,
                            color = MaterialTheme.colors.arka.success,
                            modifier = Modifier.width(140.dp)
                        )
                    }

                    item {
                        StatCard(
                            title = "Stockage utilisé",
                            value = formatFileSize(details.totalSize),
                            icon = Icons.Default.Storage,
                            color = MaterialTheme.colors.arka.warning,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section des actions rapides
 */
@Composable
private fun QuickActionsSection(
    userPermissions: PermissionLevel?,
    onCreateCategory: () -> Unit,
    onUploadFile: () -> Unit,
    onInviteMember: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Actions rapides",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (userPermissions?.canCreateContent == true) {
                    QuickActionButton(
                        text = "Nouvelle catégorie",
                        icon = Icons.Default.CreateNewFolder,
                        onClick = onCreateCategory,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionButton(
                        text = "Ajouter fichier",
                        icon = Icons.Default.CloudUpload,
                        onClick = onUploadFile,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    QuickActionButton(
                        text = "Parcourir",
                        icon = Icons.Default.Search,
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f)
                    )
                }

                QuickActionButton(
                    text = "Inviter",
                    icon = Icons.Default.PersonAdd,
                    onClick = onInviteMember,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Section des catégories de l'espace
 */
@Composable
private fun SpaceCategoriesSection(
    categories: List<Categorie>,
    onCategoryClick: (Int) -> Unit,
    userPermissions: PermissionLevel?
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
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
                Text(
                    text = "Catégories",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )

                if (userPermissions?.canCreateContent == true) {
                    TextButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Ajouter")
                    }
                }
            }

            categories.take(6).forEach { category ->
                CategoryItem(
                    category = category,
                    onClick = { onCategoryClick(category.categorieId) }
                )
            }

            if (categories.size > 6) {
                TextButton(
                    onClick = { /* TODO: Voir toutes les catégories */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voir toutes les catégories (${categories.size})")
                }
            }
        }
    }
}

/**
 * Item de catégorie
 */
@Composable
private fun CategoryItem(
    category: Categorie,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = getCategoryIcon(category.typeDossier),
            contentDescription = null,
            tint = ArkaColorUtils.getCategoryColor(category.typeDossier),
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.nomCategorie,
                style = ArkaTextStyles.categoryName,
                color = MaterialTheme.colors.onSurface
            )

            category.description?.let { description ->
                Text(
                    text = description,
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * Section des fichiers récents
 */
@Composable
private fun RecentFilesSection(
    files: List<Fichier>,
    userPermissions: PermissionLevel?
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Fichiers récents",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            files.take(5).forEach { file ->
                FileItem(
                    file = file,
                    userPermissions = userPermissions
                )
            }

            if (files.size > 5) {
                TextButton(
                    onClick = { /* TODO: Voir tous les fichiers */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voir tous les fichiers récents")
                }
            }
        }
    }
}

/**
 * Item de fichier
 */
@Composable
private fun FileItem(
    file: Fichier,
    userPermissions: PermissionLevel?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = getFileIcon(file.typeFichier),
            contentDescription = null,
            tint = ArkaColorUtils.getFileTypeColor(file.typeFichier),
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.nomFichier,
                style = ArkaTextStyles.fileName,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = "Modifié le ${file.dateDerniereModifFichier?.toLocalDate() ?: ""}",
                style = ArkaTextStyles.metadata,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }

        Text(
            text = formatFileSize(file.tailleFichier),
            style = ArkaTextStyles.fileSize,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        if (userPermissions?.canReadContent == true) {
            ArkaIconButton(
                icon = Icons.Default.OpenInNew,
                onClick = { /* TODO: Ouvrir fichier */ },
                size = 20.dp
            )
        }
    }
}

/**
 * Section des membres de l'espace
 */
@Composable
private fun SpaceMembersSection(
    members: List<MembreFamille>,
    userPermissions: PermissionLevel?,
    onManagePermissions: () -> Unit
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
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
                Text(
                    text = "Membres avec accès (${members.size})",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )

                if (userPermissions?.canManagePermissions == true) {
                    TextButton(onClick = onManagePermissions) {
                        Text("Gérer", style = ArkaTextStyles.link)
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members.take(8)) { member ->
                    MemberChip(member = member)
                }

                if (members.size > 8) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colors.surface
                        ) {
                            Text(
                                text = "+${members.size - 8}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = ArkaTextStyles.chip
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composant de membre
 */
@Composable
private fun MemberChip(member: MembreFamille) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = "${member.prenom} ${member.nom}",
                style = ArkaTextStyles.chip,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

/**
 * Dialogue de demande de permission
 */
@Composable
private fun RequestPermissionDialog(
    spaceId: Int,
    spaceName: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var selectedPermission by remember { mutableStateOf("READ") }
    var justification by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Demander un accès")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Demander un accès à l'espace \"$spaceName\"")

                // Sélection du type de permission
                Column {
                    Text("Type d'accès souhaité :", style = ArkaTextStyles.label)
                    Spacer(Modifier.height(8.dp))

                    RadioButton(
                        selected = selectedPermission == "READ",
                        onClick = { selectedPermission = "READ" }
                    )
                    Text("Lecture seule")

                    RadioButton(
                        selected = selectedPermission == "WRITE",
                        onClick = { selectedPermission = "WRITE" }
                    )
                    Text("Lecture et écriture")
                }

                // Justification
                OutlinedTextField(
                    value = justification,
                    onValueChange = { justification = it },
                    label = { Text("Justification (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // TODO: Envoyer la demande de permission
                    onSuccess()
                }
            ) {
                Text("Envoyer la demande")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// ================================================================
// UTILITAIRES
// ================================================================

private fun getCategoryIcon(type: String?): ImageVector {
    return when (type?.lowercase()) {
        "documents" -> Icons.Default.Description
        "images" -> Icons.Default.Image
        "videos" -> Icons.Default.VideoFile
        "audio" -> Icons.Default.AudioFile
        "archives" -> Icons.Default.Archive
        else -> Icons.Default.Folder
    }
}

private fun getFileIcon(fileType: String?): ImageVector {
    return when (fileType?.lowercase()) {
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx" -> Icons.Default.TableChart
        "ppt", "pptx" -> Icons.Default.Slideshow
        "jpg", "jpeg", "png", "gif", "bmp" -> Icons.Default.Image
        "mp4", "avi", "mov", "wmv" -> Icons.Default.VideoFile
        "mp3", "wav" -> Icons.Default.AudioFile
        "zip", "rar", "7z" -> Icons.Default.Archive
        "txt" -> Icons.Default.TextSnippet
        else -> Icons.Default.InsertDriveFile
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

/**
 * Bouton d'action rapide
 */
@Composable
private fun QuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ArkaOutlinedButton(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Item d'information
 */
@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = "$value $label",
            style = ArkaTextStyles.metadata,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )
    }
}

// ================================================================
// DATA CLASSES POUR LES TYPES MANQUANTS
// ================================================================

/**
 * Détails d'un espace (placeholder - à adapter selon votre modèle)
 */
data class EspaceDetails(
    val nom: String,
    val description: String?,
    val type: String,
    val categoriesCount: Int,
    val filesCount: Int,
    val totalSize: Long
)

/**
 * Niveau de permission (placeholder - à adapter selon votre modèle)
 */
data class PermissionLevel(
    val canReadContent: Boolean,
    val canCreateContent: Boolean,
    val canManagePermissions: Boolean,
    val canManageSpace: Boolean
)