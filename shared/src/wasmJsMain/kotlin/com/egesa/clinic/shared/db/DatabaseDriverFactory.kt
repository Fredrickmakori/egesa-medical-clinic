package com.egesa.clinic.shared.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker
import kotlinx.browser.window

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // Initialize WebWorkerDriver for WASM/Web platform
        // This uses a background Web Worker to run SQLite off the main thread
        // The worker is referenced by relative path 'sql-worker.js'
        return WebWorkerDriver(
            worker = Worker("sql-worker.js")
        )
    }
}
