//Il vérifie les mots de passe en clair déjà présent dans la base de données et les hashe.
//Ensuite pour les nouveaux utilisateurs lors de la création d'un nouveau mot de passe, hashe celui-ci directeement.
package utils

import ktorm.ArkaDatabase
import ktorm.MembresFamille
import org.ktorm.dsl.*
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.mindrot.jbcrypt.BCrypt

/**
 * Utilitaire de hachage des mots de passe pour Arka
 * - Hachage BCrypt sécurisé pour nouveaux utilisateurs
 * - Migration automatique des mots de passe en clair existants
 * - Vérification intelligente pour éviter les migrations répétées
 */
object PasswordHasher {

    // Niveau de complexité BCrypt (12 = recommandé pour 2024)
    private const val BCRYPT_ROUNDS = 12

    // Flag pour éviter de refaire la migration plusieurs fois
    private var migrationExecuted = false

    /**
     * Initialise le système de mots de passe
     * - Vérifie s'il y a des mots de passe en clair
     * - Les migre automatiquement si nécessaire
     * - À appeler une fois au démarrage de l'application
     */
    fun initializePasswordSystem() {
        if (migrationExecuted) {
            println("🔒 Système de mots de passe déjà initialisé")
            return
        }

        try {
            println("🔍 Vérification des mots de passe en base...")

            // Vérifier s'il y a des mots de passe en clair
            val analysis = analyzeExistingPasswords()

            if (analysis.plainTextCount > 0) {
                println("🔄 Migration automatique de ${analysis.plainTextCount} mot(s) de passe...")
                migratePlainTextPasswords()
                println("✅ Migration terminée avec succès!")
            } else {
                println("✅ Tous les mots de passe sont déjà sécurisés")
            }

            migrationExecuted = true

        } catch (e: Exception) {
            println("⚠️ Erreur lors de l'initialisation des mots de passe: ${e.message}")
            println("📝 L'application continuera avec les nouveaux utilisateurs seulement")
        }
    }

    /**
     * Hache un mot de passe en clair (pour nouveaux utilisateurs)
     * @param plainPassword Le mot de passe en clair
     * @return Le hash BCrypt du mot de passe
     */
    fun hashPassword(plainPassword: String): String {
        if (plainPassword.isBlank()) {
            throw IllegalArgumentException("Le mot de passe ne peut pas être vide")
        }

        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS))
    }

    /**
     * Vérifie si un mot de passe correspond à son hash
     * @param plainPassword Le mot de passe en clair à vérifier
     * @param hashedPassword Le hash stocké en base
     * @return true si le mot de passe correspond, false sinon
     */
    fun verifyPassword(plainPassword: String, hashedPassword: String): Boolean {
        if (plainPassword.isBlank() || hashedPassword.isBlank()) {
            return false
        }

        return try {
            BCrypt.checkpw(plainPassword, hashedPassword)
        } catch (e: Exception) {
            println("⚠️ Erreur lors de la vérification du mot de passe: ${e.message}")
            false
        }
    }

    /**
     * Vérifie si un mot de passe est déjà hashé (détection heuristique)
     * @param password Le mot de passe à analyser
     * @return true si le mot de passe semble déjà hashé
     */
    fun isAlreadyHashed(password: String): Boolean {
        // Un hash BCrypt commence toujours par $2a$, $2b$, $2x$ ou $2y$
        // et fait exactement 60 caractères
        return password.length == 60 &&
                (password.startsWith("\$2a\$") ||
                        password.startsWith("\$2b\$") ||
                        password.startsWith("\$2x\$") ||
                        password.startsWith("\$2y\$"))
    }

    /**
     * Valide la force d'un mot de passe
     * @param password Le mot de passe à valider
     * @return Une liste des critères non respectés (vide si valide)
     */
    fun validatePasswordStrength(password: String): List<String> {
        val errors = mutableListOf<String>()

        if (password.length < 8) {
            errors.add("Le mot de passe doit contenir au moins 8 caractères")
        }

        if (!password.any { it.isUpperCase() }) {
            errors.add("Le mot de passe doit contenir au moins une majuscule")
        }

        if (!password.any { it.isLowerCase() }) {
            errors.add("Le mot de passe doit contenir au moins une minuscule")
        }

        if (!password.any { it.isDigit() }) {
            errors.add("Le mot de passe doit contenir au moins un chiffre")
        }

        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        if (!password.any { it in specialChars }) {
            errors.add("Le mot de passe doit contenir au moins un caractère spécial")
        }

        return errors
    }

    /**
     * Génère un mot de passe temporaire sécurisé
     * @param length Longueur du mot de passe (minimum 8)
     * @return Un mot de passe temporaire
     */
    fun generateTemporaryPassword(length: Int = 12): String {
        require(length >= 8) { "Le mot de passe doit faire au moins 8 caractères" }

        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    // ================================================================
    // FONCTIONS PRIVÉES POUR LA MIGRATION AUTOMATIQUE
    // ================================================================

    /**
     * Analyse les mots de passe existants en base
     */
    private fun analyzeExistingPasswords(): PasswordAnalysisResult {
        return try {
            val allMembers = ArkaDatabase.instance.sequenceOf(MembresFamille).toList()

            var plainTextCount = 0
            var hashedCount = 0
            var emptyCount = 0

            for (member in allMembers) {
                when {
                    member.mdpMembre.isBlank() -> emptyCount++
                    isAlreadyHashed(member.mdpMembre) -> hashedCount++
                    else -> plainTextCount++
                }
            }

            println("📊 Analyse: ${allMembers.size} membre(s) - $plainTextCount en clair, $hashedCount hashés, $emptyCount vides")

            PasswordAnalysisResult(
                totalMembers = allMembers.size,
                plainTextCount = plainTextCount,
                hashedCount = hashedCount,
                emptyCount = emptyCount
            )

        } catch (e: Exception) {
            println("⚠️ Erreur lors de l'analyse: ${e.message}")
            PasswordAnalysisResult(0, 0, 0, 0)
        }
    }

    /**
     * Migre automatiquement tous les mots de passe en clair
     */
    private fun migratePlainTextPasswords() {
        try {
            val allMembers = ArkaDatabase.instance.sequenceOf(MembresFamille).toList()
            var successCount = 0
            var errorCount = 0

            for (member in allMembers) {
                // Ne traiter que les mots de passe en clair non vides
                if (member.mdpMembre.isNotBlank() && !isAlreadyHashed(member.mdpMembre)) {
                    try {
                        val hashedPassword = hashPassword(member.mdpMembre)

                        // Mise à jour en base
                        val updated = ArkaDatabase.instance.update(MembresFamille) {
                            set(it.mdpMembre, hashedPassword)
                            where { it.membreFamilleId eq member.membreFamilleId }
                        }

                        if (updated > 0) {
                            println("   ✅ ${member.prenomMembre}: mot de passe sécurisé")
                            successCount++
                        } else {
                            println("   ❌ ${member.prenomMembre}: échec de mise à jour")
                            errorCount++
                        }

                    } catch (e: Exception) {
                        println("   ❌ ${member.prenomMembre}: erreur - ${e.message}")
                        errorCount++
                    }
                }
            }

            println("📈 Migration: $successCount succès, $errorCount erreur(s)")

        } catch (e: Exception) {
            println("❌ Erreur lors de la migration: ${e.message}")
            throw e
        }
    }

    /**
     * Résultat de l'analyse des mots de passe
     */
    private data class PasswordAnalysisResult(
        val totalMembers: Int,
        val plainTextCount: Int,
        val hashedCount: Int,
        val emptyCount: Int
    )
}

/**
 * Extensions pour faciliter l'utilisation
 */

/**
 * Extension pour hasher facilement un String
 */
fun String.toArkaHash(): String = PasswordHasher.hashPassword(this)

/**
 * Extension pour vérifier un mot de passe
 */
fun String.verifyWith(hashedPassword: String): Boolean =
    PasswordHasher.verifyPassword(this, hashedPassword)

/**
 * Extension pour valider la force d'un mot de passe
 */
fun String.validateStrength(): List<String> =
    PasswordHasher.validatePasswordStrength(this)