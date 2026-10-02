package rp.games.baozi.game;

public final class Item {
    public final int x;
    public final int y;
    public final ItemKind kind;
    /** 飞出起点。fly 为 0 时已经落在格子上。 */
    public float fromX;
    public float fromY;
    public float fly;
    public float span;

    public Item(int x, int y, ItemKind kind) {
        this.x = x;
        this.y = y;
        this.kind = kind;
    }

    public void launch(float ox, float oy, float time) {
        fromX = ox;
        fromY = oy;
        fly = time;
        span = time;
    }
}
