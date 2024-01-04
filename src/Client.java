import java.io.*;
import java.net.*;
import java.util.*;
public class Client {

    public static Socket clientSocket;
    public static Session session = (Session) new Session(null);

    public static void main(String args[]) {
        try {
            clientSocket = new Socket(); // connections here
        }
        catch (Exception e) {
            System.out.println(e);
        }
        runShell();
    }

    public static void runShell() {
        String command;
        Scanner scanner = null;
        ;
        try {
            scanner = new Scanner(System.in);
            while (true) {
                try {
                    System.out.printf("%s $> ", session != null ? (String) session.email : "null");
                    command = scanner.nextLine();
                    runCommand(command);
                }
                catch (NoSuchElementException e) {
                    break;
                }
                catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            return;
        }
        finally {
            if (scanner != null)
                scanner.close();
        }
    }

    public static void readFile(String f) throws Exception {
        BufferedReader br = null;
        FileReader fr = null;
        try {
            fr = new FileReader(f);
            br = new BufferedReader(fr);
            String buff;
            do {
                buff = br.readLine();
                System.out.println(buff);
            }
            while (buff != null);
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            if (br != null)
                br.close();
        }
    }

    public static void runCommand(String message) throws Exception {
        message = message.trim();
        String[] args = message.split(" ");
        args[0] = args[0].toUpperCase();
        String command = args[0];
        message = String.join(" ", args);
        message = message.trim();
        if (command.equals("HELP")) {
            readFile("./help.txt");
        }
        else if (command.equals("ABONOL") || command.equals("ABONIPTAL") || command.equals("GIRIS")
                || command.equals("CIKIS")) {
            communicate(message);
        }
        else {
            System.err.println("command not found");
        }
    }

    public static void communicate(String message) throws Exception {
        InetSocketAddress destination = null;
        // ! SPECIFY CONNECTIONS FOR SERVERS
        // IF MESSAGE == ABONOL || ABONIPTAL

        if (message.contains("ABONOL"))
            destination = new InetSocketAddress(Server1.PORT);
        if (message.contains("ABONIPTAL")) {
            destination = new InetSocketAddress(Server3.PORT);
        }
        if (message.contains("GIRIS") || message.contains("CIKIS")) {
            destination = new InetSocketAddress(Server2.PORT);
        }

        if (clientSocket.isConnected()) {
            clientSocket.close();
            clientSocket = new Socket();
        }
        if (clientSocket.isClosed())
            clientSocket = new Socket();
        if (!clientSocket.isBound())
            clientSocket = new Socket();
        try {
            clientSocket.connect(destination);
        }
        catch (ConnectException e) {
            System.out.println("Sunucu kapali");
            return;
        }
        clientSocket.setTcpNoDelay(false);
        OutputStream os = clientSocket.getOutputStream();
        InputStream is = clientSocket.getInputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        ObjectInputStream ois = new ObjectInputStream(is);
        Object recvobj = null;
        String recvstr = null;
        oos.writeObject((Object) new ProtocolMessage(message));
        oos.flush();
        oos.writeObject((Object) session);
        oos.flush();
        while ((recvobj = ois.readObject()) != null) {
            try {
                if (recvobj instanceof ProtocolMessage) {
                    recvstr = (String) ((ProtocolMessage) recvobj).message;
                    System.out.println(recvstr);
                }
                else if (recvobj instanceof Session) {
                    session = (Session) recvobj;
                }
                else {
                    System.out.printf("%s type %s\r\n", recvobj.getClass(), recvobj);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}