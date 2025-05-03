import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Window
import controllers.AuthController
import controllers.CategoryController
import controllers.FileController
import controllers.FolderController
import ktorm.DatabaseManager
import org.ktorm.database.Database
import org.ktorm.database.asIterable
import routing.Router
import routing.Routes
import ui.CategoryScreen
import ui.FamilyMemberScreen
import ui.FolderScreen
import ui.HomeScreen
import ui.LoginScreen
import ui.theme.ArkaTheme
import utils.PasswordHasher

@Composable
@Preview
fun AppWithDatabase(database: Database) {
    val router = remember { Router() }
    val authController = remember { AuthController() }
    val folderController = remember { FolderController() }
    val fileController = remember { FileController() }
    val categoryController = remember { CategoryController() }

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
                        // D'abord, récupérer l'utilisateur par son email
                        val sql = "SELECT FamilyMemberID, FamilyMemberPassword FROM FamilyMember WHERE FamilyMemberMail = ?"
                        var userId: Int? = null
                        var hashedPassword: String? = null

                        database.useConnection { connection ->
                            connection.prepareStatement(sql).use { statement ->
                                statement.setString(1, email)

                                val resultSet = statement.executeQuery()
                                if (resultSet.next()) {
                                    userId = resultSet.getInt("FamilyMemberID")
                                    hashedPassword = resultSet.getString("FamilyMemberPassword")
                                }
                            }
                        }

                        // Ensuite, vérifier le mot de passe avec bcrypt
                        if (userId != null && hashedPassword != null &&
                            PasswordHasher.checkPassword(password, hashedPassword)) {
                            println("Authentification réussie avec ID: $userId, navigation vers HOME")
                            currentUserId = userId
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
    // Configuration de la base de données
    val database = Database.connect(
        url = "jdbc:mysql://localhost:3306/Arka",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = null
    )

    // Initialiser le gestionnaire de base de données
    // Utilisez le nom correct de votre méthode (setDatabase ou autre)
    try {
        DatabaseManager.getInstance() // Cela initialisera la base de données si nécessaire
        println("Base de données initialisée avec succès dans le gestionnaire")
    } catch (e: Exception) {
        println("Erreur lors de l'initialisation du gestionnaire de base de données: ${e.message}")
    }

    // Hacher les mots de passe au premier démarrage (à décommenter si nécessaire)
    PasswordHasher.encryptAllPasswords(database)

    // Test de connexion
    try {
        database.useConnection { connection ->
            val sql = "SELECT 1"
            connection.prepareStatement(sql).use { statement ->
                statement.executeQuery().asIterable().map {
                    println("Connexion à la BDD réussie : " + it.getString(1))
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
        AppWithDatabase(database)
    }
}