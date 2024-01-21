import java.io.*;
import java.net.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public abstract class IServer {
	static volatile ServerSocket serverSock = null;
	static volatile Socket incomingConnection = null;
	static volatile Queue<Socket> connectionQueue = null;
	static volatile Database database = null;

	static int[] ports = { 5001, 5002, 5003, 5004 };

	static int PORT;

	static class Listener implements Runnable {
		public void run() {
			try {
				while (true) {
					incomingConnection = serverSock.accept();
					connectionQueue.add(incomingConnection);
					System.out.printf("Log: Connection received from %s\r\n", incomingConnection.getRemoteSocketAddress());

				}
			} catch (Exception e) {
				System.out.println("ThreadException: Listener Crash");
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
					doASUP(message);
					System.out.printf("\r\n====REQUEST====\r\nrequest time : %s\r\ndatabase timestamp: %s\r\ndatabase content : %s\r\ndatabase change : %s\r\ndatabase changelog %s\r\nnchange : %s\r\n", java.sql.Date.from(Instant.now()), java.sql.Date.from(Instant.ofEpochMilli(database.lastUpdate)), database.subscriberTable.toString(), database != null && database.change != null && database.changeLog != null && database.changeLog.peek() != null ? database.changeLog.peek().timestamp : "null", database != null && database.changeLog != null ? database.changeLog.toString() : "null", database.changeLog != null ? database.changeLog.size() : 0);
				} else {
					if (recvObj != null) {
						System.out.printf("%s type %s from %s\r\n", recvObj.getClass(), recvObj, clientSocket.getPort());
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
			if (message[0].equals("ABONOL") || message[0].equals("GIRIS") || message[0].equals("CIKIS") || message[0].equals("ABONIPTAL")) {
				System.out.printf("Request: %s from %s session \r\n", Arrays.toString(message), clientSession != null ? clientSession.email : "null");
				Object obj = getReceived();
				if (obj != null && obj.getClass() == Session.class) {
					clientSession = (Session) obj;
					if (clientSession.email == null)
						clientSession = null;
				}
				asupClients(message);
			} else if (message[0].equals("SERILESTIRILMIS_NESNE")) {
				System.err.printf("Request: %s from server at %s\r\n", Arrays.toString(message), clientSocket.getRemoteSocketAddress());
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
				} else if (recvObj instanceof Database.Record) {
					if (database.subscriberTable.get(message[2]) == null) {
						if ((database.insert(message[2], (Object) recvObj)) != null) {
							System.out.println("Inserted to database");
						} else {
							System.out.println("Problem while inserting");
						}
					} else {
						if (database.modify(message[2], recvObj) != null) {
							System.out.println("Succesfully updated");
						} else {
							System.out.println("Problem while updating");
						}
					}
				} else if (recvObj instanceof Database) {
					database = (Database) recvObj;
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
			// connection =
			// DriverManager.getConnection(String.format("jdbc:mysql://localhost:3306/server%s",
			// port), "root","_toor123");
			connection = DriverManager.getConnection(String.format("jdbc:sqlite:/home/furkan/Databases/SQLite_databases/db%s.db", port));

			createTable();
		}

		public void run() {
			try {
				while (true) {
					try {
						if (database != null) {
							if (database.change != null) {
								if (database.lastUpdate == database.createTime) {
									createTable();
								} else {
									updateTable();
									database.clearChange();
								}
							}

						}
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

		public static void createTable() throws Exception {
			Statement stmt = connection.createStatement();
			stmt.execute("create table if not exists TableTracker (tablename varchar(20),submail varchar(20),operation varchar(20), tcreatTime bigint,tlastUpdate bigint, primary key (tablename));");
			long createTime = Instant.now().toEpochMilli();
			stmt.execute(String.format("insert or ignore into TableTracker values ('SubTable',null,null,\'%s\',\'%s\')", createTime, createTime));
			stmt.execute(String.format("insert or ignore into TableTracker values ('SessionTable',null,null,\'%s\',\'%s\')", createTime, createTime));
			stmt.execute(String.format("insert or ignore into TableTracker values ('RecordTable',null,null,\'%s\',\'%s\')", createTime, createTime));
			stmt.execute("create table if not exists RecordTable ( submail varchar(20),rcreatTime bigint, rlastUpdate bigint, isOnline boolean , foreign key (submail) references SubTable(submail),foreign key (isOnline) references SessionTable(isActive));");
			stmt.execute("create table if not exists SessionTable ( submail varchar(20),session varchar(20),screatTime bigint, slastUpdate bigint, isActive boolean as (slastUpdate-screatTime < 2), foreign key (submail) references SubTable(submail));");
			stmt.execute("CREATE TABLE if not exists SubTable ( submail varchar(20),subname varchar(20),subsurname varchar(20),subpaswd varchar(20),primary key (submail));");
			// stmt.execute("create table if not exists TableTracker ( submail varchar(20),
			// dlastUpdate
		}

		public static void updateTable() throws Exception {
			Change ch = database.change;
			String operation = (String) database.change.operation;
			Database.Record record = (Database.Record) ch.content; // Only records Changed in database and will be
																	// updated rest of tables
			Subscriber sub = (Subscriber) record;
			Session session = record.session;
			Statement stmt = connection.createStatement();
			try (PreparedStatement prs = connection.prepareStatement("select SubTable.*,SessionTable.screatTime,SessionTable.slastUpdate,SessionTable.isActive,RecordTable.rcreatTime,RecordTable.rlastUpdate,RecordTable.isOnline  from SubTable left join SessionTable on SubTable.submail=SessionTable.submail left join RecordTable on SubTable.submail = RecordTable.submail where SubTable.submail=(?);"
			/*
			 * "select * from SubTable natural join SessionTable natural join RecordTable;"
			 */)) {
				prs.setString(1, record.getMail());
				ResultSet rs = prs.executeQuery(); // only one row expected becuase submail unique and primary
				String where = rs.getString("submail");
				// ? if requested data is not exists in

				if (operation.equals("INSERT")) {
					if (where != null) {
						System.out.println("User already inserted");
						return;
					}
					try (PreparedStatement prs1 = connection.prepareStatement("insert into SubTable values ( (?),(?),(?),(?) )")) {
						prs1.setString(1, sub.getMail());
						prs1.setString(2, sub.getName());
						prs1.setString(3, sub.getSurname());
						prs1.setString(4, sub.getPassword());
						prs1.executeUpdate();
					} catch (Exception e) {
						System.err.println(e.getMessage());
					}
					try (PreparedStatement prs1 = connection.prepareStatement("insert into SessionTable (submail,session,screatTime,slastUpdate) values ((?),(?),(?),(?))")) {
						prs1.setString(1, record.getMail());
						prs1.setString(2, record.session.email);
						prs1.setString(3, Instant.ofEpochMilli(session.createTime).toString());
						prs1.setString(4, Instant.ofEpochMilli(session.lastlogin).toString());
						prs1.executeUpdate();
					} catch (SQLException e) {
						System.out.println(e.getMessage());
					}
					try (PreparedStatement prs1 = connection.prepareStatement("insert into RecordTable values ((?),(?),(?),(?))")) {
						prs1.setString(1, record.getMail());
						prs1.setString(2, Instant.ofEpochMilli(record.createTime).toString());
						prs1.setString(3, Instant.ofEpochMilli(record.lastUpdate).toString());
						prs1.setString(4, String.valueOf(record.isOnline));
						prs1.executeUpdate();
					} catch (Exception e) {
						System.err.println(e.getMessage());
					}
					try {
						stmt.executeUpdate(String.format("update TableTracker set submail=\'%s\',operation=\'%s\',tlastUpdate=\'%s\' where tablename='RecordTable'", record.getMail(), ch.operation, Instant.ofEpochMilli(database.lastUpdate).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						stmt.executeUpdate(String.format("update TableTracker set submail=\'%s\',operation=\'%s\',tlastUpdate=\'%s\' where tablename='SubTable'", record.getMail(), ch.operation, Instant.ofEpochMilli(database.lastUpdate).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						stmt.executeUpdate(String.format("update TableTracker set submail=\'%s\',operation=\'%s\',tlastUpdate=\'%s\' where tablename='SessionTable'", record.getMail(), ch.operation, Instant.ofEpochMilli(database.lastUpdate).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (operation.equals("UPDATE")) {
					if (where == null) {
						System.out.println("User not exists");
						return;
					}
					if (ch.operation instanceof Database.Record) {
						try (PreparedStatement prs1 = connection.prepareStatement("update SubTable set subname=(?), subsurname=(?), subpaswd=(?) where submail=(?)")) {
							prs1.setString(1, sub.getName());
							prs1.setString(2, sub.getSurname());
							prs1.setString(3, sub.getPassword());
							prs1.setString(4, record.getMail());
							prs1.executeUpdate();

						}
						try (PreparedStatement prs1 = connection.prepareStatement("update SessionTable set session=(?), screatTime=(?), slastUpdate=(?) where submail=(?)")) {
							prs1.setString(1, session.email);
							prs1.setString(2, Instant.ofEpochMilli(session.createTime).toString());
							prs1.setString(3, Instant.ofEpochMilli(session.lastlogin).toString());
							prs1.setString(4, record.getMail());
							prs1.executeUpdate();

						}
						try (PreparedStatement prs1 = connection.prepareStatement("update RecordTable set rcreatTime=(?), rlastUpdate=(?), isOnline=(?) where submail=(?)")) {
							prs1.setString(1, (Instant.ofEpochMilli(record.createTime)).toString());
							prs1.setString(2, Instant.ofEpochMilli(record.lastUpdate).toString());
							prs1.setString(3, String.valueOf(record.isOnline));
							prs1.setString(4, record.getMail());
							prs1.executeUpdate();

						}
					} else if (ch.operation instanceof Session) {
						try (PreparedStatement prs1 = connection.prepareStatement("update SessionTable set session=(?), screatTime=(?), slastUpdate=(?) where submail=(?)")) {
							prs1.setString(1, record.session.email);
							prs1.setString(2, Instant.ofEpochMilli((session.createTime)).toString());
							prs1.setString(3, Instant.ofEpochMilli(session.lastlogin).toString());
							prs1.setString(4, record.getMail());
							prs1.executeUpdate();

						}
						try (PreparedStatement prs1 = connection.prepareStatement("update RecordTable set rcreatTime=(?), rlastUpdate=(?), isOnline=(?) where submail=(?)")) {
							prs1.setString(1, Instant.ofEpochMilli(record.createTime).toString());
							prs1.setString(2, Instant.ofEpochMilli(record.lastUpdate).toString());
							prs1.setString(3, String.valueOf(record.isOnline));
							prs1.setString(4, record.getMail());
							prs1.executeUpdate();

						}
					} else if (ch.operation instanceof Subscriber) {
						try (PreparedStatement prs1 = connection.prepareStatement("update SubTable set subname=(?), subsurname=(?), subpaswd=(?) where submail=(?)")) {
							prs1.setString(1, sub.getName());
							prs1.setString(2, sub.getSurname());
							prs1.setString(3, sub.getPassword());
							prs1.setString(4, sub.getMail());
							prs1.executeUpdate();

						}
						try (PreparedStatement prs1 = connection.prepareStatement("update RecordTable set rcreatTime=(?), rlastUpdate=(?), isOnline=(?) where submail=(?)")) {
							prs1.setString(1, Instant.ofEpochMilli(record.createTime).toString());
							prs1.setString(2, Instant.ofEpochMilli(record.lastUpdate).toString());
							prs1.setString(3, String.valueOf(record.isOnline));
							prs1.setString(4, record.getMail());
							prs1.executeUpdate();

						}
					}
					try {
						stmt.executeUpdate(String.format("update TableTracker set submail=\'%s\',operation=\'%s\',tlastUpdate=\'%s\' where tablename='RecordTable'", record.getMail(), ch.operation, Instant.ofEpochMilli(database.lastUpdate).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						stmt.executeUpdate(String.format("update TableTracker set submail=\'%s\',operation=\'%s\',tlastUpdate=\'%s\' where tablename='SubTable'", record.getMail(), ch.operation, Instant.ofEpochMilli(database.lastUpdate).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						stmt.executeUpdate(String.format("update TableTracker set submail=\'%s\',operation=\'%s\',tlastUpdate=\'%s\' where tablename='SessionTable'", record.getMail(), ch.operation, Instant.ofEpochMilli(database.lastUpdate).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (operation.equals("DELETE")) {
					if (where == null) {
						System.out.println("User not exists");
						return;
					}
					try (PreparedStatement prs1 = connection.prepareStatement("delete from RecordTable where submail=(?)")) {
						prs1.setString(1, record.getMail());
						prs1.executeUpdate();
					} catch (Exception e) {
						e.printStackTrace();
					}
					try (PreparedStatement prs1 = connection.prepareStatement("delete from SessionTable where submail=(?)")) {
						prs1.setString(1, record.getMail());
						prs1.executeUpdate();
					} catch (Exception e) {
						e.printStackTrace();
					}
					try (PreparedStatement prs1 = connection.prepareStatement("delete from SubTable where submail=(?)")) {
						prs1.setString(1, record.getMail());
						prs1.executeUpdate();
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						stmt.executeUpdate(String.format("update TableTracker set submail=\'%s\',operation=\'%s\',tlastUpdate=\'%s\' where tablename='RecordTable'", record.getMail(), ch.operation, Instant.ofEpochMilli(database.lastUpdate).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						stmt.executeUpdate(String.format("update TableTracker set submail=\'%s\',operation=\'%s\',tlastUpdate=\'%s\' where tablename='SubTable'", record.getMail(), ch.operation, Instant.ofEpochMilli(database.lastUpdate).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						stmt.executeUpdate(String.format("update TableTracker set submail=\'%s\',operation=\'%s\',tlastUpdate=\'%s\' where tablename='SessionTable'", record.getMail(), ch.operation, Instant.ofEpochMilli(database.lastUpdate).toString()));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Operation not recognized " + operation);
				}
			} catch (

			Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}

		}

		public static void backupTable() throws Exception {
			if (database != null && !database.subscriberTable.isEmpty()) {
				Set<String> keys = database.subscriberTable.keySet();
				HashMap<String, Database.Record> subs = database.subscriberTable;
				Iterator<String> i = keys.iterator();

				while (true) {
					Database.Record record = subs.get((String) i.next());
					if (record != null) {
						try (PreparedStatement prs = connection.prepareStatement("insert into RecordTable ((?),(?),(?),(?))")) {
							prs.setString(1, record.getMail());
							prs.setString(2, Instant.ofEpochMilli(record.createTime).toString());
							prs.setString(3, Instant.ofEpochMilli(record.lastUpdate).toString());
							prs.setString(4, String.valueOf(record.isOnline));
						}
					} else if (record.getMail() != null) {
						try (PreparedStatement prs = connection.prepareStatement("insert into SubTable values ((?),(?),(?),(?))")) {
							prs.setString(1, record.getMail());
							prs.setString(2, record.getName());
							prs.setString(3, record.getSurname());
							prs.setString(4, record.getPassword());
						}
					} else if (record.session.email != null) {
						try (PreparedStatement prs = connection.prepareStatement("insert into SessionTable values ((?),(?),(?),(?))")) {
							prs.setString(1, record.getMail());
							prs.setString(2, record.session.email);
							prs.setString(3, Instant.ofEpochMilli(record.session.createTime).toString());
							prs.setString(4, Instant.ofEpochMilli(record.session.lastlogin).toString());
						}
					}
					if (!i.hasNext()) {
						break;
					} else
						i.next();
				}
			}
		}

		public static Database getLocalTemp() {
			try {

				Statement stmt = connection.createStatement();
				Database db = new Database(port);
				db.change = null;
				db.createTime = 0;
				db.lastUpdate = 0;
				Database.Record record = null;
				ResultSet rs = stmt.executeQuery("select *,concat(SubTable.submail) as smail from SubTable full join SessionTable full join RecordTable");
				Statement stmt2 = connection.createStatement();
				ResultSet rs2 = stmt2.executeQuery("select * from TableTracker where tablename='RecordTable'");
				while (true) {
					db.createTime = rs2.getLong("tcreatTime");
					db.lastUpdate = rs2.getLong("tlastUpdate");
					if (rs.getString("submail") == null && rs.next() == false) {
						break;
					} else if (rs.getString("submail") == null) {
						break;
					}

					record = new Database.Record(new Subscriber(rs.getString("smail"), rs.getString("subsurname"), rs.getString("submail"), rs.getString("subpaswd")));
					try {
						record.createTime = rs.getLong("rcreatTime");
					} catch (Exception e) {
						record.createTime = 0;
					}
					try {
						record.lastUpdate = rs.getLong("lastUpdate");
					} catch (Exception e) {
						record.lastUpdate = 0;
					}
					record.isOnline = rs.getBoolean("isOnline");
					record.session = new Session(record.getMail());
					try {
						record.session.createTime = rs.getLong("screatTime");
					} catch (Exception e) {
						record.session.createTime = 0;
					}
					try {
						record.session.lastlogin = rs.getLong("lastLogin");
					} catch (Exception e) {
						record.session.lastlogin = 0;
					}
					record.session.isActive = rs.getBoolean("isActive");
					db.subscriberTable.put(record.getMail(), record);
					if (rs.next() == false)
						break;
					;
				}
				return db;
			} catch (Exception e) {
				System.err.println(e.getMessage());
				return null;
			}
		}
	}

	public static Database getExternalTemp(int PORT) throws Exception {
		Database temp = null;
		for (int i : ports) {
			if (i != PORT) {
				Object recv = bringObject(new ProtocolMessage("SERILESTIRILMIS_NESNE pulldb"), i);
				if (recv instanceof Database) {
					if ((database = (Database) recv) != null) {
						if (temp == null)
							temp = database;
						if (temp != null && database != null && temp.lastUpdate < database.lastUpdate)
							temp = database;
						;
					} else {

					}
				}
			}
		}
		return temp;
	}

	public static Database syncDB(int PORT) throws Exception {
		Database localdb = null;
		Database externaldb = null;
		Database temp = null;
		localdb = DBTemp.getLocalTemp();
		externaldb = getExternalTemp(PORT);
		if (localdb != null) {
			if (externaldb != null) {
				if (localdb.lastUpdate < externaldb.lastUpdate) {
					externaldb.subscriberTable.putAll((Map<String, Database.Record>) localdb.subscriberTable);
					temp = externaldb;
				} else { // TODO: make difference with actual database and database that in ram
					localdb.subscriberTable.putAll((Map<String, Database.Record>) externaldb.subscriberTable);
					temp = localdb;
				}
			} else
				temp = localdb;
		} else if (externaldb != null) {
			temp = externaldb;
		} else {
			temp = null;
		}
		new Thread(new Dispatcher(temp, PORT, new ProtocolMessage("SERILESTIRILMIS_NESNE post null"))).start();
		return temp;
	}

	public static class Dispatcher implements Runnable {
		Object obj = null;
		int port;
		ProtocolMessage message = null;

		public Dispatcher(Object obj, int excludePort, ProtocolMessage message) {
			this.obj = obj;
			this.port = excludePort;
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
				System.err.println("Exception:Dispatcher exception");
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

	public static Object bringObject(ProtocolMessage message, int port) {
		Socket socket = null;
		Object recv = null;
		OutputStream oss = null;
		InputStream iss = null;
		ObjectOutputStream ooss = null;
		ObjectInputStream oiss;
		try {
			socket = new Socket();
			for (; !socket.isConnected() && !socket.isClosed();) {
				try {
					if (socket.isClosed())
						socket = new Socket();
					if (socket.isConnected())
						socket = new Socket();
					socket.connect(new InetSocketAddress(port));
				} catch (ConnectException e) {
					// System.out.printf("Server at %d port is closed\r\n", port);
					return null;
				}
			}
			// try connect every 3 seconds
			oss = socket.getOutputStream();
			iss = socket.getInputStream();
			ooss = new ObjectOutputStream(oss);
			oiss = new ObjectInputStream(iss);
			System.out.printf("Request: %s to %s\r\n", message.message, socket.getRemoteSocketAddress());
			ooss.writeObject((Object) message);
			recv = oiss.readObject();
			ooss.writeObject((Object) new ProtocolMessage("55 TAMM"));
			oss.close();
			iss.close();
		} catch (Exception e) {
			try {
				ooss.writeObject((Object) new ProtocolMessage("99 HATA"));
			} catch (Exception f) {
				;
			}
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
