package com.egesa.clinic.shared.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Signals the app shell to drain the local sync queue immediately. */
object SyncNotifier {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val requests: SharedFlow<Unit> = _requests.asSharedFlow()

    fun requestSync() {
        _requests.tryEmit(Unit)
    }
}
