package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import controllers.FamilyMemberController
import ktorm.FamilyMember
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FamilyMemberScreen(
    userId: Int,
    isAdmin: Boolean,
    familyMemberController: FamilyMemberController
) {
    // États
    var familyMembers by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var groupedMembers by remember { mutableStateOf<Map<String, List<FamilyMember>>>(emptyMap()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<FamilyMember?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // État de l'utilisateur actuel
    var currentUser by remember { mutableStateOf<FamilyMember?>(null) }

    // Chargement de l'utilisateur actuel
    LaunchedEffect(userId) {
        currentUser = familyMemberController.getFamilyMemberById(userId)
    }

    // Permissions
    val isParent = currentUser?.isParent ?: false
    val userFamilyId = currentUser?.familyID

    // Charger les membres
    LaunchedEffect(Unit) {
        try {
            // Si admin, charger tous les membres
            // Si parent ou enfant, charger uniquement les membres de la famille de l'utilisateur
            familyMembers = when {
                isAdmin -> familyMemberController.getAllFamilyMembers()
                userFamilyId != null -> familyMemberController.getFamilyMembersByFamilyId(userFamilyId)
                else -> currentUser?.let { listOf(it) } ?: emptyList()
            }

            // Trier les membres par prénom
            familyMembers = familyMembers.sortedBy { it.familyMemberFirstName }

            // Regrouper les membres par famille
            groupedMembers = if (isAdmin) {
                // Pour les admins: regrouper par ID de famille
                familyMembers.groupBy { member ->
                    val familyId = member.familyID
                    "Famille #$familyId"
                }
            } else {
                // Pour les parents et enfants: un seul groupe
                mapOf("Votre famille" to familyMembers)
            }

        } catch (e: Exception) {
            println("Erreur lors du chargement des membres: ${e.message}")
        } finally {
            isLoading = false
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
                "Membres de la famille",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold
            )

            // Le bouton d'ajout est visible uniquement pour les admins et les parents
            if (isAdmin || isParent) {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "+",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter un membre")
                }
            }
        }

        // Affichage du chargement ou contenu
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (familyMembers.isEmpty()) {
            // Message si aucun membre
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Aucun membre trouvé",
                        style = MaterialTheme.typography.h6,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isAdmin || isParent) {
                        Text(
                            "Ajoutez votre premier membre de famille pour commencer",
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            "Contactez un administrateur pour être ajouté à une famille",
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            // Liste des membres regroupés par famille
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pour chaque groupe de famille
                groupedMembers.forEach { (familyName, members) ->
                    item {
                        // En-tête du groupe de famille
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = familyName,
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Afficher les membres de cette famille
                    items(members) { member ->
                        MemberCard(
                            member = member,
                            isCurrentUser = member.familyMemberID == userId,
                            canEdit = isAdmin || (isParent && member.familyID == userFamilyId && !member.isAdmin),
                            canDelete = isAdmin || (isParent && member.familyID == userFamilyId && !member.isAdmin && member.familyMemberID != userId),
                            onEdit = {
                                selectedMember = member
                                showEditDialog = true
                            },
                            onDelete = {
                                // Afficher une confirmation de suppression
                                // Pour l'instant, suppression directe
                                familyMemberController.deleteFamilyMember(member.familyMemberID!!)
                                // Recharger la liste des membres
                                familyMembers = when {
                                    isAdmin -> familyMemberController.getAllFamilyMembers()
                                    userFamilyId != null -> familyMemberController.getFamilyMembersByFamilyId(userFamilyId)
                                    else -> currentUser?.let { listOf(it) } ?: emptyList()
                                }
                            }
                        )
                    }

                    // Ajouter un espace entre les groupes
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Dialogue d'ajout
    if (showAddDialog) {
        MemberDialog(
            member = null,
            canEditAdmin = isAdmin, // Seul l'admin peut changer le statut admin
            onDismiss = { showAddDialog = false },
            onSave = { firstName, email, birthDate, gender, isParentRole, isAdminRole ->
                val familyId = if (isAdmin) {
                    // Les admins peuvent sélectionner une famille, ici on prend celle par défaut
                    // En pratique, il faudrait ajouter un sélecteur de famille
                    userFamilyId ?: 1
                } else {
                    // Les parents ne peuvent ajouter que dans leur propre famille
                    userFamilyId ?: 1
                }

                val newMember = FamilyMember(
                    familyMemberFirstName = firstName,
                    familyMemberMail = email,
                    familyMemberPassword = "password123", // Ajout d'un mot de passe par défaut
                    familyMemberBirthDate = birthDate,
                    familyMemberGender = gender,
                    isParent = isParentRole,
                    isAdmin = isAdminRole,
                    familyID = familyId
                )

                val addedMember = familyMemberController.addFamilyMember(newMember)
                if (addedMember != null) {
                    // Recharger la liste des membres
                    familyMembers = when {
                        isAdmin -> familyMemberController.getAllFamilyMembers()
                        userFamilyId != null -> familyMemberController.getFamilyMembersByFamilyId(userFamilyId)
                        else -> currentUser?.let { listOf(it) } ?: emptyList()
                    }

                    // Mettre à jour le groupement
                    familyMembers = familyMembers.sortedBy { it.familyMemberFirstName }
                    groupedMembers = if (isAdmin) {
                        familyMembers.groupBy { member ->
                            val familyId = member.familyID
                            "Famille #$familyId"
                        }
                    } else {
                        mapOf("Votre famille" to familyMembers)
                    }
                }

                showAddDialog = false
            }
        )
    }

    // Dialogue d'édition
    if (showEditDialog && selectedMember != null) {
        MemberDialog(
            member = selectedMember,
            canEditAdmin = isAdmin, // Seul l'admin peut changer le statut admin
            onDismiss = { showEditDialog = false },
            onSave = { firstName, email, birthDate, gender, isParentRole, isAdminRole ->
                val updatedMember = selectedMember!!.copy(
                    familyMemberFirstName = firstName,
                    familyMemberMail = email,
                    familyMemberBirthDate = birthDate,
                    familyMemberGender = gender,
                    isParent = isParentRole,
                    isAdmin = if (isAdmin) isAdminRole else selectedMember!!.isAdmin // Préserver le statut admin si non-admin
                )

                val success = familyMemberController.updateFamilyMember(updatedMember)
                if (success) {
                    // Recharger la liste des membres
                    familyMembers = when {
                        isAdmin -> familyMemberController.getAllFamilyMembers()
                        userFamilyId != null -> familyMemberController.getFamilyMembersByFamilyId(userFamilyId)
                        else -> currentUser?.let { listOf(it) } ?: emptyList()
                    }

                    // Mettre à jour le groupement
                    familyMembers = familyMembers.sortedBy { it.familyMemberFirstName }
                    groupedMembers = if (isAdmin) {
                        familyMembers.groupBy { member ->
                            val familyId = member.familyID
                            "Famille #$familyId"
                        }
                    } else {
                        mapOf("Votre famille" to familyMembers)
                    }
                }

                showEditDialog = false
                selectedMember = null
            }
        )
    }
}

@Composable
fun MemberCard(
    member: FamilyMember,
    isCurrentUser: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val genderColor = if (member.familyMemberGender.equals("M", ignoreCase = true)) {
        Color(0xFF2196F3) // Bleu
    } else {
        Color(0xFFF06292) // Rose
    }

    Card(
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar/Initiale
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(genderColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.familyMemberFirstName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Informations
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.familyMemberFirstName,
                        style = MaterialTheme.typography.h6
                    )

                    // Indicateur utilisateur actuel
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(Vous)",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = member.familyMemberMail,
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Âge
                    Text(
                        text = "Né(e) le ${member.familyMemberBirthDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                        style = MaterialTheme.typography.caption
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Badge parent
                    if (member.isParent) {
                        Badge(backgroundColor = MaterialTheme.colors.primary) {
                            Text(
                                "Parent",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    // Badge admin
                    if (member.isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(backgroundColor = Color(0xFFF57F17)) {
                            Text(
                                "Admin",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            // Actions (conditionnelles selon les permissions)
            if (canEdit || canDelete) {
                Column {
                    if (canEdit) {
                        Button(
                            onClick = onEdit,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.primary
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text("Modifier")
                        }
                    }

                    if (canDelete) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Supprimer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberDialog(
    member: FamilyMember?,
    canEditAdmin: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate, String, Boolean, Boolean) -> Unit
) {
    var firstName by remember { mutableStateOf(member?.familyMemberFirstName ?: "") }
    var email by remember { mutableStateOf(member?.familyMemberMail ?: "") }
    var birthDateStr by remember { mutableStateOf(
        member?.familyMemberBirthDate?.format(DateTimeFormatter.ISO_DATE) ?: LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    ) }
    var gender by remember { mutableStateOf(member?.familyMemberGender ?: "M") }
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
                    label = { Text("Date de naissance (AAAA-MM-JJ)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                // Sélection du genre
                Text("Genre:", modifier = Modifier.padding(bottom = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = gender == "M",
                        onClick = { gender = "M" }
                    )
                    Text("Masculin", modifier = Modifier.clickable { gender = "M" })

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = gender == "F",
                        onClick = { gender = "F" }
                    )
                    Text("Féminin", modifier = Modifier.clickable { gender = "F" })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Case à cocher pour parent
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isParent = !isParent }
                ) {
                    Checkbox(
                        checked = isParent,
                        onCheckedChange = { isParent = it }
                    )
                    Text("Est un parent")
                }

                // Case à cocher pour admin (visible uniquement pour les admins)
                if (canEditAdmin) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isAdmin = !isAdmin }
                    ) {
                        Checkbox(
                            checked = isAdmin,
                            onCheckedChange = { isAdmin = it }
                        )
                        Text("Est un administrateur")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val birthDate = LocalDate.parse(birthDateStr)
                        onSave(firstName, email, birthDate, gender, isParent, isAdmin)
                    } catch (e: Exception) {
                        // Gérer l'erreur de format de date
                        println("Erreur de format de date: ${e.message}")
                    }
                },
                enabled = firstName.isNotBlank() && email.isNotBlank()
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
fun Badge(
    backgroundColor: Color = MaterialTheme.colors.primary,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}