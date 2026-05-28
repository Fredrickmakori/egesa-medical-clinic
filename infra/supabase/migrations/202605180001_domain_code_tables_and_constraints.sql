-- Domain code dictionaries
create table if not exists public.code_sex (code text primary key);
insert into public.code_sex(code) values ('male'),('female'),('intersex'),('unknown') on conflict do nothing;

create table if not exists public.code_visit_type (code text primary key);
insert into public.code_visit_type(code) values ('outpatient'),('inpatient'),('emergency'),('follow-up'),('anc') on conflict do nothing;

create table if not exists public.code_disposition (code text primary key);
insert into public.code_disposition(code) values ('admitted'),('discharged'),('transferred'),('referred'),('deceased') on conflict do nothing;

create table if not exists public.code_fetal_presentation (code text primary key);
insert into public.code_fetal_presentation(code) values ('cephalic'),('breech'),('transverse'),('oblique'),('unknown') on conflict do nothing;

create table if not exists public.code_delivery_mode (code text primary key);
insert into public.code_delivery_mode(code) values ('svd'),('assisted-vaginal'),('cesarean'),('vbac') on conflict do nothing;

create table if not exists public.code_delivery_outcome (code text primary key);
insert into public.code_delivery_outcome(code) values ('live-birth'),('still-birth'),('neonatal-death') on conflict do nothing;

create table if not exists public.code_who_stage (code text primary key);
insert into public.code_who_stage(code) values ('stage-1'),('stage-2'),('stage-3'),('stage-4') on conflict do nothing;

create table if not exists public.code_hiv_status (code text primary key);
insert into public.code_hiv_status(code) values ('positive'),('negative'),('unknown'),('exposed') on conflict do nothing;

create table if not exists public.code_adherence_rating (code text primary key);
insert into public.code_adherence_rating(code) values ('good'),('fair'),('poor') on conflict do nothing;

create table if not exists public.code_cohort_status (code text primary key);
insert into public.code_cohort_status(code) values ('active'),('lost-to-follow-up'),('transferred-out'),('deceased'),('stopped') on conflict do nothing;

create table if not exists public.code_muac_status (code text primary key);
insert into public.code_muac_status(code) values ('green'),('yellow'),('red') on conflict do nothing;

create table if not exists public.code_complication (code text primary key);
insert into public.code_complication(code) values ('none'),('fever'),('hemorrhage'),('sepsis'),('eclampsia'),('other') on conflict do nothing;

-- migration-safe legacy mapping for patients.sex
update public.patients
set sex = case lower(trim(sex))
  when 'm' then 'male'
  when 'male' then 'male'
  when 'f' then 'female'
  when 'female' then 'female'
  when 'intersex' then 'intersex'
  else 'unknown'
end;

alter table public.patients
  add constraint patients_sex_code_check
  check (sex in ('male','female','intersex','unknown'));
