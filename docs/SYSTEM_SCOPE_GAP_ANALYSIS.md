# Egesa Medical Clinic System Scope Gap Analysis

Date reviewed: 2026-05-29

This review compares the documented scope in `docs/scope-specification.md`, phase/login/web documentation, and Supabase migrations against the current Kotlin Multiplatform app, Ktor server, SQLDelight schema, and Supabase SQL files.

## Executive Summary

The system has a strong foundation for a clinic management platform: shared KMP UI, responsive login, JWT/RBAC, local SQLDelight persistence for clinical encounters, service-event reporting rollups, staff records, a sync queue, M-Pesa server endpoints, and Supabase schema plans.

The largest remaining gap is that many workflows are represented as UI shells, local-only capture, migrations, or mock/in-memory server data, but are not yet complete end-to-end operational modules. In particular, appointment scheduling, billing UI/invoicing, inventory, SMS, lab/imaging workflows, real Supabase-backed sync, audit logging, and printable/exportable records are not yet fully captured.

## Captured Well

| Scope area | Current evidence | Status |
|---|---|---|
| Multiplatform shell | `shared`, `androidApp`, `desktop`, WASM entry/resources, responsive shell/navigation | Partially captured. Android/Desktop are primary; Web exists; iOS source exists but is not always enabled in Gradle. |
| Device-aware login | `LoginScreen.kt`, responsive utilities, keyboard PIN handling, manual Staff ID entry, API staff directory | Captured for UI and local/backend login. |
| JWT authentication | `POST /auth/login`, `/auth/me`, JWT role/name claims | Captured for basic server auth. |
| Role-based access | `Permission`, `RolePermissionMap`, nav role visibility, Ktor endpoint permission checks | Mostly captured. Needs deeper audit logging and permission claims if desired. |
| Local clinical persistence | SQLDelight tables for patients, encounters, vitals, diagnosis, medication orders, outcomes, HTS, service events, sync queue | Captured locally. |
| Core clinical capture | OPD, ANC, maternity, CCC, NCD, HTS generic forms save local patient/encounter/diagnosis/medication/outcome/service events | Partially captured. Forms are broad but not full specialty workflows. |
| Pharmacy dispensing | Pharmacy screen saves dispense service event and medication order | Partially captured. No stock/inventory or payment linkage. |
| Ward dashboard | Ward overview, bed board, census, ATD display, nursing tasks | UI captured, but data is mostly fake/empty/in-memory. |
| MOH reporting base | MOH workbench, HTS tally, service indicator rollup, server `/reports/{report}` | Partially captured. Good foundation, not complete reporting suite. |
| M-Pesa STK server integration | `/payments/stk-push`, `/payments/{id}/status`, callback parser, payment migrations | Server-side foundation captured. Billing UI and reconciliation are incomplete. |
| Supabase schema direction | Migrations for RBAC/sync, staff, billing/payments, clinical tables, MCH/CCC/NCD, reporting | Captured as database plan. Runtime integration is incomplete. |

## Not Yet Captured Or Incomplete

### 1. Patient Reception

Documented goal:
- Registration with demographic capture and unique patient IDs.
- Appointment scheduling.
- Queue management.
- Check-in and check-out timestamps.

Current gaps:
- The Reception screen shows queue and patient list, but `+ Register` is a no-op.
- Queue data is served from `FakeRepository`/server `HospitalState`, and `HospitalState.receptionQueue()` returns empty.
- No appointment model, table, API, or UI was found.
- No check-in/check-out timestamp workflow was found.
- No provider/room calendar or conflict detection exists.

Priority:
- Add patient registration form to Reception using `LocalRepository.upsertPatient`.
- Add appointment entities, repository methods, server routes, and scheduling UI.
- Add queue/check-in/check-out state transitions.

### 2. Patient Record Management

Documented goal:
- Create, view, edit, print patient records.

Current gaps:
- Local patient upsert/list exists.
- Server patient endpoints are read-focused and backed by empty in-memory `HospitalState`.
- No patient edit/detail workflow is complete in the reception UI.
- No print/export patient record feature was found.
- No persistent server-side patient CRUD connected to Supabase was found.

Priority:
- Implement patient detail/edit screen.
- Add create/update API routes and Supabase-backed persistence.
- Add print/export patient summary.

### 3. Staff Management

Documented goal:
- User accounts, roles, role-based permissions.

Current gaps:
- Admin can add staff locally via SQLDelight.
- Server has an in-memory `AuthStore`; no server staff CRUD.
- Staff added locally does not provision a real backend login unless manually mirrored.
- No password reset, deactivate/reactivate, or audit trail persistence.
- No Supabase-backed staff authentication connection.

Priority:
- Replace `AuthStore` with persistent staff table/service.
- Add admin staff CRUD API.
- Add PIN/password reset workflow and deactivate/reactivate controls.

### 4. Consultation And Clinical Documentation

Documented goal:
- Structured consultation notes.
- Vital signs with trends.
- Referrals.

Current gaps:
- Generic program encounter forms capture a few fields.
- Repository has vital-sign methods, but no clear vitals entry/trend UI was found.
- Referral is only a field in encounter outcome; no referral workflow, destination directory, status tracking, or print/export.
- No longitudinal patient chart view combines visits, vitals, diagnoses, medications, labs, and notes.

Priority:
- Add encounter detail chart.
- Add vitals capture/trend UI.
- Add referral lifecycle and referral letters.

### 5. Diagnosis, Lab, And Imaging

Documented goal:
- Diagnostic data capture.
- Lab and imaging orders.
- Results attachment into patient chart.

Current gaps:
- Diagnosis capture exists locally.
- `TimelineEventType.LAB` exists, but no lab order/result entity, UI, API, or attachment workflow was found.
- No imaging order/result entity, UI, API, or attachment workflow was found.
- No file/document attachment storage path was found.

Priority:
- Add lab/imaging order tables and models.
- Add order/result UI and result attachment support.
- Link results into patient chart timeline.

### 6. Ward Management

Documented goal:
- Bed allocation and occupancy tracking.
- Admission, transfer, discharge lifecycle.
- Ward rounds and progress notes.

Current gaps:
- Ward UI displays bed board, census, ATD, nursing tasks.
- Most ward server data comes from `HospitalState`, which returns empty/default data unless patients have ward fields.
- ATD UI is display-only; no actual admit/transfer/discharge mutations.
- No ward round/progress note entity or UI was found.

Priority:
- Add admit/transfer/discharge commands and persistence.
- Add bed inventory and occupancy state.
- Add ward round/progress notes.

### 7. Billing, Invoicing, Payments, And Insurance

Documented goal:
- Billing/invoicing.
- Payment posting.
- Insurance claims.
- M-Pesa STK push and reconciliation.

Current gaps:
- M-Pesa server endpoints and payment/billing migrations exist.
- No billing/invoice UI was found in navigation.
- No client-side API wrapper for payment endpoints was found in `ClinicApi`.
- No payment repository implementation was found beyond contracts.
- No billing item generation from encounters/pharmacy was found.
- No insurance claim model, table, API, or UI was found.
- Reconciliation is simulated/in-memory, not persisted to billing records.

Priority:
- Add Billing workflow area and UI.
- Add client payment API integration.
- Persist billing/payment records and callback reconciliation.
- Add insurance claim entities and workflow.

### 8. Inventory

Documented goal:
- Inventory reporting.

Current gaps:
- No inventory models, tables, APIs, or UI were found.
- Pharmacy dispensing does not decrement stock.
- No reorder levels, stock adjustments, supplier records, or stock audit trail.

Priority:
- Add inventory and stock movement schema.
- Link pharmacy dispense to stock movement.
- Add stock reports.

### 9. SMS And Email Integration

Documented goal:
- Appointment reminders.
- Lab/billing/event alerts.
- Two-way SMS confirmations/cancellations.
- Email reminder support.

Current gaps:
- No SMS provider abstraction, server endpoints, queue table, or UI settings were found.
- No email provider abstraction or notification templates were found.
- No appointment reminders because appointment scheduling is not implemented.

Priority:
- Add notification queue and provider interfaces.
- Add SMS/email templates.
- Trigger reminders from appointment lifecycle.

### 10. Appointment Scheduling

Documented goal:
- Booking, rescheduling, cancellation.
- Consultation/specialist/follow-up appointment types.
- Day/week/month calendar views.
- Provider/room conflict detection.
- Patient self-service portal.
- No-show/cancellation analytics.

Current gaps:
- No appointment model/table/API/UI was found.
- No room/provider schedule model.
- No calendar view.
- No self-service portal.
- No no-show/cancellation analytics.

Priority:
- Treat this as a full missing module.
- Start with appointment schema and staff/provider calendars.
- Add conflict detection before UI polish.

### 11. Real Backend Persistence And Sync

Documented goal:
- Supabase/PostgreSQL persistence.
- Cloud sync with conflict handling.

Current gaps:
- Supabase migrations are present.
- Client has local SQLDelight and sync queue.
- Server `HospitalState` is in-memory and mostly empty/default.
- `/sync/patients` returns all in-memory patients, not Supabase deltas.
- `/sync/patients/batch` returns synthetic success without persisting updates.
- Conflict resolution returns synthetic success.
- No `SupabaseService` runtime implementation was found.

Priority:
- Implement server repository/service backed by Supabase/PostgreSQL.
- Replace synthetic sync responses with real versioned persistence.
- Implement conflict detection and merge policy.

### 12. Audit Trails

Documented goal:
- Immutable activity logs for compliance/security.

Current gaps:
- `AuditEvent` model exists.
- Admin screen keeps local UI-state audit events when adding staff.
- Server `/admin/audit-trail` reads from in-memory `HospitalState.auditTrail()`.
- No immutable persisted audit table or automatic authorization/action logging was found.

Priority:
- Add persisted audit table/service.
- Log login, failed login, CRUD, payment, sync, and permission decisions.
- Expose filtered audit UI.

### 13. Reports And Dashboards

Documented goal:
- Patient visits, billing, inventory, staff performance reports.

Current gaps:
- MOH and service indicator reporting are partially captured.
- Admin report UI only builds an endpoint string; it does not call/download/render results.
- Billing, inventory, staff performance, no-show, cancellation, and patient-flow analytics are not complete.
- `HospitalState.adminKpis()`, bottlenecks, trend, queue, ward census, tasks mostly return empty/default values.

Priority:
- Wire report UI to API.
- Add export/download.
- Implement non-MOH operational reports.
- Persist and compute dashboard metrics from real data.

### 14. Documentation Accuracy Gaps

Current gaps in docs:
- `docs/IMPLEMENTATION_SUMMARY.md`, `docs/PHASE2_IMPLEMENTATION.md`, and `docs/PHASE3_IMPLEMENTATION.md` are empty.
- `README.md` references `docs/figma-accessibility-ux-guidelines.md`, but that file was not found.
- Web docs claim all four platforms are complete, but Gradle only includes iOS conditionally and the active modules are Android/Desktop/shared/server.
- Supabase README migration order only lists schema plus billing migration, but the repository now has additional RBAC, staff, clinical, domain-code, MCH/CCC/NCD, and reporting migrations.
- Login docs contain stale demo IDs compared with the current API-first staff directory unless updated alongside code.

Priority:
- Fill Phase 2/3 docs with real current state or remove stale claims.
- Update README links and platform support language.
- Update Supabase migration order.
- Add this gap analysis to the project roadmap.

## Suggested Build Roadmap

### Phase A: Make Existing Foundations Real
1. Replace server `HospitalState` mock/in-memory data with database-backed services.
2. Implement real patient create/update/read API.
3. Wire client `ClinicApi` to payment, staff admin, patient mutation, and report endpoints.
4. Persist audit logs.

### Phase B: Complete Core Clinic Operations
1. Patient registration and check-in/check-out.
2. Appointment scheduling with provider/room conflict checks.
3. Patient chart with vitals, diagnoses, medications, referrals, labs, imaging, and timeline.
4. Admission/transfer/discharge mutations and ward notes.

### Phase C: Revenue And Communication
1. Billing/invoicing UI.
2. Payment posting and real M-Pesa reconciliation.
3. Insurance claim workflow.
4. SMS/email notification queue and templates.

### Phase D: Operational Reporting
1. Report API result viewer/downloads.
2. Billing, inventory, staff performance, patient flow, no-show/cancellation analytics.
3. Dashboard metrics from persisted data.

## Bottom Line

The system is no longer just a scaffold: authentication, RBAC, local clinical capture, MOH/service indicators, and database schemas are meaningfully started. What is not yet captured is the full operational depth: appointments, billing UI, inventory, SMS/email, lab/imaging, real database-backed server persistence, audit immutability, print/export workflows, and end-to-end sync/reconciliation.
