package com.egesa.clinic.shared.domain

import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.datetime.Clock

/**
 * Quality Control Engine for lab operations
 * Handles QC material tracking, Levey-Jennings chart analysis, and QC performance monitoring
 */
class LabQCEngine {

    /**
     * Assess QC result against expected values
     */
    fun assessQCResult(
        result: LabQCResult,
        material: LabQCMaterial,
    ): QCAssessment {
        val deviationPercent = calculateDeviation(
            result.resultValue,
            material.expectedValue,
        )

        val standardDeviation = calculateStandardDeviation(
            result.resultValue,
            material.expectedValue,
        )

        val status = when {
            deviationPercent > 3.0 || standardDeviation > 3.0 -> {
                QCResultStatus.FAILED
            }
            deviationPercent > 2.0 || standardDeviation > 2.0 -> {
                QCResultStatus.WARNING
            }
            else -> {
                QCResultStatus.PASSED
            }
        }

        val message = when (status) {
            QCResultStatus.PASSED -> "QC passed - Result within acceptable range"
            QCResultStatus.WARNING -> "QC warning - Result is ${formatDouble(deviationPercent)}% from expected"
            QCResultStatus.FAILED -> "QC FAILED - Result is ${formatDouble(deviationPercent)}% from expected"
        }

        val recommendations = mutableListOf<String>()
        if (status == QCResultStatus.FAILED) {
            recommendations.add("✗ STOP testing - Equipment requires recalibration")
            recommendations.add("✗ All recent patient results may need review")
            recommendations.add("✗ Contact equipment technician immediately")
        } else if (status == QCResultStatus.WARNING) {
            recommendations.add("⚠ Monitor equipment performance closely")
            recommendations.add("⚠ Run additional QC material")
            recommendations.add("⚠ Consider preventive maintenance")
        }

        return QCAssessment(
            status = status,
            deviationPercent = deviationPercent,
            standardDeviation = standardDeviation,
            message = message,
            recommendations = recommendations,
            isEquipmentSuitable = status != QCResultStatus.FAILED,
        )
    }

    /**
     * Check if QC material is still valid
     */
    fun isQCMaterialValid(material: LabQCMaterial): MaterialValidityStatus {
        val now = Clock.System.now().toString()

        return when {
            material.expiryDate < now -> {
                MaterialValidityStatus(
                    isValid = false,
                    reason = "Material expired on ${material.expiryDate}",
                    action = "REMOVE_FROM_USE"
                )
            }
            !material.isActive -> {
                MaterialValidityStatus(
                    isValid = false,
                    reason = "Material is inactive",
                    action = "REMOVE_FROM_USE"
                )
            }
            else -> {
                MaterialValidityStatus(
                    isValid = true,
                    reason = "Material is valid",
                    action = "OK"
                )
            }
        }
    }

    /**
     * Calculate percentage deviation from expected
     */
    private fun calculateDeviation(actual: Double, expected: Double): Double {
        if (expected == 0.0) return 0.0
        return ((actual - expected) / abs(expected)) * 100
    }

    /**
     * Calculate standard deviation multiple (z-score)
     */
    private fun calculateStandardDeviation(actual: Double, expected: Double): Double {
        // Simplified: Assume ~2% standard deviation as typical for QC
        val estimatedSD = expected * 0.02
        if (estimatedSD == 0.0) return 0.0
        return (actual - expected) / estimatedSD
    }

    /**
     * Analyze QC trend using Levey-Jennings chart rules
     */
    fun analyzeLeveyJennsingsTrend(
        recentResults: List<LabQCResult>,
        material: LabQCMaterial,
    ): TrendAnalysis {
        if (recentResults.isEmpty()) {
            return TrendAnalysis(
                isNormal = true,
                alerts = emptyList(),
                message = "Insufficient data for trend analysis"
            )
        }

        val alerts = mutableListOf<String>()

        // Rule 1: 10x rule - 10 consecutive results on same side of mean
        val sideRule10x = check10xRule(recentResults, material)
        if (!sideRule10x) alerts.add("Rule 1-10x: 10 consecutive results on same side - FAILED")

        // Rule 2: 2-2s rule - 2 consecutive results > 2 SD from mean
        val side2s = check2sRule(recentResults, material)
        if (!side2s) alerts.add("Rule 2-2s: 2 consecutive > 2 SD - FAILED")

        // Rule 3: R-4s rule - Range between consecutive results > 4 SD
        val range4s = checkR4sRule(recentResults, material)
        if (!range4s) alerts.add("Rule R-4s: Range > 4 SD - FAILED")

        // Rule 4: 4-1s rule - 4 consecutive results > 1 SD from mean (same side)
        val side4s = check4s1sRule(recentResults, material)
        if (!side4s) alerts.add("Rule 4-1s: 4 consecutive > 1 SD - FAILED")

        return TrendAnalysis(
            isNormal = alerts.isEmpty(),
            alerts = alerts,
            message = if (alerts.isEmpty()) "QC trend is normal" else "QC alerts detected - review equipment"
        )
    }

    private fun check10xRule(results: List<LabQCResult>, material: LabQCMaterial): Boolean {
        if (results.size < 10) return true

        val last10 = results.takeLast(10)
        val meanValue = material.expectedValue

        val allAbove = last10.all { it.resultValue >= meanValue }
        val allBelow = last10.all { it.resultValue <= meanValue }

        return !(allAbove || allBelow)
    }

    private fun check2sRule(results: List<LabQCResult>, material: LabQCMaterial): Boolean {
        if (results.size < 2) return true

        val last2 = results.takeLast(2)
        val estimatedSD = material.expectedValue * 0.02
        val threshold = 2.0 * estimatedSD

        return !last2.all {
            abs(it.resultValue - material.expectedValue) > threshold
        }
    }

    private fun checkR4sRule(results: List<LabQCResult>, material: LabQCMaterial): Boolean {
        if (results.size < 2) return true

        val estimatedSD = material.expectedValue * 0.02
        val threshold = 4.0 * estimatedSD

        for (i in results.indices.reversed()) {
            if (i == 0) break
            val range = abs(results[i].resultValue - results[i - 1].resultValue)
            if (range > threshold) return false
        }
        return true
    }

    private fun check4s1sRule(results: List<LabQCResult>, material: LabQCMaterial): Boolean {
        if (results.size < 4) return true

        val last4 = results.takeLast(4)
        val estimatedSD = material.expectedValue * 0.02
        val threshold = 1.0 * estimatedSD
        val meanValue = material.expectedValue

        val allAbove = last4.all {
            (it.resultValue - meanValue) > threshold
        }
        val allBelow = last4.all {
            (it.resultValue - meanValue) < -threshold
        }

        return !(allAbove || allBelow)
    }
}

data class QCAssessment(
    val status: QCResultStatus,
    val deviationPercent: Double,
    val standardDeviation: Double,
    val message: String,
    val recommendations: List<String>,
    val isEquipmentSuitable: Boolean,
)

data class MaterialValidityStatus(
    val isValid: Boolean,
    val reason: String,
    val action: String,  // "OK", "REMOVE_FROM_USE", "REVIEW"
)

data class TrendAnalysis(
    val isNormal: Boolean,
    val alerts: List<String>,
    val message: String,
)

private fun formatDouble(value: Double): String {
    val intPart = value.toInt()
    val decPart = ((value - intPart) * 100).toInt()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}

