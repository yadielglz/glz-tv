-- Update broken/dead radio station stream URLs in public.radio_stations
UPDATE public.radio_stations
SET stream_url = 'https://streams.proftsc.com/listen/woro/woro.mp3',
    updated_at = timezone('utc'::text, now())
WHERE station_code = 'RADIO_WORO';

UPDATE public.radio_stations
SET stream_url = 'https://a4.asurahosting.com:6760/radio.mp3',
    updated_at = timezone('utc'::text, now())
WHERE station_code = 'RADIO_WOYE';

UPDATE public.radio_stations
SET stream_url = 'https://stream.eleden.com:8230/lax.aac',
    updated_at = timezone('utc'::text, now())
WHERE station_code = 'RADIO_WXYX';

UPDATE public.radio_stations
SET stream_url = 'https://televicentro.streamguys1.com/wkaqfm-icy?key=b28722e05bbaeb47a258c5049395de1773281b36229b1f159d29cb29e12a5433',
    updated_at = timezone('utc'::text, now())
WHERE station_code = 'RADIO_WKAQFM';

UPDATE public.radio_stations
SET stream_url = 'https://televicentro.streamguys1.com/wkaqam-icy?key=b28722e05bbaeb47a258c5049395de1773281b36229b1f159d29cb29e12a5433',
    updated_at = timezone('utc'::text, now())
WHERE station_code = 'RADIO_WKAQAM';

UPDATE public.radio_stations
SET stream_url = 'https://server10.servistreaming.com/proxy/radioonce?mp=/stream',
    updated_at = timezone('utc'::text, now())
WHERE station_code = 'RADIO_WMSW';

UPDATE public.radio_stations
SET stream_url = 'https://server7.servistreaming.com/proxy/radioisla?mp=%2Fstream%3Ftype%3D.mp3&_=1',
    updated_at = timezone('utc'::text, now())
WHERE station_code = 'RADIO_WSKN';

UPDATE public.radio_stations
SET stream_url = 'https://lmmradiocast.com/latino99fm',
    updated_at = timezone('utc'::text, now())
WHERE station_code = 'RADIO_LAT99';
