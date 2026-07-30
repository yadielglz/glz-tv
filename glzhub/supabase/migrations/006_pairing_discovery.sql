alter table public.enrollments
  add column if not exists request_network_hash text;

create index if not exists enrollments_request_network_hash_idx
  on public.enrollments(request_network_hash);
