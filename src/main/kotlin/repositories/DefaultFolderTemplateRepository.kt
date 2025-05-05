package repositories

import ktorm.DefaultFolderTemplate
import ktorm.DefaultFolderTemplates
import ktorm.toDefaultFolderTemplate
import org.ktorm.database.Database
import org.ktorm.dsl.*

/**
 * Repository pour gérer les opérations sur les modèles de dossiers par défaut
 */
class DefaultFolderTemplateRepository(database: Database) : BaseRepository<DefaultFolderTemplate, Int>(database) {
    override val table = DefaultFolderTemplates
    override val idColumn = DefaultFolderTemplates.templateID

    /**
     * Récupère tous les modèles de dossiers par défaut
     */
    fun findAll(): List<DefaultFolderTemplate> {
        return database.from(DefaultFolderTemplates)
            .select()
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère un modèle de dossier par défaut par son ID
     */
    fun findById(id: Int): DefaultFolderTemplate? {
        return database.from(DefaultFolderTemplates)
            .select()
            .where { DefaultFolderTemplates.templateID eq id }
            .map { mapToEntity(it) }
            .firstOrNull()
    }

    /**
     * Récupère tous les modèles de dossiers par défaut pour une catégorie
     */
    fun findByCategoryId(categoryId: Int): List<DefaultFolderTemplate> {
        return database.from(DefaultFolderTemplates)
            .select()
            .where { DefaultFolderTemplates.categoryID eq categoryId }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Ajoute un nouveau modèle de dossier par défaut
     */
    fun insert(template: DefaultFolderTemplate): DefaultFolderTemplate? {
        val id = database.insertAndGenerateKey(DefaultFolderTemplates) {
            set(it.templateName, template.templateName)
            set(it.categoryID, template.categoryID)
        } as Int

        return findById(id)
    }

    /**
     * Met à jour un modèle de dossier par défaut
     */
    fun update(template: DefaultFolderTemplate): Int {
        return database.update(DefaultFolderTemplates) {
            set(it.templateName, template.templateName)
            set(it.categoryID, template.categoryID)
            where { it.templateID eq (template.templateID ?: 0) }
        }
    }

    /**
     * Supprime un modèle de dossier par défaut
     */
    fun delete(id: Int): Int {
        return database.delete(DefaultFolderTemplates) { it.templateID eq id }
    }

    override fun mapToEntity(row: QueryRowSet): DefaultFolderTemplate? {
        return row.toDefaultFolderTemplate()
    }
}