package rp.games.baozi.game;

public final class Bomb {
    public final int x;
    public final int y;
    public final int owner;
    public final int flame;
    public float fuse;

    public Bomb(int x, int y, int owner, float fuse, int flame) {
        this.x = x;
        this.y = y;
        this.owner = owner;
        this.fuse = fuse;
        this.flame = flame;
    }
}
