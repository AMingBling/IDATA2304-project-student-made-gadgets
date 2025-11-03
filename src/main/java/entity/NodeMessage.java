package entity;

import java.time.LocalDateTime;
import java.util.List;
import com.google.gson.Gson;

public class NodeMessage {

  private String nodeID;
  private String location;
  private LocalDateTime timestamp;
  private List<SensorMessage> sensorReadings;

  public NodeMessage(String nodeID, String location, LocalDateTime timestamp,
      List<SensorMessage> sensorReadings) {
    this.nodeID = nodeID;
    this.location = location;
    this.timestamp = timestamp;
    this.sensorReadings = sensorReadings;
  }

  //-------------- Getters and setters ---------------
  public String getNodeID() {
    return nodeID;
  }

  public String getLocation() {
    return location;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public List<SensorMessage> getSensorReadings() {
    return sensorReadings;
  }

  public void setNodeID(String nodeID) {
    this.nodeID = nodeID;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public void setSensorReadings(List<SensorMessage> sensorReadings) {
    this.sensorReadings = sensorReadings;
  }
  //-------------------------------------------------

  public static NodeMessage fromJson(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, NodeMessage.class);
  }

  public String toJson() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }
}