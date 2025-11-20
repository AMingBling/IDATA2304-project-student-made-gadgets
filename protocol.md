# Protocol Description - Smart Farming System

## 1. Introduction
This document describes the custom application-layer communication protocol developed for the Smart Greenhouse System project in IDATA2304. The protocol enables communication between sensor/actuator nodes and control-panels using TCP sockets.

## 2. Terminology
- **Node**: A node that collects environmental data through sensors, and hosts actuators.
- **Actuator**: A controllable device (e.g., heater, ventilation, humidifier).
- **Control Panel**: A client that visualizes data and sends commands.
- **Message**: A structured data packet exchanged between clients.
- **Command**: A control instruction sent from a control panel to a node via the server.
- **Tick**: A "tick" is a short periodic message sent by a node to indicate it is alive and report status — essentially the same as a "heartbeat".

## 3. Transport Protocol
We use **TCP** for reliable communication between nodes.
TCP was choosen over UDP for this project because of better reliability, connection- oriented communication and order og guaranteed delivery.
- **Connection-Oriented Communication**: TCP establishes a persistent connection between nodes, control panels, and the server. This connection-oriented model simplifies the communication logic, as the server can maintain state about connected clients and route messages accordingly.
- **Reliability**: TCP ensures reliable data transmission by providing mechanisms like error checking, retransmission of lost packets, and in-order delivery. This is critical for our application, as sensor data, commands, and state updates must be delivered accurately and in the correct sequence to maintain system integrity.
- **Order of guarantee**:In our system, the order of messages is important (e.g., actuator commands must be applied in the correct sequence). TCP guarantees that messages are delivered in the order they were sent, which UDP does not provide.

## 4. Port Number
The protocol uses port **5000** for all node-server communication.

## 5. Architecture
- **NodeClient (client)**
    - Implements entity.Node
    - Connects to the server over TCP and sends sensor snapshots
    - Listens for commands (e.g. ADD_SENSOR, CheckGreenHouse, CheckNode) and applies changes locally, then sends updated state.
- **ControlPanelMain (client)**
    - Implements controlpanel.ControlPanelLogic + ControlPanelUI + ControlPanelCommunication
    - Connects to the same server over TCP and issues commands.
    - Recieves node state updates from the server and displays them in the UI.
- **Server**
    - Single coordinator listening on TCP port 5000.
    - Accepts connections from nodes and control panels, validates node IDs, keeps registry of connected clients, and forwards messages between nodes and control panels according to messageType.
    - Responsible for routing: e.g. forward ToggleActuator from a control panel to the target node with the right actuatuor.

## 6. Information Flow

- **Transport:** TCP over port `5000`. The server acts as the single router and long-lived connection manager for both nodes and control panels.

- **Registration / handshake**
  - Nodes use a small plaintext handshake when they first connect: `SENSOR_NODE_CONNECTED <nodeId>` (single line). The server replies with `NODE_ID_ACCEPTED` or `NODE_ID_REJECTED` and stores the node's socket in an internal map if accepted.
  - Control panels may use the `CONTROL_PANEL_CONNECTED` plain-text registration or a JSON registration payload (e.g. `{ "messageType": "REGISTER_CONTROL_PANEL", "controlPanelId": "cp-1" }`). The server keeps a list of control panel sockets for broadcasting.

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
  - Network disconnections:
    - If a node or control panel disconnects from the server, the detection is logged and the socket is removed from the server's internal maps/lists.
    - If a node disconnects, the detection is logged in control panels.
  - Invalid messages:
    - The server validates incoming JSON messages for required fields (e.g. `messageType`, `nodeID`). Invalid messages are logged and ignored.
    - Control panels validate commands before sending to avoid malformed requests.
  - Log format and visibility
    - Server and NodeClient use timestamped log helpers for important events.

## 7. Protocol Type

### Connection model: Connection-oriented (TCP)

- The application-layer protocol runs over TCP, which provides reliable, ordered delivery of messages. Each node and control panel maintains a persistent TCP connection to the server, and keeps a long‑lived socket and a reader thread. The messages are line-delimited JSON sent over the established connection.

### State model: Stateful at application layer

- The server maintains state about connected nodes (mapping of `nodeID` to socket) and control panels (list of sockets). It also caches the last-known JSON snapshot per node for quick replies to `REQUEST_NODE`.
- Nodes maintain local state about their sensors and actuators, and periodically send snapshots to the server.
- Control panels maintain local state about known nodes and their last-received snapshots.

The protocol relies on this state to route messages correctly and provide up-to-date information.


## 8. Constants and Types

### General conventions

  - Identifiers and enum-like names: String (e.g. `nodeID`, `controlPanelId`, `sensorId`, `actuatorId`, `sensorType`, `actuatorType`, `messageType`, `command`, `alert`).
  - Numeric sensor values and thresholds: number (double precision).
  - Actuator snapshot state: boolean `on`.
  - Wire format: one JSON object per line (string).

### Message types (application layer)

  - `SENSOR_DATA_FROM_NODE` — node → server (periodic snapshot)
  - `ACTUATOR_STATUS` — node → server (actuator state report)
  - `ACTUATOR_COMMAND` — control panel → server → node (actuator control)
  - `REQUEST_NODE` / `REQUEST_STATE` — control panel → server → node (on-demand state request)
  - `ADD_SENSOR` — control panel → server → node (runtime sensor add)
  - `REMOVE_SENSOR` — control panel → server → node (runtime sensor remove)
  - `ALERT` — node → server → control panel (threshold breach alert)
  - `REGISTER_CONTROL_PANEL` — control panel → server (registration)
  -  `CONTROL_PANEL_CONNECTED` — control panel → server (registration)
  - `SENSOR_NODE_CONNECTED` — node → server (registration)
  - `NODE_ID_ACCEPTED` / `NODE_ID_REJECTED` — server → node (registration response)
 

### Why these types were chosen

- Strings for ids/types: readable, interoperable, match Java getters (`getNodeID()`, `getSensorId()`, `getActuatorType()`).
- Doubles for sensor values/thresholds: required for numeric values and validation.
- Boolean `on` in snapshots: unambiguous state and directly maps to `Actuator.isOn()` / `Actuator.setOn()`.
- String `command`: flexible to encode verbs and parameters and matches current implementation.


------------------------------------------------------------------

## 9. Message Framing and Format

- **Framing / marshalling:** application messages are sent as single-line JSON payloads (line-delimited JSON). Each message occupies one text line terminated by `\n`. The server treats a received line starting with `{` as JSON otherwise it interprets some plaintext handshake messages (see registration above).

- **Message envelope (required fields):** most messages are JSON objects containing at least:
  - `messageType` &mdash; string that indicates the semantic type (e.g. `SENSOR_DATA_FROM_NODE`, `ACTUATOR_COMMAND`, `ADD_SENSOR`, `ALERT`, `REQUEST_NODE`).
  - `nodeID` &mdash; the target or source node id (when applicable).

- **Common application-layer message types (concrete)**
  - `SENSOR_DATA_FROM_NODE` &mdash; node -> server. Contains `nodeID`, `sensors` (array of sensor objects, with their `sensorId`, `value`, `unit`, `minTreshold`, `maxTreshold`, `timestamp`), `actuators` (array of actuator objects, with their `actuatorId`, `actuatorType`, `on status`).
  - `ACTUATOR_COMMAND` &mdash; control panel -> server -> node. Contains `nodeID`, `actuatorId`, `command` (`TURN_ON` / `TURN_OFF`).
  - `ADD_SENSOR` / `REMOVE_SENSOR` &mdash; control panel -> server -> node. Contains sensor metadata (`sensorType`, `sensorId`, `minThreshold`, `maxThreshold`).
  - `REQUEST_NODE` &mdash; control panel -> server -> (possibly forwarded to node). Server may answer directly using cached JSON. Contains `controlPaneId` and `nodeId`
  - `ALERT` &mdash; node -> server -> control panels. Contains `nodeID` and `alert` text.

- **Plain-text tokens**
  - `SENSOR_NODE_CONNECTED <nodeId>` &mdash; node -> server (plain text). Server responds with `NODE_ID_ACCEPTED` or `NODE_ID_REJECTED` (plain text).
  - `CONTROL_PANEL_CONNECTED` &mdash; control-panel registration.

- **Routing / server behavior**
  - The server keeps a map `sensorNodes: Map<nodeId, Socket>` and a list of control panel sockets. On receiving a control-panel JSON command with `nodeID`, the server looks up the node socket and forwards the original JSON line if connected. Node-originated JSON lines are cached under `lastKnownNodeJson[nodeID]` and broadcast to all control panels.

- **Security / reliability notes (current limitations)**
  - There are no cryptographic protections and no message authentication: the protocol assumes a trusted environment.
  - There is no built-in acknowledgement (ACK) from the target node back to the control panel for forwarded commands; the server forwards raw JSON and logs non-delivery. If higher assurance is required, we should add explicit ACK/response messages and timeouts.
 
 --------------------------------------------------------------------

 ## 10. Error Handling

- Principles: validate early, return structured errors.
- Categories: parsing errors, validation errors, routing errors.
- Server behavior: validate incoming messages.
- Client behavior: validate before sending.
- The control panel includes additional validation at the user interface level. If the user provides invalid inputs, the application detects and handles these errors locally before attempting to send the message.


 ## 11. Realistic Scenario

 A farmer uses a control panel to monitor Node 7. The panel recieves:
 - SENSOR_DATA | Node7 | TEMP | 26.5
 - SENSOR_DATA | Node7 | HUMIDITY | 44  

 Then sends:
 - COMMAND | Node7 | ACTUATOR | HUMIDIFIER | ON  

 The humidifier turns on, and the node replies:
 - ACTUATOR_STATUS | Node7 | FAN | ON

 Farmer could also open another control panel or create a new node:
 - SENSOR_DATA | Node3 | CO2_SENSOR | 800

 Then sends:
 - COMMAND | Node3 | ACTUATOR | CO2_SUPPLY | ON  

 The CO2-supply turns on, and the node replies:
 - ACTUATOR_STATUS | Node3 | CO2_SUPPLY | ON  

 If the farmer wants to reduce the CO2 he needs to turn off:
 - COMMAND | Node3 | ACTUATOR | CO2_SUPPLY | OFF  

 And turn on ventilation:
 - COMMAND | Node3 | ACTUATOR | VENTILATION | ON  
 (Note: It is impossible for two conflicting actuators to be on at once.) 


 ## 12. Reliability Mechanisms

 What the protocol  provides (current mechanisms for error handling):

- **Server-side caching**: server caches last-known JSON per nodeID to quickly reply to REQUEST_NODE without waiting for the node to respond.
- **Node registration validation**: server validates nodeID on SENSOR_NODE_CONNECTED and replies with NODE_ID_ACCEPTED or NODE_ID_REJECTED.
- **Node disconnect notification**: server broadcasts SENSOR_NODE_DISCONNECTED to control panels when a node disconnects.
- **Control panel registration**: control panels sends REGISTER_CONTROL_PANEL on connect and the server keeps list of control panel sockets for broadcasting.
- **Control panel command validation**: ControlPanelLogic validates commands before sending to avoid malformed requests.
- **Control panel disconnect handling**: server detects control panel disconnects and removes sockets from list.
- **Short request-window**: ControlPanelLogic.requestNode uses CountDownLatch to wait for one response within ~1200 ms, reducing chance of blocking indefinitely.
- **Defensive coding**: try-catch blocks around I/O and JSON parsing to prevent crashes on malformed input or network errors.

What is missing / limitations:

- **No application-level ACK/messageId**: there is no explicit ACK from the target node back to the control panel for forwarded commands. The server logs non-delivery but does not inform the control panel. This limits delivery guarantees.
- **No persistent durable storage**: server does not persist state across restarts. All state is in-memory.
- **No reconnection logic**: nodes and control panels must manually reconnect if disconnected, and previous state is lost.
- No explicit time-to-live or expiry for cached node state beyond manual removal on disconnect.
- No security/authentication/encryption described.

## Justifications
Each design choice above is made to balance simplicity, scalability, and clarity for a distributed smart farming system. TCP was chosen for reliability. Message formats are kept simple for easy parsing. The protocol supports multiple nodes and extensible sensor/actuator types.
