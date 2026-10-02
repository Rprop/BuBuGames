package rp.games.baozi.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 按收包时刻把角色位置补间。保留最近几帧，画面画的是稍微早一点的时刻，
 * 新包到来时接着走，不会跳回上一帧。延迟为 0 时直接用最新一帧。
 * 被送回出生点时距离很大，直接跳过去。
 */
public final class SnapBuffer {
    public long delayMs;

    private static final int HISTORY = 12;

    private final Object lock = new Object();
    private final ArrayDeque<Stamp> history = new ArrayDeque<>();

    public void push(Snap snap) {
        synchronized (lock) {
            history.addLast(new Stamp(snap, System.nanoTime()));
            while (history.size() > HISTORY) history.removeFirst();
        }
    }

    public Snap sample() {
        synchronized (lock) {
            if (history.isEmpty()) return null;
            Stamp latest = history.peekLast();
            if (delayMs <= 0L || history.size() == 1) return latest.snap;
            long renderAt = System.nanoTime() - delayMs * 1_000_000L;
            Stamp before = null;
            Stamp after = null;
            for (Stamp stamp : history) {
                if (stamp.at <= renderAt) {
                    before = stamp;
                } else {
                    after = stamp;
                    break;
                }
            }
            if (before == null) return history.peekFirst().snap;
            if (after == null) return before.snap;
            long spanL = after.at - before.at;
            if (spanL < 1_000_000L || spanL > 250_000_000L) return after.snap;
            if (before.snap.players.size() != after.snap.players.size()) return after.snap;
            float u = (renderAt - before.at) / (float) spanL;
            if (u < 0f) u = 0f;
            if (u > 1f) u = 1f;
            List<PlayerView> lerped = new ArrayList<>();
            for (int i = 0; i < after.snap.players.size(); i++) {
                PlayerView nowP = after.snap.players.get(i);
                PlayerView old = before.snap.players.get(i);
                float dx = nowP.x - old.x;
                float dy = nowP.y - old.y;
                if (dx * dx + dy * dy > 6.25f) {
                    lerped.add(nowP);
                } else {
                    lerped.add(nowP.moved(old.x + dx * u, old.y + dy * u));
                }
            }
            return after.snap.copyPlayers(lerped);
        }
    }

    private static final class Stamp {
        final Snap snap;
        final long at;

        Stamp(Snap snap, long at) {
            this.snap = snap;
            this.at = at;
        }
    }
}
