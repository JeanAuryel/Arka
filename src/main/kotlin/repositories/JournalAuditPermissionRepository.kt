package repositories

import org.ktorm.entity.sequenceOf
import org.ktorm.entity.find
import org.ktorm.entity.filter
import org.ktorm.entity.toList
import org.ktorm.dsl.*
import org.ktorm.schema.Column
import ktorm.*
import java.time.LocalDateTime

/**
 * Repository pour la gestion du journal d'audit des permissions
 *
 * Responsabilités:
 * - Enregistrement des logs d'audit des permissions
 * - Traçabilité complète des actions (création, modification, suppression)
 * - Génération de rapports d'audit et compliance
 * - Historique des accès et modifications
 * - Recherche et analyse des logs de sécurité
 *
 * Utilisé par: PermissionController, DelegationController, AuditService, SecurityController
 *
 * Note: Les entrées d'audit sont créées automatiquement par les triggers SQL
 * Ce repository sert principalement à la lecture et l'analyse des logs
 */
class JournalAuditPermissionRepository : BaseRepository<JournalAuditPermissionEntity, JournalAuditPermissions>() {

    override val table = JournalAuditPermissions

    /**
     * Obtient la clé primaire d'un log d'audit
     */
    override fun JournalAuditPermissionEntity.getPrimaryKey(): Int = this.logId
    override fun getPrimaryKeyColumn(): Column<Int> = JournalAuditPermissions.logId

    /**
     * Met à jour un log d'audit (généralement pas nécessaire)
     */
    override fun update(entity: JournalAuditPermissionEntity): Int {
        return ArkaDatabase.instance.update(JournalAuditPermissions) {
            set(it.details, entity.details)
            where { it.logId eq entity.logId }
        }
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR PERMISSION
    // ================================================================

    /**
     * Récupère tous les logs d'audit pour une permission spécifique
     *
     * @param permissionId ID de la permission
     * @return Liste chronologique des logs pour cette permission
     */
    fun getLogsByPermission(permissionId: Int): List<JournalAuditPermissionEntity> {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.permissionId eq permissionId }
            .toList()
            .sortedByDescending { it.dateAction ?: LocalDateTime.MIN }
    }

    /**
     * Récupère le dernier log d'audit pour une permission
     *
     * @param permissionId ID de la permission
     * @return Le log le plus récent ou null
     */
    fun getLatestLogByPermission(permissionId: Int): JournalAuditPermissionEntity? {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.permissionId eq permissionId }
            .toList()
            .maxByOrNull { it.dateAction ?: LocalDateTime.MIN }
    }

    /**
     * Compte le nombre de logs pour une permission
     *
     * @param permissionId ID de la permission
     * @return Nombre de logs d'audit
     */
    fun countLogsByPermission(permissionId: Int): Int {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.permissionId eq permissionId }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR UTILISATEUR
    // ================================================================

    /**
     * Récupère tous les logs d'audit des actions effectuées par un utilisateur
     *
     * @param effectueeParId ID de l'utilisateur qui a effectué l'action
     * @param limit Nombre maximum de logs à retourner
     * @return Liste des logs d'audit de cet utilisateur
     */
    fun getLogsByUser(effectueeParId: Int, limit: Int = 100): List<JournalAuditPermissionEntity> {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.effectueeParId eq effectueeParId }
            .toList()
            .sortedByDescending { it.dateAction ?: LocalDateTime.MIN }
            .take(limit)
    }

    /**
     * Récupère les logs d'audit d'un utilisateur pour une période donnée
     *
     * @param effectueeParId ID de l'utilisateur
     * @param since Date de début
     * @param until Date de fin (optionnelle)
     * @return Liste des logs de la période
     */
    fun getLogsByUserInPeriod(
        effectueeParId: Int,
        since: LocalDateTime,
        until: LocalDateTime? = null
    ): List<JournalAuditPermissionEntity> {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { log ->
                var condition = (log.effectueeParId eq effectueeParId) and
                        (log.dateAction greaterEq since)

                until?.let {
                    condition = condition and (log.dateAction lessEq it)
                }

                condition
            }
            .toList()
            .sortedByDescending { it.dateAction ?: LocalDateTime.MIN }
    }

    /**
     * Compte les actions effectuées par un utilisateur
     *
     * @param effectueeParId ID de l'utilisateur
     * @param since Date depuis laquelle compter (optionnelle)
     * @return Nombre d'actions
     */
    fun countActionsByUser(effectueeParId: Int, since: LocalDateTime? = null): Int {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { log ->
                val baseCondition = log.effectueeParId eq effectueeParId
                if (since != null) {
                    baseCondition and (log.dateAction greaterEq since)
                } else {
                    baseCondition
                }
            }
            .toList()
            .size
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE PAR ACTION
    // ================================================================

    /**
     * Récupère les logs par type d'action
     *
     * @param action Type d'action (CREATION, MODIFICATION, SUPPRESSION, etc.)
     * @param limit Nombre maximum de logs
     * @return Liste des logs pour cette action
     */
    fun getLogsByAction(action: String, limit: Int = 100): List<JournalAuditPermissionEntity> {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.action eq action }
            .toList()
            .sortedByDescending { it.dateAction ?: LocalDateTime.MIN }
            .take(limit)
    }

    /**
     * Récupère les logs par type d'action dans une période
     *
     * @param action Type d'action
     * @param since Date de début
     * @param until Date de fin (optionnelle)
     * @return Liste des logs de la période
     */
    fun getLogsByActionInPeriod(
        action: String,
        since: LocalDateTime,
        until: LocalDateTime? = null
    ): List<JournalAuditPermissionEntity> {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { log ->
                var condition = (log.action eq action) and (log.dateAction greaterEq since)

                until?.let {
                    condition = condition and (log.dateAction lessEq it)
                }

                condition
            }
            .toList()
            .sortedByDescending { it.dateAction ?: LocalDateTime.MIN }
    }

    /**
     * Obtient la liste de tous les types d'actions uniques
     *
     * @return Liste des types d'actions enregistrées
     */
    fun getAllActionTypes(): List<String> {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .toList()
            .map { it.action }
            .distinct()
            .sorted()
    }

    // ================================================================
    // MÉTHODES DE RECHERCHE AVANCÉE
    // ================================================================

    /**
     * Recherche dans les détails des logs d'audit
     *
     * @param searchTerm Terme à rechercher dans les détails
     * @param limit Nombre maximum de résultats
     * @return Liste des logs contenant le terme
     */
    fun searchInDetails(searchTerm: String, limit: Int = 50): List<JournalAuditPermissionEntity> {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.details like "%${searchTerm.trim()}%" }
            .toList()
            .sortedByDescending { it.dateAction ?: LocalDateTime.MIN }
            .take(limit)
    }

    /**
     * Récupère les logs d'audit récents
     *
     * @param hours Nombre d'heures passées à considérer
     * @param limit Nombre maximum de logs
     * @return Liste des logs récents
     */
    fun getRecentLogs(hours: Int = 24, limit: Int = 100): List<JournalAuditPermissionEntity> {
        val since = LocalDateTime.now().minusHours(hours.toLong())
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.dateAction greaterEq since }
            .toList()
            .sortedByDescending { it.dateAction ?: LocalDateTime.MIN }
            .take(limit)
    }

    /**
     * Récupère les logs d'audit pour une période spécifique
     *
     * @param since Date de début
     * @param until Date de fin
     * @param actions Liste des actions à inclure (optionnelle)
     * @return Liste des logs de la période
     */
    fun getLogsInPeriod(
        since: LocalDateTime,
        until: LocalDateTime,
        actions: List<String>? = null
    ): List<JournalAuditPermissionEntity> {
        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { log ->
                var condition = (log.dateAction greaterEq since) and (log.dateAction lessEq until)

                actions?.let { actionList ->
                    condition = condition and (log.action inList actionList)
                }

                condition
            }
            .toList()
            .sortedByDescending { it.dateAction ?: LocalDateTime.MIN }
    }

    // ================================================================
    // MÉTHODES DE CRÉATION DE LOGS (pour les cas non-automatiques)
    // ================================================================

    /**
     * Crée manuellement un log d'audit
     * Note: Normalement les logs sont créés automatiquement par les triggers
     * Cette méthode est pour les cas spéciaux ou les actions custom
     *
     * @param permissionId ID de la permission (0 pour actions générales)
     * @param action Type d'action
     * @param effectueeParId ID de l'utilisateur qui effectue l'action
     * @param details Détails de l'action
     * @return Le log créé ou null en cas d'erreur
     */
    fun createManualLog(
        permissionId: Int,
        action: String,
        effectueeParId: Int,
        details: String
    ): JournalAuditPermissionEntity? {
        return try {
            val log = JournalAuditPermissionEntity {
                this.permissionId = permissionId
                this.action = action.trim()
                this.effectueeParId = effectueeParId
                this.dateAction = LocalDateTime.now()
                this.details = details.trim()
            }

            create(log)
        } catch (e: Exception) {
            println("Erreur lors de la création manuelle du log d'audit: ${e.message}")
            null
        }
    }

    /**
     * Enregistre un accès à un fichier pour audit
     *
     * @param fichierId ID du fichier
     * @param effectueeParId ID de l'utilisateur
     * @param typeAcces Type d'accès (LECTURE, MODIFICATION, etc.)
     * @return true si l'enregistrement a réussi
     */
    fun logFileAccess(fichierId: Int, effectueeParId: Int, typeAcces: String): Boolean {
        val log = createManualLog(
            permissionId = 0, // Pas de permission spécifique
            action = "ACCES_FICHIER",
            effectueeParId = effectueeParId,
            details = "Fichier ID: $fichierId, Type: $typeAcces"
        )
        return log != null
    }

    // ================================================================
    // MÉTHODES DE GÉNÉRATION DE RAPPORTS
    // ================================================================

    /**
     * Génère un rapport d'audit pour une période donnée
     *
     * @param since Date de début
     * @param until Date de fin
     * @return Map avec les statistiques du rapport
     */
    fun generateAuditReport(since: LocalDateTime, until: LocalDateTime): Map<String, Any> {
        val logs = getLogsInPeriod(since, until)

        val actionStats = logs.groupBy { it.action }.mapValues { it.value.size }
        val userStats = logs.groupBy { it.effectueeParId }.mapValues { it.value.size }
        val dailyStats = logs.groupBy {
            it.dateAction?.toLocalDate()
        }.mapValues { it.value.size }

        val topUsers = userStats.toList().sortedByDescending { it.second }.take(10)
        val topActions = actionStats.toList().sortedByDescending { it.second }.take(10)

        return mapOf(
            "periode" to mapOf("debut" to since, "fin" to until),
            "totalLogs" to logs.size,
            "utilisateursActifs" to userStats.size,
            "typesActions" to actionStats.size,
            "statistiquesActions" to actionStats,
            "statistiquesUtilisateurs" to userStats,
            "statistiquesJournalieres" to dailyStats,
            "topUtilisateurs" to topUsers,
            "topActions" to topActions
        )
    }

    /**
     * Génère un rapport de sécurité pour un utilisateur
     *
     * @param effectueeParId ID de l'utilisateur
     * @param days Nombre de jours à analyser
     * @return Map avec le rapport de sécurité
     */
    fun generateUserSecurityReport(effectueeParId: Int, days: Int = 30): Map<String, Any?> {  // Changé Any en Any?
        val since = LocalDateTime.now().minusDays(days.toLong())
        val logs = getLogsByUserInPeriod(effectueeParId, since)

        val actionStats = logs.groupBy { it.action }.mapValues { it.value.size }
        val hourlyActivity = logs.groupBy {
            it.dateAction?.hour
        }.mapValues { it.value.size }

        val suspeciousActivity = logs.filter { log ->
            log.action in listOf("SUPPRESSION", "ACCES_REFUSE", "TENTATIVE_INTRUSION")
        }

        return mapOf(
            "utilisateurId" to effectueeParId,
            "periode" to days,
            "totalActions" to logs.size,
            "actionsParType" to actionStats,
            "activiteParHeure" to hourlyActivity,
            "activitesSuspectes" to suspeciousActivity.size,
            "derniereActivite" to logs.maxByOrNull { it.dateAction ?: LocalDateTime.MIN }?.dateAction
        )
    }

    /**
     * Détecte les activités suspectes récentes
     *
     * @param hours Nombre d'heures passées à analyser
     * @return Liste des logs suspects
     */
    fun detectSuspiciousActivity(hours: Int = 24): List<JournalAuditPermissionEntity> {
        val since = LocalDateTime.now().minusHours(hours.toLong())
        val suspiciousActions = listOf("TENTATIVE_INTRUSION", "ACCES_REFUSE", "MULTIPLE_ECHECS")

        return ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { log ->
                (log.dateAction greaterEq since) and (log.action inList suspiciousActions)
            }
            .toList()
            .sortedByDescending { it.dateAction ?: LocalDateTime.MIN }
    }

    // ================================================================
    // MÉTHODES DE STATISTIQUES
    // ================================================================

    /**
     * Obtient les statistiques générales d'audit
     *
     * @param days Nombre de jours à analyser
     * @return Map avec les statistiques
     */
    fun getAuditStatistics(days: Int = 30): Map<String, Any> {
        val since = LocalDateTime.now().minusDays(days.toLong())
        val logs = ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.dateAction greaterEq since }
            .toList()

        val actionFrequency = logs.groupBy { it.action }.mapValues { it.value.size }
        val userActivity = logs.groupBy { it.effectueeParId }.mapValues { it.value.size }
        val averageActionsPerDay = if (days > 0) logs.size / days else 0

        return mapOf(
            "totalLogs" to logs.size,
            "utilisateursActifs" to userActivity.size,
            "actionsMoyennesParJour" to averageActionsPerDay,
            "actionLaPlusFrequente" to (actionFrequency.maxByOrNull { it.value }?.key ?: "Aucune"),
            "utilisateurLePlusActif" to (userActivity.maxByOrNull { it.value }?.key ?: 0)
        )
    }

    /**
     * Compte les logs par jour sur une période
     *
     * @param days Nombre de jours à analyser
     * @return Map jour -> nombre de logs
     */
    fun getLogCountByDay(days: Int = 7): Map<String, Int> {
        val since = LocalDateTime.now().minusDays(days.toLong())
        val logs = ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.dateAction greaterEq since }
            .toList()

        return logs.groupBy {
            it.dateAction?.toLocalDate()?.toString() ?: "Unknown"
        }.mapValues { it.value.size }
    }

    // ================================================================
    // MÉTHODES DE NETTOYAGE ET MAINTENANCE
    // ================================================================

    /**
     * Archive les anciens logs d'audit
     *
     * @param daysThreshold Nombre de jours après lesquels archiver
     * @return Nombre de logs archivés (supprimés)
     */
    fun archiveOldLogs(daysThreshold: Int = 365): Int {
        return try {
            val threshold = LocalDateTime.now().minusDays(daysThreshold.toLong())
            ArkaDatabase.instance.delete(JournalAuditPermissions) {
                it.dateAction less threshold
            }
        } catch (e: Exception) {
            println("Erreur lors de l'archivage des anciens logs: ${e.message}")
            0
        }
    }

    /**
     * Nettoie les logs orphelins (permissions inexistantes)
     *
     * @return Nombre de logs nettoyés
     */
    fun cleanOrphanLogs(): Int {
        return try {
            // Récupérer les IDs de permissions existantes
            val existingPermissions = ArkaDatabase.instance.sequenceOf(PermissionsActives)
                .toList()
                .map { it.permissionId }
                .toSet()

            // Supprimer les logs dont la permission n'existe plus (sauf les logs avec permissionId = 0)
            val orphanLogs = ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
                .toList()
                .filter { it.permissionId != 0 && it.permissionId !in existingPermissions }

            var deleted = 0
            orphanLogs.forEach { log ->
                if (delete(log.logId) > 0) {
                    deleted++
                }
            }

            deleted
        } catch (e: Exception) {
            println("Erreur lors du nettoyage des logs orphelins: ${e.message}")
            0
        }
    }

    /**
     * Obtient la taille approximative du journal d'audit
     *
     * @return Nombre total de logs dans le système
     */
    fun getAuditLogSize(): Int {
        return findAll().size
    }

    /**
     * Vérifie l'intégrité du journal d'audit
     *
     * @return Map avec les résultats de la vérification
     */
    fun checkAuditIntegrity(): Map<String, Any> {
        val totalLogs = getAuditLogSize()
        val logsWithoutDate = ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.dateAction.isNull() }
            .toList()
            .size

        val logsWithoutAction = ArkaDatabase.instance.sequenceOf(JournalAuditPermissions)
            .filter { it.action.isNull() }
            .toList()
            .size

        val orphanLogs = try {
            cleanOrphanLogs() // Compte les orphelins sans les supprimer
        } catch (e: Exception) {
            -1
        }

        return mapOf(
            "totalLogs" to totalLogs,
            "logsSansDate" to logsWithoutDate,
            "logsSansAction" to logsWithoutAction,
            "logsOrphelins" to orphanLogs,
            "integrite" to (logsWithoutDate == 0 && logsWithoutAction == 0)
        )
    }
}