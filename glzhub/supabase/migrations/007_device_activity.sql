alter table public.devices
  add column if not exists activity_type text not null default 'idle',
  add column if not exists activity_label text,
  add column if not exists activity_package text,
  add column if not exists activity_updated_at timestamptz;

alter table public.devices
  drop constraint if exists devices_activity_type_check;

alter table public.devices
  add constraint devices_activity_type_check
  check (activity_type in ('idle', 'channel', 'app'));
