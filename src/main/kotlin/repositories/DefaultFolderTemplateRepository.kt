package repositories

import ktorm.*
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.ktorm.schema.Column

/**
 * Repository pour la gestion des modèles de dossiers par défaut dans Arka
 */
class `DefaultFolderTemplateRepository.kt` : BaseRepository<ModeleDossierDefautEntity, org.ktorm.schema.Table<ModeleDossierDefautEntity>>() {

    override val table = ModelesDossierDefaut

    override fun getIdColumn(entity: ModeleDossierDefautEntity): Column<Int> = table.modeleId

    /**
     * Trouve tous les modèles pour une catégorie donnée, triés par ordre d'affichage
     * @param categorieId L'ID de la catégorie
     * @return Liste des modèles ordonnés
     */
    fun findByCategory(categorieId: Int): List<ModeleDossierDefautEntity> {
        return try {
            entities.filter { table.categorieId eq categorieId }
                .sortedBy { table.ordreAffichage }
                .toList()
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche des modèles pour la catégorie $categorieId: ${e.message}")
            emptyList()
        }
    }

    /**
     * Trouve un modèle par nom dans une catégorie
     * @param nomModele Le nom du modèle
     * @param categorieId L'ID de la catégorie
     * @return Le modèle trouvé ou null
     */
    fun findByNameInCategory(nomModele: String, categorieId: Int): ModeleDossierDefautEntity? {
        return try {
            entities.find {
                (table.nomModele eq nomModele) and (table.categorieId eq categorieId)
            }
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la recherche du modèle '$nomModele': ${e.message}")
            null
        }
    }

    /**
     * Crée un nouveau modèle de dossier par défaut
     * @param nomModele Le nom du modèle
     * @param categorieId L'ID de la catégorie
     * @param ordreAffichage L'ordre d'affichage (optionnel)
     * @return Le résultat de l'opération
     */
    fun createTemplate(
        nomModele: String,
        categorieId: Int,
        ordreAffichage: Int? = null
    ): RepositoryResult<ModeleDossierDefaut> {

        // Validation
        val validationErrors = validateTemplateName(nomModele)
        if (validationErrors.isNotEmpty()) {
            return RepositoryResult.ValidationError(validationErrors)
        }

        // Vérifier l'unicité dans la catégorie
        if (findByNameInCategory(nomModele, categorieId) != null) {
            return RepositoryResult.Error("Un modèle avec ce nom existe déjà dans cette catégorie")
        }

        return try {
            // Déterminer l'ordre d'affichage si non spécifié
            val ordre = ordreAffichage ?: getNextOrderInCategory(categorieId)

            val modele = ModeleDossierDefautEntity {
                this.nomModele = nomModele
                this.categorieId = categorieId
                this.ordreAffichage = ordre
            }

            if (save(modele)) {
                RepositoryResult.Success(modele.toModel())
            } else {
                RepositoryResult.Error("Échec de la création du modèle")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la création: ${e.message}")
        }
    }

    /**
     * Met à jour un modèle de dossier
     * @param modeleId L'ID du modèle
     * @param nomModele Le nouveau nom
     * @param ordreAffichage Le nouvel ordre (optionnel)
     * @return Le résultat de l'opération
     */
    fun updateTemplate(
        modeleId: Int,
        nomModele: String,
        ordreAffichage: Int? = null
    ): RepositoryResult<ModeleDossierDefaut> {

        val validationErrors = validateTemplateName(nomModele)
        if (validationErrors.isNotEmpty()) {
            return RepositoryResult.ValidationError(validationErrors)
        }

        return try {
            val modele = findById(modeleId)
            if (modele == null) {
                return RepositoryResult.Error("Modèle non trouvé")
            }

            // Vérifier l'unicité (sauf pour le modèle actuel)
            val existing = findByNameInCategory(nomModele, modele.categorieId)
            if (existing != null && existing.modeleId != modeleId) {
                return RepositoryResult.Error("Un modèle avec ce nom existe déjà dans cette catégorie")
            }

            modele.nomModele = nomModele
            ordreAffichage?.let { modele.ordreAffichage = it }

            if (update(modele)) {
                RepositoryResult.Success(modele.toModel())
            } else {
                RepositoryResult.Error("Échec de la mise à jour")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Erreur lors de la mise à jour: ${e.message}")
        }
    }

    /**
     * Réorganise l'ordre des modèles dans une catégorie
     * @param categorieId L'ID de la catégorie
     * @param nouveauxOrdres Map des modeleId vers leur nouvel ordre
     * @return Le résultat de l'opération
     */
    fun reorderTemplates(categorieId: Int, nouveauxOrdres: Map<Int, Int>): RepositoryResult<Boolean> {
        return withTransaction {
            try {
                var success = true

                for ((modeleId, nouvelOrdre) in nouveauxOrdres) {
                    val modele = findById(modeleId)
                    if (modele != null && modele.categorieId == categorieId) {
                        modele.ordreAffichage = nouvelOrdre
                        if (!update(modele)) {
                            success = false
                            break
                        }
                    }
                }

                if (success) {
                    RepositoryResult.Success(true)
                } else {
                    RepositoryResult.Error("Échec de la réorganisation")
                }
            } catch (e: Exception) {
                RepositoryResult.Error("Erreur lors de la réorganisation: ${e.message}")
            }
        } ?: RepositoryResult.Error("Erreur de transaction")
    }

    /**
     * Crée les dossiers par défaut pour un membre dans une catégorie
     * @param membreId L'ID du membre
     * @param categorieId L'ID de la catégorie
     * @return Le nombre de dossiers créés
     */
    fun createDefaultFoldersForMember(membreId: Int, categorieId: Int): Int {
        return try {
            val modeles = findByCategory(categorieId)
            var createdCount = 0

            for (modele in modeles) {
                try {
                    // Utiliser FolderRepository pour créer le dossier
                    // (ceci sera implémenté quand FolderRepository sera créé)
                    val dossier = DossierEntity {
                        nomDossier = modele.nomModele
                        membreFamilleId = membreId
                        this.categorieId = categorieId
                        estParDefault = true
                        dateCreationDossier = java.time.LocalDateTime.now()
                        dateDerniereModifDossier = java.time.LocalDateTime.now()
                    }

                    database.insert(Dossiers) {
                        set(it.nomDossier, dossier.nomDossier)
                        set(it.membreFamilleId, dossier.membreFamilleId)
                        set(it.categorieId, dossier.categorieId)
                        set(it.estParDefault, dossier.estParDefault)
                        set(it.dateCreationDossier, dossier.dateCreationDossier)
                        set(it.dateDerniereModifDossier, dossier.dateDerniereModifDossier)
                    }

                    createdCount++
                } catch (e: Exception) {
                    println("⚠️ Erreur lors de la création du dossier '${modele.nomModele}': ${e.message}")
                }
            }

            createdCount
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la création des dossiers par défaut: ${e.message}")
            0
        }
    }

    /**
     * Supprime un modèle et tous les dossiers associés (avec confirmation)
     * @param modeleId L'ID du modèle à supprimer
     * @param supprimerDossiersAssocies Si true, supprime aussi les dossiers créés à partir de ce modèle
     * @return Le résultat de l'opération
     */
    fun deleteTemplate(modeleId: Int, supprimerDossiersAssocies: Boolean = false): RepositoryResult<Boolean> {
        return withTransaction {
            try {
                val modele = findById(modeleId)
                if (modele == null) {
                    return@withTransaction RepositoryResult.Error("Modèle non trouvé")
                }

                if (supprimerDossiersAssocies) {
                    // Supprimer les dossiers créés à partir de ce modèle
                    val deletedFolders = database.delete(Dossiers) {
                        (it.nomDossier eq modele.nomModele) and
                                (it.categorieId eq modele.categorieId) and
                                (it.estParDefault eq true)
                    }
                    println("🗑️ Suppression de $deletedFolders dossier(s) basé(s) sur le modèle '${modele.nomModele}'")
                }

                // Supprimer le modèle
                val deleted = deleteById(modeleId)

                if (deleted) {
                    RepositoryResult.Success(true)
                } else {
                    RepositoryResult.Error("Échec de la suppression du modèle")
                }
            } catch (e: Exception) {
                RepositoryResult.Error("Erreur lors de la suppression: ${e.message}")
            }
        } ?: RepositoryResult.Error("Erreur de transaction")
    }

    /**
     * Obtient le prochain ordre d'affichage pour une catégorie
     */
    private fun getNextOrderInCategory(categorieId: Int): Int {
        return try {
            val maxOrder = database.from(ModelesDossierDefaut)
                .select(max(ModelesDossierDefaut.ordreAffichage))
                .where { ModelesDossierDefaut.categorieId eq categorieId }
                .map { row -> row.getInt(1) ?: 0 }
                .firstOrNull() ?: 0

            maxOrder + 1
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Valide le nom d'un modèle
     */
    private fun validateTemplateName(nomModele: String): List<String> {
        val errors = mutableListOf<String>()

        if (nomModele.isBlank()) {
            errors.add("Le nom du modèle ne peut pas être vide")
        }

        if (nomModele.length < 2) {
            errors.add("Le nom du modèle doit contenir au moins 2 caractères")
        }

        if (nomModele.length > 100) {
            errors.add("Le nom du modèle ne peut pas dépasser 100 caractères")
        }

        return errors
    }

    override fun validate(entity: ModeleDossierDefautEntity): List<String> {
        return validateTemplateName(entity.nomModele)
    }
}
