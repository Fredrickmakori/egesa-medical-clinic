package com.egesa.clinic.shared.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egesa.clinic.shared.WorkflowArea
import com.egesa.clinic.shared.ui.navigation.ClinicNavItem
import com.egesa.clinic.shared.ui.theme.Navy400
import com.egesa.clinic.shared.ui.theme.Teal200
import com.egesa.clinic.shared.ui.theme.Teal700
import com.egesa.clinic.shared.ui.theme.White

@Composable
internal fun MobileBottomNavigationBar(
    items: List<ClinicNavItem>,
    activeArea: WorkflowArea,
    onAreaSelected: (WorkflowArea) -> Unit,
    showMoreItem: Boolean = false,
    onMoreClick: (() -> Unit)? = null,
) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        containerColor = White,
        tonalElevation = 1.dp,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Box(
                        Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                            .background(
                                if (activeArea == item.area) Teal200 else Color.Transparent,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            areaIcon(item.area),
                            contentDescription = item.label,
                            tint = if (activeArea == item.area) Teal700 else Navy400,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
                label = {
                    Text(
                        item.shortLabel,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selected = activeArea == item.area,
                onClick = { onAreaSelected(item.area) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Teal700,
                    selectedTextColor = Teal700,
                    unselectedIconColor = Navy400,
                    unselectedTextColor = Navy400,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
        if (showMoreItem && onMoreClick != null) {
            NavigationBarItem(
                icon = {
                    Box(
                        Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⋯", fontSize = 14.sp, color = Navy400)
                    }
                },
                label = {
                    Text("More", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                selected = false,
                onClick = onMoreClick,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Teal700,
                    selectedTextColor = Teal700,
                    unselectedIconColor = Navy400,
                    unselectedTextColor = Navy400,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
