package ktorm

import org.ktorm.entity.Entity
import org.ktorm.schema.*
import java.time.LocalDate
import java.time.LocalDateTime

// ================================================================
// INTERFACES D'ENTITÉS KTORM
// ================================================================

/**
 * Interface Entity Famille
 */
interface FamilleEntity : Entity<FamilleEntity> {
    companion object : Entity.Factory<FamilleEntity>()

    val familleId: Int
    var nomFamille: String
    var dateCreationFamille: LocalDateTime?
}

/**
 * Interface Entity Membre de Famille
 */
interface MembreFamilleEntity : Entity<MembreFamilleEntity> {
    companion object : Entity.Factory<MembreFamilleEntity>()

    val membreFamilleId: Int
    var prenomMembre: String
    var mailMembre: String
    var mdpMembre: String
    var dateNaissanceMembre: LocalDate
    var genreMembre: String
    var estResponsable: Boolean
    var estAdmin: Boolean
    var dateAjoutMembre: LocalDateTime?
    var familleId: Int
}

/**
 * Interface Entity Espace
 */
interface EspaceEntity : Entity<EspaceEntity> {
    companion object : Entity.Factory<EspaceEntity>()

    val espaceId: Int
    var nomEspace: String
    var descriptionEspace: String?
    var dateCreationEspace: LocalDateTime?
}

/**
 * Interface Entity Catégorie
 */
interface CategorieEntity : Entity<CategorieEntity> {
    companion object : Entity.Factory<CategorieEntity>()

    val categorieId: Int
    var nomCategorie: String
    var descriptionCategorie: String?
    var dateCreationCategorie: LocalDateTime?
    var espaceId: Int
}

/**
 * Interface Entity Dossier
 */
interface DossierEntity : Entity<DossierEntity> {
    companion object : Entity.Factory<DossierEntity>()

    val dossierId: Int
    var nomDossier: String
    var dateCreationDossier: LocalDateTime?
    var dateDerniereModifDossier: LocalDateTime?
    var estParDefault: Boolean
    var membreFamilleId: Int
    var categorieId: Int
    var dossierParentId: Int?
}

/**
 * Interface Entity Fichier
 */
interface FichierEntity : Entity<FichierEntity> {
    companion object : Entity.Factory<FichierEntity>()

    val fichierId: Int
    var nomFichier: String
    var typeFichier: String?
    var tailleFichier: Long
    var dateCreationFichier: LocalDateTime?
    var dateDerniereModifFichier: LocalDateTime?
    var contenuFichier: ByteArray?
    var cheminFichier: String?
    var createurId: Int
    var proprietaireId: Int
    var dossierId: Int
}

/**
 * Interface Entity Alerte
 */
interface AlerteEntity : Entity<AlerteEntity> {
    companion object : Entity.Factory<AlerteEntity>()

    val alerteId: Int
    var typeAlerte: String
    var categorieAlerte: String?
    var uniteIntervalleAlerte: String
    var valeurIntervalleAlerte: Int
    var dateProchainDeclenchement: LocalDateTime?
    var dateDernierDeclenchement: LocalDateTime?
    var estActive: Boolean
    var dateCreationAlerte: LocalDateTime?
    var membreFamilleId: Int
}

/**
 * Interface Entity Demande de Délégation
 */
interface DemandeDelegationEntity : Entity<DemandeDelegationEntity> {
    companion object : Entity.Factory<DemandeDelegationEntity>()

    val demandeId: Int
    var proprietaireId: Int
    var beneficiaireId: Int
    var portee: String
    var cibleId: Int?
    var typePermission: String
    var dateDemande: LocalDateTime?
    var dateValidation: LocalDateTime?
    var valideeParId: Int?
    var statut: String
    var raisonDemande: String?
    var commentaireAdmin: String?
    var dateExpiration: LocalDateTime?
}

/**
 * Interface Entity Permission Active
 */
interface PermissionActiveEntity : Entity<PermissionActiveEntity> {
    companion object : Entity.Factory<PermissionActiveEntity>()

    val permissionId: Int
    var proprietaireId: Int
    var beneficiaireId: Int
    var portee: String
    var cibleId: Int?
    var typePermission: String
    var dateOctroi: LocalDateTime?
    var dateExpiration: LocalDateTime?
    var estActive: Boolean
}

/**
 * Interface Entity Journal d'Audit
 */
interface JournalAuditPermissionEntity : Entity<JournalAuditPermissionEntity> {
    companion object : Entity.Factory<JournalAuditPermissionEntity>()

    val logId: Int
    var permissionId: Int
    var action: String
    var effectueeParId: Int
    var dateAction: LocalDateTime?
    var details: String?
}

/**
 * Interface Entity Modèle Dossier Défaut
 */
interface ModeleDossierDefautEntity : Entity<ModeleDossierDefautEntity> {
    companion object : Entity.Factory<ModeleDossierDefautEntity>()

    val modeleId: Int
    var nomModele: String
    var categorieId: Int
    var ordreAffichage: Int
}

/**
 * Interface Entity Membre Espace
 */
interface MembreEspaceEntity : Entity<MembreEspaceEntity> {
    companion object : Entity.Factory<MembreEspaceEntity>()

    var membreFamilleId: Int
    var espaceId: Int
    var dateAcces: LocalDateTime?
}

// ================================================================
// DÉFINITIONS DES TABLES KTORM
// ================================================================

/**
 * Table Famille
 */
object Familles : Table<FamilleEntity>("Famille") {
    val familleId = int("FamilleID").primaryKey().bindTo { it.familleId }
    val nomFamille = varchar("NomFamille").bindTo { it.nomFamille }
    val dateCreationFamille = datetime("Date_Creation_Famille").bindTo { it.dateCreationFamille }
}

/**
 * Table Membre_Famille
 */
object MembresFamille : Table<MembreFamilleEntity>("Membre_Famille") {
    val membreFamilleId = int("MembreFamilleID").primaryKey().bindTo { it.membreFamilleId }
    val prenomMembre = varchar("PrenomMembre").bindTo { it.prenomMembre }
    val mailMembre = varchar("Mail_Membre").bindTo { it.mailMembre }
    val mdpMembre = varchar("MDP_Membre").bindTo { it.mdpMembre }
    val dateNaissanceMembre = date("DateNaissance_membre").bindTo { it.dateNaissanceMembre }
    val genreMembre = varchar("Genre_Membre").bindTo { it.genreMembre }
    val estResponsable = boolean("Est_Responsable").bindTo { it.estResponsable }
    val estAdmin = boolean("Est_Admin").bindTo { it.estAdmin }
    val dateAjoutMembre = datetime("Date_Ajout_Membre").bindTo { it.dateAjoutMembre }
    val familleId = int("FamilleID").bindTo { it.familleId }
}

/**
 * Table Espace
 */
object Espaces : Table<EspaceEntity>("Espace") {
    val espaceId = int("EspaceID").primaryKey().bindTo { it.espaceId }
    val nomEspace = varchar("NomEspace").bindTo { it.nomEspace }
    val descriptionEspace = text("Description_Espace").bindTo { it.descriptionEspace }
    val dateCreationEspace = datetime("Date_Creation_Espace").bindTo { it.dateCreationEspace }
}

/**
 * Table Categorie
 */
object Categories : Table<CategorieEntity>("Categorie") {
    val categorieId = int("CategorieID").primaryKey().bindTo { it.categorieId }
    val nomCategorie = varchar("NomCategorie").bindTo { it.nomCategorie }
    val descriptionCategorie = text("Description_Categorie").bindTo { it.descriptionCategorie }
    val dateCreationCategorie = datetime("Date_Creation_Categorie").bindTo { it.dateCreationCategorie }
    val espaceId = int("EspaceID").bindTo { it.espaceId }
}

/**
 * Table Dossier
 */
object Dossiers : Table<DossierEntity>("Dossier") {
    val dossierId = int("DossierID").primaryKey().bindTo { it.dossierId }
    val nomDossier = varchar("NomDossier").bindTo { it.nomDossier }
    val dateCreationDossier = datetime("Date_Creation_Dossier").bindTo { it.dateCreationDossier }
    val dateDerniereModifDossier = datetime("Date_Derniere_Modif_Dossier").bindTo { it.dateDerniereModifDossier }
    val estParDefault = boolean("est_Par_Default").bindTo { it.estParDefault }
    val membreFamilleId = int("MembreFamilleID").bindTo { it.membreFamilleId }
    val categorieId = int("CategorieID").bindTo { it.categorieId }
    val dossierParentId = int("DossierParentID").bindTo { it.dossierParentId }
}

/**
 * Table Fichier
 */
object Fichiers : Table<FichierEntity>("Fichier") {
    val fichierId = int("FichierID").primaryKey().bindTo { it.fichierId }
    val nomFichier = varchar("NomFichier").bindTo { it.nomFichier }
    val typeFichier = varchar("Type_Fichier").bindTo { it.typeFichier }
    val tailleFichier = long("Taille_Fichier").bindTo { it.tailleFichier }
    val dateCreationFichier = datetime("Date_Creation_Fichier").bindTo { it.dateCreationFichier }
    val dateDerniereModifFichier = datetime("Date_Derniere_Modif_Fichier").bindTo { it.dateDerniereModifFichier }
    val contenuFichier = blob("contenu_Fichier").bindTo { it.contenuFichier }
    val cheminFichier = varchar("Chemin_Fichier").bindTo { it.cheminFichier }
    val createurId = int("CreateurID").bindTo { it.createurId }
    val proprietaireId = int("ProprietaireID").bindTo { it.proprietaireId }
    val dossierId = int("DossierID").bindTo { it.dossierId }
}

/**
 * Table Alerte
 */
object Alertes : Table<AlerteEntity>("Alerte") {
    val alerteId = int("AlerteID").primaryKey().bindTo { it.alerteId }
    val typeAlerte = varchar("TypeAlerte").bindTo { it.typeAlerte }
    val categorieAlerte = varchar("Categorie_Alerte").bindTo { it.categorieAlerte }
    val uniteIntervalleAlerte = varchar("Unite_Intervalle_Alerte").bindTo { it.uniteIntervalleAlerte }
    val valeurIntervalleAlerte = int("Valeur_Intervalle_Alerte").bindTo { it.valeurIntervalleAlerte }
    val dateProchainDeclenchement = datetime("Date_Prochain_Declenchement").bindTo { it.dateProchainDeclenchement }
    val dateDernierDeclenchement = datetime("Date_Dernier_Declenchement").bindTo { it.dateDernierDeclenchement }
    val estActive = boolean("Est_Active").bindTo { it.estActive }
    val dateCreationAlerte = datetime("Date_Creation_Alerte").bindTo { it.dateCreationAlerte }
    val membreFamilleId = int("MembreFamilleID").bindTo { it.membreFamilleId }
}

/**
 * Table Demande_Delegation
 */
object DemandesDelegation : Table<DemandeDelegationEntity>("Demande_Delegation") {
    val demandeId = int("DemandeID").primaryKey().bindTo { it.demandeId }
    val proprietaireId = int("ProprietaireID").bindTo { it.proprietaireId }
    val beneficiaireId = int("BeneficiaireID").bindTo { it.beneficiaireId }
    val portee = varchar("Portee").bindTo { it.portee }
    val cibleId = int("CibleID").bindTo { it.cibleId }
    val typePermission = varchar("Type_Permission").bindTo { it.typePermission }
    val dateDemande = datetime("Date_Demande").bindTo { it.dateDemande }
    val dateValidation = datetime("Date_Validation").bindTo { it.dateValidation }
    val valideeParId = int("Validee_Par").bindTo { it.valideeParId }
    val statut = varchar("Statut").bindTo { it.statut }
    val raisonDemande = text("Raison_Demande").bindTo { it.raisonDemande }
    val commentaireAdmin = text("Commentaire_Admin").bindTo { it.commentaireAdmin }
    val dateExpiration = datetime("Date_Expiration").bindTo { it.dateExpiration }
}

/**
 * Table Permission_Active
 */
object PermissionsActives : Table<PermissionActiveEntity>("Permission_Active") {
    val permissionId = int("PermissionID").primaryKey().bindTo { it.permissionId }
    val proprietaireId = int("ProprietaireID").bindTo { it.proprietaireId }
    val beneficiaireId = int("BeneficiaireID").bindTo { it.beneficiaireId }
    val portee = varchar("Portee").bindTo { it.portee }
    val cibleId = int("CibleID").bindTo { it.cibleId }
    val typePermission = varchar("Type_Permission").bindTo { it.typePermission }
    val dateOctroi = datetime("Date_Octroi").bindTo { it.dateOctroi }
    val dateExpiration = datetime("Date_Expiration").bindTo { it.dateExpiration }
    val estActive = boolean("Est_Active").bindTo { it.estActive }
}

/**
 * Table Journal_Audit_Permission
 */
object JournalAuditPermissions : Table<JournalAuditPermissionEntity>("Journal_Audit_Permission") {
    val logId = int("LogID").primaryKey().bindTo { it.logId }
    val permissionId = int("PermissionID").bindTo { it.permissionId }
    val action = varchar("Action").bindTo { it.action }
    val effectueeParId = int("Effectuee_Par").bindTo { it.effectueeParId }
    val dateAction = datetime("Date_Action").bindTo { it.dateAction }
    val details = text("Details").bindTo { it.details }
}

/**
 * Table Modele_Dossier_Defaut
 */
object ModelesDossierDefaut : Table<ModeleDossierDefautEntity>("Modele_Dossier_Defaut") {
    val modeleId = int("ModeleID").primaryKey().bindTo { it.modeleId }
    val nomModele = varchar("NomModele").bindTo { it.nomModele }
    val categorieId = int("CategorieID").bindTo { it.categorieId }
    val ordreAffichage = int("Ordre_Affichage").bindTo { it.ordreAffichage }
}

/**
 * Table Membre_Espace
 */
object MembresEspace : Table<MembreEspaceEntity>("Membre_Espace") {
    val membreFamilleId = int("MembreFamilleID").bindTo { it.membreFamilleId }
    val espaceId = int("EspaceID").bindTo { it.espaceId }
    val dateAcces = datetime("Date_Acces").bindTo { it.dateAcces }
}