import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final int PORT = 5000;

    private static List<Socket> controlPanels = Collections.synchronizedList(new ArrayList<>());
    private static List<Socket> sensors = Collections.synchronizedList(new ArrayList<>());

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
                } else {
                    sensors.add(socket);
                    System.out.println("Sensor connected: " + socket.getInetAddress());
                }

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (isControlPanel) {
                        System.out.println("[ControlPanel] received -> " + inputLine);
                    } else {
                        System.out.println("Received from sensor: " + inputLine);
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
            } else {
                sensors.remove(socket);
            }
        }
    }
}