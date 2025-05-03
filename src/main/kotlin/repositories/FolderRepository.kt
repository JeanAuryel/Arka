package repositories

import ktorm.Folder
import ktorm.Folders
import ktorm.toFolder
import org.ktorm.database.Database
import org.ktorm.dsl.*
import java.time.LocalDateTime

/**
 * Repository pour gérer les opérations sur les dossiers
 */
class FolderRepository(database: Database) : BaseRepository<Folder, Int>(database) {
    override val table = Folders
    override val idColumn = Folders.folderID

    /**
     * Récupère tous les dossiers
     */
    fun findAll(): List<Folder> {
        return database.from(Folders)
            .select()
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère un dossier par son ID
     */
    fun findById(id: Int): Folder? {
        return database.from(Folders)
            .select()
            .where { Folders.folderID eq id }
            .map { mapToEntity(it) }
            .firstOrNull()
    }

    /**
     * Récupère les dossiers d'un membre par catégorie
     */
    fun findByMemberAndCategory(memberId: Int, categoryId: Int): List<Folder> {
        return database.from(Folders)
            .select()
            .where { (Folders.familyMemberID eq memberId) and (Folders.categoryID eq categoryId) }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère tous les dossiers d'un membre
     */
    fun findAllByMemberId(memberId: Int): List<Folder> {
        return database.from(Folders)
            .select()
            .where { Folders.familyMemberID eq memberId }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère les dossiers racines d'un membre (sans parent)
     */
    fun findRootFoldersByMemberId(memberId: Int): List<Folder> {
        return database.from(Folders)
            .select()
            .where { (Folders.familyMemberID eq memberId) and (Folders.parentFolderID.isNull()) }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère les sous-dossiers d'un dossier
     */
    fun findSubFolders(folderId: Int): List<Folder> {
        return database.from(Folders)
            .select()
            .where { Folders.parentFolderID eq folderId }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère tous les dossiers d'une catégorie
     */
    fun findAllByCategoryId(categoryId: Int): List<Folder> {
        return database.from(Folders)
            .select()
            .where { Folders.categoryID eq categoryId }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Insère un nouveau dossier
     */
    fun insert(folder: Folder): Folder? {
        val id = database.insertAndGenerateKey(Folders) {
            set(it.folderName, folder.folderName)
            set(it.folderCreationDate, folder.folderCreationDate ?: LocalDateTime.now())
            set(it.parentFolderID, folder.parentFolderID)
            set(it.familyMemberID, folder.familyMemberID)
            set(it.categoryID, folder.categoryID)
        } as Int

        return findById(id)
    }

    /**
     * Met à jour un dossier
     */
    fun update(folder: Folder): Int {
        return database.update(Folders) {
            set(it.folderName, folder.folderName)
            set(it.parentFolderID, folder.parentFolderID)
            set(it.categoryID, folder.categoryID)
            where { it.folderID eq (folder.folderID ?: 0) }
        }
    }

    /**
     * Supprime un dossier
     */
    fun delete(id: Int): Int {
        // Les sous-dossiers seront supprimés automatiquement grâce à la contrainte CASCADE
        return database.delete(Folders) { it.folderID eq id }
    }

    override fun mapToEntity(row: QueryRowSet): Folder? {
        return row.toFolder()
    }
}