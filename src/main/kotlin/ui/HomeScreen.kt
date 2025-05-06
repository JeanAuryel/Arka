package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import controllers.*

@Composable
fun HomeScreen(
    currentUserId: Int,
    onLogout: () -> Unit
) {
    // Récupération des dépendances via Koin
    val folderController = org.koin.java.KoinJavaComponent.getKoin().get<FolderController>()
    val fileController = org.koin.java.KoinJavaComponent.getKoin().get<FileController>()
    val categoryController = org.koin.java.KoinJavaComponent.getKoin().get<CategoryController>()
    val familyMemberController = org.koin.java.KoinJavaComponent.getKoin().get<FamilyMemberController>()
    val familyController = org.koin.java.KoinJavaComponent.getKoin().get<FamilyController>()

    var currentSection by remember { mutableStateOf("folders") }
    var currentUser by remember { mutableStateOf<ktorm.FamilyMember?>(null) }

    LaunchedEffect(currentUserId) {
        currentUser = familyMemberController.getFamilyMemberById(currentUserId)
    }

    val isAdmin = currentUser?.isAdmin ?: false

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            color = MaterialTheme.colors.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colors.onPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", style = MaterialTheme.typography.subtitle1, color = MaterialTheme.colors.onPrimary)
                    }

                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Arka - Gestion des documents familiaux",
                        color = MaterialTheme.colors.onPrimary,
                        style = MaterialTheme.typography.subtitle1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        CustomBadge("Admin", backgroundColor = Color(0xFFF57F17))
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        currentUser?.familyMemberFirstName ?: "",
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Déconnexion",
                            color = Color.White)

                    }
                }
            }
        }

        // Main content with sidebar and screen area
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            Surface(
                color = MaterialTheme.colors.surface,
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Menu",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = Color.White
                    )

                    MenuButton("Mes dossiers", currentSection == "folders") {
                        currentSection = "folders"
                    }

                    MenuButton("Membres de la famille", currentSection == "family_members") {
                        currentSection = "family_members"
                    }

                    if (isAdmin) {
                        MenuButton("Familles", currentSection == "families") {
                            currentSection = "families"
                        }

                        MenuButton("Catégories", currentSection == "categories") {
                            currentSection = "categories"
                        }
                    }
                }
            }

            // Main view content
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                when (currentSection) {
                    "folders" -> FolderScreen(
                        userId = currentUserId,
                        folderController = folderController,
                        categoryController = categoryController,
                        fileController = fileController
                    )
                    "family_members" -> FamilyMemberScreen(
                        userId = currentUserId,
                        isAdmin = isAdmin,
                        familyMemberController = familyMemberController
                    )
                    "families" -> if (isAdmin) {
                        FamilyScreen(
                            currentUserId = currentUserId,
                            familyController = familyController,
                            familyMemberController = familyMemberController
                        )
                    }
                    "categories" -> if (isAdmin) {
                        CategoryScreen(
                            categoryController = categoryController
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun MenuButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val background = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface

    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text,
            color = textColor,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
fun CustomBadge(text: String, backgroundColor: Color) {
    Box(
        modifier = Modifier
            .background(backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.caption
        )
    }
}