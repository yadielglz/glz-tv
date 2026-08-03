-- Migration 017: recurring device experience schedules
alter table public.devices
  add column if not exists schedule_rules jsonb not null default '{}'::jsonb;
