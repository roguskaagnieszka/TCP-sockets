import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TCPClient {

	private static class Config {
		final String host;
		final int port;
		Config(String host, int port) { this.host = host; this.port = port; }
	}

	public static void main(String[] args) {
		try {
			Config cfg = readConfig("config.txt");
			System.out.println("=========================================");
			System.out.println("   Simple TCP Client");
			System.out.println("   Connected to: " + cfg.host + ":" + cfg.port);
			System.out.println("-----------------------------------------");
			System.out.println(" Available commands:");
			System.out.println("   STATS <text>   → returns statistics (lowercase, uppercase, digits, others)");
			System.out.println("   ANAGRAM <text> → returns an anagram of the text");
			System.out.println("   DROP           → closes the connection");
			System.out.println("=========================================\n");

			try (
					Socket socket = new Socket(cfg.host, cfg.port);
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
					PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
					BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))
			) {
				String line;
				while (true) {
					System.out.print("> ");
					line = stdin.readLine();
					if (line == null) break;

					try {
						out.println(line);
						String response = in.readLine();

						if (response == null) {
							System.out.println("Server closed the connection gracefully.");
							break;
						}

						System.out.println("<< " + response);

						if (line.trim().equalsIgnoreCase("DROP")) {
							try { in.readLine(); }
							catch (SocketException ignored) {}
							System.out.println("Server closed the connection after DROP command.");
							break;
						}

					} catch (SocketTimeoutException ste) {
						System.out.println("No response from server (timeout). The server might be busy or unreachable.");
						break;

					} catch (ConnectException ce) {
						System.out.println("Could not connect to the server. Please check IP and port.");
						break;

					} catch (SocketException se) {
						System.out.println("Connection lost: server is unreachable or connection was reset.");
						break;
					}
				}

			}

		} catch (IOException e) {
			System.err.println("Client error: " + e.getMessage());
		}
	}

	private static Config readConfig(String path) throws IOException {
		Properties p = new Properties();
		try (FileInputStream fis = new FileInputStream(path)) {
			p.load(fis);
		}

		String host = p.getProperty("ip");
		String portStr = p.getProperty("port");

		if (host == null || host.isBlank() || portStr == null || portStr.isBlank()) {
			System.err.println("Error: config.txt must contain both 'ip' and 'port' entries.");
			System.exit(1);
		}

		int port;
		try {
			port = Integer.parseInt(portStr.trim());
			if (port < 1 || port > 65535) throw new NumberFormatException();
		} catch (NumberFormatException e) {
			System.err.println("Error: invalid port number (" + portStr + "). Must be between 1–65535.");
			System.exit(1);
			return null;
		}

		try {
			InetAddress.getByName(host.trim());
		} catch (UnknownHostException e) {
			System.err.println("Error: invalid IP address or hostname: " + host);
			System.exit(1);
		}

		return new Config(host.trim(), port);
	}
}
