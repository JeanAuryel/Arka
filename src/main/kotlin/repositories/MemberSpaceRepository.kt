package repositories

import org.ktorm.entity.sequenceOf
import org.ktorm.entity.find
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.ktorm.entity.add
import org.ktorm.dsl.*
import ktorm.*
import java.time.LocalDateTime

/**
 * Repository pour la gestion des accès membres-espaces (table de liaison)
 *
 * Responsabilités:
 * - Gestion des accès membres aux espaces
 * - Vérification des permissions d'accès
 * - Statistiques d'utilisation des espaces
 * - Audit des accès
 *
 * Note: Cette table de liaison a une clé primaire composée (membreFamilleId + espaceId)
 * Elle n'hérite pas directement de BaseRepository car elle a des besoins spécifiques
 *
 * Utilisé par: SpaceController, PermissionController, DelegationController
 */
class MemberSpaceRepository {

    // Référence à la base de données
    private val database = ArkaDatabase.instance

    // ================================================================
    // MÉTHODES CRUD DE BASE POUR TABLE DE LIAISON
    // ================================================================

    /**
     * Trouve un accès spécifique membre-espace
     *
     * @param membreFamilleId ID du membre
     * @param espaceId ID de l'espace
     * @return L'accès trouvé ou null
     */
    fun findByMemberAndSpace(membreFamilleId: Int, espaceId: Int): MembreEspaceEntity? {
        return database.sequenceOf(MembresEspace)
            .find {
                (it.membreFamilleId eq membreFamilleId) and (it.espaceId eq espaceId)
            }
    }

    /**
     * Crée un nouvel accès membre-espace
     * Met à jour la date d'accès si l'entrée existe déjà
     *
     * @param membreFamilleId ID du membre
     * @param espaceId ID de l'espace
     * @return L'entité créée ou mise à jour
     */
    fun createOrUpdateAccess(membreFamilleId: Int, espaceId: Int): MembreEspaceEntity {
        val existing = findByMemberAndSpace(membreFamilleId, espaceId)

        return if (existing != null) {
            // Mettre à jour la date d'accès
            updateDateAccess(membreFamilleId, espaceId)
            existing
        } else {
            // Créer un nouvel accès
            val membreEspace = MembreEspaceEntity {
                this.membreFamilleId = membreFamilleId
                this.espaceId = espaceId
                this.dateAcces = LocalDateTime.now()
            }
            database.sequenceOf(MembresEspace).add(membreEspace)
            membreEspace
        }
    }

    /**
     * Supprime un accès membre-espace
     *
     * @param membreFamilleId ID du membre
     * @param espaceId ID de l'espace
     * @return true si la suppression a réussi
     */
    fun removeAccess(membreFamilleId: Int, espaceId: Int): Boolean {
        return try {
            val rowsAffected = database.delete(MembresEspace) {
                (it.membreFamilleId eq membreFamilleId) and (it.espaceId eq espaceId)
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la suppression de l'accès membre $membreFamilleId à l'espace $espaceId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE VÉRIFICATION ET VALIDATION
    // ================================================================

    /**
     * Vérifie si un membre a accès à un espace
     * MÉTHODE CRUCIALE pour les vérifications de permissions
     *
     * @param membreFamilleId ID du membre
     * @param espaceId ID de l'espace
     * @return true si le membre a accès
     */
    fun hasAccess(membreFamilleId: Int, espaceId: Int): Boolean {
        return findByMemberAndSpace(membreFamilleId, espaceId) != null
    }

    /**
     * Vérifie si un membre a accès à plusieurs espaces
     *
     * @param membreFamilleId ID du membre
     * @param espacesIds Liste des IDs d'espaces
     * @return Map avec les résultats pour chaque espace
     */
    fun hasAccessToSpaces(membreFamilleId: Int, espacesIds: List<Int>): Map<Int, Boolean> {
        val accesses = database.sequenceOf(MembresEspace)
            .filter {
                (it.membreFamilleId eq membreFamilleId) and (it.espaceId inList espacesIds)
            }
            .toList()
            .map { it.espaceId }
            .toSet()

        return espacesIds.associateWith { it in accesses }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR MEMBRE
    // ================================================================

    /**
     * Trouve tous les espaces accessibles par un membre
     *
     * @param membreFamilleId ID du membre
     * @return Liste des accès du membre
     */
    fun findSpacesByMember(membreFamilleId: Int): List<MembreEspaceEntity> {
        return database.sequenceOf(MembresEspace)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
            .sortedByDescending { it.dateAcces }
    }

    /**
     * Trouve les IDs des espaces accessibles par un membre
     *
     * @param membreFamilleId ID du membre
     * @return Liste des IDs d'espaces
     */
    fun findSpaceIdsByMember(membreFamilleId: Int): List<Int> {
        return database.sequenceOf(MembresEspace)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
            .map { it.espaceId }
    }

    /**
     * Trouve les espaces récemment consultés par un membre
     *
     * @param membreFamilleId ID du membre
     * @param limit Nombre maximum d'espaces
     * @return Liste des accès récents
     */
    fun findRecentSpacesByMember(membreFamilleId: Int, limit: Int = 5): List<MembreEspaceEntity> {
        return database.sequenceOf(MembresEspace)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
            .sortedByDescending { it.dateAcces }
            .take(limit)
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR ESPACE
    // ================================================================

    /**
     * Trouve tous les membres ayant accès à un espace
     *
     * @param espaceId ID de l'espace
     * @return Liste des accès à l'espace
     */
    fun findMembersBySpace(espaceId: Int): List<MembreEspaceEntity> {
        return database.sequenceOf(MembresEspace)
            .filter { it.espaceId eq espaceId }
            .toList()
            .sortedByDescending { it.dateAcces }
    }

    /**
     * Trouve les IDs des membres ayant accès à un espace
     *
     * @param espaceId ID de l'espace
     * @return Liste des IDs de membres
     */
    fun findMemberIdsBySpace(espaceId: Int): List<Int> {
        return database.sequenceOf(MembresEspace)
            .filter { it.espaceId eq espaceId }
            .toList()
            .map { it.membreFamilleId }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES ET COMPTAGE
    // ================================================================

    /**
     * Compte le nombre d'espaces accessibles par un membre
     *
     * @param membreFamilleId ID du membre
     * @return Nombre d'espaces accessibles
     */
    fun countSpacesByMember(membreFamilleId: Int): Int {
        return database.sequenceOf(MembresEspace)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
            .size
    }

    /**
     * Compte le nombre de membres ayant accès à un espace
     *
     * @param espaceId ID de l'espace
     * @return Nombre de membres
     */
    fun countMembersBySpace(espaceId: Int): Int {
        return database.sequenceOf(MembresEspace)
            .filter { it.espaceId eq espaceId }
            .toList()
            .size
    }

    /**
     * Obtient les statistiques d'utilisation d'un espace
     *
     * @param espaceId ID de l'espace
     * @return Map avec les statistiques
     */
    fun getStatistiquesUtilisationEspace(espaceId: Int): Map<String, Any> {
        val acces = findMembersBySpace(espaceId)
        val nombreMembres = acces.size
        val dernierAcces: LocalDateTime? = acces.maxByOrNull { it.dateAcces ?: LocalDateTime.MIN }?.dateAcces
        val accesAujourdHui = acces.count {
            it.dateAcces?.toLocalDate() == LocalDateTime.now().toLocalDate()
        }

        return mapOf(
            "nombreMembres" to nombreMembres,
            "dernierAcces" to (dernierAcces ?: "Aucun accès"),
            "accesAujourdHui" to accesAujourdHui
        )
    }

    // ================================================================
    // MÉTHODES DE MISE À JOUR
    // ================================================================

    /**
     * Met à jour la date d'accès pour un membre-espace
     * Utilisé pour tracker l'utilisation
     *
     * @param membreFamilleId ID du membre
     * @param espaceId ID de l'espace
     * @return true si la mise à jour a réussi
     */
    fun updateDateAccess(membreFamilleId: Int, espaceId: Int): Boolean {
        return try {
            val rowsAffected = database.update(MembresEspace) {
                set(it.dateAcces, LocalDateTime.now())
                where {
                    (it.membreFamilleId eq membreFamilleId) and (it.espaceId eq espaceId)
                }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour de la date d'accès: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION EN MASSE
    // ================================================================

    /**
     * Supprime tous les accès d'un membre
     * Utilisé lors de la suppression d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Nombre d'accès supprimés
     */
    fun deleteAllByMember(membreFamilleId: Int): Int {
        return try {
            database.delete(MembresEspace) {
                it.membreFamilleId eq membreFamilleId
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression des accès du membre $membreFamilleId: ${e.message}")
            0
        }
    }

    /**
     * Supprime tous les accès à un espace
     * Utilisé lors de la suppression d'un espace
     *
     * @param espaceId ID de l'espace
     * @return Nombre d'accès supprimés
     */
    fun deleteAllBySpace(espaceId: Int): Int {
        return try {
            database.delete(MembresEspace) {
                it.espaceId eq espaceId
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression des accès à l'espace $espaceId: ${e.message}")
            0
        }
    }

    // ================================================================
    // MÉTHODES DE GESTION DES ACCÈS EN BATCH
    // ================================================================

    /**
     * Accorde l'accès à un espace pour plusieurs membres
     *
     * @param membreIds Liste des IDs de membres
     * @param espaceId ID de l'espace
     * @return Nombre d'accès créés
     */
    fun grantAccessToMembers(membreIds: List<Int>, espaceId: Int): Int {
        return try {
            database.useTransaction {
                var count = 0
                membreIds.forEach { membreId ->
                    if (!hasAccess(membreId, espaceId)) {
                        createOrUpdateAccess(membreId, espaceId)
                        count++
                    }
                }
                count
            }
        } catch (e: Exception) {
            println("Erreur lors de l'octroi d'accès en batch: ${e.message}")
            0
        }
    }

    /**
     * Révoque l'accès à un espace pour plusieurs membres
     *
     * @param membreIds Liste des IDs de membres
     * @param espaceId ID de l'espace
     * @return Nombre d'accès supprimés
     */
    fun revokeAccessFromMembers(membreIds: List<Int>, espaceId: Int): Int {
        return try {
            database.useTransaction {
                var count = 0
                membreIds.forEach { membreId ->
                    if (removeAccess(membreId, espaceId)) {
                        count++
                    }
                }
                count
            }
        } catch (e: Exception) {
            println("Erreur lors de la révocation d'accès en batch: ${e.message}")
            0
        }
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Vérifie la santé de la table de liaison
     *
     * @return true si la table fonctionne correctement
     */
    fun healthCheck(): Boolean {
        return try {
            database.sequenceOf(MembresEspace).toList().size
            true
        } catch (e: Exception) {
            println("Health check failed pour MembresEspace: ${e.message}")
            false
        }
    }

    /**
     * Compte le nombre total d'accès dans le système
     *
     * @return Nombre total d'accès
     */
    fun countTotalAccesses(): Int {
        return database.sequenceOf(MembresEspace)
            .toList()
            .size
    }
}