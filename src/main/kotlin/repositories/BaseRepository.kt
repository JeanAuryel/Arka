package repositories

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.find
import org.ktorm.entity.add
import org.ktorm.entity.toList
import org.ktorm.entity.filter
import org.ktorm.entity.drop
import org.ktorm.entity.take
import org.ktorm.dsl.*
import org.ktorm.schema.ColumnDeclaring
import ktorm.ArkaDatabase

/**
 * Repository de base pour toutes les entités Arka
 *
 * Fournit les opérations CRUD communes à tous les repositories
 * Utilise les generics pour une implémentation réutilisable
 *
 * @param E Type d'entité ktorm (ex: MembreFamilleEntity)
 * @param T Type de table ktorm (ex: MembresFamille)
 */
abstract class BaseRepository<E : Entity<E>, T : Table<E>> {

    /**
     * Référence à la table ktorm - doit être implémentée par les classes enfants
     */
    abstract val table: T

    /**
     * Obtient la clé primaire d'une entité
     * Cette méthode doit être surchargée par les repositories enfants
     *
     * @return La valeur de la clé primaire
     */
    protected abstract fun E.getPrimaryKey(): Int

    /**
     * Trouve une entité par son ID
     *
     * @param id L'identifiant de l'entité
     * @return L'entité trouvée ou null
     */
    open fun findById(id: Int): E? {
        return ArkaDatabase.instance.sequenceOf(table).find { entity ->
            // Utilisation correcte avec la colonne de la table
            getPrimaryKeyColumn() eq id
        }
    }

    /**
     * Obtient la colonne de clé primaire de la table
     * Doit être implémentée par les classes enfants
     */
    protected abstract fun getPrimaryKeyColumn(): org.ktorm.schema.Column<Int>

    /**
     * Trouve toutes les entités de ce type
     *
     * @return Liste de toutes les entités
     */
    open fun findAll(): List<E> {
        return ArkaDatabase.instance.sequenceOf(table).toList()
    }

    /**
     * Compte le nombre total d'entités
     *
     * @return Nombre d'entités
     */
    open fun count(): Int {
        return ArkaDatabase.instance.from(table)
            .select(org.ktorm.dsl.count())
            .map { it.getInt(1) }
            .first()
    }

    /**
     * Vérifie si une entité avec cet ID existe
     *
     * @param id L'identifiant à vérifier
     * @return true si l'entité existe
     */
    open fun exists(id: Int): Boolean {
        return findById(id) != null
    }

    /**
     * Sauvegarde une entité (insert ou update automatique)
     *
     * @param entity L'entité à sauvegarder
     * @return L'entité sauvegardée
     */
    open fun save(entity: E): E {
        if (isNewEntity(entity)) {
            return create(entity)
        } else {
            update(entity)
            return entity
        }
    }

    /**
     * Crée une nouvelle entité
     *
     * @param entity L'entité à créer
     * @return L'entité créée
     */
    open fun create(entity: E): E {
        ArkaDatabase.instance.sequenceOf(table).add(entity)
        return entity
    }

    /**
     * Met à jour une entité existante
     * Cette méthode doit être surchargée par les repositories enfants
     * car la logique de mise à jour varie selon les entités
     *
     * @param entity L'entité à mettre à jour
     * @return Nombre de lignes affectées
     */
    open fun update(entity: E): Int {
        // Implémentation par défaut - doit être surchargée
        entity.flushChanges()
        return 1
    }

    /**
     * Supprime une entité par son ID
     *
     * @param id L'identifiant de l'entité à supprimer
     * @return Nombre de lignes supprimées
     */
    open fun delete(id: Int): Int {
        return ArkaDatabase.instance.delete(table) {
            getPrimaryKeyColumn() eq id
        }
    }

    /**
     * Supprime une entité
     *
     * @param entity L'entité à supprimer
     * @return Nombre de lignes supprimées
     */
    open fun delete(entity: E): Int {
        return delete(entity.getPrimaryKey())
    }

    /**
     * Supprime toutes les entités
     * ATTENTION: Utiliser avec précaution !
     *
     * @return Nombre de lignes supprimées
     */
    open fun deleteAll(): Int {
        return ArkaDatabase.instance.delete(table) {
            // Condition qui est toujours vraie pour supprimer tout
            getPrimaryKeyColumn() greaterEq 0
        }
    }

    /**
     * Effectue une requête de recherche avec une condition personnalisée
     *
     * @param condition Lambda de condition de recherche
     * @return Liste des entités trouvées
     */
    protected fun findWhere(condition: (T) -> ColumnDeclaring<Boolean>): List<E> {
        return ArkaDatabase.instance.sequenceOf(table)
            .filter { condition(table) }
            .toList()
    }

    /**
     * Effectue une requête de comptage avec une condition personnalisée
     *
     * @param condition Lambda de condition de recherche
     * @return Nombre d'entités correspondantes
     */
    protected fun countWhere(condition: (T) -> ColumnDeclaring<Boolean>): Int {
        return ArkaDatabase.instance.from(table)
            .select(org.ktorm.dsl.count())
            .where(condition(table))
            .map { it.getInt(1) }
            .first()
    }

    /**
     * Vérifie si une entité est nouvelle (pas encore en base)
     *
     * @param entity L'entité à vérifier
     * @return true si l'entité est nouvelle
     */
    protected open fun isNewEntity(entity: E): Boolean {
        val primaryKey = entity.getPrimaryKey()
        return primaryKey == 0 || primaryKey < 0
    }

    /**
     * Exécute une transaction
     *
     * @param block Le code à exécuter dans la transaction
     * @return Le résultat du bloc
     */
    protected fun <R> transaction(block: () -> R): R {
        return ArkaDatabase.instance.useTransaction { block() }
    }

    /**
     * Méthode utilitaire pour la pagination
     *
     * @param page Numéro de page (commence à 0)
     * @param size Taille de la page
     * @return Liste paginée d'entités
     */
    open fun findPaginated(page: Int, size: Int): List<E> {
        return ArkaDatabase.instance.sequenceOf(table)
            .drop(page * size)
            .take(size)
            .toList()
    }

    /**
     * Vérifie la santé de la connexion à la base de données
     *
     * @return true si la connexion fonctionne
     */
    fun healthCheck(): Boolean {
        return try {
            ArkaDatabase.instance.useConnection { connection ->
                val result = connection.prepareStatement("SELECT 1").executeQuery()
                result.next() && result.getInt(1) == 1
            }
        } catch (e: Exception) {
            false
        }
    }
}