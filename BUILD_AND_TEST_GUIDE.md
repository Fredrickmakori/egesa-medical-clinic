# Quick Build & Test Guide

## Prerequisites Setup

### 1. Set JAVA_HOME (Windows)

First, find your Java installation:
```powershell
# Check if Java is installed
java -version

# Find Java installation path
$JAVA_HOME = "C:\Program Files\Java\jdk-17" # Usually something like this
# or for Android Studio's bundled JDK:
$JAVA_HOME = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\jdks\openjdk-17*"
```

Set it permanently:
```powershell
# Set for current session only
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# Set permanently (restart terminal after)
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")
```

### 2. Verify Gradle Wrapper
```powershell
cd C:\Users\USER\AndroidStudioProjects\egesa-medical-clinic-mobile-app
.\gradlew --version
```

---

## Build Steps

### Build Shared Module (with new prescription features)
```powershell
.\gradlew :shared:build -x test
```

### Build Android App
```powershell
.\gradlew :androidApp:assembleDebug
```

### Build Desktop App
```powershell
.\gradlew :desktop:build -x test
```

### Run All Tests (excluding new modules initially)
```powershell
.\gradlew test -x jvmTest
```

---

## Testing the Prescription Print Feature

### 1. After Building Android App

Run on emulator:
```powershell
.\gradlew :androidApp:installDebug
adb shell am start -n com.egesa.clinic.android/com.egesa.clinic.android.MainActivity
```

### 2. Test Workflows

**Test 1: Print Preview Screen Loads**
- Navigate to Consultation module
- Click "Preview & Print Prescription" button
- Verify screen opens with tabs

**Test 2: Format Switching**
- Click "HTML Preview" tab → See description
- Click "Text" tab → See plain text format
- Click "Markdown" tab → See markdown format

**Test 3: External Pharmacy Toggle**
- Toggle "For External Pharmacy" switch
- Verify warning banner appears
- Verify button changes to "Confirm External Purchase"

**Test 4: Download Functionality**
- Click "Download TXT" button
- Navigate to Downloads folder
- Verify file `prescription_RX-*.txt` exists

**Test 5: Print Functionality** (Android)
- Click "Print" button
- Verify system print dialog opens
- Select "Save as PDF" or printer
- Verify PDF/document created

---

## Integration Checklist

Before committing, ensure:

- [ ] JAVA_HOME is set correctly
- [ ] `./gradlew build` passes without errors
- [ ] Android app builds successfully
- [ ] No compilation errors in new files:
  - `PrescriptionPrintPreviewScreen.kt`
  - `PrescriptionPrintActions.kt`
  - `PrescriptionExtensions.kt`
  - `AndroidPrescriptionPrintActions.kt`
  - `DesktopPrescriptionPrintActions.kt`
- [ ] Shared module compiles without warnings
- [ ] All new imports are resolved

---

## Common Issues & Fixes

### Issue: "JAVA_HOME is not set"
**Fix**: See prerequisites section above

### Issue: "cannot find symbol: class PrescriptionPrintModel"
**Fix**: Verify files are in:
```
shared/src/commonMain/kotlin/com/egesa/clinic/shared/domain/
```

### Issue: Android imports not found
**Fix**: Verify files are in:
```
shared/src/androidMain/kotlin/com/egesa/clinic/android/ui/prescription/
```

### Issue: Desktop imports not found
**Fix**: Verify files are in:
```
shared/src/jvmMain/kotlin/com/egesa/clinic/desktop/ui/prescription/
```

### Issue: "Cannot resolve reference to 'PrescriptionPrintPreviewScreen'"
**Fix**: Ensure screen is being imported where used:
```kotlin
import com.egesa.clinic.shared.ui.screens.PrescriptionPrintPreviewScreen
```

---

## Quick Build Validation

Run this to validate all new prescription code:

```powershell
# Build just the Kotlin compilation for shared module
.\gradlew :shared:compileCommonMainKotlinMetadata -x test

# If that passes, check specific targets
.\gradlew :shared:compileAndroidMainKotlinMetadata -x test
.\gradlew :shared:compileJvmMainKotlinMetadata -x test
```

---

## Next: Integration Testing

Once build is successful:

1. Follow integration steps in `PRESCRIPTION_PRINT_IMPLEMENTATION.md`
2. Add buttons to Consultation and Pharmacy screens
3. Test print preview screen integration
4. Test actual printing on Android device
5. Test downloads to file system
6. Test external pharmacy workflow

---

See `PRESCRIPTION_PRINT_FEATURE_SUMMARY.md` for complete feature overview.

