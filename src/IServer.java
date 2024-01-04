import java.io.*;
import java.net.*;
import java.sql.*;
import java.time.*;
import java.util.*;

import org.sqlite.jdbc4.*;
import org.sqlite.JDBC;

import com.mysql.cj.jdbc.*;

public abstract class IServer {
	static volatile ServerSocket serverSock = null;
	static volatile Socket incomingConnection = null;
	static volatile Database database = null;

	static int[] ports = { 5001, 5002, 5003, 5004 };

	static int PORT;

	public static class Dispatcher implements Runnable {
		Object obj = null;
		int port;
		ProtocolMessage message = null;

		public Dispatcher(Object obj, int port, ProtocolMessage message) {
			this.obj = obj;
			this.port = port;
			this.message = message;
		}

		public void run() {
			try {
				for (int i : ports) {
					if (i != port) {
						sendObject(obj, i, message);
					}
				}
			} catch (Exception e) {
				System.err.println("Dispatcher exception");
			}
		}
	}

	public static void sendObject(Object object, int port, ProtocolMessage message) throws Exception {
		Socket socket = null;
		try {
			socket = new Socket();
			if (socket.isConnected()) {
				socket.close();
				socket = new Socket();
			}
			if (socket == null || socket.isClosed())
				socket = new Socket();
			if (!socket.isBound())
				socket = new Socket();
			try {
				socket.connect(new InetSocketAddress(port));
			} catch (ConnectException e) {
				System.out.println(e);
				return;
			}
			OutputStream oss = socket.getOutputStream();
			InputStream iss = socket.getInputStream();
			ObjectOutputStream ooss = new ObjectOutputStream(oss);
			ObjectInputStream oiss = new ObjectInputStream(iss);

			ooss.writeObject((Object) message);
			ooss.flush();
			ooss.writeObject((Object) object);
			ooss.flush();
		} catch (Exception e) {
			System.err.println("Object Send Crash");
			System.err.println(e);
			e.printStackTrace();
			throw e;
		} finally {
			try {
				socket.close();
				socket = null;
			} catch (Exception f) {
				f.printStackTrace();
			}
		}
	}

	public static class DBTemp implements Runnable {
		static FileInputStream fis = null;
		static FileOutputStream fos = null;
		static ObjectInputStream fois = null;
		static ObjectOutputStream foos = null;
		static File file = null;
		static int port = 0;
		static volatile Connection connection = null;

		public DBTemp(int PORT) throws Exception {
			this.port = PORT;
			file = new File(String.format("db%dtemp", port));
			fos = new FileOutputStream(file, false);
			foos = new ObjectOutputStream(fos);
			fis = new FileInputStream(file);
			fois = new ObjectInputStream(fis);
		}

		public void run() {
			try {
				if (DriverManager.drivers().toArray().length < 1)
					System.out.println("No Driver");
				else {
					Enumeration<java.sql.Driver> enumerator = DriverManager.getDrivers();
					Iterator<java.sql.Driver> i = enumerator.asIterator();
					while (i.hasNext()) {
						System.out.println(i.next());
					}
				}
				Class.forName("org.sqlite.JDBC");
				connection = DriverManager.getConnection(
						String.format("jdbc:sqlite:/home/furkan/Databases/SQLite_databases/db%s.db", port));

				while (true) {
					if (database != null) {
						if (database.change != null) {
							if (database.lastUpdate == database.createTime) {
								createTable();
							} else {
								updateTable();
							}
						}

					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

		public static void createTable() throws Exception {
			Statement stmt = connection.createStatement();
			stmt.execute(
					"CREATE TABLE if not exists RecordTable ( submail varchar(20),creatTime varchar(20), lastUpdate varchar(20), isOnline boolean , foreign key (submail) references SubTable(submail) );");
			stmt.execute(
					"CREATE TABLE if not exists SessionTable ( submail varchar(20),creatTime varchar(20), lastLogin varchar(20), isActive boolean, foreign key (submail) references SubTable(submail) );");
			stmt.execute(
					"CREATE TABLE if not exists SubTable ( submail varchar(20),subname varchar(20),subsurname varchar(20),subpaswd varchar(20),primary key (submail) );");
		}

		public static void updateTable() throws Exception {
			Change ch = database.change;
			Database.Record record = (Database.Record) ch.chcontent;
			Subscriber sub = (Subscriber) ch.chcontent;
			Session session = sub.session;

			try (PreparedStatement prs = connection.prepareStatement("select * from SubTable where submail=(?)")) {
				prs.setString(1, record.getMail());
				ResultSet rs = prs.executeQuery();
				if (!rs.next()) {
					try (PreparedStatement prs1 = connection
							.prepareStatement("insert into SubTable values ((?),(?),(?),(?))")) {
						prs1.setString(1, sub.getMail());
						prs1.setString(2, sub.getName());
						prs1.setString(3, sub.getSurname());
						prs1.setString(4, sub.getPassword());

					}
					try (PreparedStatement prs1 = connection
							.prepareStatement("insert into SessionTable values ((?),(?),(?),(?))")) {
						prs1.setString(1, session.email);
						prs1.setString(2, String.valueOf(Instant.ofEpochMilli(session.createTime)));
						prs1.setString(3, String.valueOf(Instant.ofEpochMilli(session.lastlogin)));
						prs1.setString(4, String.valueOf(session.isActive));

					}
					try (PreparedStatement prs1 = connection
							.prepareStatement("insert into RecordTable values ((?),(?),(?),(?))")) {
						prs1.setString(1, record.getMail());
						prs1.setString(2, String.valueOf(Instant.ofEpochMilli(record.createTime)));
						prs1.setString(3, String.valueOf(Instant.ofEpochMilli(record.lastUpdate)));
						prs1.setString(4, String.valueOf(record.isOnline));

					}
				} else {
					String where = rs.getString("submail");
					if (ch.chcontent instanceof Database.Record) {
						try (PreparedStatement prs1 = connection
								.prepareStatement(
										"update SubTable set subname=(?), subsurname=(?), subpaswd=(?) where submail=(?)")) {
							prs1.setString(1, sub.getName());
							prs1.setString(2, sub.getSurname());
							prs1.setString(3, sub.getPassword());
							prs1.setString(4, sub.getMail());
							prs1.executeUpdate();

						}
						try (PreparedStatement prs1 = connection
								.prepareStatement(
										"update SessionTable set creatTime=(?), lastLogin=(?), isActive=(?) where submail=(?)")) {
							prs1.setString(1, String.valueOf(Instant.ofEpochMilli(session.createTime)));
							prs1.setString(2, String.valueOf(Instant.ofEpochMilli(session.lastlogin)));
							prs1.setString(3, String.valueOf(session.isActive));
							prs1.setString(4, session.email);
							prs1.executeUpdate();

						}
						try (PreparedStatement prs1 = connection
								.prepareStatement(
										"update RecordTable set creatTime=(?), lastUpdate=(?), isOnline=(?) where submail=(?)")) {
							prs1.setString(1, String.valueOf(Instant.ofEpochMilli(record.createTime)));
							prs1.setString(2, String.valueOf(Instant.ofEpochMilli(record.lastUpdate)));
							prs1.setString(3, String.valueOf(record.isOnline));
							prs1.setString(4, record.getMail());
							prs1.executeUpdate();

						}
					} else if (ch.chcontent instanceof Session) {
						try (PreparedStatement prs1 = connection
								.prepareStatement(
										"update SessionTable set creatTime=(?), lastLogin=(?), isActive=(?) where submail=(?)")) {
							prs1.setString(1, String.valueOf(Instant.ofEpochMilli(session.createTime)));
							prs1.setString(2, String.valueOf(Instant.ofEpochMilli(session.lastlogin)));
							prs1.setString(3, String.valueOf(session.isActive));
							prs1.setString(4, session.email);
							prs1.executeUpdate();

						}
						try (PreparedStatement prs1 = connection
								.prepareStatement(
										"update RecordTable set creatTime=(?), lastUpdate=(?), isOnline=(?) where submail=(?)")) {
							prs1.setString(1, String.valueOf(Instant.ofEpochMilli(record.createTime)));
							prs1.setString(2, String.valueOf(Instant.ofEpochMilli(record.lastUpdate)));
							prs1.setString(3, String.valueOf(record.isOnline));
							prs1.setString(4, record.getMail());
							prs1.executeUpdate();

						}
					} else if (ch.chcontent instanceof Subscriber) {
						try (PreparedStatement prs1 = connection
								.prepareStatement(
										"update SubTable set subname=(?), subsurname=(?), subpaswd=(?) where submail=(?)")) {
							prs1.setString(1, sub.getName());
							prs1.setString(2, sub.getSurname());
							prs1.setString(3, sub.getPassword());
							prs1.setString(4, sub.getMail());
							prs1.executeUpdate();

						}
						try (PreparedStatement prs1 = connection
								.prepareStatement(
										"update RecordTable set creatTime=(?), lastUpdate=(?), isOnline=(?) where submail=(?)")) {
							prs1.setString(1, String.valueOf(Instant.ofEpochMilli(record.createTime)));
							prs1.setString(2, String.valueOf(Instant.ofEpochMilli(record.lastUpdate)));
							prs1.setString(3, String.valueOf(record.isOnline));
							prs1.setString(4, record.getMail());
							prs1.executeUpdate();

						}
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

		}

		public static Database getActualDatabase(int PORT) throws Exception {
			Database temp = null;
			for (int i : ports) {
				if (i != PORT) {
					if ((database = (Database) bringObject(new ProtocolMessage("SERILESTIRILMIS_NESNE pulldb"),
							i)) != null) {
						if (temp == null)
							temp = database;
						if (temp != null && database != null && database.change != null && temp.change != null
								&& temp.change.chtime < database.change.chtime)
							temp = database;
						;
					}
				}
			}
			return temp;
		}

		public static Database getTempDB() throws Exception {
			file = new File("db%dtemp".formatted(port));
			if (file != null && file.length() > -1) {
				Object obj = null;
				try {
					obj = fois.readObject();
				} catch (EOFException e) {
					obj = null;
				}
				if (obj instanceof Database) {
					return (Database) obj;
				} else
					return null;
			} else
				return null;
		}

		public static Database syncDB(int PORT) throws Exception {
			// synchronize database with its backup or running other databases
			if ((database = getTempDB()) != null) {
				return database;
			} else if ((database = getActualDatabase(PORT)) != null) {
				return database;
			} else {
				return null;
			}
		}

	}

	static class Listener implements Runnable {
		public void run() {
			try {
				while (true) {
					incomingConnection = serverSock.accept();
					System.out.printf("Connection received from %s:%s\r\n", serverSock.getInetAddress(),
							incomingConnection.getPort());

				}
			} catch (Exception e) {
				System.out.println("Listener Crash");
			}
		}
	}

	abstract static class Handler implements Runnable {
		volatile static Socket clientSocket = null;
		volatile static Session clientSession = null;
		volatile static InputStream is = null;
		volatile static OutputStream os = null;
		volatile static ObjectInputStream ois = null;
		volatile static ObjectOutputStream oos = null;
		volatile static Object request = null;
		volatile static Thread receiver = null;

		static class MessageHandler implements Runnable {
			public void run() {
				while (ois != null) {
					try {
						if (is.available() > 0) {
							if (request != null) {
								while (request != null)
									;
							}
							request = ois.readObject();
						}
					} catch (Exception e) {
						return;
					}
				}
			}
		}

		public static Object getReceived() {
			if (request == null) {
				while (true) {
					if (request != null)
						break;
				}
			}
			Object temp = request;
			request = null;
			return temp;
		}

		public Handler(Socket client) throws Exception {
			clientSocket = client;
			try {
				is = clientSocket.getInputStream(); // cilent receive stream
				os = clientSocket.getOutputStream(); // client send stream

				ois = new ObjectInputStream(is);
				oos = new ObjectOutputStream(os);
				receiver = new Thread(new MessageHandler());
				receiver.start();
			} catch (Exception e) {
				// System.out.println("======");
				System.err.println("Client Stream Crash");
				oos.writeObject((Object) new ProtocolMessage("Server Side Error"));
				oos.writeObject((Object) null);
				oos.flush();
				clientSocket.close();
				// System.out.println("======");
				// System.err.println(e);
				// e.printStackTrace();
				throw e;
			}
		}

		public void run() {
			try {
				Object recvObj = getReceived();
				ProtocolMessage recvMessage = null;
				if (recvObj instanceof ProtocolMessage) {
					recvMessage = (ProtocolMessage) recvObj;
					System.out.println(recvMessage.message);
					String[] message = recvMessage.message.split(" ");
					System.out.println(Arrays.asList(message));
					doASUP(message);
					System.out.printf(
							"\r\nrequest time : %s\r\ndatabase content : %s\r\ndatabase change : %s\r\ndatabase changelog %s\r\nnchange : %s\r\n",
							Instant.now(), database.subscriberTable.toString(),
							database != null && database.change != null && database.changeLog != null
									&& database.changeLog.peek() != null ? database.changeLog.peek().chtime : "null",
							database != null && database.changeLog != null ? database.changeLog.toString() : "null",
							database.changeLog != null ? database.changeLog.size() : 0);
				} else {
					if (recvObj != null) {
						System.out.printf("%s type %s from %s\r\n", recvObj.getClass(), recvObj,
								clientSocket.getPort());
					} else {
						System.out.println("null request");
					}
				}
			} catch (ConnectException e) {
				System.out.println("Connection Lost");
			} catch (Exception e) {
				try {
					e.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} finally {
				try {
					oos.writeObject((Object) null);
					clientSocket.close();
					System.gc();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public void doASUP(String[] message) throws Exception {
			if (message[0].equals("ABONOL") || message[0].equals("GIRIS") || message[0].equals("CIKIS")
					|| message[0].equals("ABONIPTAL")) {
				Object obj = getReceived();
				if (obj != null && obj.getClass() == Session.class) {
					clientSession = (Session) obj;
					if (clientSession.email == null)
						clientSession = null;
				}
				System.out.printf("%s request from client with %s session \r\n", Arrays.toString(message),
						clientSession != null ? clientSession.email : "null");
				asupClients(message);
			} else if (message[0].equals("SERILESTIRILMIS_NESNE")) {
				asupServers(message);
				// System.out.println("from Server %s\r\n",);
			} else {
				;
			}
		}

		abstract public void asupClients(String[] message) throws Exception;

		public void asupServers(String[] message) throws Exception {
			if (message[1].contentEquals("post")) {
				Object recvObj = getReceived();

				if (recvObj instanceof ProtocolMessage) {
					;
				} else {
					if ((database.insert(message[2], (Object) recvObj)) != null) {
						System.out.println("Inserted to database");
					} else {
						System.out.println("Cant insert to database");
					}
				}
			} else if (message[1].contentEquals("get")) {
				oos.writeObject((Object) database.subscriberTable.get(message[2]));
				oos.flush();
			} else if (message[1].contentEquals("pulldb")) {
				oos.writeObject((Object) database);
				oos.flush();
			} else if (message[1].contentEquals("delete")) {
				database.delete(message[2]);
			}
		}

	}

	public static Object bringObject(ProtocolMessage message, int port) {
		Socket socket = null;
		Object recv = null;
		try {
			socket = new Socket();
			for (; !socket.isConnected() && !socket.isClosed();) {
				try {
					if (socket.isClosed())
						socket = new Socket();
					if (socket.isConnected())
						socket = new Socket();
					else if (!socket.isBound())
						socket = new Socket();
					socket.connect(new InetSocketAddress(port));
				} catch (ConnectException e) {
					System.out.printf("Server at %d port is closed\r\n", port);
					return null;
				}
			}
			// try connect every 3 seconds
			OutputStream oss = socket.getOutputStream();
			InputStream iss = socket.getInputStream();
			ObjectOutputStream ooss = new ObjectOutputStream(oss);
			ObjectInputStream oiss = new ObjectInputStream(iss);

			ooss.writeObject((Object) message);
			recv = oiss.readObject();
			oss.close();
			iss.close();
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			try {
				socket.close();
				socket = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return recv;
	}
}
