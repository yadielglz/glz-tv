-- Hub controls availability; the television keeps the staff-selected sources.
alter table public.devices
  add column if not exists sports_bar_kiosk_enabled boolean not null default false;

alter table public.devices
  drop constraint if exists devices_activity_type_check;

alter table public.devices
  add constraint devices_activity_type_check
  check (activity_type in ('idle', 'channel', 'radio', 'sports_bar', 'app'));
