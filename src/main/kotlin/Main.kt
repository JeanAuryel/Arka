import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import controllers.*
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.ktorm.database.Database
import repositories.*
import routing.Router
import routing.Routes
import ui.*
import ui.theme.ArkaTheme
import utils.PasswordHasher

// Définition du module Koin
val appModule = module {
    // Base de données - Singleton
    single {
        Database.connect(
            url = "jdbc:mysql://localhost:3306/Arka",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = null
        )
    }

    // Repositories
    single { FamilyRepository(get()) }
    single { FamilyMemberRepository(get()) }
    single { FolderRepository(get()) }
    single { FileRepository(get()) }
    single { CategoryRepository(get()) }
    single { DefaultFolderTemplateRepository(get()) }

    // Controllers - avec résolution des dépendances circulaires
    single { AuthController(get()) }
    single { FamilyController(get()) }
    single { FamilyMemberController(get()) }

    // FolderController a une dépendance sur AuthController
    single { FolderController(get(), get(), get(), get()) }

    // FileController a une dépendance sur FolderController
    single { FileController(get(), get(), get()) }
    single { CategoryController(get(), get()) }
    single { MainController(get()) }

    // Router
    single { Router() }
}

@Composable
@Preview
fun AppWithDatabase() {
    // Récupération des dépendances depuis Koin
    val router = org.koin.java.KoinJavaComponent.getKoin().get<Router>()
    val authController = org.koin.java.KoinJavaComponent.getKoin().get<AuthController>()
    val database = org.koin.java.KoinJavaComponent.getKoin().get<Database>()

    var isAuthenticated by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf<Int?>(null) }

    // Pour déboguer la route actuelle
    println("Route actuelle: ${router.currentRoute}")

    ArkaTheme {
        Surface {
            when (router.currentRoute) {
                Routes.LOGIN -> LoginScreen { email, password ->
                    println("Tentative de connexion avec $email et un mot de passe $password")

                    // Tentative d'authentification
                    try {
                        val member = authController.authenticate(email, password)
                        if (member != null) {
                            println("Authentification réussie avec ID: ${member.familyMemberID}, navigation vers HOME")
                            currentUserId = member.familyMemberID
                            isAuthenticated = true
                            router.navigateTo(Routes.HOME)
                        } else {
                            println("Échec d'authentification - Email ou mot de passe incorrect")
                        }
                    } catch (e: Exception) {
                        println("Erreur d'authentification: ${e.message}")
                        e.printStackTrace()

                        // Pour déboguer, naviguer quand même vers HOME
                        println("Navigation forcée vers HOME pour déboguer")
                        currentUserId = 1 // ID temporaire pour test
                        isAuthenticated = true
                        router.navigateTo(Routes.HOME)
                    }
                }
                Routes.HOME -> {
                    println("Affichage de HomeScreen")
                    if (isAuthenticated && currentUserId != null) {
                        HomeScreen(
                            currentUserId = currentUserId!!,
                            onLogout = {
                                isAuthenticated = false
                                currentUserId = null
                                router.navigateTo(Routes.LOGIN)
                            }
                        )
                    } else {
                        // Redirection vers LOGIN si non authentifié
                        LaunchedEffect(Unit) {
                            router.navigateTo(Routes.LOGIN)
                        }
                    }
                }
                // Branches manquantes
                Routes.FOLDERS, Routes.FAMILY_MEMBERS, Routes.CATEGORIES, Routes.SETTINGS -> {
                    // Redirection vers HOME pour l'instant (ces routes seront gérées via HomeScreen)
                    LaunchedEffect(Unit) {
                        router.navigateTo(Routes.HOME)
                    }
                }
            }
        }
    }
}

fun main() = application {
    // Initialisation de Koin
    startKoin {
        modules(appModule)
    }

    // Récupération de la base de données depuis Koin
    val database = org.koin.java.KoinJavaComponent.getKoin().get<Database>()
    val authController = org.koin.java.KoinJavaComponent.getKoin().get<AuthController>()

    // Hacher les mots de passe au premier démarrage (à décommenter si nécessaire)
    PasswordHasher.encryptAllPasswords(database)

    // Test de connexion
    try {
        database.useConnection { connection ->
            val sql = "SELECT 1"
            connection.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        println("Connexion à la BDD réussie : " + resultSet.getString(1))
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Erreur de connexion à la base de données: ${e.message}")
        e.printStackTrace()
    }

    // Configuration de la fenêtre principale
    Window(
        onCloseRequest = ::exitApplication,
        title = "Arka - Gestion de documents familiaux"
    ) {
        AppWithDatabase()
    }
}