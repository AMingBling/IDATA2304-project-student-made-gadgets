// MainApp orchestrates existing mains: Server, NodeClient, ControlPanelMain

/**
 * Main launcher that starts Server, a NodeClient and a ControlPanel in one JVM.
 * Use for local testing only.
 */
public class  MainApp {

	public static void main(String[] args) throws Exception {
		// Start server in background thread using existing main
		Thread serverThread = new Thread(() -> network.Server.main(new String[0]), "server-thread");
		serverThread.setDaemon(true);
		serverThread.start();

		// Give server time to bind
		Thread.sleep(500);

		// Start NodeClient in background thread using its main method
		Thread nodeThread = new Thread(() -> network.NodeClient.main(new String[]{"01", "greenhouse1"}), "node-thread");
		nodeThread.setDaemon(true);
		nodeThread.start();

		// Run ControlPanelMain in foreground so user interacts with its CLI
		controlpanel.ControlPanelMain.main(new String[]{"cp1", "127.0.0.1", "5000"});
	}

}

