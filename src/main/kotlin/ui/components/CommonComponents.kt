// ================================================================
// COMMONCOMPONENTS.KT - COMPOSANTS DE BASE ARKA
// ================================================================

package ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.*

/**
 * BOUTONS PERSONNALISÉS ARKA
 */

/**
 * Bouton principal d'Arka avec style uniforme
 */
@Composable
fun ArkaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    shape: androidx.compose.ui.graphics.Shape = ArkaComponentShapes.buttonMedium
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled && !loading,
        colors = colors,
        shape = shape,
        elevation = ButtonDefaults.elevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
            disabledElevation = 0.dp
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colors.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = text,
                    style = ArkaTextStyles.navigation
                )
            }
        }
    }
}

/**
 * Bouton secondaire d'Arka
 */
@Composable
fun ArkaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled && !loading,
        shape = ArkaComponentShapes.buttonMedium,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colors.primary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = text,
                    style = ArkaTextStyles.navigation
                )
            }
        }
    }
}

/**
 * Bouton icône d'Arka
 */
@Composable
fun ArkaIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colors.onSurface
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.38f)
        )
    }
}

/**
 * CARTES PERSONNALISÉES ARKA
 */

/**
 * Carte principale d'Arka avec style uniforme
 */
@Composable
fun ArkaCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 4.dp,
    shape: androidx.compose.ui.graphics.Shape = ArkaComponentShapes.cardMedium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier.clickable { onClick() },
            elevation = elevation,
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            border = border
        ) {
            Column(content = content)
        }
    } else {
        Card(
            modifier = modifier,
            elevation = elevation,
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            border = border
        ) {
            Column(content = content)
        }
    }
}

/**
 * Carte de statistique
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trend: TrendDirection? = null,
    trendValue: String? = null,
    color: Color = MaterialTheme.colors.primary,
    onClick: (() -> Unit)? = null
) {
    ArkaCard(
        modifier = modifier.aspectRatio(1.2f),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = ArkaTextStyles.metadata,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = value,
                        style = ArkaTextStyles.bigNumber,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = color.copy(alpha = 0.8f)
                )
            }

            // Sous-titre et trend
            if (subtitle != null || (trend != null && trendValue != null)) {
                Column {
                    subtitle?.let {
                        Text(
                            text = it,
                            style = ArkaTextStyles.helpText,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (trend != null && trendValue != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (trend) {
                                    TrendDirection.UP -> Icons.Default.TrendingUp
                                    TrendDirection.DOWN -> Icons.Default.TrendingDown
                                    TrendDirection.STABLE -> Icons.Default.TrendingFlat
                                },
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = when (trend) {
                                    TrendDirection.UP -> Color(0xFF4CAF50)
                                    TrendDirection.DOWN -> Color(0xFFF44336)
                                    TrendDirection.STABLE -> Color(0xFF9E9E9E)
                                }
                            )

                            Text(
                                text = trendValue,
                                style = ArkaTextStyles.helpText,
                                color = when (trend) {
                                    TrendDirection.UP -> Color(0xFF4CAF50)
                                    TrendDirection.DOWN -> Color(0xFFF44336)
                                    TrendDirection.STABLE -> Color(0xFF9E9E9E)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CHAMPS DE SAISIE PERSONNALISÉS
 */

/**
 * Champ de texte d'Arka avec style uniforme
 */
@Composable
fun ArkaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label?.let { { Text(text = it) } },
            placeholder = placeholder?.let { { Text(text = it) } },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Masquer mot de passe" else "Afficher mot de passe"
                        )
                    }
                }
            } else trailingIcon,
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = ArkaComponentShapes.textFieldOutlined,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                errorBorderColor = MaterialTheme.colors.error
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Message d'erreur
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = ArkaTextStyles.helpText,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * BADGES ET INDICATEURS
 */

/**
 * Badge de statut
 */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primary,
    textColor: Color = MaterialTheme.colors.onPrimary
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = ArkaComponentShapes.buttonRound,
        contentColor = textColor
    ) {
        Text(
            text = text,
            style = ArkaTextStyles.badge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Indicateur de notification avec compteur
 */
@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99
) {
    if (count > 0) {
        Surface(
            modifier = modifier.size(20.dp),
            color = MaterialTheme.colors.error,
            shape = CircleShape
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = if (count > maxCount) "$maxCount+" else count.toString(),
                    style = ArkaTextStyles.badge,
                    color = MaterialTheme.colors.onError,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * DIVIDERS ET SÉPARATEURS
 */

/**
 * Séparateur personnalisé Arka
 */
@Composable
fun ArkaDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
    thickness: Dp = 1.dp,
    startIndent: Dp = 0.dp
) {
    Divider(
        modifier = modifier,
        color = color,
        thickness = thickness,
        startIndent = startIndent
    )
}

/**
 * UTILITAIRES ET ÉNUMÉRATIONS
 */

/**
 * Direction de tendance pour les statistiques
 */
enum class TrendDirection {
    UP, DOWN, STABLE
}

/**
 * Types d'alertes
 */
enum class AlertType {
    SUCCESS, WARNING, ERROR, INFO
}

/**
 * AVATAR ET IMAGES
 */

/**
 * Avatar d'utilisateur avec initiales
 */
@Composable
fun UserAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    backgroundColor: Color = MaterialTheme.colors.primary,
    textColor: Color = MaterialTheme.colors.onPrimary
) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    Surface(
        modifier = modifier.size(size),
        color = backgroundColor,
        shape = CircleShape
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = initials,
                style = ArkaTextStyles.navigation.copy(
                    fontSize = (size.value * 0.4).sp,
                    fontWeight = FontWeight.Medium
                ),
                color = textColor
            )
        }
    }
}

/**
 * Placeholder pour images
 */
@Composable
fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Image,
    contentDescription: String? = null
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
        shape = ArkaComponentShapes.cardSmall
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Tailles d'avatars prédéfinies
 */
object AvatarSizes {
    val small = 32.dp
    val medium = 40.dp
    val large = 56.dp
    val extraLarge = 72.dp
}

/**
 * Champ de mot de passe d'Arka avec visibilité toggle
 */
@Composable
fun ArkaPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    ArkaTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        leadingIcon = Icons.Default.Lock,
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe"
                )
            }
        },
        isError = isError,
        errorMessage = errorMessage,
        enabled = enabled,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Password,
            autoCorrect = false
        ),
        keyboardActions = keyboardActions
    )
}

/**
 * Message d'erreur avec style Arka
 */
@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    ArkaCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colors.error.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colors.error,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error,
                modifier = Modifier.weight(1f)
            )

            onRetry?.let {
                ArkaIconButton(
                    icon = Icons.Default.Refresh,
                    onClick = it,
                    contentDescription = "Réessayer",
                    tint = MaterialTheme.colors.error
                )
            }
        }
    }
}

/**
 * MISE À JOUR DE ArkaTextField pour supporter visualTransformation
 */
// Remplacez votre ArkaTextField existant par cette version :
@Composable
fun ArkaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None, // ✅ Ajouté
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label?.let { { Text(text = it) } },
            placeholder = placeholder?.let { { Text(text = it) } },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation, // ✅ Ajouté
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = ArkaComponentShapes.textFieldOutlined,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                errorBorderColor = MaterialTheme.colors.error
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Message d'erreur
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}