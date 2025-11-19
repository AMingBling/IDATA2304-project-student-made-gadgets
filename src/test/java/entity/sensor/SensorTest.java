package entity.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;


/**
 * Tests for base Sensor behaviour using TemperatureSensor as a concrete instance.
 *
 * <p>The following is tested:</p>
 *
 * <b>Positive tests:</b>
 * <ul>
 *   <li>constructorAndGetters: verifies constructor sets id, type, unit and thresholds.</li>
 *   <li>updateAndAdjust_updateTimestampAndValue: verifies updateValue(double) and adjustValue(double) change value and timestamp.</li>
 *   <li>toReadingJson_containsExpectedProperties: verifies toReadingJson contains sensorId, type, value, unit and timestamp.</li>
 *   <li>isOutOfRange_reflectsThresholds: verifies isOutOfRange returns true when value outside thresholds.</li>
 * </ul>
 *
 * <b>Negative tests:</b>
 * <ul>
 *   <li>constructorInvalidThresholds_throws: verifies constructor throws when max < min.</li>
 *   <li>constructorInvalidId_throws: verifies constructor throws for empty/null id.</li>
 * </ul>
 *
 * @author Group 1
 * @version 2025-11-19
 */
public class SensorTest {

    /**
   * Verify constructor initializes id, type, unit and thresholds.
   *
   * <p>Expected outcome: getters return values passed to constructor and initial value is a number.</p>
   */
    @Test
  public void constructorAndGetters() {
    TemperatureSensor ts = new TemperatureSensor("s1", 0.0, 100.0);
    assertEquals("s1", ts.getSensorId());
    assertEquals("TEMPERATURE", ts.getSensorType());
    assertEquals("°C", ts.getUnit());
    assertEquals(0.0, ts.getMinThreshold(), 1e-9);
    assertEquals(100.0, ts.getMaxThreshold(), 1e-9);
    assertNotNull(ts.getTimestamp());
    assertFalse(Double.isNaN(ts.getValue()));
  }

  /**
   * Verify updateValue(double) and adjustValue(double) modify value and update timestamp.
   *
   * <p>Expected outcome: value equals provided value after update, then changes by delta after adjust; timestamp advances.</p>
   */
  @Test
  public void updateAndAdjust_updateTimestampAndValue() throws InterruptedException {
    TemperatureSensor ts = new TemperatureSensor("s2", -50.0, 150.0);
    LocalDateTime before = ts.getTimestamp();

    Thread.sleep(5);
    ts.updateValue(25.75);
    assertEquals(25.75, ts.getValue(), 1e-9);
    assertTrue(ts.getTimestamp().isAfter(before));

    double after = ts.getValue();
    Thread.sleep(2);
    ts.adjustValue(1.25);
    assertEquals(after + 1.25, ts.getValue(), 1e-9);
    assertTrue(ts.getTimestamp().isAfter(before));
  }

  /**
   * Verify toReadingJson contains required properties.
   *
   * <p>Expected outcome: resulting JsonObject has sensorId, type, value, unit and timestamp.</p>
   */
  @Test
  public void toReadingJson_containsExpectedProperties() {
    TemperatureSensor ts = new TemperatureSensor("s3", -10.0, 50.0);
    ts.updateValue(12.0);
    JsonObject jo = ts.toReadingJson();
    assertTrue(jo.has("sensorId"));
    assertTrue(jo.has("type"));
    assertTrue(jo.has("value"));
    assertTrue(jo.has("unit"));
    assertTrue(jo.has("timestamp"));
    assertEquals("s3", jo.get("sensorId").getAsString());
  }
    
  /**
   * Verify isOutOfRange returns true when value outside thresholds and false when inside.
   *
   * <p>Expected outcome: value above max or below min yields true.</p>
   */
  @Test
  public void isOutOfRange_reflectsThresholds() {
    TemperatureSensor ts = new TemperatureSensor("s4", 0.0, 10.0);
    ts.updateValue(11.0);
    assertTrue(ts.isOutOfRange(), "Value above max should be out of range");

    ts.updateValue(5.0);
    assertFalse(ts.isOutOfRange(), "Value within range should not be out of range");

    ts.updateValue(-1.0);
    assertTrue(ts.isOutOfRange(), "Value below min should be out of range");
  }

  /**
   * Verify constructor throws when max threshold is less than min threshold.
   *
   * <p>Expected outcome: IllegalArgumentException is thrown.</p>
   */
  @Test
  public void constructorInvalidThresholds_throws() {
    assertThrows(IllegalArgumentException.class, () -> new TemperatureSensor("bad", 10.0, 5.0));
  }

  /**
   * Verify constructor throws for invalid (empty) sensor id.
   *
   * <p>Expected outcome: IllegalArgumentException is thrown for blank id.</p>
   */
  @Test
  public void constructorInvalidId_throws() {
    assertThrows(IllegalArgumentException.class, () -> new TemperatureSensor("", 0.0, 10.0));
    assertThrows(IllegalArgumentException.class, () -> new TemperatureSensor("   ", 0.0, 10.0));
  }
}
