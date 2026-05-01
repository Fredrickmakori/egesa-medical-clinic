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
