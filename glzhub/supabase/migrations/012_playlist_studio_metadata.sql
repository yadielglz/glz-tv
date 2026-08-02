-- Playlist Studio publishing and EPG metadata.

alter table public.playlists
  add column if not exists epg_url text;

alter table public.playlists
  add constraint playlists_epg_url_https
  check (epg_url is null or epg_url ~ '^https://');

create or replace function public.replace_playlist_items(
  p_owner_id uuid,
  p_playlist_id uuid,
  p_items jsonb
)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  inserted_count integer;
begin
  if not exists (
    select 1 from public.playlists
    where id = p_playlist_id and owner_id = p_owner_id
  ) then
    raise exception 'Playlist not found';
  end if;
  if jsonb_typeof(p_items) <> 'array' or jsonb_array_length(p_items) = 0 or jsonb_array_length(p_items) > 2000 then
    raise exception 'Invalid M3U channel list';
  end if;

  delete from public.playlist_items where playlist_id = p_playlist_id;
  insert into public.playlist_items (
    playlist_id, title, media_url, duration_seconds, position, metadata
  )
  select
    p_playlist_id,
    item->>'title',
    item->>'media_url',
    coalesce((item->>'duration_seconds')::integer, -1),
    coalesce((item->>'position')::integer, ordinality::integer),
    coalesce(item->'metadata', '{}'::jsonb)
  from jsonb_array_elements(p_items) with ordinality as source(item, ordinality);

  get diagnostics inserted_count = row_count;
  return inserted_count;
end;
$$;

revoke all on function public.replace_playlist_items(uuid, uuid, jsonb) from public, anon, authenticated;
grant execute on function public.replace_playlist_items(uuid, uuid, jsonb) to service_role;
