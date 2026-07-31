-- Migration 008: App settings parity (OSD timeout, auto update, wifi only) & per-device force refresh
alter table public.devices
  add column if not exists osd_timeout_seconds integer not null default 8,
  add column if not exists auto_update boolean not null default true,
  add column if not exists wifi_only boolean not null default false,
  add column if not exists force_refresh_token text;

alter table public.device_commands drop constraint if exists device_commands_action_check;
alter table public.device_commands add constraint device_commands_action_check check (action in ('install_app', 'force_refresh'));
