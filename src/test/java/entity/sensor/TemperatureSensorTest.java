package entity.sensor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test class for TemperatureSensor.
 *
 * <p>The following is tested:</p>
 *
 * <b>Positive tests:</b>
 *
 * <ul>
 *   <li>testConstructorAndGetters: verifies that the constructor initializes all fields correctly and that getters return expected values.</li>
 * </ul>
 *
 * @author Group 1
 * @version 2025-11-19
 */

public class TemperatureSensorTest {

    /**
     * Test constructor and getters of TemperatureSensor.
     *
     * <p>Expected outcome: all fields are initialized correctly.</p>
     */
    @Test
    public void testConstructorAndGetters() {
        TemperatureSensor sensor = new TemperatureSensor("temp1", 15.0, 30.0);
        assertEquals("temp1", sensor.getSensorId());
        assertEquals("TEMPERATURE", sensor.getSensorType());
        assertEquals("°C", sensor.getUnit());
        assertEquals(15.0, sensor.getMinThreshold());
        assertEquals(30.0, sensor.getMaxThreshold());
        assertEquals(20.0, sensor.getValue());
        assertNotNull(sensor.getTimestamp());
    }

    /**
   * Verify updateValue(double) sets the value and updates timestamp.
   *
   * <p>Expected outcome: value equals provided value and timestamp is updated to a time after the previous timestamp.</p>
   */
    @Test
  public void updateValueWithParameter_setsValueAndUpdatesTimestamp() throws InterruptedException {
    TemperatureSensor ts = new TemperatureSensor("t2", 0.0, 100.0);
    LocalDateTime before = ts.getTimestamp();

    Thread.sleep(5);

    ts.updateValue(25.5);
    assertEquals(25.5, ts.getValue(), 1e-6);
    assertTrue(ts.getTimestamp().isAfter(before), "Timestamp should be updated after updateValue(double)");
  }

  /**
   * Verify adjustValue(delta) changes the value by delta.
   *
   * <p>Expected outcome: new value equals old value + delta.</p>
   */
  @Test
  public void adjustValue_changesValueByDelta() {
    TemperatureSensor ts = new TemperatureSensor("t3", -50.0, 150.0);
    double before = ts.getValue();
    ts.adjustValue(2.25);
    assertEquals(before + 2.25, ts.getValue(), 1e-6);
  }

    /**
     * Verify updateValue() without parameters updates timestamp but leaves value unchanged.
     *
     *<p>Expected outcome: timestamp is advanced and value is not NaN.</p>
     */
  @Test
  public void parameterlessUpdateUpdatesTimestampOrValue() throws InterruptedException {
    TemperatureSensor ts = new TemperatureSensor("t4", -10, 50.0);
    LocalDateTime before = ts.getTimestamp();

    Thread.sleep(5);

    ts.updateValue();
    assertTrue(ts.getTimestamp().isAfter(before));
    assertFalse(Double.isNaN(ts.getValue()), "Value should remain a number after update");
  }

    /**
   * Verify that the boolean threshold helpers report correctly.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>isAboveMax() returns true when the sensor value is greater than maxThreshold.</li>
   *   <li>isBelowMin() returns true when the sensor value is less than minThreshold.</li>
   *   <li>When value is above max, isBelowMin() must be false and vice versa.</li>
   * </ul>
   */
  @Test
  public void thresholdChecks_isAboveMaxAndIsBelowMin() {
    TemperatureSensor ts = new TemperatureSensor("t5", 0.0, 100.0);
    ts.updateValue(150.0);
    assertTrue(ts.getValue() > ts.getMaxThreshold(), "Value should be above max threshold");
    assertFalse(ts.getValue() < ts.getMinThreshold(), "Value should not be below min threshold");

    ts.updateValue(-20.0);
    assertTrue(ts.getValue() < ts.getMinThreshold(), "Value should be below min threshold");
    assertFalse(ts.getValue() > ts.getMaxThreshold(), "Value should not be above max threshold");
  }
}
