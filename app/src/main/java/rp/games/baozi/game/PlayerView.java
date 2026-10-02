package rp.games.baozi.game;

public final class PlayerView {
    public final int id;
    public final float x;
    public final float y;
    public final int score;
    public final float stun;
    public final float away;
    public final int speed;
    public final int bombMax;
    public final int bombOut;
    public final int flame;
    public final int facing;
    /** 这一帧摇杆方向。被挡住时人不动，但方向还在。 */
    public final int moveX;
    public final int moveY;
    public final String name;
    public final int skin;
    public final int forks;

    public PlayerView(int id, float x, float y, int score, float stun, int speed,
                      int bombMax, int bombOut, int flame, int facing, float away,
                      int moveX, int moveY, String name, int skin, int forks) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.score = score;
        this.stun = stun;
        this.away = away;
        this.speed = speed;
        this.bombMax = bombMax;
        this.bombOut = bombOut;
        this.flame = flame;
        this.facing = facing;
        this.moveX = moveX;
        this.moveY = moveY;
        this.name = name == null ? "" : name;
        this.skin = skin;
        this.forks = forks;
    }

    public PlayerView moved(float nx, float ny) {
        return new PlayerView(id, nx, ny, score, stun, speed, bombMax, bombOut, flame, facing, away, moveX, moveY, name, skin, forks);
    }
}
