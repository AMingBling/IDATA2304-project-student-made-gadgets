package entity.sensor;

import java.time.LocalDateTime;

public class HumiditySensor extends Sensor {

    public HumiditySensor(String sensorId, double minThreshold, double maxThreshold) {
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