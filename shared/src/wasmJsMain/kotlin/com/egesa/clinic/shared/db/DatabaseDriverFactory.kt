package com.egesa.clinic.shared.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DatabaseDriverFactory {
    actual suspend fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            worker = Worker("sql-worker.js")
        ).also { ClinicDatabase.Schema.create(it).await() }
    }
}
