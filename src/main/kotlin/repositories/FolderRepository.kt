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
 * Repository pour la gestion des dossiers
 *
 * Responsabilités:
 * - CRUD des dossiers
 * - Gestion de la hiérarchie des dossiers (parent/enfant)
 * - Recherche et navigation dans l'arborescence
 * - Statistiques et requêtes métier
 * - Validation et vérifications d'intégrité
 *
 * Utilisé par: FolderController, FileController, PermissionController
 */
class FolderRepository : BaseRepository<DossierEntity, Dossiers>() {

    override val table = Dossiers

    /**
     * Obtient la clé primaire d'un dossier
     */
    override fun DossierEntity.getPrimaryKey(): Int = this.dossierId
    override fun getPrimaryKeyColumn(): Column<Int> = Dossiers.dossierId

    /**
     * Met à jour un dossier
     */
    override fun update(entity: DossierEntity): Int {
        return ArkaDatabase.instance.update(Dossiers) {
            set(it.nomDossier, entity.nomDossier)
            set(it.dateDerniereModifDossier, LocalDateTime.now())
            set(it.dossierParentId, entity.dossierParentId)
            where { it.dossierId eq entity.dossierId }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR MEMBRE ET CATÉGORIE
    // ================================================================

    /**
     * Récupère tous les dossiers d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Liste des dossiers du membre
     */
    fun getDossiersByMembre(membreFamilleId: Int): List<DossierEntity> {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
            .sortedBy { it.nomDossier }
    }

    /**
     * Récupère tous les dossiers d'une catégorie
     *
     * @param categorieId ID de la catégorie
     * @return Liste des dossiers de la catégorie
     */
    fun getDossiersByCategorie(categorieId: Int): List<DossierEntity> {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { it.categorieId eq categorieId }
            .toList()
            .sortedBy { it.nomDossier }
    }

    /**
     * Récupère les dossiers d'un membre dans une catégorie spécifique
     *
     * @param membreFamilleId ID du membre
     * @param categorieId ID de la catégorie
     * @return Liste des dossiers correspondants
     */
    fun getDossiersByMembreAndCategorie(membreFamilleId: Int, categorieId: Int): List<DossierEntity> {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter {
                (it.membreFamilleId eq membreFamilleId) and (it.categorieId eq categorieId)
            }
            .toList()
            .sortedBy { it.nomDossier }
    }

    /**
     * Compte les dossiers d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Nombre de dossiers
     */
    fun countByMembre(membreFamilleId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { it.membreFamilleId eq membreFamilleId }
            .toList()
            .size
    }

    /**
     * Compte les dossiers dans une catégorie
     *
     * @param categorieId ID de la catégorie
     * @return Nombre de dossiers
     */
    fun countByCategorie(categorieId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { it.categorieId eq categorieId }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE GESTION DE LA HIÉRARCHIE
    // ================================================================

    /**
     * Récupère les dossiers racine (sans parent) d'un membre dans une catégorie
     *
     * @param membreFamilleId ID du membre
     * @param categorieId ID de la catégorie
     * @return Liste des dossiers racine
     */
    fun getDossiersRacine(membreFamilleId: Int, categorieId: Int): List<DossierEntity> {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter {
                (it.membreFamilleId eq membreFamilleId) and
                        (it.categorieId eq categorieId) and
                        it.dossierParentId.isNull()
            }
            .toList()
            .sortedBy { it.nomDossier }
    }

    /**
     * Récupère les sous-dossiers d'un dossier parent
     *
     * @param dossierParentId ID du dossier parent
     * @return Liste des sous-dossiers
     */
    fun getSousDossiers(dossierParentId: Int): List<DossierEntity> {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { it.dossierParentId eq dossierParentId }
            .toList()
            .sortedBy { it.nomDossier }
    }

    /**
     * Vérifie si un dossier a des sous-dossiers
     *
     * @param dossierId ID du dossier
     * @return true si le dossier a des sous-dossiers
     */
    fun hasSousDossiers(dossierId: Int): Boolean {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { it.dossierParentId eq dossierId }
            .toList()
            .isNotEmpty()
    }

    /**
     * Obtient le chemin complet d'un dossier (hiérarchie)
     *
     * @param dossierId ID du dossier
     * @return Liste des dossiers du chemin (de la racine au dossier)
     */
    fun getCheminComplet(dossierId: Int): List<DossierEntity> {
        val chemin = mutableListOf<DossierEntity>()
        var currentId: Int? = dossierId

        while (currentId != null) {
            val dossier = findById(currentId)
            if (dossier != null) {
                chemin.add(0, dossier) // Ajouter au début
                currentId = dossier.dossierParentId
            } else {
                break
            }
        }

        return chemin
    }

    /**
     * Récupère tous les sous-dossiers récursivement
     *
     * @param dossierId ID du dossier racine
     * @return Liste de tous les sous-dossiers (récursif)
     */
    fun getAllSousDosiersRecursive(dossierId: Int): List<DossierEntity> {
        val allSousDossiers = mutableListOf<DossierEntity>()

        fun collectSousDossiers(parentId: Int) {
            val sousDossiers = getSousDossiers(parentId)
            allSousDossiers.addAll(sousDossiers)
            sousDossiers.forEach { collectSousDossiers(it.dossierId) }
        }

        collectSousDossiers(dossierId)
        return allSousDossiers
    }

    // ================================================================
    // MÉTHODES DE VALIDATION ET VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si un dossier avec ce nom existe dans le même contexte
     *
     * @param nomDossier Nom du dossier
     * @param membreFamilleId ID du propriétaire
     * @param categorieId ID de la catégorie
     * @param dossierParentId ID du parent (null pour racine)
     * @param excludeId ID à exclure de la vérification (pour les mises à jour)
     * @return true si le nom existe déjà
     */
    fun existsByNameInContext(
        nomDossier: String,
        membreFamilleId: Int,
        categorieId: Int,
        dossierParentId: Int?,
        excludeId: Int? = null
    ): Boolean {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { dossier ->
                var condition = (dossier.nomDossier eq nomDossier.trim()) and
                        (dossier.membreFamilleId eq membreFamilleId) and
                        (dossier.categorieId eq categorieId)

                // Gérer le parent (null ou valeur)
                condition = if (dossierParentId == null) {
                    condition and dossier.dossierParentId.isNull()
                } else {
                    condition and (dossier.dossierParentId eq dossierParentId)
                }

                // Exclure un ID spécifique (pour les mises à jour)
                if (excludeId != null) {
                    condition = condition and (dossier.dossierId neq excludeId)
                }

                condition
            }
            .toList()
            .isNotEmpty()
    }

    /**
     * Vérifie si déplacer un dossier créerait une boucle dans la hiérarchie
     *
     * @param dossierId ID du dossier à déplacer
     * @param nouveauParentId ID du nouveau parent
     * @return true si cela créerait une boucle
     */
    fun wouldCreateCycle(dossierId: Int, nouveauParentId: Int): Boolean {
        // Parcourir la hiérarchie vers le haut depuis le nouveau parent
        var currentParentId: Int? = nouveauParentId

        while (currentParentId != null) {
            if (currentParentId == dossierId) {
                return true // Boucle détectée
            }

            val parent = findById(currentParentId)
            currentParentId = parent?.dossierParentId
        }

        return false
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE AVANCÉE
    // ================================================================

    /**
     * Recherche des dossiers par nom (recherche partielle)
     *
     * @param searchTerm Terme de recherche
     * @param membreFamilleId ID du membre (optionnel)
     * @param categorieId ID de la catégorie (optionnel)
     * @return Liste des dossiers trouvés
     */
    fun searchDossiersByName(
        searchTerm: String,
        membreFamilleId: Int? = null,
        categorieId: Int? = null
    ): List<DossierEntity> {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter { dossier ->
                var condition = dossier.nomDossier like "%${searchTerm.trim()}%"

                membreFamilleId?.let {
                    condition = condition and (dossier.membreFamilleId eq it)
                }
                categorieId?.let {
                    condition = condition and (dossier.categorieId eq it)
                }

                condition
            }
            .toList()
            .sortedBy { it.nomDossier }
    }

    /**
     * Récupère les dossiers créés récemment
     *
     * @param membreFamilleId ID du membre (optionnel)
     * @param limit Nombre maximum de dossiers
     * @return Liste des dossiers récents
     */
    fun getRecentDossiers(membreFamilleId: Int? = null, limit: Int = 10): List<DossierEntity> {
        val dossiers = if (membreFamilleId != null) {
            getDossiersByMembre(membreFamilleId)
        } else {
            findAll()
        }

        return dossiers
            .sortedByDescending { it.dateCreationDossier ?: LocalDateTime.MIN }
            .take(limit)
    }

    /**
     * Récupère les dossiers par défaut d'un membre dans une catégorie
     *
     * @param membreFamilleId ID du membre
     * @param categorieId ID de la catégorie
     * @return Liste des dossiers par défaut
     */
    fun getDossiersParDefaut(membreFamilleId: Int, categorieId: Int): List<DossierEntity> {
        return ArkaDatabase.instance.sequenceOf(Dossiers)
            .filter {
                (it.membreFamilleId eq membreFamilleId) and
                        (it.categorieId eq categorieId) and
                        (it.estParDefault eq true)
            }
            .toList()
            .sortedBy { it.nomDossier }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES ET COMPTAGE
    // ================================================================

    /**
     * Compte le nombre de fichiers dans un dossier
     *
     * @param dossierId ID du dossier
     * @return Nombre de fichiers
     */
    fun countFichiersInDossier(dossierId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.dossierId eq dossierId }
            .toList()
            .size
    }

    /**
     * Calcule la taille totale des fichiers dans un dossier
     *
     * @param dossierId ID du dossier
     * @return Taille totale en octets
     */
    fun getTailleTotaleFichiers(dossierId: Int): Long {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.dossierId eq dossierId }
            .toList()
            .sumOf { it.tailleFichier }
    }

    /**
     * Obtient les statistiques complètes d'un dossier
     *
     * @param dossierId ID du dossier
     * @return Statistiques du dossier ou null si non trouvé
     */
    fun getStatistiquesDossier(dossierId: Int): Map<String, Any>? {
        val dossier = findById(dossierId) ?: return null

        val nombreSousDossiers = getSousDossiers(dossierId).size
        val nombreFichiers = countFichiersInDossier(dossierId)
        val tailleTotale = getTailleTotaleFichiers(dossierId)
        val chemin = getCheminComplet(dossierId)

        return mapOf(
            "dossierId" to dossier.dossierId,
            "nomDossier" to dossier.nomDossier,
            "nombreSousDossiers" to nombreSousDossiers,
            "nombreFichiers" to nombreFichiers,
            "tailleTotale" to tailleTotale,
            "profondeur" to chemin.size,
            "estRacine" to (dossier.dossierParentId == null)
        )
    }

    /**
     * Obtient les statistiques de suppression (nombre d'éléments qui seront supprimés)
     *
     * @param dossierId ID du dossier
     * @return Map avec les statistiques de suppression
     */
    fun getStatistiquesSuppression(dossierId: Int): Map<String, Int> {
        val sousDossiers = getAllSousDosiersRecursive(dossierId)
        val nombreSousDossiers = sousDossiers.size

        // Compter tous les fichiers (dossier + sous-dossiers)
        val tousLesDosiersIds = listOf(dossierId) + sousDossiers.map { it.dossierId }
        val nombreFichiers = ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.dossierId inList tousLesDosiersIds }
            .toList()
            .size

        return mapOf(
            "nombreDossiers" to (1 + nombreSousDossiers), // +1 pour le dossier lui-même
            "nombreFichiers" to nombreFichiers
        )
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET MISE À JOUR
    // ================================================================

    /**
     * Crée un nouveau dossier avec validation
     *
     * @param nomDossier Nom du dossier
     * @param membreFamilleId ID du propriétaire
     * @param categorieId ID de la catégorie
     * @param dossierParentId ID du parent (null pour racine)
     * @param estParDefault Si c'est un dossier par défaut
     * @return Le dossier créé ou null en cas d'erreur
     */
    fun createDossier(
        nomDossier: String,
        membreFamilleId: Int,
        categorieId: Int,
        dossierParentId: Int? = null,
        estParDefault: Boolean = false
    ): DossierEntity? {
        // Validation du nom
        val validationErrors = validateNomDossier(nomDossier)
        if (validationErrors.isNotEmpty()) {
            println("Erreurs de validation: ${validationErrors.joinToString(", ")}")
            return null
        }

        // Vérifier l'unicité dans le contexte
        if (existsByNameInContext(nomDossier, membreFamilleId, categorieId, dossierParentId)) {
            println("Un dossier avec ce nom existe déjà dans ce contexte")
            return null
        }

        return try {
            val dossier = DossierEntity {
                this.nomDossier = nomDossier.trim()
                this.membreFamilleId = membreFamilleId
                this.categorieId = categorieId
                this.dossierParentId = dossierParentId
                this.estParDefault = estParDefault
                this.dateCreationDossier = LocalDateTime.now()
                this.dateDerniereModifDossier = LocalDateTime.now()
            }

            create(dossier)
        } catch (e: Exception) {
            println("Erreur lors de la création du dossier: ${e.message}")
            null
        }
    }

    /**
     * Met à jour le nom d'un dossier avec validation
     *
     * @param dossierId ID du dossier
     * @param nouveauNom Nouveau nom
     * @return true si la mise à jour a réussi
     */
    fun updateNomDossier(dossierId: Int, nouveauNom: String): Boolean {
        val dossier = findById(dossierId) ?: return false

        // Validation du nom
        val validationErrors = validateNomDossier(nouveauNom)
        if (validationErrors.isNotEmpty()) {
            return false
        }

        // Vérifier l'unicité (sauf pour le dossier actuel)
        if (existsByNameInContext(
                nouveauNom,
                dossier.membreFamilleId,
                dossier.categorieId,
                dossier.dossierParentId,
                dossierId
            )) {
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(Dossiers) {
                set(it.nomDossier, nouveauNom.trim())
                set(it.dateDerniereModifDossier, LocalDateTime.now())
                where { it.dossierId eq dossierId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour du nom du dossier $dossierId: ${e.message}")
            false
        }
    }

    /**
     * Déplace un dossier vers un nouveau parent
     *
     * @param dossierId ID du dossier à déplacer
     * @param nouveauParentId ID du nouveau parent (null pour racine)
     * @return true si le déplacement a réussi
     */
    fun deplacerDossier(dossierId: Int, nouveauParentId: Int?): Boolean {
        val dossier = findById(dossierId) ?: return false

        // Vérifier qu'on ne crée pas une boucle
        if (nouveauParentId != null && wouldCreateCycle(dossierId, nouveauParentId)) {
            println("Impossible de déplacer: cela créerait une boucle dans la hiérarchie")
            return false
        }

        // Vérifier l'unicité dans le nouveau contexte
        if (existsByNameInContext(
                dossier.nomDossier,
                dossier.membreFamilleId,
                dossier.categorieId,
                nouveauParentId,
                dossierId
            )) {
            println("Un dossier avec ce nom existe déjà dans la destination")
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(Dossiers) {
                set(it.dossierParentId, nouveauParentId)
                set(it.dateDerniereModifDossier, LocalDateTime.now())
                where { it.dossierId eq dossierId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors du déplacement du dossier $dossierId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION
    // ================================================================

    /**
     * Supprime un dossier avec cascade (tous ses sous-dossiers et fichiers)
     * ATTENTION: Suppression définitive !
     *
     * @param dossierId ID du dossier
     * @return true si la suppression a réussi
     */
    fun deleteDossierWithCascade(dossierId: Int): Boolean {
        return try {
            transaction {
                // La suppression cascade est gérée par les contraintes FK
                val rowsAffected = delete(dossierId)
                rowsAffected > 0
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression du dossier $dossierId: ${e.message}")
            false
        }
    }

    /**
     * Supprime tous les dossiers d'un membre
     * Utilisé lors de la suppression d'un membre
     *
     * @param membreFamilleId ID du membre
     * @return Nombre de dossiers supprimés
     */
    fun deleteAllByMembre(membreFamilleId: Int): Int {
        return try {
            ArkaDatabase.instance.delete(Dossiers) {
                it.membreFamilleId eq membreFamilleId
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression des dossiers du membre $membreFamilleId: ${e.message}")
            0
        }
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Valide le nom d'un dossier
     *
     * @param nomDossier Le nom à valider
     * @return Liste des erreurs de validation
     */
    fun validateNomDossier(nomDossier: String): List<String> {
        val errors = mutableListOf<String>()

        if (nomDossier.isBlank()) {
            errors.add("Le nom du dossier ne peut pas être vide")
        }

        if (nomDossier.length > 255) {
            errors.add("Le nom du dossier ne peut pas dépasser 255 caractères")
        }

        // Caractères interdits dans les noms de fichiers/dossiers
        val forbiddenChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        val foundForbidden = forbiddenChars.filter { nomDossier.contains(it) }
        if (foundForbidden.isNotEmpty()) {
            errors.add("Le nom du dossier contient des caractères interdits: ${foundForbidden.joinToString(", ")}")
        }

        return errors
    }

    /**
     * Nettoie les dossiers orphelins (sans catégorie ou membre valide)
     *
     * @return Nombre de dossiers nettoyés
     */
    fun cleanOrphanFolders(): Int {
        return try {
            // Les dossiers orphelins seront gérés par les contraintes FK CASCADE
            // Cette méthode peut être étendue pour d'autres vérifications
            0
        } catch (e: Exception) {
            println("Erreur lors du nettoyage des dossiers orphelins: ${e.message}")
            0
        }
    }
}