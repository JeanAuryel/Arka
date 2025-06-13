package controllers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import repositories.AlertRepository
import ktorm.Alerte
import ktorm.UniteIntervalle
import ktorm.MembreFamille
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Controller responsible for alert/notification management operations.
 * Based on the Alerte entity from ktorm models.
 *
 * Key Responsibilities:
 * - CRUD operations on alerts
 * - Alert scheduling and triggering
 * - Notification delivery management
 * - User-specific alert preferences
 * - Recurring alert calculations
 *
 * Alert Types in Arka:
 * - Document expiration reminders
 * - Periodic backup notifications
 * - System maintenance alerts
 * - Custom user reminders
 */
class AlertController(
    private val alertRepository: AlertRepository,
    private val authController: AuthController
) {

    /**
     * Result wrapper for alert operations
     */
    sealed class AlertResult {
        data class Success<T>(val data: T) : AlertResult()
        data class Error(val message: String, val code: AlertErrorCode) : AlertResult()
    }

    enum class AlertErrorCode {
        NOT_FOUND,
        INVALID_INPUT,
        PERMISSION_DENIED,
        INVALID_SCHEDULE,
        INTERNAL_ERROR,
        ALERT_DISABLED
    }

    /**
     * Get all alerts for the current user
     */
    suspend fun getUserAlerts(includeInactive: Boolean = false): AlertResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext AlertResult.Error(
                    "Utilisateur non connecté",
                    AlertErrorCode.PERMISSION_DENIED
                )

            val alerts = if (includeInactive) {
                alertRepository.findByMemberId(currentUser.membreFamilleId)
            } else {
                alertRepository.findActiveByMemberId(currentUser.membreFamilleId)
            }

            return@withContext AlertResult.Success(alerts)

        } catch (e: Exception) {
            return@withContext AlertResult.Error(
                "Erreur lors de la récupération des alertes: ${e.message}",
                AlertErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Get a specific alert by ID
     */
    suspend fun getAlertById(alertId: Int): AlertResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext AlertResult.Error(
                    "Utilisateur non connecté",
                    AlertErrorCode.PERMISSION_DENIED
                )

            val alert = alertRepository.findById(alertId)
                ?: return@withContext AlertResult.Error(
                    "Alerte non trouvée",
                    AlertErrorCode.NOT_FOUND
                )

            // Check if user owns this alert or is admin
            if (alert.membreFamilleId != currentUser.membreFamilleId && !currentUser.estAdmin) {
                return@withContext AlertResult.Error(
                    "Accès refusé à cette alerte",
                    AlertErrorCode.PERMISSION_DENIED
                )
            }

            return@withContext AlertResult.Success(alert)

        } catch (e: Exception) {
            return@withContext AlertResult.Error(
                "Erreur lors de la récupération de l'alerte: ${e.message}",
                AlertErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Create a new alert
     * Based on ktorm Alerte model
     */
    suspend fun createAlert(
        type: String,
        category: String? = null,
        intervalUnit: UniteIntervalle,
        intervalValue: Int,
        firstTriggerDate: LocalDateTime? = null
    ): AlertResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext AlertResult.Error(
                    "Utilisateur non connecté",
                    AlertErrorCode.PERMISSION_DENIED
                )

            // Validation based on ktorm Alerte model
            if (type.isBlank()) {
                return@withContext AlertResult.Error(
                    "Le type d'alerte est requis",
                    AlertErrorCode.INVALID_INPUT
                )
            }

            if (intervalValue <= 0) {
                return@withContext AlertResult.Error(
                    "L'intervalle doit être positif",
                    AlertErrorCode.INVALID_INPUT
                )
            }

            if (intervalValue > 365 && intervalUnit == UniteIntervalle.JOUR) {
                return@withContext AlertResult.Error(
                    "L'intervalle en jours ne peut pas dépasser 365",
                    AlertErrorCode.INVALID_INPUT
                )
            }

            // Calculate first trigger date if not provided
            val nextTrigger = firstTriggerDate ?: calculateNextTriggerDate(
                LocalDateTime.now(),
                intervalUnit,
                intervalValue
            )

            // Create alert based on ktorm Alerte model
            val newAlert = Alerte(
                alerteId = 0, // Auto-generated
                typeAlerte = type.trim(),
                categorieAlerte = category?.trim(),
                uniteIntervalleAlerte = intervalUnit,
                valeurIntervalleAlerte = intervalValue,
                dateProchainDeclenchement = nextTrigger,
                dateDernierDeclenchement = null,
                estActive = true,
                dateCreationAlerte = LocalDateTime.now(),
                membreFamilleId = currentUser.membreFamilleId
            )

            val createdAlert = alertRepository.create(newAlert)

            return@withContext AlertResult.Success(createdAlert)

        } catch (e: Exception) {
            return@withContext AlertResult.Error(
                "Erreur lors de la création de l'alerte: ${e.message}",
                AlertErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Update an existing alert
     */
    suspend fun updateAlert(
        alertId: Int,
        type: String? = null,
        category: String? = null,
        intervalUnit: UniteIntervalle? = null,
        intervalValue: Int? = null,
        isActive: Boolean? = null
    ): AlertResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext AlertResult.Error(
                    "Utilisateur non connecté",
                    AlertErrorCode.PERMISSION_DENIED
                )

            val existingAlert = alertRepository.findById(alertId)
                ?: return@withContext AlertResult.Error(
                    "Alerte non trouvée",
                    AlertErrorCode.NOT_FOUND
                )

            // Permission check
            if (existingAlert.membreFamilleId != currentUser.membreFamilleId && !currentUser.estAdmin) {
                return@withContext AlertResult.Error(
                    "Permissions insuffisantes pour modifier cette alerte",
                    AlertErrorCode.PERMISSION_DENIED
                )
            }

            // Build updated alert with only non-null values
            var updatedAlert = existingAlert

            type?.let {
                if (it.isBlank()) {
                    return@withContext AlertResult.Error(
                        "Le type d'alerte ne peut pas être vide",
                        AlertErrorCode.INVALID_INPUT
                    )
                }
                updatedAlert = updatedAlert.copy(typeAlerte = it.trim())
            }

            category?.let {
                updatedAlert = updatedAlert.copy(categorieAlerte = it.trim())
            }

            intervalUnit?.let {
                updatedAlert = updatedAlert.copy(uniteIntervalleAlerte = it)
            }

            intervalValue?.let {
                if (it <= 0) {
                    return@withContext AlertResult.Error(
                        "L'intervalle doit être positif",
                        AlertErrorCode.INVALID_INPUT
                    )
                }
                updatedAlert = updatedAlert.copy(valeurIntervalleAlerte = it)
            }

            isActive?.let {
                updatedAlert = updatedAlert.copy(estActive = it)
            }

            // Recalculate next trigger if interval changed
            if (intervalUnit != null || intervalValue != null) {
                val newNextTrigger = calculateNextTriggerDate(
                    existingAlert.dateDernierDeclenchement ?: LocalDateTime.now(),
                    updatedAlert.uniteIntervalleAlerte,
                    updatedAlert.valeurIntervalleAlerte
                )
                updatedAlert = updatedAlert.copy(dateProchainDeclenchement = newNextTrigger)
            }

            alertRepository.update(updatedAlert)

            return@withContext AlertResult.Success(updatedAlert)

        } catch (e: Exception) {
            return@withContext AlertResult.Error(
                "Erreur lors de la mise à jour de l'alerte: ${e.message}",
                AlertErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Delete an alert
     */
    suspend fun deleteAlert(alertId: Int): AlertResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext AlertResult.Error(
                    "Utilisateur non connecté",
                    AlertErrorCode.PERMISSION_DENIED
                )

            val alert = alertRepository.findById(alertId)
                ?: return@withContext AlertResult.Error(
                    "Alerte non trouvée",
                    AlertErrorCode.NOT_FOUND
                )

            // Permission check
            if (alert.membreFamilleId != currentUser.membreFamilleId && !currentUser.estAdmin) {
                return@withContext AlertResult.Error(
                    "Permissions insuffisantes pour supprimer cette alerte",
                    AlertErrorCode.PERMISSION_DENIED
                )
            }

            alertRepository.delete(alertId)

            return@withContext AlertResult.Success(alert)

        } catch (e: Exception) {
            return@withContext AlertResult.Error(
                "Erreur lors de la suppression de l'alerte: ${e.message}",
                AlertErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Toggle alert active/inactive status
     */
    suspend fun toggleAlert(alertId: Int): AlertResult = withContext(Dispatchers.IO) {
        try {
            val alert = alertRepository.findById(alertId)
                ?: return@withContext AlertResult.Error(
                    "Alerte non trouvée",
                    AlertErrorCode.NOT_FOUND
                )

            return updateAlert(alertId, isActive = !alert.estActive)

        } catch (e: Exception) {
            return@withContext AlertResult.Error(
                "Erreur lors du basculement de l'alerte: ${e.message}",
                AlertErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Get alerts that need to be triggered now
     * This would typically be called by a background service
     */
    suspend fun getAlertsToTrigger(): AlertResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext AlertResult.Error(
                    "Utilisateur non connecté",
                    AlertErrorCode.PERMISSION_DENIED
                )

            val now = LocalDateTime.now()
            val alertsToTrigger = alertRepository.findAlertsToTrigger(now)

            // Filter by family if not admin
            val filteredAlerts = if (currentUser.estAdmin) {
                alertsToTrigger
            } else {
                alertsToTrigger.filter { it.membreFamilleId == currentUser.membreFamilleId }
            }

            return@withContext AlertResult.Success(filteredAlerts)

        } catch (e: Exception) {
            return@withContext AlertResult.Error(
                "Erreur lors de la récupération des alertes à déclencher: ${e.message}",
                AlertErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Trigger an alert manually or mark it as triggered
     */
    suspend fun triggerAlert(alertId: Int): AlertResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext AlertResult.Error(
                    "Utilisateur non connecté",
                    AlertErrorCode.PERMISSION_DENIED
                )

            val alert = alertRepository.findById(alertId)
                ?: return@withContext AlertResult.Error(
                    "Alerte non trouvée",
                    AlertErrorCode.NOT_FOUND
                )

            // Permission check
            if (alert.membreFamilleId != currentUser.membreFamilleId && !currentUser.estAdmin) {
                return@withContext AlertResult.Error(
                    "Permissions insuffisantes",
                    AlertErrorCode.PERMISSION_DENIED
                )
            }

            if (!alert.estActive) {
                return@withContext AlertResult.Error(
                    "L'alerte est désactivée",
                    AlertErrorCode.ALERT_DISABLED
                )
            }

            val now = LocalDateTime.now()
            val nextTrigger = calculateNextTriggerDate(
                now,
                alert.uniteIntervalleAlerte,
                alert.valeurIntervalleAlerte
            )

            // Update alert with trigger information
            val triggeredAlert = alert.copy(
                dateDernierDeclenchement = now,
                dateProchainDeclenchement = nextTrigger
            )

            alertRepository.update(triggeredAlert)

            return@withContext AlertResult.Success(
                AlertTriggerResult(
                    alert = triggeredAlert,
                    triggeredAt = now,
                    nextTrigger = nextTrigger,
                    message = "Alerte '${alert.typeAlerte}' déclenchée avec succès"
                )
            )

        } catch (e: Exception) {
            return@withContext AlertResult.Error(
                "Erreur lors du déclenchement de l'alerte: ${e.message}",
                AlertErrorCode.INTERNAL_ERROR
            )
        }
    }

    /**
     * Get alert statistics for current user
     */
    suspend fun getAlertStatistics(): AlertResult = withContext(Dispatchers.IO) {
        try {
            val currentUser = authController.getCurrentUser()
                ?: return@withContext AlertResult.Error(
                    "Utilisateur non connecté",
                    AlertErrorCode.PERMISSION_DENIED
                )

            val totalAlerts = alertRepository.countByMemberId(currentUser.membreFamilleId)
            val activeAlerts = alertRepository.countActiveByMemberId(currentUser.membreFamilleId)
            val pendingAlerts = alertRepository.countPendingByMemberId(currentUser.membreFamilleId)
            val recentlyTriggered = alertRepository.countRecentlyTriggeredByMemberId(
                currentUser.membreFamilleId,
                LocalDateTime.now().minusDays(7)
            )

            val statistics = AlertStatistics(
                totalAlerts = totalAlerts,
                activeAlerts = activeAlerts,
                inactiveAlerts = totalAlerts - activeAlerts,
                pendingAlerts = pendingAlerts,
                recentlyTriggered = recentlyTriggered
            )

            return@withContext AlertResult.Success(statistics)

        } catch (e: Exception) {
            return@withContext AlertResult.Error(
                "Erreur lors de la récupération des statistiques: ${e.message}",
                AlertErrorCode.INTERNAL_ERROR
            )
        }
    }

    // Private helper methods

    /**
     * Calculate next trigger date based on interval
     * Uses ktorm UniteIntervalle enum
     */
    private fun calculateNextTriggerDate(
        fromDate: LocalDateTime,
        unit: UniteIntervalle,
        value: Int
    ): LocalDateTime {
        return when (unit) {
            UniteIntervalle.JOUR -> fromDate.plusDays(value.toLong())
            UniteIntervalle.SEMAINE -> fromDate.plusWeeks(value.toLong())
            UniteIntervalle.MOIS -> fromDate.plusMonths(value.toLong())
            UniteIntervalle.ANNEE -> fromDate.plusYears(value.toLong())
        }
    }

    /**
     * Check if alert schedule is valid
     */
    private fun isValidSchedule(unit: UniteIntervalle, value: Int): Boolean {
        return when (unit) {
            UniteIntervalle.JOUR -> value in 1..365
            UniteIntervalle.SEMAINE -> value in 1..52
            UniteIntervalle.MOIS -> value in 1..12
            UniteIntervalle.ANNEE -> value in 1..10
        }
    }
}

/**
 * Data classes for alert operations results
 * Based on ktorm Alerte model
 */
data class AlertTriggerResult(
    val alert: Alerte,
    val triggeredAt: LocalDateTime,
    val nextTrigger: LocalDateTime,
    val message: String
)

data class AlertStatistics(
    val totalAlerts: Int,
    val activeAlerts: Int,
    val inactiveAlerts: Int,
    val pendingAlerts: Int,
    val recentlyTriggered: Int
)

/**
 * Predefined alert types for Arka
 */
object AlertTypes {
    const val DOCUMENT_EXPIRATION = "DOCUMENT_EXPIRATION"
    const val BACKUP_REMINDER = "BACKUP_REMINDER"
    const val SYSTEM_MAINTENANCE = "SYSTEM_MAINTENANCE"
    const val CUSTOM_REMINDER = "CUSTOM_REMINDER"
    const val FILE_CLEANUP = "FILE_CLEANUP"
    const val PASSWORD_CHANGE = "PASSWORD_CHANGE"

    fun getAllTypes(): List<String> = listOf(
        DOCUMENT_EXPIRATION,
        BACKUP_REMINDER,
        SYSTEM_MAINTENANCE,
        CUSTOM_REMINDER,
        FILE_CLEANUP,
        PASSWORD_CHANGE
    )
}