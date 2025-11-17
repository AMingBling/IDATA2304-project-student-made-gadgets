package entity.sensor;

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
        updateValue(20.0);
    }

    @Override
    public void updateValue() {
        // Temperature should only change when actuators modify it
        updateValue(clamp(getValue()));
    }

    @Override
    public void adjustValue(double delta) {
        double newVal = clamp(getValue() + delta);
        updateValue(newVal);
    }

    private double clamp(double v) {
        double min = this.minThreshold;
        double max = this.maxThreshold;
         if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
