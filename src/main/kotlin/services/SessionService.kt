package services

import controllers.AuthController
import controllers.LoginRequest
import ktorm.MembreFamille
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Service de gestion des sessions utilisateur
 *
 * Responsabilités:
 * - Authentification et déconnexion des utilisateurs
 * - Gestion de l'état de session actuel
 * - Vérification des permissions utilisateur
 * - Nettoyage des ressources de session
 * - Cache des informations utilisateur
 *
 * Utilisé par: MainController, UI Components
 * Utilise: AuthController
 *
 * Pattern: Service Layer + Facade
 */
class SessionService(
    private val authController: AuthController
) {

    /**
     * Résultats des opérations de session
     */
    sealed class SessionResult {
        data class Success(val user: MembreFamille) : SessionResult()
        data class Error(val message: String, val code: SessionErrorCode) : SessionResult()
    }

    enum class SessionErrorCode {
        INVALID_CREDENTIALS,
        USER_NOT_FOUND,
        SESSION_EXPIRED,
        INTERNAL_ERROR,
        ALREADY_AUTHENTICATED
    }

    /**
     * Données de session étendues (cache local)
     */
    private var sessionInfo: SessionInfo? = null

    data class SessionInfo(
        val user: MembreFamille,
        val loginTime: LocalDateTime,
        val lastActivity: LocalDateTime,
        val sessionId: String
    )

    // ================================================================
    // MÉTHODES D'AUTHENTIFICATION
    // ================================================================

    /**
     * Authentifie un utilisateur avec email et mot de passe
     *
     * @param email Email de l'utilisateur
     * @param password Mot de passe en clair
     * @return Résultat de l'authentification
     */
    suspend fun authenticate(email: String, password: String): SessionResult = withContext(Dispatchers.IO) {
        try {
            // Vérifier si déjà connecté
            if (isAuthenticated()) {
                return@withContext SessionResult.Error(
                    "Utilisateur déjà connecté. Déconnectez-vous d'abord.",
                    SessionErrorCode.ALREADY_AUTHENTICATED
                )
            }

            // Déléguer l'authentification au AuthController
            val loginRequest = LoginRequest(email, password)
            val authResult = authController.login(loginRequest)

            when (authResult) {
                is AuthController.AuthResult.Success -> {
                    // Créer et stocker les informations de session
                    val now = LocalDateTime.now()
                    sessionInfo = SessionInfo(
                        user = authResult.member,
                        loginTime = now,
                        lastActivity = now,
                        sessionId = generateSessionId()
                    )

                    println("✅ Session créée pour ${authResult.member.prenomMembre} (${authResult.member.mailMembre})")

                    return@withContext SessionResult.Success(authResult.member)
                }

                is AuthController.AuthResult.Error -> {
                    return@withContext SessionResult.Error(
                        authResult.message,
                        when (authResult.code) {
                            AuthController.AuthErrorCode.INVALID_CREDENTIALS -> SessionErrorCode.INVALID_CREDENTIALS
                            AuthController.AuthErrorCode.USER_NOT_FOUND -> SessionErrorCode.USER_NOT_FOUND
                            else -> SessionErrorCode.INTERNAL_ERROR
                        }
                    )
                }
            }

        } catch (e: Exception) {
            return@withContext SessionResult.Error(
                "Erreur lors de l'authentification: ${e.message}",
                SessionErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Déconnecte l'utilisateur actuel
     *
     * @return true si la déconnexion a réussi
     */
    fun logout(): Boolean {
        return try {
            val currentUser = sessionInfo?.user

            // Déconnexion via AuthController
            authController.logout()

            // Nettoyage local
            sessionInfo = null

            println("✅ Session fermée pour ${currentUser?.prenomMembre ?: "utilisateur inconnu"}")
            true

        } catch (e: Exception) {
            println("❌ Erreur lors de la déconnexion: ${e.message}")
            false
        }
    }

    // ================================================================
    // MÉTHODES D'ÉTAT DE SESSION
    // ================================================================

    /**
     * Vérifie si un utilisateur est connecté
     *
     * @return true si une session active existe
     */
    fun isAuthenticated(): Boolean {
        return authController.isAuthenticated() && sessionInfo != null
    }

    /**
     * Obtient l'utilisateur actuellement connecté
     *
     * @return L'utilisateur connecté ou null
     */
    fun getCurrentUser(): MembreFamille? {
        return authController.getCurrentUser()
    }

    /**
     * Obtient les informations de session complètes
     *
     * @return Les informations de session ou null
     */
    fun getSessionInfo(): SessionInfo? {
        return sessionInfo?.copy() // Copie défensive
    }

    /**
     * Met à jour l'heure de dernière activité
     */
    fun updateLastActivity() {
        sessionInfo = sessionInfo?.copy(lastActivity = LocalDateTime.now())
    }

    // ================================================================
    // MÉTHODES DE VÉRIFICATION DES PERMISSIONS
    // ================================================================

    /**
     * Vérifie si l'utilisateur actuel est administrateur
     *
     * @return true si l'utilisateur est admin
     */
    fun isCurrentUserAdmin(): Boolean {
        return authController.isCurrentUserAdmin()
    }

    /**
     * Vérifie si l'utilisateur actuel est responsable familial
     *
     * @return true si l'utilisateur est responsable
     */
    fun isCurrentUserResponsible(): Boolean {
        return authController.isCurrentUserResponsible()
    }

    /**
     * Vérifie si l'utilisateur appartient à une famille spécifique
     *
     * @param familyId ID de la famille à vérifier
     * @return true si l'utilisateur appartient à cette famille
     */
    fun isCurrentUserInFamily(familyId: Int): Boolean {
        return authController.isCurrentUserInFamily(familyId)
    }

    /**
     * Vérifie si l'utilisateur peut accéder à une ressource
     *
     * @param resourceOwnerId ID du propriétaire de la ressource
     * @return true si l'accès est autorisé
     */
    fun canAccessResource(resourceOwnerId: Int): Boolean {
        val currentUser = getCurrentUser() ?: return false

        return when {
            // L'utilisateur est propriétaire
            currentUser.membreFamilleId == resourceOwnerId -> true

            // L'utilisateur est admin
            isCurrentUserAdmin() -> true

            // L'utilisateur est responsable de la même famille
            isCurrentUserResponsible() &&
                    isCurrentUserInFamily(getCurrentUser()?.familleId ?: -1) -> true

            // Autres règles de permissions peuvent être ajoutées ici
            else -> false
        }
    }

    // ================================================================
    // MÉTHODES DE GESTION DE SESSION
    // ================================================================

    /**
     * Vérifie la validité de la session
     *
     * @param maxInactivityMinutes Durée max d'inactivité (par défaut 60 min)
     * @return true si la session est valide
     */
    fun isSessionValid(maxInactivityMinutes: Long = 60): Boolean {
        val info = sessionInfo ?: return false
        val now = LocalDateTime.now()

        // Vérifier la durée d'inactivité
        val inactivityDuration = java.time.Duration.between(info.lastActivity, now)
        val isActive = inactivityDuration.toMinutes() <= maxInactivityMinutes

        return isAuthenticated() && isActive
    }

    /**
     * Prolonge automatiquement la session
     *
     * @return true si la prolongation a réussi
     */
    fun extendSession(): Boolean {
        return if (isAuthenticated()) {
            updateLastActivity()
            true
        } else {
            false
        }
    }

    /**
     * Vérifie et nettoie les sessions expirées
     *
     * @param maxInactivityMinutes Durée max d'inactivité
     * @return true si une session a été expirée
     */
    fun checkAndCleanExpiredSession(maxInactivityMinutes: Long = 60): Boolean {
        return if (!isSessionValid(maxInactivityMinutes)) {
            println("⚠️ Session expirée, déconnexion automatique")
            logout()
            true
        } else {
            false
        }
    }

    // ================================================================
    // MÉTHODES DE MAINTENANCE ET NETTOYAGE
    // ================================================================

    /**
     * Vérifie la cohérence entre la session locale et AuthController
     *
     * @return true si tout est cohérent
     */
    fun validateSessionConsistency(): Boolean {
        val localUser = sessionInfo?.user
        val authUser = authController.getCurrentUser()

        return when {
            localUser == null && authUser == null -> true // Aucune session
            localUser != null && authUser != null ->
                localUser.membreFamilleId == authUser.membreFamilleId // Sessions cohérentes
            else -> {
                println("⚠️ Incohérence détectée entre sessions locale et AuthController")
                cleanup() // Nettoyage forcé
                false
            }
        }
    }

    /**
     * Nettoyage complet des ressources de session
     */
    fun cleanup() {
        try {
            logout()
            sessionInfo = null
            println("✅ Nettoyage des ressources de session terminé")
        } catch (e: Exception) {
            println("❌ Erreur lors du nettoyage de session: ${e.message}")
        }
    }

    /**
     * Vérifie session existante au démarrage de l'application
     */
    fun checkExistingSession() {
        try {
            // Vérifier si AuthController a une session active
            val currentUser = authController.getCurrentUser()

            if (currentUser != null && sessionInfo == null) {
                // Recréer les informations de session locale
                val now = LocalDateTime.now()
                sessionInfo = SessionInfo(
                    user = currentUser,
                    loginTime = now, // On ne connaît pas l'heure réelle de login
                    lastActivity = now,
                    sessionId = generateSessionId()
                )
                println("✅ Session existante détectée et restaurée pour ${currentUser.prenomMembre}")
            }

        } catch (e: Exception) {
            println("❌ Erreur lors de la vérification de session existante: ${e.message}")
        }
    }

    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================

    /**
     * Génère un ID de session unique
     *
     * @return ID de session
     */
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    /**
     * Obtient un résumé de la session actuelle
     *
     * @return Map avec les informations de session
     */
    fun getSessionSummary(): Map<String, Any?> {
        val info = sessionInfo
        val user = getCurrentUser()

        return mapOf(
            "isAuthenticated" to isAuthenticated(),
            "userName" to user?.prenomMembre,
            "userEmail" to user?.mailMembre,
            "isAdmin" to isCurrentUserAdmin(),
            "isResponsible" to isCurrentUserResponsible(),
            "loginTime" to info?.loginTime,
            "lastActivity" to info?.lastActivity,
            "sessionId" to info?.sessionId,
            "familyId" to user?.familleId
        )
    }

    /**
     * Obtient les statistiques de session
     *
     * @return Map avec les statistiques
     */
    fun getSessionStatistics(): Map<String, Any> {
        val info = sessionInfo
        val now = LocalDateTime.now()

        val sessionDuration = if (info != null) {
            java.time.Duration.between(info.loginTime, now).toMinutes()
        } else 0L

        val inactivityDuration = if (info != null) {
            java.time.Duration.between(info.lastActivity, now).toMinutes()
        } else 0L

        return mapOf(
            "sessionDurationMinutes" to sessionDuration,
            "inactivityMinutes" to inactivityDuration,
            "isValid" to isSessionValid(),
            "needsExtension" to (inactivityDuration > 30) // Suggest extension after 30 min
        )
    }
}