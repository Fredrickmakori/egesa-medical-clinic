# Device-Aware Login - Technical Reference

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│              LoginScreen (Main Composable)           │
├─────────────────────────────────────────────────────┤
│                                                     │
│  1. Detect Device Type                              │
│     val deviceType = rememberDeviceType()           │
│                                                     │
│  2. Get Layout Config                               │
│     val layoutConfig = deviceType.getLayoutConfig() │
│                                                     │
│  3. Route to Appropriate UI                         │
│     ├─ Mobile Portrait → MobileStaffSelector        │
│     ├─ Mobile Landscape → Side-by-side              │
│     └─ Desktop/Tablet → Desktop with Keyboard       │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Module: PlatformDetection.kt

### Functions

#### `rememberDeviceType(): DeviceType`
Automatically detects device type based on screen dimensions.

```kotlin
@Composable
fun rememberDeviceType(): DeviceType {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    
    return when {
        screenWidthDp > 600 -> {
            if (isLandscape(screenWidthDp, screenHeightDp)) {
                DeviceType.DESKTOP_LANDSCAPE
            } else {
                DeviceType.TABLET
            }
        }
        !isLandscape(screenWidthDp, screenHeightDp) -> DeviceType.MOBILE_PORTRAIT
        else -> DeviceType.MOBILE_LANDSCAPE
    }
}
```

**Returns:**
- `MOBILE_PORTRAIT` - Phone portrait (width ≤ 600dp, portrait)
- `MOBILE_LANDSCAPE` - Phone landscape (width ≤ 600dp, landscape)
- `TABLET` - Tablet (width > 600dp, portrait)
- `DESKTOP_LANDSCAPE` - Desktop (width > 600dp, landscape)

#### `DeviceType.getLayoutConfig(): LayoutConfig`
Returns responsive layout configuration.

```kotlin
@Composable
fun DeviceType.getLayoutConfig(): LayoutConfig = when (this) {
    DeviceType.MOBILE_PORTRAIT -> LayoutConfig(
        pinEntryWidth = 320.dp,
        pinPadHeight = 280.dp,
        showSideBranding = false,
        useFullWidth = true,
        pinDotSize = 14.dp,
        keyboardHeight = 200.dp
    )
    DeviceType.DESKTOP_LANDSCAPE -> LayoutConfig(
        pinEntryWidth = 480.dp,
        pinPadHeight = 0.dp,  // No PIN pad
        showSideBranding = true,
        useFullWidth = false,
        pinDotSize = 15.dp,
        keyboardHeight = 0.dp
    )
    // ... other configurations
}
```

#### `DeviceType.getInputMethod(): InputMethod`
Determines primary input method for device.

```kotlin
fun DeviceType.getInputMethod(): InputMethod = when (this) {
    DeviceType.MOBILE_PORTRAIT, DeviceType.MOBILE_LANDSCAPE -> InputMethod.TOUCH_AND_KEYBOARD
    else -> InputMethod.KEYBOARD_PRIMARY
}
```

### Data Classes

#### `LayoutConfig`
```kotlin
data class LayoutConfig(
    val pinEntryWidth: Dp,          // Width of PIN entry box
    val pinPadHeight: Dp,            // Height of PIN pad (0 if none)
    val showSideBranding: Boolean,   // Show left branding panel
    val useFullWidth: Boolean,       // Use full screen width
    val pinDotSize: Dp,              // Size of PIN indicator dots
    val keyboardHeight: Dp           // Height of virtual keyboard
)
```

#### Enums

**DeviceType**
```kotlin
enum class DeviceType {
    MOBILE_PORTRAIT,      // Phone portrait (width ≤ 600dp, height > width)
    MOBILE_LANDSCAPE,     // Phone landscape (width ≤ 600dp, width > height)
    TABLET,              // Tablet portrait (width > 600dp, height > width)
    DESKTOP_LANDSCAPE    // Desktop/large screen (width > 600dp, width > height)
}
```

**InputMethod**
```kotlin
enum class InputMethod {
    KEYBOARD_PRIMARY,      // Desktop: use keyboard, hide PIN pad
    TOUCH_AND_KEYBOARD     // Mobile: show PIN pad, support keyboard
}
```

---

## Module: KeyboardHandler.kt

### Composables

#### `KeyboardAwarePinEntry`
Captures keyboard events for PIN entry on desktop.

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KeyboardAwarePinEntry(
    modifier: Modifier = Modifier,
    onDigitPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    content: @Composable () -> Unit
)
```

**Parameters:**
- `onDigitPressed`: Called when numeric key (0-9) pressed
- `onBackspace`: Called when Backspace key pressed
- `onSubmit`: Called when Enter key pressed
- `content`: Child composables that receive keyboard events

**Usage Example:**
```kotlin
KeyboardAwarePinEntry(
    onDigitPressed = { digit -> pin += digit },
    onBackspace = { pin = pin.dropLast(1) },
    onSubmit = { submitPin() }
) {
    OutlinedTextField(
        value = pin,
        onValueChange = { },  // Controlled by keyboard
        readOnly = true,
        visualTransformation = PasswordVisualTransformation()
    )
}
```

**Captured Keys:**
| Key | Keycode | Action |
|-----|---------|--------|
| 0-9 | 7-16 | Digit pressed |
| Numpad 0-9 | 145-154 | Digit pressed (alternate) |
| Backspace | Key.Backspace | Delete last digit |
| Enter | Key.Enter | Submit PIN |
| Escape | Key.Escape | (Ignored, no default) |

### Utility Functions

#### `KeyEvent.getInputType(): KeyboardInputType`
Classifies keyboard input type.

```kotlin
fun KeyEvent.getInputType(): KeyboardInputType = when {
    this.key.keyCode in 7..16 || this.key.keyCode in 145..154 -> KeyboardInputType.NUMERIC
    this.key == Key.Backspace || this.key == Key.Enter -> KeyboardInputType.FUNCTIONAL
    else -> KeyboardInputType.NONE
}
```

**Returns:**
- `NUMERIC` - Number key pressed
- `FUNCTIONAL` - Control key pressed (Backspace, Enter)
- `NONE` - Other key

### Data Classes

#### `KeyboardState`
Tracks keyboard input state.

```kotlin
class KeyboardState {
    var lastKeyEvent: KeyEvent? = null
    var inputMethodAutoDetected: Boolean = false
}
```

---

## Module: LoginScreen.kt

### Main Composable

#### `LoginScreen`
Device-aware login entry point.

```kotlin
@Composable
fun LoginScreen(
    localRepository: LocalRepository,
    onLogin: (SessionState) -> Unit
)
```

**State Management:**
```kotlin
var allStaff: List<StaffMember>     // Loaded staff members
var picked: StaffMember?             // Selected staff
var pin: String                      // PIN input
var error: String?                   // Error message
var validating: Boolean              // Loading state
var loadingStaff: Boolean            // Staff loading state
var staffLoadError: String?          // Staff loading error
var reloadToken: Int                 // Reload trigger
```

**Flow:**
1. Load staff from LocalRepository or FakeRepository
2. User selects staff member
3. Route to device-appropriate PIN entry
4. Capture input (keyboard or touch)
5. Validate PIN (minimum 4 digits)
6. Call `onLogin` callback

### Desktop PIN Entry

#### `DesktopPinEntry`
Keyboard-optimized PIN entry for desktop.

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DesktopPinEntry(
    staff: StaffMember,
    pin: String,
    error: String?,
    validating: Boolean,
    onDigit: (String) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
)
```

**Features:**
- Keyboard event handling via `KeyboardAwarePinEntry`
- Read-only text field (PIN updated by keyboard events)
- Password visualization (dots)
- Eye icon toggle for PIN visibility
- Submit button with "Enter" hint
- Error message display
- Loading spinner during validation

**Keyboard Controls:**
- 0-9: Add digit to PIN
- Backspace: Delete last digit
- Enter: Submit PIN

### Mobile PIN Entry

#### `MobilePinEntry`
Touch-optimized PIN entry for mobile.

```kotlin
@Composable
private fun MobilePinEntry(
    staff: StaffMember,
    pin: String,
    error: String?,
    validating: Boolean,
    onDigit: (String) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    layoutConfig: LayoutConfig,
)
```

**Features:**
- Visual PIN pad (3x4 grid of buttons)
- Eye icon toggle for visibility
- Adaptive PIN pad height per device
- Scrollable staff selector
- Error message display
- Loading spinner during validation

**PIN Pad Layout:**
```
┌─────┬─────┬─────┐
│  1  │  2  │  3  │
├─────┼─────┼─────┤
│  4  │  5  │  6  │
├─────┼─────┼─────┤
│  7  │  8  │  9  │
├─────┼─────┼─────┤
│  ⌫  │  0  │  ✓  │
└─────┴─────┴─────┘
```

### Staff Selection

#### `MobileStaffSelector`
Responsive staff selection for mobile.

```kotlin
@Composable
private fun MobileStaffSelector(
    allStaff: List<StaffMember>,
    loading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onPick: (StaffMember) -> Unit,
    modifier: Modifier = Modifier,
)
```

**Features:**
- Searchable staff list
- Grouped by role (Doctor, Nurse, etc.)
- Compact staff row items
- Scrollable list
- Error handling with retry
- Loading spinner

---

## Keyboard Event Codes

### Numeric Keys (Desktop Layout)
```
KeyCode 7  → "0"
KeyCode 8  → "1"
KeyCode 9  → "2"
KeyCode 10 → "3"
KeyCode 11 → "4"
KeyCode 12 → "5"
KeyCode 13 → "6"
KeyCode 14 → "7"
KeyCode 15 → "8"
KeyCode 16 → "9"
```

### Numeric Keypad (Alternative)
```
KeyCode 145 → "0"
KeyCode 146 → "1"
...
KeyCode 154 → "9"
```

### Special Keys
```
Key.Backspace → Delete
Key.Enter     → Submit
Key.Escape    → No action
```

---

## State Diagram

```
┌─────────────────────┐
│   Load Staff List   │
└──────────┬──────────┘
           │
           ▼
    ┌──────────────┐
    │  Show List   │
    └──────┬───────┘
           │
           ├─── No Selection ──► Wait
           │
           └─── User Selects ──► ┌──────────────────┐
                                  │  Route by Device │
                                  └────┬─────┬──────┘
                                       │     │
                    ┌──────────────────┘     └────────────┐
                    │                                     │
                    ▼                                     ▼
            ┌────────────────┐              ┌──────────────────────┐
            │ DesktopPinEntry│              │ MobilePinEntry       │
            │ (Keyboard)     │              │ (Touch + Keyboard)   │
            └────────┬───────┘              └──────────┬───────────┘
                     │                                 │
         ┌───────────┴──────────────────────────────┬──┘
         │                                          │
         ▼                                          ▼
    ┌──────────────┐                      ┌──────────────────┐
    │ Validate PIN │                      │  Show PIN Pad    │
    │ (min 4 digits)                      │  Capture Input   │
    └────┬─────┬──┘                       └────┬────────┬────┘
         │     │                               │        │
      ✗  │     │ ✓                         ✗   │        │ ✓
         ▼     ▼                               ▼        ▼
    ┌─────────────┐                    ┌─────────────────┐
    │ Show Error  │                    │ Validate PIN    │
    └─────────────┘                    └──┬──────────┬───┘
           ▲                              │          │
           │                           ✗  │          │ ✓
           └──────────────────────────────┘          │
                                                     ▼
                                            ┌──────────────────┐
                                            │  Create Session  │
                                            │  Navigate Home   │
                                            └──────────────────┘
```

---

## Performance Considerations

### Recomposition
- `deviceType` is cached via `rememberDeviceType()`
- Layout changes only on screen dimension changes
- PIN input recomposes only on state change

### Memory
- No heavy allocations in keyboard handler
- Staff list pagination for large datasets
- Lazy grid for PIN pad buttons

### Rendering
- Desktop: Single Row layout (efficient)
- Mobile: Column layout (simple)
- Grid only shown when needed

---

## Testing Guide

### Unit Tests
```kotlin
@Test
fun testDeviceTypeDetection() {
    val deviceType = rememberDeviceType()
    assertEquals(DeviceType.MOBILE_PORTRAIT, deviceType)
}

@Test
fun testLayoutConfig() {
    val config = DeviceType.MOBILE_PORTRAIT.getLayoutConfig()
    assertEquals(320.dp, config.pinEntryWidth)
    assertTrue(config.useFullWidth)
}
```

### UI Tests
```kotlin
@Test
fun testDesktopKeyboardInput() {
    composeTestRule.setContent {
        DesktopPinEntry(...)
    }
    // Simulate keyboard input
    // Verify PIN updated
}

@Test
fun testMobilePinPad() {
    composeTestRule.setContent {
        MobilePinEntry(...)
    }
    // Tap PIN buttons
    // Verify PIN updated
}
```

---

## Integration Example

```kotlin
@Composable
fun MyApp(navController: NavHostController) {
    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                localRepository = remember { 
                    LocalRepository(get())  // Dependency injection
                },
                onLogin = { session ->
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen()
        }
    }
}
```

---

## Troubleshooting

### Keyboard events not captured
- Check `@ExperimentalComposeUiApi` is applied
- Verify `onKeyEvent` modifier is on root Composable
- Ensure TextField is inside KeyboardAwarePinEntry

### PIN pad buttons unresponsive
- Verify `onClick` handlers are connected
- Check `onDigit`, `onBack`, `onSubmit` callbacks
- Ensure buttons aren't disabled during validation

### Layout not adapting
- Check `LocalConfiguration.current` is read
- Verify `rememberDeviceType()` is called
- Inspect screen width threshold (600dp)

### Eye icon not working
- Check `showPin` state is managed
- Verify `VisualTransformation` is applied
- Ensure `IconButton` onClick updates state

---

## References

### Compose Documentation
- LocalConfiguration: Screen dimensions
- onKeyEvent: Keyboard capture
- KeyboardOptions: Input method hints
- VisualTransformation: Password masking

### Android Documentation
- Configuration changes
- Screen sizes and densities
- Keyboard input handling

---

*Last Updated: May 20, 2026*

