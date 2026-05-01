# Egesa Medical Clinic

Kotlin Multiplatform hospital management starter targeting:
- Android
- Desktop (Windows/Linux via JVM)
- Ktor server API (client-server architecture)

## Modules
- `shared`: workflow models + in-memory operational state.
- `desktop`: operator-focused dashboard with feature tabs for Reception, Consultation, Diagnosis, Ward, and Admin.
- `androidApp`: mobile operational dashboard with tabbed workflow summaries and searchable patient list.
- `server`: Ktor backend exposing patient, queue, bed, and metrics APIs for remote retrieval/backups.

## Implemented client-server and cloud-backup foundation
- REST API endpoints: `/health`, `/patients`, `/queue`, `/beds`, `/metrics`.
- Shared `RecordSyncClient` interface + `CloudSyncConfig` contract for Supabase-compatible synchronization.
- Supabase SQL schema and security policies under `infra/supabase/schema.sql`.

## Run
- Desktop: `./gradlew :desktop:run`
- Android build: `./gradlew :androidApp:assembleDebug`
- Server API: `./gradlew :server:run`

## Supabase setup
1. Create a Supabase project.
2. Run `infra/supabase/schema.sql` in Supabase SQL editor.
3. Store project URL + anon key and wire into your platform sync client implementation.

## Next steps
1. Implement concrete `RecordSyncClient` on desktop/android (Ktor client).
2. Background job for autonomous uploads (WorkManager on Android, scheduler on desktop/server).
3. Authentication and role-based permissions.
4. Report exports and analytics charts.
