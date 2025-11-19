package entity.actuator;


/**
 * Actuator that increases measured light values while it is turned on.
 *
 * <p>
 * LampBrightning simulates a brightening lamp by adding a fixed {@code brightDelta}
 * to any sensor with type "LIGHT" on each tick. The actuator applies its effect only
 * when it is switched on and the provided sensor list is non-null.
 * </p>
 *
 * Usage notes:
 * <ul>
 *   <li>Create with {@code new LampBrightning(id)}.</li>
 *   <li>Use {@code setOn(true)} / {@code setOn(false)} to enable or disable.</li>
 *   <li>Call {@code applyEffect(List&lt;Sensor&gt;)} from the node tick loop to modify sensor values.</li>
 * </ul>
 */
public class LampBrightning extends Actuator {

  private double brightDelta;

   /**
   * Create a LampBrightning actuator with a default brightening delta.
   *
   * @param id unique actuator identifier
   */
  public LampBrightning(String id) {
    super(id, "LAMP_BRIGHTNING");
    this.brightDelta = 5.0; // standard brightening per tick (tune as needed)
  }

  /**
   * Apply the brightening effect to the provided sensors.
   *
   * <p>
   * If the actuator is on, each sensor whose {@code getSensorType()} equals "LIGHT"
   * (case-insensitive) will have its value increased by {@code brightDelta}. If the actuator
   * is off or the sensor list is null this method returns without modifying anything.
   * </p>
   *
   * @param sensors list of sensors in the node (may be null)
   */
  @Override
  public void applyEffect(java.util.List<entity.sensor.Sensor> sensors) {
    if (!isOn() || sensors == null) {
      return;
    }
    for (entity.sensor.Sensor s : sensors) {
      if ("LIGHT".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(brightDelta);
      }
    }
    System.out.println("[LAMP_BRIGHTNING] applied light effect: +" + brightDelta);
  }

}
