-- Device groups and explicit per-channel allow/block/inherit policy.
create table if not exists public.box_groups (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  playlist_id uuid references public.playlists(id) on delete set null,
  default_channel_policy text not null default 'allow' check (default_channel_policy in ('allow', 'block')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.devices add column if not exists box_group_id uuid references public.box_groups(id) on delete set null;
alter table public.devices add column if not exists channel_policy_mode text not null default 'inherit' check (channel_policy_mode in ('inherit', 'allow', 'block'));
create index if not exists devices_box_group_id_idx on public.devices(box_group_id);

create table if not exists public.channel_policy_rules (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  playlist_id uuid not null references public.playlists(id) on delete cascade,
  target_type text not null check (target_type in ('group', 'device')),
  target_id uuid not null,
  playlist_item_id uuid not null references public.playlist_items(id) on delete cascade,
  decision text not null check (decision in ('allow', 'block')),
  created_at timestamptz not null default now(),
  unique (target_type, target_id, playlist_item_id)
);

create index if not exists box_groups_owner_id_idx on public.box_groups(owner_id);
create index if not exists channel_policy_target_idx on public.channel_policy_rules(target_type, target_id);
alter table public.box_groups enable row level security;
alter table public.channel_policy_rules enable row level security;

drop policy if exists "owners can manage box groups" on public.box_groups;
create policy "owners can manage box groups" on public.box_groups for all to authenticated
  using (owner_id = auth.uid()) with check (owner_id = auth.uid());
drop policy if exists "owners can manage channel policies" on public.channel_policy_rules;
create policy "owners can manage channel policies" on public.channel_policy_rules for all to authenticated
  using (owner_id = auth.uid()) with check (owner_id = auth.uid());

grant select, insert, update, delete on public.box_groups to authenticated;
grant select, insert, update, delete on public.channel_policy_rules to authenticated;
