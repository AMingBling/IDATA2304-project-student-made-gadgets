package entity.actuator;

import java.util.List;

/**
 * Class of a CO2Supply actuator that increases CO2 levels when turned on.
 */
public class CO2Supply extends Actuator {

  private double co2Delta;

  public CO2Supply(String id) {
    super(id, "CO2_SUPPLY");
    this.co2Delta = 8.0; // standard økning per tick (tune etter behov)
  }

  public CO2Supply(String id, double co2Delta) {
    super(id, "CO2_SUPPLY");
    this.co2Delta = co2Delta;
  }

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
