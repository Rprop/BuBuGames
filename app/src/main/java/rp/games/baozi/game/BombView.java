package rp.games.baozi.game;

public final class BombView {
    public final int x;
    public final int y;
    public final float fuse;
    public final int owner;
    public final int flame;

    public BombView(int x, int y, float fuse, int owner, int flame) {
        this.x = x;
        this.y = y;
        this.fuse = fuse;
        this.owner = owner;
        this.flame = flame;
    }
}
