# 🏗️ Supabase Integration Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                    EGESA Hospital Management                        │
│                    (Android, Desktop, Web, iOS)                     │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
         ┌──────────▼──────┐  ┌───────▼──────────┐
         │  LocalRepository │  │SupabasePharmacy  │
         │  (Offline Cache) │  │    Repository    │
         └──────────┬──────┘  └────────┬──────────┘
                    │                  │
                    └────────┬─────────┘
                             │
                    ┌────────▼────────┐
                    │  SupabaseClient  │
                    │  (HTTP via Ktor) │
                    └────────┬────────┘
                             │
                    ┌────────▼────────────────┐
                    │   Supabase REST API     │
                    │  https://...supabase.co │
                    └────────┬────────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
    ┌────▼──────┐    ┌──────▼──────┐    ┌──────▼──────┐
    │Medications │    │  Inventory  │    │   Medical   │
    │   Table    │    │   Tables    │    │  Supplies   │
    └────┬──────┘    └──────┬──────┘    └──────┬──────┘
         │                  │                   │
    ┌────▼──────────────────▼───────────────────▼────┐
    │         PostgreSQL Database                     │
    │  (db.vigeqwzqasblsnetbprm.supabase.co:5432)    │
    └─────────────────────────────────────────────────┘
```

---

## Data Flow

### 1. Loading Medications (Read)

```
PharmacyScreen
    │
    ├─ LaunchedEffect loads data
    │
    ├─ Call: supabasePharmacyRepo.getMedicationLookupList()
    │
    ├─ SupabasePharmacyRepositoryImpl
    │   └─ supabaseClient.select("medication_lookup_view")
    │
    ├─ SupabaseClient
    │   └─ HTTP GET /rest/v1/medication_lookup_view
    │      Header: Authorization: Bearer {anonKey}
    │
    ├─ Supabase REST API
    │   └─ Query: SELECT * FROM medication_lookup_view
    │
    ├─ PostgreSQL
    │   └─ join(medications, inventory)
    │
    └─ Return: List<MedicationLookup>
       Display: MedicationInventoryTab()
```

### 2. Recording Transaction (Write)

```
Pharmacist: "Dispense 5 tablets of Paracetamol"
    │
    ├─ PharmacyScreen.onDispense(med)
    │
    ├─ Call: repo.recordTransaction(transaction)
    │   PharmacyTransaction(
    │       transaction_type = "DISPENSING",
    │       medication_id = med.id,
    │       quantity_change = -5,
    │       reference_id = "ENC-123"
    │   )
    │
    ├─ SupabasePharmacyRepositoryImpl
    │   └─ supabaseClient.insert("pharmacy_transactions", transaction)
    │
    ├─ SupabaseClient
    │   └─ HTTP POST /rest/v1/pharmacy_transactions
    │      Body: {transaction data}
    │
    ├─ Supabase REST API
    │   └─ INSERT INTO pharmacy_transactions
    │
    ├─ PostgreSQL (Trigger fires)
    │   ├─ INSERT transaction record
    │   ├─ TRIGGER: update_inventory_quantity()
    │   │   UPDATE inventory SET quantity_in_stock = quantity_in_stock - 5
    │   └─ Next read shows updated stock
    │
    └─ Return: Success
       PharmacyScreen refetches medication list
```

---

## Database Structure

### Tables

```
┌─────────────────────────────────────────────────────────┐
│ medications                                             │
├─────────────────────────────────────────────────────────┤
│ id (PK)                   UUID                          │
│ name                      VARCHAR(255) [UNIQUE]        │
│ generic_name              VARCHAR(255)                 │
│ strength                  VARCHAR(100) [e.g., "500mg"] │
│ form                      VARCHAR(50) [e.g., "tablet"] │
│ category                  VARCHAR(100)                 │
│ reorder_level             INTEGER                      │
│ is_active                 BOOLEAN                      │
│ created_at, updated_at    TIMESTAMPS                   │
└─────────────────────────────────────────────���───────────┘

┌─────────────────────────────────────────────────────────┐
│ inventory                                               │
├─────────────────────────────────────────────────────────┤
│ id (PK)                   UUID                          │
│ medication_id (FK)        UUID → medications.id        │
│ quantity_in_stock         INTEGER                      │
│ quantity_available        INTEGER [calculated]         │
│ quantity_reserved         INTEGER                      │
│ batch_number              VARCHAR(100)                 │
│ expiry_date               DATE                         │
│ location_in_pharmacy      VARCHAR(255)                 │
│ cost_per_unit             DECIMAL(10, 2)              │
│ created_at, updated_at    TIMESTAMPS                   │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ pharmacy_transactions                                   │
├─────────────────────────────────────────────────────────┤
│ id (PK)                   UUID                          │
│ transaction_type          VARCHAR(50) [DISPENSING...]  │
│ medication_id (FK)        UUID → medications.id        │
│ quantity_change           INTEGER [+/-]               │
│ reference_id              VARCHAR(100) [encounter_id]  │
│ created_by (FK)           UUID → staff.id             │
│ notes                     TEXT                         │
│ created_at                TIMESTAMP [audit trail]      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ medical_supplies                                        │
├─────────────────────────────────────────────────────────┤
│ id (PK)                   UUID                          │
│ name                      VARCHAR(255)                 │
│ category                  VARCHAR(100)                 │
│ quantity_in_stock         INTEGER                      │
│ quantity_available        INTEGER                      │
│ reorder_level             INTEGER                      │
│ unit_cost                 DECIMAL(10, 2)              │
│ created_at, updated_at    TIMESTAMPS                   │
└─────────────────────────────────────────────────────────┘
```

### Views (Virtual Tables)

```
medication_lookup_view
├─ Joins medications + inventory
├─ Shows current availability
└─ Used by PharmacyScreen search

low_stock_medications_view
├─ Shows items below reorder_level
├─ Triggers reorder alerts
└─ Used by Low Stock tab

expired_stock_view
├─ Shows expired/expiring items
├─ Calculated based on expiry_date
└─ Used for waste alerts

medical_supplies_lookup_view
├─ Similar to medication_lookup_view
├─ For consumables
└─ Used by Medical Supplies tab
```

---

## API Endpoints (Supabase REST)

### Read Medications
```
GET /rest/v1/medication_lookup_view
?select=*
&quantity_available=gt.0

Headers:
  apikey: {SUPABASE_ANON_KEY}
  Authorization: Bearer {SUPABASE_ANON_KEY}

Response:
[
  {
    "id": "uuid",
    "medication_name": "Paracetamol",
    "strength": "500mg",
    "quantity_available": 150,
    ...
  }
]
```

### Search Medications
```
GET /rest/v1/medications
?or=(name.ilike.%query%,generic_name.ilike.%query%)
&is_active=true

Response:
[
  { "id": "uuid", "name": "Paracetamol", ... }
]
```

### Record Transaction
```
POST /rest/v1/pharmacy_transactions
Content-Type: application/json

Body:
{
  "transaction_type": "DISPENSING",
  "medication_id": "uuid",
  "quantity_change": -5,
  "reference_id": "ENC-123",
  "created_by": "staff-uuid"
}

Response:
{ "id": "uuid", "success": true }
```

### Get Low Stock
```
GET /rest/v1/low_stock_medications_view

Response:
[
  {
    "id": "uuid",
    "name": "Medication Name",
    "quantity_in_stock": 5,
    "reorder_level": 20,
    "units_to_order": 15
  }
]
```

---

## Kotlin Data Models

```kotlin
// From SupabasePharmacy.kt

@Serializable
data class Medication(
    val id: String,
    val name: String,
    val generic_name: String,
    val strength: String,
    val form: String,
    val reorder_level: Int
)

@Serializable
data class InventoryItem(
    val id: String,
    val medication_id: String,
    val quantity_in_stock: Int,
    val quantity_available: Int,
    val expiry_date: String
)

@Serializable
data class MedicationLookup(
    val medication_id: String,
    val medication_name: String,
    val quantity_available: Int,
    val form: String,
    val strength: String
)

@Serializable
data class PharmacyTransaction(
    val transaction_type: String,
    val medication_id: String,
    val quantity_change: Int,
    val reference_id: String,
    val created_by: String
)
```

---

## Row Level Security (RLS)

### Policies

```sql
-- Pharmacists can read and edit medications
CREATE POLICY "Allow pharmacist edit medications"
ON medications FOR ALL
USING (auth.uid()::text IN (
  SELECT staff_id FROM staff_roles 
  WHERE role IN ('PHARMACIST', 'ADMIN')
))

-- Clinicians can read medications but not edit
CREATE POLICY "Allow clinician read medications"
ON medications FOR SELECT
USING (auth.uid()::text IN (
  SELECT staff_id FROM staff_roles 
  WHERE role = 'CLINICIAN'
))

-- All transactions logged with created_by
CREATE POLICY "Allow pharmacy record transactions"
ON pharmacy_transactions FOR INSERT
WITH CHECK (auth.uid()::text IN (
  SELECT staff_id FROM staff_roles 
  WHERE role IN ('PHARMACIST', 'PHARMACY_TECHNICIAN')
))
```

---

## Features Enabled

### 1. Real-Time Inventory

```kotlin
// PharmacyScreen automatically calls:
medications = supabasePharmacyRepo.getMedicationLookupList()

// Shows:
✅ Current stock levels
✅ Available vs reserved quantity
✅ Batch numbers and expiry dates
✅ Location in pharmacy
```

### 2. Search by Name or Generic

```kotlin
// Built-in PostgreSQL full-text search:
repo.searchMedications("paracet")
// Returns all matching medications instantly
```

### 3. Automatic Low Stock Alerts

```kotlin
// Background check:
val lowStock = repo.getLowStockMedications()

// Triggers when quantity <= reorder_level
// Signals: "URGENT: Only 3 Paracetamol left (reorder level: 20)"
```

### 4. Audit Trail

```kotlin
// Every transaction recorded:
PharmacyTransaction(
    transaction_type = "DISPENSING",
    medication_id = "med-uuid",
    quantity_change = -5,
    reference_id = "ENC-12345",
    created_by = "dr-jane-uuid",
    created_at = "2026-06-05T14:30:00Z"
)

// Accessible via:
SELECT * FROM pharmacy_audit_trail_view
```

### 5. Expired Stock Tracking

```kotlin
// Automatic view shows:
expired_stock_view
├─ Medications past expiry
├─ Those expiring within 30 days ("EXPIRING_SOON")
└─ Waste management alerts
```

---

## Connection Security

### Development
```
🔓 Public ANON key (with RLS policies)
- Can read medications, inventory
- Cannot edit or delete
- Limited by staff_roles
```

### Production
```
🔒 Service Role key (for server-side only)
- Full database access
- Used by server for admin functions
- Never shared with client app
```

---

## Error Handling

```kotlin
try {
    val medications = repo.getMedicationLookupList()
} catch (e: Exception) {
    // Network error, timeout, or API error
    // Fallback to local cache via LocalRepository
    val Cache = localRepository.getAllMedications()
}
```

---

## Future Enhancements

- [ ] Real-time subscriptions (WebSocket)
- [ ] Batch import from CSV
- [ ] PDF printing with Supabase data
- [ ] Automated reorder to supplier
- [ ] Stock forecasting
- [ ] Multi-warehouse support
- [ ] Barcode scanning integration
- [ ] Supplier integration

---

**Ready to connect your hospital to real data! 🚀**

