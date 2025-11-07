import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



import entity.SensorMessage;
import entity.NodeMessage;
import util.SensorType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Random;



public class Node {

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    private static final String NODE_ID = "node-01";
    private static final String LOCATION = "office-1";

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
            .create();


    public static void main(String[] args) {

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Node connected to server.");

          
            // THREAD FOR INCOMING MESSAGES
            Thread listener = new Thread(() -> {
                try {
                    String incoming;

                    while ((incoming = in.readLine()) != null) {
                        parseIncoming(incoming, out);
                    }
                } catch (Exception e) {
                    System.out.println("Node listener error: " + e.getMessage());
                }
            });
            listener.setDaemon(true);
            listener.start();

           
            // PERIODIC SENSOR SIMULATION-
            Random random = new Random();

            while (true) {

                double temp = 20 + random.nextDouble() * 5;

                SensorMessage sm = new SensorMessage(
                        SensorType.TEMPERATURE,
                        NODE_ID + "-temp",
                        LocalDateTime.now(),
                        temp,
                        "°C"
                );

                NodeMessage nm = new NodeMessage(
                        NODE_ID,
                        LOCATION,
                        LocalDateTime.now(),
                        Collections.singletonList(sm)
                );
                nm.setMessageType("SENSOR_DATA_FROM_NODE");

                out.println(gson.toJson(nm));
                System.out.println("Node → ControlPanel: " + gson.toJson(nm));

                Thread.sleep(3000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  
    // PARSE ALL INCOMING MESSAGES
    private static void parseIncoming(String json, PrintWriter out) {

        try {
            // 1) Parse messageType first
            String type = gson.fromJson(json, GenericType.class).messageType;

            switch (type) {

                case "NODE_COMMAND":  // ControlPanel → Node
                    NodeMessage nodeMsg = gson.fromJson(json, NodeMessage.class);
                    handleIncomingNodeCommand(nodeMsg);
                    break;

                case "SENSOR_DATA":   // Sensor → Node → ControlPanel
                    SensorMessage sensorMsg = gson.fromJson(json, SensorMessage.class);
                    forwardSensorToControlPanel(sensorMsg, out);
                    break;

                default:
                    System.out.println("Unknown message type: " + type);
            }

        } catch (Exception e) {
            System.out.println("Failed to parse: " + json);
        }
    }

   
    // CONTROL → NODE
    private static void handleIncomingNodeCommand(NodeMessage nm) {
        System.out.println("Node received CONTROL command:");
        nm.getSensorReadings().forEach(sm -> {
            System.out.println(" Dispatch to local sensor: " + sm.getSensorId());
            // Here you would update actuators or internal state
        });
    }

    
    // SENSOR → NODE → CONTROL
    private static void forwardSensorToControlPanel(SensorMessage sm, PrintWriter out) {

        NodeMessage wrapper = new NodeMessage(
                NODE_ID,
                LOCATION,
                LocalDateTime.now(),
                Collections.singletonList(sm)
        );
        wrapper.setMessageType("NODE_WRAPPED_SENSOR_DATA");

        out.println(gson.toJson(wrapper));
        System.out.println("Node forwarded to ControlPanel: " + gson.toJson(wrapper));
    }


    // Helper: get "messageType" field
    private static class GenericType {
        String messageType;
    }

    //
    // LocalDateTime (de)serializers used by gson
    //Dette kan nok gjøres på en bedre måte! bare å importere dezirialiseren men jeg fikk det ikke til atm
    private static class LocalDateTimeSerializer implements com.google.gson.JsonSerializer<java.time.LocalDateTime> {
        @Override
        public com.google.gson.JsonElement serialize(java.time.LocalDateTime src, java.lang.reflect.Type typeOfSrc,
                                                    com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(src.toString());
        }
    }

    private static class LocalDateTimeDeserializer implements com.google.gson.JsonDeserializer<java.time.LocalDateTime> {
        @Override
        public java.time.LocalDateTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfT,
                                                   com.google.gson.JsonDeserializationContext context) {
            return java.time.LocalDateTime.parse(json.getAsString());
        }
    }




}
