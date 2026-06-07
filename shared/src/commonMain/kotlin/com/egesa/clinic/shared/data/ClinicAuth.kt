package com.egesa.clinic.shared.data

/** Shared bearer token used by HttpClient and sync uploads after login. */
object ClinicAuth {
    var accessToken: String? = null
        private set
    var facilityId: String = "default"
        private set
    var tenantCode: String? = null
        private set

    fun setSession(token: String?, facilityId: String = "default", tenantCode: String? = null) {
        accessToken = token?.takeIf { it.isNotBlank() }
        this.facilityId = facilityId.ifBlank { "default" }
        this.tenantCode = tenantCode?.trim()?.takeIf { it.isNotBlank() }
    }

    fun setAccessToken(token: String?) {
        setSession(token = token, facilityId = facilityId, tenantCode = tenantCode)
    }

    fun clearAccessToken() {
        accessToken = null
        facilityId = "default"
        tenantCode = null
    }

    fun hasToken(): Boolean = !accessToken.isNullOrBlank()
}
