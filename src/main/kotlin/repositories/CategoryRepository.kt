package repositories


import ktorm.*
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.ktorm.schema.Column
import java.time.LocalDateTime

/**
 * Repository pour la gestion des catégories dans Arka
 */
class CategoryRepository : BaseRepository<CategorieEntity, org.ktorm.schema.Table<CategorieEntity>>() {

    override val table = Categories

    override fun getIdColumn(entity: CategorieEntity): Column<Int> = table.categorieId

    /**
     * Trouve toutes les catégories d'un espace
     * @param espaceId L'ID de l'espace
     * @return Liste des catégories de l'espace
     */
    fun findBySpace(espaceId: Int): List<CategorieEntity> {
        return try {
            entities.filter { table.espaceId eq espaceId }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des catégories de l'espace $espaceId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve une catégorie par nom dans un espace donné
     * @param nomCategorie Le nom de la catégorie
     * @param espaceId L'ID de l'espace
     * @return La catégorie trouvée ou null
     */
    fun findByNameInSpace(nomCategorie: String, espaceId: Int): CategorieEntity? {
        return try {
            entities.find {
                (table.nomCategorie eq nomCategorie) and (table.espaceId eq espaceId)
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche de la catégorie '$nomCategorie': ${e.message}")
            null
        }
    }

    /**
     * Crée une nouvelle catégorie
     * @param nomCategorie Le nom de la catégorie
     * @param descriptionCategorie La description (optionnelle)
     * @param espaceId L'ID de l'espace parent
     * @return Le résultat de l'opération
     */
    fun createCategory(
        nomCategorie: String,
        descriptionCategorie: String? = null,
        espaceId: Int
    ): RepositoryResult<Categorie> {

        // Validation
        val validationErrors = validateCategory(nomCategorie)
        if (validationErrors.isNotEmpty()) {
            return RepositoryResult.ValidationError(validationErrors)
        }

        // Vérifier l'unicité dans l'espace
        if (findByNameInSpace(nomCategorie, espaceId) != null) {
            return RepositoryResult.Error("Une catégorie avec ce nom existe déjà dans cet espace")
        }

        return try {
            val categorie = CategorieEntity {
                this.nomCategorie = nomCategorie
                this.descriptionCategorie = descriptionCategorie
                this.espaceId = espaceId
                this.dateCreationCategorie = LocalDateTime.now()
            }

            if (save(categorie)) {
                RepositoryResult.Success(categorie.toModel())
            } else {
                RepositoryResult.Error("Échec de la création de la catégorie")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la création: ${e.message}")
        }
    }

    /**
     * Met à jour une catégorie
     * @param categorieId L'ID de la catégorie
     * @param nomCategorie Le nouveau nom
     * @param descriptionCategorie La nouvelle description
     * @return Le résultat de l'opération
     */
    fun updateCategory(
        categorieId: Int,
        nomCategorie: String,
        descriptionCategorie: String? = null
    ): RepositoryResult<Categorie> {

        val validationErrors = validateCategory(nomCategorie)
        if (validationErrors.isNotEmpty()) {
            return RepositoryResult.ValidationError(validationErrors)
        }

        return try {
            val categorie = findById(categorieId)
            if (categorie == null) {
                return RepositoryResult.Error("Catégorie non trouvée")
            }

            // Vérifier l'unicité (sauf pour la catégorie actuelle)
            val existing = findByNameInSpace(nomCategorie, categorie.espaceId)
            if (existing != null && existing.categorieId != categorieId) {
                return RepositoryResult.Error("Une catégorie avec ce nom existe déjà dans cet espace")
            }

            categorie.nomCategorie = nomCategorie
            categorie.descriptionCategorie = descriptionCategorie

            if (update(categorie)) {
                RepositoryResult.Success(categorie.toModel())
            } else {
                RepositoryResult.Error("Échec de la mise à jour")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la mise à jour: ${e.message}")
        }
    }

    /**
     * Obtient le nombre de dossiers dans une catégorie
     * @param categorieId L'ID de la catégorie
     * @return Le nombre de dossiers
     */
    fun getFolderCount(categorieId: Int): Int {
        return try {
            database.from(Dossiers)
                .select(count())
                .where { Dossiers.categorieId eq categorieId }
                .map { row -> row.getInt(1) }
                .first()
        } catch (e: Exception) {
            println("⚠️ Erreur lors du comptage des dossiers pour la catégorie $categorieId: ${e.message}")
            0
        }
    }

    /**
     * Recherche des catégories par nom partiel
     * @param nomPartiel Une partie du nom à rechercher
     * @param espaceId Optionnel: limiter à un espace
     * @return Liste des catégories correspondantes
     */
    fun searchByName(nomPartiel: String, espaceId: Int? = null): List<CategorieEntity> {
        return try {
            var query = entities.filter { table.nomCategorie like "%$nomPartiel%" }

            if (espaceId != null) {
                query = query.filter { table.espaceId eq espaceId }
            }

            query.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche par nom '$nomPartiel': ${e.message}")
            emptyList()
        }
    }

    /**
     * Supprime une catégorie avec tous ses dossiers (CASCADE)
     * @param categorieId L'ID de la catégorie à supprimer
     * @return Le résultat de l'opération
     */
    fun deleteCategoryWithFolders(categorieId: Int): RepositoryResult<Boolean> {
        return withTransaction {
            try {
                // Vérifier s'il y a des dossiers
                val folderCount = getFolderCount(categorieId)
                if (folderCount > 0) {
                    println("⚠️ Suppression de la catégorie avec $folderCount dossier(s)")
                }

                // La suppression CASCADE est gérée par la base de données
                val deleted = deleteById(categorieId)

                if (deleted) {
                    RepositoryResult.Success(true)
                } else {
                    RepositoryResult.Error("Catégorie non trouvée ou déjà supprimée")
                }
            } catch (e: Exception) {
                RepositoryResult.Error("Erreur lors de la suppression: ${e.message}")
            }
        } ?: RepositoryResult.Error("Erreur de transaction")
    }

    /**
     * Récupère toutes les catégories avec le nombre de dossiers
     * @param espaceId Optionnel: limiter à un espace
     * @return Liste des catégories avec statistiques
     */
    fun findAllWithFolderCount(espaceId: Int? = null): List<CategorieAvecStats> {
        return try {
            val categories = if (espaceId != null) {
                findBySpace(espaceId)
            } else {
                findAll()
            }

            categories.map { categorie ->
                CategorieAvecStats(
                    categorie = categorie.toModel(),
                    nombreDossiers = getFolderCount(categorie.categorieId)
                )
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la récupération des catégories avec stats: ${e.message}")
            emptyList()
        }
    }

    /**
     * Valide le nom d'une catégorie
     */
    private fun validateCategory(nomCategorie: String): List<String> {
        val errors = mutableListOf<String>()

        if (nomCategorie.isBlank()) {
            errors.add("Le nom de la catégorie ne peut pas être vide")
        }

        if (nomCategorie.length < 2) {
            errors.add("Le nom de la catégorie doit contenir au moins 2 caractères")
        }

        if (nomCategorie.length > 100) {
            errors.add("Le nom de la catégorie ne peut pas dépasser 100 caractères")
        }

        return errors
    }

    override fun validate(entity: CategorieEntity): List<String> {
        return validateCategory(entity.nomCategorie)
    }
}

/**
 * Classe de données pour les statistiques de catégorie
 */
data class CategorieAvecStats(
    val categorie: Categorie,
    val nombreDossiers: Int
)
