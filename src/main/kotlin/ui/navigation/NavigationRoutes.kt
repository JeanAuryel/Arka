// ================================================================
// NAVIGATIONROUTES.KT - ROUTES ET CHEMINS DE NAVIGATION ARKA
// ================================================================

package ui.navigation

/**
 * Définition des routes de navigation pour l'application Arka
 * Centralise tous les chemins et patterns de routes
 */
object NavigationRoutes {

    // ================================================================
    // ROUTES PRINCIPALES
    // ================================================================

    const val LOGIN = "/login"
    const val SETUP = "/setup"
    const val DASHBOARD = "/dashboard"

    // ================================================================
    // ROUTES AVEC PARAMÈTRES
    // ================================================================

    object Files {
        const val BASE = "/files"
        const val LIST = "/files/list"
        const val DETAILS = "/files/{fileId}"
        const val UPLOAD = "/files/upload"
        const val SEARCH = "/files/search"

        fun details(fileId: Int) = "/files/$fileId"
        fun withFolder(folderId: Int) = "/files?folderId=$folderId"
        fun withCategory(categoryId: Int) = "/files?categoryId=$categoryId"
    }

    object Folders {
        const val BASE = "/folders"
        const val LIST = "/folders/list"
        const val DETAILS = "/folders/{folderId}"
        const val CREATE = "/folders/create"

        fun details(folderId: Int) = "/folders/$folderId"
        fun withCategory(categoryId: Int) = "/folders?categoryId=$categoryId"
        fun withParent(parentId: Int) = "/folders?parentId=$parentId"
    }

    object Categories {
        const val BASE = "/categories"
        const val LIST = "/categories/list"
        const val DETAILS = "/categories/{categoryId}"
        const val CREATE = "/categories/create"
        const val MANAGE = "/categories/manage"

        fun details(categoryId: Int) = "/categories/$categoryId"
    }

    object Family {
        const val BASE = "/family"
        const val LIST = "/family/members"
        const val MEMBER_DETAILS = "/family/members/{memberId}"
        const val INVITE = "/family/invite"
        const val PERMISSIONS = "/family/permissions"

        fun memberDetails(memberId: Int) = "/family/members/$memberId"
        fun memberPermissions(memberId: Int) = "/family/permissions?memberId=$memberId"
    }

    object Delegations {
        const val BASE = "/delegations"
        const val LIST = "/delegations/list"
        const val DETAILS = "/delegations/{delegationId}"
        const val CREATE = "/delegations/create"
        const val PENDING = "/delegations/pending"
        const val GRANTED = "/delegations/granted"
        const val RECEIVED = "/delegations/received"

        fun details(delegationId: Int) = "/delegations/$delegationId"
        fun withType(type: DelegationType) = "/delegations?type=${type.name.lowercase()}"
        fun forMember(memberId: Int) = "/delegations?memberId=$memberId"
    }

    object Mobile {
        const val BASE = "/mobile"
        const val SYNC = "/mobile/sync"
        const val DEVICES = "/mobile/devices"
        const val DEVICE_DETAILS = "/mobile/devices/{deviceId}"
        const val PAIRING = "/mobile/pairing"

        fun deviceDetails(deviceId: String) = "/mobile/devices/$deviceId"
    }

    object Settings {
        const val BASE = "/settings"
        const val GENERAL = "/settings/general"
        const val SECURITY = "/settings/security"
        const val PERMISSIONS = "/settings/permissions"
        const val SYSTEM = "/settings/system"
        const val LOGS = "/settings/logs"
    }

    object Profile {
        const val BASE = "/profile"
        const val EDIT = "/profile/edit"
        const val SECURITY = "/profile/security"
        const val PREFERENCES = "/profile/preferences"
    }

    // ================================================================
    // ROUTES D'ERREUR
    // ================================================================

    object Error {
        const val NOT_FOUND = "/error/404"
        const val ACCESS_DENIED = "/error/403"
        const val SERVER_ERROR = "/error/500"
        const val NETWORK_ERROR = "/error/network"
    }

    // ================================================================
    // PATTERNS DE ROUTES
    // ================================================================

    /**
     * Patterns pour la validation et l'extraction de paramètres
     */
    object Patterns {
        const val FILE_ID = "fileId"
        const val FOLDER_ID = "folderId"
        const val CATEGORY_ID = "categoryId"
        const val MEMBER_ID = "memberId"
        const val DELEGATION_ID = "delegationId"
        const val DEVICE_ID = "deviceId"

        // Regex patterns pour validation
        const val ID_PATTERN = "\\d+"
        const val UUID_PATTERN = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
    }

    // ================================================================
    // PARAMÈTRES DE QUERY
    // ================================================================

    object QueryParams {
        const val PAGE = "page"
        const val SIZE = "size"
        const val SORT = "sort"
        const val ORDER = "order"
        const val SEARCH = "search"
        const val FILTER = "filter"
        const val CATEGORY = "category"
        const val FOLDER = "folder"
        const val MEMBER = "member"
        const val TYPE = "type"
        const val STATUS = "status"
        const val FROM_DATE = "fromDate"
        const val TO_DATE = "toDate"
    }

    // ================================================================
    // BUILDERS DE ROUTES
    // ================================================================

    /**
     * Builder pour construire des routes complexes
     */
    class RouteBuilder(private val basePath: String) {
        private val params = mutableMapOf<String, String>()
        private val queryParams = mutableMapOf<String, String>()

        fun withParam(key: String, value: Any): RouteBuilder {
            params[key] = value.toString()
            return this
        }

        fun withQuery(key: String, value: Any): RouteBuilder {
            queryParams[key] = value.toString()
            return this
        }

        fun withPagination(page: Int, size: Int): RouteBuilder {
            queryParams[QueryParams.PAGE] = page.toString()
            queryParams[QueryParams.SIZE] = size.toString()
            return this
        }

        fun withSort(field: String, order: String = "asc"): RouteBuilder {
            queryParams[QueryParams.SORT] = field
            queryParams[QueryParams.ORDER] = order
            return this
        }

        fun withSearch(query: String): RouteBuilder {
            queryParams[QueryParams.SEARCH] = query
            return this
        }

        fun build(): String {
            var route = basePath

            // Remplacer les paramètres de path
            params.forEach { (key, value) ->
                route = route.replace("{$key}", value)
            }

            // Ajouter les paramètres de query
            if (queryParams.isNotEmpty()) {
                val queryString = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
                route += "?$queryString"
            }

            return route
        }
    }

    // ================================================================
    // UTILITAIRES DE NAVIGATION
    // ================================================================

    /**
     * Extracteur de paramètres depuis une route
     */
    object RouteParser {

        /**
         * Extrait les paramètres d'une route
         */
        fun extractParams(route: String, pattern: String): Map<String, String> {
            val params = mutableMapOf<String, String>()

            // Séparer la route des query params
            val (path, query) = if (route.contains("?")) {
                val parts = route.split("?", limit = 2)
                parts[0] to parts[1]
            } else {
                route to ""
            }

            // Extraire les paramètres de path
            extractPathParams(path, pattern).forEach { (key, value) ->
                params[key] = value
            }

            // Extraire les paramètres de query
            extractQueryParams(query).forEach { (key, value) ->
                params[key] = value
            }

            return params
        }

        private fun extractPathParams(path: String, pattern: String): Map<String, String> {
            val params = mutableMapOf<String, String>()

            val pathSegments = path.split("/").filter { it.isNotEmpty() }
            val patternSegments = pattern.split("/").filter { it.isNotEmpty() }

            if (pathSegments.size != patternSegments.size) return params

            patternSegments.forEachIndexed { index, segment ->
                if (segment.startsWith("{") && segment.endsWith("}")) {
                    val paramName = segment.substring(1, segment.length - 1)
                    if (index < pathSegments.size) {
                        params[paramName] = pathSegments[index]
                    }
                }
            }

            return params
        }

        private fun extractQueryParams(query: String): Map<String, String> {
            if (query.isEmpty()) return emptyMap()

            return query.split("&")
                .mapNotNull { param ->
                    val parts = param.split("=", limit = 2)
                    if (parts.size == 2) {
                        parts[0] to parts[1]
                    } else null
                }
                .toMap()
        }

        /**
         * Vérifie si une route correspond à un pattern
         */
        fun matches(route: String, pattern: String): Boolean {
            val routePath = route.split("?")[0]
            val routeSegments = routePath.split("/").filter { it.isNotEmpty() }
            val patternSegments = pattern.split("/").filter { it.isNotEmpty() }

            if (routeSegments.size != patternSegments.size) return false

            return routeSegments.zip(patternSegments).all { (routeSegment, patternSegment) ->
                patternSegment.startsWith("{") && patternSegment.endsWith("}") ||
                        routeSegment == patternSegment
            }
        }
    }

    // ================================================================
    // FACTORY METHODS
    // ================================================================

    /**
     * Crée un builder pour une route de base
     */
    fun builder(basePath: String): RouteBuilder = RouteBuilder(basePath)

    /**
     * Routes rapides pour les cas courants
     */
    fun fileDetails(fileId: Int, folderId: Int? = null): String {
        return builder(Files.DETAILS)
            .withParam(Patterns.FILE_ID, fileId)
            .apply { folderId?.let { withQuery(QueryParams.FOLDER, it) } }
            .build()
    }

    fun folderContents(folderId: Int, page: Int = 1, size: Int = 20): String {
        return builder(Folders.DETAILS)
            .withParam(Patterns.FOLDER_ID, folderId)
            .withPagination(page, size)
            .build()
    }

    fun categoryFiles(categoryId: Int, sortBy: String = "name"): String {
        return builder(Categories.DETAILS)
            .withParam(Patterns.CATEGORY_ID, categoryId)
            .withSort(sortBy)
            .build()
    }

    fun delegationsForMember(memberId: Int, type: DelegationType = DelegationType.ALL): String {
        return builder(Delegations.LIST)
            .withQuery(QueryParams.MEMBER, memberId)
            .withQuery(QueryParams.TYPE, type.name.lowercase())
            .build()
    }

    fun searchFiles(query: String, categoryId: Int? = null, page: Int = 1): String {
        return builder(Files.SEARCH)
            .withSearch(query)
            .withPagination(page, 20)
            .apply { categoryId?.let { withQuery(QueryParams.CATEGORY, it) } }
            .build()
    }

    // ================================================================
    // VALIDATION DE ROUTES
    // ================================================================

    /**
     * Valide qu'une route est bien formée
     */
    fun isValidRoute(route: String): Boolean {
        return try {
            route.startsWith("/") &&
                    !route.contains("//") &&
                    !route.endsWith("/") || route == "/"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Normalise une route (supprime les doubles slashes, etc.)
     */
    fun normalizeRoute(route: String): String {
        return route
            .replace(Regex("//+"), "/")
            .let { if (it.endsWith("/") && it != "/") it.dropLast(1) else it }
            .let { if (!it.startsWith("/")) "/$it" else it }
    }

    /**
     * Obtient la route parent
     */
    fun getParentRoute(route: String): String? {
        val normalizedRoute = normalizeRoute(route)
        val segments = normalizedRoute.split("/").filter { it.isNotEmpty() }

        return when {
            segments.isEmpty() -> null
            segments.size == 1 -> "/"
            else -> "/" + segments.dropLast(1).joinToString("/")
        }
    }
}

/**
 * Extensions pour faciliter l'utilisation des routes
 */

/**
 * Extension pour NavigationState pour obtenir sa route par défaut
 */
fun NavigationState.getDefaultRoute(): String {
    return when (this) {
        NavigationState.LOGIN -> NavigationRoutes.LOGIN
        NavigationState.SETUP -> NavigationRoutes.SETUP
        NavigationState.DASHBOARD -> NavigationRoutes.DASHBOARD
        NavigationState.FILES -> NavigationRoutes.Files.LIST
        NavigationState.FOLDER_DETAILS -> NavigationRoutes.Folders.BASE
        NavigationState.FILE_DETAILS -> NavigationRoutes.Files.BASE
        NavigationState.CATEGORIES -> NavigationRoutes.Categories.LIST
        NavigationState.CATEGORY_DETAILS -> NavigationRoutes.Categories.BASE
        NavigationState.FAMILY_MEMBERS -> NavigationRoutes.Family.LIST
        NavigationState.MEMBER_DETAILS -> NavigationRoutes.Family.BASE
        NavigationState.DELEGATIONS -> NavigationRoutes.Delegations.LIST
        NavigationState.DELEGATION_DETAILS -> NavigationRoutes.Delegations.BASE
        NavigationState.PERMISSIONS -> NavigationRoutes.Family.PERMISSIONS
        NavigationState.SETTINGS -> NavigationRoutes.Settings.BASE
        NavigationState.USER_PROFILE -> NavigationRoutes.Profile.BASE
        NavigationState.SYSTEM_LOGS -> NavigationRoutes.Settings.LOGS
        NavigationState.FILE_UPLOAD -> NavigationRoutes.Files.UPLOAD
        NavigationState.CREATE_FOLDER -> NavigationRoutes.Folders.CREATE
        NavigationState.CREATE_CATEGORY -> NavigationRoutes.Categories.CREATE
        NavigationState.INVITE_MEMBER -> NavigationRoutes.Family.INVITE
        NavigationState.MOBILE_SYNC -> NavigationRoutes.Mobile.SYNC
        NavigationState.MOBILE_DEVICES -> NavigationRoutes.Mobile.DEVICES
        NavigationState.ERROR_404 -> NavigationRoutes.Error.NOT_FOUND
        NavigationState.ERROR_403 -> NavigationRoutes.Error.ACCESS_DENIED
        NavigationState.ERROR_500 -> NavigationRoutes.Error.SERVER_ERROR
    }
}