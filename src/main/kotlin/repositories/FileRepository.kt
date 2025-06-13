package repositories

import ktorm.*
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.sortedByDescending
import org.ktorm.entity.toList
import org.ktorm.schema.Column
import java.time.LocalDateTime

/**
 * Repository pour la gestion des fichiers dans Arka
 */
class FileRepository : BaseRepository<FichierEntity, org.ktorm.schema.Table<FichierEntity>>() {

    override val table = Fichiers

    override fun getIdColumn(entity: FichierEntity): Column<Int> = table.fichierId

    /**
     * Trouve tous les fichiers d'un dossier
     * @param dossierId L'ID du dossier
     * @return Liste des fichiers du dossier
     */
    fun findByFolder(dossierId: Int): List<FichierEntity> {
        return try {
            entities.filter { table.dossierId eq dossierId }
                .sortedByDescending { table.dateCreationFichier }
                .toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des fichiers du dossier $dossierId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve tous les fichiers d'un propriétaire
     * @param proprietaireId L'ID du propriétaire
     * @return Liste des fichiers du propriétaire
     */
    fun findByOwner(proprietaireId: Int): List<FichierEntity> {
        return try {
            entities.filter { table.proprietaireId eq proprietaireId }
                .sortedByDescending { table.dateCreationFichier }
                .toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des fichiers du propriétaire $proprietaireId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve tous les fichiers créés par un membre
     * @param createurId L'ID du créateur
     * @return Liste des fichiers créés par ce membre
     */
    fun findByCreator(createurId: Int): List<FichierEntity> {
        return try {
            entities.filter { table.createurId eq createurId }
                .sortedByDescending { table.dateCreationFichier }
                .toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des fichiers du créateur $createurId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve un fichier par nom dans un dossier
     * @param nomFichier Le nom du fichier
     * @param dossierId L'ID du dossier
     * @return Le fichier trouvé ou null
     */
    fun findByNameInFolder(nomFichier: String, dossierId: Int): FichierEntity? {
        return try {
            entities.find {
                (table.nomFichier eq nomFichier) and (table.dossierId eq dossierId)
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche du fichier '$nomFichier': ${e.message}")
            null
        }
    }

    /**
     * Trouve les fichiers par type
     * @param typeFichier Le type de fichier (extension)
     * @param proprietaireId Optionnel: limiter à un propriétaire
     * @return Liste des fichiers du type spécifié
     */
    fun findByType(typeFichier: String, proprietaireId: Int? = null): List<FichierEntity> {
        return try {
            var query = entities.filter { table.typeFichier eq typeFichier }

            proprietaireId?.let { query = query.filter { table.proprietaireId eq it } }

            query.sortedByDescending { table.dateCreationFichier }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche par type '$typeFichier': ${e.message}")
            emptyList()
        }
    }

    /**
     * Crée un nouveau fichier
     * @param fileData Les données du fichier
     * @return Le résultat de l'opération
     */
    fun createFile(fileData: CreateFileData): RepositoryResult<Fichier> {
        // Validation
        val validationErrors = validateFile(fileData)
        if (validationErrors.isNotEmpty()) {
            return RepositoryResult.ValidationError(validationErrors)
        }

        // Vérifier l'unicité dans le dossier
        if (findByNameInFolder(fileData.nomFichier, fileData.dossierId) != null) {
            return RepositoryResult.Error("Un fichier avec ce nom existe déjà dans ce dossier")
        }

        return try {
            val fichier = FichierEntity {
                this.nomFichier = fileData.nomFichier
                this.typeFichier = fileData.typeFichier
                this.tailleFichier = fileData.tailleFichier
                this.contenuFichier = fileData.contenuFichier
                this.cheminFichier = fileData.cheminFichier
                this.createurId = fileData.createurId
                this.proprietaireId = fileData.proprietaireId
                this.dossierId = fileData.dossierId
                this.dateCreationFichier = LocalDateTime.now()
                this.dateDerniereModifFichier = LocalDateTime.now()
            }

            if (save(fichier)) {
                RepositoryResult.Success(fichier.toModel())
            } else {
                RepositoryResult.Error("Échec de la création du fichier")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la création: ${e.message}")
        }
    }

    /**
     * Met à jour un fichier
     * @param fichierId L'ID du fichier
     * @param updateData Les nouvelles données
     * @return Le résultat de l'opération
     */
    fun updateFile(fichierId: Int, updateData: UpdateFileData): RepositoryResult<Fichier> {
        return try {
            val fichier = findById(fichierId)
            if (fichier == null) {
                return RepositoryResult.Error("Fichier non trouvé")
            }

            // Vérifier l'unicité si le nom change
            updateData.nomFichier?.let { nouveauNom ->
                if (nouveauNom != fichier.nomFichier) {
                    val existing = findByNameInFolder(nouveauNom, fichier.dossierId)
                    if (existing != null && existing.fichierId != fichierId) {
                        return RepositoryResult.Error("Un fichier avec ce nom existe déjà dans ce dossier")
                    }
                }
                fichier.nomFichier = nouveauNom
            }

            // Mettre à jour les autres champs
            updateData.contenuFichier?.let {
                fichier.contenuFichier = it
                fichier.tailleFichier = it.size.toLong()
            }
            updateData.cheminFichier?.let { fichier.cheminFichier = it }

            fichier.dateDerniereModifFichier = LocalDateTime.now()

            if (update(fichier)) {
                RepositoryResult.Success(fichier.toModel())
            } else {
                RepositoryResult.Error("Échec de la mise à jour")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la mise à jour: ${e.message}")
        }
    }

    /**
     * Déplace un fichier vers un autre dossier
     * @param fichierId L'ID du fichier
     * @param nouveauDossierId L'ID du nouveau dossier
     * @return Le résultat de l'opération
     */
    fun moveFile(fichierId: Int, nouveauDossierId: Int): RepositoryResult<Fichier> {
        return try {
            val fichier = findById(fichierId)
            if (fichier == null) {
                return RepositoryResult.Error("Fichier non trouvé")
            }

            // Vérifier l'unicité dans le nouveau dossier
            if (findByNameInFolder(fichier.nomFichier, nouveauDossierId) != null) {
                return RepositoryResult.Error("Un fichier avec ce nom existe déjà dans le dossier de destination")
            }

            fichier.dossierId = nouveauDossierId
            fichier.dateDerniereModifFichier = LocalDateTime.now()

            if (update(fichier)) {
                RepositoryResult.Success(fichier.toModel())
            } else {
                RepositoryResult.Error("Échec du déplacement")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors du déplacement: ${e.message}")
        }
    }

    /**
     * Change le propriétaire d'un fichier
     * @param fichierId L'ID du fichier
     * @param nouveauProprietaireId L'ID du nouveau propriétaire
     * @return Le résultat de l'opération
     */
    fun changeOwner(fichierId: Int, nouveauProprietaireId: Int): RepositoryResult<Fichier> {
        return try {
            val fichier = findById(fichierId)
            if (fichier == null) {
                return RepositoryResult.Error("Fichier non trouvé")
            }

            fichier.proprietaireId = nouveauProprietaireId
            fichier.dateDerniereModifFichier = LocalDateTime.now()

            if (update(fichier)) {
                RepositoryResult.Success(fichier.toModel())
            } else {
                RepositoryResult.Error("Échec du changement de propriétaire")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors du changement de propriétaire: ${e.message}")
        }
    }

    /**
     * Recherche des fichiers par nom
     * @param nomPartiel Une partie du nom à rechercher
     * @param proprietaireId Optionnel: limiter à un propriétaire
     * @param typeFichier Optionnel: limiter à un type
     * @return Liste des fichiers correspondants
     */
    fun searchByName(
        nomPartiel: String,
        proprietaireId: Int? = null,
        typeFichier: String? = null
    ): List<FichierEntity> {
        return try {
            var query = entities.filter { table.nomFichier like "%$nomPartiel%" }

            proprietaireId?.let { query = query.filter { table.proprietaireId eq it } }
            typeFichier?.let { query = query.filter { table.typeFichier eq it } }

            query.sortedByDescending { table.dateCreationFichier }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche par nom '$nomPartiel': ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtient les fichiers récents d'un propriétaire
     * @param proprietaireId L'ID du propriétaire
     * @param limite Le nombre maximum de fichiers
     * @return Liste des fichiers récents
     */
    fun getRecentFiles(proprietaireId: Int, limite: Int = 10): List<FichierEntity> {
        return try {
            entities.filter { table.proprietaireId eq proprietaireId }
                .sortedByDescending { table.dateDerniereModifFichier }
                .take(limite)
                .toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la récupération des fichiers récents: ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtient les fichiers volumineux d'un propriétaire
     * @param proprietaireId L'ID du propriétaire
     * @param tailleMinimaOctets La taille minimum en octets
     * @return Liste des fichiers volumineux
     */
    fun getLargeFiles(proprietaireId: Int, tailleMinimaOctets: Long = 10_000_000): List<FichierEntity> {
        return try {
            entities.filter {
                (table.proprietaireId eq proprietaireId) and
                        (table.tailleFichier greater tailleMinimaOctets)
            }.sortedByDescending { table.tailleFichier }.toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la récupération des fichiers volumineux: ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtient les statistiques de fichiers d'un propriétaire
     * @param proprietaireId L'ID du propriétaire
     * @return Les statistiques des fichiers
     */
    fun getFileStats(proprietaireId: Int): FileStats {
        return try {
            val totalFiles = count()
            val ownerFiles = database.from(Fichiers)
                .select(
                    count(),
                    sum(Fichiers.tailleFichier),
                    max(Fichiers.dateDerniereModifFichier)
                )
                .where { Fichiers.proprietaireId eq proprietaireId }
                .map { row ->
                    Triple(
                        row.getInt(1) ?: 0,
                        row.getLong(2) ?: 0L,
                        row.getLocalDateTime(3)
                    )
                }.first()

            // Statistiques par type
            val typeStats = database.from(Fichiers)
                .select(Fichiers.typeFichier, count(), sum(Fichiers.tailleFichier))
                .where { Fichiers.proprietaireId eq proprietaireId }
                .groupBy(Fichiers.typeFichier)
                .map { row ->
                    FileTypeStats(
                        type = row.getString(1) ?: "unknown",
                        count = row.getInt(2) ?: 0,
                        totalSize = row.getLong(3) ?: 0L
                    )
                }

            FileStats(
                totalFiles = ownerFiles.first,
                totalSize = ownerFiles.second,
                lastModified = ownerFiles.third,
                typeStats = typeStats
            )
        } catch (e: Exception) {
            println("⚠️ Erreur lors du calcul des stats fichiers: ${e.message}")
            FileStats(0, 0L, null, emptyList())
        }
    }

    /**
     * Obtient l'espace disque utilisé par dossier pour un propriétaire
     * @param proprietaireId L'ID du propriétaire
     * @return Map des dossiers et leur taille
     */
    fun getSpaceUsageByFolder(proprietaireId: Int): Map<Int, Long> {
        return try {
            database.from(Fichiers)
                .select(Fichiers.dossierId, sum(Fichiers.tailleFichier))
                .where { Fichiers.proprietaireId eq proprietaireId }
                .groupBy(Fichiers.dossierId)
                .associate { row ->
                    row.getInt(1) to (row.getLong(2) ?: 0L)
                }
        } catch (e: Exception) {
            println("⚠️ Erreur lors du calcul de l'espace par dossier: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Nettoie les fichiers orphelins (sans dossier parent)
     * @return Le nombre de fichiers supprimés
     */
    fun cleanOrphanFiles(): Int {
        return try {
            // Supprimer les fichiers dont le dossier n'existe plus
            database.delete(Fichiers) {
                it.dossierId notInList (
                        database.from(Dossiers).select(Dossiers.dossierId)
                        )
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors du nettoyage des fichiers orphelins: ${e.message}")
            0
        }
    }

    /**
     * Extrait le type de fichier depuis le nom
     */
    private fun extractFileType(nomFichier: String): String {
        return nomFichier.substringAfterLast('.', "").lowercase()
    }

    /**
     * Valide les données d'un fichier
     */
    private fun validateFile(fileData: CreateFileData): List<String> {
        val errors = mutableListOf<String>()

        if (fileData.nomFichier.isBlank()) {
            errors.add("Le nom du fichier ne peut pas être vide")
        }

        if (fileData.nomFichier.length > 255) {
            errors.add("Le nom du fichier ne peut pas dépasser 255 caractères")
        }

        // Caractères interdits
        val forbiddenChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        val foundForbidden = forbiddenChars.filter { fileData.nomFichier.contains(it) }
        if (foundForbidden.isNotEmpty()) {
            errors.add("Le nom du fichier contient des caractères interdits: ${foundForbidden.joinToString(", ")}")
        }

        if (fileData.tailleFichier < 0) {
            errors.add("La taille du fichier ne peut pas être négative")
        }

        // Limite de taille (100 MB par défaut)
        if (fileData.tailleFichier > 100_000_000) {
            errors.add("Le fichier dépasse la taille maximale autorisée (100 MB)")
        }

        return errors
    }

    override fun validate(entity: FichierEntity): List<String> {
        val fileData = CreateFileData(
            nomFichier = entity.nomFichier,
            typeFichier = entity.typeFichier,
            tailleFichier = entity.tailleFichier,
            contenuFichier = entity.contenuFichier,
            cheminFichier = entity.cheminFichier,
            createurId = entity.createurId,
            proprietaireId = entity.proprietaireId,
            dossierId = entity.dossierId
        )
        return validateFile(fileData)
    }
}

/**
 * Données pour créer un fichier
 */
data class CreateFileData(
    val nomFichier: String,
    val typeFichier: String?,
    val tailleFichier: Long,
    val contenuFichier: ByteArray?,
    val cheminFichier: String?,
    val createurId: Int,
    val proprietaireId: Int,
    val dossierId: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CreateFileData
        return nomFichier == other.nomFichier &&
                typeFichier == other.typeFichier &&
                tailleFichier == other.tailleFichier &&
                contenuFichier?.contentEquals(other.contenuFichier) == true &&
                cheminFichier == other.cheminFichier &&
                createurId == other.createurId &&
                proprietaireId == other.proprietaireId &&
                dossierId == other.dossierId
    }

    override fun hashCode(): Int {
        var result = nomFichier.hashCode()
        result = 31 * result + (typeFichier?.hashCode() ?: 0)
        result = 31 * result + tailleFichier.hashCode()
        result = 31 * result + (contenuFichier?.contentHashCode() ?: 0)
        result = 31 * result + (cheminFichier?.hashCode() ?: 0)
        result = 31 * result + createurId
        result = 31 * result + proprietaireId
        result = 31 * result + dossierId
        return result
    }
}

/**
 * Données pour mettre à jour un fichier
 */
data class UpdateFileData(
    val nomFichier: String? = null,
    val contenuFichier: ByteArray? = null,
    val cheminFichier: String? = null
)

/**
 * Statistiques des fichiers
 */
data class FileStats(
    val totalFiles: Int,
    val totalSize: Long,
    val lastModified: LocalDateTime?,
    val typeStats: List<FileTypeStats>
)

/**
 * Statistiques par type de fichier
 */
data class FileTypeStats(
    val type: String,
    val count: Int,
    val totalSize: Long
)