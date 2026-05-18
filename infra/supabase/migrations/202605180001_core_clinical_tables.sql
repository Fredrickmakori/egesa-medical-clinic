-- Core clinical tables aligned with local SQLDelight schema

create table if not exists public.encounters (
  encounter_id text primary key,
  patient_id text not null references public.patients(id) on delete cascade,
  encounter_datetime timestamptz not null,
  department text not null check (department in ('OPD', 'ANC', 'MATERNITY', 'CCC', 'NCD')),
  visit_type text not null,
  provider_id text,
  facility_id text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.vital_signs (
  vital_signs_id text primary key,
  encounter_id text not null references public.encounters(encounter_id) on delete cascade,
  weight_kg numeric(6,2),
  temperature_c numeric(4,1),
  systolic_bp integer,
  diastolic_bp integer,
  pulse_bpm integer,
  respiratory_rate integer,
  spo2_percent numeric(5,2),
  muac_cm numeric(5,2),
  recorded_at timestamptz not null default now()
);

create table if not exists public.diagnosis (
  diagnosis_id text primary key,
  encounter_id text not null references public.encounters(encounter_id) on delete cascade,
  diagnosis_text text not null,
  is_primary boolean not null default false,
  code_system text,
  diagnosis_code text
);

create table if not exists public.medication_order (
  medication_order_id text primary key,
  encounter_id text not null references public.encounters(encounter_id) on delete cascade,
  medication_name text not null,
  dose text,
  route text,
  frequency text,
  duration text,
  instructions text
);

create table if not exists public.encounter_outcome (
  outcome_id text primary key,
  encounter_id text not null unique references public.encounters(encounter_id) on delete cascade,
  disposition text not null,
  referral_to text,
  admitted boolean not null default false,
  discharge_notes text
);

create index if not exists idx_encounters_patient_datetime
  on public.encounters(patient_id, encounter_datetime desc);

create index if not exists idx_encounters_department_datetime
  on public.encounters(department, encounter_datetime desc);
