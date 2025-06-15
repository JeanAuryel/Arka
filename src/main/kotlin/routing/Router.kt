package ui.routing

import androidx.compose.runtime.*
import controllers.*
import org.koin.java.KoinJavaComponent.inject

// Importation des écrans avec la nouvelle organisation
import ui.screens.LoginScreen
import ui.screens.HomeScreen
import ui.screens.SettingsScreen

// Écrans admin
import ui.screens.admin.AdminPanelScreen
import ui.screens.admin.AuditScreen
import ui.screens.admin.StatisticsScreen

// Écrans notifications
import ui.screens.notifications.NotificationsScreen

// Écrans catégories
import ui.screens.categories.CategoriesScreen

// Écrans délégations
import ui.screens.delegations.DelegationsScreen

// Écrans membres famille
import ui.screens.familymembers.FamilyMembersScreen

// Écrans fichiers
import ui.screens.files.FilesScreen
import ui.screens.files.FileDetailsScreen

// Écrans dossiers
import ui.screens.folders.FoldersScreen

// Écrans espaces
import ui.screens.spaces.SpaceOverviewScreen
import ui.screens.spaces.SpaceDetailsScreen
import ui.screens.spaces.SpacePermissionScreen
import ui.screens.spaces.SpaceSettingsScreen

// Écrans permissions
import ui.screens.permissions.PermissionOverviewScreen
import ui.screens.permissions.RequestPermissionScreen

/**
 * Énumération des écrans disponibles dans Arka
 */
enum class ArkaScreen(val route: String, val title: String) {
    // Écrans principaux
    LOGIN("login", "Connexion"),
    HOME("home", "Accueil"),
    SETTINGS("settings", "Paramètres"),

    // Administration
    ADMIN_PANEL("admin/panel", "Panneau d'administration"),
    AUDIT("admin/audit", "Journal d'audit"),
    STATISTICS("admin/statistics", "Statistiques"),

    // Notifications et alertes
    NOTIFICATIONS("notifications", "Notifications"),

    // Gestion des contenus
    CATEGORIES("categories", "Catégories"),
    FILES("files", "Fichiers"),
    FILES_DETAILS("files/details", "Détails du fichier"),
    FOLDERS("folders", "Dossiers"),

    // Gestion famille
    FAMILY_MEMBERS("family/members", "Membres de la famille"),

    // Espaces
    SPACES_OVERVIEW("spaces/overview", "Vue d'ensemble des espaces"),
    SPACE_DETAILS("spaces/details", "Détails de l'espace"),
    SPACE_PERMISSIONS("spaces/permissions", "Permissions de l'espace"),
    SPACE_SETTINGS("spaces/settings", "Paramètres de l'espace"),

    // Permissions et délégations
    PERMISSIONS_OVERVIEW("permissions/overview", "Vue d'ensemble des permissions"),
    REQUEST_PERMISSION("permissions/request", "Demander une permission"),
    DELEGATIONS("delegations", "Délégations")
}

/**
 * État de navigation global
 */
class ArkaNavigationState {
    var currentScreen by mutableStateOf(ArkaScreen.LOGIN)
        private set

    var selectedItemId by mutableStateOf<Int?>(null)
        private set

    var breadcrumb by mutableStateOf<List<String>>(emptyList())
        private set

    fun navigateTo(screen: ArkaScreen, itemId: Int? = null) {
        currentScreen = screen
        selectedItemId = itemId
        updateBreadcrumb(screen)
    }

    fun navigateBack() {
        when (currentScreen) {
            // Navigation depuis les sous-écrans vers les écrans principaux
            ArkaScreen.FILES_DETAILS -> navigateTo(ArkaScreen.FILES)
            ArkaScreen.SPACE_DETAILS,
            ArkaScreen.SPACE_PERMISSIONS,
            ArkaScreen.SPACE_SETTINGS -> navigateTo(ArkaScreen.SPACES_OVERVIEW)

            // Navigation depuis les écrans admin vers home
            ArkaScreen.ADMIN_PANEL,
            ArkaScreen.AUDIT,
            ArkaScreen.STATISTICS -> navigateTo(ArkaScreen.HOME)

            // Navigation par défaut vers home
            else -> navigateTo(ArkaScreen.HOME)
        }
    }

    private fun updateBreadcrumb(screen: ArkaScreen) {
        breadcrumb = when (screen) {
            ArkaScreen.LOGIN -> emptyList()
            ArkaScreen.HOME -> listOf("Accueil")
            ArkaScreen.SETTINGS -> listOf("Accueil", "Paramètres")

            // Administration
            ArkaScreen.ADMIN_PANEL -> listOf("Accueil", "Administration")
            ArkaScreen.AUDIT -> listOf("Accueil", "Administration", "Audit")
            ArkaScreen.STATISTICS -> listOf("Accueil", "Administration", "Statistiques")

            // Fichiers
            ArkaScreen.FILES -> listOf("Accueil", "Fichiers")
            ArkaScreen.FILES_DETAILS -> listOf("Accueil", "Fichiers", "Détails")

            // Espaces
            ArkaScreen.SPACES_OVERVIEW -> listOf("Accueil", "Espaces")
            ArkaScreen.SPACE_DETAILS -> listOf("Accueil", "Espaces", "Détails")
            ArkaScreen.SPACE_PERMISSIONS -> listOf("Accueil", "Espaces", "Permissions")
            ArkaScreen.SPACE_SETTINGS -> listOf("Accueil", "Espaces", "Paramètres")

            // Autres écrans
            else -> listOf("Accueil", screen.title)
        }
    }
}

/**
 * Composant principal de routage Arka
 */
@Composable
fun ArkaRouter(
    navigationState: ArkaNavigationState,
    isDatabaseConnected: Boolean
) {
    // Injection des controllers via Koin
    val authController: AuthController by inject(AuthController::class.java)
    val familyController: FamilyController by inject(FamilyController::class.java)
    val familyMemberController: FamilyMemberController by inject(FamilyMemberController::class.java)

    // Controllers conditionnels (seulement si base de données connectée)
    val alertController: AlertController? = if (isDatabaseConnected) {
        try { inject<AlertController>(AlertController::class.java).value } catch (e: Exception) { null }
    } else null

    val delegationController: DelegationController? = if (isDatabaseConnected) {
        try { inject<DelegationController>(DelegationController::class.java).value } catch (e: Exception) { null }
    } else null

    val permissionController: PermissionController? = if (isDatabaseConnected) {
        try { inject<PermissionController>(PermissionController::class.java).value } catch (e: Exception) { null }
    } else null

    val auditController: JournalAuditPermissionController? = if (isDatabaseConnected) {
        try { inject<JournalAuditPermissionController>(JournalAuditPermissionController::class.java).value } catch (e: Exception) { null }
    } else null

    // Navigation vers les écrans
    when (navigationState.currentScreen) {
        ArkaScreen.LOGIN -> {
            LoginScreen(
                authController = authController,
                onLoginSuccess = {
                    navigationState.navigateTo(ArkaScreen.HOME)
                }
            )
        }

        ArkaScreen.HOME -> {
            HomeScreen(
                authController = authController,
                onNavigateToSettings = { navigationState.navigateTo(ArkaScreen.SETTINGS) },
                onNavigateToAdmin = { navigationState.navigateTo(ArkaScreen.ADMIN_PANEL) },
                onNavigateToFiles = { navigationState.navigateTo(ArkaScreen.FILES) },
                onNavigateToSpaces = { navigationState.navigateTo(ArkaScreen.SPACES_OVERVIEW) },
                onNavigateToNotifications = { navigationState.navigateTo(ArkaScreen.NOTIFICATIONS) },
                onNavigateToPermissions = { navigationState.navigateTo(ArkaScreen.PERMISSIONS_OVERVIEW) },
                onNavigateToFamilyMembers = { navigationState.navigateTo(ArkaScreen.FAMILY_MEMBERS) },
                onNavigateToCategories = { navigationState.navigateTo(ArkaScreen.CATEGORIES) },
                onNavigateToFolders = { navigationState.navigateTo(ArkaScreen.FOLDERS) },
                onNavigateToDelegations = { navigationState.navigateTo(ArkaScreen.DELEGATIONS) }
            )
        }

        ArkaScreen.SETTINGS -> {
            SettingsScreen(
                authController = authController,
                familyMemberController = familyMemberController,
                familyController = familyController,
                onNavigateBack = { navigationState.navigateBack() },
                onLogout = { navigationState.navigateTo(ArkaScreen.LOGIN) }
            )
        }

        // === ÉCRANS ADMIN ===
        ArkaScreen.ADMIN_PANEL -> {
            AdminPanelScreen(
                authController = authController,
                familyController = familyController,
                familyMemberController = familyMemberController,
                fileController = null, // À créer plus tard
                delegationController = delegationController,
                alertController = alertController,
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToUsers = { navigationState.navigateTo(ArkaScreen.FAMILY_MEMBERS) },
                onNavigateToStatistics = { navigationState.navigateTo(ArkaScreen.STATISTICS) },
                onNavigateToAudit = { navigationState.navigateTo(ArkaScreen.AUDIT) },
                onNavigateToSettings = { navigationState.navigateTo(ArkaScreen.SETTINGS) }
            )
        }

        ArkaScreen.AUDIT -> {
            if (auditController != null) {
                AuditScreen(
                    auditController = auditController,
                    authController = authController,
                    onNavigateBack = { navigationState.navigateBack() }
                )
            } else {
                ErrorScreen("Contrôleur d'audit non disponible") {
                    navigationState.navigateBack()
                }
            }
        }

        ArkaScreen.STATISTICS -> {
            StatisticsScreen(
                authController = authController,
                familyController = familyController,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }

        // === NOTIFICATIONS ===
        ArkaScreen.NOTIFICATIONS -> {
            if (alertController != null && delegationController != null) {
                NotificationsScreen(
                    alertController = alertController,
                    delegationController = delegationController,
                    authController = authController,
                    onNavigateBack = { navigationState.navigateBack() },
                    onNavigateToPermissionRequest = { id ->
                        navigationState.navigateTo(ArkaScreen.REQUEST_PERMISSION, id)
                    },
                    onNavigateToSpace = { id ->
                        navigationState.navigateTo(ArkaScreen.SPACE_DETAILS, id)
                    }
                )
            } else {
                ErrorScreen("Contrôleurs de notifications non disponibles") {
                    navigationState.navigateBack()
                }
            }
        }

        // === GESTION CONTENU ===
        ArkaScreen.CATEGORIES -> {
            CategoriesScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToCategory = { id ->
                    navigationState.navigateTo(ArkaScreen.FOLDERS, id)
                }
            )
        }

        ArkaScreen.FILES -> {
            FilesScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToFileDetails = { id ->
                    navigationState.navigateTo(ArkaScreen.FILES_DETAILS, id)
                }
            )
        }

        ArkaScreen.FILES_DETAILS -> {
            FilesDetailsScreen(
                fileId = navigationState.selectedItemId ?: 0,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }

        ArkaScreen.FOLDERS -> {
            FoldersScreen(
                categoryId = navigationState.selectedItemId,
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToFolder = { id ->
                    navigationState.navigateTo(ArkaScreen.FILES, id)
                }
            )
        }

        // === FAMILLE ===
        ArkaScreen.FAMILY_MEMBERS -> {
            FamilyMembersScreen(
                familyMemberController = familyMemberController,
                authController = authController,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }

        // === ESPACES ===
        ArkaScreen.SPACES_OVERVIEW -> {
            SpaceOverviewScreen(
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToSpaceDetails = { id ->
                    navigationState.navigateTo(ArkaScreen.SPACE_DETAILS, id)
                }
            )
        }

        ArkaScreen.SPACE_DETAILS -> {
            SpaceDetailsScreen(
                spaceId = navigationState.selectedItemId ?: 0,
                onNavigateBack = { navigationState.navigateBack() },
                onNavigateToPermissions = { id ->
                    navigationState.navigateTo(ArkaScreen.SPACE_PERMISSIONS, id)
                },
                onNavigateToSettings = { id ->
                    navigationState.navigateTo(ArkaScreen.SPACE_SETTINGS, id)
                }
            )
        }

        ArkaScreen.SPACE_PERMISSIONS -> {
            SpacePermissionScreen(
                spaceId = navigationState.selectedItemId ?: 0,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }

        ArkaScreen.SPACE_SETTINGS -> {
            SpaceSettingsScreen(
                spaceId = navigationState.selectedItemId ?: 0,
                onNavigateBack = { navigationState.navigateBack() }
            )
        }

        // === PERMISSIONS ===
        ArkaScreen.PERMISSIONS_OVERVIEW -> {
            if (permissionController != null) {
                PermissionOverviewScreen(
                    permissionController = permissionController,
                    authController = authController,
                    onNavigateBack = { navigationState.navigateBack() },
                    onNavigateToRequest = {
                        navigationState.navigateTo(ArkaScreen.REQUEST_PERMISSION)
                    }
                )
            } else {
                ErrorScreen("Contrôleur de permissions non disponible") {
                    navigationState.navigateBack()
                }
            }
        }

        ArkaScreen.REQUEST_PERMISSION -> {
            if (delegationController != null) {
                RequestPermissionScreen(
                    delegationController = delegationController,
                    authController = authController,
                    permissionRequestId = navigationState.selectedItemId,
                    onNavigateBack = { navigationState.navigateBack() },
                    onRequestSent = {
                        navigationState.navigateTo(ArkaScreen.PERMISSIONS_OVERVIEW)
                    }
                )
            } else {
                ErrorScreen("Contrôleur de délégations non disponible") {
                    navigationState.navigateBack()
                }
            }
        }

        // === DÉLÉGATIONS ===
        ArkaScreen.DELEGATIONS -> {
            if (delegationController != null) {
                DelegationsScreen(
                    delegationController = delegationController,
                    authController = authController,
                    onNavigateBack = { navigationState.navigateBack() },
                    onNavigateToRequest = { id ->
                        navigationState.navigateTo(ArkaScreen.REQUEST_PERMISSION, id)
                    }
                )
            } else {
                ErrorScreen("Contrôleur de délégations non disponible") {
                    navigationState.navigateBack()
                }
            }
        }
    }
}

/**
 * Écran d'erreur générique
 */
@Composable
private fun ErrorScreen(
    message: String,
    onNavigateBack: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material.Text(
            text = message,
            style = androidx.compose.material.MaterialTheme.typography.h6
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))

        androidx.compose.material.Button(onClick = onNavigateBack) {
            androidx.compose.material.Text("Retour")
        }
    }
}