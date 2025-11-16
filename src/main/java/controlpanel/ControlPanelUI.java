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
      showMenu();
      handleInput(scanner.nextLine().trim());
    }
  }

  private void showDashboard() {
    System.out.println("\n==============================================");
    System.out.println(" SMART GREENHOUSE CONTROL PANEL - SMG SYSTEM ");
    System.out.println("==============================================");
    System.out.println("     Welcome to the Smart Greenhouse System");
    System.out.println("Connected to server. Active nodes:");
    
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

  private void showMenu() {
    System.out.println("\n1) Toggle actuator");
    System.out.println("2) Broadcast command");
    System.out.println("3) Refresh data");
    System.out.println("0) Exit");
    System.out.print("Choice: ");
  }

  private void handleInput(String choice) {
    switch (choice) {
      case "1" -> System.out.println("(placeholder) Toggle actuator");
      case "2" -> System.out.println("(placeholder) Broadcast command");
      case "3" -> System.out.println("(placeholder) Refresh data");
      case "0" -> {
        System.out.println("Exiting...");
        running = false;
        logic.close();
      }
      default -> System.out.println("Unknown option.");
    }
  }
}
