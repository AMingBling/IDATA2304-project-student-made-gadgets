package entity.sensor;

import java.time.LocalDateTime;

public class HumiditySensor extends Sensor {

    /**
     * Constructor for HumiditySensor.
     * Min and max thresholds are specified in percentage. 50 and 85 would be natural values.
     * @param sensorId the id of the sensor
     * @param minThreshold minimum threshold value
     * @param maxThreshold maximum threshold value
     */
    public HumiditySensor(String sensorId,
        double minThreshold, double maxThreshold) {

        super(sensorId, "HUMIDITY", "%", minThreshold, maxThreshold);
        // startverdien i veksthus
        updateValue(50.0);
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