// package controlpanel;

// import com.google.gson.Gson;
// import com.google.gson.JsonObject;
// import com.google.gson.JsonElement;
// import com.google.gson.JsonSyntaxException;
// import entity.Actuator;

// import java.io.IOException;
// import java.util.Map;
// import java.util.HashMap;
// import java.util.concurrent.ConcurrentHashMap;

// /**
//  * Logikk-laget: behandler JSON, holder state og tilbyr API mot UI.
//  * lagt til controlpannelId
//  */
// public class ControlPanelLogic {
//     private final String controlPanelId;
//     private final Gson gson = new Gson();
//     private final ControlPanelCommunication comm;
//     private final Map<String, NodeState> nodes = new ConcurrentHashMap<>();


//     /**
//      * Oppretter ControlPanelLogic og initialiserer kommunikasjon.
//      * Kommunikasjonen vil bruke denne.handleIncomingJson som callback.
//      */
//     public ControlPanelLogic(String controlPanelId) {
//         this.controlPanelId = controlPanelId;
//         this.comm = new ControlPanelCommunication(this::handleIncomingJson, gson, controlPanelId);
//     }

//     /**
//      * Åpner forbindelse til serveren gjennom kommunikasjonslaget.
//      *
//      * @param ip serverens IP-adresse
//      * @param port serverens port
//      * @throws IOException hvis tilkobling feiler
//      */
//     public void connect(String ip, int port) throws IOException { comm.connect(ip, port); }

//     /**
//      * closing communication
//      */
//     public void close() { comm.close(); }

//     /**
//      * handling incoming json
//      */
//     public void handleIncomingJson(String json) {
//         if (json == null || !json.startsWith("{")) { System.out.println("[CP-Logic] Non-JSON ignored: " + json); return; }
//         JsonObject obj;
//         try { obj = gson.fromJson(json, JsonObject.class); } catch (JsonSyntaxException e) { System.out.println("[CP-Logic] malformed JSON: " + json); return; }
//         JsonElement mt = obj.get("messageType");
//         if (mt == null || mt.isJsonNull()) { System.out.println("[CP-Logic] Missing messageType: " + json); return; }
//         String type = mt.getAsString();
//         switch (type) {
//             case "SENSOR_DATA_FROM_NODE" -> processSensorData(json);
//             case "ACTUATOR_STATUS" -> processActuatorStatus(json);
//             default -> System.out.println("[CP-Logic] Unknown type: " + type);
//         }
//     }

//     /**
//      * parsing nodemessage with sesnoreadings and updates intern state
//      */
//     private void processSensorData(String json) {
//         try {
//             NodeMessage msg = gson.fromJson(json, NodeMessage.class);
//             if (msg == null || msg.getSensorReadings() == null) return;
//             NodeState state = nodes.computeIfAbsent(msg.getNodeID(), NodeState::new);
//             for (SensorMessage sm : msg.getSensorReadings()) state.sensors.put(sm.getSensorId(), sm);
//         } catch (Exception e) { System.out.println("[CP-Logic] parse NodeMessage error: " + e.getMessage()); }
//     }

//     /**
//      * parser nodemessage that reportsacutator status and updates intern acutator state
//      */
//     private void processActuatorStatus(String json) {
//         try {
//             NodeMessage msg = gson.fromJson(json, NodeMessage.class);
//             if (msg == null || msg.getSensorReadings() == null) return;
//             NodeState state = nodes.computeIfAbsent(msg.getNodeID(), NodeState::new);
//             for (SensorMessage sm : msg.getSensorReadings()) {
//                 try {
//                     Actuator a = new Actuator(sm.getSensorId(), util.SensorType.valueOf(sm.getType()), 0, 0);
//                     a.setOn(sm.getValue() == 1);
//                     state.actuators.put(a.getActuatorId(), a);
//                 } catch (IllegalArgumentException ex) { System.out.println("[CP-Logic] Unknown sensor type: " + sm.getType()); }
//             }
//         } catch (Exception e) { System.out.println("[CP-Logic] parse NodeMessage error: " + e.getMessage()); }
//     }

//     // UI API
//     public Map<String, NodeState> getNodes() { return nodes; }
//     public Actuator getActuator(String nodeId, String actuatorId) { NodeState ns = nodes.get(nodeId); return ns == null ? null : ns.actuators.get(actuatorId); }


//     /**
//      * sending a commando to change acutators on/off
//      */
//     public void setActuatorState(String nodeId, String actuatorId, boolean on) {
//         JsonObject obj = new JsonObject();
//         obj.addProperty("messageType", "ACTUATOR_COMMAND");
//         obj.addProperty("controlPanelId", controlPanelId); // <-- legg til origin
//         obj.addProperty("nodeID", nodeId);
//         obj.addProperty("actuatorId", actuatorId);
//         obj.addProperty("command", on ? "TURN_ON" : "TURN_OFF");
//         comm.sendJson(gson.toJson(obj));
//     }

//     /**
//      * update min/max for a sensortype on a node
//      */
//     public void updateTargetRange(String nodeId, String sensorType, double min, double max) {
//         JsonObject obj = new JsonObject();
//         obj.addProperty("messageType", "TARGET_UPDATE");
//         obj.addProperty("nodeID", nodeId);
//         obj.addProperty("sensorType", sensorType);
//         obj.addProperty("targetMin", min);
//         obj.addProperty("targetMax", max);
//         comm.sendJson(gson.toJson(obj));
//     }

//     /**
//      * holder for state per node
//      */
//     public static class NodeState {
//         public final String nodeId;
//         public final Map<String, SensorMessage> sensors = new HashMap<>();
//         public final Map<String, Actuator> actuators = new HashMap<>();
//         public NodeState(String nodeId) { this.nodeId = nodeId; }
//     }
// }