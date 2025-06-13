package repositories

import org.ktorm.entity.sequenceOf
import org.ktorm.entity.find
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.ktorm.dsl.*
import ktorm.*
import java.time.LocalDateTime

/**
 * Repository pour la gestion des familles
 *
 * Responsabilités:
 * - CRUD des familles
 * - Recherche et validation des familles
 * - Statistiques des familles
 * - Intégration avec le système de membres
 *
 * Utilisé par: FamilyController, AuthController (pour l'inscription)
 */
class FamilyRepository : BaseRepository<FamilleEntity, Familles>() {

    override val table = Familles

    /**
     * Obtient la clé primaire d'une famille
     */
    override fun FamilleEntity.getPrimaryKey(): Int = this.familleId

    /**
     * Met à jour une famille
     */
    override fun update(entity: FamilleEntity): Int {
        return ArkaDatabase.instance.update(Familles) {
            set(it.nomFamille, entity.nomFamille)
            set(it.dateCreationFamille, entity.dateCreationFamille)
            where { it.familleId eq entity.familleId }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE SPÉCIALISÉES
    // ================================================================

    /**
     * Trouve une famille par son nom
     *
     * @param familyName Le nom de la famille à rechercher
     * @return La famille trouvée ou null
     */
    fun findByName(familyName: String): FamilleEntity? {
        return ArkaDatabase.instance.sequenceOf(Familles)
            .find { it.nomFamille eq familyName.trim() }
    }

    /**
     * Vérifie si une famille avec ce nom existe déjà
     * Utilisé pour éviter les doublons lors de la création
     *
     * @param familyName Le nom à vérifier
     * @return true si le nom existe
     */
    fun existsByName(familyName: String): Boolean {
        return findByName(familyName) != null
    }

    /**
     * Recherche des familles par nom (recherche partielle)
     *
     * @param searchTerm Terme de recherche
     * @return Liste des familles trouvées
     */
    fun searchByName(searchTerm: String): List<FamilleEntity> {
        return ArkaDatabase.instance.sequenceOf(Familles)
            .filter { it.nomFamille like "%${searchTerm.trim()}%" }
            .toList()
    }

    /**
     * Trouve les familles créées récemment
     *
     * @param since Date depuis laquelle chercher
     * @return Liste des nouvelles familles
     */
    fun findRecentFamilies(since: LocalDateTime): List<FamilleEntity> {
        return ArkaDatabase.instance.sequenceOf(Familles)
            .filter { it.dateCreationFamille greaterEq since }
            .toList()
    }

    /**
     * Trouve les familles créées dans une période donnée
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Liste des familles créées dans cette période
     */
    fun findFamiliesInPeriod(startDate: LocalDateTime, endDate: LocalDateTime): List<FamilleEntity> {
        return ArkaDatabase.instance.sequenceOf(Familles)
            .filter {
                (it.dateCreationFamille greaterEq startDate) and
                        (it.dateCreationFamille lessEq endDate)
            }
            .toList()
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Compte les familles créées dans une période
     *
     * @param since Date depuis laquelle compter
     * @return Nombre de familles créées
     */
    fun countFamiliesCreatedSince(since: LocalDateTime): Int {
        return ArkaDatabase.instance.sequenceOf(Familles)
            .filter { it.dateCreationFamille greaterEq since }
            .toList()
            .size
    }

    /**
     * Obtient les statistiques générales des familles
     *
     * @return Objet contenant les statistiques
     */
    fun getFamilyStatistics(): FamilyStatistics {
        val allFamilies = findAll()

        val now = LocalDateTime.now()
        val lastMonth = now.minusMonths(1)
        val lastWeek = now.minusWeeks(1)

        return FamilyStatistics(
            totalFamilies = allFamilies.size,
            familiesCreatedLastMonth = allFamilies.count {
                it.dateCreationFamille?.isAfter(lastMonth) == true
            },
            familiesCreatedLastWeek = allFamilies.count {
                it.dateCreationFamille?.isAfter(lastWeek) == true
            },
            oldestFamily = allFamilies.minByOrNull {
                it.dateCreationFamille ?: LocalDateTime.MIN
            },
            newestFamily = allFamilies.maxByOrNull {
                it.dateCreationFamille ?: LocalDateTime.MIN
            }
        )
    }

    // ================================================================
    // MÉTHODES DE VALIDATION ET VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si une famille a des membres
     * Utilisé avant la suppression pour éviter l'orphelinat de données
     *
     * @param familyId ID de la famille
     * @return true si la famille a des membres
     */
    fun hasMembers(familyId: Int): Boolean {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .find { it.familleId eq familyId } != null
    }

    /**
     * Compte le nombre de membres d'une famille
     *
     * @param familyId ID de la famille
     * @return Nombre de membres
     */
    fun getMemberCount(familyId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter { it.familleId eq familyId }
            .toList()
            .size
    }

    /**
     * Vérifie si une famille a au moins un administrateur
     *
     * @param familyId ID de la famille
     * @return true si la famille a un admin
     */
    fun hasAdmin(familyId: Int): Boolean {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .find {
                (it.familleId eq familyId) and (it.estAdmin eq true)
            } != null
    }

    /**
     * Obtient des informations détaillées sur une famille
     *
     * @param familyId ID de la famille
     * @return Informations détaillées ou null
     */
    fun getFamilyDetails(familyId: Int): FamilyDetails? {
        val family = findById(familyId) ?: return null

        val members = ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter { it.familleId eq familyId }
            .toList()

        return FamilyDetails(
            family = family,
            memberCount = members.size,
            adminCount = members.count { it.estAdmin },
            responsibleCount = members.count { it.estResponsable },
            childrenCount = members.count { !it.estAdmin && !it.estResponsable },
            lastMemberAdded = members.maxByOrNull {
                it.dateAjoutMembre ?: LocalDateTime.MIN
            }?.dateAjoutMembre
        )
    }

    // ================================================================
    // MÉTHODES DE GESTION AVANCÉE
    // ================================================================

    /**
     * Archive une famille (soft delete)
     * Note: Pour l'instant, simple suppression. Peut être étendu plus tard.
     *
     * @param familyId ID de la famille à archiver
     * @return true si l'archivage a réussi
     */
    fun archiveFamily(familyId: Int): Boolean {
        // Pour l'instant, on supprime directement
        // Plus tard, on pourrait ajouter un champ "archived" à la table
        return delete(familyId) > 0
    }

    /**
     * Supprime une famille et toutes ses données associées
     * ATTENTION: Cette opération est irréversible !
     *
     * @param familyId ID de la famille à supprimer
     * @return Résultat de l'opération
     */
    fun deleteFamilyCompletely(familyId: Int): FamilyDeletionResult {
        return transaction {
            try {
                val family = findById(familyId)
                    ?: return@transaction FamilyDeletionResult.FamilyNotFound

                // Vérifier s'il y a des membres
                val memberCount = getMemberCount(familyId)
                if (memberCount > 0) {
                    return@transaction FamilyDeletionResult.HasMembers(memberCount)
                }

                // Supprimer la famille
                val deleted = delete(familyId)
                if (deleted > 0) {
                    FamilyDeletionResult.Success(family.nomFamille)
                } else {
                    FamilyDeletionResult.DeletionFailed
                }

            } catch (e: Exception) {
                FamilyDeletionResult.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /**
     * Clone une famille (crée une copie avec un nouveau nom)
     * Utile pour créer des familles avec des structures similaires
     *
     * @param sourceFamilyId ID de la famille source
     * @param newFamilyName Nom de la nouvelle famille
     * @return La nouvelle famille créée ou null en cas d'erreur
     */
    fun cloneFamily(sourceFamilyId: Int, newFamilyName: String): FamilleEntity? {
        val sourceFamily = findById(sourceFamilyId) ?: return null

        if (existsByName(newFamilyName)) {
            return null // Le nom existe déjà
        }

        val newFamily = FamilleEntity {
            nomFamille = newFamilyName.trim()
            dateCreationFamille = LocalDateTime.now()
        }

        return create(newFamily)
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Obtient la liste des noms de toutes les familles
     * Utile pour les interfaces de sélection
     *
     * @return Liste des noms de familles
     */
    fun getAllFamilyNames(): List<String> {
        return ArkaDatabase.instance.sequenceOf(Familles)
            .toList()
            .map { it.nomFamille }
    }

    /**
     * Vérifie la santé d'une famille
     * (au moins un admin, pas de données corrompues, etc.)
     *
     * @param familyId ID de la famille
     * @return Résultat de la vérification
     */
    fun checkFamilyHealth(familyId: Int): FamilyHealthCheck {
        val family = findById(familyId)
            ?: return FamilyHealthCheck.FamilyNotFound

        val memberCount = getMemberCount(familyId)
        val hasAdmin = hasAdmin(familyId)

        return when {
            memberCount == 0 -> FamilyHealthCheck.NoMembers
            !hasAdmin -> FamilyHealthCheck.NoAdmin
            else -> FamilyHealthCheck.Healthy
        }
    }

    /**
     * Nettoie les familles orphelines (sans membres depuis X jours)
     * Méthode de maintenance
     *
     * @param daysThreshold Nombre de jours sans membres
     * @return Nombre de familles nettoyées
     */
    fun cleanupOrphanedFamilies(daysThreshold: Int = 30): Int {
        val threshold = LocalDateTime.now().minusDays(daysThreshold.toLong())
        var cleanedCount = 0

        val allFamilies = findAll()

        for (family in allFamilies) {
            val memberCount = getMemberCount(family.familleId)
            val creationDate = family.dateCreationFamille ?: LocalDateTime.now()

            if (memberCount == 0 && creationDate.isBefore(threshold)) {
                if (delete(family.familleId) > 0) {
                    cleanedCount++
                }
            }
        }

        return cleanedCount
    }
}

// ================================================================
// CLASSES DE DONNÉES POUR LES RÉSULTATS
// ================================================================

/**
 * Statistiques des familles
 */
data class FamilyStatistics(
    val totalFamilies: Int,
    val familiesCreatedLastMonth: Int,
    val familiesCreatedLastWeek: Int,
    val oldestFamily: FamilleEntity?,
    val newestFamily: FamilleEntity?
)

/**
 * Détails complets d'une famille
 */
data class FamilyDetails(
    val family: FamilleEntity,
    val memberCount: Int,
    val adminCount: Int,
    val responsibleCount: Int,
    val childrenCount: Int,
    val lastMemberAdded: LocalDateTime?
)

/**
 * Résultat de suppression de famille
 */
sealed class FamilyDeletionResult {
    object FamilyNotFound : FamilyDeletionResult()
    data class HasMembers(val memberCount: Int) : FamilyDeletionResult()
    object DeletionFailed : FamilyDeletionResult()
    data class Success(val familyName: String) : FamilyDeletionResult()
    data class Error(val message: String) : FamilyDeletionResult()
}

/**
 * Résultat de vérification de santé de famille
 */
sealed class FamilyHealthCheck {
    object FamilyNotFound : FamilyHealthCheck()
    object NoMembers : FamilyHealthCheck()
    object NoAdmin : FamilyHealthCheck()
    object Healthy : FamilyHealthCheck()
}