package com.egesa.clinic.shared

enum class WorkflowArea {
    RECEPTION,
    CONSULTATION,
    DIAGNOSIS,
    WARD,
    ADMIN
}

enum class TimelineType {
    VISIT,
    DIAGNOSIS,
    ORDER,
    MEDICATION,
    WARD_TRANSFER
}

enum class SaveState {
    DRAFT_SAVED,
    UNSAVED_CHANGES,
    FINAL_SIGN_OFF
}

data class TimelineEvent(
    val title: String,
    val details: String,
    val timestamp: String,
    val type: TimelineType
)

data class EncounterForm(
    val chiefComplaint: String = "",
    val history: String = "",
    val examinationFindings: String = "",
    val provisionalDiagnosis: String = ""
)

data class Patient(
    val id: String,
    val fullName: String,
    val age: Int,
    val sex: String,
    val status: String,
    val assignedWard: String? = null,
    val visits: Int = 0,
    val activeDiagnosis: String = "",
    val currentMedications: List<String> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList()
)

data class DashboardMetric(
    val title: String,
    val value: String
)

class HospitalState {
    private val patients = listOf(
        Patient(
            id = "PT-001",
            fullName = "Amina Yusuf",
            age = 34,
            sex = "F",
            status = "Awaiting consultation",
            visits = 4,
            activeDiagnosis = "Acute pharyngitis",
            currentMedications = listOf("Paracetamol 500mg", "Cetirizine 10mg"),
            timeline = listOf(
                TimelineEvent("Outpatient visit", "Fever and sore throat", "2026-04-29 09:30", TimelineType.VISIT),
                TimelineEvent("Provisional diagnosis", "Acute pharyngitis", "2026-04-29 09:50", TimelineType.DIAGNOSIS),
                TimelineEvent("Lab order", "CBC", "2026-04-29 09:55", TimelineType.ORDER),
                TimelineEvent("Medication prescribed", "Paracetamol 500mg", "2026-04-29 10:05", TimelineType.MEDICATION)
            )
        ),
        Patient(
            id = "PT-002",
            fullName = "John Ouma",
            age = 58,
            sex = "M",
            status = "In diagnosis",
            visits = 7,
            activeDiagnosis = "Hypertensive urgency",
            currentMedications = listOf("Amlodipine 5mg"),
            timeline = listOf(
                TimelineEvent("ER visit", "Headache and high BP", "2026-05-01 08:10", TimelineType.VISIT),
                TimelineEvent("Imaging request", "CT brain", "2026-05-01 08:40", TimelineType.ORDER),
                TimelineEvent("Ward transfer", "To Observation Ward", "2026-05-01 09:00", TimelineType.WARD_TRANSFER)
            )
        ),
        Patient(
            id = "PT-003",
            fullName = "Martha Wekesa",
            age = 12,
            sex = "F",
            status = "Admitted",
            assignedWard = "Pediatrics",
            visits = 2,
            activeDiagnosis = "Severe malaria",
            currentMedications = listOf("Artemether/Lumefantrine"),
            timeline = listOf(
                TimelineEvent("Admission", "High fever and chills", "2026-04-30 14:20", TimelineType.VISIT),
                TimelineEvent("Diagnosis", "Severe malaria", "2026-04-30 15:00", TimelineType.DIAGNOSIS),
                TimelineEvent("Medication", "Artemether/Lumefantrine", "2026-04-30 15:10", TimelineType.MEDICATION)
            )
        )
    )

    fun allPatients(): List<Patient> = patients

    fun metrics(): List<DashboardMetric> = listOf(
        DashboardMetric("Registered Today", patients.size.toString()),
        DashboardMetric("In Wards", patients.count { it.assignedWard != null }.toString()),
        DashboardMetric("Pending Consultation", patients.count { it.status.contains("consultation", true) }.toString())
    )
}
