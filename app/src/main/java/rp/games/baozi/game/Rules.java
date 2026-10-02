package rp.games.baozi.game;

public final class Rules {
    public static final int COLS = 20;
    public static final int ROWS = 13;
    public static final int PORT = 28888;
    public static final float MATCH_SECONDS = 300f;
    public static final float FUSE = 2.35f;
    public static final float BODY = 0.30f;
    /** 圆心不在火焰格时，圆心离这格要近过这个距离才炸到。 */
    public static final float BLAST_EDGE = 0.12f;
    public static final float BLAST_TIME = 0.45f;
    public static final float STUN = 5f;
    /** 爆开后先从画面消失，再回到出生点。 */
    public static final float AWAY = 3f;
    /** 被炸出的包子和道具飞到落点的时间。 */
    public static final float FLING = 0.55f;
}
