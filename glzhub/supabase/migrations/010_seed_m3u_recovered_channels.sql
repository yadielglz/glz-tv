-- Seed data for M3U Channels & Radio Streams from playlist-studio-recovered backup

DO $$
DECLARE
  v_owner_id uuid;
  v_playlist_id uuid;
BEGIN
  SELECT id INTO v_owner_id FROM auth.users ORDER BY created_at ASC LIMIT 1;
  IF v_owner_id IS NULL THEN
    RAISE NOTICE 'No user found';
    RETURN;
  END IF;

  SELECT id INTO v_playlist_id FROM public.playlists WHERE owner_id = v_owner_id AND title = $str$GLZ TV Lineup$str$ LIMIT 1;

  IF v_playlist_id IS NULL THEN
    INSERT INTO public.playlists (owner_id, title, description, category, target_app, is_published)
    VALUES (v_owner_id, $str$GLZ TV Lineup$str$, $str$Recovered M3U channel stream lineup$str$, $str$Live TV$str$, $str$tv$str$, true)
    RETURNING id INTO v_playlist_id;

  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WKAQ-TV | TELEMUNDO$str$, $str$https://nbculocallive.akamaized.net/hls/live/2037499/puertorico/stream1/master_1080.m3u8$str$, -1, 1, $str${"tvg_id":"WKAQ.us","tvg_chno":"2","tvg_logo":"https://static.epg.best/us/WKAQ.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WLII-TV | TELEONCE$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/uni.wlii.puerto.rico.us.m3u8$str$, -1, 2, $str${"tvg_id":"WLII.us","tvg_chno":"11","tvg_logo":"https://static.epg.best/us/WLII.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WKAQ-DT | PUNTO2$str$, $str$https://nbculocallive.akamaized.net/hls/live/2037499/puertorico/stream2/master_720.m3u8$str$, -1, 3, $str${"tvg_id":"WKAQDT2.us","tvg_chno":"3","tvg_logo":"https://static.epg.best/us/WKAQDT2.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WIPR-TV | PUERTO RICO TV$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/wipr.pr.m3u8$str$, -1, 4, $str${"tvg_id":"WIPRTV.us","tvg_chno":"6","tvg_logo":"https://static.epg.best/us/WIPRTV.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$CNN HD$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cnn.us.m3u8$str$, -1, 5, $str${"tvg_id":"CNN.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/CNN.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Fox News HD$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/fox.news.us.m3u8$str$, -1, 6, $str${"tvg_id":"FoxNews.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/FoxNews.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$C-SPAN$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cspan.us.m3u8$str$, -1, 7, $str${"tvg_id":"CSPAN.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/CSPAN.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$CSPAN 2$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/c.span.2.us.m3u8$str$, -1, 8, $str${"tvg_id":"CSPAN2.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/CSPAN2.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WEATHER HD$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/the.weather.channel.us.m3u8$str$, -1, 9, $str${"tvg_id":"WeatherChannel.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WeatherChannel.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WEATHER NATION$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/weathernation.us.m3u8$str$, -1, 10, $str${"tvg_id":"WeatherNation.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WeatherNation.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$ACCUWEATHER$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/accuweather.us.m3u8$str$, -1, 11, $str${"tvg_id":"Accuweather.us","tvg_chno":null,"tvg_logo":"https://www.logo.wine/a/logo/AccuWeather/AccuWeather-Logo.wine.svg","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$ESPN HD$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/espn.us.m3u8$str$, -1, 12, $str${"tvg_id":"ESPN.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/ESPN.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$ESPN 2 HD$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/espn2.us.m3u8$str$, -1, 13, $str${"tvg_id":"ESPN2.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/ESPN2.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$ESPN News HD$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/espn.news.us.m3u8$str$, -1, 14, $str${"tvg_id":"ESPNNews.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/ESPNNews.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$ESPN U HD$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/espn.u.us.m3u8$str$, -1, 15, $str${"tvg_id":"ESPNU.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/ESPNU.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$NFL Network$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/nfl.network.us.m3u8$str$, -1, 16, $str${"tvg_id":"NFLNetwork.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/NFLNetwork.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$NBA TV$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/nba.tv.usa.us.m3u8$str$, -1, 17, $str${"tvg_id":"NBATV.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/NBATV.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Comedy Central$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/comedy.central.eastern.us.m3u8$str$, -1, 18, $str${"tvg_id":"ComedyCentral.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/ComedyCentral.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$USA Network$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/usa.network.east.us.m3u8$str$, -1, 19, $str${"tvg_id":"USANetwork.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/USANetwork.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$TBS$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/tbs.east.us.m3u8$str$, -1, 20, $str${"tvg_id":"TBS.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/TBS.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Paramount Network$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/paramount.network.usa.eastern.us.m3u8$str$, -1, 21, $str${"tvg_id":"ParamountNetwork.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/ParamountNetwork.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$TNT$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/tnt.east.us.m3u8$str$, -1, 22, $str${"tvg_id":"TNT.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/TNT.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Disney Channel$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/disney.eastern.us.m3u8$str$, -1, 23, $str${"tvg_id":"DisneyChannel.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/DisneyChannel.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Disney Junior$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/disney.junior.usa.east.us.m3u8$str$, -1, 24, $str${"tvg_id":"DisneyJunior.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/DisneyJunior.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Nickelodeon$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/nickelodeon.usa.east.us.m3u8$str$, -1, 25, $str${"tvg_id":"Nickelodeon.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/Nickelodeon.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Cartoon Network$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cartoon.network.usa.eastern.us.m3u8$str$, -1, 26, $str${"tvg_id":"CartoonNetwork.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/CartoonNetwork.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Boomerang$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/boomerang.us.m3u8$str$, -1, 27, $str${"tvg_id":"Boomerang.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/Boomerang.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$FX Network$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/fx.networks.east.coast.us.m3u8$str$, -1, 28, $str${"tvg_id":"FX.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/FX.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$FXX$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/fxx.usa.eastern.us.m3u8$str$, -1, 29, $str${"tvg_id":"FXX.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/FXX.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$FX Movie$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/fx.movie.channel.us.m3u8$str$, -1, 30, $str${"tvg_id":"FXMovieChannel.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/FXMovieChannel.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Court TV$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/court.tv.network.us.m3u8$str$, -1, 31, $str${"tvg_id":"CourtTV.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/CourtTV.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$AMC$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/amc.eastern.us.m3u8$str$, -1, 32, $str${"tvg_id":"AMC.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/AMC.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$POP$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/pop.east.us.m3u8$str$, -1, 33, $str${"tvg_id":"PopTV.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/PopTV.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Tru TV$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/trutv.usa.east.us.m3u8$str$, -1, 34, $str${"tvg_id":"truTV.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/truTV.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$ORLANDO FLORIDA$str$, $str$https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8$str$, -1, 35, $str${"tvg_id":"banner.orl","tvg_chno":null,"tvg_logo":"https://www.orlando.gov/files/sharedassets/public/v/3/documents/assets-official/cityoforlando_horizontal_logo_official.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WESH-TV | NBC 2 ORLANDO$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/nbc.wesh.winter.park.fl.us.m3u8$str$, -1, 36, $str${"tvg_id":"WESH.us","tvg_chno":"102","tvg_logo":"https://static.epg.best/us/WESH.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WKMG-TV | CBS 6 ORLANDO$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cbs.wkmg.orlando.fl.us.m3u8$str$, -1, 37, $str${"tvg_id":"WKMG.us","tvg_chno":"106","tvg_logo":"https://static.epg.best/us/WKMG.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WFTV-TV | ABC 9 ORLANDO$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/abc.wftv.orlando.fl.us.m3u8$str$, -1, 38, $str${"tvg_id":"WFTV.us","tvg_chno":"109","tvg_logo":"https://static.epg.best/us/WFTV.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WKCF-TV | CW 18 ORLANDO$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cw.wkcf.orlando.fl.us.m3u8$str$, -1, 39, $str${"tvg_id":"WKCF.us","tvg_chno":"118","tvg_logo":"https://upload.wikimedia.org/wikipedia/commons/thumb/7/7b/WKCF_logo_2024.svg/960px-WKCF_logo_2024.svg.png?_=20240627133852","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$WOFL-TV | FOX 35 ORLANDO$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/fox.wofl.orlando.fl.us.m3u8$str$, -1, 40, $str${"tvg_id":"WOFL.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WOFL.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$TeleAmazonas EC$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/teleamazonas.lat.m3u8$str$, -1, 41, $str${"tvg_id":"TeleAmazonas.ec","tvg_chno":null,"tvg_logo":"https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Teleamazonas_Logo.png/960px-Teleamazonas_Logo.png?_=20211105181900","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Antena3$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/antena.3.es.m3u8$str$, -1, 42, $str${"tvg_id":"Antena3.es","tvg_chno":null,"tvg_logo":"https://static.epg.best/es/Antena3.es.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Telecinco España$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/telecinco.es.m3u8$str$, -1, 43, $str${"tvg_id":"Telecinco.es","tvg_chno":null,"tvg_logo":"https://static.epg.best/es/Telecinco.es.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$CNN En Español$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cnn.en.espanol.mx.m3u8$str$, -1, 44, $str${"tvg_id":"CNNEspanol.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/CNNEspanol.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$CNN Colombia$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cnn.espanol.co.m3u8$str$, -1, 45, $str${"tvg_id":"CNNInt.es","tvg_chno":null,"tvg_logo":"https://pbs.twimg.com/profile_images/1641061875067527168/-dVP40Zv.jpg","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$CNN International$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cnn.international.north.america.us.m3u8$str$, -1, 46, $str${"tvg_id":"CNNInternational.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/CNNInternational.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Discovery En Español$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/discovery.en.espanol.us.m3u8$str$, -1, 47, $str${"tvg_id":"DiscoveryEspanol.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/DiscoveryEspanol.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Weather Network Canada$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/weather.network.ca.m3u8$str$, -1, 48, $str${"tvg_id":"TheWeatherNetwork.ca","tvg_chno":null,"tvg_logo":"https://static.epg.best/ca/TheWeatherNetwork.ca.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Discovery Familia$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/discovery.familia.us.m3u8$str$, -1, 49, $str${"tvg_id":"DiscoveryFamilia.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/DiscoveryFamilia.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Discovery Turbo$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/discovery.turbo.co.m3u8$str$, -1, 50, $str${"tvg_id":"DiscoveryTurboLatinAmerica.pa","tvg_chno":null,"tvg_logo":"https://static.epg.best/pa/DiscoveryTurboLatinAmerica.pa.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Discovery World$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/discovery.world.co.m3u8$str$, -1, 51, $str${"tvg_id":"DiscoveryWorld.mx","tvg_chno":null,"tvg_logo":"https://static.epg.best/mx/DiscoveryWorld.mx.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Unimás$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/unimas.network.east.us.m3u8$str$, -1, 52, $str${"tvg_id":"UniMas.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/UniMas.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$HBO 2$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/hbo.2.eastern.us.m3u8$str$, -1, 53, $str${"tvg_id":"HBO2LatinAmerica.pa","tvg_chno":null,"tvg_logo":"https://static.epg.best/pa/HBO2LatinAmerica.pa.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$CBS Miami [WFOR]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cbs.wfor.miami.fl.us.m3u8$str$, -1, 54, $str${"tvg_id":"WFOR.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WFOR.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$NBC Miami [WTVJ]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/nbc.wtvj.miami.fl.us.m3u8$str$, -1, 55, $str${"tvg_id":"WTVJ.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WTVJ.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$FOX Miami [WSVN]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/fox.wsvn.miami.fl.us.m3u8$str$, -1, 56, $str${"tvg_id":"WSVNDT1.us","tvg_chno":null,"tvg_logo":"https://en-academic.com/pictures/enwiki/49/125px-WSVN.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$ABC Miami [WPLG]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/abc.wplg.miami.fl.us.m3u8$str$, -1, 57, $str${"tvg_id":"WPLG.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WPLG.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Univisión Miami [WLTV]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/uni.wltv.miami.fl.us.m3u8$str$, -1, 58, $str${"tvg_id":"WLTV.us","tvg_chno":null,"tvg_logo":"https://upload.wikimedia.org/wikipedia/commons/thumb/e/e9/Univision_23_%282019%29.svg/960px-Univision_23_%282019%29.svg.png?_=20250627022323","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$My Network 33 [WBFS]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/mnt.wbfs.miami.fl.us.m3u8$str$, -1, 59, $str${"tvg_id":"WBFS.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WBFS.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$NBC Tampa [WFLA]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/nbc.wfla.tampa.bay.fl.us.m3u8$str$, -1, 60, $str${"tvg_id":"WFLA.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WFLA.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$CBS Tampa [WTSP]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cbs.wtsp.tampa.bay.fl.us.m3u8$str$, -1, 61, $str${"tvg_id":"WTSP.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WTSP.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$FOX Tampa [WTVT]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/fox.wtvt.tampa.bay.fl.us.m3u8$str$, -1, 62, $str${"tvg_id":"WTVT.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WTVT.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$ABC Tampa [WFTS]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/abc.wfts.tampa.bay.fl.us.m3u8$str$, -1, 63, $str${"tvg_id":"WFTS.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WFTS.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$Telemundo Tampa [WRMD]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/telemundo.wrmd.tampa.fl.us.m3u8$str$, -1, 64, $str${"tvg_id":"WRMDCD.us","tvg_chno":null,"tvg_logo":"https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Telemundo49.svg/250px-Telemundo49.svg.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$ABC Atlanta [WSB]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/abc.wsb.atlanta.ga.us.m3u8$str$, -1, 65, $str${"tvg_id":"WSB.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WSB.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$FOX Atlanta [WAGA]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/fox.waga.atlanta.ga.us.m3u8$str$, -1, 66, $str${"tvg_id":"WAGA.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WAGA.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$CBS Atlanta$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/cw.wupa.atlanta.ga.us.m3u8$str$, -1, 67, $str${"tvg_id":"WUPA.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WUPA.us.png","radio":false}$str$::jsonb);
  INSERT INTO public.playlist_items (playlist_id, title, media_url, duration_seconds, position, metadata)
  VALUES (v_playlist_id, $str$NBC Atlanta [WXIA]$str$, $str$https://tvnow.best/api/stream/mygbb8/167848/livetv.epg/nbc.wxia.atlanta.ga.us.m3u8$str$, -1, 68, $str${"tvg_id":"WXIA.us","tvg_chno":null,"tvg_logo":"https://static.epg.best/us/WXIA.us.png","radio":false}$str$::jsonb);
  END IF;

  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WORO$str$, $str$FM 92.5 | RADIO ORO$str$, $str$FM Radio$str$, $str$https://us2.internet-radio.com/proxy/woro?mp=/stream$str$, $str$https://i.ibb.co/nqbSmS1M/worofm-processed.png$str$, $str$radio.woro$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WZNT$str$, $str$FM 93.7 | Z 93$str$, $str$FM Radio$str$, $str$https://liveaudio.lamusica.com/PR_WZNT_icy$str$, $str$https://i.ibb.co/23BMsKBY/wznt-png.png$str$, $str$radio.wznt$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WODA$str$, $str$FM 94.7 | LA NUEVA 94$str$, $str$FM Radio$str$, $str$https://liveaudio.lamusica.com/PR_WODA_icy$str$, $str$https://i.ibb.co/sv1RBc08/wodalogo.png$str$, $str$radio.woda$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WFID$str$, $str$FM 95.7 | FIDELITY$str$, $str$FM Radio$str$, $str$https://server7.servistreaming.com/proxy/fidelity?mp=%2Fstream%3Ftype%3D.mp3&_=1$str$, $str$https://fidelitypr.com/wp-content/uploads/2025/09/cropped-Untitled-design-45.png$str$, $str$radio.wfid$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WRXD$str$, $str$FM 96.5 | ESTEREOTEMPO$str$, $str$FM Radio$str$, $str$https://liveaudio.lamusica.com/PR_WRXD_icy$str$, $str$https://i.ibb.co/F4GM0W81/wrxr.png$str$, $str$radio.wrxd$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WOYE$str$, $str$FM 97.3 | MAGIC$str$, $str$FM Radio$str$, $str$https://stream.eleden.com:8210/magic.aac$str$, $str$https://i.ibb.co/Z6WqXPzV/woye.png$str$, $str$radio.woye$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WPRM$str$, $str$FM 99.1 | SALSOUL$str$, $str$FM Radio$str$, $str$https://server20.servistreaming.com:9023/stream?type=.mp3&_=1$str$, $str$https://i.iheart.com/v3/catalog/live/8544?ops=ratio%281%2C1%29%2Cscale%28164%2C0%29&cacheable=true$str$, $str$radio.wprm$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WXYX$str$, $str$FM 100.7 | LA X$str$, $str$FM Radio$str$, $str$https://stream.eleden.com:8230/La X.aac$str$, $str$https://i.ibb.co/zWDcRnBw/laxpng.png$str$, $str$radio.wxyx$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WTOK$str$, $str$FM 102.5 | HOT102$str$, $str$FM Radio$str$, $str$https://server7.servistreaming.com/proxy/hot?mp=%2Fstream%3Ftype%3D.mp3&_=1$str$, $str$https://cdn-profiles.tunein.com/s29490/images/logod.png?t=638986500070000000$str$, $str$radio.wtok$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WKAQFM$str$, $str$FM 104.7 | KQ105$str$, $str$FM Radio$str$, $str$https://televicentro.streamguys1.com/wkaqfm-icy?key=ae6a3b84b2caabf9d96d28dc1d8e3ebc2cc0ecce9ef074936108cb8cccf7964d&source=tunein&source=TuneIn&gdpr=0&us_privacy=1YNY&bundle=tunein.com&lat=28.0699&long=-81.8107$str$, $str$https://i.iheart.com/v3/catalog/live/5177?ops=ratio%281%2C1%29%2Cscale%28164%2C0%29&cacheable=true$str$, $str$radio.wkaqfm$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WMEG$str$, $str$FM 106.9 | LA MEGA$str$, $str$FM Radio$str$, $str$https://liveaudio.lamusica.com/PR_WMEG_icy$str$, $str$https://i.ibb.co/Xrp2nhpQ/WMEG-PNG.png$str$, $str$radio.wmeg$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WKAQAM$str$, $str$AM 580 | WKAQ AM$str$, $str$AM Radio$str$, $str$https://televicentro.streamguys1.com/wkaqqam-icy?key=ae6a3b84b2caabf9d96d28dc1d8e3ebc2cc0ecce9ef074936108cb8cccf7964d&source=tunein&source=TuneIn&gdpr=0&us_privacy=1YNY&bundle=tunein.com&lat=28.0699&long=-81.8107$str$, $str$https://bloximages.chicago2.vip.townnews.com/wkaq580.com/content/tncms/custom/image/9732c65a-a5bb-11ee-8102-67d137cd6b72.png?resize=400%2C167$str$, $str$radio.wkaqam$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WUNO$str$, $str$AM 630 | NOTIUNO$str$, $str$AM Radio$str$, $str$https://server20.servistreaming.com:9022/stream$str$, $str$https://i.iheart.com/v3/catalog/live/8542?ops=ratio%281%2C1%29%2Cscale%28164%2C0%29&cacheable=true$str$, $str$radio.wuno$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WMSW$str$, $str$AM 1120 |  RADIO ONCE$str$, $str$AM Radio$str$, $str$http://whsh4u-panel.com:14167/stream$str$, $str$https://cdn-radiotime-logos.tunein.com/s21253d.png$str$, $str$radio.wmsw$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WSKN$str$, $str$AM 1320 | RADIO ISLA$str$, $str$AM Radio$str$, $str$https://server7.servistreaming.com/proxy/RadioIsla?mp=%2Fstream%3Ftype%3D.mp3&_=1$str$, $str$https://cdn-profiles.tunein.com/s22835/images/logod.png?t=637003437830000000$str$, $str$radio.wskn$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WNEL$str$, $str$AM 1430 | RADIO TIEMPO$str$, $str$AM Radio$str$, $str$https://server7.servistreaming.com/proxy/tiempo?mp=%2Fstream%3Ftype%3D.mp3&_=1$str$, $str$https://www.nicepng.com/png/detail/264-2646242_radio-tiempo-png-radio-tiempo-logo.png$str$, $str$radio.wnel$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_WKUM$str$, $str$AM 1470 | RADIO CUMBRE$str$, $str$AM Radio$str$, $str$https://sp.unoredcdn.net/8158/stream/1/$str$, $str$https://i.ibb.co/5g2402Cc/wkum-png.png$str$, $str$radio.wkum$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_SALSEO$str$, $str$ONLINE | SALSEO RADIO$str$, $str$Online Radio$str$, $str$https://listen.radioking.com/radio/399811/stream/452110$str$, $str$https://cdn-profiles.tunein.com/s201197/images/logod.png?t=639088402270000000$str$, $str$radio.salseo$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_LAT99$str$, $str$ONLINE | LATINO 99$str$, $str$Online Radio$str$, $str$https://lmmradiocast.com/Latino99fm?_=68068$str$, $str$https://mm.aiircdn.com/371/5928f28889f51.png$str$, $str$radio.lat99$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
  INSERT INTO public.radio_stations (owner_id, station_code, name, genre, stream_url, logo_url, epg_channel_id)
  VALUES (v_owner_id, $str$RADIO_LAVIEJAZ$str$, $str$ONLINE | LA VIEJA Z$str$, $str$Online Radio$str$, $str$https://s2.free-shoutcast.com/stream/18006$str$, $str$https://i.ibb.co/d4VZVjj2/LVZ8-removebg-preview.png$str$, $str$radio.laviejaz$str$)
  ON CONFLICT (station_code) DO UPDATE SET stream_url = EXCLUDED.stream_url, logo_url = EXCLUDED.logo_url;
END $$;
