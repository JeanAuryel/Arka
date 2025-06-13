package ktorm

import org.ktorm.entity.Entity
import org.ktorm.entity.add
import org.ktorm.entity.sequenceOf

// ================================================================
// MAPPERS : ENTITÉ → MODÈLE (Database → Domain)
// ================================================================

/**
 * Convertit une FamilleEntity en Famille
 */
fun FamilleEntity.toModel(): Famille {
    return Famille(
        familleId = this.familleId,
        nomFamille = this.nomFamille,
        dateCreationFamille = this.dateCreationFamille
    )
}

/**
 * Convertit une MembreFamilleEntity en MembreFamille
 */
fun MembreFamilleEntity.toModel(): MembreFamille {
    return MembreFamille(
        membreFamilleId = this.membreFamilleId,
        prenomMembre = this.prenomMembre,
        mailMembre = this.mailMembre,
        mdpMembre = this.mdpMembre,
        dateNaissanceMembre = this.dateNaissanceMembre,
        genreMembre = Genre.valueOf(this.genreMembre),
        estResponsable = this.estResponsable,
        estAdmin = this.estAdmin,
        dateAjoutMembre = this.dateAjoutMembre,
        familleId = this.familleId
    )
}

/**
 * Convertit une EspaceEntity en Espace
 */
fun EspaceEntity.toModel(): Espace {
    return Espace(
        espaceId = this.espaceId,
        nomEspace = this.nomEspace,
        descriptionEspace = this.descriptionEspace,
        dateCreationEspace = this.dateCreationEspace
    )
}

/**
 * Convertit une CategorieEntity en Categorie
 */
fun CategorieEntity.toModel(): Categorie {
    return Categorie(
        categorieId = this.categorieId,
        nomCategorie = this.nomCategorie,
        descriptionCategorie = this.descriptionCategorie,
        dateCreationCategorie = this.dateCreationCategorie,
        espaceId = this.espaceId
    )
}

/**
 * Convertit une DossierEntity en Dossier
 */
fun DossierEntity.toModel(): Dossier {
    return Dossier(
        dossierId = this.dossierId,
        nomDossier = this.nomDossier,
        dateCreationDossier = this.dateCreationDossier,
        dateDerniereModifDossier = this.dateDerniereModifDossier,
        estParDefault = this.estParDefault,
        membreFamilleId = this.membreFamilleId,
        categorieId = this.categorieId,
        dossierParentId = this.dossierParentId
    )
}

/**
 * Convertit une FichierEntity en Fichier
 */
fun FichierEntity.toModel(): Fichier {
    return Fichier(
        fichierId = this.fichierId,
        nomFichier = this.nomFichier,
        typeFichier = this.typeFichier,
        tailleFichier = this.tailleFichier,
        dateCreationFichier = this.dateCreationFichier,
        dateDerniereModifFichier = this.dateDerniereModifFichier,
        contenuFichier = this.contenuFichier,
        cheminFichier = this.cheminFichier,
        createurId = this.createurId,
        proprietaireId = this.proprietaireId,
        dossierId = this.dossierId
    )
}

/**
 * Convertit une AlerteEntity en Alerte
 */
fun AlerteEntity.toModel(): Alerte {
    return Alerte(
        alerteId = this.alerteId,
        typeAlerte = this.typeAlerte,
        categorieAlerte = this.categorieAlerte,
        uniteIntervalleAlerte = UniteIntervalle.valueOf(this.uniteIntervalleAlerte),
        valeurIntervalleAlerte = this.valeurIntervalleAlerte,
        dateProchainDeclenchement = this.dateProchainDeclenchement,
        dateDernierDeclenchement = this.dateDernierDeclenchement,
        estActive = this.estActive,
        dateCreationAlerte = this.dateCreationAlerte,
        membreFamilleId = this.membreFamilleId
    )
}

/**
 * Convertit une DemandeDelegationEntity en DemandeDelegation
 */
fun DemandeDelegationEntity.toModel(): DemandeDelegation {
    return DemandeDelegation(
        demandeId = this.demandeId,
        proprietaireId = this.proprietaireId,
        beneficiaireId = this.beneficiaireId,
        portee = PorteePermission.valueOf(this.portee),
        cibleId = this.cibleId,
        typePermission = TypePermission.valueOf(this.typePermission),
        dateDemande = this.dateDemande,
        dateValidation = this.dateValidation,
        valideeParId = this.valideeParId,
        statut = StatutDemande.valueOf(this.statut),
        raisonDemande = this.raisonDemande,
        commentaireAdmin = this.commentaireAdmin,
        dateExpiration = this.dateExpiration
    )
}

/**
 * Convertit une PermissionActiveEntity en PermissionActive
 */
fun PermissionActiveEntity.toModel(): PermissionActive {
    return PermissionActive(
        permissionId = this.permissionId,
        proprietaireId = this.proprietaireId,
        beneficiaireId = this.beneficiaireId,
        portee = PorteePermission.valueOf(this.portee),
        cibleId = this.cibleId,
        typePermission = TypePermission.valueOf(this.typePermission),
        dateOctroi = this.dateOctroi,
        dateExpiration = this.dateExpiration,
        estActive = this.estActive
    )
}

/**
 * Convertit une JournalAuditPermissionEntity en JournalAuditPermission
 */
fun JournalAuditPermissionEntity.toModel(): JournalAuditPermission {
    return JournalAuditPermission(
        logId = this.logId,
        permissionId = this.permissionId,
        action = this.action,
        effectueeParId = this.effectueeParId,
        dateAction = this.dateAction,
        details = this.details
    )
}

/**
 * Convertit une ModeleDossierDefautEntity en ModeleDossierDefaut
 */
fun ModeleDossierDefautEntity.toModel(): ModeleDossierDefaut {
    return ModeleDossierDefaut(
        modeleId = this.modeleId,
        nomModele = this.nomModele,
        categorieId = this.categorieId,
        ordreAffichage = this.ordreAffichage
    )
}

/**
 * Convertit une MembreEspaceEntity en MembreEspace
 */
fun MembreEspaceEntity.toModel(): MembreEspace {
    return MembreEspace(
        membreFamilleId = this.membreFamilleId,
        espaceId = this.espaceId,
        dateAcces = this.dateAcces
    )
}

// ================================================================
// MAPPERS : MODÈLE → ENTITÉ (Domain → Database)
// ================================================================

/**
 * Met à jour une FamilleEntity à partir d'un modèle Famille
 */
fun FamilleEntity.updateFromModel(famille: Famille) {
    this.nomFamille = famille.nomFamille
    famille.dateCreationFamille?.let { this.dateCreationFamille = it }
}

/**
 * Met à jour une MembreFamilleEntity à partir d'un modèle MembreFamille
 */
fun MembreFamilleEntity.updateFromModel(membre: MembreFamille) {
    this.prenomMembre = membre.prenomMembre
    this.mailMembre = membre.mailMembre
    this.mdpMembre = membre.mdpMembre
    this.dateNaissanceMembre = membre.dateNaissanceMembre
    this.genreMembre = membre.genreMembre.name
    this.estResponsable = membre.estResponsable
    this.estAdmin = membre.estAdmin
    membre.dateAjoutMembre?.let { this.dateAjoutMembre = it }
    this.familleId = membre.familleId
}

/**
 * Met à jour une EspaceEntity à partir d'un modèle Espace
 */
fun EspaceEntity.updateFromModel(espace: Espace) {
    this.nomEspace = espace.nomEspace
    this.descriptionEspace = espace.descriptionEspace
    espace.dateCreationEspace?.let { this.dateCreationEspace = it }
}

/**
 * Met à jour une CategorieEntity à partir d'un modèle Categorie
 */
fun CategorieEntity.updateFromModel(categorie: Categorie) {
    this.nomCategorie = categorie.nomCategorie
    this.descriptionCategorie = categorie.descriptionCategorie
    categorie.dateCreationCategorie?.let { this.dateCreationCategorie = it }
    this.espaceId = categorie.espaceId
}

/**
 * Met à jour une DossierEntity à partir d'un modèle Dossier
 */
fun DossierEntity.updateFromModel(dossier: Dossier) {
    this.nomDossier = dossier.nomDossier
    dossier.dateCreationDossier?.let { this.dateCreationDossier = it }
    dossier.dateDerniereModifDossier?.let { this.dateDerniereModifDossier = it }
    this.estParDefault = dossier.estParDefault
    this.membreFamilleId = dossier.membreFamilleId
    this.categorieId = dossier.categorieId
    this.dossierParentId = dossier.dossierParentId
}

/**
 * Met à jour une FichierEntity à partir d'un modèle Fichier
 */
fun FichierEntity.updateFromModel(fichier: Fichier) {
    this.nomFichier = fichier.nomFichier
    this.typeFichier = fichier.typeFichier
    this.tailleFichier = fichier.tailleFichier
    fichier.dateCreationFichier?.let { this.dateCreationFichier = it }
    fichier.dateDerniereModifFichier?.let { this.dateDerniereModifFichier = it }
    this.contenuFichier = fichier.contenuFichier
    this.cheminFichier = fichier.cheminFichier
    this.createurId = fichier.createurId
    this.proprietaireId = fichier.proprietaireId
    this.dossierId = fichier.dossierId
}

/**
 * Met à jour une AlerteEntity à partir d'un modèle Alerte
 */
fun AlerteEntity.updateFromModel(alerte: Alerte) {
    this.typeAlerte = alerte.typeAlerte
    this.categorieAlerte = alerte.categorieAlerte
    this.uniteIntervalleAlerte = alerte.uniteIntervalleAlerte.name
    this.valeurIntervalleAlerte = alerte.valeurIntervalleAlerte
    this.dateProchainDeclenchement = alerte.dateProchainDeclenchement
    this.dateDernierDeclenchement = alerte.dateDernierDeclenchement
    this.estActive = alerte.estActive
    alerte.dateCreationAlerte?.let { this.dateCreationAlerte = it }
    this.membreFamilleId = alerte.membreFamilleId
}

/**
 * Met à jour une DemandeDelegationEntity à partir d'un modèle DemandeDelegation
 */
fun DemandeDelegationEntity.updateFromModel(demande: DemandeDelegation) {
    this.proprietaireId = demande.proprietaireId
    this.beneficiaireId = demande.beneficiaireId
    this.portee = demande.portee.name
    this.cibleId = demande.cibleId
    this.typePermission = demande.typePermission.name
    demande.dateDemande?.let { this.dateDemande = it }
    this.dateValidation = demande.dateValidation
    this.valideeParId = demande.valideeParId
    this.statut = demande.statut.name
    this.raisonDemande = demande.raisonDemande
    this.commentaireAdmin = demande.commentaireAdmin
    this.dateExpiration = demande.dateExpiration
}

/**
 * Met à jour une PermissionActiveEntity à partir d'un modèle PermissionActive
 */
fun PermissionActiveEntity.updateFromModel(permission: PermissionActive) {
    this.proprietaireId = permission.proprietaireId
    this.beneficiaireId = permission.beneficiaireId
    this.portee = permission.portee.name
    this.cibleId = permission.cibleId
    this.typePermission = permission.typePermission.name
    permission.dateOctroi?.let { this.dateOctroi = it }
    this.dateExpiration = permission.dateExpiration
    this.estActive = permission.estActive
}

/**
 * Met à jour une JournalAuditPermissionEntity à partir d'un modèle JournalAuditPermission
 */
fun JournalAuditPermissionEntity.updateFromModel(journal: JournalAuditPermission) {
    this.permissionId = journal.permissionId
    this.action = journal.action
    this.effectueeParId = journal.effectueeParId
    journal.dateAction?.let { this.dateAction = it }
    this.details = journal.details
}

/**
 * Met à jour une ModeleDossierDefautEntity à partir d'un modèle ModeleDossierDefaut
 */
fun ModeleDossierDefautEntity.updateFromModel(modele: ModeleDossierDefaut) {
    this.nomModele = modele.nomModele
    this.categorieId = modele.categorieId
    this.ordreAffichage = modele.ordreAffichage
}

/**
 * Met à jour une MembreEspaceEntity à partir d'un modèle MembreEspace
 */
fun MembreEspaceEntity.updateFromModel(membreEspace: MembreEspace) {
    this.membreFamilleId = membreEspace.membreFamilleId
    this.espaceId = membreEspace.espaceId
    membreEspace.dateAcces?.let { this.dateAcces = it }
}

// ================================================================
// HELPERS POUR CRÉATION D'ENTITÉS
// ================================================================

/**
 * Crée une nouvelle FamilleEntity à partir d'un modèle Famille
 */
fun createFamilleEntity(famille: Famille): FamilleEntity {
    val entity = FamilleEntity {
        nomFamille = famille.nomFamille
        dateCreationFamille = famille.dateCreationFamille
    }
    return entity
}

/**
 * Crée une nouvelle MembreFamilleEntity à partir d'un modèle MembreFamille
 */
fun createMembreFamilleEntity(membre: MembreFamille): MembreFamilleEntity {
    val entity = MembreFamilleEntity {
        prenomMembre = membre.prenomMembre
        mailMembre = membre.mailMembre
        mdpMembre = membre.mdpMembre
        dateNaissanceMembre = membre.dateNaissanceMembre
        genreMembre = membre.genreMembre.name
        estResponsable = membre.estResponsable
        estAdmin = membre.estAdmin
        dateAjoutMembre = membre.dateAjoutMembre
        familleId = membre.familleId
    }
    return entity
}

/**
 * Crée une nouvelle DemandeDelegationEntity à partir d'un modèle DemandeDelegation
 */
fun createDemandeDelegationEntity(demande: DemandeDelegation): DemandeDelegationEntity {
    val entity = DemandeDelegationEntity {
        proprietaireId = demande.proprietaireId
        beneficiaireId = demande.beneficiaireId
        portee = demande.portee.name
        cibleId = demande.cibleId
        typePermission = demande.typePermission.name
        dateDemande = demande.dateDemande
        dateValidation = demande.dateValidation
        valideeParId = demande.valideeParId
        statut = demande.statut.name
        raisonDemande = demande.raisonDemande
        commentaireAdmin = demande.commentaireAdmin
        dateExpiration = demande.dateExpiration
    }
    return entity
}

/**
 * Crée une nouvelle PermissionActiveEntity à partir d'un modèle PermissionActive
 */
fun createPermissionActiveEntity(permission: PermissionActive): PermissionActiveEntity {
    val entity = PermissionActiveEntity {
        proprietaireId = permission.proprietaireId
        beneficiaireId = permission.beneficiaireId
        portee = permission.portee.name
        cibleId = permission.cibleId
        typePermission = permission.typePermission.name
        dateOctroi = permission.dateOctroi
        dateExpiration = permission.dateExpiration
        estActive = permission.estActive
    }
    return entity
}

// ================================================================
// EXTENSIONS POUR COLLECTIONS
// ================================================================

/**
 * Convertit une liste d'entités en liste de modèles
 */
inline fun <reified E : Entity<E>, reified M> List<E>.toModels(
    mapper: E.() -> M
): List<M> = this.map { it.mapper() }

/**
 * Convertit une séquence d'entités en liste de modèles
 */
inline fun <reified E : Entity<E>, reified M> Sequence<E>.toModels(
    crossinline mapper: E.() -> M
): List<M> = this.map { it.mapper() }.toList()
