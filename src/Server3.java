import java.net.*;
import java.time.Instant;
import java.util.*;

public class Server3 extends IServer {
	public static int PORT = 5003;

	public static void main(String[] args) {
		try {
			serverSock = new ServerSocket(PORT);
			connectionQueue = new LinkedList<Socket>();
			Thread listenThread = new Thread(new Listener());
			listenThread.start();

			Thread dbTemp = new Thread(new DBTemp(PORT));
			while (true) {
				if (database == null) {
					if ((database = DBTemp.syncDB(PORT)) == null) {
						database = new Database(PORT);
					}
					System.out.println("Current table : " + database.subscriberTable.toString());
					System.err.println("database timestamp : " + Date.from(Instant.ofEpochMilli(database.lastUpdate)));

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
			if (message[0].contentEquals("ABONIPTAL")) {
				if (clientSession != null && clientSession.email != null) { // ? karsi tarafta oturum acilmis ise
					Thread t1 = new Thread(new Dispatcher(null, PORT, new ProtocolMessage("SERILESTIRILMIS_NESNE delete " + clientSession.email)));
					t1.start();
					database.delete(clientSession.email);
					oos.writeObject(database.logout(clientSession.email));
				}
				else if (clientSession == null || (clientSession != null && clientSession.email == null)) { // ? karsi tarafta oturum acilmamis ise
					oos.writeObject(new ProtocolMessage("Login First"));;
				}
			}
		}
	}
}
