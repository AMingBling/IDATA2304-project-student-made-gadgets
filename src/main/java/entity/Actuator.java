package entity;

/**
 * Class representing an Actuator.
 */
public class Actuator {

  private String actuatorId;
  private String actuatorType;
  private boolean on = false;

  /**
   * Constructor for Actuator.
   * @param actuatorId the id of the actuator
   * @param actuatorType the type of the actuator (ex. ventilation, heater
   */
  public Actuator(String actuatorId, String actuatorType) {
    setActuatorId(actuatorId);
    setActuatorType(actuatorType);
    this.on = false;
  }

  //-------------- Setters and Getters ---------------
  /**
   * Set actuator ID
   * @param actuatorId the id of the actuator
   */
  private void setActuatorId(String actuatorId) {
    if (actuatorId == null || actuatorId.isBlank()) {
      throw new IllegalArgumentException("Actuator ID cannot be null or empty");
    }
    this.actuatorId = actuatorId;
  }

  /**
   * Set actuator type
   * @param actuatorType the type of the actuator
   */
  private void setActuatorType(String actuatorType) {
    if (actuatorType == null || actuatorType.isBlank()) {
      throw new IllegalArgumentException("Actuator type cannot be null");
    }
    this.actuatorType = actuatorType;
  }

  /**
   * Set actuator state
   * @param on true if the actuator is on, false otherwise
   */
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
