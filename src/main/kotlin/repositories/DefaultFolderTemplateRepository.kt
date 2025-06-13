package repositories

import org.ktorm.entity.sequenceOf
import org.ktorm.entity.find
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.ktorm.dsl.*
import org.ktorm.schema.Column
import ktorm.*

/**
 * Repository pour la gestion des modèles de dossiers par défaut
 *
 * Responsabilités:
 * - CRUD des modèles de dossiers par défaut
 * - Gestion des modèles par catégorie
 * - Organisation et réorganisation des modèles
 * - Création automatique de dossiers à partir des modèles
 *
 * Utilisé par: CategoryController, FolderController, FamilyMemberController
 */
class DefaultFolderTemplateRepository : BaseRepository<ModeleDossierDefautEntity, ModelesDossierDefaut>() {

    override val table = ModelesDossierDefaut

    /**
     * Obtient la clé primaire d'un modèle
     */
    override fun ModeleDossierDefautEntity.getPrimaryKey(): Int = this.modeleId
    override fun getPrimaryKeyColumn(): Column<Int> = ModelesDossierDefaut.modeleId

    /**
     * Met à jour un modèle de dossier par défaut
     */
    override fun update(entity: ModeleDossierDefautEntity): Int {
        return ArkaDatabase.instance.update(ModelesDossierDefaut) {
            set(it.nomModele, entity.nomModele)
            set(it.ordreAffichage, entity.ordreAffichage)
            where { it.modeleId eq entity.modeleId }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR CATÉGORIE
    // ================================================================

    /**
     * Récupère tous les modèles d'une catégorie triés par ordre d'affichage
     *
     * @param categorieId ID de la catégorie
     * @return Liste des modèles ordonnés
     */
    fun getModelesByCategorie(categorieId: Int): List<ModeleDossierDefautEntity> {
        return ArkaDatabase.instance.sequenceOf(ModelesDossierDefaut)
            .filter { it.categorieId eq categorieId }
            .toList()
            .sortedBy { it.ordreAffichage }
    }

    /**
     * Trouve un modèle par nom dans une catégorie
     *
     * @param nomModele Le nom du modèle
     * @param categorieId ID de la catégorie
     * @return Le modèle trouvé ou null
     */
    fun getModeleByNameAndCategorie(nomModele: String, categorieId: Int): ModeleDossierDefautEntity? {
        return ArkaDatabase.instance.sequenceOf(ModelesDossierDefaut)
            .find {
                (it.nomModele eq nomModele.trim()) and (it.categorieId eq categorieId)
            }
    }

    /**
     * Compte le nombre de modèles dans une catégorie
     *
     * @param categorieId ID de la catégorie
     * @return Nombre de modèles
     */
    fun countModelesByCategorie(categorieId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(ModelesDossierDefaut)
            .filter { it.categorieId eq categorieId }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE VALIDATION ET VÉRIFICATION
    // ================================================================

    /**
     * Vérifie si un modèle avec ce nom existe dans une catégorie
     *
     * @param nomModele Le nom du modèle
     * @param categorieId ID de la catégorie
     * @param excludeId ID à exclure de la vérification (pour les mises à jour)
     * @return true si le nom existe déjà
     */
    fun existsByNameInCategorie(nomModele: String, categorieId: Int, excludeId: Int? = null): Boolean {
        return ArkaDatabase.instance.sequenceOf(ModelesDossierDefaut)
            .filter { modele ->
                val nameCondition = (modele.nomModele eq nomModele.trim()) and (modele.categorieId eq categorieId)
                if (excludeId != null) {
                    nameCondition and (modele.modeleId neq excludeId)
                } else {
                    nameCondition
                }
            }
            .toList()
            .isNotEmpty()
    }

    // ================================================================
    // MÉTHODES DE GESTION DE L'ORDRE D'AFFICHAGE
    // ================================================================

    /**
     * Obtient le prochain ordre d'affichage pour une catégorie
     *
     * @param categorieId ID de la catégorie
     * @return Le prochain ordre d'affichage disponible
     */
    fun getNextOrdreAffichage(categorieId: Int): Int {
        val modeles = getModelesByCategorie(categorieId)
        val maxOrdre = modeles.maxByOrNull { it.ordreAffichage }?.ordreAffichage ?: 0
        return maxOrdre + 1
    }

    /**
     * Met à jour l'ordre d'affichage d'un modèle
     *
     * @param modeleId ID du modèle
     * @param nouvelOrdre Nouvel ordre d'affichage
     * @return true si la mise à jour a réussi
     */
    fun updateOrdreAffichage(modeleId: Int, nouvelOrdre: Int): Boolean {
        return try {
            val rowsAffected = ArkaDatabase.instance.update(ModelesDossierDefaut) {
                set(it.ordreAffichage, nouvelOrdre)
                where { it.modeleId eq modeleId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour de l'ordre du modèle $modeleId: ${e.message}")
            false
        }
    }

    /**
     * Réorganise l'ordre des modèles d'une catégorie
     *
     * @param categorieId ID de la catégorie
     * @param modelesOrdonnes Liste des IDs de modèles dans le nouvel ordre
     * @return true si la réorganisation a réussi
     */
    fun reorderModeles(categorieId: Int, modelesOrdonnes: List<Int>): Boolean {
        return try {
            transaction {
                modelesOrdonnes.forEachIndexed { index, modeleId ->
                    ArkaDatabase.instance.update(ModelesDossierDefaut) {
                        set(it.ordreAffichage, index + 1)
                        where {
                            (it.modeleId eq modeleId) and (it.categorieId eq categorieId)
                        }
                    }
                }
                true
            }
        } catch (e: Exception) {
            println("Erreur lors de la réorganisation des modèles: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET MISE À JOUR
    // ================================================================

    /**
     * Crée un nouveau modèle de dossier par défaut avec validation
     *
     * @param nomModele Nom du modèle
     * @param categorieId ID de la catégorie
     * @param ordreAffichage Ordre d'affichage (optionnel, auto-calculé si null)
     * @return Le modèle créé ou null en cas d'erreur
     */
    fun createModele(
        nomModele: String,
        categorieId: Int,
        ordreAffichage: Int? = null
    ): ModeleDossierDefautEntity? {
        // Validation du nom
        if (nomModele.isBlank() || nomModele.length < 2 || nomModele.length > 100) {
            println("Nom de modèle invalide: '$nomModele'")
            return null
        }

        // Vérifier l'unicité dans la catégorie
        if (existsByNameInCategorie(nomModele, categorieId)) {
            println("Un modèle avec ce nom existe déjà dans cette catégorie")
            return null
        }

        return try {
            val ordre = ordreAffichage ?: getNextOrdreAffichage(categorieId)

            val modele = ModeleDossierDefautEntity {
                this.nomModele = nomModele.trim()
                this.categorieId = categorieId
                this.ordreAffichage = ordre
            }

            create(modele)
        } catch (e: Exception) {
            println("Erreur lors de la création du modèle: ${e.message}")
            null
        }
    }

    /**
     * Met à jour un modèle avec validation
     *
     * @param modeleId ID du modèle
     * @param nouveauNom Nouveau nom
     * @param nouvelOrdre Nouvel ordre (optionnel)
     * @return true si la mise à jour a réussi
     */
    fun updateModele(modeleId: Int, nouveauNom: String, nouvelOrdre: Int? = null): Boolean {
        val modele = findById(modeleId) ?: return false

        // Validation du nom
        if (nouveauNom.isBlank() || nouveauNom.length < 2 || nouveauNom.length > 100) {
            return false
        }

        // Vérifier l'unicité (sauf pour le modèle actuel)
        if (existsByNameInCategorie(nouveauNom, modele.categorieId, modeleId)) {
            return false
        }

        return try {
            val rowsAffected = ArkaDatabase.instance.update(ModelesDossierDefaut) {
                set(it.nomModele, nouveauNom.trim())
                nouvelOrdre?.let { ordre -> set(it.ordreAffichage, ordre) }
                where { it.modeleId eq modeleId }
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour du modèle $modeleId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE CRÉATION DE DOSSIERS AUTOMATIQUE
    // ================================================================

    /**
     * Crée les dossiers par défaut pour un membre dans une catégorie
     * Utilise les modèles de la catégorie pour créer les dossiers
     *
     * @param membreFamilleId ID du membre
     * @param categorieId ID de la catégorie
     * @return Nombre de dossiers créés avec succès
     */
    fun createDefaultFoldersForMember(membreFamilleId: Int, categorieId: Int): Int {
        val modeles = getModelesByCategorie(categorieId)
        var createdCount = 0

        modeles.forEach { modele ->
            try {
                val rowsAffected = ArkaDatabase.instance.insert(Dossiers) {
                    set(it.nomDossier, modele.nomModele)
                    set(it.membreFamilleId, membreFamilleId)
                    set(it.categorieId, categorieId)
                    set(it.estParDefault, true)
                    set(it.dateCreationDossier, java.time.LocalDateTime.now())
                    set(it.dateDerniereModifDossier, java.time.LocalDateTime.now())
                }

                if (rowsAffected > 0) {
                    createdCount++
                }
            } catch (e: Exception) {
                println("Erreur lors de la création du dossier '${modele.nomModele}': ${e.message}")
            }
        }

        return createdCount
    }

    /**
     * Crée un dossier unique à partir d'un modèle
     *
     * @param modeleId ID du modèle
     * @param membreFamilleId ID du membre propriétaire
     * @return true si le dossier a été créé
     */
    fun createFolderFromTemplate(modeleId: Int, membreFamilleId: Int): Boolean {
        val modele = findById(modeleId) ?: return false

        return try {
            val rowsAffected = ArkaDatabase.instance.insert(Dossiers) {
                set(it.nomDossier, modele.nomModele)
                set(it.membreFamilleId, membreFamilleId)
                set(it.categorieId, modele.categorieId)
                set(it.estParDefault, true)
                set(it.dateCreationDossier, java.time.LocalDateTime.now())
                set(it.dateDerniereModifDossier, java.time.LocalDateTime.now())
            }
            rowsAffected > 0
        } catch (e: Exception) {
            println("Erreur lors de la création du dossier à partir du modèle $modeleId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION
    // ================================================================

    /**
     * Supprime tous les modèles d'une catégorie
     * Utilisé lors de la suppression d'une catégorie
     *
     * @param categorieId ID de la catégorie
     * @return Nombre de modèles supprimés
     */
    fun deleteAllByCategorie(categorieId: Int): Int {
        return try {
            ArkaDatabase.instance.delete(ModelesDossierDefaut) {
                it.categorieId eq categorieId
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression des modèles de la catégorie $categorieId: ${e.message}")
            0
        }
    }

    /**
     * Supprime un modèle et optionnellement les dossiers associés
     *
     * @param modeleId ID du modèle
     * @param supprimerDossiersAssocies Si true, supprime aussi les dossiers créés à partir de ce modèle
     * @return true si la suppression a réussi
     */
    fun deleteModeleWithOptions(modeleId: Int, supprimerDossiersAssocies: Boolean = false): Boolean {
        return try {
            transaction {
                val modele = findById(modeleId) ?: return@transaction false

                if (supprimerDossiersAssocies) {
                    // Supprimer les dossiers créés à partir de ce modèle
                    val deletedFolders = ArkaDatabase.instance.delete(Dossiers) {
                        (it.nomDossier eq modele.nomModele) and
                                (it.categorieId eq modele.categorieId) and
                                (it.estParDefault eq true)
                    }
                    println("Suppression de $deletedFolders dossier(s) basé(s) sur le modèle '${modele.nomModele}'")
                }

                // Supprimer le modèle
                val rowsAffected = delete(modeleId)
                rowsAffected > 0
            }
        } catch (e: Exception) {
            println("Erreur lors de la suppression du modèle $modeleId: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES DE DUPLICATION ET TRANSFERT
    // ================================================================

    /**
     * Duplique les modèles d'une catégorie vers une autre
     *
     * @param sourceCategorieId ID de la catégorie source
     * @param targetCategorieId ID de la catégorie cible
     * @return Nombre de modèles dupliqués
     */
    fun duplicateModelesToCategorie(sourceCategorieId: Int, targetCategorieId: Int): Int {
        val sourceModeles = getModelesByCategorie(sourceCategorieId)
        var duplicatedCount = 0

        sourceModeles.forEach { sourceModele ->
            // Vérifier que le nom n'existe pas déjà dans la catégorie cible
            if (!existsByNameInCategorie(sourceModele.nomModele, targetCategorieId)) {
                val nouvelOrdre = getNextOrdreAffichage(targetCategorieId)
                val nouveauModele = createModele(sourceModele.nomModele, targetCategorieId, nouvelOrdre)
                if (nouveauModele != null) {
                    duplicatedCount++
                }
            }
        }

        return duplicatedCount
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE AVANCÉE
    // ================================================================

    /**
     * Récupère tous les modèles avec leur catégorie associée
     *
     * @return Liste de paires (modèle, catégorie)
     */
    fun getAllModelesWithCategorie(): List<Pair<ModeleDossierDefautEntity, CategorieEntity>> {
        val modeles = findAll()
        val categories = ArkaDatabase.instance.sequenceOf(Categories).toList()
        val categoriesMap = categories.associateBy { it.categorieId }

        return modeles.mapNotNull { modele ->
            val categorie = categoriesMap[modele.categorieId]
            if (categorie != null) {
                Pair(modele, categorie)
            } else {
                null
            }
        }.sortedWith(compareBy({ it.second.nomCategorie }, { it.first.ordreAffichage }))
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Valide le nom d'un modèle
     *
     * @param nomModele Le nom à valider
     * @return Liste des erreurs de validation
     */
    fun validateNomModele(nomModele: String): List<String> {
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
}