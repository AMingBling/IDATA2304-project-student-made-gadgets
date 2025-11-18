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
        updateValue(1000);
    }

    @Override
    public void updateValue(double newValue) {
        this.value = newValue;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public synchronized void adjustValue(double delta) {
        updateValue(getValue() + delta);
    }

    @Override
    public synchronized void updateValue() {
        // Do nothing by default — humidity only changes when actuators run.
    }

    public boolean isAboveMax() {
        return getValue() > getMaxThreshold();
    }

    public boolean isBelowMin() {
        return getValue() < getMinThreshold();
    }

}
