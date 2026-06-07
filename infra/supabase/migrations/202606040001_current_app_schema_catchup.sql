-- Current app schema catch-up
-- Date: 2026-06-04
--
-- This migration aligns Supabase with the current SQLDelight/Ktor app schema.
-- It is intentionally forward-only and copy-paste friendly for Supabase SQL Editor.

create extension if not exists pgcrypto;

-- Patients: current app fields used by SupabasePersistence.
alter table public.patients
  add column if not exists room_bed text,
  add column if not exists acuity text not null default 'Moderate',
  add column if not exists isolation text,
  add column if not exists visits integer not null default 0,
  add column if not exists diagnosis text,
  add column if not exists triage_level integer not null default 3;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'patients_triage_level_check'
      and conrelid = 'public.patients'::regclass
  ) then
    alter table public.patients
      add constraint patients_triage_level_check check (triage_level between 1 and 5);
  end if;
end $$;

-- Staff ids are String in the app. Older migrations used uuid, which rejects
-- non-uuid staff ids during REST lookups. Drop policies that depend on id before
-- the type change, then recreate text-safe versions below.
drop policy if exists "Admins can manage all staff" on public.staff_members;
drop policy if exists "Staff can view their own profile" on public.staff_members;

do $$
begin
  if exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'staff_members'
      and column_name = 'id'
      and udt_name = 'uuid'
  ) then
    alter table public.staff_members
      alter column id drop default,
      alter column id type text using id::text;
  end if;
end $$;

alter table public.staff_members
  add column if not exists pin_hash text,
  add column if not exists active boolean not null default true,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

create policy "Admins can manage all staff" on public.staff_members
for all to authenticated using (
  exists (
    select 1 from public.staff_members
    where id = auth.uid()::text and role = 'ADMIN'
  )
);

create policy "Staff can view their own profile" on public.staff_members
for select to authenticated using (id = auth.uid()::text);

-- Encounters: tolerate older migrations that created public.encounters with
-- id/service_area columns, then add the columns and uniqueness required by the
-- current REST upsert target: on_conflict=encounter_id.
create table if not exists public.encounters (
  encounter_id text primary key,
  patient_id text not null references public.patients(id) on delete cascade,
  encounter_datetime timestamptz not null default now(),
  department text not null default 'OPD',
  visit_type text not null default 'outpatient',
  provider_id text,
  facility_id text not null default 'default',
  location_id text,
  source_type text,
  source_id text,
  status text not null default 'DRAFT',
  version integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  nursing_notes text,
  sync_state text not null default 'LOCAL_ONLY'
);

alter table public.encounters
  add column if not exists encounter_id text,
  add column if not exists patient_id text,
  add column if not exists encounter_datetime timestamptz not null default now(),
  add column if not exists department text not null default 'OPD',
  add column if not exists visit_type text not null default 'outpatient',
  add column if not exists provider_id text,
  add column if not exists facility_id text not null default 'default',
  add column if not exists location_id text,
  add column if not exists source_type text,
  add column if not exists source_id text,
  add column if not exists status text not null default 'DRAFT',
  add column if not exists version integer not null default 1,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now(),
  add column if not exists deleted_at timestamptz,
  add column if not exists nursing_notes text,
  add column if not exists sync_state text not null default 'LOCAL_ONLY';

do $$
begin
  if exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'encounters'
      and column_name = 'id'
  ) then
    execute 'update public.encounters set encounter_id = coalesce(encounter_id, id::text) where encounter_id is null';
  end if;

  if exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'encounters'
      and column_name = 'service_area'
  ) then
    execute 'update public.encounters set department = coalesce(nullif(department, ''''), service_area, ''OPD'')';
  end if;
end $$;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'encounters_encounter_id_key'
      and conrelid = 'public.encounters'::regclass
  ) then
    alter table public.encounters
      add constraint encounters_encounter_id_key unique (encounter_id);
  end if;
end $$;

create index if not exists idx_encounters_patient_datetime
  on public.encounters(patient_id, encounter_datetime desc);
create index if not exists idx_encounters_department_datetime
  on public.encounters(department, encounter_datetime desc);
create index if not exists idx_encounters_status
  on public.encounters(status);

-- Consultation sub-tables present in SQLDelight but missing from Supabase.
create table if not exists public.encounter_history (
  encounter_id text primary key references public.encounters(encounter_id) on delete cascade,
  server_id text,
  chief_complaint text not null default '',
  hpi text not null default '',
  pmh text not null default '',
  medication_history text not null default '',
  allergies text not null default '',
  family_history text not null default '',
  social_history text not null default '',
  version integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.encounter_exam (
  encounter_id text primary key references public.encounters(encounter_id) on delete cascade,
  server_id text,
  system_exam_notes text not null default '',
  version integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.encounter_plan (
  encounter_id text primary key references public.encounters(encounter_id) on delete cascade,
  server_id text,
  clinical_advice text not null default '',
  follow_up_date date,
  version integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.clinical_orders (
  order_id text primary key,
  server_id text,
  encounter_id text not null references public.encounters(encounter_id) on delete cascade,
  order_type text not null,
  order_name text not null,
  instructions text,
  status text not null default 'ORDERED',
  sync_state text not null default 'LOCAL_ONLY',
  version integer not null default 1,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index if not exists idx_clinical_orders_encounter on public.clinical_orders(encounter_id);
create index if not exists idx_clinical_orders_type on public.clinical_orders(order_type);
create index if not exists idx_clinical_orders_server_id on public.clinical_orders(server_id);

-- Version/deletion columns for existing clinical REST tables.
alter table public.diagnosis
  add column if not exists server_id text,
  add column if not exists version integer not null default 1,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now(),
  add column if not exists deleted_at timestamptz;

alter table public.medication_order
  add column if not exists server_id text,
  add column if not exists version integer not null default 1,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now(),
  add column if not exists deleted_at timestamptz;

create index if not exists idx_diagnosis_encounter on public.diagnosis(encounter_id);
create index if not exists idx_diagnosis_server_id on public.diagnosis(server_id);
create index if not exists idx_medication_order_encounter on public.medication_order(encounter_id);
create index if not exists idx_medication_order_server_id on public.medication_order(server_id);

-- Program extension tables matching current SQLDelight names/columns.
create table if not exists public.opd_visits (
  opd_id text primary key,
  encounter_id text not null unique references public.encounters(encounter_id) on delete cascade,
  age_months integer,
  age_years integer,
  residence text not null,
  danger_signs boolean not null default false,
  muac_status text,
  primary_diagnosis text not null,
  treatment text not null
);

create table if not exists public.anc_visits (
  anc_id text primary key,
  encounter_id text not null unique references public.encounters(encounter_id) on delete cascade,
  parity integer not null,
  gravida integer not null,
  lmp_date date not null,
  edd_date date not null,
  visit_number integer not null,
  gestational_age integer,
  fundal_height integer,
  urinalysis_protein text,
  hiv_status text,
  iptp_dose integer,
  ifa_dispensed boolean not null default false
);

create table if not exists public.maternity_deliveries (
  mat_id text primary key,
  encounter_id text not null unique references public.encounters(encounter_id) on delete cascade,
  admission_datetime timestamptz not null,
  delivery_datetime timestamptz,
  presentation text,
  delivery_mode text,
  blood_loss_ml integer,
  infant_sex text,
  birth_weight_g integer,
  delivery_outcome text,
  apgar_1min integer,
  apgar_5min integer,
  oxytocin_given boolean not null default false,
  discharge_maternal text
);

create table if not exists public.ccc_visits (
  ccc_number text primary key,
  encounter_id text not null unique references public.encounters(encounter_id) on delete cascade,
  art_start_date date not null,
  entry_point text,
  baseline_who_stage text,
  current_regimen text,
  regimen_change_date date,
  cohort_status text,
  viral_load_value integer,
  vl_collection_date date,
  adherence_rating text
);

create table if not exists public.ncd_followups (
  ncd_followup_id text primary key,
  encounter_id text not null unique references public.encounters(encounter_id) on delete cascade,
  ncd_id text not null,
  systolic_bp integer,
  diastolic_bp integer,
  fbs_mmol numeric(4,1),
  rbs_mmol numeric(4,1),
  hba1c_percent numeric(4,1),
  weight_kg numeric(6,2),
  creatinine_value integer,
  meds_dispensed text,
  refill_qty_days integer,
  complications text
);

-- Lab/LIMS tables from SQLDelight and LabModels.
create table if not exists public.lab_tests (
  id text primary key,
  code text not null unique,
  name text not null,
  category text not null,
  specimen_type text not null,
  department text not null,
  loinc_code text,
  default_unit text,
  default_reference_range text,
  billing_code text not null,
  price numeric(12,2) not null default 0,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.lab_orders (
  id text primary key,
  patient_id text not null references public.patients(id) on delete cascade,
  encounter_id text references public.encounters(encounter_id) on delete set null,
  ordered_by text not null,
  department text not null,
  status text not null default 'ORDERED',
  priority text not null default 'ROUTINE',
  diagnosis_hint text,
  clinical_notes text,
  sample_id text,
  billable_group_id text,
  verified_by text,
  verified_at timestamptz,
  reported_by text,
  reported_at timestamptz,
  version integer not null default 1,
  deleted_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.lab_order_items (
  id text primary key,
  order_id text not null references public.lab_orders(id) on delete cascade,
  test_id text not null,
  test_code text not null,
  test_name text not null,
  status text not null default 'ORDERED',
  priority text not null default 'ROUTINE',
  instructions text,
  billing_code text not null,
  price numeric(12,2) not null default 0,
  ordered_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.lab_samples (
  id text primary key,
  order_id text not null references public.lab_orders(id) on delete cascade,
  patient_id text not null references public.patients(id) on delete cascade,
  specimen_type text not null,
  accession_number text,
  collected_by text,
  collected_at timestamptz,
  received_by text,
  received_at timestamptz,
  rejected_reason text,
  status text not null default 'ORDERED',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.lab_results (
  id text primary key,
  order_id text not null references public.lab_orders(id) on delete cascade,
  order_item_id text not null references public.lab_order_items(id) on delete cascade,
  patient_id text not null references public.patients(id) on delete cascade,
  test_id text not null,
  test_code text not null,
  test_name text not null,
  value text not null,
  value_numeric numeric,
  unit text,
  reference_range text,
  flag text,
  comment text,
  entered_by text not null,
  entered_at timestamptz not null,
  verified_by text,
  verified_at timestamptz,
  reported_by text,
  reported_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_lab_orders_patient on public.lab_orders(patient_id);
create index if not exists idx_lab_orders_status on public.lab_orders(status);
create index if not exists idx_lab_orders_department on public.lab_orders(department);
create index if not exists idx_lab_items_order on public.lab_order_items(order_id);
create index if not exists idx_lab_samples_order on public.lab_samples(order_id);
create index if not exists idx_lab_samples_patient on public.lab_samples(patient_id);
create index if not exists idx_lab_results_order on public.lab_results(order_id);
create index if not exists idx_lab_results_order_item on public.lab_results(order_item_id);

-- Scheduling ids are String in Kotlin/SQLDelight. Convert older uuid columns to text.
do $$
begin
  alter table if exists public.schedules drop constraint if exists schedules_actor_type_check;
  alter table if exists public.slots drop constraint if exists slots_status_check;
  alter table if exists public.appointments drop constraint if exists appointments_status_check;
  alter table if exists public.appointments drop constraint if exists appointments_appointment_type_check;

  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'appointments' and column_name = 'slot_id' and udt_name = 'uuid'
  ) then
    alter table public.appointments drop constraint if exists appointments_slot_id_fkey;
    alter table public.appointments drop constraint if exists appointments_slot_id_key;
    alter table public.appointments alter column slot_id type text using slot_id::text;
  end if;

  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'appointments' and column_name = 'schedule_id' and udt_name = 'uuid'
  ) then
    alter table public.appointments drop constraint if exists appointments_schedule_id_fkey;
    alter table public.appointments alter column schedule_id type text using schedule_id::text;
  end if;

  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'slots' and column_name = 'schedule_id' and udt_name = 'uuid'
  ) then
    alter table public.slots drop constraint if exists slots_schedule_id_fkey;
    alter table public.slots alter column schedule_id type text using schedule_id::text;
  end if;

  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'slots' and column_name = 'id' and udt_name = 'uuid'
  ) then
    alter table public.slots alter column id drop default;
    alter table public.slots alter column id type text using id::text;
  end if;

  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'appointments' and column_name = 'id' and udt_name = 'uuid'
  ) then
    alter table public.appointments alter column id drop default;
    alter table public.appointments alter column id type text using id::text;
  end if;

  if exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'schedules' and column_name = 'id' and udt_name = 'uuid'
  ) then
    alter table public.schedules alter column id drop default;
    alter table public.schedules alter column id type text using id::text;
  end if;
end $$;

create table if not exists public.schedules (
  id text primary key,
  actor_type text not null,
  actor_id text not null,
  name text not null,
  active boolean not null default true
);

create table if not exists public.slots (
  id text primary key,
  schedule_id text not null references public.schedules(id) on delete cascade,
  start_time timestamptz not null,
  end_time timestamptz not null,
  status text not null default 'free',
  constraint chk_slot_times check (start_time < end_time)
);

create table if not exists public.appointments (
  id text primary key,
  patient_id text not null references public.patients(id) on delete cascade,
  schedule_id text not null references public.schedules(id) on delete cascade,
  slot_id text references public.slots(id) on delete set null,
  status text not null default 'booked',
  appointment_type text not null,
  reason text,
  start_time timestamptz not null,
  end_time timestamptz not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

do $$
begin
  if not exists (
    select 1 from pg_constraint
    where conname = 'slots_schedule_id_fkey'
      and conrelid = 'public.slots'::regclass
  ) then
    alter table public.slots
      add constraint slots_schedule_id_fkey
      foreign key (schedule_id) references public.schedules(id) on delete cascade not valid;
  end if;

  if not exists (
    select 1 from pg_constraint
    where conname = 'appointments_schedule_id_fkey'
      and conrelid = 'public.appointments'::regclass
  ) then
    alter table public.appointments
      add constraint appointments_schedule_id_fkey
      foreign key (schedule_id) references public.schedules(id) on delete cascade not valid;
  end if;

  if not exists (
    select 1 from pg_constraint
    where conname = 'appointments_slot_id_fkey'
      and conrelid = 'public.appointments'::regclass
  ) then
    alter table public.appointments
      add constraint appointments_slot_id_fkey
      foreign key (slot_id) references public.slots(id) on delete set null not valid;
  end if;
end $$;

create unique index if not exists appointments_slot_id_unique
  on public.appointments(slot_id)
  where slot_id is not null;

create index if not exists idx_slots_schedule_times on public.slots(schedule_id, start_time, end_time);
create index if not exists idx_appointments_patient_times on public.appointments(patient_id, start_time desc);
create index if not exists idx_appointments_schedule_times on public.appointments(schedule_id, start_time, end_time);

-- Local/offline sync queue matching SyncQueueEntity.
create table if not exists public.sync_queue (
  id text primary key,
  entity_type text not null,
  entity_id text not null,
  operation text not null,
  payload jsonb not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_sync_queue_created_at on public.sync_queue(created_at);
create index if not exists idx_sync_queue_entity on public.sync_queue(entity_type, entity_id);

-- RLS and broad authenticated policies for app-managed tables.
alter table public.encounters enable row level security;
alter table public.vital_signs enable row level security;
alter table public.diagnosis enable row level security;
alter table public.medication_order enable row level security;
alter table public.encounter_outcome enable row level security;
alter table public.encounter_history enable row level security;
alter table public.encounter_exam enable row level security;
alter table public.encounter_plan enable row level security;
alter table public.clinical_orders enable row level security;
alter table public.opd_visits enable row level security;
alter table public.anc_visits enable row level security;
alter table public.maternity_deliveries enable row level security;
alter table public.ccc_visits enable row level security;
alter table public.ncd_followups enable row level security;
alter table public.lab_tests enable row level security;
alter table public.lab_orders enable row level security;
alter table public.lab_order_items enable row level security;
alter table public.lab_samples enable row level security;
alter table public.lab_results enable row level security;
alter table public.schedules enable row level security;
alter table public.slots enable row level security;
alter table public.appointments enable row level security;
alter table public.sync_queue enable row level security;

drop policy if exists "Allow authenticated upsert encounters" on public.encounters;
create policy "Allow authenticated upsert encounters" on public.encounters
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert vital signs" on public.vital_signs;
create policy "Allow authenticated upsert vital signs" on public.vital_signs
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert diagnosis" on public.diagnosis;
create policy "Allow authenticated upsert diagnosis" on public.diagnosis
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert medication order" on public.medication_order;
create policy "Allow authenticated upsert medication order" on public.medication_order
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert encounter outcome" on public.encounter_outcome;
create policy "Allow authenticated upsert encounter outcome" on public.encounter_outcome
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert encounter history" on public.encounter_history;
create policy "Allow authenticated upsert encounter history" on public.encounter_history
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert encounter exam" on public.encounter_exam;
create policy "Allow authenticated upsert encounter exam" on public.encounter_exam
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert encounter plan" on public.encounter_plan;
create policy "Allow authenticated upsert encounter plan" on public.encounter_plan
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert clinical orders" on public.clinical_orders;
create policy "Allow authenticated upsert clinical orders" on public.clinical_orders
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert opd visits" on public.opd_visits;
create policy "Allow authenticated upsert opd visits" on public.opd_visits
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert anc visits" on public.anc_visits;
create policy "Allow authenticated upsert anc visits" on public.anc_visits
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert maternity deliveries" on public.maternity_deliveries;
create policy "Allow authenticated upsert maternity deliveries" on public.maternity_deliveries
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert ccc visits" on public.ccc_visits;
create policy "Allow authenticated upsert ccc visits" on public.ccc_visits
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert ncd followups" on public.ncd_followups;
create policy "Allow authenticated upsert ncd followups" on public.ncd_followups
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert lab tests" on public.lab_tests;
create policy "Allow authenticated upsert lab tests" on public.lab_tests
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert lab orders" on public.lab_orders;
create policy "Allow authenticated upsert lab orders" on public.lab_orders
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert lab order items" on public.lab_order_items;
create policy "Allow authenticated upsert lab order items" on public.lab_order_items
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert lab samples" on public.lab_samples;
create policy "Allow authenticated upsert lab samples" on public.lab_samples
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert lab results" on public.lab_results;
create policy "Allow authenticated upsert lab results" on public.lab_results
for all to authenticated using (true) with check (true);

drop policy if exists "Allow authenticated upsert sync queue" on public.sync_queue;
create policy "Allow authenticated upsert sync queue" on public.sync_queue
for all to authenticated using (true) with check (true);
