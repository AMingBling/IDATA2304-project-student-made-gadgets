package entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class NodeMessage {

  //private String messageType;
  private String nodeID;
  private String location;
  private LocalDateTime timestamp;
  private List<Sensor> sensorReadings;

  public NodeMessage(String nodeID, String location,
      List<Sensor> sensorReadings) {
    //this.messageType = "NODE_DATA";
    setNodeID(nodeID);
    setLocation(location);
    this.timestamp = LocalDateTime.now();
    setSensorReadings(sensorReadings);
  }

  //-------------- Setters and getters ---------------

//  public String getmessageType() {
//    return messageType;
//  }

//  public void setMessageType(String type) {
//        this.messageType = type;
//    }

  public void setNodeID(String nodeID) {
    if (nodeID == null || nodeID.isEmpty()) {
      throw new IllegalArgumentException("Node ID cannot be null or empty");
    }
    this.nodeID = nodeID;
  }

  public void setLocation(String location) {
    if (location == null || location.isEmpty()) {
      throw new IllegalArgumentException("Location cannot be null or empty");
    }
    this.location = location;
  }

  public void setSensorReadings(List<Sensor> sensorReadings) {
    if (sensorReadings == null || sensorReadings.isEmpty()) {
      throw new IllegalArgumentException("entity.Sensor readings cannot be null or empty");
    }
    this.sensorReadings = sensorReadings;
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

  public List<Sensor> getSensorReadings() {
    return sensorReadings;
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

  // -----------------------------------------------------

}