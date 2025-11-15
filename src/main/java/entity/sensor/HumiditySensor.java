package entity.sensor;

import java.time.LocalDateTime;

public class HumiditySensor extends Sensor {
    /**
     * Constructor for HumiditySensor.
     * Min and max thresholds are specified in percentage. 30 and 85 would be natural values.
     * @param sensorId
     * @param minThreshold
     * @param maxThreshold
     */
    public HumiditySensor(String sensorId,
        double minThreshold, double maxThreshold) {
        super(sensorId, "HUMIDITY", "%", minThreshold, maxThreshold);
    }
    
    @Override
    public void updateValue() {
        // Simulate humidity reading between 25% and 90%
        this.value = 25 + Math.random() * 65;
        this.timestamp = LocalDateTime.now();
    }

}
