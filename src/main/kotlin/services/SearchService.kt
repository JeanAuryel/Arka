package services

import controllers.FileController
import controllers.FolderController
import ktorm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import java.time.LocalDateTime

/**
 * Service de recherche globale
 *
 * Responsabilités:
 * - Recherche unifiée à travers toutes les entités de l'application
 * - Indexation et optimisation des recherches
 * - Filtrage et tri des résultats
 * - Historique des recherches utilisateur
 * - Suggestions et auto-complétion
 * - Recherche avancée avec critères multiples
 *
 * Utilisé par: MainController, UI Components de recherche
 * Utilise: FileController, FolderController, autres controllers selon besoins
 *
 * Pattern: Search Engine Pattern + Facade
 */
class SearchService(
    // TODO: Ajouter d'autres controllers quand ils seront créés
    // private val fileController: FileController,
    // private val folderController: FolderController,
    private val sessionService: SessionService
) {

    /**
     * Résultats des opérations de recherche
     */
    sealed class SearchResult {
        data class Success(val results: GlobalSearchResults) : SearchResult()
        data class Error(val message: String, val code: SearchErrorCode) : SearchResult()
    }

    enum class SearchErrorCode {
        INVALID_QUERY,
        ACCESS_DENIED,
        TIMEOUT,
        TOO_MANY_RESULTS,
        INTERNAL_ERROR
    }

    /**
     * Cache des recherches récentes pour optimiser les performances
     */
    private val searchCache = mutableMapOf<String, CachedSearchResult>()
    private val cacheValidityMinutes = 5L
    private val maxCacheSize = 50

    /**
     * Historique des recherches par utilisateur
     */
    private val searchHistory = mutableMapOf<Int, MutableList<SearchHistoryItem>>()
    private val maxHistoryPerUser = 20

    /**
     * Configuration de recherche
     */
    private val maxSearchResults = 100
    private val searchTimeoutMs = 30000L // 30 secondes

    // ================================================================
    // MÉTHODES DE RECHERCHE PRINCIPALES
    // ================================================================

    /**
     * Effectue une recherche globale dans toute l'application
     *
     * @param query Terme de recherche
     * @param filters Filtres optionnels de recherche
     * @param searchOptions Options de recherche
     * @return Résultats de recherche globaux
     */
    suspend fun performGlobalSearch(
        query: String,
        filters: SearchFilters = SearchFilters(),
        searchOptions: SearchOptions = SearchOptions()
    ): SearchResult = withContext(Dispatchers.IO) {
        return@withContext try {
            // Validation de la requête
            val validationResult = validateSearchQuery(query)
            if (validationResult != null) {
                return@withContext SearchResult.Error(validationResult, SearchErrorCode.INVALID_QUERY)
            }

            // Vérification des permissions
            val currentUser = sessionService.getCurrentUser()
                ?: return@withContext SearchResult.Error(
                    "Utilisateur non connecté",
                    SearchErrorCode.ACCESS_DENIED
                )

            // Normalisation de la requête
            val normalizedQuery = normalizeQuery(query)

            // Vérifier le cache si autorisé
            if (searchOptions.useCache) {
                val cachedResult = getCachedResult(normalizedQuery, filters)
                if (cachedResult != null) {
                    addToSearchHistory(currentUser.membreFamilleId, query, true)
                    return@withContext SearchResult.Success(cachedResult.results)
                }
            }

            println("🔍 Recherche globale: '$query' (utilisateur: ${currentUser.prenomMembre})")
            val startTime = System.currentTimeMillis()

            // Recherche parallèle dans toutes les entités
            val searchResults = supervisorScope {
                val filesSearch = async { searchFiles(normalizedQuery, filters) }
                val foldersSearch = async { searchFolders(normalizedQuery, filters) }
                val categoriesSearch = async { searchCategories(normalizedQuery, filters) }
                val membersSearch = async { searchFamilyMembers(normalizedQuery, filters) }

                SearchComponents(
                    files = filesSearch.await(),
                    folders = foldersSearch.await(),
                    categories = categoriesSearch.await(),
                    members = membersSearch.await()
                )
            }

            val searchDuration = System.currentTimeMillis() - startTime

            // Construire les résultats globaux
            val globalResults = buildGlobalResults(
                query = query,
                normalizedQuery = normalizedQuery,
                components = searchResults,
                searchTime = LocalDateTime.now(),
                duration = searchDuration,
                filters = filters
            )

            // Appliquer les options de tri et pagination
            val processedResults = processSearchResults(globalResults, searchOptions)

            // Mettre en cache si autorisé
            if (searchOptions.useCache && processedResults.totalResults <= maxSearchResults) {
                cacheSearchResult(normalizedQuery, filters, processedResults)
            }

            // Ajouter à l'historique
            addToSearchHistory(currentUser.membreFamilleId, query, false)

            println("✅ Recherche terminée: ${processedResults.totalResults} résultats en ${searchDuration}ms")
            SearchResult.Success(processedResults)

        } catch (e: Exception) {
            println("❌ Erreur lors de la recherche: ${e.message}")
            SearchResult.Error(
                "Erreur lors de la recherche: ${e.message}",
                SearchErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Recherche rapide avec auto-complétion
     *
     * @param query Début du terme de recherche
     * @param maxSuggestions Nombre max de suggestions
     * @return Liste de suggestions
     */
    suspend fun quickSearch(
        query: String,
        maxSuggestions: Int = 10
    ): List<SearchSuggestion> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (query.length < 2) return@withContext emptyList()

            val currentUser = sessionService.getCurrentUser() ?: return@withContext emptyList()
            val normalizedQuery = normalizeQuery(query)

            // Recherche rapide dans les noms/titres uniquement
            val suggestions = mutableListOf<SearchSuggestion>()

            // Suggestions depuis l'historique
            val historyUser = searchHistory[currentUser.membreFamilleId] ?: emptyList()
            historyUser.filter { it.query.contains(normalizedQuery, ignoreCase = true) }
                .take(3)
                .forEach { history ->
                    suggestions.add(
                        SearchSuggestion(
                            text = history.query,
                            type = SuggestionType.HISTORY,
                            score = 1.0
                        )
                    )
                }

            // Suggestions de fichiers (noms uniquement pour la rapidité)
            val fileNames = getFileNameSuggestions(normalizedQuery, maxSuggestions / 2)
            suggestions.addAll(fileNames)

            // Suggestions de dossiers
            val folderNames = getFolderNameSuggestions(normalizedQuery, maxSuggestions / 2)
            suggestions.addAll(folderNames)

            // Trier par score et limiter
            suggestions
                .sortedByDescending { it.score }
                .take(maxSuggestions)

        } catch (e: Exception) {
            println("❌ Erreur lors de la recherche rapide: ${e.message}")
            emptyList()
        }
    }

    /**
     * Recherche avancée avec critères multiples
     *
     * @param criteria Critères de recherche avancée
     * @return Résultats de recherche
     */
    suspend fun advancedSearch(criteria: AdvancedSearchCriteria): SearchResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUser = sessionService.getCurrentUser()
                ?: return@withContext SearchResult.Error(
                    "Utilisateur non connecté",
                    SearchErrorCode.ACCESS_DENIED
                )

            println("🔍 Recherche avancée: ${criteria.summary()}")

            // Convertir les critères en filtres de recherche
            val filters = criteria.toSearchFilters()
            val searchOptions = SearchOptions(useCache = false, sortBy = criteria.sortBy)

            // Effectuer la recherche avec les critères
            val query = criteria.textQuery ?: "*" // Recherche globale si pas de texte

            performGlobalSearch(query, filters, searchOptions)

        } catch (e: Exception) {
            SearchResult.Error(
                "Erreur lors de la recherche avancée: ${e.message}",
                SearchErrorCode.INTERNAL_ERROR
            )
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE SPÉCIALISÉES
    // ================================================================

    /**
     * Recherche dans les fichiers (placeholder)
     */
    private suspend fun searchFiles(
        query: String,
        filters: SearchFilters
    ): SearchComponentResult<Fichier> {
        return try {
            // TODO: Remplacer par fileController.searchFiles(query) quand disponible
            val files = emptyList<Fichier>()

            // Appliquer les filtres spécifiques aux fichiers
            val filteredFiles = files.filter { file ->
                applyFileFilters(file, filters)
            }

            SearchComponentResult(
                items = filteredFiles,
                totalFound = filteredFiles.size,
                searchTimeMs = 0 // À mesurer si nécessaire
            )

        } catch (e: Exception) {
            println("❌ Erreur recherche fichiers: ${e.message}")
            SearchComponentResult()
        }
    }

    /**
     * Recherche dans les dossiers (placeholder)
     */
    private suspend fun searchFolders(
        query: String,
        filters: SearchFilters
    ): SearchComponentResult<Dossier> {
        return try {
            // TODO: Remplacer par folderController.searchFolders(query) quand disponible
            val folders = emptyList<Dossier>()

            // Appliquer les filtres spécifiques aux dossiers
            val filteredFolders = folders.filter { folder ->
                applyFolderFilters(folder, filters)
            }

            SearchComponentResult(
                items = filteredFolders,
                totalFound = filteredFolders.size,
                searchTimeMs = 0
            )

        } catch (e: Exception) {
            println("❌ Erreur recherche dossiers: ${e.message}")
            SearchComponentResult()
        }
    }

    /**
     * Recherche dans les catégories
     */
    private suspend fun searchCategories(
        query: String,
        filters: SearchFilters
    ): SearchComponentResult<Categorie> {
        return try {
            // Implémentation basique - à étendre selon vos besoins
            // val categories = categoryController.searchCategories(query)
            val categories = emptyList<Categorie>() // Placeholder

            SearchComponentResult(
                items = categories,
                totalFound = categories.size,
                searchTimeMs = 0
            )

        } catch (e: Exception) {
            println("❌ Erreur recherche catégories: ${e.message}")
            SearchComponentResult()
        }
    }

    /**
     * Recherche dans les membres de famille
     */
    private suspend fun searchFamilyMembers(
        query: String,
        filters: SearchFilters
    ): SearchComponentResult<MembreFamille> {
        return try {
            // Implémentation basique - à étendre selon vos besoins
            // val members = familyMemberController.searchMembers(query)
            val members = emptyList<MembreFamille>() // Placeholder

            SearchComponentResult(
                items = members,
                totalFound = members.size,
                searchTimeMs = 0
            )

        } catch (e: Exception) {
            println("❌ Erreur recherche membres: ${e.message}")
            SearchComponentResult()
        }
    }

    // ================================================================
    // MÉTHODES DE SUGGESTIONS
    // ================================================================

    /**
     * Obtient des suggestions de noms de fichiers
     */
    private suspend fun getFileNameSuggestions(
        query: String,
        maxSuggestions: Int
    ): List<SearchSuggestion> {
        return try {
            // Implémentation simplifiée - à étendre selon vos besoins
            // val fileNames = fileController.getFileNamesSuggestions(query, maxSuggestions)
            emptyList() // Placeholder
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Obtient des suggestions de noms de dossiers
     */
    private suspend fun getFolderNameSuggestions(
        query: String,
        maxSuggestions: Int
    ): List<SearchSuggestion> {
        return try {
            // Implémentation simplifiée - à étendre selon vos besoins
            // val folderNames = folderController.getFolderNamesSuggestions(query, maxSuggestions)
            emptyList() // Placeholder
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ================================================================
    // MÉTHODES DE FILTRAGE
    // ================================================================

    /**
     * Applique les filtres spécifiques aux fichiers
     */
    private fun applyFileFilters(file: Fichier, filters: SearchFilters): Boolean {
        // Filtre par type de fichier
        if (filters.fileTypes.isNotEmpty() &&
            !filters.fileTypes.contains(file.typeFichier)) {
            return false
        }

        // Filtre par taille
        if (filters.minFileSize != null && file.tailleFichier < filters.minFileSize) {
            return false
        }
        if (filters.maxFileSize != null && file.tailleFichier > filters.maxFileSize) {
            return false
        }

        // Filtre par date
        if (filters.dateFrom != null &&
            file.dateCreationFichier?.isBefore(filters.dateFrom) == true) {
            return false
        }
        if (filters.dateTo != null &&
            file.dateCreationFichier?.isAfter(filters.dateTo) == true) {
            return false
        }

        return true
    }

    /**
     * Applique les filtres spécifiques aux dossiers
     */
    private fun applyFolderFilters(folder: Dossier, filters: SearchFilters): Boolean {
        // Filtre par catégorie
        if (filters.categoryIds.isNotEmpty() &&
            !filters.categoryIds.contains(folder.categorieId)) {
            return false
        }

        // Filtre par date de création
        if (filters.dateFrom != null &&
            folder.dateCreationDossier?.isBefore(filters.dateFrom) == true) {
            return false
        }
        if (filters.dateTo != null &&
            folder.dateCreationDossier?.isAfter(filters.dateTo) == true) {
            return false
        }

        return true
    }

    // ================================================================
    // MÉTHODES DE CACHE
    // ================================================================

    /**
     * Récupère un résultat depuis le cache
     */
    private fun getCachedResult(
        query: String,
        filters: SearchFilters
    ): CachedSearchResult? {
        val cacheKey = generateCacheKey(query, filters)
        val cached = searchCache[cacheKey] ?: return null

        // Vérifier la validité du cache
        val now = LocalDateTime.now()
        val cacheAge = java.time.Duration.between(cached.timestamp, now)

        return if (cacheAge.toMinutes() < cacheValidityMinutes) {
            println("📋 Résultat depuis le cache: $query")
            cached
        } else {
            searchCache.remove(cacheKey)
            null
        }
    }

    /**
     * Met en cache un résultat de recherche
     */
    private fun cacheSearchResult(
        query: String,
        filters: SearchFilters,
        results: GlobalSearchResults
    ) {
        val cacheKey = generateCacheKey(query, filters)
        val cached = CachedSearchResult(results, LocalDateTime.now())

        searchCache[cacheKey] = cached

        // Limiter la taille du cache
        if (searchCache.size > maxCacheSize) {
            val oldestKey = searchCache.minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { searchCache.remove(it) }
        }
    }

    /**
     * Génère une clé de cache unique
     */
    private fun generateCacheKey(query: String, filters: SearchFilters): String {
        return "${query}_${filters.hashCode()}"
    }

    // ================================================================
    // MÉTHODES D'HISTORIQUE
    // ================================================================

    /**
     * Ajoute une recherche à l'historique
     */
    private fun addToSearchHistory(userId: Int, query: String, fromCache: Boolean) {
        val history = searchHistory.getOrPut(userId) { mutableListOf() }

        val historyItem = SearchHistoryItem(
            query = query,
            timestamp = LocalDateTime.now(),
            fromCache = fromCache
        )

        // Éviter les doublons récents
        if (history.none { it.query == query &&
                    java.time.Duration.between(it.timestamp, LocalDateTime.now()).toMinutes() < 5 }) {
            history.add(historyItem)
        }

        // Limiter la taille de l'historique
        if (history.size > maxHistoryPerUser) {
            history.removeAt(0)
        }
    }

    /**
     * Obtient l'historique de recherche d'un utilisateur
     */
    fun getSearchHistory(userId: Int): List<SearchHistoryItem> {
        return searchHistory[userId]?.toList()?.reversed() ?: emptyList()
    }

    /**
     * Efface l'historique de recherche d'un utilisateur
     */
    fun clearSearchHistory(userId: Int? = null) {
        if (userId != null) {
            searchHistory[userId]?.clear()
            println("🧹 Historique de recherche effacé pour l'utilisateur $userId")
        } else {
            // Effacer tout l'historique si aucun utilisateur spécifié
            searchHistory.clear()
            println("🧹 Tout l'historique de recherche effacé")
        }
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Valide une requête de recherche
     */
    private fun validateSearchQuery(query: String): String? {
        return when {
            query.isBlank() -> "Critère de recherche requis"
            query.length < 2 -> "Le critère doit contenir au moins 2 caractères"
            query.length > 200 -> "Le critère ne peut pas dépasser 200 caractères"
            else -> null
        }
    }

    /**
     * Normalise une requête de recherche
     */
    private fun normalizeQuery(query: String): String {
        return query.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ") // Remplacer plusieurs espaces par un seul
    }

    /**
     * Construit les résultats globaux
     */
    private fun buildGlobalResults(
        query: String,
        normalizedQuery: String,
        components: SearchComponents,
        searchTime: LocalDateTime,
        duration: Long,
        filters: SearchFilters
    ): GlobalSearchResults {
        val totalResults = components.files.totalFound +
                components.folders.totalFound +
                components.categories.totalFound +
                components.members.totalFound

        return GlobalSearchResults(
            query = query,
            normalizedQuery = normalizedQuery,
            files = components.files.items,
            folders = components.folders.items,
            categories = components.categories.items,
            members = components.members.items,
            totalResults = totalResults,
            searchTime = searchTime,
            searchDurationMs = duration,
            appliedFilters = filters
        )
    }

    /**
     * Traite les résultats selon les options
     */
    private fun processSearchResults(
        results: GlobalSearchResults,
        options: SearchOptions
    ): GlobalSearchResults {
        // Appliquer le tri si spécifié
        var processedResults = results

        when (options.sortBy) {
            SearchSortBy.RELEVANCE -> {
                // Tri par pertinence (implémentation basique)
                processedResults = processedResults.copy(
                    files = results.files.sortedByDescending { calculateRelevanceScore(it, results.normalizedQuery) }
                )
            }
            SearchSortBy.DATE_DESC -> {
                processedResults = processedResults.copy(
                    files = results.files.sortedByDescending { it.dateCreationFichier }
                )
            }
            SearchSortBy.DATE_ASC -> {
                processedResults = processedResults.copy(
                    files = results.files.sortedBy { it.dateCreationFichier }
                )
            }
            SearchSortBy.NAME_ASC -> {
                processedResults = processedResults.copy(
                    files = results.files.sortedBy { it.nomFichier }
                )
            }
            SearchSortBy.NAME_DESC -> {
                processedResults = processedResults.copy(
                    files = results.files.sortedByDescending { it.nomFichier }
                )
            }
            SearchSortBy.SIZE_DESC -> {
                processedResults = processedResults.copy(
                    files = results.files.sortedByDescending { it.tailleFichier }
                )
            }
            SearchSortBy.SIZE_ASC -> {
                processedResults = processedResults.copy(
                    files = results.files.sortedBy { it.tailleFichier }
                )
            }
        }

        return processedResults
    }

    /**
     * Calcule un score de pertinence basique
     */
    private fun calculateRelevanceScore(file: Fichier, query: String): Double {
        var score = 0.0

        // Correspondance exacte dans le nom = score élevé
        if (file.nomFichier.contains(query, ignoreCase = true)) {
            score += 10.0
        }

        // Correspondance dans le type de fichier
        if (file.typeFichier?.contains(query, ignoreCase = true) == true) {
            score += 5.0
        }

        // Plus le fichier est récent, plus le score est élevé
        file.dateCreationFichier?.let { date ->
            val daysSinceCreation = java.time.Duration.between(date, LocalDateTime.now()).toDays()
            score += maxOf(0.0, 5.0 - (daysSinceCreation / 30.0)) // Décroissance sur 30 jours
        }

        return score
    }

    // ================================================================
    // MÉTHODES DE NETTOYAGE
    // ================================================================

    /**
     * Efface le cache de recherche
     */
    fun clearSearchCache() {
        searchCache.clear()
        println("🧹 Cache de recherche effacé")
    }

    /**
     * Nettoyage des ressources du service
     */
    fun cleanup() {
        searchCache.clear()
        searchHistory.clear()
        println("🧹 SearchService nettoyé")
    }

    /**
     * Obtient les statistiques du service de recherche
     */
    fun getSearchStatistics(): Map<String, Any> {
        val totalSearches = searchHistory.values.sumOf { it.size }
        val cacheHitRate = if (totalSearches > 0) {
            searchHistory.values.flatten().count { it.fromCache }.toDouble() / totalSearches
        } else 0.0

        return mapOf(
            "totalSearches" to totalSearches,
            "uniqueUsers" to searchHistory.size,
            "cacheSize" to searchCache.size,
            "cacheHitRate" to String.format("%.2f%%", cacheHitRate * 100),
            "avgSearchesPerUser" to if (searchHistory.isNotEmpty()) totalSearches / searchHistory.size else 0
        )
    }
}

// ================================================================
// DATA CLASSES POUR LA RECHERCHE
// ================================================================

data class SearchFilters(
    val fileTypes: List<String> = emptyList(),
    val categoryIds: List<Int> = emptyList(),
    val memberIds: List<Int> = emptyList(),
    val dateFrom: LocalDateTime? = null,
    val dateTo: LocalDateTime? = null,
    val minFileSize: Long? = null,
    val maxFileSize: Long? = null,
    val includeArchived: Boolean = false
)

data class SearchOptions(
    val useCache: Boolean = true,
    val sortBy: SearchSortBy = SearchSortBy.RELEVANCE,
    val maxResults: Int = 100
)

enum class SearchSortBy {
    RELEVANCE,
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    SIZE_ASC
}

data class SearchComponents(
    val files: SearchComponentResult<Fichier>,
    val folders: SearchComponentResult<Dossier>,
    val categories: SearchComponentResult<Categorie>,
    val members: SearchComponentResult<MembreFamille>
)

data class SearchComponentResult<T>(
    val items: List<T> = emptyList(),
    val totalFound: Int = 0,
    val searchTimeMs: Long = 0
)

data class GlobalSearchResults(
    val query: String,
    val normalizedQuery: String = query,
    val files: List<Fichier>,
    val folders: List<Dossier>,
    val categories: List<Categorie> = emptyList(),
    val members: List<MembreFamille> = emptyList(),
    val totalResults: Int,
    val searchTime: LocalDateTime,
    val searchDurationMs: Long = 0,
    val appliedFilters: SearchFilters = SearchFilters()
)

data class SearchSuggestion(
    val text: String,
    val type: SuggestionType,
    val score: Double = 1.0
)

enum class SuggestionType {
    HISTORY,
    FILE_NAME,
    FOLDER_NAME,
    CATEGORY_NAME,
    MEMBER_NAME
}

data class SearchHistoryItem(
    val query: String,
    val timestamp: LocalDateTime,
    val fromCache: Boolean = false
)

data class CachedSearchResult(
    val results: GlobalSearchResults,
    val timestamp: LocalDateTime
)

data class AdvancedSearchCriteria(
    val textQuery: String? = null,
    val fileTypes: List<String> = emptyList(),
    val categories: List<Int> = emptyList(),
    val dateRange: Pair<LocalDateTime, LocalDateTime>? = null,
    val sizeRange: Pair<Long, Long>? = null,
    val sortBy: SearchSortBy = SearchSortBy.RELEVANCE,
    val includeArchived: Boolean = false
) {
    fun toSearchFilters(): SearchFilters {
        return SearchFilters(
            fileTypes = fileTypes,
            categoryIds = categories,
            dateFrom = dateRange?.first,
            dateTo = dateRange?.second,
            minFileSize = sizeRange?.first,
            maxFileSize = sizeRange?.second,
            includeArchived = includeArchived
        )
    }

    fun summary(): String {
        val parts = mutableListOf<String>()
        textQuery?.let { parts.add("texte: '$it'") }
        if (fileTypes.isNotEmpty()) parts.add("types: ${fileTypes.joinToString(",")}")
        if (categories.isNotEmpty()) parts.add("catégories: ${categories.size}")
        dateRange?.let { parts.add("période: ${it.first} à ${it.second}") }
        sizeRange?.let { parts.add("taille: ${it.first}-${it.second} bytes") }

        return parts.joinToString(", ")
    }
}