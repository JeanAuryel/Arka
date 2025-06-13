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
    Card(
        modifier = modifier,
        elevation = elevation,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        border = border,
        onClick = onClick
    ) {
        Column(content = content)
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
                            style = ArkaTextStyles.caption,
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
                                style = ArkaTextStyles.caption,
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
    singleLine: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            readOnly = readOnly,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            shape = ArkaComponentShapes.textFieldOutlined,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.23f),
                errorBorderColor = MaterialTheme.colors.error
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Message d'erreur
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = ArkaTextStyles.errorText,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Champ de mot de passe avec visibilité
 */
@Composable
fun ArkaPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Mot de passe",
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    ArkaTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        leadingIcon = Icons.Default.Lock,
        trailingIcon = {
            ArkaIconButton(
                icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                onClick = { passwordVisible = !passwordVisible },
                contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe"
            )
        },
        isError = isError,
        errorMessage = errorMessage,
        enabled = enabled,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions.Default.copy(
            autoCorrect = false
        )
    )
}

/**
 * AVATARS ET ICÔNES
 */

/**
 * Avatar utilisateur avec initiales
 */
@Composable
fun UserAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    backgroundColor: Color = MaterialTheme.colors.primary,
    textColor: Color = MaterialTheme.colors.onPrimary,
    imageUrl: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // TODO: Charger l'image si imageUrl est fourni
        if (imageUrl != null) {
            // Placeholder pour l'image
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(size * 0.6f)
            )
        } else {
            // Afficher les initiales
            val initials = name.split(" ")
                .take(2)
                .map { it.firstOrNull()?.uppercase() ?: "" }
                .joinToString("")
                .take(2)

            Text(
                text = initials,
                style = MaterialTheme.typography.subtitle2.copy(
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.Medium
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Badge de statut
 */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primary,
    textColor: Color = MaterialTheme.colors.onPrimary,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = ArkaComponentShapes.badgeRound,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = textColor
                )
            }

            Text(
                text = text,
                style = ArkaTextStyles.badge,
                color = textColor
            )
        }
    }
}

/**
 * ÉTATS VIDES ET D'ERREUR
 */

/**
 * État vide avec icône et message
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )

        Text(
            text = title,
            style = ArkaTextStyles.cardTitle,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        subtitle?.let {
            Text(
                text = it,
                style = ArkaTextStyles.cardDescription,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }

        actionButton?.invoke()
    }
}

/**
 * Message d'erreur avec possibilité de retry
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
                style = ArkaTextStyles.cardDescription,
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
 * UTILITAIRES ET ÉNUMÉRATIONS
 */

/**
 * Direction de tendance pour les statistiques
 */
enum class TrendDirection {
    UP, DOWN, STABLE
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