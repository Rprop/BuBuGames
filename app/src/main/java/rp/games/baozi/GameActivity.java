package rp.games.baozi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.window.OnBackInvokedDispatcher;

import java.util.concurrent.ConcurrentLinkedQueue;

import rp.games.baozi.game.GameWorld;
import rp.games.baozi.game.InputState;
import rp.games.baozi.game.Look;
import rp.games.baozi.game.Phase;
import rp.games.baozi.game.Rules;
import rp.games.baozi.game.SnapBuffer;
import rp.games.baozi.net.LanClient;
import rp.games.baozi.net.LanHost;
import rp.games.baozi.net.Net;
import rp.games.baozi.net.Protocol;

/**
 * 房主跑完整模拟并广播画面。加入方只发送摇杆和放泡，占另一个槽位。
 * 单机练习时，另一个槽位由电脑操控。昵称和皮肤各自在设置里保存。
 */
public class GameActivity extends Activity {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_HOST = "host";
    public static final String EXTRA_PORT = "port";
    public static final String EXTRA_SECONDS = "seconds";

    private GameWorld world;
    private SnapBuffer buffer;
    private GameView view;
    private final InputState remoteInput = new InputState();
    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

    private LanHost host;
    private LanClient client;
    private Thread loop;
    private volatile boolean running;
    private volatile boolean paused;
    private String mode = "local";
    private String hostIp = "";
    private int hostPort = Rules.PORT;
    private android.graphics.Bitmap hostQr;
    private String hostLabel = "";
    private int roomEpoch;
    private boolean leaveAsk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = "local";
        hostIp = getIntent().getStringExtra(EXTRA_HOST);
        if (hostIp == null) hostIp = "";
        hostPort = getIntent().getIntExtra(EXTRA_PORT, Rules.PORT);
        if (hostPort < 1 || hostPort > 65535) hostPort = Rules.PORT;
        world = new GameWorld();
        if (!"join".equals(mode)) {
            int seconds = getIntent().getIntExtra(EXTRA_SECONDS, (int) Rules.MATCH_SECONDS);
            if (seconds > 0) {
                world.matchSeconds = seconds;
                world.left = seconds;
            }
        }
        buffer = new SnapBuffer();
        view = new GameView(this);
        view.buffer = buffer;
        view.mySlot = "join".equals(mode) ? -1 : 0;
        view.onHelp = () -> startActivity(new Intent(this, HelpActivity.class));
        view.onAgain = this::requestAgain;
        view.onLeave = this::finish;
        if ("join".equals(mode)) view.onRetry = this::retryJoin;
        setContentView(view);
        immersive();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerBack();
        }

        if ("host".equals(mode)) startHost();
        else if ("join".equals(mode)) {
            if (hostIp == null || hostIp.trim().isEmpty()) {
                finish();
                return;
            }
            startClient(hostIp);
        } else {
            startLocal();
        }
    }

    private void registerBack() {
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::confirmLeave);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        confirmLeave();
    }

    private boolean inMatch() {
        if (view != null && view.notice != null && !view.notice.isEmpty()) return false;
        if (world == null) return false;
        return world.phase == Phase.WAIT || world.phase == Phase.COUNT || world.phase == Phase.PLAY;
    }

    private void confirmLeave() {
        if (isFinishing()) return;
        if (!inMatch()) {
            finish();
            return;
        }
        if (leaveAsk) return;
        leaveAsk = true;
        new AlertDialog.Builder(this)
                .setMessage("要离开这局吗？")
                .setNegativeButton("留下", (dialog, which) -> leaveAsk = false)
                .setPositiveButton("离开", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> leaveAsk = false)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        immersive();
    }

    @Override
    protected void onPause() {
        paused = true;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        running = false;
        if (loop != null) loop.interrupt();
        if (host != null) host.close();
        if (client != null) client.close();
        super.onDestroy();
    }

    private void startHost() {
        String forwarded = Tunnel.roomAddress();
        if (forwarded != null) {
            view.roomQr = RoomCode.bitmap(forwarded, 512);
            if (view.roomQr != null) {
                view.roomLabel = forwarded;
                world.banner = "让" + guestName() + "扫这个码加入";
            } else {
                world.banner = "二维码生成失败";
            }
        } else {
            String ip = Net.lanIpv4();
            if (ip.indexOf('.') > 0) {
                view.roomQr = RoomCode.bitmap(RoomCode.encode(ip), 512);
            }
            if (view.roomQr != null) {
                view.roomLabel = ip + ":" + Rules.PORT;
                world.banner = "让" + guestName() + "扫这个码加入";
            } else {
                world.banner = "没有局域网地址\n请连上 Wi-Fi 后再开房";
            }
        }
        hostQr = view.roomQr;
        hostLabel = view.roomLabel;
        applyMine();
        buffer.push(world.toSnap());
        running = true;
        host = new LanHost(
                (dx, dy, bomb) -> {
                    remoteInput.setDir(dx, dy);
                    if (bomb) remoteInput.tapBomb();
                },
                () -> {
                    if (!running) return;
                    onSim(world::playAgain);
                },
                () -> {
                    if (!running) return;
                    int epoch = ++roomEpoch;
                    runOnUiThread(() -> {
                        if (epoch != roomEpoch) return;
                        view.roomQr = null;
                        view.roomLabel = "";
                    });
                    view.pingMs = Long.MIN_VALUE;
                    onSim(() -> {
                        world.setLook(1, "", Look.roleSkin(1));
                        world.resetMatch();
                    });
                },
                message -> {
                    if (!running) return;
                    int epoch = roomEpoch;
                    onSim(() -> world.waitAgain(guestName() + "断开了，可重试或再扫码"));
                    runOnUiThread(() -> {
                        if (isFinishing() || epoch != roomEpoch) return;
                        view.pingMs = Long.MIN_VALUE;
                        view.roomQr = hostQr;
                        view.roomLabel = hostLabel;
                    });
                },
                message -> runOnUiThread(() -> {
                    if (!isFinishing()) view.notice = message;
                }),
                rttMs -> view.pingMs = rttMs,
                (name, skin) -> {
                    if (!running) return;
                    onSim(() -> world.setLook(1, name, skin));
                }
        );
        host.start();
        startLoop();
    }

    private void startClient(String ip) {
        buffer.delayMs = 32;
        view.pingMs = Long.MIN_VALUE;
        view.canRetry = false;
        view.notice = "正在连接 " + ip + ":" + hostPort;
        client = new LanClient(
                ip,
                hostPort,
                view.input,
                snap -> {
                    view.canRetry = false;
                    view.notice = "";
                    if (snap.you == 0 || snap.you == 1) view.mySlot = snap.you;
                    buffer.push(snap);
                },
                viewPing -> view.pingMs = viewPing,
                message -> {
                    if (isFinishing()) return;
                    view.notice = message;
                    view.canRetry = true;
                },
                Profile.name(this),
                Profile.savedSkin(this)
        );
        client.start();
    }

    private void retryJoin() {
        if (client != null) client.close();
        startClient(hostIp);
    }

    private void requestAgain() {
        if ("join".equals(mode)) {
            if (client != null) client.sendAgain();
            return;
        }
        onSim(world::playAgain);
    }

    private void startLocal() {
        world.aiSlot = 1;
        applyMine();
        world.setLook(1, "AI", Look.roleSkin(1));
        world.beginCountdown();
        buffer.push(world.toSnap());
        running = true;
        startLoop();
    }

    private void startLoop() {
        loop = new Thread(() -> {
            long last = System.nanoTime();
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    final long now = System.nanoTime();
                    float dt = (now - last) / 1_000_000_000f;
                    if (dt < 0f) dt = 0f;
                    if (dt > 0.05f) dt = 0.05f;
                    last = now;
                    Runnable job;
                    while ((job = queue.poll()) != null) {
                        job.run();
                    }
                    if (!paused) {
                        rp.games.baozi.game.Input mine = view.input.poll();
                        rp.games.baozi.game.Input peer = remoteInput.poll();
                        world.step(dt, mine, peer);
                        rp.games.baozi.game.Snap snap = world.toSnap();
                        buffer.push(snap);
                        if (host != null) {
                            host.send(Protocol.snapLine(snap, 1).toString());
                        }
                    }
                    long usedMs = (System.nanoTime() - now) / 1_000_000;
                    long sleep = 16L - usedMs;
                    if (sleep > 0L) {
                        Thread.sleep(sleep);
                    }
                } catch (final InterruptedException e) {
                    break;
                } catch (final Exception e) {
                    Log.e("Baozi", "startLoop", e);
                    break;
                }
            }
        }, "baozi-sim");
        loop.start();
    }

    private String guestName() {
        final String custom = world.players[1].name;
        if (custom != null && !custom.isEmpty()) return custom;
        return "对方";
    }

    private void applyMine() {
        world.setLook(0, Profile.name(this), Profile.skinForSlot(this, 0));
    }

    private void onSim(Runnable block) {
        if (!running) return;
        queue.add(block);
    }

    @SuppressWarnings("deprecation")
    private void immersive() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setAttributes(attrs);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setStatusBarContrastEnforced(false);
        getWindow().setNavigationBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
        View content = findViewById(android.R.id.content);
        if (content != null) {
            content.setPadding(0, 0, 0, 0);
            content.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(0, 0, 0, 0);
                return WindowInsets.CONSUMED;
            });
        }
    }
}
