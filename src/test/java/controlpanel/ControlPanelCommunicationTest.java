package controlpanel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test class for ControlPanelCommunication.
 *
 * <p>The following is tested:</p>
 *
 * <b>Positive tests:</b>
 *
 * <ul>
 *   <li>sendJson_writesToInjectedOut: verifies that sendJson writes the provided JSON to the configured PrintWriter.</li>
 *   <li>startListenThread_readsLines_and_invokesCallback: verifies the listening thread reads non-empty lines and invokes the callback.</li>
 *   <li>close_interruptsReaderThread: verifies close() stops the reader thread started by startListenThread.</li>
 * </ul>
 *
 * <b>Negative tests:</b>
 *
 * <ul>
 *   <li>sendJson_withNullOut_doesNotThrow: verify sendJson is safe when the output writer is null.</li>
 *   <li>close_withoutStart_doesNotThrow: verify close() is safe even if the listener thread was never started.</li>
 *   <li>startListenThread_ignoresEmptyLines: verify listening ignores empty lines and does not invoke callback.</li>
 * </ul>
 *
 * @author Group 1
 * @version 2025-11-19
 */
public class ControlPanelCommunicationTest {

  private final PrintStream originalOut = System.out;

  @AfterEach
  public void tearDown() {
    System.setOut(originalOut);
  }

  // ----- POSITIVE TESTS -----

  /**
   * Test that sendJson writes JSON to the injected PrintWriter.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>The provided JSON string is written to the PrintWriter bound to the communication instance.</li>
   * </ul>
   */
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

  /**
   * Test that the listening thread reads non-empty lines and forwards them to the callback.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>The callback receives non-empty lines from the BufferedReader used by the communication instance.</li>
   * </ul>
   */
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

  /**
   * Test that close() interrupts and stops the reader thread.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>The reader thread created by startListenThread is alive after start and stops after close().</li>
   * </ul>
   */
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

  /**
   * Test that sendJson does not throw when the output writer is null.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>Calling sendJson with a null internal PrintWriter should not raise an exception.</li>
   * </ul>
   */
  @Test
  public void sendJson_withNullOut_doesNotThrow() throws Exception {
    ControlPanelCommunication comm = new ControlPanelCommunication(s -> {}, null, "cp-test");

    Field outField = ControlPanelCommunication.class.getDeclaredField("out");
    outField.setAccessible(true);
    outField.set(comm, null);

    assertDoesNotThrow(() -> comm.sendJson("{\"x\":1}"), "sendJson should not throw when out is null");
  }

  /**
   * Test that close() is safe to call even if the listener thread was never started.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>Calling close on a fresh instance does not throw an exception.</li>
   * </ul>
   */
  @Test
  public void close_withoutStart_doesNotThrow() {
    ControlPanelCommunication comm = new ControlPanelCommunication(s -> {}, null, "cp-test");
    assertDoesNotThrow(comm::close, "close() should be safe to call even if listener thread never started");
  }

  /**
   * Test that startListenThread ignores empty lines and does not invoke the callback.
   *
   * <p>Expected outcome:</p>
   * <ul>
   *   <li>No callback invocations occur when the input contains only empty lines.</li>
   * </ul>
   */
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