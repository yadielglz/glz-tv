-- Migration 009: Ecosystem integration for glz-radio and playlist-studio-recovered

-- Playlists schema for Playlist Studio & GLZ TV/Radio
create table if not exists public.playlists (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  description text,
  artwork_url text,
  category text not null default 'general',
  target_app text not null default 'both' check (target_app in ('tv', 'radio', 'both')),
  is_published boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.playlist_items (
  id uuid primary key default gen_random_uuid(),
  playlist_id uuid not null references public.playlists(id) on delete cascade,
  title text not null,
  artist text,
  media_url text not null,
  duration_seconds integer not null default 0,
  position integer not null default 0,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

-- Radio Stations schema for glz-radio streaming engine
create table if not exists public.radio_stations (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  station_code text not null unique,
  name text not null,
  genre text not null default 'Variety',
  stream_url text not null,
  epg_channel_id text,
  logo_url text,
  bitrate integer not null default 128,
  is_active boolean not null default true,
  request_headers jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Station Schedule linkages
create table if not exists public.station_schedules (
  id uuid primary key default gen_random_uuid(),
  station_id uuid not null references public.radio_stations(id) on delete cascade,
  playlist_id uuid not null references public.playlists(id) on delete cascade,
  start_time text not null default '00:00',
  end_time text not null default '23:59',
  days_of_week integer[] not null default '{0,1,2,3,4,5,6}',
  created_at timestamptz not null default now()
);

-- Indexes for performance
create index if not exists playlists_owner_id_idx on public.playlists(owner_id);
create index if not exists playlist_items_playlist_id_idx on public.playlist_items(playlist_id);
create index if not exists radio_stations_owner_id_idx on public.radio_stations(owner_id);
create index if not exists radio_stations_code_idx on public.radio_stations(station_code);
create index if not exists station_schedules_station_id_idx on public.station_schedules(station_id);

-- Row Level Security & Policies
alter table public.playlists enable row level security;
alter table public.playlist_items enable row level security;
alter table public.radio_stations enable row level security;
alter table public.station_schedules enable row level security;

-- Playlists RLS
drop policy if exists "owners can manage playlists" on public.playlists;
create policy "owners can manage playlists"
  on public.playlists for all
  to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

-- Radio Stations RLS
drop policy if exists "owners can manage radio stations" on public.radio_stations;
create policy "owners can manage radio stations"
  on public.radio_stations for all
  to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

-- Allow read grants for authenticated users
grant select, insert, update, delete on public.playlists to authenticated;
grant select, insert, update, delete on public.playlist_items to authenticated;
grant select, insert, update, delete on public.radio_stations to authenticated;
grant select, insert, update, delete on public.station_schedules to authenticated;
