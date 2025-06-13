// ================================================================
// THEME.KT - THÈME PRINCIPAL ARKA
// ================================================================

package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Couleurs principales d'Arka
 * Palette inspirée des couleurs familiales et chaleureuses
 */

// Couleurs primaires - Bleu familial
private val ArkaBluePrimary = Color(0xFF2E7D8B)      // Bleu principal
private val ArkaBluePrimaryVariant = Color(0xFF1E5A66) // Bleu foncé
private val ArkaBlueLight = Color(0xFF4A9BAA)        // Bleu clair

// Couleurs secondaires - Orange chaleureux
private val ArkaOrangeSecondary = Color(0xFFE67E22)   // Orange principal
private val ArkaOrangeSecondaryVariant = Color(0xFFD35400) // Orange foncé
private val ArkaOrangeLight = Color(0xFFF39C12)      // Orange clair

// Couleurs d'arrière-plan
private val ArkaBackgroundLight = Color(0xFFF8F9FA)   // Fond clair
private val ArkaBackgroundDark = Color(0xFF1A1A1A)    // Fond sombre
private val ArkaSurfaceLight = Color(0xFFFFFFFF)      // Surface claire
private val ArkaSurfaceDark = Color(0xFF2D2D2D)       // Surface sombre

// Couleurs de texte
private val ArkaTextPrimary = Color(0xFF2C3E50)       // Texte principal
private val ArkaTextSecondary = Color(0xFF7F8C8D)     // Texte secondaire
private val ArkaTextOnDark = Color(0xFFECF0F1)        // Texte sur fond sombre

// Couleurs de statut
private val ArkaSuccess = Color(0xFF27AE60)           // Succès
private val ArkaWarning = Color(0xFFF39C12)           // Avertissement
private val ArkaError = Color(0xFFE74C3C)             // Erreur
private val ArkaInfo = Color(0xFF3498DB)              // Information

/**
 * Palette de couleurs pour le thème clair
 */
private val LightColorPalette = lightColors(
    primary = ArkaBluePrimary,
    primaryVariant = ArkaBluePrimaryVariant,
    secondary = ArkaOrangeSecondary,
    secondaryVariant = ArkaOrangeSecondaryVariant,
    background = ArkaBackgroundLight,
    surface = ArkaSurfaceLight,
    error = ArkaError,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = ArkaTextPrimary,
    onSurface = ArkaTextPrimary,
    onError = Color.White
)

/**
 * Palette de couleurs pour le thème sombre
 */
private val DarkColorPalette = darkColors(
    primary = ArkaBlueLight,
    primaryVariant = ArkaBluePrimary,
    secondary = ArkaOrangeLight,
    secondaryVariant = ArkaOrangeSecondary,
    background = ArkaBackgroundDark,
    surface = ArkaSurfaceDark,
    error = ArkaError,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = ArkaTextOnDark,
    onSurface = ArkaTextOnDark,
    onError = Color.White
)

/**
 * Couleurs étendues pour Arka
 * Extensions personnalisées au-delà du Material Design standard
 */
@Stable
class ArkaColors(
    val success: Color,
    val warning: Color,
    val info: Color,
    val textSecondary: Color,
    val divider: Color,
    val cardBackground: Color,
    val selectedItem: Color,
    val isLight: Boolean
) {
    companion object {
        fun light() = ArkaColors(
            success = ArkaSuccess,
            warning = ArkaWarning,
            info = ArkaInfo,
            textSecondary = ArkaTextSecondary,
            divider = Color(0xFFE0E0E0),
            cardBackground = Color.White,
            selectedItem = ArkaBluePrimary.copy(alpha = 0.1f),
            isLight = true
        )

        fun dark() = ArkaColors(
            success = ArkaSuccess.copy(red = 0.4f),
            warning = ArkaWarning.copy(red = 0.9f),
            info = ArkaInfo.copy(blue = 0.8f),
            textSecondary = ArkaTextSecondary.copy(alpha = 0.8f),
            divider = Color(0xFF3D3D3D),
            cardBackground = Color(0xFF363636),
            selectedItem = ArkaBlueLight.copy(alpha = 0.2f),
            isLight = false
        )
    }
}

/**
 * Couleurs spécifiques pour les types de fichiers
 */
object ArkaFileColors {
    val document = Color(0xFF2196F3)      // Bleu pour documents
    val image = Color(0xFF4CAF50)         // Vert pour images
    val video = Color(0xFF9C27B0)         // Violet pour vidéos
    val audio = Color(0xFFFF9800)         // Orange pour audio
    val archive = Color(0xFF795548)       // Marron pour archives
    val pdf = Color(0xFFD32F2F)           // Rouge pour PDF
    val spreadsheet = Color(0xFF4CAF50)   // Vert pour tableaux
    val presentation = Color(0xFFFF5722)  // Orange-rouge pour présentations
    val text = Color(0xFF607D8B)          // Gris-bleu pour texte
    val unknown = Color(0xFF9E9E9E)       // Gris pour fichiers inconnus
}

/**
 * Couleurs pour les rôles familiaux
 */
object ArkaRoleColors {
    val admin = Color(0xFFD32F2F)         // Rouge pour admin
    val responsible = Color(0xFFFF9800)   // Orange pour responsable
    val member = Color(0xFF4CAF50)        // Vert pour membre
    val child = Color(0xFF2196F3)         // Bleu pour enfant
}

/**
 * Couleurs pour les statuts de permissions
 */
object ArkaPermissionColors {
    val granted = Color(0xFF4CAF50)       // Vert pour accordé
    val pending = Color(0xFFFF9800)       // Orange pour en attente
    val denied = Color(0xFFF44336)        // Rouge pour refusé
    val expired = Color(0xFF9E9E9E)       // Gris pour expiré
}

/**
 * Extension pour accéder aux couleurs Arka
 */
val Colors.arka: ArkaColors
    @Composable
    get() = if (isLight) ArkaColors.light() else ArkaColors.dark()

/**
 * Thème principal d'Arka
 */
@Composable
fun ArkaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = ArkaTypography,
        shapes = ArkaShapes,
        content = content
    )
}

/**
 * Prévisualisation du thème clair
 */
@Composable
fun ArkaLightThemePreview(content: @Composable () -> Unit) {
    ArkaTheme(darkTheme = false, content = content)
}

/**
 * Prévisualisation du thème sombre
 */
@Composable
fun ArkaDarkThemePreview(content: @Composable () -> Unit) {
    ArkaTheme(darkTheme = true, content = content)
}

/**
 * Utilitaires pour les couleurs
 */
object ArkaColorUtils {

    /**
     * Retourne une couleur avec opacité ajustée
     */
    fun Color.withAlpha(alpha: Float): Color {
        return this.copy(alpha = alpha)
    }

    /**
     * Retourne une couleur plus claire
     */
    fun Color.lighter(factor: Float = 0.2f): Color {
        return Color(
            red = (red + (1f - red) * factor).coerceIn(0f, 1f),
            green = (green + (1f - green) * factor).coerceIn(0f, 1f),
            blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
            alpha = alpha
        )
    }

    /**
     * Retourne une couleur plus foncée
     */
    fun Color.darker(factor: Float = 0.2f): Color {
        return Color(
            red = (red * (1f - factor)).coerceIn(0f, 1f),
            green = (green * (1f - factor)).coerceIn(0f, 1f),
            blue = (blue * (1f - factor)).coerceIn(0f, 1f),
            alpha = alpha
        )
    }

    /**
     * Obtient la couleur appropriée pour un type de fichier
     */
    fun getFileTypeColor(fileType: String?): Color {
        return when (fileType?.lowercase()) {
            "pdf" -> ArkaFileColors.pdf
            "doc", "docx" -> ArkaFileColors.document
            "xls", "xlsx" -> ArkaFileColors.spreadsheet
            "ppt", "pptx" -> ArkaFileColors.presentation
            "jpg", "jpeg", "png", "gif", "bmp", "svg" -> ArkaFileColors.image
            "mp4", "avi", "mov", "wmv" -> ArkaFileColors.video
            "mp3", "wav", "flac" -> ArkaFileColors.audio
            "zip", "rar", "7z", "tar" -> ArkaFileColors.archive
            "txt", "md", "rtf" -> ArkaFileColors.text
            else -> ArkaFileColors.unknown
        }
    }

    /**
     * Obtient la couleur appropriée pour un rôle
     */
    fun getRoleColor(isAdmin: Boolean, isResponsible: Boolean): Color {
        return when {
            isAdmin -> ArkaRoleColors.admin
            isResponsible -> ArkaRoleColors.responsible
            else -> ArkaRoleColors.member
        }
    }

    /**
     * Obtient la couleur appropriée pour un statut de permission
     */
    fun getPermissionStatusColor(status: String): Color {
        return when (status.lowercase()) {
            "granted", "active", "accordee" -> ArkaPermissionColors.granted
            "pending", "en_attente" -> ArkaPermissionColors.pending
            "denied", "refused", "rejetee" -> ArkaPermissionColors.denied
            "expired", "expiree" -> ArkaPermissionColors.expired
            else -> ArkaPermissionColors.pending
        }
    }
}