package com.egesa.clinic.shared.sync

import com.egesa.clinic.shared.data.LocalRepository
import kotlinx.coroutines.delay

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

data class SyncHealth(
    val status: SyncStatus = SyncStatus.IDLE,
    val pendingItems: Int = 0,
    val failedItems: Int = 0,
    val lastError: String? = null,
    val lastSyncedAt: String? = null
)

/**
 * Lightweight cross-platform sync coordinator.
 * Local-first saves are queued in SyncQueueEntity then uploaded when online.
 */
class SyncManager(
    private val localRepository: LocalRepository
) {
    suspend fun runAutoSync(
        isOnline: Boolean,
        upload: suspend (entityType: String, entityId: String, payload: String) -> Boolean
    ): SyncHealth {
        if (!isOnline) return SyncHealth(status = SyncStatus.OFFLINE)

        var failed = 0
        val pending = localRepository.getPendingSync()
        if (pending.isEmpty()) return SyncHealth(status = SyncStatus.SUCCESS, pendingItems = 0)

        pending.forEach { row ->
            val ok = upload(row.entityType, row.entityId, row.payload)
            if (ok) {
                localRepository.deleteSyncItem(row.id)
            } else {
                failed++
                delay(250)
            }
        }

        return if (failed == 0) {
            SyncHealth(status = SyncStatus.SUCCESS, pendingItems = 0)
        } else {
            SyncHealth(
                status = SyncStatus.ERROR,
                pendingItems = pending.size - (pending.size - failed),
                failedItems = failed,
                lastError = "Some records failed to sync"
            )
        }
    }
}
