package controllers

import ktorm.DatabaseManager
import ktorm.Folder
import repositories.FamilyMemberRepository
import repositories.FolderRepository
import java.time.LocalDateTime

/**
 * Contrôleur pour gérer les opérations sur les dossiers
 */
class FolderController {
    private val database = DatabaseManager.getInstance()
    private val folderRepository = FolderRepository(database)
    private val familyMemberRepository = FamilyMemberRepository(database)
    private val authController = AuthController()

    /**
     * Récupère tous les dossiers d'un membre de famille
     */
    fun getAllFoldersForMember(memberID: Int): List<Folder> {
        return folderRepository.findAllByMemberId(memberID)
    }

    /**
     * Récupère les dossiers d'un membre par catégorie
     */
    fun getFoldersByCategory(userId: Int, categoryId: Int): List<Folder> {
        return folderRepository.findByMemberAndCategory(userId, categoryId)
    }

    /**
     * Récupère les dossiers racines (sans parents) d'un membre
     */
    fun getRootFoldersForMember(memberID: Int): List<Folder> {
        return folderRepository.findRootFoldersByMemberId(memberID)
    }

    /**
     * Récupère les sous-dossiers d'un dossier
     */
    fun getSubFolders(folderID: Int): List<Folder> {
        return folderRepository.findSubFolders(folderID)
    }

    /**
     * Récupère un dossier par son ID
     */
    fun getFolderById(folderID: Int): Folder? {
        return folderRepository.findById(folderID)
    }

    /**
     * Crée un nouveau dossier
     */
    fun createFolder(folder: Folder): Folder? {
        val folderWithDate = folder.copy(
            folderCreationDate = LocalDateTime.now()
        )
        return folderRepository.insert(folderWithDate)
    }

    /**
     * Met à jour un dossier
     */
    fun updateFolder(folder: Folder): Boolean {
        return folderRepository.update(folder) > 0
    }

    /**
     * Supprime un dossier et tous ses sous-dossiers
     */
    fun deleteFolder(folderID: Int): Boolean {
        return folderRepository.delete(folderID) > 0
    }

    /**
     * Vérifie si un utilisateur a accès à un dossier (en tant que propriétaire ou parent)
     */
    fun hasAccessToFolder(memberID: Int, folderID: Int): Boolean {
        val folder = folderRepository.findById(folderID) ?: return false

        // Si l'utilisateur est le propriétaire du dossier
        if (folder.familyMemberID == memberID) {
            return true
        }

        // Si l'utilisateur est un parent, il peut accéder aux dossiers des membres de sa famille
        if (authController.isParent(memberID)) {
            val ownerMember = folder.familyMember
            if (ownerMember != null) {
                // Vérifier si le parent et le propriétaire sont dans la même famille
                return ownerMember.familyID == familyMemberRepository.findById(memberID)?.familyID
            }
        }

        return false
    }
}