# IDATA2304-project-student-made-gadgets

Welcome to Smart Greenhouse - a smart greenhouse system for farmers

Student Project

## About the system

The system consist of sensor/actuator nodes, a server, and control panels.

Both the control panels and the nodes connects to the server, and communicates via the server.

When a node is added, the user can use the control panel to add sensors with associated actuators.

The user can also check the status of nodes which checks the status of connected sensors and actuators.

If the measured values are too high or too low in relation to the desired values of the user, the associated actuators can be toggled on/off.

### Available Control Panel commands

- CheckGreenhouse
- AddSensor \<nodeId\>
- RemoveSensor \<nodeId\> \<sensorId\>
- CheckNode \<nodeId\>
- ToggleActuator \<nodeId\> \<actuatorId\> \<on|off\>
- Exit

### About commands

#### CheckGreenhouse 

*Check which greenhouse the control panel is connected to, and if there are any connected nodes*

#### AddSensor

*Add one of the available provided sensors to a specific node*

1. Choose sensor type (temperature, light, humidity, co2)
2. Set the sensor ID
3. Set minimum threshold value
4. Set maximum threshold value

If the sensor measure values out of the threshold bound, an alert message will be sent to the control panel

#### RemoveSensor

*Removes a specific sensor from a specific node*

#### CheckNode

*Check the status of a specific node including connected sensors with their current values, and actuators with their current status (on/off)*

#### ToggleActuator

*Toggles a specific actuator on/off*

#### Exit

*Exits the control panel*

## How to run the application

1. Run Server.java
2. Create a node: open a new terminal and type in ***mvn --% exec:java -Dexec.mainClass=network.NodeClient -Dexec.args="01 greenhouse1"***
3. Open ControlPanel: open a new terminal and type in ***mvn --% exec:java -Dexec.mainClass=controlpanel.ControlPanelMain -Dexec.args="cp1 127.0.0.1 5000"***

To add a new node:
- Open a new terminal and type in ***mvn --% exec:java -Dexec.mainClass=network.NodeClient -Dexec.args="<nodeId> <location>"***

To add a new control panel:
- Open a new terminael and type in ***mvn --% exec:java -Dexec.mainClass=controlpanel.ControlPanelMain -Dexec.args="<controlPanelId> 127.0.0.1 5000"***
