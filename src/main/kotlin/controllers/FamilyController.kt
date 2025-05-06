package controllers

import ktorm.Family
import repositories.FamilyRepository

/**
 * Contrôleur pour gérer les opérations sur les familles
 */
class FamilyController(private val familyRepository: FamilyRepository) {
    /**
     * Récupère toutes les familles
     */
    fun getAllFamilies(): List<Family> {
        return familyRepository.findAll()
    }

    /**
     * Récupère une famille par son ID
     */
    fun getFamilyById(familyID: Int): Family? {
        return familyRepository.findById(familyID)
    }

    /**
     * Crée une nouvelle famille
     */
    fun createFamily(family: Family): Family? {
        return familyRepository.insert(family)
    }

    /**
     * Met à jour une famille
     */
    fun updateFamily(family: Family): Boolean {
        return familyRepository.update(family) > 0
    }

    /**
     * Supprime une famille
     */
    fun deleteFamily(familyID: Int): Boolean {
        // Vérifier si la famille a des membres avant de la supprimer
        val hasFamilyMembers = hasMembers(familyID)
        if (hasFamilyMembers) {
            return false
        }
        return familyRepository.delete(familyID) > 0
    }

    /**
     * Vérifie si une famille a des membres
     */
    fun hasMembers(familyID: Int): Boolean {
        // Implémentation de la méthode manquante
        val members = familyRepository.getMembers(familyID)
        return members.isNotEmpty()
    }
}