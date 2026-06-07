package com.egesa.clinic.shared.ui.mobile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.ui.graphics.vector.ImageVector
import com.egesa.clinic.shared.WorkflowArea

internal fun WorkflowArea.displayName(): String = when (this) {
    WorkflowArea.DASHBOARD -> "Command Center"
    WorkflowArea.RECEPTION -> "Reception"
    WorkflowArea.APPOINTMENTS -> "Appointments"
    WorkflowArea.CONSULTATION -> "Consultation"
    WorkflowArea.DIAGNOSIS -> "Diagnosis"
    WorkflowArea.LAB_IMAGING -> "Lab / Imaging"
    WorkflowArea.PHARMACY -> "Pharmacy"
    WorkflowArea.WARDS -> "Wards"
    WorkflowArea.BILLING -> "Billing / M-Pesa"
    WorkflowArea.INVENTORY -> "Inventory"
    WorkflowArea.NOTIFICATIONS -> "SMS / Alerts"
    WorkflowArea.ADMIN -> "Admin"
    WorkflowArea.REPORTS -> "Reports"
    WorkflowArea.SETTINGS -> "Settings"
    WorkflowArea.MOH_REPORTS -> "MOH Reports"
}

internal fun areaIcon(area: WorkflowArea): ImageVector = when (area) {
    WorkflowArea.DASHBOARD -> Icons.Filled.Dashboard
    WorkflowArea.RECEPTION -> Icons.Filled.People
    WorkflowArea.APPOINTMENTS -> Icons.Filled.CalendarMonth
    WorkflowArea.CONSULTATION -> Icons.Filled.MedicalServices
    WorkflowArea.DIAGNOSIS -> Icons.Filled.LocalHospital
    WorkflowArea.LAB_IMAGING -> Icons.Filled.Science
    WorkflowArea.PHARMACY -> Icons.Filled.Medication
    WorkflowArea.WARDS -> Icons.Filled.LocalHospital
    WorkflowArea.BILLING -> Icons.Filled.Payments
    WorkflowArea.INVENTORY -> Icons.Filled.Inventory2
    WorkflowArea.REPORTS, WorkflowArea.MOH_REPORTS -> Icons.Filled.Assessment
    WorkflowArea.NOTIFICATIONS -> Icons.Filled.Sms
    WorkflowArea.ADMIN -> Icons.Filled.AdminPanelSettings
    WorkflowArea.SETTINGS -> Icons.Filled.Settings
}
