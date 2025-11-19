package network;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests for Server broadcast behavior.
 * trengs kanksje ikke dersom vi skal ha fokus kunn på kommunikasjon
 */
public class ServerTest {

  @AfterEach
  public void cleanupStatics() throws Exception {
    // Clear static collections to avoid cross-test interference
    Field cpField = Server.class.getDeclaredField("controlPanels");
    cpField.setAccessible(true);
    ((List<?>) cpField.get(null)).clear();

    Field subsField = Server.class.getDeclaredField("controlPanelSubscriptions");
    subsField.setAccessible(true);
    ((Map<?,?>) subsField.get(null)).clear();

    Field lastField = Server.class.getDeclaredField("lastKnownNodeJson");
    lastField.setAccessible(true);
    ((Map<?,?>) lastField.get(null)).clear();

    Field sensorField = Server.class.getDeclaredField("sensorNodes");
    sensorField.setAccessible(true);
    ((Map<?,?>) sensorField.get(null)).clear();
  }

  // ----- POSITIVE TESTS -----
  @Test
  public void broadcast_sendsMessageToConnectedControlPanel() throws Exception {
    ServerSocket ss = new ServerSocket(0);
    try {
      int port = ss.getLocalPort();
      // client connects
      Socket client = new Socket("localhost", port);
      // server-side accepted socket that Server will write to
      Socket serverSide = ss.accept();

      // register serverSide socket into Server.controlPanels
      Field cpField = Server.class.getDeclaredField("controlPanels");
      cpField.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<Socket> cps = (List<Socket>) cpField.get(null);
      cps.add(serverSide);

      // create a ClientHandler instance (package-private nested class)
      Server.ClientHandler handler = new Server.ClientHandler(serverSide);

      // invoke private broadcastToControlPanels(String)
      Method bcast = Server.ClientHandler.class.getDeclaredMethod("broadcastToControlPanels", String.class);
      bcast.setAccessible(true);

      // set client read timeout so test doesn't hang
      client.setSoTimeout(2000);
      BufferedReader clientReader = new BufferedReader(new InputStreamReader(client.getInputStream()));

      String msg = "{\"message\":\"hello-cp\"}";
      bcast.invoke(handler, msg);

      String received = clientReader.readLine(); // should receive the message
      assertNotNull(received, "Client should receive broadcasted message");
      assertTrue(received.contains("hello-cp"), "Content should match broadcast message");

      // cleanup sockets
      client.close();
      serverSide.close();
    } finally {
      ss.close();
    }
  }

  // ----- NEGATIVE TESTS -----
  @Test
  public void broadcast_handlesClosedSocketGracefully() throws Exception {
    ServerSocket ss = new ServerSocket(0);
    try {
      int port = ss.getLocalPort();
      Socket client = new Socket("localhost", port);
      Socket serverSide = ss.accept();

      // add and then immediately close the serverSide socket to simulate broken client
      Field cpField = Server.class.getDeclaredField("controlPanels");
      cpField.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<Socket> cps = (List<Socket>) cpField.get(null);
      cps.add(serverSide);
      serverSide.close(); // closed before broadcast

      Server.ClientHandler handler = new Server.ClientHandler(serverSide);
      Method bcast = Server.ClientHandler.class.getDeclaredMethod("broadcastToControlPanels", String.class);
      bcast.setAccessible(true);

      // calling broadcast should not throw even if socket closed
      assertDoesNotThrow(() -> {
        try {
          bcast.invoke(handler, "{\"x\":1}");
        } catch (Exception e) {
          // wrap reflection exceptions
          throw new RuntimeException(e);
        }
      });

      client.close();
    } finally {
      ss.close();
    }
  }
}