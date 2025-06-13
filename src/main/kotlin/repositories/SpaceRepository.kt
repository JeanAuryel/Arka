package repositories

import org.ktorm.database.Database
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.ktorm.dsl.*
import ktorm.ArkaDatabase
import ktorm.EspaceEntity
import ktorm.MembreEspaceEntity
import ktorm.Espaces
import ktorm.MembresEspace
import ktorm.toModel

/**
 * Repository pour la gestion des espaces (Espace et MembreEspace)
 * Basé sur les entités ktorm définies
 */
class SpaceRepository : BaseRepository<EspaceEntity, Espaces>() {

    /**
     * Trouve tous les espaces
     */
    fun findAll(): List<EspaceEntity> {
        return ArkaDatabase.instance.sequenceOf(Espaces).toList()
    }

    /**
     * Trouve un espace par ID
     */
    fun findById(espaceId: Int): EspaceEntity? {
        return ArkaDatabase.instance.sequenceOf(Espaces).find { it.espaceId eq espaceId }
    }

    /**
     * Vérifie si un espace existe par ID
     */
    fun exists(espaceId: Int): Boolean {
        return ArkaDatabase.instance.from(Espaces)
            .select(count())
            .where { Espaces.espaceId eq espaceId }
            .map { it.getInt(1) }
            .first() > 0
    }

    /**
     * Vérifie si un espace avec ce nom existe déjà
     */
    fun existsByName(nomEspace: String): Boolean {
        return ArkaDatabase.instance.from(Espaces)
            .select(count())
            .where { Espaces.nomEspace eq nomEspace }
            .map { it.getInt(1) }
            .first() > 0
    }

    /**
     * Crée un nouvel espace
     */
    fun create(espace: EspaceEntity): EspaceEntity {
        ArkaDatabase.instance.sequenceOf(Espaces).add(espace)
        return espace
    }

    /**
     * Met à jour un espace
     */
    fun update(espace: EspaceEntity): Int {
        return ArkaDatabase.instance.update(Espaces) {
            set(it.nomEspace, espace.nomEspace)
            set(it.descriptionEspace, espace.descriptionEspace)
            where { it.espaceId eq espace.espaceId }
        }
    }

    /**
     * Supprime un espace
     */
    fun delete(espaceId: Int): Int {
        return ArkaDatabase.instance.delete(Espaces) { it.espaceId eq espaceId }
    }

    /**
     * Trouve les espaces accessibles par un membre
     */
    fun findAccessibleByUserId(membreFamilleId: Int): List<EspaceEntity> {
        return ArkaDatabase.instance.from(Espaces)
            .innerJoin(MembresEspace, on = Espaces.espaceId eq MembresEspace.espaceId)
            .select()
            .where { MembresEspace.membreFamilleId eq membreFamilleId }
            .map { row ->
                EspaceEntity {
                    espaceId = row[Espaces.espaceId]!!
                    nomEspace = row[Espaces.nomEspace]!!
                    descriptionEspace = row[Espaces.descriptionEspace]
                    dateCreationEspace = row[Espaces.dateCreationEspace]
                }
            }
    }
}

/**
 * Repository pour la gestion des accès aux espaces (MembreEspace)
 */
class MemberSpaceRepository : BaseRepository<MembreEspaceEntity, MembresEspace>() {

    /**
     * Vérifie si un membre a accès à un espace
     */
    fun hasAccess(membreFamilleId: Int, espaceId: Int): Boolean {
        return ArkaDatabase.instance.from(MembresEspace)
            .select(count())
            .where {
                (MembresEspace.membreFamilleId eq membreFamilleId) and
                        (MembresEspace.espaceId eq espaceId)
            }
            .map { it.getInt(1) }
            .first() > 0
    }

    /**
     * Accorde l'accès à un espace pour un membre
     */
    fun create(membreEspace: MembreEspaceEntity): MembreEspaceEntity {
        ArkaDatabase.instance.sequenceOf(MembresEspace).add(membreEspace)
        return membreEspace
    }

    /**
     * Révoque l'accès à un espace pour un membre
     */
    fun removeAccess(membreFamilleId: Int, espaceId: Int): Boolean {
        val deleted = ArkaDatabase.instance.delete(MembresEspace) {
            (it.membreFamilleId eq membreFamilleId) and (it.espaceId eq espaceId)
        }
        return deleted > 0
    }

    /**
     * Supprime tous les accès pour un espace donné
     */
    fun deleteBySpaceId(espaceId: Int): Int {
        return ArkaDatabase.instance.delete(MembresEspace) { it.espaceId eq espaceId }
    }

    /**
     * Trouve tous les membres ayant accès à un espace
     */
    fun findMembersBySpaceId(espaceId: Int): List<MembreEspaceEntity> {
        return ArkaDatabase.instance.sequenceOf(MembresEspace)
            .filter { it.espaceId eq espaceId }
            .toList()
    }

    /**
     * Trouve tous les espaces accessibles par un membre
     */
    fun findSpacesByMemberId(membreFamilleId: Int): List<MembreEspaceEntity> {
        return ArkaDatabase.instance.sequenceOf(MembresEspace)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
    }
}