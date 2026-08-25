-- Prevent overlapping weekly XMLTV refreshes from exhausting Worker and database resources.
alter table public.epg_guides
  add column if not exists refresh_started_at timestamptz,
  add column if not exists refresh_error text;
