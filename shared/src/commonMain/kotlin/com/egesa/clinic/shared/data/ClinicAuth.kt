package com.egesa.clinic.shared.data

/** Shared bearer token used by HttpClient and sync uploads after login. */
object ClinicAuth {
    var accessToken: String? = null
        private set

    fun setAccessToken(token: String?) {
        accessToken = token?.takeIf { it.isNotBlank() }
    }

    fun clearAccessToken() {
        accessToken = null
    }

    fun hasToken(): Boolean = !accessToken.isNullOrBlank()
}
