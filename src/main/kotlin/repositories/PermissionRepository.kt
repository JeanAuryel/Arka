package repositories

import ktorm.*
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.sortedByDescending
import org.ktorm.entity.toList
import org.ktorm.schema.Column
import java.time.LocalDateTime

/**
 * Repository pour la gestion des permissions actives dans Arka
 */
class PermissionRepository : BaseRepository<PermissionActiveEntity, org.ktorm.schema.Table<PermissionActiveEntity>>() {

    override val table = PermissionsActives

    override fun getIdColumn(entity: PermissionActiveEntity): Column<Int> = table.permissionId

    /**
     * Trouve toutes les permissions actives d'un bénéficiaire
     * @param beneficiaireId L'ID du bénéficiaire
     * @return Liste des permissions actives
     */
    fun findActiveByBeneficiary(beneficiaireId: Int): List<PermissionActiveEntity> {
        return try {
            entities.filter {
                (table.beneficiaireId eq beneficiaireId) and
                        (table.estActive eq true) and
                        (table.dateExpiration.isNull() or (table.dateExpiration greater LocalDateTime.now()))
            }.sortedByDescending { table.dateOctroi }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des permissions du bénéficiaire $beneficiaireId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve toutes les permissions accordées par un propriétaire
     * @param proprietaireId L'ID du propriétaire
     * @return Liste des permissions accordées
     */
    fun findGrantedByOwner(proprietaireId: Int): List<PermissionActiveEntity> {
        return try {
            entities.filter {
                (table.proprietaireId eq proprietaireId) and
                        (table.estActive eq true)
            }.sortedByDescending { table.dateOctroi }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des permissions du propriétaire $proprietaireId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve les permissions pour une cible spécifique
     * @param portee La portée de la permission
     * @param cibleId L'ID de la cible
     * @return Liste des permissions pour cette cible
     */
    fun findByTarget(portee: PorteePermission, cibleId: Int): List<PermissionActiveEntity> {
        return try {
            entities.filter {
                (table.portee eq portee.name) and
                        (table.cibleId eq cibleId) and
                        (table.estActive eq true)
            }.sortedByDescending { table.dateOctroi }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des permissions pour la cible $cibleId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Vérifie si un bénéficiaire a une permission spécifique
     * @param beneficiaireId L'ID du bénéficiaire
     * @param portee La portée de la permission
     * @param cibleId L'ID de la cible (optionnel selon la portée)
     * @param typePermission Le type de permission requis
     * @return true si la permission existe et est active
     */
    fun hasPermission(
        beneficiaireId: Int,
        portee: PorteePermission,
        cibleId: Int?,
        typePermission: TypePermission
    ): Boolean {
        return try {
            val permission = entities.find {
                (table.beneficiaireId eq beneficiaireId) and
                        (table.portee eq portee.name) and
                        (if (cibleId != null) table.cibleId eq cibleId else table.cibleId.isNull()) and
                        (table.estActive eq true) and
                        (table.dateExpiration.isNull() or (table.dateExpiration greater LocalDateTime.now())) and
                        (
                                // Vérifier la hiérarchie des permissions
                                when (typePermission) {
                                    TypePermission.LECTURE -> table.typePermission.inList(
                                        listOf(
                                            TypePermission.LECTURE.name,
                                            TypePermission.ECRITURE.name,
                                            TypePermission.SUPPRESSION.name,
                                            TypePermission.ACCES_COMPLET.name
                                        )
                                    )
                                    TypePermission.ECRITURE -> table.typePermission.inList(
                                        listOf(
                                            TypePermission.ECRITURE.name,
                                            TypePermission.SUPPRESSION.name,
                                            TypePermission.ACCES_COMPLET.name
                                        )
                                    )
                                    TypePermission.SUPPRESSION -> table.typePermission.inList(
                                        listOf(
                                            TypePermission.SUPPRESSION.name,
                                            TypePermission.ACCES_COMPLET.name
                                        )
                                    )
                                    TypePermission.ACCES_COMPLET -> table.typePermission eq TypePermission.ACCES_COMPLET.name
                                }
                                )
            }

            permission != null
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la vérification de permission: ${e.message}")
            false
        }
    }

    /**
     * Crée une nouvelle permission active
     * @param permissionData Les données de la permission
     * @return Le résultat de l'opération
     */
    fun createPermission(permissionData: CreatePermissionData): RepositoryResult<PermissionActive> {
        // Validation
        val validationErrors = validatePermission(permissionData)
        if (validationErrors.isNotEmpty()) {
            return RepositoryResult.ValidationError(validationErrors)
        }

        // Vérifier qu'il n'y a pas déjà une permission identique active
        if (hasExistingActivePermission(permissionData)) {
            return RepositoryResult.Error("Une permission identique est déjà active")
        }

        return try {
            val permission = PermissionActiveEntity {
                this.proprietaireId = permissionData.proprietaireId
                this.beneficiaireId = permissionData.beneficiaireId
                this.portee = permissionData.portee.name
                this.cibleId = permissionData.cibleId
                this.typePermission = permissionData.typePermission.name
                this.dateOctroi = LocalDateTime.now()
                this.dateExpiration = permissionData.dateExpiration
                this.estActive = true
            }

            if (save(permission)) {
                RepositoryResult.Success(permission.toModel())
            } else {
                RepositoryResult.Error("Échec de la création de la permission")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la création: ${e.message}")
        }
    }

    /**
     * Désactive une permission
     * @param permissionId L'ID de la permission
     * @return Le résultat de l'opération
     */
    fun deactivatePermission(permissionId: Int): RepositoryResult<PermissionActive> {
        return try {
            val permission = findById(permissionId)
            if (permission == null) {
                return RepositoryResult.Error("Permission non trouvée")
            }

            permission.estActive = false

            if (update(permission)) {
                RepositoryResult.Success(permission.toModel())
            } else {
                RepositoryResult.Error("Échec de la désactivation")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la désactivation: ${e.message}")
        }
    }

    /**
     * Réactive une permission
     * @param permissionId L'ID de la permission
     * @return Le résultat de l'opération
     */
    fun reactivatePermission(permissionId: Int): RepositoryResult<PermissionActive> {
        return try {
            val permission = findById(permissionId)
            if (permission == null) {
                return RepositoryResult.Error("Permission non trouvée")
            }

            // Vérifier que la permission n'est pas expirée
            if (permission.dateExpiration != null && permission.dateExpiration!!.isBefore(LocalDateTime.now())) {
                return RepositoryResult.Error("Impossible de réactiver une permission expirée")
            }

            permission.estActive = true

            if (update(permission)) {
                RepositoryResult.Success(permission.toModel())
            } else {
                RepositoryResult.Error("Échec de la réactivation")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la réactivation: ${e.message}")
        }
    }

    /**
     * Met à jour la date d'expiration d'une permission
     * @param permissionId L'ID de la permission
     * @param nouvelleExpiration La nouvelle date d'expiration (null = jamais)
     * @return Le résultat de l'opération
     */
    fun updateExpiration(permissionId: Int, nouvelleExpiration: LocalDateTime?): RepositoryResult<PermissionActive> {
        return try {
            val permission = findById(permissionId)
            if (permission == null) {
                return RepositoryResult.Error("Permission non trouvée")
            }

            if (nouvelleExpiration != null && nouvelleExpiration.isBefore(LocalDateTime.now())) {
                return RepositoryResult.Error("La date d'expiration ne peut pas être dans le passé")
            }

            permission.dateExpiration = nouvelleExpiration

            if (update(permission)) {
                RepositoryResult.Success(permission.toModel())
            } else {
                RepositoryResult.Error("Échec de la mise à jour")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la mise à jour: ${e.message}")
        }
    }

    /**
     * Supprime toutes les permissions pour une cible spécifique
     * @param portee La portée de la cible
     * @param cibleId L'ID de la cible
     * @return Le nombre de permissions supprimées
     */
    fun revokeAllForTarget(portee: PorteePermission, cibleId: Int): Int {
        return try {
            database.update(PermissionsActives) {
                set(it.estActive, false)
                where {
                    (it.portee eq portee.name) and
                            (it.cibleId eq cibleId) and
                            (it.estActive eq true)
                }
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la révocation des permissions pour la cible $cibleId: ${e.message}")
            0
        }
    }

    /**
     * Trouve les permissions expirées qui sont encore actives
     * @return Liste des permissions expirées
     */
    fun findExpiredPermissions(): List<PermissionActiveEntity> {
        return try {
            entities.filter {
                (table.dateExpiration.isNotNull()) and
                        (table.dateExpiration less LocalDateTime.now()) and
                        (table.estActive eq true)
            }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des permissions expirées: ${e.message}")
            emptyList()
        }
    }

    /**
     * Nettoie automatiquement les permissions expirées
     * @return Le nombre de permissions désactivées
     */
    fun cleanExpiredPermissions(): Int {
        return try {
            database.update(PermissionsActives) {
                set(it.estActive, false)
                where {
                    (it.dateExpiration.isNotNull()) and
                            (it.dateExpiration less LocalDateTime.now()) and
                            (it.estActive eq true)
                }
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors du nettoyage des permissions expirées: ${e.message}")
            0
        }
    }

    /**
     * Obtient les statistiques des permissions
     * @param beneficiaireId Optionnel: limiter à un bénéficiaire
     * @return Les statistiques des permissions
     */
    fun getPermissionStats(beneficiaireId: Int? = null): PermissionStats {
        return try {
            val baseQuery = if (beneficiaireId != null) {
                database.from(PermissionsActives).where { PermissionsActives.beneficiaireId eq beneficiaireId }
            } else {
                database.from(PermissionsActives)
            }

            val actives = baseQuery.select(count())
                .where {
                    (PermissionsActives.estActive eq true) and
                            (PermissionsActives.dateExpiration.isNull() or (PermissionsActives.dateExpiration greater LocalDateTime.now()))
                }
                .map { it.getInt(1) }.first()

            val inactives = baseQuery.select(count())
                .where { PermissionsActives.estActive eq false }
                .map { it.getInt(1) }.first()

            val expirees = baseQuery.select(count())
                .where {
                    (PermissionsActives.dateExpiration.isNotNull()) and
                            (PermissionsActives.dateExpiration less LocalDateTime.now())
                }
                .map { it.getInt(1) }.first()

            // Statistiques par type
            val typeStats = baseQuery
                .select(PermissionsActives.typePermission, count())
                .where { PermissionsActives.estActive eq true }
                .groupBy(PermissionsActives.typePermission)
                .map { row ->
                    PermissionTypeStats(
                        type = TypePermission.valueOf(row.getString(1) ?: "LECTURE"),
                        count = row.getInt(2) ?: 0
                    )
                }

            PermissionStats(
                actives = actives,
                inactives = inactives,
                expirees = expirees,
                total = actives + inactives,
                typeStats = typeStats
            )
        } catch (e: Exception) {
            println("⚠️ Erreur lors du calcul des stats de permissions: ${e.message}")
            PermissionStats(0, 0, 0, 0, emptyList())
        }
    }

    /**
     * Obtient un résumé des permissions d'un bénéficiaire par portée
     * @param beneficiaireId L'ID du bénéficiaire
     * @return Map des portées et du nombre de permissions
     */
    fun getPermissionSummary(beneficiaireId: Int): Map<PorteePermission, Int> {
        return try {
            database.from(PermissionsActives)
                .select(PermissionsActives.portee, count())
                .where {
                    (PermissionsActives.beneficiaireId eq beneficiaireId) and
                            (PermissionsActives.estActive eq true) and
                            (PermissionsActives.dateExpiration.isNull() or (PermissionsActives.dateExpiration greater LocalDateTime.now()))
                }
                .groupBy(PermissionsActives.portee)
                .associate { row ->
                    PorteePermission.valueOf(row.getString(1) ?: "FICHIER") to (row.getInt(2) ?: 0)
                }
        } catch (e: Exception) {
            println("⚠️ Erreur lors du calcul du résumé des permissions: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Vérifie s'il existe déjà une permission active identique
     */
    private fun hasExistingActivePermission(permissionData: CreatePermissionData): Boolean {
        return try {
            val existing = entities.find {
                (table.proprietaireId eq permissionData.proprietaireId) and
                        (table.beneficiaireId eq permissionData.beneficiaireId) and
                        (table.portee eq permissionData.portee.name) and
                        (if (permissionData.cibleId != null) table.cibleId eq permissionData.cibleId else table.cibleId.isNull()) and
                        (table.typePermission eq permissionData.typePermission.name) and
                        (table.estActive eq true)
            }

            existing != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Valide une permission
     */
    private fun validatePermission(permissionData: CreatePermissionData): List<String> {
        val errors = mutableListOf<String>()

        if (permissionData.proprietaireId == permissionData.beneficiaireId) {
            errors.add("Le propriétaire et le bénéficiaire ne peuvent pas être la même personne")
        }

        if (permissionData.dateExpiration != null && permissionData.dateExpiration.isBefore(LocalDateTime.now())) {
            errors.add("La date d'expiration ne peut pas être dans le passé")
        }

        // Validation selon la portée
        when (permissionData.portee) {
            PorteePermission.DOSSIER, PorteePermission.FICHIER -> {
                if (permissionData.cibleId == null) {
                    errors.add("L'ID de la cible est requis pour cette portée")
                }
            }
            PorteePermission.CATEGORIE -> {
                if (permissionData.cibleId == null) {
                    errors.add("L'ID de la catégorie est requis")
                }
            }
            PorteePermission.ESPACE_COMPLET -> {
                // Pas de cible requise pour l'espace complet
            }
        }

        return errors
    }

    override fun validate(entity: PermissionActiveEntity): List<String> {
        val permissionData = CreatePermissionData(
            proprietaireId = entity.proprietaireId,
            beneficiaireId = entity.beneficiaireId,
            portee = PorteePermission.valueOf(entity.portee),
            cibleId = entity.cibleId,
            typePermission = TypePermission.valueOf(entity.typePermission),
            dateExpiration = entity.dateExpiration
        )
        return validatePermission(permissionData)
    }
}

/**
 * Données pour créer une permission
 */
data class CreatePermissionData(
    val proprietaireId: Int,
    val beneficiaireId: Int,
    val portee: PorteePermission,
    val cibleId: Int?,
    val typePermission: TypePermission,
    val dateExpiration: LocalDateTime?
)

/**
 * Statistiques des permissions
 */
data class PermissionStats(
    val actives: Int,
    val inactives: Int,
    val expirees: Int,
    val total: Int,
    val typeStats: List<PermissionTypeStats>
)

/**
 * Statistiques par type de permission
 */
data class PermissionTypeStats(
    val type: TypePermission,
    val count: Int
)