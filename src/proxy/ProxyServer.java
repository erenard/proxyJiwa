package proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Cette classe accepte les connections du browser, instancie un thread de gestion des requetes pour chaque connection
 */
public class ProxyServer {
	public static final int PORT = 1070;
	public static void main(String args[]) throws IOException {
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		SocketAddress address = new InetSocketAddress(PORT);
		serverSocketChannel.socket().bind(address);
		try {
			while (true) {
				SocketChannel socket = serverSocketChannel.accept();
				if(socket != null) {
					socket.configureBlocking(false);
					new BrowserConnection(socket);
				}
			}
		} finally {
			serverSocketChannel.close();
		}
	}
}
