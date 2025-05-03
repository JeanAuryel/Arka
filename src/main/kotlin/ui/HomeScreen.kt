package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.ButtonDefaults
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    currentUserId: Int,
    onLogout: () -> Unit
) {
    // État pour la section courante
    var currentSection by remember { mutableStateOf("folders") }

    Column(modifier = Modifier.fillMaxSize()) {
        // En-tête
        Surface(
            color = MaterialTheme.colors.primary,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo temporaire
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", style = MaterialTheme.typography.subtitle1, color = MaterialTheme.colors.onPrimary)
                    }

                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Arka - Gestion des documents familiaux",
                        color = MaterialTheme.colors.onPrimary
                    )
                }
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text("Déconnexion")
                }
            }
        }

        // Contenu principal avec menu latéral et zone de contenu
        Row(modifier = Modifier.fillMaxSize()) {
            // Menu latéral
            Surface(
                color = MaterialTheme.colors.surface,
                modifier = Modifier.width(200.dp).fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Menu",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    MenuButton(
                        text = "Mes dossiers",
                        isSelected = currentSection == "folders",
                        onClick = { currentSection = "folders" }
                    )

                    MenuButton(
                        text = "Membres de la famille",
                        isSelected = currentSection == "family",
                        onClick = { currentSection = "family" }
                    )

                    MenuButton(
                        text = "Catégories",
                        isSelected = currentSection == "categories",
                        onClick = { currentSection = "categories" }
                    )

                    MenuButton(
                        text = "Paramètres",
                        isSelected = currentSection == "settings",
                        onClick = { currentSection = "settings" }
                    )
                }
            }

            // Zone de contenu principale
            Surface(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                when (currentSection) {
                    "folders" -> Text("Vue des dossiers - À implémenter")
                    "family" -> Text("Gestion des membres - À implémenter")
                    "categories" -> Text("Gestion des catégories - À implémenter")
                    "settings" -> Text("Paramètres - À implémenter")
                }
            }
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.surface
    }

    val textColor = if (isSelected) {
        Color.White // Force le texte en blanc quand le bouton est sélectionné
    } else {
        MaterialTheme.colors.onSurface
    }

    Surface(
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = backgroundColor,
                contentColor = textColor
            )
        ) {
            Text(
                text = text,
                color = textColor // Force la couleur directement sur le Text également
            )
        }
    }
}