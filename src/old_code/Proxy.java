package old_code;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class OneServeur extends Thread {
	private SocketChannel browser;
	private SocketChannel webSite;
	private ConcurrentMap<String, SocketAddress> addressByHostName = new ConcurrentHashMap<String, SocketAddress>();
	private SocketAddress address;
	
	private String songId = null;
	private boolean isInfo = false;

	public OneServeur(SocketChannel s) throws IOException {
		super("Proxy");
		browser = s;
		start();
	}

	private String extractTargetHost(String httpHeader) {
		songId = null;
		isInfo = false;
		long threadId = Thread.currentThread().getId();
		StringTokenizer st = new StringTokenizer(httpHeader, "\r\n");
		boolean isFirst = true;
		while (st.hasMoreTokens()) {
			String str = st.nextToken();
			if(isFirst) {
				if(str.contains("play.php")) {
					System.err.println(threadId + ": " + str);
					String tmp = str.substring(str.indexOf("s=") + 2);
					songId = tmp.substring(0, tmp.indexOf("&"));
				}
				if(str.contains("playlist/tracks")) {
					System.err.println(threadId + ": " + str);
					isInfo = true;
				}
				isFirst = false;
			}
			if(str.startsWith("Host:")) {
				return str.substring(6);
			}
		}
		return "";
	}
	
	private String readHeader() throws IOException {
		int totalReaded;
		StringBuffer sb = new StringBuffer();
		do {
			totalReaded = 0;
			//Data Wait
			try {Thread.sleep(50);} catch (InterruptedException e) {}
			//Read Burst
			int readed;
			do {
				ByteBuffer buffer = ByteBuffer.allocate(4096);
				readed = browser.read(buffer);
				totalReaded += readed;
				if(readed > 0) {
					buffer.rewind();
					while(buffer.position() < readed) {
						sb.append((char) buffer.get());
					}
				}
			} while(readed > 0);
		} while(totalReaded > 0);
		return sb.toString();
	}
	
	private void forwardResponse() throws IOException {
		StringBuffer infoBuffer = new StringBuffer();
		FileChannel fileWriter = null;
		File tempFile = null;
		if(songId != null) {
			tempFile = new File(songId);
			if(new File(songId + ".mp3").exists()) {
				songId = null;
			} else {
				fileWriter = new FileOutputStream(tempFile).getChannel();
			}
		}
		long threadId = Thread.currentThread().getId();
		int totalReaded;
		long time = System.currentTimeMillis();
		boolean hasRead = false;
		do {
			totalReaded = 0;
			//Data Wait
			try {Thread.sleep(500);} catch (InterruptedException e) {}
			//Read Burst
			int readed;
			do {
				ByteBuffer buffer = ByteBuffer.allocate(512 * 1024);
				readed = webSite.read(buffer);
				totalReaded += readed;
				if(readed > 0) {
					hasRead = true;
					buffer.rewind();
					browser.write(ByteBuffer.wrap(buffer.array(), 0, readed));
					if(fileWriter != null) {
						buffer.rewind();
						fileWriter.write(ByteBuffer.wrap(buffer.array(), 0, readed));
					}
					if(isInfo) {
						buffer.rewind();
						infoBuffer.append(new String(buffer.array()));
					}
				}
			} while(readed > 0); //Read until there is something to read
		} while(totalReaded > 0 || (!hasRead && (System.currentTimeMillis() - time) < 30000));
		if(fileWriter != null) fileWriter.close();
		if(!hasRead) {
			System.err.println(threadId + ": TIMEOUT !!!");
		}
		if(songId != null) {
			String fileName = SongDataBase.getFileName(songId);
			if(tempFile != null && fileName != null) {
				if(!tempFile.renameTo(new File(fileName))) {
					System.out.println("N'a pas pu renommer le fichier : " + tempFile.getAbsolutePath());
				}
			} else {
				System.out.println("Nom de fichier non trouvé pour : " + songId);
			}
			copyToMp3File(tempFile);
		}
		if(isInfo) {
			SongDataBase.parseSongs(infoBuffer.toString());
		}
		if(tempFile != null) {
			if(!tempFile.delete()) System.out.println("N'a pas pu supprimer : " + tempFile.getAbsolutePath());
		}
	}

	private void copyToMp3File(File file) throws IOException {
		BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file));
		BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(file.getName() + ".mp3"));
		boolean passHeader = false;
		
		int c;
		while (reader.available() > 0) {
			c = reader.read();
			if(!passHeader && c == 't') {
				c = reader.read();
				if(c == 'e') {
					c = reader.read();
					if(c == 'x') {
						c = reader.read();
						if(c == 't') {
							c = reader.read();
							if(c == '/') {
								c = reader.read();
								if(c == 'h') {
									c = reader.read();
									if(c == 't') {
										c = reader.read();
										if(c == 'm') {
											c = reader.read();
											if(c == 'l') {
												c = reader.read();
												c = reader.read();
												c = reader.read();
												passHeader = true;
												continue;
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if(passHeader) {
				do {
					c = reader.read();
					writer.write(c);
				} while(c != -1);
			}
		}
		reader.close();
		writer.flush();
		writer.close();
	}

	public void run() {
		try {
			long time = System.currentTimeMillis();
			do {
				String httpHeader = readHeader();
				if(httpHeader.length() > 0) {
					String targetHost = extractTargetHost(httpHeader);
					if(isInfo) {
//						httpHeader = patchHeader(httpHeader);
					}
					if(targetHost.length() > 0) {
						time = System.currentTimeMillis();
						SocketAddress address = addressByHostName.get(targetHost);
						if(address == null) {
							address = new InetSocketAddress(InetAddress.getByName(targetHost), 80);
							addressByHostName.put(targetHost, address);
						}
						if(!address.equals(this.address)) {
							webSite = SocketChannel.open(address);
							webSite.configureBlocking(false);
							this.address = address;
						}
						webSite.write(ByteBuffer.wrap(httpHeader.getBytes()));
						forwardResponse();
					}
				}
			} while(System.currentTimeMillis() - time < 1000);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				webSite.close();
			} catch (Exception e) {}
			try {
				browser.close();
			} catch (Exception e) {}
		}
	}

	private String patchHeader(String httpHeader) throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(httpHeader));
		StringWriter writer = new StringWriter();
		String line = "#";
		while(!line.equals("")) {
			line = reader.readLine();
			if(!line.startsWith("Accept-Encoding: gzip,deflate")) {
				writer.write(line + "\r\n");
			} else {
//				writer.write("Accept-Encoding: identity\r\n");
			}
		}
		return writer.toString();
	}
}

public class Proxy {
	public static final int PORT = 1080;
	public static void main(String args[]) throws IOException {
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		SocketAddress address = new InetSocketAddress(PORT);
		serverSocketChannel.socket().bind(address);
		try {
			while (true) {
				SocketChannel socket = serverSocketChannel.accept();
				if(socket != null) {
					socket.configureBlocking(false);
					try {
						new OneServeur(socket);
					} catch (IOException e) {
						socket.close();
					}
				}
			}
		} finally {
			serverSocketChannel.close();
		}
	}
}
