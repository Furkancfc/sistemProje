
import java.io.*;
import java.net.*;

public class ServerDB extends IServer {
	public static final int PORT = 5004;

	public static void main(String[] args) {
		try {
			serverSock = new ServerSocket(PORT);
			Thread listenThread = new Thread(new Listener());
			listenThread.start();

			Thread dbTemp = new Thread(new DBTemp(PORT));
			while (true) {
				if (database == null) {
					if ((database = DBTemp.syncDB(PORT)) == null) {
						database = new Database(PORT);
					}
					System.out.println("Current table : " + database.subscriberTable.toString());
					dbTemp.start();
				}
				if (incomingConnection != null) {
					Thread handleThread = new Thread(new Handler(incomingConnection));
					handleThread.start();
					incomingConnection = null;
				}
			}
		}
		catch (Exception e) {
			System.err.println(e);
		}
	}

	static class Handler extends IServer.Handler {

		public Handler(Socket client) throws Exception {
			super(client);
		}

		@Override
		public void asupClients(String[] message) throws Exception {
		}

	}
}
