package entity.sensor;

import java.time.LocalDateTime;


/**
 * Class representing a Humidity Sensor.
 * <p>
 * The HumiditySensor class extends the generic Sensor class to specifically handle humidity    
 * measurements. It initializes with a default value and provides methods to update
 * and adjust the humidity level.
 * </p>
 */
public class HumiditySensor extends Sensor {

  /**
   * Constructor for HumiditySensor. Min and max thresholds are specified in percentage. 50 and 85
   * would be natural values.
   *
   * @param sensorId     the id of the sensor
   * @param minThreshold minimum threshold value
   * @param maxThreshold maximum threshold value
   */
  public HumiditySensor(String sensorId,
      double minThreshold, double maxThreshold) {

    super(sensorId, "HUMIDITY", "%", minThreshold, maxThreshold);
    // startverdien i veksthus
    updateValue(50.0);
  }

  /**
   * Update the Humidity sensor value to a new reading.
   * @param newValue the new Humidity value
   */
  @Override
  public void updateValue(double newValue) {
    this.value = newValue;
    this.timestamp = LocalDateTime.now();
  }

  /**
   * Adjust the Humidity sensor value by a delta.
   * @param delta the amount to adjust the Humidity value by
   */
  @Override
  public synchronized void adjustValue(double delta) {
    updateValue(getValue() + delta);
  }

  /**
   * Update the Humidity sensor value without parameters.
   * This method is required by the base Sensor class but does nothing by default.
   * 
   */
  @Override
  public synchronized void updateValue() {
    // Do nothing by default — humidity only changes when actuators run.
  }

  /**
   * Check if the Humidity level is above the maximum threshold .
   * 
   * @return true if above max threshold, false otherwise
   */
  public boolean isAboveMax() {
    return getValue() > getMaxThreshold();
  }

  /**
   *  Check if the Humidity level is below the minimum threshold.
   * @return true if below min threshold, false otherwise
   */
  public boolean isBelowMin() {
    return getValue() < getMinThreshold();
  }
}