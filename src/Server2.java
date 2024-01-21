import java.net.*;
import java.time.Instant;
import java.util.*;

public class Server2 extends IServer {

	// server port 5052
	static public final int PORT = 5002;

	// login server
	// public static int serversPort = 50521;
	// public static int clientsPort = 50522;
	public static void main(String args[]) {
		try {
			serverSock = new ServerSocket(PORT);
			connectionQueue = new LinkedList<Socket>();

			Thread clientListener = new Thread(new Listener());
			clientListener.start();

			Thread dbTemp = new Thread(new DBTemp(PORT));

			while (true) {
				if (database == null) {
					if ((database = syncDB(PORT)) == null) {
						database = new Database(PORT);
					}
					System.out.println("Current table : " + database.subscriberTable.toString());
					System.err.println("database timestamp : " + Date.from(Instant.ofEpochMilli(database.lastUpdate)));

					dbTemp.start();
				}
				if (incomingConnection != null) {
					Thread clientThread = new Thread(new Handler(incomingConnection));
					clientThread.start();
					incomingConnection = null;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class Handler extends IServer.Handler {

		public Handler(Socket client) throws Exception {
			super(client);
		}

		public void asupClients(String[] message) throws Exception {
			Database.Record record = null;
			if (message[0].contentEquals("GIRIS")) {
				if (message.length > 2) {
					if (clientSession == null || clientSession.email == null) { // ? baglanan kullanicinin oturumu yok ise
						if ((record = database.subscriberTable.get(message[1])) != null) { // ? Database'de kullanici var ise
							if (record.session != null && record.session.email != null) { // ? Database'deki kullanicinin
								// oturumu var ise
								if (clientSession != null && record.session.email.contentEquals(clientSession.email)) { // ? kullanicinin oturumu, giris yapmayi denedigi hesap ile ayni mi
									if ((record = database.login(message[1], message[2])) != null) {
										clientSession = record.session;
										oos.writeObject((Object) new ProtocolMessage("55 TAMM"));
										oos.flush();
										oos.writeObject((Object) new ProtocolMessage("Logged in succesfully"));
										oos.flush();
										oos.writeObject((Object) clientSession);
										oos.flush();
										new Thread(new Dispatcher((Object) record, PORT, new ProtocolMessage("SERILESTIRILMIS_NESNE post " + record.getMail()))).start();
									}
									else {
										oos.writeObject(new ProtocolMessage("Wrong password"));
									}
								}
								else { // ? kullanici mevcut oturumdan baska hesaba girmeye calisiyorsa
									if ((record = database.login(message[1], message[2])) != null) { // ? basarili giris deniyorsa
										clientSession = record.session;
										oos.writeObject((Object) new ProtocolMessage("Logged in succesfully"));
										oos.flush();
										oos.writeObject((Object) clientSession);
										oos.flush();
										new Thread(new Dispatcher((Object) record, PORT, new ProtocolMessage("SERILESTIRILMIS_NESNE post " + record.getMail()))).start();
									}
									else { // ? giris basarisiz ise
										oos.writeObject((Object) new ProtocolMessage("Wrong password"));
									}
								}
							}
							else { // ? Database'deki kullancinin oturumu yok ise
								if ((record = database.login(message[1], message[2])) != null) {
									clientSession = record.session;
									oos.writeObject(new ProtocolMessage("Logged in succesfully"));
									oos.flush();
									// ? kullanicinin oturumu yenilenir
									oos.writeObject((Object) clientSession);
									oos.flush();
									new Thread(new Dispatcher((Object) record, PORT, new ProtocolMessage("SERILESTIRILMIS_NESNE post " + record.getMail()))).start();

								}
								else {
									oos.writeObject(new ProtocolMessage("Wrong Password"));
									oos.flush();
								}
							}
						}
						else { // ? database'den kullanici bulunamadi ise
							oos.writeObject(new ProtocolMessage("User not found"));
							oos.flush();
						}
					}
					else { // ? baglanan kullanicinin oturumu var ise
						oos.writeObject(new ProtocolMessage("Already logged in"));
						oos.flush();
					}
				}
				else {
					oos.writeObject(new ProtocolMessage("Not enough arguments"));
					oos.flush();
				}
			}
			else if (message[0].contentEquals("CIKIS")) {
				if (clientSession != null && clientSession.email != null) { // ? kullanici giris yapmis ve oturumu var ise
					if ((record = database.logout(clientSession.email)) != null) {
						clientSession = record.session;
						oos.writeObject(new ProtocolMessage("Succesfully Log Out"));
						oos.flush();
						oos.writeObject((Object) clientSession);
						oos.flush();
						new Thread(new Dispatcher((Object) record, PORT, new ProtocolMessage("SERILESTIRILMIS_NESNE post " + record.getMail()))).start();

					}
					else {
						oos.writeObject(new ProtocolMessage("This user is no longer available"));
						oos.flush();
						oos.writeObject(new Session(null));
						oos.flush();
					}
				}
				else {
					oos.writeObject((Object) new ProtocolMessage("Your account is no longer available"));
					oos.flush();
					oos.writeObject((Object) new Session(null));
					oos.flush();
				}
			}
			else if (clientSession != null && clientSession.email == null) { // ? kullanici giris yapmamis ise
				oos.writeObject(new ProtocolMessage("Login First"));
			}
		}
	}
}
