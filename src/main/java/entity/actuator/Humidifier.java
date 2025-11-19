package entity.actuator;

import java.util.List;
import entity.sensor.Sensor;

/**
 * Humidifier actuator that increases humidity sensor readings while it is turned on.
 *
 * <p>
 * Instances of this class represent a humidifier attached to a node. When the actuator is
 * switched on, {@link #applyEffect(List)} increases the value of any sensor whose
 * {@code getSensorType()} equals "HUMIDITY" by a fixed delta each tick.
 * </p>
 *
 * Usage notes:
 * <ul>
 *   <li>Create with {@code new Humidifier(id)} or {@code new Humidifier(id, delta)} to tune effect size.</li>
 *   <li>Call {@code setOn(true)} to enable the humidifier and {@code setOn(false)} to disable it.</li>
 *   <li>{@link #applyEffect(List)} is a no-op when the actuator is off or the provided sensor list is null.</li>
 * </ul>
 */
public class Humidifier extends Actuator {

  private final double humidDelta;

  /**
   * Create a Humidifier with a sensible default humidity increase per tick.
   *
   * @param id unique actuator id
   */
  public Humidifier(String id) {
    super(id, "HUMIDIFIER");
    this.humidDelta = 2.0; // juster etter tick-intervall
  }




  /**
   * Apply the humidifier effect to the provided sensors.
   *
   * <p>
   * If the actuator is on, each sensor whose {@code getSensorType()} equals "HUMIDITY"
   * (case-insensitive) will have its value increased by {@code humidDelta}. If the actuator
   * is off or the sensor list is null this method returns without modifying anything.
   * </p>
   *
   * @param sensors list of sensors to apply the effect to (may be null)
   */
  @Override
  public void applyEffect(List<Sensor> sensors) {
      if (!isOn() || sensors == null) {
          return;
      }
    for (Sensor s : sensors) {
      if ("HUMIDITY".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(humidDelta);
      }
    }
    System.out.println("[Humidifier] applied humidity effect: +" + humidDelta);
  }
}