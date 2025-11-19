# Protocol Description - Smart Farming System

## 1. Introduction
This document describes the custom application-layer communication protocol developed for the Smart Farming System project in IDATA2304. The protocol enables communication between sensor/actuator nodes and control-panel nodes using TCP sockets.

## 2. Terminology
- **Node**: A node that collects environmental data through sensors, and hosts actuators.
- **Actuator**: A controllable device (e.g., heater, ventilation, humidifier).
- **Control Panel**: A client that visualizes data and sends commands.
- **Message**: A structured data packet exchanged between nodes.
- **Command**: A control instruction sent from a control panel to a node via the server.

## 3. Transport Protocol
We use **TCP** for reliable communication between nodes.

## 4. Port Number
The protocol uses port **5000** for all node-server communication.

## 5. Architecture
- **NodeClient (client)**
    - Implements entity.Node
    - Connects to the central server over TCP and sends sensor snapshots
    - Listens for commands (e.g. ADD_SENSOR, CheckGreenHouse, CheckNode) and applies changes locally, then sends updated state.
- **ControlPanelMain (client)**
    - Implements controlpanel.ControlPanelLogic + ControlPanelUI + ControlPanelCommunication
    - Connects to the same central server over TCP and issues commands.
    - Recieves node state updates from the server and displays them in the UI.
- **Central Server (server)**
    - Single coordinator listening on TCP port 5000.
    - Accepts connections from nodeclients and control panels, validates node IDs, keeps registry of connected cleints, and forwards messages between nodes and control panels according to messageType.
    - Responsible for routing: e.g. forward ToggleActuator from a control panel to the target sensor code.

## 6. Information Flow

- **Transport:** TCP over port `5000`. The server acts as the single router and long-lived connection manager for both sensor nodes and control panels.

- **Registration / handshake**
  - Sensor nodes use a small legacy plaintext handshake when they first connect: `SENSOR_NODE_CONNECTED <nodeId>` (single line). The server replies with `NODE_ID_ACCEPTED` or `NODE_ID_REJECTED` and stores the node's socket in an internal map if accepted.
  - Control panels may use the legacy `CONTROL_PANEL_CONNECTED` plain-text registration or a JSON registration payload (e.g. `{ "messageType": "REGISTER_CONTROL_PANEL", "controlPanelId": "cp-1" }`). The server keeps a list of control panel sockets for broadcasting.

- **Node -> Server (state & alerts)**
  - Nodes periodically send their state as a single line JSON object. These messages include `messageType` (commonly `SENSOR_DATA_FROM_NODE`), `nodeID`, an array of `sensors`, and an array of `actuators`. The server caches the last-known JSON per `nodeID` (used to quickly serve `REQUEST_NODE`).
  - When a node detects a threshold breach it can send an `ALERT` message (JSON with `messageType":"ALERT"`, `nodeID` and `alert` text). The server forwards these alerts to all connected control panels.

- **Control Panel -> Server -> Node (command forwarding)**
  - Control panels send JSON commands to the server. Typical commands and their flow:
    - `ACTUATOR_COMMAND`: control panel -> server -> target node. JSON shape: `{ "messageType": "ACTUATOR_COMMAND", "controlPanelId": "cp1", "nodeID": "01", "actuatorId": "s1_heater", "command": "TURN_ON/TURN_OFF" }`.
    - `ADD_SENSOR`: control panel -> server -> target node. JSON shape: `{ "messageType": "ADD_SENSOR", "controlPanelId": "cp1", "nodeID": "01", "sensorType": "TEMPERATURE", "sensorId": "s3", "minThreshold": 15, "maxThreshold": 30 }`.
    - `REMOVE_SENSOR`: control panel -> server -> target node. JSON shape: `{ "messageType": "REMOVE_SENSOR", "controlPanelId": "cp1", "nodeID": "n1", "sensorId": "s3" }`.
  - The server looks up the target node's socket in its `sensorNodes` map and forwards the raw JSON line to the node if connected. If the node is not connected, the server logs that the target node is not connected and does not forward the message.

- **Control Panel -> Server (requests and cached replies)**
  - `REQUEST_NODE` (or `REQUEST_STATE`) messages allow a control panel to ask for the latest node snapshot. If the server has a cached last-known JSON for the requested `nodeID`, it immediately replies with that JSON; otherwise it forwards a `REQUEST_STATE` message to the node and waits for the node to send an updated state which will then be broadcast to control panels.

- **Broadcasting / subscriptions**
  - The current implementation broadcasts node updates to the control panel that sent the command. There is no per-control-panel subscription model in the code; control panels are expected to filter messages they care about locally.

- **Failure modes and logging**
  - If a control panel sends a command for a node that is not connected the server will log the event (`Target node not connected`) and will not forward the command. There is no built-in error reply back to the control panel in the current server implementation for this case (only server-side logging).

## 7. Protocol Type

- **



## 8. Constants and Types

- Message types (application layer)
  - `SENSOR_DATA` — node → server (periodic snapshot)
  - `ACTUATOR_STATUS` — node → server (actuator state report)
  - `COMMAND` — control panel → server → node (actuator control)
  - `REQUEST` — control panel → server → node (on-demand state request)
  - `ADD_SENSOR` — control panel → server → node (runtime sensor add)
  - `ERROR` — any → server/control panel (error reporting)

- Sensor types
  - `TEMPERATURE`
  - `HUMIDITY`
  - `LIGHT`
  - `CO2`
 
- Actuator types
  - `FAN`
  - `HEATER`
  - `AIRCON`
  - `VENTILATION`
  - `CO2Supply`
  - `HUMIDIFIER`
  - `DEHUMIDIFIER`
  - `LAMPBRIGHTNING`
  - `LIGHTDIMMING`

- Status / state values
  - Binary / boolean: `ON` / `OFF` (or true/false)
  - Numeric readings: use numeric values (e.g., temperature = 21.5)

------------------------------------------------------------------

## 9. Message Framing and Format

- **Framing / marshalling:** application messages are sent as single-line JSON payloads (line-delimited JSON). Each message occupies one text line terminated by `\n`. The server treats a received line starting with `{` as JSON otherwise it interprets some legacy plaintext handshake messages (see registration above).

- **Message envelope (required fields):** most messages are JSON objects containing at least:
  - `messageType` &mdash; string that indicates the semantic type (e.g. `SENSOR_DATA_FROM_NODE`, `ACTUATOR_COMMAND`, `ADD_SENSOR`, `ALERT`, `REQUEST_NODE`).
  - `nodeID` &mdash; the target or source node id (when applicable).

- **Common application-layer message types (concrete)**
  - `SENSOR_DATA_FROM_NODE` &mdash; node -> server. Contains `nodeID`, `sensors` (array of sensor objects, with their `sensorId`, `value`, `unit`, `minTreshold`, `maxTreshold`, `timestamp`), `actuators` (array of actuator objects, with their `actuatorId`, `actuatorType`, `on status`).
  - `ACTUATOR_COMMAND` &mdash; control panel -> server -> node. Contains `nodeID`, `actuatorId`, `command` (`TURN_ON` / `TURN_OFF`).
  - `ADD_SENSOR` / `REMOVE_SENSOR` &mdash; control panel -> server -> node. Contains sensor metadata (`sensorType`, `sensorId`, `minThreshold`, `maxThreshold`).
  - `REQUEST_NODE` &mdash; control panel -> server -> (possibly forwarded to node). Server may answer directly using cached JSON. Contains `controlPaneId` and `nodeId`
  - `ALERT` &mdash; node -> server -> control panels. Contains `nodeID` and `alert` text.

- **Legacy plain-text tokens**
  - `SENSOR_NODE_CONNECTED <nodeId>` &mdash; node -> server (plain text). Server responds with `NODE_ID_ACCEPTED` or `NODE_ID_REJECTED` (plain text).
  - `CONTROL_PANEL_CONNECTED` &mdash; legacy control-panel registration.

- **Routing / server behavior**
  - The server keeps a map `sensorNodes: Map<nodeId, Socket>` and a list of control panel sockets. On receiving a control-panel JSON command with `nodeID`, the server looks up the node socket and forwards the original JSON line if connected. Node-originated JSON lines are cached under `lastKnownNodeJson[nodeID]` and broadcast to all control panels.

- **Security / reliability notes (current limitations)**
  - There are no cryptographic protections on the wire (no TLS) and no message authentication: the protocol assumes a trusted environment.
  - There is no built-in acknowledgement (ACK) from the target node back to the control panel for forwarded commands; the server forwards raw JSON and logs non-delivery. If higher assurance is required, we should add explicit ACK/response messages and timeouts.
 
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
