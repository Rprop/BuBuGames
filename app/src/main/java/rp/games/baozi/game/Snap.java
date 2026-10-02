package rp.games.baozi.game;

import java.util.List;

public final class Snap {
    public final float left;
    public final Phase phase;
    public final float countdown;
    public final int winner;
    public final String banner;
    public final String tiles;
    /**
     * 和 tiles 等长。硬墙是 A 到 O，软墙是 a 到 j。其余是点。
     */
    public final String walls;
    public final List<PlayerView> players;
    public final List<BombView> bombs;
    public final List<BlastView> blasts;
    public final List<ItemView> items;
    /**
     * 加入方在画面里扮演的槽位。-1 表示这份画面不指定。
     */
    public final int you;

    public Snap(float left, Phase phase, float countdown, int winner, String banner, String tiles, String walls,
                List<PlayerView> players, List<BombView> bombs, List<BlastView> blasts, List<ItemView> items) {
        this(left, phase, countdown, winner, banner, tiles, walls, players, bombs, blasts, items, -1);
    }

    public Snap(float left, Phase phase, float countdown, int winner, String banner, String tiles, String walls,
                List<PlayerView> players, List<BombView> bombs, List<BlastView> blasts, List<ItemView> items, int you) {
        this.left = left;
        this.phase = phase;
        this.countdown = countdown;
        this.winner = winner;
        this.banner = banner == null ? "" : banner;
        this.tiles = tiles == null ? "" : tiles;
        this.walls = walls == null ? "" : walls;
        this.players = players;
        this.bombs = bombs;
        this.blasts = blasts;
        this.items = items;
        this.you = you;
    }

    public Snap copyPlayers(List<PlayerView> nextPlayers) {
        return new Snap(left, phase, countdown, winner, banner, tiles, walls, nextPlayers, bombs, blasts, items, you);
    }
}
