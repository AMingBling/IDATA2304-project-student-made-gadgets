package controlpanel;

import java.util.Map;
import java.util.Scanner;

public class ControlPanelUI {
  private final ControlPanelLogic logic;
  private final Scanner scanner = new Scanner(System.in);
  private boolean running = true;

  public ControlPanelUI(ControlPanelLogic logic) {
    this.logic = logic;
  }

  public void run() {
    while (running) {
      showDashboard();
      showHelp();
      System.out.print("> ");
      String line = scanner.nextLine();
      if (line == null) break;
      handleCommand(line.trim());
    }
  }

  private void showDashboard() {
    System.out.println("\n==============================================");
    System.out.println(" SMART GREENHOUSE CONTROL PANEL - SMG SYSTEM ");
    System.out.println("==============================================");
    System.out.println("     Welcome to the Smart Greenhouse System");
    System.out.println("       Connected to server. Active nodes:");
    
    try {
      Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();
      if (nodes == null || nodes.isEmpty()) {
        System.out.println("  (no active nodes)");
        return;
      }
      int idx = 1;
      for (ControlPanelLogic.NodeState ns : nodes.values()) {
        int sensorCount = ns.sensors == null ? 0 : ns.sensors.size();
        int actuatorCount = ns.actuators == null ? 0 : ns.actuators.size();
        System.out.printf("  %d) %s — sensors:%d actuators:%d%n", idx++, ns.nodeId, sensorCount, actuatorCount);
      }

    } catch (Exception e) {
      System.out.println("  Error retrieving node data: " + e.getMessage());
    }

  }

  private void showHelp() {
    System.out.println("\nCommands: ");
    System.out.println(" Subscribe <nodeId>");
    System.out.println(" Unsubscribe <nodeId>");
    System.out.println(" Request <nodeId>");
    System.out.println(" Set <nodeId> <actuatorId> <on|off>");
    System.out.println(" AddSensor <nodeId> <sensorType> <sensorId> <min> <max>");
    System.out.println(" AddActuator <nodeId> <actuatorId> <actuatorType>");
    System.out.println(" Refresh");
    System.out.println(" Exit");
  }

  private void handleCommand(String line) {
    if (line.isEmpty()) return;
    String[] parts = line.split("\\s+");
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
        case "addsensor" -> {
          if (parts.length >= 6) {
            String nodeId = parts[1];
            String sensorType = parts[2].toUpperCase();
            String sensorId = parts[3];
            double min = Double.parseDouble(parts[4]);
            double max = Double.parseDouble(parts[5]);
            logic.addSensor(nodeId, sensorType, sensorId, min, max);
          } else {
            System.out.println("Usage: addsensor <nodeId> <sensorType> <sensorId> <min> <max>");
          }
        }
        case "addactuator" -> {
          if (parts.length >= 4) {
            String nodeId = parts[1];
            String actuatorId = parts[2];
            String actuatorType = parts[3];
            logic.addActuator(nodeId, actuatorId, actuatorType);
          } else {
            System.out.println("Usage: addactuator <nodeId> <actuatorId> <actuatorType>");
          }
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
        case "refresh" -> {
          // Refreshes dashboard
        }
        case "exit" -> {
          System.out.println("Exiting...");
          running = false;
          logic.close();
          scanner.close();
        }
        default -> System.out.println("Unknown command: " + cmd);
      }
      } catch (Exception e) {
      System.out.println("Error handling command: " + e.getMessage());
  }
}
}
