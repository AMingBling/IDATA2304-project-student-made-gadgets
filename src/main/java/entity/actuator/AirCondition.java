package entity.actuator;

import entity.sensor.Sensor;
import java.util.List;

/**
 * AirCondition actuator that lowers temperature sensor readings while enabled.
 * <p>
 * When the actuator is switched on, {@link #applyEffect(List)} decreases the value
 * of any sensor whose {@code getSensorType()} equals "TEMPERATURE" by 1.0 each tick.
 * The method is a no-op when the actuator is off.        
 * </p>
 * Usage notes:
 * <ul>
 *   <li>Create with {@code new AirCondition(id)}.</li> 
 *  <li>Enable/disable with {@code setOn(true)} / {@code setOn(false)}.</li>
 *  <li>Call {@code applyEffect(List)} from the node tick loop to apply the effect.</li>
 * </ul>
 */
public class AirCondition extends Actuator {

  public AirCondition(String id) {
    super(id, "AIRCON");
  }

  /**
   * Apply the air conditioning effect to temperature sensors in the provided list.
   * @param sensors list of sensors to potentially affect
   * 
   */
  @Override
  public void applyEffect(List<Sensor> sensors) {
    if (!isOn()) {
      return;
    }

    for (Sensor s : sensors) {
      if ("TEMPERATURE".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(-1.0);
      }
    }
    System.out.println("[AirCondition] applied temp effect: -1.0");
  }


}
