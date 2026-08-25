-- Large weekly guides belong in object storage. PostgreSQL retains searchable metadata only.
alter table public.epg_guides
  add column if not exists object_key text,
  alter column xml_content drop not null;
