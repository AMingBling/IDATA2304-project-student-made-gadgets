package entity.sensor;

import java.time.LocalDateTime;

/**
 * Class representing a Light Sensor.
 */
public class LightSensor extends Sensor {
    /**
     * Constructor for LightSensor.
     * Min and max thresholds are specified in lux. 1000 and 20 000 would be natural values.
     * @param sensorId the id of the sensor
     * @param minThreshold minimum threshold value
     * @param maxThreshold maximum threshold value
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
