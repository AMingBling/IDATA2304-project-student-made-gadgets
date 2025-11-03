package entity;

import java.time.LocalDateTime;
import com.google.gson.Gson;

public class SensorMessage {

  private SensorType type;              // "temperature", "humidity", "light", "co2"
  private String sensorId;          // unique identifier for the sensor
  private LocalDateTime timestamp;      // time the reading was taken
  private double value;             // the sensor reading value
  private String unit;              // unit of measurement

  public SensorMessage(SensorType type, String sensorId, LocalDateTime timestamp, double value,
      String unit) {
    
    setType(type);
    setSensorId(sensorId);
    setTimestamp(timestamp);
    setValue(value);
    setUnit(unit);
  }

  //-------------- Getters and setters ---------------
  public SensorType getType() {
    return type;
  }

  public String getSensorId() {
    return sensorId;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public double getValue() {
    return value;
  }

  public String getUnit() {
    return unit;
  }

  public void setType(SensorType type) {
    this.type = type;
  }

  public void setSensorId(String sensorId) {
    this.sensorId = sensorId;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }
  //-------------------------------------------------

  public static SensorMessage fromJson(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, SensorMessage.class);
  }

  public String toJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }
}