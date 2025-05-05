package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import controllers.CategoryController
import controllers.FileController
import controllers.FolderController
import ktorm.Category
import ktorm.File
import ktorm.Folder
import utils.FileUtils
import java.time.format.DateTimeFormatter

@Composable
fun FolderScreen(
    userId: Int,
    folderController: FolderController,
    fileController: FileController,
    categoryController: CategoryController
) {
    var folders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var currentPath by remember { mutableStateOf<List<Folder>>(emptyList()) }
    var currentFolder by remember { mutableStateOf<Folder?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showUploadFileDialog by remember { mutableStateOf(false) }

    // Charger les catégories
    LaunchedEffect(Unit) {
        categories = categoryController.getAllCategories()
    }

    // Charger les dossiers en fonction de la navigation
    LaunchedEffect(currentFolder, selectedCategory) {
        if (currentFolder != null) {
            // Charger les sous-dossiers du dossier actuel
            folders = folderController.getSubFolders(currentFolder!!.folderID!!)
            files = fileController.getFilesInFolder(currentFolder!!.folderID!!)
        } else if (selectedCategory != null) {
            // Charger les dossiers de la catégorie sélectionnée
            folders = folderController.getFoldersByCategory(userId, selectedCategory!!.categoryID!!)
            files = emptyList()
        } else {
            // Afficher les catégories uniquement
            folders = folderController.getRootFoldersForMember(userId)
            files = emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Barre de navigation
        FolderPathBar(
            path = currentPath,
            currentCategory = selectedCategory,
            onNavigate = { folder ->
                if (folder == null) {
                    // Retour à la racine
                    currentPath = emptyList()
                    currentFolder = null
                    selectedCategory = null
                } else {
                    // Navigation vers un dossier du chemin
                    val index = currentPath.indexOf(folder)
                    if (index >= 0) {
                        currentPath = currentPath.subList(0, index + 1)
                        currentFolder = folder
                    }
                }
            },
            onCategorySelected = { category ->
                selectedCategory = category
                currentFolder = null
                currentPath = emptyList()
            }
        )

        // Affichage des catégories si nous sommes à la racine
        if (currentFolder == null && selectedCategory == null) {
            Text(
                "Catégories",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            LazyColumn {
                items(categories) { category ->
                    CategoryListItem(
                        category = category,
                        onClick = {
                            selectedCategory = category
                        }
                    )
                }
            }
        } else {
            // Actions pour la gestion des dossiers et fichiers
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showAddFolderDialog = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Nouveau dossier")
                }

                Button(onClick = { showUploadFileDialog = true }) {
                    Text("Ajouter un fichier")
                }
            }

            // Contenu du dossier ou de la catégorie
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = 2.dp
            ) {
                if (folders.isEmpty() && files.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucun contenu disponible")
                    }
                } else {
                    LazyColumn {
                        if (folders.isNotEmpty()) {
                            item {
                                Text(
                                    "Dossiers",
                                    style = MaterialTheme.typography.h6,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            items(folders) { folder ->
                                FolderItem(
                                    folder = folder,
                                    onClick = {
                                        // Navigation vers le sous-dossier
                                        currentFolder = folder
                                        currentPath = currentPath + folder
                                    }
                                )
                            }
                        }

                        if (files.isNotEmpty()) {
                            item {
                                Text(
                                    "Fichiers",
                                    style = MaterialTheme.typography.h6,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            items(files) { file ->
                                FileItem(
                                    file = file,
                                    onClick = {
                                        // Téléchargement ou ouverture du fichier
                                        println("Téléchargement du fichier ${file.fileName}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogue d'ajout de dossier
    if (showAddFolderDialog) {
        AddFolderDialog(
            categories = categories,
            currentFolderId = currentFolder?.folderID,
            currentCategoryId = selectedCategory?.categoryID,
            userId = userId,
            onDismiss = { showAddFolderDialog = false },
            onFolderCreated = { newFolder ->
                // Actualiser la liste des dossiers
                if (currentFolder != null) {
                    folders = folderController.getSubFolders(currentFolder!!.folderID!!)
                } else if (selectedCategory != null) {
                    folders = folderController.getFoldersByCategory(userId, selectedCategory!!.categoryID!!)
                } else {
                    folders = folderController.getRootFoldersForMember(userId)
                }
                showAddFolderDialog = false
            },
            folderController = folderController
        )
    }

    // Dialogue d'ajout de fichier
    if (showUploadFileDialog) {
        UploadFileDialog(
            folderId = currentFolder?.folderID,
            onDismiss = { showUploadFileDialog = false },
            onFileUploaded = {
                // Actualiser la liste des fichiers
                if (currentFolder != null) {
                    files = fileController.getFilesInFolder(currentFolder!!.folderID!!)
                }
                showUploadFileDialog = false
            },
            fileController = fileController
        )
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône de catégorie (vous pouvez personaliser cela)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(getCategoryColor(category.categoryID ?: 0)),
                contentAlignment = Alignment.Center
            ) {
                Text("📂", fontSize = MaterialTheme.typography.h6.fontSize)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = category.categoryLabel,
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = category.categoryDescription ?: "",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
fun FolderPathBar(
    path: List<Folder>,
    currentCategory: Category?,
    onNavigate: (Folder?) -> Unit,
    onCategorySelected: (Category?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(MaterialTheme.colors.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bouton Accueil
        Button(
            onClick = {
                onNavigate(null)
                onCategorySelected(null)
            },
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text("Accueil")
        }

        // Affichage de la catégorie si sélectionnée
        if (currentCategory != null) {
            Text(" > ", color = MaterialTheme.colors.onSurface)
            Text(
                text = currentCategory.categoryLabel,
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.clickable {
                    onCategorySelected(currentCategory)
                    onNavigate(null)
                }
            )
        }

        // Chemin des dossiers
        path.forEachIndexed { index, folder ->
            Text(" > ", color = MaterialTheme.colors.onSurface)

            Text(
                text = folder.folderName,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.clickable { onNavigate(folder) }
            )
        }
    }
}

@Composable
fun FolderItem(
    folder: Folder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône simplifiée pour le dossier
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF9BBEE8)),
                contentAlignment = Alignment.Center
            ) {
                Text("📁")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = folder.folderName,
                    style = MaterialTheme.typography.subtitle1
                )

                Text(
                    text = "Créé le ${folder.folderCreationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
fun FileItem(
    file: File,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône simplifiée pour le fichier
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE8D09B)),
                contentAlignment = Alignment.Center
            ) {
                Text("📄")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.subtitle1
                )

                Text(
                    text = "Taille: ${FileUtils.formatFileSize(file.fileSize)}",
                    style = MaterialTheme.typography.caption
                )
            }

            Button(onClick = onClick) {
                Text("Télécharger")
            }
        }
    }
}

@Composable
fun AddFolderDialog(
    categories: List<Category>,
    currentFolderId: Int?,
    currentCategoryId: Int?,
    userId: Int,
    onDismiss: () -> Unit,
    onFolderCreated: (Folder) -> Unit,
    folderController: FolderController
) {
    var folderName by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(currentCategoryId ?: categories.firstOrNull()?.categoryID) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau dossier") },
        text = {
            Column {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Nom du dossier") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // Sélection de la catégorie (seulement si pas déjà dans une catégorie)
                if (currentCategoryId == null) {
                    Text("Catégorie:", modifier = Modifier.padding(bottom = 8.dp))

                    Column {
                        categories.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedCategoryId = category.categoryID },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedCategoryId == category.categoryID,
                                    onClick = { selectedCategoryId = category.categoryID }
                                )

                                Text(
                                    text = category.categoryLabel,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank() && selectedCategoryId != null) {
                        val newFolder = Folder(
                            folderName = folderName,
                            folderCreationDate = java.time.LocalDateTime.now(),
                            parentFolderID = currentFolderId,
                            familyMemberID = userId,
                            categoryID = selectedCategoryId!!
                        )

                        val createdFolder = folderController.createFolder(newFolder)
                        if (createdFolder != null) {
                            onFolderCreated(createdFolder)
                        }
                    }
                },
                enabled = folderName.isNotBlank() && selectedCategoryId != null
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun UploadFileDialog(
    folderId: Int?,
    onDismiss: () -> Unit,
    onFileUploaded: () -> Unit,
    fileController: FileController
) {
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf(0L) }
    var fileSelected by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un fichier") },
        text = {
            Column {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Nom du fichier") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // Bouton de sélection de fichier (simulé)
                Button(
                    onClick = {
                        // Simulation de sélection de fichier
                        fileSelected = true
                        fileSize = 1024 * 1024 // 1 MB
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text("Sélectionner un fichier")
                }

                if (fileSelected) {
                    Text(
                        "Fichier sélectionné: ${FileUtils.formatFileSize(fileSize)}",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank() && folderId != null && fileSelected) {
                        val newFile = File(
                            fileName = fileName,
                            fileSize = fileSize,
                            fileCreationDate = java.time.LocalDateTime.now(),
                            folderID = folderId
                        )

                        val addedFile = fileController.addFile(newFile)
                        if (addedFile != null) {
                            onFileUploaded()
                        }
                    }
                },
                enabled = fileName.isNotBlank() && folderId != null && fileSelected
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// Fonction pour attribuer une couleur à chaque catégorie
private fun getCategoryColor(categoryId: Int): Color {
    val colors = listOf(
        Color(0xFF4CAF50),  // Vert
        Color(0xFF2196F3),  // Bleu
        Color(0xFFF44336),  // Rouge
        Color(0xFFFF9800),  // Orange
        Color(0xFF9C27B0),  // Violet
        Color(0xFF795548)   // Marron
    )

    return colors[categoryId % colors.size]
}