package ktorm

import org.ktorm.dsl.QueryRowSet
import org.ktorm.entity.Entity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Fonctions d'extension pour convertir les résultats de requêtes en objets de modèle
 */

/**
 * Convertit un résultat de requête en objet Family
 */
fun QueryRowSet.toFamily(): Family {
    return Family(
        familyID = this[Families.familyID],
        familyLabel = this[Families.familyLabel] ?: ""
    )
}

/**
 * Convertit un résultat de requête en objet Category
 */
fun QueryRowSet.toCategory(): Category {
    return Category(
        categoryID = this[Categories.categoryID],
        categoryLabel = this[Categories.categoryLabel] ?: "",
        categoryDescription = this[Categories.categoryDescription] ?: ""
    )
}

/**
 * Convertit un résultat de requête en objet FamilyMember
 */
fun QueryRowSet.toFamilyMember(): FamilyMember {
    return FamilyMember(
        familyMemberID = this[FamilyMembers.familyMemberID],
        familyMemberFirstName = this[FamilyMembers.familyMemberFirstName] ?: "",
        familyMemberMail = this[FamilyMembers.familyMemberMail] ?: "",
        familyMemberPassword = this[FamilyMembers.familyMemberPassword] ?: "",
        familyMemberBirthDate = this[FamilyMembers.familyMemberBirthDate] ?: LocalDate.now(),
        familyMemberGender = this[FamilyMembers.familyMemberGender] ?: "",
        isParent = this[FamilyMembers.isParent] ?: false,
        isAdmin = this[FamilyMembers.isAdmin] ?: false,
        familyID = this[FamilyMembers.familyID] ?: 0
    )
}

/**
 * Convertit un résultat de requête en objet Folder
 */
fun QueryRowSet.toFolder(): Folder {
    return Folder(
        folderID = this[Folders.folderID],
        folderName = this[Folders.folderName] ?: "",
        folderCreationDate = this[Folders.folderCreationDate] ?: LocalDateTime.now(),
        parentFolderID = this[Folders.parentFolderID],
        familyMemberID = this[Folders.familyMemberID] ?: 0,
        categoryID = this[Folders.categoryID] ?: 0
    )
}

/**
 * Extension pour mapper un QueryRowSet vers un DefaultFolderTemplate
 */
fun QueryRowSet.toDefaultFolderTemplate(): DefaultFolderTemplate? {
    return if (this[DefaultFolderTemplates.templateID] != null) {
        DefaultFolderTemplate(
            templateID = this[DefaultFolderTemplates.templateID],
            templateName = this[DefaultFolderTemplates.templateName] ?: "",
            categoryID = this[DefaultFolderTemplates.categoryID] ?: 0
        )
    } else {
        null
    }
}

/**
 * Convertit un résultat de requête en objet File
 */
fun QueryRowSet.toFile(): File {
    return File(
        fileID = this[Files.fileID],
        fileName = this[Files.fileName] ?: "",
        fileSize = this[Files.fileSize] ?: 0L,
        fileCreationDate = this[Files.fileCreationDate] ?: LocalDateTime.now(),
        folderID = this[Files.folderID] ?: 0
    )
}