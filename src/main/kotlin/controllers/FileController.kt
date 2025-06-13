package controllers

import repositories.FileRepository
import repositories.FolderRepository
import org.ktorm.schema.Column
import ktorm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.io.File

/**
 * Controller pour la gestion des fichiers
 *
 * Responsabilités:
 * - CRUD des fichiers
 * - Gestion des propriétaires et créateurs
 * - Upload et téléchargement de fichiers
 * - Validation des types et tailles de fichiers
 * - Recherche et filtrage des fichiers
 * - Statistiques et rapports fichiers
 * - Gestion des permissions sur les fichiers
 * - Versioning et historique des modifications
 *
 * Utilisé par: UI Components, DashboardService, UploadService
 * Utilise: FileRepository, FolderRepository
 *
 * Pattern: Controller Layer + Result Pattern (standardisé avec AuthController)
 */
class FileController(
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository
) {

    /**
     * Résultats des opérations sur les fichiers - PATTERN STANDARDISÉ
     */
    sealed class FileResult<out T> {
        data class Success<T>(val data: T) : FileResult<T>()
        data class Error(val message: String, val code: FileErrorCode) : FileResult<Nothing>()
    }

    enum class FileErrorCode {
        FILE_NOT_FOUND,
        FOLDER_NOT_FOUND,
        FILE_ALREADY_EXISTS,
        INVALID_INPUT,
        INVALID_FILE_TYPE,
        FILE_TOO_LARGE,
        PERMISSION_DENIED,
        UPLOAD_FAILED,
        DOWNLOAD_FAILED,
        STORAGE_FULL,
        INTERNAL_ERROR
    }

    companion object {
        // Limites de fichiers
        const val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100MB
        const val MAX_FILES_PER_FOLDER = 1000

        // Types de fichiers autorisés
        val ALLOWED_FILE_TYPES = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "jpg", "jpeg", "png", "gif", "bmp", "svg",
            "mp4", "avi", "mov", "wmv", "mp3", "wav",
            "txt", "rtf", "zip", "rar", "7z"
        )
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET UPLOAD
    // ================================================================

    /**
     * Upload et crée un nouveau fichier
     *
     * @param fileData Données du nouveau fichier
     * @return Résultat de la création
     */
    suspend fun uploadFile(fileData: UploadFileRequest): FileResult<Fichier> = withContext(Dispatchers.IO) {
        try {
            // Validation des données
            val validationError = validateFileData(fileData)
            if (validationError != null) {
                return@withContext FileResult.Error(validationError, FileErrorCode.INVALID_INPUT)
            }

            // Vérifier que le dossier existe
            val folder = folderRepository.findById(fileData.folderId)
                ?: return@withContext FileResult.Error(
                    "Dossier non trouvé",
                    FileErrorCode.FOLDER_NOT_FOUND
                )

            // Vérifier si un fichier avec ce nom existe déjà dans le dossier
            // TODO: Implémenter vérification d'unicité quand la méthode repository sera ajoutée

            // Vérifier le nombre de fichiers dans le dossier (implémentation simplifiée)
            val allFiles = fileRepository.findAll()
            val filesInFolder = allFiles.filter { it.dossierId == fileData.folderId }
            val fileCount = filesInFolder.size
            if (fileCount >= MAX_FILES_PER_FOLDER) {
                return@withContext FileResult.Error(
                    "Le dossier contient trop de fichiers (limite: $MAX_FILES_PER_FOLDER)",
                    FileErrorCode.STORAGE_FULL
                )
            }

            // Validation du type de fichier
            val fileExtension = getFileExtension(fileData.fileName)
            if (!isAllowedFileType(fileExtension)) {
                return@withContext FileResult.Error(
                    "Type de fichier non autorisé: $fileExtension",
                    FileErrorCode.INVALID_FILE_TYPE
                )
            }

            // Validation de la taille
            if (fileData.fileSize > MAX_FILE_SIZE) {
                return@withContext FileResult.Error(
                    "Fichier trop volumineux (limite: ${MAX_FILE_SIZE / (1024 * 1024)}MB)",
                    FileErrorCode.FILE_TOO_LARGE
                )
            }

            // Traitement du contenu du fichier
            val processedContent = processFileContent(fileData.content, fileExtension)

            // Créer l'entité fichier
            val fileEntity = FichierEntity {
                nomFichier = fileData.fileName.trim()
                typeFichier = fileExtension
                tailleFichier = fileData.fileSize
                contenuFichier = processedContent
                cheminFichier = generateFilePath(fileData.fileName, folder.categorieId, fileData.folderId)
                dateCreationFichier = LocalDateTime.now()
                dateDerniereModifFichier = LocalDateTime.now()
                proprietaireId = fileData.ownerId
                createurId = fileData.creatorId
                dossierId = fileData.folderId
            }

            // Sauvegarder
            val savedFile = fileRepository.create(fileEntity)

            // Mettre à jour la date de modification du dossier parent
            updateFolderModificationDate(fileData.folderId)

            return@withContext FileResult.Success(savedFile.toModel())

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors de l'upload du fichier: ${e.message}",
                FileErrorCode.UPLOAD_FAILED
            )
        }
    }

    /**
     * Met à jour un fichier existant
     *
     * @param fileId ID du fichier
     * @param updateData Nouvelles données
     * @return Résultat de la mise à jour
     */
    suspend fun updateFile(
        fileId: Int,
        updateData: UpdateFileRequest
    ): FileResult<Fichier> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le fichier existe
            val file = fileRepository.findById(fileId)
                ?: return@withContext FileResult.Error(
                    "Fichier non trouvé",
                    FileErrorCode.FILE_NOT_FOUND
                )

            // Validation des nouvelles données
            val validationError = validateUpdateData(updateData)
            if (validationError != null) {
                return@withContext FileResult.Error(validationError, FileErrorCode.INVALID_INPUT)
            }

            // Vérifier si le nouveau nom existe déjà (si changé)
            // TODO: Implémenter vérification d'unicité quand la méthode repository sera ajoutée

            // Appliquer les modifications
            updateData.fileName?.let {
                file.nomFichier = it.trim()
                // Mettre à jour le type si l'extension a changé
                val newExtension = getFileExtension(it)
                if (isAllowedFileType(newExtension)) {
                    file.typeFichier = newExtension
                }
            }

            if (updateData.content != null) {
                val newExtension = file.typeFichier ?: ""
                file.contenuFichier = processFileContent(updateData.content, newExtension)
                file.tailleFichier = updateData.content.size.toLong()
            }

            file.dateDerniereModifFichier = LocalDateTime.now()

            // Mettre à jour
            fileRepository.update(file)

            // Mettre à jour la date de modification du dossier parent
            updateFolderModificationDate(file.dossierId)

            return@withContext FileResult.Success(file.toModel())

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors de la mise à jour: ${e.message}",
                FileErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE CONSULTATION
    // ================================================================

    /**
     * Récupère un fichier par son ID
     *
     * @param fileId ID du fichier
     * @return Résultat avec le fichier trouvé
     */
    suspend fun getFileById(fileId: Int): FileResult<Fichier> = withContext(Dispatchers.IO) {
        try {
            val file = fileRepository.findById(fileId)
                ?: return@withContext FileResult.Error(
                    "Fichier non trouvé",
                    FileErrorCode.FILE_NOT_FOUND
                )

            return@withContext FileResult.Success(file.toModel())

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors de la récupération: ${e.message}",
                FileErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère tous les fichiers d'un dossier
     *
     * @param folderId ID du dossier
     * @return Résultat avec la liste des fichiers
     */
    suspend fun getFilesByFolder(folderId: Int): FileResult<List<Fichier>> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le dossier existe
            val folder = folderRepository.findById(folderId)
                ?: return@withContext FileResult.Error(
                    "Dossier non trouvé",
                    FileErrorCode.FOLDER_NOT_FOUND
                )

            val files = fileRepository.getFichiersByDossier(folderId)
            val fileModels = files.map { fileEntity -> fileEntity.toModel() }

            return@withContext FileResult.Success(fileModels)

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors de la récupération des fichiers: ${e.message}",
                FileErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère tous les fichiers d'un propriétaire
     *
     * @param ownerId ID du propriétaire
     * @return Résultat avec la liste des fichiers
     */
    suspend fun getFilesByOwner(ownerId: Int): FileResult<List<Fichier>> = withContext(Dispatchers.IO) {
        try {
            val files = fileRepository.getFichiersByProprietaire(ownerId)
            val fileModels = files.map { it.toModel() }

            return@withContext FileResult.Success(fileModels)

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors de la récupération des fichiers du propriétaire: ${e.message}",
                FileErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Recherche des fichiers par nom ou type
     *
     * @param searchTerm Terme de recherche
     * @param fileType Type de fichier (optionnel)
     * @param folderId ID du dossier (optionnel)
     * @return Résultat avec les fichiers trouvés
     */
    suspend fun searchFiles(
        searchTerm: String,
        fileType: String? = null,
        folderId: Int? = null
    ): FileResult<List<Fichier>> = withContext(Dispatchers.IO) {
        try {
            if (searchTerm.isBlank()) {
                return@withContext FileResult.Error(
                    "Terme de recherche requis",
                    FileErrorCode.INVALID_INPUT
                )
            }

            // Recherche simplifiée dans tous les fichiers
            val allFiles = fileRepository.findAll()
            val filteredFiles = allFiles.filter { file ->
                val nameMatch = file.nomFichier?.contains(searchTerm, ignoreCase = true) == true
                val typeMatch = fileType == null || file.typeFichier == fileType
                val folderMatch = folderId == null || file.dossierId == folderId

                nameMatch && typeMatch && folderMatch
            }
            val fileModels = filteredFiles.map { fileEntity -> fileEntity.toModel() }

            return@withContext FileResult.Success(fileModels)

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors de la recherche: ${e.message}",
                FileErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère les fichiers récents d'un utilisateur
     *
     * @param userId ID de l'utilisateur
     * @param limit Nombre maximum de fichiers à retourner
     * @return Résultat avec les fichiers récents
     */
    suspend fun getRecentFiles(userId: Int, limit: Int = 10): FileResult<List<Fichier>> = withContext(Dispatchers.IO) {
        try {
            // Implémentation simplifiée des fichiers récents
            val files = fileRepository.getFichiersByProprietaire(userId)
                .sortedByDescending { it.dateCreationFichier ?: LocalDateTime.MIN }
                .take(limit)
            val fileModels = files.map { fileEntity -> fileEntity.toModel() }

            return@withContext FileResult.Success(fileModels)

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors de la récupération des fichiers récents: ${e.message}",
                FileErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE TÉLÉCHARGEMENT
    // ================================================================

    /**
     * Télécharge le contenu d'un fichier
     *
     * @param fileId ID du fichier
     * @return Résultat avec le contenu du fichier
     */
    suspend fun downloadFile(fileId: Int): FileResult<FileDownload> = withContext(Dispatchers.IO) {
        try {
            val file = fileRepository.findById(fileId)
                ?: return@withContext FileResult.Error(
                    "Fichier non trouvé",
                    FileErrorCode.FILE_NOT_FOUND
                )

            val downloadData = FileDownload(
                fileName = file.nomFichier ?: "unknown",
                fileType = file.typeFichier ?: "unknown",
                fileSize = file.tailleFichier ?: 0L,
                content = file.contenuFichier ?: byteArrayOf(),
                lastModified = file.dateDerniereModifFichier
            )

            return@withContext FileResult.Success(downloadData)

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors du téléchargement: ${e.message}",
                FileErrorCode.DOWNLOAD_FAILED
            )
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION
    // ================================================================

    /**
     * Supprime un fichier
     *
     * @param fileId ID du fichier à supprimer
     * @return Résultat de la suppression
     */
    suspend fun deleteFile(fileId: Int): FileResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le fichier existe
            val file = fileRepository.findById(fileId)
                ?: return@withContext FileResult.Error(
                    "Fichier non trouvé",
                    FileErrorCode.FILE_NOT_FOUND
                )

            val folderId = file.dossierId

            // Supprimer le fichier
            val deleted = fileRepository.delete(fileId)
            if (deleted == 0) {
                return@withContext FileResult.Error(
                    "Aucun fichier supprimé",
                    FileErrorCode.INTERNAL_ERROR
                )
            }

            // Mettre à jour la date de modification du dossier parent
            updateFolderModificationDate(file.dossierId)

            return@withContext FileResult.Success(Unit)

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors de la suppression: ${e.message}",
                FileErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques d'un fichier
     *
     * @param fileId ID du fichier
     * @return Résultat avec les statistiques
     */
    suspend fun getFileStatistics(fileId: Int): FileResult<FileStatistics> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le fichier existe
            val file = fileRepository.findById(fileId)
                ?: return@withContext FileResult.Error(
                    "Fichier non trouvé",
                    FileErrorCode.FILE_NOT_FOUND
                )

            val stats = FileStatistics(
                fileId = fileId,
                fileName = file.nomFichier ?: "unknown",
                fileType = file.typeFichier ?: "unknown",
                fileSize = file.tailleFichier ?: 0L,
                creationDate = file.dateCreationFichier,
                lastModified = file.dateDerniereModifFichier,
                ownerId = file.proprietaireId ?: 0,
                creatorId = file.createurId ?: 0,
                folderId = file.dossierId ?: 0
            )

            return@withContext FileResult.Success(stats)

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors du calcul des statistiques: ${e.message}",
                FileErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Obtient les statistiques globales des fichiers d'un utilisateur
     *
     * @param userId ID de l'utilisateur
     * @return Résultat avec les statistiques globales
     */
    suspend fun getUserFileStatistics(userId: Int): FileResult<UserFileStatistics> = withContext(Dispatchers.IO) {
        try {
            // Statistiques simplifiées basées sur les méthodes existantes
            val userFiles = fileRepository.getFichiersByProprietaire(userId)
            val totalFiles = userFiles.size
            val totalSize = userFiles.sumOf { it.tailleFichier ?: 0L }

            // Fichiers récents (30 derniers jours)
            val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
            val recentFileCount = userFiles.count {
                it.dateCreationFichier?.isAfter(thirtyDaysAgo) == true
            }

            // Grouper par type
            val filesByType = userFiles
                .groupBy { it.typeFichier ?: "unknown" }
                .mapValues { it.value.size }

            val stats = UserFileStatistics(
                userId = userId,
                totalFiles = totalFiles,
                totalSize = totalSize,
                recentFileCount = recentFileCount,
                filesByType = filesByType
            )

            return@withContext FileResult.Success(stats)

        } catch (e: Exception) {
            return@withContext FileResult.Error(
                "Erreur lors du calcul des statistiques utilisateur: ${e.message}",
                FileErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES
    // ================================================================

    private fun validateFileData(fileData: UploadFileRequest): String? {
        return when {
            fileData.fileName.isBlank() -> "Le nom du fichier ne peut pas être vide"
            fileData.fileName.length > 255 -> "Le nom du fichier ne peut pas dépasser 255 caractères"
            fileData.fileName.contains(Regex("[<>:\"/\\\\|?*]")) ->
                "Le nom du fichier contient des caractères non autorisés"
            fileData.content.isEmpty() -> "Le fichier ne peut pas être vide"
            fileData.fileSize <= 0 -> "La taille du fichier doit être positive"
            fileData.folderId <= 0 -> "ID de dossier invalide"
            fileData.ownerId <= 0 -> "ID de propriétaire invalide"
            fileData.creatorId <= 0 -> "ID de créateur invalide"
            else -> null
        }
    }

    private fun validateUpdateData(updateData: UpdateFileRequest): String? {
        return when {
            updateData.fileName != null && updateData.fileName.isBlank() ->
                "Le nom du fichier ne peut pas être vide"
            updateData.fileName != null && updateData.fileName.length > 255 ->
                "Le nom du fichier ne peut pas dépasser 255 caractères"
            updateData.fileName != null && updateData.fileName.contains(Regex("[<>:\"/\\\\|?*]")) ->
                "Le nom du fichier contient des caractères non autorisés"
            updateData.content != null && updateData.content.isEmpty() ->
                "Le fichier ne peut pas être vide"
            else -> null
        }
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    private fun isAllowedFileType(extension: String): Boolean {
        return extension in ALLOWED_FILE_TYPES
    }

    private fun processFileContent(content: ByteArray, fileType: String): ByteArray {
        // Ici on pourrait ajouter des traitements spécifiques selon le type
        // comme compression, validation, conversion, etc.
        return content
    }

    private fun generateFilePath(fileName: String, categoryId: Int, folderId: Int): String {
        val timestamp = System.currentTimeMillis()
        return "files/$categoryId/$folderId/${timestamp}_$fileName"
    }

    private suspend fun updateFolderModificationDate(folderId: Int?) {
        try {
            if (folderId != null) {
                val folder = folderRepository.findById(folderId)
                folder?.let {
                    it.dateDerniereModifDossier = LocalDateTime.now()
                    folderRepository.update(it)
                }
            }
        } catch (e: Exception) {
            // Log l'erreur mais ne fait pas échouer l'opération principale
            println("Erreur lors de la mise à jour de la date du dossier: ${e.message}")
        }
    }
}

// ================================================================
// DATA CLASSES POUR LES REQUÊTES ET RÉSULTATS
// ================================================================

/**
 * Données pour uploader un nouveau fichier
 */
data class UploadFileRequest(
    val fileName: String,
    val content: ByteArray,
    val fileSize: Long,
    val folderId: Int,
    val ownerId: Int,
    val creatorId: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UploadFileRequest
        return fileName == other.fileName && folderId == other.folderId
    }

    override fun hashCode(): Int {
        return fileName.hashCode() * 31 + folderId.hashCode()
    }
}

/**
 * Données pour mettre à jour un fichier
 */
data class UpdateFileRequest(
    val fileName: String? = null,
    val content: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UpdateFileRequest
        return fileName == other.fileName && content?.contentEquals(other.content) == true
    }

    override fun hashCode(): Int {
        var result = fileName?.hashCode() ?: 0
        result = 31 * result + (content?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Données de téléchargement d'un fichier
 */
data class FileDownload(
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val content: ByteArray,
    val lastModified: LocalDateTime?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileDownload
        return fileName == other.fileName && content.contentEquals(other.content)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}

/**
 * Statistiques d'un fichier
 */
data class FileStatistics(
    val fileId: Int,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val creationDate: LocalDateTime?,
    val lastModified: LocalDateTime?,
    val ownerId: Int,
    val creatorId: Int,
    val folderId: Int
)

/**
 * Statistiques globales des fichiers d'un utilisateur
 */
data class UserFileStatistics(
    val userId: Int,
    val totalFiles: Int,
    val totalSize: Long,
    val recentFileCount: Int,
    val filesByType: Map<String, Int>
)