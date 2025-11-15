package entity.sensor;

import java.time.LocalDateTime;

public class TemperatureSensor extends Sensor {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for TemperatureSensor.
     * Min and max thresholds are specified in degrees Celsius. 18 and 35 would be natural values.
     * @param sensorId
     * @param minThreshold
     * @param maxThreshold
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
