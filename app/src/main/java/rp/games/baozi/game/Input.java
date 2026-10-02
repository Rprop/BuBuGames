package rp.games.baozi.game;

public final class Input {
    public static final Input NONE = new Input(0, 0, false);

    public final int dx;
    public final int dy;
    public final boolean place;

    public Input(int dx, int dy, boolean place) {
        this.dx = dx;
        this.dy = dy;
        this.place = place;
    }
}
