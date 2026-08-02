-- Migration 018: admin-selected channel preview on the TV Home screen
alter table public.devices
  add column if not exists home_preview_channel_id text;
