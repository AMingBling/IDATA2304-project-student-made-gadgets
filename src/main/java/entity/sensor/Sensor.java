package entity.sensor;

import com.google.gson.Gson;
import java.time.LocalDateTime;

/**
 * Abstract class representing a generic Sensor.
 * Includes common attributes and methods for all sensor types.
 *
 */
public abstract class Sensor {

  protected String sensorId;
  protected String sensorType;
  protected double value;
  protected String unit;
  protected double minThreshold;
  protected double maxThreshold;
  protected LocalDateTime timestamp;

  /**
   * Constructor for Sensor
   *
   * @param sensorId     the id of the sensor
   * @param sensorType   the type of the sensor (ex. temperature, humidity)
   * @param unit         the unit of measurement (ex. °C, %)
   * @param minThreshold minimum threshold value
   * @param maxThreshold maximum threshold value
   */
  public Sensor(String sensorId, String sensorType, String unit,
      double minThreshold, double maxThreshold) {

    if (maxThreshold < minThreshold) {
      throw new IllegalArgumentException("Max threshold cannot be less than min threshold");
    }

    this.setSensorId(sensorId);
    this.setSensorType(sensorType);
    this.setUnit(unit);
    this.setMinThreshold(minThreshold);
    this.setMaxThreshold(maxThreshold);
    this.timestamp = LocalDateTime.now();
  }

  //-------------- Setters and Getters ---------------

  /**
   * Set sensor ID
   *
   * @param sensorId the id of the sensor
   */
  private void setSensorId(String sensorId) {
    if (sensorId == null || sensorId.isBlank()) {
      throw new IllegalArgumentException("entity.Sensor ID cannot be null or empty");
    }
    this.sensorId = sensorId;
  }

  /**
   * Set sensor type
   *
   * @param sensorType the type of the sensor
   */
  private void setSensorType(String sensorType) {
    if (sensorType == null || sensorType.isBlank()) {
      throw new IllegalArgumentException("entity.Sensor type cannot be null");
    }
    this.sensorType = sensorType;
  }

  /**
   * Set unit of measurement
   *
   * @param unit the unit of measurement
   */
  private void setUnit(String unit) {
    if (unit == null || unit.isBlank()) {
      throw new IllegalArgumentException("Unit cannot be null or empty");
    }
    this.unit = unit;
  }

  /**
   * Set minimum threshold
   *
   * @param minThreshold minimum threshold value
   */
  private void setMinThreshold(double minThreshold) {
    this.minThreshold = minThreshold;
  }

  /**
   * Set maximum threshold
   *
   * @param maxThreshold maximum threshold value
   */
  private void setMaxThreshold(double maxThreshold) {
    this.maxThreshold = maxThreshold;
  }


  /**
   * Get sensor ID
   * @return sensor ID
   */
  public String getSensorId() {
    return sensorId;
  }

  /**
   * Get sensor type
   * @return sensor type
   */
  public String getSensorType() {
    return sensorType;
  }

  /**
   * Get current sensor value
   * @return current sensor value
   */
  public synchronized double getValue() {
    return value;
  }

  /**
   * Update the sensor value to a new reading.
   * @param newValue the new sensor value
   */
  public synchronized void updateValue(double newValue) {
    this.value = newValue;
    this.timestamp = LocalDateTime.now();
  }

  /**
   * Adjust the sensor value by a delta.
   * @param delta the amount to adjust the sensor value by
   */
  public synchronized void adjustValue(double delta) {
    this.value += delta;
    this.timestamp = LocalDateTime.now();
  }






  /**
   * Get unit of measurement
   * @return unit of measurement
   */
  public String getUnit() {

    return unit;
  }


  /**
   * Get minimum threshold value
   * @return minimum threshold value
   */
  public double getMinThreshold() {
    return minThreshold;
  }

  /**
   * Get maximum threshold value
   * @return maximum threshold value
   */
  public double getMaxThreshold() {

    return maxThreshold;
  }

  /**
   * Get timestamp of the last sensor value update
   * @return timestamp of the last update
   */
  public LocalDateTime getTimestamp() {

    return timestamp;
  }

  /**
   * Abstract method to update the sensor value.
   * 
   */
  public abstract void updateValue();





  





}
