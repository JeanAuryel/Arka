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
 * Repository pour la gestion des alertes
 *
 * Responsabilités:
 * - CRUD des alertes programmées
 * - Gestion des déclenchements d'alertes
 * - Calcul des prochaines dates de déclenchement
 * - Statistiques et suivi des alertes
 * - Activation/désactivation des alertes
 *
 * Utilisé par: AlertController, NotificationService, ScheduledTasks
 */
class AlertRepository : BaseRepository<AlerteEntity, Alertes>() {

    override val table = Alertes

    /**
     * Obtient la clé primaire d'une alerte
     */
    override fun AlerteEntity.getPrimaryKey(): Int = this.alerteId
    override fun getPrimaryKeyColumn(): Column<Int> = Alertes.alerteId

    /**
     * Met à jour une alerte
     */
    override fun update(entity: AlerteEntity): Int {
        return ArkaDatabase.instance.update(Alertes) {
            set(it.typeAlerte, entity.typeAlerte)
            set(it.categorieAlerte, entity.categorieAlerte)
            set(it.uniteIntervalleAlerte, entity.uniteIntervalleAlerte)
            set(it.valeurIntervalleAlerte, entity.valeurIntervalleAlerte)
            set(it.dateProchainDeclenchement, entity.dateProchainDeclenchement)
            set(it.dateDernierDeclenchement, entity.dateDernierDeclenchement)
            set(it.estActive, entity.estActive)
            where { it.alerteId eq entity.alerteId }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR MEMBRE
    // ================================================================

    /**
     * Récupère toutes les alertes d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Liste des alertes du membre
     */
    fun getAlertesByMembre(membreFamilleId: Int): List<AlerteEntity> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
            .sortedBy { it.dateCreationAlerte ?: LocalDateTime.MIN }
    }

    /**
     * Récupère les alertes actives d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Liste des alertes actives
     */
    fun getAlertesActivesByMembre(membreFamilleId: Int): List<AlerteEntity> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter {
                (it.membreFamilleId eq membreFamilleId) and (it.estActive eq true)
            }
            .toList()
            .sortedBy { it.dateProchainDeclenchement ?: LocalDateTime.MAX }
    }

    /**
     * Compte les alertes d'un membre
     *
     * @param membreFamilleId ID du membre
     * @param actives Compter seulement les alertes actives
     * @return Nombre d'alertes
     */
    fun countAlertesByMembre(membreFamilleId: Int, actives: Boolean = false): Int {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter { alerte ->
                val baseCondition = alerte.membreFamilleId eq membreFamilleId
                if (actives) {
                    baseCondition and (alerte.estActive eq true)
                } else {
                    baseCondition
                }
            }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE GESTION DES DÉCLENCHEMENTS
    // ================================================================

    /**
     * Récupère les alertes qui doivent être déclenchées maintenant
     *
     * @param currentTime Temps actuel (optionnel, par défaut maintenant)
     * @return Liste des alertes à déclencher
     */
    fun getAlertesADeclencher(currentTime: LocalDateTime = LocalDateTime.now()): List<AlerteEntity> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter {
                (it.estActive eq true) and
                        it.dateProchainDeclenchement.isNotNull() and
                        (it.dateProchainDeclenchement lessEq currentTime)
            }
            .toList()
            .sortedBy { it.dateProchainDeclenchement }
    }

    /**
     * Récupère les alertes qui seront déclenchées bientôt
     *
     * @param membreFamilleId ID du membre (optionnel)
     * @param hoursAhead Nombre d'heures d'avance
     * @return Liste des alertes prochaines
     */
    fun getAlertesProchaines(membreFamilleId: Int? = null, hoursAhead: Int = 24): List<AlerteEntity> {
        val maintenant = LocalDateTime.now()
        val seuil = maintenant.plusHours(hoursAhead.toLong())

        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter { alerte ->
                var condition = (alerte.estActive eq true) and
                        alerte.dateProchainDeclenchement.isNotNull() and
                        (alerte.dateProchainDeclenchement greater maintenant) and
                        (alerte.dateProchainDeclenchement lessEq seuil)

                membreFamilleId?.let {
                    condition = condition and (alerte.membreFamilleId eq it)
                }

                condition
            }
            .toList()
            .sortedBy { it.dateProchainDeclenchement }
    }

    /**
     * Marque une alerte comme déclenchée et calcule le prochain déclenchement
     *
     * @param alerteId ID de l'alerte
     * @param dateActuelle Date du déclenchement (optionnelle, par défaut maintenant)
     * @return true si la mise à jour a réussi
     */
    fun marquerCommeDeclenche(alerteId: Int, dateActuelle: LocalDateTime = LocalDateTime.now()): Boolean {
        val alerte = findById(alerteId) ?: return false

        val prochainDeclenchement = calculerProchainDeclenchement(
            dateActuelle,
            UniteIntervalle.valueOf(alerte.uniteIntervalleAlerte),
            alerte.valeurIntervalleAlerte
        )

        return try {
            val rowsAffected = ArkaDatabase.instance.update(Alertes) {
                set(it.dateDernierDeclenchement, dateActuelle)
                set(it.dateProchainDeclenchement, prochainDeclenchement)
                where { it.alerteId eq alerteId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour de l'alerte $alerteId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR TYPE ET CATÉGORIE
    // ================================================================

    /**
     * Récupère les alertes par type
     *
     * @param typeAlerte Type d'alerte
     * @param membreFamilleId ID du membre (optionnel)
     * @return Liste des alertes du type spécifié
     */
    fun getAlertesByType(typeAlerte: String, membreFamilleId: Int? = null): List<AlerteEntity> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter { alerte ->
                var condition = alerte.typeAlerte eq typeAlerte

                membreFamilleId?.let {
                    condition = condition and (alerte.membreFamilleId eq it)
                }

                condition
            }
            .toList()
            .sortedBy { it.dateCreationAlerte ?: LocalDateTime.MIN }
    }

    /**
     * Récupère les alertes par catégorie
     *
     * @param categorieAlerte Catégorie d'alerte
     * @param membreFamilleId ID du membre (optionnel)
     * @return Liste des alertes de la catégorie spécifiée
     */
    fun getAlertesByCategorie(categorieAlerte: String, membreFamilleId: Int? = null): List<AlerteEntity> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .filter { alerte ->
                var condition = alerte.categorieAlerte eq categorieAlerte

                membreFamilleId?.let {
                    condition = condition and (alerte.membreFamilleId eq it)
                }

                condition
            }
            .toList()
            .sortedBy { it.dateCreationAlerte ?: LocalDateTime.MIN }
    }

    /**
     * Récupère tous les types d'alertes uniques
     *
     * @return Liste des types d'alertes
     */
    fun getAllTypesAlertes(): List<String> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .toList()
            .map { it.typeAlerte }
            .distinct()
            .sorted()
    }

    /**
     * Récupère toutes les catégories d'alertes uniques
     *
     * @return Liste des catégories d'alertes
     */
    fun getAllCategoriesAlertes(): List<String> {
        return ArkaDatabase.instance.sequenceOf(Alertes)
            .toList()
            .mapNotNull { it.categorieAlerte }
            .distinct()
            .sorted()
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET CONFIGURATION
    // ================================================================

    /**
     * Crée une nouvelle alerte avec validation
     *
     * @param typeAlerte Type d'alerte
     * @param categorieAlerte Catégorie (optionnelle)
     * @param uniteIntervalle Unité de l'intervalle
     * @param valeurIntervalle Valeur de l'intervalle
     * @param membreFamilleId ID du membre propriétaire
     * @param premierDeclenchement Date du premier déclenchement (optionnelle)
     * @return L'alerte créée ou null en cas d'erreur
     */
    fun createAlerte(
        typeAlerte: String,
        categorieAlerte: String?,
        uniteIntervalle: UniteIntervalle,
        valeurIntervalle: Int,
        membreFamilleId: Int,
        premierDeclenchement: LocalDateTime? = null
    ): AlerteEntity? {
        // Validation de base
        if (typeAlerte.isBlank()) {
            println("Le type d'alerte ne peut pas être vide")
            return null
        }

        if (valeurIntervalle <= 0) {
            println("La valeur de l'intervalle doit être positive")
            return null
        }

        val maintenant = LocalDateTime.now()
        val prochainDeclenchement = premierDeclenchement ?: calculerProchainDeclenchement(
            maintenant, uniteIntervalle, valeurIntervalle
        )

        return try {
            val alerte = AlerteEntity {
                this.typeAlerte = typeAlerte.trim()
                this.categorieAlerte = categorieAlerte?.trim()
                this.uniteIntervalleAlerte = uniteIntervalle.name
                this.valeurIntervalleAlerte = valeurIntervalle
                this.dateProchainDeclenchement = prochainDeclenchement
                this.estActive = true
                this.dateCreationAlerte = maintenant
                this.membreFamilleId = membreFamilleId
            }

            create(alerte)
        } catch (e: Exception) {
            println("Erreur lors de la création de l'alerte: ${e.message}")
            null
        }
    }

    /**
     * Met à jour la configuration d'une alerte
     *
     * @param alerteId ID de l'alerte
     * @param uniteIntervalle Nouvelle unité d'intervalle
     * @param valeurIntervalle Nouvelle valeur d'intervalle
     * @param recalculerProchainDeclenchement Recalculer la prochaine date
     * @return true si la mise à jour a réussi
     */
    fun updateConfigurationAlerte(
        alerteId: Int,
        uniteIntervalle: UniteIntervalle,
        valeurIntervalle: Int,
        recalculerProchainDeclenchement: Boolean = true
    ): Boolean {
        if (valeurIntervalle <= 0) {
            println("La valeur de l'intervalle doit être positive")
            return false
        }

        val alerte = findById(alerteId) ?: return false

        val nouvelleDate = if (recalculerProchainDeclenchement) {
            calculerProchainDeclenchement(
                alerte.dateDernierDeclenchement ?: LocalDateTime.now(),
                uniteIntervalle,
                valeurIntervalle
            )
        } else {
            alerte.dateProchainDeclenchement
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(Alertes) {
                set(it.uniteIntervalleAlerte, uniteIntervalle.name)
                set(it.valeurIntervalleAlerte, valeurIntervalle)
                set(it.dateProchainDeclenchement, nouvelleDate)
                where { it.alerteId eq alerteId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour de la configuration de l'alerte $alerteId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES D'ACTIVATION/DÉSACTIVATION
    // ================================================================

    /**
     * Active ou désactive une alerte
     *
     * @param alerteId ID de l'alerte
     * @param estActive Nouvel état actif
     * @return true si la mise à jour a réussi
     */
    fun toggleAlerte(alerteId: Int, estActive: Boolean): Boolean {
        return try {
            val rowsAffected = ArkaDatabase.instance.update(Alertes) {
                set(it.estActive, estActive)
                where { it.alerteId eq alerteId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors du toggle de l'alerte $alerteId: ${e.message}")
            false
        }
    }

    /**
     * Active toutes les alertes d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Nombre d'alertes activées
     */
    fun activerToutesAlertes(membreFamilleId: Int): Int {
        return try {
            ArkaDatabase.instance.update(Alertes) {
                set(it.estActive, true)
                where {
                    (it.membreFamilleId eq membreFamilleId) and (it.estActive eq false)
                }
            }
        } catch (e: Exception) {
            println("Erreur lors de l'activation des alertes du membre $membreFamilleId: ${e.message}")
            0
        }
    }

    /**
     * Désactive toutes les alertes d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Nombre d'alertes désactivées
     */
    fun desactiverToutesAlertes(membreFamilleId: Int): Int {
        return try {
            ArkaDatabase.instance.update(Alertes) {
                set(it.estActive, false)
                where {
                    (it.membreFamilleId eq membreFamilleId) and (it.estActive eq true)
                }
            }
        } catch (e: Exception) {
            println("Erreur lors de la désactivation des alertes du membre $membreFamilleId: ${e.message}")
            0
        }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques des alertes d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Map avec les statistiques
     */
    fun getStatistiquesAlertes(membreFamilleId: Int): Map<String, Any?> {  // Changé Any en Any?
        val alertes = getAlertesByMembre(membreFamilleId)
        val maintenant = LocalDateTime.now()

        val actives = alertes.count { it.estActive }
        val inactives = alertes.count { !it.estActive }
        val declencheesAujourdHui = alertes.count {
            it.dateDernierDeclenchement?.toLocalDate() == maintenant.toLocalDate()
        }
        val prochaineAlerte = alertes
            .filter { it.estActive && it.dateProchainDeclenchement != null }
            .minByOrNull { it.dateProchainDeclenchement!! }

        return mapOf(
            "total" to alertes.size,
            "actives" to actives,
            "inactives" to inactives,
            "declencheesAujourdHui" to declencheesAujourdHui,
            "prochaineAlerte" to prochaineAlerte?.dateProchainDeclenchement
        )
    }

    /**
     * Obtient les statistiques par type d'alerte
     *
     * @param membreFamilleId ID du membre (optionnel)
     * @return Map type -> nombre d'alertes
     */
    fun getStatistiquesParType(membreFamilleId: Int? = null): Map<String, Int> {
        val alertes = if (membreFamilleId != null) {
            getAlertesByMembre(membreFamilleId)
        } else {
            findAll()
        }

        return alertes
            .groupBy { it.typeAlerte }
            .mapValues { it.value.size }
    }

    /**
     * Obtient les statistiques par unité d'intervalle
     *
     * @param membreFamilleId ID du membre (optionnel)
     * @return Map unité -> nombre d'alertes
     */
    fun getStatistiquesParIntervalle(membreFamilleId: Int? = null): Map<UniteIntervalle, Int> {
        val alertes = if (membreFamilleId != null) {
            getAlertesByMembre(membreFamilleId)
        } else {
            findAll()
        }

        return alertes
            .groupBy { UniteIntervalle.valueOf(it.uniteIntervalleAlerte) }
            .mapValues { it.value.size }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION ET NETTOYAGE
    // ================================================================

    /**
     * Supprime toutes les alertes d'un membre
     * Utilisé lors de la suppression d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Nombre d'alertes supprimées
     */
    fun deleteAllByMembre(membreFamilleId: Int): Int {
        return try {
            ArkaDatabase.instance.delete(Alertes) {
                it.membreFamilleId eq membreFamilleId
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression des alertes du membre $membreFamilleId: ${e.message}")
            0
        }
    }

    /**
     * Supprime les alertes anciennes et inactives
     *
     * @param daysThreshold Nombre de jours après désactivation
     * @return Nombre d'alertes supprimées
     */
    fun cleanOldInactiveAlerts(daysThreshold: Int = 365): Int {
        return try {
            val threshold = LocalDateTime.now().minusDays(daysThreshold.toLong())
            ArkaDatabase.instance.delete(Alertes) {
                (it.estActive eq false) and
                        (it.dateCreationAlerte less threshold)
            }
        } catch (e: Exception) {
            println("Erreur lors du nettoyage des anciennes alertes: ${e.message}")
            0
        }
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Calcule la prochaine date de déclenchement
     *
     * @param dateBase Date de base pour le calcul
     * @param unite Unité de l'intervalle
     * @param valeur Valeur de l'intervalle
     * @return Prochaine date de déclenchement
     */
    private fun calculerProchainDeclenchement(
        dateBase: LocalDateTime,
        unite: UniteIntervalle,
        valeur: Int
    ): LocalDateTime {
        return when (unite) {
            UniteIntervalle.JOUR -> dateBase.plusDays(valeur.toLong())
            UniteIntervalle.SEMAINE -> dateBase.plusWeeks(valeur.toLong())
            UniteIntervalle.MOIS -> dateBase.plusMonths(valeur.toLong())
            UniteIntervalle.ANNEE -> dateBase.plusYears(valeur.toLong())
        }
    }

    /**
     * Valide les paramètres d'une alerte
     *
     * @param typeAlerte Type d'alerte
     * @param valeurIntervalle Valeur de l'intervalle
     * @return Liste des erreurs de validation
     */
    fun validateAlerte(typeAlerte: String, valeurIntervalle: Int): List<String> {
        val errors = mutableListOf<String>()

        if (typeAlerte.isBlank()) {
            errors.add("Le type d'alerte ne peut pas être vide")
        }

        if (typeAlerte.length > 50) {
            errors.add("Le type d'alerte ne peut pas dépasser 50 caractères")
        }

        if (valeurIntervalle <= 0) {
            errors.add("La valeur de l'intervalle doit être positive")
        }

        if (valeurIntervalle > 365) {
            errors.add("La valeur de l'intervalle ne peut pas dépasser 365")
        }

        return errors
    }
}