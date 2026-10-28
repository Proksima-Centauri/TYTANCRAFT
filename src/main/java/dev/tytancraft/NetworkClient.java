package dev.tytancraft;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class NetworkClient implements AutoCloseable {
    interface Listener {
        void onFrame(Protocol.Frame frame);
        void onFailure(String message);
    }

    private final Socket socket;
    private final Listener listener;
    private final ArrayBlockingQueue<Protocol.Frame> outgoing = new ArrayBlockingQueue<>(128);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean failureReported = new AtomicBoolean();
    private Thread readerThread;
    private Thread writerThread;

    NetworkClient(String host, int port, Listener listener) throws IOException {
        this.listener = listener;
        socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setSoTimeout(0);
    }

    void start(String nickname) throws IOException {
        writerThread = new Thread(this::writeLoop, "tytancraft-client-writer");
        readerThread = new Thread(this::readLoop, "tytancraft-client-reader");
        writerThread.setDaemon(true);
        readerThread.setDaemon(true);
        writerThread.start();
        readerThread.start();
        send(Protocol.Type.HELLO, Protocol.hello(nickname));
    }

    boolean send(Protocol.Type type, byte[] payload) {
        if (closed.get() || payload.length > Protocol.MAX_PACKET_BYTES) return false;
        if (outgoing.offer(new Protocol.Frame(type, payload))) return true;
        fail("Kolejka wysyłania jest przepełniona.");
        return false;
    }

    private void readLoop() {
        try (DataInputStream input = new DataInputStream(socket.getInputStream())) {
            while (!closed.get()) listener.onFrame(Protocol.readFrame(input));
        } catch (IOException failure) {
            if (!closed.get()) fail(failure.getMessage() == null ? "Połączenie zostało przerwane." : failure.getMessage());
        }
    }

    private void writeLoop() {
        try (DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            while (!closed.get()) {
                Protocol.Frame frame = outgoing.poll(250, TimeUnit.MILLISECONDS);
                if (frame != null) Protocol.writeFrame(output, frame.type(), frame.payload());
            }
        } catch (IOException failure) {
            if (!closed.get()) fail(failure.getMessage() == null ? "Nie udało się wysłać danych." : failure.getMessage());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void fail(String message) {
        if (!closed.compareAndSet(false, true)) return;
        try { socket.close(); } catch (IOException ignored) {}
        if (failureReported.compareAndSet(false, true)) listener.onFailure(message);
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { socket.close(); } catch (IOException ignored) {}
        if (readerThread != null) readerThread.interrupt();
        if (writerThread != null) writerThread.interrupt();
    }
}
