package entity.actuator;

import java.util.List;
import entity.sensor.Sensor;

public class DeHumidifier extends Actuator {

    private final double dryDelta;

    public DeHumidifier(String id) {
        super(id, "DEHUMIDIFIER");
        this.dryDelta = 2.0; // juster etter tick-intervall
    }

    public DeHumidifier(String id, double dryDelta) {
        super(id, "DEHUMIDIFIER");
        this.dryDelta = dryDelta;
    }

    @Override
    public void applyEffect(List<Sensor> sensors) {
        if (!isOn() || sensors == null) return;
        for (Sensor s : sensors) {
            if ("HUMIDITY".equalsIgnoreCase(s.getSensorType())) {
                s.adjustValue(-dryDelta);
            }
        }
        System.out.println("[DeHumidifier] applied humidity effect: -" + dryDelta);
    }
}