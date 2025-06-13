// ================================================================
// TYPE.KT - TYPOGRAPHIE ARKA
// ================================================================

package ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Famille de polices personnalisée pour Arka
 * Utilise des polices système par défaut avec fallback
 */
private val InterFontFamily = FontFamily.Default // À remplacer par une police personnalisée si nécessaire

/**
 * Famille de polices pour les titres
 */
private val HeadingFontFamily = FontFamily.Default

/**
 * Famille de polices pour le code et les éléments techniques
 */
private val MonospaceFontFamily = FontFamily.Monospace

/**
 * Typographie principale d'Arka
 * Basée sur Material Design avec des adaptations pour l'application familiale
 */
val ArkaTypography = Typography(
    // Titre principal - Logo, titres de page
    h1 = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),

    // Titre de section - Titres de cartes principales
    h2 = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp
    ),

    // Titre de sous-section
    h3 = TextStyle(
        fontFamily = HeadingFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Titre de composant
    h4 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.25.sp
    ),

    // Titre de sous-composant
    h5 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    // Titre de liste, label important
    h6 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // Sous-titre principal
    subtitle1 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // Sous-titre secondaire
    subtitle2 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Corps de texte principal
    body1 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    // Corps de texte secondaire
    body2 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),

    // Texte de bouton
    button = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.25.sp
    ),

    // Légende, texte d'aide
    caption = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Texte très petit
    overline = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.5.sp
    )
)

/**
 * Styles de texte étendus pour Arka
 * Styles personnalisés au-delà du Material Design standard
 */
object ArkaTextStyles {

    /**
     * Style pour les noms de fichiers
     */
    val fileName = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    /**
     * Style pour les chemins de fichiers
     */
    val filePath = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )

    /**
     * Style pour les tailles de fichiers
     */
    val fileSize = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )

    /**
     * Style pour les dates
     */
    val date = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    )

    /**
     * Style pour les badges de statut
     */
    val badge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )

    /**
     * Style pour les numéros (statistiques)
     */
    val number = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    /**
     * Style pour les numéros de grande taille
     */
    val bigNumber = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp
    )

    /**
     * Style pour les labels de formulaire
     */
    val formLabel = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    /**
     * Style pour les messages d'erreur
     */
    val errorText = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )

    /**
     * Style pour les textes d'aide
     */
    val helpText = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        fontStyle = FontStyle.Italic
    )

    /**
     * Style pour le breadcrumb
     */
    val breadcrumb = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    )

    /**
     * Style pour les éléments de navigation
     */
    val navigation = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    /**
     * Style pour les titres de cartes
     */
    val cardTitle = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    )

    /**
     * Style pour les descriptions de cartes
     */
    val cardDescription = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )

    /**
     * Style pour les métadonnées
     */
    val metadata = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )

    /**
     * Style pour le texte de code
     */
    val code = TextStyle(
        fontFamily = MonospaceFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    /**
     * Style pour les liens
     */
    val link = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )

    /**
     * Style pour les tableaux - en-tête
     */
    val tableHeader = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )

    /**
     * Style pour les tableaux - cellule
     */
    val tableCell = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    )
}

/**
 * Utilitaires de typographie pour Arka
 */
object ArkaTextUtils {

    /**
     * Retourne un style avec une couleur spécifique
     */
    fun TextStyle.withColor(color: androidx.compose.ui.graphics.Color): TextStyle {
        return this.copy(color = color)
    }

    /**
     * Retourne un style avec une taille de police différente
     */
    fun TextStyle.withSize(fontSize: androidx.compose.ui.unit.TextUnit): TextStyle {
        return this.copy(fontSize = fontSize)
    }

    /**
     * Retourne un style avec un poids de police différent
     */
    fun TextStyle.withWeight(fontWeight: FontWeight): TextStyle {
        return this.copy(fontWeight = fontWeight)
    }

    /**
     * Style pour texte en majuscules
     */
    fun TextStyle.uppercase(): TextStyle {
        return this.copy(letterSpacing = 1.sp)
    }

    /**
     * Style pour texte condensé
     */
    fun TextStyle.condensed(): TextStyle {
        return this.copy(letterSpacing = (-0.25).sp)
    }

    /**
     * Style pour texte élargi
     */
    fun TextStyle.expanded(): TextStyle {
        return this.copy(letterSpacing = 0.5.sp)
    }
}

/**
 * Prévisualisation de la typographie
 */
object ArkaTypographyPreview {
    val sampleTexts = listOf(
        "Titre Principal" to ArkaTypography.h1,
        "Titre de Section" to ArkaTypography.h2,
        "Sous-titre" to ArkaTypography.subtitle1,
        "Corps de texte principal pour la lecture" to ArkaTypography.body1,
        "Corps de texte secondaire" to ArkaTypography.body2,
        "Nom de fichier.pdf" to ArkaTextStyles.fileName,
        "12 MB" to ArkaTextStyles.fileSize,
        "15/11/2024" to ArkaTextStyles.date,
        "BADGE" to ArkaTextStyles.badge,
        "Texte d'aide et d'information" to ArkaTextStyles.helpText
    )
}