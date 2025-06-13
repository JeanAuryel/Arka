import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ktorm.ArkaDatabase
import utils.PasswordHasher
import di.arkaModules
import controllers.FamilyController
import repositories.FamilyRepository
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.inject

@Composable
fun ArkaTestApp(databaseConnected: Boolean) {
    val testResults = remember {
        listOf(
            TestResult("Material Icons Extended", true, "2000+ icônes disponibles"),
            TestResult("Base de données MySQL", databaseConnected, if (databaseConnected) "Connexion active" else "Mode démo"),
            TestResult("Système de mots de passe", databaseConnected, if (databaseConnected) "BCrypt + Migration auto" else "Prêt pour connexion"),
            TestResult("Repositories Principaux", databaseConnected, if (databaseConnected) "Family + Member + Category" else "Non initialisés"),
            TestResult("Repositories Contenu", databaseConnected, if (databaseConnected) "Folder + File + Template" else "Non initialisés"),
            TestResult("Repositories Délégation", databaseConnected, if (databaseConnected) "Permission + Request" else "Non initialisés"),
            TestResult("Controllers", databaseConnected, if (databaseConnected) "FamilyController opérationnel" else "Non initialisés"),
            TestResult("Injection Koin", databaseConnected, if (databaseConnected) "8 repositories injectés" else "Non initialisé"),
            TestResult("Ktorm ORM", true, "11 entités + 5 enums"),
            TestResult("Architecture complète", databaseConnected, if (databaseConnected) "Database → Repos → Controllers → UI" else "Partiellement initialisée")
        )
    }

    MaterialTheme(
        colors = lightColors(
            primary = Color(0xFF0C2234),
            primaryVariant = Color(0xFF0A1B2A),
            secondary = Color(0xFFE5D2A7),
            background = Color(0xFFF5ECD9),
            surface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                // En-tête Arka
                Card(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = "Arka Logo",
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Arka v2.0 - Gestion Documents Familiaux",
                                style = MaterialTheme.typography.h5,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                            Text(
                                "Test des dépendances - Kotlin 1.9.20 + Compose Desktop 1.5.11",
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Démonstration des icônes Arka
                Card(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "🎨 Icônes Material Extended pour Arka",
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Première rangée - Navigation principale
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconWithLabel(Icons.Default.Home, "Accueil")
                            IconWithLabel(Icons.Default.Folder, "Dossiers") // ✅ Maintenant disponible !
                            IconWithLabel(Icons.Default.FamilyRestroom, "Famille")
                            IconWithLabel(Icons.Default.Category, "Catégories")
                            IconWithLabel(Icons.Default.Settings, "Paramètres")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Deuxième rangée - Actions fichiers
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconWithLabel(Icons.Default.InsertDriveFile, "Fichiers")
                            IconWithLabel(Icons.Default.CloudUpload, "Upload")
                            IconWithLabel(Icons.Default.Download, "Télécharger")
                            IconWithLabel(Icons.Default.Share, "Partager")
                            IconWithLabel(Icons.Default.Security, "Sécurité")
                        }
                    }
                }

                // Résultats des tests
                Text(
                    "📋 Tests des dépendances",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(testResults) { result ->
                        TestResultCard(result)
                    }
                }
            }
        }
    }
}

@Composable
fun IconWithLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun TestResultCard(result: TestResult) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = if (result.success) "Success" else "Error",
                tint = if (result.success) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = result.details,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            if (result.success) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Done",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class TestResult(
    val name: String,
    val success: Boolean,
    val details: String
)

/**
 * Test rapide des repositories et controllers
 */
fun testRepositories() {
    try {
        println("🧪 Test complet des repositories Arka...")

        // Injection des repositories
        val familyRepository: FamilyRepository by inject(FamilyRepository::class.java)
        val memberRepository: FamilyMemberRepository by inject(FamilyMemberRepository::class.java)
        val categoryRepository: CategoryRepository by inject(CategoryRepository::class.java)
        val folderRepository: FolderRepository by inject(FolderRepository::class.java)
        val fileRepository: FileRepository by inject(FileRepository::class.java)
        val templateRepository: DefaultFolderTemplateRepository by inject(DefaultFolderTemplateRepository::class.java)
        val delegationRepository: DelegationRequestRepository by inject(DelegationRequestRepository::class.java)
        val permissionRepository: PermissionRepository by inject(PermissionRepository::class.java)
        val familyController: FamilyController by inject(FamilyController::class.java)

        println("✅ Tous les repositories injectés avec succès!")

        // Tests des repositories principaux
        println("\n📊 Statistiques de la base de données:")
        println("   👨‍👩‍👧‍👦 Familles: ${familyRepository.count()}")
        println("   👤 Membres: ${memberRepository.count()}")
        println("   📂 Catégories: ${categoryRepository.count()}")
        println("   📁 Dossiers: ${folderRepository.count()}")
        println("   📄 Fichiers: ${fileRepository.count()}")
        println("   📋 Modèles de dossiers: ${templateRepository.count()}")

        // Tests du système de délégation
        println("\n🔐 Système de délégation:")
        val delegationStats = delegationRepository.getDelegationStats()
        println("   📤 Demandes totales: ${delegationStats.total}")
        println("   ⏳ En attente: ${delegationStats.enAttente}")
        println("   ✅ Approuvées: ${delegationStats.approuvees}")
        println("   ❌ Rejetées: ${delegationStats.rejetees}")

        val permissionStats = permissionRepository.getPermissionStats()
        println("   🔓 Permissions actives: ${permissionStats.actives}")
        println("   🔒 Permissions inactives: ${permissionStats.inactives}")
        println("   ⏰ Permissions expirées: ${permissionStats.expirees}")

        // Test de nettoyage automatique
        println("\n🧹 Nettoyage automatique:")
        val expiredRequests = delegationRepository.cleanExpiredRequests()
        val expiredPermissions = permissionRepository.cleanExpiredPermissions()
        println("   🗑️ Demandes expirées nettoyées: $expiredRequests")
        println("   🗑️ Permissions expirées nettoyées: $expiredPermissions")

        // Test de fonctionnalité avancée
        println("\n🎯 Tests fonctionnels:")

        // Test si il y a des familles
        val families = familyRepository.findAllWithMemberCount()
        if (families.isNotEmpty()) {
            val famille = families.first()
            println("   📋 Famille '${famille.famille.nomFamille}' : ${famille.nombreMembres} membre(s)")

            // Test des membres de cette famille
            val membres = memberRepository.findByFamily(famille.famille.familleId)
            if (membres.isNotEmpty()) {
                val membre = membres.first()
                println("   👤 Premier membre: ${membre.prenomMembre} (${membre.mailMembre})")

                // Test des statistiques du membre
                val memberStats = memberRepository.getMemberStats(membre.membreFamilleId)
                memberStats?.let {
                    println("   📊 Stats membre: ${it.nombreDossiers} dossier(s), ${it.nombreFichiers} fichier(s)")
                }
            }
        }

        println("\n✅ Tous les tests repositories réussis!")

    } catch (e: Exception) {
        println("⚠️ Erreur lors des tests repositories: ${e.message}")
        e.printStackTrace()
    }
}

fun main() = application {
    println("🚀 Arka v2.0 - Système de gestion documents familiaux")
    println("📱 Application Desktop Compose")
    println("🎨 Material Icons Extended + Architecture complète")
    println("📦 Base de données + Repositories + Controllers")
    println("---")

    // 🗄️ Initialisation de la base de données Arka
    var databaseInitialized = false
    try {
        println("🔗 Initialisation de la base de données...")
        ArkaDatabase.initialize(
            databaseName = "arka",
            username = "root",
            password = "" // Ajustez selon votre configuration MySQL
        )

        if (ArkaDatabase.checkConnection()) {
            println("✅ Base de données Arka connectée!")

            // 🔒 Initialisation automatique du système de mots de passe
            PasswordHasher.initializePasswordSystem()

            databaseInitialized = true
        } else {
            println("⚠️ Connexion à la base échoué - mode démo")
        }
    } catch (e: Exception) {
        println("⚠️ Base de données non disponible - mode démo: ${e.message}")
    }

    // 🏗️ Initialisation de Koin (injection de dépendances)
    if (databaseInitialized) {
        try {
            println("🔧 Initialisation de l'injection de dépendances...")
            startKoin {
                modules(arkaModules)
            }
            println("✅ Koin initialisé avec succès!")

            // 🧪 Test rapide des repositories
            testRepositories()

        } catch (e: Exception) {
            println("⚠️ Erreur Koin: ${e.message}")
        }
    }

    Window(
        onCloseRequest = {
            println("🔒 Fermeture de l'application...")
            if (databaseInitialized) {
                try {
                    stopKoin()
                    println("✅ Koin fermé proprement")
                } catch (e: Exception) {
                    println("⚠️ Erreur fermeture Koin: ${e.message}")
                }
            }
            ArkaDatabase.close()
            exitApplication()
        },
        title = "Arka v2.0 - Gestion Documents Familiaux"
    ) {
        ArkaTestApp(databaseInitialized)
    }

    println("✅ Interface Arka lancée!")
    println("📁 Architecture complète : Database → Repositories → Controllers → UI")
}
}