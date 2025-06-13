// ================================================================
// FORMCOMPONENTS.KT - COMPOSANTS DE FORMULAIRE ARKA
// ================================================================

package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * CHAMPS DE RECHERCHE
 */

/**
 * Champ de recherche avec icône et suggestions
 */
@Composable
fun ArkaSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Rechercher...",
    suggestions: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    enabled: Boolean = true,
    maxSuggestions: Int = 5
) {
    var isFocused by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Champ de recherche principal
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                showSuggestions = it.isNotEmpty() && suggestions.isNotEmpty()
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    showSuggestions = focusState.isFocused && value.isNotEmpty() && suggestions.isNotEmpty()
                },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = if (value.isNotEmpty()) {
                {
                    Row {
                        if (value.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onValueChange("")
                                    showSuggestions = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { onSearch(value) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Rechercher",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else null,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(value) }),
            shape = ArkaComponentShapes.textFieldOutlined,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.23f)
            )
        )

        // Suggestions
        if (showSuggestions && isFocused) {
            ArkaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                elevation = 8.dp,
                shape = ArkaComponentShapes.containerMedium
            ) {
                LazyColumn {
                    items(suggestions.take(maxSuggestions)) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSuggestionClick(suggestion)
                                    onValueChange(suggestion)
                                    showSuggestions = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )

                            Text(
                                text = suggestion,
                                style = ArkaTextStyles.cardDescription,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * SÉLECTEURS DE DATE
 */

/**
 * Sélecteur de date simple pour Arka
 */
@Composable
fun ArkaDatePicker(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Date",
    enabled: Boolean = true,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Champ d'affichage de la date
        OutlinedTextField(
            value = selectedDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
            onValueChange = { },
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { showDatePicker = true },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Sélectionner une date"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { showDatePicker = true },
            shape = ArkaComponentShapes.textFieldOutlined,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.23f)
            )
        )
    }

    // Dialog de sélection de date simple
    if (showDatePicker) {
        ArkaSimpleDatePickerDialog(
            initialDate = selectedDate ?: LocalDate.now(),
            onDateSelected = { date ->
                onDateSelected(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            minDate = minDate,
            maxDate = maxDate
        )
    }
}

/**
 * Dialog de sélection de date simple
 */
@Composable
private fun ArkaSimpleDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var currentMonth by remember { mutableStateOf(initialDate.month) }
    var currentYear by remember { mutableStateOf(initialDate.year) }

    ArkaConfirmationDialog(
        title = "Sélectionner une date",
        message = "Date sélectionnée: ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}",
        onConfirm = { onDateSelected(selectedDate) },
        onDismiss = onDismiss,
        confirmText = "Valider",
        dismissText = "Annuler",
        icon = Icons.Default.CalendarToday
    )
}

/**
 * SÉLECTEURS DE FICHIERS
 */

/**
 * Champ de sélection de fichier
 */
@Composable
fun ArkaFilePickerField(
    selectedFile: String?,
    onFileSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Fichier",
    placeholder: String = "Aucun fichier sélectionné",
    allowedExtensions: List<String> = emptyList(),
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedFile ?: "",
            onValueChange = { },
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                ArkaIconButton(
                    icon = Icons.Default.FolderOpen,
                    onClick = {
                        // TODO: Implémenter file picker natif
                        // Pour l'instant, placeholder
                        onFileSelected("exemple_fichier.pdf")
                    },
                    enabled = enabled,
                    contentDescription = "Parcourir"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = ArkaComponentShapes.textFieldOutlined,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.23f)
            )
        )

        // Afficher les extensions autorisées
        if (allowedExtensions.isNotEmpty()) {
            Text(
                text = "Extensions autorisées: ${allowedExtensions.joinToString(", ")}",
                style = ArkaTextStyles.helpText,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * SÉLECTEURS MULTIPLES
 */

/**
 * Sélecteur multiple avec chips
 */
@Composable
fun ArkaMultiSelectField(
    label: String,
    options: List<Pair<String, String>>, // (value, label)
    selectedValues: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxSelections: Int? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        Text(
            text = label,
            style = ArkaTextStyles.formLabel,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )

        // Champ de déclenchement
        OutlinedTextField(
            value = if (selectedValues.isEmpty()) {
                "Aucune sélection"
            } else {
                "${selectedValues.size} élément(s) sélectionné(s)"
            },
            onValueChange = { },
            readOnly = true,
            enabled = enabled,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Fermer" else "Ouvrir"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = ArkaComponentShapes.textFieldOutlined
        )

        // Liste déroulante
        if (expanded) {
            ArkaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                elevation = 4.dp
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp)
                ) {
                    items(options) { (value, optionLabel) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSelection = if (selectedValues.contains(value)) {
                                        selectedValues - value
                                    } else {
                                        if (maxSelections == null || selectedValues.size < maxSelections) {
                                            selectedValues + value
                                        } else {
                                            selectedValues
                                        }
                                    }
                                    onSelectionChange(newSelection)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = selectedValues.contains(value),
                                onCheckedChange = null // Géré par le clic sur la Row
                            )

                            Text(
                                text = optionLabel,
                                style = ArkaTextStyles.cardDescription,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Chips des éléments sélectionnés
        if (selectedValues.isNotEmpty()) {
            ArkaFlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedValues.forEach { value ->
                    val optionLabel = options.find { it.first == value }?.second ?: value
                    ArkaChip(
                        text = optionLabel,
                        onRemove = {
                            onSelectionChange(selectedValues - value)
                        }
                    )
                }
            }
        }

        // Information sur les limites
        maxSelections?.let { max ->
            Text(
                text = "${selectedValues.size}/$max sélection(s)",
                style = ArkaTextStyles.helpText,
                color = if (selectedValues.size >= max) {
                    MaterialTheme.colors.arka.warning
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    }
}

/**
 * Chip avec bouton de suppression
 */
@Composable
private fun ArkaChip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colors.primary.copy(alpha = 0.12f),
        shape = ArkaComponentShapes.chipMedium,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colors.primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = ArkaTextStyles.metadata,
                color = MaterialTheme.colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Supprimer",
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() },
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

/**
 * COMPOSANTS SPÉCIALISÉS ARKA
 */

/**
 * Sélecteur de permissions Arka
 */
@Composable
fun ArkaPermissionSelector(
    selectedPermissions: Set<String>,
    onPermissionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false
) {
    val permissions = listOf(
        "READ" to "Lecture seule",
        "WRITE" to "Lecture et écriture",
        "DELETE" to "Suppression",
        "ADMIN" to "Administration complète"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Permissions",
            style = ArkaTextStyles.formLabel,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )

        permissions.forEach { (key, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (multiSelect) {
                    Checkbox(
                        checked = selectedPermissions.contains(key),
                        onCheckedChange = { checked ->
                            onPermissionChange(
                                if (checked) {
                                    selectedPermissions + key
                                } else {
                                    selectedPermissions - key
                                }
                            )
                        }
                    )
                } else {
                    RadioButton(
                        selected = selectedPermissions.contains(key),
                        onClick = { onPermissionChange(setOf(key)) }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = ArkaTextStyles.cardDescription,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )

                    Text(
                        text = getPermissionDescription(key),
                        style = ArkaTextStyles.helpText,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Flow layout simple pour les chips
 */
@Composable
private fun ArkaFlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Implémentation simplifiée - dans un vrai projet, utilisez FlowRow de Compose
    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

/**
 * Utilitaires
 */
private fun getPermissionDescription(permission: String): String {
    return when (permission) {
        "READ" -> "Peut voir et télécharger le contenu"
        "WRITE" -> "Peut modifier et ajouter du contenu"
        "DELETE" -> "Peut supprimer le contenu"
        "ADMIN" -> "Contrôle total sur le contenu et ses permissions"
        else -> ""
    }
}