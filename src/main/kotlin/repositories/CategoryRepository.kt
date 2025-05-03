package repositories

import ktorm.Categories
import ktorm.Category
import ktorm.Folders
import ktorm.toCategory
import org.ktorm.database.Database
import org.ktorm.dsl.*

/**
 * Repository pour gérer les opérations sur les catégories
 */
class CategoryRepository(database: Database) : BaseRepository<Category, Int>(database) {
    override val table = Categories
    override val idColumn = Categories.categoryID

    /**
     * Récupère toutes les catégories
     */
    fun findAll(): List<Category> {
        return database.from(Categories)
            .select()
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère une catégorie par son ID
     */
    fun findById(id: Int): Category? {
        return database.from(Categories)
            .select()
            .where { Categories.categoryID eq id }
            .map { mapToEntity(it) }
            .firstOrNull()
    }

    /**
     * Récupère une catégorie par son label
     */
    fun findByLabel(label: String): Category? {
        return database.from(Categories)
            .select()
            .where { Categories.categoryLabel eq label }
            .map { mapToEntity(it) }
            .firstOrNull()
    }

    /**
     * Insère une nouvelle catégorie
     */
    fun insert(category: Category): Category? {
        val id = database.insertAndGenerateKey(Categories) {
            set(it.categoryLabel, category.categoryLabel)
            set(it.categoryDescription, category.categoryDescription)
        } as Int

        return findById(id)
    }

    /**
     * Met à jour une catégorie
     */
    fun update(category: Category): Int {
        return database.update(Categories) {
            set(it.categoryLabel, category.categoryLabel)
            set(it.categoryDescription, category.categoryDescription)
            where { it.categoryID eq (category.categoryID ?: 0) }
        }
    }

    /**
     * Supprime une catégorie
     */
    fun delete(id: Int): Int {
        return database.delete(Categories) { it.categoryID eq id }
    }

    /**
     * Vérifie si une catégorie a des dossiers associés
     */
    fun hasAssociatedFolders(categoryId: Int): Boolean {
        return database.from(Folders)
            .select(count(idColumn))
            .where { Folders.categoryID eq categoryId }
            .map { it.getInt(1) ?: 0 }
            .first() > 0
    }

    override fun mapToEntity(row: QueryRowSet): Category? {
        return row.toCategory()
    }
}