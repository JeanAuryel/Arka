package controllers

import ktorm.*
import repositories.*
import java.time.LocalDateTime

/**
 * Controller pour la gestion des fichiers avec système de permissions
 * Orchestre les opérations métier complexes sur les fichiers
 */
class FileController(
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository,
    private val permissionRepository: PermissionRepository
) {

    /**
     * Upload un fichier avec vérifications de permissions
     * @param fileData Les données du fichier
     * @param uploaderUserId L'ID de l'utilisateur qui upload
     * @return Le résultat de l'upload
     */
    fun uploadFile(fileData: UploadFileData, uploaderUserId: Int): FileUploadResult {
        return try {
            // 1. Vérifier que le dossier de destination existe
            val folder = folderRepository.findById(fileData.dossierId)
            if (folder == null) {
                return FileUploadResult.Error("Dossier de destination non trouvé")
            }

            // 2. Vérifier les permissions d'écriture sur le dossier
            if (!hasWritePermission(uploaderUserId, fileData.dossierId)) {
                return FileUploadResult.Error("Permissions insuffisantes pour écrire dans ce dossier")
            }

            // 3. Valider la taille et le type de fichier
            val validationErrors = validateFileUpload(fileData)
            if (validationErrors.isNotEmpty()) {
                return FileUploadResult.ValidationError(validationErrors)
            }

            // 4. Créer le fichier
            val createFileData = CreateFileData(
                nomFichier = fileData.nomFichier,
                typeFichier = extractFileExtension(fileData.nomFichier),
                tailleFichier = fileData.contenuFichier?.size?.toLong() ?: 0L,
                contenuFichier = fileData.contenuFichier,
                cheminFichier = fileData.cheminFichier,
                createurId = uploaderUserId,
                proprietaireId = fileData.proprietaireId ?: uploaderUserId,
                dossierId = fileData.dossierId
            )

            val result = fileRepository.createFile(createFileData)

            when (result) {
                is RepositoryResult.Success -> {
                    FileUploadResult.Success(
                        fichier = result.data,
                        message = "Fichier uploadé avec succès"
                    )
                }
                is RepositoryResult.Error -> FileUploadResult.Error(result.message)
                is RepositoryResult.ValidationError -> FileUploadResult.ValidationError(result.errors)
            }
        } catch (e: Exception) {
            FileUploadResult.Error("Erreur lors de l'upload: ${e.message}")
        }
    }

    /**
     * Télécharge un fichier avec vérifications de permissions
     * @param fichierId L'ID du fichier
     * @param downloaderUserId L'ID de l'utilisateur qui télécharge
     * @return Le résultat du téléchargement
     */
    fun downloadFile(fichierId: Int, downloaderUserId: Int): FileDownloadResult {
        return try {
            // 1. Vérifier que le fichier existe
            val fichier = fileRepository.findById(fichierId)
            if (fichier == null) {
                return FileDownloadResult.Error("Fichier non trouvé")
            }

            // 2. Vérifier les permissions de lecture
            if (!hasReadPermission(downloaderUserId, fichier.dossierId)) {
                return FileDownloadResult.Error("Permissions insuffisantes pour lire ce fichier")
            }

            // 3. Retourner le fichier
            FileDownloadResult.Success(
                fichier = fichier.toModel(),
                contenu = fichier.contenuFichier,
                chemin = fichier.cheminFichier
            )
        } catch (e: Exception) {
            FileDownloadResult.Error("Erreur lors du téléchargement: ${e.message}")
        }
    }

    /**
     * Supprime un fichier avec vérifications de permissions
     * @param fichierId L'ID du fichier
     * @param deleterUserId L'ID de l'utilisateur qui supprime
     * @return Le résultat de la suppression
     */
    fun deleteFile(fichierId: Int, deleterUserId: Int): FileDeleteResult {
        return try {
            // 1. Vérifier que le fichier existe
            val fichier = fileRepository.findById(fichierId)
            if (fichier == null) {
                return FileDeleteResult.Error("Fichier non trouvé")
            }

            // 2. Vérifier les permissions de suppression
            if (!hasDeletePermission(deleterUserId, fichier.dossierId, fichier.proprietaireId)) {
                return FileDeleteResult.Error("Permissions insuffisantes pour supprimer ce fichier")
            }

            // 3. Supprimer le fichier
            val deleted = fileRepository.deleteById(fichierId)

            if (deleted) {
                FileDeleteResult.Success("Fichier supprimé avec succès")
            } else {
                FileDeleteResult.Error("Échec de la suppression")
            }
        } catch (e: Exception) {
            FileDeleteResult.Error("Erreur lors de la suppression: ${e.message}")
        }
    }

    /**
     * Déplace un fichier vers un autre dossier
     * @param fichierId L'ID du fichier
     * @param nouveauDossierId L'ID du nouveau dossier
     * @param moverUserId L'ID de l'utilisateur qui déplace
     * @return Le résultat du déplacement
     */
    fun moveFile(fichierId: Int, nouveauDossierId: Int, moverUserId: Int): FileMoveResult {
        return try {
            // 1. Vérifier que le fichier existe
            val fichier = fileRepository.findById(fichierId)
            if (fichier == null) {
                return FileMoveResult.Error("Fichier non trouvé")
            }

            // 2. Vérifier que le nouveau dossier existe
            val nouveauDossier = folderRepository.findById(nouveauDossierId)
            if (nouveauDossier == null) {
                return FileMoveResult.Error("Dossier de destination non trouvé")
            }

            // 3. Vérifier les permissions sur l'ancien dossier (écriture pour déplacer)
            if (!hasWritePermission(moverUserId, fichier.dossierId)) {
                return FileMoveResult.Error("Permissions insuffisantes sur le dossier source")
            }

            // 4. Vérifier les permissions sur le nouveau dossier (écriture pour ajouter)
            if (!hasWritePermission(moverUserId, nouveauDossierId)) {
                return FileMoveResult.Error("Permissions insuffisantes sur le dossier de destination")
            }

            // 5. Déplacer le fichier
            val result = fileRepository.moveFile(fichierId, nouveauDossierId)

            when (result) {
                is RepositoryResult.Success -> {
                    FileMoveResult.Success(
                        fichier = result.data,
                        message = "Fichier déplacé avec succès"
                    )
                }
                is RepositoryResult.Error -> FileMoveResult.Error(result.message)
                is RepositoryResult.ValidationError -> FileMoveResult.Error(result.errors.joinToString(", "))
            }
        } catch (e: Exception) {
            FileMoveResult.Error("Erreur lors du déplacement: ${e.message}")
        }
    }

    /**
     * Partage un fichier avec un autre utilisateur
     * @param fichierId L'ID du fichier
     * @param beneficiaireId L'ID du bénéficiaire
     * @param sharerUserId L'ID de l'utilisateur qui partage
     * @param typePermission Le type de permission à accorder
     * @param dateExpiration Date d'expiration optionnelle
     * @return Le résultat du partage
     */
    fun shareFile(
        fichierId: Int,
        beneficiaireId: Int,
        sharerUserId: Int,
        typePermission: TypePermission,
        dateExpiration: LocalDateTime? = null
    ): FileShareResult {
        return try {
            // 1. Vérifier que le fichier existe
            val fichier = fileRepository.findById(fichierId)
            if (fichier == null) {
                return FileShareResult.Error("Fichier non trouvé")
            }

            // 2. Vérifier que l'utilisateur peut partager (propriétaire ou admin)
            if (!canShareFile(sharerUserId, fichier.proprietaireId, fichier.dossierId)) {
                return FileShareResult.Error("Permissions insuffisantes pour partager ce fichier")
            }

            // 3. Créer la permission
            val permissionData = CreatePermissionData(
                proprietaireId = fichier.proprietaireId,
                beneficiaireId = beneficiaireId,
                portee = PorteePermission.FICHIER,
                cibleId = fichierId,
                typePermission = typePermission,
                dateExpiration = dateExpiration
            )

            val result = permissionRepository.createPermission(permissionData)

            when (result) {
                is RepositoryResult.Success -> {
                    FileShareResult.Success(
                        permission = result.data,
                        message = "Fichier partagé avec succès"
                    )
                }
                is RepositoryResult.Error -> FileShareResult.Error(result.message)
                is RepositoryResult.ValidationError -> FileShareResult.Error(result.errors.joinToString(", "))
            }
        } catch (e: Exception) {
            FileShareResult.Error("Erreur lors du partage: ${e.message}")
        }
    }

    /**
     * Obtient la liste des fichiers accessibles par un utilisateur
     * @param userId L'ID de l'utilisateur
     * @param dossierId Optionnel: limiter à un dossier
     * @return La liste des fichiers avec permissions
     */
    fun getAccessibleFiles(userId: Int, dossierId: Int? = null): List<FileWithPermissions> {
        return try {
            val files = if (dossierId != null) {
                fileRepository.findByFolder(dossierId)
            } else {
                fileRepository.findByOwner(userId)
            }

            files.mapNotNull { fichier ->
                val permissions = getUserFilePermissions(userId, fichier.fichierId, fichier.dossierId)
                if (permissions.canRead) {
                    FileWithPermissions(
                        fichier = fichier.toModel(),
                        permissions = permissions
                    )
                } else null
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la récupération des fichiers accessibles: ${e.message}")
            emptyList()
        }
    }

    /**
     * Recherche des fichiers avec permissions
     * @param query La requête de recherche
     * @param userId L'ID de l'utilisateur qui recherche
     * @param typeFilter Filtre par type de fichier (optionnel)
     * @return Les résultats de recherche
     */
    fun searchFiles(query: String, userId: Int, typeFilter: String? = null): List<FileWithPermissions> {
        return try {
            val searchResults = fileRepository.searchByName(query, null, typeFilter)

            searchResults.mapNotNull { fichier ->
                val permissions = getUserFilePermissions(userId, fichier.fichierId, fichier.dossierId)
                if (permissions.canRead) {
                    FileWithPermissions(
                        fichier = fichier.toModel(),
                        permissions = permissions
                    )
                } else null
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche de fichiers: ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtient les statistiques de fichiers d'un utilisateur
     * @param userId L'ID de l'utilisateur
     * @return Les statistiques détaillées
     */
    fun getUserFileStatistics(userId: Int): UserFileStatistics {
        return try {
            val ownedStats = fileRepository.getFileStats(userId)
            val sharedWithUser = getSharedFilesCount(userId)
            val recentFiles = fileRepository.getRecentFiles(userId, 5)
            val largeFiles = fileRepository.getLargeFiles(userId, 10_000_000) // > 10MB

            UserFileStatistics(
                ownedFiles = ownedStats.totalFiles,
                totalSize = ownedStats.totalSize,
                sharedWithUser = sharedWithUser,
                recentFiles = recentFiles.map { it.toModel() },
                largeFiles = largeFiles.map { it.toModel() },
                typeDistribution = ownedStats.typeStats.associate { it.type to it.count }
            )
        } catch (e: Exception) {
            println("⚠️ Erreur lors du calcul des statistiques: ${e.message}")
            UserFileStatistics(0, 0L, 0, emptyList(), emptyList(), emptyMap())
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES - VÉRIFICATIONS DE PERMISSIONS
    // ================================================================

    /**
     * Vérifie si un utilisateur a les permissions de lecture sur un dossier
     */
    private fun hasReadPermission(userId: Int, dossierId: Int): Boolean {
        return try {
            // Vérifier si l'utilisateur est propriétaire du dossier
            val dossier = folderRepository.findById(dossierId)
            if (dossier?.membreFamilleId == userId) return true

            // Vérifier les permissions déléguées
            permissionRepository.hasPermission(
                beneficiaireId = userId,
                portee = PorteePermission.DOSSIER,
                cibleId = dossierId,
                typePermission = TypePermission.LECTURE
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un utilisateur a les permissions d'écriture sur un dossier
     */
    private fun hasWritePermission(userId: Int, dossierId: Int): Boolean {
        return try {
            val dossier = folderRepository.findById(dossierId)
            if (dossier?.membreFamilleId == userId) return true

            permissionRepository.hasPermission(
                beneficiaireId = userId,
                portee = PorteePermission.DOSSIER,
                cibleId = dossierId,
                typePermission = TypePermission.ECRITURE
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un utilisateur peut supprimer un fichier
     */
    private fun hasDeletePermission(userId: Int, dossierId: Int, fileOwnerId: Int): Boolean {
        return try {
            // Propriétaire du fichier peut toujours supprimer
            if (fileOwnerId == userId) return true

            // Propriétaire du dossier peut supprimer
            val dossier = folderRepository.findById(dossierId)
            if (dossier?.membreFamilleId == userId) return true

            // Permission de suppression déléguée
            permissionRepository.hasPermission(
                beneficiaireId = userId,
                portee = PorteePermission.DOSSIER,
                cibleId = dossierId,
                typePermission = TypePermission.SUPPRESSION
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un utilisateur peut partager un fichier
     */
    private fun canShareFile(userId: Int, fileOwnerId: Int, dossierId: Int): Boolean {
        return try {
            // Propriétaire peut toujours partager
            if (fileOwnerId == userId) return true

            // Permissions d'accès complet
            permissionRepository.hasPermission(
                beneficiaireId = userId,
                portee = PorteePermission.DOSSIER,
                cibleId = dossierId,
                typePermission = TypePermission.ACCES_COMPLET
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtient les permissions d'un utilisateur sur un fichier
     */
    private fun getUserFilePermissions(userId: Int, fichierId: Int, dossierId: Int): FilePermissions {
        return try {
            val canRead = hasReadPermission(userId, dossierId)
            val canWrite = hasWritePermission(userId, dossierId)
            val canDelete = hasDeletePermission(userId, dossierId, userId) // Approximation
            val canShare = canShareFile(userId, userId, dossierId) // Approximation

            FilePermissions(
                canRead = canRead,
                canWrite = canWrite,
                canDelete = canDelete,
                canShare = canShare
            )
        } catch (e: Exception) {
            FilePermissions(false, false, false, false)
        }
    }

    /**
     * Obtient le nombre de fichiers partagés avec un utilisateur
     */
    private fun getSharedFilesCount(userId: Int): Int {
        return try {
            val permissions = permissionRepository.findActiveByBeneficiary(userId)
            permissions.count { it.portee == PorteePermission.FICHIER.name }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Valide un upload de fichier
     */
    private fun validateFileUpload(fileData: UploadFileData): List<String> {
        val errors = mutableListOf<String>()

        if (fileData.nomFichier.isBlank()) {
            errors.add("Le nom du fichier ne peut pas être vide")
        }

        if (fileData.contenuFichier == null || fileData.contenuFichier.isEmpty()) {
            errors.add("Le contenu du fichier ne peut pas être vide")
        }

        // Vérifier la taille (100MB max)
        if (fileData.contenuFichier != null && fileData.contenuFichier.size > 100_000_000) {
            errors.add("Le fichier dépasse la taille maximale autorisée (100 MB)")
        }

        return errors
    }

    /**
     * Extrait l'extension d'un fichier
     */
    private fun extractFileExtension(nomFichier: String): String {
        return nomFichier.substringAfterLast('.', "").lowercase()
    }
}

// ================================================================
// CLASSES DE DONNÉES POUR LE CONTROLLER
// ================================================================

/**
 * Données pour upload de fichier
 */
data class UploadFileData(
    val nomFichier: String,
    val contenuFichier: ByteArray?,
    val cheminFichier: String?,
    val dossierId: Int,
    val proprietaireId: Int? = null // Si null, l'uploader devient propriétaire
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UploadFileData
        return nomFichier == other.nomFichier &&
                contenuFichier?.contentEquals(other.contenuFichier) == true &&
                cheminFichier == other.cheminFichier &&
                dossierId == other.dossierId &&
                proprietaireId == other.proprietaireId
    }

    override fun hashCode(): Int {
        var result = nomFichier.hashCode()
        result = 31 * result + (contenuFichier?.contentHashCode() ?: 0)
        result = 31 * result + (cheminFichier?.hashCode() ?: 0)
        result = 31 * result + dossierId
        result = 31 * result + (proprietaireId ?: 0)
        return result
    }
}

/**
 * Permissions d'un utilisateur sur un fichier
 */
data class FilePermissions(
    val canRead: Boolean,
    val canWrite: Boolean,
    val canDelete: Boolean,
    val canShare: Boolean
)

/**
 * Fichier avec permissions
 */
data class FileWithPermissions(
    val fichier: Fichier,
    val permissions: FilePermissions
)

/**
 * Statistiques de fichiers d'un utilisateur
 */
data class UserFileStatistics(
    val ownedFiles: Int,
    val totalSize: Long,
    val sharedWithUser: Int,
    val recentFiles: List<Fichier>,
    val largeFiles: List<Fichier>,
    val typeDistribution: Map<String, Int>
)

// ================================================================
// RÉSULTATS D'OPÉRATIONS
// ================================================================

sealed class FileUploadResult {
    data class Success(val fichier: Fichier, val message: String) : FileUploadResult()
    data class Error(val message: String) : FileUploadResult()
    data class ValidationError(val errors: List<String>) : FileUploadResult()
}

sealed class FileDownloadResult {
    data class Success(val fichier: Fichier, val contenu: ByteArray?, val chemin: String?) : FileDownloadResult()
    data class Error(val message: String) : FileDownloadResult()
}

sealed class FileDeleteResult {
    data class Success(val message: String) : FileDeleteResult()
    data class Error(val message: String) : FileDeleteResult()
}

sealed class FileMoveResult {
    data class Success(val fichier: Fichier, val message: String) : FileMoveResult()
    data class Error(val message: String) : FileMoveResult()
}

sealed class FileShareResult {
    data class Success(val permission: PermissionActive, val message: String) : FileShareResult()
    data class Error(val message: String) : FileShareResult()
}