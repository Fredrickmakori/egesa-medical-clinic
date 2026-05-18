create extension if not exists pgcrypto;

create table if not exists public.patients (
  id text primary key,
  full_name text not null,
  age integer not null check (age >= 0),
  sex text not null check (sex in ('male','female','intersex','unknown')),
  status text not null,
  assigned_ward text,
  triage_level integer not null default 3,
  clinician text,
  diagnosis text,
  version integer not null default 1,
  synced_at timestamptz,
  updated_at timestamptz not null default now()
);

create table if not exists public.payment_records (
  id uuid primary key default gen_random_uuid(),
  patient_id text not null references public.patients(id) on delete cascade,
  amount numeric(12,2) not null check (amount >= 0),
  stk_request_id text,
  stk_status text not null default 'PENDING',
  synced boolean not null default false,
  last_synced_at timestamptz,
  retry_count integer not null default 0,
  sync_error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.patients enable row level security;
alter table public.payment_records enable row level security;

create policy "Allow authenticated read" on public.patients
for select to authenticated using (true);

create policy "Allow authenticated insert" on public.patients
for insert to authenticated with check (true);

create policy "Allow authenticated update" on public.patients
for update to authenticated using (true) with check (true);

create policy "Allow authenticated payment read" on public.payment_records
for select to authenticated using (true);

create policy "Allow authenticated payment insert" on public.payment_records
for insert to authenticated with check (true);

create policy "Allow authenticated payment update" on public.payment_records
for update to authenticated using (true) with check (true);

-- ── Sync metadata for cloud synchronization ─────────────────────────────��──

create table if not exists public.sync_metadata (
  entity_id text primary key,
  entity_type text not null,
  local_version integer not null default 1,
  remote_version integer not null default 0,
  last_synced_at timestamptz,
  sync_state text not null default 'PENDING',
  created_by text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index idx_sync_metadata_entity_type on public.sync_metadata(entity_type);
create index idx_sync_metadata_sync_state on public.sync_metadata(sync_state);

alter table public.sync_metadata enable row level security;

create policy "Allow authenticated read sync metadata" on public.sync_metadata
for select to authenticated using (true);

create policy "Allow authenticated insert sync metadata" on public.sync_metadata
for insert to authenticated with check (true);

create policy "Allow authenticated update sync metadata" on public.sync_metadata
for update to authenticated using (true) with check (true);



create table if not exists public.code_sex (code text primary key);
insert into public.code_sex(code) values ('male'),('female'),('intersex'),('unknown') on conflict do nothing;
create table if not exists public.code_visit_type (code text primary key);
insert into public.code_visit_type(code) values ('outpatient'),('inpatient'),('emergency'),('follow-up'),('anc') on conflict do nothing;
create table if not exists public.code_disposition (code text primary key);
insert into public.code_disposition(code) values ('admitted'),('discharged'),('transferred'),('referred'),('deceased') on conflict do nothing;
create table if not exists public.code_fetal_presentation (code text primary key);
insert into public.code_fetal_presentation(code) values ('cephalic'),('breech'),('transverse'),('oblique'),('unknown') on conflict do nothing;
create table if not exists public.code_delivery_mode (code text primary key);
insert into public.code_delivery_mode(code) values ('svd'),('assisted-vaginal'),('cesarean'),('vbac') on conflict do nothing;
create table if not exists public.code_delivery_outcome (code text primary key);
insert into public.code_delivery_outcome(code) values ('live-birth'),('still-birth'),('neonatal-death') on conflict do nothing;
create table if not exists public.code_who_stage (code text primary key);
insert into public.code_who_stage(code) values ('stage-1'),('stage-2'),('stage-3'),('stage-4') on conflict do nothing;
create table if not exists public.code_hiv_status (code text primary key);
insert into public.code_hiv_status(code) values ('positive'),('negative'),('unknown'),('exposed') on conflict do nothing;
create table if not exists public.code_adherence_rating (code text primary key);
insert into public.code_adherence_rating(code) values ('good'),('fair'),('poor') on conflict do nothing;
create table if not exists public.code_cohort_status (code text primary key);
insert into public.code_cohort_status(code) values ('active'),('lost-to-follow-up'),('transferred-out'),('deceased'),('stopped') on conflict do nothing;
create table if not exists public.code_muac_status (code text primary key);
insert into public.code_muac_status(code) values ('green'),('yellow'),('red') on conflict do nothing;
create table if not exists public.code_complication (code text primary key);
insert into public.code_complication(code) values ('none'),('fever'),('hemorrhage'),('sepsis'),('eclampsia'),('other') on conflict do nothing;
