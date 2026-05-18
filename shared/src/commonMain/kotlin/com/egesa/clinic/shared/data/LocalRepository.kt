package com.egesa.clinic.shared.data

import com.egesa.clinic.shared.StaffMember
import com.egesa.clinic.shared.UserRole
import com.egesa.clinic.shared.db.ClinicDatabase
import com.egesa.clinic.shared.db.DatabaseDriverFactory
import kotlinx.datetime.Clock

class LocalRepository(databaseDriverFactory: DatabaseDriverFactory) {
    
    private val database = ClinicDatabase(
        driver = databaseDriverFactory.createDriver()
    )
    
    private val dbQueries = database.clinicDatabaseQueries

    fun getAllStaff(): List<StaffMember> {
        return dbQueries.selectAllStaff().executeAsList().map {
            StaffMember(
                id = it.id,
                fullName = it.fullName,
                role = UserRole.valueOf(it.role),
                department = it.department ?: ""
            )
        }
    }

    fun insertStaff(staff: StaffMember, pin: String? = null) {
        dbQueries.insertStaff(
            id = staff.id,
            fullName = staff.fullName,
            role = staff.role.name,
            department = staff.department,
            pin = pin,
            active =1,
            lastUpdated = Clock.System.now().toString()
        )
    }

    fun seedAdminIfEmpty() {
        if (getAllStaff().none { it.role == UserRole.ADMIN }) {
            insertStaff(
                StaffMember(
                    id = "ADMIN-001",
                    fullName = "System Admin",
                    role = UserRole.ADMIN,
                    department = "Administration"
                ),
                pin = "1234"
            )
        }
    }
}
