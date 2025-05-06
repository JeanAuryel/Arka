package controllers

import ktorm.Category
import ktorm.DefaultFolderTemplate
import repositories.CategoryRepository
import repositories.DefaultFolderTemplateRepository

/**
 * Contrôleur pour gérer les opérations sur les catégories
 */
class CategoryController(
    private val categoryRepository: CategoryRepository,
    private val templateRepository: DefaultFolderTemplateRepository
) {
    /**
     * Récupère toutes les catégories
     */
    fun getAllCategories(): List<Category> {
        return categoryRepository.findAll()
    }

    /**
     * Récupère une catégorie par son ID
     */
    fun getCategoryById(id: Int): Category? {
        return categoryRepository.findById(id)
    }

    /**
     * Récupère une catégorie par son libellé
     */
    fun getCategoryByLabel(label: String): Category? {
        return categoryRepository.findByLabel(label)
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
    fun deleteCategory(id: Int): Boolean {
        return categoryRepository.delete(id) > 0
    }

    /**
     * Récupère les modèles de dossiers par défaut pour une catégorie
     */
    fun getDefaultFolderTemplates(categoryId: Int): List<DefaultFolderTemplate> {
        return templateRepository.findByCategoryId(categoryId)
    }

    /**
     * Récupère tous les modèles de dossiers par défaut
     */
    fun getAllDefaultFolderTemplates(): List<DefaultFolderTemplate> {
        return templateRepository.findAll()
    }
}