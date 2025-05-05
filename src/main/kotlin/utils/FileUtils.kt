package utils

/**
 * Classe utilitaire pour les opérations sur les fichiers
 */
object FileUtils {
    /**
     * Formate la taille d'un fichier en taille lisible
     *
     * @param size Taille en octets
     * @return Chaîne formatée avec unité appropriée
     */
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size o"
            size < 1024 * 1024 -> "${size / 1024} Ko"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} Mo"
            else -> "${size / (1024 * 1024 * 1024)} Go"
        }
    }
}