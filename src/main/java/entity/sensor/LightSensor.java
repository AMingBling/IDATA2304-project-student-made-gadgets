package entity.sensor;

import java.time.LocalDateTime;

/**
 * Class representing a Light Sensor.
 * <p>
 * The LightSensor class extends the generic Sensor class to specifically handle light
 * measurements. It initializes with a default value and provides methods to update and adjust
 * the light level.
 * 
 */
public class LightSensor extends Sensor {

  /**
   * Constructor for LightSensor. Min and max thresholds are specified in lux. 1000 and 20 000 would
   * be natural values.
   *
   * @param sensorId     the id of the sensor
   * @param minThreshold minimum threshold value
   * @param maxThreshold maximum threshold value
   */
  public LightSensor(String sensorId,
      double minThreshold, double maxThreshold) {
    super(sensorId, "LIGHT", "lux", minThreshold, maxThreshold);
    updateValue(15000);
  }

  /**
   * Update the Light sensor value to a new reading.
   * @param newValue the new Light value
   * 
   */
  @Override
  public void updateValue(double newValue) {
    this.value = newValue;
    this.timestamp = LocalDateTime.now();
  }

  /**
   * Adjust the Light sensor value by a delta.
   * @param delta the amount to adjust the Light value by
   * 
   */
  @Override
  public synchronized void adjustValue(double delta) {
    updateValue(getValue() + delta);
  }

  /**
   * Update the Light sensor value without parameters.
   * This method is required by the base Sensor class but does nothing by default.
   * 
   */
  @Override
  public synchronized void updateValue() {
    // Do nothing by default — humidity only changes when actuators run.
  }




}
