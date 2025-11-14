
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;

import java.time.LocalDateTime;
import java.lang.reflect.Type;

/**
 * Leser linjer fra socket.
*Hvis linjen ser ut som JSON (starter med '{') forsøker den å deserialisere til SensorMessage ved hjelp av Gson.
*Lokal tidspunkt håndteres via en liten (de)serializer som bruker ISO-8601-tekst (LocalDateTime.toString()/parse()).
*Logger parse-resultatet eller feilmeldinger.
 */

public class ControlPanel {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        // Gson med adapter for LocalDateTime (ISO-8601)
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
                .create();

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("CONTROL_PANEL_CONNECTED");

            String input;
            while ((input = in.readLine()) != null) {
                input = input.trim();
                if (input.isEmpty()) continue;

                // Enkel sjekk: JSON-objekt starter med '{'
                if (input.startsWith("{")) {
                    try {
                        SensorMessage msg = gson.fromJson(input, SensorMessage.class);
                        System.out.println("[ControlPanel] parsed SensorMessage -> id: " + msg.getSensorId()
                                + ", type: " + msg.getType()
                                + ", value: " + msg.getValue()
                                + ", unit: " + msg.getUnit()
                                + ", ts: " + msg.getTimestamp());
                    } catch (JsonSyntaxException e) {
                        System.out.println("[ControlPanel] malformed JSON: " + input);
                    } catch (Exception e) {
                        System.out.println("[ControlPanel] parse error: " + e.getMessage());
                    }
                } else {
                    // Ikke-JSON tekstmelding
                    System.out.println("[ControlPanel] received -> " + input);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Serializer & Deserializer for LocalDateTime using ISO-8601 (e.g. 2025-11-07T12:34:56)
    private static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString());
        }
    }
}