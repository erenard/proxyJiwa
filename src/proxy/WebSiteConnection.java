package proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.StringTokenizer;

/**
 * Tunnel déscendant.
 * Copie le flux venant du serveur vers le browser.
 */
public class WebSiteConnection extends Thread {
	private final SocketChannel webSite;
	private final String hostName;
	private final SocketChannel browser;
	private File dumpFile;
	private FileChannel file;
	private ByteBuffer buffer = ByteBuffer.allocate(1024); //512Ko
	
	private final int TIME_OUT = 60000; //60s
	private long time = System.currentTimeMillis();

	public WebSiteConnection(SocketChannel browser, String hostName) throws IOException {
		this.hostName = hostName;
		this.browser = browser;
		/*
		 * Connection au site distant et transfert de l'entete HTTP
		 */
		SocketAddress address = new InetSocketAddress(InetAddress.getByName(hostName), 80);
		webSite = SocketChannel.open(address);
		webSite.configureBlocking(false);
		start();
	}

	public void close() {
		closeFile();
		try {
			webSite.close();
			setName("Disconnected " + getName());
		} catch (IOException e) {}
	}
	
	private void closeFile() {
		if(dumpFile != null) {
			new Mp3Converter(dumpFile);
			dumpFile = null;
		}
		try {
			if(file != null && file.isOpen()) file.close();
		} catch (IOException e) {}
	}
	
	@Override
	public void run() {
		try {
			while(webSite.isConnected() && browser.isConnected() && System.currentTimeMillis() - time < TIME_OUT) {
				//Forwarding...
				buffer.clear();
				int forwarded = webSite.read(buffer);
				if(forwarded > 0) {
					buffer.rewind();
					browser.write(ByteBuffer.wrap(buffer.array(), 0, forwarded));
					time = System.currentTimeMillis();
					if(file != null && file.isOpen()) {
						buffer.rewind();
						file.write(ByteBuffer.wrap(buffer.array(), 0, forwarded));
					}
				}
				//Data Wait
				Thread.sleep(1); //Every 50ms
			}
		} catch(IOException e) {
			System.err.println(this.getId() + " " + hostName);
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.err.println(this.getId() + " " + hostName);
			e.printStackTrace();
		} finally {
			close();
		}
	}

	/**
	 * @return the hostName
	 */
	public String getHostName() {
		return hostName;
	}

	public void writeString(String httpHeader) {
		try {
			if(file != null && file.isOpen()) {
				closeFile();
			}
			webSite.write(ByteBuffer.wrap(httpHeader.getBytes()));
			time = System.currentTimeMillis();
			String songId = null;
			if(httpHeader.contains("GET ")) {
				String url = null;
				{
					StringTokenizer st = new StringTokenizer(httpHeader, "\r\n");
					while (url == null && st.hasMoreTokens()) {
						String str = st.nextToken();
						if(str.startsWith("GET ")) {
							url = str.substring(4);
						}
					}
				}
				System.out.println(url);
				this.setName(url);
				if(url.contains("play.php")) {
					String tmp = url.substring(url.indexOf("s=") + 2);
					songId = tmp.substring(0, tmp.indexOf("&"));
				}
			}
			if(songId != null) {
				dumpFile = new File(songId + ".tmp");
				if(dumpFile.exists()) dumpFile.delete();
				file = new FileOutputStream(dumpFile).getChannel();
			}
		} catch (IOException e) {
			System.err.println(this.getId() + " " + hostName);
			e.printStackTrace();
			close();
		}
	}

	public boolean isConnected() {
		synchronized (webSite) {
			return webSite.isConnected();
		}
	}
}
