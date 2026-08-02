-- Reorder a complete playlist in one database call, avoiding Worker subrequest limits.
create or replace function public.reorder_playlist_items(
  p_owner_id uuid,
  p_playlist_id uuid,
  p_item_ids uuid[]
)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  expected_count integer;
  updated_count integer;
begin
  if not exists (select 1 from public.playlists where id = p_playlist_id and owner_id = p_owner_id) then
    raise exception 'Playlist not found';
  end if;
  select count(*) into expected_count from public.playlist_items where playlist_id = p_playlist_id;
  if cardinality(p_item_ids) <> expected_count
     or (select count(distinct value) from unnest(p_item_ids) value) <> expected_count
     or exists (select 1 from unnest(p_item_ids) value where not exists (
       select 1 from public.playlist_items where id = value and playlist_id = p_playlist_id
     )) then
    raise exception 'Invalid channel order';
  end if;

  update public.playlist_items item
  set position = ordered.position
  from (
    select value as id, ordinality::integer as position
    from unnest(p_item_ids) with ordinality as source(value, ordinality)
  ) ordered
  where item.id = ordered.id and item.playlist_id = p_playlist_id;
  get diagnostics updated_count = row_count;
  return updated_count;
end;
$$;

revoke all on function public.reorder_playlist_items(uuid, uuid, uuid[]) from public, anon, authenticated;
grant execute on function public.reorder_playlist_items(uuid, uuid, uuid[]) to service_role;
