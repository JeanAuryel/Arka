package repositories

import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.Table

/**
 * Classe de base pour les repositories avec les opérations communes
 */
abstract class BaseRepository<T : Any, ID : Any>(protected val database: Database) {

    abstract val table: Table<*>
    abstract val idColumn: org.ktorm.schema.Column<ID>

    /**
     * Vérifie si une entité existe par son ID
     */
    fun exists(id: ID): Boolean {
        return database.from(table)
            .select(count(idColumn))  // Spécifier la colonne pour count()
            .where(idColumn eq id)
            .map { it.getInt(1)?: 0 }  // Spécifier la colonne pour count()
            .first() > 0
    }

    /**
     * Compte le nombre total d'entités
     */
    fun count(): Int {
        return database.from(table)
            .select(count(idColumn))  // Spécifier la colonne pour count()
            .map { it.getInt(1)?: 0 }  // Spécifier la colonne pour count()
            .first()
    }

    /**
     * Conversion abstraite d'un résultat de requête vers l'entité
     */
    abstract fun mapToEntity(row: QueryRowSet): T?
}