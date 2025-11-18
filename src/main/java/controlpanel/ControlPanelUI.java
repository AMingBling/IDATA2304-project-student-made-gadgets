package controlpanel;

import java.util.Map;
import java.util.Scanner;

/**
 * Interactive command-line UI for a Control Panel.
 *
 * <p>This class provides a simple text-based interface that allows an operator
 * to list known nodes, request node state, add/remove sensors, spawn simulated
 * nodes and control actuators. It delegates all business logic and networking
 * to {@link ControlPanelLogic}.
 */
public class ControlPanelUI {
  private final ControlPanelLogic logic;
  private final Scanner scanner = new Scanner(System.in);
  private boolean running = true;

  /**
   * Create a new ControlPanelUI bound to the provided logic instance.
   *
   * @param logic the {@link ControlPanelLogic} instance used for commands and state
   */
  public ControlPanelUI(ControlPanelLogic logic) {
    this.logic = logic;
  }

  /**
   * Start the interactive UI loop.
   *
   * <p>This method blocks until the user issues the {@code Exit} command. It
   * prints a dashboard and a help menu on each iteration and reads commands
   * from standard input.</p>
   */
  public void run() {
    boolean first = true;
    while (running) {
      if (first) {
        showDashboard();
        showHelp();
        first = false;
      }
      System.out.print("> ");
      String line = scanner.nextLine();
      if (line == null) break;
      handleCommand(line.trim());
      // Show dashboard/help after handling the command so any immediate responses
      // (for example the response to CheckNode) are printed before the menu.
      showDashboard();
      showHelp();
    }
  }

  /**
   * Print a compact dashboard showing connected nodes and counts of sensors
   * and actuators for each node.
   */
  private void showDashboard() {
    System.out.println("\n==============================================");
    System.out.println(" SMART GREENHOUSE CONTROL PANEL - SMG SYSTEM ");
    System.out.println("==============================================");
    System.out.println("     Welcome to the Smart Greenhouse System");    
  }

  /**
   * Print a brief list of available commands and their usage.
   */
  private void showHelp() {
    System.out.println("\nCommands: ");
    System.out.println(" - CheckGreenhouse");
    System.out.println(" - AddNode <nodeId> <location>");
    System.out.println(" - AddSensor <nodeId>");
    System.out.println(" - RemoveSensor <nodeId> <sensorId>");
    System.out.println(" - CheckNode <nodeId>");
    System.out.println(" - ToggleActuator <nodeId> <actuatorId> <on|off>");
    System.out.println(" - Exit\n");
  }

  /**
   * Print a detailed list of connected nodes and basic counts for sensors/actuators.
   */
  private void checkGreenhouse() {
    Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();
    if (nodes == null || nodes.isEmpty()) {
      System.out.println("No connected nodes.");
      return;
    }
    // Collect distinct locations
    java.util.Set<String> locations = new java.util.TreeSet<>();
    for (ControlPanelLogic.NodeState ns : nodes.values()) {
      if (ns != null && ns.location != null && !ns.location.isBlank()) locations.add(ns.location);
    }

    if (locations.size() == 1) {
      // Single greenhouse connected: show explicit message
      String loc = locations.iterator().next();
      System.out.println("+nYour Control Panel is connected to " + loc);
    } else {
      System.out.println("\nConnected nodes (" + nodes.size() + "): ");
    }
    java.util.List<String> ids = new java.util.ArrayList<>(nodes.keySet());
    java.util.Collections.sort(ids);
    for (String id : ids) {
      ControlPanelLogic.NodeState ns = nodes.get(id);
      int sensorCount = (ns == null || ns.sensors == null) ? 0 : ns.sensors.size();
      int actuatorCount = (ns == null || ns.actuators == null) ? 0 : ns.actuators.size();
      String loc = (ns == null || ns.location == null) ? "" : ns.location;
      System.out.printf("- Node %s (location: %s): sensors = %d actuators = %d%n", id, loc, sensorCount, actuatorCount);
    }
  }

  /**
   * Parse and execute a single command entered by the user.
   *
   * <p>Supported commands include: {@code Request}, {@code AddSensor}, {@code
   * RemoveSensor}, {@code Set}, {@code SpawnNode} and {@code Exit}.</p>
   *
   * @param line the raw command line entered by the user
   */
  private void handleCommand(String line) {
    if (line.isEmpty()) return;
    String[] parts = line.split("\\s+");
    String cmd = parts[0].toLowerCase();
    // Backwards-compatible alias: treat old 'spawnnode' command as 'addnode'
    if (cmd.equals("spawnnode")) cmd = "addnode";
    try {
      switch (cmd) {
        case "checknode" -> {
          if (parts.length >= 2) logic.requestNode(parts[1]); else System.out.println("Usage: checknode <nodeId>");
        }
        case "checkgreenhouse" -> {
          checkGreenhouse();
        }
        case "addsensor" -> {
          if (parts.length >= 2) {
            String nodeId = parts[1];
            // Determine available sensor types and filter out those already present
            java.util.List<String> allTypes = java.util.Arrays.asList("TEMPERATURE", "LIGHT", "HUMIDITY", "CO2");
            java.util.Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();

            // Immediately inform user if the nodeId is not connected/known
            if (nodes == null || !nodes.containsKey(nodeId)) {
              System.out.println("\nNode '" + nodeId + "' is not connected to the server. Use CheckGreenhouse to see connected nodes.");
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

            System.out.println("\nAvailable sensor types:");
            for (int i = 0; i < available.size(); i++) {
              System.out.printf("  %d) %s%n", i + 1, available.get(i));
            }
            System.out.print("\nChoose type (number): ");
            String choiceLine = scanner.nextLine();
            int choice = 0;
            try { choice = Integer.parseInt(choiceLine.trim()); } catch (Exception e) { choice = 0; }
            if (choice < 1 || choice > available.size()) {
              System.out.println("Invalid selection.");
              break;
            }
            String sensorType = available.get(choice - 1);

            System.out.print("\nSensor ID (e.g. s1): ");
            String sensorId = scanner.nextLine().trim();
            if (sensorId.isEmpty()) { System.out.println("Sensor ID cannot be empty."); break; }
            // Check duplicate sensorId on node (use cached state)
            if (nodes != null && nodes.containsKey(nodeId)) {
              ControlPanelLogic.NodeState nsCheck = nodes.get(nodeId);
              if (nsCheck != null && nsCheck.sensors != null && nsCheck.sensors.containsKey(sensorId)) {
                System.out.println("Sensor ID '" + sensorId + "' already exists on node " + nodeId + ". ID must be uniqe.");
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
              System.out.print("\nMin threshold (number) (" + recMinStr + " " + unit + " recommended): ");
            } else {
              System.out.print("\nMin threshold (number): ");
            }
            String minLine = scanner.nextLine().trim();
            if (minLine.isEmpty() && recommended != null) minLine = recMinStr;

            if (recommended != null) {
              System.out.print("\nMax threshold (number) (" + recMaxStr + " " + unit + " recommended): ");
            } else {
              System.out.print("\nMax threshold (number): ");
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

            boolean added = logic.addSensor(nodeId, sensorType, sensorId, min, max);
            if (added) {
              System.out.println("\nSuccessfully added sensor ID:" + sensorId + " type:" + sensorType + " to Node " + nodeId);
            }
          } else {
            System.out.println("Usage: addsensor <nodeId>");
          }
        }
        case "addnode" -> {
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
          if (!ok) System.out.println("Failed to add node. See log for details."); else System.out.println("Successfully added node: " + nodeId);
        }
        case "toggleactuator" -> {
          if (parts.length >= 4) {
            String nodeId = parts[1];
            String actuatorId = parts[2];
            boolean on = parts[3].equalsIgnoreCase("on") || parts[3].equalsIgnoreCase("true");
            java.util.Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();
            if (nodes == null || !nodes.containsKey(nodeId)) {
              System.out.println("Node '" + nodeId + "' is not known. Use CheckGreenhouse to list nodes.");
              break;
            }
            ControlPanelLogic.NodeState ns = nodes.get(nodeId);
            if (ns == null || ns.actuators == null || !ns.actuators.containsKey(actuatorId)) {
              System.out.println("Warning: actuator '" + actuatorId + "' not found on node " + nodeId + ". Sending command anyway.");
            }
            logic.setActuatorState(nodeId, actuatorId, on);
            if (on) {
              System.out.println("\nSuccessfully turned on actuator ID:" + actuatorId + " to Node " + nodeId);
            } else {
              System.out.println("\nSuccessfully turned off actuator ID:" + actuatorId + " from Node " + nodeId);
            }
          } else {
            System.out.println("Usage: toggleactuator <nodeId> <actuatorId> <on|off>");
          }
        }
        case "removesensor" -> {
          if (parts.length >= 3) {
            String nodeId = parts[1];
            String sensorId = parts[2];
            java.util.Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();
            if (nodes == null || !nodes.containsKey(nodeId)) {
              System.out.println("Node '" + nodeId + "' is not known. Use CheckGreenhouse to list nodes.");
              break;
            }
            ControlPanelLogic.NodeState ns = nodes.get(nodeId);
            if (ns == null || ns.sensors == null || !ns.sensors.containsKey(sensorId)) {
              System.out.println("Sensor '" + sensorId + "' not found on node " + nodeId + ".");
              break;
            }
            String sensorType = ns.sensors.get(sensorId).getSensorType();
            boolean removed = logic.removeSensor(nodeId, sensorId);
            if (removed) {
              System.out.println("\nSuccessfully removed sensor ID:" + sensorId + " type:" + sensorType + " from Node " + nodeId);
            }
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
   * Return a recommended [min, max] threshold pair for the given sensor type.
   *
   * @param sensorType sensor type name (e.g. TEMPERATURE)
   * @return two-element array {min, max} or {@code null} if no recommendation
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

  /**
   * Return a human-readable unit string for the given sensor type.
   *
   * @param sensorType sensor type name
   * @return unit string (e.g. "°C", "%", "lux") or empty string if unknown
   */
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
