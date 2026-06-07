-- Eagle Tech HMIS Solutions hospital tenant database template.
-- Apply this inside each hospital's separate Supabase/Postgres project.

create extension if not exists pgcrypto;

create table if not exists public.staff_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  staff_code text not null unique,
  full_name text not null,
  email_or_phone text,
  role text not null check (
    role in ('HOSPITAL_ADMIN', 'DOCTOR', 'NURSE', 'RECEPTIONIST', 'PHARMACIST', 'TREASURER', 'LAB_TECH')
  ),
  department text,
  pin_hash text,
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.pharmacy_items (
  id uuid primary key default gen_random_uuid(),
  item_name text not null,
  sku text unique,
  quantity_on_hand numeric(12,2) not null default 0,
  reorder_level numeric(12,2) not null default 0,
  unit text not null default 'unit',
  active boolean not null default true,
  updated_at timestamptz not null default now()
);

create table if not exists public.prescription_queue (
  id uuid primary key default gen_random_uuid(),
  patient_id text not null,
  encounter_id text,
  medication_name text not null,
  status text not null default 'PENDING'
    check (status in ('PENDING', 'READY', 'DISPENSED', 'CANCELLED')),
  prescribed_by uuid references auth.users(id),
  dispensed_by uuid references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.invoices (
  id uuid primary key default gen_random_uuid(),
  invoice_number text not null unique,
  patient_id text not null,
  status text not null default 'OPEN'
    check (status in ('OPEN', 'PARTIAL', 'PAID', 'VOID')),
  total_amount numeric(12,2) not null default 0,
  paid_amount numeric(12,2) not null default 0,
  created_by uuid references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.audit_events (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid references auth.users(id),
  actor_role text,
  action text not null,
  module text not null,
  context_reference text,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

alter table public.staff_profiles enable row level security;
alter table public.pharmacy_items enable row level security;
alter table public.prescription_queue enable row level security;
alter table public.invoices enable row level security;
alter table public.audit_events enable row level security;

create or replace function public.current_staff_role()
returns text
language sql
stable
security invoker
as $$
  select role
    from public.staff_profiles
   where user_id = auth.uid()
     and active
   limit 1;
$$;

create or replace function public.has_role(allowed_roles text[])
returns boolean
language sql
stable
security invoker
as $$
  select coalesce(public.current_staff_role() = any(allowed_roles), false);
$$;

create policy "Hospital admins manage staff"
on public.staff_profiles
for all
to authenticated
using (public.has_role(array['HOSPITAL_ADMIN']))
with check (public.has_role(array['HOSPITAL_ADMIN']));

create policy "Staff read own profile"
on public.staff_profiles
for select
to authenticated
using (user_id = auth.uid() or public.has_role(array['HOSPITAL_ADMIN']));

create policy "Pharmacy staff manage inventory"
on public.pharmacy_items
for all
to authenticated
using (public.has_role(array['HOSPITAL_ADMIN', 'PHARMACIST']))
with check (public.has_role(array['HOSPITAL_ADMIN', 'PHARMACIST']));

create policy "Clinical and pharmacy staff read prescriptions"
on public.prescription_queue
for select
to authenticated
using (public.has_role(array['HOSPITAL_ADMIN', 'DOCTOR', 'NURSE', 'PHARMACIST']));

create policy "Doctors create prescriptions"
on public.prescription_queue
for insert
to authenticated
with check (public.has_role(array['HOSPITAL_ADMIN', 'DOCTOR']));

create policy "Pharmacists update prescriptions"
on public.prescription_queue
for update
to authenticated
using (public.has_role(array['HOSPITAL_ADMIN', 'PHARMACIST']))
with check (public.has_role(array['HOSPITAL_ADMIN', 'PHARMACIST']));

create policy "Treasury manages invoices"
on public.invoices
for all
to authenticated
using (public.has_role(array['HOSPITAL_ADMIN', 'TREASURER', 'RECEPTIONIST']))
with check (public.has_role(array['HOSPITAL_ADMIN', 'TREASURER', 'RECEPTIONIST']));

create policy "Admins read audit events"
on public.audit_events
for select
to authenticated
using (public.has_role(array['HOSPITAL_ADMIN']));

create policy "Authenticated staff create audit events"
on public.audit_events
for insert
to authenticated
with check (public.current_staff_role() is not null);
