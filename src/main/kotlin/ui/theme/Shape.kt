// ================================================================
// SHAPE.KT - FORMES ET BORDURES ARKA
// ================================================================

package ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Système de formes pour Arka
 * Définit les coins arrondis et formes utilisées dans l'application
 */

/**
 * Formes principales Material Design adaptées pour Arka
 */
val ArkaShapes = Shapes(
    small = RoundedCornerShape(4.dp),      // Petits éléments (badges, chips)
    medium = RoundedCornerShape(8.dp),     // Éléments moyens (boutons, champs)
    large = RoundedCornerShape(12.dp)      // Grands éléments (cartes, dialogs)
)

/**
 * Formes étendues pour des cas d'usage spécifiques
 */
object ArkaExtendedShapes {

    // Formes rondes
    val extraSmall = RoundedCornerShape(2.dp)      // Très petits éléments
    val extraLarge = RoundedCornerShape(16.dp)     // Très grands éléments
    val huge = RoundedCornerShape(24.dp)           // Éléments énormes (modals)

    // Formes partiellement arrondies
    val topRounded = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    val bottomRounded = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )

    val leftRounded = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 0.dp,
        bottomStart = 12.dp,
        bottomEnd = 0.dp
    )

    val rightRounded = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 12.dp,
        bottomStart = 0.dp,
        bottomEnd = 12.dp
    )

    // Coins spécifiques
    val topLeftOnly = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 0.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    val topRightOnly = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 12.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    val bottomLeftOnly = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 12.dp,
        bottomEnd = 0.dp
    )

    val bottomRightOnly = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 0.dp,
        bottomEnd = 12.dp
    )
}

/**
 * Formes spécifiques aux composants Arka
 */
object ArkaComponentShapes {

    // Formes pour les cartes
    val cardSmall = RoundedCornerShape(6.dp)        // Petites cartes
    val cardMedium = RoundedCornerShape(10.dp)      // Cartes moyennes
    val cardLarge = RoundedCornerShape(14.dp)       // Grandes cartes
    val cardElevated = RoundedCornerShape(12.dp)    // Cartes avec élévation

    // Formes pour les boutons
    val buttonSmall = RoundedCornerShape(6.dp)      // Petits boutons
    val buttonMedium = RoundedCornerShape(8.dp)     // Boutons moyens
    val buttonLarge = RoundedCornerShape(10.dp)     // Grands boutons
    val buttonRound = RoundedCornerShape(50)        // Boutons ronds (FAB)

    // Formes pour les champs de saisie
    val textFieldOutlined = RoundedCornerShape(8.dp) // Champs outlined
    val textFieldFilled = RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 8.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    // Formes pour les dialogs
    val dialogSmall = RoundedCornerShape(12.dp)     // Petits dialogs
    val dialogMedium = RoundedCornerShape(16.dp)    // Dialogs moyens
    val dialogLarge = RoundedCornerShape(20.dp)     // Grands dialogs
    val dialogFullscreen = RoundedCornerShape(0.dp) // Dialogs plein écran

    // Formes pour les conteneurs
    val containerSmall = RoundedCornerShape(4.dp)   // Petits conteneurs
    val containerMedium = RoundedCornerShape(8.dp)  // Conteneurs moyens
    val containerLarge = RoundedCornerShape(12.dp)  // Grands conteneurs

    // Formes pour les éléments de liste
    val listItemFirst = RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 8.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    val listItemMiddle = RoundedCornerShape(0.dp)

    val listItemLast = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 8.dp,
        bottomEnd = 8.dp
    )

    val listItemSingle = RoundedCornerShape(8.dp)

    // Formes pour les badges et chips
    val badgeRound = RoundedCornerShape(50)         // Badges ronds
    val badgeSquare = RoundedCornerShape(4.dp)      // Badges carrés
    val chipSmall = RoundedCornerShape(12.dp)       // Petits chips
    val chipMedium = RoundedCornerShape(16.dp)      // Chips moyens

    // Formes pour les éléments de navigation
    val tabIndicator = RoundedCornerShape(2.dp)     // Indicateur d'onglet
    val navigationItem = RoundedCornerShape(20.dp)  // Élément de navigation
    val bottomSheet = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    // Formes pour les éléments multimédia
    val imageSmall = RoundedCornerShape(4.dp)       // Petites images
    val imageMedium = RoundedCornerShape(8.dp)      // Images moyennes
    val imageLarge = RoundedCornerShape(12.dp)      // Grandes images
    val avatar = RoundedCornerShape(50)             // Avatars

    // Formes pour les éléments de fichier
    val fileIcon = RoundedCornerShape(6.dp)         // Icônes de fichier
    val folderIcon = RoundedCornerShape(4.dp)       // Icônes de dossier
    val thumbnail = RoundedCornerShape(8.dp)        // Miniatures
    val preview = RoundedCornerShape(12.dp)         // Prévisualisations
}

/**
 * Formes spéciales pour des cas d'usage particuliers
 */
object ArkaSpecialShapes {

    // Forme en coin pour les notifications
    val notificationCorner = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 8.dp,
        bottomStart = 8.dp,
        bottomEnd = 8.dp
    )

    // Forme pour les pointers/tooltips
    val tooltip = RoundedCornerShape(6.dp)

    // Forme pour les barres de progression
    val progressBar = RoundedCornerShape(2.dp)
    val progressBarThick = RoundedCornerShape(4.dp)

    // Forme pour les dividers avec coins arrondis
    val dividerRounded = RoundedCornerShape(1.dp)

    // Formes pour les éléments flottants
    val floatingSmall = RoundedCornerShape(12.dp)
    val floatingMedium = RoundedCornerShape(16.dp)
    val floatingLarge = RoundedCornerShape(20.dp)

    // Formes pour les overlays
    val overlayCorner = RoundedCornerShape(8.dp)
    val overlayFull = RoundedCornerShape(0.dp)
}

/**
 * Utilitaires pour les formes
 */
object ArkaShapeUtils {

    /**
     * Crée une forme avec des coins personnalisés
     */
    fun customCorners(
        topStart: androidx.compose.ui.unit.Dp = 0.dp,
        topEnd: androidx.compose.ui.unit.Dp = 0.dp,
        bottomStart: androidx.compose.ui.unit.Dp = 0.dp,
        bottomEnd: androidx.compose.ui.unit.Dp = 0.dp
    ): Shape = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomStart = bottomStart,
        bottomEnd = bottomEnd
    )

    /**
     * Crée une forme ronde uniforme
     */
    fun uniform(radius: androidx.compose.ui.unit.Dp): Shape = RoundedCornerShape(radius)

    /**
     * Crée une forme complètement ronde
     */
    fun circle(): Shape = RoundedCornerShape(50)

    /**
     * Crée une forme sans coins arrondis
     */
    fun rectangle(): Shape = RoundedCornerShape(0.dp)

    /**
     * Adaptation responsive des formes selon la taille
     */
    fun responsive(
        small: Shape,
        medium: Shape,
        large: Shape,
        screenWidth: androidx.compose.ui.unit.Dp
    ): Shape {
        return when {
            screenWidth < 600.dp -> small
            screenWidth < 1200.dp -> medium
            else -> large
        }
    }

    /**
     * Formes pour les états interactifs
     */
    fun forState(
        normal: Shape,
        pressed: Shape = normal,
        disabled: Shape = normal
    ): Triple<Shape, Shape, Shape> {
        return Triple(normal, pressed, disabled)
    }
}

/**
 * Collections de formes prédéfinies pour différents thèmes
 */
object ArkaShapeCollections {

    // Collection moderne (coins plus arrondis)
    val modern = Shapes(
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(18.dp)
    )

    // Collection classique (coins moins arrondis)
    val classic = Shapes(
        small = RoundedCornerShape(2.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(6.dp)
    )

    // Collection cozy (coins très arrondis, familial)
    val cozy = Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp)
    )

    // Collection sharp (coins angulaires)
    val sharp = Shapes(
        small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(2.dp),
        large = RoundedCornerShape(4.dp)
    )
}