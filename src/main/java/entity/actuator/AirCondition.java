package entity.actuator;

import entity.sensor.Sensor;
import java.util.List;

public class AirCondition extends Actuator {

    public AirCondition(String id) {
        super(id, "AIRCON");
    }

    @Override
    public void applyEffect(List<Sensor> sensors) {
        if (!isOn()) return;

        for (Sensor s : sensors) {
            if ("TEMPERATURE".equalsIgnoreCase(s.getSensorType())) {
                s.adjustValue(-1.0); 
            }
        }
    }
}
