package entity.actuator;



/**
 * Actuator that gradually reduces measured light values while it is turned on.
 *
 * <p>
 * LampDimming simulates a dimming lamp by subtracting a fixed delta from any sensor
 * with type "LIGHT" on each tick. The actuator only applies its effect when it is
 * switched on and the provided sensor list is non-null.
 * </p>
 *
 * Usage notes:
 * - The default {@code dimDelta} provides a small decrement per tick; tune if needed.
 * - applyEffect mutates the Sensor values by calling {@code adjustValue(...)} on matching sensors.
 */
public class LampDimming extends Actuator {

  private double dimDelta;

  /**
   * Create a LampDimming actuator with a default dimming delta.
   *
   * @param id unique actuator identifier
   */
  public LampDimming(String id) {
    super(id, "LAMP_DIMMING");
    this.dimDelta = 5.0; // standard dimming per tick (tune as needed)
  }

  /**
   * Apply the dimming effect to the provided sensors.
   *
   * <p>
   * If the actuator is on, each sensor whose {@code getSensorType()} equals "LIGHT"
   * (case-insensitive) will have its value decreased by {@code dimDelta}. If the actuator
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
        s.adjustValue(-dimDelta);
      }
    }
    System.out.println("[LAMP_DIMMING] applied light effect: -" + dimDelta);
  }


}
