package entity;

public class Actuator {

  private String actuatorId;
  private String actuatorType;
  private boolean on = false;

  public Actuator(String actuatorId, String actuatorType) {
    setActuatorId(actuatorId);
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

  private void setActuatorType(String actuatorType) {
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

  public String getActuatorType() {
    return actuatorType;
  }

  public boolean isOn() {
    return on;
  }
  //----------------------------------------------


}
