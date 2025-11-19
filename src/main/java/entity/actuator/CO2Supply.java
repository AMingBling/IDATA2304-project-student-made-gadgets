package entity.actuator;

import java.util.List;

/**
 * CO2 Supply actuator that increases CO2 sensor readings while enabled.
 * <p>
 * When the actuator is switched on, {@link #applyEffect(List)} increases the value
 * of any sensor whose {@code getSensorType()} equals "CO2" by the configured
 * {@code co2Delta} each tick. The method is a no-op when the actuator
 * is off or the provided sensor list is null.
 * </p> 
 */
public class CO2Supply extends Actuator {

  private double co2Delta;

  /**
   * Create a CO2Supply with a sensible default CO2 increase delta per tick.
   * @param id unique actuator id
   */
  public CO2Supply(String id) {
    super(id, "CO2_SUPPLY");
    this.co2Delta = 8.0; 
  }

  /**
   * Create a CO2Supply with a specified CO2 increase delta per tick.
   * @param id unique actuator id
   * @param co2Delta amount to increase CO2 sensors by when enabled
   */
  public CO2Supply(String id, double co2Delta) {
    super(id, "CO2_SUPPLY");
    this.co2Delta = co2Delta;
  }

  /**
   * Apply the CO2 increasing effect to CO2 sensors in the provided list.
   * @param sensors list of sensors to potentially affect
   * 
   */
  @Override
  public void applyEffect(List<entity.sensor.Sensor> sensors) {
    if (!isOn() || sensors == null) {
      return;
    }
    for (entity.sensor.Sensor s : sensors) {
      if ("CO2".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(co2Delta);
      }
    }
    System.out.println("[CO2_SUPPLY] applied CO2 effect: +" + co2Delta);
  }

}
