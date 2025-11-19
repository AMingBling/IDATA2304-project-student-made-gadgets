package entity.actuator;

import java.util.List;
import entity.sensor.Sensor;

/**
 * Class of a Ventilation actuator that decreases CO2 levels when turned on.
 */
public class Ventilation extends Actuator {

  private final double co2Delta;

  public Ventilation(String id) {
    super(id, "VENTILATION");
    this.co2Delta = 8.0; // standard reduksjon per tick (tune etter behov)
  }

  public Ventilation(String id, double co2Delta) {
    super(id, "VENTILATION");
    this.co2Delta = co2Delta;
  }

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



