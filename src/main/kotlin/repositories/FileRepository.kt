package repositories

import ktorm.File
import ktorm.Files
import ktorm.toFile
import org.ktorm.database.Database
import org.ktorm.dsl.*
import java.time.LocalDateTime

/**
 * Repository pour gérer les opérations sur les fichiers
 */
class FileRepository(database: Database) : BaseRepository<File, Int>(database) {
    override val table = Files
    override val idColumn = Files.fileID

    /**
     * Récupère tous les fichiers
     */
    fun findAll(): List<File> {
        return database.from(Files)
            .select()
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère un fichier par son ID
     */
    fun findById(id: Int): File? {
        return database.from(Files)
            .select()
            .where { Files.fileID eq id }
            .map { mapToEntity(it) }
            .firstOrNull()
    }

    /**
     * Récupère tous les fichiers d'un dossier
     */
    fun findByFolderId(folderId: Int): List<File> {
        return database.from(Files)
            .select()
            .where { Files.folderID eq folderId }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère les fichiers par nom (recherche)
     */
    fun findByName(name: String): List<File> {
        return database.from(Files)
            .select()
            .where { Files.fileName like "%$name%" }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Insère un nouveau fichier
     */
    fun insert(file: File): File? {
        val id = database.insertAndGenerateKey(Files) {
            set(it.fileName, file.fileName)
            set(it.fileSize, file.fileSize)
            set(it.fileCreationDate, file.fileCreationDate ?: LocalDateTime.now())
            set(it.folderID, file.folderID)
        } as Int

        return findById(id)
    }

    /**
     * Met à jour un fichier
     */
    fun update(file: File): Int {
        return database.update(Files) {
            set(it.fileName, file.fileName)
            set(it.fileSize, file.fileSize)
            set(it.folderID, file.folderID)
            where { it.fileID eq (file.fileID ?: 0) }
        }
    }

    /**
     * Supprime un fichier
     */
    fun delete(id: Int): Int {
        return database.delete(Files) { it.fileID eq id }
    }

    override fun mapToEntity(row: QueryRowSet): File? {
        return row.toFile()
    }
}