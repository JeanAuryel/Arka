package controllers

import repositories.CategoryRepository
import repositories.DefaultFolderTemplateRepository
import ktorm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Controller pour la gestion des catégories
 *
 * Responsabilités:
 * - CRUD des catégories
 * - Gestion des relations catégorie-espace
 * - Validation des données catégories
 * - Création automatique de dossiers par défaut
 * - Statistiques et rapports catégories
 * - Gestion de l'organisation hiérarchique
 *
 * Utilisé par: UI Components, DashboardService, FolderController
 * Utilise: CategoryRepository, DefaultFolderTemplateRepository
 *
 * Pattern: Controller Layer + Result Pattern (standardisé avec AuthController)
 */
class CategoryController(
    private val categoryRepository: CategoryRepository,
    private val defaultFolderTemplateRepository: DefaultFolderTemplateRepository
) {

    /**
     * Résultats des opérations sur les catégories - PATTERN STANDARDISÉ
     */
    sealed class CategoryResult<out T> {
        data class Success<T>(val data: T) : CategoryResult<T>()
        data class Error(val message: String, val code: CategoryErrorCode) : CategoryResult<Nothing>()
    }

    enum class CategoryErrorCode {
        CATEGORY_NOT_FOUND,
        SPACE_NOT_FOUND,
        CATEGORY_ALREADY_EXISTS,
        INVALID_INPUT,
        PERMISSION_DENIED,
        HAS_FOLDERS,
        INTERNAL_ERROR
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET MODIFICATION
    // ================================================================

    /**
     * Crée une nouvelle catégorie
     *
     * @param categoryData Données de la nouvelle catégorie
     * @return Résultat de la création
     */
    suspend fun createCategory(categoryData: CreateCategoryRequest): CategoryResult<Categorie> = withContext(Dispatchers.IO) {
        try {
            // Validation des données
            val validationError = validateCategoryData(categoryData)
            if (validationError != null) {
                return@withContext CategoryResult.Error(validationError, CategoryErrorCode.INVALID_INPUT)
            }

            // Vérifier si une catégorie avec ce nom existe déjà dans l'espace
            val existingCategory = categoryRepository.getCategorieByNameAndEspace(
                categoryData.name,
                categoryData.spaceId
            )
            if (existingCategory != null) {
                return@withContext CategoryResult.Error(
                    "Une catégorie avec ce nom existe déjà dans cet espace",
                    CategoryErrorCode.CATEGORY_ALREADY_EXISTS
                )
            }

            // Créer l'entité catégorie
            val categoryEntity = CategorieEntity {
                nomCategorie = categoryData.name.trim()
                descriptionCategorie = categoryData.description?.trim()
                dateCreationCategorie = LocalDateTime.now()
                espaceId = categoryData.spaceId
            }

            // Sauvegarder
            val savedCategory = categoryRepository.create(categoryEntity)

            // Créer automatiquement les dossiers par défaut si demandé
            if (categoryData.createDefaultFolders) {
                createDefaultFoldersForCategory(savedCategory.categorieId)
            }

            return@withContext CategoryResult.Success(savedCategory.toModel())

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors de la création de la catégorie: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Met à jour une catégorie existante
     *
     * @param categoryId ID de la catégorie
     * @param updateData Nouvelles données
     * @return Résultat de la mise à jour
     */
    suspend fun updateCategory(
        categoryId: Int,
        updateData: UpdateCategoryRequest
    ): CategoryResult<Categorie> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la catégorie existe
            val category = categoryRepository.findById(categoryId)
                ?: return@withContext CategoryResult.Error(
                    "Catégorie non trouvée",
                    CategoryErrorCode.CATEGORY_NOT_FOUND
                )

            // Validation des nouvelles données
            val validationError = validateUpdateData(updateData)
            if (validationError != null) {
                return@withContext CategoryResult.Error(validationError, CategoryErrorCode.INVALID_INPUT)
            }

            // Vérifier si le nouveau nom existe déjà (si changé)
            if (updateData.name != null && updateData.name != category.nomCategorie) {
                val existingCategory = categoryRepository.getCategorieByNameAndEspace(
                    updateData.name,
                    category.espaceId
                )
                if (existingCategory != null && existingCategory.categorieId != categoryId) {
                    return@withContext CategoryResult.Error(
                        "Une autre catégorie avec ce nom existe déjà dans cet espace",
                        CategoryErrorCode.CATEGORY_ALREADY_EXISTS
                    )
                }
            }

            // Appliquer les modifications
            updateData.name?.let { category.nomCategorie = it.trim() }
            updateData.description?.let { category.descriptionCategorie = it.trim() }

            // Mettre à jour
            categoryRepository.update(category)

            return@withContext CategoryResult.Success(category.toModel())

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors de la mise à jour: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE CONSULTATION
    // ================================================================

    /**
     * Récupère une catégorie par son ID
     *
     * @param categoryId ID de la catégorie
     * @return Résultat avec la catégorie trouvée
     */
    suspend fun getCategoryById(categoryId: Int): CategoryResult<Categorie> = withContext(Dispatchers.IO) {
        try {
            val category = categoryRepository.findById(categoryId)
                ?: return@withContext CategoryResult.Error(
                    "Catégorie non trouvée",
                    CategoryErrorCode.CATEGORY_NOT_FOUND
                )

            return@withContext CategoryResult.Success(category.toModel())

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors de la récupération: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère toutes les catégories d'un espace
     *
     * @param spaceId ID de l'espace
     * @return Résultat avec la liste des catégories
     */
    suspend fun getCategoriesBySpace(spaceId: Int): CategoryResult<List<Categorie>> = withContext(Dispatchers.IO) {
        try {
            val categories = categoryRepository.getCategoriesByEspace(spaceId)
            val categoryModels = categories.map { it.toModel() }

            return@withContext CategoryResult.Success(categoryModels)

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors de la récupération des catégories: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Recherche des catégories par nom
     *
     * @param searchTerm Terme de recherche
     * @param spaceId ID de l'espace (optionnel)
     * @return Résultat avec les catégories trouvées
     */
    suspend fun searchCategories(
        searchTerm: String,
        spaceId: Int? = null
    ): CategoryResult<List<Categorie>> = withContext(Dispatchers.IO) {
        try {
            if (searchTerm.isBlank()) {
                return@withContext CategoryResult.Error(
                    "Terme de recherche requis",
                    CategoryErrorCode.INVALID_INPUT
                )
            }

            val categories = categoryRepository.searchCategoriesByName(searchTerm, spaceId)
            val categoryModels = categories.map { it.toModel() }

            return@withContext CategoryResult.Success(categoryModels)

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors de la recherche: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère toutes les catégories disponibles
     *
     * @return Résultat avec toutes les catégories
     */
    suspend fun getAllCategories(): CategoryResult<List<Categorie>> = withContext(Dispatchers.IO) {
        try {
            val categories = categoryRepository.findAll()
            val categoryModels = categories.map { it.toModel() }

            return@withContext CategoryResult.Success(categoryModels)

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors de la récupération de toutes les catégories: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION
    // ================================================================

    /**
     * Supprime une catégorie
     * Vérifie qu'elle n'a plus de dossiers avant suppression
     *
     * @param categoryId ID de la catégorie à supprimer
     * @return Résultat de la suppression
     */
    suspend fun deleteCategory(categoryId: Int): CategoryResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la catégorie existe
            val category = categoryRepository.findById(categoryId)
                ?: return@withContext CategoryResult.Error(
                    "Catégorie non trouvée",
                    CategoryErrorCode.CATEGORY_NOT_FOUND
                )

            // Vérifier qu'elle n'a plus de dossiers (implémentation simplifiée)
            // TODO: Implémenter la vérification des dossiers quand FolderRepository sera prêt

            // Supprimer d'abord les modèles de dossiers par défaut
            deleteDefaultFoldersForCategory(categoryId)

            // Supprimer la catégorie
            val deleted = categoryRepository.delete(categoryId)
            if (deleted == 0) {
                return@withContext CategoryResult.Error(
                    "Aucune catégorie supprimée",
                    CategoryErrorCode.INTERNAL_ERROR
                )
            }

            return@withContext CategoryResult.Success(Unit)

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors de la suppression: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE GESTION DES MODÈLES PAR DÉFAUT
    // ================================================================

    /**
     * Récupère les modèles de dossiers par défaut d'une catégorie
     *
     * @param categoryId ID de la catégorie
     * @return Résultat avec les modèles par défaut
     */
    suspend fun getDefaultFolderTemplates(categoryId: Int): CategoryResult<List<ModeleDossierDefaut>> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la catégorie existe
            val category = categoryRepository.findById(categoryId)
                ?: return@withContext CategoryResult.Error(
                    "Catégorie non trouvée",
                    CategoryErrorCode.CATEGORY_NOT_FOUND
                )

            val templates = defaultFolderTemplateRepository.getModelesByCategorie(categoryId)
            val templateModels = templates.map { it.toModel() }

            return@withContext CategoryResult.Success(templateModels)

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors de la récupération des modèles: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Ajoute un modèle de dossier par défaut à une catégorie
     *
     * @param categoryId ID de la catégorie
     * @param templateName Nom du modèle
     * @return Résultat de l'ajout
     */
    suspend fun addDefaultFolderTemplate(
        categoryId: Int,
        templateName: String
    ): CategoryResult<ModeleDossierDefaut> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la catégorie existe
            val category = categoryRepository.findById(categoryId)
                ?: return@withContext CategoryResult.Error(
                    "Catégorie non trouvée",
                    CategoryErrorCode.CATEGORY_NOT_FOUND
                )

            // Valider le nom du modèle
            if (templateName.isBlank() || templateName.length > 100) {
                return@withContext CategoryResult.Error(
                    "Nom de modèle invalide",
                    CategoryErrorCode.INVALID_INPUT
                )
            }

            // Vérifier si un modèle avec ce nom existe déjà
            val existingTemplate = defaultFolderTemplateRepository.getModeleByNameAndCategorie(
                templateName.trim(),
                categoryId
            )
            if (existingTemplate != null) {
                return@withContext CategoryResult.Error(
                    "Un modèle avec ce nom existe déjà dans cette catégorie",
                    CategoryErrorCode.CATEGORY_ALREADY_EXISTS
                )
            }

            // Obtenir l'ordre d'affichage suivant (implémentation simplifiée)
            val existingTemplates = defaultFolderTemplateRepository.getModelesByCategorie(categoryId)
            val nextOrder = existingTemplates.size + 1

            // Créer le modèle
            val templateEntity = ModeleDossierDefautEntity {
                nomModele = templateName.trim()
                categorieId = categoryId
                ordreAffichage = nextOrder
            }

            val savedTemplate = defaultFolderTemplateRepository.create(templateEntity)
            return@withContext CategoryResult.Success(savedTemplate.toModel())

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors de l'ajout du modèle: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques d'une catégorie
     *
     * @param categoryId ID de la catégorie
     * @return Résultat avec les statistiques
     */
    suspend fun getCategoryStatistics(categoryId: Int): CategoryResult<CategoryStatistics> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la catégorie existe
            val category = categoryRepository.findById(categoryId)
                ?: return@withContext CategoryResult.Error(
                    "Catégorie non trouvée",
                    CategoryErrorCode.CATEGORY_NOT_FOUND
                )

            val folderCount = 0 // TODO: Implémenter quand FolderRepository sera prêt
            val fileCount = 0   // TODO: Implémenter quand FileRepository sera prêt
            val totalSize = 0L  // TODO: Implémenter quand FileRepository sera prêt
            val lastActivity: LocalDateTime? = null // TODO: Implémenter
            val defaultTemplateCount = defaultFolderTemplateRepository.getModelesByCategorie(categoryId).size

            val stats = CategoryStatistics(
                categoryId = categoryId,
                categoryName = category.nomCategorie,
                folderCount = folderCount,
                fileCount = fileCount,
                totalSize = totalSize,
                lastActivity = lastActivity,
                defaultTemplateCount = defaultTemplateCount,
                creationDate = category.dateCreationCategorie
            )

            return@withContext CategoryResult.Success(stats)

        } catch (e: Exception) {
            return@withContext CategoryResult.Error(
                "Erreur lors du calcul des statistiques: ${e.message}",
                CategoryErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES
    // ================================================================

    private suspend fun createDefaultFoldersForCategory(categoryId: Int) {
        // Logique pour créer automatiquement les dossiers par défaut
        // basée sur les modèles prédéfinis
        try {
            val predefinedTemplates = listOf(
                "Documents personnels",
                "Photos",
                "Certificats",
                "Factures",
                "Correspondance"
            )

            predefinedTemplates.forEachIndexed { index, templateName ->
                val templateEntity = ModeleDossierDefautEntity {
                    nomModele = templateName
                    categorieId = categoryId
                    ordreAffichage = index + 1
                }
                defaultFolderTemplateRepository.create(templateEntity)
            }
        } catch (e: Exception) {
            // Log l'erreur mais ne fait pas échouer la création de catégorie
            println("Erreur lors de la création des dossiers par défaut: ${e.message}")
        }
    }

    private suspend fun deleteDefaultFoldersForCategory(categoryId: Int) {
        try {
            // Supprimer tous les modèles de cette catégorie
            val templates = defaultFolderTemplateRepository.getModelesByCategorie(categoryId)
            templates.forEach { template ->
                defaultFolderTemplateRepository.delete(template.modeleId)
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression des modèles par défaut: ${e.message}")
        }
    }

    private fun validateCategoryData(categoryData: CreateCategoryRequest): String? {
        return when {
            categoryData.name.isBlank() -> "Le nom de la catégorie ne peut pas être vide"
            categoryData.name.length < 2 -> "Le nom de la catégorie doit contenir au moins 2 caractères"
            categoryData.name.length > 100 -> "Le nom de la catégorie ne peut pas dépasser 100 caractères"
            categoryData.description != null && categoryData.description.length > 500 ->
                "La description ne peut pas dépasser 500 caractères"
            categoryData.spaceId <= 0 -> "ID d'espace invalide"
            else -> null
        }
    }

    private fun validateUpdateData(updateData: UpdateCategoryRequest): String? {
        return when {
            updateData.name != null && updateData.name.isBlank() ->
                "Le nom de la catégorie ne peut pas être vide"
            updateData.name != null && updateData.name.length < 2 ->
                "Le nom de la catégorie doit contenir au moins 2 caractères"
            updateData.name != null && updateData.name.length > 100 ->
                "Le nom de la catégorie ne peut pas dépasser 100 caractères"
            updateData.description != null && updateData.description.length > 500 ->
                "La description ne peut pas dépasser 500 caractères"
            else -> null
        }
    }
}

// ================================================================
// DATA CLASSES POUR LES REQUÊTES ET RÉSULTATS
// ================================================================

/**
 * Données pour créer une nouvelle catégorie
 */
data class CreateCategoryRequest(
    val name: String,
    val description: String? = null,
    val spaceId: Int,
    val createDefaultFolders: Boolean = true
)

/**
 * Données pour mettre à jour une catégorie
 */
data class UpdateCategoryRequest(
    val name: String? = null,
    val description: String? = null
)

/**
 * Statistiques d'une catégorie
 */
data class CategoryStatistics(
    val categoryId: Int,
    val categoryName: String,
    val folderCount: Int,
    val fileCount: Int,
    val totalSize: Long,
    val lastActivity: LocalDateTime?,
    val defaultTemplateCount: Int,
    val creationDate: LocalDateTime?
)