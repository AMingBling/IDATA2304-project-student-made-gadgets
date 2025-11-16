package controlpanel;

import controlpanel.ControlPanelLogic;


// mvn --% exec:java -Dexec.mainClass=controlpanel.ControlPanelMain -Dexec.args="<id> <ip> <port>" 
// ex: mvn --% exec:java -Dexec.mainClass=controlpanel.ControlPanelMain -Dexec.args="cp1 127.0.0.1 5000"
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
    } catch (Exception e) {
      System.err.println("Failed to connect to server: " + e.getMessage());
      return;
    }

    // Keep the main thread alive to maintain the connection
    try {
      Thread.sleep(Long.MAX_VALUE);
    } catch (InterruptedException e) {
      System.out.println("Control Panel interrupted, shutting down.");
    } finally {
      if (logic != null) {
        logic.close();
      }
    }
  }

}
