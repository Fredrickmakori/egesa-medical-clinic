-- Migration: 202606050002_laboratory_management_system
-- Description: Complete laboratory management with tests, QC, equipment, reagents, and quality tracking

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================================================
-- LAB TESTS AND PANELS
-- ============================================================================

-- Master lab tests catalog
CREATE TABLE IF NOT EXISTS public.lab_tests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,  -- e.g., "CBC", "FBS"
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),  -- e.g., "Hematology", "Biochemistry", "Immunology"
    specimen_type VARCHAR(100) NOT NULL,  -- e.g., "Whole Blood", "Serum", "Urine", "CSF"
    specimen_volume_ml DECIMAL(10, 2),
    specimen_container VARCHAR(100),  -- e.g., "EDTA tube", "SST tube", "Sterile container"
    specimen_storage_temp VARCHAR(50),  -- e.g., "Room Temp", "2-8C", "-20C"
    specimen_stability_hours INTEGER,
    turnaround_time_hours INTEGER DEFAULT 24,
    loincCode VARCHAR(50),  -- LOINC code for standardization
    default_unit VARCHAR(50),
    default_reference_range_male VARCHAR(100),
    default_reference_range_female VARCHAR(100),
    default_reference_range_pediatric VARCHAR(100),
    billing_code VARCHAR(50),
    price DECIMAL(10, 2),
    cost DECIMAL(10, 2),
    requires_fasting BOOLEAN DEFAULT FALSE,
    requires_special_preparation TEXT,
    is_critical_value_test BOOLEAN DEFAULT FALSE,
    critical_value_low DECIMAL(15, 4),
    critical_value_high DECIMAL(15, 4),
    method VARCHAR(255),  -- e.g., "Automated hematology analyzer", "Manual microscopy"
    equipment_id UUID,  -- Reference to equipment that runs this test
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

CREATE INDEX idx_lab_tests_code ON public.lab_tests(code);
CREATE INDEX idx_lab_tests_category ON public.lab_tests(category);
CREATE INDEX idx_lab_tests_specimen ON public.lab_tests(specimen_type);

-- Lab test panels (grouped tests like "Complete Blood Count" contains multiple tests)
CREATE TABLE IF NOT EXISTS public.lab_test_panels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    test_ids UUID[] NOT NULL,  -- Array of lab_tests.id
    specimen_type VARCHAR(100),
    turnaround_time_hours INTEGER DEFAULT 24,
    billing_code VARCHAR(50),
    price DECIMAL(10, 2),
    cost DECIMAL(10, 2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID
);

-- Reference ranges by age group and gender (for dynamic flagging)
CREATE TABLE IF NOT EXISTS public.lab_reference_ranges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_id UUID NOT NULL REFERENCES public.lab_tests(id) ON DELETE CASCADE,
    age_group VARCHAR(50),  -- e.g., "Adult", "Child (5-12)", "Infant (0-1)", "Pediatric"
    gender VARCHAR(20),  -- "M", "F", "All"
    unit VARCHAR(50),
    range_low DECIMAL(15, 4),
    range_high DECIMAL(15, 4),
    critical_low DECIMAL(15, 4),
    critical_high DECIMAL(15, 4),
    notes TEXT,
    effective_from DATE,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ref_ranges_test ON public.lab_reference_ranges(test_id);

-- ============================================================================
-- LAB SPECIMENS AND SAMPLES
-- ============================================================================

-- Track individual specimens collected
CREATE TABLE IF NOT EXISTS public.lab_specimens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    accession_number VARCHAR(100) UNIQUE,  -- Barcode/tracking number
    order_id UUID NOT NULL,  -- References lab_orders
    patient_id UUID NOT NULL,
    test_id UUID REFERENCES public.lab_tests(id),
    specimen_type VARCHAR(100),
    container_id VARCHAR(100),  -- Physical barcode
    volume_actual_ml DECIMAL(10, 2),
    volume_required_ml DECIMAL(10, 2),
    collected_by UUID NOT NULL,  -- Staff ID
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_by UUID,
    received_at TIMESTAMP WITH TIME ZONE,
    sample_quality VARCHAR(50),  -- "Acceptable", "Hemolyzed", "Clotted", etc.
    rejection_reason VARCHAR(255),
    rejection_reason_id UUID,  -- Reference to rejection reasons
    storage_location VARCHAR(255),  -- e.g., "Fridge A, Shelf 2, Position 15"
    storage_temperature VARCHAR(50),
    status VARCHAR(50) DEFAULT 'COLLECTED',  -- COLLECTED, RECEIVED, ACCEPTED, REJECTED, TESTING, COMPLETED
    chain_of_custody_complete BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_specimens_accession ON public.lab_specimens(accession_number);
CREATE INDEX idx_specimens_order ON public.lab_specimens(order_id);
CREATE INDEX idx_specimens_patient ON public.lab_specimens(patient_id);
CREATE INDEX idx_specimens_status ON public.lab_specimens(status);

-- Specimen rejection reasons
CREATE TABLE IF NOT EXISTS public.lab_rejection_reasons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE,
    reason VARCHAR(255) NOT NULL,
    category VARCHAR(50),  -- "Pre-analytical", "Collection", "Storage"
    severity VARCHAR(50),  -- "Minor", "Major"
    is_active BOOLEAN DEFAULT TRUE
);

-- ============================================================================
-- LAB EQUIPMENT AND INSTRUMENTS
-- ============================================================================

-- Lab equipment/instruments
CREATE TABLE IF NOT EXISTS public.lab_equipment (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    manufacturer VARCHAR(255),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    equipment_type VARCHAR(100),  -- "Hematology Analyzer", "Chemistry Analyzer", etc.
    location VARCHAR(255),
    acquisition_date DATE,
    warranty_expiry DATE,
    purchase_cost DECIMAL(12, 2),
    status VARCHAR(50) DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, MAINTENANCE, DECOMMISSIONED
    operational_from TIMESTAMP WITH TIME ZONE,
    operational_until TIMESTAMP WITH TIME ZONE,
    last_calibration_date DATE,
    next_calibration_due DATE,
    last_maintenance_date DATE,
    next_maintenance_due DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Equipment calibration records
CREATE TABLE IF NOT EXISTS public.lab_equipment_calibration (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    equipment_id UUID NOT NULL REFERENCES public.lab_equipment(id),
    calibration_date DATE NOT NULL,
    calibration_time TIMESTAMP WITH TIME ZONE,
    performed_by UUID NOT NULL,
    calibration_result VARCHAR(50),  -- "PASSED", "FAILED", "OUT_OF_SPEC"
    notes TEXT,
    certificate_url VARCHAR(500),
    next_due_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_calibration_equipment ON public.lab_equipment_calibration(equipment_id);

-- Equipment maintenance records
CREATE TABLE IF NOT EXISTS public.lab_equipment_maintenance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    equipment_id UUID NOT NULL REFERENCES public.lab_equipment(id),
    maintenance_date DATE,
    maintenance_type VARCHAR(50),  -- "PREVENTIVE", "CORRECTIVE"
    performed_by UUID,
    maintenance_contractor VARCHAR(255),
    description TEXT,
    parts_replaced TEXT,
    cost DECIMAL(10, 2),
    notes TEXT,
    status VARCHAR(50),  -- "SCHEDULED", "IN_PROGRESS", "COMPLETED"
    downtime_hours DECIMAL(10, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- LAB QUALITY CONTROL
-- ============================================================================

-- QC Materials (control solution, standard, etc.)
CREATE TABLE IF NOT EXISTS public.lab_qc_materials (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE,
    name VARCHAR(255),
    material_type VARCHAR(100),  -- "Control", "Calibrator", "Reference Standard"
    lot_number VARCHAR(100),
    manufacturer VARCHAR(255),
    received_date DATE,
    expiry_date DATE,
    storage_location VARCHAR(255),
    test_ids UUID[],  -- Which tests this material is used for
    expected_value DECIMAL(15, 4),
    expected_range_low DECIMAL(15, 4),
    expected_range_high DECIMAL(15, 4),
    unit VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- QC Results (Levey-Jennings chart data)
CREATE TABLE IF NOT EXISTS public.lab_qc_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    qc_material_id UUID NOT NULL REFERENCES public.lab_qc_materials(id),
    equipment_id UUID NOT NULL REFERENCES public.lab_equipment(id),
    test_id UUID REFERENCES public.lab_tests(id),
    qc_date DATE NOT NULL,
    qc_time TIMESTAMP WITH TIME ZONE,
    result_value DECIMAL(15, 4) NOT NULL,
    unit VARCHAR(50),
    status VARCHAR(50),  -- "PASSED", "WARNING", "FAILED"
    control_number INTEGER,  -- Sequence number (1st run, 2nd run, etc.)
    performed_by UUID,
    mean_deviation DECIMAL(15, 4),  -- For Levey-Jennings
    sd_deviation DECIMAL(15, 4),  -- Standard deviation multiple
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_qc_results_material ON public.lab_qc_results(qc_material_id);
CREATE INDEX idx_qc_results_equipment ON public.lab_qc_results(equipment_id);
CREATE INDEX idx_qc_results_date ON public.lab_qc_results(qc_date);

-- ============================================================================
-- LAB REAGENTS AND CONSUMABLES
-- ============================================================================

-- Lab reagents tracking (with expiry alerts)
CREATE TABLE IF NOT EXISTS public.lab_reagents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE,
    name VARCHAR(255) NOT NULL,
    reagent_type VARCHAR(100),  -- "Reagent", "Control", "Calibrator"
    manufacturer VARCHAR(255),
    lot_number VARCHAR(100),
    catalog_number VARCHAR(100),
    received_date DATE,
    expiry_date DATE NOT NULL,
    storage_location VARCHAR(255),
    storage_temperature VARCHAR(50),  -- "Room Temp", "2-8C", "-20C"
    quantity DECIMAL(10, 2),
    unit_of_measurement VARCHAR(50),  -- "mL", "Units", "Doses"
    usage_count INTEGER DEFAULT 0,
    test_ids UUID[],  -- Which tests use this reagent
    reorder_level DECIMAL(10, 2),
    status VARCHAR(50) DEFAULT 'IN_USE',  -- IN_USE, RESERVED, EXPIRED, DEPLETED
    cost DECIMAL(10, 2),
    supplier_id VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reagents_expiry ON public.lab_reagents(expiry_date);
CREATE INDEX idx_reagents_status ON public.lab_reagents(status);

-- ============================================================================
-- LAB RESULTS AND VALIDATION
-- ============================================================================

-- Extended lab results with validation flags
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS specimen_id UUID REFERENCES public.lab_specimens(id);
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS result_status VARCHAR(50) DEFAULT 'PRELIMINARY';  -- PRELIMINARY, VERIFIED, REPORTED, CORRECTED, CANCELLED
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS is_critical_value BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS critical_notification_sent BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS previous_result_value VARCHAR(100);  -- For delta check
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS delta_check_performed BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS delta_check_passed BOOLEAN;
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS reflexive_test_ordered BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS method_used VARCHAR(255);
ALTER TABLE IF EXISTS public.lab_results ADD COLUMN IF NOT EXISTS qc_performance_id UUID;  -- Link to QC result used

-- ============================================================================
-- LAB STAFF COMPETENCIES
-- ============================================================================

-- Staff competency for different tests
CREATE TABLE IF NOT EXISTS public.lab_staff_competencies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    staff_id UUID NOT NULL,
    test_id UUID NOT NULL REFERENCES public.lab_tests(id),
    competency_level VARCHAR(50),  -- "Trainee", "Competent", "Advanced", "Supervisor"
    certification_date DATE,
    certification_expiry DATE,
    certified_by UUID,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_competencies_staff ON public.lab_staff_competencies(staff_id);
CREATE INDEX idx_competencies_test ON public.lab_staff_competencies(test_id);

-- ============================================================================
-- LAB WORKFLOW STATUS AUDIT
-- ============================================================================

-- Track all status transitions for audit trail
CREATE TABLE IF NOT EXISTS public.lab_order_status_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id VARCHAR(100) NOT NULL,
    specimen_id UUID,
    result_id UUID,
    from_status VARCHAR(50),
    to_status VARCHAR(50),
    changed_by UUID,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_status_audit_order ON public.lab_order_status_audit(order_id);
CREATE INDEX idx_status_audit_date ON public.lab_order_status_audit(changed_at);

-- ============================================================================
-- LAB CRITICAL ALERTS
-- ============================================================================

-- Critical value alerts
CREATE TABLE IF NOT EXISTS public.lab_critical_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    result_id UUID NOT NULL REFERENCES public.lab_results(id),
    order_id VARCHAR(100),
    patient_id UUID,
    test_name VARCHAR(255),
    critical_value DECIMAL(15, 4),
    threshold_type VARCHAR(50),  -- "HIGH", "LOW", "PANIC"
    alert_severity VARCHAR(50),  -- "INFO", "WARNING", "CRITICAL"
    notified_to_roles VARCHAR[] DEFAULT ARRAY['DOCTOR', 'LAB_SUPERVISOR'],
    notification_sent_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by UUID,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledgement_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- LAB TURNAROUND TIME TRACKING
-- ============================================================================

-- Track turnaround time (TAT) for performance monitoring
CREATE TABLE IF NOT EXISTS public.lab_tat_tracking (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id VARCHAR(100) NOT NULL,
    test_id UUID REFERENCES public.lab_tests(id),
    ordered_at TIMESTAMP WITH TIME ZONE,
    specimen_collected_at TIMESTAMP WITH TIME ZONE,
    specimen_received_at TIMESTAMP WITH TIME ZONE,
    analysis_started_at TIMESTAMP WITH TIME ZONE,
    analysis_completed_at TIMESTAMP WITH TIME ZONE,
    result_verified_at TIMESTAMP WITH TIME ZONE,
    result_reported_at TIMESTAMP WITH TIME ZONE,
    expected_tat_hours INTEGER,
    actual_tat_hours DECIMAL(10, 2),
    tat_met BOOLEAN,
    delays_reasons TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tat_order ON public.lab_tat_tracking(order_id);
CREATE INDEX idx_tat_test ON public.lab_tat_tracking(test_id);

-- ============================================================================
-- VIEWS FOR COMMON QUERIES
-- ============================================================================

-- Current specimen status
CREATE OR REPLACE VIEW public.lab_specimens_current_status_view AS
SELECT
    s.id,
    s.accession_number,
    s.order_id,
    s.patient_id,
    t.name as test_name,
    s.specimen_type,
    s.status,
    s.collected_at,
    s.storage_location,
    CASE
        WHEN s.status = 'REJECTED' THEN 'REJECTED'
        WHEN s.storage_temperature = '2-8C' AND CURRENT_TIMESTAMP > s.received_at + INTERVAL '7 days' THEN 'EXPIRING_SOON'
        WHEN s.storage_temperature = 'Room Temp' AND CURRENT_TIMESTAMP > s.received_at + INTERVAL '1 day' THEN 'EXPIRING_SOON'
        ELSE 'OK'
    END as specimen_quality_status
FROM public.lab_specimens s
LEFT JOIN public.lab_tests t ON s.test_id = t.id
ORDER BY s.collected_at DESC;

-- Outstanding results to be entered
CREATE OR REPLACE VIEW public.lab_pending_results_view AS
SELECT
    lo.id as order_id,
    lo.patient_id,
    lo.priority,
    lo.status,
    count(loi.id) as total_tests,
    count(CASE WHEN lr.id IS NOT NULL THEN 1 END) as results_entered,
    count(loi.id) - count(CASE WHEN lr.id IS NOT NULL THEN 1 END) as pending_tests,
    max(loi.ordered_at) as order_date,
    array_agg(loi.test_name) as test_names
FROM public.lab_orders lo
JOIN public.lab_order_items loi ON lo.id = loi.order_id
LEFT JOIN public.lab_results lr ON loi.id = lr.order_item_id
WHERE lo.status NOT IN ('REPORTED', 'CANCELLED')
GROUP BY lo.id, lo.patient_id, lo.priority, lo.status
ORDER BY lo.priority DESC, lo.created_at ASC;

-- Equipment status summary
CREATE OR REPLACE VIEW public.lab_equipment_status_view AS
SELECT
    e.id,
    e.code,
    e.name,
    e.status,
    e.last_calibration_date,
    e.next_calibration_due,
    CASE
        WHEN e.next_calibration_due < CURRENT_DATE THEN 'OVERDUE'
        WHEN e.next_calibration_due <= CURRENT_DATE + INTERVAL '7 days' THEN 'DUE_SOON'
        ELSE 'OK'
    END as calibration_status,
    e.last_maintenance_date,
    e.next_maintenance_due,
    CASE
        WHEN e.next_maintenance_due < CURRENT_DATE THEN 'OVERDUE'
        WHEN e.next_maintenance_due <= CURRENT_DATE + INTERVAL '7 days' THEN 'DUE_SOON'
        ELSE 'OK'
    END as maintenance_status
FROM public.lab_equipment e
ORDER BY e.name;

-- Reagent expiry alert
CREATE OR REPLACE VIEW public.lab_reagents_expiry_view AS
SELECT
    id,
    code,
    name,
    lot_number,
    expiry_date,
    storage_location,
    quantity,
    CASE
        WHEN expiry_date < CURRENT_DATE THEN 'EXPIRED'
        WHEN expiry_date <= CURRENT_DATE + INTERVAL '30 days' THEN 'EXPIRING_SOON'
        ELSE 'OK'
    END as expiry_status,
    CASE
        WHEN quantity <= reorder_level THEN 'REORDER_NEEDED'
        ELSE 'IN_STOCK'
    END as stock_status
FROM public.lab_reagents
WHERE is_active = TRUE
ORDER BY expiry_date ASC;

-- Critical values summary
CREATE OR REPLACE VIEW public.lab_critical_values_view AS
SELECT
    ca.id,
    ca.order_id,
    ca.patient_id,
    ca.test_name,
    ca.critical_value,
    ca.threshold_type,
    ca.alert_severity,
    ca.created_at,
    ca.acknowledged_by,
    CASE WHEN ca.acknowledged_at IS NULL THEN 'UNACKNOWLEDGED' ELSE 'ACKNOWLEDGED' END as status
FROM public.lab_critical_alerts ca
ORDER BY ca.created_at DESC;

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

ALTER TABLE public.lab_tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.lab_specimens ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.lab_qc_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.lab_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.lab_critical_alerts ENABLE ROW LEVEL SECURITY;

-- Lab data: allow all authenticated users (role checks happen at app level)
CREATE POLICY "Allow authenticated read lab tests"
ON public.lab_tests FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated manage lab tests"
ON public.lab_tests FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Allow authenticated read lab specimens"
ON public.lab_specimens FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated manage lab specimens"
ON public.lab_specimens FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Allow authenticated read lab qc results"
ON public.lab_qc_results FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated manage lab qc results"
ON public.lab_qc_results FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Allow authenticated read lab results ext"
ON public.lab_results FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated manage lab results ext"
ON public.lab_results FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Allow authenticated read lab critical alerts"
ON public.lab_critical_alerts FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow authenticated manage lab critical alerts"
ON public.lab_critical_alerts FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ============================================================================
-- SAMPLE DATA
-- ============================================================================

-- Insert common lab tests
INSERT INTO public.lab_tests (code, name, category, specimen_type, specimen_volume_ml, default_unit, default_reference_range_male, price, loincCode) VALUES
('CBC', 'Complete Blood Count', 'Hematology', 'Whole Blood (EDTA)', 2, 'varies', '4.5-11.0 K/uL (WBC)', 400, '55282-0'),
('FBS', 'Fasting Blood Sugar', 'Biochemistry', 'Plasma (Fluoride)', 2, 'mg/dL', '70-100', 200, '2345-7'),
('RFT', 'Renal Function Tests', 'Biochemistry', 'Serum', 5, 'varies', 'See reference ranges', 350, 'TBD'),
('LFT', 'Liver Function Tests', 'Biochemistry', 'Serum', 5, 'varies', 'See reference ranges', 350, 'TBD'),
('TSH', 'Thyroid Stimulating Hormone', 'Endocrinology', 'Serum', 3, 'mIU/L', '0.4-4.0', 500, '3016-3'),
('HIV', 'HIV Antibody Test', 'Immunology', 'Serum', 3, 'Qualitative', 'Negative', 600, '95165-2'),
('RPR', 'Rapid Plasma Reagin', 'Immunology', 'Serum', 3, 'Qualitative', 'Negative', 300, '5290-7'),
('HBsAg', 'Hepatitis B Surface Antigen', 'Immunology', 'Serum', 3, 'Qualitative', 'Negative', 450, '20485-0'),
('U_Protein', 'Urine Protein', 'Urinalysis', 'Urine', 30, 'mg/L', 'Negative (<10)', 150, '2889-4'),
('Blood_Culture', 'Blood Culture', 'Microbiology', 'Whole Blood', 8, 'Qualitative', 'Negative', 800, '3114-8')
ON CONFLICT (code) DO NOTHING;

-- Insert specimen rejection reasons
INSERT INTO public.lab_rejection_reasons (code, reason, category, severity) VALUES
('HEMOLYZED', 'Hemolyzed specimen', 'Pre-analytical', 'Major'),
('CLOTTED', 'Blood clotted in non-gel tube', 'Pre-analytical', 'Major'),
('INSUFFICIENT', 'Insufficient volume', 'Collection', 'Major'),
('WRONG_TUBE', 'Wrong tube type', 'Collection', 'Major'),
('WRONG_PATIENT', 'Wrong patient label', 'Pre-analytical', 'Major'),
('CONTAMINATED', 'Visibly contaminated', 'Pre-analytical', 'Major'),
('EXPIRED', 'Specimen expired', 'Storage', 'Major'),
('POOR_CONDITION', 'Poor specimen condition', 'Pre-analytical', 'Minor')
ON CONFLICT (code) DO NOTHING;

