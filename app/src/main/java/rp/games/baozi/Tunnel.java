package rp.games.baozi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import rp.games.baozi.game.Rules;

/**
 * 把本机游戏端口经 l2tp_transfer 转到服务器上的远程端口。
 */
public final class Tunnel {
    private static final String PREF = "baozi_forward";
    private static final String BIN = "libl2tp_transfer.so";

    public interface Listener {
        void onChanged();
    }

    private static Process process;
    private static volatile boolean starting;
    private static volatile boolean stopAsked;
    private static volatile String liveHost;
    private static volatile int livePort;
    private static volatile String status = "未开启";
    private static volatile String detail = "";
    private static Listener listener;

    public static void setListener(final Listener next) {
        listener = next;
    }

    public static boolean isOn() {
        synchronized (Tunnel.class) {
            return starting || (process != null && process.isAlive());
        }
    }

    /**
     * 转发开着时，房间二维码用这个地址。
     */
    public static String roomAddress() {
        synchronized (Tunnel.class) {
            if (liveHost == null || livePort <= 0 || stopAsked) return null;
            boolean alive = starting || (process != null && process.isAlive());
            if (!alive) return null;
            if (liveHost.indexOf(':') >= 0) return "[" + liveHost + "]:" + livePort;
            return liveHost + ":" + livePort;
        }
    }

    public static String statusText() {
        final String extra = detail;
        if (extra == null || extra.isEmpty()) return status;
        return status + "\n" + extra;
    }

    public static void save(final Context context, final String server, final String user, final String pass, final int port) {
        prefs(context).edit()
                .putString("server", server)
                .putString("user", user)
                .putString("pass", pass)
                .putInt("port", port)
                .apply();
    }

    public static String server(final Context context) {
        return prefs(context).getString("server", "");
    }

    public static String user(final Context context) {
        return prefs(context).getString("user", "");
    }

    public static String pass(final Context context) {
        return prefs(context).getString("pass", "");
    }

    public static int port(final Context context) {
        return prefs(context).getInt("port", 0);
    }

    public static boolean arm64() {
        for (final String abi : Build.SUPPORTED_ABIS) {
            if ("arm64-v8a".equals(abi)) return true;
        }
        return false;
    }

    public static void start(final Context context, final String server, final String user, final String pass, final int remotePort) {
        final Context app = context.getApplicationContext();
        synchronized (Tunnel.class) {
            if (starting || (process != null && process.isAlive())) return;
            starting = true;
            stopAsked = false;
            liveHost = server;
            livePort = remotePort;
            detail = "";
            status = "正在开启…";
        }
        publish();
        final Thread thread = new Thread(() -> run(app, server, user, pass, remotePort), "l2tp-transfer");
        thread.setDaemon(true);
        thread.start();
    }

    public static void stop() {
        final Process current;
        synchronized (Tunnel.class) {
            stopAsked = true;
            current = process;
            if (current == null && !starting) {
                liveHost = null;
                livePort = 0;
                detail = "";
                status = "未开启";
            }
        }
        if (current != null) {
            current.destroy();
            current.destroyForcibly();
        }
        publish();
    }

    private static void run(final Context app, final String server, final String user, final String pass, final int remotePort) {
        Process started = null;
        try {
            final File bin = binary(app);
            final ProcessBuilder builder = new ProcessBuilder(
                    bin.getAbsolutePath(),
                    "-s", server,
                    "-u", user,
                    "-P", pass,
                    "-R", remotePort + ":" + Rules.PORT
            );
            builder.redirectErrorStream(true);
            builder.directory(app.getFilesDir());
            started = builder.start();
            synchronized (Tunnel.class) {
                if (stopAsked) {
                    started.destroy();
                    started.destroyForcibly();
                    process = null;
                    liveHost = null;
                    livePort = 0;
                    detail = "";
                    status = "转发已关闭";
                    starting = false;
                    publish();
                    return;
                }
                process = started;
                status = "转发已开启，创建房间的二维码是 " + shownAddress(server, remotePort);
                starting = false;
            }
            publish();
            String last = "";
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(started.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    Log.i("Tunnel", trimmed);
                    if (trimmed.length() > 120) trimmed = trimmed.substring(0, 120);
                    last = trimmed;
                    synchronized (Tunnel.class) {
                        detail = last;
                    }
                    publish();
                }
            }
            final int code = started.waitFor();
            synchronized (Tunnel.class) {
                process = null;
                liveHost = null;
                livePort = 0;
                starting = false;
                if (stopAsked) {
                    detail = "";
                    status = "转发已关闭";
                } else if (last.isEmpty()) {
                    detail = "";
                    status = "转发已退出（" + code + "）";
                } else {
                    detail = "";
                    status = "转发已退出（" + code + "）\n" + last;
                }
            }
        } catch (Exception error) {
            Log.e("Tunnel", "start failed", error);
            if (started != null) {
                started.destroy();
                started.destroyForcibly();
            }
            String message = error.getMessage();
            if (message == null || message.isEmpty()) message = error.getClass().getSimpleName();
            synchronized (Tunnel.class) {
                process = null;
                liveHost = null;
                livePort = 0;
                starting = false;
                detail = "";
                status = stopAsked ? "转发已关闭" : "启动失败：" + message;
            }
        }
        publish();
    }

    private static String shownAddress(final String server, final int remotePort) {
        if (server.indexOf(':') >= 0) return "[" + server + "]:" + remotePort;
        return server + ":" + remotePort;
    }

    private static File binary(final Context context) {
        return new File(context.getApplicationInfo().nativeLibraryDir, BIN);
    }

    private static SharedPreferences prefs(final Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static void publish() {
        final Listener current = listener;
        if (current != null) current.onChanged();
    }
}
