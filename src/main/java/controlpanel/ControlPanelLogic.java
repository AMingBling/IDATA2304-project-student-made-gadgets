package controlpanel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import entity.sensor.Sensor;
import entity.Node;
import entity.actuator.Actuator;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
 * Control panel logic and state manager.
 *
 * <p>This class encapsulates the logic for a Control Panel. Responsibilities
 * include:
 * <ul>
 *   <li>Maintaining a local cache of known nodes and their sensor/actuator state</li>
 *   <li>Receiving and parsing line-delimited JSON messages from the {@code Server}</li>
 *   <li>Forwarding commands from the UI to the server (ADD_SENSOR, REMOVE_SENSOR,
 *       ACTUATOR_COMMAND, REQUEST_NODE, TARGET_UPDATE)</li>
 *   <li>Spawning local simulated {@code NodeClient} instances for quick testing</li>
 *   <li>Providing a small programmatic API used by {@code ControlPanelUI}</li>
 * </ul>
 *
 * <p>Incoming JSON messages are passed into {@link #handleIncomingJson(String)}
 * (registered as a callback by {@link controlpanel.ControlPanelCommunication}).
 * The class is thread-safe for its public methods where appropriate (internal
 * structures use concurrent maps). Printing of periodic node updates can be
 * toggled using {@link #setShowNodeUpdates(boolean)} to avoid noisy terminals.
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

  // Track nodes recently requested so we only print the first response during the request window
  private final Map<String, CountDownLatch> requestLatches = new ConcurrentHashMap<>();
  private final Set<String> requestPrinted = ConcurrentHashMap.newKeySet();


  /**
   * Create a new ControlPanelLogic instance for a given control panel id.
   *
   * <p>The instance sets up the {@link ControlPanelCommunication} and registers
   * {@link #handleIncomingJson(String)} as the callback for incoming JSON lines.
   *
   * @param controlPanelId identifier used to register this control panel with the server
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
   * Connect to the server using the underlying communication layer.
   *
   * @param ip server IP
   * @param port server TCP port
   * @throws IOException if the connection fails
   */
  public void connect(String ip, int port) throws IOException {
    comm.connect(ip, port);
  }

  /**
   * Close the communication channel and any spawned simulated nodes.
   *
   * <p>This closes the underlying {@link ControlPanelCommunication} and
   * shuts down any simulated {@link NodeClient} instances created via
   * {@link #spawnNode(String, String)}. The method is safe to call multiple
   * times.
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

  /**
   * Disconnect and remove a previously spawned simulated node.
   *
   * @param nodeId id of the spawned node to disconnect
   * @return true if a spawned node or socket was found and removed
   */
  public boolean disconnectSpawnedNode(String nodeId) {
    NodeClient nc = spawnedNodes.remove(nodeId);
    Socket s = spawnedSockets.remove(nodeId);
    if (nc == null && s == null) return false;
    if (nc != null) try { nc.close(); } catch (Exception ignored) {}
    if (s != null) try { s.close(); } catch (Exception ignored) {}
    return true;
  }

  /**
   * Handle a single line of JSON received from the server.
   *
   * <p>Recognized message types are {@code SENSOR_DATA_FROM_NODE},
   * {@code ACTUATOR_STATUS} and {@code ALERT}. For backward compatibility,
   * a payload containing a {@code nodeID} but lacking {@code messageType}
   * will be treated as {@code SENSOR_DATA_FROM_NODE}.
   *
   * @param json the raw line-delimited JSON string received from the server
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

      // Try to extract sensor id from the alert string. Accept both
      // 'sensor=<id>' and 'sensor = <id>' (and variations like 'sensor: <id>').
      String sensorId = null;
      java.util.regex.Pattern p = java.util.regex.Pattern.compile("sensor\\s*(?:=|:)\\s*([^\\s,]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
      java.util.regex.Matcher m = p.matcher(alert);
      if (m.find()) {
        sensorId = m.group(1);
      }

      String key = nodeId + ":" + (sensorId == null ? alert : sensorId);
      // If we've already shown this alert for this sensor, ignore
      if (shownAlerts.contains(key)) return;

      // First time: print alert and remember it
      System.out.println("! ALERT from node " + (nodeId.isEmpty() ? "?" : nodeId) + ": " + alert + " !\n");
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

      // Store location from incoming node snapshot so the UI can show it
      try {
        if (node.getLocation() != null) state.location = node.getLocation();
      } catch (Exception ignored) {}

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
    // If this node was recently requested, only allow printing the first update
    if (state != null && state.nodeId != null && requestLatches.containsKey(state.nodeId)) {
      if (requestPrinted.contains(state.nodeId)) return; // already printed one response for this request
      // mark as printed
      requestPrinted.add(state.nodeId);
      // after printing we'll count down the latch to notify any waiting thread
    }
    System.out.println("\n ---- NODE UPDATE ----");
    System.out.println("Node ID: " + state.nodeId);
    System.out.println(" Sensors:");
    for (Sensor sensor : state.sensors.values()) {
      System.out.printf("  - ID: %s, Type: %s, Value: %.2f %s%n",
          sensor.getSensorId(), sensor.getSensorType(), sensor.getValue(), sensor.getUnit());
    }
    System.out.println(" Actuators:");
    // Print actuators sorted by actuatorId so those tied to the same sensor
    // (e.g. actuator ids starting with "s1_") appear grouped together.
    java.util.List<Actuator> actuators = new java.util.ArrayList<>(state.actuators.values());
    actuators.sort((a, b) -> a.getActuatorId().compareToIgnoreCase(b.getActuatorId()));
    for (Actuator actuator : actuators) {
      System.out.printf("  - ID: %s, Type: %s, State: %s%n",
          actuator.getActuatorId(), actuator.getActuatorType(), actuator.isOn() ? "ON" : "OFF");
    }
    System.out.println("---------------------\n");
  }

  // UI API
  /**
   * Return an unmodifiable view of the cached nodes state map.
   *
   * @return map nodeId -> {@link NodeState}
   */
  public Map<String, NodeState> getNodes() {
    return nodes;
  }

  /**
   * Lookup an actuator in the cached node state.
   *
   * @param nodeId node id
   * @param actuatorId actuator id
   * @return actuator object or {@code null} if not found
   */
  public Actuator getActuator(String nodeId, String actuatorId) {
    NodeState ns = nodes.get(nodeId);
    return ns == null ? null : ns.actuators.get(actuatorId);
  }


  /**
   * Send an actuator command to turn an actuator on or off on a remote node.
   *
   * @param nodeId target node id
   * @param actuatorId actuator id to control
   * @param on true to turn on, false to turn off
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
   * Update the target min/max range for a sensor type on a node.
   *
   * @param nodeId target node id
   * @param sensorType sensor type name (e.g. TEMPERATURE, HUMIDITY)
   * @param min target minimum
   * @param max target maximum
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

  /**
   * Request the current state of a specific node from the server.
   *
   * <p>This sends a {@code REQUEST_NODE} message to the server which will
   * either reply with a cached node JSON or forward the request to the node
   * to fetch a fresh state.
   *
   * @param nodeId the id of the node to request
   */
  public void requestNode(String nodeId) {
    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "REQUEST_NODE");
    obj.addProperty("controlPanelId", controlPanelId);
    obj.addProperty("nodeID", nodeId);
    // Temporarily enable node update printing so the single Request response is shown
    boolean previous = this.showNodeUpdates;
    setShowNodeUpdates(true);
    // Track this node so we only print the first response during the request window
    CountDownLatch latch = new CountDownLatch(1);
    requestPrinted.remove(nodeId);
    requestLatches.put(nodeId, latch);
    comm.sendJson(gson.toJson(obj));
    try {
      // Wait for the first response (up to 3s) so it prints before the menu is shown again
      latch.await(1200, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    } finally {
      requestLatches.remove(nodeId);
      requestPrinted.remove(nodeId);
      setShowNodeUpdates(previous);
    }
  }

  /**
   * Request that a node adds a sensor at runtime. The control panel sends an
   * {@code ADD_SENSOR} message containing the type, id and threshold values.
   *
   * @param nodeId target node id
   * @param sensorType sensor type (TEMPERATURE, LIGHT, HUMIDITY, CO2)
   * @param sensorId unique sensor id for the node
   * @param minThreshold minimum threshold value for alerts
   * @param maxThreshold maximum threshold value for alerts
   */
  public boolean addSensor(String nodeId, String sensorType, String sensorId, double minThreshold, double maxThreshold) {
      // Prevent adding duplicate sensor types for the same node
      NodeState ns = nodes.get(nodeId);
      if (ns != null && ns.sensors != null) {
        for (entity.sensor.Sensor s : ns.sensors.values()) {
          if (s.getSensorType() != null && s.getSensorType().equalsIgnoreCase(sensorType)) {
            System.out.println("Node " + nodeId + " already has a sensor of type " + sensorType + ". Skipping add.");
            return false;
          }
        }
      }

      // Prevent duplicate sensor IDs on the same node
      if (ns != null && ns.sensors != null && ns.sensors.containsKey(sensorId)) {
        System.out.println("Sensor ID '" + sensorId + "' already exists on node " + nodeId + ". Skipping add.");
        return false;
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
      return true;
  }
  

  /**
   * Request the node to remove a sensor (and associated actuators) at runtime.
   */
  /**
   * Request the node to remove a sensor (and associated actuators) at runtime.
   * Returns true if a REMOVE_SENSOR message was actually sent to the server.
   */
  public boolean removeSensor(String nodeId, String sensorId) {
    // Basic validation against cached state: if we don't know the node or sensor,
    // report and skip sending the request.
    NodeState ns = nodes.get(nodeId);
    if (ns == null || ns.sensors == null || !ns.sensors.containsKey(sensorId)) {
      System.out.println("Sensor '" + sensorId + "' not found on node " + nodeId + ". Skipping remove.");
      return false;
    }

    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "REMOVE_SENSOR");
    obj.addProperty("controlPanelId", controlPanelId);
    obj.addProperty("nodeID", nodeId);
    obj.addProperty("sensorId", sensorId);
    comm.sendJson(gson.toJson(obj));
    return true;
  }

  /**
   * Per-node cache containing the known sensors and actuators for a node.
   *
   * <p>The maps are mutable and updated by {@link ControlPanelLogic} when new
   * snapshots arrive from the server.
   */
  public static class NodeState {
    public final String nodeId;
    public String location = "";
    public final Map<String, Sensor> sensors = new HashMap<>();
    public final Map<String, Actuator> actuators = new HashMap<>();

    public NodeState(String nodeId) {
      this.nodeId = nodeId;
    }
  }
}