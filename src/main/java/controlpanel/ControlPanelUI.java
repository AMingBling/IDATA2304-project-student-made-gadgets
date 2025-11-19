package controlpanel;

import java.util.Map;
import java.util.Scanner;

/**
 * Interactive command-line UI for a Control Panel.
 *
 * <p>
 * This class provides a simple text-based interface that allows an operator
 * to list known nodes, request node state, add/remove sensors, spawn simulated
 * nodes and control
 * actuators. It delegates all business logic and networking to
 * {@link ControlPanelLogic}.
 */
public class ControlPanelUI {

  private final ControlPanelLogic logic;
  private final Scanner scanner = new Scanner(System.in);
  private boolean running = true;
  private static final Map<String, double[]> ABSOLUTE_LIMITS = Map.of(
      "TEMPERATURE", new double[] { 0, 40 },
      "HUMIDITY", new double[] { 0, 100 },
      "LIGHT", new double[] { 0, 30000 },
      "CO2", new double[] { 400, 5000 });

  /**
   * Create a new ControlPanelUI bound to the provided logic instance.
   *
   * @param logic the {@link ControlPanelLogic} instance used for commands and
   *              state
   */
  public ControlPanelUI(ControlPanelLogic logic) {
    this.logic = logic;
  }

  

  /**
   * Start the interactive UI loop.
   *
   * <p>
   * This method blocks until the user issues the {@code Exit} command. It
   * prints a dashboard and a help menu on each iteration and reads commands from
   * standard
   * input.
   * </p>
   */
  public void run() {
    while (running) {
      showDashboard();
      showHelp();
      System.out.print("> ");
      String line = scanner.nextLine();
      if (line == null) {
        break;
      }

      if (line.trim().isEmpty()) {
        System.out.println("\nPlease write a command (cannot be empty).");
        continue; // viser menyen på nytt i neste loop
      }

      handleCommand(line.trim());
    }
  }

  /**
   * Print a compact dashboard showing connected nodes and counts of sensors and
   * actuators for
   * each node.
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
    // System.out.println(" - AddNode <nodeId> <location>");
    System.out.println(" - AddSensor <nodeId>");
    System.out.println(" - RemoveSensor <nodeId> <sensorId>");
    System.out.println(" - CheckNode <nodeId>");
    System.out.println(" - ToggleActuator <nodeId> <actuatorId> <on|off>");
    System.out.println(" - CheckAllSensorsOfType");
    System.out.println(" - Exit\n");
  }

  /**
   * Print a detailed list of connected nodes and basic counts for
   * sensors/actuators.
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
      if (ns != null && ns.location != null && !ns.location.isBlank()) {
        locations.add(ns.location);
      }
    }

    if (locations.size() == 1) {
      // Single greenhouse connected: show explicit message
      String loc = locations.iterator().next();
      System.out.println("\nYour Control Panel is connected to " + loc);
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
      System.out.printf("- Node %s (location: %s): sensors = %d actuators = %d%n", id, loc,
          sensorCount, actuatorCount);
    }
  }

  /**
   * Validate that the provided command arguments are sufficient.
   *
   * @param parts          the split command line parts
   * @param requiredTokens number of required tokens (including command itself)
   * @param usage          usage string to print on error
   * @return true if validation passed, false if not (and error printed)
   */
  private boolean validateArgs(String[] parts, int requiredTokens, String usage) {
    if (parts == null) {
      return false;
    }
    if (parts.length < requiredTokens) {
      System.out.println("Please write a command (cannot be empty).");
      System.out.println(usage);
      return false;
    }
    // check that required arguments are not empty strings
    for (int i = 1; i < requiredTokens; i++) {
      if (parts[i] == null || parts[i].trim().isEmpty()) {
        System.out.println("Please write a command (cannot be empty).");
        System.out.println(usage);
        return false;
      }
    }
    return true;
  }

  /**
   * Parse and execute a single command entered by the user.
   *
   * <p>
   * Supported commands include: {@code Request}, {@code AddSensor}, {@code
   * RemoveSensor}, {@code Set}, {@code SpawnNode} and {@code Exit}.
   * </p>
   *
   * @param line the raw command line entered by the user
   */
  private void handleCommand(String line) {
    if (line == null) {
      return;
    }
    line = line.trim();
    if (line.isEmpty()) {
      System.out.println("Please write a command (cannot be empty).");
      return;
    }

    String[] parts = line.split("\\s+");
    String cmd = parts[0].toLowerCase();
    if (cmd.equals("spawnnode")) {
      cmd = "addnode";
    }

    try {
      switch (cmd) {
        case "checknode" -> {
          if (!validateArgs(parts, 2, "Usage: CheckNode <nodeId>")) {
            return;
          }
          String nodeId = parts[1];
          java.util.Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();
          if (nodes == null || !nodes.containsKey(nodeId)) {
            System.out.println("\nNode '" + nodeId
                + "' is not connected to the server. Use CheckGreenhouse to see connected nodes.");
            break;
          }
          logic.requestNode(nodeId);
        }

        case "checkgreenhouse" -> {
          checkGreenhouse();
        }

        // case "addsensor" -> {
        // if (!validateArgs(parts, 2, "Usage: AddSensor <nodeId>")) return;
        // String nodeId = parts[1];

        // java.util.List<String> allTypes = java.util.Arrays.asList("TEMPERATURE",
        // "LIGHT", "HUMIDITY", "CO2");
        // java.util.Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();

        // if (nodes == null || !nodes.containsKey(nodeId)) {
        // System.out.println("\nNode '" + nodeId + "' is not connected to the server.
        // Use CheckGreenhouse to see connected nodes.");
        // break;
        // }

        // java.util.Set<String> existing = new java.util.HashSet<>();
        // ControlPanelLogic.NodeState ns = nodes.get(nodeId);
        // if (ns != null && ns.sensors != null) {
        // for (entity.sensor.Sensor s : ns.sensors.values()) if (s != null &&
        // s.getSensorType() != null) existing.add(s.getSensorType().toUpperCase());
        // }

        // java.util.List<String> available = new java.util.ArrayList<>();
        // for (String t : allTypes) if (!existing.contains(t)) available.add(t);

        // if (available.isEmpty()) {
        // System.out.println("Node " + nodeId + " already has all supported sensor
        // types.");
        // break;
        // }

        // System.out.println("\nAvailable sensor types:");
        // for (int i = 0; i < available.size(); i++) System.out.printf(" %d) %s%n", i +
        // 1, available.get(i));

        // System.out.print("\nChoose type (number): ");
        // String choiceLine = scanner.nextLine();
        // if (choiceLine == null || choiceLine.trim().isEmpty()) {
        // System.out.println("Please write a command (cannot be empty).");
        // break;
        // }
        // int choice;
        // try { choice = Integer.parseInt(choiceLine.trim()); } catch (Exception e) {
        // choice = 0; }
        // if (choice < 1 || choice > available.size()) {
        // System.out.println("Invalid selection.");
        // break;
        // }
        // }

        case "addsensor" -> {
          if (parts.length >= 2) {
            String nodeId = parts[1];
            // Determine available sensor types and filter out those already present
            java.util.List<String> allTypes = java.util.Arrays.asList("TEMPERATURE", "LIGHT",
                "HUMIDITY", "CO2");
            java.util.Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();

            // Immediately inform user if the nodeId is not connected/known
            if (nodes == null || !nodes.containsKey(nodeId)) {
              System.out.println("\nNode '" + nodeId
                  + "' is not connected to the server. Use CheckGreenhouse to see connected nodes.");
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
            for (String t : allTypes) {
              if (!existing.contains(t)) {
                available.add(t);
              }
            }

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
            try {
              choice = Integer.parseInt(choiceLine.trim());
            } catch (Exception e) {
              choice = 0;
            }
            if (choice < 1 || choice > available.size()) {
              System.out.println("Invalid selection.");
              break;
            }
            String sensorType = available.get(choice - 1);

            // Sensor ID assignment is handled by the logic layer; UI does not generate IDs.

            // Get recommended ranges for the chosen sensor type
            double[] recommended = getRecommendedRange(sensorType);
            String unit = getUnitForType(sensorType);
            String recMinStr = recommended != null ? String.valueOf((int) recommended[0]) : "";
            String recMaxStr = recommended != null ? String.valueOf((int) recommended[1]) : "";

            // Prompt with recommended values and allow empty input to accept recommendation
            if (recommended != null) {
              System.out.print(
                  "\nMin threshold (number) (" + recMinStr + " " + unit + " recommended): ");
            } else {
              System.out.print("\nMin threshold (number): ");
            }
            String minLine = scanner.nextLine().trim();
            if (minLine.isEmpty() && recommended != null) {
              minLine = recMinStr;
            }

            if (recommended != null) {
              System.out.print(
                  "\nMax threshold (number) (" + recMaxStr + " " + unit + " recommended): ");
            } else {
              System.out.print("\nMax threshold (number): ");
            }
            String maxLine = scanner.nextLine().trim();
            if (maxLine.isEmpty() && recommended != null) {
              maxLine = recMaxStr;
            }

            double min, max;
            try {
              min = Double.parseDouble(minLine);
              max = Double.parseDouble(maxLine);
            } catch (NumberFormatException nfe) {
              System.out.println("Invalid threshold numbers.");
              break;
            }

            String validationMessage = logic.validateThresholds(sensorType, min, max);
            if (validationMessage != null) {
              System.out.println("\nSensor not added: " + validationMessage);
              break;
            }

            String assigned = logic.addSensorAuto(nodeId, sensorType, min, max);
            if (assigned != null) {
              System.out.println(
                  "\nSuccessfully added sensor ID:" + assigned + " type:" + sensorType + " to Node "
                      + nodeId);
            }
          } else {
            System.out.println("Usage: addsensor <nodeId>");
          }
        }
        case "checkallsensorsoftype", "checkallsensors" -> {
          // Prompt user to select sensor type
          java.util.List<String> types = java.util.Arrays.asList("TEMPERATURE", "LIGHT", "HUMIDITY",
              "CO2");
          System.out.println("\nSensor types:");
          for (int i = 0; i < types.size(); i++) {
            System.out.printf("  %d) %s%n", i + 1, types.get(i));
          }
          System.out.print("\nChoose type (number): ");
          String choiceLine = scanner.nextLine();
          int choice = 0;
          try {
            choice = Integer.parseInt(choiceLine.trim());
          } catch (Exception e) {
            choice = 0;
          }
          if (choice < 1 || choice > types.size()) {
            System.out.println("Invalid selection.");
            break;
          }
          String sensorType = types.get(choice - 1);
          java.util.Map<String, java.util.List<entity.sensor.Sensor>> found = logic.getSensorsByType(
              sensorType);
          if (found == null || found.isEmpty()) {
            System.out.println("No sensors of type " + sensorType + " found.");
            break;
          }
          System.out.println("\nSensors of type " + sensorType + ":");
          // Print grouped by node
          java.util.List<String> nodeIds = new java.util.ArrayList<>(found.keySet());
          java.util.Collections.sort(nodeIds);
          for (String nid : nodeIds) {
            System.out.println("Node: " + nid);
            java.util.List<entity.sensor.Sensor> list = found.get(nid);
            list.sort((a, b) -> a.getSensorId().compareToIgnoreCase(b.getSensorId()));
            for (entity.sensor.Sensor s : list) {
              System.out.printf("  - ID: %s, Value: %.2f %s, Range: %.2f - %.2f, Type: %s%n",
                  s.getSensorId(), s.getValue(), s.getUnit(), s.getMinThreshold(),
                  s.getMaxThreshold(), s.getSensorType());
            }
          }
        }
        case "toggleactuator" -> {
          if (!validateArgs(parts, 4, "Usage: ToggleActuator <nodeId> <actuatorId> <on|off>")) {
            return;
          }
          String nodeId = parts[1];
          String actuatorId = parts[2];
          String onOff = parts[3].toLowerCase();
          if (!("on".equals(onOff) || "off".equals(onOff) || "true".equals(onOff) || "false".equals(
              onOff))) {
            System.out.println("Invalid value: " + parts[3] + ". Use 'on' or 'off'.");
            System.out.println("Usage: ToggleActuator <nodeId> <actuatorId> <on|off>");
            break;
          }
          boolean on = onOff.equals("on") || onOff.equals("true");
          java.util.Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();
          if (nodes == null || !nodes.containsKey(nodeId)) {
            System.out.println(
                "Node '" + nodeId + "' is not known. Use CheckGreenhouse to list nodes.");
            break;
          }
            ControlPanelLogic.NodeState ns2 = nodes.get(nodeId);
            if (ns2 == null || ns2.actuators == null || !ns2.actuators.containsKey(actuatorId)) {
            System.out.println("Actuator '" + actuatorId + "' not found on node " + nodeId
              + ". Command not sent.");
            break;
            }
            logic.setActuatorState(nodeId, actuatorId, on);
            System.out.println(
              on ? ("\nSuccessfully turned on actuator ID:" + actuatorId + " on Node " + nodeId)
                : ("\nSuccessfully turned off actuator ID:" + actuatorId + " on Node "
                  + nodeId));
        }

        case "removesensor" -> {
          if (!validateArgs(parts, 3, "Usage: RemoveSensor <nodeId> <sensorId>")) {
            return;
          }
          String nodeId = parts[1];
          String sensorId = parts[2];
          java.util.Map<String, ControlPanelLogic.NodeState> nodes = logic.getNodes();
          if (nodes == null || !nodes.containsKey(nodeId)) {
            System.out.println(
                "Node '" + nodeId + "' is not known. Use CheckGreenhouse to list nodes.");
            break;
          }
          ControlPanelLogic.NodeState ns3 = nodes.get(nodeId);
          if (ns3 == null || ns3.sensors == null || !ns3.sensors.containsKey(sensorId)) {
            System.out.println("Sensor '" + sensorId + "' not found on node " + nodeId + ".");
            break;
          }
          String sensorType = ns3.sensors.get(sensorId).getSensorType();
          boolean removed = logic.removeSensor(nodeId, sensorId);
          if (removed) {
            System.out.println(
                "\nSuccessfully removed sensor ID:" + sensorId + " type:" + sensorType
                    + " from Node " + nodeId);
          }
        }

        case "exit" -> {
          System.out.println("Exiting...");
          running = false;
          logic.close();
        }

        default -> {
          System.out.println("\nUnknown command: " + cmd);
          System.out.println("\nValid commands:");
          System.out.println(" - CheckGreenhouse");
          System.out.println(" - AddSensor <nodeId>");
          System.out.println(" - RemoveSensor <nodeId> <sensorId>");
          System.out.println(" - CheckNode <nodeId>");
          System.out.println(" - ToggleActuator <nodeId> <actuatorId> <on|off>");
          System.out.println(" - Exit");
        }
      }
    } catch (Exception e)

    {
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
    if (sensorType == null) {
      return null;
    }
    switch (sensorType.toUpperCase()) {
      case "TEMPERATURE":
        return new double[] { 15, 30 };
      case "LIGHT":
        return new double[] { 1000, 20000 };
      case "HUMIDITY":
        return new double[] { 50, 85 };
      case "CO2":
        return new double[] { 800, 1500 };
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
    if (sensorType == null) {
      return "";
    }
    switch (sensorType.toUpperCase()) {
      case "TEMPERATURE":
        return "°C";
      case "LIGHT":
        return "lux";
      case "HUMIDITY":
        return "%";
      case "CO2":
        return "ppm";
      default:
        return "";
    }
  }
}
