# IDATA2304-project-student-made-gadgets

Welcome to Smart Greenhouse - a smart greenhouse system for farmers

Student Project

## About the system

The system consist of sensor/actuator nodes, a server, and control panels.

Both the control panels and the nodes connects to the server, and communicates via the server.

When a node is added, the user can use the control panel to add sensors with associated actuators.

The user can also check the status of nodes which checks the status of connected sensors and actuators, or check the status of a specific sensor type.

If the measured values are too high or too low in relation to the desired values of the user, the associated actuators can be toggled on/off.

### Tresholds for Actuators
* The thresholds for the actuators work by allowing the user to define a minimum and maximum value for a sensor. These thresholds determine the range within which the sensor operates. If the sensor's measured value goes outside this range, the associated actuator (e.g., a heater or cooler) is triggered to bring the value back within the defined range
* Set Thresholds: When adding a sensor (e.g., a temperature sensor), the user is prompted to set a minimum threshold and a maximum threshold. For example:  
  Minimum threshold: 18°C
  Maximum threshold: 25°C
* If the user puts on the heater for example, the temperature wil increase to 25 and when it crosses 25 the user gets a message that they should put on aircondition.
* If the measured values goes beyond the absolute limits of the sensors (system defaults), the actuators will automatically turn off.

### Extra work done beyond the basic requirements:
*The user can set maximum and minimum tresholds for the actuators, in addition maximum and minimum limits are also set so if the user tries to set a treshold outside the limit, the system will reject it and notify the user. over this they wil get a message*

*Automatic Sensor IDs and Data Retrieval: We have added functionality to automatically generate unique IDs for sensors, simplifying the process of adding new sensors. Additionally, we have implemented a method to retrieve information for all sensors of a specific type, making it easier to manage and analyze sensor data.*

*We have implemented comprehensive error-handling mechanisms to ensure the safety and reliability of the system. For example, users cannot set thresholds for sensors below or above predefined safe limits, ensuring that the plants are not harmed. Additionally, the application gracefully handles invalid or unsupported user inputs by providing clear feedback and preventing unexpected behavior.*

*Test classes were made verify the functionality of critical components, including communication between nodes and the server. These tests help ensure that the application behaves as expected under various scenarios and maintains a high level of reliability.*

### Available Control Panel commands

- CheckGreenhouse
- AddSensor \<nodeId\>
- RemoveSensor \<nodeId\> \<sensorId\>
- CheckNode \<nodeId\>
- ToggleActuator \<nodeId\> \<actuatorId\> \<on|off\>
- CheckAllSensorOfType
- Exit

### About commands

#### CheckGreenhouse 

*Check which greenhouse the control panel is connected to, and if there are any connected nodes*

#### AddSensor

*Add one of the available provided sensors to a specific node*

1. Choose sensor type (temperature, light, humidity, co2)
2. Set minimum threshold value
3. Set maximum threshold value

If the sensor measure values out of the threshold bound, an alert message will be sent to the control panel.

The sensor will automatically get assigned an ID. 

#### RemoveSensor

*Removes a specific sensor from a specific node*

#### CheckNode

*Check the status of a specific node including connected sensors with their current values, and actuators with their current status (on/off)*

#### ToggleActuator

*Toggles a specific actuator on/off*

#### CheckAllSensorsOfType

*Check the status of all sensors of a specific type*

#### Exit

*Exits the control panel*

## How to run the application
**NOTE** Standard location for NodeClient: greenhouse1
1. Run Server.java
2. **Create a node:** open a new terminal and input:  
```mvn --% exec:java -Dexec.mainClass=network.NodeClient -Dexec.args="01 greenhouse1"```
3. **Open ControlPanel:** open a new terminal and input:  
```mvn --% exec:java -Dexec.mainClass=controlpanel.ControlPanelMain -Dexec.args="cp1 127.0.0.1 5000"```

To add a new node:
- **Open a new terminal and input:**  
```mvn --% exec:java -Dexec.mainClass=network.NodeClient -Dexec.args="<nodeId> greenhouse1"```

To add a new control panel:
- **Open a new terminal and input:**  
```mvn --% exec:java -Dexec.mainClass=controlpanel.ControlPanelMain -Dexec.args="<controlPanelId> 127.0.0.1 5000"```
