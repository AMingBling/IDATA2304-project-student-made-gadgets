package entity.sensor;

/**
 * Class representing a Temperature Sensor.
 * Extends the base Sensor class.
 */
public class TemperatureSensor extends Sensor {

  /**
   * Constructor for TemperatureSensor.
   * Min and max thresholds are specified in degrees Celsius. 15 and 30 would be
   * natural values.
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

  /**
   * Updates the sensor value.
   * @param newValue the new sensor value
   */
  @Override
  public void updateValue(double newValue) {
    // Temperature should only change when actuators modify it
    this.value = newValue;
    this.timestamp = java.time.LocalDateTime.now();
  }

  /**
   * Adjusts the sensor value by a given delta.
   * @param delta the amount to adjust the sensor value by
   */
  @Override
  public void adjustValue(double delta) {
    updateValue(getValue() + delta);
  }

  /**
   * Periodically updates the sensor value with a small random noise.
   * This simulates minor fluctuations in temperature readings.
   *
   */
  @Override
  public void updateValue() {

    double noise = (Math.random() - 0.5) * 0.02;
    updateValue(getValue() + noise);
  }






}
