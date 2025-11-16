package entity.sensor;

public class TemperatureSensor extends Sensor {

    

    public TemperatureSensor(String sensorId, double minThreshold, double maxThreshold) {
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
