package controllers

import ktorm.DatabaseManager
import ktorm.Family
import repositories.FamilyRepository

/**
 * Contrôleur pour gérer les opérations sur les familles
 */
class FamilyController {
    private val database = DatabaseManager.getInstance()
    private val familyRepository = FamilyRepository(database)

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
        val hasMembers = familyRepository.hasMembers(familyID)
        if (hasMembers) {
            return false
        }
        return familyRepository.delete(familyID) > 0
    }
}