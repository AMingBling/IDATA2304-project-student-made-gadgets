package entity.sensor;

import java.time.LocalDateTime;

/**
 * Class representing a Temperature Sensor.
 */
public class TemperatureSensor extends Sensor {

    /**
     * Constructor for TemperatureSensor.
     * Min and max thresholds are specified in degrees Celsius. 18 and 35 would be natural values.
     * @param sensorId the id of the sensor
     * @param minThreshold minimum threshold value
     * @param maxThreshold maximum threshold value
     */
    public TemperatureSensor(String sensorId,
        double minThreshold, double maxThreshold) {
        super(sensorId, "TEMPERATURE", "°C", minThreshold, maxThreshold);
    }
    
    @Override
    public void updateValue() {
        // Simulate temperature reading between 15 and 40 degrees Celsius
        this.value = 15 + Math.random() * 25;
        this.timestamp = LocalDateTime.now();
    }

}
