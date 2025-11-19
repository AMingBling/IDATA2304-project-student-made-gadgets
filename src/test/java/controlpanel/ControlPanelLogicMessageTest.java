package controlpanel;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.function.Consumer;

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
 * Message-focused tests: only verify that ControlPanelLogic produces/sends correct JSON messages.
 *
 * <p>The following is tested:</p>
 *
 * <b>Positive tests:</b>
 * <ul>
 *   <li>addSensor_sendsAddSensorMessage: verifies that addSensor sends an ADD_SENSOR JSON payload with expected fields.</li>
 *   <li>setActuatorState_sendsActuatorCommand: verifies that setActuatorState sends ACTUATOR_COMMAND JSON with node and actuator identifiers and command.</li>
 * </ul>
 *
 * <b>Negative tests:</b>
 * <ul>
 *   <li>addSensor_duplicateType_doesNotSend: verifies that adding a duplicate sensor type does not send JSON.</li>
 *   <li>removeSensor_missing_doesNotSend: verifies that removing a missing sensor/node does not send JSON.</li>
 * </ul>
 *
 * @author Group 1
 * @version 2025-11-19
 */
public class ControlPanelLogicMessageTest {

  // lightweight TestComm that captures last sent JSON
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

  private ControlPanelLogic cp;
  private TestControlPanelCommunication tc;

  private Gson buildGson() {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, t, c) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, t, c) -> LocalDateTime.parse(json.getAsString()))
        .create();
  }

  private TestControlPanelCommunication injectTestComm(ControlPanelLogic cp) throws Exception {
    TestControlPanelCommunication t = new TestControlPanelCommunication(s -> {}, buildGson(), "testCP");
    Field f = ControlPanelLogic.class.getDeclaredField("comm");
    f.setAccessible(true);
    f.set(cp, t);
    return t;
  }

  @BeforeEach
  public void setup() throws Exception {
    cp = new ControlPanelLogic("testCP");
    tc = injectTestComm(cp);
  }

  // ----- Positive (should send) -----

  /**
   * Test that addSensor sends an ADD_SENSOR JSON message with expected fields.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>addSensor(...) returns true.</li>
   *   <li>ControlPanelCommunication.sendJson is invoked with JSON containing "ADD_SENSOR", the node id, sensor id and sensor type.</li>
   * </ul>
   */
  @Test
  public void addSensor_sendsAddSensorMessage() {
    boolean ok = cp.addSensor("node1", "TEMPERATURE", "s1", 5.0, 40.0);
    assertTrue(ok);
    assertNotNull(tc.lastSent, "Expected an ADD_SENSOR JSON to be sent");
    assertTrue(tc.lastSent.toUpperCase().contains("ADD_SENSOR"));
    assertTrue(tc.lastSent.contains("\"nodeID\"") && tc.lastSent.contains("node1"));
    assertTrue(tc.lastSent.toUpperCase().contains("TEMPERATURE"));
    assertTrue(tc.lastSent.contains("s1"));
  }

  /**
   * Test that setActuatorState sends an ACTUATOR_COMMAND JSON with node and actuator identifiers and the correct command.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>ControlPanelCommunication.sendJson is invoked with JSON containing "ACTUATOR_COMMAND".</li>
   *   <li>JSON contains the node id, actuator id and either TURN_ON or TURN_OFF.</li>
   * </ul>
   */
  @Test
  public void setActuatorState_sendsActuatorCommand() {
    cp.setActuatorState("nX", "a1", true);
    assertNotNull(tc.lastSent);
    assertTrue(tc.lastSent.toUpperCase().contains("ACTUATOR_COMMAND"));
    assertTrue(tc.lastSent.contains("nX"));
    assertTrue(tc.lastSent.contains("a1"));
    assertTrue(tc.lastSent.contains("TURN_ON") || tc.lastSent.contains("TURN_OFF"));
  }

  // ----- Negative (should not send) -----

  /**
   * Test that attempting to add a sensor of a type that already exists on the node is rejected and no JSON is sent.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>addSensor(...) returns false when a sensor of the same type already exists on the node.</li>
   *   <li>No JSON is sent to ControlPanelCommunication.</li>
   * </ul>
   */
  @Test
  public void addSensor_duplicateType_doesNotSend() throws Exception {
    // prepare node with existing temperature sensor to trigger rejection
    ControlPanelLogic.NodeState ns = new ControlPanelLogic.NodeState("nodeA");
    ns.sensors.put("existingTemp", new entity.sensor.TemperatureSensor("existingTemp", 0, 100));
    cp.getNodes().put("nodeA", ns);

    boolean ok = cp.addSensor("nodeA", "TEMPERATURE", "sNew", 1.0, 10.0);
    assertFalse(ok);
    assertNull(tc.lastSent, "No JSON should be sent for rejected duplicate addSensor");
  }

  /**
   * Test that removing a sensor for a missing node or unknown sensor returns false and does not send JSON.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>removeSensor returns false for unknown node/sensor.</li>
   *   <li>No JSON is sent to ControlPanelCommunication.</li>
   * </ul>
   */
  @Test
  public void removeSensor_missing_doesNotSend() {
    boolean ok = cp.removeSensor("noNode", "noSensor");
    assertFalse(ok);
    assertNull(tc.lastSent, "No JSON should be sent for invalid removeSensor");
  }
}
