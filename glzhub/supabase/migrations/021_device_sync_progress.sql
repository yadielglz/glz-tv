alter table public.devices
  add column if not exists sync_status text not null default 'idle',
  add column if not exists sync_progress integer not null default 0,
  add column if not exists sync_message text,
  add column if not exists sync_updated_at timestamptz;

alter table public.devices
  drop constraint if exists devices_sync_status_check,
  drop constraint if exists devices_sync_progress_check;

alter table public.devices
  add constraint devices_sync_status_check
    check (sync_status in ('idle', 'queued', 'syncing', 'complete', 'failed')),
  add constraint devices_sync_progress_check
    check (sync_progress between 0 and 100);
