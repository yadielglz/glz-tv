create extension if not exists pgcrypto;

create table if not exists public.enrollments (
  installation_id text primary key,
  pairing_code text not null unique,
  token_hash text not null,
  platform text not null,
  model text not null,
  app_version text not null,
  expires_at timestamptz not null,
  created_at timestamptz not null default now()
);

create table if not exists public.devices (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  installation_id text not null unique,
  token_hash text not null unique,
  name text not null default 'New TV',
  guest_name text not null default 'Guest',
  platform text not null,
  model text not null,
  app_version text not null,
  playlist_url text,
  epg_url text,
  request_headers jsonb not null default '{}'::jsonb,
  visible_apps jsonb not null default '[]'::jsonb,
  theme_mode text not null default 'adaptive',
  weather_location text not null default 'San Juan',
  start_destination text not null default 'Home',
  config_version bigint not null default 1,
  last_seen_at timestamptz,
  last_error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists devices_owner_id_idx on public.devices(owner_id);
create index if not exists devices_token_hash_idx on public.devices(token_hash);
create index if not exists enrollments_pairing_code_idx on public.enrollments(pairing_code);

alter table public.enrollments enable row level security;
alter table public.devices enable row level security;

drop policy if exists "owners can read devices" on public.devices;
create policy "owners can read devices"
  on public.devices for select
  to authenticated
  using (owner_id = auth.uid());

drop policy if exists "owners can update devices" on public.devices;
create policy "owners can update devices"
  on public.devices for update
  to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

revoke all on public.enrollments from anon, authenticated;
grant select, update on public.devices to authenticated;
