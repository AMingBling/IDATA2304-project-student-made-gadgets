package entity.actuator;

import java.util.List;
import entity.sensor.Sensor;

/**
 * Heater actuator that increases temperature sensor readings while enabled.
 *
 * <p>
 * Instances represent a heater attached to a node. When the heater is switched on,
 * {@link #applyEffect(java.util.List)} increases the value of any sensor whose
 * {@code getSensorType()} equals "TEMPERATURE" by the configured {@code heatDelta}
 * each tick.
 * </p>
 *
 * Usage notes:
 * <ul>
 *   <li>Create with {@code new Heater(id)} to use the default delta, or
 *       {@code new Heater(id, heatDelta)} to specify a custom effect size.</li>
 *   <li>Use {@code setOn(true)} / {@code setOn(false)} to enable or disable the heater.</li>
 *   <li>{@code applyEffect(List)} is a no-op when the actuator is off or the sensor list is null.</li>
 * </ul>
 */
public class Heater extends Actuator {

  private final double heatDelta;

  /**
   * Create a heater with a default heating delta per tick.
   *
   * @param id unique actuator identifier
   */
  public Heater(String id) {
    super(id, "HEATER");
    this.heatDelta = 1.0; // standard økning per tick (tune etter behov)
  }

  /**
   * Create a heater with a custom heating delta per tick.
   *
   * @param id unique actuator identifier
   * @param heatDelta amount to add to temperature sensor value each tick when the heater is on
   */
  public Heater(String id, double heatDelta) {
    super(id, "HEATER");
    this.heatDelta = heatDelta;
  }

  /**
   * Apply the heating effect to the provided sensors.
   *
   * <p>
   * If the heater is on, each sensor whose {@code getSensorType()} equals "TEMPERATURE"
   * (case-insensitive) will have its value increased by {@code heatDelta}. If the heater
   * is off or the sensor list is null this method returns without modifying anything.
   * </p>
   *
   * @param sensors list of sensors in the node (may be null)
   */
  @Override
  public void applyEffect(List<Sensor> sensors) {
    if (!isOn()) {
      return;
    }
    
    for (Sensor s : sensors) {
      if ("TEMPERATURE".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(heatDelta);
      }
    }
    System.out.println("[Heater] applied temp effect: +" + heatDelta);
  }
}