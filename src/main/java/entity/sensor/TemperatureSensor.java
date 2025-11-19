package entity.sensor;

/**
 * Class representing a Temperature Sensor.
 */
public class TemperatureSensor extends Sensor {

  /**
   * Constructor for TemperatureSensor. Min and max thresholds are specified in degrees Celsius. 15
   * and 30 would be natural values.
   *
   * @param sensorId     the id of the sensor
   * @param minThreshold minimum threshold value
   * @param maxThreshold maximum threshold value
   */
  public TemperatureSensor(String sensorId,
      double minThreshold, double maxThreshold) {
    super(sensorId, "TEMPERATURE", "°C", minThreshold, maxThreshold);
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
    if (v < min) {
      return min;
    }
    if (v > max) {
      return max;
    }
    return v;
  }

  /**
   * Above or under thresholds, needs to know what value is when the tick is right under threshold,
   * this is where the message wil come
   *
   * @return true if the sensor value is above the maximum threshold
   */
  public boolean isAboveMax() {
    return getValue() > getMaxThreshold();
  }

  public boolean isBelowMin() {
    return getValue() < getMinThreshold();
  }

}
