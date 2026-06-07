# Current System Lacking Audit

Date reviewed: 2026-05-29

This audit compares the thesis/system documentation in the referenced Word files and `docs/scope-specification.md` with the current Kotlin Multiplatform worktree. Evidence was checked in the shared KMP models/UI/repository, SQLDelight schema, Ktor server routes, M-Pesa service, and Supabase migrations.

## Executive Summary

The current system has meaningful foundations: Kotlin Multiplatform shared models and UI, local SQLDelight persistence, JWT login, role-aware navigation, basic patient registration, active queue check-in/check-out, clinical register capture for OPD/ANC/Maternity/CCC/NCD/HTS, pharmacy dispensing capture, MOH/service-event summaries, Ktor endpoints, M-Pesa STK route scaffolding, and Supabase migration plans.

What is still lacking is the complete end-to-end hospital workflow described in the documentation. Many modules exist as local capture screens, mock/in-memory server state, DTOs, or database migrations, but not as durable, production-ready workflows. The biggest missing areas are appointment scheduling, full patient charting, lab and radiology orders/results, billing and invoicing UI, insurance claims, stock/inventory control, SMS/email reminders, immutable audit logging, real database-backed sync, and print/export outputs.

## Current Implementation Captured Well

| Area | Evidence | Current state |
|---|---|---|
| Kotlin Multiplatform structure | `shared`, `androidApp`, `desktop`, `server`, `shared/src/*Main` | Captured for Android/Desktop/shared/server, with web/iOS source present but not equally production-proven. |
| Authentication and RBAC foundation | `server/src/main/kotlin/com/egesa/clinic/server/Auth.kt`, `HospitalModels.kt`, `NavGraph.kt`, `Server.kt` | Basic JWT login and role-aware permissions exist. |
| Patient registration and queue foundation | `AreaScreen.kt`, `ClinicApi.kt`, `Server.kt`, `HospitalState` | Reception can register patients, save locally, call API/fallback, add queue entries, and check patients out. |
| Local clinical persistence | `ClinicDatabase.sq`, `LocalRepository.kt` | Patients, encounters, vitals, diagnoses, medication orders, outcomes, HTS entries, service events, staff, and sync queue exist locally. |
| Clinical register capture | `DepartmentScreens.kt` | OPD, ANC, Maternity, CCC, NCD, and HTS screens save local encounter/service-event data. |
| Pharmacy dispensing foundation | `DepartmentScreens.kt`, `LocalRepository.kt` | Dispensing saves a medication order and service event locally. |
| Ward dashboard foundation | `AreaScreen.kt`, `Server.kt`, `HospitalState` | Ward overview, bed board, census, ATD, tasks, and handoff views/routes exist, but most data is empty/default/display-only. |
| MOH/service reporting foundation | `MohReportScreen.kt`, `Reporting.kt`, `ServiceEventEntity`, reporting Supabase migration | Service-event rollups and MOH report routes exist as a foundation. |
| M-Pesa server foundation | `MpesaService.kt`, `/payments/stk-push`, `/payments/callback/mpesa`, billing migration | Daraja STK push and callback parsing are started server-side. |
| Supabase schema direction | `infra/supabase/migrations/*` | Migrations describe intended persistent data, RBAC, billing, clinical, MCH/CCC/NCD, and reporting structure. |

## Gaps Against The Documentation

### 1. Persistent Backend Is Still Missing

Documented expectation:
- Supabase/PostgreSQL persistence for patients, staff, billing, payments, clinical data, audit logs, and sync.

Current evidence:
- `Server.kt` creates `val state = HospitalState()`.
- `HospitalState` stores `patients`, `queue`, `pendingStk`, and `auditEvents` in mutable in-memory lists.
- `/sync/patients/batch` returns synthetic `synced` results without persisting updates.
- Supabase SQL migrations exist, but no runtime Supabase/PostgreSQL service is wired into the server.

What is lacking:
- Database-backed repository/services in the Ktor server.
- Durable patient, queue, billing, payment, ward, audit, and reporting persistence.
- Real versioned sync and conflict resolution.

Priority:
- Replace `HospitalState` as the server source of truth with database-backed services.
- Keep `HospitalState` only for demo/mock mode, if needed.

### 2. Patient Record Management Is Incomplete

Documented expectation:
- Create, view, edit, and print patient records.
- Longitudinal patient chart combining visits, vitals, diagnoses, medicines, labs, imaging, referrals, and notes.

Current evidence:
- Reception can create/register patients.
- `PatientCard` displays basic patient details.
- `LocalRepository` can read patients and encounters.
- The older `ConsultationScreen` has a timeline-style patient view, but the router now sends consultation/diagnosis to `ClinicalProgramsScreen`, not the longitudinal chart.

What is lacking:
- Patient detail screen.
- Patient edit workflow.
- Patient chart timeline backed by local/server records.
- Print/export patient summary.
- Duplicate patient search/merge workflow.

Priority:
- Add a patient profile/chart screen connected to `PatientEntity`, `EncounterEntity`, vitals, diagnoses, medication orders, outcomes, and service events.

### 3. Appointment Scheduling Is Not Implemented

Documented expectation:
- Booking, rescheduling, cancellation.
- Consultation/specialist/follow-up appointment types.
- Provider and room calendars.
- Day/week/month views.
- Conflict detection.
- Reminder support and no-show analytics.

Current evidence:
- No appointment model/table/API/UI was found.
- No calendar model, provider availability, room availability, conflict rule, or appointment status workflow was found.

What is lacking:
- This is a full missing module.

Priority:
- Add appointment schema first, then server routes, repository methods, and calendar UI.

### 4. Consultation Workflow Is Too Generic

Documented expectation:
- Structured consultation notes, vitals, symptoms, findings, diagnosis, orders, prescriptions, referrals, and follow-up.

Current evidence:
- `DepartmentScreens.kt` provides generic program tabs and saves a small set of encounter fields.
- `ClinicDatabase.sq` has `VitalSignsEntity`, but the current clinical form does not expose full vital-sign entry/trend views.
- `EncounterOutcomeEntity` has `referral_to`, but there is no referral lifecycle.

What is lacking:
- Complete clinician consultation workspace.
- Structured history/examination/assessment/plan notes.
- Vitals entry and trend charts.
- Referral creation, status tracking, referral letters, and destination directory.
- Follow-up plan connected to scheduling.

Priority:
- Build a clinician encounter screen that saves structured notes, vitals, diagnoses, medication orders, orders, referral, and follow-up in one workflow.

### 5. Lab And Radiology Workflows Are Missing

Documented expectation:
- Lab and imaging order workflows.
- Result attachment into patient chart.

Current evidence:
- `TimelineEventType.LAB` exists.
- No lab order/result table exists in SQLDelight.
- No radiology order/result table exists in SQLDelight.
- No lab/radiology UI or API routes were found.
- No attachment/file storage flow was found.

What is lacking:
- Lab order, specimen, result, verification, and report flow.
- Imaging order, status, finding/report, and result flow.
- Results appearing in patient chart/timeline.
- File/document attachment support.

Priority:
- Add order/result entities and screens before trying to polish reporting.

### 6. Billing, Invoicing, And Insurance Are Mostly Absent

Documented expectation:
- Billing/invoicing, payment posting, insurance claims, and automatic reconciliation against billing records.

Current evidence:
- `PaymentContracts.kt` defines payment abstractions.
- `MpesaService.kt` can initiate STK push and parse callbacks.
- `Server.kt` exposes payment routes.
- Supabase billing/payment migrations exist.
- `ClinicApi.kt` does not expose payment methods.
- Navigation has no Billing area.
- SQLDelight has no invoice, billing item, payment, payer, or insurance claim tables.

What is lacking:
- Billing/invoice UI.
- Automatic bill generation from consultation, lab, radiology, pharmacy, procedures, and ward charges.
- Client-side M-Pesa integration.
- Payment persistence and reconciliation against invoices.
- Insurance eligibility, claim creation, claim tracking, and uncovered balance routing.

Priority:
- Add billing domain models and local/server persistence, then add billing UI and link service events to charge items.

### 7. M-Pesa Is Server-Only And Not Reconciled

Documented expectation:
- STK push from billing context with automatic reconciliation.

Current evidence:
- STK push endpoint exists.
- Callback parser exists.
- `HospitalState.reconcilePendingStkRequests` only removes IDs from an in-memory list.
- No durable payment record update is performed by callback parsing.

What is lacking:
- Billing-context initiation from the client.
- Persisted pending payment record before STK request.
- Callback-to-payment update.
- Invoice balance update.
- Receipt/reconciliation report.

Priority:
- Create a `PaymentService` backed by database tables and call it from the callback route.

### 8. Inventory And Stock Control Are Missing

Documented expectation:
- Inventory reporting, pharmacy stock control, stock movement, and stock reports.

Current evidence:
- Pharmacy dispensing records medication/service events only.
- No inventory/stock tables, models, routes, or UI were found.

What is lacking:
- Item master, stock batches, stock balances, stock movements, supplier records, reorder levels, expiry tracking, stock audit trail, and inventory reports.

Priority:
- Add inventory schema and make pharmacy dispense decrement stock.

### 9. Ward Management Is Display-Heavy But Not Operational

Documented expectation:
- Bed allocation, occupancy, admission, transfer, discharge lifecycle, ward rounds, and progress notes.

Current evidence:
- Ward screens show overview, bed board, census, ATD, tasks, and handoff.
- Server only exposes read routes such as `/wards/overview`, `/wards/beds`, `/wards/atd`.
- `HospitalState.atdState()` returns hardcoded sample discharge checklist data.
- No admit/transfer/discharge mutation routes were found.
- No ward-round/progress-note entities were found.

What is lacking:
- Bed master and occupancy persistence.
- Admission, transfer, and discharge commands.
- Ward round notes.
- Nursing progress notes.
- Real ward census from persisted admission data.

Priority:
- Add ward admission lifecycle tables and mutation routes.

### 10. SMS And Email Integration Are Missing

Documented expectation:
- Appointment reminders, lab/billing alerts, event notifications, two-way SMS, and email reminders.

Current evidence:
- No SMS/email provider abstraction, notification queue, templates, routes, or settings were found.
- Appointment scheduling is absent, so appointment reminders cannot yet trigger.

What is lacking:
- Notification queue.
- SMS provider integration.
- Email provider integration.
- Message templates.
- Delivery status and reply handling.

Priority:
- Implement after appointment scheduling and key event workflows exist.

### 11. Audit Trail Is Not Immutable Or Comprehensive

Documented expectation:
- Immutable activity logs for compliance and security.

Current evidence:
- `AuditEvent` model exists.
- `HospitalState` has an in-memory `auditEvents` list.
- `/admin/audit-trail` returns the in-memory list.
- No automatic server logging was found for login, failed login, patient registration, queue changes, payment callbacks, sync, or permission checks.

What is lacking:
- Persisted append-only audit table/service.
- Automatic audit writes across clinical, billing, auth, admin, sync, and reporting actions.
- Audit filtering/search/export.

Priority:
- Add audit middleware/service and write audit events at route boundaries.

### 12. Reports Are Partial

Documented expectation:
- Patient visits, billing, inventory, staff performance, ward, MOH, no-show/cancellation, and patient-flow reports.

Current evidence:
- MOH/service indicators have a foundation.
- `AdminScreen.kt` builds a report URL string with JSON/CSV format options.
- Dashboard server values such as `adminKpis`, bottlenecks, trends, ward census, and tasks are empty/default in `HospitalState`.

What is lacking:
- Report viewer/download integration.
- Billing reports.
- Inventory reports.
- Staff performance reports.
- Appointment no-show/cancellation analytics.
- Patient-flow analytics from real persisted queue events.

Priority:
- Wire reporting UI to real endpoints after persistence is in place.

### 13. Roles Are Too Narrow For The Documentation

Documented expectation:
- System administrator, facility administrator, registrar, triage nurse, clinician, ward nurse, pharmacist, laboratory technologist, radiology staff, cashier, insurance officer, records officer, reporting officer, and queue manager.

Current evidence:
- Current `UserRole` values are `RECEPTIONIST`, `DOCTOR`, `NURSE`, `PHARMACIST`, and `ADMIN`.
- `Permission` has a small set of broad permissions.

What is lacking:
- Role granularity for lab, radiology, cashier, insurance, reporting, records, ward nurse, facility admin, and queue manager.
- Permission mapping for appointments, reports, inventory, claims, lab/radiology, audit export, and staff lifecycle.

Priority:
- Expand roles and permissions before implementing module-level security for the missing modules.

### 14. Documentation Accuracy Needs Cleanup

Current evidence:
- `docs/IMPLEMENTATION_SUMMARY.md`, `docs/PHASE2_IMPLEMENTATION.md`, and `docs/PHASE3_IMPLEMENTATION.md` are empty.
- Existing web docs claim broad platform completion and cloud sync, while current runtime evidence shows cloud sync is not production-backed.
- `infra/supabase/README.md` does not fully reflect all current migrations.
- `Server.kt` `/scope` says appointments are a core module, but no appointment implementation exists.

What is lacking:
- Documentation should distinguish implemented, partial, planned, and mock/demo features.
- Empty docs should be filled or removed.
- Claims about cloud sync and web/iOS completeness should be narrowed to current evidence.

## Recommended Implementation Roadmap

### Phase 1: Make The Current Foundations Real

1. Replace server `HospitalState` with database-backed repositories.
2. Add durable patient registration, queue, and audit persistence.
3. Implement real sync upload/download with versions and conflict handling.
4. Update documentation to mark mock/demo features accurately.

### Phase 2: Complete Core Clinic Workflows

1. Patient detail/edit/chart screen.
2. Structured consultation workspace with vitals and notes.
3. Lab and radiology orders/results.
4. Ward admission/transfer/discharge with notes and census.

### Phase 3: Add Scheduling And Communication

1. Appointment schema, API, and calendar UI.
2. Provider/room conflict detection.
3. Reminder queue.
4. SMS/email provider integration.

### Phase 4: Add Revenue Cycle And Stock

1. Billing/invoice models and UI.
2. Service-event-to-charge generation.
3. M-Pesa client integration and callback reconciliation.
4. Insurance claims.
5. Inventory and stock movement.

### Phase 5: Complete Reporting And Governance

1. Report viewer/downloads.
2. Billing, inventory, staff performance, queue, and appointment analytics.
3. Immutable audit trail with filters/export.
4. Role matrix expansion and permission hardening.

## Bottom Line

The current system is a solid prototype/foundation, not yet the full system described in the documentation. The most urgent technical gap is durable backend persistence and real sync, because many other modules depend on it. The most visible user-facing gaps are appointment scheduling, full patient charting, lab/radiology workflows, billing/invoicing, insurance, inventory, SMS/email, and print/export.
