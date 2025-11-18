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
             System.out.println("[NodeClient] Sent ALERT: " + alert);
           }
           sendCurrentNode();
         } catch (Exception e) {
           System.out.println("[NodeClient] control loop error: " + e.getMessage());
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

  public void sendCurrentNode() {
    if (node == null) {
      return;
    }
    sendNode(node);
  }

  public void sendNode(Node n) {
    if (n == null) {
      throw new IllegalArgumentException("Node cannot be null");
    }
    JsonObject obj = JsonParser.parseString(gson.toJson(n)).getAsJsonObject();
    obj.addProperty("messageType", "SENSOR_DATA_FROM_NODE");
    out.println(obj.toString());
    out.flush();
    System.out.println("Node -> Server: " + obj.toString());
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

    // ...existing code...
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
          String targetType = target.getActuatorType() != null ? target.getActuatorType().toUpperCase() : "";
  
          // Hvis vi slår på en aktuator, slå av konfliktende aktuatorer
          if (on) {
            for (entity.actuator.Actuator a : node.getActuators()) {
              if (a == null) continue;
              if (a.getActuatorId().equals(actuatorId)) continue;
              String t = a.getActuatorType() != null ? a.getActuatorType().toUpperCase() : "";
              if (isConflict(targetType, t)) {
                a.setOn(false);
                System.out.println("Actuator " + a.getActuatorId() + " (type=" + a.getActuatorType() + ") turned OFF due to conflict with " + actuatorId);
              }
            }
          }
  
          target.setOn(on);
          System.out.println("Actuator " + actuatorId + " set to " + on);
        } else {
          System.out.println("Actuator not found: " + actuatorId);
        }
  
        sendCurrentNode();
      }
    }
  
    // Hjelpemetode for konfliktregler (legg til flere par ved behov)
    private boolean isConflict(String typeA, String typeB) {
      if (typeA == null || typeB == null) return false;
      if (typeA.equals(typeB)) return false; // samme type - ikke konflikt her
  
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
  
      // Flere regler kan legges til her
      return false;
    }
  // ...existing code...

  private void handleAddSensor(JsonObject obj) {
    try {
      String sensorType = obj.has("sensorType") ? obj.get("sensorType").getAsString() : null;
      String sensorId = obj.has("sensorId") ? obj.get("sensorId").getAsString() : null;
      double min = obj.has("minThreshold") ? obj.get("minThreshold").getAsDouble() : 0.0;
      double max = obj.has("maxThreshold") ? obj.get("maxThreshold").getAsDouble() : 100.0;
      if (sensorType != null && sensorId != null) {
        if ("TEMPERATURE".equals(sensorType)) {
          node.addSensor(new TemperatureSensor(sensorId, min, max));
          node.addActuator(new Heater(sensorId + "_heater"));
          node.addActuator(new AirCondition(sensorId + "_ac"));
        } else if ("HUMIDITY".equals(sensorType)) {
          node.addSensor(new HumiditySensor(sensorId, min, max));
          node.addActuator(new Humidifier(sensorId + "_humidifier"));
          node.addActuator(new DeHumidifier(sensorId + "_dehumidifier"));
        }
        System.out.println(
            "Added sensor " + sensorId + " of type " + sensorType + " with actuators.");
        sendCurrentNode();
      }
    } catch (Exception e) {
      System.out.println("Failed to add sensor: " + e.getMessage());
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
        System.out.println("Usage: NodeClient <ID> <Location>");
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

        System.out.println("SensorNode " + nodeId + " connected to server.");

        out.println("SENSOR_NODE_CONNECTED " + nodeId);
        String serverResponse = in.readLine();
        if (serverResponse == null || !serverResponse.equals("NODE_ID_ACCEPTED")) {
            System.out.println("Node ID rejected or unexpected response. Exiting.");
            return;
        }

        List<Sensor> sensors = new ArrayList<>();
        List<Actuator> actuators = new ArrayList<>();
        Node nodeObj = new Node(nodeId, location, sensors, actuators);

        NodeClient nodeClient = new NodeClient(nodeObj, out, in, gson);
        nodeClient.start();
        nodeClient.startControlLoop(10000); // 10s tick
        nodeClient.sendCurrentNode();
       

        // 💡 Keep node alive forever
        while (true) Thread.sleep(1000);

    } catch (Exception e) {
        e.printStackTrace();
    }
}

//----------------------------------------




}