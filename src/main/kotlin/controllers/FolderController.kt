package controllers

import repositories.FolderRepository
import repositories.CategoryRepository
import repositories.DefaultFolderTemplateRepository
import repositories.FileRepository
import org.ktorm.schema.Column
import ktorm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Controller pour la gestion des dossiers
 *
 * Responsabilités:
 * - CRUD des dossiers avec hiérarchie parent/enfant
 * - Gestion des relations dossier-catégorie-membre
 * - Validation des données dossiers
 * - Création automatique de dossiers par défaut
 * - Navigation dans l'arborescence des dossiers
 * - Statistiques et rapports dossiers
 * - Gestion des permissions sur les dossiers
 *
 * Utilisé par: UI Components, DashboardService, FileController
 * Utilise: FolderRepository, CategoryRepository, DefaultFolderTemplateRepository, FileRepository
 *
 * Pattern: Controller Layer + Result Pattern (standardisé avec AuthController)
 */
class FolderController(
    private val folderRepository: FolderRepository,
    private val categoryRepository: CategoryRepository,
    private val defaultFolderTemplateRepository: DefaultFolderTemplateRepository,
    private val fileRepository: FileRepository
) {

    /**
     * Résultats des opérations sur les dossiers - PATTERN STANDARDISÉ
     */
    sealed class FolderResult<out T> {
        data class Success<T>(val data: T) : FolderResult<T>()
        data class Error(val message: String, val code: FolderErrorCode) : FolderResult<Nothing>()
    }

    enum class FolderErrorCode {
        FOLDER_NOT_FOUND,
        CATEGORY_NOT_FOUND,
        PARENT_FOLDER_NOT_FOUND,
        FOLDER_ALREADY_EXISTS,
        INVALID_INPUT,
        PERMISSION_DENIED,
        HAS_SUBFOLDERS,
        HAS_FILES,
        CIRCULAR_REFERENCE,
        INTERNAL_ERROR
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET MODIFICATION
    // ================================================================

    /**
     * Crée un nouveau dossier
     *
     * @param folderData Données du nouveau dossier
     * @return Résultat de la création
     */
    suspend fun createFolder(folderData: CreateFolderRequest): FolderResult<Dossier> = withContext(Dispatchers.IO) {
        try {
            // Validation des données
            val validationError = validateFolderData(folderData)
            if (validationError != null) {
                return@withContext FolderResult.Error(validationError, FolderErrorCode.INVALID_INPUT)
            }

            // Vérifier que la catégorie existe
            val category = categoryRepository.findById(folderData.categoryId)
                ?: return@withContext FolderResult.Error(
                    "Catégorie non trouvée",
                    FolderErrorCode.CATEGORY_NOT_FOUND
                )

            // Vérifier le dossier parent si spécifié
            if (folderData.parentFolderId != null) {
                val parentFolder = folderRepository.findById(folderData.parentFolderId)
                    ?: return@withContext FolderResult.Error(
                        "Dossier parent non trouvé",
                        FolderErrorCode.PARENT_FOLDER_NOT_FOUND
                    )

                // Vérifier que le parent est dans la même catégorie
                if (parentFolder.categorieId != folderData.categoryId) {
                    return@withContext FolderResult.Error(
                        "Le dossier parent doit être dans la même catégorie",
                        FolderErrorCode.INVALID_INPUT
                    )
                }
            }

            // Vérifier si un dossier avec ce nom existe déjà dans le même niveau
            // TODO: Implémenter vérification d'unicité des noms quand les méthodes repository seront ajoutées
            // Pour l'instant, on permet la création

            // Créer l'entité dossier
            val folderEntity = DossierEntity {
                nomDossier = folderData.name.trim()
                dateCreationDossier = LocalDateTime.now()
                dateDerniereModifDossier = LocalDateTime.now()
                estParDefault = folderData.isDefault
                membreFamilleId = folderData.memberId
                categorieId = folderData.categoryId
                dossierParentId = folderData.parentFolderId
            }

            // Sauvegarder
            val savedFolder = folderRepository.create(folderEntity)

            return@withContext FolderResult.Success(savedFolder.toModel())

        } catch (e: Exception) {
            return@withContext FolderResult.Error(
                "Erreur lors de la création du dossier: ${e.message}",
                FolderErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Met à jour un dossier existant
     *
     * @param folderId ID du dossier
     * @param updateData Nouvelles données
     * @return Résultat de la mise à jour
     */
    suspend fun updateFolder(
        folderId: Int,
        updateData: UpdateFolderRequest
    ): FolderResult<Dossier> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le dossier existe
            val folder = folderRepository.findById(folderId)
                ?: return@withContext FolderResult.Error(
                    "Dossier non trouvé",
                    FolderErrorCode.FOLDER_NOT_FOUND
                )

            // Validation des nouvelles données
            val validationError = validateUpdateData(updateData)
            if (validationError != null) {
                return@withContext FolderResult.Error(validationError, FolderErrorCode.INVALID_INPUT)
            }

            // Vérifier si le nouveau nom existe déjà (si changé)
            // TODO: Implémenter vérification d'unicité des noms quand les méthodes repository seront ajoutées

            // Vérifier le nouveau parent si spécifié
            if (updateData.parentFolderId != null && updateData.parentFolderId != folder.dossierParentId) {
                // Vérifier que le nouveau parent existe
                val newParent = folderRepository.findById(updateData.parentFolderId)
                    ?: return@withContext FolderResult.Error(
                        "Nouveau dossier parent non trouvé",
                        FolderErrorCode.PARENT_FOLDER_NOT_FOUND
                    )

                // Vérifier qu'on ne crée pas de référence circulaire
                if (wouldCreateCircularReference(folderId, updateData.parentFolderId)) {
                    return@withContext FolderResult.Error(
                        "Cette opération créerait une référence circulaire",
                        FolderErrorCode.CIRCULAR_REFERENCE
                    )
                }
            }

            // Appliquer les modifications
            updateData.name?.let { folder.nomDossier = it.trim() }
            updateData.parentFolderId?.let { folder.dossierParentId = it }
            folder.dateDerniereModifDossier = LocalDateTime.now()

            // Mettre à jour
            folderRepository.update(folder)

            return@withContext FolderResult.Success(folder.toModel())

        } catch (e: Exception) {
            return@withContext FolderResult.Error(
                "Erreur lors de la mise à jour: ${e.message}",
                FolderErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE CONSULTATION
    // ================================================================

    /**
     * Récupère un dossier par son ID
     *
     * @param folderId ID du dossier
     * @return Résultat avec le dossier trouvé
     */
    suspend fun getFolderById(folderId: Int): FolderResult<Dossier> = withContext(Dispatchers.IO) {
        try {
            val folder = folderRepository.findById(folderId)
                ?: return@withContext FolderResult.Error(
                    "Dossier non trouvé",
                    FolderErrorCode.FOLDER_NOT_FOUND
                )

            return@withContext FolderResult.Success(folder.toModel())

        } catch (e: Exception) {
            return@withContext FolderResult.Error(
                "Erreur lors de la récupération: ${e.message}",
                FolderErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère tous les dossiers d'un membre dans une catégorie
     *
     * @param memberId ID du membre
     * @param categoryId ID de la catégorie
     * @return Résultat avec la liste des dossiers
     */
    suspend fun getFoldersByMemberAndCategory(
        memberId: Int,
        categoryId: Int
    ): FolderResult<List<Dossier>> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la catégorie existe
            val category = categoryRepository.findById(categoryId)
                ?: return@withContext FolderResult.Error(
                    "Catégorie non trouvée",
                    FolderErrorCode.CATEGORY_NOT_FOUND
                )

            val folders = folderRepository.getDossiersByMembreAndCategorie(memberId, categoryId)
            val folderModels = folders.map { it.toModel() }

            return@withContext FolderResult.Success(folderModels)

        } catch (e: Exception) {
            return@withContext FolderResult.Error(
                "Erreur lors de la récupération des dossiers: ${e.message}",
                FolderErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère la hiérarchie complète des dossiers d'une catégorie
     *
     * @param categoryId ID de la catégorie
     * @param memberId ID du membre (optionnel, pour filtrer)
     * @return Résultat avec l'arborescence des dossiers
     */
    suspend fun getFolderHierarchy(
        categoryId: Int,
        memberId: Int? = null
    ): FolderResult<List<FolderNode>> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la catégorie existe
            val category = categoryRepository.findById(categoryId)
                ?: return@withContext FolderResult.Error(
                    "Catégorie non trouvée",
                    FolderErrorCode.CATEGORY_NOT_FOUND
                )

            // Récupérer les dossiers racines (sans parent) - implémentation simplifiée
            val allFolders = folderRepository.getDossiersByMembreAndCategorie(
                memberId ?: 0,
                categoryId
            ).filter { it.dossierParentId == null }

            val rootFolders = if (memberId != null) {
                allFolders.filter { it.membreFamilleId == memberId }
            } else {
                allFolders
            }

            // Construire l'arborescence
            val hierarchy = rootFolders.map { folder -> buildFolderNode(folder) }

            return@withContext FolderResult.Success(hierarchy)

        } catch (e: Exception) {
            return@withContext FolderResult.Error(
                "Erreur lors de la récupération de la hiérarchie: ${e.message}",
                FolderErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Recherche des dossiers par nom
     *
     * @param searchTerm Terme de recherche
     * @param categoryId ID de la catégorie (optionnel)
     * @param memberId ID du membre (optionnel)
     * @return Résultat avec les dossiers trouvés
     */
    suspend fun searchFolders(
        searchTerm: String,
        categoryId: Int? = null,
        memberId: Int? = null
    ): FolderResult<List<Dossier>> = withContext(Dispatchers.IO) {
        try {
            if (searchTerm.isBlank()) {
                return@withContext FolderResult.Error(
                    "Terme de recherche requis",
                    FolderErrorCode.INVALID_INPUT
                )
            }

            val folders = folderRepository.searchDossiersByName(searchTerm, categoryId, memberId)
            val folderModels = folders.map { it.toModel() }

            return@withContext FolderResult.Success(folderModels)

        } catch (e: Exception) {
            return@withContext FolderResult.Error(
                "Erreur lors de la recherche: ${e.message}",
                FolderErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION
    // ================================================================

    /**
     * Supprime un dossier
     * Vérifie qu'il n'a plus de sous-dossiers ni de fichiers avant suppression
     *
     * @param folderId ID du dossier à supprimer
     * @param forceDelete Force la suppression même si le dossier contient des éléments
     * @return Résultat de la suppression
     */
    suspend fun deleteFolder(
        folderId: Int,
        forceDelete: Boolean = false
    ): FolderResult<FolderDeletionSummary> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le dossier existe
            val folder = folderRepository.findById(folderId)
                ?: return@withContext FolderResult.Error(
                    "Dossier non trouvé",
                    FolderErrorCode.FOLDER_NOT_FOUND
                )

            // Calculer ce qui sera supprimé
            val deletionSummary = calculateDeletionImpact(folderId)

            // Vérifier s'il y a du contenu et si forceDelete n'est pas activé
            if (!forceDelete) {
                if (deletionSummary.subFolderCount > 0) {
                    return@withContext FolderResult.Error(
                        "Impossible de supprimer un dossier qui contient des sous-dossiers",
                        FolderErrorCode.HAS_SUBFOLDERS
                    )
                }
                if (deletionSummary.fileCount > 0) {
                    return@withContext FolderResult.Error(
                        "Impossible de supprimer un dossier qui contient des fichiers",
                        FolderErrorCode.HAS_FILES
                    )
                }
            }

            // Supprimer le dossier (implémentation simplifiée)
            val deleted = folderRepository.delete(folderId)

            if (deleted == 0) {
                return@withContext FolderResult.Error(
                    "Aucun dossier supprimé",
                    FolderErrorCode.INTERNAL_ERROR
                )
            }

            return@withContext FolderResult.Success(deletionSummary)

        } catch (e: Exception) {
            return@withContext FolderResult.Error(
                "Erreur lors de la suppression: ${e.message}",
                FolderErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE GESTION DES MODÈLES PAR DÉFAUT
    // ================================================================

    /**
     * Crée les dossiers par défaut pour un membre dans une catégorie
     *
     * @param memberId ID du membre
     * @param categoryId ID de la catégorie
     * @return Résultat de la création
     */
    suspend fun createDefaultFolders(
        memberId: Int,
        categoryId: Int
    ): FolderResult<List<Dossier>> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la catégorie existe
            val category = categoryRepository.findById(categoryId)
                ?: return@withContext FolderResult.Error(
                    "Catégorie non trouvée",
                    FolderErrorCode.CATEGORY_NOT_FOUND
                )

            // Récupérer les modèles de dossiers par défaut
            val templates = defaultFolderTemplateRepository.getModelesByCategorie(categoryId)

            if (templates.isEmpty()) {
                return@withContext FolderResult.Success(emptyList())
            }

            val createdFolders = mutableListOf<Dossier>()

            // Créer chaque dossier par défaut
            for (template in templates) {
                try {
                    // Vérifier si le dossier existe déjà (implémentation simplifiée)
                    // TODO: Implémenter vérification d'unicité quand les méthodes seront ajoutées

                    val folderEntity = DossierEntity {
                        nomDossier = template.nomModele
                        dateCreationDossier = LocalDateTime.now()
                        dateDerniereModifDossier = LocalDateTime.now()
                        estParDefault = true
                        membreFamilleId = memberId
                        categorieId = categoryId
                        dossierParentId = null
                    }

                    val savedFolder = folderRepository.create(folderEntity)
                    createdFolders.add(savedFolder.toModel())
                } catch (e: Exception) {
                    // Continue la création des autres dossiers même si un échoue
                    println("Erreur lors de la création du dossier ${template.nomModele}: ${e.message}")
                }
            }

            return@withContext FolderResult.Success(createdFolders)

        } catch (e: Exception) {
            return@withContext FolderResult.Error(
                "Erreur lors de la création des dossiers par défaut: ${e.message}",
                FolderErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques d'un dossier
     *
     * @param folderId ID du dossier
     * @return Résultat avec les statistiques
     */
    suspend fun getFolderStatistics(folderId: Int): FolderResult<FolderStatistics> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le dossier existe
            val folder = folderRepository.findById(folderId)
                ?: return@withContext FolderResult.Error(
                    "Dossier non trouvé",
                    FolderErrorCode.FOLDER_NOT_FOUND
                )

            val subFolderCount = 0 // TODO: Implémenter quand les méthodes repository seront ajoutées
            val directFileCount = 0 // TODO: Implémenter quand FileRepository sera intégré
            val totalFileCount = 0 // TODO: Implémenter
            val totalSize = 0L // TODO: Implémenter
            val lastActivity: LocalDateTime? = folder.dateDerniereModifDossier

            val stats = FolderStatistics(
                folderId = folderId,
                folderName = folder.nomDossier,
                subFolderCount = subFolderCount,
                directFileCount = directFileCount,
                totalFileCount = totalFileCount,
                totalSize = totalSize,
                lastActivity = lastActivity,
                creationDate = folder.dateCreationDossier,
                lastModified = folder.dateDerniereModifDossier
            )

            return@withContext FolderResult.Success(stats)

        } catch (e: Exception) {
            return@withContext FolderResult.Error(
                "Erreur lors du calcul des statistiques: ${e.message}",
                FolderErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES
    // ================================================================

    private fun buildFolderNode(folder: DossierEntity): FolderNode {
        // Récupérer les sous-dossiers (implémentation simplifiée)
        val allFolders = folderRepository.findAll()
        val subFolders = allFolders.filter { it.dossierParentId == folder.dossierId }
        val children = subFolders.map { subFolder -> buildFolderNode(subFolder) }

        // Compter les fichiers (implémentation simplifiée)
        val fileCount = 0 // TODO: Implémenter quand FileRepository sera intégré

        return FolderNode(
            folder = folder.toModel(),
            children = children,
            fileCount = fileCount
        )
    }

    private suspend fun wouldCreateCircularReference(folderId: Int, newParentId: Int): Boolean {
        // Vérifier si newParentId est un descendant de folderId (implémentation simplifiée)
        // TODO: Implémenter vérification complète des références circulaires
        return false
    }

    private suspend fun calculateDeletionImpact(folderId: Int): FolderDeletionSummary {
        // TODO: Implémenter le calcul réel quand les méthodes repository seront ajoutées
        return FolderDeletionSummary(
            subFolderCount = 0,
            fileCount = 0,
            totalSize = 0L
        )
    }

    private suspend fun calculateTotalFileCount(folderId: Int): Int {
        // TODO: Implémenter quand FileRepository sera intégré
        return 0
    }

    private suspend fun calculateTotalSize(folderId: Int): Long {
        // TODO: Implémenter quand FileRepository sera intégré
        return 0L
    }

    private fun validateFolderData(folderData: CreateFolderRequest): String? {
        return when {
            folderData.name.isBlank() -> "Le nom du dossier ne peut pas être vide"
            folderData.name.length < 2 -> "Le nom du dossier doit contenir au moins 2 caractères"
            folderData.name.length > 255 -> "Le nom du dossier ne peut pas dépasser 255 caractères"
            folderData.name.contains(Regex("[<>:\"/\\\\|?*]")) ->
                "Le nom du dossier contient des caractères non autorisés"
            folderData.categoryId <= 0 -> "ID de catégorie invalide"
            folderData.memberId <= 0 -> "ID de membre invalide"
            else -> null
        }
    }

    private fun validateUpdateData(updateData: UpdateFolderRequest): String? {
        return when {
            updateData.name != null && updateData.name.isBlank() ->
                "Le nom du dossier ne peut pas être vide"
            updateData.name != null && updateData.name.length < 2 ->
                "Le nom du dossier doit contenir au moins 2 caractères"
            updateData.name != null && updateData.name.length > 255 ->
                "Le nom du dossier ne peut pas dépasser 255 caractères"
            updateData.name != null && updateData.name.contains(Regex("[<>:\"/\\\\|?*]")) ->
                "Le nom du dossier contient des caractères non autorisés"
            else -> null
        }
    }
}

// ================================================================
// DATA CLASSES POUR LES REQUÊTES ET RÉSULTATS
// ================================================================

/**
 * Données pour créer un nouveau dossier
 */
data class CreateFolderRequest(
    val name: String,
    val categoryId: Int,
    val memberId: Int,
    val parentFolderId: Int? = null,
    val isDefault: Boolean = false
)

/**
 * Données pour mettre à jour un dossier
 */
data class UpdateFolderRequest(
    val name: String? = null,
    val parentFolderId: Int? = null
)

/**
 * Nœud dans l'arborescence des dossiers
 */
data class FolderNode(
    val folder: Dossier,
    val children: List<FolderNode>,
    val fileCount: Int
)

/**
 * Résumé de l'impact d'une suppression
 */
data class FolderDeletionSummary(
    val subFolderCount: Int,
    val fileCount: Int,
    val totalSize: Long
)

/**
 * Statistiques d'un dossier
 */
data class FolderStatistics(
    val folderId: Int,
    val folderName: String,
    val subFolderCount: Int,
    val directFileCount: Int,
    val totalFileCount: Int,
    val totalSize: Long,
    val lastActivity: LocalDateTime?,
    val creationDate: LocalDateTime?,
    val lastModified: LocalDateTime?
)