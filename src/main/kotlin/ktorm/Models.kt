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