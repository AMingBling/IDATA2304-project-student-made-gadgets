// package controlpanel;

// import static org.junit.jupiter.api.Assertions.assertTrue;
// import org.junit.jupiter.api.Test;

// /**
//  * Test class for ControlPanelLogic.
//  *
//  * <p>The following is tested:</p>
//  *
//  * <b>Positive tests:</b>
//  *
//  * <ul>
//  *   <li>handleIncomingJson_updatesNodes: verifies that a SENSOR_DATA_FROM_NODE JSON payload is parsed and the node state is stored.</li>
//  *   <li>handleIncomingJson_ignoresNonJson: verifies that non-JSON input is ignored and does not modify state.</li>
//  * </ul>
//  *
//  * @author Group 1
//  * @version 2025-11-18
//  */
// public class ControlPanelLogicTest {

//   /**
//    * Test that a SENSOR_DATA_FROM_NODE message updates the internal nodes map.
//    *
//    * <p>Expected outcome:</p>
//    * <ul>
//    *   <li>The nodes map contains an entry for node id "n1".</li>
//    *   <li>The node "n1" contains a sensor with id "temp1".</li>
//    *   <li>The node "n1" contains an actuator with id "act1".</li>
//    * </ul>
//    */
//   @Test
//   public void handleIncomingJson_updatesNodes() {
//     ControlPanelLogic cpl = new ControlPanelLogic("cp-test");

//     String json = "{"
//         + "\"messageType\":\"SENSOR_DATA_FROM_NODE\","
//         + "\"nodeID\":\"n1\","
//         + "\"sensors\":[{"
//         +   "\"sensorId\":\"temp1\","
//         +   "\"sensorType\":\"TEMPERATURE\","
//         +   "\"value\":25.0,"
//         +   "\"unit\":\"C\","
//         +   "\"minThreshold\":10.0,"
//         +   "\"maxThreshold\":30.0"
//         + "}],"
//         + "\"actuators\":[{"
//         +   "\"actuatorId\":\"act1\","
//         +   "\"actuatorType\":\"HEATER\","
//         +   "\"on\":false"
//         + "}]"
//         + "}";

//     cpl.handleIncomingJson(json);

//     assertTrue(cpl.getNodes().containsKey("n1"));
//     assertTrue(cpl.getNodes().get("n1").sensors.containsKey("temp1"));
//     assertTrue(cpl.getNodes().get("n1").actuators.containsKey("act1"));
//   }

//   /**
//    * Test that non-JSON input is ignored.
//    *
//    * <p>Expected outcome: the nodes map remains empty after passing invalid input.</p>
//    */
// @Test
//   public void handleIncomingJson_ignoresNonJson() {
//     ControlPanelLogic cpl = new ControlPanelLogic("cp-test");
//     cpl.handleIncomingJson("NOT A JSON MESSAGE");
//     assertTrue(cpl.getNodes().isEmpty());
//   }
// }