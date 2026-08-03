alter table public.devices
  add column if not exists keep_awake_home boolean not null default false;
