package entity;

import util.SensorType;

public class Actuator {

  private String actuatorId;
  private SensorType sensorType;
  private boolean on = false;
  private double targetMin;
  private double targetMax;

  public Actuator(String actuatorId, SensorType sensorType, double targetMin, double targetMax) {
    setActuatorId(actuatorId);
    setSensorType(sensorType);
    setTargetMin(targetMin);
    setTargetMax(targetMax);
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

  private void setTargetMin(double targetMin) {
    this.targetMin = targetMin;
  }

  private void setTargetMax(double targetMax) {
    this.targetMax = targetMax;
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

  public double getTargetMin() {
    return targetMin;
  }

  public double getTargetMax() {
    return targetMax;
  }

  public boolean isOn() {
    return on;
  }
  //----------------------------------------------


}
