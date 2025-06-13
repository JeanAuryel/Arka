package ktorm

import org.ktorm.database.Database
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import java.sql.DriverManager

/**
 * Configuration de la base de données Arka
 * MySQL + Ktorm ORM
 */
object ArkaDatabase {

    // Configuration par défaut
    private const val DEFAULT_HOST = "localhost"
    private const val DEFAULT_PORT = 3306
    private const val DEFAULT_DATABASE = "arka"
    private const val DEFAULT_USERNAME = "root"
    private const val DEFAULT_PASSWORD = ""

    // Instance de la base de données
    lateinit var instance: Database
        private set

    /**
     * Initialise la connexion à la base de données
     */
    fun initialize(
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        databaseName: String = DEFAULT_DATABASE,
        username: String = DEFAULT_USERNAME,
        password: String = DEFAULT_PASSWORD,
        enableLogging: Boolean = true
    ) {
        try {
            // Construction de l'URL JDBC
            val jdbcUrl = "jdbc:mysql://$host:$port/$databaseName" +
                    "?useSSL=false" +
                    "&serverTimezone=UTC" +
                    "&allowPublicKeyRetrieval=true" +
                    "&useUnicode=true" +
                    "&characterEncoding=UTF-8"

            println("🔗 Connexion à la base de données Arka...")
            println("📍 URL: $host:$port/$databaseName")

            // Test de connexion JDBC direct
            DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
                println("✅ Test de connexion JDBC réussi")
                println("📊 Driver: ${connection.metaData.driverName}")
                println("🔖 Version: ${connection.metaData.databaseProductVersion}")
            }

            // Configuration Ktorm
            instance = if (enableLogging) {
                Database.connect(
                    url = jdbcUrl,
                    driver = "com.mysql.cj.jdbc.Driver",
                    user = username,
                    password = password,
                    logger = ConsoleLogger(threshold = LogLevel.INFO)
                )
            } else {
                Database.connect(
                    url = jdbcUrl,
                    driver = "com.mysql.cj.jdbc.Driver",
                    user = username,
                    password = password
                )
            }

            println("🎯 Base de données Arka initialisée avec succès!")

        } catch (e: Exception) {
            println("❌ Erreur lors de l'initialisation de la base de données:")
            println("   ${e.message}")
            throw ArkaDataBaseException("Impossible de se connecter à la base de données", e)
        }
    }

    /**
     * Vérifie la connexion à la base de données
     */
    fun checkConnection(): Boolean {
        return try {
            instance.useConnection { connection ->
                val result = connection.prepareStatement("SELECT 1").executeQuery()
                result.next() && result.getInt(1) == 1
            }
        } catch (e: Exception) {
            println("⚠️ Problème de connexion à la base de données: ${e.message}")
            false
        }
    }

    /**
     * Ferme la connexion à la base de données
     */
    fun close() {
        if (::instance.isInitialized) {
            try {
                // Ktorm ne nécessite pas de fermeture explicite du pool de connexions
                println("📴 Connexion à la base de données fermée")
            } catch (e: Exception) {
                println("⚠️ Erreur lors de la fermeture: ${e.message}")
            }
        }
    }

    /**
     * Obtient des informations sur la base de données
     */
    fun getDatabaseInfo(): DatabaseInfo {
        return instance.useConnection { connection ->
            val metaData = connection.metaData
            DatabaseInfo(
                productName = metaData.databaseProductName,
                productVersion = metaData.databaseProductVersion,
                driverName = metaData.driverName,
                driverVersion = metaData.driverVersion,
                url = metaData.url,
                userName = metaData.userName
            )
        }
    }
}

/**
 * Informations sur la base de données
 */
data class DatabaseInfo(
    val productName: String,
    val productVersion: String,
    val driverName: String,
    val driverVersion: String,
    val url: String,
    val userName: String
)

/**
 * Exception personnalisée pour les erreurs de base de données Arka
 */
class ArkaDataBaseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)