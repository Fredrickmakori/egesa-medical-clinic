# Egesa Medical Clinic

Starter Kotlin Multiplatform hospital management app targeting:
- Android
- Desktop (Windows/Linux via JVM)

## Modules
- `shared`: shared domain models and in-memory state container.
- `desktop`: Compose Desktop app shell for reception, consultation, diagnosis, ward ops, and admin panel.
- `androidApp`: Android app shell using Compose UI with the same feature tabs.

## Run
- Desktop: `./gradlew :desktop:run`
- Android: `./gradlew :androidApp:assembleDebug`

## Notes
This is an initial scaffold focused on architecture and feature areas. Next steps:
1. Persistence layer (SQLDelight/Room)
2. Authentication/roles
3. Networking/API for multi-user sync
4. Report exports and dashboards

## UX and Figma Standards
- See `docs/figma-accessibility-ux-guidelines.md` for required annotation rules covering accessibility, risky-action confirmations, interruption-safe UX, and localization patterns.
