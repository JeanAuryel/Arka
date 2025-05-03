package ktorm

import org.ktorm.schema.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Définition de la table Family
 */
object Families : Table<Nothing>("Family") {
    val familyID = int("FamilyID").primaryKey()
    val familyLabel = varchar("FamilyLabel")
}

/**
 * Définition de la table Category
 */
object Categories : Table<Nothing>("Category") {
    val categoryID = int("CategoryID").primaryKey()
    val categoryLabel = varchar("CategoryLabel")
    val categoryDescription = varchar("CategoryDescription")
}

/**
 * Définition de la table FamilyMember
 */
object FamilyMembers : Table<Nothing>("FamilyMember") {
    val familyMemberID = int("FamilyMemberID").primaryKey()
    val familyMemberFirstName = varchar("FamilyMemberFirstName")
    val familyMemberMail = varchar("FamilyMemberMail")
    val familyMemberPassword = varchar("FamilyMemberPassword")
    val familyMemberBirthDate = date("FamilyMemberBirthDate")
    val familyMemberGender = varchar("FamilyMemberGender")
    val isParent = boolean("isParent")
    val isAdmin = boolean("isAdmin")
    val familyID = int("FamilyID")
}

/**
 * Définition de la table Folder
 */
object Folders : Table<Nothing>("Folder") {
    val folderID = int("FolderID").primaryKey()
    val folderName = varchar("FolderName")
    val folderCreationDate = datetime("FolderCreationDate")
    val parentFolderID = int("ParentFolderID")
    val familyMemberID = int("FamilyMemberID")
    val categoryID = int("CategoryID")
}

/**
 * Définition de la table File
 */
object Files : Table<Nothing>("File") {
    val fileID = int("FileID").primaryKey()
    val fileName = varchar("FileName")
    val fileSize = long("FileSize")
    val fileCreationDate = datetime("FIleCreationDate")
    val folderID = int("FolderID")
}