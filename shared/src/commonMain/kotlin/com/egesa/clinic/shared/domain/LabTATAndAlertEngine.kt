package com.egesa.clinic.shared.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Turnaround Time (TAT) Tracking Engine
 * Monitors and analyzes lab order processing times
 */
class LabTATEngine {

    /**
     * Calculate actual TAT from order to report
     */
    fun calculateTAT(tracking: LabTATTracking): TATCalculation {
        if (tracking.resultReportedAt == null) {
            return TATCalculation(
                isComplete = false,
                actualTatHours = null,
                tatStatus = "IN_PROGRESS",
                complianceStatus = "PENDING",
            )
        }

        val orderedInstant = parseToInstant(tracking.orderedAt)
        val reportedInstant = parseToInstant(tracking.resultReportedAt!!)

        val actualHours = calculateHoursDifference(orderedInstant, reportedInstant)
        val tatMet = actualHours <= tracking.expectedTatHours

        return TATCalculation(
            isComplete = true,
            actualTatHours = actualHours,
            expectedTatHours = tracking.expectedTatHours,
            tatStatus = if (tatMet) "COMPLETE_ON_TIME" else "COMPLETE_LATE",
            complianceStatus = if (tatMet) "COMPLIANT" else "NON_COMPLIANT",
            hoursOverdue = if (!tatMet) actualHours - tracking.expectedTatHours else null,
        )
    }

    /**
     * Check if order is close to TAT breach
     */
    fun checkTATCompliance(tracking: LabTATTracking): TATComplianceStatus {
        val now = Clock.System.now()
        val orderedInstant = parseToInstant(tracking.orderedAt)
        val elapsedHours = calculateHoursDifference(orderedInstant, now)

        val warningThreshold = tracking.expectedTatHours * 0.75  // 75% of TAT
        val criticalThreshold = tracking.expectedTatHours * 0.90  // 90% of TAT

        return when {
            elapsedHours >= tracking.expectedTatHours -> {
                TATComplianceStatus(
                    status = "BREACHED",
                    hoursRemaining = 0,
                    percentageUsed = 100.0,
                    isAlert = true,
                    message = "TAT BREACHED - Order is ${elapsedHours - tracking.expectedTatHours} hours overdue"
                )
            }
            elapsedHours >= criticalThreshold -> {
                TATComplianceStatus(
                    status = "CRITICAL",
                    hoursRemaining = (tracking.expectedTatHours - elapsedHours).toInt(),
                    percentageUsed = (elapsedHours / tracking.expectedTatHours) * 100,
                    isAlert = true,
                    message = "TAT critical - ${(tracking.expectedTatHours - elapsedHours).toInt()} hours remaining"
                )
            }
            elapsedHours >= warningThreshold -> {
                TATComplianceStatus(
                    status = "WARNING",
                    hoursRemaining = (tracking.expectedTatHours - elapsedHours).toInt(),
                    percentageUsed = (elapsedHours / tracking.expectedTatHours) * 100,
                    isAlert = false,
                    message = "TAT approaching - ${(tracking.expectedTatHours - elapsedHours).toInt()} hours remaining"
                )
            }
            else -> {
                TATComplianceStatus(
                    status = "ON_TRACK",
                    hoursRemaining = (tracking.expectedTatHours - elapsedHours).toInt(),
                    percentageUsed = (elapsedHours / tracking.expectedTatHours) * 100,
                    isAlert = false,
                    message = "TAT on track"
                )
            }
        }
    }

    /**
     * Identify bottlenecks in the lab workflow
     */
    fun identifyBottlenecks(tracking: LabTATTracking): List<String> {
        val bottlenecks = mutableListOf<String>()

        // Check specimen collection delay
        if (tracking.specimenCollectedAt != null) {
            val collectionDelay = calculateHoursDifference(
                parseToInstant(tracking.orderedAt),
                parseToInstant(tracking.specimenCollectedAt!!)
            )
            if (collectionDelay > 2) {
                bottlenecks.add("Specimen collection delayed: ${collectionDelay.toInt()} hours")
            }
        }

        // Check specimen receiving delay
        if (tracking.specimenReceivedAt != null && tracking.specimenCollectedAt != null) {
            val receivingDelay = calculateHoursDifference(
                parseToInstant(tracking.specimenCollectedAt!!),
                parseToInstant(tracking.specimenReceivedAt!!)
            )
            if (receivingDelay > 1) {
                bottlenecks.add("Specimen receiving delayed: ${receivingDelay.toInt()} hours")
            }
        }

        // Check analysis delay
        if (tracking.analysisStartedAt != null && tracking.specimenReceivedAt != null) {
            val analysisStartDelay = calculateHoursDifference(
                parseToInstant(tracking.specimenReceivedAt!!),
                parseToInstant(tracking.analysisStartedAt!!)
            )
            if (analysisStartDelay > 2) {
                bottlenecks.add("Analysis startup delayed: ${analysisStartDelay.toInt()} hours")
            }
        }

        // Check verification delay
        if (tracking.resultVerifiedAt != null && tracking.analysisCompletedAt != null) {
            val verificationDelay = calculateHoursDifference(
                parseToInstant(tracking.analysisCompletedAt!!),
                parseToInstant(tracking.resultVerifiedAt!!)
            )
            if (verificationDelay > 1) {
                bottlenecks.add("Result verification delayed: ${verificationDelay.toInt()} hours")
            }
        }

        return bottlenecks
    }

    private fun parseToInstant(isoString: String): Instant {
        return try {
            Instant.parse(isoString)
        } catch (e: Exception) {
            Clock.System.now()
        }
    }

    private fun calculateHoursDifference(from: Instant, to: Instant): Double {
        val diffMs = (to.toEpochMilliseconds() - from.toEpochMilliseconds()).toLong()
        return diffMs / (1000.0 * 60.0 * 60.0)
    }
}

data class TATCalculation(
    val isComplete: Boolean,
    val actualTatHours: Double?,
    val expectedTatHours: Int? = null,
    val tatStatus: String,  // "IN_PROGRESS", "COMPLETE_ON_TIME", "COMPLETE_LATE"
    val complianceStatus: String,  // "PENDING", "COMPLIANT", "NON_COMPLIANT"
    val hoursOverdue: Double? = null,
)

data class TATComplianceStatus(
    val status: String,  // "ON_TRACK", "WARNING", "CRITICAL", "BREACHED"
    val hoursRemaining: Int,
    val percentageUsed: Double,
    val isAlert: Boolean,
    val message: String,
)

/**
 * Critical Value Alert Engine
 */
class LabCriticalAlertEngine {

    /**
     * Create critical alert for a result
     */
    fun createCriticalAlert(
        result: LabResultExtended,
        test: LabTestDetail,
        order: LabOrder,
    ): LabCriticalAlert {
        val severity = when {
            result.isCriticalValue -> AlertSeverity.CRITICAL
            result.flag == LabResultFlag.HIGH || result.flag == LabResultFlag.LOW -> AlertSeverity.WARNING
            else -> AlertSeverity.INFO
        }

        val thresholdType = when {
            result.valueNumeric != null && test.criticalValueHigh != null && result.valueNumeric > test.criticalValueHigh -> "HIGH"
            result.valueNumeric != null && test.criticalValueLow != null && result.valueNumeric < test.criticalValueLow -> "LOW"
            else -> "ABNORMAL"
        }

        return LabCriticalAlert(
            id = "ALERT-${Clock.System.now().toEpochMilliseconds()}",
            resultId = result.id,
            orderId = order.id,
            patientId = order.patientId,
            testName = test.name,
            criticalValue = result.valueNumeric ?: 0.0,
            thresholdType = thresholdType,
            alertSeverity = severity,
            notifiedToRoles = getNotificationRoles(severity),
            notificationSentAt = null,
            acknowledgedBy = null,
            acknowledgedAt = null,
            acknowledgementNotes = null,
            createdAt = Clock.System.now().toString(),
        )
    }

    /**
     * Get roles that should be notified based on severity
     */
    private fun getNotificationRoles(severity: AlertSeverity): List<String> {
        return when (severity) {
            AlertSeverity.CRITICAL -> listOf("DOCTOR", "LAB_SUPERVISOR", "NURSE", "ADMIN")
            AlertSeverity.WARNING -> listOf("DOCTOR", "LAB_SUPERVISOR")
            AlertSeverity.INFO -> listOf("LAB_TECHNICIAN", "LAB_SUPERVISOR")
        }
    }

    /**
     * Mark alert as acknowledged
     */
    fun acknowledgeAlert(
        alert: LabCriticalAlert,
        acknowledgedBy: String,
        notes: String? = null,
    ): LabCriticalAlert {
        return alert.copy(
            acknowledgedBy = acknowledgedBy,
            acknowledgedAt = Clock.System.now().toString(),
            acknowledgementNotes = notes,
        )
    }

    /**
     * Check if alert needs escalation (not acknowledged within timeframe)
     */
    fun checkEscalation(alert: LabCriticalAlert): EscalationStatus {
        if (alert.acknowledgedAt != null) {
            return EscalationStatus(
                needsEscalation = false,
                message = "Alert acknowledged by ${alert.acknowledgedBy}"
            )
        }

        val createdInstant = try {
            Instant.parse(alert.createdAt)
        } catch (e: Exception) {
            Clock.System.now()
        }

        val now = Clock.System.now()
        val minutesSinceCreation = (now.toEpochMilliseconds() - createdInstant.toEpochMilliseconds()) / (1000.0 * 60.0)

        return when {
            minutesSinceCreation > 60 && alert.alertSeverity == AlertSeverity.CRITICAL -> {
                EscalationStatus(
                    needsEscalation = true,
                    message = "CRITICAL alert not acknowledged for ${minutesSinceCreation.toInt()} minutes - ESCALATE TO ADMIN"
                )
            }
            minutesSinceCreation > 120 && alert.alertSeverity == AlertSeverity.WARNING -> {
                EscalationStatus(
                    needsEscalation = true,
                    message = "WARNING alert not acknowledged for ${minutesSinceCreation.toInt()} minutes"
                )
            }
            else -> {
                EscalationStatus(
                    needsEscalation = false,
                    message = "Alert within escalation timeframe"
                )
            }
        }
    }
}

data class EscalationStatus(
    val needsEscalation: Boolean,
    val message: String,
)

