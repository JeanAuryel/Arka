package repositories

import ktorm.Family
import ktorm.Families
import ktorm.toFamily
import ktorm.FamilyMember
import ktorm.FamilyMembers
import ktorm.toFamilyMember
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
     * Recherche des familles par mot clé dans le nom
     */
    fun findByKeyword(keyword: String): List<Family> {
        return database.from(Families)
            .select()
            .where { Families.familyLabel like "%$keyword%" }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère tous les membres d'une famille
     */
    fun getMembers(familyId: Int): List<FamilyMember> {
        return database.from(FamilyMembers)
            .select()
            .where { FamilyMembers.familyID eq familyId }
            .map { it.toFamilyMember() }
            .filterNotNull()
    }

    /**
     * Ajoute une nouvelle famille
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

    override fun mapToEntity(row: QueryRowSet): Family? {
        return row.toFamily()
    }
}