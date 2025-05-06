package controllers

import org.ktorm.database.Database

/**
 * Contrôleur principal qui initialise et coordonne l'application
 */
class MainController(private val authController: AuthController) {
    /**
     * Initialise l'application lors du premier démarrage
     */
    fun initializeApplication(database: Database) {
        // Hacher les mots de passe existants (à exécuter une seule fois)
        authController.initializePasswords(database)

        // Autres initialisations si nécessaire
        println("Application Arka initialisée avec succès")
    }

    /**
     * Démarre l'application
     */
    fun startApplication() {
        // Code pour démarrer l'application
        println("Application Arka démarrée")
    }
}