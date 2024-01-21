
import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.*;

public class ServerDB extends IServer {
	public static final int PORT = 5004;

	// private static class Synchroniser implements Runnable{
	// 	public Synchroniser(){

	// 	}
	// 	public void run(){
	// 		while(true){
				
	// 		}
			
	// 		return;
	// 	}
	// }
	public static void main(String[] args) {
		try {
			serverSock = new ServerSocket(PORT);
			connectionQueue = new LinkedList<Socket>();
			Thread listenThread = new Thread(new Listener());
			listenThread.start();
			// Thread synchronizer = new Thread(new Synchroniser());
			// synchronizer.start();

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
		public void asupClients(String[] message) throws Exception {}

	}
}
