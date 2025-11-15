package entity.sensor;

import java.time.LocalDateTime;

public class CO2Sensor extends Sensor {
    /**
     * Constructor for CO2Sensor.
     * Min and max thresholds are specified in ppm. 300 and 2000 would be natural values.
     * @param sensorId
     * @param minThreshold
     * @param maxThreshold
     */
    public CO2Sensor(String sensorId,
        double minThreshold, double maxThreshold) {
        super(sensorId, "CO2", "ppm", minThreshold, maxThreshold);
    }
    
    @Override
    public void updateValue() {
        // Simulate CO2 reading between 250 and 2150 ppm
        this.value = 250 + Math.random() * 1900;
        this.timestamp = LocalDateTime.now();
    }

}
