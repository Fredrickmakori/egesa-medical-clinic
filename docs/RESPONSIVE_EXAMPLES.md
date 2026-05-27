# Responsive Screen Implementation Examples

This document provides practical examples for making screens responsive using the new responsive utilities.

## Example 1: Dashboard Screen (Already responsive)

The dashboard uses responsive grid columns:

```kotlin
val columns = responsiveGridColumns()  // 1, 2, or 3 based on screen size

LazyColumn {
    item {
        // KPI cards adapt: 1 per row mobile, 2 per row tablet, 3 per row desktop
        kpis!!.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { metric ->
                    MetricCard(
                        title = metric.title,
                        value = metric.value,
                        modifier = Modifier.weight(1f),  // Equal width
                    )
                }
            }
        }
    }
}
```

## Example 2: Two-Panel Layout

Making a two-panel layout responsive:

```kotlin
@Composable
fun PatientDetailScreen(patient: Patient) {
    if (isWideLayout()) {
        // Desktop/Tablet: Side-by-side
        Row(Modifier.fillMaxSize()) {
            PatientListPanel(Modifier.width(300.dp))
            PatientDetailPanel(patient, Modifier.weight(1f))
        }
    } else {
        // Mobile: Tab-based or stacked
        Column(Modifier.fillMaxSize()) {
            var selectedTab by remember { mutableStateOf(0) }
            
            TabRow(selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("List")
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Details")
                }
            }
            
            when (selectedTab) {
                0 -> PatientListPanel()
                1 -> PatientDetailPanel(patient)
            }
        }
    }
}
```

## Example 3: Form Layout

Making forms responsive:

```kotlin
@Composable
fun PatientFormScreen(patient: Patient) {
    ResponsiveColumn {
        val padding = responsiveHorizontalPadding()
        
        Column(
            Modifier
                .padding(horizontal = padding.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Full width on mobile, side-by-side on larger screens
            if (isWideLayout()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = patient.firstName,
                        label = { Text("First Name") },
                        modifier = Modifier.weight(1f),
                    )
                    TextField(
                        value = patient.lastName,
                        label = { Text("Last Name") },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                TextField(
                    value = patient.firstName,
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = patient.lastName,
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            
            // Rest of form fields...
        }
    }
}
```

## Example 4: Table/List with Actions

Making tables responsive:

```kotlin
@Composable
fun PatientsListScreen(patients: List<Patient>) {
    if (isWideLayout()) {
        // Desktop: Full data table
        LazyColumn {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Name", Modifier.weight(2f), fontWeight = FontWeight.Bold)
                    Text("Status", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Ward", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Actions", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
            }
            items(patients) { patient ->
                PatientTableRow(patient)
            }
        }
    } else {
        // Mobile: Card layout
        LazyColumn {
            items(patients) { patient ->
                PatientCard(patient)
            }
        }
    }
}
```

## Example 5: Bottom Sheet vs Dialog

Using appropriate modals for screen size:

```kotlin
@Composable
fun PatientActionsScreen(patient: Patient) {
    var showActions by remember { mutableStateOf(false) }
    
    if (shouldUseCompactUI()) {
        // Mobile: Bottom sheet
        if (showActions) {
            ModalBottomSheet(onDismissRequest = { showActions = false }) {
                PatientActionsContent(patient)
            }
        }
    } else {
        // Desktop: Dialog or side panel
        if (showActions) {
            AlertDialog(
                onDismissRequest = { showActions = false },
                title = { Text("Actions") },
                text = { PatientActionsContent(patient) },
            )
        }
    }
}
```

## Example 6: Multi-Step Form

Responsive form wizard:

```kotlin
@Composable
fun RegistrationWizard() {
    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf("Patient Info", "Contact", "Medical History")
    
    Column(Modifier.fillMaxSize()) {
        // Progress indicator - different layout based on screen
        if (isWideLayout()) {
            HorizontalProgressIndicator(
                currentStep = currentStep,
                steps = steps,
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        } else {
            VerticalProgressIndicator(currentStep, steps.size)
        }
        
        // Form content
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (currentStep) {
                0 -> PatientInfoStep { currentStep = 1 }
                1 -> ContactStep { currentStep = 2 }
                2 -> MedicalHistoryStep { currentStep = 3 }
            }
        }
        
        // Navigation buttons
        Row(
            Modifier
                .fillMaxWidth()
                .padding(responsiveHorizontalPadding().dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentStep > 0) {
                Button(onClick = { currentStep-- }) { Text("Back") }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { currentStep++ }) { Text("Next") }
        }
    }
}
```

## Example 7: Message/Chat Layout

Responsive messaging interface:

```kotlin
@Composable
fun MessagingScreen() {
    if (isWideLayout()) {
        // Tablet/Desktop: Split view
        Row(Modifier.fillMaxSize()) {
            ConversationList(Modifier.width(300.dp))
            VerticalDivider()
            MessageThread(Modifier.weight(1f))
        }
    } else {
        // Mobile: Single view with navigation
        var showList by remember { mutableStateOf(true) }
        
        Box(Modifier.fillMaxSize()) {
            if (showList) {
                ConversationList {
                    showList = false
                }
            } else {
                MessageThread(
                    onBack = { showList = true }
                )
            }
        }
    }
}
```

## Example 8: Sidebar Content

Responsive sidebar/navigation content:

```kotlin
@Composable
fun DashboardWithSidebar() {
    Row(Modifier.fillMaxSize()) {
        // Sidebar - conditional width
        if (shouldShowSidebar()) {
            Box(Modifier.width(responsiveSidebarWidth().dp)) {
                SidebarContent()
            }
        }
        
        // Main content
        Column(Modifier.weight(1f)) {
            TopBar()
            MainContent()
        }
    }
}
```

## Checklist for Making Screens Responsive

- [ ] Use `responsiveHorizontalPadding()` instead of hardcoded padding
- [ ] Use `responsiveGridColumns()` for grid layouts
- [ ] Use `isWideLayout()` to switch between side-by-side and stacked
- [ ] Use `shouldUseCompactUI()` for mobile-specific UX
- [ ] Test on COMPACT (< 600dp), MEDIUM (600-840dp), and EXPANDED (>= 840dp) sizes
- [ ] Verify bottom nav shows only on COMPACT screens
- [ ] Verify sidebar shows on MEDIUM+ screens
- [ ] Test landscape and portrait orientations
- [ ] Ensure touch targets are at least 48dp on mobile
- [ ] Use `Modifier.weight(1f)` for flexible layouts
- [ ] Avoid hardcoded widths/heights where possible
- [ ] Use `LazyColumn`/`LazyRow` for scrollable content
- [ ] Test on actual devices: phones, tablets, desktops


