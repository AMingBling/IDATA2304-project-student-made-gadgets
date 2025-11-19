package entity.sensor;

import java.time.LocalDateTime;

/**
 * Class representing a CO2 sensor.
 * <p>
 * The CO2Sensor class extends the generic Sensor class to specifically handle CO2
 * measurements. It initializes with a default value and provides methods to update   
 * and adjust the CO2 level.
 * </p>
 */
public class CO2Sensor extends Sensor {

  /**
   * Constructor for CO2Sensor. Min and max thresholds are specified in ppm. 800 and 1500 would be
   * natural values.
   *
   * @param sensorId     the id of the sensor
   * @param minThreshold minimum threshold value
   * @param maxThreshold maximum threshold value
   */
  public CO2Sensor(String sensorId,
      double minThreshold, double maxThreshold) {
    super(sensorId, "CO2", "ppm", minThreshold, maxThreshold);
    updateValue(1000);
  }

  /**
   * Update the CO2 sensor value to a new reading.
   * @param newValue the new CO2 value
   * 
   */
  @Override
  public void updateValue(double newValue) {
    this.value = newValue;
    this.timestamp = LocalDateTime.now();
  }

  /**
   * Adjust the CO2 sensor value by a delta.
   * @param delta the amount to adjust the CO2 value by
   * 
   */
  @Override
  public synchronized void adjustValue(double delta) {
    updateValue(getValue() + delta);
  }

  
  /**
   * Update the CO2 sensor value without parameters.
   * This method is required by the base Sensor class but does nothing by default.
   * 
   */
  @Override
  public synchronized void updateValue() {
    // Do nothing by default — humidity only changes when actuators run.
  }

  /**
   * Check if the CO2 level is above the maximum threshold .
   * @return true if above max threshold, false otherwise 
   */
  public boolean isAboveMax() {
    return getValue() > getMaxThreshold();
  }

  /**
   * Check if the CO2 level is below the minimum threshold.
   * @return true if below min threshold, false otherwise
   */
  public boolean isBelowMin() {
    return getValue() < getMinThreshold();
  }

}
