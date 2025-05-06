package repositories

import ktorm.FamilyMember
import ktorm.FamilyMembers
import ktorm.toFamilyMember
import org.ktorm.database.Database
import org.ktorm.dsl.*

/**
 * Repository pour gérer les opérations sur les membres de famille
 */
class FamilyMemberRepository(database: Database) : BaseRepository<FamilyMember, Int>(database) {
    override val table = FamilyMembers
    override val idColumn = FamilyMembers.familyMemberID

    /**
     * Récupère tous les membres
     */
    fun findAll(): List<FamilyMember> {
        return database.from(FamilyMembers)
            .select()
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère un membre par son ID
     */
    fun findById(id: Int): FamilyMember? {
        return database.from(FamilyMembers)
            .select()
            .where { FamilyMembers.familyMemberID eq id }
            .map { mapToEntity(it) }
            .firstOrNull()
    }

    /**
     * Récupère un membre par son email
     */
    fun findByEmail(email: String): FamilyMember? {
        return database.from(FamilyMembers)
            .select()
            .where { FamilyMembers.familyMemberMail eq email }
            .map { mapToEntity(it) }
            .firstOrNull()
    }

    /**
     * Récupère tous les membres d'une famille
     */
    fun findByFamilyId(familyId: Int): List<FamilyMember> {
        return database.from(FamilyMembers)
            .select()
            .where { FamilyMembers.familyID eq familyId }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère tous les parents d'une famille
     */
    fun findParentsByFamilyId(familyId: Int): List<FamilyMember> {
        return database.from(FamilyMembers)
            .select()
            .where { (FamilyMembers.familyID eq familyId) and (FamilyMembers.isParent eq true) }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Récupère tous les enfants d'une famille
     */
    fun findChildrenByFamilyId(familyId: Int): List<FamilyMember> {
        return database.from(FamilyMembers)
            .select()
            .where { (FamilyMembers.familyID eq familyId) and (FamilyMembers.isParent eq false) }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Recherche des membres par prénom
     */
    fun findByFirstName(firstName: String): List<FamilyMember> {
        return database.from(FamilyMembers)
            .select()
            .where { FamilyMembers.familyMemberFirstName like "%$firstName%" }
            .map { mapToEntity(it) }
            .filterNotNull()
    }

    /**
     * Ajoute un nouveau membre
     */
    fun insert(member: FamilyMember): FamilyMember? {
        val id = database.insertAndGenerateKey(FamilyMembers) {
            set(it.familyMemberFirstName, member.familyMemberFirstName)
            set(it.familyMemberMail, member.familyMemberMail)
            set(it.familyMemberPassword, member.familyMemberPassword)
            set(it.familyMemberBirthDate, member.familyMemberBirthDate)
            set(it.familyMemberGender, member.familyMemberGender)
            set(it.isParent, member.isParent)
            set(it.isAdmin, member.isAdmin)
            set(it.familyID, member.familyID)
        } as Int

        return findById(id)
    }

    /**
     * Met à jour un membre
     */
    fun update(member: FamilyMember): Int {
        return database.update(FamilyMembers) {
            set(it.familyMemberFirstName, member.familyMemberFirstName)
            set(it.familyMemberMail, member.familyMemberMail)
            set(it.familyMemberPassword, member.familyMemberPassword)
            set(it.familyMemberBirthDate, member.familyMemberBirthDate)
            set(it.familyMemberGender, member.familyMemberGender)
            set(it.isParent, member.isParent)
            set(it.isAdmin, member.isAdmin)
            set(it.familyID, member.familyID)
            where { it.familyMemberID eq (member.familyMemberID ?: 0) }
        }
    }

    /**
     * Supprime un membre
     */
    fun delete(id: Int): Int {
        return database.delete(FamilyMembers) { it.familyMemberID eq id }
    }

    override fun mapToEntity(row: QueryRowSet): FamilyMember? {
        return row.toFamilyMember()
    }
}