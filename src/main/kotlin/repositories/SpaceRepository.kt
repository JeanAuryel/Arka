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
 * Repository pour la gestion des espaces
 *
 * Responsabilités:
 * - CRUD des espaces
 * - Gestion des accès aux espaces
 * - Validation et recherche d'espaces
 * - Statistiques et requêtes métier
 *
 * Utilisé par: SpaceController, CategoryController, DelegationController
 */
class SpaceRepository : BaseRepository<EspaceEntity, Espaces>() {

    override val table = Espaces

    /**
     * Obtient la clé primaire d'un espace
     */
    override fun EspaceEntity.getPrimaryKey(): Int = this.espaceId
    override fun getPrimaryKeyColumn(): Column<Int> = Espaces.espaceId

    /**
     * Met à jour un espace
     */
    override fun update(entity: EspaceEntity): Int {
        return ArkaDatabase.instance.update(Espaces) {
            set(it.nomEspace, entity.nomEspace)
            set(it.descriptionEspace, entity.descriptionEspace)
            where { it.espaceId eq entity.espaceId }
        }
    }

    // ================================================================
    // MÉTHODES DE VALIDATION ET VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si un espace avec ce nom existe déjà
     * Utilisé pour éviter les doublons lors de la création
     *
     * @param nomEspace Le nom de l'espace à vérifier
     * @param excludeId ID à exclure de la vérification (pour les mises à jour)
     * @return true si le nom existe déjà
     */
    fun existsByName(nomEspace: String, excludeId: Int? = null): Boolean {
        return ArkaDatabase.instance.sequenceOf(Espaces)
            .filter { espace ->
                val nameCondition = espace.nomEspace eq nomEspace.trim()
                if (excludeId != null) {
                    nameCondition and (espace.espaceId neq excludeId)
                } else {
                    nameCondition
                }
            }
            .toList()
            .isNotEmpty()
    }

    /**
     * Trouve un espace par son nom exact
     *
     * @param nomEspace Le nom de l'espace
     * @return L'espace trouvé ou null
     */
    fun findByName(nomEspace: String): EspaceEntity? {
        return ArkaDatabase.instance.sequenceOf(Espaces)
            .find { it.nomEspace eq nomEspace.trim() }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE AVANCÉE
    // ================================================================

    /**
     * Recherche des espaces par nom (recherche partielle)
     *
     * @param searchTerm Terme de recherche
     * @return Liste des espaces trouvés
     */
    fun searchByName(searchTerm: String): List<EspaceEntity> {
        return ArkaDatabase.instance.sequenceOf(Espaces)
            .filter { it.nomEspace like "%${searchTerm.trim()}%" }
            .toList()
            .sortedBy { it.nomEspace }
    }

    /**
     * Trouve les espaces créés récemment
     *
     * @param since Date depuis laquelle chercher
     * @param limit Nombre maximum d'espaces à retourner
     * @return Liste des espaces récents
     */
    fun findRecentSpaces(since: LocalDateTime, limit: Int = 10): List<EspaceEntity> {
        return ArkaDatabase.instance.sequenceOf(Espaces)
            .filter { it.dateCreationEspace greaterEq since }
            .toList()
            .sortedByDescending { it.dateCreationEspace }
            .take(limit)
    }

    /**
     * Trouve tous les espaces triés par nom
     *
     * @return Liste des espaces triés alphabétiquement
     */
    fun findAllSorted(): List<EspaceEntity> {
        return ArkaDatabase.instance.sequenceOf(Espaces)
            .toList()
            .sortedBy { it.nomEspace }
    }

    // ================================================================
    // MÉTHODES DE GESTION DES ACCÈS
    // ================================================================

    /**
     * Trouve les espaces accessibles par un membre
     * Utilise la table de liaison MembreEspace
     *
     * @param membreFamilleId ID du membre
     * @return Liste des espaces accessibles
     */
    fun findAccessibleByMember(membreFamilleId: Int): List<EspaceEntity> {
        val espacesIds = ArkaDatabase.instance.sequenceOf(MembresEspace)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
            .map { it.espaceId }

        return if (espacesIds.isNotEmpty()) {
            ArkaDatabase.instance.sequenceOf(Espaces)
                .filter { it.espaceId inList espacesIds }
                .toList()
                .sortedBy { it.nomEspace }
        } else {
            emptyList()
        }
    }

    /**
     * Vérifie si un membre a accès à un espace
     *
     * @param membreFamilleId ID du membre
     * @param espaceId ID de l'espace
     * @return true si le membre a accès
     */
    fun hasAccessByMember(membreFamilleId: Int, espaceId: Int): Boolean {
        return ArkaDatabase.instance.sequenceOf(MembresEspace)
            .find {
                (it.membreFamilleId eq membreFamilleId) and (it.espaceId eq espaceId)
            } != null
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES ET COMPTAGE
    // ================================================================

    /**
     * Compte le nombre de catégories dans un espace
     *
     * @param espaceId ID de l'espace
     * @return Nombre de catégories
     */
    fun countCategoriesInSpace(espaceId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(Categories)
            .filter { it.espaceId eq espaceId }
            .toList()
            .size
    }

    /**
     * Compte le nombre de membres ayant accès à un espace
     *
     * @param espaceId ID de l'espace
     * @return Nombre de membres
     */
    fun countMembersWithAccess(espaceId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(MembresEspace)
            .filter { it.espaceId eq espaceId }
            .toList()
            .size
    }

    /**
     * Obtient les statistiques complètes d'un espace
     *
     * @param espaceId ID de l'espace
     * @return Map avec les statistiques
     */
    fun getStatistiquesEspace(espaceId: Int): Map<String, Any> {
        val nombreCategories = countCategoriesInSpace(espaceId)
        val nombreMembres = countMembersWithAccess(espaceId)

        // Compter les dossiers via les catégories
        val categoriesIds = ArkaDatabase.instance.sequenceOf(Categories)
            .filter { it.espaceId eq espaceId }
            .toList()
            .map { it.categorieId }

        val nombreDossiers = if (categoriesIds.isNotEmpty()) {
            ArkaDatabase.instance.sequenceOf(Dossiers)
                .filter { it.categorieId inList categoriesIds }
                .toList()
                .size
        } else {
            0
        }

        return mapOf(
            "nombreCategories" to nombreCategories,
            "nombreMembres" to nombreMembres,
            "nombreDossiers" to nombreDossiers
        )
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION AVEC VÉRIFICATIONS
    // ================================================================

    /**
     * Vérifie si un espace peut être supprimé (pas de catégories)
     *
     * @param espaceId ID de l'espace
     * @return true si l'espace peut être supprimé
     */
    fun canBeDeleted(espaceId: Int): Boolean {
        return countCategoriesInSpace(espaceId) == 0
    }

    /**
     * Supprime un espace avec toutes ses dépendances
     * Supprime d'abord les accès membres puis l'espace
     *
     * @param espaceId ID de l'espace
     * @return true si la suppression a réussi
     */
    fun deleteWithCascade(espaceId: Int): Boolean {
        return try {
            transaction {
                // Supprimer les accès membres à cet espace
                ArkaDatabase.instance.delete(MembresEspace) {
                    it.espaceId eq espaceId
                }

                // Supprimer l'espace (les catégories seront supprimées par cascade FK)
                val rowsAffected = delete(espaceId)
                rowsAffected > 0
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression de l'espace $espaceId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE MISE À JOUR SPÉCIFIQUES
    // ================================================================

    /**
     * Met à jour uniquement la description d'un espace
     *
     * @param espaceId ID de l'espace
     * @param nouvelleDescription Nouvelle description
     * @return true si la mise à jour a réussi
     */
    fun updateDescription(espaceId: Int, nouvelleDescription: String?): Boolean {
        return try {
            val rowsAffected = ArkaDatabase.instance.update(Espaces) {
                set(it.descriptionEspace, nouvelleDescription)
                where { it.espaceId eq espaceId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour de la description de l'espace $espaceId: ${e.message}")
            false
        }
    }

    /**
     * Met à jour le nom d'un espace avec validation
     *
     * @param espaceId ID de l'espace
     * @param nouveauNom Nouveau nom
     * @return true si la mise à jour a réussi
     */
    fun updateName(espaceId: Int, nouveauNom: String): Boolean {
        if (existsByName(nouveauNom, espaceId)) {
            return false // Le nom existe déjà
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(Espaces) {
                set(it.nomEspace, nouveauNom.trim())
                where { it.espaceId eq espaceId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour du nom de l'espace $espaceId: ${e.message}")
            false
        }
    }
}