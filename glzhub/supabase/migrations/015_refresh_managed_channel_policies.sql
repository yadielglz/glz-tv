-- Force existing managed boxes to adopt policy-filtered Hub URLs immediately.
update public.devices
set config_version = config_version + 1,
    force_refresh_token = gen_random_uuid()::text
where assigned_playlist_id is not null or box_group_id is not null;
