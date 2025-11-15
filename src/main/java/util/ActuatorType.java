package util;

public enum ActuatorType {
  FAN("fan"),
  HEATER("heater"),
  HUMIDIFIER("humidifier"),
  CO2("co2"),
  LAMPS("lamps");

  private String actuatorType;

  ActuatorType(String actuatorType) {
    this.actuatorType = actuatorType;
  }

  public String getActuatorType() {
    return actuatorType;
  }
}
