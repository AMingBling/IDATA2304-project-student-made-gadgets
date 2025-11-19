package entity.sensor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
  public void updateValueWithParameter_setsValueAndUpdatesTimestamp() throws InterruptedException {
    TemperatureSensor ts = new TemperatureSensor("t2", 0.0, 100.0);
    LocalDateTime before = ts.getTimestamp();

    Thread.sleep(5);

    ts.updateValue(25.5);
    assertEquals(25.5, ts.getValue(), 1e-6);
    assertTrue(ts.getTimestamp().isAfter(before), "Timestamp should be updated after updateValue(double)");
  }
    
}
