-- Managed XMLTV guides imported or fetched in GLZ Hub.
create table if not exists public.epg_guides (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  playlist_id uuid not null unique references public.playlists(id) on delete cascade,
  name text not null default 'TV Guide',
  source_url text,
  xml_content text not null,
  channel_count integer not null default 0,
  programme_count integer not null default 0,
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  constraint epg_guides_source_url_https check (source_url is null or source_url ~ '^https://')
);

create index if not exists epg_guides_owner_id_idx on public.epg_guides(owner_id);
alter table public.epg_guides enable row level security;
drop policy if exists "owners can manage epg guides" on public.epg_guides;
create policy "owners can manage epg guides" on public.epg_guides for all to authenticated
  using (owner_id = auth.uid()) with check (owner_id = auth.uid());
grant select, insert, update, delete on public.epg_guides to authenticated;
