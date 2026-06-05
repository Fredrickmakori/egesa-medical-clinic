package com.egesa.clinic.shared.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.WorkflowArea
import com.egesa.clinic.shared.ui.components.Avatar
import com.egesa.clinic.shared.ui.components.SyncStatusIndicator
import com.egesa.clinic.shared.ui.navigation.SessionState
import com.egesa.clinic.shared.ui.navigation.areaWelcomeMessage
import com.egesa.clinic.shared.ui.theme.Indigo100
import com.egesa.clinic.shared.ui.theme.Indigo700
import com.egesa.clinic.shared.ui.theme.Slate500
import com.egesa.clinic.shared.ui.theme.Slate900
import com.egesa.clinic.shared.ui.theme.White

@Composable
internal fun MobileTopBar(
    session: SessionState,
    activeArea: WorkflowArea,
    onLogout: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(White).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(areaIcon(activeArea), contentDescription = null, tint = Indigo700, modifier = Modifier.size(22.dp))
            Column {
                Text(activeArea.displayName(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                Text(
                    areaWelcomeMessage(
                        session.role,
                        activeArea,
                        session.fullName.substringBefore(' ').ifBlank { session.fullName },
                    ),
                    fontSize = 10.sp,
                    color = Slate500,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SyncStatusIndicator(session.syncStatus, showLabel = false)
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                    .background(Indigo100).clickable(onClick = onLogout),
                contentAlignment = Alignment.Center,
            ) {
                Avatar(initials = session.initials, size = 28, bg = Indigo700)
            }
        }
    }
}

@Composable
internal fun MobileSearchBar(session: SessionState) {
    var search by remember { mutableStateOf("") }
    MobileGlobalSearchBox(
        value = search,
        onValueChange = { search = it },
        session = session,
        modifier = Modifier.fillMaxWidth().background(White).padding(horizontal = 14.dp, vertical = 8.dp),
        placeholder = "Search patient, visit, invoice",
    )
}
