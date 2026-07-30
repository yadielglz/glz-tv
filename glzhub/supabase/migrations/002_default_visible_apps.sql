update public.devices
set visible_apps = '[
  "com.google.android.youtube.tv",
  "com.netflix.ninja",
  "com.bamnetworks.mobile.android.gameday.atbat",
  "com.android.mgsandroid",
  "com.glztech.radiostream",
  "com.live.geesports",
  "com.cbs.ott",
  "com.disney.disneyplus",
  "com.peacocktv.peacockandroid",
  "com.TWCableTV"
]'::jsonb,
config_version = config_version + 1,
updated_at = now()
where visible_apps = '[]'::jsonb;
