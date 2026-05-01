create table if not exists public.billing_items (
  id uuid primary key default gen_random_uuid(),
  patient_id text not null references public.patients(id) on delete cascade,
  category text not null,
  description text not null,
  quantity numeric(12,2) not null check (quantity > 0),
  unit_price numeric(12,2) not null check (unit_price >= 0),
  total numeric(12,2) not null check (total >= 0),
  status text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.payments (
  id uuid primary key default gen_random_uuid(),
  patient_id text not null references public.patients(id) on delete cascade,
  category text not null,
  amount numeric(12,2) not null check (amount >= 0),
  phone_number text,
  checkout_request_id text,
  receipt_number text,
  status text not null,
  result_code text,
  result_desc text,
  created_at timestamptz not null default now()
);

create index if not exists idx_billing_items_patient_id on public.billing_items(patient_id);
create index if not exists idx_billing_items_status on public.billing_items(status);

create index if not exists idx_payments_patient_id on public.payments(patient_id);
create index if not exists idx_payments_checkout_request_id on public.payments(checkout_request_id);
create index if not exists idx_payments_status on public.payments(status);

alter table public.payments enable row level security;

create policy "Allow hospital roles read payments" on public.payments
for select to authenticated
using (
  (auth.jwt() -> 'app_metadata' ->> 'hospital_role') in ('admin', 'billing', 'finance')
);

create policy "Allow hospital roles insert payments" on public.payments
for insert to authenticated
with check (
  (auth.jwt() -> 'app_metadata' ->> 'hospital_role') in ('admin', 'billing', 'finance')
);

create policy "Allow hospital roles update payments" on public.payments
for update to authenticated
using (
  (auth.jwt() -> 'app_metadata' ->> 'hospital_role') in ('admin', 'billing', 'finance')
)
with check (
  (auth.jwt() -> 'app_metadata' ->> 'hospital_role') in ('admin', 'billing', 'finance')
);
