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
import entity.sensor.TemperatureSensor;
import entity.sensor.LightSensor;
import entity.sensor.HumiditySensor;
import entity.actuator.Actuator;
import entity.sensor.CO2Sensor;
import com.google.gson.JsonObject;
import java.lang.reflect.Type;

import java.time.LocalDateTime;
import java.util.List;
import com.google.gson.Gson;

import entity.sensor.Sensor;

/**
 * Represents a Node in the network, containing sensors and actuators.
 */
public class Node {

  private String nodeID;
  private String location;
  private LocalDateTime timestamp;
  private List<Sensor> sensors;
  private List<Actuator> actuators;
  // Track sensors that are currently in an alerted (out-of-range) state so we
  // only emit the first alert when they cross the threshold.
  private final java.util.Set<String> alertedSensors = new java.util.HashSet<>();


  /**
   * Constructor for Node
   *
   * @param nodeID   Unique identifier for the node
   * @param location Physical location of the node
   * @param sensors  List of sensors associated with the node
   * @param actuators List of actuators associated with the node
   */
  public Node(String nodeID, String location,
      List<Sensor> sensors, List<Actuator> actuators) {
    setNodeID(nodeID);
    setLocation(location);
    this.timestamp = LocalDateTime.now();
    setSensors(sensors);
    setActuators(actuators);
  }

  //-------------- Setters and getters ---------------

  /**
   * Set node ID
   * @param nodeID Unique identifier for the node
   */
  public void setNodeID(String nodeID) {
    if (nodeID == null || nodeID.isBlank()) {
      throw new IllegalArgumentException("Node ID cannot be null or empty");
    }
    this.nodeID = nodeID;
  }

  /**
   * Set node location
   * @param location Physical location of the node
   */
  public void setLocation(String location) {
    if (location == null || location.isBlank()) {
      throw new IllegalArgumentException("Location cannot be null or empty");
    }
    this.location = location;
  }

  /**
  for (Sensor s : sensors) {
   * @param sensors List of sensors associated with the node
   */
  public void setSensors(List<Sensor> sensors) {
    if (sensors == null) {
      this.sensors = new java.util.ArrayList<>();
    } else {
      this.sensors = sensors;
    }
  }

  public List<Sensor> getSensors() {
    return this.sensors;
  }

  public void setActuators(List<Actuator> actuators) {
    if (actuators == null) {
      this.actuators = new java.util.ArrayList<>();
    } else {
      this.actuators = actuators;
    }
  }

  public List<Actuator> getActuators() {
    return this.actuators;
  }

  public String getNodeID() {
    return this.nodeID;
  }

  public String getLocation() {
    return this.location;
  }

  public LocalDateTime getTimestamp() {
    return this.timestamp;
  }

  public String getActuatorsAsJson() {
    Gson gson = gsonWithLocalDateTime();
    return gson.toJson(this.actuators);
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

  //--------------------------------------------------

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
        // Custom deserializer for Sensor to handle polymorphic concrete types
        .registerTypeAdapter(Sensor.class, new JsonDeserializer<Sensor>() {
          @Override
          public Sensor deserialize(JsonElement json, Type typeOfT,
              JsonDeserializationContext context) {
            try {
              JsonObject obj = json.getAsJsonObject();
              if (obj.has("sensorType") && !obj.get("sensorType").isJsonNull()) {
                String st = obj.get("sensorType").getAsString();
                switch (st) {
                  case "TEMPERATURE":
                    return context.deserialize(json, TemperatureSensor.class);
                  case "LIGHT":
                    return context.deserialize(json, LightSensor.class);
                  case "HUMIDITY":
                    return context.deserialize(json, HumiditySensor.class);
                  case "CO2":
                    return context.deserialize(json, CO2Sensor.class);
                  default:
                    // Fallback: try TemperatureSensor
                    return context.deserialize(json, TemperatureSensor.class);
                }
              }
            } catch (Exception ignored) {
            }
            return null;
          }
        })
          // Custom deserializer for Actuator to instantiate concrete actuator subclasses
          .registerTypeAdapter(Actuator.class, new JsonDeserializer<Actuator>() {
            @Override
            public Actuator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
              try {
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("actuatorType") && !obj.get("actuatorType").isJsonNull()) {
                  String at = obj.get("actuatorType").getAsString();
                    switch (at.toUpperCase()) {
                    case "HEATER":
                      return context.deserialize(json, entity.actuator.Heater.class);
                    case "FAN":
                    case "VENTILATION":
                      return context.deserialize(json, entity.actuator.Ventilation.class);
                    case "HUMIDIFIER":
                      return context.deserialize(json, entity.actuator.Humidifier.class);
                    case "DEHUMIDIFIER":
                      return context.deserialize(json, entity.actuator.DeHumidifier.class);
                    case "AIRCON":
                    case "AIRCONDITION":
                      return context.deserialize(json, entity.actuator.AirCondition.class);
                    case "LAMP_DIMMING":
                    case "LAMP_DIM":
                      return context.deserialize(json, entity.actuator.LampDimming.class);
                    case "LAMP_BRIGHTNING":
                    case "LAMP_BRIGHTENING":
                    case "LAMP_BRIGHT":
                      return context.deserialize(json, entity.actuator.LampBrightning.class);
                    case "CO2_SUPPLY":
                    case "CO2SUPPLY":
                    case "CO2":
                      return context.deserialize(json, entity.actuator.CO2Supply.class);
                    default:
                      // fallback: try to create a simple Ventilation with given id (safe default)
                      try {
                        String id = obj.has("actuatorId") ? obj.get("actuatorId").getAsString() : "unknown";
                        return new entity.actuator.Ventilation(id);
                      } catch (Exception e) {
                        return null;
                      }
                  }
                }
              } catch (Exception ignored) {
              }
              return null;
            }
          })
        .create();
  }

  public void updateAllSensors() {
    for (Sensor sensor : sensors) {
      sensor.updateValue();
    }
    this.timestamp = LocalDateTime.now();
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
  //WHAT IS USED FOR THE ACTUATORS TO WORK

  /**
   * Bruker epsilon
   * What si used for the actuators to continue
   * @return 
   */
    public String applyActuatorEffects() {
    final double EPS = 0.01;
    if (sensors == null || actuators == null) return null;

    // 1) Apply per-tick actuator effects (delegér til actuatorene)
    for (entity.actuator.Actuator act : actuators) {
      if (act == null || !act.isOn()) continue;
      try {
        act.applyEffect(sensors);
      } catch (Exception e) {
        System.out.println("[Node] actuator.applyEffect failed for " + act.getActuatorId() + ": " + e.getMessage());
      }
    }

    // 2) After effects: check thresholds for each sensor type and return an alert string
    // but only when a sensor transitions from in-range -> out-of-range. When a
    // sensor returns to range we clear its alerted state so future crossings can
    // trigger alerts again.
    for (Sensor s : sensors) {
      String type = s.getSensorType() == null ? "" : s.getSensorType().toUpperCase();
      double v = s.getValue();
      double max = s.getMaxThreshold();
      double min = s.getMinThreshold();
      String sid = s.getSensorId();

      boolean currentlyAlerted = alertedSensors.contains(sid);

      if ("TEMPERATURE".equals(type)) {
        if (v > max + EPS) {
          if (!currentlyAlerted) {
            alertedSensors.add(sid);
            return String.format("TEMP_OVER_MAX node=%s sensor=%s value=%.2f max=%.2f — please turn on AirCondition and Turn off Heater",
                nodeID, sid, v, max);
          }
        } else if (v < min - EPS) {
          if (!currentlyAlerted) {
            alertedSensors.add(sid);
            return String.format("TEMP_BELOW_MIN node=%s sensor=%s value=%.2f min=%.2f — please Turn on Heater and Turn off AirCondition",
                nodeID, sid, v, min);
          }
        } else {
          // in range: clear
          alertedSensors.remove(sid);
        }
      } else if ("HUMIDITY".equals(type)) {
        if (v > max + EPS) {
          if (!currentlyAlerted) {
            alertedSensors.add(sid);
            return String.format("HUMIDITY_OVER_MAX node=%s sensor=%s value=%.2f max=%.2f — please turn on DeHumidifier and Turn off Humidifier",
                nodeID, sid, v, max);
          }
        } else if (v < min - EPS) {
          if (!currentlyAlerted) {
            alertedSensors.add(sid);
            return String.format("HUMIDITY_BELOW_MIN node=%s sensor=%s value=%.2f min=%.2f — please Turn on Humidifier and Turn off DeHumidifier",
                nodeID, sid, v, min);
          }
        } else {
          alertedSensors.remove(sid);
        }
      } else if ("LIGHT".equals(type)) {
        if (v > max + EPS) {
          if (!currentlyAlerted) {
            alertedSensors.add(sid);
            return String.format("LIGHT_OVER_MAX node=%s sensor=%s value=%.2f max=%.2f — please turn on LampDim and Turn off LampBright",
                nodeID, sid, v, max);
          }
        } else if (v < min - EPS) {
          if (!currentlyAlerted) {
            alertedSensors.add(sid);
            return String.format("LIGHT_BELOW_MIN node=%s sensor=%s value=%.2f min=%.2f — please Turn on LampBright and Turn off LampDim",
                nodeID, sid, v, min);
          }
        } else {
          alertedSensors.remove(sid);
        }
      } else if ("CO2".equals(type)) {
        if (v > max + EPS) {
          if (!currentlyAlerted) {
            alertedSensors.add(sid);
            return String.format("CO2_OVER_MAX node=%s sensor=%s value=%.2f max=%.2f — please turn on Ventilation and Turn off CO2Supply",
                nodeID, sid, v, max);
          }
        } else if (v < min - EPS) {
          if (!currentlyAlerted) {
            alertedSensors.add(sid);
            return String.format("CO2_BELOW_MIN node=%s sensor=%s value=%.2f min=%.2f — please Turn on CO2Supply and Turn off Ventilation",
                nodeID, sid, v, min);
          }
        } else {
          alertedSensors.remove(sid);
        }
      }
    }

    return null;
    }
 

  
     
public void applyImmediateActuatorEffect(entity.actuator.Actuator a) {
    // intentionally do nothing: user must toggle actuators manually.
    if (a == null) return;
    System.out.println("[Node] applyImmediateActuatorEffect called for " + a.getActuatorId() + " — no automatic changes (user controls actuators).");
}

 


/**
 * For controlling the temperature
 * DETTE KAN GÅ BORT MULIGENS
 */

public void controlTemperature() {
    TemperatureSensor tempSensor = null;
    for (Sensor s : sensors) {
        if ("TEMPERATURE".equalsIgnoreCase(s.getSensorType())) {
            tempSensor = (TemperatureSensor) s;
            break;
        }
    }
    if (tempSensor == null) return;

    // Oppdater sensorer basert på aktive aktuatorer
    for (Actuator actuator : actuators) {
        actuator.applyEffect(sensors);
    }

    // Sjekk grenser og gi beskjed
    if (tempSensor.isAboveMax()) {
        System.out.println("⚠ Temperaturen er over maksverdi! Slå av Heater og slå på AirCondition.");
    } else if (tempSensor.isBelowMin()) {
        System.out.println("⚠ Temperaturen er under minverdi! Slå av AirCondition og slå på Heater.");
    }
}

}