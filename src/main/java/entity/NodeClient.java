package entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import util.SensorType;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;


/**
 * SensorNodes kan bli kjørt i terminalen ved hjelp av kommandoen mvn exec:java
 * "-Dexec.mainClass=entity.SensorNode" "-Dexec.args=<ID> <Lokasjon>" og kobler seg til Serveren
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

    // Simulate periodic sensor readings
//    simulateSensorData();
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
    System.out.println("Node → Server: " + json);
  }

//  private void simulateSensorData() {
//    Random random = new Random();
//
//    try {
//      while (true) {
//        double temp = 20 + random.nextDouble() * 5;
//
//        Sensor sm = new Sensor(
//            "sensor-001",
//            SensorType.TEMPERATURE,
//            nodeId + "-temp",
//            LocalDateTime.now(),
//            temp,
//            "°C"
//        );
//
//        Node nm = new Node(
//            nodeId,
//            location,
//            LocalDateTime.now(),
//            Collections.singletonList(sm)
//        );
//        nm.setMessageType("SENSOR_DATA_FROM_NODE");
//
//        out.println(gson.toJson(nm));
//        System.out.println("Node → Server: " + gson.toJson(nm));
//
//        Thread.sleep(3000);
//      }
//    } catch (InterruptedException e) {
//      System.out.println("entity.Sensor simulation stopped.");
//    }
//  }

  private void listenForCommands() {
    try {
      String incoming;
      while ((incoming = in.readLine()) != null) {
        System.out.println("Command received: " + incoming);
        // Could be parsed further later
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

  // ---------- MAIN ----------

//  public static void main(String[] args) {
//    if (args.length < 2) {
//      System.out.println("Usage: java SensorNode <nodeId> <location>");
//      return;
//    }
//
//    String nodeId = args[0];
//    String location = args[1];
//
//    final String SERVER_IP = "127.0.0.1";
//    final int SERVER_PORT = 5000;
//
//    Gson gson = new GsonBuilder()
//        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
//        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
//        .create();
//
//    try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
//        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
//        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
//
//      System.out.println("SensorNode " + nodeId + " connected to server.");
//
//      // Let server know who we are and wait for ack
//      out.println("SENSOR_NODE_CONNECTED " + nodeId);
//      String serverResponse = in.readLine();
//      if (serverResponse == null) {
//        System.out.println("No response from server after registering. Exiting.");
//        return;
//      }
//      if (serverResponse.equals("NODE_ID_REJECTED")) {
//        System.out.println("Node ID '" + nodeId + "' rejected by server (duplicate). Exiting.");
//        return;
//      }
//      if (!serverResponse.equals("NODE_ID_ACCEPTED")) {
//        System.out.println("Unexpected server response: " + serverResponse + ". Exiting.");
//        return;
//      }
//
//      NodeClient node = new NodeClient(nodeId, location, out, in, gson);
//      node.start();
//
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage: java SensorNode <nodeId> <location>");
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
      Sensor initSensor = new Sensor(nodeId + "-sensor-0", SensorType.TEMPERATURE, "°C", 0.0,
          100.0);
      java.util.List<Sensor> sensors = new java.util.ArrayList<>();
      sensors.add(initSensor);

      Node nodeObj = new Node(nodeId, location, sensors);

      NodeClient nodeClient = new NodeClient(nodeObj, out, in, gson);
      nodeClient.start();

      // Send initial node state to server
      nodeClient.sendCurrentNode();

      System.out.println("Press ENTER to exit.");
      new java.util.Scanner(System.in).nextLine();

      nodeClient.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
