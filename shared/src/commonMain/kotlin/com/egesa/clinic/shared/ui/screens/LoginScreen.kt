package com.egesa.clinic.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.StaffMember
import com.egesa.clinic.shared.data.FakeRepository
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.ui.components.RoleBadge
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.theme.*
import com.egesa.clinic.shared.ui.responsive.*
import kotlinx.coroutines.launch


@Composable
fun LoginScreen(localRepository: LocalRepository, onLogin: (SessionState) -> Unit) {
    val deviceType = rememberDeviceType()
    val layoutConfig = deviceType.getLayoutConfig()
    val scope = rememberCoroutineScope()

    var allStaff   by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
    var picked     by remember { mutableStateOf<StaffMember?>(null) }
    var manualStaffId by remember { mutableStateOf("") }
    var pin        by remember { mutableStateOf("") }
    var error      by remember { mutableStateOf<String?>(null) }
    var validating by remember { mutableStateOf(false) }
    var loadingStaff by remember { mutableStateOf(true) }
    var staffLoadError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    var manualMode by remember { mutableStateOf(false) }

    LaunchedEffect(reloadToken) {
        loadingStaff = true
        staffLoadError = null
        try {
            val apiStaff = FakeRepository.getStaff()
            val localStaff = localRepository.getAllStaff()
            allStaff = (apiStaff + localStaff).distinctBy { it.id }
        } catch (_: Exception) {
            allStaff = runCatching { localRepository.getAllStaff() }.getOrDefault(emptyList())
            if (allStaff.isEmpty()) {
                staffLoadError = "Unable to load staff. Enter your Staff ID or retry."
                manualMode = true
            } else {
                staffLoadError = "Using saved staff profiles. API staff directory is unavailable."
            }
        } finally {
            loadingStaff = false
        }
    }

    val continueWithManualStaffId = {
        val staffId = manualStaffId.trim()
        if (staffId.isBlank()) {
            error = "Enter your Staff ID"
        } else {
            picked = StaffMember(
                id = staffId,
                fullName = staffId,
                role = UserRole.RECEPTIONIST,
                department = "Manual sign in"
            )
            pin = ""
            error = null
        }
    }

    val handleSubmit = {
        val staff = picked
        when {
            staff == null -> error = "Select a staff profile first"
            pin.length < 4 -> error = "PIN must be at least 4 digits"
            validating -> Unit
            else -> {
                validating = true
                error = null
                scope.launch {
                    try {
                        val result = FakeRepository.login(staff.id, pin)
                        val response = result.getOrElse {
                            error = it.message?.takeIf { message -> message.isNotBlank() } ?: "Invalid credentials"
                            return@launch
                        }
                        onLogin(
                            SessionState(
                                staffId = staff.id,
                                fullName = response.staffName.ifBlank { staff.fullName },
                                role = runCatching { UserRole.valueOf(response.role) }.getOrDefault(staff.role),
                                shiftLabel = "Day shift",
                                token = response.accessToken
                            )
                        )
                    } finally {
                        validating = false
                    }
                }
                Unit
            }
        }
    }

    // Mobile portrait: Full width, no side branding
    when (deviceType) {
        DeviceType.MOBILE_PORTRAIT -> {
            Column(
                Modifier.fillMaxSize().background(Navy900).padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (picked == null) {
                    if (manualMode || (allStaff.isEmpty() && !loadingStaff)) {
                        ManualStaffIdEntry(
                            staffId = manualStaffId,
                            error = staffLoadError,
                            onStaffIdChange = { manualStaffId = it },
                            onContinue = continueWithManualStaffId,
                            onUseStaffList = if (allStaff.isNotEmpty()) {{ manualMode = false; error = null }} else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        MobileStaffSelector(
                            allStaff = allStaff,
                            loading = loadingStaff,
                            loadError = staffLoadError,
                            onRetry = { reloadToken += 1 },
                            onManualEntry = { manualMode = true; error = null },
                            onPick = { picked = it; pin = ""; error = null },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    MobilePinEntry(
                        staff      = picked!!,
                        pin        = pin,
                        error      = error,
                        validating = validating,
                        onDigit    = { if (pin.length < 6) { pin += it; error = null } },
                        onBack     = { pin = pin.dropLastOrEmpty() },
                        onCancel   = { picked = null; pin = ""; error = null },
                        onSubmit   = handleSubmit,
                        layoutConfig = layoutConfig
                    )
                }
            }
        }
        // Mobile landscape: Side-by-side if space allows
        DeviceType.MOBILE_LANDSCAPE -> {
            Box(Modifier.fillMaxSize().background(Navy900)) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().padding(16.dp), contentAlignment = Alignment.Center) {
                        if (picked == null) {
                            if (manualMode || (allStaff.isEmpty() && !loadingStaff)) {
                                ManualStaffIdEntry(
                                    staffId = manualStaffId,
                                    error = staffLoadError,
                                    onStaffIdChange = { manualStaffId = it },
                                    onContinue = continueWithManualStaffId,
                                    onUseStaffList = if (allStaff.isNotEmpty()) {{ manualMode = false; error = null }} else null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                MobileStaffSelector(
                                    allStaff = allStaff,
                                    loading = loadingStaff,
                                    loadError = staffLoadError,
                                    onRetry = { reloadToken += 1 },
                                    onManualEntry = { manualMode = true; error = null },
                                    onPick = { picked = it; pin = ""; error = null },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    if (picked != null) {
                        Box(Modifier.weight(1f).fillMaxHeight().padding(16.dp), contentAlignment = Alignment.Center) {
                            MobilePinEntry(
                                staff      = picked!!,
                                pin        = pin,
                                error      = error,
                                validating = validating,
                                onDigit    = { if (pin.length < 6) { pin += it; error = null } },
                                onBack     = { pin = pin.dropLastOrEmpty() },
                                onCancel   = { picked = null; pin = ""; error = null },
                                onSubmit   = handleSubmit,
                                layoutConfig = layoutConfig
                            )
                        }
                    }
                }
            }
        }
        // Tablet and Desktop: Side-by-side with branding
        else -> {
            Box(Modifier.fillMaxSize().background(Navy900)) {
                Row(Modifier.fillMaxSize()) {
                    // Left panel: branding
                    Column(
                        Modifier.weight(1f).fillMaxHeight().background(Navy950).padding(40.dp),
                        verticalArrangement   = Arrangement.Center,
                        horizontalAlignment   = Alignment.Start,
                    ) {
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Navy700),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("EC", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Egesa Medical", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = White, lineHeight = 34.sp)
                        Text("Clinic", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Teal500, lineHeight = 34.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Integrated Hospital Management\nSystem for clinical staff.",
                            fontSize = 14.sp, color = Navy200, lineHeight = 22.sp,
                        )
                        Spacer(Modifier.height(40.dp))
                        listOf("System Online", "Synced", "All services running").forEach { item ->
                            Row(
                                Modifier.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(Teal500))
                                Text(item, fontSize = 12.sp, color = Navy200)
                            }
                        }
                    }

                    // Right panel: auth (Desktop with keyboard support)
                    Box(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (picked == null) {
                            if (manualMode || (allStaff.isEmpty() && !loadingStaff)) {
                                ManualStaffIdEntry(
                                    staffId = manualStaffId,
                                    error = staffLoadError,
                                    onStaffIdChange = { manualStaffId = it },
                                    onContinue = continueWithManualStaffId,
                                    onUseStaffList = if (allStaff.isNotEmpty()) {{ manualMode = false; error = null }} else null,
                                    modifier = Modifier.width(400.dp)
                                )
                            } else {
                                StaffSelector(
                                    allStaff = allStaff,
                                    loading = loadingStaff,
                                    loadError = staffLoadError,
                                    onRetry = { reloadToken += 1 },
                                    onManualEntry = { manualMode = true; error = null },
                                    onPick = { picked = it; pin = ""; error = null },
                                )
                            }
                        } else {
                            DesktopPinEntry(
                                staff      = picked!!,
                                pin        = pin,
                                error      = error,
                                validating = validating,
                                onDigit    = { if (pin.length < 6) { pin += it; error = null } },
                                onBack     = { pin = pin.dropLastOrEmpty() },
                                onCancel   = { picked = null; pin = ""; error = null },
                                onSubmit   = handleSubmit,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Staff selector with search ─────────────────────────────────────────────────

@Composable
private fun ManualStaffIdEntry(
    staffId: String,
    error: String?,
    onStaffIdChange: (String) -> Unit,
    onContinue: () -> Unit,
    onUseStaffList: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Navy950).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sign in", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
        Text("Enter the Staff ID issued by the clinic.", fontSize = 13.sp, color = Navy200)

        OutlinedTextField(
            value = staffId,
            onValueChange = onStaffIdChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Staff ID (e.g. DR-001)", fontSize = 13.sp, color = Navy200) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SidebarActive,
                unfocusedContainerColor = SidebarActive,
                focusedBorderColor = Teal500,
                unfocusedBorderColor = SidebarBorder,
                focusedTextColor = White,
                unfocusedTextColor = White,
            ),
        )

        if (!error.isNullOrBlank()) {
            Text(error, color = StatusCritical, fontSize = 12.sp)
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Teal500, contentColor = Navy950),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
        }

        if (onUseStaffList != null) {
            TextButton(onClick = onUseStaffList, modifier = Modifier.fillMaxWidth()) {
                Text("Choose from staff list", color = Navy200, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StaffSelector(
    allStaff: List<StaffMember>,
    loading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onManualEntry: () -> Unit,
    onPick: (StaffMember) -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, allStaff) {
        if (query.isBlank()) allStaff
        else allStaff.filter { s ->
            s.fullName.contains(query, ignoreCase = true) ||
            s.id.contains(query, ignoreCase = true) ||
            s.role.name.contains(query, ignoreCase = true) ||
            s.department.contains(query, ignoreCase = true)
        }
    }

    Column(Modifier.width(400.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Welcome back", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
            Text("Select your profile or enter a Staff ID.", fontSize = 13.sp, color = Navy200)
        }

        // Search field
        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("Search name, ID, role, department…", fontSize = 13.sp, color = Navy200) },
            singleLine    = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape         = RoundedCornerShape(8.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = SidebarActive,
                unfocusedContainerColor = SidebarActive,
                focusedBorderColor      = Teal500,
                unfocusedBorderColor    = SidebarBorder,
                focusedTextColor        = White,
                unfocusedTextColor      = White,
                cursorColor             = Teal500,
            ),
            leadingIcon   = { Text("⌕", color = Navy200, fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp)) },
            trailingIcon  = if (query.isNotBlank()) {{
                Text("✕", color = Navy200, fontSize = 12.sp,
                    modifier = Modifier.padding(end = 4.dp).clickable { query = "" })
            }} else null,
        )

        TextButton(onClick = onManualEntry, modifier = Modifier.align(Alignment.End)) {
            Text("Enter Staff ID manually", color = Navy200, fontSize = 12.sp)
        }

        // Staff list
        if (loading) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal500, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        } else if (loadError != null) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SidebarActive).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(loadError, fontSize = 13.sp, color = Color(0xFFFFB4AB))
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            }
        } else if (allStaff.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(8.dp)).background(SidebarActive),
                contentAlignment = Alignment.Center,
            ) {
                Text("No staff profiles are available.", fontSize = 13.sp, color = Navy200)
            }
        } else if (filtered.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(8.dp)).background(SidebarActive),
                contentAlignment = Alignment.Center,
            ) {
                Text("No staff found for \"$query\"", fontSize = 13.sp, color = Navy200)
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Group by role
                val grouped = filtered.groupBy { it.role }
                listOf(UserRole.DOCTOR, UserRole.NURSE, UserRole.PHARMACIST, UserRole.RECEPTIONIST, UserRole.ADMIN)
                    .forEach { role ->
                        val group = grouped[role] ?: return@forEach
                        item {
                            Text(
                                role.name.lowercase().replaceFirstChar { it.uppercase() } + "s",
                                fontSize  = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color     = Navy200,
                                letterSpacing = 1.sp,
                                modifier  = Modifier.padding(top = 6.dp, bottom = 2.dp),
                            )
                        }
                        items(group) { staff ->
                            StaffRow(staff = staff, onClick = { onPick(staff) })
                        }
                    }
            }
        }
    }
}

// ── Mobile Staff Selector ──────────────────────────────────────────────────

@Composable
private fun MobileStaffSelector(
    allStaff: List<StaffMember>,
    loading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    onManualEntry: () -> Unit,
    onPick: (StaffMember) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, allStaff) {
        if (query.isBlank()) allStaff
        else allStaff.filter { s ->
            s.fullName.contains(query, ignoreCase = true) ||
            s.id.contains(query, ignoreCase = true) ||
            s.role.name.contains(query, ignoreCase = true) ||
            s.department.contains(query, ignoreCase = true)
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Welcome", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = White)
            Text("Select profile or enter Staff ID", fontSize = 12.sp, color = Navy200)
        }

        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("Search…", fontSize = 12.sp, color = Navy200) },
            singleLine    = true,
            shape         = RoundedCornerShape(8.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = SidebarActive,
                unfocusedContainerColor = SidebarActive,
                focusedBorderColor      = Teal500,
                unfocusedBorderColor    = SidebarBorder,
                focusedTextColor        = White,
                unfocusedTextColor      = White,
                cursorColor             = Teal500,
            ),
        )

        TextButton(onClick = onManualEntry, modifier = Modifier.align(Alignment.End)) {
            Text("Enter Staff ID", color = Navy200, fontSize = 12.sp)
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal500, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        } else if (loadError != null) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SidebarActive).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(loadError, fontSize = 12.sp, color = Color(0xFFFFB4AB))
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
            }
        } else if (allStaff.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)).background(SidebarActive), contentAlignment = Alignment.Center) {
                Text("No staff profiles", fontSize = 12.sp, color = Navy200)
            }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(8.dp)).background(SidebarActive), contentAlignment = Alignment.Center) {
                Text("No match for \"$query\"", fontSize = 12.sp, color = Navy200)
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val grouped = filtered.groupBy { it.role }
                listOf(UserRole.DOCTOR, UserRole.NURSE, UserRole.PHARMACIST, UserRole.RECEPTIONIST, UserRole.ADMIN)
                    .forEach { role ->
                        val group = grouped[role] ?: return@forEach
                        item {
                            Text(
                                role.name.lowercase().replaceFirstChar { it.uppercase() } + "s",
                                fontSize  = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color     = Navy200,
                                modifier  = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }
                        items(group) { staff ->
                            MobileStaffRow(staff = staff, onClick = { onPick(staff) })
                        }
                    }
            }
        }
    }
}

@Composable
private fun MobileStaffRow(staff: StaffMember, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SidebarActive)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(Navy700),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                staff.fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(staff.fullName, color = White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(staff.department, color = Navy200, fontSize = 10.sp)
        }
        RoleBadge(staff.role.name)
    }
}
@Composable
private fun StaffRow(staff: StaffMember, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SidebarActive)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Avatar circle
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(Navy700),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                staff.fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(staff.fullName, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(staff.department, color = Navy200, fontSize = 11.sp)
        }
        RoleBadge(staff.role.name)
        Text("›", color = Navy200, fontSize = 18.sp)
    }
}

// ── Desktop PIN Entry (Keyboard Support) ──────────────────────────────────

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
) {
    Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Enter PIN", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
            Text("Use keyboard (0-9, Backspace to delete, Enter to submit)", fontSize = 12.sp, color = Navy200)
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SidebarActive).padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(50)).background(Navy700),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    staff.fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                    color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(staff.fullName, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(staff.role.name + "  ·  " + staff.department, color = Navy200, fontSize = 11.sp)
            }
            TextButton(onClick = onCancel) {
                Text("Change", color = Navy200, fontSize = 12.sp)
            }
        }

        // PIN input field with keyboard support
        KeyboardAwarePinEntry(
            modifier = Modifier.fillMaxWidth(),
            onDigitPressed = onDigit,
            onBackspace = onBack,
            onSubmit = onSubmit,
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = { },  // Controlled by keyboard events
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter PIN…", fontSize = 13.sp, color = Navy200) },
                singleLine = true,
                readOnly = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SidebarActive,
                    unfocusedContainerColor = SidebarActive,
                    focusedBorderColor = Teal500,
                    unfocusedBorderColor = SidebarBorder,
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                ),
            )
        }

        // PIN dot display
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(6) { i ->
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i < pin.length) Teal500 else SidebarActive),
                )
                if (i < 5) Spacer(Modifier.width(8.dp))
            }
        }

        // Error message
        if (error != null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF3D1313)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(StatusCritical))
                Text(error, color = Color(0xFFFCA5A5), fontSize = 12.sp)
            }
        }

        // Submit button
        if (validating) {
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = Teal500, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    Text("Verifying…", fontSize = 12.sp, color = Navy200)
                }
            }
        } else {
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal600),
            ) {
                Text("Submit (Enter)", fontWeight = FontWeight.SemiBold)
            }
        }

        Text("💡 Tip: Use keyboard numeric keys or numpad for faster entry", fontSize = 11.sp, color = Navy200)
    }
}

// ── Mobile PIN Entry (Touch + Eye Icon) ────────────────────────────────────

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
) {
    var showPin by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Enter PIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
            Text("Use PIN pad or keyboard", fontSize = 11.sp, color = Navy200)
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SidebarActive).padding(10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(Navy700),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    staff.fullName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                    color = White, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(staff.fullName, color = White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(staff.role.name + "  ·  " + staff.department, color = Navy200, fontSize = 10.sp)
            }
            TextButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                Text("✕", color = Navy200, fontSize = 14.sp)
            }
        }

        // PIN Input with visibility toggle
        OutlinedTextField(
            value = pin,
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter PIN…", fontSize = 12.sp, color = Navy200) },
            singleLine = true,
            readOnly = true,
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showPin = !showPin }) {
                    Text(
                        text = if (showPin) "HIDE" else "SHOW",
                        color = Teal500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SidebarActive,
                unfocusedContainerColor = SidebarActive,
                focusedBorderColor = Teal500,
                unfocusedBorderColor = SidebarBorder,
                focusedTextColor = White,
                unfocusedTextColor = White,
            ),
        )

        // PIN dot display
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(6) { i ->
                Box(
                    Modifier
                        .size(layoutConfig.pinDotSize)
                        .clip(RoundedCornerShape(50))
                        .background(if (i < pin.length) Teal500 else SidebarActive),
                )
                if (i < 5) Spacer(Modifier.width(6.dp))
            }
        }

        // Error message
        if (error != null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF3D1313)).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(4.dp).clip(RoundedCornerShape(50)).background(StatusCritical))
                Text(error, color = Color(0xFFFCA5A5), fontSize = 11.sp)
            }
        }

        // PIN pad for mobile
        if (layoutConfig.pinPadHeight > 0.dp) {
            val keys = listOf("1","2","3","4","5","6","7","8","9","⌫","0","✓")
            if (validating) {
                Box(Modifier.fillMaxWidth().height(layoutConfig.pinPadHeight), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Teal500, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        Text("Verifying…", fontSize = 11.sp, color = Navy200)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(3),
                    modifier              = Modifier.fillMaxWidth().heightIn(max = layoutConfig.pinPadHeight),
                    verticalArrangement   = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    userScrollEnabled     = false,
                ) {
                    items(keys) { key ->
                        Button(
                            onClick = {
                                when (key) {
                                    "⌫" -> onBack()
                                    "✓" -> onSubmit()
                                    else -> onDigit(key)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape    = RoundedCornerShape(6.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = if (key == "✓") Teal600 else SidebarActive,
                                contentColor   = White,
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                        ) {
                            Text(key, fontSize = if (key.length > 1) 14.sp else 17.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

private fun String.dropLastOrEmpty() = if (isEmpty()) this else dropLast(1)
