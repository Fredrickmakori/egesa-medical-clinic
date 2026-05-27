-- Migration: Add RBAC support and cloud sync metadata
-- Date: 2026-05-17
-- Description:
--   1. Add version tracking to patients table (version, synced_at)
--   2. Create sync_metadata table for tracking entity sync state
--   3. Enable RLS on sync_metadata for secured access

-- Step 1: Alter patients table to add sync tracking columns
ALTER TABLE public.patients
ADD COLUMN IF NOT EXISTS version integer NOT NULL DEFAULT 1,
ADD COLUMN IF NOT EXISTS synced_at timestamptz;

-- Step 2: Create sync_metadata table
CREATE TABLE IF NOT EXISTS public.sync_metadata (
  entity_id text primary key,
  entity_type text not null,
  local_version integer not null default 1,
  remote_version integer not null default 0,
  last_synced_at timestamptz,
  sync_state text not null default 'PENDING',  -- PENDING, SYNCING, SYNCED, CONFLICT
  created_by text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Step 3: Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_sync_metadata_entity_type ON public.sync_metadata(entity_type);
CREATE INDEX IF NOT EXISTS idx_sync_metadata_sync_state ON public.sync_metadata(sync_state);
CREATE INDEX IF NOT EXISTS idx_sync_metadata_updated_at ON public.sync_metadata(updated_at DESC);

-- Step 4: Enable RLS on sync_metadata
ALTER TABLE public.sync_metadata ENABLE ROW LEVEL SECURITY;

-- Step 5: Create RLS policies for sync_metadata
CREATE POLICY "Allow authenticated read sync metadata" ON public.sync_metadata
FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated insert sync metadata" ON public.sync_metadata
FOR INSERT TO authenticated WITH CHECK (true);

CREATE POLICY "Allow authenticated update sync metadata" ON public.sync_metadata
FOR UPDATE TO authenticated USING (true) WITH CHECK (true);

-- Step 6: Create a function to update sync metadata on patient updates
CREATE OR REPLACE FUNCTION public.update_sync_metadata()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.sync_metadata (entity_id, entity_type, local_version, sync_state, created_by)
  VALUES (NEW.id, 'Patient', NEW.version, 'PENDING', COALESCE(current_user, 'system'))
  ON CONFLICT (entity_id) DO UPDATE
  SET local_version = NEW.version,
      sync_state = 'PENDING',
      updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 7: Create trigger to automatically update sync_metadata on patient updates
DROP TRIGGER IF EXISTS trigger_patient_sync_metadata ON public.patients;
CREATE TRIGGER trigger_patient_sync_metadata
AFTER INSERT OR UPDATE ON public.patients
FOR EACH ROW
EXECUTE FUNCTION public.update_sync_metadata();

-- Verification queries (run these to verify migration success):
-- SELECT * FROM information_schema.columns WHERE table_name='patients' AND column_name IN ('version', 'synced_at');
-- SELECT * FROM public.sync_metadata LIMIT 1;
-- SELECT * FROM pg_stat_all_indexes WHERE relname = 'sync_metadata';

