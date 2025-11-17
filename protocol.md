# Protocol Description - Smart Farming System

## 1. Introduction
This document describes the custom application-layer communication protocol developed for the Smart Farming System project in IDATA2304. The protocol enables communication between sensor/actuator nodes and control-panel nodes using TCP sockets.

## 2. Terminology
- **Sensor Node**: A node that collects environmental data and hosts actuators.
- **Actuator**: A controllable device (e.g., fan, heater, window opener).
- **Control Panel Node**: A node that visualizes data and sends commands.
- **Message**: A structured data packet exchanged between nodes.
- **Command**: A control instruction sent from a control panel to a sensor node.

## 3. Transport Protocol
We use **TCP** for reliable communication between nodes.

## 4. Port Number
The protocol uses port **5000** for all node-server communication.

## 5. Architecture
- **Sensor Node (client)**
    - Implements entity.Node and network.NodeClient.
    - Connects to the central server over TCP and sends sensor snapshots
    - Listens for commands (ACTUATOR_COMMAND, REQUEST_NODE, ADD_SENSOR) and applies changes locally, then sends updated state.
- **Control Panel (client)**
    - Implements controlpanel.ControlPanelLogic + ControlPanelUI / ControlPanelMain.
    - Connects to the same central server over TCP and issues commands.
    - Recieves node state updates from the server and displays them in the UI.
- **Central Server (server)**
    - Single coordinator listening on TCP port 5000.
    - Accepts connections from sensor nodes and control panels, validates node IDs, keeps registry of connected cleints, and forwards messages between nodes and control panels according to messageType.
    - Responsible for routing: e.g. forward ACTUATOR_COMMAND from a control panel to the target sensor code.

## 6. Informmation Flow.
- **Transport:** TCP over port 5000. Server is the single router.
- **Sensor nodes -> Server**
    - Sensor nodes periodically send their state to the server.
- **Control panels -> Server -> Sensor node (command forwarding)**
    - User issues a command in the UI. Control panel sends the command to the server, which forwards it to the target sensor node.
- **Control panels -> Server**
    - Control panel can request the state of a node. Server forwards the request to node or replies from cached state.
- **Control panels -> Server -> Sensor node (runtime changes)**
    - Control panels can instruct nodes to add sensors. Server forwards to the target node, node applies the change and replies with updated state.

## 7. Protocol Type
- **Connection-oriented (TCP):** uses stream sockets for reliable, ordered delivery between clients and central server.
- **Stateful server:** the server keeps active connections and a registry of known node/client IDs (used for routing)
- **Application-layer messages:** simple JSON messages with a messageType field (extensible and human-readable)
- **Router pattern:** single server acts as the central router - supports point-to-point forwarding (control-panel -> node).
- **Design goals:** reliability and simplicity (TCP + JSON), easy to extend with new messageTypes, and straightforward error/reconnect handling on client side.

## 8. Constants and Types

- Message types (application layer)
  - `SENSOR_DATA` — node → server (periodic snapshot)
  - `ACTUATOR_STATUS` — node → server (actuator state report)
  - `COMMAND` — control panel → server → node (actuator control)
  - `REQUEST` — control panel → server → node (on-demand state request)
  - `ADD_SENSOR` — control panel → server → node (runtime sensor add)
  - `ERROR` — any → server/control panel (error reporting)

- Sensor types
  - `TEMPERATURE` / `TEMP`
  - `HUMIDITY`
  - `LIGHT`
  - `CO2`
 
- Actuator types
  - `FAN`
  - `HEATER`
  - `HUMIDIFIER`
  - `WINDOW`
  - `LAMPS`

- Status / state values
  - Binary / boolean: `ON` / `OFF` (or true/false)
  - Position/state: `OPEN` / `CLOSED`
  - Numeric readings: use numeric values (e.g., temperature = 21.5)

------------------------------------------------------------------

  ## 9. Message Format MÅ FINNE UT AV DET HER
- **Marshalling**: Fixed-size fields / separator-based / TLV (choose one)



- **Examples**:



- **Direction**:
 
 --------------------------------------------------------------------

 ## 10. Error Handling

 Det her må også gås over, har ingen anelse.

 --------------------------------------------------------------------

 ## 11. Realistic Scenario
 A farmer uses a control panel to monitor Node 7. The panel recieves:
 - SENSOR_DATA|Node7|TEMP|26.5
 - SENSOR_DATA|Node7|HUMIDITY||44
 Then sends:
 - COMMAND|Node7|ACTUATOR|FAN|ON
 The fan turns on, and the node replies:
 - ACTUATOR_STATUS|Node7|FAN|ON

 ## 12. Reliability Mechanisms

 - Transport guarantees
    - Use TCP(connection-oriented) for ordered, reliable delivery.

 - Må fikse mer her idk

## Justifications
Each design choice above is made to balance simplicity, scalability, and clarity for a distributed smart farming system. TCP was chosen for reliability. Message formats are kept simple for easy parsing. The protocol supports multiple nodes and extensible sensor/actuator types.

Her kan man bullshitte mer