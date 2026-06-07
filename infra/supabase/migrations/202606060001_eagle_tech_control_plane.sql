-- Eagle Tech HMIS Solutions control-plane and tenant bootstrap scaffold.
-- Apply this to the Eagle Tech platform/control-plane database.
-- Each hospital's clinical database should then receive the tenant template below.

create extension if not exists pgcrypto;

create table if not exists public.hospital_tenants (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  hospital_name text not null,
  legal_name text,
  contact_name text not null,
  contact_email_or_phone text not null,
  county_or_region text,
  status text not null default 'PENDING_APPROVAL'
    check (status in ('PENDING_APPROVAL', 'PROVISIONING', 'ACTIVE', 'SUSPENDED', 'FAILED')),
  supabase_project_ref text,
  api_base_url text,
  desktop_build_profile text,
  branding jsonb not null default '{}'::jsonb,
  requested_modules text[] not null default array['RECEPTION', 'CONSULTATION', 'PHARMACY', 'BILLING', 'REPORTS'],
  created_by uuid references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.tenant_provisioning_events (
  id uuid primary key default gen_random_uuid(),
  hospital_tenant_id uuid not null references public.hospital_tenants(id) on delete cascade,
  event_type text not null,
  status text not null default 'PENDING',
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.platform_staff_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  role text not null check (role in ('SUPER_ADMIN', 'SUPPORT')),
  active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.hospital_tenants enable row level security;
alter table public.tenant_provisioning_events enable row level security;
alter table public.platform_staff_profiles enable row level security;

create or replace function public.is_platform_super_admin()
returns boolean
language sql
stable
security invoker
as $$
  select exists (
    select 1
      from public.platform_staff_profiles p
     where p.user_id = auth.uid()
       and p.role = 'SUPER_ADMIN'
       and p.active
  );
$$;

create policy "Platform admins manage tenants"
on public.hospital_tenants
for all
to authenticated
using (public.is_platform_super_admin())
with check (public.is_platform_super_admin());

create policy "Platform admins manage provisioning events"
on public.tenant_provisioning_events
for all
to authenticated
using (public.is_platform_super_admin())
with check (public.is_platform_super_admin());

create policy "Platform staff can read own profile"
on public.platform_staff_profiles
for select
to authenticated
using (user_id = auth.uid() or public.is_platform_super_admin());

create policy "Platform admins manage staff profiles"
on public.platform_staff_profiles
for all
to authenticated
using (public.is_platform_super_admin())
with check (public.is_platform_super_admin());

-- Tenant clinical database template notes:
-- 1. Create staff_profiles(user_id uuid references auth.users, role text, active boolean).
-- 2. Enable RLS on every exposed table.
-- 3. Authorize by staff_profiles.role inside that hospital database.
-- 4. Store role/app authorization in database rows or app_metadata, never user_metadata.
-- 5. Keep Supabase service_role keys only on the server/provisioning worker.
