package network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 5000;

    private static List<Socket> controlPanels = Collections.synchronizedList(new ArrayList<>());
    // Subscriptions: controlPanel socket -> set of nodeIDs it wants updates for
    private static Map<Socket, Set<String>> controlPanelSubscriptions = new ConcurrentHashMap<>();
    // Store last-known node JSON per nodeID so REQUEST_NODE can be answered immediately
    private static Map<String, String> lastKnownNodeJson = new ConcurrentHashMap<>();
    // Map nodeId -> socket for sensor nodes to prevent duplicate node IDs
    private static Map<String, Socket> sensorNodes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private boolean isControlPanel = false;
            private String nodeId = null;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String initialMessage = in.readLine();
                boolean handledInitial = false;
                if (initialMessage != null) {
                    String trimmed = initialMessage.trim();
                    // Old plain-text protocol
                    if (trimmed.equals("CONTROL_PANEL_CONNECTED")) {
                        isControlPanel = true;
                        controlPanels.add(socket);
                        System.out.println("Control panel connected: " + socket.getInetAddress());
                        handledInitial = true;
                    } else if (trimmed.startsWith("SENSOR_NODE_CONNECTED")) {
                    // Expect format: SENSOR_NODE_CONNECTED <nodeId>
                        String[] parts = trimmed.split(" ", 2);
                    if (parts.length < 2) {
                        out.println("NODE_ID_REJECTED");
                        socket.close();
                        return;
                    }
                    nodeId = parts[1].trim();
                    // Check for duplicate nodeId
                    if (sensorNodes.containsKey(nodeId)) {
                        out.println("NODE_ID_REJECTED");
                        System.out.println("Rejected duplicate nodeId '" + nodeId + "' from " + socket.getInetAddress());
                        socket.close();
                        return;
                    } else {
                        sensorNodes.put(nodeId, socket);
                        out.println("NODE_ID_ACCEPTED");
                        System.out.println("entity.Sensor connected: " + socket.getInetAddress() + " (" + nodeId + ")");
                    }
                    } else if (trimmed.startsWith("{")) {
                        // Fallback: try JSON registration for control panel (newer protocol)
                        try {
                            Gson gson = new Gson();
                            JsonObject obj = gson.fromJson(trimmed, JsonObject.class);
                            if (obj != null && obj.has("messageType") && "REGISTER_CONTROL_PANEL".equals(obj.get("messageType").getAsString())) {
                                isControlPanel = true;
                                controlPanels.add(socket);
                                // initialize empty subscription set (user must subscribe explicitly)
                                controlPanelSubscriptions.put(socket, ConcurrentHashMap.newKeySet());
                                String cpId = obj.has("controlPanelId") ? obj.get("controlPanelId").getAsString() : "<unknown>";
                                System.out.println("Control panel registered: " + socket.getInetAddress() + " (id=" + cpId + ")");
                                handledInitial = true;
                            }
                        } catch (JsonSyntaxException ignored) {
                            // Not JSON or malformed - fall through to generic fallback
                        }
                    }

                    if (!handledInitial) {
                        // Fallback: treat as a sensor without explicit id (rare)
                        System.out.println("entity.Sensor connected (no id): " + socket.getInetAddress());
                    }
                }

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (isControlPanel) {
                        System.out.println("[ControlPanel] received -> " + inputLine);
                        // Expect control panel to send JSON commands (ACTUATOR_COMMAND, TARGET_UPDATE)
                        if (inputLine.trim().startsWith("{")) {
                            try {
                                Gson gson = new Gson();
                                JsonObject obj = gson.fromJson(inputLine, JsonObject.class);
                                String mt = obj.has("messageType") && !obj.get("messageType").isJsonNull()
                                    ? obj.get("messageType").getAsString()
                                    : null;
                                if ("ACTUATOR_COMMAND".equals(mt) || "TARGET_UPDATE".equals(mt)) {
                                    if (obj.has("nodeID") && !obj.get("nodeID").isJsonNull()) {
                                        String targetNode = obj.get("nodeID").getAsString();
                                        Socket nodeSocket = sensorNodes.get(targetNode);
                                        if (nodeSocket != null && !nodeSocket.isClosed()) {
                                            try {
                                                PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);
                                                nodeOut.println(inputLine);
                                                System.out.println("Forwarded command to node " + targetNode);
                                            } catch (IOException e) {
                                                System.out.println("Error forwarding to node: " + e.getMessage());
                                            }
                                        } else {
                                            System.out.println("Target node not connected: " + targetNode);
                                        }
                                    } else {
                                        System.out.println("No nodeID in control panel message: " + inputLine);
                                    }
                                } else if ("SUBSCRIBE_NODE".equals(mt) || "UNSUBSCRIBE_NODE".equals(mt)) {
                                    if (obj.has("nodeID") && !obj.get("nodeID").isJsonNull()) {
                                        String targetNode = obj.get("nodeID").getAsString();
                                        Set<String> subs = controlPanelSubscriptions.get(socket);
                                        if (subs == null) {
                                            subs = ConcurrentHashMap.newKeySet();
                                            controlPanelSubscriptions.put(socket, subs);
                                        }
                                        if ("SUBSCRIBE_NODE".equals(mt)) {
                                            subs.add(targetNode);
                                            System.out.println("Control panel " + socket.getInetAddress() + " subscribed to " + targetNode);
                                            // acknowledge subscription
                                            JsonObject ack = new JsonObject();
                                            ack.addProperty("messageType", "SUBSCRIBE_ACK");
                                            ack.addProperty("nodeID", targetNode);
                                            ack.addProperty("status", "OK");
                                            try { out.println(ack.toString()); } catch (Exception ignored) {}
                                        } else {
                                            subs.remove(targetNode);
                                            System.out.println("Control panel " + socket.getInetAddress() + " unsubscribed from " + targetNode);
                                            // acknowledge unsubscription
                                            JsonObject ack = new JsonObject();
                                            ack.addProperty("messageType", "UNSUBSCRIBE_ACK");
                                            ack.addProperty("nodeID", targetNode);
                                            ack.addProperty("status", "OK");
                                            try { out.println(ack.toString()); } catch (Exception ignored) {}
                                        }
                                    } else {
                                        System.out.println("No nodeID in subscribe/unsubscribe message: " + inputLine);
                                    }
                                } else if ("REQUEST_NODE".equals(mt)) {
                                    if (obj.has("nodeID") && !obj.get("nodeID").isJsonNull()) {
                                        String targetNode = obj.get("nodeID").getAsString();
                                        String last = lastKnownNodeJson.get(targetNode);
                                        if (last != null) {
                                            // send last-known directly to this control panel
                                            try { out.println(last); } catch (Exception ignored) {}
                                            System.out.println("Served last-known state for " + targetNode + " to " + socket.getInetAddress());
                                        } else {
                                            // forward REQUEST_STATE to node if connected
                                            Socket nodeSocket = sensorNodes.get(targetNode);
                                            if (nodeSocket != null && !nodeSocket.isClosed()) {
                                                try {
                                                    JsonObject req = new JsonObject();
                                                    req.addProperty("messageType", "REQUEST_STATE");
                                                    req.addProperty("requester", "controlpanel");
                                                    PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);
                                                    nodeOut.println(req.toString());
                                                    System.out.println("Forwarded REQUEST_STATE to node " + targetNode);
                                                } catch (IOException e) {
                                                    System.out.println("Error forwarding REQUEST_STATE to node: " + e.getMessage());
                                                }
                                            } else {
                                                System.out.println("Target node not connected for REQUEST_NODE: " + targetNode);
                                            }
                                        }
                                    }
                                } else {
                                    // Unknown control-panel message type; ignore or log
                                    System.out.println("[ControlPanel] Unknown messageType: " + mt);
                                }
                            } catch (JsonSyntaxException e) {
                                System.out.println("[ControlPanel] malformed JSON: " + inputLine);
                            }
                        } else {
                            System.out.println("[ControlPanel] Non-JSON message ignored: " + inputLine);
                        }
                    } else {
                        System.out.println("Received from node: " + inputLine);
                        // Ensure control panels receive a messageType so they can handle it;
                        // add messageType if missing (merge into top-level JSON)
                        String toSend = inputLine;
                        String nodeIdForMsg = null;
                        if (inputLine.trim().startsWith("{")) {
                            try {
                                Gson gson = new Gson();
                                JsonObject obj = gson.fromJson(inputLine, JsonObject.class);
                                // Store last-known JSON for this node
                                if (obj != null && obj.has("nodeID") && !obj.get("nodeID").isJsonNull()) {
                                    String nid = obj.get("nodeID").getAsString();
                                    lastKnownNodeJson.put(nid, inputLine);
                                }
                                if (obj != null && !obj.has("messageType")) {
                                    obj.addProperty("messageType", "SENSOR_DATA_FROM_NODE");
                                }
                                if (obj != null && obj.has("nodeID") && !obj.get("nodeID").isJsonNull()) {
                                    nodeIdForMsg = obj.get("nodeID").getAsString();
                                }
                                toSend = gson.toJson(obj);
                            } catch (JsonSyntaxException ignored) {
                                // keep original message if not valid JSON
                            }
                        }
                        broadcastToControlPanels(toSend, nodeIdForMsg);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection lost with " + socket.getInetAddress());
            } finally {
                cleanup();
            }
        }

        private void broadcastToControlPanels(String message) {
            broadcastToControlPanels(message, null);
        }

        private void broadcastToControlPanels(String message, String nodeId) {
            synchronized (controlPanels) {
                for (Socket cpSocket : controlPanels) {
                    try {
                        // If a nodeId is provided, only send to control panels subscribed to that node
                        if (nodeId != null) {
                            Set<String> subs = controlPanelSubscriptions.get(cpSocket);
                            if (subs == null || !subs.contains(nodeId)) {
                                continue;
                            }
                        }
                        PrintWriter cpOut = new PrintWriter(cpSocket.getOutputStream(), true);
                        cpOut.println(message);
                    } catch (IOException e) {
                        System.out.println("Error sending to control panel: " + cpSocket.getInetAddress());
                    }
                }
            }
        }

        private void cleanup() {
            try {
                socket.close();
             } catch (IOException e) {
                 System.out.println("Error closing socket with " + socket.getInetAddress());
              }
            if (isControlPanel) {
                controlPanels.remove(socket);
                return;
            }

            // Only remove duplicate nodeId if it was *actually registered*
            if (nodeId != null && sensorNodes.get(nodeId) == socket) {
                sensorNodes.remove(nodeId);
                System.out.println("Node removed: " + nodeId);
            }
        }

    }
}