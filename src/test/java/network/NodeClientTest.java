package network;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import entity.Node;
import entity.actuator.AirCondition;
import entity.actuator.Heater;

/**
 * Test class for NodeClient.
 *
 * <p>The following is tested:</p>
 *
 * <b>Positive tests:</b>
 *
 * <ul>
 *   <li>sendNodeWritesSensorDataMessage: verifies that the node client writes a SENSOR_DATA_FROM_NODE message and includes a node id field.</li>
 *   <li>actuatorCommandTurnsOnAndResolvesConflict: verifies that an ACTUATOR_COMMAND turns on the specified heater and that conflicting actuators (AC) are turned off to resolve the conflict.</li>
 *   <li>addAndRemoveSensorCommandsModifyNode: verifies that ADD_SENSOR adds a sensor to the node and that REMOVE_SENSOR removes it afterwards.</li>
 * </ul>
 * @author Group 1
 * @version 2025-11-18
 */
public class NodeClientTest {

    private Gson buildGson() {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, t, c) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, t, c) -> LocalDateTime.parse(json.getAsString()))
        .create();
  }

  /**
     * Test that sendCurrentNode writes a SENSOR_DATA_FROM_NODE message.
     *
     * <p>Expected outcome: the output contains "SENSOR_DATA_FROM_NODE" and a node id field ("nodeID" or "nodeId").</p>
     */
    @Test
  public void sendNodeWritesSensorDataMessage() {
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw, true);
    BufferedReader in = new BufferedReader(new StringReader("")); // no incoming commands
    Gson gson = buildGson();

    List<entity.sensor.Sensor> sensors = new ArrayList<>();
    List<entity.actuator.Actuator> actuators = new ArrayList<>();
    Node node = new Node("n1", "loc", sensors, actuators);

    NodeClient nc = new NodeClient(node, null, out, in, gson);
    nc.sendCurrentNode();

    String written = sw.toString();
    assertTrue(written.contains("\"messageType\":\"SENSOR_DATA_FROM_NODE\""), "Must include SENSOR_DATA_FROM_NODE");
    assertTrue(written.contains("\"nodeID\"") || written.contains("\"nodeId\""), "Must include node id field");
  }

  /**
   * Test that an ACTUATOR_COMMAND turns on the heater and resolves conflicts with AC.
   *
   * <p>Expected outcome: heater is ON after processing the command and AC is OFF to resolve the conflict.</p>
   */
  @Test
  public void actuatorCommandTurnsOnAndResolvesConflict() throws Exception {
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw, true);

    String cmdJson = "{"
        + "\"messageType\":\"ACTUATOR_COMMAND\","
        + "\"actuatorId\":\"heater1\","
        + "\"command\":\"TURN_ON\""
        + "}\n";
    BufferedReader in = new BufferedReader(new StringReader(cmdJson));

    Gson gson = buildGson();
    List<entity.sensor.Sensor> sensors = new ArrayList<>();
    List<entity.actuator.Actuator> actuators = new ArrayList<>();
    Heater heater = new Heater("heater1");
    AirCondition ac = new AirCondition("ac1");
    heater.setOn(false);
    ac.setOn(true);
    actuators.add(heater);
    actuators.add(ac);

    Node node = new Node("nodeX", "loc", sensors, actuators);

    NodeClient nc = new NodeClient(node, null, out, in, gson);
    nc.start();
    Thread.sleep(200);

    assertTrue(heater.isOn(), "Heater should be ON after command");
    assertFalse(ac.isOn(), "AC should be OFF to resolve conflict");
    nc.close();
}

 /**
   * Test that ADD_SENSOR followed by REMOVE_SENSOR modifies the node's sensors list.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>After the ADD_SENSOR command is processed the node contains a sensor with id "temp1".</li>
   *   <li>After the REMOVE_SENSOR command is processed the sensor "temp1" is no longer present.</li>
   * </ul>
   *
   * <p>This test includes waiting loops to allow asynchronous processing of commands.</p>
   * 
   * @author Group 1
   * @version 2025-11-18
   */
@Test
  public void addAndRemoveSensorCommandsModifyNode() throws Exception {
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw, true);

    String add = "{"
        + "\"messageType\":\"ADD_SENSOR\","
        + "\"sensorType\":\"TEMPERATURE\","
        + "\"sensorId\":\"temp1\","
        + "\"minThreshold\":10.0,"
        + "\"maxThreshold\":30.0"
        + "}\n";
    String remove = "{"
        + "\"messageType\":\"REMOVE_SENSOR\","
        + "\"sensorId\":\"temp1\""
        + "}\n";
    BufferedReader in = new BufferedReader(new StringReader(add + remove));

    Gson gson = buildGson();
    List<entity.sensor.Sensor> sensors = new ArrayList<>();
    List<entity.actuator.Actuator> actuators = new ArrayList<>();
    Node node = new Node("nA", "loc", sensors, actuators);

    NodeClient nc = new NodeClient(node, null, out, in, gson);
    nc.start();

    // wait until sensor is added (timeout 5000 ms)
    long start = System.currentTimeMillis();
    boolean addedSensor = false;
    while (System.currentTimeMillis() - start < 5000) {
      if (node.getSensors().stream().anyMatch(s -> "temp1".equals(s.getSensorId()))) {
        addedSensor = true;
        break;
      }
      Thread.sleep(50);
    }
    assertTrue(addedSensor, "Sensor temp1 should be added");

    // wait until sensor is removed (timeout 5000 ms)
    start = System.currentTimeMillis();
    boolean removed = false;
    while (System.currentTimeMillis() - start < 5000) {
      if (node.getSensors().stream().noneMatch(s -> "temp1".equals(s.getSensorId()))) {
        removed = true;
        break;
      }
      Thread.sleep(50);
    }
    assertTrue(removed, "Sensor temp1 should be removed after REMOVE_SENSOR");
    nc.close();
  }
}
