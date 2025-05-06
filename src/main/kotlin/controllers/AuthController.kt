package controllers

import ktorm.FamilyMembers
import ktorm.toFamilyMember
import repositories.FamilyMemberRepository
import utils.PasswordHasher
import org.ktorm.dsl.*

/**
 * Contrôleur pour gérer l'authentification des utilisateurs
 */
class AuthController(private val memberRepository: FamilyMemberRepository) {
    /**
     * Authentifie un utilisateur avec son email et son mot de passe
     * @param email Email de l'utilisateur
     * @param password Mot de passe en clair
     * @return L'utilisateur si l'authentification est réussie, null sinon
     */
    fun authenticate(email: String, password: String): ktorm.FamilyMember? {
        val member = memberRepository.findByEmail(email) ?: return null

        // Vérifier le mot de passe avec bcrypt
        return if (PasswordHasher.checkPassword(password, member.familyMemberPassword)) {
            member  // Authentification réussie
        } else {
            null    // Échec de l'authentification
        }
    }

    /**
     * Vérifie si un utilisateur a les droits d'administration
     */
    fun isAdmin(memberID: Int): Boolean {
        return memberRepository.findById(memberID)?.isAdmin ?: false
    }

    /**
     * Vérifie si un utilisateur est un parent
     */
    fun isParent(memberID: Int): Boolean {
        return memberRepository.findById(memberID)?.isParent ?: false
    }

    /**
     * Exécute une seule fois le hachage de tous les mots de passe
     */
    fun initializePasswords(database: org.ktorm.database.Database) {
        PasswordHasher.encryptAllPasswords(database)
    }
}