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
 * Repository pour la gestion des membres de famille
 *
 * Responsabilités:
 * - CRUD des membres de famille
 * - Recherche par email (utilisé par AuthController)
 * - Gestion des rôles (admin, responsable)
 * - Statistiques et requêtes métier
 *
 * Utilisé par: AuthController, FamilyMemberController
 */
class FamilyMemberRepository : BaseRepository<MembreFamilleEntity, MembresFamille>() {

    override val table = MembresFamille

    /**
     * Obtient la clé primaire d'un membre
     */
    override fun MembreFamilleEntity.getPrimaryKey(): Int = this.membreFamilleId
    override fun getPrimaryKeyColumn(): Column<Int> = MembresFamille.membreFamilleId

    /**
     * Met à jour un membre de famille
     */
    override fun update(entity: MembreFamilleEntity): Int {
        return ArkaDatabase.instance.update(MembresFamille) {
            set(it.prenomMembre, entity.prenomMembre)
            set(it.mailMembre, entity.mailMembre)
            set(it.mdpMembre, entity.mdpMembre)
            set(it.dateNaissanceMembre, entity.dateNaissanceMembre)
            set(it.genreMembre, entity.genreMembre)
            set(it.estResponsable, entity.estResponsable)
            set(it.estAdmin, entity.estAdmin)
            set(it.familleId, entity.familleId)
            where { it.membreFamilleId eq entity.membreFamilleId }
        }
    }

    // ================================================================
    // MÉTHODES SPÉCIFIQUES POUR L'AUTHENTIFICATION
    // ================================================================

    /**
     * Trouve un membre par son adresse email
     * CRUCIAL pour AuthController.login()
     *
     * @param email L'adresse email à rechercher
     * @return Le membre trouvé ou null
     */
    fun findByEmail(email: String): MembreFamilleEntity? {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .find { it.mailMembre eq email.lowercase().trim() }
    }

    /**
     * Vérifie si un email existe déjà
     * Utilisé pour éviter les doublons lors de l'inscription
     *
     * @param email L'email à vérifier
     * @return true si l'email existe
     */
    fun existsByEmail(email: String): Boolean {
        return findByEmail(email) != null
    }

    /**
     * Met à jour le mot de passe d'un membre
     * Utilisé par AuthController.changePassword()
     *
     * @param memberId ID du membre
     * @param newHashedPassword Nouveau mot de passe haché
     * @return Nombre de lignes mises à jour
     */
    fun updatePassword(memberId: Int, newHashedPassword: String): Int {
        return ArkaDatabase.instance.update(MembresFamille) {
            set(it.mdpMembre, newHashedPassword)
            where { it.membreFamilleId eq memberId }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR FAMILLE
    // ================================================================

    /**
     * Trouve tous les membres d'une famille
     *
     * @param familyId ID de la famille
     * @return Liste des membres de la famille
     */
    fun findByFamilyId(familyId: Int): List<MembreFamilleEntity> {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter { it.familleId eq familyId }
            .toList()
    }

    /**
     * Trouve les administrateurs d'une famille
     *
     * @param familyId ID de la famille
     * @return Liste des administrateurs
     */
    fun findAdminsByFamilyId(familyId: Int): List<MembreFamilleEntity> {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter { (it.familleId eq familyId) and (it.estAdmin eq true) }
            .toList()
    }

    /**
     * Trouve les responsables d'une famille
     *
     * @param familyId ID de la famille
     * @return Liste des responsables
     */
    fun findResponsiblesByFamilyId(familyId: Int): List<MembreFamilleEntity> {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter { (it.familleId eq familyId) and (it.estResponsable eq true) }
            .toList()
    }

    /**
     * Trouve les enfants d'une famille (ni admin ni responsable)
     *
     * @param familyId ID de la famille
     * @return Liste des enfants
     */
    fun findChildrenByFamilyId(familyId: Int): List<MembreFamilleEntity> {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter {
                (it.familleId eq familyId) and
                        (it.estAdmin eq false) and
                        (it.estResponsable eq false)
            }
            .toList()
    }

    // ================================================================
    // MÉTHODES DE COMPTAGE ET STATISTIQUES
    // ================================================================

    /**
     * Compte les membres d'une famille
     *
     * @param familyId ID de la famille
     * @return Nombre de membres
     */
    fun countByFamilyId(familyId: Int): Int {
        return countWhere { it.familleId eq familyId }
    }

    /**
     * Compte les administrateurs d'une famille
     *
     * @param familyId ID de la famille
     * @return Nombre d'administrateurs
     */
    fun countAdminsByFamilyId(familyId: Int): Int {
        return countWhere { (it.familleId eq familyId) and (it.estAdmin eq true) }
    }

    /**
     * Compte les responsables d'une famille
     *
     * @param familyId ID de la famille
     * @return Nombre de responsables
     */
    fun countResponsiblesByFamilyId(familyId: Int): Int {
        return countWhere { (it.familleId eq familyId) and (it.estResponsable eq true) }
    }

    /**
     * Compte les enfants d'une famille
     *
     * @param familyId ID de la famille
     * @return Nombre d'enfants
     */
    fun countChildrenByFamilyId(familyId: Int): Int {
        return countWhere {
            (it.familleId eq familyId) and
                    (it.estAdmin eq false) and
                    (it.estResponsable eq false)
        }
    }

    // ================================================================
    // MÉTHODES DE GESTION DES RÔLES
    // ================================================================

    /**
     * Met à jour les rôles d'un membre
     *
     * @param memberId ID du membre
     * @param isAdmin Nouveau statut admin
     * @param isResponsible Nouveau statut responsable
     * @return Nombre de lignes mises à jour
     */
    fun updateRoles(memberId: Int, isAdmin: Boolean, isResponsible: Boolean): Int {
        return ArkaDatabase.instance.update(MembresFamille) {
            set(it.estAdmin, isAdmin)
            set(it.estResponsable, isResponsible)
            where { it.membreFamilleId eq memberId }
        }
    }

    /**
     * Promeut un membre au rang d'administrateur
     *
     * @param memberId ID du membre
     * @return Nombre de lignes mises à jour
     */
    fun promoteToAdmin(memberId: Int): Int {
        return updateRoles(memberId, isAdmin = true, isResponsible = true)
    }

    /**
     * Révoque les privilèges administrateur d'un membre
     *
     * @param memberId ID du membre
     * @return Nombre de lignes mises à jour
     */
    fun revokeAdminRights(memberId: Int): Int {
        return updateRoles(memberId, isAdmin = false, isResponsible = false)
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE AVANCÉE
    // ================================================================

    /**
     * Recherche des membres par nom (recherche partielle)
     *
     * @param searchTerm Terme de recherche
     * @param familyId ID de la famille (optionnel)
     * @return Liste des membres trouvés
     */
    fun searchByName(searchTerm: String, familyId: Int? = null): List<MembreFamilleEntity> {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter { member ->
                val nameCondition = member.prenomMembre like "%${searchTerm.trim()}%"
                if (familyId != null) {
                    nameCondition and (member.familleId eq familyId)
                } else {
                    nameCondition
                }
            }
            .toList()
    }

    /**
     * Trouve les membres ajoutés récemment
     *
     * @param familyId ID de la famille
     * @param since Date depuis laquelle chercher
     * @return Liste des nouveaux membres
     */
    fun findRecentMembers(familyId: Int, since: LocalDateTime): List<MembreFamilleEntity> {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter {
                (it.familleId eq familyId) and
                        (it.dateAjoutMembre greaterEq since)
            }
            .toList()
    }

    /**
     * Obtient la date de dernière activité des membres d'une famille
     * Utilise une approche simplifiée avec entity sequence
     *
     * @param familyId ID de la famille
     * @return Date de dernière activité ou null
     */
    fun getLastActivityByFamilyId(familyId: Int): LocalDateTime? {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .filter { it.familleId eq familyId }
            .toList()
            .mapNotNull { it.dateAjoutMembre }
            .maxOrNull()
    }

    // ================================================================
    // MÉTHODES DE VALIDATION ET VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si un membre appartient à une famille spécifique
     *
     * @param memberId ID du membre
     * @param familyId ID de la famille
     * @return true si le membre appartient à la famille
     */
    fun isMemberOfFamily(memberId: Int, familyId: Int): Boolean {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .find {
                (it.membreFamilleId eq memberId) and
                        (it.familleId eq familyId)
            } != null
    }

    /**
     * Vérifie si un membre a des privilèges administrateur
     *
     * @param memberId ID du membre
     * @return true si le membre est admin
     */
    fun isAdmin(memberId: Int): Boolean {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .find { it.membreFamilleId eq memberId }
            ?.estAdmin ?: false
    }

    /**
     * Vérifie si un membre est responsable
     *
     * @param memberId ID du membre
     * @return true si le membre est responsable
     */
    fun isResponsible(memberId: Int): Boolean {
        return ArkaDatabase.instance.sequenceOf(MembresFamille)
            .find { it.membreFamilleId eq memberId }
            ?.estResponsable ?: false
    }
}