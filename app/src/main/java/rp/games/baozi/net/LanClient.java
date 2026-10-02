package rp.games.baozi.net;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import rp.games.baozi.game.Input;
import rp.games.baozi.game.InputState;
import rp.games.baozi.game.Snap;

public final class LanClient {
    public interface SnapCallback {
        void onSnap(Snap snap);
    }

    public interface EndCallback {
        void onEnded(String message);
    }

    public interface PingCallback {
        void onPing(int ms);
    }

    private static final String TAG = "Baozi";
    /**
     * 连上之后断线，这段时间内能再连上就接着打。
     */
    private static final long RECONNECT_WINDOW_NS = 2_000_000_000L;
    private static final int RECONNECT_TIMEOUT_MS = 600;

    private final String host;
    private final int port;
    private final InputState input;
    private final SnapCallback onSnap;
    private final PingCallback onPing;
    private final EndCallback onEnded;
    private final String name;
    private final int skin;
    private final Object writeLock = new Object();

    private volatile boolean running;
    private volatile Socket socket;
    private volatile OutputStreamWriter writer;
    private long pingSentMs;
    private long pingSentAt;

    public LanClient(String host, int port, InputState input, SnapCallback onSnap, PingCallback onPing, EndCallback onEnded, String name, int skin) {
        this.host = host;
        this.port = port;
        this.input = input;
        this.onSnap = onSnap;
        this.onPing = onPing;
        this.onEnded = onEnded;
        this.name = name == null ? "" : name;
        this.skin = skin;
    }

    public void start() {
        running = true;
        startSender();
        new Thread(() -> {
            boolean joined = false;
            boolean immediate = true;
            boolean loggedDrop = false;
            long reconnectDeadline = 0L;
            while (running) {
                final Socket sock = new Socket();
                try {
                    sock.setTcpNoDelay(true);
                    sock.setKeepAlive(true);
                    synchronized (writeLock) {
                        socket = sock;
                    }
                    sock.connect(new InetSocketAddress(host, port), joined ? RECONNECT_TIMEOUT_MS : 8000);
                    if (!running) return;
                    attachWriter(sock);
                    joined = true;
                    immediate = true;
                    loggedDrop = false;
                    reconnectDeadline = 0L;
                    readUntilEnd(sock);
                    if (!running) return;
                    noteDrop(null, loggedDrop);
                    loggedDrop = true;
                } catch (final Exception e) {
                    if (!running) return;
                    if (!joined) {
                        Log.e(TAG, "加入失败", e);
                        onEnded.onEnded("连不上房主，确认地址和端口");
                        return;
                    }
                    if (!loggedDrop) Log.w(TAG, "连接中断，尝试重连", e);
                    loggedDrop = true;
                } finally {
                    detach(sock);
                }
                if (!running) return;
                if (reconnectDeadline == 0L) reconnectDeadline = System.nanoTime() + RECONNECT_WINDOW_NS;
                if (System.nanoTime() >= reconnectDeadline) {
                    onEnded.onEnded("和房主断开了");
                    return;
                }
                if (!immediate) {
                    try {
                        Thread.sleep(100L);
                    } catch (final InterruptedException ignored) {
                        return;
                    }
                    if (!running) return;
                }
                immediate = false;
            }
        }, "baozi-client").start();
    }

    public void sendAgain() {
        final OutputStreamWriter out = writer;
        if (out == null || !running) return;
        try {
            synchronized (writeLock) {
                out.write(Protocol.againLine());
                out.write('\n');
                out.flush();
            }
        } catch (Exception ignored) {
        }
    }

    public void close() {
        running = false;
        detach(socket);
    }

    private void attachWriter(final Socket sock) throws Exception {
        final OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8);
        synchronized (writeLock) {
            if (socket != sock) throw new java.io.IOException("closed");
            writer = out;
        }
    }

    private void detach(final Socket sock) {
        if (sock == null) return;
        synchronized (writeLock) {
            if (socket == sock) {
                socket = null;
                writer = null;
            }
        }
        try {
            sock.close();
        } catch (final Exception ignored) {
        }
    }

    private void dropWriter(final OutputStreamWriter out) {
        final Socket dead;
        synchronized (writeLock) {
            if (writer != out) return;
            writer = null;
            dead = socket;
            socket = null;
        }
        if (dead != null) {
            try {
                dead.close();
            } catch (final Exception ignored) {
            }
        }
    }

    private void readUntilEnd(final Socket sock) throws Exception {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
        while (running && socket == sock) {
            final String line = reader.readLine();
            if (line == null) return;
            final JSONObject json;
            try {
                json = new JSONObject(line);
            } catch (final JSONException ignored) {
                continue;
            }
            final long pong = Protocol.pongMs(json);
            if (pong >= 0L) {
                notePong(pong);
                continue;
            }
            final Snap snap = Protocol.parseSnap(json);
            if (snap == null) continue;
            synchronized (writeLock) {
                if (writer == null || socket != sock) return;
                writer.write(Protocol.ackLine());
                writer.write('\n');
                writer.flush();
            }
            onSnap.onSnap(snap);
        }
    }

    private void noteDrop(final Exception error, final boolean logged) {
        if (logged) return;
        if (error == null) Log.w(TAG, "连接中断，尝试重连");
        else Log.w(TAG, "连接中断，尝试重连", error);
    }

    private void startSender() {
        new Thread(() -> {
            Input last = null;
            OutputStreamWriter applied = null;
            while (running) {
                final OutputStreamWriter out = writer;
                if (out == null) {
                    applied = null;
                    try {
                        Thread.sleep(16L);
                    } catch (final InterruptedException ignored) {
                        break;
                    }
                    continue;
                }
                if (out != applied) {
                    applied = out;
                    last = null;
                    pingSentMs = 0L;
                    pingSentAt = 0L;
                    try {
                        sendWho(out);
                    } catch (final Exception ignored) {
                        dropWriter(out);
                        continue;
                    }
                }
                final Input sample = input.poll();
                try {
                    maybePing(out);
                    if (last == null ||
                            sample.dx != last.dx ||
                            sample.dy != last.dy ||
                            sample.place != last.place) {
                        last = sample;
                        final String line = Protocol.inputLine(sample.dx, sample.dy, sample.place);
                        synchronized (writeLock) {
                            if (writer != out) continue;
                            out.write(line);
                            out.write('\n');
                            out.flush();
                        }
                    }
                } catch (final Exception ignored) {
                    dropWriter(out);
                    last = null;
                }
            }
        }, "baozi-send").start();
    }

    private void sendWho(final OutputStreamWriter out) throws Exception {
        synchronized (writeLock) {
            if (writer != out) return;
            out.write(Protocol.whoLine(name, skin));
            out.write('\n');
            out.flush();
        }
    }

    /**
     * 大约每秒测一次。包里带发出时的毫秒时间，回包原样带回。
     */
    private void maybePing(final OutputStreamWriter out) throws Exception {
        final long now = System.nanoTime();
        if (pingSentMs != 0L && now - pingSentAt < Protocol.PING_PERIOD_MS * 1_000_000L) {
            return;
        }
        final long ms = System.currentTimeMillis();
        pingSentMs = ms;
        pingSentAt = now;
        synchronized (writeLock) {
            out.write(Protocol.pingLine(ms));
            out.write('\n');
            out.flush();
        }
    }

    private void notePong(final long ms) {
        if (ms != pingSentMs || onPing == null) return;
        long rtt = System.currentTimeMillis() - ms;
        if (rtt < 0L) rtt = 0L;
        if (rtt > 9999L) rtt = 9999L;
        onPing.onPing((int) rtt);
    }
}
