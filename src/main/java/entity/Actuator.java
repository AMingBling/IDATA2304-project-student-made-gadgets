package entity;

import util.ActuatorType;
import util.SensorType;

public class Actuator {

  private String actuatorId;
  private SensorType sensorType;
  private ActuatorType actuatorType;
  private boolean on = false;

  public Actuator(String actuatorId, SensorType sensorType, ActuatorType actuatorType) {
    setActuatorId(actuatorId);
    setSensorType(sensorType);
    setActuatorType(actuatorType);
    this.on = false;
  }

  //-------------- Setters and Getters ---------------
  private void setActuatorId(String actuatorId) {
    if (actuatorId == null || actuatorId.isEmpty()) {
      throw new IllegalArgumentException("Actuator ID cannot be null or empty");
    }
    this.actuatorId = actuatorId;
  }

  private void setSensorType(SensorType sensorType) {
    if (sensorType == null) {
      throw new IllegalArgumentException("entity.Sensor type cannot be null");
    }
    this.sensorType = sensorType;
  }

  private void setActuatorType(ActuatorType actuatorType) {
    if (actuatorType == null) {
      throw new IllegalArgumentException("Actuator type cannot be null");
    }
    this.actuatorType = actuatorType;
  }

  public void setOn(boolean on) {
    this.on = on;
  }

  public String getActuatorId() {
    return actuatorId;
  }

  public SensorType getSensorType() {
    return sensorType;
  }

  public ActuatorType getActuatorType() {
    return actuatorType;
  }

  public boolean isOn() {
    return on;
  }
  //----------------------------------------------


}
