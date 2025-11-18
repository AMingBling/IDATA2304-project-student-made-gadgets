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
    System.out.println(" AddSensor <nodeId>");
    System.out.println(" RemoveSensor <nodeId> <sensorId>");
    System.out.println(" Request <nodeId>");
    System.out.println(" Set <nodeId> <actuatorId> <on|off>");
    System.out.println(" SpawnNode <nodeId> <location>");
    System.out.println(" Exit");
  }

  private void handleCommand(String line) {
    if (line.isEmpty()) return;
    String[] parts = line.split("\\s+");
    String cmd = parts[0].toLowerCase();
    try {
      switch (cmd) {
        case "request" -> {
          if (parts.length >= 2) logic.requestNode(parts[1]); else System.out.println("Usage: request <nodeId>");
        }
        case "addsensor" -> {
          if (parts.length >= 2) {
            String nodeId = parts[1];
            // Determine available sensor types and filter out those already present
            java.util.List<String> allTypes = java.util.Arrays.asList("TEMPERATURE", "LIGHT", "HUMIDITY", "CO2");
            java.util.Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();

            // Immediately inform user if the nodeId is not connected/known
            if (nodes == null || !nodes.containsKey(nodeId)) {
              System.out.println("Node '" + nodeId + "' is not connected to the server. Use Request <nodeId> or check the node connection.");
              break;
            }
            java.util.Set<String> existing = new java.util.HashSet<>();
            if (nodes != null && nodes.containsKey(nodeId)) {
              ControlPanelLogic.NodeState ns = nodes.get(nodeId);
              if (ns != null && ns.sensors != null) {
                for (entity.sensor.Sensor s : ns.sensors.values()) {
                  existing.add(s.getSensorType().toUpperCase());
                }
              }
            }

            java.util.List<String> available = new java.util.ArrayList<>();
            for (String t : allTypes) if (!existing.contains(t)) available.add(t);

            if (available.isEmpty()) {
              System.out.println("Node " + nodeId + " already has all supported sensor types.");
              break;
            }

            System.out.println("Available sensor types:");
            for (int i = 0; i < available.size(); i++) {
              System.out.printf("  %d) %s%n", i + 1, available.get(i));
            }
            System.out.print("Choose type (number): ");
            String choiceLine = scanner.nextLine();
            int choice = 0;
            try { choice = Integer.parseInt(choiceLine.trim()); } catch (Exception e) { choice = 0; }
            if (choice < 1 || choice > available.size()) {
              System.out.println("Invalid selection.");
              break;
            }
            String sensorType = available.get(choice - 1);

            System.out.print("Sensor ID (e.g. s1): ");
            String sensorId = scanner.nextLine().trim();
            if (sensorId.isEmpty()) { System.out.println("Sensor ID cannot be empty."); break; }
            // Check duplicate sensorId on node (use cached state)
            if (nodes != null && nodes.containsKey(nodeId)) {
              ControlPanelLogic.NodeState nsCheck = nodes.get(nodeId);
              if (nsCheck != null && nsCheck.sensors != null && nsCheck.sensors.containsKey(sensorId)) {
                System.out.println("Sensor ID '" + sensorId + "' already exists on node " + nodeId + ". Aborting.");
                break;
              }
            }

            // Get recommended ranges for the chosen sensor type
            double[] recommended = getRecommendedRange(sensorType);
            String unit = getUnitForType(sensorType);
            String recMinStr = recommended != null ? String.valueOf((int)recommended[0]) : "";
            String recMaxStr = recommended != null ? String.valueOf((int)recommended[1]) : "";

            // Prompt with recommended values and allow empty input to accept recommendation
            if (recommended != null) {
              System.out.print("Min threshold (number) (" + recMinStr + " " + unit + " recommended): ");
            } else {
              System.out.print("Min threshold (number): ");
            }
            String minLine = scanner.nextLine().trim();
            if (minLine.isEmpty() && recommended != null) minLine = recMinStr;

            if (recommended != null) {
              System.out.print("Max threshold (number) (" + recMaxStr + " " + unit + " recommended): ");
            } else {
              System.out.print("Max threshold (number): ");
            }
            String maxLine = scanner.nextLine().trim();
            if (maxLine.isEmpty() && recommended != null) maxLine = recMaxStr;

            double min, max;
            try {
              min = Double.parseDouble(minLine);
              max = Double.parseDouble(maxLine);
            } catch (NumberFormatException nfe) {
              System.out.println("Invalid threshold numbers.");
              break;
            }

            logic.addSensor(nodeId, sensorType, sensorId, min, max);
          } else {
            System.out.println("Usage: addsensor <nodeId>");
          }
        }
        case "spawnnode" -> {
          String nodeId;
          String location;
          if (parts.length >= 3) {
            nodeId = parts[1];
            location = parts[2];
          } else {
            System.out.print("Node ID: ");
            nodeId = scanner.nextLine().trim();
            if (nodeId.isEmpty()) { System.out.println("Node ID cannot be empty."); break; }
            System.out.print("Location: ");
            location = scanner.nextLine().trim();
            if (location.isEmpty()) { System.out.println("Location cannot be empty."); break; }
          }
          boolean ok = logic.spawnNode(nodeId, location);
          if (!ok) System.out.println("Failed to spawn node. See log for details.");
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
        case "removesensor" -> {
          if (parts.length >= 3) {
            String nodeId = parts[1];
            String sensorId = parts[2];
            logic.removeSensor(nodeId, sensorId);
          } else {
            System.out.println("Usage: removesensor <nodeId> <sensorId>");
          }
        }
        case "exit" -> {
          System.out.println("Exiting...");
          running = false;
          logic.close();
        }
        default -> System.out.println("Unknown command: " + cmd);
      }
      } catch (Exception e) {
      System.out.println("Error handling command: " + e.getMessage());
  }
}

  /**
   * Return recommended [min,max] for a sensor type, or null if none.
   */
  private double[] getRecommendedRange(String sensorType) {
    if (sensorType == null) return null;
    switch (sensorType.toUpperCase()) {
      case "TEMPERATURE":
        return new double[]{15, 30};
      case "LIGHT":
        return new double[]{1000, 20000};
      case "HUMIDITY":
        return new double[]{50, 85};
      case "CO2":
        return new double[]{800, 1500};
      default:
        return null;
    }
  }

  private String getUnitForType(String sensorType) {
    if (sensorType == null) return "";
    switch (sensorType.toUpperCase()) {
      case "TEMPERATURE": return "°C";
      case "LIGHT": return "lux";
      case "HUMIDITY": return "%";
      case "CO2": return "ppm";
      default: return "";
    }
  }
}
