// ...existing code...
import entity.SensorMessage;
import util.SensorType;
import java.time.LocalDateTime;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

/**
 * opens a TCP connectin to ""127.0.0.1""
 * packs a random value in infinite loop and sens it in a sensor message over socket
 * logs sent json to the consoule
 *
 */

public class Node {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);
            Random random = new Random();

            while (true) {
                //double sensorValue = random.nextDouble() * 100.0;
                SensorMessage msg = new SensorMessage(
                    SensorType.TEMPERATURE,
                    "node-01-temp",
                    LocalDateTime.now(),
                    22.3,
                    "°C"
                );

                out.println(msg.toJson());
                System.out.println("Sent: " + msg.toJson());

                Thread.sleep(3000);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}