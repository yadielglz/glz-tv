// GLZ TV - Modern STB App Logic
// Spectrum/AT&T inspired, modular, and maintainable

// --- Embedded M3U Content (move this to a separate file or fetch from server for production) ---
const EMBEDDED_M3U = `#EXTM3U url-tvg="https://epg.best/1eef8-shw7fc.xml.gz" x-tvg-url="https://epg.best/1eef8-shw7fc.xml.gz"
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
  const channels = [];
  const lines = m3uContent.split('\n');
  let currentChannel = null;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line.startsWith('#EXTINF:')) {
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
    } else if (line && !line.startsWith('#') && currentChannel) {
      currentChannel.url = line.trim();
      channels.push(currentChannel);
      currentChannel = null;
    }
  }
  return channels;
}

// --- State ---
let channels = [];
let current = 0;
let standby = true;
let hls = null;
let multiviewMode = false;
let multiviewStreams = [];

// --- DOM Elements ---
const video = document.getElementById('video');
const audio = document.getElementById('audio');
const channelNumber = document.getElementById('channel-number');
const channelName = document.getElementById('channel-name');
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
const guideBtn = document.getElementById('guide-btn');
const multiviewBtn = document.getElementById('multiview-btn');
const headerRemoteBtn = document.getElementById('header-remote-btn');
const numpadBtns = document.querySelectorAll('.numpad-btn');
const prevBtn = document.getElementById('prev-btn');
const nextBtn = document.getElementById('next-btn');
const statusMessage = document.getElementById('status-message');
const loadingIndicator = document.getElementById('loading-indicator');
const multiviewGrid = document.getElementById('multiview-grid');
const videoContainer = document.getElementById('video-container');
const helpOverlay = document.getElementById('help-overlay');
const closeHelp = document.getElementById('close-help');
const helpBody = document.getElementById('help-body');
const searchInput = document.getElementById('search-input');

// --- Utility Functions ---
function showStatus(msg, duration = 2000) {
  statusMessage.textContent = msg;
  statusMessage.style.display = '';
  setTimeout(() => {
    statusMessage.style.display = 'none';
  }, duration);
}
function showLoading() {
  loadingIndicator.style.display = 'flex';
}
function hideLoading() {
  loadingIndicator.style.display = 'none';
}
function updateTime() {
  const now = new Date();
  const hours = now.getHours().toString().padStart(2, '0');
  const minutes = now.getMinutes().toString().padStart(2, '0');
  timeDisplay.textContent = `${hours}:${minutes}`;
  dateDisplay.textContent = now.toLocaleDateString(undefined, { month: 'short', day: '2-digit' }).toUpperCase();
}
setInterval(updateTime, 1000);

function updateChannelDisplay(idx = current) {
  if (!channels.length || !channels[idx]) {
    channelNumber.textContent = '000';
    channelName.textContent = 'No Channel';
    return;
  }
  channelNumber.textContent = channels[idx].chno ? channels[idx].chno : (idx + 1).toString().padStart(3, '0');
  channelName.textContent = channels[idx].name;
}

function stopPlayback() {
  if (hls) { hls.destroy(); hls = null; }
  video.pause();
  video.src = '';
  audio.pause();
  audio.src = '';
}

function stopMultiviewStreams() {
  multiviewStreams.forEach(stream => {
    if (stream && stream.destroy) {
      stream.destroy();
    }
  });
  multiviewStreams = [];
  
  // Clear all multiview video elements
  const multiviewVideos = multiviewGrid.querySelectorAll('video');
  multiviewVideos.forEach(video => {
    video.pause();
    video.src = '';
  });
}

function startMultiview() {
  if (standby) {
    showStatus('Power on first.');
    return;
  }
  
  multiviewMode = true;
  stopPlayback(); // Stop single video
  multiviewGrid.style.display = '';
  video.style.display = 'none';
  audio.style.display = 'none';
  
  // Start 4 streams (channels 0-3)
  const startChannel = Math.max(0, current - 1); // Start from current channel - 1
  const multiviewVideos = multiviewGrid.querySelectorAll('video');
  
  for (let i = 0; i < 4; i++) {
    const channelIndex = (startChannel + i) % channels.length;
    const videoElement = multiviewVideos[i];
    const cell = multiviewGrid.children[i];
    
    if (channels[channelIndex] && videoElement) {
      const ch = channels[channelIndex];
      const isAudio = /\.(mp3|aac|m4a|ogg|flac|wav)(\?|$)/i.test(ch.url) || ch.url.includes('icy') || ch.url.includes('audio');
      
      if (isAudio) {
        // Skip audio channels in multiview
        continue;
      }
      
      const isHls = /\.m3u8(\?|$)/i.test(ch.url);
      
      if (isHls && window.Hls) {
        const hlsInstance = new Hls();
        hlsInstance.loadSource(ch.url);
        hlsInstance.attachMedia(videoElement);
        hlsInstance.on(Hls.Events.ERROR, function (event, data) {
          if (data.fatal) {
            console.error(`Multiview stream error for channel ${channelIndex}:`, data);
          }
        });
        multiviewStreams.push(hlsInstance);
      } else {
        videoElement.src = ch.url;
      }
      
      // Update cell overlay
      const overlay = cell.querySelector('.cell-overlay');
      if (overlay) {
        const numberEl = overlay.querySelector('.cell-channel');
        const nameEl = overlay.querySelector('.cell-name');
        if (numberEl) numberEl.textContent = ch.chno || (channelIndex + 1);
        if (nameEl) nameEl.textContent = ch.name;
      }
      
      // Set active state
      cell.classList.toggle('active', channelIndex === current);
      
      // Add click handler to switch to this channel
      cell.onclick = () => {
        current = channelIndex;
        updateChannelDisplay();
        // Update active state
        multiviewGrid.querySelectorAll('.multiview-cell').forEach(c => c.classList.remove('active'));
        cell.classList.add('active');
      };
      
      videoElement.play().catch(e => {
        console.error(`Failed to play multiview stream ${channelIndex}:`, e);
      });
    }
  }
}

function stopMultiview() {
  multiviewMode = false;
  stopMultiviewStreams();
  multiviewGrid.style.display = 'none';
  video.style.display = '';
  audio.style.display = '';
  
  // Resume single channel playback
  if (!standby && channels.length > 0 && current >= 0 && current < channels.length) {
    playChannel(current);
  }
}

function playChannel(idx) {
  if (!channels[idx]) return;
  current = idx;
  
  // If in multiview mode, just update the display and active cell
  if (multiviewMode) {
    updateChannelDisplay();
    multiviewGrid.querySelectorAll('.multiview-cell').forEach((cell, i) => {
      const channelIndex = (Math.max(0, current - 1) + i) % channels.length;
      cell.classList.toggle('active', channelIndex === current);
    });
    return;
  }
  
  stopPlayback();
  const ch = channels[idx];
  const isAudio = /\.(mp3|aac|m4a|ogg|flac|wav)(\?|$)/i.test(ch.url) || ch.url.includes('icy') || ch.url.includes('audio');
  const isHls = /\.m3u8(\?|$)/i.test(ch.url);
  video.style.display = isAudio ? 'none' : '';
  audio.style.display = isAudio ? '' : 'none';
  if (isAudio) {
    audio.src = ch.url;
    audio.play().catch(e => showStatus('Failed to play audio stream'));
  } else if (isHls && window.Hls) {
    hls = new Hls();
    hls.loadSource(ch.url);
    hls.attachMedia(video);
    hls.on(Hls.Events.ERROR, function (event, data) {
      if (data.fatal) showStatus('Stream error.');
    });
    video.play().catch(e => showStatus('Failed to play video stream'));
  } else {
    video.src = ch.url;
    video.play().catch(e => showStatus('Failed to play video stream'));
  }
  updateChannelDisplay();
  showChannelBanner();
}

function setStandby(on) {
  standby = on;
  standbyScreen.style.display = standby ? '' : 'none';
  if (standby) {
    stopPlayback();
    if (multiviewMode) {
      stopMultiview();
    }
    channelNumber.textContent = '-- --';
    channelName.textContent = 'STANDBY';
  } else {
    if (channels.length > 0 && current >= 0 && current < channels.length) {
      playChannel(current);
    } else {
      updateChannelDisplay();
    }
  }
}

function showChannelBanner() {
  if (!channels[current]) return;
  bannerChannel.textContent = channels[current].chno || (current + 1);
  bannerName.textContent = channels[current].name;
  channelBanner.classList.add('show');
  setTimeout(() => channelBanner.classList.remove('show'), 2500);
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
  const filtered = filter
    ? channels.filter(ch => ch.name.toLowerCase().includes(filter.toLowerCase()) || (ch.chno && ch.chno.includes(filter)))
    : channels;
  channelList.innerHTML = filtered.map((ch, i) =>
    `<div class="channel-item${i === current ? ' active' : ''}" data-idx="${i}" tabindex="0">
      <img class="channel-logo" src="${ch.logo || ''}" onerror="this.style.display='none'" />
      <div class="channel-details">
        <div class="channel-number-small">${ch.chno || (i + 1)}</div>
        <div class="channel-name-small">${ch.name}</div>
      </div>
    </div>`
  ).join('');
  channelList.querySelectorAll('.channel-item').forEach(item => {
    item.onclick = () => {
      playChannel(Number(item.dataset.idx));
      hideGuide();
    };
    item.onkeydown = (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        playChannel(Number(item.dataset.idx));
        hideGuide();
      }
    };
  });
}

// --- Event Listeners ---
powerBtn.onclick = () => setStandby(!standby);
guideBtn.onclick = showGuide;
closeGuide.onclick = hideGuide;
toggleRemote.onclick = () => remoteControl.classList.toggle('visible');
headerRemoteBtn.onclick = () => remoteControl.classList.toggle('visible');
multiviewBtn.onclick = () => {
  if (multiviewMode) {
    stopMultiview();
  } else {
    startMultiview();
  }
};
prevBtn.onclick = () => {
  if (!standby && channels.length > 0) {
    playChannel((current - 1 + channels.length) % channels.length);
  }
};
nextBtn.onclick = () => {
  if (!standby && channels.length > 0) {
    playChannel((current + 1) % channels.length);
  }
};
searchInput.oninput = (e) => renderChannelList(e.target.value);

numpadBtns.forEach(btn => {
  btn.onclick = () => {
    if (standby) {
      showStatus('Power on first.');
      return;
    }
    // Simple channel number entry
    let num = btn.dataset.num;
    let entry = channelNumber.textContent.replace(/[^0-9]/g, '') + num;
    if (entry.length > 3) entry = entry.slice(-3);
    channelNumber.textContent = entry.padStart(3, '0');
    let idx = channels.findIndex(ch => ch.chno === entry);
    if (idx === -1 && parseInt(entry, 10) > 0 && parseInt(entry, 10) <= channels.length) {
      idx = parseInt(entry, 10) - 1;
    }
    if (idx >= 0) {
      setTimeout(() => playChannel(idx), 600);
    } else {
      setTimeout(() => updateChannelDisplay(), 1200);
    }
  };
});

// Keyboard navigation
window.addEventListener('keydown', (e) => {
  if (e.key === 'F1') remoteControl.classList.toggle('visible');
  if (e.key === 'F2') showGuide();
  if (e.key === 'F3') {
    if (multiviewMode) {
      stopMultiview();
    } else {
      startMultiview();
    }
  }
  if (e.key === 'F4') setStandby(!standby);
  if (e.key === 'ArrowUp') showGuide();
  if (e.key === 'ArrowLeft') prevBtn.onclick();
  if (e.key === 'ArrowRight') nextBtn.onclick();
  if (e.key === 'Escape') {
    remoteControl.classList.remove('visible');
    hideGuide();
    helpOverlay.style.display = 'none';
  }
  if (e.key === '?') helpOverlay.style.display = 'flex';
});

closeHelp.onclick = () => helpOverlay.style.display = 'none';

// --- Initialization ---
(function init() {
  channels = parseM3U(EMBEDDED_M3U);
  if (channels.length > 0) current = 0;
  setStandby(true);
  updateTime();
  updateChannelDisplay();
  remoteControl.classList.add('visible');
  // Help overlay content
  helpBody.innerHTML = `
    <div><kbd>F1</kbd>: Toggle Remote</div>
    <div><kbd>F2</kbd>: Show Guide</div>
    <div><kbd>F3</kbd>: Toggle Multiview</div>
    <div><kbd>F4</kbd>: Power On/Off</div>
    <div><kbd>←/→</kbd>: Prev/Next Channel</div>
    <div><kbd>↑</kbd>: Show Guide</div>
    <div><kbd>?</kbd>: Show Help</div>
    <div><kbd>ESC</kbd>: Hide Remote/Guide/Help</div>
    <div><strong>Mobile:</strong> Use header remote button to show/hide remote</div>
  `;
})(); 
