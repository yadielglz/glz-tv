-- Optional labels shown in the GLZ TV header. Blank values keep automatic detection enabled.
alter table public.devices
  add column if not exists custom_connection_label text,
  add column if not exists custom_isp_name text;
