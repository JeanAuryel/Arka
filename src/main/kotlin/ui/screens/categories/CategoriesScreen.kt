package ui.screens.categories

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import ui.screens.StatsCard
import ui.screens.familymembers.UserRole
import java.time.Duration

// ================================================================
// CATEGORIESSCREEN.KT - GESTION DES CATÉGORIES ARKA (DESKTOP)
// ================================================================

package ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import controllers.*
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Écran de gestion des catégories pour application desktop
 *
 * Fonctionnalités:
 * - Vue grille et liste des catégories
 * - Création et modification de catégories
 * - Gestion des couleurs et icônes
 * - Statistiques par catégorie
 * - Réorganisation par drag & drop
 * - Permissions par catégorie
 * - Archivage de catégories
 */
@Composable
fun CategoriesScreen(
    categoryController: CategoryController,
    fileController: FileController,
    currentUserRole: UserRole,
    onCategoryClick: (Int) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // États principaux
    var categories by remember { mutableStateOf<List<CategoryInfo>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CategoryInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // États UI
    var viewMode by remember { mutableStateOf(CategoryViewMode.GRID) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(CategorySortOption.NAME_ASC) }
    var showCategoryDetails by remember { mutableStateOf(false) }
    var showArchivedCategories by remember { mutableStateOf(false) }

    // Dialogues
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryInfo?>(null) }

    // Statistiques
    var categoryStats by remember { mutableStateOf<CategoryStats?>(null) }

    // Chargement des données
    fun loadCategories() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                // Charger les catégories
                val categoriesResult = categoryController.getAllCategories(includeArchived = showArchivedCategories)
                if (categoriesResult is CategoryController.CategoryResult.Success) {
                    categories = categoriesResult.data.map { CategoryInfo.fromEntity(it) }
                }

                // Charger les statistiques
                val statsResult = categoryController.getCategoryStatistics()
                if (statsResult is CategoryController.CategoryResult.Success) {
                    categoryStats = CategoryStats.fromEntity(statsResult.data)
                }

            } catch (e: Exception) {
                errorMessage = "Erreur lors du chargement: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Filtrage et tri
    val filteredAndSortedCategories = remember(categories, searchQuery, sortOption) {
        categories
            .filter { category ->
                if (searchQuery.isNotBlank()) {
                    category.name.contains(searchQuery, ignoreCase = true) ||
                            category.description.contains(searchQuery, ignoreCase = true)
                } else true
            }
            .sortedWith { a, b ->
                when (sortOption) {
                    CategorySortOption.NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                    CategorySortOption.NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                    CategorySortOption.FILES_ASC -> a.fileCount.compareTo(b.fileCount)
                    CategorySortOption.FILES_DESC -> b.fileCount.compareTo(a.fileCount)
                    CategorySortOption.CREATED_ASC -> a.createdDate.compareTo(b.createdDate)
                    CategorySortOption.CREATED_DESC -> b.createdDate.compareTo(a.createdDate)
                    CategorySortOption.MODIFIED_ASC -> a.modifiedDate.compareTo(b.modifiedDate)
                    CategorySortOption.MODIFIED_DESC -> b.modifiedDate.compareTo(a.modifiedDate)
                }
            }
    }

    // Chargement initial
    LaunchedEffect(showArchivedCategories) {
        loadCategories()
    }

    // Layout principal
    Row(modifier = Modifier.fillMaxSize()) {
        // Panneau latéral avec statistiques (280dp)
        CategoryStatsPanel(
            stats = categoryStats,
            showArchived = showArchivedCategories,
            onToggleArchived = { showArchivedCategories = it },
            onCreateCategory = if (canCreateCategory(currentUserRole)) {
                { showCreateCategoryDialog = true }
            } else null,
            modifier = Modifier.width(280.dp)
        )

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

        // Panneau principal
        Column(modifier = Modifier.weight(1f)) {
            // Barre d'outils
            CategoriesToolbar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                sortOption = sortOption,
                onSortChange = { sortOption = it },
                onCreateCategory = if (canCreateCategory(currentUserRole)) {
                    { showCreateCategoryDialog = true }
                } else null,
                onRefresh = { loadCategories() },
                categoryCount = filteredAndSortedCategories.size,
                totalCount = categories.size,
                showArchived = showArchivedCategories
            )

            Row(modifier = Modifier.weight(1f)) {
                // Zone principale des catégories
                Box(
                    modifier = Modifier
                        .weight(if (showCategoryDetails) 0.7f else 1f)
                        .fillMaxHeight()
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ArkaLoadingIndicator(message = "Chargement des catégories...")
                            }
                        }

                        errorMessage != null -> {
                            ErrorDisplay(
                                message = errorMessage!!,
                                onRetry = { loadCategories() }
                            )
                        }

                        filteredAndSortedCategories.isEmpty() -> {
                            EmptyCategoriesView(
                                hasCategories = categories.isNotEmpty(),
                                searchQuery = searchQuery,
                                showArchived = showArchivedCategories,
                                onClearSearch = { searchQuery = "" },
                                onCreateCategory = if (canCreateCategory(currentUserRole)) {
                                    { showCreateCategoryDialog = true }
                                } else null
                            )
                        }

                        else -> {
                            when (viewMode) {
                                CategoryViewMode.GRID -> {
                                    CategoriesGridView(
                                        categories = filteredAndSortedCategories,
                                        selectedCategory = selectedCategory,
                                        currentUserRole = currentUserRole,
                                        onCategoryClick = { category ->
                                            selectedCategory = category
                                            showCategoryDetails = true
                                            onCategoryClick(category.id)
                                        },
                                        onEditCategory = { category ->
                                            categoryToEdit = category
                                            showEditCategoryDialog = true
                                        },
                                        onDeleteCategory = { category ->
                                            categoryToEdit = category
                                            showDeleteConfirmation = true
                                        },
                                        onManagePermissions = { category ->
                                            categoryToEdit = category
                                            showPermissionsDialog = true
                                        }
                                    )
                                }

                                CategoryViewMode.LIST -> {
                                    CategoriesListView(
                                        categories = filteredAndSortedCategories,
                                        selectedCategory = selectedCategory,
                                        currentUserRole = currentUserRole,
                                        onCategoryClick = { category ->
                                            selectedCategory = category
                                            showCategoryDetails = true
                                            onCategoryClick(category.id)
                                        },
                                        onEditCategory = { category ->
                                            categoryToEdit = category
                                            showEditCategoryDialog = true
                                        },
                                        onDeleteCategory = { category ->
                                            categoryToEdit = category
                                            showDeleteConfirmation = true
                                        },
                                        onManagePermissions = { category ->
                                            categoryToEdit = category
                                            showPermissionsDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Panneau de détails (si activé)
                if (showCategoryDetails && selectedCategory != null) {
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

                    CategoryDetailsPanel(
                        category = selectedCategory!!,
                        currentUserRole = currentUserRole,
                        onClose = { showCategoryDetails = false },
                        onEditCategory = {
                            categoryToEdit = selectedCategory
                            showEditCategoryDialog = true
                        },
                        onManagePermissions = {
                            categoryToEdit = selectedCategory
                            showPermissionsDialog = true
                        },
                        onViewFiles = { onCategoryClick(selectedCategory!!.id) },
                        modifier = Modifier.width(350.dp)
                    )
                }
            }
        }
    }

    // Dialogues
    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onConfirm = { name, description, color, icon ->
                scope.launch {
                    try {
                        // TODO: Créer catégorie
                        loadCategories()
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de la création: ${e.message}"
                    }
                }
                showCreateCategoryDialog = false
            },
            onDismiss = { showCreateCategoryDialog = false }
        )
    }

    if (showEditCategoryDialog && categoryToEdit != null) {
        EditCategoryDialog(
            category = categoryToEdit!!,
            onConfirm = { updatedCategory ->
                scope.launch {
                    try {
                        // TODO: Mettre à jour catégorie
                        loadCategories()
                        if (selectedCategory?.id == categoryToEdit?.id) {
                            selectedCategory = updatedCategory
                        }
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de la mise à jour: ${e.message}"
                    }
                }
                showEditCategoryDialog = false
                categoryToEdit = null
            },
            onDismiss = {
                showEditCategoryDialog = false
                categoryToEdit = null
            }
        )
    }

    if (showDeleteConfirmation && categoryToEdit != null) {
        DeleteCategoryConfirmationDialog(
            category = categoryToEdit!!,
            onConfirm = {
                scope.launch {
                    try {
                        // TODO: Supprimer/archiver catégorie
                        if (selectedCategory?.id == categoryToEdit?.id) {
                            selectedCategory = null
                            showCategoryDetails = false
                        }
                        loadCategories()
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de la suppression: ${e.message}"
                    }
                }
                showDeleteConfirmation = false
                categoryToEdit = null
            },
            onDismiss = {
                showDeleteConfirmation = false
                categoryToEdit = null
            }
        )
    }

    if (showPermissionsDialog && categoryToEdit != null) {
        CategoryPermissionsDialog(
            category = categoryToEdit!!,
            onPermissionsChanged = { permissions ->
                scope.launch {
                    try {
                        // TODO: Mettre à jour permissions
                        loadCategories()
                    } catch (e: Exception) {
                        errorMessage = "Erreur lors de la mise à jour des permissions: ${e.message}"
                    }
                }
                showPermissionsDialog = false
                categoryToEdit = null
            },
            onDismiss = {
                showPermissionsDialog = false
                categoryToEdit = null
            }
        )
    }
}

/**
 * Panneau latéral avec statistiques des catégories
 */
@Composable
private fun CategoryStatsPanel(
    stats: CategoryStats?,
    showArchived: Boolean,
    onToggleArchived: (Boolean) -> Unit,
    onCreateCategory: (() -> Unit)?,
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
                    text = "Catégories",
                    style = ArkaTextStyles.cardTitle,
                    fontWeight = FontWeight.Bold
                )
            }

            if (stats != null) {
                item {
                    StatsCard(
                        title = "Catégories actives",
                        value = "${stats.activeCategories}",
                        subtitle = "sur ${stats.totalCategories} catégories",
                        icon = Icons.Default.Category,
                        color = Color(0xFF4CAF50)
                    )
                }

                item {
                    StatsCard(
                        title = "Fichiers organisés",
                        value = "${stats.totalFiles}",
                        subtitle = "dans toutes les catégories",
                        icon = Icons.Default.Description,
                        color = Color(0xFF2196F3)
                    )
                }

                item {
                    StatsCard(
                        title = "Catégorie principale",
                        value = stats.mostUsedCategory?.name ?: "Aucune",
                        subtitle = "${stats.mostUsedCategory?.fileCount ?: 0} fichiers",
                        icon = Icons.Default.Star,
                        color = Color(0xFFFF9800)
                    )
                }

                if (stats.archivedCategories > 0) {
                    item {
                        StatsCard(
                            title = "Catégories archivées",
                            value = "${stats.archivedCategories}",
                            subtitle = "Non visibles",
                            icon = Icons.Default.Archive,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }

            item {
                Divider()
            }

            item {
                Text(
                    text = "Affichage",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleArchived(!showArchived) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showArchived,
                        onCheckedChange = onToggleArchived
                    )

                    Column {
                        Text(
                            text = "Inclure les archivées",
                            style = ArkaTextStyles.cardDescription
                        )

                        Text(
                            text = "Afficher les catégories archivées",
                            style = ArkaTextStyles.helpText,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (onCreateCategory != null) {
                item {
                    Divider()
                }

                item {
                    ArkaButton(
                        text = "Nouvelle catégorie",
                        onClick = onCreateCategory,
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
                        text = "Exporter structure",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.Download,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ArkaOutlinedButton(
                        text = "Réorganiser",
                        onClick = { /* TODO */ },
                        icon = Icons.Default.DragIndicator,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Barre d'outils de gestion des catégories
 */
@Composable
private fun CategoriesToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    viewMode: CategoryViewMode,
    onViewModeChange: (CategoryViewMode) -> Unit,
    sortOption: CategorySortOption,
    onSortChange: (CategorySortOption) -> Unit,
    onCreateCategory: (() -> Unit)?,
    onRefresh: () -> Unit,
    categoryCount: Int,
    totalCount: Int,
    showArchived: Boolean
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
                    text = "Gestion des catégories",
                    style = ArkaTextStyles.cardTitle,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (categoryCount == totalCount) {
                        "$totalCount catégorie${if (totalCount > 1) "s" else ""}"
                    } else {
                        "$categoryCount sur $totalCount catégorie${if (totalCount > 1) "s" else ""}"
                    } + if (showArchived) " (avec archivées)" else "",
                    style = ArkaTextStyles.helpText,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Rechercher une catégorie...") },
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
                modifier = Modifier.width(300.dp)
            )

            // Tri
            var showSort by remember { mutableStateOf(false) }
            IconButton(onClick = { showSort = true }) {
                Icon(Icons.Default.Sort, contentDescription = "Trier")
            }

            DropdownMenu(
                expanded = showSort,
                onDismissRequest = { showSort = false }
            ) {
                CategorySortOption.values().forEach { sort ->
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

            // Mode d'affichage
            Row {
                IconButton(
                    onClick = { onViewModeChange(CategoryViewMode.LIST) }
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Vue liste",
                        tint = if (viewMode == CategoryViewMode.LIST) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { onViewModeChange(CategoryViewMode.GRID) }
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Vue grille",
                        tint = if (viewMode == CategoryViewMode.GRID) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Divider(modifier = Modifier.height(24.dp).width(1.dp))

            // Actions
            if (onCreateCategory != null) {
                ArkaButton(
                    text = "Nouvelle",
                    onClick = onCreateCategory,
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
 * Vue en grille des catégories
 */
@Composable
private fun CategoriesGridView(
    categories: List<CategoryInfo>,
    selectedCategory: CategoryInfo?,
    currentUserRole: UserRole,
    onCategoryClick: (CategoryInfo) -> Unit,
    onEditCategory: (CategoryInfo) -> Unit,
    onDeleteCategory: (CategoryInfo) -> Unit,
    onManagePermissions: (CategoryInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(200.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(categories) { category ->
            CategoryGridCard(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                currentUserRole = currentUserRole,
                onClick = { onCategoryClick(category) },
                onEdit = { onEditCategory(category) },
                onDelete = { onDeleteCategory(category) },
                onManagePermissions = { onManagePermissions(category) }
            )
        }
    }
}

/**
 * Carte de catégorie dans la vue grille
 */
@Composable
private fun CategoryGridCard(
    category: CategoryInfo,
    isSelected: Boolean,
    currentUserRole: UserRole,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManagePermissions: () -> Unit
) {
    ArkaCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f)
                else Color.Transparent,
                shape = ArkaComponentShapes.cardMedium
            ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // En-tête avec couleur et actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicateur de couleur
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = category.color ?: MaterialTheme.colors.primary,
                                shape = CircleShape
                            )
                    )

                    // Icône de catégorie
                    Icon(
                        imageVector = category.getIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = category.color ?: MaterialTheme.colors.primary
                    )

                    if (category.isArchived) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archivée",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Menu d'actions
                var showActionsMenu by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showActionsMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions",
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showActionsMenu,
                    onDismissRequest = { showActionsMenu = false }
                ) {
                    if (canEditCategory(currentUserRole)) {
                        DropdownMenuItem(onClick = {
                            onEdit()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Modifier")
                            }
                        }
                    }

                    if (canManageCategoryPermissions(currentUserRole)) {
                        DropdownMenuItem(onClick = {
                            onManagePermissions()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Permissions")
                            }
                        }
                    }

                    if (canDeleteCategory(currentUserRole)) {
                        Divider()
                        DropdownMenuItem(onClick = {
                            onDelete()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    if (category.isArchived) Icons.Default.Delete else Icons.Default.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colors.error
                                )
                                Text(
                                    if (category.isArchived) "Supprimer" else "Archiver",
                                    color = MaterialTheme.colors.error
                                )
                            }
                        }
                    }
                }
            }

            // Nom et description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = category.name,
                    style = ArkaTextStyles.cardTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (category.description.isNotEmpty()) {
                    Text(
                        text = category.description,
                        style = ArkaTextStyles.cardDescription,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Statistiques
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )

                    Text(
                        text = "${category.fileCount}",
                        style = ArkaTextStyles.metadata,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = formatFileSize(category.totalSize),
                    style = ArkaTextStyles.fileSize,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Vue en liste des catégories
 */
@Composable
private fun CategoriesListView(
    categories: List<CategoryInfo>,
    selectedCategory: CategoryInfo?,
    currentUserRole: UserRole,
    onCategoryClick: (CategoryInfo) -> Unit,
    onEditCategory: (CategoryInfo) -> Unit,
    onDeleteCategory: (CategoryInfo) -> Unit,
    onManagePermissions: (CategoryInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        // En-tête des colonnes
        item {
            CategoriesListHeader()
        }

        // Liste des catégories
        items(categories) { category ->
            CategoryListItem(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                currentUserRole = currentUserRole,
                onClick = { onCategoryClick(category) },
                onEdit = { onEditCategory(category) },
                onDelete = { onDeleteCategory(category) },
                onManagePermissions = { onManagePermissions(category) }
            )
        }
    }
}

/**
 * En-tête de la liste des catégories
 */
@Composable
private fun CategoriesListHeader() {
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
                text = "Catégorie",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.3f)
            )

            Text(
                text = "Description",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.25f)
            )

            Text(
                text = "Fichiers",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.1f)
            )

            Text(
                text = "Taille",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.1f)
            )

            Text(
                text = "Modifiée",
                style = ArkaTextStyles.tableHeader,
                modifier = Modifier.weight(0.15f)
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
 * Item de catégorie dans la liste
 */
@Composable
private fun CategoryListItem(
    category: CategoryInfo,
    isSelected: Boolean,
    currentUserRole: UserRole,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onManagePermissions: () -> Unit
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
            // Catégorie (nom + icône + couleur)
            Row(
                modifier = Modifier.weight(0.3f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicateur de couleur
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = category.color ?: MaterialTheme.colors.primary,
                            shape = CircleShape
                        )
                )

                Icon(
                    imageVector = category.getIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = category.color ?: MaterialTheme.colors.primary
                )

                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.name,
                            style = ArkaTextStyles.cardDescription,
                            fontWeight = FontWeight.Medium
                        )

                        if (category.isArchived) {
                            StatusBadge(
                                text = "Archivée",
                                backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                                textColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Description
            Text(
                text = category.description,
                style = ArkaTextStyles.metadata,
                modifier = Modifier.weight(0.25f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Nombre de fichiers
            Text(
                text = "${category.fileCount}",
                style = ArkaTextStyles.metadata,
                modifier = Modifier.weight(0.1f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Taille totale
            Text(
                text = formatFileSize(category.totalSize),
                style = ArkaTextStyles.fileSize,
                modifier = Modifier.weight(0.1f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Date de modification
            Text(
                text = formatDate(category.modifiedDate),
                style = ArkaTextStyles.date,
                modifier = Modifier.weight(0.15f),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )

            // Actions
            Row(
                modifier = Modifier.weight(0.1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                var showActionsMenu by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { showActionsMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Menu des actions (même logique que la vue grille)
                DropdownMenu(
                    expanded = showActionsMenu,
                    onDismissRequest = { showActionsMenu = false }
                ) {
                    if (canEditCategory(currentUserRole)) {
                        DropdownMenuItem(onClick = {
                            onEdit()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Modifier")
                            }
                        }
                    }

                    if (canManageCategoryPermissions(currentUserRole)) {
                        DropdownMenuItem(onClick = {
                            onManagePermissions()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Permissions")
                            }
                        }
                    }

                    if (canDeleteCategory(currentUserRole)) {
                        Divider()
                        DropdownMenuItem(onClick = {
                            onDelete()
                            showActionsMenu = false
                        }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    if (category.isArchived) Icons.Default.Delete else Icons.Default.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colors.error
                                )
                                Text(
                                    if (category.isArchived) "Supprimer" else "Archiver",
                                    color = MaterialTheme.colors.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vue vide quand aucune catégorie ne correspond aux critères
 */
@Composable
private fun EmptyCategoriesView(
    hasCategories: Boolean,
    searchQuery: String,
    showArchived: Boolean,
    onClearSearch: () -> Unit,
    onCreateCategory: (() -> Unit)?
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
                imageVector = if (hasCategories) Icons.Default.SearchOff else Icons.Default.Category,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
            )

            Text(
                text = when {
                    !hasCategories -> "Aucune catégorie créée"
                    searchQuery.isNotEmpty() -> "Aucune catégorie trouvée pour \"$searchQuery\""
                    !showArchived -> "Toutes les catégories sont archivées"
                    else -> "Aucune catégorie"
                },
                style = ArkaTextStyles.cardTitle,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = when {
                    !hasCategories -> "Créez votre première catégorie pour organiser vos fichiers"
                    searchQuery.isNotEmpty() -> "Essayez de modifier votre recherche"
                    !showArchived -> "Activez l'affichage des catégories archivées"
                    else -> "Commencez par créer des catégories"
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
                !hasCategories && onCreateCategory != null -> {
                    ArkaButton(
                        text = "Créer ma première catégorie",
                        onClick = onCreateCategory,
                        icon = Icons.Default.Add
                    )
                }
            }
        }
    }
}

/**
 * Panneau de détails d'une catégorie
 */
@Composable
private fun CategoryDetailsPanel(
    category: CategoryInfo,
    currentUserRole: UserRole,
    onClose: () -> Unit,
    onEditCategory: () -> Unit,
    onManagePermissions: () -> Unit,
    onViewFiles: () -> Unit,
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
                        text = "Détails de la catégorie",
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
                // Présentation de la catégorie
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icône grande avec couleur
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = (category.color ?: MaterialTheme.colors.primary).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.getIcon(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = category.color ?: MaterialTheme.colors.primary
                        )
                    }

                    Text(
                        text = category.name,
                        style = ArkaTextStyles.cardTitle,
                        fontWeight = FontWeight.Bold
                    )

                    if (category.description.isNotEmpty()) {
                        Text(
                            text = category.description,
                            style = ArkaTextStyles.cardDescription,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    if (category.isArchived) {
                        StatusBadge(
                            text = "Archivée",
                            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                            textColor = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            item {
                Divider()
            }

            item {
                // Statistiques
                CategoryDetailSection(
                    title = "Statistiques",
                    items = listOf(
                        "Fichiers" to "${category.fileCount}",
                        "Taille totale" to formatFileSize(category.totalSize),
                        "Dossiers" to "${category.folderCount}",
                        "Dernier ajout" to formatRelativeTime(category.lastFileAdded)
                    )
                )
            }

            item {
                CategoryDetailSection(
                    title = "Informations",
                    items = listOf(
                        "Créée le" to formatDate(category.createdDate),
                        "Modifiée le" to formatDate(category.modifiedDate),
                        "Créée par" to category.createdBy,
                        "Couleur" to (category.colorName ?: "Par défaut")
                    )
                )
            }

            if (category.permissions.isNotEmpty()) {
                item {
                    CategoryPermissionsSection(
                        permissions = category.permissions
                    )
                }
            }

            item {
                Divider()
            }

            item {
                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArkaButton(
                        text = "Voir les fichiers",
                        onClick = onViewFiles,
                        icon = Icons.Default.FolderOpen,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (canEditCategory(currentUserRole)) {
                        ArkaOutlinedButton(
                            text = "Modifier catégorie",
                            onClick = onEditCategory,
                            icon = Icons.Default.Edit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (canManageCategoryPermissions(currentUserRole)) {
                        ArkaOutlinedButton(
                            text = "Gérer permissions",
                            onClick = onManagePermissions,
                            icon = Icons.Default.Security,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section de détails d'une catégorie
 */
@Composable
private fun CategoryDetailSection(
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
        }
    }
}

/**
 * Section des permissions d'une catégorie
 */
@Composable
private fun CategoryPermissionsSection(
    permissions: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Permissions spéciales",
            style = ArkaTextStyles.cardDescription,
            fontWeight = FontWeight.Medium
        )

        permissions.forEach { permission ->
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
    }
}

// ================================================================
// DIALOGUES
// ================================================================

/**
 * Dialogue de création de catégorie
 */
@Composable
private fun CreateCategoryDialog(
    onConfirm: (String, String, Color?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var selectedIcon by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer une nouvelle catégorie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Nom
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de la catégorie") },
                    placeholder = { Text("Ex: Documents personnels") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optionnelle)") },
                    placeholder = { Text("Décrivez le contenu de cette catégorie") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Sélecteur de couleur
                Text(
                    text = "Couleur de la catégorie",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                CategoryColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )

                // Sélecteur d'icône
                Text(
                    text = "Icône de la catégorie",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                CategoryIconSelector(
                    selectedIcon = selectedIcon,
                    onIconSelected = { selectedIcon = it }
                )
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Créer",
                onClick = { onConfirm(name, description, selectedColor, selectedIcon) },
                enabled = name.isNotBlank()
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
 * Dialogue d'édition de catégorie
 */
@Composable
private fun EditCategoryDialog(
    category: CategoryInfo,
    onConfirm: (CategoryInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var description by remember { mutableStateOf(category.description) }
    var selectedColor by remember { mutableStateOf(category.color) }
    var selectedIcon by remember { mutableStateOf(category.iconName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la catégorie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Couleur",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                CategoryColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )

                Text(
                    text = "Icône",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                CategoryIconSelector(
                    selectedIcon = selectedIcon,
                    onIconSelected = { selectedIcon = it }
                )
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Enregistrer",
                onClick = {
                    onConfirm(
                        category.copy(
                            name = name,
                            description = description,
                            color = selectedColor,
                            iconName = selectedIcon
                        )
                    )
                },
                enabled = name.isNotBlank()
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
 * Sélecteur de couleur pour catégorie
 */
@Composable
private fun CategoryColorSelector(
    selectedColor: Color?,
    onColorSelected: (Color?) -> Unit
) {
    val predefinedColors = listOf(
        null, // Couleur par défaut
        Color(0xFFF44336), // Rouge
        Color(0xFFE91E63), // Rose
        Color(0xFF9C27B0), // Violet
        Color(0xFF673AB7), // Violet foncé
        Color(0xFF3F51B5), // Indigo
        Color(0xFF2196F3), // Bleu
        Color(0xFF03A9F4), // Bleu clair
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Vert
        Color(0xFF8BC34A), // Vert clair
        Color(0xFFCDDC39), // Lime
        Color(0xFFFFEB3B), // Jaune
        Color(0xFFFFC107), // Ambre
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Orange rouge
        Color(0xFF795548), // Marron
        Color(0xFF9E9E9E), // Gris
        Color(0xFF607D8B)  // Bleu gris
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(predefinedColors.size) { index ->
            val color = predefinedColors[index]

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = color ?: MaterialTheme.colors.primary,
                        shape = CircleShape
                    )
                    .border(
                        width = if (selectedColor == color) 3.dp else 1.dp,
                        color = if (selectedColor == color) MaterialTheme.colors.onSurface
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (color == null) {
                    Text(
                        text = "Auto",
                        style = ArkaTextStyles.metadata,
                        color = MaterialTheme.colors.onPrimary
                    )
                }

                if (selectedColor == color) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (color == null) MaterialTheme.colors.onPrimary else Color.White
                    )
                }
            }
        }
    }
}

/**
 * Sélecteur d'icône pour catégorie
 */
@Composable
private fun CategoryIconSelector(
    selectedIcon: String?,
    onIconSelected: (String?) -> Unit
) {
    val predefinedIcons = listOf(
        null to Icons.Default.Category,
        "folder" to Icons.Default.Folder,
        "description" to Icons.Default.Description,
        "image" to Icons.Default.Image,
        "video" to Icons.Default.VideoFile,
        "audio" to Icons.Default.AudioFile,
        "work" to Icons.Default.Work,
        "school" to Icons.Default.School,
        "home" to Icons.Default.Home,
        "favorite" to Icons.Default.Favorite,
        "star" to Icons.Default.Star,
        "bookmark" to Icons.Default.Bookmark
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(predefinedIcons.size) { index ->
            val (iconName, icon) = predefinedIcons[index]

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (selectedIcon == iconName) MaterialTheme.colors.primary.copy(alpha = 0.1f)
                        else Color.Transparent,
                        shape = ArkaComponentShapes.cardSmall
                    )
                    .border(
                        width = if (selectedIcon == iconName) 2.dp else 1.dp,
                        color = if (selectedIcon == iconName) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                        shape = ArkaComponentShapes.cardSmall
                    )
                    .clickable { onIconSelected(iconName) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (selectedIcon == iconName) MaterialTheme.colors.primary
                    else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Dialogue de confirmation de suppression
 */
@Composable
private fun DeleteCategoryConfirmationDialog(
    category: CategoryInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (category.isArchived) "Supprimer définitivement" else "Archiver la catégorie")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (category.isArchived) {
                        "Êtes-vous sûr de vouloir supprimer définitivement la catégorie \"${category.name}\" ?"
                    } else {
                        "Êtes-vous sûr de vouloir archiver la catégorie \"${category.name}\" ?"
                    }
                )

                if (category.fileCount > 0) {
                    Text(
                        text = if (category.isArchived) {
                            "Cette action supprimera définitivement la catégorie et :"
                        } else {
                            "Cette action archivera la catégorie et :"
                        },
                        style = ArkaTextStyles.cardDescription,
                        fontWeight = FontWeight.Medium
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• ${category.fileCount} fichier${if (category.fileCount > 1) "s" else ""} seront déplacés vers \"Sans catégorie\"")
                        Text("• ${category.folderCount} dossier${if (category.folderCount > 1) "s" else ""} seront également déplacés")
                        if (category.isArchived) {
                            Text("• Cette action est irréversible")
                        }
                    }
                }
            }
        },
        confirmButton = {
            ArkaButton(
                text = if (category.isArchived) "Supprimer" else "Archiver",
                onClick = onConfirm,
                colors = if (category.isArchived) {
                    ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
                icon = if (category.isArchived) Icons.Default.Delete else Icons.Default.Archive
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
 * Dialogue de gestion des permissions de catégorie
 */
@Composable
private fun CategoryPermissionsDialog(
    category: CategoryInfo,
    onPermissionsChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPermissions by remember { mutableStateOf(category.permissions.toSet()) }

    val availablePermissions = listOf(
        "READ_ONLY" to "Lecture seule pour tous",
        "WRITE_RESTRICTED" to "Écriture restreinte aux responsables",
        "ADMIN_ONLY" to "Accès administrateur uniquement",
        "INHERIT_PARENT" to "Hériter des permissions parent",
        "PUBLIC_READ" to "Lecture publique (famille)",
        "PRIVATE" to "Privé (créateur uniquement)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions de \"${category.name}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Configurez les permissions pour cette catégorie :",
                    style = ArkaTextStyles.cardDescription
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
            }
        },
        confirmButton = {
            ArkaButton(
                text = "Enregistrer",
                onClick = { onPermissionsChanged(selectedPermissions.toList()) }
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
 * Modes d'affichage des catégories
 */
enum class CategoryViewMode {
    LIST, GRID
}

/**
 * Options de tri pour les catégories
 */
enum class CategorySortOption(val displayName: String, val icon: ImageVector) {
    NAME_ASC("Nom (A-Z)", Icons.Default.SortByAlpha),
    NAME_DESC("Nom (Z-A)", Icons.Default.SortByAlpha),
    FILES_ASC("Moins de fichiers", Icons.Default.Description),
    FILES_DESC("Plus de fichiers", Icons.Default.Description),
    CREATED_ASC("Plus anciennes", Icons.Default.DateRange),
    CREATED_DESC("Plus récentes", Icons.Default.DateRange),
    MODIFIED_ASC("Anciennes modifications", Icons.Default.Update),
    MODIFIED_DESC("Récentes modifications", Icons.Default.Update)
}

/**
 * Informations d'une catégorie
 */
data class CategoryInfo(
    val id: Int,
    val name: String,
    val description: String,
    val color: Color?,
    val colorName: String?,
    val iconName: String?,
    val fileCount: Int,
    val folderCount: Int,
    val totalSize: Long,
    val isArchived: Boolean,
    val createdDate: LocalDateTime,
    val modifiedDate: LocalDateTime,
    val lastFileAdded: LocalDateTime,
    val createdBy: String,
    val permissions: List<String>
) {
    fun getIcon(): ImageVector {
        return when (iconName) {
            "folder" -> Icons.Default.Folder
            "description" -> Icons.Default.Description
            "image" -> Icons.Default.Image
            "video" -> Icons.Default.VideoFile
            "audio" -> Icons.Default.AudioFile
            "work" -> Icons.Default.Work
            "school" -> Icons.Default.School
            "home" -> Icons.Default.Home
            "favorite" -> Icons.Default.Favorite
            "star" -> Icons.Default.Star
            "bookmark" -> Icons.Default.Bookmark
            else -> Icons.Default.Category
        }
    }

    companion object {
        fun fromEntity(entity: Any): CategoryInfo {
            // TODO: Mapper depuis les entités réelles
            return CategoryInfo(
                id = 1,
                name = "Documents",
                description = "Documents personnels et administratifs",
                color = Color(0xFF2196F3),
                colorName = "Bleu",
                iconName = "description",
                fileCount = 25,
                folderCount = 5,
                totalSize = 1024 * 1024 * 50L, // 50MB
                isArchived = false,
                createdDate = LocalDateTime.now().minusDays(30),
                modifiedDate = LocalDateTime.now().minusDays(2),
                lastFileAdded = LocalDateTime.now().minusHours(6),
                createdBy = "John Doe",
                permissions = emptyList()
            )
        }
    }
}

/**
 * Statistiques des catégories
 */
data class CategoryStats(
    val totalCategories: Int,
    val activeCategories: Int,
    val archivedCategories: Int,
    val totalFiles: Int,
    val mostUsedCategory: CategoryInfo?
) {
    companion object {
        fun fromEntity(entity: Any): CategoryStats {
            // TODO: Mapper depuis les entités réelles
            return CategoryStats(
                totalCategories = 8,
                activeCategories = 6,
                archivedCategories = 2,
                totalFiles = 150,
                mostUsedCategory = null
            )
        }
    }
}

// ================================================================
// FONCTIONS DE VÉRIFICATION DES PERMISSIONS
// ================================================================

private fun canCreateCategory(userRole: UserRole): Boolean {
    return userRole == UserRole.ADMIN || userRole == UserRole.RESPONSIBLE
}

private fun canEditCategory(userRole: UserRole): Boolean {
    return userRole == UserRole.ADMIN || userRole == UserRole.RESPONSIBLE
}

private fun canDeleteCategory(userRole: UserRole): Boolean {
    return userRole == UserRole.ADMIN || userRole == UserRole.RESPONSIBLE
}

private fun canManageCategoryPermissions(userRole: UserRole): Boolean {
    return userRole == UserRole.ADMIN
}

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatDate(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return dateTime.format(formatter)
}

private fun formatRelativeTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val diff = Duration.between(dateTime, now)

    return when {
        diff.toDays() > 7 -> formatDate(dateTime)
        diff.toDays() > 0 -> "Il y a ${diff.toDays()} jour${if (diff.toDays() > 1) "s" else ""}"
        diff.toHours() > 0 -> "Il y a ${diff.toHours()} heure${if (diff.toHours() > 1) "s" else ""}"
        diff.toMinutes() > 0 -> "Il y a ${diff.toMinutes()} minute${if (diff.toMinutes() > 1) "s" else ""}"
        else -> "À l'instant"
    }
}