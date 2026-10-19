-- Hub-managed video-on-demand catalog shown on the GLZ TV home screen.
create table if not exists public.movies (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  stream_url text not null,
  poster_url text,
  backdrop_url text,
  description text,
  release_year integer check (release_year is null or release_year between 1888 and 2200),
  runtime_minutes integer check (runtime_minutes is null or runtime_minutes > 0),
  genres text[] not null default '{}',
  external_id text,
  request_headers jsonb not null default '{}'::jsonb,
  is_active boolean not null default true,
  show_on_home boolean not null default true,
  sort_order integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_movies_owner_home
  on public.movies (owner_id, show_on_home, is_active, sort_order, created_at);

alter table public.movies enable row level security;

drop policy if exists "Owners manage movies" on public.movies;
create policy "Owners manage movies" on public.movies
  for all using (auth.uid() = owner_id) with check (auth.uid() = owner_id);

grant select, insert, update, delete on public.movies to authenticated;

alter table public.devices drop constraint if exists devices_activity_type_check;
alter table public.devices add constraint devices_activity_type_check
  check (activity_type in ('idle', 'channel', 'radio', 'sports_bar', 'app', 'movie'));
