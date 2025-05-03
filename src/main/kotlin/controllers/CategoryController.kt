package controllers

import ktorm.Category
import ktorm.DatabaseManager
import repositories.CategoryRepository

/**
 * Contrôleur pour gérer les opérations sur les catégories
 */
class CategoryController {
    private val database = DatabaseManager.getInstance()
    private val categoryRepository = CategoryRepository(database)

    /**
     * Récupère toutes les catégories
     */
    fun getAllCategories(): List<Category> {
        return categoryRepository.findAll()
    }

    /**
     * Récupère une catégorie par son ID
     */
    fun getCategoryById(categoryID: Int): Category? {
        return categoryRepository.findById(categoryID)
    }

    /**
     * Ajoute une nouvelle catégorie
     */
    fun addCategory(category: Category): Category? {
        return categoryRepository.insert(category)
    }

    /**
     * Met à jour une catégorie
     */
    fun updateCategory(category: Category): Boolean {
        return categoryRepository.update(category) > 0
    }

    /**
     * Supprime une catégorie
     */
    fun deleteCategory(categoryID: Int): Boolean {
        // Vérifier si la catégorie a des dossiers associés avant de la supprimer
        val hasAssociatedFolders = categoryRepository.hasAssociatedFolders(categoryID)
        if (hasAssociatedFolders) {
            return false
        }
        return categoryRepository.delete(categoryID) > 0
    }
}