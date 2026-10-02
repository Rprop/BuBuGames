package rp.games.baozi.net;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import rp.games.baozi.game.Input;
import rp.games.baozi.game.Rules;

public final class LanHost {
    public interface InputCallback {
        void onInput(int dx, int dy, boolean bomb);
    }

    public interface AgainCallback {
        void onAgain();
    }

    public interface ReadyCallback {
        void onReady();
    }

    public interface EndCallback {
        void onEnded(String message);
    }

    public interface PingCallback {
        void onPing(long rttMs);
    }

    public interface WhoCallback {
        void onWho(String name, int skin);
    }

    private static final String TAG = "Baozi";
    /**
     * 断线后这段时间内连回来，就接着当前这局，不重开。
     */
    private static final long REJOIN_GRACE_NS = 4_000_000_000L;

    private final InputCallback onInput;
    private final AgainCallback onAgain;
    private final ReadyCallback onReady;
    private final EndCallback onEnded;
    private final EndCallback onFailed;
    private final PingCallback onPing;
    private final WhoCallback onWho;
    private final Object writeLock = new Object();
    private final Object poke = new Object();
    private final Object ackLock = new Object();
    private final Object linkLock = new Object();
    private final AtomicReference<String> pending = new AtomicReference<>();
    /**
     * 正在等这一帧的 ack。收到或连接断开后清掉。
     */
    private boolean waitingAck;
    /**
     * 这一帧写出前的时间，纳秒。
     */
    private long snapSentAt;
    /**
     * 上次把延迟交给界面的时间。房主大约每秒刷新一次。
     */
    private volatile long lastPingReportAt;
    /**
     * 断线时刻，纳秒。0 表示没有在等对方重连。
     */
    private long dropAt;
    /**
     * 已经有人进过房间。宽限内连回来不算新的一局。
     */
    private boolean seated;

    private volatile boolean running;
    private volatile boolean sessionAlive;
    private ServerSocket server;
    private volatile Socket client;
    private volatile OutputStreamWriter writer;

    public LanHost(InputCallback onInput, AgainCallback onAgain, ReadyCallback onReady, EndCallback onEnded, EndCallback onFailed, PingCallback onPing, WhoCallback onWho) {
        this.onInput = onInput;
        this.onAgain = onAgain;
        this.onReady = onReady;
        this.onEnded = onEnded;
        this.onFailed = onFailed;
        this.onPing = onPing;
        this.onWho = onWho;
    }

    public void start() {
        running = true;
        startSender();
        Thread thread = new Thread(() -> {
            ServerSocket ss;
            try {
                ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(Rules.PORT));
                ss.setSoTimeout(400);
                server = ss;
            } catch (final Exception e) {
                Log.e(TAG, "HostStart", e);
                if (running) {
                    onFailed.onEnded("端口 " + Rules.PORT + " 绑定失败:" + e.getMessage());
                }
                return;
            }
            while (running) {
                final Socket sock;
                try {
                    sock = ss.accept();
                } catch (final SocketTimeoutException timeout) {
                    if (!running) return;
                    expireGrace();
                    continue;
                } catch (final Exception e) {
                    if (!running) return;
                    Log.e(TAG, "HostLoop", e);
                    continue;
                }
                try {
                    sock.setTcpNoDelay(true);
                    sock.setKeepAlive(true);
                    if (!running) return;
                    final boolean resume = attach(sock);
                    if (!resume && onReady != null) onReady.onReady();
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
                    while (running && sessionAlive && client == sock) {
                        final String line = reader.readLine();
                        if (line == null) break;
                        try {
                            final JSONObject json = new JSONObject(line);
                            if (Protocol.isAgain(json)) {
                                if (onAgain != null) onAgain.onAgain();
                                continue;
                            }
                            final long sentMs = Protocol.pingMs(json);
                            if (sentMs >= 0L) {
                                reply(Protocol.pongLine(sentMs));
                                continue;
                            }
                            if (Protocol.isAck(json)) {
                                noteAck();
                                continue;
                            }
                            if (Protocol.isWho(json)) {
                                if (onWho != null) onWho.onWho(Protocol.whoName(json), Protocol.whoSkin(json));
                                continue;
                            }
                            final Input input = Protocol.parseInput(json);
                            if (input != null) {
                                onInput.onInput(input.dx, input.dy, input.place);
                            }
                        } catch (final Exception ignored) {
                        }
                    }
                } catch (final Exception e) {
                    if (running) loseLinkIfCurrent(sock, e);
                } finally {
                    if (!loseLinkIfCurrent(sock, null)) {
                        try {
                            sock.close();
                        } catch (final Exception ignored) {
                        }
                    }
                }
            }
        }, "baozi-host");
        thread.start();
    }

    /**
     * 立刻回包，不和局面快照互相覆盖。
     */
    public void reply(final String line) {
        final OutputStreamWriter out = writer;
        if (out != null && sessionAlive) {
            try {
                synchronized (writeLock) {
                    out.write(line);
                    out.write('\n');
                    out.flush();
                }
            } catch (final Exception e) {
                loseLinkIfWriter(out, e);
            }
        }
    }

    /**
     * 只留还没写进发送缓冲的最新一帧。缓冲写满时这一帧会停在这里被更新的帧换掉。
     */
    public void send(String line) {
        pending.set(line);
        synchronized (poke) {
            poke.notifyAll();
        }
    }

    private void startSender() {
        new Thread(() -> {
            while (running) {
                final String line = pending.getAndSet(null);
                if (line == null) {
                    synchronized (poke) {
                        if (pending.get() == null) {
                            try {
                                poke.wait(20L);
                            } catch (InterruptedException ignored) {
                                break;
                            }
                        }
                    }
                    continue;
                }
                if (!sessionAlive) continue;
                final OutputStreamWriter out = writer;
                if (out != null) {
                    try {
                        synchronized (writeLock) {
                            armAck();
                            out.write(line);
                            out.write('\n');
                            out.flush();
                        }
                        awaitAck();
                    } catch (final Exception e) {
                        cancelAck();
                        loseLinkIfWriter(out, e);
                    }
                }
            }
        }, "baozi-host-send").start();
    }

    /**
     * 记下发送时间，并开始等对方的 ack。必须在写出之前调用，避免回包比等待更早到达。
     */
    private void armAck() {
        synchronized (ackLock) {
            waitingAck = true;
            snapSentAt = System.nanoTime();
        }
    }

    private void awaitAck() {
        synchronized (ackLock) {
            while (running && sessionAlive && waitingAck) {
                try {
                    ackLock.wait();
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    private void noteAck() {
        final long sentAt;
        synchronized (ackLock) {
            if (!waitingAck) return;
            waitingAck = false;
            sentAt = snapSentAt;
            ackLock.notifyAll();
        }
        if (onPing == null) return;
        long rtt = (System.nanoTime() - sentAt) / 1_000_000L;
        if (rtt < 0L) rtt = 0L;
        if (rtt > 9999L) rtt = 9999L;
        final long now = System.nanoTime();
        if (lastPingReportAt != 0L && now - lastPingReportAt < 1_000_000_000L) return;
        lastPingReportAt = now;
        onPing.onPing(rtt);
    }

    private void cancelAck() {
        synchronized (ackLock) {
            waitingAck = false;
            ackLock.notifyAll();
        }
    }

    /**
     * 换上新连接。宽限内回来则接着打，否则当成新加入。
     */
    private boolean attach(final Socket sock) throws Exception {
        synchronized (linkLock) {
            final long now = System.nanoTime();
            final boolean resume = seated && dropAt != 0L && now - dropAt < REJOIN_GRACE_NS;
            if (!resume) seated = true;
            dropAt = 0L;
            client = sock;
            writer = new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8);
            sessionAlive = true;
            return resume;
        }
    }

    /**
     * 宽限用完仍没连回来，才结束这局。
     */
    private void expireGrace() {
        synchronized (linkLock) {
            if (sessionAlive || dropAt == 0L) return;
            if (System.nanoTime() - dropAt < REJOIN_GRACE_NS) return;
            dropAt = 0L;
            seated = false;
        }
        if (onEnded != null) onEnded.onEnded("连接断开了");
    }

    /**
     * @return 这条连接当时还在用，并且已经卸掉
     */
    private boolean loseLinkIfCurrent(final Socket sock, final Exception error) {
        return loseLink(() -> client == sock, error);
    }

    private void loseLinkIfWriter(final OutputStreamWriter out, final Exception error) {
        loseLink(() -> writer == out, error);
    }

    private boolean loseLink(final SameLink same, final Exception error) {
        final Socket dead;
        final long droppedAt;
        synchronized (linkLock) {
            if (!sessionAlive || !same.matches()) return false;
            sessionAlive = false;
            cancelAck();
            lastPingReportAt = 0L;
            dead = client;
            client = null;
            writer = null;
            droppedAt = System.nanoTime();
            dropAt = droppedAt;
        }
        if (error != null) Log.w(TAG, "连接中断，等待重连", error);
        else Log.w(TAG, "连接中断，等待重连");
        try {
            if (dead != null) dead.close();
        } catch (final Exception ignored) {
        }
        if (onInput == null) return true;
        synchronized (linkLock) {
            if (sessionAlive || dropAt != droppedAt) return true;
        }
        onInput.onInput(0, 0, false);
        return true;
    }

    private interface SameLink {
        boolean matches();
    }

    public void close() {
        running = false;
        sessionAlive = false;
        cancelAck();
        synchronized (poke) {
            poke.notifyAll();
        }
        try {
            if (client != null) client.close();
        } catch (Exception ignored) {
        }
        try {
            if (server != null) server.close();
        } catch (Exception ignored) {
        }
    }
}
