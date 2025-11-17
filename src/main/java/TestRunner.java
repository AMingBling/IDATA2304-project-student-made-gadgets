// import controlpanel.ControlPanelLogic;

// public class TestRunner {

//   public static void main(String[] args) throws Exception {
//     // Start server in background (daemon) so JVM can exit after test
//     Thread serverThread = new Thread(() -> network.Server.main(new String[0]), "test-server");
//     serverThread.setDaemon(true);
//     serverThread.start();

//     Thread.sleep(500);

//     // Start a NodeClient in background (daemon)
//     Thread nodeThread = new Thread(() -> network.NodeClient.main(new String[]{"01", "greenhouse1"}), "test-node");
//     nodeThread.setDaemon(true);
//     nodeThread.start();

//     Thread.sleep(500);

//     // Create control panel logic and connect
//     ControlPanelLogic cp = new ControlPanelLogic("testrun-cp");
//     cp.connect("127.0.0.1", 5000);
//     System.out.println("TestRunner: ControlPanel connected.");

//     // Add a sensor to node 01
//     System.out.println("TestRunner: Sending ADD_SENSOR");
//     cp.addSensor("01", "TEMPERATURE", "s1", 18.0, 26.0);

//     Thread.sleep(500);

//     // Request the node state
//     System.out.println("TestRunner: Sending REQUEST_NODE");
//     cp.requestNode("01");

//     // Wait for responses to arrive and be logged
//     Thread.sleep(1500);

//     cp.close();

//     System.out.println("TestRunner: Done.");
//   }
// }
