# IDATA2304-project-student-made-gadgets

Welcome to Smart Greenhouse - a smart greenhouse system for farmers

Student Project

## About the system

The system consist of sensor/actuator nodes, a server, and control panels.

Both the control panels and the nodes connects to the server.

## How to test the application

1. Run Server.java
2. Create a node: open a new terminal and type in ***mvn --% exec:java -Dexec.mainClass=network.NodeClient -Dexec.args="01 greenhouse1"***
3. Open ControlPanel: open a new terminal and type in ***mvn --% exec:java -Dexec.mainClass=controlpanel.ControlPanelMain -Dexec.args="cp1 127.0.0.1 5000"***
