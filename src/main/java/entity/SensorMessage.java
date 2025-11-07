package entity;

import java.time.LocalDateTime;
import com.google.gson.Gson;
import util.SensorType;

public class SensorMessage {

  private String messageType;
  private SensorType type;              // "temperature", "humidity", "light", "co2"
  private String sensorId;          // unique identifier for the sensor
  private LocalDateTime timestamp;      // time the reading was taken
  private double value;             // the sensor reading value
  private String unit;              // unit of measurement

  public SensorMessage(SensorType type, String sensorId, LocalDateTime timestamp, double value,
      String unit) {
    
    this.messageType = "SENSOR_DATA";
    setType(type);
    setSensorId(sensorId);
    setTimestamp(timestamp);
    setValue(value);
    setUnit(unit);
  }

  //-------------- Getters and setters ---------------

  public String getmessageType() {
    return messageType;

  }

  public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
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
    if (type == null) {
      throw new IllegalArgumentException("Sensor type cannot be null");
    }
    this.type = type;
  }

  public void setSensorId(String sensorId) {
    if (sensorId == null || sensorId.isEmpty()) {
      throw new IllegalArgumentException("Sensor ID cannot be null or empty");
    }
    this.sensorId = sensorId;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp cannot be null");
    }
    this.timestamp = timestamp;
  }

  public void setValue(double value) {
    if (Double.isNaN(value)) {
      throw new IllegalArgumentException("Sensor value cannot be NaN");
    }
    this.value = value;
  }

  public void setUnit(String unit) {
    if (unit == null || unit.isEmpty()) {
      throw new IllegalArgumentException("Unit cannot be null or empty");
    }
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