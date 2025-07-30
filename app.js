// GLZ TV - Modern STB App Logic
// Spectrum/AT&T inspired, modular, and maintainable
// Version: 2024-01-27-v3 - Reverted stream testing and CORS code, restored original playback

// --- Embedded M3U Content (move this to a separate file or fetch from server for production) ---
const EMBEDDED_M3U = `#EXTM3U url-tvg="https://epgshare01.online/epgshare01/epg_ripper_US_LOCALS2.xml.gz" x-tvg-url="https://epgshare01.online/epgshare01/epg_ripper_US_LOCALS2.xml.gz"
#EXTINF:-1 tvg-id="WKAQ.us" tvg-name="WKAQ   TELEMUNDO PR" tvg-logo="https://i.ibb.co/gLVK5Swz/TEL.png" tvg-chno="102" channel-id="102" group-title="TV",WKAQ   TELEMUNDO PR
https://nbculocallive.akamaized.net/hls/live/2037499/puertorico/stream1/master_1080.m3u8
#EXTINF:-1 tvg-id="WKAQDT2.us" tvg-name="WKAQ2   PUNTO 2" tvg-logo="https://static.epg.best/us/WKAQDT2.us.png" tvg-chno="103" channel-id="103" group-title="TV",WKAQ2   PUNTO 2
https://nbculocallive.akamaized.net/hls/live/2037499/puertorico/stream2/master_720.m3u8
#EXTINF:-1 tvg-id="UniMas.us" tvg-name="UNIMAS HD" tvg-logo="https://static.epg.best/us/UniMas.us.png" tvg-chno="104" channel-id="104" group-title="TV",UNIMAS HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/unimas.network.east.us.m3u8
#EXTINF:-1 tvg-id="WLII.us" tvg-name="WLII   TELEONCE" tvg-logo="https://static.epg.best/us/WLII.us.png" tvg-chno="105" channel-id="105" group-title="TV",WLII   TELEONCE
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/uni.wlii.puerto.rico.us.m3u8
#EXTINF:-1 tvg-id="WIPRTV.us" tvg-name="WIPR   PUERTO RICO TV" tvg-logo="https://static.epg.best/us/WIPRTV.us.png" tvg-chno="106" channel-id="106" group-title="TV",WIPR   PUERTO RICO TV
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/wipr.pr.m3u8
#EXTINF:-1 tvg-id="ESPN.us" tvg-name="ESPN HD" tvg-logo="https://i.ibb.co/mrs1mDXw/ESPN-png.png" tvg-chno="110" channel-id="110" group-title="TV",ESPN HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/espn.us.m3u8
#EXTINF:-1 tvg-id="ESPN2.us" tvg-name="ESPN 2 HD" tvg-logo="https://static.epg.best/us/ESPN2.us.png" tvg-chno="111" channel-id="111" group-title="TV",ESPN 2 HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/espn2.us.m3u8
#EXTINF:-1 tvg-id="ESPNNews.us" tvg-name="ESPN NEWS HD" tvg-logo="https://static.epg.best/us/ESPNNews.us.png" tvg-chno="112" channel-id="112" group-title="TV",ESPN NEWS HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/espn.news.us.m3u8
#EXTINF:-1 tvg-id="ESPNU.us" tvg-name="ESPN U HD" tvg-logo="https://static.epg.best/us/ESPNU.us.png" tvg-chno="113" channel-id="113" group-title="TV",ESPN U HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/espn.u.us.m3u8
#EXTINF:-1 tvg-id="FoxSports1.us" tvg-name="FOX SPORTS 1" tvg-logo="https://i.ibb.co/0pqwYytp/FS1.png" tvg-chno="114" channel-id="114" group-title="TV",FOX SPORTS 1
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.sports.1.us.m3u8
#EXTINF:-1 tvg-id="FoxSports2.us" tvg-name="FOX SPORTS 2" tvg-logo="https://i.ibb.co/PGYdrN3r/FS2.png" tvg-chno="115" channel-id="115" group-title="TV",FOX SPORTS 2
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.sports.2.us.m3u8
#EXTINF:-1 tvg-id="MLBNetwork.us" tvg-name="MLB NETWORK" tvg-logo="https://static.epg.best/us/MLBNetwork.us.png" tvg-chno="116" channel-id="116" group-title="TV",MLB NETWORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/mlb.network.us.m3u8
#EXTINF:-1 tvg-id="NFLNetwork.us" tvg-name="NFL NETWORK" tvg-logo="https://static.epg.best/us/NFLNetwork.us.png" tvg-chno="117" channel-id="117" group-title="TV",NFL NETWORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nfl.network.us.m3u8
#EXTINF:-1 tvg-id="NBATV.us" tvg-name="NBA TV" tvg-logo="https://static.epg.best/us/NBATV.us.png" tvg-chno="118" channel-id="118" group-title="TV",NBA TV
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nba.tv.usa.us.m3u8
#EXTINF:-1 tvg-id="YESNetwork.us" tvg-name="YES NETWORK" tvg-logo="https://static.epg.best/us/YESNetwork.us.png" tvg-chno="119" channel-id="119" group-title="TV",YES NETWORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/yes.network.us.m3u8
#EXTINF:-1 tvg-id="SportsNetNewYork.us" tvg-name="SPORTSNET NEW YORK" tvg-logo="https://static.epg.best/us/SportsNetNewYork.us.png" tvg-chno="120" channel-id="120" group-title="TV",SPORTSNET NEW YORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/sny.sportsnet.new.york.us.m3u8
#EXTINF:-1 tvg-id="WESH.us" tvg-name="WESH   NBC ORLANDO" tvg-logo="https://static.epg.best/us/WESH.us.png" tvg-chno="121" channel-id="121" group-title="TV",WESH   NBC ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nbc.wesh.winter.park.fl.us.m3u8
#EXTINF:-1 tvg-id="WKMG.us" tvg-name="WKMG   CBS ORLANDO" tvg-logo="https://static.epg.best/us/WKMG.us.png" tvg-chno="122" channel-id="122" group-title="TV",WKMG   CBS ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cbs.wkmg.orlando.fl.us.m3u8
#EXTINF:-1 tvg-id="WFTV.us" tvg-name="WFTV   ABC ORLANDO" tvg-logo="https://static.epg.best/us/WFTV.us.png" tvg-chno="123" channel-id="123" group-title="TV",WFTV   ABC ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/abc.wftv.orlando.fl.us.m3u8
#EXTINF:-1 tvg-id="WOFL.us" tvg-name="WOFL   FOX ORLANDO" tvg-logo="https://static.epg.best/us/WOFL.us.png" tvg-chno="124" channel-id="124" group-title="TV",WOFL   FOX ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.wofl.orlando.fl.us.m3u8
#EXTINF:-1 tvg-id="WKCF.us" tvg-name="WKCF   CW 18 ORLANDO" tvg-logo="https://i.ibb.co/cK2GwV7r/CW18-Clear.png" tvg-chno="125" channel-id="125" group-title="TV",WKCF   CW 18 ORLANDO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cw.wkcf.orlando.fl.us.m3u8
#EXTINF:-1 tvg-id="ComedyCentral.us" tvg-name="COMEDY CENTRAL" tvg-logo="https://static.epg.best/us/ComedyCentral.us.png" tvg-chno="129" channel-id="129" group-title="TV",COMEDY CENTRAL
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/comedy.central.eastern.us.m3u8
#EXTINF:-1 tvg-id="USANetwork.us" tvg-name="USA NETWORK" tvg-logo="https://i.ibb.co/VprcTcV8/USAN.png" tvg-chno="130" channel-id="130" group-title="TV",USA NETWORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/usa.network.east.us.m3u8
#EXTINF:-1 tvg-id="TBS.us" tvg-name="TBS HD" tvg-logo="https://static.epg.best/us/TBS.us.png" tvg-chno="131" channel-id="131" group-title="TV",TBS HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/tbs.east.us.m3u8
#EXTINF:-1 tvg-id="ParamountNetwork.us" tvg-name="PARAMOUNT NETWORK" tvg-logo="https://static.epg.best/us/ParamountNetwork.us.png" tvg-chno="132" channel-id="132" group-title="TV",PARAMOUNT NETWORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/paramount.network.usa.eastern.us.m3u8
#EXTINF:-1 tvg-id="TNT.us" tvg-name="TNT HD" tvg-logo="https://static.epg.best/us/TNT.us.png" tvg-chno="133" channel-id="133" group-title="TV",TNT HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/tnt.east.us.m3u8
#EXTINF:-1 tvg-id="FX.us" tvg-name="FX NETWORK" tvg-logo="https://static.epg.best/us/FX.us.png" tvg-chno="134" channel-id="134" group-title="TV",FX NETWORK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fx.networks.east.coast.us.m3u8
#EXTINF:-1 tvg-id="FXX.us" tvg-name="FXX HD" tvg-logo="https://static.epg.best/us/FXX.us.png" tvg-chno="135" channel-id="135" group-title="TV",FXX HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fxx.usa.eastern.us.m3u8
#EXTINF:-1 tvg-id="FXMovieChannel.us" tvg-name="FX MOVIE" tvg-logo="https://static.epg.best/us/FXMovieChannel.us.png" tvg-chno="136" channel-id="136" group-title="TV",FX MOVIE
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fx.movie.channel.us.m3u8
#EXTINF:-1 tvg-id="DisneyChannel.us" tvg-name="DISNEY" tvg-logo="https://static.epg.best/us/DisneyChannel.us.png" tvg-chno="137" channel-id="137" group-title="TV",DISNEY
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/disney.eastern.us.m3u8
#EXTINF:-1 tvg-id="DisneyJunior.us" tvg-name="DISNEY JR" tvg-logo="https://static.epg.best/us/DisneyJunior.us.png" tvg-chno="138" channel-id="138" group-title="TV",DISNEY JR
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/disney.junior.usa.east.us.m3u8
#EXTINF:-1 tvg-id="Nickelodeon.us" tvg-name="NICK" tvg-logo="https://i.ibb.co/4w26j1Bj/nick-nickelodeon-logo-removebg-preview.png" tvg-chno="139" channel-id="139" group-title="TV",NICK
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nickelodeon.usa.east.us.m3u8
#EXTINF:-1 tvg-id="CartoonNetwork.us" tvg-name="CARTOON HD" tvg-logo="https://static.epg.best/us/CartoonNetwork.us.png" tvg-chno="140" channel-id="140" group-title="TV",CARTOON HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cartoon.network.usa.eastern.us.m3u8
#EXTINF:-1 tvg-id="Boomerang.us" tvg-name="BOOMERANG" tvg-logo="https://static.epg.best/us/Boomerang.us.png" tvg-chno="141" channel-id="141" group-title="TV",BOOMERANG
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/boomerang.us.m3u8
#EXTINF:-1 tvg-id="WFOR.us" tvg-name="WFOR   CBS MIAMI" tvg-logo="https://image.roku.com/developer_channels/prod/74a606e94c1077b1b1ae6d33f8b6c9b854fce9dd54784386679e9d10cf002cdd.png" tvg-chno="142" channel-id="142" group-title="TV",WFOR   CBS MIAMI
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cbs.wfor.miami.fl.us.m3u8
#EXTINF:-1 tvg-id="WTVJ.us" tvg-name="WTVJ   NBC MIAMI" tvg-logo="https://static.epg.best/us/WTVJ.us.png" tvg-chno="143" channel-id="143" group-title="TV",WTVJ   NBC MIAMI
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nbc.wtvj.miami.fl.us.m3u8
#EXTINF:-1 tvg-id="WSVN.us" tvg-name="WSVN   FOX MIAMI" tvg-logo="https://static.epg.best/us/WSVN.us.png" tvg-chno="144" channel-id="144" group-title="TV",WSVN   FOX MIAMI
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.wsvn.miami.fl.us.m3u8
#EXTINF:-1 tvg-id="WBFS.us" tvg-name="WBFS   MY TV 33 MIAMI" tvg-logo="https://static.epg.best/us/WBFS.us.png" tvg-chno="145" channel-id="145" group-title="TV",WBFS   MY TV 33 MIAMI
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/mnt.wbfs.miami.fl.us.m3u8
#EXTINF:-1 tvg-id="WLTV.us" tvg-name="WLTV   UNI MIAMI" tvg-logo="https://i.ibb.co/ycTh0gZ6/6d7ecddc-43af-4668-801c-edde6aa56925.jpg" tvg-chno="146" channel-id="146" group-title="TV",WLTV   UNI MIAMI
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/uni.wltv.miami.fl.us.m3u8
#EXTINF:-1 tvg-id="CourtTV.us" tvg-name="COURT TV" tvg-logo="https://static.epg.best/us/CourtTV.us.png" tvg-chno="150" channel-id="150" group-title="TV",COURT TV
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/court.tv.network.us.m3u8
#EXTINF:-1 tvg-id="AMC.us" tvg-name="AMC HD" tvg-logo="https://static.epg.best/us/AMC.us.png" tvg-chno="151" channel-id="151" group-title="TV",AMC HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/amc.eastern.us.m3u8
#EXTINF:-1 tvg-id="PopTV.us" tvg-name="POP TV" tvg-logo="https://static.epg.best/us/PopTV.us.png" tvg-chno="152" channel-id="152" group-title="TV",POP TV
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/pop.east.us.m3u8
#EXTINF:-1 tvg-id="truTV.us" tvg-name="TRU TV" tvg-logo="https://static.epg.best/us/truTV.us.png" tvg-chno="153" channel-id="153" group-title="TV",TRU TV
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/trutv.usa.east.us.m3u8
#EXTINF:-1 tvg-id="Telecinco.es" tvg-name="TELECINCO ES" tvg-logo="https://static.epg.best/es/Telecinco.es.png" tvg-chno="160" channel-id="160" group-title="TV",TELECINCO ES
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/telecinco.es.m3u8
#EXTINF:-1 tvg-id="Antena3.es" tvg-name="ANTENA 3" tvg-logo="https://static.epg.best/es/Antena3.es.png" tvg-chno="161" channel-id="161" group-title="TV",ANTENA 3
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/antena.3.es.m3u8
#EXTINF:-1 tvg-id="TeleAmazonas.ec" tvg-name="TELEAMAZONAS EC" tvg-logo="https://i.ibb.co/B2pq2sKq/TATV.jpg" tvg-chno="163" channel-id="163" group-title="TV",TELEAMAZONAS EC
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/teleamazonas.lat.m3u8
#EXTINF:-1 tvg-id="CNNEspanol.us" tvg-name="CNN EN ESPANOL" tvg-logo="https://static.epg.best/us/CNNEspanol.us.png" tvg-chno="164" channel-id="164" group-title="TV",CNN EN ESPANOL
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cnn.en.espanol.mx.m3u8
#EXTINF:-1 tvg-id="CNNInt.es" tvg-name="CNN COLOMBIA" tvg-logo="https://i.ibb.co/YTbGBh6x/CNNCO.jpg" tvg-chno="165" channel-id="165" group-title="TV",CNN COLOMBIA
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cnn.espanol.co.m3u8
#EXTINF:-1 tvg-id="CNNInternational.us" tvg-name="CNN INTERNATIONAL" tvg-logo="https://static.epg.best/us/CNNInternational.us.png" tvg-chno="166" channel-id="166" group-title="TV",CNN INTERNATIONAL
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cnn.international.north.america.us.m3u8
#EXTINF:-1 tvg-id="CNN.us" tvg-name="CNN HD" tvg-logo="https://static.epg.best/us/CNN.us.png" tvg-chno="167" channel-id="167" group-title="TV",CNN HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cnn.us.m3u8
#EXTINF:-1 tvg-id="CSPAN.us" tvg-name="C-SPAN" tvg-logo="https://static.epg.best/us/CSPAN.us.png" tvg-chno="168" channel-id="168" group-title="TV",C-SPAN
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cspan.us.m3u8
#EXTINF:-1 tvg-id="CSPAN2.us" tvg-name="CSPAN 2" tvg-logo="https://static.epg.best/us/CSPAN2.us.png" tvg-chno="169" channel-id="169" group-title="TV",CSPAN 2
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/c.span.2.us.m3u8
#EXTINF:-1 tvg-id="FoxNews.us" tvg-name="FOX NEWS" tvg-logo="https://static.epg.best/us/FoxNews.us.png" tvg-chno="170" channel-id="170" group-title="TV",FOX NEWS
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.news.us.m3u8
#EXTINF:-1 tvg-id="WeatherChannel.us" tvg-name="WEATHER HD" tvg-logo="https://static.epg.best/us/WeatherChannel.us.png" tvg-chno="171" channel-id="171" group-title="TV",WEATHER HD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/the.weather.channel.us.m3u8
#EXTINF:-1 tvg-id="WeatherNation.us" tvg-name="WEATHERNATION" tvg-logo="https://static.epg.best/us/WeatherNation.us.png" tvg-chno="172" channel-id="172" group-title="TV",WEATHERNATION
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/weathernation.us.m3u8
#EXTINF:-1 tvg-id="TheWeatherNetwork.ca" tvg-name="WEATHER NETWORK CANADA" tvg-logo="https://static.epg.best/ca/TheWeatherNetwork.ca.png" tvg-chno="173" channel-id="173" group-title="TV",WEATHER NETWORK CANADA
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/weather.network.ca.m3u8
#EXTINF:-1 tvg-id="Accuweather.us" tvg-name="ACCUWEATHER" tvg-logo="https://i.ibb.co/xnLzN1K/aw-logo.png" tvg-chno="174" channel-id="174" group-title="TV",ACCUWEATHER
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/accuweather.us.m3u8
#EXTINF:-1 tvg-id="HBO.us" tvg-name="HBO" tvg-logo="https://static.epg.best/us/HBO.us.png" tvg-chno="175" channel-id="175" group-title="TV",HBO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/hbo.eastern.us.m3u8
#EXTINF:-1 tvg-id="HBO2.us" tvg-name="HBO 2" tvg-logo="https://static.epg.best/us/HBO2.us.png" tvg-chno="176" channel-id="176" group-title="TV",HBO 2
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/hbo.2.eastern.us.m3u8
#EXTINF:-1 tvg-id="HBOComedy.us" tvg-name="HBO Comedy" tvg-logo="https://static.epg.best/us/HBOComedy.us.png" tvg-chno="177" channel-id="177" group-title="TV",HBO Comedy
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/hbo.comedy.east.us.m3u8
#EXTINF:-1 tvg-id="WFLA.us" tvg-name="WFLA   NBC TAMPA" tvg-logo="https://image.roku.com/developer_channels/prod/74a606e94c1077b1b1ae6d33f8b6c9b854fce9dd54784386679e9d10cf002cdd.png" tvg-chno="180" channel-id="180" group-title="TV",WFLA   NBC TAMPA
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nbc.wfla.tampa.bay.fl.us.m3u8
#EXTINF:-1 tvg-id="WTSP.us" tvg-name="WTSP   CBS TAMPA" tvg-logo="https://static.epg.best/us/WTSP.us.png" tvg-chno="181" channel-id="181" group-title="TV",WTSP   CBS TAMPA
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/cbs.wtsp.tampa.bay.fl.us.m3u8
#EXTINF:-1 tvg-id="WTVT.us" tvg-name="WTVT   FOX TAMPA" tvg-logo="https://static.epg.best/us/WTVT.us.png" tvg-chno="182" channel-id="182" group-title="TV",WTVT   FOX TAMPA
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.wtvt.tampa.bay.fl.us.m3u8
#EXTINF:-1 tvg-id="WFTS.us" tvg-name="WFTS   ABC TAMPA" tvg-logo="https://static.epg.best/us/WFTS.us.png" tvg-chno="183" channel-id="183" group-title="TV",WFTS   ABC TAMPA
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/abc.wfts.tampa.bay.fl.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="WRMD   TELEMUNDO TAMPA" tvg-logo="https://static.wikia.nocookie.net/tvstations/images/2/2c/Telemundo_49_2018.png" tvg-chno="184" channel-id="184" group-title="TV",WRMD   TELEMUNDO TAMPA
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/telemundo.wrmd.tampa.fl.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="BALLY SPORTS SUN" tvg-logo="https://i.ibb.co/jv7mZWLM/Bally-Sports-Sun.png" tvg-chno="185" channel-id="185" group-title="TV",BALLY SPORTS SUN
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/bally.sports.sun.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="BALLY SPORTS FLORIDA" tvg-logo="https://i.ibb.co/cXyJC9Nf/Bally-Sports-Florida-logo.png" tvg-chno="186" channel-id="186" group-title="TV",BALLY SPORTS FLORIDA
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/fox.sports.florida.us.m3u8
#EXTINF:-1 tvg-id="DiscoveryEspanol.us" tvg-name="DISCOVERY EN ESPAÑOL" tvg-logo="https://static.epg.best/us/DiscoveryEspanol.us.png" tvg-chno="187" channel-id="187" group-title="TV",DISCOVERY EN ESPAÑOL
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/discovery.en.espanol.us.m3u8
#EXTINF:-1 tvg-id="DiscoveryFamilia.us" tvg-name="DISCOVERY FAMILIA" tvg-logo="https://static.epg.best/us/DiscoveryFamilia.us.png" tvg-chno="188" channel-id="188" group-title="TV",DISCOVERY FAMILIA
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/discovery.familia.us.m3u8
#EXTINF:-1 tvg-id="DiscoveryTurboLatinAmerica.pa" tvg-name="DISCOVERY TURBO" tvg-logo="https://static.epg.best/pa/DiscoveryTurboLatinAmerica.pa.png" tvg-chno="189" channel-id="189" group-title="TV",DISCOVERY TURBO
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/discovery.turbo.co.m3u8
#EXTINF:-1 tvg-id="DiscoveryWorld.mx" tvg-name="DISCOVERY WORLD" tvg-logo="https://static.epg.best/mx/DiscoveryWorld.mx.png" tvg-chno="190" channel-id="190" group-title="TV",DISCOVERY WORLD
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/discovery.world.co.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[MLB PPV] NY YANKEES" tvg-logo="https://logos-world.net/wp-content/uploads/2020/05/New-York-Yankees-Logo.png" tvg-chno="200" channel-id="200" group-title="TV",[MLB PPV] NY YANKEES
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/mlb.teams.nyy.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[MLB PPV] NY METS" tvg-logo="https://logos-world.net/wp-content/uploads/2020/05/New-York-Mets-Emblem.png" tvg-chno="201" channel-id="201" group-title="TV",[MLB PPV] NY METS
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/mlb.teams.nym.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[MLB PPV] MIA MARLINS" tvg-logo="https://brandlogos.net/wp-content/uploads/2025/02/miami_marlins-logo_brandlogos.net_9fcup.png" tvg-chno="202" channel-id="202" group-title="TV",[MLB PPV] MIA MARLINS
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/mlb.teams.mia.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[MLB PPV] DET TIGERS" tvg-logo="https://loodibee.com/wp-content/uploads/Detroit-Tigers-Logo-1994-2005.png" tvg-chno="203" channel-id="203" group-title="TV",[MLB PPV] DET TIGERS
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/mlb.teams.det.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[MLB PPV] TB RAYS" tvg-logo="https://nbcsports.brightspotcdn.com/dims4/default/08447de/2147483647/strip/true/crop/1708x961+0+2/resize/1440x810!/quality/90/?url=https%3A%2F%2Fnbc-sports-production-nbc-sports.s3.us-east-1.amazonaws.com%2Fbrightspot%2Fda%2Fb1%2F467bd3792de4c26bd6743d2b7266%2Ftbr2015-3d-primary-rgb-e1442678074633.png" tvg-chno="204" channel-id="204" group-title="TV",[MLB PPV] TB RAYS
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/mlb.teams.tb.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[NBA PPV] GS WARRIORS" tvg-logo="https://athlonsports.com/.image/c_limit%2Ccs_srgb%2Cq_auto:good%2Cw_500/MjA1MjcyMjkxNzYxNDY0ODUy/golden-state-warriors-logo.webp" tvg-chno="205" channel-id="205" group-title="TV",[NBA PPV] GS WARRIORS
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nba.golden.state.warriors.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[NBA PPV] NY KNICKS" tvg-logo="https://upload.wikimedia.org/wikipedia/sco/d/d8/NewYorkKnicks.PNG" tvg-chno="206" channel-id="206" group-title="TV",[NBA PPV] NY KNICKS
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nba.new.york.knicks.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[NBA PPV] DEN NUGGETS" tvg-logo="https://www.flagcolorcodes.com/data/NBA-Denver-Nuggets.png" tvg-chno="207" channel-id="207" group-title="TV",[NBA PPV] DEN NUGGETS
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nba.denver.nuggets.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[NBA PPV] OKC THUNDER" tvg-logo="https://i.pinimg.com/736x/47/ea/93/47ea93e7a7b5f898d833ef218f3cd833.jpg" tvg-chno="208" channel-id="208" group-title="TV",[NBA PPV] OKC THUNDER
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nba.oklahoma.city.thunder.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[NBA PPV] IND PACERS" tvg-logo="https://www.flagcolorcodes.com/data/NBA-Indiana-Pacers.png" tvg-chno="209" channel-id="209" group-title="TV",[NBA PPV] IND PACERS
https://starlite.best/api/stream/throwaway.mgk343@gmail.com/167848/livetv.epg/nba.indiana.pacers.us.m3u8
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] WKAQ 580 [AM: 580 Khz] WKAQ-AM" tvg-logo="https://i.ibb.co/Vc9wh11d/wkaqam.png" tvg-chno="210" channel-id="210" group-title="TV",[R] WKAQ 580 [AM: 580 Khz] WKAQ-AM
https://televicentro.streamguys1.com/wkaqqam-icy?key=ae6a3b84b2caabf9d96d28dc1d8e3ebc2cc0ecce9ef074936108cb8cccf7964d&source=tunein&source=TuneIn&gdpr=0&us_privacy=1YNY&bundle=tunein.com&lat=28.0699&long=-81.8107
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] NotiUno [AM: 630 Khz] WUNO-AM" tvg-logo="https://www.unoradio.com/logos/logos/notiuno/notiuno630.png" tvg-chno="211" channel-id="211" group-title="TV",[R] NotiUno [AM: 630 Khz] WUNO-AM
https://server20.servistreaming.com:9022/stream
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Radio Once [AM: 1120 Khz] WMSW-AM" tvg-logo="https://www.radioonce.com/templates/rt_galatea/custom/images/demo/home/header/logo-radio11.png" tvg-chno="212" channel-id="212" group-title="TV",[R] Radio Once [AM: 1120 Khz] WMSW-AM
http://whsh4u-panel.com:14167/stream
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Radio Isla [AM: 1320 Khz] WSKN-AM" tvg-logo="https://radioisla.tv/wp-content/uploads/2019/06/Logo-Radio-Isla.png" tvg-chno="213" channel-id="213" group-title="TV",[R] Radio Isla [AM: 1320 Khz] WSKN-AM
https://server7.servistreaming.com/proxy/RadioIsla?mp=%2Fstream%3Ftype%3D.mp3&_=1
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Radio Tiempo [AM: 1430 Khz] WNLE-AM" tvg-logo="https://radiotiempo.net/wp-content/uploads/2022/08/tiempo-172x128.png" tvg-chno="214" channel-id="214" group-title="TV",[R] Radio Tiempo [AM: 1430 Khz] WNLE-AM
https://server7.servistreaming.com/proxy/tiempo?mp=%2Fstream%3Ftype%3D.mp3&_=1
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Radio Cumbre [AM: 1470 Khz] WKUM-AM" tvg-logo="https://i.ibb.co/5g2402Cc/wkum-png.png" tvg-chno="215" channel-id="215" group-title="TV",[R] Radio Cumbre [AM: 1470 Khz] WKUM-AM
https://sp.unoredcdn.net/8158/stream/1/
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Radio Oro [FM: 92.5 Mhz] WORO-FM" tvg-logo="https://i.ibb.co/nqbSmS1M/worofm-processed.png" tvg-chno="216" channel-id="216" group-title="TV",[R] Radio Oro [FM: 92.5 Mhz] WORO-FM
https://us2.internet-radio.com/proxy/woro?mp=/stream
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Z 93 [FM: 93.7 Mhz] WZNT-FM" tvg-logo="https://i.ibb.co/23BMsKBY/wznt-png.png" tvg-chno="217" channel-id="217" group-title="TV",[R] Z 93 [FM: 93.7 Mhz] WZNT-FM
https://liveaudio.lamusica.com/PR_WZNT_icy
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] La Nueva 94 [FM: 94.7 Mhz] WODA-FM" tvg-logo="https://i.ibb.co/sv1RBc08/wodalogo.png" tvg-chno="218" channel-id="218" group-title="TV",[R] La Nueva 94 [FM: 94.7 Mhz] WODA-FM
https://liveaudio.lamusica.com/PR_WODA_icy
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Fidelity [FM: 95.7 Mhz] WFID-FM" tvg-logo="https://fidelitypr.com/wp-content/uploads/2022/01/cropped-Redisen%CC%83o-Logo-Fidelity-3-15-2048x677.png" tvg-chno="219" channel-id="219" group-title="TV",[R] Fidelity [FM: 95.7 Mhz] WFID-FM
https://server7.servistreaming.com/proxy/fidelity?mp=%2Fstream%3Ftype%3D.mp3&_=1
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Estéreotempo [FM; 96.5 Mhz] WRXD-FM" tvg-logo="https://i.ibb.co/F4GM0W81/wrxr.png" tvg-chno="220" channel-id="220" group-title="TV",[R] Estéreotempo [FM; 96.5 Mhz] WRXD-FM
https://liveaudio.lamusica.com/PR_WRXD_icy
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Magic [FM; 97.3 Mhz] WOYE-FM" tvg-logo="https://i.ibb.co/Z6WqXPzV/woye.png" tvg-chno="221" channel-id="221" group-title="TV",[R] Magic [FM; 97.3 Mhz] WOYE-FM
https://stream.eleden.com:8210/magic.aac
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Salsoul [FM: 99.1 Mhz] WPRM-FM" tvg-logo="https://salsoul.com/wp-content/uploads/2020/12/cropped-salsoul-2.png" tvg-chno="222" channel-id="222" group-title="TV",[R] Salsoul [FM: 99.1 Mhz] WPRM-FM
https://server20.servistreaming.com:9023/stream?type=.mp3&_=1
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] LA X [FM: 100.7 Mhz] WXYX-FM" tvg-logo="https://i.ibb.co/zWDcRnBw/laxpng.png" tvg-chno="223" channel-id="223" group-title="TV",[R] LA X [FM: 100.7 Mhz] WXYX-FM
https://stream.eleden.com:8230/La X.aac
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] HOT 102 [FM 102.5 Mhz] WTOK-FM" tvg-logo="https://hot102pr.com/wp-content/uploads/2023/10/Artboard-4.png" tvg-chno="224" channel-id="224" group-title="TV",[R] HOT 102 [FM 102.5 Mhz] WTOK-FM
https://server7.servistreaming.com/proxy/hot?mp=%2Fstream%3Ftype%3D.mp3&_=1
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] La Mega PR [FM: 106.9 Mhz] WMEG-FM" tvg-logo="https://i.ibb.co/Xrp2nhpQ/WMEG-PNG.png" tvg-chno="225" channel-id="225" group-title="TV",[R] La Mega PR [FM: 106.9 Mhz] WMEG-FM
https://liveaudio.lamusica.com/PR_WMEG_icy
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] KQ105 [FM: 104.7 Mhz] WKAQ-FM" tvg-logo="https://upload.wikimedia.org/wikipedia/en/8/80/KQ_105_WKAQ-FM_2014_logo.png" tvg-chno="226" channel-id="226" group-title="TV",[R] KQ105 [FM: 104.7 Mhz] WKAQ-FM
https://televicentro.streamguys1.com/wkaqfm-icy?key=ae6a3b84b2caabf9d96d28dc1d8e3ebc2cc0ecce9ef074936108cb8cccf7964d&source=tunein&source=TuneIn&gdpr=0&us_privacy=1YNY&bundle=tunein.com&lat=28.0699&long=-81.8107
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Latino 99" tvg-logo="https://mm.aiircdn.com/371/5928f28889f51.png" tvg-chno="227" channel-id="227" group-title="TV",[R] Latino 99
https://lmmradiocast.com/Latino99fm?_=68068
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] Salseo Radio" tvg-logo="https://static.wixstatic.com/media/8dfec0_3e265a2f0fb5417c90c55be4e4e7d3cf~mv2.png/v1/fill/w_276,h_120,al_c,q_85,usm_0.66_1.00_0.01,enc_auto/LOGOSALSEORADIO_clipped_rev_2_v2%20(1).png" tvg-chno="228" channel-id="228" group-title="TV",[R] Salseo Radio
https://listen.radioking.com/radio/399811/stream/452110
#EXTINF:-1 tvg-id="DummyChannel.us" tvg-name="[R] La Vieja Z [GLZ Radio]" tvg-logo="https://i.ibb.co/d4VZVjj2/LVZ8-removebg-preview.png" tvg-chno="229" channel-id="229" group-title="TV",[R] La Vieja Z [GLZ Radio]
https://s3.free-shoutcast.com/stream/18094
`;

// --- M3U Parser ---
function parseM3U(m3uContent) {
  console.log('Starting M3U parsing...');
  console.log('M3U content length:', m3uContent.length);
  console.log('First 200 chars:', m3uContent.substring(0, 200));
  
  const channels = [];
  const lines = m3uContent.split('\n');
  console.log('Total lines:', lines.length);
  
  let currentChannel = null;
  let extinfCount = 0;
  let urlCount = 0;
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line.startsWith('#EXTINF:')) {
      extinfCount++;
      currentChannel = {};
      const lastCommaIndex = line.lastIndexOf(',');
      if (lastCommaIndex !== -1) {
        currentChannel.name = line.substring(lastCommaIndex + 1).trim();
      }
      const logoMatch = line.match(/tvg-logo="([^"]*)"/);
      if (logoMatch) currentChannel.logo = logoMatch[1];
      const chnoMatch = line.match(/tvg-chno="([^"]*)"/);
      if (chnoMatch) currentChannel.chno = chnoMatch[1];
      const idMatch = line.match(/tvg-id="([^"]*)"/);
      if (idMatch) currentChannel.id = idMatch[1];
      const nameMatch = line.match(/tvg-name="([^"]*)"/);
      if (nameMatch && !currentChannel.name) currentChannel.name = nameMatch[1];
      
      console.log(`EXTINF ${extinfCount}:`, currentChannel.name, 'ID:', currentChannel.id);
    } else if (line && !line.startsWith('#') && currentChannel) {
      urlCount++;
      currentChannel.url = line.trim();
      channels.push(currentChannel);
      console.log(`URL ${urlCount}:`, currentChannel.url.substring(0, 50) + '...');
      currentChannel = null;
    }
  }
  
  console.log('Parsing complete. EXTINF count:', extinfCount, 'URL count:', urlCount, 'Channels:', channels.length);
  return channels;
}

// --- State ---
let channels = [];
let current = 0;
let standby = true;
let hls = null;
let lastChannel = 0;
let favorites = JSON.parse(localStorage.getItem('glz-favorites') || '[]');
let isMuted = false;
let connectionStatus = 'connected';
let channelInput = '';
let pwaInstallPrompt = null;

// EPG State
let epgData = {};
let epgLoading = false;
let epgLastUpdate = null;
const EPG_CACHE_DURATION = 30 * 60 * 1000; // 30 minutes
// EPG Sources
const EPG_SOURCES = {
  epgshare01: 'https://epgshare01.online/epgshare01/epg_ripper_ALL_SOURCES1.xml.gz',
  epgshare02: 'https://epgshare02.online/epgshare02/epg_ripper_ALL_SOURCES2.xml.gz',
  epgshare03: 'https://epgshare03.online/epgshare03/epg_ripper_ALL_SOURCES3.xml.gz',
  uslocals: 'https://epgshare01.online/epgshare01/epg_ripper_US_LOCALS2.xml.gz'
};

let EPG_URL = EPG_SOURCES.uslocals; // Default to US Locals for better local channel coverage
const LOCAL_EPG_PROXY = 'http://localhost:3000/api/epg';
const CLOUD_EPG_PROXY = '/api/epg'; // Netlify function

// --- DOM Elements ---
const video = document.getElementById('video');
const audio = document.getElementById('audio');
const channelNumber = document.getElementById('channel-number');
const channelName = document.getElementById('channel-name');
const currentProgram = document.getElementById('current-program');
const timeDisplay = document.getElementById('time-display');
const dateDisplay = document.getElementById('date-display');
const standbyScreen = document.getElementById('standby-screen');
const channelBanner = document.getElementById('channel-banner');
const bannerChannel = document.getElementById('banner-channel');
const bannerName = document.getElementById('banner-name');
const progressBar = document.getElementById('progress-bar');
const channelGuide = document.getElementById('channel-guide');
const channelList = document.getElementById('channel-list');
const closeGuide = document.getElementById('close-guide');
const remoteControl = document.getElementById('remote-control');
const toggleRemote = document.getElementById('toggle-remote');
const powerBtn = document.getElementById('power-btn');
const headerRemoteBtn = document.getElementById('header-remote-btn');
const mobileRemoteBtn = document.getElementById('mobile-remote-btn');
const mobileChannelList = document.getElementById('mobile-channel-list');
const mobileChannelToggle = document.getElementById('mobile-channel-toggle');
const numpadBtns = document.querySelectorAll('.numpad-btn');
const prevBtn = document.getElementById('prev-btn');
const nextBtn = document.getElementById('next-btn');
const statusMessage = document.getElementById('status-message');
const loadingIndicator = document.getElementById('loading-indicator');
const videoContainer = document.getElementById('video-container');
const helpOverlay = document.getElementById('help-overlay');
const closeHelp = document.getElementById('close-help');
const helpBody = document.getElementById('help-body');
const searchInput = document.getElementById('search-input');
const stbContainer = document.querySelector('.stb-container');

// Options panel elements
const optionsOverlay = document.getElementById('options-overlay');
const closeOptions = document.getElementById('close-options');
const headerOptionsBtn = document.getElementById('header-options-btn');
const reloadEpgBtn = document.getElementById('reload-epg-btn');
const clearCacheBtn = document.getElementById('clear-cache-btn');
const epgStatus = document.getElementById('epg-status');
const epgLastUpdateDisplay = document.getElementById('epg-last-update');
const connectionStatusDisplay = document.getElementById('connection-status-display');

// EPG Settings elements
const epgSourceSelect = document.getElementById('epg-source-select');
const customEpgUrlItem = document.getElementById('custom-epg-url-item');
const epgUrlInput = document.getElementById('epg-url-input');
const epgAutoRefresh = document.getElementById('epg-auto-refresh');
const cacheDurationSelect = document.getElementById('cache-duration-select');
const themeToggleBtn = document.getElementById('theme-toggle-btn');
const themeToggleText = document.getElementById('theme-toggle-text');
const miniGuide = document.getElementById('mini-guide');
const miniGuideClose = document.getElementById('mini-guide-close');
const miniGuideContent = document.getElementById('mini-guide-content');

// Remote control elements (updated IDs to match new HTML)
const volumeDownBtn = document.getElementById('volume-down-btn');
const volumeUpBtn = document.getElementById('volume-up-btn');
const muteBtn = document.getElementById('mute-btn');
const channelDownBtn = document.getElementById('channel-down-btn');
const channelUpBtn = document.getElementById('channel-up-btn');
const lastChannelBtn = document.getElementById('last-channel-btn');

// --- EPG Functions ---

/**
 * Fetches and parses EPG data from the XML URL
 */
async function fetchEPGData() {
  if (epgLoading) {
    console.log('EPG already loading...');
    return;
  }
  
  // Check cache
  const cached = localStorage.getItem('glz-epg-cache');
  const cacheTime = localStorage.getItem('glz-epg-cache-time');
  
  if (cached && cacheTime) {
    const age = Date.now() - parseInt(cacheTime);
    const cacheDuration = parseInt(localStorage.getItem('glz-epg-cache-duration') || '30') * 60 * 1000;
    
    if (age < cacheDuration) {
      console.log('Using cached EPG data');
      epgData = JSON.parse(cached);
      epgLastUpdate = parseInt(cacheTime);
      
      console.log('✅ Using cached EPG data');
      return;
    }
  }
  
  epgLoading = true;
  console.log('Fetching EPG data from:', EPG_URL);
  console.log('💡 Note: This is a large EPG file (~150MB compressed). Loading may take a moment...');
  
  try {
    // For very large EPG files, try direct access first, then fallback to proxies
    const proxies = [
      EPG_URL, // Try direct access first for large files
      CLOUD_EPG_PROXY, // Cloud proxy (works on mobile)
      LOCAL_EPG_PROXY, // Local proxy (development only)
      `https://api.allorigins.win/raw?url=${encodeURIComponent(EPG_URL)}`,
      `https://cors-anywhere.herokuapp.com/${EPG_URL}`,
      `https://thingproxy.freeboard.io/fetch/${EPG_URL}`,
      `https://api.codetabs.com/v1/proxy?quest=${encodeURIComponent(EPG_URL)}`
    ];
    
    let xmlText = null;
    let lastError = null;
    
    for (const proxyUrl of proxies) {
      try {
        console.log('Trying proxy:', proxyUrl);
        
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 60000); // 60 second timeout for large files
        
        const response = await fetch(proxyUrl, {
          signal: controller.signal,
          headers: {
            'Accept': 'application/xml, text/xml, */*',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
          }
        });
        
        clearTimeout(timeoutId);
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        xmlText = await response.text();
        console.log('EPG XML received from proxy, length:', xmlText.length);
        console.log('First 500 chars of XML:', xmlText.substring(0, 500));
        break; // Success, exit the loop
        
      } catch (error) {
        console.log('Proxy failed:', error.message);
        lastError = error;
        continue; // Try next proxy
      }
    }
    
    if (!xmlText) {
      throw new Error('All proxies failed: ' + lastError?.message);
    }
    
    // Parse XML
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlText, 'text/xml');
    
    // Check for parsing errors
    const parseError = xmlDoc.querySelector('parsererror');
    if (parseError) {
      throw new Error('XML parsing failed: ' + parseError.textContent);
    }
    
    console.log('XML parsed successfully, checking for channels...');
    console.log('XML document root:', xmlDoc.documentElement?.tagName);
    console.log('Number of programme elements:', xmlDoc.querySelectorAll('programme').length);
    
    // Parse programs
    epgData = parseEPGXML(xmlDoc);
    
    // Debug: Check if we got any data
    console.log('EPG channels found:', Object.keys(epgData).length);
    console.log('Sample EPG channel IDs:', Object.keys(epgData).slice(0, 5));
    console.log('Sample channel data:', Object.values(epgData).slice(0, 1));
    
    // Cache the data
    localStorage.setItem('glz-epg-cache', JSON.stringify(epgData));
    localStorage.setItem('glz-epg-cache-time', Date.now().toString());
    epgLastUpdate = Date.now();
    
    // Update options display if it's open
    updateOptionsDisplay();
    
    // Update header program display
    if (channels[current] && channels[current].id) {
      updateHeaderProgram(channels[current].id);
    }
    
    console.log('EPG data parsed successfully:', Object.keys(epgData).length, 'channels');
    console.log('Available EPG channel IDs (first 20):', Object.keys(epgData).slice(0, 20));
    
  } catch (error) {
    console.error('EPG fetch failed:', error);
    showStatus('EPG data unavailable', 3000);
    console.log('❌ EPG data not available - no fallback will be shown');
  } finally {
    epgLoading = false;
  }
}

/**
 * Parses XML EPG data into a structured format
 */
function parseEPGXML(xmlDoc) {
  const epg = {};
  const programmes = xmlDoc.querySelectorAll('programme');
  
  programmes.forEach(programme => {
    const channel = programme.getAttribute('channel');
    const start = programme.getAttribute('start');
    const stop = programme.getAttribute('stop');
    const title = programme.querySelector('title')?.textContent || 'Unknown';
    const desc = programme.querySelector('desc')?.textContent || '';
    const category = programme.querySelector('category')?.textContent || '';
    const rating = programme.querySelector('rating')?.querySelector('value')?.textContent || '';
    
    if (!epg[channel]) {
      epg[channel] = [];
    }
    
    epg[channel].push({
      start: new Date(start),
      stop: new Date(stop),
      title,
      desc,
      category,
      rating
    });
  });
  
  // Sort programs by start time for each channel
  Object.keys(epg).forEach(channel => {
    epg[channel].sort((a, b) => a.start - b.start);
  });
  
  return epg;
}

/**
 * Gets current and next program for a channel
 */
function getCurrentProgram(channelId) {
  console.log('getCurrentProgram called with channelId:', channelId);
  console.log('Available EPG channels:', Object.keys(epgData));
  
  if (!channelId || !epgData || Object.keys(epgData).length === 0) {
    console.log('No EPG data available');
    return null;
  }
  
  // Try multiple channel ID formats for better matching
  const possibleIds = [
    channelId,
    channelId?.toLowerCase(),
    channelId?.toUpperCase(),
    channelId?.replace(/\.us$/, ''),
    channelId?.replace(/\.us$/, '.us'),
    channelId?.replace(/\.es$/, ''),
    channelId?.replace(/\.ec$/, ''),
    channelId?.toString(),
    // Try without country codes
    channelId?.replace(/\.(us|es|ec)$/, ''),
    // Try with different separators
    channelId?.replace(/\./g, '_'),
    channelId?.replace(/_/g, '.'),
    // Try common variations
    channelId?.replace(/\.us$/, '.US'),
    channelId?.replace(/\.es$/, '.ES'),
    channelId?.replace(/\.ec$/, '.EC')
  ].filter(Boolean);
  
  console.log('Trying channel IDs:', possibleIds);
  
  let foundChannelId = null;
  for (const id of possibleIds) {
    if (epgData[id] && epgData[id].length > 0) {
      foundChannelId = id;
      console.log('Found EPG data for channel ID:', id);
      break;
    }
  }
  
  // If still not found, try partial matching
  if (!foundChannelId) {
    const baseId = channelId.replace(/\.(us|es|ec)$/, '').toLowerCase();
    for (const epgId of Object.keys(epgData)) {
      if (epgId.toLowerCase().includes(baseId) || baseId.includes(epgId.toLowerCase())) {
        if (epgData[epgId] && epgData[epgId].length > 0) {
          foundChannelId = epgId;
          console.log('Found EPG data via partial match:', epgId);
          break;
        }
      }
    }
  }
  
  if (!foundChannelId) {
    console.log('No EPG data found for any channel ID variation');
    return null;
  }
  
  const now = new Date();
  const programs = epgData[foundChannelId];
  
  // Find current program
  const currentProgram = programs.find(prog => 
    prog.start <= now && prog.stop > now
  );
  
  if (currentProgram) {
    // Find next program
    const nextProgram = programs.find(prog => 
      prog.start >= currentProgram.stop
    );
    
    return {
      current: currentProgram,
      next: nextProgram || null
    };
  }
  
  return null;
}

/**
 * Formats time for display
 */
function formatTime(date) {
  return date.toLocaleTimeString('en-US', { 
    hour: 'numeric', 
    minute: '2-digit',
    hour12: true 
  });
}

/**
 * Gets program duration in minutes
 */
function getProgramDuration(start, stop) {
  return Math.round((stop - start) / (1000 * 60));
}



// --- Utility Functions ---

/**
 * Adjusts the main container's padding to make space for the remote on mobile.
 */
function adjustLayoutForRemote() {
  // This function is no longer needed with the new layout
  // The remote is now part of the sidebar on desktop
  console.log('adjustLayoutForRemote called - no longer needed');
}

/**
 * Shows the remote control and adjusts the layout.
 */
function showRemote() {
  remoteControl.classList.add('visible');
  // Use a small timeout to ensure the element is visible before getting its height
  setTimeout(adjustLayoutForRemote, 50);
}

/**
 * Hides the remote control and adjusts the layout.
 */
function hideRemote() {
  remoteControl.classList.remove('visible');
  adjustLayoutForRemote();
}

/**
 * Toggles the visibility of the remote control.
 */
function toggleRemoteVisibility() {
  if (remoteControl.classList.contains('visible')) {
    hideRemote();
  } else {
    showRemote();
  }
}

function showStatus(msg, duration = 3000) {
  const statusElement = document.getElementById('status-message');
  if (statusElement) {
    statusElement.textContent = msg;
    statusElement.classList.add('show');
    
    setTimeout(() => {
      statusElement.classList.remove('show');
    }, duration);
  }
}

// Options Panel Functions
function showOptions() {
  if (optionsOverlay) {
    optionsOverlay.style.display = 'flex';
    loadEPGSettings();
    updateOptionsDisplay();
  }
}

function hideOptions() {
  if (optionsOverlay) {
    optionsOverlay.style.display = 'none';
  }
}

function updateOptionsDisplay() {
  // Update EPG status
  if (epgStatus) {
    if (epgLoading) {
      epgStatus.textContent = 'Loading...';
      epgStatus.className = 'epg-status loading';
    } else if (epgData && Object.keys(epgData).length > 0) {
      epgStatus.textContent = 'Available';
      epgStatus.className = 'epg-status available';
    } else {
      epgStatus.textContent = 'Unavailable';
      epgStatus.className = 'epg-status unavailable';
    }
  }
  
  // Update EPG last update time
  if (epgLastUpdateDisplay) {
    if (epgLastUpdate) {
      const date = new Date(epgLastUpdate);
      epgLastUpdateDisplay.textContent = date.toLocaleString();
    } else {
      epgLastUpdateDisplay.textContent = 'Never';
    }
  }
  
  // Update connection status
  if (connectionStatusDisplay) {
    connectionStatusDisplay.textContent = navigator.onLine ? 'Online' : 'Offline';
    connectionStatusDisplay.style.borderColor = navigator.onLine ? 'var(--success-color)' : 'var(--error-color)';
    connectionStatusDisplay.style.color = navigator.onLine ? 'var(--success-color)' : 'var(--error-color)';
  }
}

async function reloadEPGData() {
  if (epgLoading) {
    showStatus('EPG already loading...', 2000);
    return;
  }
  
  showStatus('Reloading EPG data...', 2000);
  console.log('Manual EPG reload requested');
  
  // Clear cache to force fresh fetch
  localStorage.removeItem('glz-epg-cache');
  localStorage.removeItem('glz-epg-cache-time');
  
  // Update display to show loading
  updateOptionsDisplay();
  
  try {
    await fetchEPGData();
    showStatus('EPG data reloaded successfully!', 3000);
    updateOptionsDisplay();
  } catch (error) {
    console.error('Manual EPG reload failed:', error);
    showStatus('EPG reload failed', 3000);
    updateOptionsDisplay();
  }
}

function clearAllCache() {
  showStatus('Clearing cache...', 2000);
  
  // Clear all localStorage items
  const keysToKeep = ['glz-favorites', 'glz-last-channel'];
  const keysToRemove = [];
  
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key && !keysToKeep.includes(key)) {
      keysToRemove.push(key);
    }
  }
  
  keysToRemove.forEach(key => localStorage.removeItem(key));
  
  showStatus('Cache cleared successfully!', 3000);
  updateOptionsDisplay();
}

// EPG Settings Management
function loadEPGSettings() {
  // Load EPG source
  const savedSource = localStorage.getItem('glz-epg-source') || 'epgshare01';
  if (epgSourceSelect) {
    epgSourceSelect.value = savedSource;
  }
  
  // Load custom URL
  const customUrl = localStorage.getItem('glz-epg-custom-url');
  if (epgUrlInput && customUrl) {
    epgUrlInput.value = customUrl;
  }
  
  // Load auto refresh setting
  const autoRefresh = localStorage.getItem('glz-epg-auto-refresh') !== 'false';
  if (epgAutoRefresh) {
    epgAutoRefresh.checked = autoRefresh;
  }
  
  // Load cache duration
  const cacheDuration = localStorage.getItem('glz-epg-cache-duration') || '30';
  if (cacheDurationSelect) {
    cacheDurationSelect.value = cacheDuration;
  }
  
  // Update EPG URL based on settings
  updateEPGURL();
  
  // Show/hide custom URL input
  updateCustomUrlVisibility();
}

function saveEPGSettings() {
  // Save EPG source
  if (epgSourceSelect) {
    localStorage.setItem('glz-epg-source', epgSourceSelect.value);
  }
  
  // Save custom URL
  if (epgUrlInput) {
    localStorage.setItem('glz-epg-custom-url', epgUrlInput.value);
  }
  
  // Save auto refresh setting
  if (epgAutoRefresh) {
    localStorage.setItem('glz-epg-auto-refresh', epgAutoRefresh.checked.toString());
  }
  
  // Save cache duration
  if (cacheDurationSelect) {
    localStorage.setItem('glz-epg-cache-duration', cacheDurationSelect.value);
  }
  
  // Update EPG URL
  updateEPGURL();
  
  showStatus('EPG settings saved', 2000);
}

function updateEPGURL() {
  if (!epgSourceSelect) return;
  
  const selectedSource = epgSourceSelect.value;
  
  if (selectedSource === 'custom' && epgUrlInput) {
    EPG_URL = epgUrlInput.value.trim();
  } else if (EPG_SOURCES[selectedSource]) {
    EPG_URL = EPG_SOURCES[selectedSource];
  }
  
  console.log('EPG URL updated to:', EPG_URL);
}

function updateCustomUrlVisibility() {
  if (!epgSourceSelect || !customEpgUrlItem) return;
  
  if (epgSourceSelect.value === 'custom') {
    customEpgUrlItem.style.display = 'flex';
  } else {
    customEpgUrlItem.style.display = 'none';
  }
}

async function changeEPGSource() {
  saveEPGSettings();
  
  // Clear current EPG cache
  localStorage.removeItem('glz-epg-cache');
  localStorage.removeItem('glz-epg-cache-time');
  
  showStatus('EPG source changed, reloading data...', 2000);
  
  // Reload EPG data with new source
  try {
    await fetchEPGData();
    showStatus('EPG data loaded from new source!', 3000);
  } catch (error) {
    console.error('Failed to load EPG from new source:', error);
    showStatus('Failed to load EPG from new source', 3000);
  }
}
function showLoading() {
  if (loadingIndicator) {
    loadingIndicator.style.display = 'flex';
    // Update loading text with current channel info
    const loadingText = loadingIndicator.querySelector('.loading-text');
    if (loadingText && channels[current]) {
      loadingText.textContent = `Loading ${channels[current].name}...`;
    }
  }
}
function hideLoading() {
  loadingIndicator.style.display = 'none';
}
function updateTime() {
  const now = new Date();
  let hours = now.getHours();
  const minutes = now.getMinutes().toString().padStart(2, '0');
  const ampm = hours >= 12 ? 'PM' : 'AM';
  hours = hours % 12;
  hours = hours ? hours : 12; // the hour '0' should be '12'
  const timeString = `${hours}:${minutes} ${ampm}`;
  
  // Update desktop time display
  timeDisplay.textContent = timeString;
  

}
setInterval(updateTime, 1000);

// --- Enhanced Utility Functions ---

function updateChannelDisplay(idx = current) {
  if (!channels.length || !channels[idx]) {
    // Update desktop elements
    channelNumber.textContent = '000';
    channelName.textContent = 'STANDBY';
    // Hide logo and show placeholder
    document.getElementById('channel-logo-img').style.display = 'none';
    document.getElementById('channel-logo-placeholder').style.display = 'block';
    document.getElementById('channel-logo-placeholder').textContent = 'TV';
    
    // Update mobile elements
    const mobileChannelNumber = document.getElementById('mobile-channel-number');
    const mobileChannelName = document.getElementById('mobile-channel-name');
    const mobileLogoImg = document.getElementById('mobile-channel-logo-img');
    const mobileLogoPlaceholder = document.getElementById('mobile-channel-logo-placeholder');
    
    if (mobileChannelNumber) mobileChannelNumber.textContent = '000';
    if (mobileChannelName) mobileChannelName.textContent = 'STANDBY';
    if (mobileLogoImg) mobileLogoImg.style.display = 'none';
    if (mobileLogoPlaceholder) {
      mobileLogoPlaceholder.style.display = 'block';
      mobileLogoPlaceholder.textContent = 'TV';
    }
    return;
  }
  
  const channel = channels[idx];
  
  // Add animation to channel number
  channelNumber.classList.add('scale-in');
  setTimeout(() => channelNumber.classList.remove('scale-in'), 300);
  
  channelNumber.textContent = channel.chno ? channel.chno : (idx + 1).toString().padStart(3, '0');
  channelName.textContent = channel.name;
  
  // Update mobile elements
  const mobileChannelNumber = document.getElementById('mobile-channel-number');
  const mobileChannelName = document.getElementById('mobile-channel-name');
  if (mobileChannelNumber) mobileChannelNumber.textContent = channel.chno ? channel.chno : (idx + 1).toString().padStart(3, '0');
  if (mobileChannelName) mobileChannelName.textContent = channel.name;
  
  // Update channel logo with animation (desktop)
  const logoImg = document.getElementById('channel-logo-img');
  const logoPlaceholder = document.getElementById('channel-logo-placeholder');
  
  if (channel.logo && channel.logo.trim()) {
    logoImg.classList.add('fade-in');
    logoImg.src = channel.logo;
    logoImg.style.display = 'block';
    logoPlaceholder.style.display = 'none';
    setTimeout(() => logoImg.classList.remove('fade-in'), 300);
    
    // Handle logo load error
    logoImg.onerror = () => {
      logoImg.style.display = 'none';
      logoPlaceholder.style.display = 'block';
      logoPlaceholder.textContent = channel.name.substring(0, 3);
    };
  } else {
    logoImg.style.display = 'none';
    logoPlaceholder.style.display = 'block';
    logoPlaceholder.textContent = channel.name.substring(0, 3);
    logoPlaceholder.classList.add('fade-in');
    setTimeout(() => logoPlaceholder.classList.remove('fade-in'), 300);
  }
  
  // Update mobile logo
  const mobileLogoImg = document.getElementById('mobile-channel-logo-img');
  const mobileLogoPlaceholder = document.getElementById('mobile-channel-logo-placeholder');
  
  if (mobileLogoImg && mobileLogoPlaceholder) {
    if (channel.logo && channel.logo.trim()) {
      mobileLogoImg.src = channel.logo;
      mobileLogoImg.style.display = 'block';
      mobileLogoPlaceholder.style.display = 'none';
      
      // Handle logo load error
      mobileLogoImg.onerror = () => {
        mobileLogoImg.style.display = 'none';
        mobileLogoPlaceholder.style.display = 'block';
        mobileLogoPlaceholder.textContent = channel.name.substring(0, 3);
      };
    } else {
      mobileLogoImg.style.display = 'none';
      mobileLogoPlaceholder.style.display = 'block';
      mobileLogoPlaceholder.textContent = channel.name.substring(0, 3);
    }
  }
  
  // EPG functionality removed
  
  // Update favorites button state
  updateFavoritesButton();
}

/**
 * Test if a stream URL is accessible
 */


/**
 * Updates mobile current program display
 */
function updateMobileCurrentProgram(channelId) {
  const mobileCurrentProgram = document.getElementById('mobile-current-program');
  if (!mobileCurrentProgram) return;
  
  const programInfo = getCurrentProgram(channelId);
  
  if (programInfo && programInfo.current) {
    mobileCurrentProgram.textContent = programInfo.current.title;
    mobileCurrentProgram.style.display = 'block';
  } else {
    mobileCurrentProgram.textContent = '';
    mobileCurrentProgram.style.display = 'none';
  }
}

/**
 * Updates EPG display for a channel
 */
function updateEPGDisplay(channelId) {
  if (!channelId) {
    return;
  }
  
  const programInfo = getCurrentProgram(channelId);
  
  const epgElement = document.getElementById('epg-info');
  
  if (!epgElement) {
    return;
  }
  
  if (programInfo && programInfo.current) {
    const current = programInfo.current;
    const next = programInfo.next;
    
    const now = new Date();
    const progress = ((now - current.start) / (current.stop - current.start)) * 100;
    
    epgElement.innerHTML = `
      <div class="epg-current">
        <div class="epg-title">${current.title}</div>
        <div class="epg-time">${formatTime(current.start)} - ${formatTime(current.stop)} (${getProgramDuration(current.start, current.stop)}min)</div>
        <div class="epg-progress">
          <div class="epg-progress-bar" style="width: ${progress}%"></div>
        </div>
      </div>
      ${next ? `
        <div class="epg-next">
          <div class="epg-next-label">Next:</div>
          <div class="epg-next-title">${next.title}</div>
          <div class="epg-next-time">${formatTime(next.start)} - ${formatTime(next.stop)}</div>
        </div>
      ` : ''}
    `;
    epgElement.style.display = 'block';
    epgElement.classList.add('show');
  } else {
    epgElement.classList.remove('show');
    setTimeout(() => {
      if (!epgElement.classList.contains('show')) {
        epgElement.style.display = 'none';
      }
    }, 300);
  }
}

/**
 * Updates the header with current program information
 */
function updateHeaderProgram(channelId) {
  if (!currentProgram) return;
  
  const programInfo = getCurrentProgram(channelId);
  
  if (programInfo && programInfo.current) {
    const current = programInfo.current;
    const now = new Date();
    const progress = ((now - current.start) / (current.stop - current.start)) * 100;
    
    currentProgram.innerHTML = `
      <span class="program-title">${current.title}</span>
      <span class="program-time">${formatTime(current.start)} - ${formatTime(current.stop)}</span>
    `;
    currentProgram.classList.add('show');
    
    // Update mobile program display
    const mobileCurrentProgram = document.getElementById('mobile-current-program');
    if (mobileCurrentProgram) {
      mobileCurrentProgram.textContent = current.title;
    }
  } else {
    currentProgram.innerHTML = '';
    currentProgram.classList.remove('show');
    
    // Clear mobile program display
    const mobileCurrentProgram = document.getElementById('mobile-current-program');
    if (mobileCurrentProgram) {
      mobileCurrentProgram.textContent = '';
    }
  }
}

/**
 * Theme Management
 */
function initTheme() {
  const savedTheme = localStorage.getItem('glz-theme') || 'light-glass';
  setTheme(savedTheme);
}

/**
 * Mini Guide Management
 */
function showMiniGuide() {
  if (!miniGuide) return;
  
  // Get current channel and nearby channels
  const currentChannel = channels[current];
  const nearbyChannels = getNearbyChannels(current, 5); // Get 5 channels before and after
  
  // Populate mini guide content
  miniGuideContent.innerHTML = nearbyChannels.map((channel, index) => {
    const isActive = channel === currentChannel;
    const programInfo = getCurrentProgram(channel.id);
    const programTitle = programInfo?.current?.title || 'No program info';
    
    return `
      <div class="mini-guide-item ${isActive ? 'active' : ''}" data-channel-index="${channels.indexOf(channel)}">
        <div class="mini-guide-channel-logo">
          ${channel.logo ? `<img src="${channel.logo}" alt="${channel.name}">` : '<div class="channel-logo-placeholder">TV</div>'}
        </div>
        <div class="mini-guide-channel-info">
          <div class="mini-guide-channel-number">${channel.chno || (channels.indexOf(channel) + 1)}</div>
          <div class="mini-guide-channel-name">${channel.name}</div>
          <div class="mini-guide-program">${programTitle}</div>
        </div>
      </div>
    `;
  }).join('');
  
  // Add click handlers
  miniGuideContent.querySelectorAll('.mini-guide-item').forEach((item, index) => {
    item.style.animationDelay = `${index * 0.1}s`;
    item.classList.add('slide-in-right');
    
    item.addEventListener('click', () => {
      const channelIndex = parseInt(item.dataset.channelIndex);
      if (channelIndex !== current) {
        playChannel(channelIndex);
      }
      hideMiniGuide();
    });
  });
  
  miniGuide.classList.add('show', 'scale-in');
}

function hideMiniGuide() {
  if (!miniGuide) return;
  miniGuide.classList.remove('show');
}

function toggleMobileChannelList() {
  if (!mobileChannelList || !mobileChannelToggle) return;
  
  const isVisible = mobileChannelList.style.display === 'block';
  
  if (isVisible) {
    mobileChannelList.style.display = 'none';
    mobileChannelToggle.classList.remove('active');
  } else {
    mobileChannelList.style.display = 'block';
    mobileChannelToggle.classList.add('active');
    // Re-render the list to ensure it's up to date
    renderMobileChannelList();
  }
}

function getNearbyChannels(centerIndex, range) {
  const start = Math.max(0, centerIndex - range);
  const end = Math.min(channels.length, centerIndex + range + 1);
  return channels.slice(start, end);
}

/**
 * Collapsible Sections Management
 */
function initCollapsibleSections() {
  const sectionHeaders = document.querySelectorAll('.section-header');
  
  sectionHeaders.forEach(header => {
    header.addEventListener('click', () => {
      const targetId = header.getAttribute('data-target');
      const content = document.getElementById(targetId);
      const section = header.closest('.collapsible');
      
      if (content && section) {
        section.classList.toggle('collapsed');
        
        // Save state to localStorage
        const sectionName = targetId.replace('-content', '');
        const isCollapsed = section.classList.contains('collapsed');
        localStorage.setItem(`glz-collapsed-${sectionName}`, isCollapsed);
      }
    });
  });
  
  // Restore collapsed state from localStorage
  const collapsibleSections = document.querySelectorAll('.collapsible');
  collapsibleSections.forEach(section => {
    const content = section.querySelector('.section-content');
    if (content) {
      const sectionName = content.id.replace('-content', '');
      const isCollapsed = localStorage.getItem(`glz-collapsed-${sectionName}`) === 'true';
      if (isCollapsed) {
        section.classList.add('collapsed');
      }
    }
  });
}

function setTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('glz-theme', theme);
  updateThemeButton(theme);
}

function toggleTheme() {
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light-glass';
  const newTheme = currentTheme === 'dark' ? 'light-glass' : 'dark';
  setTheme(newTheme);
}

function updateThemeButton(theme) {
  if (!themeToggleBtn || !themeToggleText) return;
  
  if (theme === 'dark') {
    themeToggleText.textContent = 'Light Mode';
    themeToggleBtn.querySelector('svg').innerHTML = '<circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>';
  } else {
    themeToggleText.textContent = 'Dark Mode';
    themeToggleBtn.querySelector('svg').innerHTML = '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>';
  }
}

// Favorites Management
function toggleFavorite() {
  if (!channels[current]) return;
  
  const channelId = channels[current].chno || current.toString();
  const index = favorites.indexOf(channelId);
  
  if (index > -1) {
    favorites.splice(index, 1);
    showStatus('Removed from favorites');
  } else {
    favorites.push(channelId);
    showStatus('Added to favorites');
  }
  
  localStorage.setItem('glz-favorites', JSON.stringify(favorites));
  updateFavoritesButton();
}

function updateFavoritesButton() {
  if (!channels[current]) return;
  
  const channelId = channels[current].chno || current.toString();
  const isFavorite = favorites.includes(channelId);
  
  favoritesBtn.classList.toggle('active', isFavorite);
}

function showFavorites() {
  if (favorites.length === 0) {
    showStatus('No favorites yet. Add channels to favorites first.');
    return;
  }
  
  const favoriteChannels = channels.filter((ch, idx) => {
    const channelId = ch.chno || (idx + 1).toString();
    return favorites.includes(channelId);
  });
  
  if (favoriteChannels.length === 0) {
    showStatus('No favorite channels found');
    return;
  }
  
  // Show favorites in guide
  showGuide();
  renderChannelList('', favoriteChannels);
  showStatus(`Showing ${favoriteChannels.length} favorite channels`);
}

// Volume Controls
function adjustVolume(delta) {
  const currentVolume = video.volume || audio.volume || 0;
  const newVolume = Math.max(0, Math.min(1, currentVolume + delta));
  
  video.volume = newVolume;
  audio.volume = newVolume;
  
  showStatus(`Volume: ${Math.round(newVolume * 100)}%`);
}

function toggleMute() {
  isMuted = !isMuted;
  video.muted = isMuted;
  audio.muted = isMuted;
  
  muteBtn.classList.toggle('muted', isMuted);
  showStatus(isMuted ? 'Muted' : 'Unmuted');
}

// Channel Navigation
function goToLastChannel() {
  if (lastChannel !== current && channels[lastChannel]) {
    const temp = current;
    current = lastChannel;
    lastChannel = temp;
    playChannel(current);
    showStatus('Last channel');
  }
}

function toggleClosedCaptions() {
  const video = document.getElementById('video');
  if (video) {
    if (video.textTracks.length > 0) {
      const track = video.textTracks[0];
      track.mode = track.mode === 'showing' ? 'hidden' : 'showing';
      showStatus(track.mode === 'showing' ? 'Closed captions enabled' : 'Closed captions disabled');
    } else {
      showStatus('No closed captions available');
    }
  }
}

// Connection Health Monitoring
function updateConnectionStatus() {
  const isConnected = navigator.onLine && connectionStatus === 'connected';
  // connectionStatusBtn.classList.toggle('connected', isConnected); // Removed from new layout
  // connectionStatusBtn.classList.toggle('disconnected', !isConnected); // Removed from new layout
  
  // Only show connection issues if we're not already showing a status
  if (!isConnected && !document.getElementById('status-message').classList.contains('show')) {
    showStatus('Connection issues detected', 2000);
  }
}

// PWA Installation
function setupPWA() {
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    pwaInstallPrompt = e;
    // pwaInstallBtn.style.display = 'flex'; // PWA button removed from new layout
  });
  
  window.addEventListener('appinstalled', () => {
    pwaInstallPrompt = null;
    // pwaInstallBtn.style.display = 'none'; // PWA button removed from new layout
    showStatus('App installed successfully!');
  });
}

function installPWA() {
  if (pwaInstallPrompt) {
    pwaInstallPrompt.prompt();
  } else {
    showStatus('Installation not available');
  }
}

// Touch Gestures
function setupTouchGestures() {
  let startX = 0;
  let startY = 0;
  let startTime = 0;
  let isVideoControl = false;
  
  if (!videoContainer) {
    console.warn('videoContainer not found, skipping touch gestures');
    return;
  }
  
  videoContainer.addEventListener('touchstart', (e) => {
    // Don't interfere with video controls
    if (e.target.tagName === 'VIDEO' || e.target.tagName === 'AUDIO') {
      isVideoControl = true;
      return;
    }
    
    startX = e.touches[0].clientX;
    startY = e.touches[0].clientY;
    startTime = Date.now();
    isVideoControl = false;
  });
  
  videoContainer.addEventListener('touchend', (e) => {
    if (standby || isVideoControl) return;
    
    const endX = e.changedTouches[0].clientX;
    const endY = e.changedTouches[0].clientY;
    const endTime = Date.now();
    const deltaX = endX - startX;
    const deltaY = endY - startY;
    const deltaTime = endTime - startTime;
    
    // Minimum swipe distance and time
    if (deltaTime < 300 && Math.abs(deltaX) > 50 && Math.abs(deltaY) < 100) {
      if (deltaX > 0) {
        // Swipe right - previous channel
        playChannel((current - 1 + channels.length) % channels.length);
      } else {
        // Swipe left - next channel
        playChannel((current + 1) % channels.length);
      }
    }
  });
}

// Enhanced Channel Input
function handleChannelInput(num) {
  channelInput += num;
  channelNumber.textContent = channelInput.padStart(3, '0');
  
  // Auto-enter after 3 digits
  if (channelInput.length >= 3) {
    setTimeout(() => {
      const channelNum = parseInt(channelInput);
      const channelIndex = channels.findIndex(ch => ch.chno === channelInput) || 
                          (channelNum > 0 && channelNum <= channels.length ? channelNum - 1 : -1);
      
      if (channelIndex >= 0) {
        playChannel(channelIndex);
      } else {
        showStatus('Channel not found');
        updateChannelDisplay();
      }
      channelInput = '';
    }, 1000);
  }
}

function stopPlayback() {
  console.log('Stopping playback');
  
  if (hls) {
    hls.destroy();
    hls = null;
  }
  
  video.pause();
  audio.pause();
  
  video.classList.remove('loading', 'playing');
  audio.classList.remove('loading', 'playing');
  
  // Clear sources immediately
  video.src = '';
  audio.src = '';
}

function playChannel(idx) {
  console.log('playChannel called with index:', idx);
  
  // Check if video and audio elements exist
  if (!video || !audio) {
    console.error('Video or audio elements not found');
    return;
  }
  
  if (!channels[idx]) {
    console.error('No channel found for index:', idx);
    return;
  }

  const channel = channels[idx];
  console.log('Playing channel:', channel.name, 'URL:', channel.url);

  // Save current channel
  if (current !== idx) {
    lastChannel = current;
    localStorage.setItem('glz-last-channel', current.toString());
  }
  current = idx;

  // Update UI
  updateChannelDisplay();
  showLoading();

  // Stop any current playback
  if (hls) {
    hls.destroy();
    hls = null;
  }
  video.pause();
  audio.pause();
  video.src = '';
  audio.src = '';

  // Determine stream type
  const isAudio = /\.(mp3|aac|m4a|ogg|flac|wav)(\?|$)/i.test(channel.url) || 
                  channel.url.includes('icy') || 
                  channel.url.includes('audio');
  const isHls = /\.m3u8(\?|$)/i.test(channel.url);

  console.log('Stream type - isAudio:', isAudio, 'isHls:', isHls);

  // Set display
  video.style.display = isAudio ? 'none' : 'block';
  audio.style.display = isAudio ? 'block' : 'none';

  // Update status
  connectionStatus = 'connecting';
  updateConnectionStatus();

  if (isAudio) {
    // Audio stream
    console.log('Playing audio stream');
    audio.src = channel.url;
    audio.load();
    audio.play()
      .then(() => {
        console.log('Audio playback started successfully');
        connectionStatus = 'connected';
        updateConnectionStatus();
        hideLoading();
        audio.classList.add('playing');
        setTimeout(() => showChannelBanner(), 500);
      })
      .catch(error => {
        console.error('Audio playback failed:', error);
        connectionStatus = 'error';
        updateConnectionStatus();
        hideLoading();
        showStatus('Failed to play audio stream');
      });
  } else if (isHls && window.Hls && Hls.isSupported()) {
    // HLS stream
    console.log('Playing HLS stream with HLS.js');
    hls = new Hls({
      enableWorker: true,
      lowLatencyMode: true,
      backBufferLength: 90
    });

    hls.loadSource(channel.url);
    hls.attachMedia(video);

    hls.on(Hls.Events.MANIFEST_LOADED, () => {
      console.log('HLS manifest loaded');
      video.play()
        .then(() => {
          console.log('HLS video playback started successfully');
          connectionStatus = 'connected';
          updateConnectionStatus();
          hideLoading();
          video.classList.add('playing');
          setTimeout(() => showChannelBanner(), 500);
        })
        .catch(error => {
          console.error('HLS video play failed:', error);
          connectionStatus = 'error';
          updateConnectionStatus();
          hideLoading();
          showStatus('Failed to play HLS stream');
        });
    });

    hls.on(Hls.Events.ERROR, (event, data) => {
      console.error('HLS error:', event, data);
      if (data.fatal) {
        connectionStatus = 'error';
        updateConnectionStatus();
        hideLoading();
        showStatus('HLS stream error');
      }
    });
  } else {
    // Direct video stream
    console.log('Playing direct video stream');
    video.src = channel.url;
    video.load();
    video.play()
      .then(() => {
        console.log('Direct video playback started successfully');
        connectionStatus = 'connected';
        updateConnectionStatus();
        hideLoading();
        video.classList.add('playing');
        setTimeout(() => showChannelBanner(), 500);
      })
      .catch(error => {
        console.error('Direct video playback failed:', error);
        connectionStatus = 'error';
        updateConnectionStatus();
        hideLoading();
        showStatus('Failed to play video stream');
      });
  }

  // Update mobile channel list
  renderMobileChannelList();
}

function setStandby(on) {
  standby = on;
  if (standby) {
    standbyScreen.classList.add('show');
    stopPlayback();
    channelNumber.textContent = '-- --';
    channelName.textContent = 'STANDBY';
    // Hide logo and show placeholder for standby
    const channelLogoImg = document.getElementById('channel-logo-img');
    const channelLogoPlaceholder = document.getElementById('channel-logo-placeholder');
    if (channelLogoImg) channelLogoImg.style.display = 'none';
    if (channelLogoPlaceholder) {
      channelLogoPlaceholder.style.display = 'block';
      channelLogoPlaceholder.textContent = 'TV';
    }
  } else {
    standbyScreen.classList.remove('show');
    if (channels.length > 0 && current >= 0 && current < channels.length) {
      playChannel(current);
    } else {
      updateChannelDisplay();
    }
  }
}

function showChannelBanner() {
  if (!channels[current]) return;
  
  const channel = channels[current];
  const programInfo = getCurrentProgram(channel.id);
  
  // Update banner content
  bannerChannel.textContent = channel.chno || (current + 1);
  bannerName.textContent = channel.name;
  
  // Add program information if available
  if (programInfo && programInfo.current) {
    const progressBar = document.getElementById('progress-bar');
    if (progressBar) {
      const now = new Date();
      const progress = Math.max(0, Math.min(100, ((now - programInfo.current.start) / (programInfo.current.stop - programInfo.current.start)) * 100));
      progressBar.style.width = `${progress}%`;
    }
  }
  
  // Show banner with smooth animation
  channelBanner.classList.add('show');
  
  // Hide banner after delay
  setTimeout(() => {
    if (channelBanner.classList.contains('show')) {
      channelBanner.classList.remove('show');
    }
  }, 3000);
}

function showGuide() {
  channelGuide.classList.add('open');
  renderChannelList();
  searchInput.value = '';
}
function hideGuide() {
  channelGuide.classList.remove('open');
}
function renderChannelList(filter = '') {
  console.log('Rendering channel list with', channels.length, 'channels');
  
  if (!channelList) {
    console.error('Channel list element not found');
    return;
  }
  
  const filtered = filter
    ? channels.filter(ch => ch.name.toLowerCase().includes(filter.toLowerCase()) || (ch.chno && ch.chno.includes(filter)))
    : channels;
    
  channelList.innerHTML = filtered.map((ch, i) => {
    return `<div class="channel-item${i === current ? ' active' : ''}" data-idx="${i}" tabindex="0">
      <img class="channel-logo" src="${ch.logo || ''}" onerror="this.style.display='none'" />
      <div class="channel-details">
        <div class="channel-number-small">${ch.chno || (i + 1)}</div>
        <div class="channel-name-small">${ch.name}</div>
      </div>
    </div>`;
  }).join('');
  
  channelList.querySelectorAll('.channel-item').forEach(item => {
    item.onclick = () => {
      playChannel(Number(item.dataset.idx));
    };
    item.onkeydown = (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        playChannel(Number(item.dataset.idx));
      }
    };
  });
  
  console.log('Channel list rendered with', channelList.children.length, 'items');
  console.log('Channel list element styles:', window.getComputedStyle(channelList));
  console.log('Channel list parent sidebar:', channelList.parentElement);
  console.log('Sidebar element:', channelList.parentElement.parentElement);
  console.log('Sidebar styles:', window.getComputedStyle(channelList.parentElement.parentElement));
  console.log('Main content styles:', window.getComputedStyle(document.querySelector('.main-content')));
  
  // Also render mobile channel list
  renderMobileChannelList(filter);
}

function renderMobileChannelList(filter = '') {
  if (!mobileChannelList) {
    console.warn('Mobile channel list element not found');
    return;
  }
  
  console.log('Rendering mobile channel list with', channels.length, 'channels');
  
  const filtered = filter
    ? channels.filter(ch => ch.name.toLowerCase().includes(filter.toLowerCase()) || (ch.chno && ch.chno.includes(filter)))
    : channels;
  
  mobileChannelList.innerHTML = filtered.map((ch, i) => {
    return `<div class="mobile-channel-item${i === current ? ' active' : ''}" data-idx="${i}">
      <div class="mobile-channel-logo">
        ${ch.logo ? `<img src="${ch.logo}" alt="${ch.name}" onerror="this.parentElement.innerHTML='<div class=\\'channel-logo-placeholder\\'>TV</div>'">` : '<div class="channel-logo-placeholder">TV</div>'}
      </div>
      <div class="mobile-channel-info">
        <div class="mobile-channel-number">${ch.chno || (i + 1)}</div>
        <div class="mobile-channel-name">${ch.name}</div>
      </div>
    </div>`;
  }).join('');
  
  // Add click handlers for mobile channel list
  mobileChannelList.querySelectorAll('.mobile-channel-item').forEach(item => {
    item.onclick = () => {
      playChannel(Number(item.dataset.idx));
    };
  });
  
  console.log('Mobile channel list rendered with', mobileChannelList.children.length, 'items');
}

// --- Enhanced Event Listeners ---
if (powerBtn) powerBtn.onclick = () => setStandby(!standby);
if (guideBtn) guideBtn.onclick = showGuide;
if (closeGuide) closeGuide.onclick = hideGuide;
if (toggleRemote) toggleRemote.onclick = toggleRemoteVisibility;
if (headerRemoteBtn) headerRemoteBtn.onclick = toggleRemoteVisibility;
if (mobileRemoteBtn) mobileRemoteBtn.onclick = toggleRemoteVisibility;
if (mobileChannelToggle) mobileChannelToggle.onclick = toggleMobileChannelList;

// Options panel controls
if (headerOptionsBtn) headerOptionsBtn.onclick = showOptions;
if (closeOptions) closeOptions.onclick = hideOptions;
if (reloadEpgBtn) reloadEpgBtn.onclick = reloadEPGData;
if (clearCacheBtn) clearCacheBtn.onclick = clearAllCache;
if (themeToggleBtn) themeToggleBtn.onclick = toggleTheme;
if (miniGuideClose) miniGuideClose.onclick = hideMiniGuide;

// EPG Settings controls
if (epgSourceSelect) {
  epgSourceSelect.onchange = () => {
    updateCustomUrlVisibility();
    changeEPGSource();
  };
}
if (epgUrlInput) {
  epgUrlInput.onchange = saveEPGSettings;
  epgUrlInput.onblur = saveEPGSettings;
}
if (epgAutoRefresh) {
  epgAutoRefresh.onchange = saveEPGSettings;
}
if (cacheDurationSelect) {
  cacheDurationSelect.onchange = saveEPGSettings;
}

// Channel navigation
if (prevBtn) prevBtn.onclick = () => {
  if (!standby && channels.length > 0) {
    lastChannel = current;
    playChannel((current - 1 + channels.length) % channels.length);
  }
};
if (nextBtn) nextBtn.onclick = () => {
  if (!standby && channels.length > 0) {
    lastChannel = current;
    playChannel((current + 1) % channels.length);
  }
};

// Enhanced remote controls
if (volumeDownBtn) volumeDownBtn.onclick = () => adjustVolume(-0.1);
if (volumeUpBtn) volumeUpBtn.onclick = () => adjustVolume(0.1);
if (muteBtn) muteBtn.onclick = toggleMute;
if (channelDownBtn) channelDownBtn.onclick = () => {
  if (!standby && channels.length > 0) {
    lastChannel = current;
    playChannel((current - 1 + channels.length) % channels.length);
  }
};
if (channelUpBtn) channelUpBtn.onclick = () => {
  if (!standby && channels.length > 0) {
    lastChannel = current;
    playChannel((current + 1) % channels.length);
  }
};
if (lastChannelBtn) lastChannelBtn.onclick = goToLastChannel;
if (enterBtn) enterBtn.onclick = () => {
  if (channelInput) {
    const channelNum = parseInt(channelInput);
    const channelIndex = channels.findIndex(ch => ch.chno === channelInput) || 
                        (channelNum > 0 && channelNum <= channels.length ? channelNum - 1 : -1);
    
    if (channelIndex >= 0) {
      playChannel(channelIndex);
    } else {
      showStatus('Channel not found');
      updateChannelDisplay();
    }
    channelInput = '';
  }
};
if (favoritesBtn) favoritesBtn.onclick = toggleFavorite;
// PWA and connection status buttons removed from new layout

if (searchInput) searchInput.oninput = (e) => renderChannelList(e.target.value);

// Enhanced number pad
if (numpadBtns && numpadBtns.length > 0) {
  numpadBtns.forEach(btn => {
    if (btn) {
      btn.onclick = () => {
        if (standby) {
          showStatus('Power on first.');
          return;
        }
        
        if (btn.classList.contains('enter')) return; // Skip enter button
        
        const num = btn.dataset.num;
        handleChannelInput(num);
      };
    }
  });
}

// Keyboard navigation
window.addEventListener('keydown', (e) => {
  if (e.key === 'F1') toggleRemoteVisibility();
  if (e.key === 'F2') showGuide();
  if (e.key === 'F4') setStandby(!standby);
  if (e.key === 'ArrowUp') showGuide();
  if (e.key === 'ArrowLeft' && prevBtn) prevBtn.onclick();
  if (e.key === 'ArrowRight' && nextBtn) nextBtn.onclick();
  if (e.key === 'Escape') {
    hideRemote();
    hideGuide();
    hideOptions();
    helpOverlay.style.display = 'none';
  }
  if (e.key === '?') helpOverlay.style.display = 'flex';
  if (e.key === 'F3') showOptions();
if (e.key === 'F4') toggleTheme();
if (e.key === 'F5') setStandby(!standby);
if (e.key === 'G') showMiniGuide();
if (e.key === 'Escape') {
  hideMiniGuide();
  hideGuide();
  hideOptions();
  helpOverlay.style.display = 'none';
}
});

window.addEventListener('resize', adjustLayoutForRemote);

// closeHelp.onclick = () => helpOverlay.style.display = 'none';

// --- Liquid Glass Effects ---
function addLiquidEffects() {
  // Add ripple effect to buttons
  document.querySelectorAll('button').forEach(button => {
    button.addEventListener('click', function(e) {
      const ripple = document.createElement('span');
      const rect = this.getBoundingClientRect();
      const size = Math.max(rect.width, rect.height);
      const x = e.clientX - rect.left - size / 2;
      const y = e.clientY - rect.top - size / 2;
      
      ripple.style.width = ripple.style.height = size + 'px';
      ripple.style.left = x + 'px';
      ripple.style.top = y + 'px';
      ripple.classList.add('ripple');
      
      this.appendChild(ripple);
      
      setTimeout(() => {
        ripple.remove();
      }, 600);
    });
  });
}

// Add CSS for ripple effect
const rippleStyle = document.createElement('style');
rippleStyle.textContent = `
  button {
    position: relative;
    overflow: hidden;
  }
  
  .ripple {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.3);
    transform: scale(0);
    animation: ripple-animation 0.6s linear;
    pointer-events: none;
  }
  
  @keyframes ripple-animation {
    to {
      transform: scale(4);
      opacity: 0;
    }
  }
`;
document.head.appendChild(rippleStyle);

// --- Enhanced Channel Banner Animation ---
function showChannelBanner() {
  if (!channels[current]) return;
  // bannerChannel.textContent = channels[current].chno || (current + 1); // Removed from new layout
  // bannerName.textContent = channels[current].name; // Removed from new layout
  
  // Channel banner functionality removed from new layout
  console.log('Channel banner would show for:', channels[current].name);
}

// --- Enhanced Status Messages ---
// showStatus function is defined above

// --- Enhanced Initialization ---
(function init() {
  console.log('=== INITIALIZATION START ===');
  
  // Check for missing DOM elements
  console.log('Checking DOM elements...');
  const requiredElements = {
    'video': video,
    'audio': audio,
    'channelNumber': channelNumber,
    'channelName': channelName,
    'powerBtn': powerBtn,
    'guideBtn': guideBtn,
    'volumeDownBtn': volumeDownBtn,
    'volumeUpBtn': volumeUpBtn,
    'muteBtn': muteBtn,
    'channelDownBtn': channelDownBtn,
    'channelUpBtn': channelUpBtn,
    'lastChannelBtn': lastChannelBtn,
    'enterBtn': enterBtn,
    'favoritesBtn': favoritesBtn,
    // 'pwaInstallBtn': pwaInstallBtn, // Removed from new layout
    // 'connectionStatusBtn': connectionStatusBtn // Removed from new layout
  };
  
  Object.entries(requiredElements).forEach(([name, element]) => {
    if (!element) {
      console.warn(`Missing DOM element: ${name}`);
    }
  });
  
  channels = parseM3U(EMBEDDED_M3U);
  console.log('Channels loaded:', channels.length);
  
  if (channels.length > 0) {
    current = 0;
  } else {
    console.error('NO CHANNELS LOADED!');
  }
  
  // Load last channel from localStorage
  const lastChannelIndex = localStorage.getItem('glz-last-channel');
  if (lastChannelIndex && channels[parseInt(lastChannelIndex)]) {
    current = parseInt(lastChannelIndex);
  }
  
  setStandby(false); // Start with a channel instead of standby
  updateTime();
  updateChannelDisplay();
  
  // Actually start playing the current channel
  if (channels.length > 0 && current >= 0 && current < channels.length) {
    playChannel(current);
  }
  
  // Initialize channel lists
  console.log('About to render channel list, channelList element:', channelList);
  renderChannelList();
  renderMobileChannelList();
  
  // Add liquid glass effects
  addLiquidEffects();
  
  // Setup new features
  setupPWA();
  setupTouchGestures();
  initTheme();
  initCollapsibleSections();
  
  // Mobile-specific optimizations
  if ('ontouchstart' in window || window.innerWidth <= 768) {
    // Optimize touch targets
    document.body.classList.add('mobile-device');
    // Force mobile layout
    document.body.classList.add('mobile-layout');
  } else {
    // Desktop-specific optimizations
    document.body.classList.add('desktop-layout');
  }
  
  // Setup connection monitoring
  window.addEventListener('online', updateConnectionStatus);
  window.addEventListener('offline', updateConnectionStatus);
  updateConnectionStatus();
  
  // Add click handler to standby screen to exit standby
  if (standbyScreen) {
    standbyScreen.addEventListener('click', () => {
      if (standby) {
        setStandby(false);
      }
    });
  }
  
  // Check if power button is available
  if (!powerBtn) {
    console.error('Power button not found!');
  } else {
    // Power button functionality
    powerBtn.addEventListener('click', () => {
      if (isStandby) {
        setStandby(false);
        playChannel(currentChannelIndex);
      } else {
        setStandby(true);
        stopPlayback();
      }
    });
  }

  // Options button functionality
  if (headerOptionsBtn) {
    headerOptionsBtn.addEventListener('click', () => {
      showSettings();
    });
  }

  // Remote button event listeners
  const remoteButtons = {
    'channel-up-btn': () => {
      if (currentChannelIndex < channels.length - 1) {
        playChannel(currentChannelIndex + 1);
      }
    },
    'channel-down-btn': () => {
      if (currentChannelIndex > 0) {
        playChannel(currentChannelIndex - 1);
      }
    },
    'volume-up-btn': () => adjustVolume(0.1),
    'volume-down-btn': () => adjustVolume(-0.1),
    'mute-btn': () => toggleMute(),
    'last-channel-btn': () => goToLastChannel(),
    'cc-btn': () => toggleClosedCaptions()
  };

  // Add event listeners to remote buttons
  Object.entries(remoteButtons).forEach(([id, handler]) => {
    const button = document.getElementById(id);
    if (button) {
      button.addEventListener('click', handler);
    } else {
      console.warn(`Remote button not found: ${id}`);
    }
  });
  
  // EPG functionality removed
  
  // Enhanced help overlay content - removed from new layout
  
  // Add smooth entrance animation
  document.body.style.opacity = '0';
  setTimeout(() => {
    document.body.style.transition = 'opacity 1s ease-in-out';
    document.body.style.opacity = '1';
  }, 100);
  
  // Add keyboard handler to exit standby
  document.addEventListener('keydown', (e) => {
    if (standby && (e.key === ' ' || e.key === 'Enter')) {
      e.preventDefault();
      setStandby(false);
    }
    // Also allow any key to exit standby for easier testing
    if (standby && e.key !== 'F1' && e.key !== 'F2' && e.key !== 'F3' && e.key !== 'F4' && e.key !== 'F5') {
      setStandby(false);
    }
  });

  // Initialize settings and weather
  initSettings();
  initWeather();
})();

// Settings functionality
function showSettings() {
  const settingsOverlay = document.getElementById('settings-overlay');
  if (settingsOverlay) {
    settingsOverlay.classList.add('show');
    updateSettingsDisplay();
  }
}

function hideSettings() {
  const settingsOverlay = document.getElementById('settings-overlay');
  if (settingsOverlay) {
    settingsOverlay.classList.remove('show');
  }
}

function initSettings() {
  const closeSettingsBtn = document.getElementById('close-settings');
  const clearCacheBtn = document.getElementById('clear-cache-btn');
  const themeSelect = document.getElementById('theme-select');
  const weatherLocationInput = document.getElementById('weather-location-input');
  const updateWeatherBtn = document.getElementById('update-weather-btn');
  const weatherUnitsSelect = document.getElementById('weather-units-select');

  if (closeSettingsBtn) {
    closeSettingsBtn.addEventListener('click', hideSettings);
  }

  if (clearCacheBtn) {
    clearCacheBtn.addEventListener('click', () => {
      clearAllCache();
      showStatus('Cache cleared successfully');
    });
  }

  if (themeSelect) {
    themeSelect.addEventListener('change', (e) => {
      setTheme(e.target.value);
      showStatus('Theme updated');
    });
  }

  if (updateWeatherBtn) {
    updateWeatherBtn.addEventListener('click', () => {
      const location = weatherLocationInput.value.trim();
      if (location) {
        localStorage.setItem('glz-weather-location', location);
        fetchWeather(location);
        showStatus('Weather location updated');
      }
    });
  }

  if (weatherUnitsSelect) {
    weatherUnitsSelect.addEventListener('change', (e) => {
      localStorage.setItem('glz-weather-units', e.target.value);
      const location = localStorage.getItem('glz-weather-location') || 'San Juan, PR';
      fetchWeather(location);
      showStatus('Weather units updated');
    });
  }

  // Close settings on overlay click
  const settingsOverlay = document.getElementById('settings-overlay');
  if (settingsOverlay) {
    settingsOverlay.addEventListener('click', (e) => {
      if (e.target === settingsOverlay) {
        hideSettings();
      }
    });
  }
}

function updateSettingsDisplay() {
  const lastChannelDisplay = document.getElementById('last-channel-display');
  const channelCountDisplay = document.getElementById('channel-count-display');
  const themeSelect = document.getElementById('theme-select');
  const weatherLocationInput = document.getElementById('weather-location-input');
  const weatherUnitsSelect = document.getElementById('weather-units-select');

  if (lastChannelDisplay) {
    const lastChannel = localStorage.getItem('glz-last-channel');
    if (lastChannel && channels[parseInt(lastChannel)]) {
      const channel = channels[parseInt(lastChannel)];
      lastChannelDisplay.textContent = `${channel.number} - ${channel.name}`;
    } else {
      lastChannelDisplay.textContent = 'None';
    }
  }

  if (channelCountDisplay) {
    channelCountDisplay.textContent = channels.length;
  }

  if (themeSelect) {
    const currentTheme = localStorage.getItem('glz-theme') || 'auto';
    themeSelect.value = currentTheme;
  }

  if (weatherLocationInput) {
    const location = localStorage.getItem('glz-weather-location') || 'San Juan, PR';
    weatherLocationInput.value = location;
  }

  if (weatherUnitsSelect) {
    const units = localStorage.getItem('glz-weather-units') || 'imperial';
    weatherUnitsSelect.value = units;
  }
}

// Weather functionality
function initWeather() {
  const location = localStorage.getItem('glz-weather-location') || 'San Juan, PR';
  fetchWeather(location);
  
  // Update weather every 30 minutes
  setInterval(() => {
    const currentLocation = localStorage.getItem('glz-weather-location') || 'San Juan, PR';
    fetchWeather(currentLocation);
  }, 30 * 60 * 1000);
}

async function fetchWeather(location) {
  try {
    const units = localStorage.getItem('glz-weather-units') || 'imperial';
    const apiKey = 'c464a91ff8b9454fe6721e52a798faae';
    
    // Fetch current weather
    const currentResponse = await fetch(`https://api.openweathermap.org/data/2.5/weather?q=${location}&units=${units}&appid=${apiKey}`);
    
    if (!currentResponse.ok) {
      throw new Error(`Weather API error: ${currentResponse.status}`);
    }
    
    const currentWeather = await currentResponse.json();
    
    // Fetch 5-day forecast
    const forecastResponse = await fetch(`https://api.openweathermap.org/data/2.5/forecast?q=${location}&units=${units}&appid=${apiKey}`);
    
    if (!forecastResponse.ok) {
      throw new Error(`Forecast API error: ${forecastResponse.status}`);
    }
    
    const forecastData = await forecastResponse.json();
    
    updateWeatherDisplay(currentWeather, forecastData, units);
  } catch (error) {
    console.error('Weather fetch error:', error);
    updateWeatherDisplay(null, null, 'imperial');
  }
}

function updateWeatherDisplay(currentWeather, forecastData, units) {
  // Update header weather widget
  const weatherIcon = document.getElementById('weather-icon');
  const weatherTemp = document.getElementById('weather-temp');
  const weatherLocation = document.getElementById('weather-location');

  // Update bottom bar weather
  const weatherIconLarge = document.getElementById('weather-icon-large');
  const weatherTempLarge = document.getElementById('weather-temp-large');
  const weatherLocationLarge = document.getElementById('weather-location-large');
  const weatherDescription = document.getElementById('weather-description');
  const weatherHumidity = document.getElementById('weather-humidity');
  const weatherWind = document.getElementById('weather-wind');
  const weatherFeelsLike = document.getElementById('weather-feels-like');
  const weatherForecast = document.getElementById('weather-forecast');

  if (!currentWeather) {
    // Set fallback values
    const fallbackIcon = '🌤️';
    const fallbackTemp = '--°';
    const fallbackLocation = 'Weather unavailable';
    
    [weatherIcon, weatherIconLarge].forEach(el => { if (el) el.textContent = fallbackIcon; });
    [weatherTemp, weatherTempLarge].forEach(el => { if (el) el.textContent = fallbackTemp; });
    [weatherLocation, weatherLocationLarge].forEach(el => { if (el) el.textContent = fallbackLocation; });
    if (weatherDescription) weatherDescription.textContent = '--';
    if (weatherHumidity) weatherHumidity.textContent = '--%';
    if (weatherWind) weatherWind.textContent = '-- mph';
    if (weatherFeelsLike) weatherFeelsLike.textContent = '--°';
    if (weatherForecast) weatherForecast.innerHTML = '<div class="forecast-day"><div class="forecast-date">Forecast unavailable</div></div>';
    return;
  }

  // Icon mapping
  const iconMap = {
    'Clear': '☀️',
    'Clouds': '☁️',
    'Rain': '🌧️',
    'Snow': '❄️',
    'Thunderstorm': '⛈️',
    'Drizzle': '🌦️',
    'Mist': '🌫️',
    'Fog': '🌫️',
    'Haze': '🌫️'
  };

  const weatherIconText = iconMap[currentWeather.weather[0].main] || '🌤️';
  const temp = Math.round(currentWeather.main.temp);
  const unit = units === 'imperial' ? '°F' : '°C';
  const windUnit = units === 'imperial' ? 'mph' : 'm/s';
  const feelsLike = Math.round(currentWeather.main.feels_like);
  const humidity = currentWeather.main.humidity;
  const windSpeed = Math.round(currentWeather.wind.speed);

  // Update header widget
  if (weatherIcon) weatherIcon.textContent = weatherIconText;
  if (weatherTemp) weatherTemp.textContent = `${temp}${unit}`;
  if (weatherLocation) weatherLocation.textContent = currentWeather.name;

  // Update bottom bar
  if (weatherIconLarge) weatherIconLarge.textContent = weatherIconText;
  if (weatherTempLarge) weatherTempLarge.textContent = `${temp}${unit}`;
  if (weatherLocationLarge) weatherLocationLarge.textContent = currentWeather.name;
  if (weatherDescription) weatherDescription.textContent = currentWeather.weather[0].description;
  if (weatherHumidity) weatherHumidity.textContent = `${humidity}%`;
  if (weatherWind) weatherWind.textContent = `${windSpeed} ${windUnit}`;
  if (weatherFeelsLike) weatherFeelsLike.textContent = `${feelsLike}${unit}`;

  // Update forecast
  if (weatherForecast && forecastData) {
    updateForecastDisplay(forecastData, units);
  }
}

function updateForecastDisplay(forecastData, units) {
  const weatherForecast = document.getElementById('weather-forecast');
  if (!weatherForecast) return;

  // Group forecast by day and get daily high/low
  const dailyForecasts = {};
  const iconMap = {
    'Clear': '☀️',
    'Clouds': '☁️',
    'Rain': '🌧️',
    'Snow': '❄️',
    'Thunderstorm': '⛈️',
    'Drizzle': '🌦️',
    'Mist': '🌫️',
    'Fog': '🌫️',
    'Haze': '🌫️'
  };

  forecastData.list.forEach(item => {
    const date = new Date(item.dt * 1000);
    const day = date.toLocaleDateString('en-US', { weekday: 'short' });
    
    if (!dailyForecasts[day]) {
      dailyForecasts[day] = {
        high: -Infinity,
        low: Infinity,
        icon: item.weather[0].main,
        description: item.weather[0].description
      };
    }
    
    const temp = item.main.temp;
    if (temp > dailyForecasts[day].high) {
      dailyForecasts[day].high = temp;
      dailyForecasts[day].icon = item.weather[0].main;
    }
    if (temp < dailyForecasts[day].low) {
      dailyForecasts[day].low = temp;
    }
  });

  // Generate forecast HTML
  const unit = units === 'imperial' ? '°F' : '°C';
  const forecastHTML = Object.entries(dailyForecasts).slice(0, 5).map(([day, forecast]) => {
    const icon = iconMap[forecast.icon] || '🌤️';
    const high = Math.round(forecast.high);
    const low = Math.round(forecast.low);
    
    return `
      <div class="forecast-day">
        <div class="forecast-date">${day}</div>
        <div class="forecast-icon">${icon}</div>
        <div class="forecast-temp">
          <span class="forecast-high">${high}°</span>
          <span class="forecast-low">${low}°</span>
        </div>
      </div>
    `;
  }).join('');

  weatherForecast.innerHTML = forecastHTML;
} 
