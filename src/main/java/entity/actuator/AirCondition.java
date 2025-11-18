package entity.actuator;

import entity.sensor.Sensor;
import java.util.List;

/**
 * The class of an AirCondition actuator that decreases temperature when turned on.
 */
public class AirCondition extends Actuator {

  public AirCondition(String id) {
    super(id, "AIRCON");
  }

  @Override
  public void applyEffect(List<Sensor> sensors) {
      if (!isOn()) {
          return;
      }

    for (Sensor s : sensors) {
      if ("TEMPERATURE".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(-1.0);
      }
    }
    System.out.println("[AirCondition] applied temp effect: -1.0");
  }
  


  
}
