-- Backend persistence foundation for Ktor Supabase REST integration.
-- Adds the remote tables/columns required by the current SQLDelight-modeled data.

alter table public.patients
  add column if not exists room_bed text,
  add column if not exists acuity text not null default 'Moderate',
  add column if not exists isolation text,
  add column if not exists visits integer not null default 0;

-- Existing migrations created this constraint with the default inline name.
alter table public.encounters drop constraint if exists encounters_department_check;

alter table public.encounters
  add column if not exists sync_state text not null default 'LOCAL_ONLY';

alter table public.vital_signs
  add column if not exists height_cm numeric(6,2),
  add column if not exists bmi numeric(5,2);

create table if not exists public.queue_entries (
  patient_id text primary key references public.patients(id) on delete cascade,
  name text not null,
  triage_level integer not null default 3 check (triage_level between 1 and 5),
  status text not null default 'WAITING',
  checked_in_at timestamptz not null default now(),
  checked_out_at timestamptz,
  updated_at timestamptz not null default now()
);

create index if not exists idx_queue_entries_status_checked_in
  on public.queue_entries(status, checked_in_at);

create table if not exists public.audit_events (
  id uuid primary key default gen_random_uuid(),
  user_name text not null,
  user_id text,
  action text not null,
  module text not null,
  timestamp timestamptz not null default now(),
  context_reference text,
  permission text,
  granted boolean not null default true
);

create index if not exists idx_audit_events_timestamp
  on public.audit_events(timestamp desc);

create index if not exists idx_audit_events_module
  on public.audit_events(module);

create table if not exists public.service_events (
  service_event_id text primary key,
  encounter_id text not null references public.encounters(encounter_id) on delete cascade,
  program text not null,
  indicator_category text not null,
  service_code text,
  value_text text,
  quantity integer not null default 1,
  event_datetime timestamptz not null default now(),
  sync_state text not null default 'LOCAL_ONLY'
);

create index if not exists idx_service_events_encounter
  on public.service_events(encounter_id);

create index if not exists idx_service_events_program_datetime
  on public.service_events(program, event_datetime desc);

create table if not exists public.hts_register (
  hts_id text primary key,
  encounter_id text not null references public.encounters(encounter_id) on delete cascade,
  serial_number text,
  hts_number text,
  population_type text not null,
  testing_point text not null,
  test_1_result text,
  test_2_result text,
  final_result text not null,
  couple_testing text,
  recency_test_result text,
  referred_to text,
  linked_to_care boolean not null default false,
  remarks text
);

create index if not exists idx_hts_register_encounter
  on public.hts_register(encounter_id);

create table if not exists public.patient_documents (
  document_id text primary key,
  patient_id text not null references public.patients(id) on delete cascade,
  document_type text not null,
  image_uri text not null,
  verification_status text not null default 'PENDING_REVIEW',
  extracted_full_name text,
  extracted_identifier text,
  extracted_birth_date text,
  extracted_sex text,
  extracted_guardian_name text,
  notes text,
  captured_at timestamptz not null default now()
);

create index if not exists idx_patient_documents_patient
  on public.patient_documents(patient_id, captured_at desc);

alter table public.queue_entries enable row level security;
alter table public.audit_events enable row level security;
alter table public.service_events enable row level security;
alter table public.hts_register enable row level security;
alter table public.patient_documents enable row level security;

create policy "Allow authenticated read queue entries" on public.queue_entries
for select to authenticated using (true);
create policy "Allow authenticated upsert queue entries" on public.queue_entries
for all to authenticated using (true) with check (true);

create policy "Allow authenticated read audit events" on public.audit_events
for select to authenticated using (true);
create policy "Allow authenticated insert audit events" on public.audit_events
for insert to authenticated with check (true);

create policy "Allow authenticated read service events" on public.service_events
for select to authenticated using (true);
create policy "Allow authenticated upsert service events" on public.service_events
for all to authenticated using (true) with check (true);

create policy "Allow authenticated read hts register" on public.hts_register
for select to authenticated using (true);
create policy "Allow authenticated upsert hts register" on public.hts_register
for all to authenticated using (true) with check (true);

create policy "Allow authenticated read patient documents" on public.patient_documents
for select to authenticated using (true);
create policy "Allow authenticated upsert patient documents" on public.patient_documents
for all to authenticated using (true) with check (true);

-- Scheduling & Appointments Schema
create table if not exists public.schedules (
  id uuid primary key default gen_random_uuid(),
  actor_type text not null check (actor_type in ('practitioner', 'location')),
  actor_id text not null,
  name text not null,
  active boolean not null default true
);

create table if not exists public.slots (
  id uuid primary key default gen_random_uuid(),
  schedule_id uuid not null references public.schedules(id) on delete cascade,
  start_time timestamptz not null,
  end_time timestamptz not null,
  status text not null default 'free' check (status in ('free', 'busy', 'reserved')),
  constraint chk_slot_times check (start_time < end_time)
);

create table if not exists public.appointments (
  id uuid primary key default gen_random_uuid(),
  patient_id text not null references public.patients(id) on delete cascade,
  schedule_id uuid not null references public.schedules(id) on delete cascade,
  slot_id uuid unique references public.slots(id) on delete set null,
  status text not null default 'booked' check (status in ('proposed', 'booked', 'cancelled', 'noshow', 'fulfilled')),
  appointment_type text not null check (appointment_type in ('consultation', 'specialist', 'follow-up', 'procedure')),
  reason text,
  start_time timestamptz not null,
  end_time timestamptz not null,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

create index if not exists idx_slots_schedule_times on public.slots(schedule_id, start_time, end_time);
create index if not exists idx_appointments_schedule_times on public.appointments(schedule_id, start_time, end_time);

alter table public.schedules enable row level security;
alter table public.slots enable row level security;
alter table public.appointments enable row level security;

create policy "Allow authenticated read schedules" on public.schedules
for select to authenticated using (true);
create policy "Allow authenticated upsert schedules" on public.schedules
for all to authenticated using (true) with check (true);

create policy "Allow authenticated read slots" on public.slots
for select to authenticated using (true);
create policy "Allow authenticated upsert slots" on public.slots
for all to authenticated using (true) with check (true);

create policy "Allow authenticated read appointments" on public.appointments
for select to authenticated using (true);
create policy "Allow authenticated upsert appointments" on public.appointments
for all to authenticated using (true) with check (true);
