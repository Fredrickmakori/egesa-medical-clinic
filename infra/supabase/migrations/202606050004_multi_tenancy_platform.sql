-- Migration: 202606050004_multi_tenancy_platform
-- Description: Sets up the database tables and columns for multi-tenant clinic and hospital management.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- HOSPITALS (TENANTS) TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.hospitals (
    id TEXT PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    name VARCHAR(255) NOT NULL,
    tenant_code VARCHAR(100) NOT NULL UNIQUE,
    contact_email VARCHAR(255) NOT NULL,
    billing_plan VARCHAR(50) NOT NULL DEFAULT 'basic', -- basic, premium, enterprise
    billing_status VARCHAR(50) NOT NULL DEFAULT 'trialing', -- active, past_due, trialing, cancelled
    amount_billed DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hospitals_tenant_code ON public.hospitals(tenant_code);
CREATE INDEX IF NOT EXISTS idx_hospitals_billing_status ON public.hospitals(billing_status);

-- ============================================================================
-- SEED DEFAULT HOSPITAL (EGESA MEDICAL CLINIC)
-- ============================================================================

INSERT INTO public.hospitals (id, name, tenant_code, contact_email, billing_plan, billing_status, amount_billed)
VALUES (
    'default', 
    'Egesa Medical Clinic', 
    'egesa', 
    'info@egesamedical.com', 
    'enterprise', 
    'active', 
    150.00
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- ALTER PATIENTS, STAFF, QUEUE, APPOINTMENTS, AND SCHEDULES TO SUPPORT TENANCY
-- ============================================================================

-- 1. Alter public.patients
ALTER TABLE public.patients ADD COLUMN IF NOT EXISTS facility_id TEXT NOT NULL DEFAULT 'default';
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'patients_facility_id_fkey'
          AND conrelid = 'public.patients'::regclass
    ) THEN
        ALTER TABLE public.patients
        ADD CONSTRAINT patients_facility_id_fkey
        FOREIGN KEY (facility_id) REFERENCES public.hospitals(id) ON DELETE CASCADE;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_patients_facility_id ON public.patients(facility_id);

-- 2. Alter public.staff_members
ALTER TABLE public.staff_members ADD COLUMN IF NOT EXISTS facility_id TEXT NOT NULL DEFAULT 'default';
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'staff_members_facility_id_fkey'
          AND conrelid = 'public.staff_members'::regclass
    ) THEN
        ALTER TABLE public.staff_members
        ADD CONSTRAINT staff_members_facility_id_fkey
        FOREIGN KEY (facility_id) REFERENCES public.hospitals(id) ON DELETE CASCADE;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_staff_members_facility_id ON public.staff_members(facility_id);

-- 3. Alter public.queue_entries
ALTER TABLE public.queue_entries ADD COLUMN IF NOT EXISTS facility_id TEXT NOT NULL DEFAULT 'default';
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'queue_entries_facility_id_fkey'
          AND conrelid = 'public.queue_entries'::regclass
    ) THEN
        ALTER TABLE public.queue_entries
        ADD CONSTRAINT queue_entries_facility_id_fkey
        FOREIGN KEY (facility_id) REFERENCES public.hospitals(id) ON DELETE CASCADE;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_queue_entries_facility_id ON public.queue_entries(facility_id);

-- 4. Alter public.schedules
ALTER TABLE public.schedules ADD COLUMN IF NOT EXISTS facility_id TEXT NOT NULL DEFAULT 'default';
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'schedules_facility_id_fkey'
          AND conrelid = 'public.schedules'::regclass
    ) THEN
        ALTER TABLE public.schedules
        ADD CONSTRAINT schedules_facility_id_fkey
        FOREIGN KEY (facility_id) REFERENCES public.hospitals(id) ON DELETE CASCADE;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_schedules_facility_id ON public.schedules(facility_id);

-- 5. Alter public.appointments
ALTER TABLE public.appointments ADD COLUMN IF NOT EXISTS facility_id TEXT NOT NULL DEFAULT 'default';
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'appointments_facility_id_fkey'
          AND conrelid = 'public.appointments'::regclass
    ) THEN
        ALTER TABLE public.appointments
        ADD CONSTRAINT appointments_facility_id_fkey
        FOREIGN KEY (facility_id) REFERENCES public.hospitals(id) ON DELETE CASCADE;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_appointments_facility_id ON public.appointments(facility_id);

-- Ensure all pre-existing records reference the default hospital
UPDATE public.patients SET facility_id = 'default' WHERE facility_id IS NULL OR facility_id = '';
UPDATE public.staff_members SET facility_id = 'default' WHERE facility_id IS NULL OR facility_id = '';
UPDATE public.queue_entries SET facility_id = 'default' WHERE facility_id IS NULL OR facility_id = '';
UPDATE public.schedules SET facility_id = 'default' WHERE facility_id IS NULL OR facility_id = '';
UPDATE public.appointments SET facility_id = 'default' WHERE facility_id IS NULL OR facility_id = '';

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

ALTER TABLE public.hospitals ENABLE ROW LEVEL SECURITY;

-- Select policy: Authenticated staff can view their own hospital details
DROP POLICY IF EXISTS "Allow staff to read their own hospital" ON public.hospitals;
CREATE POLICY "Allow staff to read their own hospital" ON public.hospitals
FOR SELECT TO authenticated
USING (
    id = (
        SELECT facility_id FROM public.staff_members 
        WHERE id = auth.uid()::text LIMIT 1
    )
);

-- Insert policy: Anyone can register a new hospital from the landing page
DROP POLICY IF EXISTS "Allow public hospital self-registration" ON public.hospitals;
CREATE POLICY "Allow public hospital self-registration" ON public.hospitals
FOR INSERT TO anon, authenticated
WITH CHECK (true);

-- Update policy: Only hospital admins can update hospital details
DROP POLICY IF EXISTS "Allow admins to update hospital" ON public.hospitals;
CREATE POLICY "Allow admins to update hospital" ON public.hospitals
FOR UPDATE TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM public.staff_members
        WHERE id = auth.uid()::text AND facility_id = public.hospitals.id AND role = 'ADMIN'
    )
) WITH CHECK (
    EXISTS (
        SELECT 1 FROM public.staff_members
        WHERE id = auth.uid()::text AND facility_id = public.hospitals.id AND role = 'ADMIN'
    )
);

-- Update patient policies to enforce tenant isolation
DROP POLICY IF EXISTS "Allow authenticated read" ON public.patients;
CREATE POLICY "Allow authenticated read" ON public.patients
FOR SELECT TO authenticated
USING (
    facility_id = (
        SELECT facility_id FROM public.staff_members 
        WHERE id = auth.uid()::text LIMIT 1
    )
);

DROP POLICY IF EXISTS "Allow authenticated insert" ON public.patients;
CREATE POLICY "Allow authenticated insert" ON public.patients
FOR INSERT TO authenticated
WITH CHECK (
    facility_id = (
        SELECT facility_id FROM public.staff_members 
        WHERE id = auth.uid()::text LIMIT 1
    )
);

DROP POLICY IF EXISTS "Allow authenticated update" ON public.patients;
CREATE POLICY "Allow authenticated update" ON public.patients
FOR UPDATE TO authenticated
USING (
    facility_id = (
        SELECT facility_id FROM public.staff_members 
        WHERE id = auth.uid()::text LIMIT 1
    )
) WITH CHECK (
    facility_id = (
        SELECT facility_id FROM public.staff_members 
        WHERE id = auth.uid()::text LIMIT 1
    )
);
