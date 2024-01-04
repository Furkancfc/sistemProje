import java.io.*;
import java.net.*;

public class DBsync {
	static volatile Socket s1;
	static volatile Socket s2;
	static volatile Socket s3;;

	public static void main(String[] args) {
		s1 = new Socket();
		s2 = new Socket();
		s3 = new Socket();
		try {
			Thread t1 = new Thread(new Server1Handler());
			Thread t2 = new Thread(new Server2Handler());
			Thread t3 = new Thread(new Server3Handler());
			t1.start();
			t2.start();
			t3.start();
			while (true) {
				if (s1.isClosed())
					s1 = new Socket();
				if (s2.isClosed())
					s2 = new Socket();
				if (s3.isClosed())
					s3 = new Socket();
			}

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class Server1Handler implements Runnable {
		public void run() {
			while (true) {
				try {
					s1.connect(new InetSocketAddress(5001));
					if (s1.isConnected()) {

					}

				}
				catch (Exception e) {

				}
			}
		}
	}

	static class Server2Handler implements Runnable {
		public void run() {
			while (true) {
				try {
					s2.connect(new InetSocketAddress(5002));
					if (s2.isConnected()) {

					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	static class Server3Handler implements Runnable {
		public void run() {
			while (true) {
				try {
					s3.connect(new InetSocketAddress(5003));
					if (s3.isConnected()) {

					}
				}
				catch (Exception e)

				{
					e.printStackTrace();
				}
			}
		}
	}
}
