package rp.games.baozi.game;

public final class Player {
    public final int id;
    public float spawnX;
    public float spawnY;
    public float x;
    public float y;
    public int score;
    public int speedLevel;
    public int bombLevel;
    public int flameLevel;
    public int heldSpeed;
    public int heldBomb;
    public int heldFlame;
    /** 被困住还没爆开时，每点一次放炮按钮用掉一把。 */
    public int forks;
    public int facing = 2;
    public float stun;
    /** 爆开后离开画面的剩余时间。大于 0 时不画出来。 */
    public float away;
    public int moveX;
    public int moveY;
    public int passX = -1;
    public int passY = -1;
    /** 空字符串时界面用角色名。 */
    public String name = "";
    public int skin;

    public Player(final int id) {
        this.id = id;
        this.skin = Look.roleSkin(id);
    }

    public int bombMax() {
        return 1 + bombLevel;
    }

    public int flame() {
        return 1 + flameLevel;
    }

    public float speed() {
        return 3.0f + speedLevel * 0.34f;
    }
}
