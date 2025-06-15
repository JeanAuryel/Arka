package ui.screens.files

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import ui.screens.spaces.ViewMode

// ================================================================
// FILESSCREEN.KT - ÉCRAN DE GESTION DES FICHIERS ARKA (DESKTOP)
// ================================================================

package ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import controllers.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran principal de gestion des fichiers pour application desktop
 *
 * Fonctionnalités desktop:
 * - Layout en panneaux avec barre latérale pour navigation
 * - Support drag & drop pour upload
 * - Menus contextuels (clic droit)
 * - Raccourcis clavier (Ctrl+A, Suppr, F2, etc.)
 * - Multi-sélection avec Ctrl/Shift
 * - Vue détaillée avec colonnes redimensionnables
 * - Prévisualisation de fichiers
 * - Barre d'outils complète
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FilesScreen(
    fileController: FileController,
    folderController: FolderController,
    categoryController: CategoryController,
    onNavigateBack: () -> Unit,
    initialFolderId: Int? = null,
    initialCategoryId: Int? = null
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // États principaux
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var folders by remember { mutableStateOf<List<FolderItem>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryItem>>(emptyList()) }
    var currentFolder by remember { mutableStateOf<FolderItem?>(null) }
    var currentCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // États UI
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    var filterType by remember { mutableStateOf(FileFilter.ALL) }
    var selectedItems by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showFileDetails by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }

    // Dialogues et menus
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var contextMenuItem by remember { mutableStateOf<FileItem?>(null) }

    // Navigation et breadcrumb
    var navigationStack by remember { mutableStateOf<List<NavigationItem>>(emptyList()) }

    // Chargement des données
    fun loadData() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                // Charger les catégories pour le panneau latéral
                val categoriesResult = categoryController.getAllCategories()
                if (categoriesResult is CategoryController.CategoryResult.Success) {
                    categories = categoriesResult.data.map { cat ->
                        CategoryItem(
                            id = cat.id,
                            name = cat.nom,
                            description = cat.description,
                            color = cat.couleur,
                            fileCount = 0 // À calculer
                        )
                    }
                }

                // Charger le dossier/catégorie actuel
                when {
                    initialFolderId != null -> {
                        val folderResult = folderController.getFolderById(initialFolderId)
                        if (folderResult is FolderController.FolderResult.Success) {
                            currentFolder = FolderItem.fromEntity(folderResult.data)
                        }
                    }
                    initialCategoryId != null -> {
                        val categoryResult = categoryController.getCategoryById(initialCategoryId)
                        if (categoryResult is CategoryController.CategoryResult.Success) {
                            currentCategory = CategoryItem.fromEntity(categoryResult.data)
                        }
                    }
                }

                // Charger fichiers et dossiers
                loadFilesAndFolders()

            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadFilesAndFolders() {
        scope.launch {
            try {
                // Charger les fichiers selon le contexte
                val filesResult = when {
                    currentFolder != null -> fileController.getFilesByFolder(currentFolder!!.id)
                    currentCategory != null -> fileController.getFilesByCategory(currentCategory!!.id)
                    else -> fileController.getAllFiles()
                }

                if (filesResult is FileController.FileResult.Success) {
                    files = filesResult.data.map { FileItem.fromEntity(it) }
                }

                // Charger les dossiers selon le contexte
                val foldersResult = when {
                    currentFolder != null -> folderController.getSubFolders(currentFolder!!.id)
                    currentCategory != null -> folderController.getFoldersByCategory(currentCategory!!.id)
                    else -> folderController.getRootFolders()
                }

                if (foldersResult is FolderController.FolderResult.Success) {
                    folders = foldersResult.data.map { FolderItem.fromEntity(it) }
                }

            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement des fichiers: ${e.message}"
            }
        }
    }

    // Gestion des raccourcis clavier
    val onKeyEvent: (KeyEvent) -> Boolean = { keyEvent ->
        if (keyEvent.type == KeyEventType.KeyDown) {
            when {
                // Ctrl+A - Sélectionner tout
                keyEvent.isCtrlPressed && keyEvent.key == Key.A -> {
                    selectedItems = files.map { it.id }.toSet()
                    true
                }
                // Suppr - Supprimer les éléments sélectionnés
                keyEvent.key == Key.Delete && selectedItems.isNotEmpty() -> {
                    showDeleteConfirmation = true
                    true
                }
                // F2 - Renommer
                keyEvent.key == Key.F2 && selectedItems.size == 1 -> {
                    showRenameDialog = true
                    true
                }
                // Ctrl+F - Rechercher
                keyEvent.isCtrlPressed && keyEvent.key == Key.F -> {
                    focusManager.clearFocus()
                    true
                }
                // Échap - Annuler sélection
                keyEvent.key == Key.Escape -> {
                    selectedItems = emptySet()
                    contextMenuPosition = null
                    true
                }
                else -> false
            }
        } else false
    }

    // Chargement initial
    LaunchedEffect(initialFolderId, initialCategoryId) {
        loadData()
    }

    // Layout principal desktop avec panneaux
    Row(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent(onKeyEvent)
    ) {
        // Panneau latéral de navigation (250dp fixe)
        SideNavigationPanel(
            categories = categories,
            currentCategory = currentCategory,
            onCategoryClick = { category ->
                currentCategory = category
                currentFolder = null
                selectedItems = emptySet()
                loadFilesAndFolders()
            },
            onRootClick = {
                currentCategory = null
                currentFolder = null
                selectedItems = emptySet()
                loadFilesAndFolders()
            },
            modifier = Modifier.width(250.dp)
        )

        // Séparateur vertical
        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        )

        // Panneau principal
        Column(modifier = Modifier.weight(1f)) {
            // Barre d'outils
            FilesToolbar(
                currentFolder = currentFolder,
                currentCategory = currentCategory,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                sortOption = sortOption,
                onSortChange = { sortOption = it },
                filterType = filterType,
                onFilterChange = { filterType = it },
                selectedCount = selectedItems.size,
                onCreateFolder = { showCreateFolderDialog = true },
                onUploadFiles = { showUploadDialog = true },
                onRefresh = { loadFilesAndFolders() },
                onToggleDetails = { showFileDetails = !showFileDetails }
            )

            // Breadcrumb de navigation
            if (currentFolder != null || currentCategory != null) {
                FilesBreadcrumb(
                    currentFolder = currentFolder,
                    currentCategory = currentCategory,
                    onNavigateToParent = { /* TODO: Navigation parent */ },
                    onNavigateToRoot = {
                        currentFolder = null
                        currentCategory = null
                        loadFilesAndFolders()
                    }
                )
            }

            Row(modifier = Modifier.weight(1f)) {
                // Zone principale des fichiers
                Box(
                    modifier = Modifier
                        .weight(if (showFileDetails) 0.7f else 1f)
                        .fillMaxHeight()
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ArkaLoadingIndicator(message = "Chargement des fichiers...")
                            }
                        }

                        errorMessage != null -> {
                            ErrorScreen(
                                message = errorMessage!!,
                                onRetry = { loadData() }
                            )
                        }

                        else -> {
                            FilesContent(
                                files = files.filter { file ->
                                    if (searchQuery.isBlank()) true
                                    else file.name.contains(searchQuery, ignoreCase = true)
                                }.let { filteredFiles ->
                                    when (filterType) {
                                        FileFilter.IMAGES -> filteredFiles.filter { it.isImage() }
                                        FileFilter.DOCUMENTS -> filteredFiles.filter { it.isDocument() }
                                        FileFilter.VIDEOS -> filteredFiles.filter { it.isVideo() }
                                        FileFilter.AUDIO -> filteredFiles.filter { it.isAudio() }
                                        FileFilter.ALL -> filteredFiles
                                    }
                                }.sortedWith { a, b ->
                                    when (sortOption) {
                                        SortOption.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                                        SortOption.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                                        SortOption.DATE_ASC -> a.modifiedDate.compareTo(b.modifiedDate)
                                        SortOption.DATE_DESC -> b.modifiedDate.compareTo(a.modifiedDate)
                                        SortOption.SIZE_ASC -> a.size.compareTo(b.size)
                                        SortOption.SIZE_DESC -> b.size.compareTo(a.size)
                                        SortOption.TYPE_ASC -> a.extension.compareTo(b.extension)
                                        SortOption.TYPE_DESC -> b.extension.compareTo(a.extension)
                                    }
                                },
                                folders = folders,
                                viewMode = viewMode,
                                selectedItems = selectedItems,
                                onItemClick = { item, isCtrlPressed, isShiftPressed ->
                                    handleItemSelection(item, isCtrlPressed, isShiftPressed, selectedItems) { newSelection ->
                                        selectedItems = newSelection
                                    }
                                },
                                onItemDoubleClick = { item ->
                                    when (item) {
                                        is FileItem -> {
                                            selectedFile = item
                                            // Ouvrir le fichier ou afficher les détails
                                        }
                                        is FolderItem -> {
                                            currentFolder = item
                                            currentCategory = null
                                            selectedItems = emptySet()
                                            loadFilesAndFolders()
                                        }
                                    }
                                },
                                onContextMenu = { item, position ->
                                    contextMenuItem = item as? FileItem
                                    contextMenuPosition = position
                                }
                            )
                        }
                    }

                    // Menu contextuel
                    contextMenuPosition?.let { (x, y) ->
                        ContextMenu(
                            x = x,
                            y = y,
                            onDismiss = { contextMenuPosition = null },
                            item = contextMenuItem,
                            onOpen = { /* TODO */ },
                            onRename = { showRenameDialog = true },
                            onDelete = { showDeleteConfirmation = true },
                            onShare = { /* TODO */ },
                            onDownload = { /* TODO */ }
                        )
                    }
                }

                // Panneau des détails (si activé)
                if (showFileDetails && selectedFile != null) {
                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                    )

                    FileDetailsPanel(
                        file = selectedFile!!,
                        onClose = { showFileDetails = false },
                        modifier = Modifier.width(300.dp)
                    )
                }
            }
        }
    }

    // Dialogues
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { folderName ->
                scope.launch {
                    try {
                        // TODO: Créer le dossier
                        loadFilesAndFolders()
                    } catch (e: Exception) {
                        errorMessage = "Erreur création dossier: ${e.message}"
                    }
                }
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }

    if (showUploadDialog) {
        UploadFilesDialog(
            onFilesSelected = { files ->
                scope.launch {
                    try {
                        // TODO: Upload des fichiers
                        loadFilesAndFolders()
                    } catch (e: Exception) {
                        errorMessage = "Erreur upload: ${e.message}"
                    }
                }
                showUploadDialog = false
            },
            onDismiss = { showUploadDialog = false }
        )
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            itemCount = selectedItems.size,
            onConfirm = {
                scope.launch {
                    try {
                        // TODO: Supprimer les fichiers sélectionnés
                        selectedItems = emptySet()
                        loadFilesAndFolders()
                    } catch (e: Exception) {
                        errorMessage = "Erreur suppression: ${e.message}"
                    }
                }
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    if (showRenameDialog && contextMenuItem != null) {
        RenameDialog(
            currentName = contextMenuItem!!.name,
            onConfirm = { newName ->
                scope.launch {
                    try {
                        // TODO: Renommer le fichier
                        loadFilesAndFolders()
                    } catch (e: Exception) {
                        errorMessage = "Erreur renommage: ${e.message}"
                    }
                }
                showRenameDialog = false
                contextMenuItem = null
            },
            onDismiss = {
                showRenameDialog = false
                contextMenuItem = null
            }
        )
    }
}

/**
 * Panneau latéral de navigation avec catégories
 */
@Composable
private fun SideNavigationPanel(
    categories: List<CategoryItem>,
    currentCategory: CategoryItem?,
    onCategoryClick: (CategoryItem?) -> Unit,
    onRootClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colors.surface,
        elevation = 1.dp
    ) {
        LazyColumn(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Accueil / Tous les fichiers
            item {
                NavigationItem(
                    icon = Icons.Default.Home,
                    text = "Tous les fichiers",
                    isSelected = currentCategory == null,
                    onClick = onRootClick
                )
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // En-tête Catégories
            item {
                Text(
                    text = "CATÉGORIES",
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Liste des catégories
            items(categories) { category ->
                NavigationItem(
                    icon = Icons.Default.Category,
                    text = category.name,
                    isSelected = currentCategory?.id == category.id,
                    onClick = { onCategoryClick(category) },
                    badge = if (category.fileCount > 0) category.fileCount.toString() else null,
                    color = category.color?.let { Color(it.toLong(16)) }
                )
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Actions rapides
            item {
                Text(
                    text = "ACTIONS",
                    style = ArkaTextStyles.metadata,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                NavigationItem(
                    icon = Icons.Default.CloudUpload,
                    text = "Téléverser",
                    isSelected = false,
                    onClick = { /* TODO */ }
                )
            }

            item {
                NavigationItem(
                    icon = Icons.Default.CreateNewFolder,
                    text = "Nouveau dossier",
                    isSelected = false,
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

/**
 * Item de navigation dans le panneau latéral
 */
@Composable
private fun NavigationItem(
    icon: ImageVector,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badge: String? = null,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.12f) else Color.Transparent,
        shape = ArkaComponentShapes.cardSmall
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color ?: if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = text,
                style = ArkaTextStyles.navigation,
                color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            badge?.let {
                StatusBadge(
                    text = it,
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                    textColor = MaterialTheme.colors.primary
                )
            }
        }
    }
}

/**
 * Barre d'outils principale
 */
@Composable
private fun FilesToolbar(
    currentFolder: FolderItem?,
    currentCategory: CategoryItem?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    filterType: FileFilter,
    onFilterChange: (FileFilter) -> Unit,
    selectedCount: Int,
    onCreateFolder: () -> Unit,
    onUploadFiles: () -> Unit,
    onRefresh: () -> Unit,
    onToggleDetails: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Titre de la section
            Text(
                text = when {
                    currentFolder != null -> currentFolder.name
                    currentCategory != null -> currentCategory.name
                    else -> "Tous les fichiers"
                },
                style = ArkaTextStyles.cardTitle,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Barre de recherche
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { /* Focus sur le premier élément */ }
                ),
                modifier = Modifier.width(250.dp)
            )

            // Séparateur
            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
            )

            // Filtres
            var showFilters by remember { mutableStateOf(false) }
            IconButton(onClick = { showFilters = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtres",
                    tint = if (filterType != FileFilter.ALL) MaterialTheme.colors.primary
                    else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            DropdownMenu(
                expanded = showFilters,
                onDismissRequest = { showFilters = false }
            ) {
                FileFilter.values().forEach { filter ->
                    DropdownMenuItem(
                        onClick = {
                            onFilterChange(filter)
                            showFilters = false
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(filter.displayName)
                            if (filterType == filter) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colors.primary
                                )
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
                SortOption.values().forEach { sort ->
                    DropdownMenuItem(
                        onClick = {
                            onSortChange(sort)
                            showSort = false
                        }
                    ) {
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
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                        }
                    }
                }
            }

            // Mode d'affichage
            Row {
                IconButton(
                    onClick = { onViewModeChange(ViewMode.LIST) }
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Vue liste",
                        tint = if (viewMode == ViewMode.LIST) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { onViewModeChange(ViewMode.GRID) }
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Vue grille",
                        tint = if (viewMode == ViewMode.GRID) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Divider(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
            )

            // Actions
            if (selectedCount > 0) {
                Text(
                    text = "$selectedCount sélectionné${if (selectedCount > 1) "s" else ""}",
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.primary
                )
            }

            IconButton(onClick = onCreateFolder) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Nouveau dossier")
            }

            IconButton(onClick = onUploadFiles) {
                Icon(Icons.Default.CloudUpload, contentDescription = "Téléverser")
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
            }

            IconButton(onClick = onToggleDetails) {
                Icon(Icons.Default.Info, contentDescription = "Détails")
            }
        }
    }
}

// ================================================================
// COMPOSANTS SPÉCIALISÉS ET MODÈLES DE DONNÉES
// ================================================================

/**
 * Contenu principal des fichiers (liste ou grille)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FilesContent(
    files: List<FileItem>,
    folders: List<FolderItem>,
    viewMode: ViewMode,
    selectedItems: Set<Int>,
    onItemClick: (Any, Boolean, Boolean) -> Unit,
    onItemDoubleClick: (Any) -> Unit,
    onContextMenu: (Any, Pair<Float, Float>) -> Unit
) {
    val allItems = folders + files

    if (allItems.isEmpty()) {
        // Vue vide
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                )

                Text(
                    text = "Aucun fichier dans ce dossier",
                    style = ArkaTextStyles.cardTitle,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = "Glissez-déposez des fichiers ici ou utilisez le bouton de téléversement",
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        when (viewMode) {
            ViewMode.LIST -> {
                FilesListView(
                    items = allItems,
                    selectedItems = selectedItems,
                    onItemClick = onItemClick,
                    onItemDoubleClick = onItemDoubleClick,
                    onContextMenu = onContextMenu
                )
            }

            ViewMode.GRID -> {
                FilesGridView(
                    items = allItems,
                    selectedItems = selectedItems,
                    onItemClick = onItemClick,
                    onItemDoubleClick = onItemDoubleClick,
                    onContextMenu = onContextMenu
                )
            }
        }
    }
}

/**
 * Vue en liste détaillée (style Windows Explorer)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FilesListView(
    items: List<Any>,
    selectedItems: Set<Int>,
    onItemClick: (Any, Boolean, Boolean) -> Unit,
    onItemDoubleClick: (Any) -> Unit,
    onContextMenu: (Any, Pair<Float, Float>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // En-tête des colonnes
        item {
            FileListHeader()
        }

        // Items
        items(items) { item ->
            val isSelected = when (item) {
                is FileItem -> selectedItems.contains(item.id)
                is FolderItem -> selectedItems.contains(item.id)
                else -> false
            }

            FileListItem(
                item = item,
                isSelected = isSelected,
                onItemClick = onItemClick,
                onItemDoubleClick = onItemDoubleClick,
                onContextMenu = onContextMenu
            )
        }
    }
}

/**
 * En-tête des colonnes pour la vue liste
 */
@Composable
private fun FileListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nom",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.4f)
            )

            Text(
                text = "Taille",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.15f)
            )

            Text(
                text = "Type",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.15f)
            )

            Text(
                text = "Modifié",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.3f)
            )
        }
    }
}

/**
 * Item de fichier dans la vue liste
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileListItem(
    item: Any,
    isSelected: Boolean,
    onItemClick: (Any, Boolean, Boolean) -> Unit,
    onItemDoubleClick: (Any) -> Unit,
    onContextMenu: (Any, Pair<Float, Float>) -> Unit
) {
    var clickCount by remember { mutableStateOf(0) }

    LaunchedEffect(clickCount) {
        if (clickCount == 1) {
            delay(300) // Délai pour détecter double-clic
            if (clickCount == 1) {
                clickCount = 0
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .onPointerEvent(PointerEventType.Press) { event ->
                when (event.button) {
                    PointerButton.Primary -> {
                        val isCtrl = event.keyboardModifiers.isCtrlPressed
                        val isShift = event.keyboardModifiers.isShiftPressed

                        clickCount++
                        if (clickCount == 2) {
                            onItemDoubleClick(item)
                            clickCount = 0
                        } else {
                            onItemClick(item, isCtrl, isShift)
                        }
                    }

                    PointerButton.Secondary -> {
                        val position = event.changes.first().position
                        onContextMenu(item, position.x to position.y)
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icône et nom
            Row(
                modifier = Modifier.weight(0.4f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (item) {
                        is FolderItem -> Icons.Default.Folder
                        is FileItem -> getFileTypeIcon(item.extension)
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when (item) {
                        is FolderItem -> Color(0xFFFFB74D)
                        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    }
                )

                Text(
                    text = when (item) {
                        is FileItem -> item.name
                        is FolderItem -> item.name
                        else -> "Unknown"
                    },
                    style = ArkaTextStyles.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Taille
            Text(
                text = when (item) {
                    is FileItem -> formatFileSize(item.size)
                    is FolderItem -> "${item.itemCount} éléments"
                    else -> "-"
                },
                style = ArkaTextStyles.fileSize,
                modifier = Modifier.weight(0.15f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Type
            Text(
                text = when (item) {
                    is FileItem -> item.extension.uppercase()
                    is FolderItem -> "Dossier"
                    else -> "-"
                },
                style = ArkaTextStyles.metadata,
                modifier = Modifier.weight(0.15f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Date de modification
            Text(
                text = when (item) {
                    is FileItem -> formatDate(item.modifiedDate)
                    is FolderItem -> formatDate(item.modifiedDate)
                    else -> "-"
                },
                style = ArkaTextStyles.date,
                modifier = Modifier.weight(0.3f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Vue en grille (style icônes)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FilesGridView(
    items: List<Any>,
    selectedItems: Set<Int>,
    onItemClick: (Any, Boolean, Boolean) -> Unit,
    onItemDoubleClick: (Any) -> Unit,
    onContextMenu: (Any, Pair<Float, Float>) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(120.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            val isSelected = when (item) {
                is FileItem -> selectedItems.contains(item.id)
                is FolderItem -> selectedItems.contains(item.id)
                else -> false
            }

            FileGridItem(
                item = item,
                isSelected = isSelected,
                onItemClick = onItemClick,
                onItemDoubleClick = onItemDoubleClick,
                onContextMenu = onContextMenu
            )
        }
    }
}

/**
 * Item de fichier dans la vue grille
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileGridItem(
    item: Any,
    isSelected: Boolean,
    onItemClick: (Any, Boolean, Boolean) -> Unit,
    onItemDoubleClick: (Any) -> Unit,
    onContextMenu: (Any, Pair<Float, Float>) -> Unit
) {
    var clickCount by remember { mutableStateOf(0) }

    LaunchedEffect(clickCount) {
        if (clickCount == 1) {
            delay(300)
            if (clickCount == 1) {
                clickCount = 0
            }
        }
    }

    Surface(
        modifier = Modifier
            .size(120.dp)
            .background(
                if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else Color.Transparent,
                shape = ArkaComponentShapes.cardSmall
            )
            .onPointerEvent(PointerEventType.Press) { event ->
                when (event.button) {
                    PointerButton.Primary -> {
                        val isCtrl = event.keyboardModifiers.isCtrlPressed
                        val isShift = event.keyboardModifiers.isShiftPressed

                        clickCount++
                        if (clickCount == 2) {
                            onItemDoubleClick(item)
                            clickCount = 0
                        } else {
                            onItemClick(item, isCtrl, isShift)
                        }
                    }

                    PointerButton.Secondary -> {
                        val position = event.changes.first().position
                        onContextMenu(item, position.x to position.y)
                    }
                }
            },
        shape = ArkaComponentShapes.cardSmall
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icône large
            Icon(
                imageVector = when (item) {
                    is FolderItem -> Icons.Default.Folder
                    is FileItem -> getFileTypeIcon(item.extension)
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = when (item) {
                    is FolderItem -> Color(0xFFFFB74D)
                    else -> MaterialTheme.colors.primary.copy(alpha = 0.7f)
                }
            )

            // Nom du fichier/dossier
            Text(
                text = when (item) {
                    is FileItem -> item.name
                    is FolderItem -> item.name
                    else -> "Unknown"
                },
                style = ArkaTextStyles.fileName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.onSurface
            )

            // Informations supplémentaires
            Text(
                text = when (item) {
                    is FileItem -> formatFileSize(item.size)
                    is FolderItem -> "${item.itemCount} éléments"
                    else -> ""
                },
                style = ArkaTextStyles.metadata,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ================================================================
// COMPOSANTS DE DIALOGUE
// ================================================================

@Composable
private fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un nouveau dossier") },
        text = {
            Column {
                Text("Nom du dossier :")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Nouveau dossier") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Créer",
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank()
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
private fun UploadFilesDialog(
    onFilesSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Implémentation simplifiée - remplacer par un vrai sélecteur de fichiers
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Téléverser des fichiers") },
        text = {
            Column {
                Text("Glissez-déposez vos fichiers dans cette zone ou cliquez pour sélectionner.")
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .border(
                            2.dp,
                            MaterialTheme.colors.primary.copy(alpha = 0.3f),
                            ArkaComponentShapes.cardMedium
                        ),
                    color = MaterialTheme.colors.primary.copy(alpha = 0.05f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colors.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Zone de glisser-déposer",
                                style = ArkaTextStyles.helpText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Sélectionner fichiers",
                onClick = {
                    // TODO: Ouvrir sélecteur de fichiers système
                    onFilesSelected(emptyList())
                }
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
private fun DeleteConfirmationDialog(
    itemCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmer la suppression") },
        text = {
            Text(
                if (itemCount == 1) {
                    "Êtes-vous sûr de vouloir supprimer cet élément ?"
                } else {
                    "Êtes-vous sûr de vouloir supprimer ces $itemCount éléments ?"
                }
            )
        },
        confirmButton = {
            ArkaButton(
                text = "Supprimer",
                onClick = onConfirm,
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

@Composable
private fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer") },
        text = {
            Column {
                Text("Nouveau nom :")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Renommer",
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != currentName
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
 * Menu contextuel (clic droit)
 */
@Composable
private fun ContextMenu(
    x: Float,
    y: Float,
    onDismiss: () -> Unit,
    item: FileItem?,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    // Implémentation simplifiée du menu contextuel
    Box {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            modifier = Modifier.background(MaterialTheme.colors.surface)
        ) {
            DropdownMenuItem(onClick = onOpen) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Ouvrir")
                }
            }

            DropdownMenuItem(onClick = onRename) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Renommer")
                }
            }

            DropdownMenuItem(onClick = onShare) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Partager")
                }
            }

            DropdownMenuItem(onClick = onDownload) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Télécharger")
                }
            }

            Divider()

            DropdownMenuItem(onClick = onDelete) {
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

/**
 * Panneau de détails de fichier
 */
@Composable
private fun FileDetailsPanel(
    file: FileItem,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colors.surface,
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // En-tête
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Détails",
                    style = ArkaTextStyles.cardTitle,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            Divider()

            // Prévisualisation/Icône
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileTypeIcon(file.extension),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colors.primary.copy(alpha = 0.7f)
                )
            }

            // Informations du fichier
            FileDetailRow("Nom", file.name)
            FileDetailRow("Taille", formatFileSize(file.size))
            FileDetailRow("Type", file.extension.uppercase())
            FileDetailRow("Créé", formatDate(file.createdDate))
            FileDetailRow("Modifié", formatDate(file.modifiedDate))

            if (file.uploadedBy.isNotEmpty()) {
                FileDetailRow("Téléversé par", file.uploadedBy)
            }

            Divider()

            // Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ArkaButton(
                    text = "Télécharger",
                    onClick = { /* TODO */ },
                    icon = Icons.Default.Download,
                    modifier = Modifier.fillMaxWidth()
                )

                ArkaOutlinedButton(
                    text = "Partager",
                    onClick = { /* TODO */ },
                    icon = Icons.Default.Share,
                    modifier = Modifier.fillMaxWidth()
                )

                if (file.isShared) {
                    ArkaOutlinedButton(
                        text = "Gérer partage",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.ManageAccounts,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FileDetailRow(
    label: String,
    value: String
) {
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
            style = ArkaTextStyles.cardDescription,
            color = MaterialTheme.colors.onSurface
        )
    }
}

/**
 * Breadcrumb de navigation
 */
@Composable
private fun FilesBreadcrumb(
    currentFolder: FolderItem?,
    currentCategory: CategoryItem?,
    onNavigateToParent: () -> Unit,
    onNavigateToRoot: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colors.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onNavigateToRoot) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Accueil")
                }
            }

            if (currentCategory != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )

                Text(
                    text = currentCategory.name,
                    style = ArkaTextStyles.breadcrumb,
                    color = MaterialTheme.colors.primary
                )
            }

            if (currentFolder != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )

                Text(
                    text = currentFolder.name,
                    style = ArkaTextStyles.breadcrumb,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}

/**
 * Écran d'erreur
 */
@Composable
private fun ErrorScreen(
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
                text = "Une erreur s'est produite",
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
 * Modes d'affichage
 */
enum class ViewMode {
    LIST, GRID
}

/**
 * Options de tri
 */
enum class SortOption(val displayName: String, val icon: ImageVector) {
    NAME_ASC("Nom (A-Z)", Icons.Default.SortByAlpha),
    NAME_DESC("Nom (Z-A)", Icons.Default.SortByAlpha),
    DATE_ASC("Date (ancien)", Icons.Default.DateRange),
    DATE_DESC("Date (récent)", Icons.Default.DateRange),
    SIZE_ASC("Taille (petit)", Icons.Default.DataUsage),
    SIZE_DESC("Taille (grand)", Icons.Default.DataUsage),
    TYPE_ASC("Type (A-Z)", Icons.Default.Category),
    TYPE_DESC("Type (Z-A)", Icons.Default.Category)
}

/**
 * Filtres de fichiers
 */
enum class FileFilter(val displayName: String, val icon: ImageVector) {
    ALL("Tous les fichiers", Icons.Default.SelectAll),
    IMAGES("Images", Icons.Default.Image),
    DOCUMENTS("Documents", Icons.Default.Description),
    VIDEOS("Vidéos", Icons.Default.VideoFile),
    AUDIO("Audio", Icons.Default.AudioFile)
}

/**
 * Item de fichier pour l'affichage
 */
data class FileItem(
    val id: Int,
    val name: String,
    val extension: String,
    val size: Long,
    val folderId: Int?,
    val categoryId: Int?,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val uploadedBy: String,
    val isShared: Boolean
) {
    fun isImage(): Boolean = extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp", "svg")
    fun isDocument(): Boolean = extension.lowercase() in listOf("pdf", "doc", "docx", "txt", "rtf", "odt")
    fun isVideo(): Boolean = extension.lowercase() in listOf("mp4", "avi", "mov", "wmv", "flv", "mkv")
    fun isAudio(): Boolean = extension.lowercase() in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a")

    companion object {
        fun fromEntity(entity: Any): FileItem {
            // TODO: Mapper depuis les entités réelles
            return FileItem(
                id = 1,
                name = "Fichier exemple",
                extension = "pdf",
                size = 1024L,
                folderId = null,
                categoryId = null,
                createdDate = LocalDateTime.now(),
                modifiedDate = LocalDateTime.now(),
                uploadedBy = "Utilisateur",
                isShared = false
            )
        }
    }
}

/**
 * Item de dossier pour l'affichage
 */
data class FolderItem(
    val id: Int,
    val name: String,
    val parentId: Int?,
    val categoryId: Int?,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val itemCount: Int
) {
    companion object {
        fun fromEntity(entity: Any): FolderItem {
            // TODO: Mapper depuis les entités réelles
            return FolderItem(
                id = 1,
                name = "Dossier exemple",
                parentId = null,
                categoryId = null,
                createdDate = LocalDateTime.now(),
                modifiedDate = LocalDateTime.now(),
                itemCount = 0
            )
        }
    }
}

/**
 * Item de catégorie pour l'affichage
 */
data class CategoryItem(
    val id: Int,
    val name: String,
    val description: String,
    val color: String?,
    val fileCount: Int
) {
    companion object {
        fun fromEntity(entity: Any): CategoryItem {
            // TODO: Mapper depuis les entités réelles
            return CategoryItem(
                id = 1,
                name = "Catégorie exemple",
                description = "Description",
                color = null,
                fileCount = 0
            )
        }
    }
}

/**
 * Item de navigation
 */
data class NavigationItem(
    val id: Int,
    val name: String,
    val type: String, // "folder", "category", "root"
    val parentId: Int?
)

/**
 * Gestion de la sélection multiple
 */
private fun handleItemSelection(
    item: Any,
    isCtrlPressed: Boolean,
    isShiftPressed: Boolean,
    currentSelection: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit
) {
    val itemId = when (item) {
        is FileItem -> item.id
        is FolderItem -> item.id
        else -> return
    }

    when {
        isCtrlPressed -> {
            // Ajouter/retirer de la sélection
            if (currentSelection.contains(itemId)) {
                onSelectionChange(currentSelection - itemId)
            } else {
                onSelectionChange(currentSelection + itemId)
            }
        }

        isShiftPressed -> {
            // TODO: Sélection en range (nécessite de garder l'index du dernier item sélectionné)
            onSelectionChange(currentSelection + itemId)
        }

        else -> {
            // Sélection simple
            onSelectionChange(setOf(itemId))
        }
    }
}

/**
 * Obtient l'icône appropriée pour un type de fichier
 */
private fun getFileTypeIcon(extension: String): ImageVector {
    return when (extension.lowercase()) {
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx" -> Icons.Default.TableChart
        "ppt", "pptx" -> Icons.Default.Slideshow
        "txt", "rtf" -> Icons.Default.TextSnippet
        "jpg", "jpeg", "png", "gif", "bmp", "svg" -> Icons.Default.Image
        "mp4", "avi", "mov", "wmv", "flv", "mkv" -> Icons.Default.VideoFile
        "mp3", "wav", "flac", "aac", "ogg", "m4a" -> Icons.Default.AudioFile
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.Archive
        "exe", "msi" -> Icons.Default.Apps
        "html", "htm", "css", "js" -> Icons.Default.Code
        else -> Icons.Default.InsertDriveFile
    }
}

/**
 * Formate la taille de fichier
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * Formate une date
 */
private fun formatDate(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    return dateTime.format(formatter)
}