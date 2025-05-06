package controllers

import ktorm.Categories
import ktorm.DefaultFolderTemplate
import ktorm.Folder
import repositories.DefaultFolderTemplateRepository
import repositories.FamilyMemberRepository
import repositories.FolderRepository
import java.time.LocalDateTime

/**
 * Contrôleur pour gérer les opérations sur les dossiers
 */
class FolderController(
    private val folderRepository: FolderRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val templateRepository: DefaultFolderTemplateRepository,
    private val authController: AuthController
) {
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
        // Vérifier s'il y a des dossiers existants pour cette catégorie
        val existingFolders = folderRepository.findByMemberAndCategory(userId, categoryId)

        // Si aucun dossier n'existe pour cette catégorie, créer les dossiers par défaut
        if (existingFolders.isEmpty()) {
            return createDefaultFoldersForCategory(userId, categoryId)
        }

        return existingFolders
    }

    /**
     * Crée les dossiers par défaut pour une catégorie
     */
    private fun createDefaultFoldersForCategory(userId: Int, categoryId: Int): List<Folder> {
        val templates = templateRepository.findByCategoryId(categoryId)
        val createdFolders = mutableListOf<Folder>()

        templates.forEach { template ->
            val folder = Folder(
                folderName = template.templateName,
                folderCreationDate = LocalDateTime.now(),
                parentFolderID = null,
                familyMemberID = userId,
                categoryID = categoryId
            )

            val createdFolder = createFolder(folder)
            if (createdFolder != null) {
                createdFolders.add(createdFolder)
            }
        }

        return createdFolders
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

    /**
     * Initialise les dossiers par défaut pour un nouveau membre
     */
    fun initializeDefaultFoldersForMember(memberID: Int, categoryController: CategoryController) {
        // Récupérer toutes les catégories existantes
        val categories = categoryController.getAllCategories()

        // Pour chaque catégorie, créer les dossiers par défaut
        categories.forEach { category ->
            val categoryId = category.categoryID
            if (categoryId != null) {
                createDefaultFoldersForCategory(memberID, categoryId)
            }
        }
    }

    /**
     * Récupère les modèles de dossiers par défaut pour une catégorie
     */
    fun getDefaultFolderTemplatesForCategory(categoryId: Int): List<DefaultFolderTemplate> {
        return templateRepository.findByCategoryId(categoryId)
    }

    /**
     * Organise les dossiers d'un membre par catégorie
     */
    fun getFoldersByCategories(memberID: Int): Map<Int, List<Folder>> {
        val folders = getAllFoldersForMember(memberID)
        return folders.groupBy { folder -> folder.categoryID ?: 0 }
    }

    /**
     * Récupère le nombre de dossiers par catégorie pour un membre
     */
    fun getFolderCountByCategory(memberID: Int): Map<Int, Int> {
        val foldersByCategory = getFoldersByCategories(memberID)
        return foldersByCategory.mapValues { entry -> entry.value.size }
    }
}