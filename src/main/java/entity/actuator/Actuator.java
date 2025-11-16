package entity.actuator;

import java.util.List;

import entity.sensor.Sensor;

/**
 * Abstract Actuator class representing a generic actuator in the system.
 */
public abstract class Actuator {

  private String actuatorId;
  private String actuatorType;
  private boolean on = false;

  /**
   * Constructor for Actuator.
   *
   * @param actuatorId   unique identifier for the actuator
   * @param actuatorType type of the actuator (e.g., Heater, AirCondition)
   */
  public Actuator(String actuatorId, String actuatorType) {
    setActuatorId(actuatorId);
    setActuatorType(actuatorType);
    this.on = false;
  }

  //-------------- Setters and Getters ---------------

  /**
   * Sets the actuator ID.
   *
   * @param actuatorId unique identifier for the actuator
   */
  private void setActuatorId(String actuatorId) {
    if (actuatorId == null || actuatorId.isEmpty()) {
      throw new IllegalArgumentException("Actuator ID cannot be null or empty");
    }
    this.actuatorId = actuatorId;
  }

  /**
   * Sets the actuator type.
   *
   * @param actuatorType type of the actuator
   */
  private void setActuatorType(String actuatorType) {
    if (actuatorType == null) {
      throw new IllegalArgumentException("Actuator type cannot be null");
    }
    this.actuatorType = actuatorType;
  }

  /**
   * Sets the on/off state of the actuator.
   *
   * @param on true to turn on, false to turn off
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

  /**
   * Applies the effect of the actuator on the provided sensors.
   *
   * @param sensors list of sensors to be affected
   */
  public abstract void applyEffect(List<Sensor> sensors);
}
