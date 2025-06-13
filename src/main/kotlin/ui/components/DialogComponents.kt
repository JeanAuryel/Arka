// ================================================================
// DIALOGCOMPONENTS.KT - DIALOGS ARKA
// ================================================================

package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ui.theme.*

/**
 * DIALOGS DE CONFIRMATION
 */

/**
 * Dialog de confirmation générique pour Arka
 */
@Composable
fun ArkaConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirmer",
    dismissText: String = "Annuler",
    icon: ImageVector? = Icons.Default.HelpOutline,
    isDestructive: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        ArkaCard(
            modifier = modifier.width(400.dp),
            shape = ArkaComponentShapes.dialogMedium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // En-tête avec icône et titre
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (isDestructive) MaterialTheme.colors.error else MaterialTheme.colors.primary
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.onSurface
                    )
                }

                // Message
                Text(
                    text = message,
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    lineHeight = ArkaTextStyles.cardDescription.lineHeight
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ArkaOutlinedButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    ArkaButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = if (isDestructive) {
                            ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.error,
                                contentColor = MaterialTheme.colors.onError
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Dialog de suppression avec confirmation
 */
@Composable
fun ArkaDeleteConfirmationDialog(
    itemName: String,
    itemType: String = "élément",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    additionalInfo: String? = null
) {
    ArkaConfirmationDialog(
        title = "Supprimer $itemType",
        message = buildString {
            append("Êtes-vous sûr de vouloir supprimer \"$itemName\" ?")
            if (additionalInfo != null) {
                append("\n\n$additionalInfo")
            }
            append("\n\nCette action est irréversible.")
        },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmText = "Supprimer",
        dismissText = "Annuler",
        icon = Icons.Default.Delete,
        isDestructive = true
    )
}

/**
 * DIALOGS D'INFORMATION
 */

/**
 * Dialog d'information générique
 */
@Composable
fun ArkaInfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    buttonText: String = "Compris"
) {
    Dialog(onDismissRequest = onDismiss) {
        ArkaCard(
            modifier = modifier.width(380.dp),
            shape = ArkaComponentShapes.dialogMedium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // En-tête
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colors.primary
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Message
                Text(
                    text = message,
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )

                // Bouton
                ArkaButton(
                    text = buttonText,
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

/**
 * DIALOGS D'ERREUR
 */

/**
 * Dialog d'erreur avec possibilité de retry
 */
@Composable
fun ArkaErrorDialog(
    title: String = "Erreur",
    message: String,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        ArkaCard(
            modifier = modifier.width(400.dp),
            shape = ArkaComponentShapes.dialogMedium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // En-tête d'erreur
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colors.error
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colors.error
                    )
                }

                // Message d'erreur
                Text(
                    text = message,
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (onRetry != null) {
                        Arrangement.spacedBy(12.dp)
                    } else {
                        Arrangement.End
                    }
                ) {
                    if (onRetry != null) {
                        ArkaOutlinedButton(
                            text = "Fermer",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )

                        ArkaButton(
                            text = "Réessayer",
                            onClick = {
                                onRetry()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Refresh
                        )
                    } else {
                        ArkaButton(
                            text = "Fermer",
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

/**
 * DIALOGS DE PROGRESSION
 */

/**
 * Dialog de progression avec barre de progression
 */
@Composable
fun ArkaProgressDialog(
    title: String,
    message: String? = null,
    progress: Float? = null, // null pour indéterminé
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { onCancel?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onCancel != null,
            dismissOnClickOutside = false
        )
    ) {
        ArkaCard(
            modifier = modifier.width(350.dp),
            shape = ArkaComponentShapes.dialogMedium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Titre
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )

                // Message
                message?.let {
                    Text(
                        text = it,
                        style = ArkaTextStyles.cardDescription,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }

                // Barre de progression
                if (progress != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = ArkaTextStyles.metadata,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colors.primary
                    )
                }

                // Bouton d'annulation
                onCancel?.let {
                    ArkaOutlinedButton(
                        text = "Annuler",
                        onClick = it,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

/**
 * DIALOGS SPÉCIALISÉS ARKA
 */

/**
 * Dialog de sélection de membres de famille
 */
@Composable
fun ArkaFamilyMemberSelectionDialog(
    title: String = "Sélectionner un membre",
    members: List<Pair<String, String>>, // (id, name)
    selectedMembers: Set<String> = emptySet(),
    multiSelect: Boolean = false,
    onSelectionChange: (Set<String>) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentSelection by remember { mutableStateOf(selectedMembers) }

    Dialog(onDismissRequest = onDismiss) {
        ArkaCard(
            modifier = Modifier.width(400.dp),
            shape = ArkaComponentShapes.dialogMedium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Titre
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold
                )

                // Liste des membres
                Column(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (multiSelect) {
                                Checkbox(
                                    checked = currentSelection.contains(id),
                                    onCheckedChange = { checked ->
                                        currentSelection = if (checked) {
                                            currentSelection + id
                                        } else {
                                            currentSelection - id
                                        }
                                        onSelectionChange(currentSelection)
                                    }
                                )
                            } else {
                                RadioButton(
                                    selected = currentSelection.contains(id),
                                    onClick = {
                                        currentSelection = setOf(id)
                                        onSelectionChange(currentSelection)
                                    }
                                )
                            }

                            UserAvatar(
                                name = name,
                                size = AvatarSizes.small
                            )

                            Text(
                                text = name,
                                style = ArkaTextStyles.cardTitle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ArkaOutlinedButton(
                        text = "Annuler",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    ArkaButton(
                        text = "Confirmer",
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = currentSelection.isNotEmpty()
                    )
                }
            }
        }
    }
}

/**
 * Dialog de partage de fichier/dossier
 */
@Composable
fun ArkaShareDialog(
    itemName: String,
    itemType: String = "fichier",
    onShare: (permissions: Set<String>, members: Set<String>) -> Unit,
    onDismiss: () -> Unit,
    availableMembers: List<Pair<String, String>> = emptyList()
) {
    var selectedPermissions by remember { mutableStateOf(setOf("READ")) }
    var selectedMembers by remember { mutableStateOf<Set<String>>(emptySet()) }

    val permissionOptions = listOf(
        "READ" to "Lecture seule",
        "WRITE" to "Lecture et écriture",
        "DELETE" to "Tous les droits"
    )

    Dialog(onDismissRequest = onDismiss) {
        ArkaCard(
            modifier = Modifier.width(450.dp),
            shape = ArkaComponentShapes.dialogMedium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // En-tête
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colors.primary
                    )

                    Text(
                        text = "Partager $itemType",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Nom: $itemName",
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )

                // Sélection des permissions
                Text(
                    text = "Permissions",
                    style = ArkaTextStyles.formLabel,
                    fontWeight = FontWeight.Medium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    permissionOptions.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedPermissions.contains(key),
                                onClick = { selectedPermissions = setOf(key) }
                            )
                            Text(
                                text = label,
                                style = ArkaTextStyles.cardDescription
                            )
                        }
                    }
                }

                // Sélection des membres
                if (availableMembers.isNotEmpty()) {
                    Text(
                        text = "Partager avec",
                        style = ArkaTextStyles.formLabel,
                        fontWeight = FontWeight.Medium
                    )

                    Column(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableMembers.forEach { (id, name) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(
                                    checked = selectedMembers.contains(id),
                                    onCheckedChange = { checked ->
                                        selectedMembers = if (checked) {
                                            selectedMembers + id
                                        } else {
                                            selectedMembers - id
                                        }
                                    }
                                )

                                UserAvatar(
                                    name = name,
                                    size = AvatarSizes.small
                                )

                                Text(
                                    text = name,
                                    style = ArkaTextStyles.cardDescription
                                )
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ArkaOutlinedButton(
                        text = "Annuler",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    ArkaButton(
                        text = "Partager",
                        onClick = {
                            onShare(selectedPermissions, selectedMembers)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedMembers.isNotEmpty(),
                        icon = Icons.Default.Share
                    )
                }
            }
        }
    }
}

/**
 * Dialog de succès avec animation
 */
@Composable
fun ArkaSuccessDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    autoHideDelay: Long = 3000L
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(autoHideDelay)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        ArkaCard(
            modifier = Modifier.width(350.dp),
            shape = ArkaComponentShapes.dialogMedium,
            elevation = 8.dp,
            backgroundColor = MaterialTheme.colors.arka.success.copy(alpha = 0.1f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colors.arka.success
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colors.arka.success,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = ArkaTextStyles.cardDescription,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}