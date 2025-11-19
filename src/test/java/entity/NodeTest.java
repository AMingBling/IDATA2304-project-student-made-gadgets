package entity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import entity.actuator.Heater;
import entity.sensor.TemperatureSensor;

/**
 * Test class for Node.
 *
 * <p>The following is tested:</p>
 *
 * <b>Positive tests:</b>
 *
 * <ul>
 *   <li>constructorInitializesLists: verifies that the Node holds the provided sensor and actuator lists after construction.</li>
 *   <li>sensorsListIsMutable: verifies sensors can be added and removed via getSensors().</li>
 *   <li>actuatorsListIsMutable: verifies actuators can be added and removed via getActuators().</li>
 * </ul>
 *
 * @author Group 1
 * @version 2025-11-19
 */
public class NodeTest {
    

/**
 * Test that the Node constructor initializes its sensor and actuator lists.
 *
 * <p>Expected outcome: both sensor and actuator lists are empty immediately after construction.</p>
 */
@Test
public void constructorInitializesLists() {
    List<entity.sensor.Sensor> sensors = new ArrayList<>();
    List<entity.actuator.Actuator> actuators = new ArrayList<>();
    Node node = new Node("n-test", "loc", sensors, actuators);

    assertEquals(0, node.getSensors().size());
    assertEquals(0, node.getActuators().size());
  }

  /**
   * Test that sensors can be added to and removed from the node.
   *
   * <p>Expected outcome: a TemperatureSensor added to the node is present, and can be removed.</p>
   */
  @Test
  public void sensorsListIsMutable() {
    List<entity.sensor.Sensor> sensors = new ArrayList<>();
    List<entity.actuator.Actuator> actuators = new ArrayList<>();
    Node node = new Node("n-test", "loc", sensors, actuators);
    TemperatureSensor ts = new TemperatureSensor("temp-1", 0.0, 0.0);
    node.getSensors().add(ts);

    boolean found = node.getSensors().stream().anyMatch(s -> "temp-1".equals(s.getSensorId()));
    assertTrue(found, "Added temperature sensor should be present in node.getSensors()");

    node.getSensors().removeIf(s -> "temp-1".equals(s.getSensorId()));
    boolean stillPresent = node.getSensors().stream().anyMatch(s -> "temp-1".equals(s.getSensorId()));
    assertFalse(stillPresent, "Temperature sensor should be removable from node.getSensors()");
  }

    /**
     * Test that actuators can be added to and removed from the node.
     *
     * <p>Expected outcome: an actuator added to the node is present, and can be removed.</p>
     */
    @Test
  public void actuatorsListIsMutable() {
    List<entity.sensor.Sensor> sensors = new ArrayList<>();
    List<entity.actuator.Actuator> actuators = new ArrayList<>();
    Node node = new Node("n-act", "loc", sensors, actuators);

    Heater heater = new Heater("heater-1");
    node.getActuators().add(heater);

    boolean found = node.getActuators().stream().anyMatch(a -> "heater-1".equals(a.getActuatorId()));
    assertTrue(found, "Added heater should be present in node.getActuators()");

    node.getActuators().removeIf(a -> "heater-1".equals(a.getActuatorId()));
    boolean stillPresent = node.getActuators().stream().anyMatch(a -> "heater-1".equals(a.getActuatorId()));
    assertFalse(stillPresent, "Heater should be removable from node.getActuators()");
  }
}