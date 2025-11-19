package entity.sensor;


import static org.junit.jupiter.api.Assertions.*;

import entity.Node;
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
 * Test class for Node.
 *
 * <p>The following is tested:</p>
 *
 * <b>Positive tests:</b>
 *
 * <ul>
 *   <li>temperature_absoluteLimit_turnsOffHeater: verifies that when temperature exceeds absolute limits, the heater is turned off automatically.</li>
 *   <li>light_absoluteMin_turnsOffDimmer: verifies that when light level drops below absolute minimum, the lamp dimmer is turned off automatically.</li>
 *   <li>userThreshold_generatesUserAlert_butNoAutoShutdown: verifies that when temperature exceeds user-defined thresholds, an alert is generated but actuators remain unaffected.</li>
 * </ul>
 *
 * @author Group 1
 * @version 2025-11-19
 */
public class NodeTest {

/**
   * Test that when a temperature sensor reads above the node's absolute maximum the node
   * applies automatic safety measures.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>applyActuatorEffects returns a non-null message describing the event.</li>
   *   <li>The heater actuator is turned off to prevent overheating.</li>
   * </ul>
   */
  @Test
  public void temperature_absoluteLimit_turnsOffHeater() {
    TemperatureSensor ts = new TemperatureSensor("t1", 10.0, 40.0);
    ts.updateValue(41.0);

    Heater heater = new Heater("t1_heater");
    AirCondition ac = new AirCondition("t1_ac");
    heater.setOn(true);
    ac.setOn(true);

    Node node = new Node("nodeT", "nodeT description", new java.util.ArrayList<entity.sensor.Sensor>(), new java.util.ArrayList<entity.actuator.Actuator>());
    node.addSensor(ts);
    node.addActuator(heater);
    node.addActuator(ac);

    String msg = node.applyActuatorEffects();

    assertNotNull(msg);
    assertFalse(heater.isOn(), "Heater should be auto-turned off at absolute high temp");
  }

  /**
   * Test that when a light sensor reads below the node's absolute minimum the node
   * applies automatic safety measures to lighting actuators.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>applyActuatorEffects returns a non-null message describing the event.</li>
   *   <li>The lamp dimming actuator is turned off to prevent operation in invalid light conditions.</li>
   * </ul>
   */
  @Test
  public void light_absoluteMin_turnsOffDimmer() {
    LightSensor ls = new LightSensor("l1", 1.0, 1000.0);
    ls.updateValue(0.0);

    LampDimming dim = new LampDimming("l1_dimming");
    LampBrightning bright = new LampBrightning("l1_bright");
    dim.setOn(true);
    bright.setOn(true);

    Node node = new Node("nodeL", "nodeL description", new java.util.ArrayList<entity.sensor.Sensor>(), new java.util.ArrayList<entity.actuator.Actuator>());
    node.addSensor(ls);
    node.addActuator(dim);
    node.addActuator(bright);

    String msg = node.applyActuatorEffects();

    assertNotNull(msg);
    assertFalse(dim.isOn(), "Dimmer should be auto-turned off at 0 light");
  }

  /**
   * Test that when a sensor crosses user-defined thresholds the node generates an alert
   * but does not perform absolute safety shutdown.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>applyActuatorEffects returns a non-null message that includes a user-level alert (e.g. "TEMP_OVER_MAX").</li>
   *   <li>Actuators (heater) remain in their prior state (no automatic shutdown).</li>
   * </ul>
   */
  @Test
  public void userThreshold_generatesUserAlert_butNoAutoShutdown() {
    TemperatureSensor ts = new TemperatureSensor("t2", 10.0, 30.0);
    ts.updateValue(35.0);

    Heater heater = new Heater("t2_heater");
    heater.setOn(true);

    Node node = new Node("nodeT2", "nodeT2 description", new java.util.ArrayList<entity.sensor.Sensor>(), new java.util.ArrayList<entity.actuator.Actuator>());
    node.addSensor(ts);
    node.addActuator(heater);

    String msg = node.applyActuatorEffects();

    assertNotNull(msg);
    assertTrue(msg.toUpperCase().contains("TEMP_OVER_MAX"), "Expect user-level alert");
    assertTrue(heater.isOn(), "Heater should remain on (no absolute auto-shutdown)");
  }
}