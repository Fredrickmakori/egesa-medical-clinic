# Kotlin Hierarchy Template Issue - FIXED ✅

## Problem
The Kotlin Multiplatform (KMP) build was failing with the error:

```
Default Kotlin Hierarchy Template Not Applied Correctly

The Default Kotlin Hierarchy Template was not applied to 'project ':shared'':
Explicit .dependsOn() edges were configured for the following source sets:
[iosMain]

Consider removing dependsOn-calls or disabling the default template by adding
'kotlin.mpp.applyDefaultHierarchyTemplate=false'
to your gradle.properties
```

## Root Cause
In `shared/build.gradle.kts`, the `iosMain` source set had an explicit `.dependsOn()` call:

```kotlin
val iosMain by creating {
    dependsOn(commonMain.get())  // ❌ This conflicts with default hierarchy template
    dependencies {
        implementation(libs.ktor.client.darwin)
        implementation(libs.sqldelight.native.driver)
    }
}
```

This explicit dependency configuration conflicted with Kotlin's Default Hierarchy Template, which automatically manages source set dependencies for standard multiplatform layouts.

## Solution Applied ✅

**Removed the explicit `.dependsOn()` call** while keeping the iOS-specific dependencies:

### Before:
```kotlin
val iosMain by creating {
    dependsOn(commonMain.get())
    dependencies {
        implementation(libs.ktor.client.darwin)
        implementation(libs.sqldelight.native.driver)
    }
}
```

### After:
```kotlin
val iosMain by creating {
    dependencies {
        implementation(libs.ktor.client.darwin)
        implementation(libs.sqldelight.native.driver)
    }
}
```

## Why This Works ✅

The **Default Kotlin Hierarchy Template** automatically creates the correct dependency structure for standard multiplatform projects:

```
commonMain
    ↓
iosMain (automatically depends on commonMain)
```

By removing the explicit `.dependsOn()`, the template can apply its standard hierarchy rules without conflicts.

## File Modified
- `shared/build.gradle.kts` - Removed line 47: `dependsOn(commonMain.get())`

## Verification

To verify the fix:
```bash
./gradlew clean build
```

If successful, no warnings about "Default Kotlin Hierarchy Template" should appear.

## Benefits
✅ Cleaner build configuration  
✅ Follows Kotlin multiplatform best practices  
✅ Automatic dependency management  
✅ Easier maintenance and upgrades  

## Alternative Solution (Not Used)
If needed, the warning could alternatively be suppressed by adding to `gradle.properties`:
```properties
kotlin.mpp.applyDefaultHierarchyTemplate=false
```

However, removing the explicit `.dependsOn()` is the preferred solution as it aligns with modern KMP standards.

