package ktorm

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Modèle de données pour une famille
 */
data class Family(
    val familyID: Int? = null,
    val familyLabel: String
)

/**
 * Modèle de données pour une catégorie
 */
data class Category(
    val categoryID: Int? = null,
    val categoryLabel: String,
    val categoryDescription: String
)

/**
 * Modèle de données pour un membre de la famille
 */
data class FamilyMember(
    val familyMemberID: Int? = null,
    val familyMemberFirstName: String,
    val familyMemberMail: String,
    val familyMemberPassword: String,
    val familyMemberBirthDate: LocalDate,
    val familyMemberGender: String,
    val isParent: Boolean,
    val isAdmin: Boolean,
    val familyID: Int,
    // Relations
    var family: Family? = null
)

/**
 * Modèle de données pour un dossier
 */
data class Folder(
    val folderID: Int? = null,
    val folderName: String,
    val folderCreationDate: LocalDateTime,
    val parentFolderID: Int? = null,
    val familyMemberID: Int,
    val categoryID: Int,
    // Relations
    var parentFolder: Folder? = null,
    var familyMember: FamilyMember? = null,
    var category: Category? = null,
    var subFolders: List<Folder> = emptyList(),
    var files: List<File> = emptyList()
)

/**
 * Modèle de données pour un fichier
 */
data class File(
    val fileID: Int? = null,
    val fileName: String,
    val fileSize: Long,
    val fileCreationDate: LocalDateTime,
    val folderID: Int,
    // Relations
    var folder: Folder? = null
)