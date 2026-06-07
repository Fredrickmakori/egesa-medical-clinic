package com.egesa.clinic.shared.domain

import kotlin.math.abs
import kotlinx.datetime.Clock

/**
 * Core validation engine for lab results
 * Handles result flagging, critical value detection, delta checks, etc.
 */
class LabResultValidationEngine {

    /**
     * Validate result and flag appropriately
     */
    fun validateResult(
        result: LabResultExtended,
        referenceRange: LabReferenceRange,
        test: LabTestDetail,
        previousResult: LabResultExtended? = null,
        patientAge: Int? = null,
        patientGender: String? = null,
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        var flag: LabResultFlag? = null
        var isCritical = false

        // Parse numeric value
        val numericValue = result.valueNumeric
            ?: result.value.toDoubleOrNull()

        if (numericValue == null) {
            warnings.add("Non-numeric result: ${result.value}")
            return ValidationResult(
                isValid = true,
                flag = LabResultFlag.NORMAL,
                isCriticalValue = false,
                errors = errors,
                warnings = warnings,
            )
        }

        // Range check
        val rangeLow = referenceRange.rangeLow
        val rangeHigh = referenceRange.rangeHigh

        when {
            numericValue < (referenceRange.criticalLow ?: rangeLow) -> {
                if (referenceRange.criticalLow != null && numericValue < referenceRange.criticalLow) {
                    flag = LabResultFlag.CRITICAL
                    isCritical = true
                    warnings.add("CRITICAL LOW: ${numericValue} (Critical threshold: ${referenceRange.criticalLow})")
                } else {
                    flag = LabResultFlag.LOW
                    warnings.add("LOW: ${numericValue} (Range: $rangeLow - $rangeHigh)")
                }
            }
            numericValue > (referenceRange.criticalHigh ?: rangeHigh) -> {
                if (referenceRange.criticalHigh != null && numericValue > referenceRange.criticalHigh) {
                    flag = LabResultFlag.CRITICAL
                    isCritical = true
                    warnings.add("CRITICAL HIGH: ${numericValue} (Critical threshold: ${referenceRange.criticalHigh})")
                } else {
                    flag = LabResultFlag.HIGH
                    warnings.add("HIGH: ${numericValue} (Range: $rangeLow - $rangeHigh)")
                }
            }
            else -> {
                flag = LabResultFlag.NORMAL
            }
        }

        // Delta check (compare with previous result)
        if (previousResult != null && previousResult.valueNumeric != null) {
            val deltaCheckResult = performDeltaCheck(
                currentValue = numericValue,
                previousValue = previousResult.valueNumeric!!,
                test = test,
            )

            if (!deltaCheckResult.passed) {
                warnings.add("Delta check failed: ${deltaCheckResult.message}")
                // Flag doesn't change, but we note it
            }
        }

        // Test-specific validation
        if (test.isCriticalValueTest && isCritical) {
            warnings.add("CRITICAL VALUE TEST - Immediate notification required")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            flag = flag ?: LabResultFlag.NORMAL,
            isCriticalValue = isCritical,
            errors = errors,
            warnings = warnings,
            shouldTriggerReflexiveTest = shouldTriggerReflexiveTest(result, flag),
        )
    }

    /**
     * Delta check: Detects unusual changes from previous results
     */
    private fun performDeltaCheck(
        currentValue: Double,
        previousValue: Double,
        test: LabTestDetail,
    ): DeltaCheckResult {
        if (previousValue == 0.0) return DeltaCheckResult(passed = true, message = "No previous value for comparison")

        val percentChange = ((currentValue - previousValue) / abs(previousValue)) * 100

        // Define thresholds by test type
        val deltaThreshold = when (test.category) {
            "Hematology" -> 25.0  // 25% change
            "Biochemistry" -> 20.0  // 20% change
            "Immunology" -> 30.0  // 30% change
            else -> 25.0  // Default 25%
        }

        val passed = abs(percentChange) <= deltaThreshold
        val message = if (!passed) {
            val pct = abs(percentChange).toInt()
            "Result changed by $pct% (threshold: $deltaThreshold%)"
        } else {
            "Delta check passed"
        }

        return DeltaCheckResult(passed = passed, message = message)
    }

    /**
     * Determine if reflexive testing should be triggered
     */
    private fun shouldTriggerReflexiveTest(
        result: LabResultExtended,
        flag: LabResultFlag?,
    ): Boolean {
        // Example: HIV positive requires reflexive CD4 count
        // This would be more elaborate in production with test-specific rules
        return flag == LabResultFlag.ABNORMAL || flag == LabResultFlag.CRITICAL
    }

    /**
     * Check if result needs immediate notification
     */
    fun needsImmediateNotification(result: LabResultExtended): Boolean {
        return result.isCriticalValue && !result.criticalNotificationSent
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val flag: LabResultFlag,
    val isCriticalValue: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val shouldTriggerReflexiveTest: Boolean = false,
)

data class DeltaCheckResult(
    val passed: Boolean,
    val message: String,
)


