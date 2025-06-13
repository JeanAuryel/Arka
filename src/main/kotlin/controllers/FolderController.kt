package controllers

import ktorm.*
import repositories.*

/**
 * Controller pour la gestion des dossiers avec système de permissions
 * Orchestre les opérations métier complexes sur les dossiers
 */
class FolderController(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository,
    private val permissionRepository: PermissionRepository,
    private val templateRepository: DefaultFolderTemplateRepository
) {

    /**
     * Crée un nouveau dossier avec vérifications de permissions
     * @param folderData Les données du dossier
     * @param creatorUserId L'ID de l'utilisateur qui crée
     * @return Le résultat de la création
     */
    fun createFolder(folderData: CreateFolderData, creatorUserId: Int): FolderCreateResult {
        return try {
            // 1. Vérifier les permissions sur le dossier parent ou la catégorie
            if (folderData.dossierParentId != null) {
                if (!hasWritePermissionOnFolder(creatorUserId, folderData.dossierParentId)) {
                    return FolderCreateResult.Error("Permissions insuffisantes pour créer un dossier ici")
                }
            } else {
                if (!hasWritePermissionOnCategory(creatorUserId, folderData.categorieId)) {
                    return FolderCreateResult.Error("Permissions insuffisantes pour créer un dossier dans cette catégorie")
                }
            }

            // 2. Créer le dossier
            val result = folderRepository.createFolder(
                nomDossier = folderData.nomDossier,
                membreId = folderData.membreId,
                categorieId = folderData.categorieId,
                dossierParentId = folderData.dossierParentId,
                estParDefault = false
            )

            when (result) {
                is RepositoryResult.Success -> {
                    FolderCreateResult.Success(
                        dossier = result.data,
                        message = "Dossier créé avec succès"
                    )
                }
                is RepositoryResult.Error -> FolderCreateResult.Error(result.message)
                is RepositoryResult.ValidationError -> FolderCreateResult.ValidationError(result.errors)
            }
        } catch (e: Exception) {
            FolderCreateResult.Error("Erreur lors de la création: ${e.message}")
        }
    }

    /**
     * Obtient l'arborescence des dossiers accessibles par un utilisateur
     * @param userId L'ID de l'utilisateur
     * @param categorieId L'ID de la catégorie
     * @param membreId Optionnel: limiter aux dossiers d'un membre
     * @return L'arborescence des dossiers
     */
    fun getFolderTree(userId: Int, categorieId: Int, membreId: Int? = null): FolderTreeResult {
        return try {
            // 1. Récupérer les dossiers racine
            val rootFolders = if (membreId != null) {
                folderRepository.findRootFolders(membreId, categorieId)
            } else {
                folderRepository.findByCategory(categorieId)
                    .filter { it.dossierParentId == null }
            }

            // 2. Filtrer selon les permissions et construire l'arbre
            val accessibleTree = rootFolders.mapNotNull { folder ->
                if (hasReadPermissionOnFolder(userId, folder.dossierId)) {
                    buildFolderNode(folder, userId)
                } else null
            }

            FolderTreeResult.Success(accessibleTree)
        } catch (e: Exception) {
            FolderTreeResult.Error("Erreur lors de la récupération de l'arborescence: ${e.message}")
        }
    }

    /**
     * Déplace un dossier vers un nouveau parent
     * @param dossierId L'ID du dossier à déplacer
     * @param nouveauParentId L'ID du nouveau parent (null pour racine)
     * @param moverUserId L'ID de l'utilisateur qui déplace
     * @return Le résultat du déplacement
     */
    fun moveFolder(dossierId: Int, nouveauParentId: Int?, moverUserId: Int): FolderMoveResult {
        return try {
            // 1. Vérifier que le dossier existe
            val dossier = folderRepository.findById(dossierId)
            if (dossier == null) {
                return FolderMoveResult.Error("Dossier non trouvé")
            }

            // 2. Vérifier les permissions sur le dossier source
            if (!hasWritePermissionOnFolder(moverUserId, dossierId)) {
                return FolderMoveResult.Error("Permissions insuffisantes sur le dossier source")
            }

            // 3. Vérifier les permissions sur la destination
            if (nouveauParentId != null) {
                if (!hasWritePermissionOnFolder(moverUserId, nouveauParentId)) {
                    return FolderMoveResult.Error("Permissions insuffisantes sur le dossier de destination")
                }
            } else {
                if (!hasWritePermissionOnCategory(moverUserId, dossier.categorieId)) {
                    return FolderMoveResult.Error("Permissions insuffisantes pour déplacer à la racine")
                }
            }

            // 4. Déplacer le dossier
            val result = folderRepository.moveFolder(dossierId, nouveauParentId)

            when (result) {
                is RepositoryResult.Success -> {
                    FolderMoveResult.Success(
                        dossier = result.data,
                        message = "Dossier déplacé avec succès"
                    )
                }
                is RepositoryResult.Error -> FolderMoveResult.Error(result.message)
                is RepositoryResult.ValidationError -> FolderMoveResult.Error(result.errors.joinToString(", "))
            }
        } catch (e: Exception) {
            FolderMoveResult.Error("Erreur lors du déplacement: ${e.message}")
        }
    }

    /**
     * Supprime un dossier avec tout son contenu
     * @param dossierId L'ID du dossier à supprimer
     * @param deleterUserId L'ID de l'utilisateur qui supprime
     * @return Le résultat de la suppression
     */
    fun deleteFolder(dossierId: Int, deleterUserId: Int): FolderDeleteResult {
        return try {
            // 1. Vérifier que le dossier existe
            val dossier = folderRepository.findById(dossierId)
            if (dossier == null) {
                return FolderDeleteResult.Error("Dossier non trouvé")
            }

            // 2. Vérifier les permissions de suppression
            if (!hasDeletePermissionOnFolder(deleterUserId, dossierId, dossier.membreFamilleId)) {
                return FolderDeleteResult.Error("Permissions insuffisantes pour supprimer ce dossier")
            }

            // 3. Calculer les statistiques avant suppression
            val stats = calculateDeletionImpact(dossierId)

            // 4. Supprimer le dossier (CASCADE gère les sous-dossiers et fichiers)
            val result = folderRepository.deleteFolderWithContent(dossierId)

            when (result) {
                is RepositoryResult.Success -> {
                    FolderDeleteResult.Success(
                        deletionStats = stats,
                        message = "Dossier supprimé avec succès"
                    )
                }
                is RepositoryResult.Error -> FolderDeleteResult.Error(result.message)
                is RepositoryResult.ValidationError -> FolderDeleteResult.Error(result.errors.joinToString(", "))
            }
        } catch (e: Exception) {
            FolderDeleteResult.Error("Erreur lors de la suppression: ${e.message}")
        }
    }

    /**
     * Crée les dossiers par défaut pour un nouveau membre dans une catégorie
     * @param membreId L'ID du membre
     * @param categorieId L'ID de la catégorie
     * @param creatorUserId L'ID de l'utilisateur qui initie (pour vérifier permissions)
     * @return Le résultat de la création
     */
    fun createDefaultFolders(membreId: Int, categorieId: Int, creatorUserId: Int): DefaultFoldersResult {
        return try {
            // 1. Vérifier les permissions (seuls admin/responsable peuvent créer pour autrui)
            if (membreId != creatorUserId && !isAdminOrResponsible(creatorUserId)) {
                return DefaultFoldersResult.Error("Permissions insuffisantes pour créer des dossiers pour ce membre")
            }

            // 2. Créer les dossiers par défaut
            val createdCount = templateRepository.createDefaultFoldersForMember(membreId, categorieId)

            if (createdCount > 0) {
                DefaultFoldersResult.Success(
                    createdCount = createdCount,
                    message = "$createdCount dossier(s) par défaut créé(s)"
                )
            } else {
                DefaultFoldersResult.Error("Aucun modèle de dossier par défaut trouvé pour cette catégorie")
            }
        } catch (e: Exception) {
            DefaultFoldersResult.Error("Erreur lors de la création des dossiers par défaut: ${e.message}")
        }
    }

    /**
     * Partage un dossier avec un autre utilisateur
     * @param dossierId L'ID du dossier
     * @param beneficiaireId L'ID du bénéficiaire
     * @param sharerUserId L'ID de l'utilisateur qui partage
     * @param typePermission Le type de permission à accorder
     * @param includeSubfolders Si true, inclut les sous-dossiers
     * @return Le résultat du partage
     */
    fun shareFolder(
        dossierId: Int,
        beneficiaireId: Int,
        sharerUserId: Int,
        typePermission: TypePermission,
        includeSubfolders: Boolean = false
    ): FolderShareResult {
        return try {
            // 1. Vérifier que le dossier existe
            val dossier = folderRepository.findById(dossierId)
            if (dossier == null) {
                return FolderShareResult.Error("Dossier non trouvé")
            }

            // 2. Vérifier les permissions de partage
            if (!canShareFolder(sharerUserId, dossier.membreFamilleId, dossierId)) {
                return FolderShareResult.Error("Permissions insuffisantes pour partager ce dossier")
            }

            // 3. Créer la permission principale
            val permissionData = CreatePermissionData(
                proprietaireId = dossier.membreFamilleId,
                beneficiaireId = beneficiaireId,
                portee = PorteePermission.DOSSIER,
                cibleId = dossierId,
                typePermission = typePermission,
                dateExpiration = null
            )

            val result = permissionRepository.createPermission(permissionData)

            when (result) {
                is RepositoryResult.Success -> {
                    var sharedFolders = 1

                    // 4. Partager les sous-dossiers si demandé
                    if (includeSubfolders) {
                        sharedFolders += shareSubfoldersRecursively(dossierId, beneficiaireId, dossier.membreFamilleId, typePermission)
                    }

                    FolderShareResult.Success(
                        permission = result.data,
                        sharedFoldersCount = sharedFolders,
                        message = if (includeSubfolders) {
                            "Dossier et $sharedFolders sous-dossier(s) partagé(s)"
                        } else {
                            "Dossier partagé avec succès"
                        }
                    )
                }
                is RepositoryResult.Error -> FolderShareResult.Error(result.message)
                is RepositoryResult.ValidationError -> FolderShareResult.Error(result.errors.joinToString(", "))
            }
        } catch (e: Exception) {
            FolderShareResult.Error("Erreur lors du partage: ${e.message}")
        }
    }

    /**
     * Obtient les statistiques détaillées d'un dossier
     * @param dossierId L'ID du dossier
     * @param userId L'ID de l'utilisateur qui demande
     * @return Les statistiques du dossier
     */
    fun getFolderStatistics(dossierId: Int, userId: Int): FolderStatisticsResult {
        return try {
            // 1. Vérifier les permissions de lecture
            if (!hasReadPermissionOnFolder(userId, dossierId)) {
                return FolderStatisticsResult.Error("Permissions insuffisantes pour consulter ce dossier")
            }

            // 2. Calculer les statistiques
            val folderStats = folderRepository.getFolderStats(dossierId)
            if (folderStats == null) {
                return FolderStatisticsResult.Error("Dossier non trouvé")
            }

            // 3. Calculer les statistiques étendues
            val extendedStats = calculateExtendedStats(dossierId, userId)

            FolderStatisticsResult.Success(
                folderStats = folderStats,
                extendedStats = extendedStats
            )
        } catch (e: Exception) {
            FolderStatisticsResult.Error("Erreur lors du calcul des statistiques: ${e.message}")
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES - VÉRIFICATIONS DE PERMISSIONS
    // ================================================================

    /**
     * Vérifie si un utilisateur a les permissions de lecture sur un dossier
     */
    private fun hasReadPermissionOnFolder(userId: Int, dossierId: Int): Boolean {
        return try {
            val dossier = folderRepository.findById(dossierId)
            if (dossier?.membreFamilleId == userId) return true

            permissionRepository.hasPermission(
                beneficiaireId = userId,
                portee = PorteePermission.DOSSIER,
                cibleId = dossierId,
                typePermission = TypePermission.LECTURE
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un utilisateur a les permissions d'écriture sur un dossier
     */
    private fun hasWritePermissionOnFolder(userId: Int, dossierId: Int): Boolean {
        return try {
            val dossier = folderRepository.findById(dossierId)
            if (dossier?.membreFamilleId == userId) return true

            permissionRepository.hasPermission(
                beneficiaireId = userId,
                portee = PorteePermission.DOSSIER,
                cibleId = dossierId,
                typePermission = TypePermission.ECRITURE
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un utilisateur a les permissions d'écriture sur une catégorie
     */
    private fun hasWritePermissionOnCategory(userId: Int, categorieId: Int): Boolean {
        return try {
            permissionRepository.hasPermission(
                beneficiaireId = userId,
                portee = PorteePermission.CATEGORIE,
                cibleId = categorieId,
                typePermission = TypePermission.ECRITURE
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un utilisateur peut supprimer un dossier
     */
    private fun hasDeletePermissionOnFolder(userId: Int, dossierId: Int, folderOwnerId: Int): Boolean {
        return try {
            if (folderOwnerId == userId) return true

            permissionRepository.hasPermission(
                beneficiaireId = userId,
                portee = PorteePermission.DOSSIER,
                cibleId = dossierId,
                typePermission = TypePermission.SUPPRESSION
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un utilisateur peut partager un dossier
     */
    private fun canShareFolder(userId: Int, folderOwnerId: Int, dossierId: Int): Boolean {
        return try {
            if (folderOwnerId == userId) return true

            permissionRepository.hasPermission(
                beneficiaireId = userId,
                portee = PorteePermission.DOSSIER,
                cibleId = dossierId,
                typePermission = TypePermission.ACCES_COMPLET
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Construit récursivement un nœud de l'arbre des dossiers
     */
    private fun buildFolderNode(folder: DossierEntity, userId: Int): FolderTreeNode {
        val subFolders = folderRepository.findSubFolders(folder.dossierId)
            .filter { hasReadPermissionOnFolder(userId, it.dossierId) }
            .map { buildFolderNode(it, userId) }

        val fileCount = fileRepository.getFileCount(folder.dossierId)
        val permissions = getFolderUserPermissions(userId, folder.dossierId)

        return FolderTreeNode(
            dossier = folder.toModel(),
            subFolders = subFolders,
            fileCount = fileCount,
            permissions = permissions
        )
    }

    /**
     * Obtient les permissions d'un utilisateur sur un dossier
     */
    private fun getFolderUserPermissions(userId: Int, dossierId: Int): FolderPermissions {
        return FolderPermissions(
            canRead = hasReadPermissionOnFolder(userId, dossierId),
            canWrite = hasWritePermissionOnFolder(userId, dossierId),
            canDelete = hasDeletePermissionOnFolder(userId, dossierId, userId),
            canShare = canShareFolder(userId, userId, dossierId)
        )
    }

    /**
     * Partage récursivement les sous-dossiers
     */
    private fun shareSubfoldersRecursively(
        dossierId: Int,
        beneficiaireId: Int,
        proprietaireId: Int,
        typePermission: TypePermission
    ): Int {
        return try {
            val subFolders = folderRepository.findSubFolders(dossierId)
            var sharedCount = 0

            for (subFolder in subFolders) {
                try {
                    val permissionData = CreatePermissionData(
                        proprietaireId = proprietaireId,
                        beneficiaireId = beneficiaireId,
                        portee = PorteePermission.DOSSIER,
                        cibleId = subFolder.dossierId,
                        typePermission = typePermission,
                        dateExpiration = null
                    )

                    val result = permissionRepository.createPermission(permissionData)
                    if (result.isSuccess) {
                        sharedCount++
                        sharedCount += shareSubfoldersRecursively(subFolder.dossierId, beneficiaireId, proprietaireId, typePermission)
                    }
                } catch (e: Exception) {
                    println("⚠️ Erreur partage sous-dossier ${subFolder.dossierId}: ${e.message}")
                }
            }

            sharedCount
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Calcule l'impact d'une suppression de dossier
     */
    private fun calculateDeletionImpact(dossierId: Int): FolderDeletionImpact {
        return try {
            val stats = folderRepository.getFolderStats(dossierId)
            val subFolderCount = countSubFoldersRecursively(dossierId)
            val totalFileCount = countFilesRecursively(dossierId)
            val totalSize = calculateTotalSizeRecursively(dossierId)

            FolderDeletionImpact(
                foldersDeleted = 1 + subFolderCount,
                filesDeleted = totalFileCount,
                totalSizeFreed = totalSize
            )
        } catch (e: Exception) {
            FolderDeletionImpact(0, 0, 0L)
        }
    }

    /**
     * Calcule les statistiques étendues d'un dossier
     */
    private fun calculateExtendedStats(dossierId: Int, userId: Int): ExtendedFolderStats {
        return try {
            val permissions = permissionRepository.findByTarget(PorteePermission.DOSSIER, dossierId)
            val sharedWithCount = permissions.count { it.estActive }
            val recentFiles = fileRepository.findByFolder(dossierId).take(5)

            ExtendedFolderStats(
                sharedWithUsers = sharedWithCount,
                hasActivePermissions = permissions.isNotEmpty(),
                recentFiles = recentFiles.map { it.toModel() }
            )
        } catch (e: Exception) {
            ExtendedFolderStats(0, false, emptyList())
        }
    }

    /**
     * Compte récursivement les sous-dossiers
     */
    private fun countSubFoldersRecursively(dossierId: Int): Int {
        return try {
            val subFolders = folderRepository.findSubFolders(dossierId)
            var count = subFolders.size

            for (subFolder in subFolders) {
                count += countSubFoldersRecursively(subFolder.dossierId)
            }

            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Compte récursivement les fichiers
     */
    private fun countFilesRecursively(dossierId: Int): Int {
        return try {
            var count = fileRepository.getFileCount(dossierId)
            val subFolders = folderRepository.findSubFolders(dossierId)

            for (subFolder in subFolders) {
                count += countFilesRecursively(subFolder.dossierId)
            }

            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Calcule récursivement la taille totale
     */
    private fun calculateTotalSizeRecursively(dossierId: Int): Long {
        return try {
            var size = folderRepository.getTotalSize(dossierId)
            val subFolders = folderRepository.findSubFolders(dossierId)

            for (subFolder in subFolders) {
                size += calculateTotalSizeRecursively(subFolder.dossierId)
            }

            size
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Vérifie si un utilisateur est admin ou responsable
     */
    private fun isAdminOrResponsible(userId: Int): Boolean {
        // TODO: Implémenter la vérification des rôles
        // Cette méthode devrait vérifier dans la base si l'utilisateur est admin ou responsable
        return false
    }
}

// ================================================================
// CLASSES DE DONNÉES POUR LE CONTROLLER
// ================================================================

/**
 * Données pour créer un dossier
 */
data class CreateFolderData(
    val nomDossier: String,
    val membreId: Int,
    val categorieId: Int,
    val dossierParentId: Int? = null
)

/**
 * Nœud de l'arbre des dossiers
 */
data class FolderTreeNode(
    val dossier: Dossier,
    val subFolders: List<FolderTreeNode>,
    val fileCount: Int,
    val permissions: FolderPermissions
)

/**
 * Permissions sur un dossier
 */
data class FolderPermissions(
    val canRead: Boolean,
    val canWrite: Boolean,
    val canDelete: Boolean,
    val canShare: Boolean
)

/**
 * Impact d'une suppression de dossier
 */
data class FolderDeletionImpact(
    val foldersDeleted: Int,
    val filesDeleted: Int,
    val totalSizeFreed: Long
)

/**
 * Statistiques étendues d'un dossier
 */
data class ExtendedFolderStats(
    val sharedWithUsers: Int,
    val hasActivePermissions: Boolean,
    val recentFiles: List<Fichier>
)

// ================================================================
// RÉSULTATS D'OPÉRATIONS
// ================================================================

sealed class FolderCreateResult {
    data class Success(val dossier: Dossier, val message: String) : FolderCreateResult()
    data class Error(val message: String) : FolderCreateResult()
    data class ValidationError(val errors: List<String>) : FolderCreateResult()
}

sealed class FolderTreeResult {
    data class Success(val tree: List<FolderTreeNode>) : FolderTreeResult()
    data class Error(val message: String) : FolderTreeResult()
}

sealed class FolderMoveResult {
    data class Success(val dossier: Dossier, val message: String) : FolderMoveResult()
    data class Error(val message: String) : FolderMoveResult()
}

sealed class FolderDeleteResult {
    data class Success(val deletionStats: FolderDeletionImpact, val message: String) : FolderDeleteResult()
    data class Error(val message: String) : FolderDeleteResult()
}

sealed class DefaultFoldersResult {
    data class Success(val createdCount: Int, val message: String) : DefaultFoldersResult()
    data class Error(val message: String) : DefaultFoldersResult()
}

sealed class FolderShareResult {
    data class Success(val permission: PermissionActive, val sharedFoldersCount: Int, val message: String) : FolderShareResult()
    data class Error(val message: String) : FolderShareResult()
}

sealed class FolderStatisticsResult {
    data class Success(val folderStats: FolderStats, val extendedStats: ExtendedFolderStats) : FolderStatisticsResult()
    data class Error(val message: String) : FolderStatisticsResult()
}