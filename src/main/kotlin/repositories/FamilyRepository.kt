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
 * Repository pour la gestion des familles - Exemple de correction
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
     * Implémentation requise : obtient la clé primaire d'une famille
     */
    override fun FamilleEntity.getPrimaryKey(): Int = this.familleId

    /**
     * Implémentation requise : obtient la colonne de clé primaire
     */
    override fun getPrimaryKeyColumn(): Column<Int> = Familles.familleId

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
     * @return Map contenant les statistiques
     */
    fun getFamilyStatistics(): Map<String, Any?> {
        val allFamilies = findAll()

        val now = LocalDateTime.now()
        val lastMonth = now.minusMonths(1)
        val lastWeek = now.minusWeeks(1)

        val familiesCreatedLastMonth = allFamilies.count {
            it.dateCreationFamille?.isAfter(lastMonth) == true
        }
        val familiesCreatedLastWeek = allFamilies.count {
            it.dateCreationFamille?.isAfter(lastWeek) == true
        }
        val oldestFamily = allFamilies.minByOrNull {
            it.dateCreationFamille ?: LocalDateTime.MIN
        }
        val newestFamily = allFamilies.maxByOrNull {
            it.dateCreationFamille ?: LocalDateTime.MIN
        }

        return mapOf(
            "totalFamilies" to allFamilies.size,
            "familiesCreatedLastMonth" to familiesCreatedLastMonth,
            "familiesCreatedLastWeek" to familiesCreatedLastWeek,
            "oldestFamilyDate" to oldestFamily?.dateCreationFamille,
            "newestFamilyDate" to newestFamily?.dateCreationFamille
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
     * @return Map avec les détails ou null
     */
    fun getFamilyDetails(familyId: Int): Map<String, Any?>? {
        val family = findById(familyId) ?: return null

        val members = ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter { it.familleId eq familyId }
            .toList()

        val adminCount = members.count { it.estAdmin }
        val responsibleCount = members.count { it.estResponsable }
        val childrenCount = members.count { !it.estAdmin && !it.estResponsable }
        val lastMemberAdded = members.maxByOrNull {
            it.dateAjoutMembre ?: LocalDateTime.MIN
        }?.dateAjoutMembre

        return mapOf(
            "familyId" to family.familleId,
            "familyName" to family.nomFamille,
            "creationDate" to family.dateCreationFamille,
            "memberCount" to members.size,
            "adminCount" to adminCount,
            "responsibleCount" to responsibleCount,
            "childrenCount" to childrenCount,
            "lastMemberAdded" to lastMemberAdded
        )
    }

    // ================================================================
    // MÉTHODES DE CRÉATION AVEC VALIDATION
    // ================================================================

    /**
     * Crée une nouvelle famille avec validation
     *
     * @param nomFamille Nom de la famille
     * @return La famille créée ou null en cas d'erreur
     */
    fun createFamille(nomFamille: String): FamilleEntity? {
        if (nomFamille.isBlank() || nomFamille.length < 2 || nomFamille.length > 100) {
            println("Nom de famille invalide: '$nomFamille'")
            return null
        }

        if (existsByName(nomFamille)) {
            println("Une famille avec ce nom existe déjà")
            return null
        }

        return try {
            val famille = FamilleEntity {
                this.nomFamille = nomFamille.trim()
                this.dateCreationFamille = LocalDateTime.now()
            }

            create(famille)
        } catch (e: Exception) {
            println("Erreur lors de la création de la famille: ${e.message}")
            null
        }
    }
}