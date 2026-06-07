package com.egesa.clinic.shared.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.WorkflowArea
import com.egesa.clinic.shared.ui.components.Avatar
import com.egesa.clinic.shared.ui.navigation.ClinicNavItem
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.theme.Navy50
import com.egesa.clinic.shared.ui.theme.Navy800
import com.egesa.clinic.shared.ui.theme.Slate100
import com.egesa.clinic.shared.ui.theme.Slate200
import com.egesa.clinic.shared.ui.theme.Slate400
import com.egesa.clinic.shared.ui.theme.Slate500
import com.egesa.clinic.shared.ui.theme.Slate700
import com.egesa.clinic.shared.ui.theme.Slate900
import com.egesa.clinic.shared.ui.theme.StatusCritical
import com.egesa.clinic.shared.ui.theme.Teal100
import com.egesa.clinic.shared.ui.theme.Teal600
import com.egesa.clinic.shared.ui.theme.Teal700
import com.egesa.clinic.shared.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileModuleOverflowSheet(
    navItems: List<ClinicNavItem>,
    overflowItems: List<ClinicNavItem>,
    activeArea: WorkflowArea,
    session: SessionState,
    onDismiss: () -> Unit,
    onSelectArea: (WorkflowArea) -> Unit,
    onLogout: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(Modifier.padding(16.dp).navigationBarsPadding()) {
            Text(
                "All Modules",
                style = MaterialTheme.typography.titleMedium,
                color = Slate900,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (navItems.isEmpty()) {
                Text(
                    "No modules are assigned to your role.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                overflowItems.forEach { item ->
                    MobileOverflowNavItem(
                        item = item,
                        selected = activeArea == item.area,
                        onClick = {
                            onSelectArea(item.area)
                            onDismiss()
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Slate200)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(Slate100).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Avatar(initials = session.initials, size = 36)
                    Column {
                        Text(session.fullName, style = MaterialTheme.typography.titleSmall)
                        Text(session.shiftLabel, style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                }
                TextButton(onClick = { onDismiss(); onLogout() }) {
                    Text("Sign out", color = StatusCritical)
                }
            }
        }
    }
}

@Composable
private fun MobileOverflowNavItem(
    item: ClinicNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) Navy50 else Color.Transparent
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(if (selected) Teal100 else Slate100),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.shortLabel.take(2),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Teal700 else Slate500,
                letterSpacing = 0.5.sp,
            )
        }
        Text(
            item.label,
            modifier = Modifier.weight(1f).clickable(onClick = onClick),
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Navy800 else Slate700,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (selected) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Teal600))
        }
    }
}
