package entity.actuator;

import static org.junit.jupiter.api.Assertions.*;

import entity.sensor.Sensor;
import entity.sensor.TemperatureSensor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Test class for Actuator base behaviour.
 *
 * <p>The following is tested:</p>
 *
 * <b>Positive tests:</b>
 * <ul>
 *   <li>constructorAndGetters: verifies id/type getters return constructor values.</li>
 *   <li>setOn_togglesState: verifies setOn changes isOn state.</li>
 *   <li>applyEffect_isInvoked: verifies a concrete actuator implementation receives applyEffect calls.</li>
 * </ul>
 *
 * <b>Negative tests:</b>
 * <ul>
 *   <li>constructorRejectsNullOrEmptyId: verifies constructor throws on null/empty id.</li>
 *   <li>constructorRejectsNullType: verifies constructor throws on null type.</li>
 * </ul>
 *
 * @author Group 1
 * @version 2025-11-19
 */
public class ActuatorTest {

  
  private static class TestActuator extends Actuator {
    public boolean applied = false;

    public TestActuator(final String id, final String type) {
      super(id, type);
    }

    @Override
    public void applyEffect(final List<Sensor> sensors) {
      applied = true;
      
      if (sensors != null && !sensors.isEmpty() && sensors.get(0) instanceof TemperatureSensor) {
        TemperatureSensor ts = (TemperatureSensor) sensors.get(0);
        ts.adjustValue(0.5);
      }
    }
  }

  /**
   * Verify constructor initializes id and type.
   *
   * <p>Expected outcome: getters return values passed to constructor.</p>
   */
  @Test
    public void constructorAndGetters() {
    TestActuator ta = new TestActuator("a1", "TestType");
    assertEquals("a1", ta.getActuatorId());
    assertEquals("TestType", ta.getActuatorType());
  }

  /**
   * Verify setOn changes isOn state.
   *
   * <p>Expected outcome: isOn reflects last setOn call.</p>
   */
  @Test
    public void setOn_togglesState() {
    TestActuator ta = new TestActuator("a2", "TestType");
    ta.setOn(true);
    assertTrue(ta.isOn());
    ta.setOn(false);
    assertFalse(ta.isOn());
  }

  /**
   * Verify applyEffect is invoked on actuator.
   *
   * <p>Expected outcome: applyEffect sets applied flag to true and affects sensor value.</p>
   */
  @Test
    public void applyEffect_isInvoked() {
    TestActuator ta = new TestActuator("a3", "TestType");
    TemperatureSensor ts = new TemperatureSensor("s1", 0.0, 100.0);
    double before = ts.getValue();
    
    List<Sensor> sensors = new ArrayList<>();
    sensors.add(ts);
    
    ta.applyEffect(sensors);
    assertTrue(ta.applied, "applyEffect should set applied to true");
    assertEquals(before + 0.5, ts.getValue(), 1e-9, "TemperatureSensor value should be adjusted by 0.5");
  }

  /**
   * Verify constructor throws on null/empty id.
   *
   * <p>Expected outcome: IllegalArgumentException is thrown.</p>
   */
  @Test
    public void constructorRejectsNullOrEmptyId() {
    assertThrows(IllegalArgumentException.class, () -> new TestActuator(null, "TestType"));
    assertThrows(IllegalArgumentException.class, () -> new TestActuator("", "TestType"));
    assertThrows(IllegalArgumentException.class, () -> new TestActuator("   ", "TestType"));
  }

  /**
   * Verify constructor throws on null type.
   *
   * <p>Expected outcome: IllegalArgumentException is thrown.</p>
   */
  @Test
  public void constructorRejectsNullType() {
    assertThrows(IllegalArgumentException.class, () -> new TestActuator("act-x", null));
  }
}