-- Migration: Add ANC, Maternity, CCC, and NCD extension tables
-- Date: 2026-05-18

-- Base encounters table to guarantee FK integrity for extension follow-up records.
create table if not exists public.encounters (
  id uuid primary key default gen_random_uuid(),
  patient_id text not null references public.patients(id) on delete cascade,
  encounter_datetime timestamptz not null default now(),
  service_area text not null,
  provider text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_encounters_patient_id on public.encounters(patient_id);
create index if not exists idx_encounters_datetime on public.encounters(encounter_datetime desc);

-- 1) ANC
create table if not exists public.anc_registration (
  id bigserial primary key,
  patient_id text not null references public.patients(id) on delete cascade,
  lmp_date date not null,
  edd_date date not null,
  gravida integer not null check (gravida between 0 and 20),
  parity integer not null check (parity between 0 and 20),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint anc_registration_edd_after_lmp check (edd_date > lmp_date),
  constraint anc_registration_parity_leq_gravida check (parity <= gravida)
);

create table if not exists public.anc_visit (
  encounter_id uuid primary key references public.encounters(id) on delete cascade,
  anc_registration_id bigint references public.anc_registration(id) on delete set null,
  visit_number integer not null check (visit_number between 1 and 20),
  gestational_age_weeks numeric(4,1) check (gestational_age_weeks is null or gestational_age_weeks between 4 and 45),
  fundal_height_cm numeric(4,1) check (fundal_height_cm is null or fundal_height_cm between 8 and 50),
  urinalysis text,
  hiv_status text check (hiv_status is null or hiv_status in ('POSITIVE','NEGATIVE','UNKNOWN')),
  iptp_dose_count integer check (iptp_dose_count is null or iptp_dose_count between 0 and 10),
  ifa_tablets_dispensed integer check (ifa_tablets_dispensed is null or ifa_tablets_dispensed between 0 and 1000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 2) Maternity
create table if not exists public.maternity_episode (
  parturient_serial bigserial primary key,
  patient_id text not null references public.patients(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.delivery_event (
  encounter_id uuid primary key references public.encounters(id) on delete cascade,
  maternity_episode_id bigint references public.maternity_episode(parturient_serial) on delete set null,
  admission_datetime timestamptz not null,
  delivery_datetime timestamptz,
  presentation text check (presentation is null or presentation in ('CEPHALIC','BREECH','SHOULDER','OTHER')),
  mode_of_delivery text check (mode_of_delivery is null or mode_of_delivery in ('SVD','VACUUM','FORCEPS','CS','VBAC','OTHER')),
  estimated_blood_loss_ml integer check (estimated_blood_loss_ml is null or estimated_blood_loss_ml between 0 and 5000),
  birthweight_grams integer check (birthweight_grams is null or birthweight_grams between 500 and 6000),
  apgar_1_min integer check (apgar_1_min is null or apgar_1_min between 0 and 10),
  apgar_5_min integer check (apgar_5_min is null or apgar_5_min between 0 and 10),
  delivery_outcome text check (delivery_outcome is null or delivery_outcome in ('LIVE_BIRTH','FRESH_STILLBIRTH','MACERATED_STILLBIRTH','EARLY_NEONATAL_DEATH','ABORTION','OTHER')),
  oxytocin_administered boolean,
  maternal_discharge_status text check (maternal_discharge_status is null or maternal_discharge_status in ('STABLE','REFERRED','AMA','DECEASED')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint delivery_event_timeline check (delivery_datetime is null or delivery_datetime >= admission_datetime)
);

-- 3) CCC
create table if not exists public.ccc_enrollment (
  ccc_number text primary key,
  patient_id text not null references public.patients(id) on delete cascade,
  art_start_date date,
  entry_point text,
  baseline_who_stage integer check (baseline_who_stage is null or baseline_who_stage between 1 and 4),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.ccc_followup (
  encounter_id uuid primary key references public.encounters(id) on delete cascade,
  ccc_number text references public.ccc_enrollment(ccc_number) on delete set null,
  regimen text,
  regimen_switch_date date,
  cohort_status text,
  viral_load_copies_ml integer check (viral_load_copies_ml is null or viral_load_copies_ml between 0 and 100000000),
  viral_load_date date,
  adherence_percent integer check (adherence_percent is null or adherence_percent between 0 and 100),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ccc_followup_vl_date_pair check (
    (viral_load_copies_ml is null and viral_load_date is null)
    or (viral_load_copies_ml is not null and viral_load_date is not null)
  )
);

-- 4) NCD
create table if not exists public.ncd_enrollment (
  ncd_id text primary key,
  patient_id text not null references public.patients(id) on delete cascade,
  condition_set text[] not null check (cardinality(condition_set) > 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.ncd_followup (
  encounter_id uuid primary key references public.encounters(id) on delete cascade,
  ncd_id text references public.ncd_enrollment(ncd_id) on delete set null,
  systolic_bp integer check (systolic_bp is null or systolic_bp between 60 and 260),
  diastolic_bp integer check (diastolic_bp is null or diastolic_bp between 30 and 160),
  fbs_mmol_l numeric(4,1) check (fbs_mmol_l is null or fbs_mmol_l between 1.0 and 40.0),
  rbs_mmol_l numeric(4,1) check (rbs_mmol_l is null or rbs_mmol_l between 1.0 and 50.0),
  hba1c_percent numeric(4,1) check (hba1c_percent is null or hba1c_percent between 3.0 and 20.0),
  creatinine_umol_l numeric(6,1) check (creatinine_umol_l is null or creatinine_umol_l between 10.0 and 2000.0),
  medications text,
  refill_days integer check (refill_days is null or refill_days between 0 and 365),
  complications text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ncd_followup_bp_pair check (
    (systolic_bp is null and diastolic_bp is null)
    or (systolic_bp is not null and diastolic_bp is not null)
  )
);

create index if not exists idx_anc_registration_patient_id on public.anc_registration(patient_id);
create index if not exists idx_maternity_episode_patient_id on public.maternity_episode(patient_id);
create index if not exists idx_ccc_enrollment_patient_id on public.ccc_enrollment(patient_id);
create index if not exists idx_ncd_enrollment_patient_id on public.ncd_enrollment(patient_id);
