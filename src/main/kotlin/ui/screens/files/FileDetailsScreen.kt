// ================================================================
// FILEDETAILSSCREEN.KT - ÉCRAN DÉTAILS D'UN FICHIER ARKA
// ================================================================

package ui.screens.files

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
import androidx.compose.ui.unit.sp
import controllers.*
import ktorm.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran de détails d'un fichier Arka
 *
 * Fonctionnalités:
 * - Prévisualisation du fichier (selon le type)
 * - Métadonnées complètes du fichier
 * - Historique des versions
 * - Gestion des permissions de partage
 * - Actions contextuelles (télécharger, renommer, supprimer)
 * - Commentaires et annotations
 * - Tags et classification
 */
@Composable
fun FileDetailsScreen(
    fileId: Int,
    fileController: FileController,
    permissionController: PermissionController,
    versionController: VersionController,
    commentController: CommentController,
    authController: AuthController,
    onBack: () -> Unit,
    onNavigateToEdit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États de l'écran
    var isLoading by remember { mutableStateOf(true) }
    var fileDetails by remember { mutableStateOf<FileDetails?>(null) }
    var fileVersions by remember { mutableStateOf<List<FileVersion>>(emptyList()) }
    var fileComments by remember { mutableStateOf<List<FileComment>>(emptyList()) }
    var userPermissions by remember { mutableStateOf<FilePermissions?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    // Charger les données du fichier
    LaunchedEffect(fileId) {
        scope.launch {
            isLoading = true
            try {
                // Charger les détails du fichier
                val fileResult = fileController.getFileDetails(fileId)
                if (fileResult is FileController.FileResult.Success) {
                    fileDetails = fileResult.data
                }

                // Charger les versions
                val versionsResult = versionController.getFileVersions(fileId)
                if (versionsResult is VersionController.VersionResult.Success) {
                    fileVersions = versionsResult.data
                }

                // Charger les commentaires
                val commentsResult = commentController.getFileComments(fileId)
                if (commentsResult is CommentController.CommentResult.Success) {
                    fileComments = commentsResult.data
                }

                // Vérifier les permissions de l'utilisateur
                currentUser?.let { user ->
                    val permissionResult = permissionController.getFilePermissions(user.membreFamilleId, fileId)
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

    val tabs = listOf(
        "Aperçu" to Icons.Default.Visibility,
        "Métadonnées" to Icons.Default.Info,
        "Versions" to Icons.Default.History,
        "Commentaires" to Icons.Default.Comment
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Barre de navigation supérieure
        ArkaTopAppBar(
            title = fileDetails?.name ?: "Fichier",
            subtitle = fileDetails?.let { formatFileSize(it.size) },
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
            actions = {
                // Télécharger
                if (userPermissions?.canDownload == true) {
                    ArkaIconButton(
                        icon = Icons.Default.Download,
                        onClick = {
                            // TODO: Télécharger le fichier
                        },
                        tooltip = "Télécharger"
                    )
                }

                // Partager
                if (userPermissions?.canShare == true) {
                    ArkaIconButton(
                        icon = Icons.Default.Share,
                        onClick = { showShareDialog = true },
                        tooltip = "Partager"
                    )
                }

                // Éditer
                if (userPermissions?.canEdit == true) {
                    ArkaIconButton(
                        icon = Icons.Default.Edit,
                        onClick = onNavigateToEdit,
                        tooltip = "Éditer"
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
                        if (userPermissions?.canEdit == true) {
                            DropdownMenuItem(onClick = {
                                showMenu = false
                                showRenameDialog = true
                            }) {
                                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Renommer")
                            }

                            DropdownMenuItem(onClick = {
                                showMenu = false
                                showVersionDialog = true
                            }) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Nouvelle version")
                            }
                        }

                        if (userPermissions?.canDelete == true) {
                            DropdownMenuItem(onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Supprimer")
                            }
                        }

                        DropdownMenuItem(onClick = {
                            showMenu = false
                            // TODO: Copier le lien
                        }) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Copier le lien")
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
                        message = "Chargement du fichier..."
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

            fileDetails == null -> {
                ArkaErrorState(
                    message = "Fichier introuvable",
                    onRetry = { onBack() }
                )
            }

            else -> {
                Column {
                    // Informations principales du fichier
                    FileHeaderCard(
                        fileDetails = fileDetails!!,
                        userPermissions = userPermissions
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
                                icon = { Icon(icon, contentDescription = title) }
                            )
                        }
                    }

                    // Contenu des onglets
                    when (selectedTab) {
                        0 -> FilePreviewTab(fileDetails!!)
                        1 -> FileMetadataTab(fileDetails!!)
                        2 -> FileVersionsTab(fileVersions, userPermissions)
                        3 -> FileCommentsTab(fileComments, userPermissions, fileId)
                    }
                }
            }
        }
    }

    // Dialogues
    if (showShareDialog) {
        ShareFileDialog(
            fileDetails = fileDetails!!,
            onDismiss = { showShareDialog = false },
            onSuccess = {
                showShareDialog = false
                // Rafraîchir les données
            }
        )
    }

    if (showRenameDialog) {
        RenameFileDialog(
            currentName = fileDetails?.name ?: "",
            onDismiss = { showRenameDialog = false },
            onSuccess = { newName ->
                showRenameDialog = false
                // TODO: Renommer le fichier
            }
        )
    }

    if (showDeleteDialog) {
        DeleteFileDialog(
            fileName = fileDetails?.name ?: "",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                // TODO: Supprimer le fichier puis retourner
                onBack()
            }
        )
    }

    if (showVersionDialog) {
        UploadNewVersionDialog(
            fileId = fileId,
            onDismiss = { showVersionDialog = false },
            onSuccess = {
                showVersionDialog = false
                // Rafraîchir les versions
            }
        )
    }
}

/**
 * Carte d'en-tête du fichier
 */
@Composable
private fun FileHeaderCard(
    fileDetails: FileDetails,
    userPermissions: FilePermissions?
) {
    ArkaCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône du fichier
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ArkaColorUtils.getFileTypeColor(fileDetails.type).copy(alpha = 0.1f),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = getFileIcon(fileDetails.type),
                    contentDescription = null,
                    tint = ArkaColorUtils.getFileTypeColor(fileDetails.type),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            // Informations principales
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = fileDetails.name,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.Storage,
                        text = formatFileSize(fileDetails.size)
                    )

                    InfoChip(
                        icon = Icons.Default.Extension,
                        text = fileDetails.type.uppercase()
                    )

                    fileDetails.version?.let { version ->
                        InfoChip(
                            icon = Icons.Default.Tag,
                            text = "v$version"
                        )
                    }
                }

                Text(
                    text = "Modifié le ${fileDetails.lastModified.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))}",
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = "Créé par ${fileDetails.createdBy}",
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            // Badge de statut
            Surface(
                color = when {
                    userPermissions?.canEdit == true -> MaterialTheme.colors.primary
                    userPermissions?.canDownload == true -> MaterialTheme.colors.secondary
                    else -> MaterialTheme.colors.surface
                }.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = when {
                        userPermissions?.canEdit == true -> "Éditable"
                        userPermissions?.canDownload == true -> "Lecture"
                        else -> "Aucun accès"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = ArkaTextStyles.badge,
                    color = when {
                        userPermissions?.canEdit == true -> MaterialTheme.colors.primary
                        userPermissions?.canDownload == true -> MaterialTheme.colors.secondary
                        else -> MaterialTheme.colors.onSurface
                    }
                )
            }
        }
    }
}

/**
 * Onglet de prévisualisation du fichier
 */
@Composable
private fun FilePreviewTab(fileDetails: FileDetails) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ArkaCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Aperçu du fichier",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Zone de prévisualisation selon le type de fichier
                    FilePreviewContent(fileDetails)
                }
            }
        }

        // Actions rapides
        item {
            QuickActionsCard(fileDetails)
        }
    }
}

/**
 * Contenu de prévisualisation selon le type de fichier
 */
@Composable
private fun FilePreviewContent(fileDetails: FileDetails) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        color = MaterialTheme.colors.background,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (fileDetails.type.lowercase()) {
                "pdf" -> {
                    // TODO: Intégrer un lecteur PDF
                    PreviewPlaceholder(
                        icon = Icons.Default.PictureAsPdf,
                        message = "Prévisualisation PDF",
                        action = "Cliquez pour ouvrir"
                    )
                }

                in listOf("jpg", "jpeg", "png", "gif", "bmp") -> {
                    // TODO: Afficher l'image
                    PreviewPlaceholder(
                        icon = Icons.Default.Image,
                        message = "Aperçu de l'image",
                        action = "Cliquez pour agrandir"
                    )
                }

                in listOf("mp4", "avi", "mov", "wmv") -> {
                    // TODO: Lecteur vidéo
                    PreviewPlaceholder(
                        icon = Icons.Default.VideoFile,
                        message = "Lecteur vidéo",
                        action = "Cliquez pour lire"
                    )
                }

                in listOf("mp3", "wav") -> {
                    // TODO: Lecteur audio
                    PreviewPlaceholder(
                        icon = Icons.Default.AudioFile,
                        message = "Lecteur audio",
                        action = "Cliquez pour écouter"
                    )
                }

                "txt" -> {
                    // TODO: Afficher le contenu texte
                    PreviewPlaceholder(
                        icon = Icons.Default.TextSnippet,
                        message = "Contenu du fichier texte",
                        action = "Cliquez pour voir le contenu"
                    )
                }

                else -> {
                    PreviewPlaceholder(
                        icon = getFileIcon(fileDetails.type),
                        message = "Aperçu non disponible",
                        action = "Téléchargez pour ouvrir"
                    )
                }
            }
        }
    }
}

/**
 * Placeholder de prévisualisation
 */
@Composable
private fun PreviewPlaceholder(
    icon: ImageVector,
    message: String,
    action: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
        )

        Text(
            text = action,
            style = ArkaTextStyles.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )

        Button(
            onClick = { /* TODO: Action de prévisualisation */ }
        ) {
            Text("Ouvrir l'aperçu")
        }
    }
}

/**
 * Carte d'actions rapides
 */
@Composable
private fun QuickActionsCard(fileDetails: FileDetails) {
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
                ArkaOutlinedButton(
                    text = "Télécharger",
                    icon = Icons.Default.Download,
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )

                ArkaOutlinedButton(
                    text = "Partager",
                    icon = Icons.Default.Share,
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )

                ArkaOutlinedButton(
                    text = "Copier lien",
                    icon = Icons.Default.Link,
                    onClick = { /* TODO */ },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Onglet des métadonnées
 */
@Composable
private fun FileMetadataTab(fileDetails: FileDetails) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MetadataSection(
                title = "Informations générales",
                items = listOf(
                    "Nom" to fileDetails.name,
                    "Type" to fileDetails.type.uppercase(),
                    "Taille" to formatFileSize(fileDetails.size),
                    "Version" to (fileDetails.version?.toString() ?: "1.0"),
                    "Checksum" to (fileDetails.checksum ?: "Non disponible")
                )
            )
        }

        item {
            MetadataSection(
                title = "Dates importantes",
                items = listOf(
                    "Créé le" to fileDetails.created.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                    "Modifié le" to fileDetails.lastModified.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                    "Dernier accès" to (fileDetails.lastAccessed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) ?: "Jamais")
                )
            )
        }

        item {
            MetadataSection(
                title = "Propriétaires et créateurs",
                items = listOf(
                    "Créé par" to fileDetails.createdBy,
                    "Propriétaire" to fileDetails.owner,
                    "Dernière modification par" to (fileDetails.lastModifiedBy ?: "Inconnu")
                )
            )
        }

        item {
            MetadataSection(
                title = "Localisation",
                items = listOf(
                    "Espace" to fileDetails.spaceName,
                    "Catégorie" to fileDetails.categoryName,
                    "Dossier" to (fileDetails.folderName ?: "Racine"),
                    "Chemin complet" to fileDetails.fullPath
                )
            )
        }

        // Tags et classification
        if (fileDetails.tags.isNotEmpty()) {
            item {
                TagsSection(tags = fileDetails.tags)
            }
        }
    }
}

/**
 * Section de métadonnées
 */
@Composable
private fun MetadataSection(
    title: String,
    items: List<Pair<String, String>>
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$label:",
                        style = ArkaTextStyles.label,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = value,
                        style = ArkaTextStyles.value,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.weight(2f)
                    )
                }
            }
        }
    }
}

/**
 * Section des tags
 */
@Composable
private fun TagsSection(tags: List<String>) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tags et classification",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tag ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = ArkaTextStyles.chip,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Onglet des versions
 */
@Composable
private fun FileVersionsTab(
    versions: List<FileVersion>,
    userPermissions: FilePermissions?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historique des versions (${versions.size})",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )

                if (userPermissions?.canEdit == true) {
                    Button(
                        onClick = { /* TODO: Nouvelle version */ }
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nouvelle version")
                    }
                }
            }
        }

        if (versions.isEmpty()) {
            item {
                ArkaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )

                        Text(
                            text = "Aucune version antérieure",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )

                        Text(
                            text = "L'historique des versions apparaîtra ici",
                            style = ArkaTextStyles.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            items(versions) { version ->
                VersionItem(
                    version = version,
                    userPermissions = userPermissions
                )
            }
        }
    }
}

/**
 * Item de version
 */
@Composable
private fun VersionItem(
    version: FileVersion,
    userPermissions: FilePermissions?
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur de version
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (version.isCurrent) {
                    MaterialTheme.colors.primary.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colors.surface
                }
            ) {
                Text(
                    text = "v${version.number}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = ArkaTextStyles.badge,
                    color = if (version.isCurrent) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface
                    }
                )
            }

            // Informations de la version
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(version.size),
                        style = ArkaTextStyles.fileName,
                        color = MaterialTheme.colors.onSurface
                    )

                    if (version.isCurrent) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colors.primary
                        ) {
                            Text(
                                text = "ACTUELLE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = ArkaTextStyles.chip.copy(fontSize = 10.sp),
                                color = MaterialTheme.colors.onPrimary
                            )
                        }
                    }
                }

                Text(
                    text = "Créée le ${version.created.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))}",
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = "Par ${version.createdBy}",
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )

                version.comment?.let { comment ->
                    Text(
                        text = comment,
                        style = ArkaTextStyles.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Actions
            if (userPermissions?.canDownload == true) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArkaIconButton(
                        icon = Icons.Default.Download,
                        onClick = { /* TODO: Télécharger cette version */ },
                        tooltip = "Télécharger"
                    )

                    if (!version.isCurrent && userPermissions?.canEdit == true) {
                        ArkaIconButton(
                            icon = Icons.Default.Restore,
                            onClick = { /* TODO: Restaurer cette version */ },
                            tooltip = "Restaurer"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Onglet des commentaires
 */
@Composable
private fun FileCommentsTab(
    comments: List<FileComment>,
    userPermissions: FilePermissions?,
    fileId: Int
) {
    var newComment by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Zone de nouveau commentaire
        if (userPermissions?.canComment == true) {
            item {
                ArkaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Ajouter un commentaire",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedTextField(
                            value = newComment,
                            onValueChange = { newComment = it },
                            placeholder = { Text("Écrivez votre commentaire...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    // TODO: Ajouter le commentaire
                                    newComment = ""
                                },
                                enabled = newComment.isNotBlank()
                            ) {
                                Text("Publier")
                            }
                        }
                    }
                }
            }
        }

        // Liste des commentaires
        if (comments.isEmpty()) {
            item {
                ArkaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                        )

                        Text(
                            text = "Aucun commentaire",
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )

                        Text(
                            text = "Soyez le premier à commenter ce fichier",
                            style = ArkaTextStyles.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    userPermissions = userPermissions
                )
            }
        }
    }
}

/**
 * Item de commentaire
 */
@Composable
private fun CommentItem(
    comment: FileComment,
    userPermissions: FilePermissions?
) {
    ArkaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.primary
                    )

                    Text(
                        text = comment.author,
                        style = ArkaTextStyles.commentAuthor,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface
                    )
                }

                Text(
                    text = comment.created.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            Text(
                text = comment.content,
                style = ArkaTextStyles.commentContent,
                color = MaterialTheme.colors.onSurface
            )

            // Actions du commentaire
            if (userPermissions?.canComment == true) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        onClick = { /* TODO: Répondre */ }
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Répondre")
                    }

                    if (comment.canEdit) {
                        TextButton(
                            onClick = { /* TODO: Éditer */ }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Éditer")
                        }
                    }

                    if (comment.canDelete) {
                        TextButton(
                            onClick = { /* TODO: Supprimer */ }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Supprimer")
                        }
                    }
                }
            }
        }
    }
}

// ================================================================
// DIALOGUES
// ================================================================

/**
 * Dialogue de partage de fichier
 */
@Composable
private fun ShareFileDialog(
    fileDetails: FileDetails,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    // TODO: Implémenter l'interface de partage
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Partager le fichier") },
        text = { Text("Fonctionnalité de partage en cours de développement") },
        confirmButton = {
            Button(onClick = onSuccess) { Text("OK") }
        }
    )
}

/**
 * Dialogue de renommage
 */
@Composable
private fun RenameFileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer le fichier") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nouveau nom") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onSuccess(newName) },
                enabled = newName.isNotBlank() && newName != currentName
            ) {
                Text("Renommer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/**
 * Dialogue de suppression
 */
@Composable
private fun DeleteFileDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer le fichier") },
        text = { Text("Êtes-vous sûr de vouloir supprimer \"$fileName\" ? Cette action est irréversible.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Text("Supprimer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/**
 * Dialogue d'upload de nouvelle version
 */
@Composable
private fun UploadNewVersionDialog(
    fileId: Int,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    // TODO: Implémenter l'interface d'upload
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle version") },
        text = { Text("Fonctionnalité d'upload en cours de développement") },
        confirmButton = {
            Button(onClick = onSuccess) { Text("OK") }
        }
    )
}

// ================================================================
// COMPOSANTS UTILITAIRES
// ================================================================

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                style = ArkaTextStyles.chip,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

// ================================================================
// DATA CLASSES ET ENUMS
// ================================================================

data class FileDetails(
    val id: Int,
    val name: String,
    val type: String,
    val size: Long,
    val version: Int? = null,
    val checksum: String? = null,
    val created: LocalDateTime,
    val lastModified: LocalDateTime,
    val lastAccessed: LocalDateTime? = null,
    val createdBy: String,
    val owner: String,
    val lastModifiedBy: String? = null,
    val spaceName: String,
    val categoryName: String,
    val folderName: String? = null,
    val fullPath: String,
    val tags: List<String> = emptyList()
)

data class FileVersion(
    val number: Int,
    val size: Long,
    val created: LocalDateTime,
    val createdBy: String,
    val comment: String? = null,
    val isCurrent: Boolean = false
)

data class FileComment(
    val id: Int,
    val content: String,
    val author: String,
    val created: LocalDateTime,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false
)

data class FilePermissions(
    val canDownload: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean,
    val canShare: Boolean,
    val canComment: Boolean
)

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

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