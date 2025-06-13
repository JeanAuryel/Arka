package repositories

import ktorm.*
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.ktorm.schema.Column
import java.time.LocalDateTime

/**
 * Repository pour la gestion des dossiers dans Arka
 */
class FolderRepository : BaseRepository<DossierEntity, org.ktorm.schema.Table<DossierEntity>>() {

    override val table = Dossiers

    override fun getIdColumn(entity: DossierEntity): Column<Int> = table.dossierId

    /**
     * Trouve tous les dossiers d'un membre
     * @param membreId L'ID du membre
     * @return Liste des dossiers du membre
     */
    fun findByMember(membreId: Int): List<DossierEntity> {
        return try {
            entities.filter { table.membreFamilleId eq membreId }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des dossiers du membre $membreId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve tous les dossiers d'une catégorie
     * @param categorieId L'ID de la catégorie
     * @return Liste des dossiers de la catégorie
     */
    fun findByCategory(categorieId: Int): List<DossierEntity> {
        return try {
            entities.filter { table.categorieId eq categorieId }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des dossiers de la catégorie $categorieId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve tous les dossiers d'un membre dans une catégorie
     * @param membreId L'ID du membre
     * @param categorieId L'ID de la catégorie
     * @return Liste des dossiers correspondants
     */
    fun findByMemberAndCategory(membreId: Int, categorieId: Int): List<DossierEntity> {
        return try {
            entities.filter {
                (table.membreFamilleId eq membreId) and (table.categorieId eq categorieId)
            }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des dossiers: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve les sous-dossiers d'un dossier parent
     * @param dossierParentId L'ID du dossier parent
     * @return Liste des sous-dossiers
     */
    fun findSubFolders(dossierParentId: Int): List<DossierEntity> {
        return try {
            entities.filter { table.dossierParentId eq dossierParentId }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des sous-dossiers: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve les dossiers racine (sans parent) d'un membre dans une catégorie
     * @param membreId L'ID du membre
     * @param categorieId L'ID de la catégorie
     * @return Liste des dossiers racine
     */
    fun findRootFolders(membreId: Int, categorieId: Int): List<DossierEntity> {
        return try {
            entities.filter {
                (table.membreFamilleId eq membreId) and
                        (table.categorieId eq categorieId) and
                        (table.dossierParentId.isNull())
            }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des dossiers racine: ${e.message}")
            emptyList()
        }
    }

    /**
     * Crée un nouveau dossier
     * @param nomDossier Le nom du dossier
     * @param membreId L'ID du propriétaire
     * @param categorieId L'ID de la catégorie
     * @param dossierParentId L'ID du dossier parent (optionnel)
     * @param estParDefault Si c'est un dossier par défaut
     * @return Le résultat de l'opération
     */
    fun createFolder(
        nomDossier: String,
        membreId: Int,
        categorieId: Int,
        dossierParentId: Int? = null,
        estParDefault: Boolean = false
    ): RepositoryResult<Dossier> {

        // Validation
        val validationErrors = validateFolderName(nomDossier)
        if (validationErrors.isNotEmpty()) {
            return RepositoryResult.ValidationError(validationErrors)
        }

        // Vérifier l'unicité dans le même contexte (membre + catégorie + parent)
        if (folderExistsInContext(nomDossier, membreId, categorieId, dossierParentId)) {
            return RepositoryResult.Error("Un dossier avec ce nom existe déjà dans ce contexte")
        }

        return try {
            val dossier = DossierEntity {
                this.nomDossier = nomDossier
                this.membreFamilleId = membreId
                this.categorieId = categorieId
                this.dossierParentId = dossierParentId
                this.estParDefault = estParDefault
                this.dateCreationDossier = LocalDateTime.now()
                this.dateDerniereModifDossier = LocalDateTime.now()
            }

            if (save(dossier)) {
                RepositoryResult.Success(dossier.toModel())
            } else {
                RepositoryResult.Error("Échec de la création du dossier")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la création: ${e.message}")
        }
    }

    /**
     * Met à jour un dossier
     * @param dossierId L'ID du dossier
     * @param nomDossier Le nouveau nom
     * @return Le résultat de l'opération
     */
    fun updateFolder(dossierId: Int, nomDossier: String): RepositoryResult<Dossier> {
        val validationErrors = validateFolderName(nomDossier)
        if (validationErrors.isNotEmpty()) {
            return RepositoryResult.ValidationError(validationErrors)
        }

        return try {
            val dossier = findById(dossierId)
            if (dossier == null) {
                return RepositoryResult.Error("Dossier non trouvé")
            }

            // Vérifier l'unicité (sauf pour le dossier actuel)
            if (folderExistsInContext(nomDossier, dossier.membreFamilleId, dossier.categorieId, dossier.dossierParentId, dossierId)) {
                return RepositoryResult.Error("Un dossier avec ce nom existe déjà dans ce contexte")
            }

            dossier.nomDossier = nomDossier
            dossier.dateDerniereModifDossier = LocalDateTime.now()

            if (update(dossier)) {
                RepositoryResult.Success(dossier.toModel())
            } else {
                RepositoryResult.Error("Échec de la mise à jour")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la mise à jour: ${e.message}")
        }
    }

    /**
     * Déplace un dossier vers un nouveau parent
     * @param dossierId L'ID du dossier à déplacer
     * @param nouveauParentId Le nouvel ID parent (null pour racine)
     * @return Le résultat de l'opération
     */
    fun moveFolder(dossierId: Int, nouveauParentId: Int?): RepositoryResult<Dossier> {
        return try {
            val dossier = findById(dossierId)
            if (dossier == null) {
                return RepositoryResult.Error("Dossier non trouvé")
            }

            // Vérifier qu'on ne crée pas une boucle (dossier parent de lui-même)
            if (nouveauParentId != null && wouldCreateCycle(dossierId, nouveauParentId)) {
                return RepositoryResult.Error("Impossible de déplacer: cela créerait une boucle dans la hiérarchie")
            }

            // Vérifier l'unicité dans le nouveau contexte
            if (folderExistsInContext(dossier.nomDossier, dossier.membreFamilleId, dossier.categorieId, nouveauParentId, dossierId)) {
                return RepositoryResult.Error("Un dossier avec ce nom existe déjà dans la destination")
            }

            dossier.dossierParentId = nouveauParentId
            dossier.dateDerniereModifDossier = LocalDateTime.now()

            if (update(dossier)) {
                RepositoryResult.Success(dossier.toModel())
            } else {
                RepositoryResult.Error("Échec du déplacement")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors du déplacement: ${e.message}")
        }
    }

    /**
     * Supprime un dossier avec tous ses sous-dossiers et fichiers (CASCADE)
     * @param dossierId L'ID du dossier à supprimer
     * @return Le résultat de l'opération avec statistiques
     */
    fun deleteFolderWithContent(dossierId: Int): RepositoryResult<FolderDeletionStats> {
        return withTransaction {
            try {
                val stats = getFolderDeletionStats(dossierId)

                if (stats.totalFolders == 0) {
                    return@withTransaction RepositoryResult.Error("Dossier non trouvé")
                }

                // La suppression CASCADE est gérée par la base de données
                val deleted = deleteById(dossierId)

                if (deleted) {
                    RepositoryResult.Success(stats)
                } else {
                    RepositoryResult.Error("Échec de la suppression")
                }
            } catch (e: Exception) {
                RepositoryResult.Error("Erreur lors de la suppression: ${e.message}")
            }
        } ?: RepositoryResult.Error("Erreur de transaction")
    }

    /**
     * Recherche des dossiers par nom
     * @param nomPartiel Une partie du nom à rechercher
     * @param membreId Optionnel: limiter à un membre
     * @param categorieId Optionnel: limiter à une catégorie
     * @return Liste des dossiers correspondants
     */
    fun searchByName(nomPartiel: String, membreId: Int? = null, categorieId: Int? = null): List<DossierEntity> {
        return try {
            var query = entities.filter { table.nomDossier like "%$nomPartiel%" }

            membreId?.let { query = query.filter { table.membreFamilleId eq it } }
            categorieId?.let { query = query.filter { table.categorieId eq it } }

            query.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche par nom '$nomPartiel': ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtient le nombre de fichiers dans un dossier
     * @param dossierId L'ID du dossier
     * @return Le nombre de fichiers
     */
    fun getFileCount(dossierId: Int): Int {
        return try {
            database.from(Fichiers)
                .select(count())
                .where { Fichiers.dossierId eq dossierId }
                .map { row -> row.getInt(1) }
                .first()
        } catch (e: Exception) {
            println("⚠️ Erreur lors du comptage des fichiers pour le dossier $dossierId: ${e.message}")
            0
        }
    }

    /**
     * Obtient la taille totale des fichiers dans un dossier
     * @param dossierId L'ID du dossier
     * @return La taille totale en octets
     */
    fun getTotalSize(dossierId: Int): Long {
        return try {
            database.from(Fichiers)
                .select(sum(Fichiers.tailleFichier))
                .where { Fichiers.dossierId eq dossierId }
                .map { row -> row.getLong(1) ?: 0L }
                .first()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Obtient les statistiques d'un dossier avec ses sous-dossiers
     * @param dossierId L'ID du dossier
     * @return Les statistiques complètes
     */
    fun getFolderStats(dossierId: Int): FolderStats? {
        return try {
            val dossier = findById(dossierId)?.toModel() ?: return null

            val subFolders = findSubFolders(dossierId).map { it.toModel() }
            val fileCount = getFileCount(dossierId)
            val totalSize = getTotalSize(dossierId)

            FolderStats(
                dossier = dossier,
                sousDossiers = subFolders,
                nombreFichiers = fileCount,
                tailleTotal = totalSize
            )
        } catch (e: Exception) {
            println("⚠️ Erreur lors du calcul des stats pour le dossier $dossierId: ${e.message}")
            null
        }
    }

    /**
     * Vérifie si un dossier existe dans un contexte donné
     */
    private fun folderExistsInContext(
        nomDossier: String,
        membreId: Int,
        categorieId: Int,
        parentId: Int?,
        excludeId: Int? = null
    ): Boolean {
        return try {
            var query = entities.filter {
                (table.nomDossier eq nomDossier) and
                        (table.membreFamilleId eq membreId) and
                        (table.categorieId eq categorieId)
            }

            // Gérer le parent (null ou valeur)
            query = if (parentId == null) {
                query.filter { table.dossierParentId.isNull() }
            } else {
                query.filter { table.dossierParentId eq parentId }
            }

            // Exclure un ID spécifique (pour les mises à jour)
            excludeId?.let { query = query.filter { table.dossierId neq it } }

            query.toList().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si déplacer un dossier créerait une boucle
     */
    private fun wouldCreateCycle(dossierId: Int, nouveauParentId: Int): Boolean {
        return try {
            // Parcourir la hiérarchie vers le haut depuis le nouveau parent
            var currentParentId: Int? = nouveauParentId

            while (currentParentId != null) {
                if (currentParentId == dossierId) {
                    return true // Boucle détectée
                }

                val parent = findById(currentParentId)
                currentParentId = parent?.dossierParentId
            }

            false
        } catch (e: Exception) {
            true // En cas d'erreur, refuser pour éviter les problèmes
        }
    }

    /**
     * Calcule les statistiques de suppression avant de supprimer
     */
    private fun getFolderDeletionStats(dossierId: Int): FolderDeletionStats {
        return try {
            val totalFolders = 1 + countSubFoldersRecursive(dossierId)
            val totalFiles = database.from(Fichiers)
                .select(count())
                .where { Fichiers.dossierId eq dossierId }
                .map { row -> row.getInt(1) }
                .first()

            FolderDeletionStats(
                totalFolders = totalFolders,
                totalFiles = totalFiles
            )
        } catch (e: Exception) {
            FolderDeletionStats(0, 0)
        }
    }

    /**
     * Compte récursivement les sous-dossiers
     */
    private fun countSubFoldersRecursive(dossierId: Int): Int {
        return try {
            val subFolders = findSubFolders(dossierId)
            var count = subFolders.size

            for (subFolder in subFolders) {
                count += countSubFoldersRecursive(subFolder.dossierId)
            }

            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Valide le nom d'un dossier
     */
    private fun validateFolderName(nomDossier: String): List<String> {
        val errors = mutableListOf<String>()

        if (nomDossier.isBlank()) {
            errors.add("Le nom du dossier ne peut pas être vide")
        }

        if (nomDossier.length < 1) {
            errors.add("Le nom du dossier doit contenir au moins 1 caractère")
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

    override fun validate(entity: DossierEntity): List<String> {
        return validateFolderName(entity.nomDossier)
    }
}

/**
 * Statistiques d'un dossier
 */
data class FolderStats(
    val dossier: Dossier,
    val sousDossiers: List<Dossier>,
    val nombreFichiers: Int,
    val tailleTotal: Long
)

/**
 * Statistiques de suppression d'un dossier
 */
data class FolderDeletionStats(
    val totalFolders: Int,
    val totalFiles: Int
)
