create table if not exists public.sites (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  address text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (owner_id, name)
);

alter table public.sites enable row level security;

drop policy if exists "owners manage sites" on public.sites;
create policy "owners manage sites" on public.sites
  for all to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

grant select, insert, update, delete on public.sites to authenticated;

insert into public.sites (owner_id, name)
select owners.owner_id, coalesce(nullif(profile.property_name, ''), 'Default Property')
from (
  select owner_id from public.devices
  union
  select owner_id from public.guest_experience_profiles
) owners
left join public.guest_experience_profiles profile on profile.owner_id = owners.owner_id
where not exists (
  select 1 from public.sites site where site.owner_id = owners.owner_id
);

alter table public.devices
  add column if not exists site_id uuid references public.sites(id) on delete set null;

create index if not exists devices_site_id_idx on public.devices(site_id);

update public.devices device
set site_id = (
  select site.id from public.sites site
  where site.owner_id = device.owner_id
  order by site.created_at
  limit 1
)
where device.site_id is null;

alter table public.guest_experience_profiles
  add column if not exists site_id uuid references public.sites(id) on delete cascade;

update public.guest_experience_profiles profile
set site_id = (
  select site.id from public.sites site
  where site.owner_id = profile.owner_id
  order by site.created_at
  limit 1
)
where profile.site_id is null;

alter table public.guest_experience_profiles
  drop constraint if exists guest_experience_profiles_pkey;

alter table public.guest_experience_profiles
  alter column site_id set not null;

alter table public.guest_experience_profiles
  add constraint guest_experience_profiles_pkey primary key (site_id);

create index if not exists guest_experience_owner_idx
  on public.guest_experience_profiles(owner_id);
