-- Optional backup stream URL for automatic live event failover
alter table public.event_channels add column if not exists backup_stream_url text;
