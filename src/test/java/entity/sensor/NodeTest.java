package entity.sensor;


import static org.junit.jupiter.api.Assertions.*;

import entity.actuator.AirCondition;
import entity.actuator.Heater;
import entity.actuator.LampBrightning;
import entity.actuator.LampDimming;
import entity.actuator.Ventilation;
import entity.sensor.CO2Sensor;
import entity.sensor.LightSensor;
import entity.sensor.TemperatureSensor;
import org.junit.jupiter.api.Test;

/**
 * Node tests using public API: addSensor/addActuator and applyActuatorEffects().
 */
public class NodeTest {

  @Test
  public void temperature_absoluteLimit_turnsOffHeater() {
    TemperatureSensor ts = new TemperatureSensor("t1", 10.0, 40.0);
    ts.updateValue(41.0);

    Heater heater = new Heater("t1_heater");
    AirCondition ac = new AirCondition("t1_ac");
    heater.setOn(true);
    ac.setOn(true);

    Node node = new Node("nodeT");          // use simple constructor
    node.addSensor(ts);
    node.addActuator(heater);
    node.addActuator(ac);

    String msg = node.applyActuatorEffects();

    assertNotNull(msg);
    assertFalse(heater.isOn(), "Heater should be auto-turned off at absolute high temp");
  }

  @Test
  public void light_absoluteMin_turnsOffDimmer() {
    LightSensor ls = new LightSensor("l1", 1.0, 1000.0);
    ls.updateValue(0.0);

    LampDimming dim = new LampDimming("l1_dimming");
    LampBrightning bright = new LampBrightning("l1_bright");
    dim.setOn(true);
    bright.setOn(true);

    Node node = new Node("nodeL");
    node.addSensor(ls);
    node.addActuator(dim);
    node.addActuator(bright);

    String msg = node.applyActuatorEffects();

    assertNotNull(msg);
    assertFalse(dim.isOn(), "Dimmer should be auto-turned off at 0 light");
  }

  @Test
  public void userThreshold_generatesUserAlert_butNoAutoShutdown() {
    TemperatureSensor ts = new TemperatureSensor("t2", 10.0, 30.0);
    ts.updateValue(35.0);

    Heater heater = new Heater("t2_heater");
    heater.setOn(true);

    Node node = new Node("nodeT2");
    node.addSensor(ts);
    node.addActuator(heater);

    String msg = node.applyActuatorEffects();

    assertNotNull(msg);
    assertTrue(msg.toUpperCase().contains("TEMP_OVER_MAX"), "Expect user-level alert");
    assertTrue(heater.isOn(), "Heater should remain on (no absolute auto-shutdown)");
  }
}