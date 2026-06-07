# 🏥 Supabase Integration Setup Guide

## Overview
This guide walks you through connecting your EGESA hospital management system to Supabase for real pharmacy data, inventory management, and medical supplies tracking.

---

## ⚠️ CRITICAL SECURITY NOTICE

**YOUR DATABASE PASSWORD HAS BEEN EXPOSED IN CHAT**

**IMMEDIATE ACTIONS REQUIRED:**
1. ✅ Change your Supabase database password immediately
2. ✅ Regenerate all API keys
3. ✅ Rotate service role keys
4. ✅ Review audit logs for unauthorized access
5. ✅ Never share passwords or credentials in plain text again

**New Connection String (after password change):**
```
postgresql://postgres:[NEW_PASSWORD]@db.vigeqwzqasblsnetbprm.supabase.co:5432/postgres
```

---

## Step 1: Update Android Manifest Permissions

Add these to `androidApp/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## Step 2: Update build.gradle for Supabase Dependencies

Add to `shared/build.gradle.kts` in the `commonMain` dependencies:

```kotlin
dependencies {
    // Existing dependencies...
    
    // Supabase client would go here if using native Supabase library
    // For now, we're using Ktor which requires no additional dependencies
    
    // Make sure you have these for database work:
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
}
```

---

## Step 3: Run Supabase Migrations

### 3.1: Access Supabase SQL Editor

1. Go to [Supabase Dashboard](https://app.supabase.com)
2. Select your project: **vigeqwzqasblsnetbprm**
3. Click **SQL Editor** in the left sidebar
4. Click **New Query**

### 3.2: Run Migrations in Order

⚠️ **RUN THESE IN EXACT ORDER:**

**Migration 1: Schema Foundation**
```sql
-- Copy content from: infra/supabase/schema.sql
-- Paste into SQL Editor and click "Run"
```

**Migration 2-10: Feature Migrations**

Run each of these in order. In SQL Editor, click "New Query" for each:

```
1. infra/supabase/migrations/202605010001_billing_and_payments.sql
2. infra/supabase/migrations/001_add_rbac_and_sync.sql
3. infra/supabase/migrations/002_staff_and_rbac.sql
4. infra/supabase/migrations/202605180001_domain_code_tables_and_constraints.sql
5. infra/supabase/migrations/202605180001_core_clinical_tables.sql
6. infra/supabase/migrations/202605180001_mch_ccc_ncd_extensions.sql
7. infra/supabase/migrations/202605180001_reporting_pipeline.sql
8. infra/supabase/migrations/202605290001_backend_persistence_foundation.sql
9. infra/supabase/migrations/202606040001_current_app_schema_catchup.sql
10. infra/supabase/migrations/202606050001_pharmacy_inventory_system.sql ✨ NEW
```

### 3.3: Verify Migrations Succeeded

After each migration, you should see:
- ✅ "Query executed successfully"
- No error messages
- Check **Database** → **Tables** to verify new tables are created

---

## Step 4: Create Environment Configuration

### 4.1: Create `local.properties` (Already exists, update it)

```properties
# Supabase Configuration
SUPABASE_URL=https://vigeqwzqasblsnetbprm.supabase.co
SUPABASE_ANON_KEY=your_anon_key_here  # Get from Supabase → Settings → API
SUPABASE_SERVICE_ROLE_KEY=your_service_role_key_here

# Database Connection
DATABASE_URL=postgresql://postgres:[NEW_PASSWORD]@db.vigeqwzqasblsnetbprm.supabase.co:5432/postgres
```

### 4.2: Get Supabase Keys

1. Open [Supabase Dashboard](https://app.supabase.com)
2. Go to **Settings** → **API**
3. Copy:
   - **Project URL** (should match above)
   - **anon public key** → Paste as `SUPABASE_ANON_KEY`
   - **service_role secret** → Paste as `SUPABASE_SERVICE_ROLE_KEY`

---

## Step 5: Initialize Supabase Client in Your App

### 5.1: In ClinicApp.kt or MainActivity

```kotlin
import com.egesa.clinic.shared.data.SupabaseConfig
import com.egesa.clinic.shared.data.SupabaseClient
import com.egesa.clinic.shared.data.SupabasePharmacyRepositoryImpl
import io.ktor.client.*

// Initialize Supabase
val supabaseConfig = SupabaseConfig(
    url = BuildConfig.SUPABASE_URL,
    anonKey = BuildConfig.SUPABASE_ANON_KEY
)

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

val supabaseClient = SupabaseClient(supabaseConfig, httpClient)
val pharmacyRepository = SupabasePharmacyRepositoryImpl(supabaseClient)

// Pass to PharmacyScreen
PharmacyScreen(
    localRepository = localRepository,
    session = session,
    supabasePharmacyRepo = pharmacyRepository  // ← Now uses real data!
)
```

---

## Step 6: Test the Connection

### 6.1: Run Unit Test

Create `shared/src/commonTest/kotlin/SupabasePharmacyTest.kt`:

```kotlin
@Test
suspend fun testSupabaseConnection() {
    val config = SupabaseConfig(
        url = "https://vigeqwzqasblsnetbprm.supabase.co",
        anonKey = "YOUR_ANON_KEY"
    )
    val httpClient = HttpClient() { install(JsonFeature) }
    val supabaseClient = SupabaseClient(config, httpClient)
    val repo = SupabasePharmacyRepositoryImpl(supabaseClient)
    
    val medications = repo.getMedications()
    assert(medications.isNotEmpty()) { "Should load medications from Supabase" }
}
```

### 6.2: Run on Android

```bash
./gradlew :androidApp:installDebug
# Open PharmacyScreen - should show real medications from Supabase
```

---

## Step 7: Verify Data in Supabase

### 7.1: Check Tables

In Supabase Dashboard:
1. Click **Table Editor**
2. You should see:
   - ✅ `medications` (with sample Paracetamol, Amoxicillin, etc.)
   - ✅ `inventory` (stock levels)
   - ✅ `medical_supplies` (gloves, gauze, syringes)
   - ✅ `pharmacy_transactions` (audit trail)

### 7.2: Query Data Directly

In SQL Editor:

```sql
-- Check medications
SELECT id, name, strength, form, reorder_level FROM medications;

-- Check inventory
SELECT m.name, i.quantity_in_stock, i.quantity_available 
FROM inventory i 
JOIN medications m ON i.medication_id = m.id;

-- Check low stock
SELECT * FROM low_stock_medications_view;
```

---

## Step 8: Features Now Enabled

✅ **Pharmacy Screen** - Real-time medication lookup from Supabase
✅ **Search** - Full-text search for medications
✅ **Inventory Tracking** - Live stock levels
✅ **Low Stock Alerts** - Automatic when below reorder level
✅ **Medical Supplies** - Non-medication items tracking
✅ **Audit Trail** - Every transaction logged
✅ **Prescription Dispensing** - Auto-updates inventory
✅ **External Pharmacy** - Mark prescriptions for outside purchase

---

## Step 9: Connection String Details

Your connection information:

```
Host: db.vigeqwzqasblsnetbprm.supabase.co
Port: 5432
Database: postgres
User: postgres
Password: [CHANGE THIS FIRST]
```

**PostgreSQL Connection:**
```bash
psql -h db.vigeqwzqasblsnetbprm.supabase.co -U postgres -d postgres
# Then enter password
```

**PgAdmin Connection:**
```
Host: db.vigeqwzqasblsnetbprm.supabase.co
Port: 5432
Maintenance DB: postgres
Username: postgres
Password: [YOUR NEW PASSWORD]
```

---

## Step 10: Common Issues & Fixes

### Issue: "Connection refused"
**Cause**: Firewall/wrong credentials
**Fix**: 
- Verify password was changed in Supabase
- Check IP whitelist in Supabase Settings → Database
- Ensure internet connection works

### Issue: "Authentication failed"
**Cause**: Wrong API key
**Fix**:
- Copy fresh key from Supabase → Settings → API
- Use `anon public key`, not `service_role secret`

### Issue: "No medications showing"
**Cause**: Migrations didn't run or data not inserted
**Fix**:
- Check all migrations ran successfully
- Run: `SELECT COUNT(*) FROM medications;` in SQL Editor
- Should return 5 (from sample data)

### Issue: "Cannot see tables in database"
**Cause**: Migrations failed silently
**Fix**:
- Check error messages in SQL Editor
- Run migrations one by one
- Don't skip any migration

---

## Deployment to Production

### Update Server Environment Variables

In DigitalOcean or your server:

```bash
export SUPABASE_URL=https://vigeqwzqasblsnetbprm.supabase.co
export SUPABASE_ANON_KEY=your_key_here
export DATABASE_URL=postgresql://...
```

### Update Android App Secrets

In `gradle.properties`:

```properties
SUPABASE_URL=https://vigeqwzqasblsnetbprm.supabase.co
SUPABASE_ANON_KEY=your_production_key
```

Or use Android Keystore for sensitive values.

---

## Next Steps

1. ✅ Change database password (DO THIS FIRST)
2. ✅ Run all migrations in order
3. ✅ Update build.gradle with dependencies
4. ✅ Initialize SupabaseClient in app
5. ✅ Test PharmacyScreen with real data
6. ✅ Verify audit trail works
7. ✅ Deploy to production server

---

## Backup & Restore

### Backup Your Database

```bash
# Using pg_dump
pg_dump -h db.vigeqwzqasblsnetbprm.supabase.co \
        -U postgres \
        -d postgres \
        > backup_$(date +%Y%m%d).sql
```

### Restore from Backup

```bash
psql -h db.vigeqwzqasblsnetbprm.supabase.co \
     -U postgres \
     -d postgres \
     < backup_20260605.sql
```

---

## Support

For Supabase issues:
- [Supabase Docs](https://supabase.com/docs)
- [Database Connections Guide](https://supabase.com/docs/guides/database/connecting)
- [Supabase Community](https://supabase.com/community)

For app integration:
- Check `SupabasePharmacy.kt` for the main integration
- Review `PharmacyScreen.kt` for UI implementation
- See migrations in `infra/supabase/migrations/`

---

**Ready to connect to real data! 🚀**

