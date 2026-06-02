# Egesa Medical Clinic Production Readiness Plan

## 1. Purpose

This plan defines the work required to move the Egesa Medical Clinic system from a functional prototype into a production-ready clinic operations platform. The target production system must support reliable day-to-day facility workflows, offline use, role-based access, synchronized central persistence, auditability, data-driven reporting, safe deployment, and maintainable operations.

The plan is based on the current project architecture:

- Kotlin Multiplatform shared client logic and Compose Multiplatform UI.
- Android and Desktop clients as primary production targets.
- SQLDelight local database for offline-first persistence.
- Ktor server for authentication, APIs, sync, reporting, and integrations.
- Supabase/PostgreSQL migrations as the central persistence direction.
- Existing foundations for JWT authentication, role-aware navigation, local clinical records, service events, sync queue, MOH reporting, and M-Pesa server endpoints.

## 2. Production Readiness Goals

The system is production ready when a real facility can use it to complete daily workflows without data loss, unsafe access, or manual reconciliation outside the system.

Core goals:

- Complete real workflows from registration to reporting.
- Enforce role-based permissions in the UI and server API.
- Save all critical writes locally first and sync them safely.
- Replace mock and in-memory server data with database-backed services.
- Provide traceable patient, billing, inventory, and reporting records.
- Maintain audit logs for sensitive actions.
- Support deployment, monitoring, backup, rollback, and user training.

## 3. Readiness Workstreams

### 3.1 Architecture And Codebase Stabilization

Objective:
Establish a stable technical base before expanding clinical scope.

Deliverables:

- Remove constructor cycles and runtime recursion risks in repositories.
- Separate UI, view state, domain rules, repositories, local persistence, sync, and API integration.
- Introduce clear repository interfaces for patient, encounter, appointment, billing, inventory, reporting, audit, and staff administration.
- Replace direct screen-to-database logic where it creates duplicated business rules.
- Define a shared domain validation layer for workflow rules.
- Add feature flags or configuration for modules that are not production ready.

Acceptance criteria:

- `:desktop:compileKotlinJvm` and `:androidApp:compileDebugKotlin` pass.
- App starts on desktop and Android without runtime startup crashes.
- No production navigation item opens a purely static placeholder without a visible "not configured" or disabled state.
- Core repositories can be constructed without circular dependencies.

Priority:
Immediate.

### 3.2 Backend Persistence Replacement

Objective:
Replace mock/in-memory server state with persistent database-backed services.

Deliverables:

- Implement Supabase/PostgreSQL-backed services for patients, staff, encounters, appointments, billing, payments, reports, audit logs, inventory, and notifications.
- Replace `HospitalState` and synthetic server responses with real database reads/writes.
- Add server repository layer with transaction boundaries.
- Add database migration verification and seed scripts for development/staging.
- Add environment-based configuration for local, staging, and production.

Acceptance criteria:

- Creating or updating a patient persists centrally.
- Staff and role records survive server restart.
- Report endpoints read from persisted records.
- Payment callback state is persisted and linked to billing records.
- Server can run against a fresh migrated database with documented seed data.

Priority:
Immediate to high.

### 3.3 Offline-First Synchronization

Objective:
Make local-first operation reliable during connectivity loss.

Deliverables:

- Finalize the local outbox/sync queue schema and payload format.
- Assign every synced entity a stable local ID, optional server ID, version, sync state, created/updated timestamps, and deleted timestamp.
- Implement background upload for queued local changes.
- Implement server delta download by `updatedAt` or version cursor.
- Add conflict detection for stale versions.
- Define conflict policies per entity type:
  - Patient demographics: manual review for high-risk conflicts.
  - Encounters and service events: append-only or latest valid version with audit.
  - Billing and payments: no silent overwrite; require reconciliation.
  - Inventory movements: append-only movement ledger.
- Add sync status UI and failed sync retry controls.

Acceptance criteria:

- Users can register, consult, dispense, and record payments while offline.
- Queued writes sync successfully when network returns.
- Duplicate patient and encounter records are not created during retry.
- Conflicts are detected and visible.
- Failed sync records can be retried or escalated without data loss.

Priority:
High.

### 3.4 Authentication, Authorization, And Staff Administration

Objective:
Move from basic login and navigation filtering to enforceable production access control.

Deliverables:

- Persist staff users and roles centrally.
- Add staff create, edit, deactivate, reset PIN/password, and role assignment workflows.
- Enforce permissions on every server route.
- Keep UI navigation role-aware, but treat server authorization as authoritative.
- Add session expiry, refresh, and logout handling.
- Add audit logs for login, failed login, permission denial, staff changes, and role changes.

Required production roles:

- Registrar.
- Triage nurse.
- Clinician.
- Ward nurse.
- Ward clinician.
- Laboratory technologist.
- Radiology staff.
- Pharmacist.
- Cashier.
- Insurance officer.
- Reporting officer.
- Facility administrator.
- System administrator.

Acceptance criteria:

- A user cannot call a restricted API endpoint without permission.
- Deactivated users cannot log in.
- Role changes take effect on next session refresh or login.
- Permission denials are logged.
- Admin staff changes are auditable.

Priority:
High.

### 3.5 Patient Reception And Records

Objective:
Make reception workflows fully operational.

Deliverables:

- Registration form with required demographic fields, duplicate search, and patient ID generation.
- Patient search by name, ID, phone, and document number.
- Patient detail/edit screen.
- Patient chart shell showing visits, vitals, diagnoses, medications, labs, imaging, billing, and documents.
- Check-in and check-out workflow.
- Queue assignment to triage, consultation, lab, pharmacy, billing, and ward.
- Patient record print/export summary.

Acceptance criteria:

- Registrar can register a patient and place them in queue.
- Existing patients can be found before duplicate registration.
- Patient records can be edited with audit trail.
- Patient chart shows real transactional history.
- Check-in/check-out timestamps are persisted and reportable.

Priority:
High.

### 3.6 Appointment Scheduling

Objective:
Deliver real scheduling rather than static appointment screens.

Deliverables:

- Appointment tables and API routes.
- Provider and room schedules.
- Booking, rescheduling, cancellation, and no-show workflows.
- Day/week/month calendar views.
- Conflict detection for provider and room double-booking.
- Follow-up appointment creation from consultation.
- Reminder triggers for SMS/email queue.
- Appointment analytics for cancellations, no-shows, and patient flow.

Acceptance criteria:

- Staff can book and reschedule appointments without conflicts.
- Cancelled and no-show appointments are tracked.
- Appointment can check in to a real encounter workflow.
- Reminders can be queued from appointment records.

Priority:
Medium to high.

### 3.7 Clinical Workflow Completion

Objective:
Make consultation and clinical programs safe, structured, and traceable.

Deliverables:

- Structured encounter workflow for history, examination, diagnosis, plan, orders, prescription, disposition, and follow-up.
- Vital signs capture and trend display.
- Domain validation for finalizing encounters.
- Referral workflow with destination, reason, status, and printable referral note.
- Program-specific forms for OPD, ANC, maternity, CCC, NCD, and HTS where required.
- Patient chart integration for all clinical events.

Acceptance criteria:

- Clinician can start, save draft, and finalize consultation.
- Required fields are enforced before finalization.
- Vitals and diagnoses appear in patient chart.
- Referral and follow-up actions are persisted.
- Finalized encounters generate service events for reporting and billing.

Priority:
High.

### 3.8 Laboratory And Imaging

Objective:
Support order-to-result workflows.

Deliverables:

- Lab order creation from consultation.
- Lab worklist by department, status, and date.
- Sample collection, receiving, rejection, processing, verification, and reporting states.
- Result entry with reference ranges, flags, comments, and verification.
- Imaging order and result workflow.
- Attachment support for imaging reports or scanned documents.
- Result notification events.
- Patient chart result timeline.

Acceptance criteria:

- Clinician can order lab/imaging tests.
- Lab technologist can process orders through valid states.
- Results return to patient chart.
- Invalid status transitions are blocked.
- Reportable lab events are generated from transactional records.

Priority:
High.

### 3.9 Pharmacy And Inventory

Objective:
Connect dispensing to prescriptions, stock, billing, and audit.

Deliverables:

- Prescription worklist for pharmacy.
- Dispense, partial dispense, substitution, rejection, and external purchase workflows.
- Inventory item master for medicines and consumables.
- Stock batches, expiry dates, suppliers, reorder levels, and stock adjustments.
- Stock movement ledger.
- Automatic stock decrement on dispense.
- Low-stock and expiry alerts.
- Inventory reports.

Acceptance criteria:

- Pharmacist dispenses against a valid prescription.
- Stock movements are traceable and cannot be silently edited.
- Dispensing updates prescription status.
- Stock levels and low-stock alerts are accurate.
- Inventory reports are generated from stock movement records.

Priority:
High.

### 3.10 Billing, Payments, And Insurance

Objective:
Turn M-Pesa and billing foundations into a complete revenue workflow.

Deliverables:

- Tariff and charge item configuration.
- Automatic charge generation from registration, consultation, lab, imaging, pharmacy, ward, and procedures.
- Invoice creation and adjustment workflow.
- Cash, M-Pesa, insurance, waiver, and split payment support.
- M-Pesa STK initiation from billing context.
- Callback reconciliation into payment records.
- Receipt printing/export.
- Insurance claim capture, submission status, approval, rejection, and reconciliation.

Acceptance criteria:

- Cashier can create and settle an invoice.
- M-Pesa payment callback updates the correct invoice.
- Overpayment, underpayment, and failed payment states are visible.
- Insurance balances and patient balances are separated.
- Billing reports reconcile with payment records.

Priority:
High.

### 3.11 Ward Management

Objective:
Support inpatient bed and admission lifecycle.

Deliverables:

- Bed inventory by ward, room, and bed.
- Admission, transfer, discharge, and death/discharge outcome workflows.
- Ward rounds and progress notes.
- Nursing task list and medication administration events.
- Discharge summary with required validation.
- Ward census and occupancy reports.

Acceptance criteria:

- Patient can be admitted to an available bed.
- Transfers update occupancy correctly.
- Discharge requires summary and final outcome.
- Ward census reflects real bed state.
- Ward events are visible in patient chart.

Priority:
Medium to high.

### 3.12 Reporting And Dashboards

Objective:
Make reports data-driven and traceable.

Deliverables:

- MOH reporting from service events and clinical transaction tables.
- Operational reports for visits, billing, inventory, staff activity, appointments, no-shows, queue times, and ward occupancy.
- Report viewer and export/download support.
- Dashboard KPIs from persisted data.
- Source traceability from report totals to transaction rows.
- Date range, department, provider, and program filters.

Acceptance criteria:

- Report totals match source records.
- Reports can be exported.
- Dashboard values survive server restart and are not mock values.
- Reporting officer can generate monthly summaries without manual counting.

Priority:
Medium to high.

### 3.13 Audit, Compliance, And Data Governance

Objective:
Provide accountability and protect patient data.

Deliverables:

- Immutable audit log table and API.
- Audit events for login, failed login, create, update, delete, print/export, payment, sync, permission denial, and admin changes.
- Patient record access logs for sensitive views.
- Soft delete strategy for clinical and financial records.
- Backup and restore procedures.
- Data retention policy.
- Privacy controls for exported documents.

Acceptance criteria:

- Critical actions are auditable by user, timestamp, entity, action, and source device.
- Clinical and financial records are not hard-deleted in normal workflows.
- Backups can be restored in staging.
- Exported patient records include appropriate facility and patient context.

Priority:
High.

### 3.14 Observability And Operations

Objective:
Make production issues visible and actionable.

Deliverables:

- Application error logging.
- Server request logging with correlation IDs.
- Sync health dashboard.
- Failed transaction alerts.
- Background job monitoring.
- Database migration status checks.
- Basic usage metrics by module.
- Operational admin screens for departments, tariffs, queues, templates, roles, and facilities.

Acceptance criteria:

- Admin can see failed syncs and retry status.
- Server errors can be traced to request IDs.
- Failed M-Pesa callbacks or unreconciled payments are visible.
- Deployment includes a documented health check.

Priority:
Medium.

### 3.15 Testing And Quality Assurance

Objective:
Prevent production regressions and validate clinic workflows.

Deliverables:

- Unit tests for domain validation and status transitions.
- Repository tests for SQLDelight persistence.
- Sync tests for queue upload, retry, conflict, and duplicate prevention.
- API tests for authentication, authorization, and key workflows.
- UI smoke tests for Android/Desktop critical paths.
- Migration tests against a clean database.
- Manual UAT scripts for each role.
- Performance tests on low-cost Android devices.

Acceptance criteria:

- CI or local release checklist runs compile and test tasks.
- Core workflows have automated coverage.
- UAT sign-off is collected for registrar, clinician, pharmacist, cashier, lab, and administrator workflows.
- App remains responsive with realistic patient and transaction volumes.

Priority:
High.

## 4. Implementation Phases

### Phase 0: Stabilize The Current Build

Target:
Prepare the current codebase for reliable development.

Tasks:

- Fix compile and startup runtime errors.
- Identify and disable incomplete production navigation paths.
- Add smoke test checklist for login, navigation, patient list, encounter save, lab module, pharmacy, and reporting screen.
- Document known non-production modules.

Exit gate:

- Desktop and Android compile.
- App starts without startup crash.
- Known gaps are documented and visible.

### Phase 1: Make Persistence Real

Target:
Replace mock data with durable local and central persistence.

Tasks:

- Implement central patient/staff/encounter services.
- Wire server APIs to PostgreSQL/Supabase.
- Implement real patient create/update/list/detail.
- Persist audit events.
- Add staging seed data.

Exit gate:

- Server restart does not lose staff, patient, encounter, or audit data.
- Client can create and retrieve records from real persistence.

### Phase 2: Production Offline Sync

Target:
Make local-first operation reliable.

Tasks:

- Standardize sync metadata on all core entities.
- Implement upload, retry, and status display.
- Implement server delta download.
- Add conflict detection.
- Add sync admin/health view.

Exit gate:

- A complete registration and consultation can be done offline and later synced.
- Sync failures are visible and recoverable.

### Phase 3: Core Clinic Workflow Completion

Target:
Complete reception, consultation, lab/imaging, pharmacy, billing, and chart workflows.

Tasks:

- Complete patient registration and chart.
- Complete structured consultation finalization.
- Complete lab order/result workflow.
- Complete pharmacy dispense workflow.
- Complete billing and payment workflow.
- Generate service events for reporting and billing.

Exit gate:

- A patient can move from registration to consultation, lab, pharmacy, billing, and reportable completion.

### Phase 4: Administration, Security, And Compliance

Target:
Make the system safe for real users and patient data.

Tasks:

- Complete staff administration.
- Enforce server-side permissions.
- Add audit trail viewer.
- Add configuration screens for roles, departments, tariffs, queues, and templates.
- Add backup and restore procedure.

Exit gate:

- Unauthorized API actions are blocked.
- Admin changes and clinical/financial actions are auditable.

### Phase 5: Reporting, Inventory, Appointments, Ward, And Notifications

Target:
Complete operational modules beyond core outpatient flow.

Tasks:

- Add appointment scheduling and reminders.
- Add inventory and stock movement ledger.
- Add ward admission/transfer/discharge.
- Complete MOH and operational reporting.
- Add SMS/email queue and provider integration.

Exit gate:

- Facility managers can track appointments, stock, ward occupancy, billing, and service reports from source data.

### Phase 6: Pilot And Production Rollout

Target:
Deploy safely into a real facility.

Tasks:

- Create staging environment.
- Run UAT with real clinic roles.
- Train users.
- Pilot one department.
- Monitor errors, sync, payments, and reports.
- Fix pilot blockers.
- Approve go-live.

Exit gate:

- Pilot department completes agreed workflows for a full operating period.
- Backup, restore, rollback, and support processes are tested.
- Facility signs off for production use.

## 5. Critical Workflow Acceptance Scenarios

The following scenarios must pass before production rollout:

1. Registrar registers a new patient, avoids duplicate registration, checks the patient in, and assigns queue destination.
2. Triage nurse records vitals and sends the patient to consultation.
3. Clinician documents history, examination, diagnosis, plan, lab order, prescription, referral or disposition, then finalizes the encounter.
4. Lab technologist receives an order, records sample collection, enters results, verifies results, and returns results to the patient chart.
5. Pharmacist dispenses medication from prescription and stock is decremented.
6. Cashier generates invoice, initiates M-Pesa STK push or records cash payment, and prints receipt.
7. Ward nurse admits, transfers, and discharges an inpatient with accurate bed state.
8. Reporting officer generates monthly MOH and operational reports from source transactions.
9. Administrator creates a staff user, assigns a role, deactivates the user, and reviews audit logs.
10. A full offline workflow is performed, queued, synced, and verified centrally after connectivity returns.

## 6. Production Non-Functional Requirements

### Reliability

- Critical writes are durable locally before the UI reports success.
- Sync retries do not duplicate records.
- App recovers from restart with unsynced data intact.

### Performance

- Search and queue screens remain responsive with realistic patient volume.
- Reports use indexed queries or precomputed rollups where needed.
- Low-cost Android devices remain usable for common workflows.

### Security

- API routes require authentication.
- Permissions are checked server-side.
- Sensitive exports and printouts include only necessary data.
- Secrets are not hardcoded in client code.

### Maintainability

- Business rules live in domain/service layers rather than only UI screens.
- Migrations are versioned and documented.
- Modules have clear ownership and test coverage.

### Usability

- Common actions require minimal typing.
- Touch targets are large enough for tablet use.
- Patient context is visible on clinical, billing, pharmacy, and lab screens.
- Errors tell users how to recover.

## 7. Release Gates

### Development Gate

- Code compiles for Desktop and Android.
- Unit tests pass.
- New migrations are reviewed.
- No known startup crash.

### Staging Gate

- Fresh database migration succeeds.
- Seed data loads.
- Core UAT scripts pass.
- Sync and payment callbacks are tested.
- Backup and restore are tested.

### Pilot Gate

- Facility users complete training.
- Pilot department workflows pass for live-like data.
- Critical bugs have workarounds or fixes.
- Support contact and escalation process are defined.

### Production Gate

- Product owner/facility administrator signs off.
- Rollback plan is documented.
- Monitoring is active.
- Backups are scheduled.
- User accounts and roles are reviewed.

## 8. Risk Register

| Risk | Impact | Mitigation |
|---|---|---|
| Mock server state remains in production path | Data loss after restart | Replace with database-backed services before pilot |
| Offline sync duplicates records | Unsafe patient and billing records | Stable IDs, idempotent APIs, unique constraints, retry tests |
| Permissions only enforced in UI | Unauthorized access | Server-side permission middleware and audit logging |
| Billing and M-Pesa reconciliation mismatch | Financial loss | Payment ledger, callback idempotency, reconciliation reports |
| Inventory not linked to dispensing | Stock inaccuracies | Append-only stock movement ledger |
| Reports are hardcoded or manually counted | Invalid reporting | Generate reports from transactional source data |
| Poor low-end device performance | Low adoption | Performance testing with realistic data |
| Inadequate user training | Workflow failure | Role-based UAT and pilot support |
| No backup/restore process | Production data loss | Scheduled backups and tested restore drills |

## 9. Documentation Deliverables

Before production, the project should include:

- Updated README with supported platforms and setup.
- Installation and deployment guide.
- Staging and production environment guide.
- Database migration order and rollback notes.
- User manuals by role.
- Admin configuration guide.
- Backup and restore guide.
- Sync troubleshooting guide.
- Payment reconciliation guide.
- UAT scripts and sign-off sheet.
- Known limitations and release notes.

## 10. Immediate Next Actions

Recommended next implementation order:

1. Finish build/runtime stabilization and document disabled incomplete modules.
2. Replace in-memory server state with PostgreSQL-backed patient, staff, encounter, and audit services.
3. Complete patient registration, search, queue, and chart.
4. Standardize sync metadata and implement reliable upload/retry/status.
5. Complete consultation finalization with validation and service-event generation.
6. Complete billing/payment workflow before expanding advanced modules.
7. Add audit logging and server-side permission enforcement across all routes.
8. Build UAT scripts for each role and run them in staging.

## 11. Definition Of Production Ready

The system should not be considered production ready until all of the following are true:

- Users can complete full real workflows end to end.
- Offline operation works without data loss.
- Sync handles retries, duplicates, and conflicts safely.
- Roles and permissions are enforced in the server.
- Reports are generated from source transactions.
- Audit logs are persisted and searchable.
- Billing and payment reconciliation are traceable.
- Inventory movements are traceable where dispensing is active.
- Backups and restore have been tested.
- A pilot deployment has been completed and signed off.
