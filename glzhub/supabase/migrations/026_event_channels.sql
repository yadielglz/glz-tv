-- Create event_channels table for temporary sports and live event streams
create table if not exists public.event_channels (
  id uuid primary key default gen_random_uuid(),
  tvg_id text not null,
  tvg_name text,
  title text not null,
  sport_league text not null default 'SPORTS',
  group_title text not null default 'Major League Sports (Events)',
  logo_url text,
  stream_url text not null,
  channel_number text,
  start_time timestamptz not null,
  end_time timestamptz not null,
  pre_buffer_hours integer not null default 2,
  post_buffer_hours integer not null default 2,
  status text not null default 'scheduled' check (status in ('scheduled', 'active', 'expired', 'disabled')),
  auto_ingested boolean not null default true,
  is_online boolean not null default true,
  health_status text not null default 'unknown',
  last_checked_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_event_channels_active 
  on public.event_channels (status, start_time, end_time);

create unique index if not exists idx_event_channels_tvg_id
  on public.event_channels (tvg_id);

alter table public.event_channels add column if not exists is_online boolean not null default true;
alter table public.event_channels add column if not exists health_status text not null default 'unknown';
alter table public.event_channels add column if not exists last_checked_at timestamptz;

