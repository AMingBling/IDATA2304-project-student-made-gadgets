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
      node.getActuators().stream()
          .filter(a -> a.getActuatorId().equals(actuatorId))
          .findFirst()
          .ifPresent(a -> {
            a.setOn(on);
            System.out.println("Actuator " + actuatorId + " set to " + on);
          });
      sendCurrentNode();
    }
  }

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
        else if ("CO2".equals(sensorType)) {
          node.addSensor(new CO2Sensor(sensorId, min, max));
          node.addActuator(new Ventilation(sensorId + "_ventilation"));
          node.addActuator(new CO2Supply(sensorId + "_co2_supply"));
        }
        else if ("LIGHT".equals(sensorType)) {
          node.addSensor(new LightSensor(sensorId, min, max));
          node.addActuator(new LampDimming(sensorId + "_lamp_dimming"));
          node.addActuator(new LampBrightning(sensorId + "_lamp_brightning"));
        }  
        System.out.println(
            "Added sensor " + sensorId + " of type " + sensorType + " with actuators.");
        sendCurrentNode();
      }
    } catch (Exception e) {
      System.out.println("Failed to add sensor: " + e.getMessage());
    }
  }

  private void handleRemoveSensor(JsonObject obj) {
    try {
      String sensorId = obj.has("sensorId") ? obj.get("sensorId").getAsString() : null;
      if (sensorId == null) return;
      // Remove sensors with matching id
      node.getSensors().removeIf(s -> sensorId.equals(s.getSensorId()));
      // Remove actuators that were created for the sensor (use prefix matching)
      String prefix = sensorId + "_";
      node.getActuators().removeIf(a -> a.getActuatorId() != null && a.getActuatorId().startsWith(prefix));
      System.out.println("Removed sensor " + sensorId + " and associated actuators");
      sendCurrentNode();
    } catch (Exception e) {
      System.out.println("Failed to remove sensor: " + e.getMessage());
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
        nodeClient.sendCurrentNode();

        // 💡 Keep node alive forever
        while (true) Thread.sleep(1000);

    } catch (Exception e) {
        e.printStackTrace();
    }
}



}