-- Reporting pipeline foundation for monthly MOH exports.

create table if not exists public.clinical_register_entries (
  id uuid primary key default gen_random_uuid(),
  encounter_id text not null,
  encounter_date date not null,
  facility text not null,
  department text not null,
  program text not null,
  register_code text not null,
  required_fields_complete boolean not null default false,
  indicator_key text not null,
  indicator_value integer not null default 0,
  created_at timestamptz not null default now()
);

create index if not exists idx_register_entries_month
  on public.clinical_register_entries ((to_char(encounter_date, 'YYYY-MM')));
create index if not exists idx_register_entries_register_code
  on public.clinical_register_entries (register_code);

create or replace view public.reporting_reconciliation_monthly as
select
  to_char(encounter_date, 'YYYY-MM') as report_month,
  facility,
  department,
  program,
  register_code,
  count(distinct encounter_id) as total_encounters,
  count(distinct encounter_id) filter (where required_fields_complete) as completed_encounters,
  count(distinct encounter_id) filter (where not required_fields_complete) as missing_required_fields,
  sum(indicator_value)::int as register_total
from public.clinical_register_entries
group by 1,2,3,4,5;

create or replace view public.moh204_monthly_opd as
select * from public.reporting_reconciliation_monthly where register_code = 'MOH204';

create or replace view public.moh405_monthly_anc as
select * from public.reporting_reconciliation_monthly where register_code = 'MOH405';

create or replace view public.moh333_monthly_maternity as
select * from public.reporting_reconciliation_monthly where register_code = 'MOH333';

create or replace view public.moh361b_monthly_ccc as
select * from public.reporting_reconciliation_monthly where register_code = 'MOH361B';

create or replace view public.moh272_273_monthly_ncd as
select * from public.reporting_reconciliation_monthly where register_code in ('MOH272', 'MOH273');
