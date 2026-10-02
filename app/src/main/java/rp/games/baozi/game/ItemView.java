package rp.games.baozi.game;

public final class ItemView {
    public final int x;
    public final int y;
    public final ItemKind kind;
    public final float fromX;
    public final float fromY;
    public final float fly;
    public final float span;

    public ItemView(int x, int y, ItemKind kind) {
        this(x, y, kind, 0f, 0f, 0f, 0f);
    }

    public ItemView(int x, int y, ItemKind kind, float fromX, float fromY, float fly, float span) {
        this.x = x;
        this.y = y;
        this.kind = kind;
        this.fromX = fromX;
        this.fromY = fromY;
        this.fly = fly;
        this.span = span;
    }
}
