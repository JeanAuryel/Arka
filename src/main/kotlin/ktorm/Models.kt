package ktorm

import java.time.LocalDate
import java.time.LocalDateTime

// ================================================================
// ENUMS POUR LE SYSTÈME ARKA
// ================================================================

enum class Genre {
    M, F, Autre
}

enum class UniteIntervalle {
    JOUR, SEMAINE, MOIS, ANNEE
}

enum class StatutDemande {
    EN_ATTENTE, APPROUVEE, REJETEE, REVOQUEE
}

enum class PorteePermission {
    ESPACE_COMPLET, CATEGORIE, DOSSIER, FICHIER
}

enum class TypePermission {
    LECTURE, ECRITURE, SUPPRESSION, ACCES_COMPLET
}

// ================================================================
// MODÈLES PRINCIPAUX ARKA
// ================================================================

/**
 * Modèle Famille - Entité racine du système
 */
data class Famille(
    val familleId: Int = 0,
    val nomFamille: String,
    val dateCreationFamille: LocalDateTime? = null
)

/**
 * Modèle Membre de Famille - Utilisateurs du système
 */
data class MembreFamille(
    val membreFamilleId: Int = 0,
    val prenomMembre: String,
    val mailMembre: String,
    val mdpMembre: String, // Hash BCrypt
    val dateNaissanceMembre: LocalDate,
    val genreMembre: Genre,
    val estResponsable: Boolean = false,
    val estAdmin: Boolean = false,
    val dateAjoutMembre: LocalDateTime? = null,
    val familleId: Int
)

/**
 * Modèle Espace - Conteneur principal de catégories
 */
data class Espace(
    val espaceId: Int = 0,
    val nomEspace: String,
    val descriptionEspace: String? = null,
    val dateCreationEspace: LocalDateTime? = null
)

/**
 * Modèle Catégorie - Groupement de dossiers
 */
data class Categorie(
    val categorieId: Int = 0,
    val nomCategorie: String,
    val descriptionCategorie: String? = null,
    val dateCreationCategorie: LocalDateTime? = null,
    val espaceId: Int
)

/**
 * Modèle Dossier - Conteneur de fichiers
 */
data class Dossier(
    val dossierId: Int = 0,
    val nomDossier: String,
    val dateCreationDossier: LocalDateTime? = null,
    val dateDerniereModifDossier: LocalDateTime? = null,
    val estParDefault: Boolean = false,
    val membreFamilleId: Int,
    val categorieId: Int,
    val dossierParentId: Int? = null
)

/**
 * Modèle Fichier - Document stocké
 */
data class Fichier(
    val fichierId: Int = 0,
    val nomFichier: String,
    val typeFichier: String? = null,
    val tailleFichier: Long = 0,
    val dateCreationFichier: LocalDateTime? = null,
    val dateDerniereModifFichier: LocalDateTime? = null,
    val contenuFichier: ByteArray? = null,
    val cheminFichier: String? = null,
    val createurId: Int,
    val proprietaireId: Int,
    val dossierId: Int
) {
    // Override equals et hashCode pour ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fichier

        if (fichierId != other.fichierId) return false
        if (nomFichier != other.nomFichier) return false
        if (typeFichier != other.typeFichier) return false
        if (tailleFichier != other.tailleFichier) return false
        if (dateCreationFichier != other.dateCreationFichier) return false
        if (dateDerniereModifFichier != other.dateDerniereModifFichier) return false
        if (contenuFichier != null) {
            if (other.contenuFichier == null) return false
            if (!contenuFichier.contentEquals(other.contenuFichier)) return false
        } else if (other.contenuFichier != null) return false
        if (cheminFichier != other.cheminFichier) return false
        if (createurId != other.createurId) return false
        if (proprietaireId != other.proprietaireId) return false
        if (dossierId != other.dossierId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fichierId
        result = 31 * result + nomFichier.hashCode()
        result = 31 * result + (typeFichier?.hashCode() ?: 0)
        result = 31 * result + tailleFichier.hashCode()
        result = 31 * result + (dateCreationFichier?.hashCode() ?: 0)
        result = 31 * result + (dateDerniereModifFichier?.hashCode() ?: 0)
        result = 31 * result + (contenuFichier?.contentHashCode() ?: 0)
        result = 31 * result + (cheminFichier?.hashCode() ?: 0)
        result = 31 * result + createurId
        result = 31 * result + proprietaireId
        result = 31 * result + dossierId
        return result
    }
}

/**
 * Modèle Alerte - Notifications programmées
 */
data class Alerte(
    val alerteId: Int = 0,
    val typeAlerte: String,
    val categorieAlerte: String? = null,
    val uniteIntervalleAlerte: UniteIntervalle = UniteIntervalle.JOUR,
    val valeurIntervalleAlerte: Int = 1,
    val dateProchainDeclenchement: LocalDateTime? = null,
    val dateDernierDeclenchement: LocalDateTime? = null,
    val estActive: Boolean = true,
    val dateCreationAlerte: LocalDateTime? = null,
    val membreFamilleId: Int
)

// ================================================================
// MODÈLES POUR LE SYSTÈME DE DÉLÉGATION
// ================================================================

/**
 * Modèle Demande de Délégation - Requêtes de permissions
 */
data class DemandeDelegation(
    val demandeId: Int = 0,
    val proprietaireId: Int,
    val beneficiaireId: Int,
    val portee: PorteePermission,
    val cibleId: Int? = null,
    val typePermission: TypePermission,
    val dateDemande: LocalDateTime? = null,
    val dateValidation: LocalDateTime? = null,
    val valideeParId: Int? = null,
    val statut: StatutDemande = StatutDemande.EN_ATTENTE,
    val raisonDemande: String? = null,
    val commentaireAdmin: String? = null,
    val dateExpiration: LocalDateTime? = null
)

/**
 * Modèle Permission Active - Permissions accordées
 */
data class PermissionActive(
    val permissionId: Int = 0,
    val proprietaireId: Int,
    val beneficiaireId: Int,
    val portee: PorteePermission,
    val cibleId: Int? = null,
    val typePermission: TypePermission,
    val dateOctroi: LocalDateTime? = null,
    val dateExpiration: LocalDateTime? = null,
    val estActive: Boolean = true
)

/**
 * Modèle Journal d'Audit - Traçabilité des permissions
 */
data class JournalAuditPermission(
    val logId: Int = 0,
    val permissionId: Int,
    val action: String,
    val effectueeParId: Int,
    val dateAction: LocalDateTime? = null,
    val details: String? = null
)

// ================================================================
// MODÈLES UTILITAIRES
// ================================================================

/**
 * Modèle Template de Dossier par Défaut
 */
data class ModeleDossierDefaut(
    val modeleId: Int = 0,
    val nomModele: String,
    val categorieId: Int,
    val ordreAffichage: Int = 0
)

/**
 * Modèle Liaison Membre-Espace
 */
data class MembreEspace(
    val membreFamilleId: Int,
    val espaceId: Int,
    val dateAcces: LocalDateTime? = null
)

// ================================================================
// MODÈLES DE TRANSFERT (DTO) POUR L'UI
// ================================================================

/**
 * DTO pour affichage des permissions avec noms d'utilisateurs
 */
data class PermissionAvecNoms(
    val permission: PermissionActive,
    val proprietaireNom: String,
    val proprietaireMail: String,
    val beneficiaireNom: String,
    val beneficiaireMail: String,
    val nomCible: String? = null // Nom du dossier/fichier/catégorie ciblé
)

/**
 * DTO pour affichage des demandes avec noms d'utilisateurs
 */
data class DemandeAvecNoms(
    val demande: DemandeDelegation,
    val proprietaireNom: String,
    val proprietaireMail: String,
    val beneficiaireNom: String,
    val beneficiaireMail: String,
    val validateurNom: String? = null,
    val nomCible: String? = null
)

/**
 * DTO pour navigation avec données complètes
 */
data class DossierComplet(
    val dossier: Dossier,
    val proprietaireNom: String,
    val categorieNom: String,
    val espaceNom: String,
    val nombreFichiers: Int,
    val tailleTotal: Long
)

/**
 * DTO pour statistiques des permissions
 */
data class StatistiquesPermissions(
    val nombrePermissionsActives: Int,
    val nombreDemandesEnAttente: Int,
    val nombrePermissionsExpirees: Int,
    val dernierAcces: LocalDateTime? = null
)

// ================================================================
// ENUM POUR LES TYPES D'UTILISATEURS (basé sur estAdmin/estResponsable)
// ================================================================

/**
 * Types d'utilisateurs dans le système Arka
 * Basé sur les booléens estAdmin et estResponsable
 */
enum class TypeUtilisateur {
    ADMIN,           // estAdmin = true
    RESPONSABLE,     // estResponsable = true, estAdmin = false
    MEMBRE_ORDINAIRE // estAdmin = false, estResponsable = false
}

/**
 * Extension pour obtenir le type d'utilisateur à partir d'un MembreFamille
 */
fun MembreFamille.getTypeUtilisateur(): TypeUtilisateur {
    return when {
        estAdmin -> TypeUtilisateur.ADMIN
        estResponsable -> TypeUtilisateur.RESPONSABLE
        else -> TypeUtilisateur.MEMBRE_ORDINAIRE
    }
}

/**
 * Extension pour obtenir le libellé du rôle
 */
fun MembreFamille.getRoleLibelle(): String {
    return when {
        estAdmin -> "Administrateur"
        estResponsable -> "Responsable"
        else -> "Membre"
    }
}

/**
 * Extension pour vérifier les permissions
 */
fun MembreFamille.peutGererFamille(): Boolean = estAdmin || estResponsable
fun MembreFamille.peutGererPermissions(): Boolean = estAdmin
fun MembreFamille.peutVoirTousFichiers(): Boolean = estAdmin || estResponsable

// ================================================================
// STATISTIQUES FAMILIALES ADAPTÉES AU SYSTÈME RÉEL
// ================================================================

/**
 * Statistiques familiales pour le dashboard
 * Adaptées au système de rôles réel d'Arka
 */
data class FamilyStatistics(
    val familyId: Int,
    val familyName: String,
    val memberCount: Int,
    val adminCount: Int,           // Nombre d'admins (estAdmin = true)
    val responsibleCount: Int,     // Nombre de responsables (estResponsable = true, estAdmin = false)
    val ordinaryMemberCount: Int,  // Nombre de membres ordinaires
    val activeMembers: Int,
    val totalFiles: Int,
    val totalFolders: Int,
    val totalCategories: Int,
    val totalStorageSize: Long,
    val lastActivity: LocalDateTime?,
    val creationDate: LocalDateTime?
)

// ================================================================
// UTILITAIRES POUR LA GESTION DES RÔLES
// ================================================================

/**
 * Classe utilitaire pour la gestion des rôles dans Arka
 */
object ArkaRoleUtils {

    /**
     * Obtient tous les admins d'une famille
     */
    fun List<MembreFamille>.getAdmins(): List<MembreFamille> {
        return this.filter { it.estAdmin }
    }

    /**
     * Obtient tous les responsables (non-admins) d'une famille
     */
    fun List<MembreFamille>.getResponsables(): List<MembreFamille> {
        return this.filter { it.estResponsable && !it.estAdmin }
    }

    /**
     * Obtient tous les membres ordinaires d'une famille
     */
    fun List<MembreFamille>.getMembresOrdinaires(): List<MembreFamille> {
        return this.filter { !it.estAdmin && !it.estResponsable }
    }

    /**
     * Obtient tous les membres avec privilèges (admins + responsables)
     */
    fun List<MembreFamille>.getMembresAvecPrivileges(): List<MembreFamille> {
        return this.filter { it.estAdmin || it.estResponsable }
    }

    /**
     * Vérifie si un membre peut accéder aux données d'un autre membre
     */
    fun peutAccederAux(demandeur: MembreFamille, cible: MembreFamille): Boolean {
        return when {
            demandeur.membreFamilleId == cible.membreFamilleId -> true // Ses propres données
            demandeur.estAdmin -> true // Les admins peuvent tout voir
            demandeur.estResponsable -> true // Les responsables peuvent tout voir
            else -> false // Les membres ordinaires ne peuvent voir que leurs données
        }
    }

    /**
     * Vérifie si un membre peut modifier les données d'un autre membre
     */
    fun peutModifier(demandeur: MembreFamille, cible: MembreFamille): Boolean {
        return when {
            demandeur.membreFamilleId == cible.membreFamilleId -> true // Ses propres données
            demandeur.estAdmin -> true // Les admins peuvent tout modifier
            demandeur.estResponsable && !cible.estAdmin -> true // Les responsables peuvent modifier les non-admins
            else -> false
        }
    }

    /**
     * Obtient la couleur associée au type d'utilisateur
     */
    fun getCouleurRole(membre: MembreFamille): androidx.compose.ui.graphics.Color {
        return when {
            membre.estAdmin -> androidx.compose.ui.graphics.Color(0xFFD32F2F) // Rouge pour admin
            membre.estResponsable -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange pour responsable
            else -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Vert pour membre
        }
    }
}

// ================================================================
// MODÈLES POUR LES STATISTIQUES DE RÔLES
// ================================================================

/**
 * Statistiques détaillées des rôles dans une famille
 */
data class StatistiquesRoles(
    val nombreAdmins: Int,
    val nombreResponsables: Int,
    val nombreMembresOrdinaires: Int,
    val pourcentageAdmins: Double,
    val pourcentageResponsables: Double,
    val pourcentageMembresOrdinaires: Double,
    val membresPlusActifs: List<MembreFamille>,
    val derniereConnexionAdmin: LocalDateTime?,
    val derniereConnexionResponsable: LocalDateTime?
)

/**
 * Informations d'un membre avec contexte familial
 */
data class MembreAvecContexte(
    val membre: MembreFamille,
    val famille: Famille,
    val typeUtilisateur: TypeUtilisateur,
    val roleLibelle: String,
    val nombreFichiersCreés: Int,
    val nombreDossiersGérés: Int,
    val derniereActivite: LocalDateTime?,
    val peutGererFamille: Boolean,
    val peutGererPermissions: Boolean
)

// ================================================================
// VALIDATION ET BUSINESS RULES
// ================================================================

/**
 * Règles métier pour la gestion des rôles
 */
object ArkaRoleValidation {

    /**
     * Vérifie qu'une famille a au moins un administrateur
     */
    fun validateFamilleAMinimumUnAdmin(membres: List<MembreFamille>): Boolean {
        return membres.any { it.estAdmin }
    }

    /**
     * Vérifie si un membre peut être supprimé (ne doit pas être le dernier admin)
     */
    fun peutSupprimerMembre(membreASupprimer: MembreFamille, tousLesMembres: List<MembreFamille>): Boolean {
        if (!membreASupprimer.estAdmin) return true

        val autresAdmins = tousLesMembres.filter {
            it.estAdmin && it.membreFamilleId != membreASupprimer.membreFamilleId
        }

        return autresAdmins.isNotEmpty()
    }

    /**
     * Vérifie si un membre peut changer le rôle d'un autre membre
     */
    fun peutChangerRole(demandeur: MembreFamille, cible: MembreFamille): Boolean {
        return when {
            demandeur.membreFamilleId == cible.membreFamilleId -> false // Pas ses propres rôles
            !demandeur.estAdmin -> false // Seuls les admins peuvent changer les rôles
            else -> true
        }
    }
}

// ================================================================
// FACTORY POUR CRÉER DES STATISTIQUES
// ================================================================

/**
 * Factory pour créer des statistiques familiales
 */
object FamilyStatisticsFactory {

    fun createFromMembers(
        famille: Famille,
        membres: List<MembreFamille>,
        totalFiles: Int = 0,
        totalFolders: Int = 0,
        totalCategories: Int = 0,
        totalStorageSize: Long = 0L,
        lastActivity: LocalDateTime? = null
    ): FamilyStatistics {

        val admins = membres.count { it.estAdmin }
        val responsables = membres.count { it.estResponsable && !it.estAdmin }
        val membresOrdinaires = membres.count { !it.estAdmin && !it.estResponsable }

        return FamilyStatistics(
            familyId = famille.familleId,
            familyName = famille.nomFamille,
            memberCount = membres.size,
            adminCount = admins,
            responsibleCount = responsables,
            ordinaryMemberCount = membresOrdinaires,
            activeMembers = membres.size, // Tous actifs par défaut
            totalFiles = totalFiles,
            totalFolders = totalFolders,
            totalCategories = totalCategories,
            totalStorageSize = totalStorageSize,
            lastActivity = lastActivity,
            creationDate = famille.dateCreationFamille
        )
    }
}