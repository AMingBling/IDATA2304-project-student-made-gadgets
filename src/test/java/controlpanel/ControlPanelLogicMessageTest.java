package controlpanel;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Message-focused tests: only verify that ControlPanelLogic produces/sends correct JSON messages.
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

  @Test
  public void removeSensor_missing_doesNotSend() {
    boolean ok = cp.removeSensor("noNode", "noSensor");
    assertFalse(ok);
    assertNull(tc.lastSent, "No JSON should be sent for invalid removeSensor");
  }
}
