-- Persistent key-value Hub settings (e.g. customizable Live Event channel ranges)
create table if not exists public.hub_settings (
  key text primary key,
  value jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

alter table public.hub_settings enable row level security;

insert into public.hub_settings (key, value)
values (
  'live_event_channel_ranges',
  '{"mode":"dashed","mlb":31,"major":30,"ppv":29}'::jsonb
)
on conflict (key) do nothing;
