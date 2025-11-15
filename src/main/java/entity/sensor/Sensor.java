package entity.sensor;

import com.google.gson.Gson;
import java.time.LocalDateTime;


public abstract class Sensor {

  protected String sensorId;
  protected String sensorType;
  protected double value;
  protected String unit;
  protected double minThreshold;
  protected double maxThreshold;
  protected LocalDateTime timestamp;
  private static final long serialVersionUID = 1L;

  public Sensor(String sensorId, String sensorType, String unit,
      double minThreshold, double maxThreshold) {

    if (sensorId == null || sensorId.isBlank()) {
      throw new IllegalArgumentException("Sensor ID cannot be null or empty");
    }
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

  private void setSensorId(String sensorId) {
    if (sensorId == null || sensorId.isEmpty()) {
      throw new IllegalArgumentException("entity.Sensor ID cannot be null or empty");
    }
    this.sensorId = sensorId;
  }

  private void setSensorType(String sensorType) {
    if (sensorType == null) {
      throw new IllegalArgumentException("entity.Sensor type cannot be null");
    }
    this.sensorType = sensorType;
  }

  private void setUnit(String unit) {
    if (unit == null || unit.isEmpty()) {
      throw new IllegalArgumentException("Unit cannot be null or empty");
    }
    this.unit = unit;
  }

  private void setMinThreshold(double minThreshold) {
    this.minThreshold = minThreshold;
  }

  private void setMaxThreshold(double maxThreshold) {
    this.maxThreshold = maxThreshold;
  }


  public String getSensorId() {
    return sensorId;
  }

  public String getSensorType() {
    return sensorType;
  }

  public double getValue() {
    return value;
  }

  public String getUnit() {
    return unit;
  }

  public double getMinThreshold() {
    return minThreshold;
  }

  public double getMaxThreshold() {
    return maxThreshold;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public abstract void updateValue();
  //-------------------------------------------------

  /**
   * Convert Sensor object to JSON string
   * @return JSON representation of the Sensor object
   */
  public String toJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }

  /**
   * Create Sensor object from JSON string
   * @param json JSON representation of a Sensor object
   * @return Sensor object
   */
  public static Sensor fromJson(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, Sensor.class);
  }
}
