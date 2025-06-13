package repositories

import org.ktorm.entity.sequenceOf
import org.ktorm.entity.find
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.ktorm.dsl.*
import org.ktorm.schema.Column
import ktorm.*
import java.time.LocalDateTime

/**
 * Repository pour la gestion des catégories
 *
 * Responsabilités:
 * - CRUD des catégories
 * - Gestion des catégories par espace
 * - Validation et recherche de catégories
 * - Statistiques et requêtes métier
 *
 * Utilisé par: CategoryController, FolderController, DelegationController
 */
class CategoryRepository : BaseRepository<CategorieEntity, Categories>() {

    override val table = Categories

    /**
     * Obtient la clé primaire d'une catégorie
     */
    override fun CategorieEntity.getPrimaryKey(): Int = this.categorieId
    override fun getPrimaryKeyColumn(): Column<Int> = Categories.categorieId

    /**
     * Met à jour une catégorie
     */
    override fun update(entity: CategorieEntity): Int {
        return ArkaDatabase.instance.update(Categories) {
            set(it.nomCategorie, entity.nomCategorie)
            set(it.descriptionCategorie, entity.descriptionCategorie)
            where { it.categorieId eq entity.categorieId }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR ESPACE
    // ================================================================

    /**
     * Récupère toutes les catégories d'un espace
     *
     * @param espaceId ID de l'espace
     * @return Liste des catégories de l'espace
     */
    fun getCategoriesByEspace(espaceId: Int): List<CategorieEntity> {
        return ArkaDatabase.instance.sequenceOf(Categories)
            .filter { it.espaceId eq espaceId }
            .toList()
            .sortedBy { it.nomCategorie }
    }

    /**
     * Trouve une catégorie par nom dans un espace
     *
     * @param nomCategorie Le nom de la catégorie
     * @param espaceId ID de l'espace
     * @return La catégorie trouvée ou null
     */
    fun getCategorieByNameAndEspace(nomCategorie: String, espaceId: Int): CategorieEntity? {
        return ArkaDatabase.instance.sequenceOf(Categories)
            .find {
                (it.nomCategorie eq nomCategorie.trim()) and (it.espaceId eq espaceId)
            }
    }

    /**
     * Compte les catégories dans un espace
     *
     * @param espaceId ID de l'espace
     * @return Nombre de catégories
     */
    fun countByEspace(espaceId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(Categories)
            .filter { it.espaceId eq espaceId }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE VALIDATION ET VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si une catégorie avec ce nom existe dans un espace
     *
     * @param nomCategorie Le nom de la catégorie
     * @param espaceId ID de l'espace
     * @param excludeId ID à exclure de la vérification (pour les mises à jour)
     * @return true si le nom existe déjà
     */
    fun existsByNameInEspace(nomCategorie: String, espaceId: Int, excludeId: Int? = null): Boolean {
        return ArkaDatabase.instance.sequenceOf(Categories)
            .filter { categorie ->
                val nameCondition = (categorie.nomCategorie eq nomCategorie.trim()) and (categorie.espaceId eq espaceId)
                if (excludeId != null) {
                    nameCondition and (categorie.categorieId neq excludeId)
                } else {
                    nameCondition
                }
            }
            .toList()
            .isNotEmpty()
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE AVANCÉE
    // ================================================================

    /**
     * Recherche des catégories par nom (recherche partielle)
     *
     * @param searchTerm Terme de recherche
     * @param espaceId ID de l'espace (optionnel)
     * @return Liste des catégories trouvées
     */
    fun searchCategoriesByName(searchTerm: String, espaceId: Int? = null): List<CategorieEntity> {
        return ArkaDatabase.instance.sequenceOf(Categories)
            .filter { categorie ->
                val nameCondition = categorie.nomCategorie like "%${searchTerm.trim()}%"
                if (espaceId != null) {
                    nameCondition and (categorie.espaceId eq espaceId)
                } else {
                    nameCondition
                }
            }
            .toList()
            .sortedBy { it.nomCategorie }
    }

    /**
     * Récupère les catégories créées récemment
     *
     * @param espaceId ID de l'espace (optionnel)
     * @param limit Nombre maximum de catégories
     * @return Liste des catégories récentes
     */
    fun getRecentCategories(espaceId: Int? = null, limit: Int = 10): List<CategorieEntity> {
        val categories = if (espaceId != null) {
            getCategoriesByEspace(espaceId)
        } else {
            findAll()
        }

        return categories
            .sortedByDescending { it.dateCreationCategorie }
            .take(limit)
    }

    /**
     * Trouve toutes les catégories triées par nom
     *
     * @return Liste des catégories triées alphabétiquement
     */
    fun findAllSorted(): List<CategorieEntity> {
        return findAll().sortedBy { it.nomCategorie }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES ET COMPTAGE
    // ================================================================

    /**
     * Compte le nombre de dossiers dans une catégorie
     *
     * @param categorieId ID de la catégorie
     * @return Nombre de dossiers
     */
    fun countDossiersInCategorie(categorieId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { it.categorieId eq categorieId }
            .toList()
            .size
    }

    /**
     * Récupère toutes les catégories avec le nombre de dossiers
     *
     * @param espaceId ID de l'espace (optionnel)
     * @return Liste de paires (catégorie, nombre de dossiers)
     */
    fun getCategoriesWithDossierCount(espaceId: Int? = null): List<Pair<CategorieEntity, Int>> {
        val categories = if (espaceId != null) {
            getCategoriesByEspace(espaceId)
        } else {
            findAllSorted()
        }

        return categories.map { categorie ->
            val count = countDossiersInCategorie(categorie.categorieId)
            Pair(categorie, count)
        }
    }

    /**
     * Obtient les statistiques complètes d'une catégorie
     *
     * @param categorieId ID de la catégorie
     * @return Map avec les statistiques
     */
    fun getStatistiquesCategorie(categorieId: Int): Map<String, Any> {
        val nombreDossiers = countDossiersInCategorie(categorieId)

        // Compter les fichiers via les dossiers
        val dossiersIds = ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { it.categorieId eq categorieId }
            .toList()
            .map { it.dossierId }

        val nombreFichiers = if (dossiersIds.isNotEmpty()) {
            ArkaDatabase.instance.sequenceOf(Fichiers)
                .filter { it.dossierId inList dossiersIds }
                .toList()
                .size
        } else {
            0
        }

        // Calculer la taille totale des fichiers
        val tailleTotale = if (dossiersIds.isNotEmpty()) {
            ArkaDatabase.instance.sequenceOf(Fichiers)
                .filter { it.dossierId inList dossiersIds }
                .toList()
                .sumOf { it.tailleFichier }
        } else {
            0L
        }

        return mapOf(
            "nombreDossiers" to nombreDossiers,
            "nombreFichiers" to nombreFichiers,
            "tailleTotale" to tailleTotale
        )
    }

    // ================================================================
    // MÉTHODES DE MISE À JOUR SPÉCIFIQUES
    // ================================================================

    /**
     * Met à jour uniquement la description d'une catégorie
     *
     * @param categorieId ID de la catégorie
     * @param nouvelleDescription Nouvelle description
     * @return true si la mise à jour a réussi
     */
    fun updateDescription(categorieId: Int, nouvelleDescription: String?): Boolean {
        return try {
            val rowsAffected = ArkaDatabase.instance.update(Categories) {
                set(it.descriptionCategorie, nouvelleDescription)
                where { it.categorieId eq categorieId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour de la description de la catégorie $categorieId: ${e.message}")
            false
        }
    }

    /**
     * Met à jour le nom d'une catégorie avec validation
     *
     * @param categorieId ID de la catégorie
     * @param nouveauNom Nouveau nom
     * @return true si la mise à jour a réussi
     */
    fun updateName(categorieId: Int, nouveauNom: String): Boolean {
        val categorie = findById(categorieId) ?: return false

        if (existsByNameInEspace(nouveauNom, categorie.espaceId, categorieId)) {
            return false // Le nom existe déjà
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(Categories) {
                set(it.nomCategorie, nouveauNom.trim())
                where { it.categorieId eq categorieId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour du nom de la catégorie $categorieId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION AVEC VÉRIFICATIONS
    // ================================================================

    /**
     * Vérifie si une catégorie peut être supprimée (pas de dossiers)
     *
     * @param categorieId ID de la catégorie
     * @return true si la catégorie peut être supprimée
     */
    fun canBeDeleted(categorieId: Int): Boolean {
        return countDossiersInCategorie(categorieId) == 0
    }

    /**
     * Supprime une catégorie avec cascade (tous ses dossiers)
     * ATTENTION: Supprime définitivement tous les dossiers !
     *
     * @param categorieId ID de la catégorie
     * @return true si la suppression a réussi
     */
    fun deleteWithCascade(categorieId: Int): Boolean {
        return try {
            transaction {
                // La suppression cascade est gérée par la contrainte FK
                val rowsAffected = delete(categorieId)
                rowsAffected > 0
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression de la catégorie $categorieId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE CRÉATION AVEC VALIDATION
    // ================================================================

    /**
     * Crée une nouvelle catégorie avec validation
     *
     * @param nomCategorie Nom de la catégorie
     * @param descriptionCategorie Description (optionnelle)
     * @param espaceId ID de l'espace parent
     * @return La catégorie créée ou null en cas d'erreur
     */
    fun createCategorie(
        nomCategorie: String,
        descriptionCategorie: String?,
        espaceId: Int
    ): CategorieEntity? {
        // Validation du nom
        if (nomCategorie.isBlank() || nomCategorie.length < 2 || nomCategorie.length > 100) {
            println("Nom de catégorie invalide: '$nomCategorie'")
            return null
        }

        // Vérifier l'unicité dans l'espace
        if (existsByNameInEspace(nomCategorie, espaceId)) {
            println("Une catégorie avec ce nom existe déjà dans cet espace")
            return null
        }

        return try {
            val categorie = CategorieEntity {
                this.nomCategorie = nomCategorie.trim()
                this.descriptionCategorie = descriptionCategorie
                this.espaceId = espaceId
                this.dateCreationCategorie = LocalDateTime.now()
            }

            create(categorie)
        } catch (e: Exception) {
            println("Erreur lors de la création de la catégorie: ${e.message}")
            null
        }
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Valide le nom d'une catégorie
     *
     * @param nomCategorie Le nom à valider
     * @return Liste des erreurs de validation
     */
    fun validateNomCategorie(nomCategorie: String): List<String> {
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
}