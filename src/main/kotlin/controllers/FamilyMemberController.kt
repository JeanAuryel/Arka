package controllers

import ktorm.FamilyMember
import repositories.FamilyMemberRepository
import java.time.LocalDate

/**
 * Contrôleur pour gérer les opérations sur les membres de famille
 */
class FamilyMemberController(private val familyMemberRepository: FamilyMemberRepository) {
    /**
     * Récupère tous les membres de famille
     */
    fun getAllFamilyMembers(): List<FamilyMember> {
        return familyMemberRepository.findAll()
    }

    /**
     * Récupère un membre par son ID
     */
    fun getFamilyMemberById(id: Int): FamilyMember? {
        return familyMemberRepository.findById(id)
    }

    /**
     * Récupère un membre par son email
     */
    fun getFamilyMemberByEmail(email: String): FamilyMember? {
        return familyMemberRepository.findByEmail(email)
    }

    /**
     * Récupère tous les membres d'une famille
     */
    fun getFamilyMembersByFamilyId(familyId: Int): List<FamilyMember> {
        return familyMemberRepository.findByFamilyId(familyId)
    }

    /**
     * Récupère tous les parents d'une famille
     */
    fun getParentsByFamilyId(familyId: Int): List<FamilyMember> {
        return familyMemberRepository.findParentsByFamilyId(familyId)
    }

    /**
     * Récupère tous les enfants d'une famille
     */
    fun getChildrenByFamilyId(familyId: Int): List<FamilyMember> {
        return familyMemberRepository.findChildrenByFamilyId(familyId)
    }

    /**
     * Ajoute un nouveau membre de famille
     */
    fun addFamilyMember(member: FamilyMember): FamilyMember? {
        // Vérifier si l'email est déjà utilisé
        val existingMember = getFamilyMemberByEmail(member.familyMemberMail)
        if (existingMember != null) {
            return null
        }

        // Hasher le mot de passe si nécessaire (non inclus dans cette implémentation)
        // Si le membre n'a pas de mot de passe, générer un mot de passe temporaire
        val memberWithPassword = if (member.familyMemberPassword.isNullOrBlank()) {
            member.copy(familyMemberPassword = generateTemporaryPassword())
        } else {
            member
        }

        return familyMemberRepository.insert(memberWithPassword)
    }

    /**
     * Met à jour un membre de famille
     */
    fun updateFamilyMember(member: FamilyMember): Boolean {
        // Vérifier si le membre existe
        val existingMember = getFamilyMemberById(member.familyMemberID ?: 0)
        if (existingMember == null) {
            return false
        }

        // Vérifier si l'email est déjà utilisé par un autre membre
        val memberWithSameEmail = getFamilyMemberByEmail(member.familyMemberMail)
        if (memberWithSameEmail != null && memberWithSameEmail.familyMemberID != member.familyMemberID) {
            return false
        }

        // Conserver le mot de passe existant si non fourni
        val memberToUpdate = if (member.familyMemberPassword.isNullOrBlank()) {
            member.copy(familyMemberPassword = existingMember.familyMemberPassword)
        } else {
            // Hasher le nouveau mot de passe si nécessaire (non inclus ici)
            member
        }

        return familyMemberRepository.update(memberToUpdate) > 0
    }

    /**
     * Supprime un membre de famille
     */
    fun deleteFamilyMember(id: Int): Boolean {
        return familyMemberRepository.delete(id) > 0
    }

    /**
     * Vérifie si un utilisateur est un parent
     */
    fun isParent(memberId: Int): Boolean {
        val member = getFamilyMemberById(memberId)
        return member?.isParent ?: false
    }

    /**
     * Vérifie si un utilisateur est un administrateur
     */
    fun isAdmin(memberId: Int): Boolean {
        val member = getFamilyMemberById(memberId)
        return member?.isAdmin ?: false
    }

    /**
     * Change le statut parent d'un membre
     */
    fun setParentStatus(memberId: Int, isParent: Boolean): Boolean {
        val member = getFamilyMemberById(memberId) ?: return false
        val updatedMember = member.copy(isParent = isParent)
        return updateFamilyMember(updatedMember)
    }

    /**
     * Change le statut admin d'un membre
     */
    fun setAdminStatus(memberId: Int, isAdmin: Boolean): Boolean {
        val member = getFamilyMemberById(memberId) ?: return false
        val updatedMember = member.copy(isAdmin = isAdmin)
        return updateFamilyMember(updatedMember)
    }

    /**
     * Génère un mot de passe temporaire aléatoire
     */
    private fun generateTemporaryPassword(): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..10).map { chars.random() }.joinToString("")
    }
}