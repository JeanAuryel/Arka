// ================================================================
// REQUESTPERMISSIONSCREEN.KT - FORMULAIRE DE DEMANDE DE PERMISSIONS
// ================================================================

package ui.screens.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import controllers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.components.*
import ui.theme.*
import java.time.LocalDateTime

/**
 * Écran de demande de permissions
 *
 * Fonctionnalités:
 * - Formulaire de demande d'accès à un espace
 * - Sélection du type de permission souhaité
 * - Justification de la demande
 * - Suivi des demandes en cours
 * - Historique des demandes
 */
@Composable
fun RequestPermissionScreen(
    delegationController: DelegationRequestController,
    familyController: FamilyController,
    authController: AuthController,
    targetSpaceId: Int? = null,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser = authController.getCurrentUser()

    // États du formulaire
    var selectedSpaceId by remember { mutableStateOf(targetSpaceId) }
    var selectedPermissionType by remember { mutableStateOf(TypePermission.LECTURE) }
    var justification by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submissionResult by remember { mutableStateOf<String?>(null) }

    // États de données
    var availableSpaces by remember { mutableStateOf<List<Espace>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<DemandeDelegation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Charger les données initiales
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            scope.launch {
                isLoading = true
                try {
                    // Charger les espaces disponibles (où l'utilisateur n'a pas encore de permissions)
                    val spacesResult = familyController.getAvailableSpacesForMember(currentUser.id)
                    if (spacesResult is FamilyController.FamilyResult.Success) {
                        availableSpaces = spacesResult.data
                    }

                    // Charger les demandes en cours
                    val requestsResult = delegationController.getPendingRequestsByMember(currentUser.id)
                    if (requestsResult is DelegationRequestController.DelegationResult.Success) {
                        pendingRequests = requestsResult.data
                    }

                } catch (e: Exception) {
                    errorMessage = "Erreur lors du chargement: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Demander une permission",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bouton historique
            IconButton(onClick = onNavigateToHistory) {
                Icon(Icons.Default.History, contentDescription = "Historique")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Formulaire de demande
                item {
                    RequestForm(
                        availableSpaces = availableSpaces,
                        selectedSpaceId = selectedSpaceId,
                        selectedPermissionType = selectedPermissionType,
                        justification = justification,
                        isSubmitting = isSubmitting,
                        onSpaceSelected = { selectedSpaceId = it },
                        onPermissionTypeSelected = { selectedPermissionType = it },
                        onJustificationChanged = { justification = it },
                        onSubmit = {
                            if (selectedSpaceId != null && currentUser != null) {
                                scope.launch {
                                    isSubmitting = true
                                    try {
                                        val request = DemandeDelegation(
                                            id = 0, // Auto-généré
                                            membreFamilleId = currentUser.id,
                                            espaceId = selectedSpaceId!!,
                                            typePermissionDemandee = selectedPermissionType,
                                            justification = justification,
                                            statut = StatutDemande.EN_ATTENTE,
                                            dateCreation = LocalDateTime.now(),
                                            dateReponse = null,
                                            membreFamilleIdValidateur = null
                                        )

                                        val result = delegationController.createDelegationRequest(request)

                                        when (result) {
                                            is DelegationRequestController.DelegationResult.Success -> {
                                                submissionResult = "Demande envoyée avec succès !"
                                                // Réinitialiser le formulaire
                                                selectedSpaceId = null
                                                selectedPermissionType = TypePermission.LECTURE
                                                justification = ""
                                                // Recharger les demandes en cours
                                                val updatedRequests = delegationController.getPendingRequestsByMember(currentUser.id)
                                                if (updatedRequests is DelegationRequestController.DelegationResult.Success) {
                                                    pendingRequests = updatedRequests.data
                                                }
                                            }
                                            is DelegationRequestController.DelegationResult.Error -> {
                                                submissionResult = "Erreur: ${result.message}"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        submissionResult = "Erreur lors de l'envoi: ${e.message}"
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            }
                        }
                    )
                }

                // Demandes en cours
                item {
                    PendingRequestsSection(
                        requests = pendingRequests,
                        availableSpaces = availableSpaces,
                        onCancelRequest = { request ->
                            scope.launch {
                                val result = delegationController.cancelDelegationRequest(request.id)
                                if (result is DelegationRequestController.DelegationResult.Success) {
                                    pendingRequests = pendingRequests.filter { it.id != request.id }
                                }
                            }
                        }
                    )
                }
            }
        }

        // Affichage du résultat de soumission
        submissionResult?.let { result ->
            LaunchedEffect(result) {
                // Afficher pendant 3 secondes puis effacer
                delay(3000)
                submissionResult = null
            }

            Snackbar(
                modifier = Modifier.padding(16.dp),
                backgroundColor = if (result.startsWith("Erreur")) {
                    Color.Red.copy(alpha = 0.8f)
                } else {
                    Color.Green.copy(alpha = 0.8f)
                }
            ) {
                Text(
                    text = result,
                    color = Color.White
                )
            }
        }

        // Gestion des erreurs
        errorMessage?.let { error ->
            LaunchedEffect(error) {
                delay(5000)
                errorMessage = null
            }
        }
    }
}

/**
 * Formulaire de demande de permission
 */
@Composable
private fun RequestForm(
    availableSpaces: List<Espace>,
    selectedSpaceId: Int?,
    selectedPermissionType: TypePermission,
    justification: String,
    isSubmitting: Boolean,
    onSpaceSelected: (Int) -> Unit,
    onPermissionTypeSelected: (TypePermission) -> Unit,
    onJustificationChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Nouvelle demande",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sélection de l'espace
            Text(
                text = "Espace demandé *",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (availableSpaces.isEmpty()) {
                Text(
                    text = "Aucun espace disponible pour demander des permissions",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            } else {
                availableSpaces.forEach { space ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedSpaceId == space.id,
                                onClick = { onSpaceSelected(space.id) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSpaceId == space.id,
                            onClick = { onSpaceSelected(space.id) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = space.nom,
                                style = MaterialTheme.typography.body1,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = space.description ?: "Aucune description",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sélection du type de permission
            Text(
                text = "Type de permission souhaité *",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            TypePermission.values().forEach { permissionType ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedPermissionType == permissionType,
                            onClick = { onPermissionTypeSelected(permissionType) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedPermissionType == permissionType,
                        onClick = { onPermissionTypeSelected(permissionType) }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = getPermissionLabel(permissionType),
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.Medium,
                            color = getPermissionColor(permissionType)
                        )
                        Text(
                            text = getPermissionDescription(permissionType),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Justification
            Text(
                text = "Justification *",
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = justification,
                onValueChange = onJustificationChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Expliquez pourquoi vous avez besoin de cette permission...") },
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton de soumission
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && selectedSpaceId != null && justification.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Envoi en cours...")
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Envoyer")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Envoyer la demande")
                }
            }
        }
    }
}

/**
 * Section des demandes en cours
 */
@Composable
private fun PendingRequestsSection(
    requests: List<DemandeDelegation>,
    availableSpaces: List<Espace>,
    onCancelRequest: (DemandeDelegation) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, contentDescription = "En attente")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Demandes en cours (${requests.size})",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (requests.isEmpty()) {
                Text(
                    text = "Aucune demande en cours",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            } else {
                requests.forEach { request ->
                    val space = availableSpaces.find { it.id == request.espaceId }

                    PendingRequestItem(
                        request = request,
                        spaceName = space?.nom ?: "Espace inconnu",
                        onCancel = { onCancelRequest(request) }
                    )

                    if (request != requests.last()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Item de demande en cours
 */
@Composable
private fun PendingRequestItem(
    request: DemandeDelegation,
    spaceName: String,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = spaceName,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "Permission: ${getPermissionLabel(request.typePermissionDemandee)}",
                    style = MaterialTheme.typography.body2,
                    color = getPermissionColor(request.typePermissionDemandee)
                )

                Text(
                    text = "Demandé le ${request.dateCreation}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            // Statut
            Chip(
                backgroundColor = when (request.statut) {
                    StatutDemande.EN_ATTENTE -> Color.Orange.copy(alpha = 0.2f)
                    StatutDemande.APPROUVEE -> Color.Green.copy(alpha = 0.2f)
                    StatutDemande.REFUSEE -> Color.Red.copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = when (request.statut) {
                        StatutDemande.EN_ATTENTE -> "En attente"
                        StatutDemande.APPROUVEE -> "Approuvée"
                        StatutDemande.REFUSEE -> "Refusée"
                    },
                    style = MaterialTheme.typography.caption,
                    color = when (request.statut) {
                        StatutDemande.EN_ATTENTE -> Color.Orange
                        StatutDemande.APPROUVEE -> Color.Green
                        StatutDemande.REFUSEE -> Color.Red
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bouton d'annulation (seulement si en attente)
            if (request.statut == StatutDemande.EN_ATTENTE) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Annuler",
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Justification
        if (request.justification.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"${request.justification}\"",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

// Fonctions utilitaires
private fun getPermissionLabel(type: TypePermission): String = when (type) {
    TypePermission.LECTURE -> "Lecture seule"
    TypePermission.ECRITURE -> "Lecture/Écriture"
    TypePermission.ADMIN -> "Administration"
}

private fun getPermissionDescription(type: TypePermission): String = when (type) {
    TypePermission.LECTURE -> "Consulter les fichiers et dossiers"
    TypePermission.ECRITURE -> "Consulter, ajouter et modifier des fichiers"
    TypePermission.ADMIN -> "Contrôle total sur l'espace"
}

private fun getPermissionColor(type: TypePermission): Color = when (type) {
    TypePermission.LECTURE -> Color(0xFF4CAF50)
    TypePermission.ECRITURE -> Color(0xFFF57C00)
    TypePermission.ADMIN -> Color(0xFFF44336)
}