// ================================================================
// LOADINGCOMPONENTS.KT - COMPOSANTS DE CHARGEMENT ARKA
// ================================================================

package ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.theme.*

/**
 * INDICATEURS DE CHARGEMENT PRINCIPAL
 */

/**
 * Indicateur de chargement principal d'Arka
 * Utilisé pour les chargements de page ou d'écran complet
 */
@Composable
fun ArkaLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colors.primary,
    strokeWidth: Dp = 4.dp,
    message: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = strokeWidth
        )

        message?.let {
            Text(
                text = it,
                style = ArkaTextStyles.cardDescription,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Indicateur de chargement avec animation rotative personnalisée
 * Utilise l'icône de dossier d'Arka pour rester dans le thème familial
 */
@Composable
fun ArkaFamilyLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    message: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .rotate(rotationAngle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Chargement",
                modifier = Modifier.size(size * 0.8f),
                tint = MaterialTheme.colors.primary
            )
        }

        message?.let {
            Text(
                text = it,
                style = ArkaTextStyles.cardDescription,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * CHARGEMENTS SPÉCIALISÉS
 */

/**
 * Indicateur de chargement pour les fichiers
 * Avec barre de progression et informations détaillées
 */
@Composable
fun ArkaFileLoadingIndicator(
    fileName: String,
    progress: Float? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.InsertDriveFile,
    showCancel: Boolean = false,
    onCancel: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = ArkaComponentShapes.cardMedium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colors.primary
                )

                Text(
                    text = fileName,
                    style = ArkaTextStyles.cardTitle,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                if (showCancel && onCancel != null) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Annuler",
                            tint = MaterialTheme.colors.error
                        )
                    }
                }
            }

            if (progress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colors.primary,
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = ArkaTextStyles.helpText,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colors.primary,
                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

/**
 * Indicateur de chargement pour la synchronisation mobile
 */
@Composable
fun ArkaMobileSyncLoadingIndicator(
    deviceName: String,
    syncType: String = "Synchronisation",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface,
        shape = ArkaComponentShapes.cardSmall
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colors.primary.copy(alpha = alpha)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = syncType,
                    style = ArkaTextStyles.cardDescription,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = deviceName,
                    style = ArkaTextStyles.helpText,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }

            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colors.primary,
                strokeWidth = 2.dp
            )
        }
    }
}

/**
 * ÉTATS DE CHARGEMENT COMPLEXES
 */

/**
 * État de chargement avec étapes multiples
 * Utile pour les opérations complexes comme l'initialisation du serveur
 */
@Composable
fun ArkaMultiStepLoadingIndicator(
    steps: List<LoadingStep>,
    currentStepIndex: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = ArkaComponentShapes.cardMedium
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Initialisation d'Arka",
                style = ArkaTextStyles.cardTitle,
                fontWeight = FontWeight.Bold
            )

            steps.forEachIndexed { index, step ->
                LoadingStepItem(
                    step = step,
                    isActive = index == currentStepIndex,
                    isCompleted = index < currentStepIndex,
                    isUpcoming = index > currentStepIndex
                )
            }
        }
    }
}

/**
 * Item individuel d'étape de chargement
 */
@Composable
private fun LoadingStepItem(
    step: LoadingStep,
    isActive: Boolean,
    isCompleted: Boolean,
    isUpcoming: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Indicateur d'état
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> MaterialTheme.colors.primary
                        isActive -> MaterialTheme.colors.primary.copy(alpha = 0.2f)
                        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
                isActive -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colors.primary,
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Description de l'étape
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = ArkaTextStyles.cardDescription,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                color = when {
                    isCompleted -> MaterialTheme.colors.primary
                    isActive -> MaterialTheme.colors.onSurface
                    else -> MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                }
            )

            if (step.description.isNotEmpty()) {
                Text(
                    text = step.description,
                    style = ArkaTextStyles.helpText,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * COMPOSANTS DE SQUELETTE (SKELETON LOADING)
 */

/**
 * Chargement squelette pour les cartes de fichiers
 */
@Composable
fun ArkaFileCardSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = 2.dp,
        shape = ArkaComponentShapes.cardSmall
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icône simulée
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = alpha))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Titre simulé
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = alpha))
                )

                // Description simulée
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = alpha))
                )

                // Métadonnées simulées
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = alpha))
                )
            }
        }
    }
}

/**
 * Chargement squelette pour liste de membres famille
 */
@Composable
fun ArkaFamilyMemberSkeleton(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar simulé
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = alpha))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Nom simulé
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = alpha))
            )

            // Rôle simulé
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = alpha))
            )
        }

        // Status simulé
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = alpha))
        )
    }
}

/**
 * UTILITAIRES ET MODÈLES DE DONNÉES
 */

/**
 * Modèle pour les étapes de chargement
 */
data class LoadingStep(
    val title: String,
    val description: String = "",
    val icon: ImageVector? = null
)

/**
 * États de chargement courants pour Arka
 */
object ArkaLoadingSteps {
    val serverInitialization = listOf(
        LoadingStep("Initialisation du serveur", "Démarrage des services principaux"),
        LoadingStep("Chargement de la base de données", "Connexion et vérification des données"),
        LoadingStep("Configuration familiale", "Chargement des profils et permissions"),
        LoadingStep("Préparation de l'interface", "Initialisation des composants"),
        LoadingStep("Finalisation", "Arka est prêt à être utilisé")
    )

    val mobileSync = listOf(
        LoadingStep("Connexion mobile", "Établissement de la liaison sécurisée"),
        LoadingStep("Authentification", "Vérification des permissions"),
        LoadingStep("Synchronisation", "Transfert des données")
    )
}

/**
 * Fonction utilitaire pour créer des listes de squelettes
 */
@Composable
fun ArkaSkeletonList(
    count: Int = 5,
    skeletonItem: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(count) {
            skeletonItem()
        }
    }
}