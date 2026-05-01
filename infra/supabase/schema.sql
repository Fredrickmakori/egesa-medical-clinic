create extension if not exists pgcrypto;

create table if not exists public.patients (
  id text primary key,
  full_name text not null,
  age integer not null check (age >= 0),
  sex text not null,
  status text not null,
  assigned_ward text,
  triage_level integer not null default 3,
  clinician text,
  diagnosis text,
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
