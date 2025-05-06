package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import controllers.FamilyController
import controllers.FamilyMemberController
import ktorm.Family
import ktorm.FamilyMember

@Composable
fun FamilyScreen(
    currentUserId: Int = 1,
    familyController: FamilyController = org.koin.java.KoinJavaComponent.getKoin().get(),
    familyMemberController: FamilyMemberController = org.koin.java.KoinJavaComponent.getKoin().get()
) {
    // États
    var families by remember { mutableStateOf<List<Family>>(emptyList()) }
    var selectedFamily by remember { mutableStateOf<Family?>(null) }
    var familyMembers by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // État de l'utilisateur actuel
    var currentUser by remember { mutableStateOf<FamilyMember?>(null) }

    // Chargement de l'utilisateur actuel
    LaunchedEffect(currentUserId) {
        currentUser = familyMemberController.getFamilyMemberById(currentUserId)
    }

    // Permissions
    val isAdmin = currentUser?.isAdmin ?: false
    val isParent = currentUser?.isParent ?: false
    val userFamilyId = currentUser?.familyID

    // Chargement des familles
    LaunchedEffect(Unit) {
        try {
            // Si admin, charger toutes les familles
            // Sinon, charger uniquement la famille de l'utilisateur
            families = if (isAdmin) {
                familyController.getAllFamilies()
            } else {
                userFamilyId?.let { familyId ->
                    val family = familyController.getFamilyById(familyId)
                    if (family != null) listOf(family) else emptyList()
                } ?: emptyList()
            }

            if (families.isNotEmpty()) {
                selectedFamily = families.first()
            }
        } catch (e: Exception) {
            println("Erreur lors du chargement des familles: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Chargement des membres quand une famille est sélectionnée
    LaunchedEffect(selectedFamily) {
        if (selectedFamily != null) {
            val familyId = selectedFamily!!.familyID
            if (familyId != null) {
                familyMembers = familyMemberController.getFamilyMembersByFamilyId(familyId)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Gestion des familles",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold
            )
        }

        // Affichage du chargement ou contenu
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (families.isEmpty()) {
            // Message si aucune famille
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Aucune famille trouvée",
                        style = MaterialTheme.typography.h6,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Affichage basique des familles
            LazyColumn {
                items(families) { family ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Famille: ${family.familyLabel}",
                                style = MaterialTheme.typography.h6
                            )
                        }
                    }
                }
            }
        }
    }
}