-- Migration 027: Fix event_channels column types and unique constraints
-- 1. Alter buffer columns to NUMERIC so decimal buffers like 1.5 work seamlessly
alter table public.event_channels alter column pre_buffer_hours type numeric using pre_buffer_hours::numeric;
alter table public.event_channels alter column post_buffer_hours type numeric using post_buffer_hours::numeric;

-- 2. Add explicit UNIQUE constraint on tvg_id so PostgREST on_conflict=tvg_id works
alter table public.event_channels drop constraint if exists event_channels_tvg_id_key;
alter table public.event_channels add constraint event_channels_tvg_id_key unique (tvg_id);
