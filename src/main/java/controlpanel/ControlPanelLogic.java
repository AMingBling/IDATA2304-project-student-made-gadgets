package controlpanel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import entity.sensor.Sensor;
import entity.Node;
import entity.actuator.Actuator;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import network.NodeClient;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Logikk-laget: behandler JSON, holder state og tilbyr API mot UI. lagt til controlpannelId
 */
public class ControlPanelLogic {

  private final String controlPanelId;
  private final Gson gson = new Gson();
  private final ControlPanelCommunication comm;
  private final Map<String, NodeState> nodes = new ConcurrentHashMap<>();
  // spawned simulated nodes created by this control panel logic (nodeId -> NodeClient)
  private final Map<String, NodeClient> spawnedNodes = new ConcurrentHashMap<>();
  private final Map<String, Socket> spawnedSockets = new ConcurrentHashMap<>();
  // When true, incoming node updates are printed to the ControlPanel terminal.
  // Default false to avoid periodic output in the control panel UI.
  private volatile boolean showNodeUpdates = false;
  // Track alerts already shown in this ControlPanel so each sensor alert is printed only once.
  private final Set<String> shownAlerts = ConcurrentHashMap.newKeySet();


  /**
   * Oppretter ControlPanelLogic og initialiserer kommunikasjon. Kommunikasjonen vil bruke
   * denne.handleIncomingJson som callback.
   */
  public ControlPanelLogic(String controlPanelId) {
    this.controlPanelId = controlPanelId;
    this.comm = new ControlPanelCommunication(this::handleIncomingJson, gson, controlPanelId);
  }

  /**
   * Enable or disable printing incoming node state updates to the ControlPanel terminal.
   * By default printing is disabled to avoid periodic noise in the ControlPanel UI.
   *
   * @param show true to enable printing of node updates, false to silence them
   */
  public void setShowNodeUpdates(boolean show) {
    this.showNodeUpdates = show;
  }

  /**
   * Åpner forbindelse til serveren gjennom kommunikasjonslaget.
   *
   * @param ip   serverens IP-adresse
   * @param port serverens port
   * @throws IOException hvis tilkobling feiler
   */
  public void connect(String ip, int port) throws IOException {
    comm.connect(ip, port);
  }

  /**
   * closing communication
   */
  public void close() {
    comm.close();
    // close any spawned simulated nodes
    for (NodeClient nc : spawnedNodes.values()) {
      try { nc.close(); } catch (Exception ignored) {}
    }
    for (Socket s : spawnedSockets.values()) {
      try { s.close(); } catch (Exception ignored) {}
    }
  }

  /**
   * Spawn a simulated NodeClient that connects to the same server the control panel is connected to.
   * UI should call this method rather than performing networking itself.
   * Returns true if the node was spawned and accepted by server.
   */
  public boolean spawnNode(String nodeId, String location) {
    if (nodeId == null || nodeId.isBlank()) return false;
    if (spawnedNodes.containsKey(nodeId)) return false;
    if (!comm.isConnected()) {
      System.out.println("Control panel not connected to server. Call connect() first.");
      return false;
    }

    String ip = comm.getConnectedIp();
    int port = comm.getConnectedPort();
    if (ip == null || port <= 0) {
      System.out.println("Control panel has no server info.");
      return false;
    }

    try {
      Socket socket = new Socket(ip, port);
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      // Prepare Gson with LocalDateTime adapters matching node client
      com.google.gson.Gson gson = new GsonBuilder()
          .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
          .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) -> LocalDateTime.parse(json.getAsString()))
          .create();

      out.println("SENSOR_NODE_CONNECTED " + nodeId);
      String serverResponse = in.readLine();
      if (serverResponse == null || !serverResponse.equals("NODE_ID_ACCEPTED")) {
        try { socket.close(); } catch (Exception ignored) {}
        System.out.println("Spawn node rejected by server: " + serverResponse);
        return false;
      }

      List<entity.sensor.Sensor> sensors = new ArrayList<>();
      List<entity.actuator.Actuator> actuators = new ArrayList<>();
      entity.Node nodeObj = new entity.Node(nodeId, location, sensors, actuators);
      NodeClient nodeClient = new NodeClient(nodeObj, out, in, gson);
      nodeClient.start();
      nodeClient.sendCurrentNode();
      spawnedNodes.put(nodeId, nodeClient);
      spawnedSockets.put(nodeId, socket);
      System.out.println("Spawned simulated node: " + nodeId);
      return true;
    } catch (Exception e) {
      System.out.println("Failed to spawn node: " + e.getMessage());
      return false;
    }
  }

  public boolean disconnectSpawnedNode(String nodeId) {
    NodeClient nc = spawnedNodes.remove(nodeId);
    Socket s = spawnedSockets.remove(nodeId);
    if (nc == null && s == null) return false;
    if (nc != null) try { nc.close(); } catch (Exception ignored) {}
    if (s != null) try { s.close(); } catch (Exception ignored) {}
    return true;
  }

  /**
   * handling incoming json
   */
  public void handleIncomingJson(String json) {
    if (json == null || !json.startsWith("{")) {
      System.out.println("[CP-Logic] Non-JSON ignored: " + json);
      return;
    }
    JsonObject obj;
    try {
      obj = gson.fromJson(json, JsonObject.class);
    } catch (JsonSyntaxException e) {
      System.out.println("[CP-Logic] malformed JSON: " + json);
      return;
    }
    
    if (!obj.has("messageType")) {
      // Some server responses (cached node JSON) may not include a messageType.
      // If the payload contains a nodeID we treat it as SENSOR_DATA_FROM_NODE for backward compatibility.
      if (obj.has("nodeID") && !obj.get("nodeID").isJsonNull()) {
        updateNodeState(json);
        return;
      }
      System.out.println("[CP-Logic] Missing messageType in JSON: " + json);
      return;
    }

    String type = obj.get("messageType").getAsString();
    switch (type) {
      case "SENSOR_DATA_FROM_NODE" -> updateNodeState(json);
      case "ACTUATOR_STATUS" -> processActuatorStatus(json);
      case "ALERT" -> handleAlert(json);
      default -> System.out.println("[CP-Logic] Unknown type: " + type);
    }
  }

  /**
   * Handle an incoming ALERT JSON message from a node forwarded by the server.
   * Ensures that the same sensor alert is only printed once to this ControlPanel.
   *
   * Expected JSON shape: { messageType: "ALERT", nodeID: "<node>", alert: "... sensor=<sensorId> ..." }
   */
  private void handleAlert(String json) {
    try {
      JsonObject obj = gson.fromJson(json, JsonObject.class);
      String nodeId = obj.has("nodeID") && !obj.get("nodeID").isJsonNull() ? obj.get("nodeID").getAsString() : "";
      String alert = obj.has("alert") && !obj.get("alert").isJsonNull() ? obj.get("alert").getAsString() : null;
      if (alert == null) return;

      // Try to extract sensor id from the alert string: look for 'sensor=<id>'
      String sensorId = null;
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("sensor=([^\\s,]+)").matcher(alert);
      if (m.find()) sensorId = m.group(1);

      String key = nodeId + ":" + (sensorId == null ? alert : sensorId);
      // If we've already shown this alert for this sensor, ignore
      if (shownAlerts.contains(key)) return;

      // First time: print alert and remember it
      System.out.println("*** ALERT from node " + (nodeId.isEmpty() ? "?" : nodeId) + ": " + alert);
      shownAlerts.add(key);
    } catch (Exception e) {
      System.out.println("[CP-Logic] Failed to process ALERT: " + e.getMessage());
    }
  }

  /**
   * parsing nodemessage with sesnoreadings and updates intern state
   */
  private void updateNodeState(String json) {
    try {
      Node node = Node.nodeFromJson(json);
      if (node == null) return;

      NodeState state = nodes.computeIfAbsent(node.getNodeID(), NodeState::new);

      // Replace existing state for this node with the incoming snapshot.
      state.sensors.clear();
      state.actuators.clear();
      if (node.getSensors() != null) {
        for (Sensor sensor : node.getSensors()) {
          state.sensors.put(sensor.getSensorId(), sensor);
        }
      }
      if (node.getActuators() != null) {
        for (Actuator actuator : node.getActuators()) {
          state.actuators.put(actuator.getActuatorId(), actuator);
        }
      }

      printNodeState(state);

    } catch (Exception e) {
      System.out.println("[CP-Logic] Error updating Node state: " + e.getMessage());
    }
  }

  /**
   * parser nodemessage that reportsacutator status and updates intern acutator state
   */
  private void processActuatorStatus(String json) {
    try {
      Node node = Node.nodeFromJson(json);
      if (node == null) return;

      NodeState state = nodes.computeIfAbsent(node.getNodeID(), NodeState::new);

      for (Actuator actuator : node.getActuators()) {
//        try {
//          Actuator a = new Actuator(sm.getSensorId(), util.SensorType.valueOf(sm.getType()), 0, 0);
//          a.setOn(sm.getValue() == 1);
//          state.actuators.put(a.getActuatorId(), a);
//        } catch (IllegalArgumentException ex) {
//          System.out.println("[CP-Logic] Unknown sensor type: " + sm.getType());
//        }
        state.actuators.put(actuator.getActuatorId(), actuator);
      }
    } catch (Exception e) {
      System.out.println("[CP-Logic] Error processing Actuator status: " + e.getMessage());
    }
  }

  private void printNodeState(NodeState state) {
    if (!showNodeUpdates) return;
    System.out.println("\n ---- NODE UPDATE ----");
    System.out.println("Node ID: " + state.nodeId);
    System.out.println(" Sensors:");
    for (Sensor sensor : state.sensors.values()) {
      System.out.printf("  - ID: %s, Type: %s, Value: %.2f %s%n",
          sensor.getSensorId(), sensor.getSensorType(), sensor.getValue(), sensor.getUnit());
    }
    System.out.println(" Actuators:");
    for (Actuator actuator : state.actuators.values()) {
      System.out.printf("  - ID: %s, Type: %s, State: %s%n",
          actuator.getActuatorId(), actuator.getActuatorType(), actuator.isOn() ? "ON" : "OFF");
    }
    System.out.println("---------------------\n");
  }

  // UI API
  public Map<String, NodeState> getNodes() {
    return nodes;
  }

  public Actuator getActuator(String nodeId, String actuatorId) {
    NodeState ns = nodes.get(nodeId);
    return ns == null ? null : ns.actuators.get(actuatorId);
  }


  /**
   * sending a commando to change acutators on/off
   */
  public void setActuatorState(String nodeId, String actuatorId, boolean on) {
    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "ACTUATOR_COMMAND");
    obj.addProperty("controlPanelId", controlPanelId); // <-- legg til origin
    obj.addProperty("nodeID", nodeId);
    obj.addProperty("actuatorId", actuatorId);
    obj.addProperty("command", on ? "TURN_ON" : "TURN_OFF");
    comm.sendJson(gson.toJson(obj));
  }

  /**
   * update min/max for a sensortype on a node
   */
  public void updateTargetRange(String nodeId, String sensorType, double min, double max) {
    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "TARGET_UPDATE");
    obj.addProperty("nodeID", nodeId);
    obj.addProperty("sensorType", sensorType);
    obj.addProperty("targetMin", min);
    obj.addProperty("targetMax", max);
    comm.sendJson(gson.toJson(obj));
  }

  // ---------- ControlPanel actions ----------
  // Subscriptions removed: control panels no longer subscribe/unsubscribe to nodes.

  public void requestNode(String nodeId) {
    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "REQUEST_NODE");
    obj.addProperty("controlPanelId", controlPanelId);
    obj.addProperty("nodeID", nodeId);
    // Temporarily enable node update printing so the single Request response is shown
    boolean previous = this.showNodeUpdates;
    setShowNodeUpdates(true);
    comm.sendJson(gson.toJson(obj));
    // Restore previous verbosity after a short delay (3 seconds)
    new Thread(() -> {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
      }
      setShowNodeUpdates(previous);
    }, "cp-request-verbosity-restorer").start();
  }

  /**
   * Request the node to add a sensor at runtime.
   * Fields: sensorType (TEMPERATURE|LIGHT|HUMIDITY|CO2), sensorId, minThreshold, maxThreshold
   */
  public void addSensor(String nodeId, String sensorType, String sensorId, double minThreshold, double maxThreshold) {
      // Prevent adding duplicate sensor types for the same node
      NodeState ns = nodes.get(nodeId);
      if (ns != null && ns.sensors != null) {
        for (entity.sensor.Sensor s : ns.sensors.values()) {
          if (s.getSensorType() != null && s.getSensorType().equalsIgnoreCase(sensorType)) {
            System.out.println("Node " + nodeId + " already has a sensor of type " + sensorType + ". Skipping add.");
            return;
          }
        }
      }

      // Prevent duplicate sensor IDs on the same node
      if (ns != null && ns.sensors != null && ns.sensors.containsKey(sensorId)) {
        System.out.println("Sensor ID '" + sensorId + "' already exists on node " + nodeId + ". Skipping add.");
        return;
      }

      JsonObject obj = new JsonObject();
      obj.addProperty("messageType", "ADD_SENSOR");
      obj.addProperty("controlPanelId", controlPanelId);
      obj.addProperty("nodeID", nodeId);
      obj.addProperty("sensorType", sensorType);
      obj.addProperty("sensorId", sensorId);
      obj.addProperty("minThreshold", minThreshold);
      obj.addProperty("maxThreshold", maxThreshold);
      comm.sendJson(gson.toJson(obj));
  }

  /**
   * Request the node to remove a sensor (and associated actuators) at runtime.
   */
  public void removeSensor(String nodeId, String sensorId) {
    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "REMOVE_SENSOR");
    obj.addProperty("controlPanelId", controlPanelId);
    obj.addProperty("nodeID", nodeId);
    obj.addProperty("sensorId", sensorId);
    comm.sendJson(gson.toJson(obj));
  }

  /**
   * Request the node to add an actuator at runtime.
   */
  // addActuator removed: actuators are created automatically on the node when a sensor is added

  /**
   * holder for state per node
   */
  public static class NodeState {
    public final String nodeId;
    public final Map<String, Sensor> sensors = new HashMap<>();
    public final Map<String, Actuator> actuators = new HashMap<>();

    public NodeState(String nodeId) {
      this.nodeId = nodeId;
    }
  }
}