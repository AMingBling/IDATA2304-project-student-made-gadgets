import controlpanel.ControlPanelLogic;
import entity.Actuator;
import entity.Node;
import entity.sensor.Sensor;
import entity.sensor.TemperatureSensor;
import network.NodeClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main launcher that starts Server, a NodeClient and a ControlPanel in one JVM.
 * Use for local testing only.
 */
public class MainApp {

	public static void main(String[] args) throws Exception {
		// Start server in background thread by invoking its main method
		Thread serverThread = new Thread(() -> {
			try {
				network.Server.main(new String[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, "server-thread");
		serverThread.setDaemon(true);
		serverThread.start();

		// Wait briefly for server to bind
		Thread.sleep(500);

		final String SERVER_IP = "127.0.0.1";
		final int SERVER_PORT = 5000;

		// Create Gson instance used by NodeClient
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(LocalDateTime.class,
						(JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
								new JsonPrimitive(src.toString()))
				.registerTypeAdapter(LocalDateTime.class,
						(JsonDeserializer<LocalDateTime>) (json, type, context) ->
								LocalDateTime.parse(json.getAsString()))
				.create();

		// Start a node client programmatically
		String nodeId = "01";
		String location = "greenhouse1";

		Socket nodeSocket = new Socket(SERVER_IP, SERVER_PORT);
		PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);
		BufferedReader nodeIn = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));

		// Register node id with server (plain-text protocol)
		nodeOut.println("SENSOR_NODE_CONNECTED " + nodeId);
		String serverResponse = nodeIn.readLine();
		if (serverResponse == null || serverResponse.equals("NODE_ID_REJECTED")) {
			System.err.println("Failed to register node with server: " + serverResponse);
		}

		// create sensors and actuators
		Sensor initSensor = new TemperatureSensor("1", 20.0, 26.0);
		List<Sensor> sensors = new ArrayList<>();
		sensors.add(initSensor);
		List<Actuator> actuators = new ArrayList<>();
		actuators.add(new Actuator("1", "FAN"));

		Node nodeObj = new Node(nodeId, location, sensors, actuators);

		NodeClient nc = new NodeClient(nodeObj, nodeOut, nodeIn, gson);
		nc.start();
		nc.sendCurrentNode();

		// Start control panel logic and interactive CLI in this process
		ControlPanelLogic cpLogic = new ControlPanelLogic("cp1");
		cpLogic.connect(SERVER_IP, SERVER_PORT);
		System.out.println("Control Panel cp1 connected to server at " + SERVER_IP + ":" + SERVER_PORT);

		try (Scanner sc = new Scanner(System.in)) {
			System.out.println("Enter commands: subscribe <nodeId> | unsubscribe <nodeId> | request <nodeId> | set <nodeId> <actuatorId> <on|off> | exit");
			while (true) {
				System.out.print("> ");
				String line = sc.nextLine();
				if (line == null) break;
				String[] parts = line.trim().split("\\s+");
				if (parts.length == 0) continue;
				String cmd = parts[0].toLowerCase();
				switch (cmd) {
					case "subscribe" -> {
						if (parts.length >= 2) cpLogic.subscribe(parts[1]); else System.out.println("Usage: subscribe <nodeId>");
					}
					case "unsubscribe" -> {
						if (parts.length >= 2) cpLogic.unsubscribe(parts[1]); else System.out.println("Usage: unsubscribe <nodeId>");
					}
					case "request" -> {
						if (parts.length >= 2) cpLogic.requestNode(parts[1]); else System.out.println("Usage: request <nodeId>");
					}
					case "set" -> {
						if (parts.length >= 4) {
							String nid = parts[1];
							String aid = parts[2];
							boolean on = parts[3].equalsIgnoreCase("on") || parts[3].equalsIgnoreCase("true");
							cpLogic.setActuatorState(nid, aid, on);
						} else {
							System.out.println("Usage: set <nodeId> <actuatorId> <on|off>");
						}
					}
					case "exit" -> {
						System.out.println("Shutting down...");
						cpLogic.close();
						nc.close();
						nodeSocket.close();
						return;
					}
					default -> System.out.println("Unknown command: " + cmd);
				}
			}
		}
	}

}

