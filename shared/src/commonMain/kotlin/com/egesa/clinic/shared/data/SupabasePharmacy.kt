package com.egesa.clinic.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Supabase configuration for connecting to the backend database.
 * Contains connection details and API credentials.
 */
@Serializable
data class SupabaseConfig(
    val url: String = "https://vigeqwzqasblsnetbprm.supabase.co",
    val anonKey: String = "",  // Should be set from environment
    val serviceRoleKey: String = "",  // Should be set from environment
    val projectId: String = "vigeqwzqasblsnetbprm"
)

/**
 * Supabase client for database queries via REST API.
 * Handles authentication and common operations.
 */
class SupabaseClient(
    @PublishedApi internal val config: SupabaseConfig,
    @PublishedApi internal val httpClient: HttpClient
) {
    @PublishedApi internal val json = Json { ignoreUnknownKeys = true }

    /**
     * Execute a SELECT query against Supabase REST API
     */
    suspend inline fun <reified T> select(
        table: String,
        filter: String? = null,
        select: String = "*"
    ): List<T> {
        try {
            val url = "${config.url}/rest/v1/$table?select=$select" +
                (filter?.let { "&$it" } ?: "")

            val response = httpClient.get(url) {
                header("apikey", config.anonKey)
                header("Authorization", "Bearer ${config.anonKey}")
            }

            return response.body()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Insert a record into Supabase
     */
    suspend inline fun <reified T> insert(table: String, data: T): Boolean {
        return try {
            val url = "${config.url}/rest/v1/$table"
            val response = httpClient.post(url) {
                header("apikey", config.anonKey)
                header("Authorization", "Bearer ${config.anonKey}")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(data))
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Update a record in Supabase
     */
    suspend inline fun <reified T> update(
        table: String,
        id: String,
        data: T
    ): Boolean {
        return try {
            val url = "${config.url}/rest/v1/$table?id=eq.$id"
            val response = httpClient.patch(url) {
                header("apikey", config.anonKey)
                header("Authorization", "Bearer ${config.anonKey}")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(data))
            }
            response.status.value in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

// Data models for pharmacy and inventory

@Serializable
data class Medication(
    val id: String,
    val name: String,
    val generic_name: String? = null,
    val strength: String? = null,
    val form: String? = null,  // tablet, capsule, injection, etc.
    val unit_price: Double? = null,
    val reorder_level: Int? = null,
    val supplier_id: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class InventoryItem(
    val id: String,
    val medication_id: String,
    val quantity_in_stock: Int,
    val quantity_available: Int,
    val batch_number: String? = null,
    val expiry_date: String? = null,
    val location_in_pharmacy: String? = null,
    val cost_per_unit: Double? = null,
    val date_received: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class MedicalSupply(
    val id: String,
    val name: String,
    val category: String,  // e.g., "bandages", "gloves", "syringes"
    val unit_of_measurement: String,  // e.g., "box", "packet", "unit"
    val quantity_in_stock: Int,
    val reorder_level: Int,
    val unit_cost: Double? = null,
    val supplier_id: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class PharmacyTransaction(
    val id: String,
    val transaction_type: String,  // "DISPENSING", "RECEIPT", "LOSS", "ADJUSTMENT"
    val medication_id: String? = null,
    val quantity: Int,
    val reference_id: String? = null,  // Encounter ID, purchase order ID, etc.
    val notes: String? = null,
    val created_by: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class MedicationLookup(
    val id: String,
    val medication_id: String,
    val medication_name: String,
    val generic_name: String? = null,
    val quantity_available: Int,
    val form: String? = null,
    val strength: String? = null
)

// Supabase Repository Interface

interface SupabasePharmacyRepository {
    suspend fun getMedications(): List<Medication>
    suspend fun getMedicationById(id: String): Medication?
    suspend fun getInventoryByMedicationId(medicationId: String): InventoryItem?
    suspend fun getMedicalSupplies(): List<MedicalSupply>
    suspend fun getMedicalSupplyById(id: String): MedicalSupply?
    suspend fun getMedicationLookupList(): List<MedicationLookup>
    suspend fun recordTransaction(transaction: PharmacyTransaction): Boolean
    suspend fun searchMedications(query: String): List<Medication>
    suspend fun getLowStockMedications(): List<InventoryItem>
}

/**
 * Implementation of pharmacy repository using Supabase REST API
 */
class SupabasePharmacyRepositoryImpl(
    private val supabaseClient: SupabaseClient
) : SupabasePharmacyRepository {

    override suspend fun getMedications(): List<Medication> {
        return supabaseClient.select("medications")
    }

    override suspend fun getMedicationById(id: String): Medication? {
        val results: List<Medication> = supabaseClient.select("medications", "id=eq.$id")
        return results.firstOrNull()
    }

    override suspend fun getInventoryByMedicationId(medicationId: String): InventoryItem? {
        val results: List<InventoryItem> = supabaseClient.select(
            "inventory",
            "medication_id=eq.$medicationId"
        )
        return results.firstOrNull()
    }

    override suspend fun getMedicalSupplies(): List<MedicalSupply> {
        return supabaseClient.select("medical_supplies")
    }

    override suspend fun getMedicalSupplyById(id: String): MedicalSupply? {
        val results: List<MedicalSupply> = supabaseClient.select("medical_supplies", "id=eq.$id")
        return results.firstOrNull()
    }

    override suspend fun getMedicationLookupList(): List<MedicationLookup> {
        // This uses a view that joins medications with current inventory
        return supabaseClient.select("medication_lookup_view")
    }

    override suspend fun recordTransaction(transaction: PharmacyTransaction): Boolean {
        return supabaseClient.insert("pharmacy_transactions", transaction)
    }

    override suspend fun searchMedications(query: String): List<Medication> {
        // Using text search filter
        val filter = "or=(name.ilike.%$query%,generic_name.ilike.%$query%)"
        return supabaseClient.select("medications", filter)
    }

    override suspend fun getLowStockMedications(): List<InventoryItem> {
        // Using a view that shows items below reorder level
        return supabaseClient.select("low_stock_medications_view")
    }
}

