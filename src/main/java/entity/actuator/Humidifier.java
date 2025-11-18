package entity.actuator;

import java.util.List;
import entity.sensor.Sensor;

public class Humidifier extends Actuator {

    private final double humidDelta;

    public Humidifier(String id) {
        super(id, "HUMIDIFIER");
        this.humidDelta = 2.0; // juster etter tick-intervall
    }

    public Humidifier(String id, double humidDelta) {
        super(id, "HUMIDIFIER");
        this.humidDelta = humidDelta;
    }

    @Override
    public void applyEffect(List<Sensor> sensors) {
        if (!isOn() || sensors == null) return;
        for (Sensor s : sensors) {
            if ("HUMIDITY".equalsIgnoreCase(s.getSensorType())) {
                s.adjustValue(humidDelta);
            }
        }
        System.out.println("[Humidifier] applied humidity effect: +" + humidDelta);
    }
}