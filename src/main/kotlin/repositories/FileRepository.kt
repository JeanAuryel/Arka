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
 * Repository pour la gestion des fichiers
 *
 * Responsabilités:
 * - CRUD des fichiers
 * - Gestion des propriétaires et créateurs
 * - Recherche et filtrage des fichiers
 * - Statistiques et requêtes métier
 * - Validation et vérifications d'intégrité
 *
 * Utilisé par: FileController, FolderController, PermissionController
 */
class FileRepository : BaseRepository<FichierEntity, Fichiers>() {

    override val table = Fichiers

    /**
     * Obtient la clé primaire d'un fichier
     */
    override fun FichierEntity.getPrimaryKey(): Int = this.fichierId
    override fun getPrimaryKeyColumn(): Column<Int> = Fichiers.fichierId

    /**
     * Met à jour un fichier
     */
    override fun update(entity: FichierEntity): Int {
        return ArkaDatabase.instance.update(Fichiers) {
            set(it.nomFichier, entity.nomFichier)
            set(it.typeFichier, entity.typeFichier)
            set(it.tailleFichier, entity.tailleFichier)
            set(it.contenuFichier, entity.contenuFichier)
            set(it.cheminFichier, entity.cheminFichier)
            set(it.dateDerniereModifFichier, LocalDateTime.now())
            where { it.fichierId eq entity.fichierId }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR DOSSIER ET PROPRIÉTAIRE
    // ================================================================

    /**
     * Récupère tous les fichiers d'un dossier
     *
     * @param dossierId ID du dossier
     * @return Liste des fichiers du dossier triés par date de création
     */
    fun getFichiersByDossier(dossierId: Int): List<FichierEntity> {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.dossierId eq dossierId }
            .toList()
            .sortedByDescending { it.dateCreationFichier ?: LocalDateTime.MIN }
    }

    /**
     * Récupère tous les fichiers d'un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @return Liste des fichiers du propriétaire
     */
    fun getFichiersByProprietaire(proprietaireId: Int): List<FichierEntity> {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.proprietaireId eq proprietaireId }
            .toList()
            .sortedByDescending { it.dateCreationFichier ?: LocalDateTime.MIN }
    }

    /**
     * Récupère tous les fichiers créés par un membre
     *
     * @param createurId ID du créateur
     * @return Liste des fichiers créés par ce membre
     */
    fun getFichiersByCreateur(createurId: Int): List<FichierEntity> {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.createurId eq createurId }
            .toList()
            .sortedByDescending { it.dateCreationFichier ?: LocalDateTime.MIN }
    }

    /**
     * Trouve un fichier par nom dans un dossier
     *
     * @param nomFichier Nom du fichier
     * @param dossierId ID du dossier
     * @return Le fichier trouvé ou null
     */
    fun getFichierByNameInDossier(nomFichier: String, dossierId: Int): FichierEntity? {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .find {
                (it.nomFichier eq nomFichier.trim()) and (it.dossierId eq dossierId)
            }
    }

    /**
     * Compte les fichiers d'un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @return Nombre de fichiers
     */
    fun countByProprietaire(proprietaireId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.proprietaireId eq proprietaireId }
            .toList()
            .size
    }

    /**
     * Compte les fichiers dans un dossier
     *
     * @param dossierId ID du dossier
     * @return Nombre de fichiers
     */
    fun countByDossier(dossierId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.dossierId eq dossierId }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE VALIDATION ET VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si un fichier avec ce nom existe dans un dossier
     *
     * @param nomFichier Nom du fichier
     * @param dossierId ID du dossier
     * @param excludeId ID à exclure de la vérification (pour les mises à jour)
     * @return true si le nom existe déjà
     */
    fun existsByNameInDossier(nomFichier: String, dossierId: Int, excludeId: Int? = null): Boolean {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { fichier ->
                val nameCondition = (fichier.nomFichier eq nomFichier.trim()) and (fichier.dossierId eq dossierId)
                if (excludeId != null) {
                    nameCondition and (fichier.fichierId neq excludeId)
                } else {
                    nameCondition
                }
            }
            .toList()
            .isNotEmpty()
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE AVANCÉE
    // ================================================================

    /**
     * Recherche des fichiers par nom (recherche partielle)
     *
     * @param searchTerm Terme de recherche
     * @param proprietaireId ID du propriétaire (optionnel)
     * @param dossierId ID du dossier (optionnel)
     * @return Liste des fichiers trouvés
     */
    fun searchFichiersByName(
        searchTerm: String,
        proprietaireId: Int? = null,
        dossierId: Int? = null
    ): List<FichierEntity> {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { fichier ->
                var condition = fichier.nomFichier like "%${searchTerm.trim()}%"

                proprietaireId?.let {
                    condition = condition and (fichier.proprietaireId eq it)
                }
                dossierId?.let {
                    condition = condition and (fichier.dossierId eq it)
                }

                condition
            }
            .toList()
            .sortedBy { it.nomFichier }
    }

    /**
     * Récupère les fichiers par type
     *
     * @param typeFichier Type de fichier (extension)
     * @param proprietaireId ID du propriétaire (optionnel)
     * @return Liste des fichiers du type spécifié
     */
    fun getFichiersByType(typeFichier: String, proprietaireId: Int? = null): List<FichierEntity> {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { fichier ->
                var condition = fichier.typeFichier eq typeFichier.lowercase()

                proprietaireId?.let {
                    condition = condition and (fichier.proprietaireId eq it)
                }

                condition
            }
            .toList()
            .sortedByDescending { it.dateCreationFichier ?: LocalDateTime.MIN }
    }

    /**
     * Récupère les fichiers récents d'un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @param limit Nombre maximum de fichiers
     * @return Liste des fichiers récents
     */
    fun getFichiersRecents(proprietaireId: Int, limit: Int = 10): List<FichierEntity> {
        return getFichiersByProprietaire(proprietaireId)
            .sortedByDescending { it.dateDerniereModifFichier ?: LocalDateTime.MIN }
            .take(limit)
    }

    /**
     * Récupère les fichiers volumineux d'un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @param tailleMinimaOctets Taille minimum en octets
     * @return Liste des fichiers volumineux triés par taille
     */
    fun getFichiersVolumineux(proprietaireId: Int, tailleMinimaOctets: Long = 10_000_000): List<FichierEntity> {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter {
                (it.proprietaireId eq proprietaireId) and (it.tailleFichier greater tailleMinimaOctets)
            }
            .toList()
            .sortedByDescending { it.tailleFichier }
    }

    /**
     * Récupère les fichiers créés dans une période
     *
     * @param since Date de début
     * @param until Date de fin (optionnelle)
     * @param proprietaireId ID du propriétaire (optionnel)
     * @return Liste des fichiers de la période
     */
    fun getFichiersParPeriode(
        since: LocalDateTime,
        until: LocalDateTime? = null,
        proprietaireId: Int? = null
    ): List<FichierEntity> {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { fichier ->
                var condition = fichier.dateCreationFichier greaterEq since

                until?.let {
                    condition = condition and (fichier.dateCreationFichier lessEq it)
                }
                proprietaireId?.let {
                    condition = condition and (fichier.proprietaireId eq it)
                }

                condition
            }
            .toList()
            .sortedByDescending { it.dateCreationFichier ?: LocalDateTime.MIN }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES ET ANALYSE
    // ================================================================

    /**
     * Calcule la taille totale des fichiers d'un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @return Taille totale en octets
     */
    fun getTailleTotaleByProprietaire(proprietaireId: Int): Long {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.proprietaireId eq proprietaireId }
            .toList()
            .sumOf { it.tailleFichier }
    }

    /**
     * Calcule la taille totale des fichiers dans un dossier
     *
     * @param dossierId ID du dossier
     * @return Taille totale en octets
     */
    fun getTailleTotaleByDossier(dossierId: Int): Long {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.dossierId eq dossierId }
            .toList()
            .sumOf { it.tailleFichier }
    }

    /**
     * Obtient les statistiques par type de fichier pour un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @return Map type -> (nombre, taille totale)
     */
    fun getStatistiquesParType(proprietaireId: Int): Map<String, Pair<Int, Long>> {
        val fichiers = getFichiersByProprietaire(proprietaireId)

        return fichiers
            .groupBy { it.typeFichier ?: "unknown" }
            .mapValues { (_, fichiersList) ->
                Pair(
                    fichiersList.size,
                    fichiersList.sumOf { it.tailleFichier }
                )
            }
    }

    /**
     * Obtient les statistiques complètes d'un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @return Map avec toutes les statistiques
     */
    fun getStatistiquesCompletes(proprietaireId: Int): Map<String, Any> {
        val fichiers = getFichiersByProprietaire(proprietaireId)
        val nombreFichiers = fichiers.size
        val tailleTotale = fichiers.sumOf { it.tailleFichier }
        val derniereFichier = fichiers.maxByOrNull { it.dateDerniereModifFichier ?: LocalDateTime.MIN }
        val typesStats = getStatistiquesParType(proprietaireId)

        return mapOf(
            "nombreFichiers" to nombreFichiers,
            "tailleTotale" to tailleTotale,
            "derniereModification" to (derniereFichier?.dateDerniereModifFichier ?: "Aucune"),
            "nombreTypes" to typesStats.size,
            "typeLePlusUtilise" to (typesStats.maxByOrNull { it.value.first }?.key ?: "Aucun")
        )
    }

    /**
     * Obtient l'utilisation d'espace par dossier pour un propriétaire
     *
     * @param proprietaireId ID du propriétaire
     * @return Map dossierId -> taille totale
     */
    fun getUtilisationEspaceParDossier(proprietaireId: Int): Map<Int, Long> {
        return ArkaDatabase.instance.sequenceOf(Fichiers)
            .filter { it.proprietaireId eq proprietaireId }
            .toList()
            .groupBy { it.dossierId }
            .mapValues { (_, fichiers) -> fichiers.sumOf { it.tailleFichier } }
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET MISE À JOUR
    // ================================================================

    /**
     * Crée un nouveau fichier avec validation
     *
     * @param nomFichier Nom du fichier
     * @param typeFichier Type/extension du fichier
     * @param tailleFichier Taille en octets
     * @param contenuFichier Contenu binaire (optionnel)
     * @param cheminFichier Chemin externe (optionnel)
     * @param createurId ID du créateur
     * @param proprietaireId ID du propriétaire
     * @param dossierId ID du dossier de destination
     * @return Le fichier créé ou null en cas d'erreur
     */
    fun createFichier(
        nomFichier: String,
        typeFichier: String?,
        tailleFichier: Long,
        contenuFichier: ByteArray? = null,
        cheminFichier: String? = null,
        createurId: Int,
        proprietaireId: Int,
        dossierId: Int
    ): FichierEntity? {
        // Validation
        val validationErrors = validateNomFichier(nomFichier, tailleFichier)
        if (validationErrors.isNotEmpty()) {
            println("Erreurs de validation: ${validationErrors.joinToString(", ")}")
            return null
        }

        // Vérifier l'unicité dans le dossier
        if (existsByNameInDossier(nomFichier, dossierId)) {
            println("Un fichier avec ce nom existe déjà dans ce dossier")
            return null
        }

        return try {
            val fichier = FichierEntity {
                this.nomFichier = nomFichier.trim()
                this.typeFichier = typeFichier?.lowercase()
                this.tailleFichier = tailleFichier
                this.contenuFichier = contenuFichier
                this.cheminFichier = cheminFichier
                this.createurId = createurId
                this.proprietaireId = proprietaireId
                this.dossierId = dossierId
                this.dateCreationFichier = LocalDateTime.now()
                this.dateDerniereModifFichier = LocalDateTime.now()
            }

            create(fichier)
        } catch (e: Exception) {
            println("Erreur lors de la création du fichier: ${e.message}")
            null
        }
    }

    /**
     * Met à jour le nom d'un fichier avec validation
     *
     * @param fichierId ID du fichier
     * @param nouveauNom Nouveau nom
     * @return true si la mise à jour a réussi
     */
    fun updateNomFichier(fichierId: Int, nouveauNom: String): Boolean {
        val fichier = findById(fichierId) ?: return false

        // Validation du nom
        val validationErrors = validateNomFichier(nouveauNom, fichier.tailleFichier)
        if (validationErrors.isNotEmpty()) {
            return false
        }

        // Vérifier l'unicité (sauf pour le fichier actuel)
        if (existsByNameInDossier(nouveauNom, fichier.dossierId, fichierId)) {
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(Fichiers) {
                set(it.nomFichier, nouveauNom.trim())
                set(it.dateDerniereModifFichier, LocalDateTime.now())
                where { it.fichierId eq fichierId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour du nom du fichier $fichierId: ${e.message}")
            false
        }
    }

    /**
     * Met à jour le contenu d'un fichier
     *
     * @param fichierId ID du fichier
     * @param nouveauContenu Nouveau contenu binaire
     * @return true si la mise à jour a réussi
     */
    fun updateContenuFichier(fichierId: Int, nouveauContenu: ByteArray): Boolean {
        return try {
            val rowsAffected = ArkaDatabase.instance.update(Fichiers) {
                set(it.contenuFichier, nouveauContenu)
                set(it.tailleFichier, nouveauContenu.size.toLong())
                set(it.dateDerniereModifFichier, LocalDateTime.now())
                where { it.fichierId eq fichierId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour du contenu du fichier $fichierId: ${e.message}")
            false
        }
    }

    /**
     * Déplace un fichier vers un autre dossier
     *
     * @param fichierId ID du fichier
     * @param nouveauDossierId ID du nouveau dossier
     * @return true si le déplacement a réussi
     */
    fun deplacerFichier(fichierId: Int, nouveauDossierId: Int): Boolean {
        val fichier = findById(fichierId) ?: return false

        // Vérifier l'unicité dans le nouveau dossier
        if (existsByNameInDossier(fichier.nomFichier, nouveauDossierId)) {
            println("Un fichier avec ce nom existe déjà dans le dossier de destination")
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(Fichiers) {
                set(it.dossierId, nouveauDossierId)
                set(it.dateDerniereModifFichier, LocalDateTime.now())
                where { it.fichierId eq fichierId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors du déplacement du fichier $fichierId: ${e.message}")
            false
        }
    }

    /**
     * Change le propriétaire d'un fichier
     *
     * @param fichierId ID du fichier
     * @param nouveauProprietaireId ID du nouveau propriétaire
     * @return true si le changement a réussi
     */
    fun changerProprietaire(fichierId: Int, nouveauProprietaireId: Int): Boolean {
        return try {
            val rowsAffected = ArkaDatabase.instance.update(Fichiers) {
                set(it.proprietaireId, nouveauProprietaireId)
                set(it.dateDerniereModifFichier, LocalDateTime.now())
                where { it.fichierId eq fichierId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors du changement de propriétaire du fichier $fichierId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION ET NETTOYAGE
    // ================================================================

    /**
     * Supprime tous les fichiers d'un dossier
     * Utilisé lors de la suppression d'un dossier
     *
     * @param dossierId ID du dossier
     * @return Nombre de fichiers supprimés
     */
    fun deleteAllByDossier(dossierId: Int): Int {
        return try {
            ArkaDatabase.instance.delete(Fichiers) {
                it.dossierId eq dossierId
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression des fichiers du dossier $dossierId: ${e.message}")
            0
        }
    }

    /**
     * Supprime tous les fichiers d'un propriétaire
     * Utilisé lors de la suppression d'un membre
     *
     * @param proprietaireId ID du propriétaire
     * @return Nombre de fichiers supprimés
     */
    fun deleteAllByProprietaire(proprietaireId: Int): Int {
        return try {
            ArkaDatabase.instance.delete(Fichiers) {
                it.proprietaireId eq proprietaireId
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression des fichiers du propriétaire $proprietaireId: ${e.message}")
            0
        }
    }

    /**
     * Nettoie les fichiers orphelins (dossier inexistant)
     *
     * @return Nombre de fichiers nettoyés
     */
    fun cleanOrphanFiles(): Int {
        return try {
            // Supprimer les fichiers dont le dossier n'existe plus
            val dossiersExistants = ArkaDatabase.instance.sequenceOf(Dossiers)
                .toList()
                .map { it.dossierId }
                .toSet()

            val fichiersOrphelins = ArkaDatabase.instance.sequenceOf(Fichiers)
                .toList()
                .filter { it.dossierId !in dossiersExistants }

            var deleted = 0
            fichiersOrphelins.forEach { fichier ->
                if (delete(fichier.fichierId) > 0) {
                    deleted++
                }
            }

            deleted
        } catch (e: Exception) {
            println("Erreur lors du nettoyage des fichiers orphelins: ${e.message}")
            0
        }
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Valide le nom et la taille d'un fichier
     *
     * @param nomFichier Le nom à valider
     * @param tailleFichier La taille en octets
     * @return Liste des erreurs de validation
     */
    fun validateNomFichier(nomFichier: String, tailleFichier: Long): List<String> {
        val errors = mutableListOf<String>()

        if (nomFichier.isBlank()) {
            errors.add("Le nom du fichier ne peut pas être vide")
        }

        if (nomFichier.length > 255) {
            errors.add("Le nom du fichier ne peut pas dépasser 255 caractères")
        }

        // Caractères interdits dans les noms de fichiers
        val forbiddenChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        val foundForbidden = forbiddenChars.filter { nomFichier.contains(it) }
        if (foundForbidden.isNotEmpty()) {
            errors.add("Le nom du fichier contient des caractères interdits: ${foundForbidden.joinToString(", ")}")
        }

        if (tailleFichier < 0) {
            errors.add("La taille du fichier ne peut pas être négative")
        }

        // Limite de taille (100 MB par défaut)
        if (tailleFichier > 100_000_000) {
            errors.add("Le fichier dépasse la taille maximale autorisée (100 MB)")
        }

        return errors
    }

    /**
     * Extrait le type de fichier depuis le nom
     *
     * @param nomFichier Nom du fichier
     * @return Extension en minuscules
     */
    fun extractFileType(nomFichier: String): String {
        return nomFichier.substringAfterLast('.', "").lowercase()
    }

    /**
     * Formate la taille d'un fichier en format lisible
     *
     * @param taille Taille en octets
     * @return Taille formatée (ex: "1.5 MB")
     */
    fun formatFileSize(taille: Long): String {
        return when {
            taille < 1024 -> "$taille B"
            taille < 1024 * 1024 -> "${String.format("%.1f", taille / 1024.0)} KB"
            taille < 1024 * 1024 * 1024 -> "${String.format("%.1f", taille / (1024.0 * 1024.0))} MB"
            else -> "${String.format("%.1f", taille / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
}