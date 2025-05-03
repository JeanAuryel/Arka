package utils

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.*
import org.mindrot.jbcrypt.BCrypt


// Définition de la table pour Ktorm (si pas déjà définie ailleurs)
object FamilyMembers : Table<Nothing>("FamilyMember") {
    val familyMemberID = int("FamilyMemberID").primaryKey()
    val familyMemberFirstName = varchar("FamilyMemberFirstName")
    val familyMemberPassword = varchar("FamilyMemberPassword")
    // Vous pouvez omettre cette définition si vous l'avez déjà ailleurs
}

/**
 * Classe utilitaire pour gérer le hachage des mots de passe
 */
object PasswordHasher {

    /**
     * Hache un mot de passe avec bcrypt
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Vérifie si un mot de passe correspond à un hash bcrypt
     */
    fun checkPassword(password: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(password, hashedPassword)
    }

    /**
     * Convertit tous les mots de passe en clair en hachage bcrypt
     */
    fun encryptAllPasswords(database: Database) {
        println("Début du processus de hachage des mots de passe...")

        // Récupérer tous les membres avec leurs mots de passe actuels
        val members = database.from(FamilyMembers)
            .select(FamilyMembers.familyMemberID, FamilyMembers.familyMemberFirstName, FamilyMembers.familyMemberPassword)
            .map {
                val id = it[FamilyMembers.familyMemberID]!!
                val name = it[FamilyMembers.familyMemberFirstName]!!
                val currentPassword = it[FamilyMembers.familyMemberPassword]!!
                Triple(id, name, currentPassword)
            }

        println("Nombre total de membres trouvés : ${members.size}")

        // Pour chaque membre, crypter le mot de passe et mettre à jour la BDD
        var updateCount = 0
        for ((id, name, currentPassword) in members) {
            // Vérifier si le mot de passe est déjà au format bcrypt
            if (!currentPassword.startsWith("$2a$") && !currentPassword.startsWith("$2b$") && !currentPassword.startsWith("$2y$")) {
                // Le mot de passe est en clair, il faut le crypter
                val hashedPassword = hashPassword(currentPassword)

                // Mettre à jour le mot de passe dans la base de données
                val affected = database.update(FamilyMembers) {
                    set(it.familyMemberPassword, hashedPassword)
                    where {
                        it.familyMemberID eq id
                    }
                }

                if (affected > 0) {
                    println("Mot de passe haché pour $name (ID: $id)")
                    updateCount++
                } else {
                    println("⚠️ Échec de la mise à jour pour $name (ID: $id)")
                }
            } else {
                println("Le mot de passe de $name (ID: $id) est déjà haché")
            }
        }

        println("Processus terminé : $updateCount mots de passe ont été hachés avec bcrypt")
    }
}