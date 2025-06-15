// ================================================================
// ARKANAVIGATION.KT - SYSTÈME DE NAVIGATION PRINCIPAL ARKA
// ================================================================

package ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import services.NavigationService
import ui.components.*

/**
 * Composant principal de navigation pour Arka
 * Gère la navigation entre tous les écrans de l'application
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ArkaNavigation(
    modifier: Modifier = Modifier,
    navigationService: NavigationService,
    startDestination: String = NavigationRoutes.LOGIN,
    showNavigationUI: Boolean = true
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // État de navigation synchronisé avec le service
    val navigationState by navigationService.navigationState.collectAsState()
    val navigationParams by navigationService.navigationParams.collectAsState()

    // État UI
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)

    // Configuration des éléments de navigation
    val navigationItems = remember(navigationState) {
        ArkaNavigationItems.getMainNavigation(
            currentState = navigationState,
            onNavigate = { destination ->
                navigationService.navigateTo(destination)
                navController.navigate(destination.getDefaultRoute())
            }
        )
    }

    // Layout principal avec navigation
    Box(modifier = modifier.fillMaxSize()) {
        if (showNavigationUI && shouldShowNavigation(navigationState)) {
            // Layout avec navigation latérale pour desktop
            Row(modifier = Modifier.fillMaxSize()) {
                // Navigation latérale
                ArkaSideNavigation(
                    items = navigationItems,
                    modifier = Modifier.width(280.dp),
                    header = {
                        ArkaNavigationHeader(
                            currentUser = getCurrentUser(),
                            onProfileClick = {
                                navigationService.navigateTo(NavigationState.USER_PROFILE)
                                navController.navigate(NavigationRoutes.Profile.BASE)
                            }
                        )
                    },
                    footer = {
                        ArkaNavigationFooter(
                            onSettingsClick = {
                                navigationService.navigateTo(NavigationState.SETTINGS)
                                navController.navigate(NavigationRoutes.Settings.BASE)
                            }
                        )
                    }
                )

                // Contenu principal
                Column(modifier = Modifier.weight(1f)) {
                    // Barre de navigation supérieure
                    ArkaTopAppBar(
                        title = getPageTitle(navigationState, navigationParams),
                        subtitle = getPageSubtitle(navigationState, navigationParams),
                        navigationIcon = if (navigationService.canNavigateBack()) {
                            androidx.compose.material.icons.Icons.Default.ArrowBack
                        } else null,
                        onNavigationClick = if (navigationService.canNavigateBack()) {
                            {
                                navigationService.navigateBack()
                                navController.popBackStack()
                            }
                        } else null,
                        actions = {
                            ArkaTopBarActions(
                                currentState = navigationState,
                                onAction = { action ->
                                    handleTopBarAction(action, navigationService, navController)
                                }
                            )
                        }
                    )

                    // Fil d'Ariane
                    if (shouldShowBreadcrumb(navigationState)) {
                        ArkaBreadcrumb(
                            items = getBreadcrumbItems(navigationService, navController),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Contenu de navigation
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.weight(1f),
                        enterTransition = { slideInHorizontally(tween(300)) { it } },
                        exitTransition = { slideOutHorizontally(tween(300)) { -it } }
                    ) {
                        setupNavigationGraph(navController, navigationService)
                    }
                }
            }
        } else {
            // Layout plein écran (login, setup, erreurs)
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                setupNavigationGraph(navController, navigationService)
            }
        }
    }

    // Synchronisation entre NavController et NavigationService
    LaunchedEffect(navigationState) {
        val route = navigationState.getDefaultRoute()
        if (currentRoute != route) {
            navController.navigate(route) {
                // Configuration de navigation
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}

/**
 * Configuration du graphe de navigation
 */
@ExperimentalAnimationApi
private fun androidx.navigation.NavGraphBuilder.setupNavigationGraph(
    navController: NavHostController,
    navigationService: NavigationService
) {
    // ================================================================
    // AUTHENTIFICATION
    // ================================================================

    composable(
        route = NavigationRoutes.LOGIN,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) }
    ) {
        LoginScreen(
            onLoginSuccess = {
                navigationService.navigateTo(NavigationState.DASHBOARD)
                navController.navigate(NavigationRoutes.DASHBOARD) {
                    popUpTo(NavigationRoutes.LOGIN) { inclusive = true }
                }
            },
            onSetupRequired = {
                navigationService.navigateTo(NavigationState.SETUP)
                navController.navigate(NavigationRoutes.SETUP)
            }
        )
    }

    composable(NavigationRoutes.SETUP) {
        SetupScreen(
            onSetupComplete = {
                navigationService.navigateTo(NavigationState.DASHBOARD)
                navController.navigate(NavigationRoutes.DASHBOARD) {
                    popUpTo(NavigationRoutes.SETUP) { inclusive = true }
                }
            }
        )
    }

    // ================================================================
    // PAGES PRINCIPALES
    // ================================================================

    composable(NavigationRoutes.DASHBOARD) {
        DashboardScreen(
            onNavigateToFiles = {
                navigationService.navigateTo(NavigationState.FILES)
                navController.navigate(NavigationRoutes.Files.LIST)
            },
            onNavigateToFamily = {
                navigationService.navigateTo(NavigationState.FAMILY_MEMBERS)
                navController.navigate(NavigationRoutes.Family.LIST)
            },
            onNavigateToDelegations = {
                navigationService.navigateTo(NavigationState.DELEGATIONS)
                navController.navigate(NavigationRoutes.Delegations.LIST)
            }
        )
    }

    // ================================================================
    // GESTION DES FICHIERS
    // ================================================================

    composable(NavigationRoutes.Files.LIST) {
        FilesListScreen(
            onFileClick = { fileId ->
                navigationService.navigateToFile(fileId)
                navController.navigate(NavigationRoutes.Files.details(fileId))
            },
            onFolderClick = { folderId ->
                navigationService.navigateToFolder(folderId)
                navController.navigate(NavigationRoutes.Folders.details(folderId))
            },
            onUploadFiles = {
                navigationService.navigateTo(NavigationState.FILE_UPLOAD)
                navController.navigate(NavigationRoutes.Files.UPLOAD)
            }
        )
    }

    composable(NavigationRoutes.Files.DETAILS) { backStackEntry ->
        val fileId = backStackEntry.arguments?.getString("fileId")?.toIntOrNull()
        if (fileId != null) {
            FileDetailsScreen(
                fileId = fileId,
                onBack = {
                    navigationService.navigateBack()
                    navController.popBackStack()
                },
                onEditFile = {
                    // Naviguer vers l'éditeur
                }
            )
        } else {
            ErrorScreen(message = "Fichier non trouvé")
        }
    }

    composable(NavigationRoutes.Files.UPLOAD) {
        FileUploadScreen(
            onUploadComplete = {
                navigationService.navigateTo(NavigationState.FILES)
                navController.navigate(NavigationRoutes.Files.LIST) {
                    popUpTo(NavigationRoutes.Files.UPLOAD) { inclusive = true }
                }
            },
            onCancel = {
                navigationService.navigateBack()
                navController.popBackStack()
            }
        )
    }

    // ================================================================
    // GESTION DES DOSSIERS
    // ================================================================

    composable(NavigationRoutes.Folders.DETAILS) { backStackEntry ->
        val folderId = backStackEntry.arguments?.getString("folderId")?.toIntOrNull()
        if (folderId != null) {
            FolderDetailsScreen(
                folderId = folderId,
                onFileClick = { fileId ->
                    navigationService.navigateToFile(fileId, folderId)
                    navController.navigate(NavigationRoutes.Files.details(fileId))
                },
                onCreateSubfolder = {
                    navigationService.navigateTo(NavigationState.CREATE_FOLDER, mapOf("parentId" to folderId))
                    navController.navigate(NavigationRoutes.Folders.CREATE + "?parentId=$folderId")
                }
            )
        } else {
            ErrorScreen(message = "Dossier non trouvé")
        }
    }

    composable(NavigationRoutes.Folders.CREATE) {
        CreateFolderScreen(
            onFolderCreated = { folderId ->
                navigationService.navigateToFolder(folderId)
                navController.navigate(NavigationRoutes.Folders.details(folderId)) {
                    popUpTo(NavigationRoutes.Folders.CREATE) { inclusive = true }
                }
            },
            onCancel = {
                navigationService.navigateBack()
                navController.popBackStack()
            }
        )
    }

    // ================================================================
    // GESTION DES CATÉGORIES
    // ================================================================

    composable(NavigationRoutes.Categories.LIST) {
        CategoriesListScreen(
            onCategoryClick = { categoryId ->
                navigationService.navigateTo(NavigationState.CATEGORY_DETAILS, mapOf("categoryId" to categoryId))
                navController.navigate(NavigationRoutes.Categories.details(categoryId))
            },
            onCreateCategory = {
                navigationService.navigateTo(NavigationState.CREATE_CATEGORY)
                navController.navigate(NavigationRoutes.Categories.CREATE)
            }
        )
    }

    composable(NavigationRoutes.Categories.DETAILS) { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId")?.toIntOrNull()
        if (categoryId != null) {
            CategoryDetailsScreen(
                categoryId = categoryId,
                onFileClick = { fileId ->
                    navigationService.navigateToFile(fileId)
                    navController.navigate(NavigationRoutes.Files.details(fileId))
                }
            )
        } else {
            ErrorScreen(message = "Catégorie non trouvée")
        }
    }

    // ================================================================
    // GESTION FAMILIALE
    // ================================================================

    composable(NavigationRoutes.Family.LIST) {
        FamilyMembersScreen(
            onMemberClick = { memberId ->
                navigationService.navigateTo(NavigationState.MEMBER_DETAILS, mapOf("memberId" to memberId))
                navController.navigate(NavigationRoutes.Family.memberDetails(memberId))
            },
            onInviteMember = {
                navigationService.navigateTo(NavigationState.INVITE_MEMBER)
                navController.navigate(NavigationRoutes.Family.INVITE)
            }
        )
    }

    composable(NavigationRoutes.Family.MEMBER_DETAILS) { backStackEntry ->
        val memberId = backStackEntry.arguments?.getString("memberId")?.toIntOrNull()
        if (memberId != null) {
            MemberDetailsScreen(
                memberId = memberId,
                onEditPermissions = {
                    navController.navigate(NavigationRoutes.Family.memberPermissions(memberId))
                }
            )
        } else {
            ErrorScreen(message = "Membre non trouvé")
        }
    }

    // ================================================================
    // DÉLÉGATIONS
    // ================================================================

    composable(NavigationRoutes.Delegations.LIST) {
        DelegationsScreen(
            onDelegationClick = { delegationId ->
                navigationService.navigateTo(NavigationState.DELEGATION_DETAILS, mapOf("delegationId" to delegationId))
                navController.navigate(NavigationRoutes.Delegations.details(delegationId))
            },
            onCreateDelegation = {
                navController.navigate(NavigationRoutes.Delegations.CREATE)
            }
        )
    }

    // ================================================================
    // PARAMÈTRES ET PROFIL
    // ================================================================

    composable(NavigationRoutes.Settings.BASE) {
        SettingsScreen(
            onNavigateToSecurity = {
                navController.navigate(NavigationRoutes.Settings.SECURITY)
            },
            onNavigateToLogs = {
                navigationService.navigateTo(NavigationState.SYSTEM_LOGS)
                navController.navigate(NavigationRoutes.Settings.LOGS)
            }
        )
    }

    composable(NavigationRoutes.Profile.BASE) {
        ProfileScreen(
            onEditProfile = {
                navController.navigate(NavigationRoutes.Profile.EDIT)
            }
        )
    }

    // ================================================================
    // SYNCHRONISATION MOBILE
    // ================================================================

    composable(NavigationRoutes.Mobile.SYNC) {
        MobileSyncScreen(
            onViewDevices = {
                navigationService.navigateTo(NavigationState.MOBILE_DEVICES)
                navController.navigate(NavigationRoutes.Mobile.DEVICES)
            }
        )
    }

    composable(NavigationRoutes.Mobile.DEVICES) {
        MobileDevicesScreen(
            onDeviceClick = { deviceId ->
                navController.navigate(NavigationRoutes.Mobile.deviceDetails(deviceId))
            }
        )
    }

    // ================================================================
    // GESTION D'ERREURS
    // ================================================================

    composable(NavigationRoutes.Error.NOT_FOUND) {
        ErrorScreen(
            title = "Page non trouvée",
            message = "La page que vous cherchez n'existe pas.",
            onReturnHome = {
                navigationService.navigateTo(NavigationState.DASHBOARD)
                navController.navigate(NavigationRoutes.DASHBOARD) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    composable(NavigationRoutes.Error.ACCESS_DENIED) {
        ErrorScreen(
            title = "Accès refusé",
            message = "Vous n'avez pas les permissions pour accéder à cette page.",
            onReturnHome = {
                navigationService.navigateTo(NavigationState.DASHBOARD)
                navController.navigate(NavigationRoutes.DASHBOARD) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}

// ================================================================
// COMPOSANTS HELPER
// ================================================================

/**
 * En-tête de navigation avec profil utilisateur
 */
@Composable
private fun ArkaNavigationHeader(
    currentUser: Any?, // À remplacer par le type User approprié
    onProfileClick: () -> Unit
) {
    Column {
        // Logo Arka
        Text(
            text = "ARKA",
            style = ArkaTextStyles.logo,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Profil utilisateur
        if (currentUser != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                UserAvatar(
                    name = "Utilisateur", // currentUser.name
                    size = 32.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Utilisateur", // currentUser.name
                        style = ArkaTextStyles.navigation
                    )
                    Text(
                        text = "Admin", // currentUser.role.displayName
                        style = ArkaTextStyles.helpText
                    )
                }

                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = androidx.compose.ui.Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Pied de page de navigation
 */
@Composable
private fun ArkaNavigationFooter(
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = "Version 1.0.0",
            style = ArkaTextStyles.helpText
        )

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                contentDescription = "Paramètres"
            )
        }
    }
}

/**
 * Actions de la barre supérieure
 */
@Composable
private fun ArkaTopBarActions(
    currentState: NavigationState,
    onAction: (String) -> Unit
) {
    when (currentState) {
        NavigationState.FILES -> {
            IconButton(onClick = { onAction("upload") }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.CloudUpload,
                    contentDescription = "Téléverser"
                )
            }
            IconButton(onClick = { onAction("search") }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Search,
                    contentDescription = "Rechercher"
                )
            }
        }
        NavigationState.FAMILY_MEMBERS -> {
            IconButton(onClick = { onAction("invite") }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.PersonAdd,
                    contentDescription = "Inviter"
                )
            }
        }
        NavigationState.CATEGORIES -> {
            IconButton(onClick = { onAction("create") }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "Créer"
                )
            }
        }
        else -> {
            // Pas d'actions spécifiques
        }
    }
}

// ================================================================
// FONCTIONS UTILITAIRES
// ================================================================

private fun shouldShowNavigation(state: NavigationState): Boolean {
    return when (state) {
        NavigationState.LOGIN,
        NavigationState.SETUP,
        NavigationState.ERROR_404,
        NavigationState.ERROR_403,
        NavigationState.ERROR_500 -> false
        else -> true
    }
}

private fun shouldShowBreadcrumb(state: NavigationState): Boolean {
    return when (state) {
        NavigationState.DASHBOARD,
        NavigationState.LOGIN,
        NavigationState.SETUP -> false
        else -> true
    }
}

private fun getPageTitle(state: NavigationState, params: Map<String, Any>): String {
    return when (state) {
        NavigationState.FILE_DETAILS -> params["fileName"] as? String ?: "Détails du fichier"
        NavigationState.FOLDER_DETAILS -> params["folderName"] as? String ?: "Détails du dossier"
        NavigationState.CATEGORY_DETAILS -> params["categoryName"] as? String ?: "Détails de la catégorie"
        NavigationState.MEMBER_DETAILS -> params["memberName"] as? String ?: "Profil membre"
        else -> state.title
    }
}

private fun getPageSubtitle(state: NavigationState, params: Map<String, Any>): String? {
    return when (state) {
        NavigationState.FILE_DETAILS -> params["folderName"] as? String
        NavigationState.FOLDER_DETAILS -> params["categoryName"] as? String
        else -> null
    }
}

private fun getBreadcrumbItems(
    navigationService: NavigationService,
    navController: NavHostController
): List<BreadcrumbItem> {
    return navigationService.getBreadcrumb().map { state ->
        BreadcrumbItem(
            text = state.title,
            onClick = {
                navigationService.navigateTo(state)
                navController.navigate(state.getDefaultRoute())
            }
        )
    }
}

private fun handleTopBarAction(
    action: String,
    navigationService: NavigationService,
    navController: NavHostController
) {
    when (action) {
        "upload" -> {
            navigationService.navigateTo(NavigationState.FILE_UPLOAD)
            navController.navigate(NavigationRoutes.Files.UPLOAD)
        }
        "search" -> {
            navController.navigate(NavigationRoutes.Files.SEARCH)
        }
        "invite" -> {
            navigationService.navigateTo(NavigationState.INVITE_MEMBER)
            navController.navigate(NavigationRoutes.Family.INVITE)
        }
        "create" -> {
            navigationService.navigateTo(NavigationState.CREATE_CATEGORY)
            navController.navigate(NavigationRoutes.Categories.CREATE)
        }
    }
}

private fun getCurrentUser(): Any? {
    // À implémenter avec le service d'authentification
    return null
}

// ================================================================
// EXTENSION POUR ArkaNavigationItems
// ================================================================

object ArkaNavigationItems {
    fun getMainNavigation(
        currentState: NavigationState,
        onNavigate: (NavigationState) -> Unit
    ): List<NavigationItem> {
        return listOf(
            home(currentState == NavigationState.DASHBOARD) { onNavigate(NavigationState.DASHBOARD) },
            files(currentState == NavigationState.FILES) { onNavigate(NavigationState.FILES) },
            family(currentState == NavigationState.FAMILY_MEMBERS) { onNavigate(NavigationState.FAMILY_MEMBERS) },
            permissions(currentState == NavigationState.DELEGATIONS) { onNavigate(NavigationState.DELEGATIONS) },
            settings(currentState == NavigationState.SETTINGS) { onNavigate(NavigationState.SETTINGS) }
        )
    }
}

// ================================================================
// PLACEHOLDER SCREENS (À REMPLACER PAR LES VRAIS ÉCRANS)
// ================================================================

@Composable private fun LoginScreen(onLoginSuccess: () -> Unit, onSetupRequired: () -> Unit) {
    // Placeholder
    Column { Text("Login Screen") }
}

@Composable private fun SetupScreen(onSetupComplete: () -> Unit) {
    Column { Text("Setup Screen") }
}

@Composable private fun DashboardScreen(
    onNavigateToFiles: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToDelegations: () -> Unit
) {
    Column { Text("Dashboard Screen") }
}

@Composable private fun FilesListScreen(
    onFileClick: (Int) -> Unit,
    onFolderClick: (Int) -> Unit,
    onUploadFiles: () -> Unit
) {
    Column { Text("Files List Screen") }
}

@Composable private fun FileDetailsScreen(
    fileId: Int,
    onBack: () -> Unit,
    onEditFile: () -> Unit
) {
    Column { Text("File Details Screen - ID: $fileId") }
}

@Composable private fun FileUploadScreen(
    onUploadComplete: () -> Unit,
    onCancel: () -> Unit
) {
    Column { Text("File Upload Screen") }
}

@Composable private fun FolderDetailsScreen(
    folderId: Int,
    onFileClick: (Int) -> Unit,
    onCreateSubfolder: () -> Unit
) {
    Column { Text("Folder Details Screen - ID: $folderId") }
}

@Composable private fun CreateFolderScreen(
    onFolderCreated: (Int) -> Unit,
    onCancel: () -> Unit
) {
    Column { Text("Create Folder Screen") }
}

@Composable private fun CategoriesListScreen(
    onCategoryClick: (Int) -> Unit,
    onCreateCategory: () -> Unit
) {
    Column { Text("Categories List Screen") }
}

@Composable private fun CategoryDetailsScreen(
    categoryId: Int,
    onFileClick: (Int) -> Unit
) {
    Column { Text("Category Details Screen - ID: $categoryId") }
}

@Composable private fun FamilyMembersScreen(
    onMemberClick: (Int) -> Unit,
    onInviteMember: () -> Unit
) {
    Column { Text("Family Members Screen") }
}

@Composable private fun MemberDetailsScreen(
    memberId: Int,
    onEditPermissions: () -> Unit
) {
    Column { Text("Member Details Screen - ID: $memberId") }
}

@Composable private fun DelegationsScreen(
    onDelegationClick: (Int) -> Unit,
    onCreateDelegation: () -> Unit
) {
    Column { Text("Delegations Screen") }
}

@Composable private fun SettingsScreen(
    onNavigateToSecurity: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    Column { Text("Settings Screen") }
}

@Composable private fun ProfileScreen(
    onEditProfile: () -> Unit
) {
    Column { Text("Profile Screen") }
}

@Composable private fun MobileSyncScreen(
    onViewDevices: () -> Unit
) {
    Column { Text("Mobile Sync Screen") }
}

@Composable private fun MobileDevicesScreen(
    onDeviceClick: (String) -> Unit
) {
    Column { Text("Mobile Devices Screen") }
}

@Composable private fun ErrorScreen(
    title: String = "Erreur",
    message: String = "Une erreur est survenue",
    onReturnHome: (() -> Unit)? = null
) {
    Column {
        Text(title)
        Text(message)
        onReturnHome?.let {
            Button(onClick = it) {
                Text("Retour à l'accueil")
            }
        }
    }
}