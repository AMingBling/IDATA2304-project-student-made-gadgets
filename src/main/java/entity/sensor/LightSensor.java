package entity.sensor;

import java.time.LocalDateTime;

public class LightSensor extends Sensor {
    /**
     * Constructor for LightSensor.
     * Min and max thresholds are specified in lux. 200 and 20 000 would be natural values.
     * @param sensorId
     * @param minThreshold
     * @param maxThreshold
     */
    public LightSensor(String sensorId,
        double minThreshold, double maxThreshold) {
        super(sensorId, "LIGHT", "lux", minThreshold, maxThreshold);
    }
    
    @Override
    public void updateValue() {
        // Simulate light reading between 100 and 21 000 lux
        this.value = 100 + Math.random() * 21000;
        this.timestamp = LocalDateTime.now();
    }

}
