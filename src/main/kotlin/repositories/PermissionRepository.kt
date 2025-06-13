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
 * Repository pour la gestion des permissions actives
 *
 * Responsabilités:
 * - CRUD des permissions accordées
 * - Vérification des droits d'accès (méthode critique hasPermission)
 * - Gestion des expirations automatiques
 * - Statistiques et audit des permissions
 * - Nettoyage et maintenance des permissions
 *
 * Utilisé par: PermissionController, DelegationController, tous les controllers pour vérification d'accès
 */
class PermissionRepository : BaseRepository<PermissionActiveEntity, PermissionsActives>() {

    override val table = PermissionsActives

    /**
     * Obtient la clé primaire d'une permission
     */
    override fun PermissionActiveEntity.getPrimaryKey(): Int = this.permissionId
    override fun getPrimaryKeyColumn(): Column<Int> = PermissionsActives.permissionId

    /**
     * Met à jour une permission
     */
    override fun update(entity: PermissionActiveEntity): Int {
        return ArkaDatabase.instance.update(PermissionsActives) {
            set(it.estActive, entity.estActive)
            set(it.dateExpiration, entity.dateExpiration)
            set(it.typePermission, entity.typePermission)
            where { it.permissionId eq entity.permissionId }
        }
    }

    // ================================================================
    // MÉTHODES DE VÉRIFICATION D'ACCÈS (CRITIQUES)
    // ================================================================

    /**
     * Vérifie si un bénéficiaire a une permission spécifique
     * MÉTHODE CRITIQUE utilisée par tous les controllers pour l'autorisation
     *
     * @param beneficiaireId ID du bénéficiaire
     * @param portee Portée de la permission
     * @param cibleId ID de la cible (optionnel selon la portée)
     * @param typePermission Type de permission requis
     * @return true si la permission existe et est active
     */
    fun hasPermission(
        beneficiaireId: Int,
        portee: PorteePermission,
        cibleId: Int?,
        typePermission: TypePermission
    ): Boolean {
        return try {
            val maintenant = LocalDateTime.now()

            // Rechercher une permission active qui couvre le type demandé
            val permission = ArkaDatabase.instance.sequenceOf(PermissionsActives)
                .find { permission ->
                    val baseCondition = (permission.beneficiaireId eq beneficiaireId) and
                            (permission.portee eq portee.name) and
                            (permission.estActive eq true)

                    // Gérer la cible (null ou valeur)
                    val targetCondition = if (cibleId != null) {
                        baseCondition and (permission.cibleId eq cibleId)
                    } else {
                        baseCondition and permission.cibleId.isNull()
                    }

                    // Vérifier l'expiration
                    val expirationCondition = targetCondition and
                            (permission.dateExpiration.isNull() or (permission.dateExpiration greater maintenant))

                    // Vérifier la hiérarchie des permissions
                    val permissionTypeCondition = when (typePermission) {
                        TypePermission.LECTURE -> permission.typePermission inList listOf(
                            TypePermission.LECTURE.name,
                            TypePermission.ECRITURE.name,
                            TypePermission.SUPPRESSION.name,
                            TypePermission.ACCES_COMPLET.name
                        )
                        TypePermission.ECRITURE -> permission.typePermission inList listOf(
                            TypePermission.ECRITURE.name,
                            TypePermission.SUPPRESSION.name,
                            TypePermission.ACCES_COMPLET.name
                        )
                        TypePermission.SUPPRESSION -> permission.typePermission inList listOf(
                            TypePermission.SUPPRESSION.name,
                            TypePermission.ACCES_COMPLET.name
                        )
                        TypePermission.ACCES_COMPLET -> permission.typePermission eq TypePermission.ACCES_COMPLET.name
                    }

                    expirationCondition and permissionTypeCondition
                }

            permission != null
        } catch (e: Exception) {
            println("Erreur lors de la vérification de permission: ${e.message}")
            false
        }
    }

    /**
     * Vérifie les permissions pour plusieurs cibles à la fois
     *
     * @param beneficiaireId ID du bénéficiaire
     * @param checks Liste des vérifications à effectuer
     * @return Map avec les résultats pour chaque vérification
     */
    fun hasPermissions(
        beneficiaireId: Int,
        checks: List<PermissionCheck>
    ): Map<PermissionCheck, Boolean> {
        return checks.associateWith { check ->
            hasPermission(beneficiaireId, check.portee, check.cibleId, check.typePermission)
        }
    }

    /**
     * Récupère toutes les permissions actives d'un bénéficiaire
     *
     * @param beneficiaireId ID du bénéficiaire
     * @return Liste des permissions actives non expirées
     */
    fun getPermissionsActivesByBeneficiaire(beneficiaireId: Int): List<PermissionActiveEntity> {
        val maintenant = LocalDateTime.now()
        return ArkaDatabase.instance.sequenceOf(PermissionsActives)
            .filter {
                (it.beneficiaireId eq beneficiaireId) and
                        (it.estActive eq true) and
                        (it.dateExpiration.isNull() or (it.dateExpiration greater maintenant))
            }
            .toList()
            .sortedByDescending { it.dateOctroi ?: LocalDateTime.MIN }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR PROPRIÉTAIRE
    // ================================================================

    /**
     * Récupère toutes les permissions accordées par un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @param includeInactive Inclure les permissions inactives
     * @return Liste des permissions accordées
     */
    fun getPermissionsAccordeesParProprietaire(proprietaireId: Int, includeInactive: Boolean = false): List<PermissionActiveEntity> {
        return ArkaDatabase.instance.sequenceOf(PermissionsActives)
            .filter { permission ->
                val baseCondition = permission.proprietaireId eq proprietaireId
                if (includeInactive) {
                    baseCondition
                } else {
                    baseCondition and (permission.estActive eq true)
                }
            }
            .toList()
            .sortedByDescending { it.dateOctroi ?: LocalDateTime.MIN }
    }

    /**
     * Compte les permissions accordées par un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @param actives Compter seulement les permissions actives
     * @return Nombre de permissions accordées
     */
    fun countPermissionsAccordees(proprietaireId: Int, actives: Boolean = true): Int {
        return ArkaDatabase.instance.sequenceOf(PermissionsActives)
            .filter { permission ->
                val baseCondition = permission.proprietaireId eq proprietaireId
                if (actives) {
                    baseCondition and (permission.estActive eq true)
                } else {
                    baseCondition
                }
            }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR CIBLE
    // ================================================================

    /**
     * Récupère les permissions pour une cible spécifique
     *
     * @param portee Portée de la permission
     * @param cibleId ID de la cible
     * @return Liste des permissions pour cette cible
     */
    fun getPermissionsByTarget(portee: PorteePermission, cibleId: Int): List<PermissionActiveEntity> {
        return ArkaDatabase.instance.sequenceOf(PermissionsActives)
            .filter {
                (it.portee eq portee.name) and
                        (it.cibleId eq cibleId) and
                        (it.estActive eq true)
            }
            .toList()
            .sortedByDescending { it.dateOctroi ?: LocalDateTime.MIN }
    }

    /**
     * Compte les bénéficiaires ayant accès à une cible
     *
     * @param portee Portée de la permission
     * @param cibleId ID de la cible
     * @return Nombre de bénéficiaires uniques
     */
    fun countBeneficiairesForTarget(portee: PorteePermission, cibleId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(PermissionsActives)
            .filter {
                (it.portee eq portee.name) and
                        (it.cibleId eq cibleId) and
                        (it.estActive eq true)
            }
            .toList()
            .map { it.beneficiaireId }
            .toSet()
            .size
    }

    // ================================================================
    // MÉTHODES DE VALIDATION ET VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si une permission identique existe déjà
     *
     * @param proprietaireId ID du propriétaire
     * @param beneficiaireId ID du bénéficiaire
     * @param portee Portée de la permission
     * @param cibleId ID de la cible (optionnel)
     * @param typePermission Type de permission
     * @return true si une permission identique existe
     */
    fun existsIdenticalPermission(
        proprietaireId: Int,
        beneficiaireId: Int,
        portee: PorteePermission,
        cibleId: Int?,
        typePermission: TypePermission
    ): Boolean {
        return ArkaDatabase.instance.sequenceOf(PermissionsActives)
            .find { permission ->
                val baseCondition = (permission.proprietaireId eq proprietaireId) and
                        (permission.beneficiaireId eq beneficiaireId) and
                        (permission.portee eq portee.name) and
                        (permission.typePermission eq typePermission.name) and
                        (permission.estActive eq true)

                if (cibleId != null) {
                    baseCondition and (permission.cibleId eq cibleId)
                } else {
                    baseCondition and permission.cibleId.isNull()
                }
            } != null
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET GESTION
    // ================================================================

    /**
     * Crée une nouvelle permission avec validation
     *
     * @param proprietaireId ID du propriétaire
     * @param beneficiaireId ID du bénéficiaire
     * @param portee Portée de la permission
     * @param cibleId ID de la cible (optionnel selon la portée)
     * @param typePermission Type de permission
     * @param dateExpiration Date d'expiration (optionnelle)
     * @return La permission créée ou null en cas d'erreur
     */
    fun createPermission(
        proprietaireId: Int,
        beneficiaireId: Int,
        portee: PorteePermission,
        cibleId: Int?,
        typePermission: TypePermission,
        dateExpiration: LocalDateTime?
    ): PermissionActiveEntity? {
        // Validation de base
        if (proprietaireId == beneficiaireId) {
            println("Le propriétaire et le bénéficiaire ne peuvent pas être la même personne")
            return null
        }

        if (dateExpiration != null && dateExpiration.isBefore(LocalDateTime.now())) {
            println("La date d'expiration ne peut pas être dans le passé")
            return null
        }

        // Vérifier qu'il n'y a pas déjà une permission identique active
        if (existsIdenticalPermission(proprietaireId, beneficiaireId, portee, cibleId, typePermission)) {
            println("Une permission identique est déjà active")
            return null
        }

        return try {
            val permission = PermissionActiveEntity {
                this.proprietaireId = proprietaireId
                this.beneficiaireId = beneficiaireId
                this.portee = portee.name
                this.cibleId = cibleId
                this.typePermission = typePermission.name
                this.dateOctroi = LocalDateTime.now()
                this.dateExpiration = dateExpiration
                this.estActive = true
            }

            create(permission)
        } catch (e: Exception) {
            println("Erreur lors de la création de la permission: ${e.message}")
            null
        }
    }

    /**
     * Désactive une permission
     *
     * @param permissionId ID de la permission
     * @return true si la désactivation a réussi
     */
    fun desactiverPermission(permissionId: Int): Boolean {
        return try {
            val rowsAffected = ArkaDatabase.instance.update(PermissionsActives) {
                set(it.estActive, false)
                where { it.permissionId eq permissionId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la désactivation de la permission $permissionId: ${e.message}")
            false
        }
    }

    /**
     * Réactive une permission
     *
     * @param permissionId ID de la permission
     * @return true si la réactivation a réussi
     */
    fun reactiverPermission(permissionId: Int): Boolean {
        val permission = findById(permissionId) ?: return false

        // Vérifier que la permission n'est pas expirée
        if (permission.dateExpiration != null && permission.dateExpiration!!.isBefore(LocalDateTime.now())) {
            println("Impossible de réactiver une permission expirée")
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(PermissionsActives) {
                set(it.estActive, true)
                where { it.permissionId eq permissionId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la réactivation de la permission $permissionId: ${e.message}")
            false
        }
    }

    /**
     * Met à jour la date d'expiration d'une permission
     *
     * @param permissionId ID de la permission
     * @param nouvelleExpiration Nouvelle date d'expiration (null = jamais)
     * @return true si la mise à jour a réussi
     */
    fun updateExpiration(permissionId: Int, nouvelleExpiration: LocalDateTime?): Boolean {
        if (nouvelleExpiration != null && nouvelleExpiration.isBefore(LocalDateTime.now())) {
            println("La date d'expiration ne peut pas être dans le passé")
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(PermissionsActives) {
                set(it.dateExpiration, nouvelleExpiration)
                where { it.permissionId eq permissionId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour de l'expiration de la permission $permissionId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE RÉVOCATION EN MASSE
    // ================================================================

    /**
     * Révoque toutes les permissions pour une cible spécifique
     *
     * @param portee Portée de la cible
     * @param cibleId ID de la cible
     * @return Nombre de permissions révoquées
     */
    fun revoquerToutesPermissionsPourCible(portee: PorteePermission, cibleId: Int): Int {
        return try {
            ArkaDatabase.instance.update(PermissionsActives) {
                set(it.estActive, false)
                where {
                    (it.portee eq portee.name) and
                            (it.cibleId eq cibleId) and
                            (it.estActive eq true)
                }
            }
        } catch (e: Exception) {
            println("Erreur lors de la révocation des permissions pour la cible $cibleId: ${e.message}")
            0
        }
    }

    /**
     * Révoque toutes les permissions d'un bénéficiaire
     *
     * @param beneficiaireId ID du bénéficiaire
     * @return Nombre de permissions révoquées
     */
    fun revoquerToutesPermissionsBeneficiaire(beneficiaireId: Int): Int {
        return try {
            ArkaDatabase.instance.update(PermissionsActives) {
                set(it.estActive, false)
                where {
                    (it.beneficiaireId eq beneficiaireId) and
                            (it.estActive eq true)
                }
            }
        } catch (e: Exception) {
            println("Erreur lors de la révocation des permissions du bénéficiaire $beneficiaireId: ${e.message}")
            0
        }
    }

    /**
     * Révoque toutes les permissions accordées par un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @return Nombre de permissions révoquées
     */
    fun revoquerToutesPermissionsProprietaire(proprietaireId: Int): Int {
        return try {
            ArkaDatabase.instance.update(PermissionsActives) {
                set(it.estActive, false)
                where {
                    (it.proprietaireId eq proprietaireId) and
                            (it.estActive eq true)
                }
            }
        } catch (e: Exception) {
            println("Erreur lors de la révocation des permissions du propriétaire $proprietaireId: ${e.message}")
            0
        }
    }

    // ================================================================
    // MÉTHODES DE GESTION DES EXPIRATIONS
    // ================================================================

    /**
     * Récupère les permissions expirées qui sont encore actives
     *
     * @return Liste des permissions expirées
     */
    fun getPermissionsExpirees(): List<PermissionActiveEntity> {
        val maintenant = LocalDateTime.now()
        return ArkaDatabase.instance.sequenceOf(PermissionsActives)
            .filter {
                it.dateExpiration.isNotNull() and
                        (it.dateExpiration less maintenant) and
                        (it.estActive eq true)
            }
            .toList()
    }

    /**
     * Nettoie automatiquement les permissions expirées
     *
     * @return Nombre de permissions désactivées
     */
    fun cleanPermissionsExpirees(): Int {
        return try {
            val maintenant = LocalDateTime.now()
            ArkaDatabase.instance.update(PermissionsActives) {
                set(it.estActive, false)
                where {
                    it.dateExpiration.isNotNull() and
                            (it.dateExpiration less maintenant) and
                            (it.estActive eq true)
                }
            }
        } catch (e: Exception) {
            println("Erreur lors du nettoyage des permissions expirées: ${e.message}")
            0
        }
    }

    /**
     * Récupère les permissions qui expireront bientôt
     *
     * @param daysAhead Nombre de jours d'avance pour l'alerte
     * @return Liste des permissions qui expireront bientôt
     */
    fun getPermissionsExpirantBientot(daysAhead: Int = 7): List<PermissionActiveEntity> {
        val maintenant = LocalDateTime.now()
        val seuil = maintenant.plusDays(daysAhead.toLong())

        return ArkaDatabase.instance.sequenceOf(PermissionsActives)
            .filter {
                it.dateExpiration.isNotNull() and
                        (it.dateExpiration greater maintenant) and
                        (it.dateExpiration lessEq seuil) and
                        (it.estActive eq true)
            }
            .toList()
            .sortedBy { it.dateExpiration }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques des permissions
     *
     * @param beneficiaireId ID du bénéficiaire (optionnel)
     * @return Map avec les statistiques
     */
    fun getStatistiquesPermissions(beneficiaireId: Int? = null): Map<String, Any> {
        val permissions = if (beneficiaireId != null) {
            ArkaDatabase.instance.sequenceOf(PermissionsActives)
                .filter { it.beneficiaireId eq beneficiaireId }
                .toList()
        } else {
            findAll()
        }

        val maintenant = LocalDateTime.now()
        val actives = permissions.count {
            it.estActive && (it.dateExpiration == null || it.dateExpiration!!.isAfter(maintenant))
        }
        val inactives = permissions.count { !it.estActive }
        val expirees = permissions.count {
            it.dateExpiration != null && it.dateExpiration!!.isBefore(maintenant)
        }

        return mapOf(
            "total" to permissions.size,
            "actives" to actives,
            "inactives" to inactives,
            "expirees" to expirees
        )
    }

    /**
     * Obtient les statistiques par type de permission
     *
     * @return Map type -> nombre de permissions
     */
    fun getStatistiquesParType(): Map<TypePermission, Int> {
        val permissions = getPermissionsActivesByBeneficiaire(-1).ifEmpty { findAll() }
        return permissions
            .filter { it.estActive }
            .groupBy { TypePermission.valueOf(it.typePermission) }
            .mapValues { it.value.size }
    }

    /**
     * Obtient les statistiques par portée
     *
     * @return Map portée -> nombre de permissions
     */
    fun getStatistiquesParPortee(): Map<PorteePermission, Int> {
        val permissions = findAll()
        return permissions
            .filter { it.estActive }
            .groupBy { PorteePermission.valueOf(it.portee) }
            .mapValues { it.value.size }
    }

    /**
     * Obtient un résumé des permissions d'un bénéficiaire par portée
     *
     * @param beneficiaireId ID du bénéficiaire
     * @return Map des portées et du nombre de permissions
     */
    fun getResumePermissions(beneficiaireId: Int): Map<PorteePermission, Int> {
        val permissions = getPermissionsActivesByBeneficiaire(beneficiaireId)
        return permissions
            .groupBy { PorteePermission.valueOf(it.portee) }
            .mapValues { it.value.size }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION ET NETTOYAGE
    // ================================================================

    /**
     * Supprime toutes les permissions d'un membre
     * Utilisé lors de la suppression d'un membre
     *
     * @param membreId ID du membre
     * @return Nombre de permissions supprimées
     */
    fun deleteAllByMembre(membreId: Int): Int {
        return try {
            val asProprietaire = ArkaDatabase.instance.delete(PermissionsActives) {
                it.proprietaireId eq membreId
            }
            val asBeneficiaire = ArkaDatabase.instance.delete(PermissionsActives) {
                it.beneficiaireId eq membreId
            }
            asProprietaire + asBeneficiaire
        } catch (e: Exception) {
            println("Erreur lors de la suppression des permissions du membre $membreId: ${e.message}")
            0
        }
    }

    /**
     * Archive les anciennes permissions inactives
     *
     * @param daysThreshold Nombre de jours après désactivation
     * @return Nombre de permissions archivées
     */
    fun archiveOldInactivePermissions(daysThreshold: Int = 90): Int {
        return try {
            val threshold = LocalDateTime.now().minusDays(daysThreshold.toLong())
            ArkaDatabase.instance.delete(PermissionsActives) {
                (it.estActive eq false) and
                        (it.dateOctroi less threshold)
            }
        } catch (e: Exception) {
            println("Erreur lors de l'archivage des anciennes permissions: ${e.message}")
            0
        }
    }
}

// ================================================================
// CLASSES DE DONNÉES UTILITAIRES
// ================================================================

/**
 * Classe pour vérifier une permission spécifique
 */
data class PermissionCheck(
    val portee: PorteePermission,
    val cibleId: Int?,
    val typePermission: TypePermission
)