import java.io.*;
import java.net.*;

public class TCPServer {
	public static final int PORT = 5000;
	public static void main(String[] args) throws IOException {
		ServerSocket s = new ServerSocket(PORT);
		System.out.println("Working: " + s);
		try {
			Socket mysocket = s.accept();
			try {
				System.out.println("Connection accepted: "+ mysocket);
				BufferedReader in = new BufferedReader(new InputStreamReader(mysocket.getInputStream()));
				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mysocket.getOutputStream())),true);
				while (true) {
					String str = in.readLine();
					if (str.equals("END")) break;
					System.out.println("Received: " + str);
					out.println(str);
				}			
			} finally {
				System.out.println("Closing...");
				mysocket.close();
			}
		} finally {
			s.close();
		}
	}
}