-- Allow each GLZ TV device to use one managed TV playlist.
-- A null assignment means all published TV playlists owned by the device owner.

alter table public.devices
  add column if not exists assigned_playlist_id uuid
  references public.playlists(id) on delete set null;

create index if not exists devices_assigned_playlist_id_idx
  on public.devices(assigned_playlist_id);
