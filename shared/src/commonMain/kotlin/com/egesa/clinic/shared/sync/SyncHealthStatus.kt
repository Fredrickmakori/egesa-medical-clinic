package com.egesa.clinic.shared.sync

import kotlinx.serialization.Serializable

@Serializable
data class SyncHealthStatus(
    val status: String,
    val pendingCount: Int,
    val lastSyncTime: Long
)
