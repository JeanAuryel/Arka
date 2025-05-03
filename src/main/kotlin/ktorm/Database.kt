package ktorm

import org.ktorm.database.Database

/**
 * Classe responsable de la gestion de la connexion à la base de données
 */
object DatabaseManager {

    private var instance: Database? = null

    /**
     * Obtient une instance de la connexion à la base de données
     * @return L'instance de la base de données
     */
    fun getInstance(): Database {
        if (instance == null) {
            instance = Database.connect(
                url = "jdbc:mysql://localhost:3306/Arka",
                driver = "com.mysql.cj.jdbc.Driver",
                user = "root", // À remplacer par votre utilisateur
                password = null // À remplacer par votre mot de passe
            )
        }
        return instance!!
    }
}