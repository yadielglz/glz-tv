alter table public.devices
  add column if not exists room_number text,
  add column if not exists arrival_date date,
  add column if not exists departure_date date;

create table if not exists public.guest_experience_profiles (
  owner_id uuid primary key references auth.users(id) on delete cascade,
  property_name text not null default 'GLZ Hotel',
  welcome_message text not null default 'Relax, explore, and enjoy your stay.',
  logo_url text,
  hero_image_url text,
  wifi_name text,
  wifi_instructions text,
  checkout_time text,
  front_desk text,
  notice_title text,
  notice_body text,
  services jsonb not null default '[]'::jsonb,
  updated_at timestamptz not null default now()
);

alter table public.guest_experience_profiles enable row level security;

drop policy if exists "owners manage guest experience" on public.guest_experience_profiles;
create policy "owners manage guest experience" on public.guest_experience_profiles
  for all to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

grant select, insert, update, delete on public.guest_experience_profiles to authenticated;
