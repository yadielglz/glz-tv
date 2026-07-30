alter table public.devices
  add column if not exists captions_enabled boolean not null default false,
  add column if not exists captions_language text not null default 'en',
  add column if not exists auto_start boolean not null default false,
  add column if not exists resume_last_channel boolean not null default true;

create table if not exists public.app_catalog (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  package_name text not null,
  source_type text not null check (source_type in ('play_store', 'repository')),
  source_url text,
  version_name text,
  sha256 text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (owner_id, package_name)
);

create table if not exists public.device_commands (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  device_id uuid not null references public.devices(id) on delete cascade,
  action text not null check (action in ('install_app')),
  payload jsonb not null,
  status text not null default 'pending' check (status in ('pending', 'delivered', 'completed', 'failed')),
  delivered_at timestamptz,
  completed_at timestamptz,
  result_message text,
  created_at timestamptz not null default now()
);

create index if not exists app_catalog_owner_idx on public.app_catalog(owner_id);
create index if not exists device_commands_device_status_idx on public.device_commands(device_id, status, created_at);

alter table public.app_catalog enable row level security;
alter table public.device_commands enable row level security;

drop policy if exists "owners manage app catalog" on public.app_catalog;
create policy "owners manage app catalog" on public.app_catalog
  for all to authenticated using (owner_id = auth.uid()) with check (owner_id = auth.uid());
drop policy if exists "owners read device commands" on public.device_commands;
create policy "owners read device commands" on public.device_commands
  for select to authenticated using (owner_id = auth.uid());

grant select, insert, update, delete on public.app_catalog to authenticated;
grant select on public.device_commands to authenticated;
