package entity.actuator;

import java.util.List;
import entity.sensor.Sensor;

/**
 * Dehumidifier actuator that lowers humidity sensor readings while enabled.
 *
 * <p>
 * When the actuator is switched on, {@link #applyEffect(List)} decreases the value
 * of any sensor whose {@code getSensorType()} equals "HUMIDITY" by the configured
 * {@code dryDelta} each tick. The method is a no-op when the actuator is off or the
 * provided sensor list is null.
 * </p>
 *
 * Usage notes:
 * <ul>
 *   <li>Create with {@code new DeHumidifier(id)} (default delta) or
 *       {@code new DeHumidifier(id, dryDelta)} to specify a custom effect size.</li>
 *   <li>Enable/disable with {@code setOn(true)} / {@code setOn(false)}.</li>
 *   <li>Call {@code applyEffect(List)} from the node tick loop to apply the effect.</li>
 * </ul>
 */
public class DeHumidifier extends Actuator {

  private final double dryDelta;


  /**
   * Create a DeHumidifier with a sensible default drying delta per tick.
   *
   * @param id unique actuator id
   */
  public DeHumidifier(String id) {
    super(id, "DEHUMIDIFIER");
    this.dryDelta = 2.0; // juster etter tick-intervall
  }



  /**
   * Create a DeHumidifier with a specified drying delta per tick.
   *
   * @param id unique actuator id
   * @param dryDelta amount to decrease humidity sensors by when enabled
   */
//  public DeHumidifier(String id, double dryDelta) {
//    super(id, "DEHUMIDIFIER");
//    this.dryDelta = dryDelta;
//  }

  /**
   * Apply the dehumidifying effect to humidity sensors in the provided list.
   * 
   * @param sensors list of sensors to potentially affect
   */
  @Override
  public void applyEffect(List<Sensor> sensors) {
    if (!isOn() || sensors == null) {
      return;
    }
    for (Sensor s : sensors) {
      if ("HUMIDITY".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(-dryDelta);
      }
    }
    System.out.println("[DeHumidifier] applied humidity effect: -" + dryDelta);
  }
}