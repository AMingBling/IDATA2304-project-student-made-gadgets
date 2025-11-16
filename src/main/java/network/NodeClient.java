package network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import entity.Actuator;
import entity.Node;
import entity.sensor.Sensor;
import entity.sensor.TemperatureSensor;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;


// mvn --% exec:java -Dexec.mainClass=network.NodeClient -Dexec.args="<id> <location>"
// ex: mvn --% exec:java -Dexec.mainClass=network.NodeClient -Dexec.args="01 greenhouse1"
/**
 * NodeClients kan bli kjørt i terminalen ved hjelp av kommandoen mvn exec:java
 * "-DesorNodesxec.mainClass=network.NodeClient" "-Dexec.args=<ID> <Lokasjon>" og kobler seg til Serveren
 */
public class NodeClient {

  private Node node;
  private final PrintWriter out;
  private final BufferedReader in;
  private final Gson gson;
  private Thread listener;


  public NodeClient(Node node, PrintWriter out, BufferedReader in, Gson gson) {
    this.node = node;
    this.out = out;
    this.in = in;
    this.gson = gson;
  }

  // ---------- METHODS ----------

  public void start() {
    // Start thread to listen for messages from server
    listener = new Thread(this::listenForCommands);
    listener.setDaemon(true);
    listener.start();

    startSensorLoop();
  }

  private void startSensorLoop() {
    Thread sensorThread = new Thread(() -> {
      try {
        while (true) {
          Thread.sleep(10000); // Update every 10 seconds
          node.updateAllSensors();
          sendCurrentNode();
        }
      } catch (InterruptedException e) {
        // Thread interrupted, exit gracefully
      }
    });
    sensorThread.setDaemon(true);
    sensorThread.start();
  }

  public void sendCurrentNode() {
    if (node == null) {
      return;
    }
    sendNode(node);
  }

  /**
   * Send any Node object to the server (serialized to JSON).
   */
  public void sendNode(Node n) {
    if (n == null) {
      return;
    }
    String json = (gson != null) ? n.nodeToJson(gson) : n.nodeToJson();
    out.println(json);
    out.flush();
    System.out.println("Node -> Server: " + json);
  }


  private void listenForCommands() {
    try {
      String incoming;
      while ((incoming = in.readLine()) != null) {
        System.out.println("Command received: " + incoming);
        String trimmed = incoming.trim();
        if (trimmed.startsWith("{")) {
          try {
            JsonObject obj = JsonParser.parseString(trimmed).getAsJsonObject();
            String mt = obj.has("messageType") && !obj.get("messageType").isJsonNull()
                ? obj.get("messageType").getAsString()
                : null;

            if ("ACTUATOR_COMMAND".equals(mt)) {
              // apply actuator change locally and send updated node state
              String actuatorId = obj.has("actuatorId") ? obj.get("actuatorId").getAsString() : null;
              String command = obj.has("command") ? obj.get("command").getAsString() : null;
              if (actuatorId != null && command != null) {
                boolean on = "TURN_ON".equals(command);
                node.getActuators().stream()
                    .filter(a -> a.getActuatorId().equals(actuatorId))
                    .findFirst()
                    .ifPresent(a -> {
                      a.setOn(on);
                      System.out.println("Actuator " + actuatorId + " set to " + on);
                    });
                // send updated node state to server so control panels get the update
                sendCurrentNode();
              }
            } else if ("REQUEST_STATE".equals(mt) || "REQUEST_NODE".equals(mt)) {
              // send current node state immediately
              sendCurrentNode();
            }
          } catch (Exception e) {
            System.out.println("Failed to parse command JSON: " + e.getMessage());
          }
        }
      }
    } catch (IOException e) {
      System.out.println("Lost connection to server.");
    }
  }

  public void close() {
    try {
      if (in != null) {
        in.close();
      }
    } catch (IOException ignored) {
    }
    if (out != null) {
      out.close();
    }
    if (listener != null && listener.isAlive()) {
      listener.interrupt();
    }
  }



  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("mvn exec:java\r\n" + //
                " * \"-DesorNodesxec.mainClass=network.NodeClient\" \"-Dexec.args=<ID> <Lokasjon>\"");
      return;
    }

    String nodeId = args[0];
    String location = args[1];

    final String SERVER_IP = "127.0.0.1";
    final int SERVER_PORT = 5000;

    Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class,
            (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                new JsonPrimitive(src.toString()))
        .registerTypeAdapter(LocalDateTime.class,
            (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                LocalDateTime.parse(json.getAsString()))
        .create();

    try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

      System.out.println("SensorNode " + nodeId + " connected to server.");

      // Register node id with server
      out.println("SENSOR_NODE_CONNECTED " + nodeId);
      String serverResponse = in.readLine();
      if (serverResponse == null) {
        System.out.println("No response from server after registering. Exiting.");
        return;
      }
      if (serverResponse.equals("NODE_ID_REJECTED")) {
        System.out.println("Node ID '" + nodeId + "' rejected by server (duplicate). Exiting.");
        return;
      }
      if (!serverResponse.equals("NODE_ID_ACCEPTED")) {
        System.out.println("Unexpected server response: " + serverResponse + ". Exiting.");
        return;
      }

      // Create a minimal initial sensor (Node requires at least one sensor)
      Sensor initSensor = new TemperatureSensor("1", 20.0,
          26.0);
      java.util.List<Sensor> sensors = new ArrayList<>();
      sensors.add(initSensor);

      Actuator initActuator = new Actuator("1", "FAN");
      java.util.List<Actuator> actuators = new ArrayList<>();
      actuators.add(initActuator);

      Node nodeObj = new Node(nodeId, location, sensors, actuators);

      NodeClient nodeClient = new NodeClient(nodeObj, out, in, gson);
      nodeClient.start();

      // Send initial node state to server
      nodeClient.sendCurrentNode();

      System.out.println("Press ENTER to exit.");
      try (java.util.Scanner sc = new java.util.Scanner(System.in)) {
        sc.nextLine();
      }

      nodeClient.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
