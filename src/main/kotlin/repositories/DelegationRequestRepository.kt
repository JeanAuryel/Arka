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
 * Repository pour la gestion des demandes de délégation
 *
 * Responsabilités:
 * - CRUD des demandes de délégation
 * - Gestion du workflow des demandes (création, approbation, rejet, révocation)
 * - Recherche et filtrage des demandes
 * - Statistiques et audit des demandes
 * - Nettoyage automatique des demandes expirées
 *
 * Utilisé par: DelegationController, PermissionController, AdminController
 */
class DelegationRequestRepository : BaseRepository<DemandeDelegationEntity, DemandesDelegation>() {

    override val table = DemandesDelegation

    /**
     * Obtient la clé primaire d'une demande de délégation
     */
    override fun DemandeDelegationEntity.getPrimaryKey(): Int = this.demandeId
    override fun getPrimaryKeyColumn(): Column<Int> = DemandesDelegation.demandeId

    /**
     * Met à jour une demande de délégation
     */
    override fun update(entity: DemandeDelegationEntity): Int {
        return ArkaDatabase.instance.update(DemandesDelegation) {
            set(it.statut, entity.statut)
            set(it.dateValidation, entity.dateValidation)
            set(it.valideeParId, entity.valideeParId)
            set(it.commentaireAdmin, entity.commentaireAdmin)
            where { it.demandeId eq entity.demandeId }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR STATUT
    // ================================================================

    /**
     * Récupère toutes les demandes en attente
     *
     * @return Liste des demandes en attente triées par date
     */
    fun getDemandesEnAttente(): List<DemandeDelegationEntity> {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter { it.statut eq StatutDemande.EN_ATTENTE.name }
            .toList()
            .sortedByDescending { it.dateDemande ?: LocalDateTime.MIN }
    }

    /**
     * Récupère les demandes par statut
     *
     * @param statut Statut des demandes
     * @return Liste des demandes du statut spécifié
     */
    fun getDemandesByStatut(statut: StatutDemande): List<DemandeDelegationEntity> {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter { it.statut eq statut.name }
            .toList()
            .sortedByDescending { it.dateDemande ?: LocalDateTime.MIN }
    }

    /**
     * Compte les demandes par statut
     *
     * @param statut Statut à compter
     * @return Nombre de demandes
     */
    fun countByStatut(statut: StatutDemande): Int {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter { it.statut eq statut.name }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR UTILISATEUR
    // ================================================================

    /**
     * Récupère les demandes d'un propriétaire (demandes reçues)
     *
     * @param proprietaireId ID du propriétaire
     * @param statut Statut optionnel pour filtrer
     * @return Liste des demandes reçues
     */
    fun getDemandesByProprietaire(proprietaireId: Int, statut: StatutDemande? = null): List<DemandeDelegationEntity> {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter { demande ->
                val baseCondition = demande.proprietaireId eq proprietaireId
                if (statut != null) {
                    baseCondition and (demande.statut eq statut.name)
                } else {
                    baseCondition
                }
            }
            .toList()
            .sortedByDescending { it.dateDemande ?: LocalDateTime.MIN }
    }

    /**
     * Récupère les demandes d'un bénéficiaire (demandes faites)
     *
     * @param beneficiaireId ID du bénéficiaire
     * @param statut Statut optionnel pour filtrer
     * @return Liste des demandes faites
     */
    fun getDemandesByBeneficiaire(beneficiaireId: Int, statut: StatutDemande? = null): List<DemandeDelegationEntity> {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter { demande ->
                val baseCondition = demande.beneficiaireId eq beneficiaireId
                if (statut != null) {
                    baseCondition and (demande.statut eq statut.name)
                } else {
                    baseCondition
                }
            }
            .toList()
            .sortedByDescending { it.dateDemande ?: LocalDateTime.MIN }
    }

    /**
     * Compte les demandes en attente d'un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @return Nombre de demandes en attente
     */
    fun countDemandesEnAttenteByProprietaire(proprietaireId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter {
                (it.proprietaireId eq proprietaireId) and (it.statut eq StatutDemande.EN_ATTENTE.name)
            }
            .toList()
            .size
    }

    /**
     * Compte les demandes d'un bénéficiaire par statut
     *
     * @param beneficiaireId ID du bénéficiaire
     * @param statut Statut à compter
     * @return Nombre de demandes
     */
    fun countDemandesByBeneficiaireAndStatut(beneficiaireId: Int, statut: StatutDemande): Int {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter {
                (it.beneficiaireId eq beneficiaireId) and (it.statut eq statut.name)
            }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR CIBLE
    // ================================================================

    /**
     * Récupère les demandes pour une cible spécifique
     *
     * @param portee Portée de la permission
     * @param cibleId ID de la cible
     * @return Liste des demandes pour cette cible
     */
    fun getDemandesByTarget(portee: PorteePermission, cibleId: Int): List<DemandeDelegationEntity> {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter {
                (it.portee eq portee.name) and (it.cibleId eq cibleId)
            }
            .toList()
            .sortedByDescending { it.dateDemande ?: LocalDateTime.MIN }
    }

    /**
     * Récupère les demandes par portée
     *
     * @param portee Portée des permissions
     * @return Liste des demandes de cette portée
     */
    fun getDemandesByPortee(portee: PorteePermission): List<DemandeDelegationEntity> {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter { it.portee eq portee.name }
            .toList()
            .sortedByDescending { it.dateDemande ?: LocalDateTime.MIN }
    }

    // ================================================================
    // MÉTHODES DE VALIDATION ET VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si une demande similaire existe déjà en attente
     *
     * @param proprietaireId ID du propriétaire
     * @param beneficiaireId ID du bénéficiaire
     * @param portee Portée de la permission
     * @param cibleId ID de la cible (optionnel)
     * @param typePermission Type de permission
     * @return La demande existante ou null
     */
    fun findExistingRequest(
        proprietaireId: Int,
        beneficiaireId: Int,
        portee: PorteePermission,
        cibleId: Int?,
        typePermission: TypePermission
    ): DemandeDelegationEntity? {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .find { demande ->
                val baseCondition = (demande.proprietaireId eq proprietaireId) and
                        (demande.beneficiaireId eq beneficiaireId) and
                        (demande.portee eq portee.name) and
                        (demande.typePermission eq typePermission.name) and
                        (demande.statut eq StatutDemande.EN_ATTENTE.name)

                if (cibleId != null) {
                    baseCondition and (demande.cibleId eq cibleId)
                } else {
                    baseCondition and demande.cibleId.isNull()
                }
            }
    }

    /**
     * Vérifie si une demande peut être approuvée
     *
     * @param demandeId ID de la demande
     * @return true si la demande peut être approuvée
     */
    fun canBeApproved(demandeId: Int): Boolean {
        val demande = findById(demandeId) ?: return false
        return demande.statut == StatutDemande.EN_ATTENTE.name
    }

    /**
     * Vérifie si une demande peut être révoquée
     *
     * @param demandeId ID de la demande
     * @return true si la demande peut être révoquée
     */
    fun canBeRevoked(demandeId: Int): Boolean {
        val demande = findById(demandeId) ?: return false
        return demande.statut == StatutDemande.APPROUVEE.name
    }

    // ================================================================
    // MÉTHODES DE GESTION DU WORKFLOW
    // ================================================================

    /**
     * Crée une nouvelle demande de délégation avec validation
     *
     * @param proprietaireId ID du propriétaire
     * @param beneficiaireId ID du bénéficiaire
     * @param portee Portée de la permission
     * @param cibleId ID de la cible (optionnel selon la portée)
     * @param typePermission Type de permission demandé
     * @param raisonDemande Raison de la demande
     * @param dateExpiration Date d'expiration (optionnelle)
     * @return La demande créée ou null en cas d'erreur
     */
    fun createDemandeDelegation(
        proprietaireId: Int,
        beneficiaireId: Int,
        portee: PorteePermission,
        cibleId: Int?,
        typePermission: TypePermission,
        raisonDemande: String?,
        dateExpiration: LocalDateTime?
    ): DemandeDelegationEntity? {
        // Validation de base
        if (proprietaireId == beneficiaireId) {
            println("Le propriétaire et le bénéficiaire ne peuvent pas être la même personne")
            return null
        }

        if (dateExpiration != null && dateExpiration.isBefore(LocalDateTime.now())) {
            println("La date d'expiration ne peut pas être dans le passé")
            return null
        }

        // Vérifier qu'il n'y a pas déjà une demande similaire en attente
        if (findExistingRequest(proprietaireId, beneficiaireId, portee, cibleId, typePermission) != null) {
            println("Une demande similaire est déjà en attente")
            return null
        }

        return try {
            val demande = DemandeDelegationEntity {
                this.proprietaireId = proprietaireId
                this.beneficiaireId = beneficiaireId
                this.portee = portee.name
                this.cibleId = cibleId
                this.typePermission = typePermission.name
                this.dateDemande = LocalDateTime.now()
                this.statut = StatutDemande.EN_ATTENTE.name
                this.raisonDemande = raisonDemande
                this.dateExpiration = dateExpiration
            }

            create(demande)
        } catch (e: Exception) {
            println("Erreur lors de la création de la demande: ${e.message}")
            null
        }
    }

    /**
     * Approuve une demande de délégation
     *
     * @param demandeId ID de la demande
     * @param validateurId ID de celui qui valide
     * @param commentaire Commentaire de validation (optionnel)
     * @return true si l'approbation a réussi
     */
    fun approuverDemande(demandeId: Int, validateurId: Int, commentaire: String? = null): Boolean {
        val demande = findById(demandeId) ?: return false

        if (demande.statut != StatutDemande.EN_ATTENTE.name) {
            println("Cette demande a déjà été traitée")
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(DemandesDelegation) {
                set(it.statut, StatutDemande.APPROUVEE.name)
                set(it.dateValidation, LocalDateTime.now())
                set(it.valideeParId, validateurId)
                set(it.commentaireAdmin, commentaire)
                where { it.demandeId eq demandeId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de l'approbation de la demande $demandeId: ${e.message}")
            false
        }
    }

    /**
     * Rejette une demande de délégation
     *
     * @param demandeId ID de la demande
     * @param validateurId ID de celui qui rejette
     * @param raisonRejet Raison du rejet
     * @return true si le rejet a réussi
     */
    fun rejeterDemande(demandeId: Int, validateurId: Int, raisonRejet: String): Boolean {
        val demande = findById(demandeId) ?: return false

        if (demande.statut != StatutDemande.EN_ATTENTE.name) {
            println("Cette demande a déjà été traitée")
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(DemandesDelegation) {
                set(it.statut, StatutDemande.REJETEE.name)
                set(it.dateValidation, LocalDateTime.now())
                set(it.valideeParId, validateurId)
                set(it.commentaireAdmin, raisonRejet)
                where { it.demandeId eq demandeId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors du rejet de la demande $demandeId: ${e.message}")
            false
        }
    }

    /**
     * Révoque une demande approuvée
     *
     * @param demandeId ID de la demande
     * @param revoqueurId ID de celui qui révoque
     * @param raisonRevocation Raison de la révocation
     * @return true si la révocation a réussi
     */
    fun revoquerDemande(demandeId: Int, revoqueurId: Int, raisonRevocation: String): Boolean {
        val demande = findById(demandeId) ?: return false

        if (demande.statut != StatutDemande.APPROUVEE.name) {
            println("Seules les demandes approuvées peuvent être révoquées")
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(DemandesDelegation) {
                set(it.statut, StatutDemande.REVOQUEE.name)
                set(it.dateValidation, LocalDateTime.now())
                set(it.valideeParId, revoqueurId)
                set(it.commentaireAdmin, raisonRevocation)
                where { it.demandeId eq demandeId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la révocation de la demande $demandeId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE AVANCÉE
    // ================================================================

    /**
     * Recherche des demandes par période
     *
     * @param since Date de début
     * @param until Date de fin (optionnelle)
     * @param statut Statut optionnel pour filtrer
     * @return Liste des demandes de la période
     */
    fun getDemandesParPeriode(
        since: LocalDateTime,
        until: LocalDateTime? = null,
        statut: StatutDemande? = null
    ): List<DemandeDelegationEntity> {
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter { demande ->
                var condition = demande.dateDemande greaterEq since

                until?.let {
                    condition = condition and (demande.dateDemande lessEq it)
                }
                statut?.let {
                    condition = condition and (demande.statut eq it.name)
                }

                condition
            }
            .toList()
            .sortedByDescending { it.dateDemande ?: LocalDateTime.MIN }
    }

    /**
     * Récupère les demandes récentes
     *
     * @param limit Nombre maximum de demandes
     * @param statut Statut optionnel pour filtrer
     * @return Liste des demandes récentes
     */
    fun getDemandesRecentes(limit: Int = 10, statut: StatutDemande? = null): List<DemandeDelegationEntity> {
        val demandes = if (statut != null) {
            getDemandesByStatut(statut)
        } else {
            findAll()
        }

        return demandes
            .sortedByDescending { it.dateDemande ?: LocalDateTime.MIN }
            .take(limit)
    }

    // ================================================================
    // MÉTHODES DE GESTION DES EXPIRATIONS
    // ================================================================

    /**
     * Récupère les demandes expirées qui n'ont pas été traitées
     *
     * @return Liste des demandes expirées
     */
    fun getDemandesExpirees(): List<DemandeDelegationEntity> {
        val maintenant = LocalDateTime.now()
        return ArkaDatabase.instance.sequenceOf(DemandesDelegation)
            .filter {
                it.dateExpiration.isNotNull() and
                        (it.dateExpiration less maintenant) and
                        (it.statut eq StatutDemande.EN_ATTENTE.name)
            }
            .toList()
    }

    /**
     * Nettoie automatiquement les demandes expirées
     *
     * @return Nombre de demandes nettoyées
     */
    fun cleanDemandesExpirees(): Int {
        return try {
            val maintenant = LocalDateTime.now()
            ArkaDatabase.instance.update(DemandesDelegation) {
                set(it.statut, StatutDemande.REJETEE.name)
                set(it.dateValidation, maintenant)
                set(it.commentaireAdmin, "Demande expirée automatiquement")
                where {
                    it.dateExpiration.isNotNull() and
                            (it.dateExpiration less maintenant) and
                            (it.statut eq StatutDemande.EN_ATTENTE.name)
                }
            }
        } catch (e: Exception) {
            println("Erreur lors du nettoyage des demandes expirées: ${e.message}")
            0
        }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques des demandes
     *
     * @param proprietaireId ID du propriétaire (optionnel)
     * @return Map avec les statistiques
     */
    fun getStatistiquesDemandes(proprietaireId: Int? = null): Map<String, Any> {
        val demandes = if (proprietaireId != null) {
            getDemandesByProprietaire(proprietaireId)
        } else {
            findAll()
        }

        val enAttente = demandes.count { it.statut == StatutDemande.EN_ATTENTE.name }
        val approuvees = demandes.count { it.statut == StatutDemande.APPROUVEE.name }
        val rejetees = demandes.count { it.statut == StatutDemande.REJETEE.name }
        val revoquees = demandes.count { it.statut == StatutDemande.REVOQUEE.name }

        return mapOf(
            "total" to demandes.size,
            "enAttente" to enAttente,
            "approuvees" to approuvees,
            "rejetees" to rejetees,
            "revoquees" to revoquees,
            "tauxApprobation" to if (demandes.isNotEmpty()) {
                (approuvees.toDouble() / (approuvees + rejetees) * 100).toInt()
            } else 0
        )
    }

    /**
     * Obtient les statistiques par type de permission
     *
     * @return Map type -> nombre de demandes
     */
    fun getStatistiquesParTypePermission(): Map<TypePermission, Int> {
        val demandes = findAll()
        return demandes
            .groupBy { TypePermission.valueOf(it.typePermission) }
            .mapValues { it.value.size }
    }

    /**
     * Obtient les statistiques par portée
     *
     * @return Map portée -> nombre de demandes
     */
    fun getStatistiquesParPortee(): Map<PorteePermission, Int> {
        val demandes = findAll()
        return demandes
            .groupBy { PorteePermission.valueOf(it.portee) }
            .mapValues { it.value.size }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION ET NETTOYAGE
    // ================================================================

    /**
     * Supprime toutes les demandes d'un utilisateur
     * Utilisé lors de la suppression d'un membre
     *
     * @param membreId ID du membre
     * @return Nombre de demandes supprimées
     */
    fun deleteAllByMembre(membreId: Int): Int {
        return try {
            val asProprietaire = ArkaDatabase.instance.delete(DemandesDelegation) {
                it.proprietaireId eq membreId
            }
            val asBeneficiaire = ArkaDatabase.instance.delete(DemandesDelegation) {
                it.beneficiaireId eq membreId
            }
            asProprietaire + asBeneficiaire
        } catch (e: Exception) {
            println("Erreur lors de la suppression des demandes du membre $membreId: ${e.message}")
            0
        }
    }

    /**
     * Archive les anciennes demandes traitées
     *
     * @param daysThreshold Nombre de jours après traitement
     * @return Nombre de demandes archivées
     */
    fun archiveOldProcessedRequests(daysThreshold: Int = 90): Int {
        return try {
            val threshold = LocalDateTime.now().minusDays(daysThreshold.toLong())
            ArkaDatabase.instance.delete(DemandesDelegation) {
                (it.dateValidation.isNotNull()) and
                        (it.dateValidation less threshold) and
                        (it.statut inList listOf(StatutDemande.REJETEE.name, StatutDemande.REVOQUEE.name))
            }
        } catch (e: Exception) {
            println("Erreur lors de l'archivage des anciennes demandes: ${e.message}")
            0
        }
    }
}