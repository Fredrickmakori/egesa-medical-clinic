# Egesa Medical Clinic — In-Scope Functional Specification

## Platform and architecture scope
- **Multiplatform clients**: Android + Desktop built from shared Kotlin Multiplatform code.
- **Secure access**: JWT-based authentication with role-aware access controls.
- **Database server**: PostgreSQL via Supabase migrations under `infra/supabase/` for persistent data, billing, and payment records.
- **Integration layer**: Ktor server for APIs, M-Pesa STK push orchestration, and integration callbacks.

## 2.1 Administrative functions
- Patient record management: create, view, edit, print patient records.
- Staff management: user accounts, roles, role-based permissions.
- Reporting: patient visits, billing, inventory, and staff performance.
- Audit trails: immutable activity logs for compliance and security.

## 2.2 Patient reception
- Registration with demographic capture and unique patient IDs.
- Appointment scheduling with booking, rescheduling, cancellation.
- Queue management for realtime patient flow.
- Check-in and check-out time stamping.

## 2.3 Patient consultation
- Structured consultation notes for clinicians.
- Vital-sign entry and trend tracking.
- Referral management to internal specialists/external facilities.

## 2.4 Patient diagnosis
- Diagnostic data capture (symptoms, findings, diagnosis).
- Lab and imaging order workflows.
- Results attachment into patient chart.

## 2.5 Ward management
- Bed allocation and occupancy tracking.
- Admission, transfer, and discharge lifecycle.
- Ward rounds and progress notes.

## 2.6 Outpatient services
- Outpatient visit and follow-up tracking.
- Prescription generation and print support with interaction checks.
- Billing/invoicing with payment posting and insurance claims.

## 2.7 M-Pesa STK push integration
- Mobile payment via STK push from billing context.
- Automatic reconciliation against billing records.

## 2.8 SMS integration
- Appointment reminder automation.
- Alert notifications for lab results, billing, and events.
- Two-way SMS for confirmation/cancellation responses.

## 2.9 Patient appointment and clinic checkup scheduling
- Comprehensive booking for staff and patient appointment requests.
- Multiple appointment types (consultation, specialist, follow-up).
- Calendar views: day/week/month for providers and rooms.
- Conflict detection for provider/room double-booking.
- Reminder support via SMS and email.
- Optional patient self-service portal for requests/cancellations.
- Analytics and reports on no-shows, cancellations, and patient flow.
