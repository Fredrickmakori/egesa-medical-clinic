package com.egesa.clinic.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import kotlinx.serialization.Serializable
import java.sql.DriverManager
import java.time.YearMonth

private val supportedReports = setOf(
    "moh204_monthly_opd",
    "moh405_monthly_anc",
    "moh333_monthly_maternity",
    "moh361b_monthly_ccc",
    "moh272_273_monthly_ncd"
)

@Serializable
data class ReportRow(
    val reportMonth: String,
    val facility: String,
    val department: String,
    val program: String,
    val totalEncounters: Int,
    val completedEncounters: Int,
    val missingRequiredFields: Int,
    val registerTotal: Int,
    val reconciliationDiff: Int,
    val flagged: Boolean
)

@Serializable
data class ReportResponse(
    val report: String,
    val fromMonth: String,
    val toMonth: String,
    val format: String,
    val count: Int,
    val rows: List<ReportRow>
)

object ReportingService {
    private fun dbUrl(): String = System.getenv("DATABASE_URL") ?: ""

    fun queryReport(
        report: String,
        fromMonth: String,
        toMonth: String,
        department: String?,
        program: String?
    ): List<ReportRow> {
        require(report in supportedReports) { "Unsupported report '$report'" }
        val url = dbUrl()
        if (url.isBlank()) return emptyList()

        val sql = """
            select report_month,
                   facility,
                   department,
                   program,
                   total_encounters,
                   completed_encounters,
                   missing_required_fields,
                   register_total,
                   (register_total - total_encounters) as reconciliation_diff,
                   (missing_required_fields > 0 OR register_total <> total_encounters) as flagged
              from public.$report
             where report_month >= ? and report_month <= ?
               and (? is null or department = ?)
               and (? is null or program = ?)
             order by report_month, facility
        """.trimIndent()

        DriverManager.getConnection(url).use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, fromMonth)
                ps.setString(2, toMonth)
                ps.setString(3, department)
                ps.setString(4, department)
                ps.setString(5, program)
                ps.setString(6, program)
                ps.executeQuery().use { rs ->
                    val rows = mutableListOf<ReportRow>()
                    while (rs.next()) {
                        rows += ReportRow(
                            reportMonth = rs.getString("report_month"),
                            facility = rs.getString("facility"),
                            department = rs.getString("department"),
                            program = rs.getString("program"),
                            totalEncounters = rs.getInt("total_encounters"),
                            completedEncounters = rs.getInt("completed_encounters"),
                            missingRequiredFields = rs.getInt("missing_required_fields"),
                            registerTotal = rs.getInt("register_total"),
                            reconciliationDiff = rs.getInt("reconciliation_diff"),
                            flagged = rs.getBoolean("flagged")
                        )
                    }
                    return rows
                }
            }
        }
    }
}

suspend fun ApplicationCall.respondReport(report: String) {
    val fromMonth = request.queryParameters["fromMonth"]
    val toMonth = request.queryParameters["toMonth"]
    val department = request.queryParameters["department"]
    val program = request.queryParameters["program"]
    val format = request.queryParameters["format"]?.lowercase() ?: "json"

    if (fromMonth == null || toMonth == null) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "fromMonth and toMonth are required (YYYY-MM)"))
        return
    }
    if (!isYearMonth(fromMonth) || !isYearMonth(toMonth)) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid month format, expected YYYY-MM"))
        return
    }

    val rows = runCatching {
        ReportingService.queryReport(report, fromMonth, toMonth, department, program)
    }.getOrElse {
        respond(HttpStatusCode.InternalServerError, mapOf("error" to (it.message ?: "Report query failed")))
        return
    }

    if (format == "csv") {
        respondText(
            text = rows.toCsv(),
            contentType = ContentType.Text.CSV,
            status = HttpStatusCode.OK
        )
        return
    }

    respond(
        ReportResponse(
            report = report,
            fromMonth = fromMonth,
            toMonth = toMonth,
            format = "json",
            count = rows.size,
            rows = rows
        )
    )
}

private fun isYearMonth(value: String): Boolean = runCatching { YearMonth.parse(value) }.isSuccess

private fun List<ReportRow>.toCsv(): String {
    val header = "reportMonth,facility,department,program,totalEncounters,completedEncounters,missingRequiredFields,registerTotal,reconciliationDiff,flagged"
    val body = joinToString("\n") { row ->
        listOf(
            row.reportMonth,
            row.facility,
            row.department,
            row.program,
            row.totalEncounters,
            row.completedEncounters,
            row.missingRequiredFields,
            row.registerTotal,
            row.reconciliationDiff,
            row.flagged
        ).joinToString(",") { it.toString().replace(",", " ") }
    }
    return if (body.isBlank()) header else "$header\n$body"
}
