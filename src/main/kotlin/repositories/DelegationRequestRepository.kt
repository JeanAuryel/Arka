package repositories

import ktorm.*
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.sortedByDescending
import org.ktorm.entity.toList
import org.ktorm.schema.Column
import java.time.LocalDateTime

/**
 * Repository pour la gestion des demandes de délégation dans Arka
 */
class DelegationRequestRepository : BaseRepository<DemandeDelegationEntity, org.ktorm.schema.Table<DemandeDelegationEntity>>() {

    override val table = DemandesDelegation

    override fun getIdColumn(entity: DemandeDelegationEntity): Column<Int> = table.demandeId

    /**
     * Trouve toutes les demandes en attente
     * @return Liste des demandes en attente
     */
    fun findPendingRequests(): List<DemandeDelegationEntity> {
        return try {
            entities.filter { table.statut eq StatutDemande.EN_ATTENTE.name }
                .sortedByDescending { table.dateDemande }
                .toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des demandes en attente: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve les demandes d'un propriétaire (demandes reçues)
     * @param proprietaireId L'ID du propriétaire
     * @param statut Optionnel: filtrer par statut
     * @return Liste des demandes
     */
    fun findByOwner(proprietaireId: Int, statut: StatutDemande? = null): List<DemandeDelegationEntity> {
        return try {
            var query = entities.filter { table.proprietaireId eq proprietaireId }

            statut?.let { query = query.filter { table.statut eq it.name } }

            query.sortedByDescending { table.dateDemande }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des demandes du propriétaire $proprietaireId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve les demandes d'un bénéficiaire (demandes faites)
     * @param beneficiaireId L'ID du bénéficiaire
     * @param statut Optionnel: filtrer par statut
     * @return Liste des demandes
     */
    fun findByBeneficiary(beneficiaireId: Int, statut: StatutDemande? = null): List<DemandeDelegationEntity> {
        return try {
            var query = entities.filter { table.beneficiaireId eq beneficiaireId }

            statut?.let { query = query.filter { table.statut eq it.name } }

            query.sortedByDescending { table.dateDemande }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des demandes du bénéficiaire $beneficiaireId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve les demandes pour une cible spécifique
     * @param portee La portée (DOSSIER, FICHIER, etc.)
     * @param cibleId L'ID de la cible
     * @return Liste des demandes pour cette cible
     */
    fun findByTarget(portee: PorteePermission, cibleId: Int): List<DemandeDelegationEntity> {
        return try {
            entities.filter {
                (table.portee eq portee.name) and (table.cibleId eq cibleId)
            }.sortedByDescending { table.dateDemande }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des demandes pour la cible $cibleId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Vérifie si une demande similaire existe déjà
     * @param proprietaireId L'ID du propriétaire
     * @param beneficiaireId L'ID du bénéficiaire
     * @param portee La portée
     * @param cibleId L'ID de la cible
     * @param typePermission Le type de permission
     * @return La demande existante ou null
     */
    fun findExistingRequest(
        proprietaireId: Int,
        beneficiaireId: Int,
        portee: PorteePermission,
        cibleId: Int?,
        typePermission: TypePermission
    ): DemandeDelegationEntity? {
        return try {
            entities.find {
                (table.proprietaireId eq proprietaireId) and
                        (table.beneficiaireId eq beneficiaireId) and
                        (table.portee eq portee.name) and
                        (table.typePermission eq typePermission.name) and
                        (if (cibleId != null) table.cibleId eq cibleId else table.cibleId.isNull()) and
                        (table.statut eq StatutDemande.EN_ATTENTE.name)
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche de demande existante: ${e.message}")
            null
        }
    }

    /**
     * Crée une nouvelle demande de délégation
     * @param requestData Les données de la demande
     * @return Le résultat de l'opération
     */
    fun createDelegationRequest(requestData: CreateDelegationRequestData): RepositoryResult<DemandeDelegation> {
        // Validation
        val validationErrors = validateDelegationRequest(requestData)
        if (validationErrors.isNotEmpty()) {
            return RepositoryResult.ValidationError(validationErrors)
        }

        // Vérifier qu'il n'y a pas déjà une demande similaire en attente
        if (findExistingRequest(
                requestData.proprietaireId,
                requestData.beneficiaireId,
                requestData.portee,
                requestData.cibleId,
                requestData.typePermission
            ) != null
        ) {
            return RepositoryResult.Error("Une demande similaire est déjà en attente")
        }

        return try {
            val demande = DemandeDelegationEntity {
                this.proprietaireId = requestData.proprietaireId
                this.beneficiaireId = requestData.beneficiaireId
                this.portee = requestData.portee.name
                this.cibleId = requestData.cibleId
                this.typePermission = requestData.typePermission.name
                this.dateDemande = LocalDateTime.now()
                this.statut = StatutDemande.EN_ATTENTE.name
                this.raisonDemande = requestData.raisonDemande
                this.dateExpiration = requestData.dateExpiration
            }

            if (save(demande)) {
                RepositoryResult.Success(demande.toModel())
            } else {
                RepositoryResult.Error("Échec de la création de la demande")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la création: ${e.message}")
        }
    }

    /**
     * Approuve une demande de délégation
     * @param demandeId L'ID de la demande
     * @param validateurId L'ID de celui qui valide
     * @param commentaire Commentaire de validation (optionnel)
     * @return Le résultat de l'opération
     */
    fun approveDelegationRequest(
        demandeId: Int,
        validateurId: Int,
        commentaire: String? = null
    ): RepositoryResult<DemandeDelegation> {
        return try {
            val demande = findById(demandeId)
            if (demande == null) {
                return RepositoryResult.Error("Demande non trouvée")
            }

            if (demande.statut != StatutDemande.EN_ATTENTE.name) {
                return RepositoryResult.Error("Cette demande a déjà été traitée")
            }

            demande.statut = StatutDemande.APPROUVEE.name
            demande.dateValidation = LocalDateTime.now()
            demande.valideeParId = validateurId
            demande.commentaireAdmin = commentaire

            if (update(demande)) {
                RepositoryResult.Success(demande.toModel())
            } else {
                RepositoryResult.Error("Échec de l'approbation")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de l'approbation: ${e.message}")
        }
    }

    /**
     * Rejette une demande de délégation
     * @param demandeId L'ID de la demande
     * @param validateurId L'ID de celui qui rejette
     * @param raisonRejet La raison du rejet
     * @return Le résultat de l'opération
     */
    fun rejectDelegationRequest(
        demandeId: Int,
        validateurId: Int,
        raisonRejet: String
    ): RepositoryResult<DemandeDelegation> {
        return try {
            val demande = findById(demandeId)
            if (demande == null) {
                return RepositoryResult.Error("Demande non trouvée")
            }

            if (demande.statut != StatutDemande.EN_ATTENTE.name) {
                return RepositoryResult.Error("Cette demande a déjà été traitée")
            }

            demande.statut = StatutDemande.REJETEE.name
            demande.dateValidation = LocalDateTime.now()
            demande.valideeParId = validateurId
            demande.commentaireAdmin = raisonRejet

            if (update(demande)) {
                RepositoryResult.Success(demande.toModel())
            } else {
                RepositoryResult.Error("Échec du rejet")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors du rejet: ${e.message}")
        }
    }

    /**
     * Révoque une demande approuvée
     * @param demandeId L'ID de la demande
     * @param revoqueuId L'ID de celui qui révoque
     * @param raisonRevocation La raison de la révocation
     * @return Le résultat de l'opération
     */
    fun revokeDelegationRequest(
        demandeId: Int,
        revoqueuId: Int,
        raisonRevocation: String
    ): RepositoryResult<DemandeDelegation> {
        return try {
            val demande = findById(demandeId)
            if (demande == null) {
                return RepositoryResult.Error("Demande non trouvée")
            }

            if (demande.statut != StatutDemande.APPROUVEE.name) {
                return RepositoryResult.Error("Seules les demandes approuvées peuvent être révoquées")
            }

            demande.statut = StatutDemande.REVOQUEE.name
            demande.dateValidation = LocalDateTime.now()
            demande.valideeParId = revoqueuId
            demande.commentaireAdmin = raisonRevocation

            if (update(demande)) {
                RepositoryResult.Success(demande.toModel())
            } else {
                RepositoryResult.Error("Échec de la révocation")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la révocation: ${e.message}")
        }
    }

    /**
     * Obtient les statistiques des demandes
     * @param proprietaireId Optionnel: limiter à un propriétaire
     * @return Les statistiques des demandes
     */
    fun getDelegationStats(proprietaireId: Int? = null): DelegationStats {
        return try {
            val baseQuery = if (proprietaireId != null) {
                database.from(DemandesDelegation).where { DemandesDelegation.proprietaireId eq proprietaireId }
            } else {
                database.from(DemandesDelegation)
            }

            val enAttente = baseQuery.select(count())
                .where { DemandesDelegation.statut eq StatutDemande.EN_ATTENTE.name }
                .map { it.getInt(1) }.first()

            val approuvees = baseQuery.select(count())
                .where { DemandesDelegation.statut eq StatutDemande.APPROUVEE.name }
                .map { it.getInt(1) }.first()

            val rejetees = baseQuery.select(count())
                .where { DemandesDelegation.statut eq StatutDemande.REJETEE.name }
                .map { it.getInt(1) }.first()

            val revoquees = baseQuery.select(count())
                .where { DemandesDelegation.statut eq StatutDemande.REVOQUEE.name }
                .map { it.getInt(1) }.first()

            DelegationStats(
                enAttente = enAttente,
                approuvees = approuvees,
                rejetees = rejetees,
                revoquees = revoquees,
                total = enAttente + approuvees + rejetees + revoquees
            )
        } catch (e: Exception) {
            println("⚠️ Erreur lors du calcul des stats de délégation: ${e.message}")
            DelegationStats(0, 0, 0, 0, 0)
        }
    }

    /**
     * Trouve les demandes expirées qui n'ont pas été traitées
     * @return Liste des demandes expirées
     */
    fun findExpiredRequests(): List<DemandeDelegationEntity> {
        return try {
            entities.filter {
                (table.dateExpiration.isNotNull()) and
                        (table.dateExpiration less LocalDateTime.now()) and
                        (table.statut eq StatutDemande.EN_ATTENTE.name)
            }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des demandes expirées: ${e.message}")
            emptyList()
        }
    }

    /**
     * Nettoie automatiquement les demandes expirées
     * @return Le nombre de demandes nettoyées
     */
    fun cleanExpiredRequests(): Int {
        return try {
            val expiredRequests = findExpiredRequests()
            var cleanedCount = 0

            for (request in expiredRequests) {
                request.statut = StatutDemande.REJETEE.name
                request.dateValidation = LocalDateTime.now()
                request.commentaireAdmin = "Demande expirée automatiquement"

                if (update(request)) {
                    cleanedCount++
                }
            }

            cleanedCount
        } catch (e: Exception) {
            println("⚠️ Erreur lors du nettoyage des demandes expirées: ${e.message}")
            0
        }
    }

    /**
     * Valide une demande de délégation
     */
    private fun validateDelegationRequest(requestData: CreateDelegationRequestData): List<String> {
        val errors = mutableListOf<String>()

        if (requestData.proprietaireId == requestData.beneficiaireId) {
            errors.add("Le propriétaire et le bénéficiaire ne peuvent pas être la même personne")
        }

        if (requestData.raisonDemande?.isBlank() == true) {
            errors.add("La raison de la demande ne peut pas être vide")
        }

        if (requestData.dateExpiration != null && requestData.dateExpiration.isBefore(LocalDateTime.now())) {
            errors.add("La date d'expiration ne peut pas être dans le passé")
        }

        // Validation selon la portée
        when (requestData.portee) {
            PorteePermission.DOSSIER, PorteePermission.FICHIER -> {
                if (requestData.cibleId == null) {
                    errors.add("L'ID de la cible est requis pour cette portée")
                }
            }
            PorteePermission.CATEGORIE -> {
                if (requestData.cibleId == null) {
                    errors.add("L'ID de la catégorie est requis")
                }
            }
            PorteePermission.ESPACE_COMPLET -> {
                // Pas de cible requise pour l'espace complet
            }
        }

        return errors
    }

    override fun validate(entity: DemandeDelegationEntity): List<String> {
        val requestData = CreateDelegationRequestData(
            proprietaireId = entity.proprietaireId,
            beneficiaireId = entity.beneficiaireId,
            portee = PorteePermission.valueOf(entity.portee),
            cibleId = entity.cibleId,
            typePermission = TypePermission.valueOf(entity.typePermission),
            raisonDemande = entity.raisonDemande,
            dateExpiration = entity.dateExpiration
        )
        return validateDelegationRequest(requestData)
    }
}

/**
 * Données pour créer une demande de délégation
 */
data class CreateDelegationRequestData(
    val proprietaireId: Int,
    val beneficiaireId: Int,
    val portee: PorteePermission,
    val cibleId: Int?,
    val typePermission: TypePermission,
    val raisonDemande: String?,
    val dateExpiration: LocalDateTime?
)

/**
 * Statistiques des demandes de délégation
 */
data class DelegationStats(
    val enAttente: Int,
    val approuvees: Int,
    val rejetees: Int,
    val revoquees: Int,
    val total: Int
)
