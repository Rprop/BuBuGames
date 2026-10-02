package rp.games.baozi.net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rp.games.baozi.game.BlastView;
import rp.games.baozi.game.BombView;
import rp.games.baozi.game.Input;
import rp.games.baozi.game.ItemKind;
import rp.games.baozi.game.ItemView;
import rp.games.baozi.game.Look;
import rp.games.baozi.game.Phase;
import rp.games.baozi.game.PlayerView;
import rp.games.baozi.game.Snap;

public final class Protocol {
    /**
     * 加入方两次测速的间隔。
     */
    public static final long PING_PERIOD_MS = 1000L;

    public static String inputLine(final int dx, final int dy, final boolean bomb) throws JSONException {
        return new JSONObject()
                .put("op", "in")
                .put("dx", dx)
                .put("dy", dy)
                .put("bomb", bomb ? 1 : 0)
                .toString();
    }

    public static String againLine() {
        return "{\"op\":\"again\"}";
    }

    public static String ackLine() {
        return "{\"op\":\"ack\"}";
    }

    /**
     * 加入方告诉房主自己的昵称和皮肤。皮肤 -1 表示沿用角色默认套。
     */
    public static String whoLine(final String name, final int skin) throws JSONException {
        int n = skin;
        if (n < 0 || n >= Look.SKINS) n = -1;
        return new JSONObject()
                .put("op", "who")
                .put("name", Look.clean(name))
                .put("skin", n)
                .toString();
    }

    public static boolean isWho(final JSONObject json) {
        return "who".equals(json.optString("op"));
    }

    public static String whoName(final JSONObject json) {
        return Look.clean(json.optString("name"));
    }

    /** 不是合法皮肤时返回 -1。 */
    public static int whoSkin(final JSONObject json) {
        return Look.clampSkin(json.optInt("skin", -1), -1);
    }

    public static boolean isAck(final JSONObject json) {
        return "ack".equals(json.optString("op"));
    }

    public static boolean isAgain(final JSONObject json) {
        try {
            return "again".equals(json.optString("op"));
        } catch (final Exception ignored) {
            return false;
        }
    }

    public static String pingLine(final long ms) throws JSONException {
        return new JSONObject().put("op", "ping").put("ms", ms).toString();
    }

    public static String pongLine(final long ms) throws JSONException {
        return new JSONObject().put("op", "pong").put("ms", ms).toString();
    }

    /**
     * 不是测速包时返回 -1。值是加入方发出时的毫秒时间。
     */
    public static long pingMs(final JSONObject json) {
        return opMs(json, "ping");
    }

    /**
     * 不是测速回包时返回 -1。
     */
    public static long pongMs(final JSONObject json) {
        return opMs(json, "pong");
    }

    private static long opMs(final JSONObject json, final String op) {
        if (!op.equals(json.optString("op"))) {
            return -1L;
        }
        return json.optLong("ms", -1L);
    }

    public static Input parseInput(final JSONObject json) {
        if (!"in".equals(json.optString("op"))) {
            return null;
        }
        return new Input(clamp(json.optInt("dx")), clamp(json.optInt("dy")), json.optInt("bomb") == 1);
    }

    public static JSONObject snapLine(final Snap snap, final int guestSlot) throws JSONException {
        final JSONObject obj = new JSONObject();
        obj.put("op", "snap");
        obj.put("left", (double) snap.left);
        obj.put("phase", phaseName(snap.phase));
        obj.put("count", (double) snap.countdown);
        obj.put("winner", snap.winner);
        obj.put("banner", snap.banner);
        obj.put("tiles", snap.tiles);
        obj.put("walls", snap.walls);
        final JSONArray players = new JSONArray();
        for (final PlayerView p : snap.players) {
            players.put(new JSONObject()
                    .put("id", p.id)
                    .put("x", (double) p.x)
                    .put("y", (double) p.y)
                    .put("score", p.score)
                    .put("stun", (double) p.stun)
                    .put("away", (double) p.away)
                    .put("speed", p.speed)
                    .put("bombMax", p.bombMax)
                    .put("bombOut", p.bombOut)
                    .put("flame", p.flame)
                    .put("facing", p.facing)
                    .put("mx", p.moveX)
                    .put("my", p.moveY)
                    .put("name", p.name)
                    .put("skin", p.skin)
                    .put("forks", p.forks));
        }
        obj.put("players", players);
        final JSONArray bombs = new JSONArray();
        for (final BombView b : snap.bombs) {
            bombs.put(new JSONObject()
                    .put("x", b.x)
                    .put("y", b.y)
                    .put("fuse", (double) b.fuse)
                    .put("owner", b.owner)
                    .put("flame", b.flame));
        }
        obj.put("bombs", bombs);
        final JSONArray blasts = new JSONArray();
        for (final BlastView b : snap.blasts) {
            blasts.put(new JSONObject()
                    .put("x", b.x)
                    .put("y", b.y)
                    .put("life", (double) b.life));
        }
        obj.put("blasts", blasts);
        final  JSONArray items = new JSONArray();
        for (final ItemView item : snap.items) {
            items.put(new JSONObject()
                    .put("x", item.x)
                    .put("y", item.y)
                    .put("kind", kindName(item.kind))
                    .put("fx", (double) item.fromX)
                    .put("fy", (double) item.fromY)
                    .put("fly", (double) item.fly)
                    .put("span", (double) item.span));
        }
        obj.put("items", items);
        obj.put("you", guestSlot);
        return obj;
    }

    public static Snap parseSnap(final JSONObject json) {
        try {
            if (!"snap".equals(json.optString("op"))) return null;
            return new Snap(
                    (float) json.optDouble("left", 0.0),
                    parsePhase(json.optString("phase")),
                    (float) json.optDouble("count", 0.0),
                    json.optInt("winner", -1),
                    json.optString("banner"),
                    json.optString("tiles"),
                    json.optString("walls"),
                    readPlayers(json.optJSONArray("players")),
                    readBombs(json.optJSONArray("bombs")),
                    readBlasts(json.optJSONArray("blasts")),
                    readItems(json.optJSONArray("items")),
                    json.optInt("you", -1)
            );
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static List<PlayerView> readPlayers(final JSONArray array) {
        if (array == null) return Collections.emptyList();
        final List<PlayerView> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            final JSONObject p = array.optJSONObject(i);
            if (p == null) continue;
            final int id = p.optInt("id");
            list.add(new PlayerView(
                    id,
                    (float) p.optDouble("x"),
                    (float) p.optDouble("y"),
                    p.optInt("score"),
                    (float) p.optDouble("stun"),
                    p.optInt("speed"),
                    p.optInt("bombMax", 1),
                    p.optInt("bombOut"),
                    p.optInt("flame", 1),
                    p.optInt("facing", 2),
                    (float) p.optDouble("away"),
                    p.optInt("mx"),
                    p.optInt("my"),
                    Look.clean(p.optString("name")),
                    Look.clampSkin(p.optInt("skin", -1), Look.roleSkin(id)),
                    p.optInt("forks")
            ));
        }
        return list;
    }

    private static List<BombView> readBombs(final JSONArray array) {
        if (array == null) return Collections.emptyList();
        final List<BombView> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            final JSONObject b = array.optJSONObject(i);
            if (b == null) continue;
            list.add(new BombView(
                    b.optInt("x"),
                    b.optInt("y"),
                    (float) b.optDouble("fuse"),
                    b.optInt("owner"),
                    b.optInt("flame", 1)
            ));
        }
        return list;
    }

    private static List<BlastView> readBlasts(final JSONArray array) {
        if (array == null) return Collections.emptyList();
        final List<BlastView> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            final JSONObject b = array.optJSONObject(i);
            if (b == null) continue;
            list.add(new BlastView(b.optInt("x"), b.optInt("y"), (float) b.optDouble("life")));
        }
        return list;
    }

    private static List<ItemView> readItems(final JSONArray array) {
        if (array == null) return Collections.emptyList();
        final List<ItemView> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            final JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            final ItemKind kind = parseKind(item.optString("kind"));
            if (kind == null) continue;
            list.add(new ItemView(
                    item.optInt("x"),
                    item.optInt("y"),
                    kind,
                    (float) item.optDouble("fx"),
                    (float) item.optDouble("fy"),
                    (float) item.optDouble("fly"),
                    (float) item.optDouble("span")
            ));
        }
        return list;
    }

    private static String phaseName(final Phase phase) {
        if (phase == Phase.COUNT) return "count";
        if (phase == Phase.PLAY) return "play";
        if (phase == Phase.OVER) return "over";
        return "wait";
    }

    private static Phase parsePhase(final String name) {
        if ("count".equals(name)) return Phase.COUNT;
        if ("play".equals(name)) return Phase.PLAY;
        if ("over".equals(name)) return Phase.OVER;
        return Phase.WAIT;
    }

    private static String kindName(final ItemKind kind) {
        if (kind == ItemKind.SPEED) return "speed";
        if (kind == ItemKind.BOMB) return "bomb";
        if (kind == ItemKind.RANGE) return "range";
        if (kind == ItemKind.FORK) return "fork";
        return "bun";
    }

    private static ItemKind parseKind(final String name) {
        if ("speed".equals(name)) return ItemKind.SPEED;
        if ("bomb".equals(name)) return ItemKind.BOMB;
        if ("range".equals(name)) return ItemKind.RANGE;
        if ("fork".equals(name)) return ItemKind.FORK;
        if ("bun".equals(name)) return ItemKind.BUN;
        return null;
    }

    private static int clamp(final int v) {
        if (v < -1) return -1;
        if (v > 1) return 1;
        return v;
    }
}
