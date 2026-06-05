# Production Readiness Backlog

This backlog breaks `docs/PRODUCTION_READINESS_PLAN.md` into implementation-sized tasks. Items are grouped by production priority so the system can move from stabilization to safe facility rollout without mixing foundational fixes with feature expansion.

## P0 Stabilization

- [ ] Add repository construction tests for `LocalRepository`, `LocalLabRepository`, and `LocalEncounterRepository`.
- [ ] Add a minimal lab create/load repository test to prevent dependency-cycle regressions.
- [ ] Add a desktop startup smoke checklist covering login, navigation, patient list, encounter save, lab, pharmacy, and reporting.
- [ ] Fix or explicitly document incomplete navigation paths and placeholder production modules.
- [ ] Keep `:desktop:compileKotlinJvm` passing.
- [ ] Keep `:androidApp:compileDebugKotlin` passing.

## P1 Persistence

- [ ] Replace server mock/in-memory state with PostgreSQL/Supabase-backed services.
- [ ] Persist staff, patients, encounters, and audit events centrally.
- [ ] Add persistent patient create/update/list/detail APIs.
- [ ] Add persistent staff administration APIs.
- [ ] Add seed data for development and staging.
- [ ] Add migration verification for clean database setup.

## P2 Sync

- [ ] Standardize sync metadata across core entities.
- [ ] Implement upload, retry, and sync status display.
- [ ] Implement server delta download.
- [ ] Add conflict detection for stale versions.
- [ ] Add conflict resolution flows for high-risk records.
- [ ] Add sync health monitoring for administrators.

## P3 Core Workflows

- [ ] Complete patient registration, search, queue assignment, and patient chart.
- [ ] Complete structured consultation with validation before finalization.
- [ ] Complete laboratory order, worklist, sample, result, verification, and chart return workflows.
- [ ] Complete pharmacy dispensing from prescriptions.
- [ ] Connect pharmacy dispensing to stock movements.
- [ ] Complete billing invoices, payment posting, M-Pesa reconciliation, and receipts.
- [ ] Add validation gates before finalization, dispensing, billing, and discharge.

## P4 Governance

- [ ] Enforce permissions on server routes, not only in navigation.
- [ ] Persist immutable audit logs for login, permissions, clinical, billing, sync, and admin actions.
- [ ] Add audit log filtering and review UI.
- [ ] Add admin configuration screens for departments, tariffs, queues, templates, roles, and facilities.
- [ ] Add backup and restore documentation.
- [ ] Add privacy controls for print/export workflows.

## P5 Rollout

- [ ] Add role-based UAT scripts.
- [ ] Add staging environment checklist.
- [ ] Add backup/restore drill checklist.
- [ ] Add pilot department rollout checklist.
- [ ] Add production rollback checklist.
- [ ] Add user training materials by role.
- [ ] Add release notes and known limitations for each production candidate.
