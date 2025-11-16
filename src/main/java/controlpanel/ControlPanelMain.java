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

    // Simple interactive CLI for testing: subscribe/unsubscribe/request/set/exit
    try (java.util.Scanner sc = new java.util.Scanner(System.in)) {
      System.out.println("Enter commands: subscribe <nodeId> | unsubscribe <nodeId> | request <nodeId> | set <nodeId> <actuatorId> <on|off> | exit");
      while (true) {
        System.out.print("> ");
        String line = sc.nextLine();
        if (line == null) break;
        String[] parts = line.trim().split("\\s+");
        if (parts.length == 0) continue;
        String cmd = parts[0].toLowerCase();
        try {
          switch (cmd) {
            case "subscribe" -> {
              if (parts.length >= 2) logic.subscribe(parts[1]); else System.out.println("Usage: subscribe <nodeId>");
            }
            case "unsubscribe" -> {
              if (parts.length >= 2) logic.unsubscribe(parts[1]); else System.out.println("Usage: unsubscribe <nodeId>");
            }
            case "request" -> {
              if (parts.length >= 2) logic.requestNode(parts[1]); else System.out.println("Usage: request <nodeId>");
            }
            case "set" -> {
              if (parts.length >= 4) {
                String nodeId = parts[1];
                String actuatorId = parts[2];
                boolean on = parts[3].equalsIgnoreCase("on") || parts[3].equalsIgnoreCase("true");
                logic.setActuatorState(nodeId, actuatorId, on);
              } else {
                System.out.println("Usage: set <nodeId> <actuatorId> <on|off>");
              }
            }
            case "exit" -> {
              System.out.println("Exiting...");
              sc.close();
              if (logic != null) logic.close();
              return;
            }
            default -> System.out.println("Unknown command: " + cmd);
          }
        } catch (Exception e) {
          System.out.println("Error handling command: " + e.getMessage());
        }
      }
    }
  }

}
