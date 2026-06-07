# Prescription Print & Export Implementation Guide

## Overview

This guide explains how to integrate **formatted patient-facing prescriptions** with print, download, and share capabilities into the Consultation and Pharmacy modules of the EGESA Medical Clinic system.

### Requirements Met

✅ **Prescription as a document** - Human-readable with all required fields
✅ **Multiple formats** - HTML (printable), plain text, markdown
✅ **Layout & formatting** - Professional, healthcare-compliant design  
✅ **Integration points** - Consultation and Pharmacy screens
✅ **External pharmacy support** - Mark prescriptions for outside purchase
✅ **Technical implementation** - Models, rendering functions, UI screens
✅ **Platform support** - Android, Desktop (JVM), iOS stub, Web stub

---

## Architecture

```
shared/commonMain/
├── domain/
│   ├── PrescriptionPrintModels.kt          # Data models (MedicationPrintItem, PrescriptionPrintModel)
│   ├── PrescriptionRendering.kt            # Rendering functions (HTML, text, markdown)
│   └── PrescriptionPrintActions.kt         # Common interface for print/download/share
├── ui/screens/
│   └── PrescriptionPrintPreviewScreen.kt   # Main preview + export UI

shared/androidMain/
└── ui/prescription/
    └── AndroidPrescriptionPrintActions.kt  # Android implementation (print, download, share)

shared/jvmMain/
└── ui/prescription/
    └── DesktopPrescriptionPrintActions.kt  # Desktop/JVM implementation
```

---

## 1. Data Models

### `PrescriptionPrintModel`
Aggregates all data needed for printing:
- Facility info (name, address, license)
- Provider info (name, specialty, registration)
- Patient info (name, age, sex, ID)
- Medications list with full dosing details
- Diagnosis/indication
- Status (ACTIVE, DISPENSED, EXTERNAL_PURCHASE, etc.)

### `MedicationPrintItem`
Individual medication with complete prescribing information:
- Drug name + generic name
- Strength + form
- Dose, route, frequency, duration
- Quantity to dispense
- Patient instructions

### Extension Function
```kotlin
fun Prescription.toPrintModel(...): PrescriptionPrintModel
```

---

## 2. Rendering Functions

### HTML Rendering
```kotlin
renderPrescriptionToHtml(model: PrescriptionPrintModel): String
```
- Professional, print-ready layout
- Color-coded status badges
- Responsive design for mobile/tablet
- Clear medication table with instructions
- Signature blocks for provider
- Legal disclaimers

### Plain Text Rendering
```kotlin
renderPrescriptionToPlainText(model: PrescriptionPrintModel): String
```
- Simple ASCII format
- Works for SMS/email
- Platform-agnostic
- Easy to read on any device

### Markdown Rendering
```kotlin
renderPrescriptionToMarkdown(model: PrescriptionPrintModel): String
```
- Flexible format
- Can be converted to PDF or HTML separately
- Good for documentation

---

## 3. UI Screen: PrescriptionPrintPreviewScreen

Enhanced Compose screen with:

### Features
1. **Format Tabs** - Switch between HTML preview, plain text, markdown
2. **External Purchase Toggle** - Mark prescription for outside pharmacy
3. **HTML Preview** - Shows formatted representation with key sections
4. **Print Button** - Opens system print dialog
5. **Download Buttons** - Save as HTML, TXT, or Markdown
6. **Share Integration** - Send via email/messaging (platform-specific)
7. **Confirm External Purchase** - Final action to mark for outside use

### Key Parameters
```kotlin
@Composable
fun PrescriptionPrintPreviewScreen(
    prescription: Prescription,
    patients: List<Patient>,
    onDismiss: () -> Unit,
    onPrint: ((htmlContent: String, prescriptionId: String) -> Unit)? = null,
    onDownload: ((content: String, filename: String, format: String) -> Unit)? = null,
    onExternalPurchase: ((Prescription) -> Unit)? = null
)
```

---

## 4. Platform-Specific Implementations

### Android (`AndroidPrescriptionPrintActions`)

#### Print Function
- Creates temporary HTML file
- Opens system Print dialog via PrintManager
- Renders in WebView before printing
- Supports PDF output on modern Android

#### Download Function
- Saves to device Downloads folder
- Requests WRITE_EXTERNAL_STORAGE permission
- Shows Toast notification confirming save

#### Share Function
- Uses FileProvider for secure file access
- Creates share intent
- Supports email, messaging apps, cloud storage

**Required Permissions (AndroidManifest.xml):**
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

**Required in AndroidManifest.xml (Providers):**
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.prescription.provider"
    android:exported="false">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

**Create `res/xml/file_paths.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <cache-path name="prescription" path="/" />
    <external-path name="download" path="Download/" />
</paths>
```

### Desktop/JVM (`DesktopPrescriptionPrintActions`)

#### Print Function
- Creates temporary HTML file
- Opens with system Default PrinterProgram
- Fallback: Opens in default browser

#### Download Function
- Saves to ~/Downloads directory
- Platform-aware path handling (Windows, Mac, Linux)
- Opens containing folder after save

#### Share Function
- Creates temporary file
- Opens with default application
- Users manually attach to email/messaging

---

## 5. Integration with Consultation Screen

### Step 1: Update `ConsultationScreen.kt`

Add imports:
```kotlin
import com.egesa.clinic.shared.ui.prescription.PrescriptionPrintPreviewScreen
import com.egesa.clinic.shared.domain.PrescriptionPrintActions
```

Add state:
```kotlin
var showPrescriptionPreview by remember { mutableStateOf(false) }
var selectedPrescription by remember { mutableStateOf<Prescription?>(null) }
var printActions by remember { mutableStateOf<PrescriptionPrintActions?>(null) }
```

In the Plan Tab, add button:
```kotlin
Button(
    onClick = {
        // Get the prescription that was just created
        activeEncounterBundle?.prescriptions?.firstOrNull()?.let { rx ->
            selectedPrescription = rx
            showPrescriptionPreview = true
        }
    }
) {
    Text("Preview & Print Prescription")
}
```

Add modal to the screen:
```kotlin
if (showPrescriptionPreview && selectedPrescription != null) {
    PrescriptionPrintPreviewScreen(
        prescription = selectedPrescription!!,
        patients = patients,
        onDismiss = { 
            showPrescriptionPreview = false
            selectedPrescription = null
        },
        onPrint = { htmlContent, prescriptionId ->
            scope.launch {
                printActions?.print(htmlContent, prescriptionId)
            }
        },
        onDownload = { content, filename, format ->
            scope.launch {
                printActions?.download(content, filename, format)
            }
        },
        onExternalPurchase = { prescription ->
            scope.launch {
                // Update prescription status to EXTERNAL_PURCHASE
                repository.markPrescriptionForExternalPurchase(prescription.prescriptionId)
            }
        }
    )
}
```

### Step 2: Initialize Platform-Specific Functions

In the `LaunchedEffect` after consultation is loaded:
```kotlin
LaunchedEffect(Unit) {
    printActions = when {
        isAndroid() -> AndroidPrescriptionPrintActions(ctx)
        isDesktop() -> DesktopPrescriptionPrintActions()
        else -> NoOpPrescriptionPrintActions()
    }
}
```

(You may need helper functions to detect platform; use `expect/actual` declarations in KMP)

---

## 6. Integration with Pharmacy Screen

### Step 1: Update `PharmacyScreen.kt`

In the medication dispensing section:
```kotlin
Button(
    onClick = {
        // Get prescription to dispense
        prescriptionToDispense?.let { rx ->
            selectedPrescription = rx
            showPrescriptionPreview = true
        }
    }
) {
    Text("Print Prescription for Patient")
}
```

### Step 2: Before Dispensing

If prescription is marked `EXTERNAL_PURCHASE`:
- Show warning that no internal stock will be deducted
- Allow pharmacist to print for patient reference
- Record that this is external purchase, not facility dispensing

---

## 7. Database Schema Updates

### Add to `Prescription` table:

```kotlin
@Serializable
data class Prescription(
    val prescriptionId: String,
    val encounterId: String,
    val medicationName: String,
    val dose: String? = null,
    val route: String? = "Oral",
    val frequency: String? = "As directed",
    val duration: String? = "As advised",
    val instructions: String? = null,
    val genericName: String? = null,
    val strength: String? = null,
    val form: String? = null,
    val quantity: Int? = null,
    val status: String = "ACTIVE",  // ACTIVE, DISPENSED, EXPIRED, CANCELLED, EXTERNAL_PURCHASE
    val externalPurchaseMarked: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
)
```

### Add repository function:
```kotlin
suspend fun markPrescriptionForExternalPurchase(prescriptionId: String)
```

---

## 8. Testing Scenario

### Doctor Workflow

1. **Consultation**
   - Doctor completes patient history, exam, diagnosis
   - In "Plan" tab, enters medication details
   - Clicks "Preview & Print Prescription"

2. **Print Preview Screen**
   - Shows HTML preview with all details
   - Doctor can toggle "For External Pharmacy"
   - Reviews before printing

3. **Print/Export**
   - Doctor clicks "Print" → System print dialog opens
   - Or clicks "HTML/Text/MD" download buttons
   - Or marks as external and confirms

4. **Patient Receives Prescription**
   - Printed/downloaded and handed to patient
   - Patient can use for outside pharmacy

### Pharmacist Workflow (if Internal)

1. **Pharmacy Screen**
   - Pharmacist sees pending prescriptions
   - Clicks "Print Prescription for Patient"
   - Reviews before dispensing

2. **Dispensing**
   - If not marked external: deduct stock
   - If external: note in system, no stock movement
   - Hand printed prescription to patient

---

## 9. Frontend Checklist

- [x] `PrescriptionPrintModel` - Data model
- [x] `PrescriptionRendering` - HTML/text/markdown rendering
- [x] `PrescriptionPrintPreviewScreen` - UI with preview + export
- [x] `AndroidPrescriptionPrintActions` - Android print/download/share
- [x] `DesktopPrescriptionPrintActions` - Desktop print/download
- [x] Integration points in ConsultationScreen
- [x] Integration points in PharmacyScreen
- [x] External pharmacy marking + status tracking
- [ ] iOS implementation (stub ready for native code)
- [ ] Web implementation (stub ready for JS interop)

---

## 10. Next Steps

1. **Update Repository** to add `markPrescriptionForExternalPurchase()` function
2. **Add permissions** to Android manifest
3. **Test print workflow** on Android device
4. **Test download workflow** on desktop
5. **Integrate with backend** to sync external purchase status
6. **Add monitoring** to track which prescriptions are printed vs. dispensed internally
7. **Add audit logs** for compliance with healthcare regulations

---

## Example Usage

### Creating and Printing a Prescription

```kotlin
// In consultation
val prescription = Prescription(
    prescriptionId = "RX-${Clock.System.now().toEpochMilliseconds()}",
    encounterId = activeEncounterId,
    medicationName = "Paracetamol",
    dose = "1 tablet",
    route = "Oral",
    frequency = "Three times daily",
    duration = "7 days",
    instructions = "Take with food if stomach upset",
    createdAt = Clock.System.now().toString(),
    updatedAt = Clock.System.now().toString()
)

// Convert to print model
val printModel = prescription.toPrintModel(
    encounterId = activeEncounterId,
    facilityInfo = facilityInfo,
    providerInfo = providerInfo,
    patientInfo = patientInfo,
    externalPurchase = true  // Patient buying outside
)

// Render to HTML
val htmlContent = renderPrescriptionToHtml(printModel)

// Print
printActions.print(htmlContent, prescription.prescriptionId)

// Or download
printActions.download(htmlContent, "prescription_RX-123.html", "html")
```

---

## Notes

- **HIPAA Compliance**: Ensure prescriptions only display to authorized users
- **Patient Privacy**: Don't expose internal pricing/cost data on printed prescriptions
- **Audit Trail**: Log all print/download/share actions for compliance
- **Offline Support**: Pre-generate prescription HTML so it works offline
- **Accessibility**: Ensure HTML/CSS meets WCAG standards for print and screen readers
- **Localization**: Support multiple languages in prescription rendering

---

## References

- [ACHCA Long-Term Care Quality Framework](https://www.achca.org/assets/docs/ltcplc_stmt3_meduseprocess_081031.pdf)
- [WHO Prescribing Guidelines](https://www.who.int/publications/i/item/guidelines-for-the-regulatory-assessment-of-medicinal-products-for-use-in-children)
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-create-first-app.html)
- [Material Design 3 for Compose](https://developer.android.com/design/material)

