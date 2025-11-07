import java.util.Scanner;

/**
 * Main class for the Smart Greenhouse System control panel.
 */
class Main {
  public static final Scanner scanner = new Scanner(System.in); 

  public static boolean RUNNING = true;


  /**
  * Main control panel constructor.
  */
  private Main() {
  }

  /**
   * Run the main control panel loop.
   */
  public void run() {
    System.out.println("Welcome to the Smart Greenhouse System");
    while (RUNNING) {
      System.out.println("\n== Control Panel ==");
      System.out.println("1) List rooms");
      System.out.println("2) Create room");
      System.out.println("3) Open room");
      System.out.println("4) Remove room");
      System.out.println("0) Exit");
      System.out.print("Choose: ");
      String choice = scanner.nextLine().trim();
      switch (choice) {
        case "1" -> listRooms();
        case "2" -> createRoom();
        case "3" -> openRoom();
        case "4" -> removeRoom();
        case "0" -> {
          System.out.println("Exiting.");
          exit();
        }
        default -> System.out.println("Unknown option.");
      }
    }
  }

  public static void main(String[] args) {
    new Main().run();
  }

  /* 
  * List rooms function - placeholder 
  */
  private static void listRooms() {
    System.out.println("(placeholder) listRooms not implemented.");
    }

  /* 
  * Create room function - placeholder 
  */
  private static void createRoom() {
    System.out.println("(placeholder) createRoom not implemented.");
    }

  /*
  * Open room function - placeholder
  */
  private static void openRoom() {
    System.out.println("(placeholder) openRoom not implemented.");
    }

  /*
  * Remove room function - placeholder
  */
  private static void removeRoom() {
    System.out.println("(placeholder) removeRoom not implemented.");
  }

  /**
   * Exit the application by stopping the main loop and closing resources.
   */
  private static void exit() {
    RUNNING = false;
    scanner.close();
  }

}
