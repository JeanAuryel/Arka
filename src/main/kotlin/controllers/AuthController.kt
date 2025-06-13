package controllers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import repositories.FamilyMemberRepository
import utils.PasswordHasher
import ktorm.*
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.add
import org.ktorm.dsl.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Controller responsible for authentication and authorization operations.
 * Handles login, logout, registration, and session management.
 *
 * Best Practices Applied:
 * - Separation of concerns: Only handles auth logic
 * - Coroutines for async operations
 * - Proper error handling with sealed classes
 * - Input validation
 * - Secure password handling
 */
class AuthController(
    private val familyMemberRepository: FamilyMemberRepository,
    private val passwordHasher: PasswordHasher
) {

    /**
     * Sealed class for authentication results
     * This provides type-safe error handling and clear API contracts
     */
    sealed class AuthResult {
        data class Success(val member: MembreFamille, val token: String? = null) : AuthResult()
        data class Error(val message: String, val code: AuthErrorCode) : AuthResult()
    }

    enum class AuthErrorCode {
        INVALID_CREDENTIALS,
        USER_NOT_FOUND,
        EMAIL_ALREADY_EXISTS,
        INVALID_INPUT,
        INTERNAL_ERROR,
        ACCOUNT_DISABLED
    }

    /**
     * Current authenticated user session
     * In a real app, this would be managed by a proper session store
     */
    private var currentUser: MembreFamille? = null

    /**
     * Authenticate a user with email and password
     *
     * @param loginRequest Contains email and password
     * @return AuthResult with either success or error details
     */
    suspend fun login(loginRequest: LoginRequest): AuthResult = withContext(Dispatchers.IO) {
        try {
            // Input validation
            if (loginRequest.email.isBlank() || loginRequest.password.isBlank()) {
                return@withContext AuthResult.Error(
                    "Email et mot de passe requis",
                    AuthErrorCode.INVALID_INPUT
                )
            }

            if (!isValidEmail(loginRequest.email)) {
                return@withContext AuthResult.Error(
                    "Format d'email invalide",
                    AuthErrorCode.INVALID_INPUT
                )
            }

            // Find user by email using repository method
            val memberEntity = familyMemberRepository.findByEmail(loginRequest.email)
                ?: return@withContext AuthResult.Error(
                    "Utilisateur non trouvé",
                    AuthErrorCode.USER_NOT_FOUND
                )

            // Convert entity to model
            val member = memberEntity.toModel()

            // Verify password using the correct property
            if (!passwordHasher.verifyPassword(loginRequest.password, member.mdpMembre)) {
                return@withContext AuthResult.Error(
                    "Identifiants invalides",
                    AuthErrorCode.INVALID_CREDENTIALS
                )
            }

            // Set current user session
            currentUser = member

            return@withContext AuthResult.Success(member)

        } catch (e: Exception) {
            return@withContext AuthResult.Error(
                "Erreur lors de la connexion: ${e.message}",
                AuthErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Register a new family member
     * Note: In Arka, registration is typically done by family admins
     */
    suspend fun register(registerRequest: RegisterRequest): AuthResult = withContext(Dispatchers.IO) {
        try {
            // Input validation
            if (!isValidRegisterRequest(registerRequest)) {
                return@withContext AuthResult.Error(
                    "Données d'inscription invalides",
                    AuthErrorCode.INVALID_INPUT
                )
            }

            // Check if email already exists using repository method
            if (familyMemberRepository.findByEmail(registerRequest.email) != null) {
                return@withContext AuthResult.Error(
                    "Cette adresse email est déjà utilisée",
                    AuthErrorCode.EMAIL_ALREADY_EXISTS
                )
            }

            // Hash password
            val hashedPassword = passwordHasher.hashPassword(registerRequest.password)

            // ✅ Créer l'entité avec la méthode create() existante
            val newMemberEntity = MembreFamilleEntity {
                prenomMembre = registerRequest.firstName.trim()
                mailMembre = registerRequest.email.lowercase().trim()
                mdpMembre = hashedPassword
                dateNaissanceMembre = registerRequest.dateOfBirth
                genreMembre = registerRequest.gender
                estResponsable = false
                estAdmin = false
                dateAjoutMembre = LocalDateTime.now()
                familleId = registerRequest.familyId
            }

            // Utiliser la méthode create() du BaseRepository
            val createdMember = familyMemberRepository.create(newMemberEntity)
                ?: return@withContext AuthResult.Error(
                    "Erreur lors de la création du membre",
                    AuthErrorCode.INTERNAL_ERROR
                )

            return@withContext AuthResult.Success(createdMember.toModel())

        } catch (e: Exception) {
            return@withContext AuthResult.Error(
                "Erreur lors de l'inscription: ${e.message}",
                AuthErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Logout current user
     */
    fun logout() {
        currentUser = null
    }

    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): MembreFamille? = currentUser

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean = currentUser != null

    /**
     * Check if current user is admin
     */
    fun isCurrentUserAdmin(): Boolean = currentUser?.estAdmin == true

    /**
     * Check if current user is family responsible
     */
    fun isCurrentUserResponsible(): Boolean = currentUser?.estResponsable == true

    /**
     * Check if current user belongs to specific family
     */
    fun isCurrentUserInFamily(familyId: Int): Boolean = currentUser?.familleId == familyId

    /**
     * Update current user password
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val user = currentUser ?: return@withContext AuthResult.Error(
                "Utilisateur non connecté",
                AuthErrorCode.INVALID_CREDENTIALS
            )

            // Verify current password
            if (!passwordHasher.verifyPassword(currentPassword, user.mdpMembre)) {
                return@withContext AuthResult.Error(
                    "Mot de passe actuel incorrect",
                    AuthErrorCode.INVALID_CREDENTIALS
                )
            }

            // Validate new password
            if (!isValidPassword(newPassword)) {
                return@withContext AuthResult.Error(
                    "Le nouveau mot de passe ne respecte pas les critères",
                    AuthErrorCode.INVALID_INPUT
                )
            }

            // Hash new password
            val newHashedPassword = passwordHasher.hashPassword(newPassword)

            // Get entity from repository
            val memberEntity = familyMemberRepository.findById(user.membreFamilleId)
                ?: return@withContext AuthResult.Error(
                    "Utilisateur non trouvé",
                    AuthErrorCode.USER_NOT_FOUND
                )

            // Update password in entity using ktorm update
            ArkaDatabase.instance.update(MembresFamille) {
                set(it.mdpMembre, newHashedPassword)
                where { it.membreFamilleId eq user.membreFamilleId }
            }

            // Update current session
            currentUser = user.copy(mdpMembre = newHashedPassword)

            return@withContext AuthResult.Success(currentUser!!)

        } catch (e: Exception) {
            return@withContext AuthResult.Error(
                "Erreur lors du changement de mot de passe: ${e.message}",
                AuthErrorCode.INTERNAL_ERROR
            )
        }
    }

    // Private helper methods for validation

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        return emailRegex.matches(email)
    }

    private fun isValidPassword(password: String): Boolean {
        // Arka password policy: minimum 8 characters, at least one number and one letter
        return password.length >= 8 &&
                password.any { it.isDigit() } &&
                password.any { it.isLetter() }
    }

    private fun isValidRegisterRequest(request: RegisterRequest): Boolean {
        return request.firstName.isNotBlank() &&
                request.email.isNotBlank() &&
                isValidEmail(request.email) &&
                isValidPassword(request.password) &&
                request.familyId > 0
    }
}

/**
 * Data classes for authentication requests and responses
 * These should ideally be in a separate models file
 */
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val firstName: String,
    val email: String,
    val password: String,
    val dateOfBirth: LocalDate,
    val gender: String,
    val familyId: Int
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: MembreFamille? = null,
    val token: String? = null
)