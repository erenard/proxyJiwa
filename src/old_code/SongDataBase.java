package old_code;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;


public class SongDataBase {

	private static ConcurrentHashMap<String, String> nameById = new ConcurrentHashMap<String, String>();
	
	public static void parseSongs(String zippedJson) throws IOException {
		
		InputStream reader = new ByteArrayInputStream(zippedJson.getBytes());
		OutputStream writer = new ByteArrayOutputStream();
		
		boolean passHeader = false;

		int c;
		while ((c = reader.read()) != -1) {
			if(!passHeader && c == ' ') {
				c = reader.read();
				if(c == 'g') {
					c = reader.read();
					if(c == 'z') {
						c = reader.read();
						if(c == 'i') {
							c = reader.read();
							if(c == 'p') {
								//Retour chariot
								reader.read();
								reader.read();
								//Saut de ligne
								reader.read();
								reader.read();
								//Checksum
								reader.read();
								reader.read();
								reader.read();
								//Retour chariot
								reader.read();
								reader.read();
								passHeader = true;
							}
						}
					}
				}
			}
			while(passHeader && (c = reader.read())!= -1) {
				writer.write(c);
			}
		}

		GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(writer.toString().getBytes()));
		StringBuilder json = new StringBuilder();
		
		try {
			while((c = gzipInputStream.read()) != -1) {
				json.append((char) c);
			}
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			gzipInputStream.close();
		}

		String songId = "";
		String artist = "";
		String album = "";
		String title = "";
		String separator = " ### ";
		
		StringTokenizer tracks = new StringTokenizer(json.toString(), "},{");
		while(tracks.hasMoreTokens()) {
			String track = tracks.nextToken();
			System.out.println(track);
			int songIndex = track.indexOf("\"songId\":");
			if(songIndex != -1) {
				track = track.substring(songIndex + "\"songId\":".length());
				songId = track.substring(0, track.indexOf(','));
				
				int songNameIndex = track.indexOf("\"songName\":\"");
				if(songNameIndex != -1) {
					track = track.substring(songNameIndex + "\"songName\":\"".length());
					title = track.substring(0, track.indexOf('"'));
				}

				int artistNameIndex = track.indexOf("\"artistName\":\"");
				if(artistNameIndex != -1) {
					track = track.substring(artistNameIndex + "\"artistName\":\"".length());
					artist = track.substring(0, track.indexOf('"'));
				}

				int albumNameIndex = track.indexOf("\"albumName\":\"");
				if(albumNameIndex != -1) {
					track = track.substring(songIndex + "\"albumName\":\"".length());
					album = track.substring(0, track.indexOf(','));
				}
				
				synchronized (nameById) {
					System.out.println(artist + separator + album + separator + title);
					nameById.put(songId, artist + separator + album + separator + title);
				}
			}
		}
	}
	
	public static String getFileName(String songId) {
		synchronized (nameById) {
			return nameById.get(songId);
		}
	}
}

/*

{"page":[{"trackId":676270,"songId":198988,"songName":"parle a ma main feat yelle g christelle","artistId"

:17853,"artistName":"Fatal Bazooka","secArtistsNames":null,"albumId":137807,"albumName":"T'As Vu","songPopularity"

:389902,"itunesTrackUrl":null,"albumReleaseDate":"2007-01-01","duration":"252","playlistTrackId":0,"trackPosition"

:0},{"trackId":1983408,"songId":304475,"songName":"Trankillement","artistId":17853,"artistName":"Fatal

 Bazooka","secArtistsNames":null,"albumId":231664,"albumName":"T'as Vu","songPopularity":248320,"itunesTrackUrl"

:null,"albumReleaseDate":"2009-04-20","duration":"207","playlistTrackId":0,"trackPosition":0},{"trackId"

:1983394,"songId":183203,"songName":"J'Aime Trop Ton Boule","artistId":17853,"artistName":"Fatal Bazooka"

,"secArtistsNames":null,"albumId":231664,"albumName":"T'as Vu","songPopularity":295620,"itunesTrackUrl"

:null,"albumReleaseDate":"2009-04-20","duration":"221","playlistTrackId":0,"trackPosition":0},{"trackId"

:1983403,"songId":197499,"songName":"Chienne De Vie\nfeat. Tristesse Au Soleil","artistId":17853,"artistName"

:"Fatal Bazooka","secArtistsNames":null,"albumId":231664,"albumName":"T'as Vu","songPopularity":263860

,"itunesTrackUrl":null,"albumReleaseDate":"2009-04-20","duration":"225","playlistTrackId":0,"trackPosition"

:0},{"trackId":323930,"songId":265938,"songName":"mauvaise foi nocturne feat vitoo","artistId":17853

,"artistName":"Fatal Bazooka","secArtistsNames":null,"albumId":98533,"albumName":"Mauvaise foi nocturne

 : la r\u00e9ponse","songPopularity":272240,"itunesTrackUrl":"http:\/\/itunes.apple.com\/WebObjects\

/MZStore.woa\/wa\/viewAlbum?i=214240863&id=214240862&s=143442&partnerId=2003","albumReleaseDate":"2007-03-01"

,"duration":"370","playlistTrackId":0,"trackPosition":0},{"trackId":242946,"songId":198971,"songName"

:"Fous Ta Cagoule","artistId":17853,"artistName":"Fatal Bazooka","secArtistsNames":null,"albumId":137807

,"albumName":"T'as vu","songPopularity":101270,"itunesTrackUrl":"http:\/\/itunes.apple.com\/WebObjects

\/MZStore.woa\/wa\/viewAlbum?i=202313662&id=202313650&s=143442&partnerId=2003","albumReleaseDate":"2007-05-28"

,"duration":"212","playlistTrackId":0,"trackPosition":0},{"trackId":1983405,"songId":182143,"songName"

:"Auto-Clash","artistId":17853,"artistName":"Fatal Bazooka","secArtistsNames":null,"albumId":231664,"albumName"

:"T'as Vu","songPopularity":159200,"itunesTrackUrl":null,"albumReleaseDate":"2009-04-20","duration":"293"

,"playlistTrackId":0,"trackPosition":0},{"trackId":1983409,"songId":198198,"songName":"Crepes Au Froment"

,"artistId":17853,"artistName":"Fatal Bazooka","secArtistsNames":null,"albumId":231664,"albumName":"T'as

 Vu","songPopularity":99300,"itunesTrackUrl":null,"albumReleaseDate":"2009-04-20","duration":"356","playlistTrackId"

:0,"trackPosition":0},{"trackId":1983398,"songId":182353,"songName":"Ouais ma gueule","artistId":17853

,"artistName":"Fatal Bazooka","secArtistsNames":null,"albumId":231664,"albumName":"T'as Vu","songPopularity"

:96071,"itunesTrackUrl":null,"albumReleaseDate":"2009-04-20","duration":"222","playlistTrackId":0,"trackPosition"

:0},{"trackId":1983399,"songId":182220,"songName":"Ego Trip","artistId":17853,"artistName":"Fatal Bazooka"

,"secArtistsNames":null,"albumId":231664,"albumName":"T'as Vu","songPopularity":85315,"itunesTrackUrl"

:null,"albumReleaseDate":"2009-04-20","duration":"257","playlistTrackId":0,"trackPosition":0}],"total"

:10,"pageSize":25,"success":true}

*/