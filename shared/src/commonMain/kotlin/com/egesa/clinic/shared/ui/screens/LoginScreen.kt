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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.StaffMember
import com.egesa.clinic.shared.data.FakeRepository
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.ui.components.RoleBadge
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.theme.*

@Composable
fun LoginScreen(localRepository: LocalRepository, onLogin: (SessionState) -> Unit) {
    var allStaff   by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
    var picked     by remember { mutableStateOf<StaffMember?>(null) }
    var pin        by remember { mutableStateOf("") }
    var error      by remember { mutableStateOf<String?>(null) }
    var validating by remember { mutableStateOf(false) }
    var loadingStaff by remember { mutableStateOf(true) }
    var staffLoadError by remember { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }

    LaunchedEffect(reloadToken) {
        loadingStaff = true
        staffLoadError = null
        try {
            val localStaff = localRepository.getAllStaff()
            allStaff = if (localStaff.isNotEmpty()) localStaff else FakeRepository.getStaff()
        } catch (e: Exception) {
            allStaff = emptyList()
            staffLoadError = "Unable to load staff list. Please retry or check connectivity."
        } finally {
            loadingStaff = false
        }
    }

    Box(Modifier.fillMaxSize().background(Navy900)) {
        Row(Modifier.fillMaxSize()) {
            // ── Left panel: branding ────────────────────────────────────────
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
                // System status indicators
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

            // ── Right panel: auth ───────────────────────────────────────────
            Box(
                Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (picked == null) {
                    StaffSelector(
                        allStaff = allStaff,
                        loading = loadingStaff,
                        loadError = staffLoadError,
                        onRetry = { reloadToken++ },
                        onPick = { picked = it; pin = ""; error = null },
                    )
                } else {
                    PinEntry(
                        staff      = picked!!,
                        pin        = pin,
                        error      = error,
                        validating = validating,
                        onDigit    = { if (pin.length < 6) { pin += it; error = null } },
                        onBack     = { pin = pin.dropLastOrEmpty() },
                        onCancel   = { picked = null; pin = ""; error = null },
                        onSubmit   = {
                            when {
                                pin.length < 4 -> error = "PIN must be at least 4 digits"
                                else           -> {
                                    validating = true
                                    // TODO: replace with FakeRepository.validatePin() in a LaunchedEffect
                                    onLogin(
                                        SessionState(
                                            staffId    = picked!!.id,
                                            fullName   = picked!!.fullName,
                                            role       = picked!!.role,
                                            shiftLabel = "Day shift",
                                        )
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

// ── Staff selector with search ─────────────────────────────────────────────────

@Composable
private fun StaffSelector(
    allStaff: List<StaffMember>,
    loading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
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
            Text("Select your profile to continue", fontSize = 13.sp, color = Navy200)
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
                listOf(UserRole.DOCTOR, UserRole.NURSE, UserRole.RECEPTIONIST, UserRole.ADMIN)
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

// ── PIN entry ──────────────────────────────────────────────────────────────────

@Composable
private fun PinEntry(
    staff: StaffMember,
    pin: String,
    error: String?,
    validating: Boolean,
    onDigit: (String) -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(Modifier.width(340.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Enter your PIN", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
            Text("PIN is case-sensitive and secure", fontSize = 13.sp, color = Navy200)
        }

        // Selected staff chip
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

        // PIN dot display
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(6) { i ->
                Box(
                    Modifier.size(13.dp).clip(RoundedCornerShape(50))
                        .background(if (i < pin.length) Teal500 else SidebarActive),
                )
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

        // PIN pad — safe to use LazyVerticalGrid here because this widget
        // is NOT inside a LazyColumn or other lazy layout.
        val keys = listOf("1","2","3","4","5","6","7","8","9","⌫","0","✓")
        if (validating) {
            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = Teal500, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    Text("Verifying…", fontSize = 12.sp, color = Navy200)
                }
            }
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Fixed(3),
                modifier              = Modifier.fillMaxWidth().height(220.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (key == "✓") Teal600 else SidebarActive,
                            contentColor   = White,
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                    ) {
                        Text(key, fontSize = if (key.length > 1) 16.sp else 19.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun String.dropLastOrEmpty() = if (isEmpty()) this else dropLast(1)
