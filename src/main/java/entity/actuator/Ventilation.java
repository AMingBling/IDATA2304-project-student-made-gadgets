package entity.actuator;

import java.util.List;
import entity.sensor.Sensor;

/**
 * Ventilation actuator that lowers CO2 sensor readings while it is turned on.
 *
 * <p>
 * When active, this actuator applies a fixed reduction (co2Delta) to all CO2 sensors
 * in the node each tick. The default constructor uses a sensible default delta;
 * an alternative constructor allows tuning the delta.
 * </p>
 *
 * Usage notes:
 * - applyEffect(List&lt;Sensor&gt;) is a no-op when the actuator is off or when the provided
 *   sensor list is null.
 * - Only sensors with type "CO2" are affected.
 */
public class Ventilation extends Actuator {

  private final double co2Delta;

  /**
   * Create a Ventilation actuator with the default CO2 reduction per tick.
   *
   * The default reduction is chosen to provide a noticeable but safe change per tick.
   *
   * @param id unique actuator id
   */
  public Ventilation(String id) {
    super(id, "VENTILATION");
    this.co2Delta = 8.0; // standard reduksjon per tick (tune etter behov)
  }

  /**
   * Create a Ventilation actuator with a custom CO2 reduction per tick.
   *
   * @param id unique actuator id
   * @param co2Delta amount (positive) to subtract from CO2 sensor value each tick when on
   */
  public Ventilation(String id, double co2Delta) {
    super(id, "VENTILATION");
    this.co2Delta = co2Delta;
  }

  /**
   * Apply the ventilation effect to the provided sensors.
   *
   * <p>
   * If the actuator is on, each sensor with type "CO2" will have its value decreased by
   * {@code co2Delta}. If the actuator is off or sensors is null, this method returns
   * immediately without modifying anything.
   * </p>
   *
   * @param sensors list of sensors to apply the effect to
   */
  @Override
  public void applyEffect(List<Sensor> sensors) {
    if (!isOn() || sensors == null) {
      return;
    }
    for (Sensor s : sensors) {
      if ("CO2".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(-co2Delta);
      }
    }
    System.out.println("[VENTILATION] applied CO2 effect: -" + co2Delta);
  }
}



