package repositories

import ktorm.Families
import ktorm.Family
import ktorm.FamilyMembers
import ktorm.toFamily
import org.ktorm.database.Database
import org.ktorm.dsl.*

/**
 * Repository pour gérer les opérations sur les familles
 */
class FamilyRepository(database: Database) : BaseRepository<Family, Int>(database) {
    override val table = Families
    override val idColumn = Families.familyID

    /**
     * Récupère toutes les familles
     */
    fun findAll(): List<Family> {
        return database.from(Families)
            .select()
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère une famille par son ID
     */
    fun findById(id: Int): Family? {
        return database.from(Families)
            .select()
            .where { Families.familyID eq id }
            .map { mapToEntity(it) }
            .firstOrNull()
    }

    /**
     * Récupère une famille par son label
     */
    fun findByLabel(label: String): Family? {
        return database.from(Families)
            .select()
            .where { Families.familyLabel eq label }
            .map { mapToEntity(it) }
            .firstOrNull()
    }

    /**
     * Insère une nouvelle famille
     */
    fun insert(family: Family): Family? {
        val id = database.insertAndGenerateKey(Families) {
            set(it.familyLabel, family.familyLabel)
        } as Int

        return findById(id)
    }

    /**
     * Met à jour une famille
     */
    fun update(family: Family): Int {
        return database.update(Families) {
            set(it.familyLabel, family.familyLabel)
            where { it.familyID eq (family.familyID ?: 0) }
        }
    }

    /**
     * Supprime une famille
     */
    fun delete(id: Int): Int {
        return database.delete(Families) { it.familyID eq id }
    }

    /**
     * Vérifie si une famille a des membres
     */
    fun hasMembers(familyID: Int): Boolean {
        return database.from(FamilyMembers)
            .select(count(idColumn))
            .where { FamilyMembers.familyID eq familyID }
            .map { it.getInt(1) ?: 0 }
            .first() > 0
    }

    override fun mapToEntity(row: QueryRowSet): Family? {
        return row.toFamily()
    }
}