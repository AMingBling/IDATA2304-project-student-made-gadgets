package util;

public enum SensorType {
  TEMPERATURE("temperature"),
  HUMIDITY("humidity"),
  LIGHT("light"),
  CO2("co2");

  private String sensorType;

  SensorType(String sensorType) {
    this.sensorType = sensorType;
  }

  public String getSensorType() {
    return sensorType;
  }
}
