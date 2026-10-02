package rp.games.baozi.game;

public final class InputState {
    private final Object lock = new Object();
    private int dx;
    private int dy;
    private boolean bomb;

    public void setDir(int x, int y) {
        synchronized (lock) {
            dx = clamp(x);
            dy = clamp(y);
        }
    }

    public void tapBomb() {
        synchronized (lock) {
            bomb = true;
        }
    }

    public Input poll() {
        synchronized (lock) {
            Input snap = new Input(dx, dy, bomb);
            bomb = false;
            return snap;
        }
    }

    private static int clamp(int v) {
        if (v < -1) return -1;
        if (v > 1) return 1;
        return v;
    }
}
