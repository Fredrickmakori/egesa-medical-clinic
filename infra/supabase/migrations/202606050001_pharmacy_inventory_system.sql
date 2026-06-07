-- Migration: 202606050001_pharmacy_inventory_system
-- Description: Create tables for pharmacy management, medication inventory, and medical supplies

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For text search

-- Medications table - master list of all medications available
CREATE TABLE IF NOT EXISTS public.medications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    generic_name VARCHAR(255),
    strength VARCHAR(100),  -- e.g., "500mg", "10%"
    form VARCHAR(50),  -- e.g., "tablet", "capsule", "injection", "cream"
    category VARCHAR(100),  -- e.g., "analgesics", "antibiotics", "antihistamines"
    unit_of_measurement VARCHAR(20) DEFAULT 'tablet',  -- e.g., "tablet", "ml", "unit"
    reorder_level INTEGER DEFAULT 10,
    supplier_id UUID,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_medications_name ON public.medications USING GIN (name gin_trgm_ops);
CREATE INDEX idx_medications_form ON public.medications(form);
CREATE INDEX idx_medications_category ON public.medications(category);

-- Inventory table - tracks current stock levels
CREATE TABLE IF NOT EXISTS public.inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    medication_id UUID NOT NULL REFERENCES public.medications(id) ON DELETE CASCADE,
    quantity_in_stock INTEGER NOT NULL DEFAULT 0,
    quantity_available INTEGER NOT NULL DEFAULT 0,  -- quantity_in_stock - quantity_reserved
    quantity_reserved INTEGER NOT NULL DEFAULT 0,  -- For pending orders/prescriptions
    batch_number VARCHAR(100),
    expiry_date DATE,
    location_in_pharmacy VARCHAR(255),  -- Storage location/shelf
    cost_per_unit DECIMAL(10, 2),
    selling_price_per_unit DECIMAL(10, 2),
    date_received DATE,
    warehouse_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(medication_id, batch_number)
);

CREATE INDEX idx_inventory_medication ON public.inventory(medication_id);
CREATE INDEX idx_inventory_expiry ON public.inventory(expiry_date);
CREATE INDEX idx_inventory_warehouse ON public.inventory(warehouse_id);
CREATE INDEX idx_inventory_location ON public.inventory(location_in_pharmacy);

-- Medical Supplies table - for consumables like bandages, gloves, syringes
CREATE TABLE IF NOT EXISTS public.medical_supplies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),  -- e.g., "wound care", "protective", "diagnostic", "consumables"
    description TEXT,
    unit_of_measurement VARCHAR(20),  -- e.g., "box", "packet", "unit", "pair"
    quantity_in_stock INTEGER NOT NULL DEFAULT 0,
    quantity_available INTEGER NOT NULL DEFAULT 0,
    reorder_level INTEGER DEFAULT 10,
    unit_cost DECIMAL(10, 2),
    supplier_id UUID,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_medical_supplies_category ON public.medical_supplies(category);
CREATE INDEX idx_medical_supplies_name ON public.medical_supplies USING GIN (name gin_trgm_ops);

-- Pharmacy Transactions table - audit trail for all stock movements
CREATE TABLE IF NOT EXISTS public.pharmacy_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_type VARCHAR(50) NOT NULL,  -- DISPENSING, RECEIPT, LOSS, ADJUSTMENT, TRANSFER
    medication_id UUID REFERENCES public.medications(id),
    medical_supply_id UUID REFERENCES public.medical_supplies(id),
    quantity_change INTEGER NOT NULL,  -- Positive for additions, negative for removals
    inventory_id UUID REFERENCES public.inventory(id),
    reference_type VARCHAR(50),  -- "ENCOUNTER", "PURCHASE_ORDER", "INTERNAL", "WRITE_OFF"
    reference_id VARCHAR(100),  -- Encounter ID, PO ID, etc.
    batch_number VARCHAR(100),
    notes TEXT,
    created_by UUID NOT NULL,  -- Staff member who recorded transaction
    facility_id UUID,
    warehouse_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_medication ON public.pharmacy_transactions(medication_id);
CREATE INDEX idx_transactions_reference ON public.pharmacy_transactions(reference_id);
CREATE INDEX idx_transactions_created_by ON public.pharmacy_transactions(created_by);
CREATE INDEX idx_transactions_created_at ON public.pharmacy_transactions(created_at);
CREATE INDEX idx_transactions_type ON public.pharmacy_transactions(transaction_type);

-- Medication to Encounter prescriptions tracking
-- Prescriptions columns (only if prescriptions table exists)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'prescriptions') THEN
    ALTER TABLE public.prescriptions ADD COLUMN IF NOT EXISTS inventory_id UUID;
    ALTER TABLE public.prescriptions ADD COLUMN IF NOT EXISTS dispensed_quantity INTEGER;
    ALTER TABLE public.prescriptions ADD COLUMN IF NOT EXISTS dispensed_at TIMESTAMP WITH TIME ZONE;
    ALTER TABLE public.prescriptions ADD COLUMN IF NOT EXISTS dispensed_by UUID;
    ALTER TABLE public.prescriptions ADD COLUMN IF NOT EXISTS external_purchase BOOLEAN DEFAULT FALSE;
  END IF;
END $$;

-- ============================================================================
-- VIEWS FOR LOOKUP AND REPORTING
-- ============================================================================

-- Medication lookup view - shows medications with current availability
CREATE OR REPLACE VIEW public.medication_lookup_view AS
SELECT
    m.id,
    m.id as medication_id,
    m.name as medication_name,
    m.generic_name,
    m.strength,
    m.form,
    COALESCE(SUM(i.quantity_available), 0) as quantity_available,
    m.unit_of_measurement,
    MAX(i.expiry_date) as expiry_date,
    STRING_AGG(DISTINCT i.batch_number, ', ') as available_batches
FROM public.medications m
LEFT JOIN public.inventory i ON m.id = i.medication_id AND i.quantity_available > 0
WHERE m.is_active = TRUE
GROUP BY m.id, m.name, m.generic_name, m.strength, m.form, m.unit_of_measurement;

-- Low stock medications view
CREATE OR REPLACE VIEW public.low_stock_medications_view AS
SELECT
    i.id,
    m.id as medication_id,
    m.name,
    m.generic_name,
    m.strength,
    m.form,
    i.quantity_in_stock,
    i.quantity_available,
    m.reorder_level,
    (m.reorder_level - i.quantity_in_stock) as units_to_order,
    i.expiry_date,
    i.batch_number,
    i.location_in_pharmacy,
    i.updated_at
FROM public.inventory i
JOIN public.medications m ON i.medication_id = m.id
WHERE i.quantity_available <= m.reorder_level
  AND m.is_active = TRUE
ORDER BY (m.reorder_level - i.quantity_in_stock) DESC;

-- Expired or expiring medications view
CREATE OR REPLACE VIEW public.expired_stock_view AS
SELECT
    i.id,
    m.id as medication_id,
    m.name,
    m.strength,
    m.form,
    i.batch_number,
    i.quantity_in_stock,
    i.expiry_date,
    CASE
        WHEN i.expiry_date < CURRENT_DATE THEN 'EXPIRED'
        WHEN i.expiry_date <= CURRENT_DATE + INTERVAL '30 days' THEN 'EXPIRING_SOON'
        ELSE 'OK'
    END as status
FROM public.inventory i
JOIN public.medications m ON i.medication_id = m.id
WHERE i.expiry_date <= CURRENT_DATE + INTERVAL '30 days'
ORDER BY i.expiry_date ASC;

-- Medical supplies lookup view
CREATE OR REPLACE VIEW public.medical_supplies_lookup_view AS
SELECT
    id,
    name,
    category,
    unit_of_measurement,
    quantity_available,
    quantity_in_stock,
    reorder_level,
    (reorder_level - quantity_in_stock) as units_to_order
FROM public.medical_supplies
WHERE is_active = TRUE
ORDER BY name;

-- Pharmacy audit trail by encounter
CREATE OR REPLACE VIEW public.pharmacy_audit_trail_view AS
SELECT
    pt.id,
    pt.transaction_type,
    pt.reference_id,
    m.name as medication_name,
    pt.quantity_change,
    u.full_name as recorded_by,
    pt.created_at,
    pt.notes
FROM public.pharmacy_transactions pt
LEFT JOIN public.medications m ON pt.medication_id = m.id
LEFT JOIN public.staff_members u ON pt.created_by::text = u.id::text
ORDER BY pt.created_at DESC;

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

-- Enable RLS on pharmacy tables
ALTER TABLE public.medications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.inventory ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_supplies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pharmacy_transactions ENABLE ROW LEVEL SECURITY;

-- Simplified RLS: allow all authenticated users (role checks happen at app level)
CREATE POLICY "Allow authenticated read medications"
ON public.medications FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated manage medications"
ON public.medications FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Allow authenticated read inventory"
ON public.inventory FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated manage inventory"
ON public.inventory FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Allow authenticated read supplies"
ON public.medical_supplies FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated manage supplies"
ON public.medical_supplies FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Allow authenticated read transactions"
ON public.pharmacy_transactions FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated insert transactions"
ON public.pharmacy_transactions FOR INSERT TO authenticated WITH CHECK (true);

-- ============================================================================
-- TRIGGERS FOR AUDIT AND AUTO-UPDATES
-- ============================================================================

-- Trigger to update inventory when quantity changes
CREATE OR REPLACE FUNCTION update_inventory_available()
RETURNS TRIGGER AS $$
BEGIN
    NEW.quantity_available = NEW.quantity_in_stock - COALESCE(NEW.quantity_reserved, 0);
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS inventory_available_trigger ON public.inventory;
CREATE TRIGGER inventory_available_trigger
BEFORE UPDATE ON public.inventory
FOR EACH ROW
EXECUTE FUNCTION update_inventory_available();

-- Trigger to auto-update medication in prescriptions when dispensed
-- (only created if prescriptions table exists)
CREATE OR REPLACE FUNCTION record_prescription_transaction()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.dispensed_quantity IS NOT NULL AND OLD.dispensed_quantity IS NULL THEN
        INSERT INTO public.pharmacy_transactions (
            transaction_type,
            medication_id,
            quantity_change,
            reference_type,
            reference_id,
            notes,
            created_by
        ) VALUES (
            'DISPENSING',
            NULL,
            -NEW.dispensed_quantity,
            'ENCOUNTER',
            NEW.encounter_id,
            'Prescription dispensed',
            NEW.dispensed_by
        );
    END IF;
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'prescriptions') THEN
    DROP TRIGGER IF EXISTS prescription_transaction_trigger ON public.prescriptions;
    CREATE TRIGGER prescription_transaction_trigger
    AFTER UPDATE ON public.prescriptions
    FOR EACH ROW
    EXECUTE FUNCTION record_prescription_transaction();
  END IF;
END $$;

-- Insert audit row whenever a transaction is recorded
CREATE OR REPLACE FUNCTION log_pharmacy_transaction()
RETURNS TRIGGER AS $$
BEGIN
    NEW.created_at = CURRENT_TIMESTAMP;
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS pharmacy_transaction_log_trigger ON public.pharmacy_transactions;
CREATE TRIGGER pharmacy_transaction_log_trigger
BEFORE INSERT ON public.pharmacy_transactions
FOR EACH ROW
EXECUTE FUNCTION log_pharmacy_transaction();

-- ============================================================================
-- SAMPLE DATA (Optional - Comment out if not needed)
-- ============================================================================

-- Insert sample medications
INSERT INTO public.medications (name, generic_name, strength, form, category, reorder_level) VALUES
('Paracetamol', 'Acetaminophen', '500mg', 'tablet', 'analgesics', 100),
('Amoxicillin', 'Amoxicillin', '500mg', 'capsule', 'antibiotics', 50),
('Metformin', 'Metformin', '500mg', 'tablet', 'antidiabetics', 75),
('Aspirin', 'Aspirin', '100mg', 'tablet', 'analgesics', 60),
('Ibuprofen', 'Ibuprofen', '200mg', 'tablet', 'nonsteroidal_anti_inflammatory', 80)
ON CONFLICT (name) DO NOTHING;

-- Insert sample medical supplies
INSERT INTO public.medical_supplies (name, category, unit_of_measurement, quantity_in_stock, reorder_level) VALUES
('Surgical Gloves (Powder-Free)', 'protective', 'box', 50, 10),
('Sterile Gauze Pads', 'wound_care', 'packet', 200, 50),
('Alcohol Hand Sanitizer', 'hygiene', 'bottle', 30, 10),
('Syringes (3ml)', 'medical_devices', 'box', 500, 100),
('Adhesive Bandages', 'wound_care', 'box', 100, 25)
ON CONFLICT (name) DO NOTHING;

