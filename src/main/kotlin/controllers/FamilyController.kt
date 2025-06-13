package controllers

import repositories.FamilyRepository
import repositories.FamilyMemberRepository
import ktorm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Controller pour la gestion des familles
 *
 * Responsabilités:
 * - CRUD des familles
 * - Validation des données famille
 * - Gestion des relations famille-membres
 * - Statistiques et rapports famille
 * - Vérifications de sécurité et intégrité
 *
 * Utilisé par: UI Components, MainController
 * Utilise: FamilyRepository, FamilyMemberRepository
 *
 * Pattern: Controller Layer + Result Pattern (standardisé avec AuthController)
 */
class FamilyController(
    private val familyRepository: FamilyRepository,
    private val familyMemberRepository: FamilyMemberRepository
) {

    /**
     * Résultats des opérations sur les familles - PATTERN STANDARDISÉ
     */
    sealed class FamilyResult<out T> {
        data class Success<T>(val data: T) : FamilyResult<T>()
        data class Error(val message: String, val code: FamilyErrorCode) : FamilyResult<Nothing>()
    }

    enum class FamilyErrorCode {
        FAMILY_NOT_FOUND,
        FAMILY_ALREADY_EXISTS,
        INVALID_INPUT,
        HAS_MEMBERS,
        PERMISSION_DENIED,
        INTERNAL_ERROR
    }

    // ================================================================
    // MÉTHODES DE CRÉATION ET MODIFICATION
    // ================================================================

    /**
     * Crée une nouvelle famille
     *
     * @param familyName Nom de la famille
     * @param createdBy ID du membre qui crée la famille (optionnel)
     * @return Résultat de la création
     */
    suspend fun createFamily(
        familyName: String,
        createdBy: Int? = null
    ): FamilyResult<Famille> = withContext(Dispatchers.IO) {
        try {
            // Validation d'entrée
            val validationError = validateFamilyName(familyName)
            if (validationError != null) {
                return@withContext FamilyResult.Error(validationError, FamilyErrorCode.INVALID_INPUT)
            }

            // Vérifier si le nom existe déjà
            if (familyRepository.existsByName(familyName)) {
                return@withContext FamilyResult.Error(
                    "Une famille avec ce nom existe déjà",
                    FamilyErrorCode.FAMILY_ALREADY_EXISTS
                )
            }

            // Créer la famille
            val createdFamily = familyRepository.createFamille(familyName)
                ?: return@withContext FamilyResult.Error(
                    "Erreur lors de la création de la famille",
                    FamilyErrorCode.INTERNAL_ERROR
                )

            return@withContext FamilyResult.Success(createdFamily.toModel())

        } catch (e: Exception) {
            return@withContext FamilyResult.Error(
                "Erreur lors de la création: ${e.message}",
                FamilyErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Met à jour les informations d'une famille
     *
     * @param familyId ID de la famille
     * @param newFamilyName Nouveau nom de la famille
     * @return Résultat de la mise à jour
     */
    suspend fun updateFamily(
        familyId: Int,
        newFamilyName: String
    ): FamilyResult<Famille> = withContext(Dispatchers.IO) {
        try {
            // Validation d'entrée
            val validationError = validateFamilyName(newFamilyName)
            if (validationError != null) {
                return@withContext FamilyResult.Error(validationError, FamilyErrorCode.INVALID_INPUT)
            }

            // Vérifier que la famille existe
            val family = familyRepository.findById(familyId)
                ?: return@withContext FamilyResult.Error(
                    "Famille non trouvée",
                    FamilyErrorCode.FAMILY_NOT_FOUND
                )

            // Vérifier si le nouveau nom existe déjà (autre famille)
            val existingFamily = familyRepository.findByName(newFamilyName)
            if (existingFamily != null && existingFamily.familleId != familyId) {
                return@withContext FamilyResult.Error(
                    "Une autre famille avec ce nom existe déjà",
                    FamilyErrorCode.FAMILY_ALREADY_EXISTS
                )
            }

            // Mettre à jour
            family.nomFamille = newFamilyName
            familyRepository.update(family)

            return@withContext FamilyResult.Success(family.toModel())

        } catch (e: Exception) {
            return@withContext FamilyResult.Error(
                "Erreur lors de la mise à jour: ${e.message}",
                FamilyErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE CONSULTATION
    // ================================================================

    /**
     * Récupère une famille par son ID
     *
     * @param familyId ID de la famille
     * @return Résultat avec la famille trouvée
     */
    suspend fun getFamilyById(familyId: Int): FamilyResult<Famille> = withContext(Dispatchers.IO) {
        try {
            val family = familyRepository.findById(familyId)
                ?: return@withContext FamilyResult.Error(
                    "Famille non trouvée",
                    FamilyErrorCode.FAMILY_NOT_FOUND
                )

            return@withContext FamilyResult.Success(family.toModel())

        } catch (e: Exception) {
            return@withContext FamilyResult.Error(
                "Erreur lors de la récupération: ${e.message}",
                FamilyErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Récupère toutes les familles
     *
     * @return Résultat avec la liste des familles
     */
    suspend fun getAllFamilies(): FamilyResult<List<Famille>> = withContext(Dispatchers.IO) {
        try {
            val families = familyRepository.findAll()
            val familyModels = families.map { it.toModel() }

            return@withContext FamilyResult.Success(familyModels)

        } catch (e: Exception) {
            return@withContext FamilyResult.Error(
                "Erreur lors de la récupération des familles: ${e.message}",
                FamilyErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Recherche des familles par nom
     *
     * @param searchTerm Terme de recherche
     * @return Résultat avec les familles trouvées
     */
    suspend fun searchFamilies(searchTerm: String): FamilyResult<List<Famille>> = withContext(Dispatchers.IO) {
        try {
            if (searchTerm.isBlank()) {
                return@withContext FamilyResult.Error(
                    "Terme de recherche requis",
                    FamilyErrorCode.INVALID_INPUT
                )
            }

            val families = familyRepository.searchByName(searchTerm)
            val familyModels = families.map { it.toModel() }

            return@withContext FamilyResult.Success(familyModels)

        } catch (e: Exception) {
            return@withContext FamilyResult.Error(
                "Erreur lors de la recherche: ${e.message}",
                FamilyErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE SUPPRESSION
    // ================================================================

    /**
     * Supprime une famille
     * Vérifie qu'elle n'a plus de membres avant suppression
     *
     * @param familyId ID de la famille à supprimer
     * @return Résultat de la suppression
     */
    suspend fun deleteFamily(familyId: Int): FamilyResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la famille existe
            val family = familyRepository.findById(familyId)
                ?: return@withContext FamilyResult.Error(
                    "Famille non trouvée",
                    FamilyErrorCode.FAMILY_NOT_FOUND
                )

            // Vérifier qu'elle n'a plus de membres
            if (familyRepository.hasMembers(familyId)) {
                return@withContext FamilyResult.Error(
                    "Impossible de supprimer une famille qui a encore des membres",
                    FamilyErrorCode.HAS_MEMBERS
                )
            }

            // Supprimer
            val deleted = familyRepository.delete(familyId)
            if (deleted == 0) {
                return@withContext FamilyResult.Error(
                    "Aucune famille supprimée",
                    FamilyErrorCode.INTERNAL_ERROR
                )
            }

            return@withContext FamilyResult.Success(Unit)

        } catch (e: Exception) {
            return@withContext FamilyResult.Error(
                "Erreur lors de la suppression: ${e.message}",
                FamilyErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques d'une famille
     *
     * @param familyId ID de la famille
     * @return Résultat avec les statistiques
     */
    suspend fun getFamilyStatistics(familyId: Int): FamilyResult<FamilyStatistics> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que la famille existe
            val family = familyRepository.findById(familyId)
                ?: return@withContext FamilyResult.Error(
                    "Famille non trouvée",
                    FamilyErrorCode.FAMILY_NOT_FOUND
                )

            // Obtenir les détails complets
            val details = familyRepository.getFamilyDetails(familyId)
                ?: return@withContext FamilyResult.Error(
                    "Impossible d'obtenir les détails de la famille",
                    FamilyErrorCode.INTERNAL_ERROR
                )

            val stats = FamilyStatistics(
                familyId = familyId,
                familyName = details["familyName"] as String,
                memberCount = details["memberCount"] as Int,
                adminCount = details["adminCount"] as Int,
                responsibleCount = details["responsibleCount"] as Int,
                childrenCount = details["childrenCount"] as Int,
                creationDate = details["creationDate"] as LocalDateTime?,
                lastMemberAdded = details["lastMemberAdded"] as LocalDateTime?
            )

            return@withContext FamilyResult.Success(stats)

        } catch (e: Exception) {
            return@withContext FamilyResult.Error(
                "Erreur lors du calcul des statistiques: ${e.message}",
                FamilyErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Obtient les statistiques générales de toutes les familles
     *
     * @return Résultat avec les statistiques globales
     */
    suspend fun getGlobalStatistics(): FamilyResult<GlobalFamilyStatistics> = withContext(Dispatchers.IO) {
        try {
            val stats = familyRepository.getFamilyStatistics()

            val globalStats = GlobalFamilyStatistics(
                totalFamilies = stats["totalFamilies"] as Int,
                familiesCreatedLastMonth = stats["familiesCreatedLastMonth"] as Int,
                familiesCreatedLastWeek = stats["familiesCreatedLastWeek"] as Int,
                oldestFamilyDate = stats["oldestFamilyDate"] as LocalDateTime?,
                newestFamilyDate = stats["newestFamilyDate"] as LocalDateTime?
            )

            return@withContext FamilyResult.Success(globalStats)

        } catch (e: Exception) {
            return@withContext FamilyResult.Error(
                "Erreur lors du calcul des statistiques globales: ${e.message}",
                FamilyErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES PRIVÉES DE VALIDATION
    // ================================================================

    private fun validateFamilyName(familyName: String): String? {
        return when {
            familyName.isBlank() -> "Le nom de famille ne peut pas être vide"
            familyName.length < 2 -> "Le nom de famille doit contenir au moins 2 caractères"
            familyName.length > 100 -> "Le nom de famille ne peut pas dépasser 100 caractères"
            !familyName.matches(Regex("^[a-zA-ZÀ-ÿ0-9\\s\\-']+$")) ->
                "Le nom de famille contient des caractères non autorisés"
            else -> null
        }
    }
}

// ================================================================
// DATA CLASSES POUR LES RÉSULTATS
// ================================================================

/**
 * Statistiques d'une famille spécifique
 */
data class FamilyStatistics(
    val familyId: Int,
    val familyName: String,
    val memberCount: Int,
    val adminCount: Int,
    val responsibleCount: Int,
    val childrenCount: Int,
    val creationDate: LocalDateTime?,
    val lastMemberAdded: LocalDateTime?
)

/**
 * Statistiques globales de toutes les familles
 */
data class GlobalFamilyStatistics(
    val totalFamilies: Int,
    val familiesCreatedLastMonth: Int,
    val familiesCreatedLastWeek: Int,
    val oldestFamilyDate: LocalDateTime?,
    val newestFamilyDate: LocalDateTime?
)