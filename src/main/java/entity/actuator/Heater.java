package entity.actuator;

import java.util.List;
import entity.sensor.Sensor;

/**
 * Class of a Heater actuator that increases temperature when turned on.
 */
public class Heater extends Actuator {

  private final double heatDelta;

  public Heater(String id) {
    super(id, "HEATER");
    this.heatDelta = 1.0; // standard økning per tick (tune etter behov)
  }

  public Heater(String id, double heatDelta) {
    super(id, "HEATER");
    this.heatDelta = heatDelta;
  }

   @Override
  public void applyEffect(List<Sensor> sensors) {
    if (!isOn()) return;
    double heatDelta = 1.0; // eller bruk eksisterende felt
    for (Sensor s : sensors) {
      if ("TEMPERATURE".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(heatDelta);
      }
    }
    System.out.println("[Heater] applied temp effect: +" + heatDelta);
  }
}