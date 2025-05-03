package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import controllers.CategoryController
import controllers.FamilyMemberController
import controllers.FileController
import controllers.FolderController
import ktorm.Category
import ktorm.FamilyMember
import ktorm.File
import ktorm.Folder
import ui.theme.ArkaTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
@Preview
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo temporaire
        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(bottom = 16.dp)
                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text("Arka", style = MaterialTheme.typography.h4)
        }

        Card(
            modifier = Modifier.width(400.dp).padding(16.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Connexion",
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.h5
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colors.error)
                }
                Button(
                    onClick = {
                        if (email.text.isNotEmpty() && password.text.isNotEmpty()) {
                            onLogin(email.text, password.text)
                        } else {
                            errorMessage = "Veuillez remplir tous les champs"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Se connecter",
                        color = Color.White // Force explicitement la couleur du texte
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    ArkaTheme {
        LoginScreen { _, _ -> }
    }
}

@Composable
fun FamilyMemberScreen(
    userId: Int,
    isAdmin: Boolean,
    familyMemberController: FamilyMemberController
) {
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Charger les membres de la famille
    LaunchedEffect(Unit) {
        val currentUser = familyMemberController.getMemberById(userId)
        if (currentUser != null) {
            members = familyMemberController.getAllMembersOfFamily(currentUser.familyID)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestion des membres de la famille", style = MaterialTheme.typography.h5)

            if (isAdmin) {
                Button(onClick = { showAddDialog = true }) {
                    Text("Ajouter un membre")
                }
            }
        }

        // Liste des membres
        if (members.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun membre trouvé")
            }
        } else {
            LazyColumn {
                items(members) { member ->
                    FamilyMemberItem(
                        member = member,
                        isAdmin = isAdmin,
                        isCurrentUser = member.familyMemberID == userId,
                        onEdit = {
                            selectedMember = member
                            showEditDialog = true
                        },
                        onDelete = {
                            selectedMember = member
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Dialogue d'ajout de membre
    if (showAddDialog) {
        FamilyMemberDialog(
            member = null,
            onDismiss = { showAddDialog = false },
            onSave = { firstName, email, birthDate, gender, isParent, isAdmin ->
                val currentUser = familyMemberController.getMemberById(userId)
                if (currentUser != null) {
                    val newMember = FamilyMember(
                        familyMemberFirstName = firstName,
                        familyMemberMail = email,
                        familyMemberPassword = "password123", // Mot de passe temporaire à changer
                        familyMemberBirthDate = birthDate,
                        familyMemberGender = gender,
                        isParent = isParent,
                        isAdmin = isAdmin,
                        familyID = currentUser.familyID
                    )

                    val addedMember = familyMemberController.addMember(newMember, "password123")
                    if (addedMember != null) {
                        members = familyMemberController.getAllMembersOfFamily(currentUser.familyID)
                    }
                }

                showAddDialog = false
            }
        )
    }

    // Dialogue de modification de membre
    if (showEditDialog && selectedMember != null) {
        FamilyMemberDialog(
            member = selectedMember,
            onDismiss = { showEditDialog = false },
            onSave = { firstName, email, birthDate, gender, isParent, isAdmin ->
                val updatedMember = selectedMember!!.copy(
                    familyMemberFirstName = firstName,
                    familyMemberMail = email,
                    familyMemberBirthDate = birthDate,
                    familyMemberGender = gender,
                    isParent = isParent,
                    isAdmin = isAdmin
                )

                val success = familyMemberController.updateMember(updatedMember)
                if (success) {
                    val currentUser = familyMemberController.getMemberById(userId)
                    if (currentUser != null) {
                        members = familyMemberController.getAllMembersOfFamily(currentUser.familyID)
                    }
                }

                showEditDialog = false
                selectedMember = null
            }
        )
    }

    // Dialogue de confirmation de suppression
    if (showDeleteDialog && selectedMember != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer le membre") },
            text = { Text("Êtes-vous sûr de vouloir supprimer ${selectedMember!!.familyMemberFirstName} ?") },
            confirmButton = {
                Button(
                    onClick = {
                        val success = familyMemberController.deleteMember(selectedMember!!.familyMemberID!!)
                        if (success) {
                            val currentUser = familyMemberController.getMemberById(userId)
                            if (currentUser != null) {
                                members = familyMemberController.getAllMembersOfFamily(currentUser.familyID)
                            }
                        }

                        showDeleteDialog = false
                        selectedMember = null
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
fun FamilyMemberItem(
    member: FamilyMember,
    isAdmin: Boolean,
    isCurrentUser: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.familyMemberFirstName + (if (isCurrentUser) " (Vous)" else ""),
                    style = MaterialTheme.typography.h6
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = member.familyMemberMail,
                    style = MaterialTheme.typography.body2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Né(e) le: ${member.familyMemberBirthDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    style = MaterialTheme.typography.body2
                )

                Text(
                    text = "Rôle: ${if (member.isAdmin) "Administrateur" else if (member.isParent) "Parent" else "Enfant"}",
                    style = MaterialTheme.typography.body2
                )
            }

            if (isAdmin && !isCurrentUser) {
                Row {
                    Button(
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
            } else if (isCurrentUser) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Modifier mon profil")
                }
            }
        }
    }
}

@Composable
fun FamilyMemberDialog(
    member: FamilyMember?,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate, String, Boolean, Boolean) -> Unit
) {
    var firstName by remember { mutableStateOf(member?.familyMemberFirstName ?: "") }
    var email by remember { mutableStateOf(member?.familyMemberMail ?: "") }

    // Gestion de la date de naissance (simplifiée)
    var birthDateStr by remember {
        mutableStateOf(
            member?.familyMemberBirthDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""
        )
    }

    var gender by remember { mutableStateOf(member?.familyMemberGender ?: "Homme") }
    var isParent by remember { mutableStateOf(member?.isParent ?: false) }
    var isAdmin by remember { mutableStateOf(member?.isAdmin ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (member == null) "Ajouter un membre" else "Modifier le membre") },
        text = {
            Column {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = birthDateStr,
                    onValueChange = { birthDateStr = it },
                    label = { Text("Date de naissance (JJ/MM/AAAA)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                // Sélection du genre (simplifiée)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Genre: ", modifier = Modifier.padding(end = 8.dp))

                    Button(
                        onClick = { gender = "Homme" },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (gender == "Homme")
                                MaterialTheme.colors.primary else MaterialTheme.colors.surface
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Homme")
                    }

                    Button(
                        onClick = { gender = "Femme" },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (gender == "Femme")
                                MaterialTheme.colors.primary else MaterialTheme.colors.surface
                        )
                    ) {
                        Text("Femme")
                    }
                }

                // Rôles
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isParent,
                        onCheckedChange = { isParent = it }
                    )

                    Text("Parent", modifier = Modifier.padding(end = 16.dp))

                    Checkbox(
                        checked = isAdmin,
                        onCheckedChange = { isAdmin = it }
                    )

                    Text("Administrateur")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val birthDate = LocalDate.parse(
                            birthDateStr,
                            DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        )
                        onSave(firstName, email, birthDate, gender, isParent, isAdmin)
                    } catch (e: Exception) {
                        // Gestion des erreurs de format de date
                        println("Erreur de format de date: ${e.message}")
                    }
                },
                enabled = firstName.isNotBlank() && email.isNotBlank() && birthDateStr.isNotBlank()
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
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Charger les catégories
    LaunchedEffect(Unit) {
        try {
            categories = categoryController.getAllCategories()
        } catch (e: Exception) {
            println("Erreur lors du chargement des catégories: ${e.message}")
        }
    }

    // Charger les dossiers en fonction de la catégorie sélectionnée ou du dossier courant
    LaunchedEffect(currentFolder, selectedCategoryId) {
        try {
            if (currentFolder != null) {
                // Chargement des sous-dossiers
                folders = folderController.getSubFolders(currentFolder!!.folderID!!)
                files = fileController.getFilesInFolder(currentFolder!!.folderID!!)
            } else if (selectedCategoryId != null) {
                // Chargement des dossiers par catégorie
                folders = folderController.getFoldersByCategory(userId, selectedCategoryId!!)
                files = emptyList()
            } else {
                // Chargement des dossiers racines
                folders = folderController.getRootFoldersForMember(userId)
                files = emptyList()
            }
        } catch (e: Exception) {
            println("Erreur lors du chargement des dossiers: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sélecteur de catégorie
        if (currentFolder == null) {
            CategorySelector(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { categoryId ->
                    selectedCategoryId = categoryId
                }
            )
        }

        // Barre de navigation
        if (currentFolder != null || selectedCategoryId != null) {
            NavigationBar(
                currentPath = currentPath,
                currentCategory = if (selectedCategoryId != null)
                    categories.find { it.categoryID == selectedCategoryId } else null,
                onNavigate = { folder ->
                    if (folder == null) {
                        // Retour à la racine
                        currentPath = emptyList()
                        currentFolder = null
                    } else {
                        // Navigation vers un dossier du chemin
                        val index = currentPath.indexOf(folder)
                        if (index >= 0) {
                            currentPath = currentPath.subList(0, index + 1)
                            currentFolder = folder
                        }
                    }
                },
                onBackToCategories = {
                    currentPath = emptyList()
                    currentFolder = null
                    selectedCategoryId = null
                }
            )
        }

        // Barre d'actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = { /* Code pour créer un dossier */ },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Nouveau dossier")
            }

            Button(onClick = { /* Code pour ajouter un fichier */ }) {
                Text("Ajouter un fichier")
            }
        }

        // Contenu principal
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (folders.isEmpty() && files.isEmpty()) {
            // Message si aucun élément
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (selectedCategoryId != null)
                            "Aucun dossier dans cette catégorie"
                        else if (currentFolder != null)
                            "Ce dossier est vide"
                        else
                            "Aucun dossier trouvé",
                        style = MaterialTheme.typography.h6,
                        textAlign = TextAlign.Companion.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { /* Code pour créer un dossier */ }) {
                        Text("Créer un dossier")
                    }
                }
            }
        } else {
            // Liste des dossiers et fichiers
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (folders.isNotEmpty()) {
                    item {
                        Text(
                            "Dossiers",
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(folders) { folder ->
                        FolderCard(
                            folder = folder,
                            onClick = {
                                currentFolder = folder
                                currentPath = currentPath + folder
                            }
                        )
                    }
                }

                if (files.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Fichiers",
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                    }

                    items(files) { file ->
                        FileCard(file = file, onClick = { /* Télécharger ou ouvrir */ })
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            "Sélectionnez une catégorie",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bouton "Tous"
            OutlinedButton(
                onClick = { onCategorySelected(null) },
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (selectedCategoryId == null)
                        MaterialTheme.colors.primary.copy(alpha = 0.12f)
                    else
                        Color.Companion.Transparent
                )
            ) {
                Text("Tous")
            }

            // Boutons de catégories
            categories.forEach { category ->
                OutlinedButton(
                    onClick = { onCategorySelected(category.categoryID) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (selectedCategoryId == category.categoryID)
                            MaterialTheme.colors.primary.copy(alpha = 0.12f)
                        else
                            Color.Companion.Transparent
                    )
                ) {
                    Text(category.categoryLabel)
                }
            }
        }
    }
}

@Composable
fun NavigationBar(
    currentPath: List<Folder>,
    currentCategory: Category?,
    onNavigate: (Folder?) -> Unit,
    onBackToCategories: () -> Unit
) {
    Surface(
        color = MaterialTheme.colors.surface,
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton retour aux catégories
            TextButton(onClick = onBackToCategories) {
                Text("Catégories")
            }

            Text(" > ")

            // Catégorie actuelle (si applicable)
            if (currentCategory != null) {
                TextButton(onClick = { onNavigate(null) }) {
                    Text(currentCategory.categoryLabel)
                }
            }

            // Chemin des dossiers
            currentPath.forEachIndexed { index, folder ->
                Text(" > ")

                TextButton(onClick = { onNavigate(folder) }) {
                    Text(folder.folderName)
                }
            }
        }
    }
}

@Composable
fun FolderCard(
    folder: Folder,
    onClick: () -> Unit
) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône stylisée pour le dossier
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📁",
                    style = MaterialTheme.typography.h6
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    folder.folderName,
                    style = MaterialTheme.typography.subtitle1
                )

                Text(
                    "Créé le ${folder.folderCreationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
fun FileCard(
    file: File,
    onClick: () -> Unit
) {
    Card(
        elevation = 2.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône stylisée pour le fichier
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colors.secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📄",
                    style = MaterialTheme.typography.h6
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.fileName,
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis
                )

                Text(
                    "Taille: ${formatFileSize(file.fileSize)}",
                    style = MaterialTheme.typography.caption
                )
            }

            Spacer(Modifier.width(8.dp))

            OutlinedButton(onClick = onClick) {
                Text("Télécharger")
            }
        }
    }
}

// Fonction utilitaire pour formater la taille des fichiers
fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size o"
        size < 1024 * 1024 -> "${size / 1024} Ko"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} Mo"
        else -> "${size / (1024 * 1024 * 1024)} Go"
    }
}