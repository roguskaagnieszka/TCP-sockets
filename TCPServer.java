import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;

public class TCPServer {
	public static final int DEFAULT_PORT = 5000;

	public static void main(String[] args) {
		int port = DEFAULT_PORT;
		if (args.length >= 1) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Invalid port \"" + args[0] + "\". Falling back to " + DEFAULT_PORT);
			}
		}

		printLocalAddresses(port);

		ExecutorService pool = Executors.newCachedThreadPool();
		try (ServerSocket server = new ServerSocket()) {
			server.setReuseAddress(true);
			server.bind(new InetSocketAddress(port));
			System.out.println("Server listening on port " + port + " ...");

			String ip = getLocalIPv4();
			saveServerInfo(ip, port);

			while (true) {
				Socket client = server.accept();
				pool.submit(() -> handleClient(client));
			}
		} catch (BindException be) {
			System.err.println("ERROR: Port " + port + " is already in use. " +
					"Close the app using it, or pick another port.");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			pool.shutdown();
		}
	}

	private static void printLocalAddresses(int port) {
		System.out.println("=== Server network info ===");
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface ni = ifaces.nextElement();
				if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
				Enumeration<InetAddress> addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					InetAddress a = addrs.nextElement();
					if (a instanceof Inet4Address) {
						System.out.printf("Interface %-15s -> %s:%d%n", ni.getName(), a.getHostAddress(), port);
					}
				}
			}
		} catch (SocketException e) {
			System.err.println("Cannot enumerate interfaces: " + e.getMessage());
		}
		System.out.println("===========================\n");
	}

	private static void handleClient(Socket socket) {
		String remote = socket.getRemoteSocketAddress().toString();
		System.out.println("Connection accepted: " + remote);
		try (
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)), true)
		) {
			String line;
			while ((line = in.readLine()) != null) {
				if (line.isEmpty()) continue;

				String response = processCommand(line);
				out.println(response);

				if ("DROP".equalsIgnoreCase(line.trim())) {
					System.out.println("Client requested DROP: " + remote);
					break;
				}
			}
		} catch (IOException e) {
			System.err.println("I/O error with " + remote + ": " + e.getMessage());
		} finally {
			try { socket.close(); } catch (IOException ignored) {}
			System.out.println("Connection closed: " + remote);
		}
	}

	private static String processCommand(String raw) {
		String line = raw.trim();
		String cmd, arg;
		int sp = line.indexOf(' ');
		if (sp == -1) {
			cmd = line;
			arg = "";
		} else {
			cmd = line.substring(0, sp);
			arg = line.substring(sp + 1);
		}

		switch (cmd.toUpperCase(Locale.ROOT)) {
			case "STATS":
				return stats(arg);
			case "ANAGRAM":
				return anagram(arg);
			case "DROP":
				return "Bye!";
			default:
				return "ERROR: Unknown command. Use STATS <text> | ANAGRAM <text> | DROP";
		}
	}

	private static String stats(String text) {
		int lower = 0, upper = 0, digits = 0, other = 0;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (Character.isLowerCase(ch)) lower++;
			else if (Character.isUpperCase(ch)) upper++;
			else if (Character.isDigit(ch)) digits++;
			else other++;
		}
		return String.format("STATS lower=%d upper=%d digits=%d other=%d", lower, upper, digits, other);
	}

	private static String anagram(String text) {
		List<Character> chars = new ArrayList<>();
		for (char c : text.toCharArray()) chars.add(c);
		Collections.shuffle(chars, new Random());
		StringBuilder sb = new StringBuilder(chars.size());
		for (char c : chars) sb.append(c);
		return sb.toString();
	}

	private static String getLocalIPv4() {
		try {
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			while (nets.hasMoreElements()) {
				NetworkInterface ni = nets.nextElement();
				if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
				Enumeration<InetAddress> addrs = ni.getInetAddresses();
				while (addrs.hasMoreElements()) {
					InetAddress a = addrs.nextElement();
					if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
						return a.getHostAddress();
					}
				}
			}
		} catch (SocketException ignored) { }
		return "127.0.0.1";
	}

	private static void saveServerInfo(String ip, int port) {
		File file = new File("config.txt");
		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
			pw.println("ip=" + ip);
			pw.println("port=" + port);
			pw.flush();
			System.out.println("Config file written: " + file.getAbsolutePath());
		} catch (IOException e) {
			System.err.println("Failed to write config.txt: " + e.getMessage());
		}
	}
}
