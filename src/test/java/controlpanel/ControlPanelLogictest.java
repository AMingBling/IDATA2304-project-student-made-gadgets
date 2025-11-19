
package controlpanel;


import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Classic test class for ControlPanelLogic with positive and negative tests.
 */
public class ControlPanelLogicTest {

  private ControlPanelLogic cp;
  private TestControlPanelCommunication tc;
  private final PrintStream originalOut = System.out;

  // ----- Helper TestComm that captures outgoing JSON -----
  public static class TestControlPanelCommunication extends ControlPanelCommunication {
    public volatile String lastSent = null;
    public TestControlPanelCommunication(Consumer<String> callback, Gson gson, String controlPanelId) {
      super(callback, gson, controlPanelId);
    }
    @Override
    public void sendJson(String json) {
      this.lastSent = json;
    }
  }

  private Gson buildGson() {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, t, c) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, t, c) -> LocalDateTime.parse(json.getAsString()))
        .create();
  }

  // Inject TestControlPanelCommunication into ControlPanelLogic via reflection
  private TestControlPanelCommunication injectTestComm(ControlPanelLogic cp) throws Exception {
    TestControlPanelCommunication tc = new TestControlPanelCommunication(s -> {}, buildGson(), "testCP");
    Field f = ControlPanelLogic.class.getDeclaredField("comm");
    f.setAccessible(true);
    f.set(cp, tc);
    return tc;
  }

  @BeforeEach
  public void setUp() throws Exception {
    cp = new ControlPanelLogic("testCP");
    tc = injectTestComm(cp);
  }

  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
  }

  // ----- POSITIVE TESTS -----

  @Test
  public void addSensor_positive_sendsAddSensorJson() {
    boolean ok = cp.addSensor("n1", "TEMPERATURE", "s1", 10.0, 30.0);
    assertTrue(ok, "addSensor should succeed for a new sensor");
    assertNotNull(tc.lastSent, "sendJson should have been called");
    assertTrue(tc.lastSent.toUpperCase().contains("ADD_SENSOR"), "Should send ADD_SENSOR");
    assertTrue(tc.lastSent.contains("s1"), "Should include sensorId");
  }

  @Test
  public void removeSensor_positive_sendsRemoveSensorJson() {
    // prepare cached node state with a sensor id
    ControlPanelLogic.NodeState ns = new ControlPanelLogic.NodeState("n2");
    cp.getNodes().put("n2", ns);
    ns.sensors.put("s_remove", null);

    boolean ok = cp.removeSensor("n2", "s_remove");
    assertTrue(ok, "removeSensor should return true when sensor exists");
    assertNotNull(tc.lastSent, "sendJson should have been called");
    assertTrue(tc.lastSent.toUpperCase().contains("REMOVE_SENSOR"));
    assertTrue(tc.lastSent.contains("s_remove"));
  }

  @Test
  public void setActuatorState_positive_sendsActuatorCommand() {
    cp.setActuatorState("nodeX", "act1", true);
    assertNotNull(tc.lastSent, "sendJson should have been called");
    assertTrue(tc.lastSent.toUpperCase().contains("ACTUATOR_COMMAND"));
    assertTrue(tc.lastSent.contains("nodeX"));
    assertTrue(tc.lastSent.contains("act1"));
    assertTrue(tc.lastSent.contains("TURN_ON") || tc.lastSent.contains("TURN_OFF"));
  }

  // ----- NEGATIVE TESTS -----

  @Test
  public void addSensor_negative_rejectsDuplicateType() {
    // add existing TEMPERATURE sensor to node to cause duplicate-type rejection
    ControlPanelLogic.NodeState ns = new ControlPanelLogic.NodeState("nD");
    entity.sensor.TemperatureSensor ts = new entity.sensor.TemperatureSensor("s_existing", 0.0, 100.0);
    ns.sensors.put(ts.getSensorId(), ts);
    cp.getNodes().put("nD", ns);

    boolean ok = cp.addSensor("nD", "TEMPERATURE", "s_new", 5.0, 50.0);
    assertFalse(ok, "addSensor should reject duplicate sensor type on same node");
    assertNull(tc.lastSent, "No JSON should be sent for rejected addSensor");
  }

  @Test
  public void removeSensor_negative_missingSensor() {
    boolean ok = cp.removeSensor("noNode", "noSensor");
    assertFalse(ok, "removeSensor should return false when node/sensor unknown");
    assertNull(tc.lastSent, "No JSON should be sent when remove is rejected");
  }

  @Test
  public void handleAlert_negative_deduplicatesRepeatedAlerts() {
    // capture stdout
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    System.setOut(new PrintStream(baos));

    String alertJson = "{\"messageType\":\"ALERT\",\"nodeID\":\"n1\",\"alert\":\"TEMP_OVER_MAX sensor=s1 value=100\"}";
    cp.handleIncomingJson(alertJson);
    cp.handleIncomingJson(alertJson);

    String out = baos.toString();
    int occurrences = out.split("! ALERT from node").length - 1;
    assertEquals(1, occurrences, "Alert should be printed only once for repeated identical ALERT messages");
  }
}
=======
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

