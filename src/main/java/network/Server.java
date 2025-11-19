package network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Server class to handle connections from sensor nodes and control panels.
 */
public class Server {

    private static final int PORT = 5000;
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void log(String role, String fmt, Object... args) {
        String msg = String.format(fmt, args);
        System.out.printf("\n[%s] %s: %s%n", LOG_TIME.format(LocalDateTime.now()), role, msg);
    }

    private static List<Socket> controlPanels = Collections.synchronizedList(new ArrayList<>());
    // Map socket -> controlPanelId (if provided during registration) for nicer disconnect logs
    private static Map<Socket, String> controlPanelIds = new ConcurrentHashMap<>();
    // Store last-known node JSON per nodeID so REQUEST_NODE can be answered immediately
    private static Map<String, String> lastKnownNodeJson = new ConcurrentHashMap<>();
    // Map nodeId -> socket for sensor nodes to prevent duplicate node IDs
    private static Map<String, Socket> sensorNodes = new ConcurrentHashMap<>();

  /**
   * Main method to start the server.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log("Server", "Started on port %d", PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("Server", "New client connected: %s", clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  /**
   * ClientHandler class to manage individual client connections.
   */
  static class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isControlPanel = false;
    private String nodeId = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;        
    }


    /**
     * Run method to handle client communication.
     */
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
                        // No id provided in legacy protocol; record address for later
                        controlPanelIds.put(socket, socket.getInetAddress().toString());
                        log("Server", "Control panel connected: %s", socket.getInetAddress());
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
                        log("Server", "Rejected duplicate nodeId '%s' from %s", nodeId, socket.getInetAddress());
                        socket.close();
                        return;
                    } else {
                        sensorNodes.put(nodeId, socket);
                        out.println("NODE_ID_ACCEPTED");
                        log("Server", "Node connected: %s (id=%s)", socket.getInetAddress(), nodeId);
                    }
                    } else if (trimmed.startsWith("{")) {
                        // Fallback: try JSON registration for control panel (newer protocol)
                        try {
                            Gson gson = new Gson();
                            JsonObject obj = gson.fromJson(trimmed, JsonObject.class);
                                if (obj != null && obj.has("messageType") && "REGISTER_CONTROL_PANEL".equals(obj.get("messageType").getAsString())) {
                                isControlPanel = true;
                                controlPanels.add(socket);
                                String cpId = obj.has("controlPanelId") ? obj.get("controlPanelId").getAsString() : socket.getInetAddress().toString();
                                controlPanelIds.put(socket, cpId);
                                log("Server", "Control panel registered: %s (id=%s)", socket.getInetAddress(), cpId);
                                handledInitial = true;
                            }
                        } catch (JsonSyntaxException ignored) {
                            // Not JSON or malformed - fall through to generic fallback
                        }
                    }

                    if (!handledInitial) {
                        // Fallback: treat as a sensor without explicit id (rare)
                        log("Server", "Node connected (no id): %s", socket.getInetAddress());
                    }
                }

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (isControlPanel) {
                        log("ControlPanel", "Received -> %s", inputLine);
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
                                                log("Server", "Forwarded command to node %s", targetNode);
                                            } catch (IOException e) {
                                                log("Server", "Error forwarding to node: %s", e.getMessage());
                                            }
                                        } else {
                                            log("Server", "Target node not connected: %s", targetNode);
                                        }
                                    } else {
                                        log("Server", "No nodeID in control panel message: %s", inputLine);
                                    }
                                
                                } else if ("REQUEST_NODE".equals(mt)) {
                                    String targetNode = obj.get("nodeID").getAsString();

                                    String lastJson = lastKnownNodeJson.get(targetNode);
                                    if (lastJson != null) {
                                        out.println(lastJson);
                                        log("Server", "Served cached state of %s to control panel", targetNode);
                                        // keep the control panel connection open so it can send further commands
                                        continue;
                                    }

                                    Socket nodeSocket = sensorNodes.get(targetNode);
                                    if (nodeSocket != null && !nodeSocket.isClosed()) {
                                        PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);

                                        JsonObject requestObj = new JsonObject();
                                        requestObj.addProperty("messageType", "REQUEST_STATE");
                                        nodeOut.println(requestObj.toString());
                                        log("Server", "Requested state of %s from node", targetNode);
                                    } else {
                                        System.out.println("Requested node not connected: " + targetNode);
                                    }
                                } else if ("ADD_SENSOR".equals(mt)) {
                                    String targetNode = obj.get("nodeID").getAsString();

                                    Socket nodeSocket = sensorNodes.get(targetNode);
                                    if (nodeSocket != null && !nodeSocket.isClosed()) {
                                        PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);
                                        nodeOut.println(inputLine);
                                        log("Server", "Forwarded ADD_SENSOR to node %s", targetNode);
                                    } else {
                                        System.out.println("Target node not connected: " + targetNode);
                                    }
                                } else if ("REMOVE_SENSOR".equals(mt)) {
                                    String targetNode = obj.get("nodeID").getAsString();

                                    Socket nodeSocket = sensorNodes.get(targetNode);
                                    if (nodeSocket != null && !nodeSocket.isClosed()) {
                                        PrintWriter nodeOut = new PrintWriter(nodeSocket.getOutputStream(), true);
                                        nodeOut.println(inputLine);
                                        log("Server", "Forwarded REMOVE_SENSOR to node %s", targetNode);
                                    } else {
                                        System.out.println("Target node not connected: " + targetNode);
                                    }
                                }
                                else {
                                    // Unknown control-panel message type; ignore or log
                                    log("ControlPanel", "Unknown messageType: %s", mt);
                                }
                            } catch (JsonSyntaxException e) {
                                log("ControlPanel", "Malformed JSON: %s", inputLine);
                            }
                        } else {
                            log("ControlPanel", "Non-JSON message ignored: %s", inputLine);
                        }
                    } else {

                        System.out.println("\n Received from node: " + inputLine);

                        log("Node", "Received -> %s", inputLine);

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
                log("Server", "Connection lost with %s", socket.getInetAddress());
            } finally {
                cleanup();
            }
        }

   

    /**
     * Broadcast a message to all connected control panels, optionally filtering by nodeId subscription.
     *
     * @param message the message to broadcast
     * @param nodeId  the nodeId to filter subscriptions (if null, send to all)
     */
    private void broadcastToControlPanels(String message, String nodeId) {
      synchronized (controlPanels) {
        for (Socket cpSocket : controlPanels) {
                        try {
                                                // Broadcast node updates to all control panels (no subscription model in UI)
                        PrintWriter cpOut = new PrintWriter(cpSocket.getOutputStream(), true);
                        cpOut.println(message);
          } catch (IOException e) {
                        log("Server", "Error sending to control panel: %s", cpSocket.getInetAddress());
          }
        }
      }
    }

    /**
     * Cleanup resources on client disconnect.
     */
    private void cleanup() {
            try {
                socket.close();
            } catch (IOException e) {
                log("Server", "Error closing socket with %s", socket.getInetAddress());
            }
            if (isControlPanel) {
                String cpId = controlPanelIds.remove(socket);
                controlPanels.remove(socket);
                log("Server", "Control Panel removed: %s", cpId != null ? cpId : socket.getInetAddress());
                return;
            }

                        // Only remove duplicate nodeId if it was *actually registered*
                        if (nodeId != null && sensorNodes.get(nodeId) == socket) {
                                sensorNodes.remove(nodeId);
                                log("Server", "Node removed: %s", nodeId);
                                // Notify control panels that this node disconnected so they can update their cache/UI
                                try {
                                        com.google.gson.JsonObject outObj = new com.google.gson.JsonObject();
                                        outObj.addProperty("messageType", "SENSOR_NODE_DISCONNECTED");
                                        outObj.addProperty("nodeID", nodeId);
                                        String payload = new com.google.gson.Gson().toJson(outObj);
                                        broadcastToControlPanels(payload, nodeId);
                                } catch (Exception ignored) {}
                        }
        }

    }
}