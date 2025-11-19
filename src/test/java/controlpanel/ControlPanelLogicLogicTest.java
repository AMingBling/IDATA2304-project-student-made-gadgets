
package controlpanel;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

/**
 * Test class for ControlPanelLogic.
 *
 * <p>The following is tested:</p>
 *
 * <b>Positive tests:</b>
 *
 * <ul>
 *   <li>addSensor_positive_sendsAddSensorJson: verifies that addSensor returns true and sends an ADD_SENSOR JSON payload.</li>
 *   <li>removeSensor_positive_sendsRemoveSensorJson: verifies that removeSensor returns true for existing sensor and sends REMOVE_SENSOR JSON.</li>
 *   <li>setActuatorState_positive_sendsActuatorCommand: verifies that setActuatorState sends an ACTUATOR_COMMAND JSON with correct fields.</li>
 * </ul>
 *
 * <b>Negative tests:</b>
 *
 * <ul>
 *   <li>addSensor_negative_rejectsDuplicateType: verifies that adding a duplicate sensor type to a node is rejected and no JSON is sent.</li>
 *   <li>removeSensor_negative_missingSensor: verifies that removing a non-existing sensor returns false and no JSON is sent.</li>
 *   <li>handleAlert_negative_deduplicatesRepeatedAlerts: verifies that identical ALERT messages are deduplicated when printed to stdout.</li>
 * </ul>
 *
 * @author Group 1
 * @version 2025-11-18
 */
public class ControlPanelLogicLogicTest {

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

  /**
   * Test that addSensor returns true and sends an ADD_SENSOR JSON payload.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>addSensor(...) returns true for a new sensor.</li>
   *   <li>ControlPanelCommunication.sendJson is invoked with JSON containing "ADD_SENSOR" and the sensor id.</li>
   * </ul>
   */
  @Test
  public void addSensor_positive_sendsAddSensorJson() {
    boolean ok = cp.addSensor("n1", "TEMPERATURE", "s1", 10.0, 30.0);
    assertTrue(ok, "addSensor should succeed for a new sensor");
    assertNotNull(tc.lastSent, "sendJson should have been called");
    assertTrue(tc.lastSent.toUpperCase().contains("ADD_SENSOR"), "Should send ADD_SENSOR");
    assertTrue(tc.lastSent.contains("s1"), "Should include sensorId");
  }

  /**
   * Test that removeSensor returns true for an existing sensor and sends REMOVE_SENSOR JSON.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>removeSensor(...) returns true when the sensor exists in cached node state.</li>
   *   <li>A REMOVE_SENSOR JSON is sent containing the sensor id.</li>
   * </ul>
   */
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

  /**
   * Test that setActuatorState sends an ACTUATOR_COMMAND JSON with expected fields.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>ControlPanelCommunication.sendJson is invoked with JSON containing "ACTUATOR_COMMAND".</li>
   *   <li>The JSON contains node id, actuator id and either TURN_ON or TURN_OFF.</li>
   * </ul>
   */
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

  /**
   * Test that adding a duplicate sensor type to a node is rejected.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>addSensor returns false when node already has a sensor of the same type.</li>
   *   <li>No JSON is sent to ControlPanelCommunication for the rejected request.</li>
   * </ul>
   */
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

  /**
   * Test that removing a sensor from a missing node or unknown sensor fails.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>removeSensor returns false when node or sensor does not exist.</li>
   *   <li>No JSON is sent to ControlPanelCommunication.</li>
   * </ul>
   */
  @Test
  public void removeSensor_negative_missingSensor() {
    boolean ok = cp.removeSensor("noNode", "noSensor");
    assertFalse(ok, "removeSensor should return false when node/sensor unknown");
    assertNull(tc.lastSent, "No JSON should be sent when remove is rejected");
  }

  /**
   * Test that repeated identical ALERT messages are deduplicated when printed.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>Printing of identical ALERT messages happens only once.</li>
   *   <li>Subsequent identical alerts are ignored (deduplicated).</li>
   * </ul>
   */
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