import java.net.*;
import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;

public class Server1 extends IServer {
    static final public int PORT = 5001;

    public static void main(String args[]) {
        try {
            serverSock = new ServerSocket(PORT);
            connectionQueue = new LinkedList<Socket>();
            Thread clientListener = new Thread(new Listener());
            clientListener.start();
            // Connection accepted and creating handler for each client on server side
            Thread dbTemp = new Thread(new DBTemp(PORT));
            while (true) {
                try {
                    if (database == null) {
                        if ((database = DBTemp.syncDB(PORT)) == null) {
                            database = new Database(PORT);
                        }
                        System.out.println("Current table : " + database.subscriberTable.toString());
                        System.err.println("database timestamp : " + Date.from(Instant.ofEpochMilli(database.lastUpdate)));
                        dbTemp.start();
                    }
                    if (!connectionQueue.isEmpty()) {
                        Thread clientThread = null;
                        try {
                            clientThread = new Thread(new Handler(connectionQueue.poll()));
                            clientThread.start();
                            incomingConnection = null;
                        }
                        catch (Exception e) {
                            incomingConnection = null;
                        }
                    }

                }
                catch (Exception e) {
                    System.out.println("Thread crash");
                    break;
                }
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static class Handler extends IServer.Handler {
        public Handler(Socket client) throws Exception {
            super(client);
        }

        @Override
        public void asupClients(String[] message) throws Exception {
            if (message[0].contentEquals("ABONOL")) {
                if (clientSession == null || clientSession.email == null) {
                    if (message.length > 2) {
                        if (database.subscriberTable.get(message[1]) == null) {
                            Subscriber sub = new Subscriber(message[1], message[2]);
                            Database.Record record = null;
                            if ((record = database.insert(message[1], sub)) != null) {
                                clientSession = record.session;
                                oos.writeObject((Object) clientSession);
                                oos.flush();
                                oos.writeObject((Object) new ProtocolMessage("Succesfully Subscribed"));
                                new Thread(new Dispatcher((Object) record, PORT, new ProtocolMessage("SERILESTIRILMIS_NESNE post " + message[1]))).start();
                            }
                            else {
                                oos.writeObject((Object) new ProtocolMessage("Can't Register"));
                            }
                            oos.flush();
                        }
                        else {
                            oos.writeObject((Object) new ProtocolMessage("User Already Exist"));
                            oos.flush();
                        }
                    }
                    else {
                        oos.writeObject((Object) new ProtocolMessage("Enter mail adress and password"));
                        oos.flush();
                    }
                }
                else {
                    if (!clientSession.isActive) {
                        oos.writeObject((Object) new ProtocolMessage("Session Expired"));
                        oos.flush();
                        oos.writeObject((Object) null);
                        oos.flush();
                    }
                    else {
                        oos.writeObject((Object) new ProtocolMessage("Logout first"));
                        oos.flush();
                    }
                }
            }
        }

    }
}
