package entity.sensor;

import java.time.LocalDateTime;

/**
 * Class representing a Light Sensor.
 */
public class LightSensor extends Sensor {

  /**
   * Constructor for LightSensor. Min and max thresholds are specified in lux. 1000 and 20 000 would
   * be natural values.
   *
   * @param sensorId     the id of the sensor
   * @param minThreshold minimum threshold value
   * @param maxThreshold maximum threshold value
   */
  public LightSensor(String sensorId,
      double minThreshold, double maxThreshold) {
    super(sensorId, "LIGHT", "lux", minThreshold, maxThreshold);
    updateValue(15000);
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
