-- Migration: Add Staff and RBAC support
-- Date: 2026-05-18
-- Description:
--   1. Create staff_members table
--   2. Create roles and permissions structure (simplified as enum/text for now)
--   3. Seed initial admin user

-- Step 1: Create staff_members table
CREATE TABLE IF NOT EXISTS public.staff_members (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  full_name text NOT NULL,
  role text NOT NULL, -- RECEPTIONIST, DOCTOR, NURSE, ADMIN
  department text,
  pin_hash text, -- For local auth
  active boolean DEFAULT true,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

-- Step 2: Enable RLS
ALTER TABLE public.staff_members ENABLE ROW LEVEL SECURITY;

-- Step 3: Seed initial admin user
-- NOTE: In a real production system, the pin_hash would be properly hashed.
-- For this setup, we'll use a placeholder '1234' (assuming hashing happens at the app/backend level)
INSERT INTO public.staff_members (full_name, role, department, pin_hash)
VALUES ('System Admin', 'ADMIN', 'Administration', '1234')
ON CONFLICT DO NOTHING;

-- Step 4: Create local_sync_queue for offline-first support
CREATE TABLE IF NOT EXISTS public.local_sync_queue (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_type text NOT NULL, -- Patient, Staff, etc.
  entity_id text NOT NULL,
  operation text NOT NULL, -- INSERT, UPDATE, DELETE
  payload jsonb,
  created_at timestamptz DEFAULT now(),
  processed_at timestamptz
);

-- Step 5: Policies
CREATE POLICY "Admins can manage all staff" ON public.staff_members
FOR ALL TO authenticated USING (
  EXISTS (
    SELECT 1 FROM public.staff_members
    WHERE id = auth.uid() AND role = 'ADMIN'
  )
);

CREATE POLICY "Staff can view their own profile" ON public.staff_members
FOR SELECT TO authenticated USING (id = auth.uid());
