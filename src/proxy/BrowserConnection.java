package proxy;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Cette classe écoute les demandes du browser.
 * Pour chaque demande :
 * 
 * 1. Résolution du DNS
 * 2. Arret du Tunnel correspondant à la requète précédente
 * 3. Instanciation et démarrage d'un Tunnel déscendant
 * 4. Envoi de la demande du browser
 */
public class BrowserConnection extends Thread {
	
	private final SocketChannel browser;
	private final Map<String, WebSiteConnection> webSiteConnectionsByHostName = new HashMap<String, WebSiteConnection>();
	private final ByteBuffer buffer = ByteBuffer.allocate(512 * 1024);//512ko
	
	public BrowserConnection(SocketChannel s) {
		super("BrowserConnection");
		browser = s;
		start();
	}

	@Override
	public void run() {
		try {
			while(browser.isConnected()) {
				/*
				 * Lecture du header HTTP
				 */
				String httpHeader = null;
				{
					StringBuffer sb = new StringBuffer();
					//Read Burst
					int readed;
					do {
						buffer.clear();
						readed = browser.read(buffer);
						if(readed > 0) {
							buffer.rewind();
							while(buffer.position() < readed) {
								sb.append((char) buffer.get());
							}
							//Data Wait
							Thread.sleep(50);
						}
					} while(readed > 0);
					httpHeader = sb.toString();
				}
				if(httpHeader != null && httpHeader.length() > 0) {
					if(httpHeader.contains("Host:")) {
						/*
						 * Extraction des infos du header
						 * url, songId, hostName
						 */
						String hostName = null;
						{
							StringTokenizer st = new StringTokenizer(httpHeader, "\r\n");
							while (hostName == null && st.hasMoreTokens()) {
								String str = st.nextToken();
								if(str.startsWith("Host: ")) {
									hostName = str.substring(6);
								}
							}
						}
						if(hostName != null) {
							WebSiteConnection webSiteConnection = webSiteConnectionsByHostName.get(hostName);
							if(webSiteConnection != null) {
								if(webSiteConnection.isConnected()) {
									System.err.println(this.getId() + " reuse connection " + hostName);
								} else {
									webSiteConnection.close();
									System.err.println(this.getId() + " close connection " + hostName);
								}
							}
							if(webSiteConnection == null){
								System.err.println(this.getId() + " create connection " + hostName);
								webSiteConnection = new WebSiteConnection(browser, hostName);
								webSiteConnectionsByHostName.put(hostName, webSiteConnection);
							}
							webSiteConnection.writeString(httpHeader);
						}
					}
				}
				//Suppression des connections fermés
				for (Iterator<String> iterator = webSiteConnectionsByHostName.keySet().iterator(); iterator.hasNext();) {
					WebSiteConnection webSiteConnection = webSiteConnectionsByHostName.get(iterator.next());
					if(!webSiteConnection.isConnected()) {
						System.err.println(this.getId() + " clean connection " + webSiteConnection.getHostName());
						iterator.remove();
						webSiteConnection.close();
					}
				}
				//Déconnection éventuelle si plus aucunes connections ouvertes
				if(webSiteConnectionsByHostName.isEmpty()) browser.close();
				Thread.sleep(50);
			}
		} catch(UnknownHostException e) {
			System.err.println(e.getMessage());
		} catch(IOException e) {
			e.printStackTrace();
		} catch(InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {browser.close();} catch(IOException e) {}
		}
	}
}
