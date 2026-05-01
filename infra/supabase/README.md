# Supabase setup

This repo includes SQL for the hospital `patients` table and RLS policies.

## Create the Supabase project
1. Sign into Supabase and create a new project.
2. Open SQL Editor and run `infra/supabase/schema.sql`.
3. Copy:
   - Project URL
   - anon key

## Usage in app
Use these values to configure `CloudSyncConfig` in the shared module and implement a platform `RecordSyncClient`.

## Optional REST backup flow
- Server module exposes `/patients` on port `8080`.
- You can run a scheduled job/worker to push data from local state to Supabase REST endpoint:
  - `POST {SUPABASE_URL}/rest/v1/patients`
  - `apikey: {anon/service key}`
  - `Authorization: Bearer {key}`
  - `Prefer: resolution=merge-duplicates`

## Migration run order
Run SQL files in this sequence from Supabase SQL Editor or CLI migrations:
1. `infra/supabase/schema.sql`
2. `infra/supabase/migrations/202605010001_billing_and_payments.sql`

## Rollback notes
If you need to rollback `202605010001_billing_and_payments.sql`, run in reverse dependency order:
1. Drop payment RLS policies:
   - `drop policy if exists "Allow hospital roles read payments" on public.payments;`
   - `drop policy if exists "Allow hospital roles insert payments" on public.payments;`
   - `drop policy if exists "Allow hospital roles update payments" on public.payments;`
2. Drop indexes:
   - `drop index if exists public.idx_payments_status;`
   - `drop index if exists public.idx_payments_checkout_request_id;`
   - `drop index if exists public.idx_payments_patient_id;`
   - `drop index if exists public.idx_billing_items_status;`
   - `drop index if exists public.idx_billing_items_patient_id;`
3. Drop tables:
   - `drop table if exists public.payments;`
   - `drop table if exists public.billing_items;`
