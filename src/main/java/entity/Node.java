package entity;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import entity.sensor.Sensor;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import com.google.gson.Gson;

/**
 * Represents a Node in the IoT network, containing sensors and actuators.
 */
public class Node {

  private String nodeID;
  private String location;
  private LocalDateTime timestamp;
  private List<Sensor> sensors;
  private List<Actuator> actuators;
  private static final long serialVersionUID = 1L;

  public Node(String nodeID, String location,
      List<Sensor> sensors, List<Actuator> actuators) {
    setNodeID(nodeID);
    setLocation(location);
    this.timestamp = LocalDateTime.now();
    setSensors(sensors);
    setActuators(actuators);
  }

  //-------------- Setters and getters ---------------
  public void setNodeID(String nodeID) {
    if (nodeID == null || nodeID.isBlank()) {
      throw new IllegalArgumentException("Node ID cannot be null or empty");
    }
    this.nodeID = nodeID;
  }

  public void setLocation(String location) {
    if (location == null || location.isBlank()) {
      throw new IllegalArgumentException("Location cannot be null or empty");
    }
    this.location = location;
  }

  public void setSensors(List<Sensor> sensors) {
    if (sensors == null || sensors.isEmpty()) {
      throw new IllegalArgumentException("entity.Sensor readings cannot be null or empty");
    }
    this.sensors = sensors;
  }

  public void setActuators(List<Actuator> actuators) {
    if (actuators == null || actuators.isEmpty()) {
      throw new IllegalArgumentException("Actuators cannot be null or empty");
    }
    this.actuators = actuators;
  }

  public String getNodeID() {
    return nodeID;
  }

  public String getLocation() {
    return location;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public List<Sensor> getSensors() {
    return sensors;
  }

  public List<Actuator> getActuators() {
    return actuators;
  }
  //-------------------------------------------------

  /**
   * Add sensors to the node
   *
   * @param sensor Sensor to be added
   */
  public void addSensor(Sensor sensor) {
    this.sensors.add(sensor);
  }

  /**
   * Add actuator to the node
   *
   * @param actuator Actuator to be added
   */
  public void addActuator(Actuator actuator) {
    this.actuators.add(actuator);
  }

  // JSON helpers that include LocalDateTime adapters
  public static Node nodeFromJson(String json) {
    return gsonWithLocalDateTime().fromJson(json, Node.class);
  }

  public static Node nodeFromJson(String json, Gson gson) {
    return gson.fromJson(json, Node.class);
  }

  public String nodeToJson() {
    return gsonWithLocalDateTime().toJson(this);
  }

  public String nodeToJson(Gson gson) {
    return gson.toJson(this);
  }

  private static Gson gsonWithLocalDateTime() {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
        .create();
  }

  private static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {

    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc,
        JsonSerializationContext context) {
      return new JsonPrimitive(src.toString());
    }
  }

  private static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context)
        throws JsonParseException {
      return LocalDateTime.parse(json.getAsString());
    }
  }

  // -----------------------------------------------------

}