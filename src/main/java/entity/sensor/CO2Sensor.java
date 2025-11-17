package entity.sensor;

import java.time.LocalDateTime;

/**
 * Class representing a CO2 Sensor.
 */
public class CO2Sensor extends Sensor {
    /**
     * Constructor for CO2Sensor.
     * Min and max thresholds are specified in ppm. 800 and 1500 would be natural values.
     * @param sensorId the id of the sensor
     * @param minThreshold minimum threshold value
     * @param maxThreshold maximum threshold value
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
