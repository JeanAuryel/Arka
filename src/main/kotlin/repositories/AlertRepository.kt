package repositories

import org.ktorm.database.Database
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.ktorm.dsl.*
import ktorm.ArkaDatabase
import ktorm.AlerteEntity
import ktorm.Alertes
import ktorm.toModel
import java.time.LocalDateTime

/**
 * Repository pour la gestion des alertes
 * Basé sur l'entité AlerteEntity définie dans ktorm
 */
class AlertRepository : BaseRepository<AlerteEntity>() {

    /**
     * Trouve toutes les alertes d'un membre
     */
    fun findByMemberId(membreFamilleId: Int): List<AlerteEntity> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
    }

    /**
     * Trouve les alertes actives d'un membre
     */
    fun findActiveByMemberId(membreFamilleId: Int): List<AlerteEntity> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter { (it.membreFamilleId eq membreFamilleId) and (it.estActive eq true) }
            .toList()
    }

    /**
     * Trouve une alerte par ID
     */
    fun findById(alerteId: Int): AlerteEntity? {
        return ArkaDatabase.instance.sequenceOf(Alertes).find { it.alerteId eq alerteId }
    }

    /**
     * Crée une nouvelle alerte
     */
    fun create(alerte: AlerteEntity): AlerteEntity {
        ArkaDatabase.instance.sequenceOf(Alertes).add(alerte)
        return alerte
    }

    /**
     * Met à jour une alerte
     */
    fun update(alerte: AlerteEntity): Int {
        return ArkaDatabase.instance.update(Alertes) {
            set(it.typeAlerte, alerte.typeAlerte)
            set(it.categorieAlerte, alerte.categorieAlerte)
            set(it.uniteIntervalleAlerte, alerte.uniteIntervalleAlerte)
            set(it.valeurIntervalleAlerte, alerte.valeurIntervalleAlerte)
            set(it.dateProchainDeclenchement, alerte.dateProchainDeclenchement)
            set(it.dateDernierDeclenchement, alerte.dateDernierDeclenchement)
            set(it.estActive, alerte.estActive)
            where { it.alerteId eq alerte.alerteId }
        }
    }

    /**
     * Supprime une alerte
     */
    fun delete(alerteId: Int): Int {
        return ArkaDatabase.instance.delete(Alertes) { it.alerteId eq alerteId }
    }

    /**
     * Trouve les alertes qui doivent être déclenchées
     */
    fun findAlertsToTrigger(currentTime: LocalDateTime): List<AlerteEntity> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter {
                (it.estActive eq true) and
                        (it.dateProchainDeclenchement lessEq currentTime)
            }
            .toList()
    }

    /**
     * Compte le nombre total d'alertes d'un membre
     */
    fun countByMemberId(membreFamilleId: Int): Int {
        return ArkaDatabase.instance.from(Alertes)
            .select(count())
            .where { Alertes.membreFamilleId eq membreFamilleId }
            .map { it.getInt(1) }
            .first()
    }

    /**
     * Compte le nombre d'alertes actives d'un membre
     */
    fun countActiveByMemberId(membreFamilleId: Int): Int {
        return ArkaDatabase.instance.from(Alertes)
            .select(count())
            .where {
                (Alertes.membreFamilleId eq membreFamilleId) and
                        (Alertes.estActive eq true)
            }
            .map { it.getInt(1) }
            .first()
    }

    /**
     * Compte le nombre d'alertes en attente d'un membre
     */
    fun countPendingByMemberId(membreFamilleId: Int): Int {
        return ArkaDatabase.instance.from(Alertes)
            .select(count())
            .where {
                (Alertes.membreFamilleId eq membreFamilleId) and
                        (Alertes.estActive eq true) and
                        (Alertes.dateProchainDeclenchement lessEq LocalDateTime.now())
            }
            .map { it.getInt(1) }
            .first()
    }

    /**
     * Compte le nombre d'alertes récemment déclenchées d'un membre
     */
    fun countRecentlyTriggeredByMemberId(membreFamilleId: Int, since: LocalDateTime): Int {
        return ArkaDatabase.instance.from(Alertes)
            .select(count())
            .where {
                (Alertes.membreFamilleId eq membreFamilleId) and
                        (Alertes.dateDernierDeclenchement greaterEq since)
            }
            .map { it.getInt(1) }
            .first()
    }
}