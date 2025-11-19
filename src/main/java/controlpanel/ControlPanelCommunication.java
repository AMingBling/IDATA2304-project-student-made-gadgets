package controlpanel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Simple communication helper for a Control Panel.
 *
 * <p>This class opens a TCP connection to the server, reads line-delimited JSON messages
 * on a background daemon thread and forwards each received line to a provided callback
 * {@code Consumer<String>}. It provides thread-safe sending of JSON strings back to the server and
 * a safe {@code close()} method that can be called multiple times.</p>
 *
 * <p>The class stores the connection info (ip and port) after a successful
 * {@link #connect(String, int)}.
 * The provided {@code Gson} instance may be {@code null} if not needed by callers of this class,
 * but it is used when registering the control panel identity at connect time.</p>
 */
public class ControlPanelCommunication {

  private final String controlPanelId;
  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;
  private volatile Thread readerThread;
  private final Gson gson;
  private final Consumer<String> onJson;
  // connection info stored after successful connect
  private String connectedIp = null;
  private int connectedPort = -1;

  /**
   * Create a communication instance for a control panel.
   *
   * @param onJson         callback that receives each line-delimited JSON string from the server.
   *                       The callback is invoked on the internal reader thread; callers should
   *                       handle threading if they need UI-thread execution.
   * @param gson           Gson instance used when registering identity during
   *                       {@link #connect(String, int)}. May be {@code null} if not required by the
   *                       caller.
   * @param controlPanelId identifier string for this control panel; sent to server during connect.
   */
  public ControlPanelCommunication(Consumer<String> onJson, Gson gson, String controlPanelId) {
    this.onJson = onJson;
    this.gson = gson;
    this.controlPanelId = controlPanelId;
  }

  /**
   * Open a TCP connection to the given server IP and port.
   *
   * <p>After a successful connect this method sends a registration message
   * (messageType = {@code REGISTER_CONTROL_PANEL}) containing the configured
   * {@code controlPanelId}. It then starts a background daemon thread which continuously reads
   * lines from the socket input stream and forwards them to {@code onJson}.</p>
   *
   * @param ip   server IPv4/IPv6 address or hostname (e.g. {@code "127.0.0.1"})
   * @param port server TCP port (e.g. {@code 5000})
   * @throws IOException when an I/O error occurs while creating the socket or streams
   */
  public void connect(String ip, int port) throws IOException {
    socket = new Socket(ip, port);
    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    out = new PrintWriter(socket.getOutputStream(), true);
    // remember connection info
    this.connectedIp = ip;
    this.connectedPort = port;
    // Register this control panel identity at server
    JsonObject reg = new JsonObject();
    reg.addProperty("messageType", "REGISTER_CONTROL_PANEL");
    reg.addProperty("controlPanelId", controlPanelId);
    out.println(gson.toJson(reg));

    startListenThread();
  }

  /**
   * Start the background thread that listens for incoming JSON messages.
   * The thread is a daemon thread named "cp-comm-reader".
   * It reads lines from the input stream and forwards them to the onJson callback.
   * If the connection is lost, it prints a message to standard output.
   *
   */
  private void startListenThread() {
    readerThread = new Thread(() -> {
      try {
        String line;
        while ((line = in.readLine()) != null) {
          line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
          try {
            onJson.accept(line);
          } catch (Exception e) {
            System.err.println("Listener error: " + e.getMessage());
          }
        }
      } catch (IOException e) {
        System.out.println("[CP-Comm] Lost connection: " + e.getMessage());
      }
    }, "cp-comm-reader");
    readerThread.setDaemon(true);
    readerThread.start();
  }

  /**
   * Sender en JSON-streng til serveren. Metoden er trådsikker.
   *
   * @param json JSON-tekst (linjedelimited) som skal sendes til server
   */
  public void sendJson(String json) {
    if (out == null) {
      System.err.println("[CP-Comm] Not connected");
      return;
    }
    synchronized (out) {
      out.println(json);
    }
  }

  /**
   * Lukker reader-tråd, input/output-strømmer og socket. Kan kalles flere ganger.
   */
  public void close() {
    try {
        if (readerThread != null) {
            readerThread.interrupt();
        }
    } catch (Exception ignored) {
    }
    try {
        if (in != null) {
            in.close();
        }
    } catch (Exception ignored) {
    }
    try {
        if (out != null) {
            out.close();
        }
    } catch (Exception ignored) {
    }
    try {
        if (socket != null) {
            socket.close();
        }
    } catch (Exception ignored) {
    }
  }






}