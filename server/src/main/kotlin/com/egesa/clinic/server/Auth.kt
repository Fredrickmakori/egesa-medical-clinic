package com.egesa.clinic.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.egesa.clinic.shared.Permission
import com.egesa.clinic.shared.RolePermissionMap
import com.egesa.clinic.shared.UserRole
import io.ktor.server.auth.jwt.JWTPrincipal
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.hours

@Serializable
data class LoginRequest(
    val staffId: String,
    val pin: String,
    val tenantCode: String? = null
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val expiresAtEpochSeconds: Long,
    val role: String,
    val staffName: String,
    val facilityId: String,
    val tenantCode: String? = null
)

data class AuthUser(
    val id: String,
    val name: String,
    val role: UserRole,
    val pin: String,
    val facilityId: String = "default",
    val tenantCode: String? = null
)

object AuthStore {
    private val users = listOf(
        AuthUser("AD-001", "Admin User", UserRole.ADMIN, "8357", "default"),
        AuthUser("DR-001", "Dr. James Kamau", UserRole.DOCTOR, "2211", "default"),
        AuthUser("NR-001", "Nurse Faith Wanjiku", UserRole.NURSE, "1144", "default"),
        AuthUser("PH-001", "Pharmacist Brian Otieno", UserRole.PHARMACIST, "6677", "default"),
        AuthUser("RC-001", "Mary Otieno", UserRole.RECEPTIONIST, "9911", "default")
    ).associateBy { it.id }

    fun verify(staffId: String, pin: String): AuthUser? = users[staffId]?.takeIf { it.pin == pin }

    fun staffDirectory(): List<AuthUser> = users.values.sortedBy { it.name }
}

object JwtConfig {
    private val issuer = "egesa-medical-clinic"
    private val audience = "egesa-medical-users"
    private val secret = System.getenv("JWT_SECRET") ?: "dev-only-change-in-production"
    private val algorithm = Algorithm.HMAC256(secret)

    fun createToken(user: AuthUser): LoginResponse {
        val now = Clock.System.now()
        val exp = now.plus(8.hours)
        val token = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(user.id)
            .withClaim("role", user.role.name)
            .withClaim("name", user.name)
            .withClaim("facility_id", user.facilityId)
            .withClaim("tenant_code", user.tenantCode)
            .withIssuedAt(java.util.Date(now.toEpochMilliseconds()))
            .withExpiresAt(java.util.Date(exp.toEpochMilliseconds()))
            .sign(algorithm)
        return LoginResponse(
            accessToken = token,
            expiresAtEpochSeconds = exp.epochSeconds,
            role = user.role.name,
            staffName = user.name,
            facilityId = user.facilityId,
            tenantCode = user.tenantCode
        )
    }

    fun verifier() = JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build()
}

fun JWTPrincipal.role(): String? = payload.getClaim("role")?.asString()
fun JWTPrincipal.userId(): String? = payload.subject
fun JWTPrincipal.name(): String? = payload.getClaim("name")?.asString()
fun JWTPrincipal.facilityId(): String = payload.getClaim("facility_id")?.asString() ?: "default"
fun JWTPrincipal.tenantCode(): String? = payload.getClaim("tenant_code")?.asString()

fun Instant.toEpochSeconds(): Long = epochSeconds

// ── Permission checking helpers ────────────────────────────────────────────

fun requirePermission(principal: JWTPrincipal?, permission: Permission): Boolean {
    val roleStr = principal?.role() ?: return false
    val userRole = try {
        UserRole.valueOf(roleStr)
    } catch (e: Exception) {
        return false
    }
    return RolePermissionMap.hasPermission(userRole, permission)
}

fun requireRole(principal: JWTPrincipal?, requiredRole: UserRole): Boolean {
    val roleStr = principal?.role() ?: return false
    return roleStr == requiredRole.name
}

fun hasAllPermissions(principal: JWTPrincipal?, vararg permissions: Permission): Boolean {
    return permissions.all { requirePermission(principal, it) }
}

fun hasAnyPermission(principal: JWTPrincipal?, vararg permissions: Permission): Boolean {
    return permissions.any { requirePermission(principal, it) }
}
