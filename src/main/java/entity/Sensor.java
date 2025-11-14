package entity;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import util.SensorType;

public class Sensor {

  private String sensorId;
  private SensorType sensorType;
  private double value;
  private String unit;
  private double minThreshold;
  private double maxThreshold;
  private LocalDateTime timestamp;

  public Sensor(String sensorId, SensorType sensorType, String unit,
      double minThreshold, double maxThreshold) {
    setSensorId(sensorId);
    setSensorType(sensorType);
    setUnit(unit);
    setMinThreshold(minThreshold);
    setMaxThreshold(maxThreshold);
    this.value = 0.0;
    this.timestamp = LocalDateTime.now();
  }

  //-------------- Setters and Getters ---------------

  private void setSensorId(String sensorId) {
    if (sensorId == null || sensorId.isEmpty()) {
      throw new IllegalArgumentException("entity.Sensor ID cannot be null or empty");
    }
    this.sensorId = sensorId;
  }

  private void setSensorType(SensorType sensorType) {
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

  public SensorType getSensorType() {
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
