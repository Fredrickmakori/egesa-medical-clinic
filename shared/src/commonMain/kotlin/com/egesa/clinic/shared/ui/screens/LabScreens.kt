package com.egesa.clinic.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.Permission
import com.egesa.clinic.shared.data.LabWorklistQuery
import com.egesa.clinic.shared.data.LocalRepository
import com.egesa.clinic.shared.domain.LabOrder
import com.egesa.clinic.shared.domain.LabOrderItem
import com.egesa.clinic.shared.domain.LabOrderStatus
import com.egesa.clinic.shared.domain.LabPriority
import com.egesa.clinic.shared.domain.LabResult
import com.egesa.clinic.shared.ui.components.ClinicCard
import com.egesa.clinic.shared.ui.components.ClinicDropdownField
import com.egesa.clinic.shared.ui.components.DashboardMetricUi
import com.egesa.clinic.shared.ui.components.EmptyStateCard
import com.egesa.clinic.shared.ui.components.FormActionRow
import com.egesa.clinic.shared.ui.components.ModuleHeader
import com.egesa.clinic.shared.ui.components.ModuleKpiStrip
import com.egesa.clinic.shared.ui.components.SectionHeader
import com.egesa.clinic.shared.ui.components.StatusDot
import com.egesa.clinic.shared.ui.components.TextBadge
import com.egesa.clinic.shared.ui.components.ToolbarSearchField
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

// ── Main Lab Module ───────────────────────────────────────────────────────────

/**
 * Main entry point for the Lab module, shown when user navigates to LAB_IMAGING.
 * Provides a tabbed interface: Worklist · Result Entry · Patient Orders
 */
@Composable
fun LabModuleScreen(localRepository: LocalRepository, session: SessionState) {
    var activeTab by remember { mutableStateOf(0) }
    var selectedOrder by remember { mutableStateOf<LabOrder?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Load aggregate counts for KPI strip
    var allOrders by remember { mutableStateOf<List<LabOrder>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            allOrders = try {
                localRepository.getLabWorklist(
                    LabWorklistQuery(department = "LAB", status = LabOrderStatus.ORDERED)
                )
            } catch (_: Exception) { emptyList() }
        }
    }

    val tabs = listOf("Worklist", "Result Entry", "Patient Orders")

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Header + KPI ──────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ModuleHeader(
                title = "Lab / Imaging",
                subtitle = "Order tests, track specimens, enter results, and review patient lab history.",
            )

            ModuleKpiStrip(
                listOf(
                    DashboardMetricUi("Pending", "${allOrders.size}", "Awaiting processing", Indigo700),
                    DashboardMetricUi("In Process", "–", "Under analysis", Sky700),
                    DashboardMetricUi("Verified", "–", "Ready for report", Teal700),
                ),
            )
        }

        // ── Tabs ──────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = White,
            contentColor = Indigo700,
            indicator = { tabPositions ->
                if (activeTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = Indigo700,
                    )
                }
            },
            divider = { HorizontalDivider(color = Slate200) },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == index) Indigo700 else Slate500,
                        )
                    },
                )
            }
        }

        // ── Tab Content ───────────────────────────────────────────────────
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF6F7F9)),
        ) {
            when (activeTab) {
                0 -> LabWorklistScreen(
                    localRepository = localRepository,
                    session = session,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onSelectOrder = { order ->
                        selectedOrder = order
                        activeTab = 1 // Switch to result entry
                    },
                )
                1 -> LabResultEntryScreen(
                    localRepository = localRepository,
                    session = session,
                    order = selectedOrder,
                )
                2 -> LabOrderDetailsScreen(
                    localRepository = localRepository,
                    patientId = selectedOrder?.patientId,
                )
            }
        }
    }
}

// ── Worklist Tab ──────────────────────────────────────────────────────────────

@Composable
private fun LabWorklistScreen(
    localRepository: LocalRepository,
    session: SessionState,
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    onSelectOrder: (LabOrder) -> Unit = {},
) {
    var orders by remember { mutableStateOf<List<LabOrder>>(emptyList()) }
    var statusFilter by remember { mutableStateOf(LabOrderStatus.ORDERED.name) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            isLoading = true
            orders = try {
                localRepository.getLabWorklist(
                    LabWorklistQuery(
                        department = "LAB",
                        status = runCatching { LabOrderStatus.valueOf(statusFilter) }.getOrDefault(LabOrderStatus.ORDERED),
                    )
                )
            } catch (_: Exception) { emptyList() }
            isLoading = false
        }
    }

    LaunchedEffect(statusFilter) { refresh() }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // Search + filter bar
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                ToolbarSearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Search by patient ID or test name",
                    modifier = Modifier.weight(1f),
                )
                ClinicDropdownField(
                    value = statusFilter,
                    onValueChange = { statusFilter = it },
                    label = "Status",
                    options = LabOrderStatus.entries.map { it.name },
                    modifier = Modifier.width(200.dp),
                    displayFormatter = { it.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } },
                )
            }
        }

        // Section header with refresh
        item {
            SectionHeader(
                "Orders (${orders.size})",
                action = {
                    IconButton(onClick = { refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp),
                            tint = Slate500,
                        )
                    }
                },
            )
        }

        // Loading state
        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(color = Navy800, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        Text("Loading worklist…", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                }
            }
        } else if (orders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No lab orders",
                    detail = "No orders found with status '${statusFilter.replace("_", " ")}'. Try a different filter.",
                )
            }
        } else {
            // Filter by search query
            val filtered = if (searchQuery.isBlank()) orders else orders.filter { order ->
                order.patientId.contains(searchQuery, ignoreCase = true) ||
                    order.items.any { it.testName.contains(searchQuery, ignoreCase = true) } ||
                    order.id.contains(searchQuery, ignoreCase = true)
            }

            items(filtered) { order ->
                LabOrderCard(
                    order = order,
                    session = session,
                    onOpen = { onSelectOrder(order) },
                    onCollectSample = {
                        scope.launch {
                            localRepository.updateLabOrderStatus(order.id, LabOrderStatus.SAMPLE_COLLECTED, session.staffId)
                            refresh()
                        }
                    },
                )
            }

            if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    EmptyStateCard(
                        title = "No matches",
                        detail = "No orders match \"$searchQuery\". Try a different search term.",
                    )
                }
            }
        }
    }
}

// ── Order Card ────────────────────────────────────────────────────────────────

@Composable
private fun LabOrderCard(
    order: LabOrder,
    session: SessionState,
    onOpen: () -> Unit,
    onCollectSample: () -> Unit,
) {
    ClinicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
    ) {
        Column(
            Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Top row: Order ID + Status + Priority
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            order.id,
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate900,
                        )
                        PriorityBadge(order.priority)
                    }
                    Text(
                        "Patient: ${order.patientId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate500,
                    )
                }
                OrderStatusBadge(order.status)
            }

            // Test names
            if (order.items.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Slate400,
                    )
                    Text(
                        order.items.joinToString(" · ") { it.testName },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600,
                    )
                }
            }

            HorizontalDivider(color = Slate100)

            // Action buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy800),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open", style = MaterialTheme.typography.labelMedium)
                }

                if (session.hasPermission(Permission.LAB_RESULT_MANAGE) && order.status == LabOrderStatus.ORDERED) {
                    Button(
                        onClick = onCollectSample,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("Collect Sample", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ── Result Entry Tab ──────────────────────────────────────────────────────────

@Composable
private fun LabResultEntryScreen(
    localRepository: LocalRepository,
    session: SessionState,
    order: LabOrder?,
) {
    var selectedOrder by remember(order) { mutableStateOf(order) }
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<ResultEntryStatus>(ResultEntryStatus.Idle) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            SectionHeader("Result Entry")
        }

        // No order selected
        if (selectedOrder == null) {
            item {
                EmptyStateCard(
                    title = "No order selected",
                    detail = "Select an order from the Worklist tab to enter results.",
                )
            }
            return@LazyColumn
        }

        val currentOrder = selectedOrder!!

        // ── Order Summary Card ────────────────────────────────────────────
        item {
            ClinicCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Order Summary", style = MaterialTheme.typography.titleSmall, color = Slate900)
                            Text("Order ID: ${currentOrder.id}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                        }
                        OrderStatusBadge(currentOrder.status)
                    }

                    HorizontalDivider(color = Slate100)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Patient", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(currentOrder.patientId, style = MaterialTheme.typography.bodyMedium, color = Slate900)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Priority", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            PriorityBadge(currentOrder.priority)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Tests", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Text(
                                currentOrder.items.joinToString(", ") { it.testName },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate900,
                            )
                        }
                    }
                }
            }
        }

        // ── Result Entry Form ─────────────────────────────────────────────
        item {
            ClinicCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter Results", style = MaterialTheme.typography.titleSmall, color = Slate900)

                    LabFormField(
                        value = value,
                        onValueChange = { value = it },
                        label = "Result Value",
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabFormField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = "Unit",
                            modifier = Modifier.weight(1f),
                        )
                        LabFormField(
                            value = reference,
                            onValueChange = { reference = it },
                            label = "Reference Range",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    LabFormField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = "Comments (optional)",
                    )

                    // Result flag preview
                    AnimatedVisibility(
                        visible = value.isNotBlank() && reference.isNotBlank(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        ResultFlagPreview(value, reference)
                    }
                }
            }
        }

        // ── Status feedback ───────────────────────────────────────────────
        when (val s = status) {
            is ResultEntryStatus.Success -> {
                item {
                    ClinicCard(Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusStable, modifier = Modifier.size(20.dp))
                            Text(s.message, style = MaterialTheme.typography.bodySmall, color = StatusStable)
                        }
                    }
                }
            }
            is ResultEntryStatus.Error -> {
                item {
                    ClinicCard(Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusDot(StatusCritical, 8)
                            Text(s.message, style = MaterialTheme.typography.bodySmall, color = StatusCritical)
                        }
                    }
                }
            }
            else -> {}
        }

        // ── Action Buttons ────────────────────────────────────────────────
        item {
            FormActionRow(
                cancelLabel = "Clear",
                onCancel = {
                    value = ""
                    unit = ""
                    reference = ""
                    comment = ""
                    status = ResultEntryStatus.Idle
                },
                primaryLabel = if (status is ResultEntryStatus.Saving) "Saving…" else "Save Result",
                onPrimary = {
                    val current = selectedOrder ?: return@FormActionRow
                    val item = current.items.firstOrNull() ?: return@FormActionRow
                    if (value.isBlank()) {
                        status = ResultEntryStatus.Error("Result value is required.")
                        return@FormActionRow
                    }
                    status = ResultEntryStatus.Saving
                    scope.launch {
                        try {
                            localRepository.saveLabResults(
                                orderId = current.id,
                                results = listOf(
                                    LabResult(
                                        id = "LRES-${Clock.System.now().toEpochMilliseconds()}",
                                        orderId = current.id,
                                        orderItemId = item.id,
                                        patientId = current.patientId,
                                        testId = item.testId,
                                        testCode = item.testCode,
                                        testName = item.testName,
                                        value = value,
                                        unit = unit.ifBlank { null },
                                        referenceRange = reference.ifBlank { null },
                                        comment = comment.ifBlank { null },
                                        enteredBy = session.staffId,
                                        enteredAt = Clock.System.now().toString(),
                                        createdAt = Clock.System.now().toString(),
                                        updatedAt = Clock.System.now().toString(),
                                    )
                                ),
                                actorId = session.staffId,
                            )
                            localRepository.updateLabOrderStatus(current.id, LabOrderStatus.IN_PROCESS, session.staffId)
                            localRepository.updateLabOrderStatus(current.id, LabOrderStatus.VERIFIED, session.staffId)
                            localRepository.updateLabOrderStatus(current.id, LabOrderStatus.REPORTED, session.staffId)
                            status = ResultEntryStatus.Success("Results saved and order progressed to REPORTED.")
                        } catch (e: Exception) {
                            status = ResultEntryStatus.Error("Save failed: ${e.message ?: "Unknown error"}")
                        }
                    }
                },
                primaryEnabled = session.hasPermission(Permission.LAB_RESULT_MANAGE) && status !is ResultEntryStatus.Saving,
            )
        }
    }
}

// ── Patient Orders Tab ────────────────────────────────────────────────────────

@Composable
private fun LabOrderDetailsScreen(
    localRepository: LocalRepository,
    patientId: String?,
) {
    var orders by remember { mutableStateOf<List<LabOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(patientId) {
        isLoading = true
        orders = if (patientId.isNullOrBlank()) emptyList()
        else try { localRepository.getLabOrdersForPatient(patientId) } catch (_: Exception) { emptyList() }
        isLoading = false
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            SectionHeader(
                if (patientId != null) "Orders for $patientId" else "Patient Lab Orders",
            )
        }

        if (patientId.isNullOrBlank()) {
            item {
                EmptyStateCard(
                    title = "No patient selected",
                    detail = "Select an order from the Worklist tab to view patient lab history.",
                )
            }
            return@LazyColumn
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Navy800, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            }
            return@LazyColumn
        }

        if (orders.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No lab orders",
                    detail = "No lab orders found for patient $patientId.",
                )
            }
        } else {
            items(orders) { order ->
                LabOrderDetailCard(order)
            }
        }
    }
}

@Composable
private fun LabOrderDetailCard(order: LabOrder) {
    ClinicCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(order.id, style = MaterialTheme.typography.titleSmall, color = Slate900)
                    Text("Patient: ${order.patientId}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
                OrderStatusBadge(order.status)
            }

            HorizontalDivider(color = Slate100)

            // ── Status Timeline ───────────────────────────────────────────
            OrderStatusTimeline(order)

            // ── Verification/Report info ──────────────────────────────────
            if (order.verifiedBy != null || order.reportedBy != null) {
                HorizontalDivider(color = Slate100)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    if (order.verifiedBy != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Verified", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusStable, modifier = Modifier.size(14.dp))
                                Text(order.verifiedBy ?: "–", style = MaterialTheme.typography.bodySmall, color = Slate700)
                            }
                        }
                    }
                    if (order.reportedBy != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Reported", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Indigo700, modifier = Modifier.size(14.dp))
                                Text(order.reportedBy ?: "–", style = MaterialTheme.typography.bodySmall, color = Slate700)
                            }
                        }
                    }
                }
            }

            // ── Test Items ────────────────────────────────────────────────
            if (order.items.isNotEmpty()) {
                HorizontalDivider(color = Slate100)
                Text("Test Items", style = MaterialTheme.typography.labelMedium, color = Slate500)
                order.items.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusDot(Navy200, 5)
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(item.testName, style = MaterialTheme.typography.bodySmall, color = Slate900)
                                Text(item.billingCode, style = MaterialTheme.typography.bodySmall, color = Slate400, fontSize = 10.sp)
                            }
                        }
                        Text(
                            "KES ${item.price.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700,
                        )
                    }
                }
            }
        }
    }
}

// ── Status Timeline ───────────────────────────────────────────────────────────

@Composable
private fun OrderStatusTimeline(order: LabOrder) {
    val steps = listOf(
        "Ordered" to LabOrderStatus.ORDERED,
        "Sample" to LabOrderStatus.SAMPLE_COLLECTED,
        "In Process" to LabOrderStatus.IN_PROCESS,
        "Verified" to LabOrderStatus.VERIFIED,
        "Reported" to LabOrderStatus.REPORTED,
    )

    val currentIndex = steps.indexOfFirst { it.second == order.status }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, (label, _) ->
            val isCompleted = index <= currentIndex
            val isCurrent = index == currentIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Dot
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> Indigo700
                                isCompleted -> StatusStable
                                else -> Slate200
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCompleted && !isCurrent) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color.White,
                        )
                    }
                }

                // Label
                Text(
                    label,
                    fontSize = 9.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCompleted) Slate900 else Slate400,
                )
            }
        }
    }
}

// ── Shared UI Components ──────────────────────────────────────────────────────

@Composable
private fun PriorityBadge(priority: LabPriority) {
    val (fg, bg) = when (priority) {
        LabPriority.STAT -> Color.White to StatusCritical
        LabPriority.URGENT -> StatusWarning to Color(0xFFFEF9C3)
        LabPriority.ROUTINE -> StatusInfo to Color(0xFFDBEAFE)
    }
    TextBadge(
        text = priority.name,
        fg = fg,
        bg = bg,
    )
}

@Composable
private fun OrderStatusBadge(status: LabOrderStatus) {
    val (fg, bg) = when (status) {
        LabOrderStatus.ORDERED -> Indigo700 to Indigo100
        LabOrderStatus.SAMPLE_COLLECTED -> Amber700 to Amber100
        LabOrderStatus.IN_PROCESS -> Sky700 to Sky100
        LabOrderStatus.VERIFIED -> Teal700 to Teal100
        LabOrderStatus.REPORTED -> StatusStable to Color(0xFFD1FAE5)
    }
    TextBadge(
        text = status.name.replace("_", " "),
        fg = fg,
        bg = bg,
    )
}

@Composable
private fun LabFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Indigo700,
            unfocusedBorderColor = Slate200,
            focusedContainerColor = White,
            unfocusedContainerColor = White,
        ),
    )
}

@Composable
private fun ResultFlagPreview(resultValue: String, referenceRange: String) {
    // Simple parsing: try to detect HIGH / LOW / NORMAL
    val numResult = resultValue.toDoubleOrNull()
    val parts = referenceRange.split("-", "–").mapNotNull { it.trim().toDoubleOrNull() }
    val low = parts.getOrNull(0)
    val high = parts.getOrNull(1)

    if (numResult != null && low != null && high != null) {
        val (flag, color) = when {
            numResult < low -> "▼ LOW" to StatusWarning
            numResult > high -> "▲ HIGH" to StatusCritical
            else -> "● NORMAL" to StatusStable
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.1f))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color, 8)
            Text(
                flag,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                "Result: $resultValue  ·  Range: $referenceRange",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600,
            )
        }
    }
}

// ── Status sealed class ───────────────────────────────────────────────────────

private sealed class ResultEntryStatus {
    object Idle : ResultEntryStatus()
    object Saving : ResultEntryStatus()
    data class Success(val message: String) : ResultEntryStatus()
    data class Error(val message: String) : ResultEntryStatus()
}
