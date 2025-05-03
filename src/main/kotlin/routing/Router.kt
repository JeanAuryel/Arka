package routing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Les routes disponibles dans l'application
 */
enum class Routes {
    LOGIN,
    HOME,
    FOLDERS,
    FAMILY_MEMBERS,
    CATEGORIES,
    SETTINGS
}

/**
 * Classe de routage pour gérer la navigation entre les écrans
 */
class Router {
    var currentRoute by mutableStateOf(Routes.LOGIN)
        private set

    /**
     * Navigue vers une route spécifique
     */
    fun navigateTo(route: Routes) {
        currentRoute = route
    }

    /**
     * Retourne à la route précédente (simplifié)
     * Dans une application plus complexe, on pourrait maintenir un historique
     */
    fun goBack() {
        // Version simplifiée : on retourne à HOME par défaut
        currentRoute = Routes.HOME
    }
}