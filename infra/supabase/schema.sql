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

alter table public.patients enable row level security;

create policy "Allow authenticated read" on public.patients
for select to authenticated using (true);

create policy "Allow authenticated insert" on public.patients
for insert to authenticated with check (true);

create policy "Allow authenticated update" on public.patients
for update to authenticated using (true) with check (true);
