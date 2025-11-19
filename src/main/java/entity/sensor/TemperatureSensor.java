package entity.sensor;

/**
 * Class representing a Temperature Sensor.
 */
public class TemperatureSensor extends Sensor {

    // Absolute allowable limits for temperature sensor thresholds
    private static final double ABS_MIN = 0.0;
    private static final double ABS_MAX = 40.0;

    /**
     * Constructor for TemperatureSensor.
     * Min and max thresholds are specified in degrees Celsius.
     * @param sensorId the id of the sensor
     * @param minThreshold minimum threshold value
     * @param maxThreshold maximum threshold value
     */
    public TemperatureSensor(String sensorId,
                             double minThreshold, double maxThreshold) {
        super(sensorId, "TEMPERATURE", "°C", minThreshold, maxThreshold);

        // --- VALIDATION OF USER-INPUT THRESHOLDS ---
        if (minThreshold < ABS_MIN || minThreshold > ABS_MAX) {
            throw new IllegalArgumentException(
                "Temperature min threshold must be between " + ABS_MIN +
                " and " + ABS_MAX);
        }

        if (maxThreshold < ABS_MIN || maxThreshold > ABS_MAX) {
            throw new IllegalArgumentException(
                "Temperature max threshold must be between " + ABS_MIN +
                " and " + ABS_MAX);
        }

        if (minThreshold >= maxThreshold) {
            throw new IllegalArgumentException(
                "Min threshold cannot be greater than or equal to max threshold.");
        }
        // -------------------------------------------

        updateValue(20.0);
    }

    @Override
    public void updateValue(double newValue) {
        // Temperature should only change when actuators modify it
        this.value = newValue;
        this.timestamp = java.time.LocalDateTime.now();
    }

    @Override
    public void adjustValue(double delta) {
        updateValue(getValue() + delta);
    }

    // Implement parameterless updateValue required by base Sensor
    @Override
    public void updateValue() {
        // Optional: add tiny noise so sensor updates even without actuator input
        double noise = (Math.random() - 0.5) * 0.02;
        updateValue(getValue() + noise);
    }

    private double clamp(double v) {
        double min = this.minThreshold;
        double max = this.maxThreshold;
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    public boolean isAboveMax() {
        return getValue() > getMaxThreshold();
    }

    public boolean isBelowMin() {
        return getValue() < getMinThreshold();
    }
}
