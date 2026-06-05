package com.egesa.clinic.shared.domain

/**
 * Rendering functions to convert PrescriptionPrintModel to human-readable formats.
 * Supports HTML for web/desktop printing, and plain text for alternative uses.
 */

/**
 * Renders a prescription to HTML suitable for printing or web display.
 * This can be converted to PDF on server or client-side.
 */
fun renderPrescriptionToHtml(model: PrescriptionPrintModel): String {
    val statusBadge = when (model.status) {
        PrescriptionPrintStatus.EXTERNAL_PURCHASE -> "<span style='background:#ff9800;color:white;padding:4px 8px;border-radius:4px;font-weight:bold;'>EXTERNAL PURCHASE</span>"
        PrescriptionPrintStatus.DISPENSED -> "<span style='background:#4caf50;color:white;padding:4px 8px;border-radius:4px;font-weight:bold;'>DISPENSED</span>"
        PrescriptionPrintStatus.EXPIRED -> "<span style='background:#f44336;color:white;padding:4px 8px;border-radius:4px;font-weight:bold;'>EXPIRED</span>"
        PrescriptionPrintStatus.CANCELLED -> "<span style='background:#9e9e9e;color:white;padding:4px 8px;border-radius:4px;font-weight:bold;'>CANCELLED</span>"
        else -> "<span style='background:#2196f3;color:white;padding:4px 8px;border-radius:4px;font-weight:bold;'>ACTIVE</span>"
    }

    val medicationsHtml = model.medications.mapIndexed { index, med ->
        """
        <tr>
            <td style='padding:8px;border-bottom:1px solid #ddd;'>${index + 1}</td>
            <td style='padding:8px;border-bottom:1px solid #ddd;'>
                <strong>${med.medicationName}</strong>${if (med.genericName != null) " (${med.genericName})" else ""}
                ${if (med.strength != null) "<br/><small>${med.strength}${if (med.form != null) " ${med.form}" else ""}</small>" else ""}
            </td>
            <td style='padding:8px;border-bottom:1px solid #ddd;'>${med.dose}</td>
            <td style='padding:8px;border-bottom:1px solid #ddd;'>${med.route}</td>
            <td style='padding:8px;border-bottom:1px solid #ddd;'>${med.frequency}</td>
            <td style='padding:8px;border-bottom:1px solid #ddd;'>${med.duration}</td>
            ${if (med.quantity != null) "<td style='padding:8px;border-bottom:1px solid #ddd;'>${med.quantity}</td>" else ""}
        </tr>
        ${if (med.patientInstructions != null) 
            "<tr><td colspan='7' style='padding:8px;background:#f5f5f5;font-style:italic;'><strong>Instructions:</strong> ${med.patientInstructions}</td></tr>" 
        else ""}
        """
    }.joinToString("\n")

    val diagnosisSection = if (model.diagnosis != null) {
        "<div style='margin:16px 0;'><strong>Primary Diagnosis:</strong> ${model.diagnosis}</div>"
    } else ""

    val indicationSection = if (model.indication != null) {
        "<div style='margin:16px 0;'><strong>Indication:</strong> ${model.indication}</div>"
    } else ""

    val externalPurchaseWarning = if (model.externalPurchaseMarked) {
        """
        <div style='background:#fff3cd;border:2px solid #ffc107;padding:12px;border-radius:4px;margin:16px 0;color:#856404;'>
            <strong>⚠️ For External Purchase</strong><br/>
            This prescription is provided for the patient to purchase medications from an outside pharmacy.
            No internal stock movement will be recorded.
        </div>
        """
    } else ""

    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>Prescription - ${model.patient.patientName}</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; color: #333; line-height: 1.6; }
        .header { border-bottom: 3px solid #1976d2; padding-bottom: 16px; margin-bottom: 20px; }
        .facility-info { display: flex; justify-content: space-between; margin-bottom: 8px; }
        .facility-name { font-size: 18px; font-weight: bold; color: #1976d2; }
        .facility-contact { font-size: 12px; color: #666; }
        .prescription-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 16px 0; padding: 12px; background: #f5f5f5; border-radius: 4px; }
        .meta-item { font-size: 13px; }
        .meta-label { font-weight: bold; color: #1976d2; }
        .patient-info { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; margin: 16px 0; padding: 12px; background: #e3f2fd; border-radius: 4px; border-left: 4px solid #1976d2; }
        .patient-field { font-size: 13px; }
        .medication-table { width: 100%; border-collapse: collapse; margin: 16px 0; }
        .medication-table th { background: #1976d2; color: white; padding: 10px; text-align: left; font-weight: bold; }
        .medication-table td { padding: 8px; border-bottom: 1px solid #ddd; }
        .medication-table tr:hover { background: #f9f9f9; }
        .prescription-number { position: absolute; top: 20px; right: 20px; font-size: 12px; color: #999; }
        .provider-section { margin-top: 20px; border-top: 1px solid #ddd; padding-top: 16px; }
        .signature-block { display: inline-block; width: 180px; text-align: center; }
        .signature-line { border-top: 2px solid #000; margin-top: 20px; font-size: 12px; }
        .footer { margin-top: 20px; padding-top: 16px; border-top: 1px solid #ddd; font-size: 12px; color: #666; }
        .status-badge { display: inline-block; padding: 6px 12px; border-radius: 4px; font-weight: bold; }
        .warning-box { background: #fff3cd; border: 2px solid #ffc107; padding: 12px; border-radius: 4px; margin: 16px 0; color: #856404; }
        @media print {
            body { margin: 0; }
            .no-print { display: none; }
        }
    </style>
</head>
<body>
    <div class='prescription-number'>Rx #${model.prescriptionId}</div>
    
    <div class='header'>
        <div class='facility-info'>
            <div>
                <div class='facility-name'>${model.facility.facilityName}</div>
                ${if (model.facility.address != null) "<div class='facility-contact'>${model.facility.address}</div>" else ""}
                ${if (model.facility.phoneNumber != null) "<div class='facility-contact'>Phone: ${model.facility.phoneNumber}</div>" else ""}
            </div>
            <div style='text-align:right;'>
                ${if (model.facility.licenseNumber != null) "<div class='facility-contact'>License: ${model.facility.licenseNumber}</div>" else ""}
                <div class='facility-contact'>Date: ${model.dateIssued.split("T")[0]}</div>
            </div>
        </div>
    </div>

    <div class='prescription-meta'>
        <div class='meta-item'>
            <span class='meta-label'>Prescription ID:</span> ${model.prescriptionId}
        </div>
        <div class='meta-item'>
            <span class='meta-label'>Status:</span> $statusBadge
        </div>
        <div class='meta-item'>
            <span class='meta-label'>Encounter ID:</span> ${model.encounterId}
        </div>
    </div>

    <div class='patient-info'>
        <div class='patient-field'>
            <span style='color:#1976d2;font-weight:bold;'>Patient Name</span><br/>
            ${model.patient.patientName}
        </div>
        <div class='patient-field'>
            <span style='color:#1976d2;font-weight:bold;'>Patient ID</span><br/>
            ${model.patient.patientId ?: "N/A"}
        </div>
        <div class='patient-field'>
            <span style='color:#1976d2;font-weight:bold;'>Age / Sex</span><br/>
            ${model.patient.age ?: "N/A"} / ${model.patient.sex ?: "N/A"}
        </div>
    </div>

    $diagnosisSection
    $indicationSection
    $externalPurchaseWarning

    <h3 style='margin-top:20px;color:#1976d2;border-bottom:2px solid #1976d2;padding-bottom:8px;'>Medications</h3>
    <table class='medication-table'>
        <thead>
            <tr>
                <th style='width:5%;'>#</th>
                <th style='width:25%;'>Medication</th>
                <th style='width:15%;'>Dose</th>
                <th style='width:12%;'>Route</th>
                <th style='width:18%;'>Frequency</th>
                <th style='width:12%;'>Duration</th>
                ${if (model.medications.any { it.quantity != null }) "<th style='width:8%;'>Qty</th>" else ""}
            </tr>
        </thead>
        <tbody>
            $medicationsHtml
        </tbody>
    </table>

    <div class='provider-section'>
        <div style='display:inline-block;margin-right:40px;'>
            <div style='margin-bottom:4px;'><strong>Prescribing Provider:</strong></div>
            <div>${model.provider.providerName}</div>
            ${if (model.provider.specialty != null) "<div style='font-size:12px;color:#666;'>${model.provider.specialty}</div>" else ""}
            ${if (model.provider.registrationNumber != null) "<div style='font-size:12px;color:#666;'>Reg: ${model.provider.registrationNumber}</div>" else ""}
        </div>
        <div class='signature-block'>
            <div style='border-bottom:2px solid #000;height:40px;'></div>
            <div style='margin-top:4px;font-size:11px;'>Provider Signature</div>
        </div>
        <div class='signature-block' style='margin-left:20px;'>
            <div style='border-bottom:2px solid #000;height:40px;'></div>
            <div style='margin-top:4px;font-size:11px;'>Date Signed</div>
        </div>
    </div>

    <div class='footer'>
        <p><strong>Patient Instructions:</strong></p>
        <ul>
            <li>Take medications exactly as prescribed.</li>
            <li>Do not exceed the recommended dosage.</li>
            <li>Contact your doctor if you experience any side effects.</li>
            <li>Keep all medications out of reach of children.</li>
            <li>Store medications in a cool, dry place unless otherwise directed.</li>
        </ul>
        ${if (model.disclaimers != null) "<p><strong>Disclaimers:</strong><br/>${model.disclaimers}</p>" else ""}
        <p style='margin-top:16px;text-align:center;color:#999;'>This prescription is valid for ${model.medications.firstOrNull()?.refills ?: 0} refills.</p>
    </div>

    <div class='footer' style='margin-top:30px;border-top:2px solid #ddd;padding-top:16px;text-align:center;color:#999;font-size:11px;'>
        <p>Electronically generated prescription. Produced by Egesa Medical Clinic Management System.</p>
        <p>This document should be kept in the patient's medical record and/or provided to the patient.</p>
    </div>
</body>
</html>
    """.trimIndent()
}

/**
 * Renders a prescription to plain text format (for terminals, emails, or simple exports).
 */
fun renderPrescriptionToPlainText(model: PrescriptionPrintModel): String {
    val builder = StringBuilder()

    builder.append("═══════════════════════════════════════════════════════════\n")
    builder.append("                        PRESCRIPTION\n")
    builder.append("═══════════════════════════════════════════════════════════\n\n")

    // Facility info
    builder.append("${model.facility.facilityName}\n")
    if (model.facility.address != null) builder.append("${model.facility.address}\n")
    if (model.facility.phoneNumber != null) builder.append("Phone: ${model.facility.phoneNumber}\n")
    if (model.facility.licenseNumber != null) builder.append("License: ${model.facility.licenseNumber}\n")
    builder.append("\n")

    // Prescription meta
    builder.append("Prescription ID: ${model.prescriptionId}\n")
    builder.append("Encounter ID: ${model.encounterId}\n")
    builder.append("Date Issued: ${model.dateIssued.split("T")[0]}\n")
    builder.append("Status: ${model.status}\n")
    builder.append("\n")

    // Patient info
    builder.append("─────────────────────────────────────────────────────────────\n")
    builder.append("PATIENT INFORMATION\n")
    builder.append("─────────────────────────────────────────────────────────────\n")
    builder.append("Name: ${model.patient.patientName}\n")
    if (model.patient.patientId != null) builder.append("ID: ${model.patient.patientId}\n")
    if (model.patient.age != null) builder.append("Age: ${model.patient.age}\n")
    if (model.patient.sex != null) builder.append("Sex: ${model.patient.sex}\n")
    builder.append("\n")

    // Diagnosis
    if (model.diagnosis != null) {
        builder.append("Diagnosis: ${model.diagnosis}\n")
    }
    if (model.indication != null) {
        builder.append("Indication: ${model.indication}\n")
    }

    // Medications
    builder.append("────────────────────���────────────────────────────────────────\n")
    builder.append("MEDICATIONS\n")
    builder.append("─────────────────────────────────────────────────────────────\n\n")

    model.medications.forEachIndexed { index, med ->
        builder.append("${index + 1}. ${med.medicationName}")
        if (med.genericName != null) builder.append(" (${med.genericName})")
        builder.append("\n")

        if (med.strength != null) {
            builder.append("   Strength: ${med.strength}")
            if (med.form != null) builder.append(" ${med.form}")
            builder.append("\n")
        }

        builder.append("   Dose: ${med.dose}\n")
        builder.append("   Route: ${med.route}\n")
        builder.append("   Frequency: ${med.frequency}\n")
        builder.append("   Duration: ${med.duration}\n")

        if (med.quantity != null) {
            builder.append("   Quantity: ${med.quantity}\n")
        }

        if (med.patientInstructions != null) {
            builder.append("   Instructions: ${med.patientInstructions}\n")
        }

        if (med.notes != null) {
            builder.append("   Notes: ${med.notes}\n")
        }

        builder.append("\n")
    }

    // Provider info
    builder.append("─────────────────────────────────────────────────────────────\n")
    builder.append("PRESCRIBING PROVIDER\n")
    builder.append("─────────────────────────────────────────────────────────────\n")
    builder.append("Name: ${model.provider.providerName}\n")
    if (model.provider.specialty != null) builder.append("Specialty: ${model.provider.specialty}\n")
    if (model.provider.registrationNumber != null) builder.append("Reg #: ${model.provider.registrationNumber}\n")
    builder.append("\n")

    // Footer
    if (model.externalPurchaseMarked) {
        builder.append("⚠️  FOR EXTERNAL PURCHASE - Patient to buy from outside pharmacy\n")
        builder.append("   No internal stock movement will be recorded.\n\n")
    }

    builder.append("═══════════════════════════════════════════════════════════\n")
    builder.append("Patient Instructions: Take medications exactly as prescribed.\n")
    builder.append("═══════════════════════════════════════════════════════════\n")

    return builder.toString()
}

/**
 * Renders a prescription to markdown format (for flexible use and conversion).
 */
fun renderPrescriptionToMarkdown(model: PrescriptionPrintModel): String {
    val builder = StringBuilder()

    builder.append("# PRESCRIPTION\n\n")
    builder.append("**Prescription ID:** ${model.prescriptionId}  \n")
    builder.append("**Encounter ID:** ${model.encounterId}  \n")
    builder.append("**Date Issued:** ${model.dateIssued.split("T")[0]}  \n")
    builder.append("**Status:** ${model.status}\n\n")

    // Facility
    builder.append("## Facility\n\n")
    builder.append("**${model.facility.facilityName}**\n\n")
    if (model.facility.address != null) builder.append("${model.facility.address}  \n")
    if (model.facility.phoneNumber != null) builder.append("Phone: ${model.facility.phoneNumber}  \n")
    if (model.facility.licenseNumber != null) builder.append("License: ${model.facility.licenseNumber}  \n")
    builder.append("\n")

    // Patient
    builder.append("## Patient\n\n")
    builder.append("| Field | Value |\n")
    builder.append("|-------|-------|\n")
    builder.append("| Name | ${model.patient.patientName} |\n")
    if (model.patient.patientId != null) builder.append("| ID | ${model.patient.patientId} |\n")
    if (model.patient.age != null) builder.append("| Age | ${model.patient.age} |\n")
    if (model.patient.sex != null) builder.append("| Sex | ${model.patient.sex} |\n")
    builder.append("\n")

    // Clinical info
    if (model.diagnosis != null || model.indication != null) {
        builder.append("## Clinical Information\n\n")
        if (model.diagnosis != null) builder.append("**Diagnosis:** ${model.diagnosis}\n\n")
        if (model.indication != null) builder.append("**Indication:** ${model.indication}\n\n")
    }

    // Medications
    builder.append("## Medications\n\n")
    model.medications.forEachIndexed { index, med ->
        builder.append("### ${index + 1}. ${med.medicationName}")
        if (med.genericName != null) builder.append(" (${med.genericName})")
        builder.append("\n\n")
        if (med.strength != null || med.form != null) {
            builder.append("**Strength:** ${med.strength ?: "N/A"}${if (med.form != null) " ${med.form}" else ""}\n\n")
        }
        builder.append("- **Dose:** ${med.dose}\n")
        builder.append("- **Route:** ${med.route}\n")
        builder.append("- **Frequency:** ${med.frequency}\n")
        builder.append("- **Duration:** ${med.duration}\n")
        if (med.quantity != null) builder.append("- **Quantity:** ${med.quantity}\n")
        if (med.patientInstructions != null) builder.append("- **Instructions:** ${med.patientInstructions}\n")
        if (med.notes != null) builder.append("- **Notes:** ${med.notes}\n")
        builder.append("\n")
    }

    // Provider
    builder.append("## Prescribing Provider\n\n")
    builder.append("**${model.provider.providerName}**\n\n")
    if (model.provider.specialty != null) builder.append("Specialty: ${model.provider.specialty}  \n")
    if (model.provider.registrationNumber != null) builder.append("Registration: ${model.provider.registrationNumber}  \n")
    builder.append("\n")

    // External purchase warning
    if (model.externalPurchaseMarked) {
        builder.append("⚠️  **FOR EXTERNAL PURCHASE**\n\n")
        builder.append("This prescription is provided for the patient to purchase medications from an outside pharmacy.\n")
        builder.append("No internal stock movement will be recorded.\n\n")
    }

    return builder.toString()
}

