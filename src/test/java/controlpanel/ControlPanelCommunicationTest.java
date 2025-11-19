package controlpanel;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * ControlPanelCommunication tests.
 * ----- POSITIVE TESTS ----- (happy path)
 * ----- NEGATIVE TESTS ----- (error / edge cases)
 */
public class ControlPanelCommunicationTest {

  private final PrintStream originalOut = System.out;

  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
  }

  // ----- POSITIVE TESTS -----

  @Test
  public void sendJson_writesToInjectedOut() throws Exception {
    ControlPanelCommunication comm = new ControlPanelCommunication(s -> {}, null, "cp-test");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(baos, true);

    Field outField = ControlPanelCommunication.class.getDeclaredField("out");
    outField.setAccessible(true);
    outField.set(comm, pw);

    String json = "{\"message\":\"hello\"}";
    comm.sendJson(json);

    String written = baos.toString();
    assertTrue(written.contains(json), "sendJson should write JSON to the injected PrintWriter");
  }

  @Test
  public void startListenThread_readsLines_and_invokesCallback() throws Exception {
    List<String> received = new CopyOnWriteArrayList<>();
    ControlPanelCommunication comm = new ControlPanelCommunication(received::add, null, "cp-test");

    String input = "line1\n\nline2\n";
    BufferedReader br = new BufferedReader(new StringReader(input));

    Field inField = ControlPanelCommunication.class.getDeclaredField("in");
    inField.setAccessible(true);
    inField.set(comm, br);

    Method startMethod = ControlPanelCommunication.class.getDeclaredMethod("startListenThread");
    startMethod.setAccessible(true);
    startMethod.invoke(comm);

    Thread.sleep(150);

    assertTrue(received.contains("line1"), "callback should receive first non-empty line");
    assertTrue(received.contains("line2"), "callback should receive second non-empty line");

    // stop thread if still alive
    comm.close();
  }

  @Test
  public void close_interruptsReaderThread() throws Exception {
    ControlPanelCommunication comm = new ControlPanelCommunication(s -> {}, null, "cp-test");

    PipedWriter pw = new PipedWriter();
    PipedReader pr = new PipedReader(pw);
    BufferedReader br = new BufferedReader(pr);

    Field inField = ControlPanelCommunication.class.getDeclaredField("in");
    inField.setAccessible(true);
    inField.set(comm, br);

    Method startMethod = ControlPanelCommunication.class.getDeclaredMethod("startListenThread");
    startMethod.setAccessible(true);
    startMethod.invoke(comm);

    Field readerField = ControlPanelCommunication.class.getDeclaredField("readerThread");
    readerField.setAccessible(true);
    Thread reader = (Thread) readerField.get(comm);

    assertNotNull(reader, "readerThread should be created");
    assertTrue(reader.isAlive(), "readerThread should be alive after start");

    comm.close();

    reader.join(500);
    assertFalse(reader.isAlive(), "readerThread should stop after close()");
  }

  // ----- NEGATIVE TESTS -----

  @Test
  public void sendJson_withNullOut_doesNotThrow() throws Exception {
    ControlPanelCommunication comm = new ControlPanelCommunication(s -> {}, null, "cp-test");

    Field outField = ControlPanelCommunication.class.getDeclaredField("out");
    outField.setAccessible(true);
    outField.set(comm, null);

    assertDoesNotThrow(() -> comm.sendJson("{\"x\":1}"), "sendJson should not throw when out is null");
  }

  @Test
  public void close_withoutStart_doesNotThrow() {
    ControlPanelCommunication comm = new ControlPanelCommunication(s -> {}, null, "cp-test");
    assertDoesNotThrow(comm::close, "close() should be safe to call even if listener thread never started");
  }

  @Test
  public void startListenThread_ignoresEmptyLines() throws Exception {
    List<String> received = new ArrayList<>();
    ControlPanelCommunication comm = new ControlPanelCommunication(received::add, null, "cp-test");

    String input = "\n\n\n"; // only empty lines
    BufferedReader br = new BufferedReader(new StringReader(input));
    Field inField = ControlPanelCommunication.class.getDeclaredField("in");
    inField.setAccessible(true);
    inField.set(comm, br);

    Method startMethod = ControlPanelCommunication.class.getDeclaredMethod("startListenThread");
    startMethod.setAccessible(true);
    startMethod.invoke(comm);

    Thread.sleep(100);

    assertTrue(received.isEmpty(), "No callbacks should be invoked for only-empty input");

    comm.close();
  }
}