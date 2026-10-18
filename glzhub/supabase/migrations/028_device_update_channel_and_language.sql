-- Migration 028: Device update channel (5-ring OTA model), app language parity, and remote update check command
alter table public.devices
  add column if not exists update_channel text not null default 'production',
  add column if not exists app_language text not null default 'en';

alter table public.device_commands
  drop constraint if exists device_commands_action_check;

alter table public.device_commands
  add constraint device_commands_action_check
  check (action in ('install_app', 'force_refresh', 'check_update'));
