package util;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Enkel kommunikasjonsklasse for Control Panel.
 * Åpner en TCP-forbindelse mot server, leser linjedelimited JSON i egen tråd
 * og videresender hver mottatt linje til en callback (Consumer<String>).
 * Har metoder for å sende JSON tilbake til server og for å lukke forbindelsen.
 */
public class ControlPanelCommunication {

    private final String controlPanelId;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile Thread readerThread;
    private final Gson gson;
    private final Consumer<String> onJson;

    /**
     * Oppretter en kommunikasjonsinstans.
     *
     * @param onJson callback som mottar hver linjedelimited JSON som kommer fra server
     * @param gson   Gson-instans brukt ved behov (kan være null hvis ikke brukt her)
     */
    public ControlPanelCommunication(Consumer<String> onJson, Gson gson, String controlPanelId) {
        this.onJson = onJson;
        this.gson = gson;
        this.controlPanelId = controlPanelId;
    }

    /**
     * Åpner TCP-tilkobling mot gitt ip og port.
     * Starter en lesetråd som kaller {@code onJson.accept(line)} for hver mottatt linje.
     *
     * @param ip   serverens ip-adresse (f.eks. "127.0.0.1")
     * @param port serverens port (f.eks. 5000)
     * @throws IOException ved I/O-feil ved oppkobling
     */
    public void connect(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Register this control panel identity at server
        JsonObject reg = new JsonObject();
        reg.addProperty("messageType", "REGISTER_CONTROL_PANEL");
        reg.addProperty("controlPanelId", controlPanelId);
        out.println(gson.toJson(reg));

        startListenThread();
    }

    /**
     * Starter bakgrunnstråd som leser linjer fra socket og sender dem til callback.
     * Tråden settes som daemon slik at JVM kan avslutte hvis kun daemon-tråder kjører.
     */

    private void startListenThread() {
        readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    try { onJson.accept(line); } catch (Exception e) { System.err.println("Listener error: " + e.getMessage()); }
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
        if (out == null) { System.err.println("[CP-Comm] Not connected"); return; }
        synchronized (out) { out.println(json); }
    }

    /**
     * Lukker reader-tråd, input/output-strømmer og socket. Kan kalles flere ganger.
     */
    public void close() {
        try { if (readerThread != null) readerThread.interrupt(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }
}