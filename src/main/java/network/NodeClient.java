package network;

import com.google.gson.*;
import entity.Node;
import entity.actuator.*;
import entity.sensor.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Client class for a Node that connects to the server, sends its state, and listens for commands.
 *
 * <p>This class represents a sensor/actuator node in the system. It is responsible for:
 * <ul>
 *   <li>establishing and maintaining a TCP connection to the {@code Server},</li>
 *   <li>serializing and sending the {@link entity.Node} state as JSON,</li>
 *   <li>listening for and handling incoming JSON commands such as
 *       {@code ACTUATOR_COMMAND}, {@code REQUEST_STATE}, {@code ADD_SENSOR}, and {@code REMOVE_SENSOR},</li>
 *   <li>providing a small runtime loop for keeping the client alive.</li>
 * </ul>
 *
 * <p>When executed as a standalone program, the {@link #main(String[])} method
 * connects to a server on {@code 127.0.0.1:5000}, registers the node id and
 * location, and begins sending state updates.
 */
public class NodeClient {

  private Node node;
  private final PrintWriter out;
  private final BufferedReader in;
  private final Gson gson;
  private Thread listener;

  private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd HH:mm:ss");

  private static void log(String role, String fmt, Object... args) {
    String msg = String.format(fmt, args);
    System.out.printf("\n[%s] %s: %s%n", LOG_TIME.format(LocalDateTime.now()), role, msg);
  }


  /**
   * Construct a NodeClient instance.
   *
   * @param node the {@link entity.Node} model representing sensors and actuators
   * @param out  the {@link PrintWriter} used to send messages to the server
   * @param in   the {@link BufferedReader} used to receive messages from the server
   * @param gson the {@link Gson} instance used for JSON serialization/deserialization
   */
  public NodeClient(Node node, PrintWriter out, BufferedReader in, Gson gson) {
    this.node = node;
    this.out = out;
    this.in = in;
    this.gson = gson;

  }

  /**
   * Start the client's background listener thread.
   *
   * <p>This will spawn a daemon thread that listens for incoming JSON commands
   * from the server and reacts to them. The method returns immediately.
   */
  public void start() {
    listener = new Thread(this::listenForCommands);
    listener.setDaemon(true);
    listener.start();
  }

  public void startControlLoop(long tickMillis) {
    Thread t = new Thread(() -> {
      try {
        while (true) {
          try {
            node.updateAllSensors();
            String alert = node.applyActuatorEffects(); // må finnes i Node
            if (alert != null) {
              JsonObject al = new JsonObject();
              al.addProperty("messageType", "ALERT");
              al.addProperty("nodeID", node.getNodeID());
              al.addProperty("alert", alert);
              out.println(al.toString());
              out.flush();
              log("NodeClient", "Sent ALERT: %s", alert);
            }
            sendCurrentNode();
          } catch (Exception e) {
            log("NodeClient", "Control loop error: %s", e.getMessage());
          }
          Thread.sleep(tickMillis);
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }, "NodeClient-ControlLoop");
    t.setDaemon(true);
    t.start();
  }


  /**
   * Send the current {@link entity.Node} state to the server.
   *
   * <p>If the internal node model is {@code null} this method does nothing.
   */
  public void sendCurrentNode() {
    if (node == null) {
      return;
    }
    sendNode(node);
  }

  /**
   * Serialize and send the provided {@link entity.Node} to the server as JSON.
   *
   * @param n the node to send; must not be {@code null}
   * @throws IllegalArgumentException when {@code n} is {@code null}
   */
  public void sendNode(Node n) {
    if (n == null) {
      throw new IllegalArgumentException("Node cannot be null");
    }
    JsonObject obj = JsonParser.parseString(gson.toJson(n)).getAsJsonObject();
    obj.addProperty("messageType", "SENSOR_DATA_FROM_NODE");
    out.println(obj.toString());
    out.flush();
    System.out.println("\n Node -> Server: " + obj.toString());
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
              handleActuatorCommand(obj);
            } else if ("REQUEST_STATE".equals(mt) || "REQUEST_NODE".equals(mt)) {
              node.updateAllSensors();
              sendCurrentNode();
            } else if ("ADD_SENSOR".equals(mt)) {
              handleAddSensor(obj);
            } else if ("REMOVE_SENSOR".equals(mt)) {
              handleRemoveSensor(obj);
            }
          } catch (Exception e) {
            System.out.println("Error processing command: " + e.getMessage());
          }
        }
      }
    } catch (IOException e) {
      System.out.println("Error reading from server: " + e.getMessage());
    }
  }

  private void handleActuatorCommand(JsonObject obj) {
    String actuatorId = obj.has("actuatorId") ? obj.get("actuatorId").getAsString() : null;
    String command = obj.has("command") ? obj.get("command").getAsString() : null;
    if (actuatorId != null && command != null) {
      boolean on = "TURN_ON".equals(command);

      // Finn target-aktuator først
      entity.actuator.Actuator target = node.getActuators().stream()
          .filter(a -> a.getActuatorId().equals(actuatorId))
          .findFirst()
          .orElse(null);

      if (target != null) {
        String targetType =
            target.getActuatorType() != null ? target.getActuatorType().toUpperCase() : "";

        // Hvis vi slår på en aktuator, slå av konfliktende aktuatorer
        if (on) {
          for (entity.actuator.Actuator a : node.getActuators()) {
            if (a == null) {
              continue;
            }
            if (a.getActuatorId().equals(actuatorId)) {
              continue;
            }
            String t = a.getActuatorType() != null ? a.getActuatorType().toUpperCase() : "";
            if (isConflict(targetType, t)) {
              a.setOn(false);
              log("NodeClient", "Actuator %s (type=%s) turned OFF due to conflict with %s",
                  a.getActuatorId(), a.getActuatorType(), actuatorId);
            }
          }

        }

        target.setOn(on);
        log("NodeClient", "Actuator %s set to %s", actuatorId, on);
      } else {
        log("NodeClient", "Actuator not found: %s", actuatorId);
      }

      sendCurrentNode();
    }
  }

  // Hjelpemetode for konfliktregler (legg til flere par ved behov)
  private boolean isConflict(String typeA, String typeB) {
    if (typeA == null || typeB == null) {
      return false;
    }
    if (typeA.equals(typeB)) {
      return false; // samme type - ikke konflikt her
    }

    // Heater vs AirCondition
    if ((typeA.equals("HEATER") && (typeB.equals("AIRCON") || typeB.equals("AIRCONDITION")))
        || (typeB.equals("HEATER") && (typeA.equals("AIRCON") || typeA.equals("AIRCONDITION")))) {
      return true;
    }

    // Humidifier vs DeHumidifier
    if ((typeA.equals("HUMIDIFIER") && typeB.equals("DEHUMIDIFIER"))
        || (typeB.equals("HUMIDIFIER") && typeA.equals("DEHUMIDIFIER"))) {
      return true;
    }
    // add lamp conflict: Brightening vs Dimming
    if ((typeA.equals("BRIGHT") && typeB.equals("DIM")) || (typeB.equals("BRIGHT") && typeA.equals(
        "DIM"))) {
      return true;
    }

    // add humidity conflicts if not present...
    if ((typeA.equals("HUMIDIFIER") && typeB.equals("DEHUMIDIFIER")) || (typeB.equals("HUMIDIFIER")
        && typeA.equals("DEHUMIDIFIER"))) {
      return true;
    }
    if ((typeA.contains("BRIGHT") && typeB.contains("DIM")) || (typeB.contains("BRIGHT")
        && typeA.contains("DIM"))) {
      return true;
    }

    return false;
  }


  private void handleRemoveSensor(JsonObject obj) {
    try {
      String sensorId = obj.has("sensorId") ? obj.get("sensorId").getAsString() : null;
      if (sensorId == null) {
        return;
      }
      // Remove sensors with matching id
      node.getSensors().removeIf(s -> sensorId.equals(s.getSensorId()));
      // Remove actuators that were created for the sensor (use prefix matching)
      String prefix = sensorId + "_";

      node.getActuators()
          .removeIf(a -> a.getActuatorId() != null && a.getActuatorId().startsWith(prefix));
      log("NodeClient", "Removed sensor %s and associated actuators", sensorId);
      sendCurrentNode();
    } catch (Exception e) {
      System.out.println("Failed to remove sensor: " + e.getMessage());
    }
  }

  private void handleAddSensor(JsonObject obj) {
    try {
      String sensorType = obj.has("sensorType") ? obj.get("sensorType").getAsString() : null;
      String sensorId = obj.has("sensorId") ? obj.get("sensorId").getAsString() : null;
      double min = obj.has("minThreshold") ? obj.get("minThreshold").getAsDouble() : 0.0;
      double max = obj.has("maxThreshold") ? obj.get("maxThreshold").getAsDouble() : 100.0;

      if (sensorType == null || sensorId == null) {
        return;
      }

      if ("TEMPERATURE".equalsIgnoreCase(sensorType)) {
        node.addSensor(new TemperatureSensor(sensorId, min, max));
        node.addActuator(new Heater(sensorId + "_heater"));
        node.addActuator(new AirCondition(sensorId + "_ac"));

      } else if ("HUMIDITY".equalsIgnoreCase(sensorType)) {
        node.addSensor(new HumiditySensor(sensorId, min, max));
        node.addActuator(new Humidifier(sensorId + "_humidifier"));
        node.addActuator(new DeHumidifier(sensorId + "_dehumidifier"));

      } else if ("CO2".equalsIgnoreCase(sensorType)) {
        node.addSensor(new CO2Sensor(sensorId, min, max));
        node.addActuator(new Ventilation(sensorId + "_ventilation"));
        node.addActuator(new CO2Supply(sensorId + "_co2_supply"));

      } else if ("LIGHT".equalsIgnoreCase(sensorType) || "LUMINANCE".equalsIgnoreCase(sensorType)) {
        node.addSensor(new LightSensor(sensorId, min, max));
        node.addActuator(new LampDimming(sensorId + "_lamp_dimming"));
        node.addActuator(new LampBrightning(sensorId + "_lamp_brightning"));

      } else {
        System.out.println("Unsupported sensor type: " + sensorType);
        return;
      }

      System.out.println("Added sensor " + sensorId + " of type " + sensorType + " with actuators.");
      sendCurrentNode();

    } catch (Exception e) {
      System.out.println("Failed to add sensor: " + e.getMessage());
    }
  }

  /**
   * Close the client's IO resources and stop the listener thread.
   */
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

  /**
   * Command line entry point for starting a standalone NodeClient.
   *
   * <p>Usage: {@code NodeClient <ID> <Location>}.
   *
   * @param args program arguments: node id and location
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      log("NodeClient", "Usage: NodeClient <ID> <Location>");
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

    try {
      Socket socket = new Socket(SERVER_IP, SERVER_PORT);
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      log("NodeClient", "Node %s connected to server", nodeId);

      out.println("SENSOR_NODE_CONNECTED " + nodeId);
      String serverResponse = in.readLine();
      if (serverResponse == null || !serverResponse.equals("NODE_ID_ACCEPTED")) {
        log("NodeClient", "Node ID rejected or unexpected response. Exiting.");
        return;
      }

      List<Sensor> sensors = new ArrayList<>();
      List<Actuator> actuators = new ArrayList<>();
      Node nodeObj = new Node(nodeId, location, sensors, actuators);

      NodeClient nodeClient = new NodeClient(nodeObj, out, in, gson);
      nodeClient.start();
      nodeClient.startControlLoop(3000); // 5s tick
      nodeClient.sendCurrentNode();

      // Keep node alive forever
      while (true) {
        Thread.sleep(1000);
      }

    } catch (Exception e) {
        System.err.println("[NC] Failed to connect to server: " + e.getMessage());
    }
  }

}