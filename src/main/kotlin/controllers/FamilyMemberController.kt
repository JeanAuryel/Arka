package controllers

import ktorm.DatabaseManager
import ktorm.FamilyMember
import repositories.FamilyMemberRepository
import repositories.FamilyRepository
import utils.PasswordHasher

/**
 * Contrôleur pour gérer les opérations liées aux membres de famille
 */
class FamilyMemberController {
    private val database = DatabaseManager.getInstance()
    private val memberRepository = FamilyMemberRepository(database)
    private val familyRepository = FamilyRepository(database)

    /**
     * Récupère tous les membres d'une famille
     */
    fun getAllMembersOfFamily(familyID: Int): List<FamilyMember> {
        return memberRepository.findAllByFamilyId(familyID)
    }

    /**
     * Récupère les informations d'un membre spécifique
     */
    fun getMemberById(memberID: Int): FamilyMember? {
        return memberRepository.findById(memberID)
    }

    /**
     * Ajoute un nouveau membre à une famille
     */
    fun addMember(member: FamilyMember, plainPassword: String): FamilyMember? {
        // Vérifier que la famille existe
        val family = familyRepository.findById(member.familyID) ?: return null

        // Créer un membre avec le mot de passe haché
        val memberWithHashedPassword = member.copy(
            familyMemberPassword = PasswordHasher.hashPassword(plainPassword)
        )

        // Enregistrer le membre dans la base de données
        return memberRepository.insert(memberWithHashedPassword)
    }

    /**
     * Met à jour les informations d'un membre
     */
    fun updateMember(member: FamilyMember): Boolean {
        return memberRepository.update(member) > 0
    }

    /**
     * Change le mot de passe d'un membre
     */
    fun changePassword(memberID: Int, newPassword: String): Boolean {
        val hashedPassword = PasswordHasher.hashPassword(newPassword)
        return memberRepository.updatePassword(memberID, hashedPassword) > 0
    }

    /**
     * Supprime un membre
     */
    fun deleteMember(memberID: Int): Boolean {
        return memberRepository.delete(memberID) > 0
    }
}