alter table public.devices
  drop constraint if exists devices_activity_type_check;

alter table public.devices
  add constraint devices_activity_type_check
  check (activity_type in ('idle', 'channel', 'radio', 'app'));
