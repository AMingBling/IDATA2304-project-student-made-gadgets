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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logikk-laget: behandler JSON, holder state og tilbyr API mot UI. lagt til controlpannelId
 */
public class ControlPanelLogic {

  private final String controlPanelId;
  private final Gson gson = new Gson();
  private final ControlPanelCommunication comm;
  private final Map<String, NodeState> nodes = new ConcurrentHashMap<>();


  /**
   * Oppretter ControlPanelLogic og initialiserer kommunikasjon. Kommunikasjonen vil bruke
   * denne.handleIncomingJson som callback.
   */
  public ControlPanelLogic(String controlPanelId) {
    this.controlPanelId = controlPanelId;
    this.comm = new ControlPanelCommunication(this::handleIncomingJson, gson, controlPanelId);
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
    JsonElement mt = obj.get("messageType");
    if (mt == null || mt.isJsonNull()) {
      System.out.println("[CP-Logic] Missing messageType: " + json);
      return;
    }
    String type = mt.getAsString();
    switch (type) {
      case "SENSOR_DATA_FROM_NODE" -> processNodeUpdate(json);
      case "ACTUATOR_STATUS" -> processActuatorStatus(json);
      default -> System.out.println("[CP-Logic] Unknown type: " + type);
    }
  }

  /**
   * parsing nodemessage with sesnoreadings and updates intern state
   */
  private void processNodeUpdate(String json) {
    System.out.println("[CP-Logic] Raw node JSON: " + json);
    try {
      Node node = Node.nodeFromJson(json);
      if (node == null) return;

      NodeState state = nodes.computeIfAbsent(node.getNodeID(), NodeState::new);

      for (Sensor sensor : node.getSensors()) {
        state.sensors.put(sensor.getSensorId(), sensor);
      }

      for (Actuator actuator : node.getActuators()) {
        state.actuators.put(actuator.getActuatorId(), actuator);
      }
      // Console-friendly summary for interactive testing
      try {
        StringBuilder sb = new StringBuilder();
        sb.append("[CP-Logic] Node update -> id=").append(node.getNodeID())
          .append(" location=").append(node.getLocation())
          .append(" sensors=").append(state.sensors.size())
          .append(" actuators=").append(state.actuators.size())
          .append("\n");
        for (Sensor s : state.sensors.values()) {
          sb.append("  - sensor ").append(s.getSensorId()).append(" (type=")
            .append(s.getSensorType()).append(") value=").append(s.getValue()).append('\n');
        }
        for (Actuator a : state.actuators.values()) {
          sb.append("  - actuator ").append(a.getActuatorId()).append(" type=")
            .append(a.getActuatorType()).append(" on=").append(a.isOn()).append('\n');
        }
        System.out.print(sb.toString());
      } catch (Exception e) {
        // Non-fatal logging error
        System.out.println("[CP-Logic] Updated node " + node.getNodeID());
      }
    } catch (Exception e) {
      System.out.println("[CP-Logic] Error processing Node update: " + e.getMessage());
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
  public void subscribe(String nodeId) {
    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "SUBSCRIBE_NODE");
    obj.addProperty("controlPanelId", controlPanelId);
    obj.addProperty("nodeID", nodeId);
    comm.sendJson(gson.toJson(obj));
  }

  public void unsubscribe(String nodeId) {
    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "UNSUBSCRIBE_NODE");
    obj.addProperty("controlPanelId", controlPanelId);
    obj.addProperty("nodeID", nodeId);
    comm.sendJson(gson.toJson(obj));
  }

  public void requestNode(String nodeId) {
    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "REQUEST_NODE");
    obj.addProperty("controlPanelId", controlPanelId);
    obj.addProperty("nodeID", nodeId);
    comm.sendJson(gson.toJson(obj));
  }

  /**
   * Request the node to add a sensor at runtime.
   * Fields: sensorType (TEMPERATURE|LIGHT|HUMIDITY|CO2), sensorId, minThreshold, maxThreshold
   */
  public void addSensor(String nodeId, String sensorType, String sensorId, double minThreshold, double maxThreshold) {
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
   * Request the node to add an actuator at runtime.
   */
  public void addActuator(String nodeId, String actuatorId, String actuatorType) {
    JsonObject obj = new JsonObject();
    obj.addProperty("messageType", "ADD_ACTUATOR");
    obj.addProperty("controlPanelId", controlPanelId);
    obj.addProperty("nodeID", nodeId);
    obj.addProperty("actuatorId", actuatorId);
    obj.addProperty("actuatorType", actuatorType);
    comm.sendJson(gson.toJson(obj));
  }

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