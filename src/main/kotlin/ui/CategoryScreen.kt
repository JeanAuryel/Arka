package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import controllers.CategoryController
import ktorm.Category

@Composable
fun CategoryScreen(categoryController: CategoryController) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Charger les catégories
    LaunchedEffect(Unit) {
        try {
            categories = categoryController.getAllCategories()
        } catch (e: Exception) {
            println("Erreur lors du chargement des catégories: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // En-tête
        Text(
            "Gestion des catégories",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Bouton d'ajout
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.End).padding(bottom = 16.dp)
        ) {
            Text("Ajouter une catégorie")
        }

        // Affichage du chargement ou contenu
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (categories.isEmpty()) {
            // Message si aucune catégorie
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucune catégorie trouvée",
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Grille de catégories
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 250.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categories) { category ->
                    CategoryCard(
                        category = category,
                        onEdit = {
                            selectedCategory = category
                            showEditDialog = true
                        },
                        onDelete = {
                            selectedCategory = category
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Dialogues
    if (showAddDialog) {
        CategoryDialog(
            category = null,
            onDismiss = { showAddDialog = false },
            onSave = { labelText: String, descriptionText: String ->
                val newCategory = Category(
                    categoryLabel = labelText,
                    categoryDescription = descriptionText
                )

                val addedCategory = categoryController.addCategory(newCategory)
                if (addedCategory != null) {
                    categories = categoryController.getAllCategories()
                }
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && selectedCategory != null) {
        CategoryDialog(
            category = selectedCategory,
            onDismiss = { showEditDialog = false },
            onSave = { labelText: String, descriptionText: String ->
                val updatedCategory = selectedCategory!!.copy(
                    categoryLabel = labelText,
                    categoryDescription = descriptionText
                )

                val success = categoryController.updateCategory(updatedCategory)
                if (success) {
                    categories = categoryController.getAllCategories()
                }

                showEditDialog = false
                selectedCategory = null
            }
        )
    }

    if (showDeleteDialog && selectedCategory != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer la catégorie") },
            text = { Text("Êtes-vous sûr de vouloir supprimer la catégorie '${selectedCategory!!.categoryLabel}' ?") },
            confirmButton = {
                Button(
                    onClick = {
                        val success = categoryController.deleteCategory(selectedCategory!!.categoryID!!)
                        if (success) {
                            categories = categoryController.getAllCategories()
                        }

                        showDeleteDialog = false
                        selectedCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.error,
                        contentColor = MaterialTheme.colors.onError
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun CategoryCard(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Attribution d'une couleur unique basée sur l'ID de la catégorie
    val categoryColor = listOf(
        Color(0xFF4CAF50), // Vert
        Color(0xFF2196F3), // Bleu
        Color(0xFFF44336), // Rouge
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Violet
        Color(0xFF795548)  // Marron
    )[category.categoryID?.rem(6) ?: 0]

    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // En-tête coloré
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(categoryColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category.categoryLabel,
                    style = MaterialTheme.typography.h6,
                    color = Color.White
                )
            }

            // Description
            Text(
                category.categoryDescription,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(16.dp)
            )

            // Boutons d'action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Modifier")
                }

                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.error,
                        contentColor = MaterialTheme.colors.onError
                    )
                ) {
                    Text("Supprimer")
                }
            }
        }
    }
}

@Composable
fun CategoryDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var label by remember { mutableStateOf(category?.categoryLabel ?: "") }
    var description by remember { mutableStateOf(category?.categoryDescription ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Ajouter une catégorie" else "Modifier la catégorie") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Libellé") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(label, description) },
                enabled = label.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}