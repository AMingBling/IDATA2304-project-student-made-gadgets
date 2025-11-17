package entity.actuator;

public class LampDimming extends Actuator {

  private double dimDelta;

  public LampDimming(String id) {
    super(id, "LAMP_DIMMING");
    this.dimDelta = 5.0; // standard dimming per tick (tune as needed)
  }

  @Override
  public void applyEffect(java.util.List<entity.sensor.Sensor> sensors) {
    if (!isOn() || sensors == null) return;
    for (entity.sensor.Sensor s : sensors) {
      if ("LIGHT".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(-dimDelta);
      }
    }
    System.out.println("[LAMP_DIMMING] applied light effect: -" + dimDelta);
  }








}
