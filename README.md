# Egesa Medical Clinic

Kotlin Multiplatform hospital management starter targeting:
- Android
- Desktop (Windows/Linux via JVM)

## Modules
- `shared`: workflow models + in-memory operational state.
- `desktop`: operator-focused dashboard with feature tabs for Reception, Consultation, Diagnosis, Ward, and Admin.
- `androidApp`: mobile operational dashboard with tabbed workflow summaries and searchable patient list.

## Implemented UI flows
- Reception queue cards with triage and waiting-time indicators.
- Consultation and diagnosis workbench lists with patient/clinician/diagnosis context.
- Ward bed-board overview (occupied vs available beds).
- Admin KPI cards for total registrations, pending consultation, admitted patients, and active clinicians.
- Global patient search by ID/name.

## Run
- Desktop: `./gradlew :desktop:run`
- Android build: `./gradlew :androidApp:assembleDebug`

## Next steps
1. Persistence layer (Room or SQLDelight)
2. Authentication and role-based permissions
3. Multi-user synchronization API
4. Report exports and analytics charts
