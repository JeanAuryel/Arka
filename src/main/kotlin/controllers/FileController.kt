package controllers

import ktorm.File
import repositories.FileRepository
import repositories.FolderRepository
import java.time.LocalDateTime

/**
 * Contrôleur pour gérer les opérations sur les fichiers
 */
class FileController(
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository,
    private val folderController: FolderController
) {
    /**
     * Récupère tous les fichiers d'un dossier
     */
    fun getFilesInFolder(folderID: Int): List<File> {
        return fileRepository.findByFolderId(folderID)
    }

    /**
     * Récupère un fichier par son ID
     */
    fun getFileById(fileID: Int): File? {
        return fileRepository.findById(fileID)
    }

    /**
     * Ajoute un nouveau fichier dans un dossier
     */
    fun addFile(file: File): File? {
        val fileWithDate = file.copy(
            fileCreationDate = LocalDateTime.now()
        )
        return fileRepository.insert(fileWithDate)
    }

    /**
     * Met à jour les informations d'un fichier
     */
    fun updateFile(file: File): Boolean {
        return fileRepository.update(file) > 0
    }

    /**
     * Supprime un fichier
     */
    fun deleteFile(fileID: Int): Boolean {
        return fileRepository.delete(fileID) > 0
    }

    /**
     * Vérifie si un utilisateur a accès à un fichier
     */
    fun hasAccessToFile(memberID: Int, fileID: Int): Boolean {
        val file = fileRepository.findById(fileID) ?: return false

        // Vérifier l'accès au dossier contenant le fichier
        return folderController.hasAccessToFolder(memberID, file.folderID)
    }
}