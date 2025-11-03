
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class Sensor {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT)) {
            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);

            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Random random = new Random();

            while (true) {
                // Simuler sensor data
                int sensorData = random.nextInt(100);
                out.println("Sensor data: " + sensorData);
                System.out.println("Sent: Sensor data: " + sensorData);

                // Vent 3 sekunder før neste sending
                Thread.sleep(3000);
            }
           

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
}