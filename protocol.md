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
  - If a control panel sends a command for a node that is not connected the server will log the event (`Target node not connected`) and will not forward the command. There is no built-in error reply back to the control panel in the current server implementation for this case (only server-side logging).

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

- Principles: validate early, return structured errors.
- Categories: parsing errors, validation errors, routing errors.
- Server behavior: validate incoming messages.
- Client behavior: validate before sending.

 --------------------------------------------------------------------

 ## 11. Realistic Scenario
 A farmer uses a control panel to monitor Node 7. The panel recieves:
 - SENSOR_DATA|Node7|TEMP|26.5
 - SENSOR_DATA|Node7|HUMIDITY||44  
 Then sends:
 - COMMAND|Node7|ACTUATOR|FAN|ON  
 The fan turns on, and the node replies:
 - ACTUATOR_STATUS|Node7|FAN|ON

 Farmer could also open another control panel or create a new node:
 - SENSOR_DATA|Node3|CO2_SENSOR|800  
 Then sends:
 - COMMAND|Node3|ACTUATOR|CO2_SUPPLY|ON  
 The fan turns on, and the node replies:
 - ACTUATOR_STATUS|Node3|CO2_SUPPLY|ON  
 If the farmer wants to reduce the CO2 he needs to turn off:
 - COMMAND|Node3|ACTUATOR|CO2_SUPPLY|OFF  
 And turn on FAN
 - COMMAND|Node3|ACTUATOR|FAN|ON  
 (Note: It is impossible for both to be on at once.)  


 ## 12. Reliability Mechanisms
 What the protocol  provides (current mechanisms for error handeling):

Thread-safe sending — ControlPanelCommunication.sendJson(String) synchronizes on out and checks that out != null before sending. This prevents corrupted output from multiple threads and handles cases where the connection is not established.
(location: ControlPanelCommunication.sendJson)

Background reader with robust error handling — A dedicated daemon thread (cp-comm-reader) reads line-delimited JSON messages. The reader catches IOException and logs a message when the connection is lost instead of crashing. It also protects against empty lines and exceptions thrown by the callback (onJson).
(location: ControlPanelCommunication.startListenThread)

Graceful close / resource cleanup — ControlPanelCommunication.close() and ControlPanelLogic.close() attempt to interrupt the reader thread and close in, out, and the socket. The method can be called multiple times without throwing errors (exceptions are ignored). This minimizes leaks on network failures or shutdown.
(location: ControlPanelCommunication.close, ControlPanelLogic.close)

Input validation / tolerance for malformed JSON — ControlPanelLogic.handleIncomingJson catches JsonSyntaxException, ignores non-JSON lines, and provides a fallback for messages lacking messageType but containing nodeID. This prevents malformed or partially formatted traffic from breaking state.
(location: ControlPanelLogic.handleIncomingJson)

Idempotence / duplicate control at logic level — Before sending addSensor, both the sensor type and sensor ID are checked so duplicates are not added to the cache; removeSensor also verifies that a sensor exists before sending the request. This reduces unnecessary or conflicting messages to the server.
(location: ControlPanelLogic.addSensor, ControlPanelLogic.removeSensor)

Local alarm deduplication — shownAlerts (a concurrent set) ensures that the same sensor alert is shown only once per control panel, preventing flapping from spamming the UI/terminal. Regex extraction of the sensor makes identification robust against small variations.
(location: ControlPanelLogic.handleAlert and the shownAlerts field)

Short waiting window for requests — When requestNode runs, a REQUEST_NODE is sent and a CountDownLatch waits until one response is received (max wait ~1200 ms). The class also uses requestLatches and requestPrinted to show only the first response within this window so that periodic updates from other nodes do not drown out the requested output. This creates limited synchronization between sender and receiver.
(location: ControlPanelLogic.requestNode, requestLatches, requestPrinted, printNodeState)

Thread-safe internal state — Use of ConcurrentHashMap for main maps (nodes, spawnedNodes, spawnedSockets) and concurrent sets for some structures gives basic thread safety for updates from both the reader thread and UI threads.
(location: fields in ControlPanelLogic)

summary:
The code provides error handeling by messages if the user writes something that is not valid, and solid basic reliability mechanisms—thread-safe I/O, defensive parsing, alarm deduplication, and limited request-response synchronization
Note: there is currently no application-level ACK/messageId or heartbeat implemented; the server logs when a target node is offline and does not send an explicit error back to the control panel — adding messageId/ACK and retry would be required for stronger delivery guarantees."
 

## Justifications
Each design choice above is made to balance simplicity, scalability, and clarity for a distributed smart farming system. TCP was chosen for reliability. Message formats are kept simple for easy parsing. The protocol supports multiple nodes and extensible sensor/actuator types.

Her kan man bullshitte mer
