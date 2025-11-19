package controlpanel;

/**
 * Standalone entry point for launching a Control Panel instance.
 *
 * <p>Usage example (PowerShell):
 * <pre>
 * mvn --% exec:java -Dexec.mainClass=controlpanel.ControlPanelMain -Dexec.args="cp1 127.0.0.1 5000"
 * </pre>
 *
 * <p>The program expects three command-line arguments: {@code controlPanelId},
 * {@code serverIp} and {@code serverPort}. It will create a {@link ControlPanelLogic} instance,
 * connect it to the server, register a shutdown hook to close the connection on JVM exit, and then
 * start the interactive {@link ControlPanelUI}.
 */
public class ControlPanelMain {

  public static void main(String[] args) {

    if (args.length < 3) {
      System.out.println("Usage: java ControlPanelMain <controlPanelId> <serverIp> <serverPort>");
      return;
    }

    String controlPanelId = args[0];
    String serverIp = args[1];
    int serverPort = Integer.parseInt(args[2]);

    ControlPanelLogic logic = null;

    try {
      logic = new ControlPanelLogic(controlPanelId);
      logic.connect(serverIp, serverPort);
      System.out.println(
          "Control Panel " + controlPanelId + " connected to server at " + serverIp + ":"
              + serverPort);
      Runtime.getRuntime().addShutdownHook(new Thread(logic::close));

      ControlPanelUI ui = new ControlPanelUI(logic);
      ui.run();
    } catch (Exception e) {
      System.err.println("[CP] Failed to connect to server: " + e.getMessage());
    }

  }

}
