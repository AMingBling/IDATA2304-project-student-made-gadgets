package entity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import util.SensorType;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Random;

/**
 * SensorNodes kan bli kjørt i terminalen ved hjelp av kommandoen mvn exec:java "-Dexec.mainClass=entity.SensorNode" "-Dexec.args=<ID> <Lokasjon>"
 * og kobler seg til Serveren
 */
public class SensorNode {

    private final String nodeId;
    private final String location;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Gson gson;

    public SensorNode(String nodeId, String location, PrintWriter out, BufferedReader in, Gson gson) {
        this.nodeId = nodeId;
        this.location = location;
        this.out = out;
        this.in = in;
        this.gson = gson;
    }

    // ---------- METHODS ----------

    public void start() {
        // Start thread to listen for messages from server
        Thread listener = new Thread(this::listenForCommands);
        listener.setDaemon(true);
        listener.start();

        // Simulate periodic sensor readings
        simulateSensorData();
    }

    private void simulateSensorData() {
        Random random = new Random();

        try {
            while (true) {
                double temp = 20 + random.nextDouble() * 5;

                SensorMessage sm = new SensorMessage(
                        SensorType.TEMPERATURE,
                        nodeId + "-temp",
                        LocalDateTime.now(),
                        temp,
                        "°C"
                );

                NodeMessage nm = new NodeMessage(
                        nodeId,
                        location,
                        LocalDateTime.now(),
                        Collections.singletonList(sm)
                );
                nm.setMessageType("SENSOR_DATA_FROM_NODE");

                out.println(gson.toJson(nm));
                System.out.println("Node → Server: " + gson.toJson(nm));

                Thread.sleep(3000);
            }
        } catch (InterruptedException e) {
            System.out.println("Sensor simulation stopped.");
        }
    }

    private void listenForCommands() {
        try {
            String incoming;
            while ((incoming = in.readLine()) != null) {
                System.out.println("Command received: " + incoming);
                // Could be parsed further later
            }
        } catch (IOException e) {
            System.out.println("Lost connection to server.");
        }
    }

    // ---------- MAIN ----------

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java SensorNode <nodeId> <location>");
            return;
        }

        String nodeId = args[0];
        String location = args[1];

        final String SERVER_IP = "127.0.0.1";
        final int SERVER_PORT = 5000;

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
                .create();

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("SensorNode " + nodeId + " connected to server.");

            // Let server know who we are and wait for ack
            out.println("SENSOR_NODE_CONNECTED " + nodeId);
            String serverResponse = in.readLine();
            if (serverResponse == null) {
                System.out.println("No response from server after registering. Exiting.");
                return;
            }
            if (serverResponse.equals("NODE_ID_REJECTED")) {
                System.out.println("Node ID '" + nodeId + "' rejected by server (duplicate). Exiting.");
                return;
            }
            if (!serverResponse.equals("NODE_ID_ACCEPTED")) {
                System.out.println("Unexpected server response: " + serverResponse + ". Exiting.");
                return;
            }

            SensorNode node = new SensorNode(nodeId, location, out, in, gson);
            node.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- TIME SERIALIZERS ----------
    private static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString());
        }
    }
}
