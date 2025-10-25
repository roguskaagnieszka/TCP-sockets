import java.net.*;
import java.io.*;

public class TCPClient {
	public static void main(String[] args) throws IOException {
		InetAddress addr = InetAddress.getByName(null);
		System.out.println("addr = " + addr);
		Socket mysocket = new Socket(addr, TCPServer.PORT);
		try {
			System.out.println("Socket : " + mysocket);
			BufferedReader in = new BufferedReader(new InputStreamReader(mysocket.getInputStream()));
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mysocket.getOutputStream())),true);
			for(int i = 0; i < 10; i ++) {
				out.println("Hi: " + i);
				String str = in.readLine();
				System.out.println(str);
			}
			out.println("END");
		} finally {
			System.out.println("Closing...");
			mysocket.close();
		}
	}
}