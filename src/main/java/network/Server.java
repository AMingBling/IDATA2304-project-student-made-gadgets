package network;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 5000;

    private static List<Socket> controlPanels = Collections.synchronizedList(new ArrayList<>());
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
                if (initialMessage != null && initialMessage.equals("CONTROL_PANEL_CONNECTED")) {
                    isControlPanel = true;
                    controlPanels.add(socket);
                    System.out.println("Control panel connected: " + socket.getInetAddress());
                } else if (initialMessage != null && initialMessage.startsWith("SENSOR_NODE_CONNECTED")) {
                    // Expect format: SENSOR_NODE_CONNECTED <nodeId>
                    String[] parts = initialMessage.split(" ", 2);
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
                } else {
                    // Fallback: treat as a sensor without explicit id (rare)
                    System.out.println("entity.Sensor connected (no id): " + socket.getInetAddress());
                }

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (isControlPanel) {
                        System.out.println("[ControlPanel] received -> " + inputLine);
                    } else {
                        System.out.println("Received from node: " + inputLine);
                        broadcastToControlPanels(inputLine);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection lost with " + socket.getInetAddress());
            } finally {
                cleanup();
            }
        }

        private void broadcastToControlPanels(String message) {
            synchronized (controlPanels) {
                for (Socket cpSocket : controlPanels) {
                    try {
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