package entity.actuator;

public class LampBrightning extends Actuator {

  private double brightDelta;

  public LampBrightning(String id) {
    super(id, "LAMP_BRIGHTNING");
    this.brightDelta = 5.0; // standard brightening per tick (tune as needed)
  }

  @Override
  public void applyEffect(java.util.List<entity.sensor.Sensor> sensors) {
    if (!isOn() || sensors == null) return;
    for (entity.sensor.Sensor s : sensors) {
      if ("LIGHT".equalsIgnoreCase(s.getSensorType())) {
        s.adjustValue(brightDelta);
      }
    }
    System.out.println("[LAMP_BRIGHTNING] applied light effect: +" + brightDelta);
  }

}
