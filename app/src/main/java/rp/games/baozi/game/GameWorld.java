package rp.games.baozi.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 抢包子。房主权威模拟。
 * 硬墙炸不开，软块会被糖泡拆掉。被炸到会在原地停 5 秒，爆开后消失 3 秒，再回到这一局的出生点。
 */
public final class GameWorld {
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    /** 中间没有空路、曼哈顿距离又达到这么远时，先不追玩家。 */
    private static final int CHASE_FAR = 8;
    /** 场内打散的硬墙数量，和原来整齐柱子的块数一样。 */
    private static final int HARD_SCATTER = 32;
    /** 硬墙放完后，其余空格变成软墙的概率。出生点旁边除外。 */
    private static final float SOFT_CHANCE = 0.6f;
    private static final int WALL_KINDS = 15;
    private static final int SOFT_KINDS = 10;
    /** 每一局里硬墙、软墙各自先抽出这么多种，再只在这几种里贴。 */
    private static final int HARD_PALETTE = 3;
    private static final int SOFT_PALETTE = 2;
    /** 四个角。每局两人各随机占一角，这一局被炸回去还是这里。 */
    private static final int[][] CORNERS = {
            {1, 1},
            {Rules.COLS - 2, 1},
            {1, Rules.ROWS - 2},
            {Rules.COLS - 2, Rules.ROWS - 2}
    };

    public Phase phase = Phase.WAIT;
    public String banner = "";
    public int winner = -1;
    public float left = Rules.MATCH_SECONDS;
    /** 这一次开房或单机选定的时长。再来一局继续用它。 */
    public float matchSeconds = Rules.MATCH_SECONDS;
    public float countdown = 3f;
    public boolean aiForSecond;
    /** 单机时由电脑控制的槽位。-1 表示没有电脑。 */
    public int aiSlot = -1;

    private final Random rng = new Random(System.nanoTime());
    private final Tile[][] map = new Tile[Rules.ROWS][Rules.COLS];
    /** 墙体贴图编号。硬墙 0 到 14，软墙 0 到 9，空地为 -1。 */
    private int[][] wallSkin;
    private final ArrayList<Bomb> bombs = new ArrayList<>();
    private final ArrayList<Blast> blasts = new ArrayList<>();
    private final ArrayList<Item> items = new ArrayList<>();
    private final HashSet<Cell> safe = new HashSet<>();
    private float bunTimer = 6f;
    private int aiDx;
    private int aiDy;
    private int fleeX = -1;
    private int fleeY = -1;
    /** 原地左右或上下对转时，先站定这么久再重新选方向。 */
    private float aiPause;
    private float spinX;
    private float spinY;
    private float spinWindow;
    private int spinAxis;
    private int spinSign;
    private int spinFlips;
    private boolean spinAnchor;
    /** 自己的糖泡封住、而且里面躲不开爆炸的死胡同。 */
    private boolean[][] pocket;
    /** 比外面更短、只有这颗糖泡一个出口的小死胡同。 */
    private boolean[][] alley;

    public final Player[] players = new Player[2];

    public GameWorld() {
        players[0] = new Player(0);
        players[1] = new Player(1);
        rollSpawns();
        generate();
    }

    public void beginCountdown() {
        if (phase != Phase.WAIT) return;
        banner = "";
        countdown = 3f;
        left = matchSeconds;
        phase = Phase.COUNT;
    }

    public void abort(String message) {
        if (phase == Phase.OVER) return;
        banner = message;
        winner = -1;
        phase = Phase.OVER;
    }

    /** 正常结束后再开一局。连接不断，地图重新生成。 */
    public void playAgain() {
        if (phase != Phase.OVER || winner < 0) return;
        resetMatch();
    }

    /** 对手重新连上，或房主开始下一局。 */
    public void resetMatch() {
        bombs.clear();
        blasts.clear();
        items.clear();
        banner = "";
        winner = -1;
        left = matchSeconds;
        countdown = 3f;
        bunTimer = 6f;
        aiDx = 0;
        aiDy = 0;
        fleeX = -1;
        fleeY = -1;
        aiPause = 0f;
        spinWindow = 0f;
        spinAxis = 0;
        spinSign = 0;
        spinFlips = 0;
        spinAnchor = false;
        rollSpawns();
        for (Player player : players) {
            player.score = 0;
            player.speedLevel = 0;
            player.bombLevel = 0;
            player.flameLevel = 0;
            player.heldSpeed = 0;
            player.heldBomb = 0;
            player.heldFlame = 0;
            player.forks = 0;
            player.stun = 0f;
            player.away = 0f;
            player.moveX = 0;
            player.moveY = 0;
            player.passX = -1;
            player.passY = -1;
        }
        generate();
        phase = Phase.COUNT;
    }

    /** 只改昵称和皮肤，不碰这一局里的位置和分数。皮肤超出范围时保持原样。 */
    public void setLook(int id, String name, int skin) {
        if (id != 0 && id != 1) return;
        players[id].name = Look.clean(name);
        if (skin >= 0 && skin < Look.SKINS) players[id].skin = skin;
    }

    /** 对手断开后回到等加入，二维码可以继续用。 */
    public void waitAgain(String message) {
        banner = message == null ? "" : message;
        winner = -1;
        phase = Phase.WAIT;
    }

    public void step(float dt, Input input0, Input input1) {
        float d = clamp(dt, 0f, 0.05f);
        if (phase == Phase.COUNT) {
            countdown -= d;
            if (countdown <= 0f) {
                countdown = 0f;
                phase = Phase.PLAY;
            }
        } else if (phase == Phase.PLAY) {
            stepPlay(d, input0, input1);
        }
    }

    public Snap toSnap() {
        StringBuilder tiles = new StringBuilder(Rules.COLS * Rules.ROWS);
        StringBuilder walls = new StringBuilder(Rules.COLS * Rules.ROWS);
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                if (map[y][x] == Tile.HARD) tiles.append('#');
                else if (map[y][x] == Tile.SOFT) tiles.append('o');
                else tiles.append('.');
                int skin = wallSkin == null ? -1 : wallSkin[y][x];
                if (map[y][x] == Tile.HARD && skin >= 0 && skin < WALL_KINDS) {
                    walls.append((char) ('A' + skin));
                } else if (map[y][x] == Tile.SOFT && skin >= 0 && skin < SOFT_KINDS) {
                    walls.append((char) ('a' + skin));
                } else {
                    walls.append('.');
                }
            }
        }
        List<PlayerView> playerViews = new ArrayList<>();
        for (Player p : players) {
            playerViews.add(new PlayerView(
                    p.id, p.x, p.y, p.score, p.stun, p.speedLevel,
                    p.bombMax(), countBombs(p.id), p.flame(), p.facing, p.away, p.moveX, p.moveY,
                    p.name, p.skin, p.forks
            ));
        }
        List<BombView> bombViews = new ArrayList<>();
        for (Bomb bomb : bombs) {
            bombViews.add(new BombView(bomb.x, bomb.y, bomb.fuse, bomb.owner, bomb.flame));
        }
        List<BlastView> blastViews = new ArrayList<>();
        for (Blast blast : blasts) {
            blastViews.add(new BlastView(blast.x, blast.y, blast.life));
        }
        List<ItemView> itemViews = new ArrayList<>();
        for (Item item : items) {
            itemViews.add(new ItemView(item.x, item.y, item.kind, item.fromX, item.fromY, item.fly, item.span));
        }
        return new Snap(left, phase, countdown, winner, banner, tiles.toString(), walls.toString(),
                playerViews, bombViews, blastViews, itemViews);
    }

    /** 两人各抽一个角，清出落点和朝地图内侧的两格。 */
    private void rollSpawns() {
        safe.clear();
        final int first = rng.nextInt(CORNERS.length);
        int second = rng.nextInt(CORNERS.length - 1);
        if (second >= first) second++;
        assign(players[0], CORNERS[first]);
        assign(players[1], CORNERS[second]);
        stand(players[0]);
        stand(players[1]);
    }

    private void assign(final Player player, final int[] corner) {
        final int cellX = corner[0];
        final int cellY = corner[1];
        player.spawnX = cellX + 0.5f;
        player.spawnY = cellY + 0.5f;
        safe.add(new Cell(cellX, cellY));
        safe.add(new Cell(cellX == 1 ? cellX + 1 : cellX - 1, cellY));
        safe.add(new Cell(cellX, cellY == 1 ? cellY + 1 : cellY - 1));
    }

    private void stand(final Player player) {
        player.x = player.spawnX;
        player.y = player.spawnY;
        player.facing = 2;
        player.moveX = 0;
        player.moveY = 0;
        player.passX = -1;
        player.passY = -1;
    }

    private void generate() {
        wallSkin = new int[Rules.ROWS][Rules.COLS];
        int[] hardSkins = pickSkins(WALL_KINDS, HARD_PALETTE);
        int[] softSkins = pickSkins(SOFT_KINDS, SOFT_PALETTE);
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                map[y][x] = Tile.EMPTY;
                wallSkin[y][x] = -1;
            }
        }
        ArrayList<int[]> spots = new ArrayList<>();
        for (int y = 1; y < Rules.ROWS - 1; y++) {
            for (int x = 1; x < Rules.COLS - 1; x++) {
                if (safe.contains(new Cell(x, y))) continue;
                spots.add(new int[]{x, y});
            }
        }
        Collections.shuffle(spots, rng);
        int placed = 0;
        for (int[] spot : spots) {
            if (placed >= HARD_SCATTER) break;
            int x = spot[0];
            int y = spot[1];
            map[y][x] = Tile.HARD;
            if (!interiorOpen()) {
                map[y][x] = Tile.EMPTY;
                continue;
            }
            wallSkin[y][x] = hardSkins[rng.nextInt(hardSkins.length)];
            placed++;
        }
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                if (map[y][x] != Tile.EMPTY) continue;
                if (safe.contains(new Cell(x, y))) continue;
                if (rng.nextFloat() < SOFT_CHANCE) {
                    map[y][x] = Tile.SOFT;
                    wallSkin[y][x] = softSkins[rng.nextInt(softSkins.length)];
                }
            }
        }
    }

    /** 从全部贴图里不重复抽出这一局要用的几种。 */
    private int[] pickSkins(int kinds, int count) {
        ArrayList<Integer> bag = new ArrayList<>();
        for (int i = 0; i < kinds; i++) bag.add(i);
        Collections.shuffle(bag, rng);
        int n = Math.min(count, bag.size());
        int[] picked = new int[n];
        for (int i = 0; i < n; i++) picked[i] = bag.get(i);
        return picked;
    }

    /** 不是硬墙的格子还能互相走到。硬墙炸不开，不能把一块空地封死。 */
    private boolean interiorOpen() {
        int total = 0;
        int sx = -1;
        int sy = -1;
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                if (map[y][x] == Tile.HARD) continue;
                total++;
                sx = x;
                sy = y;
            }
        }
        if (total == 0 || sx < 0) return false;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        boolean[][] seen = new boolean[Rules.ROWS][Rules.COLS];
        queue.add(new int[]{sx, sy});
        seen[sy][sx] = true;
        int reached = 0;
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            reached++;
            for (int[] dir : DIRS) {
                int nx = cur[0] + dir[0];
                int ny = cur[1] + dir[1];
                if (!inBounds(nx, ny) || seen[ny][nx] || map[ny][nx] == Tile.HARD) continue;
                seen[ny][nx] = true;
                queue.add(new int[]{nx, ny});
            }
        }
        return reached == total;
    }

    private void stepPlay(float dt, Input input0, Input input1) {
        decayBlasts(dt);
        for (Item item : items) {
            if (item.fly > 0f) item.fly = Math.max(0f, item.fly - dt);
        }
        for (Player p : players) {
            if (p.stun > 0f) {
                p.stun = Math.max(0f, p.stun - dt);
                if (p.stun == 0f) burstOut(p);
            } else if (p.away > 0f) {
                p.away = Math.max(0f, p.away - dt);
                if (p.away == 0f) arrive(p);
            }
        }
        updateBombs(dt);
        Input first = aiSlot == 0 ? aiInput(players[0], dt) : input0;
        Input second = aiSlot == 1 || aiForSecond ? aiInput(players[1], dt) : input1;
        act(players[0], first, dt);
        act(players[1], second, dt);
        tripTrapped();
        pry(first, second);
        pickups();
        maybeSpawnBun(dt);
        left -= dt;
        if (left <= 0f) finishMatch();
    }

    /** 别人走进被困玩家所在的格子时，立刻爆开，不必等满 5 秒。 */
    private void tripTrapped() {
        for (Player trapped : players) {
            if (trapped.stun <= 0f) continue;
            int cx = floor(trapped.x);
            int cy = floor(trapped.y);
            for (Player other : players) {
                if (other == trapped || other.stun > 0f || other.away > 0f) continue;
                if (floor(other.x) != cx || floor(other.y) != cy) continue;
                trapped.stun = 0f;
                burstOut(trapped);
                break;
            }
        }
    }

    /** 还没被走进爆开时，点一次放炮按钮用掉一把叉子，留在原地。 */
    private void pry(Input input0, Input input1) {
        pryOne(players[0], input0);
        pryOne(players[1], input1);
    }

    private void pryOne(Player p, Input input) {
        if (p.stun <= 0f || !input.place || p.forks <= 0) return;
        p.forks -= 1;
        p.stun = 0f;
    }

    private void finishMatch() {
        left = 0f;
        phase = Phase.OVER;
        if (players[0].score > players[1].score) winner = 0;
        else if (players[1].score > players[0].score) winner = 1;
        else winner = 2;
    }

    private void act(Player p, Input input, float dt) {
        if (p.stun > 0f || p.away > 0f) {
            p.moveX = 0;
            p.moveY = 0;
            return;
        }
        int dx = clampInt(input.dx, -1, 1);
        int dy = clampInt(input.dy, -1, 1);
        if (dx != 0 && dy != 0) {
            if (Math.abs(dx) >= Math.abs(dy)) dy = 0;
            else dx = 0;
        }
        p.moveX = dx;
        p.moveY = dy;
        if (input.place) tryPlace(p);
        if (dx != 0 || dy != 0) {
            float dist = p.speed() * dt;
            slide(p, dx * dist, dy * dist);
            if (dx > 0) p.facing = 1;
            else if (dx < 0) p.facing = 3;
            else if (dy < 0) p.facing = 0;
            else p.facing = 2;
        }
        p.x = clamp(p.x, 0.2f, Rules.COLS - 0.2f);
        p.y = clamp(p.y, 0.2f, Rules.ROWS - 0.2f);
        if (p.passX >= 0 && !circleHitsTile(p.x, p.y, Rules.BODY, p.passX, p.passY)) {
            p.passX = -1;
            p.passY = -1;
        }
    }

    private void tryPlace(Player p) {
        int tx = floor(p.x);
        int ty = floor(p.y);
        if (!canPlace(p, tx, ty)) return;
        float dx = p.x - (tx + 0.5f);
        float dy = p.y - (ty + 0.5f);
        if (dx * dx + dy * dy > 0.20f) return;
        bombs.add(new Bomb(tx, ty, p.id, Rules.FUSE, p.flame()));
        p.passX = tx;
        p.passY = ty;
    }

    private boolean canPlace(Player p, int tx, int ty) {
        if (!inBounds(tx, ty)) return false;
        if (map[ty][tx] != Tile.EMPTY) return false;
        if (hasBomb(tx, ty)) return false;
        return countBombs(p.id) < p.bombMax();
    }

    private void slide(Player p, float mx, float my) {
        if (mx != 0f) moveAxis(p, mx, 0f);
        if (my != 0f) moveAxis(p, 0f, my);
    }

    /**
     * 正前方走不通时，把人拢进能走进去的那一列。
     * 前方那格是空的，就对齐它的中线。前方是墙、但旁边一列是空格，而且人已经靠向那一侧，就先横着挪进去。
     */
    private void moveAxis(Player p, float mx, float my) {
        if (!blocked(p.x + mx, p.y + my, p)) {
            p.x += mx;
            p.y += my;
            return;
        }
        int tx = floor(p.x);
        int ty = floor(p.y);
        int fx = tx + (mx > 0f ? 1 : (mx < 0f ? -1 : 0));
        int fy = ty + (my > 0f ? 1 : (my < 0f ? -1 : 0));
        boolean vertical = my != 0f;
        if (laneOpen(p, fx, fy)) {
            nudgeInto(p, mx, my, vertical ? fx : fy, vertical, 0.55f);
            return;
        }
        int side = sideLane(p, fx, fy, mx, my, vertical);
        if (side >= 0) nudgeInto(p, mx, my, side, vertical, 1.05f);
    }

    /** 正前方是墙时，人已经进入靠空格那一侧的外半段，且这一列能继续往前走，就返回那一列。 */
    private int sideLane(Player p, int fx, int fy, float mx, float my, boolean vertical) {
        int stepX = mx > 0f ? 1 : (mx < 0f ? -1 : 0);
        int stepY = my > 0f ? 1 : (my < 0f ? -1 : 0);
        int found = -1;
        float foundDist = 0f;
        for (int dir = -1; dir <= 1; dir += 2) {
            int gx = vertical ? fx + dir : fx;
            int gy = vertical ? fy : fy + dir;
            if (!laneOpen(p, gx, gy) || !laneOpen(p, gx + stepX, gy + stepY)) continue;
            float pos = vertical ? p.x : p.y;
            float cell = vertical ? fx : fy;
            float toward = dir > 0 ? pos - cell : (cell + 1f) - pos;
            if (toward < 0.75f) continue;
            float dist = Math.abs(pos - ((vertical ? gx : gy) + 0.5f));
            if (found >= 0 && dist >= foundDist) continue;
            found = vertical ? gx : gy;
            foundDist = dist;
        }
        return found;
    }

    private boolean laneOpen(Player p, int x, int y) {
        return inBounds(x, y) && map[y][x] == Tile.EMPTY && !bombBlocks(p, x, y);
    }

    private void nudgeInto(Player p, float mx, float my, int lane, boolean vertical, float maxOffset) {
        float center = lane + 0.5f;
        float pos = vertical ? p.x : p.y;
        float offset = pos - center;
        float legal = 0.5f - Rules.BODY - 0.02f;
        if (Math.abs(offset) > maxOffset) return;
        float target = center + clamp(offset, -legal, legal);
        float delta = target - pos;
        float step = Math.max(Math.abs(mx) + Math.abs(my), 0.05f);
        if (Math.abs(delta) > step) delta = Math.copySign(step, delta);
        float ax = p.x + (vertical ? delta : 0f);
        float ay = p.y + (vertical ? 0f : delta);
        if (blocked(ax, ay, p)) return;
        if (!blocked(ax + mx, ay + my, p)) {
            p.x = ax + mx;
            p.y = ay + my;
        } else {
            p.x = ax;
            p.y = ay;
        }
    }

    private boolean bombBlocks(Player p, int tx, int ty) {
        for (Bomb bomb : bombs) {
            if (bomb.x != tx || bomb.y != ty) continue;
            if (bomb.x == p.passX && bomb.y == p.passY) return false;
            if (circleHitsTile(p.x, p.y, Rules.BODY, bomb.x, bomb.y)) return false;
            return true;
        }
        return false;
    }

    private boolean blocked(float x, float y, Player p) {
        float r = Rules.BODY;
        int minX = floor(x - r);
        int maxX = floor(x + r);
        int minY = floor(y - r);
        int maxY = floor(y + r);
        for (int ty = minY; ty <= maxY; ty++) {
            for (int tx = minX; tx <= maxX; tx++) {
                boolean solid = !inBounds(tx, ty) || map[ty][tx] != Tile.EMPTY;
                if (solid && circleHitsTile(x, y, r, tx, ty)) return true;
            }
        }
        for (Bomb bomb : bombs) {
            if (bomb.x == p.passX && bomb.y == p.passY) continue;
            if (circleHitsTile(p.x, p.y, r, bomb.x, bomb.y)) continue;
            if (circleHitsTile(x, y, r, bomb.x, bomb.y)) return true;
        }
        return false;
    }

    private boolean circleHitsTile(float cx, float cy, float r, int tx, int ty) {
        float nearestX = clamp(cx, tx, tx + 1f);
        float nearestY = clamp(cy, ty, ty + 1f);
        float dx = cx - nearestX;
        float dy = cy - nearestY;
        return dx * dx + dy * dy < r * r;
    }

    private void decayBlasts(float dt) {
        Iterator<Blast> it = blasts.iterator();
        while (it.hasNext()) {
            Blast blast = it.next();
            blast.life -= dt;
            if (blast.life <= 0f) it.remove();
        }
    }

    private void updateBombs(float dt) {
        ArrayList<Cell> drops = new ArrayList<>();
        HashSet<Cell> hits = new HashSet<>();
        ArrayList<Bomb> wave = new ArrayList<>();
        Iterator<Bomb> it = bombs.iterator();
        while (it.hasNext()) {
            Bomb bomb = it.next();
            bomb.fuse -= dt;
            if (bomb.fuse <= 0f || hasBlast(bomb.x, bomb.y)) {
                wave.add(bomb);
                it.remove();
            }
        }
        int guard = 0;
        while (!wave.isEmpty() && guard++ < 64) {
            detonate(wave.remove(wave.size() - 1), drops, hits, wave);
        }
        for (Cell drop : drops) spawnDrop(drop.x, drop.y);
        hurt(hits);
    }

    private void detonate(Bomb bomb, List<Cell> drops, HashSet<Cell> hits, ArrayList<Bomb> wave) {
        ArrayList<Cell> cells = new ArrayList<>();
        cells.add(new Cell(bomb.x, bomb.y));
        igniteAt(bomb.x, bomb.y, wave);
        for (int[] dir : DIRS) {
            for (int step = 1; step <= bomb.flame; step++) {
                int x = bomb.x + dir[0] * step;
                int y = bomb.y + dir[1] * step;
                if (!inBounds(x, y)) break;
                Tile tile = map[y][x];
                if (tile == Tile.HARD) break;
                cells.add(new Cell(x, y));
                igniteAt(x, y, wave);
                if (tile == Tile.SOFT) {
                    map[y][x] = Tile.EMPTY;
                    drops.add(new Cell(x, y));
                    break;
                }
            }
        }
        Iterator<Item> itemIt = items.iterator();
        while (itemIt.hasNext()) {
            Item item = itemIt.next();
            for (Cell cell : cells) {
                if (cell.x == item.x && cell.y == item.y) {
                    itemIt.remove();
                    break;
                }
            }
        }
        for (Cell cell : cells) {
            hits.add(cell);
            if (!hasBlast(cell.x, cell.y)) {
                blasts.add(new Blast(cell.x, cell.y, Rules.BLAST_TIME));
            }
        }
    }

    /** 这一格上还没炸的糖泡立刻进入同一次爆炸。 */
    private void igniteAt(int x, int y, ArrayList<Bomb> wave) {
        Iterator<Bomb> it = bombs.iterator();
        while (it.hasNext()) {
            Bomb other = it.next();
            if (other.x != x || other.y != y) continue;
            it.remove();
            wave.add(other);
        }
    }

    private void spawnDrop(int x, int y) {
        if (items.size() >= 16) return;
        if (itemAt(x, y) != null) return;
        float roll = rng.nextFloat();
        ItemKind kind;
        if (roll < 0.40f) kind = ItemKind.BUN;
        else if (roll < 0.56f) kind = ItemKind.SPEED;
        else if (roll < 0.70f) kind = ItemKind.BOMB;
        else if (roll < 0.84f) kind = ItemKind.RANGE;
        else if (roll < 0.88f) kind = ItemKind.FORK;
        else return;
        items.add(new Item(x, y, kind));
    }

    private void hurt(HashSet<Cell> cells) {
        if (cells.isEmpty()) return;
        for (Player p : players) {
            if (p.stun > 0f || p.away > 0f) continue;
            boolean hit = false;
            for (Cell cell : cells) {
                if (flameTouches(p, cell)) {
                    hit = true;
                    break;
                }
            }
            if (hit) trap(p);
        }
    }

    /** 圆心在火焰格里就炸到。圆心在外面时，要靠到格子边缘 0.12 以内。 */
    private boolean flameTouches(Player p, Cell cell) {
        if (floor(p.x) == cell.x && floor(p.y) == cell.y) return true;
        return circleHitsTile(p.x, p.y, Rules.BLAST_EDGE, cell.x, cell.y);
    }

    private void trap(Player p) {
        p.stun = Rules.STUN;
        p.moveX = 0;
        p.moveY = 0;
    }

    /** 停满 5 秒后，掉一半包子和身上全部道具到随机空格，人先消失。 */
    private void burstOut(Player p) {
        ArrayList<ItemKind> burst = new ArrayList<>();
        int buns = p.score / 2;
        p.score -= buns;
        addHeld(burst, ItemKind.BUN, buns);
        addHeld(burst, ItemKind.SPEED, p.speedLevel + p.heldSpeed);
        addHeld(burst, ItemKind.BOMB, p.bombLevel + p.heldBomb);
        addHeld(burst, ItemKind.RANGE, p.flameLevel + p.heldFlame);
        addHeld(burst, ItemKind.FORK, p.forks);
        p.speedLevel = 0;
        p.bombLevel = 0;
        p.flameLevel = 0;
        p.heldSpeed = 0;
        p.heldBomb = 0;
        p.heldFlame = 0;
        p.forks = 0;
        scatter(burst, p.x, p.y);
        p.away = Rules.AWAY;
        p.moveX = 0;
        p.moveY = 0;
        p.passX = -1;
        p.passY = -1;
    }

    private void arrive(Player p) {
        stand(p);
    }

    private void pickups() {
        for (Player p : players) {
            if (p.stun > 0f || p.away > 0f) continue;
            int tx = floor(p.x);
            int ty = floor(p.y);
            Item item = itemAt(tx, ty);
            if (item == null) continue;
            if (item.fly > 0f) continue;
            float dx = p.x - (tx + 0.5f);
            float dy = p.y - (ty + 0.5f);
            if (dx * dx + dy * dy > 0.16f) continue;
            if (!grant(p, item.kind)) continue;
            items.remove(item);
        }
    }

    private boolean grant(Player p, ItemKind kind) {
        if (kind == ItemKind.BUN) {
            p.score += 1;
            return true;
        }
        if (kind == ItemKind.SPEED) {
            if (p.speedLevel < 5) p.speedLevel += 1;
            else p.heldSpeed += 1;
            return true;
        }
        if (kind == ItemKind.BOMB) {
            if (p.bombLevel < 5) p.bombLevel += 1;
            else p.heldBomb += 1;
            return true;
        }
        if (kind == ItemKind.FORK) {
            p.forks += 1;
            return true;
        }
        if (kind != ItemKind.RANGE) return false;
        if (p.flameLevel < 5) p.flameLevel += 1;
        else p.heldFlame += 1;
        return true;
    }

    private static void addHeld(List<ItemKind> burst, ItemKind kind, int count) {
        for (int i = 0; i < count; i++) burst.add(kind);
    }

    /** 把要爆出的东西从人物身上飞到随机空格。格子不够时，多出来的直接丢掉。 */
    private void scatter(List<ItemKind> kinds, float ox, float oy) {
        if (kinds.isEmpty()) return;
        ArrayList<Cell> spots = new ArrayList<>();
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                if (map[y][x] != Tile.EMPTY) continue;
                if (itemAt(x, y) != null) continue;
                if (hasBomb(x, y)) continue;
                spots.add(new Cell(x, y));
            }
        }
        Collections.shuffle(kinds, rng);
        Collections.shuffle(spots, rng);
        int n = Math.min(kinds.size(), spots.size());
        for (int i = 0; i < n; i++) {
            Cell spot = spots.get(i);
            Item item = new Item(spot.x, spot.y, kinds.get(i));
            item.launch(ox, oy, Rules.FLING);
            items.add(item);
        }
    }

    private void maybeSpawnBun(float dt) {
        bunTimer -= dt;
        if (bunTimer > 0f) return;
        bunTimer = 7.5f;
        if (countKind(ItemKind.BUN) >= 5) return;
        spawnBunRandom();
    }

    private void spawnBunRandom() {
        ArrayList<Cell> spots = new ArrayList<>();
        for (int y = 1; y < Rules.ROWS - 1; y++) {
            for (int x = 1; x < Rules.COLS - 1; x++) {
                if (safe.contains(new Cell(x, y))) continue;
                if (map[y][x] != Tile.EMPTY) continue;
                if (itemAt(x, y) != null) continue;
                if (hasBomb(x, y)) continue;
                if (hasBlast(x, y)) continue;
                boolean occupied = false;
                for (Player p : players) {
                    if (floor(p.x) == x && floor(p.y) == y) {
                        occupied = true;
                        break;
                    }
                }
                if (occupied) continue;
                spots.add(new Cell(x, y));
            }
        }
        if (spots.isEmpty()) return;
        Cell spot = spots.get(rng.nextInt(spots.size()));
        items.add(new Item(spot.x, spot.y, ItemKind.BUN));
    }

    private Input aiInput(Player p, float dt) {
        if (p.away > 0f) {
            aiDx = 0;
            aiDy = 0;
            fleeX = -1;
            fleeY = -1;
            return Input.NONE;
        }
        if (p.stun > 0f) {
            aiDx = 0;
            aiDy = 0;
            fleeX = -1;
            fleeY = -1;
            clearSpin();
            return p.forks > 0 ? new Input(0, 0, true) : Input.NONE;
        }
        refreshPockets();
        int tx = floor(p.x);
        int ty = floor(p.y);
        if (aiPause > 0f && !threatened(tx, ty)) {
            aiPause = Math.max(0f, aiPause - dt);
            aiDx = 0;
            aiDy = 0;
            return Input.NONE;
        }
        aiPause = 0f;
        if (fleeX >= 0 && (threatened(fleeX, fleeY) || inPocket(fleeX, fleeY))) {
            fleeX = -1;
            fleeY = -1;
        }
        if (fleeX >= 0 && tx == fleeX && ty == fleeY && nearCenter(p) && !threatened(tx, ty)) {
            fleeX = -1;
            fleeY = -1;
            aiDx = 0;
            aiDy = 0;
        }
        if (threatened(tx, ty)) {
            if (fleeX < 0) {
                int[] safe = nearestSafe(tx, ty, -1, -1, -1, true);
                if (safe != null) {
                    fleeX = safe[0];
                    fleeY = safe[1];
                }
            }
            if (fleeX >= 0) {
                Input step = followFlee(p, tx, ty);
                if (step != null) return watch(p, dt, step);
                fleeX = -1;
                fleeY = -1;
            }
            return watch(p, dt, commit(p, safestNeighbor(tx, ty)));
        }
        Input fight = fight(p, tx, ty);
        if (fight != null) return watch(p, dt, fight);
        if (nearCenter(p) && itemAt(tx, ty) != null) {
            aiDx = 0;
            aiDy = 0;
            return watch(p, dt, Input.NONE);
        }
        if (nearCenter(p) && chaseStep(p, tx, ty) == null) {
            Input placed = tryAiPlace(p, tx, ty);
            if (placed != null) return watch(p, dt, placed);
        }
        if (fleeX >= 0) {
            Input step = followFlee(p, tx, ty);
            if (step != null) return watch(p, dt, step);
            fleeX = -1;
            fleeY = -1;
        }
        if (!nearCenter(p)) {
            int nextX = tx + aiDx;
            int nextY = ty + aiDy;
            // 还贴在角落时不要沿用旧方向。0.12 的探测刚能迈出，下一步又顶回墙角，就会左右对转。
            if (linedUp(p, aiDx, aiDy) && canStep(p, aiDx, aiDy) && !threatened(nextX, nextY)) {
                return watch(p, dt, new Input(aiDx, aiDy, false));
            }
            Input fix = hold(moveToward(p, tx, ty));
            if (fix != null) return watch(p, dt, fix);
            aiDx = 0;
            aiDy = 0;
        }
        int[] dir = pathStep(p, tx, ty);
        if (dir == null && (aiDx != 0 || aiDy != 0) && walkable(tx + aiDx, ty + aiDy)
                && !threatened(tx + aiDx, ty + aiDy)) {
            dir = new int[]{aiDx, aiDy};
        }
        if (dir == null) dir = openStep(p, tx, ty);
        return watch(p, dt, commit(p, dir));
    }

    /**
     * 人几乎没挪动，方向却在左右或上下对调，就站定一会儿再重新算。
     * 只停这个电脑，糖泡和对局继续走。
     */
    private Input watch(Player p, float dt, Input input) {
        if (!spinAnchor) {
            spinX = p.x;
            spinY = p.y;
            spinAnchor = true;
        }
        int axis = input.dx != 0 ? 1 : (input.dy != 0 ? 2 : 0);
        int sign = axis == 1 ? input.dx : input.dy;
        float dist = Math.abs(p.x - spinX) + Math.abs(p.y - spinY);
        boolean flip = axis != 0 && axis == spinAxis && sign == -spinSign && dist < 0.18f;
        if (flip) {
            spinFlips += 1;
            if (spinWindow <= 0f) spinWindow = 0.45f;
        } else if (spinWindow > 0f) {
            spinWindow -= dt;
            if (spinWindow <= 0f) spinFlips = 0;
        }
        if (dist >= 0.18f) {
            spinX = p.x;
            spinY = p.y;
            spinFlips = 0;
            spinWindow = 0f;
        }
        if (axis != 0) {
            spinAxis = axis;
            spinSign = sign;
        }
        if (spinFlips < 3) return input;
        clearSpin();
        spinX = p.x;
        spinY = p.y;
        spinAnchor = true;
        aiPause = 0.4f;
        aiDx = 0;
        aiDy = 0;
        return Input.NONE;
    }

    private void clearSpin() {
        spinFlips = 0;
        spinWindow = 0f;
        spinAxis = 0;
        spinSign = 0;
        spinAnchor = false;
    }

    /** 身旁有软块、还有空余糖泡，并且放完来得及逃出，就再放一颗。 */
    private Input tryAiPlace(Player p, int tx, int ty) {
        if (!besideSoft(tx, ty) || !canPlace(p, tx, ty)) return null;
        int[] escape = escapeRoute(p, tx, ty, p.flame(), false);
        if (escape == null || !canStep(p, escape[0], escape[1])) return null;
        fleeX = escape[2];
        fleeY = escape[3];
        aiDx = escape[0];
        aiDy = escape[1];
        return new Input(0, 0, true);
    }

    /** 追被困的玩家、在能炸到的位置放泡，或者离开玩家的火力线。 */
    private Input fight(Player p, int tx, int ty) {
        Player other = foe(p);
        if (other == null) return null;
        int ox = floor(other.x);
        int oy = floor(other.y);
        if (other.stun > 0f) return huntStunned(p, tx, ty, ox, oy);
        if (wouldHitFoe(p, tx, ty) && canPlace(p, tx, ty)
                && escapeRoute(p, tx, ty, p.flame(), true) != null) {
            if (!nearCenter(p)) {
                Input fix = hold(moveToward(p, tx, ty));
                if (fix != null) return fix;
            } else {
                Input bomb = tryBombFoe(p, tx, ty);
                if (bomb != null) return bomb;
            }
        }
        if (foeCanHit(p, tx, ty)) {
            int[] step = dodgeStep(p, tx, ty);
            if (step != null) return commit(p, step);
        }
        return null;
    }

    /** 这一格的火力能打到玩家，并且放完能逃到玩家炸不到的格子。 */
    private Input tryBombFoe(Player p, int tx, int ty) {
        if (!wouldHitFoe(p, tx, ty) || !canPlace(p, tx, ty)) return null;
        int[] escape = escapeRoute(p, tx, ty, p.flame(), true);
        if (escape == null || !canStep(p, escape[0], escape[1])) return null;
        fleeX = escape[2];
        fleeY = escape[3];
        aiDx = escape[0];
        aiDy = escape[1];
        return new Input(0, 0, true);
    }

    private Player foe(Player p) {
        Player other = players[1 - p.id];
        if (other.away > 0f) return null;
        return other;
    }

    private boolean foeCanHit(Player p, int x, int y) {
        Player other = foe(p);
        if (other == null || other.stun > 0f) return false;
        if (countBombs(other.id) >= other.bombMax()) return false;
        int ox = floor(other.x);
        int oy = floor(other.y);
        if (ox == x && oy == y) return false;
        return blastHits(ox, oy, other.flame(), x, y);
    }

    private boolean wouldHitFoe(Player p, int x, int y) {
        Player other = foe(p);
        if (other == null) return false;
        return blastHits(x, y, p.flame(), floor(other.x), floor(other.y));
    }

    /** 离开玩家当前能炸到的十字。返回第一步方向。 */
    private int[] dodgeStep(Player p, int sx, int sy) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        boolean[][] seen = new boolean[Rules.ROWS][Rules.COLS];
        int[][] prevX = new int[Rules.ROWS][Rules.COLS];
        int[][] prevY = new int[Rules.ROWS][Rules.COLS];
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                prevX[y][x] = -1;
                prevY[y][x] = -1;
            }
        }
        queue.add(new int[]{sx, sy, 0});
        seen[sy][sx] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int x = cur[0];
            int y = cur[1];
            int dist = cur[2];
            if (dist > 0 && !foeCanHit(p, x, y) && !threatened(x, y)) {
                return firstDirs(prevX, prevY, x, y, sx, sy);
            }
            if (dist > Rules.COLS + Rules.ROWS) continue;
            for (int[] dir : DIRS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (!walkable(nx, ny) || seen[ny][nx] || hasBlast(nx, ny) || threatened(nx, ny)) continue;
                if (inPocket(nx, ny)) continue;
                seen[ny][nx] = true;
                prevX[ny][nx] = x;
                prevY[ny][nx] = y;
                queue.add(new int[]{nx, ny, dist + 1});
            }
        }
        return null;
    }

    /** 走向能炸到玩家的格子。玩家被困住时改成直接踩上去。返回 dx, dy, 距离。 */
    private int[] chaseStep(Player p, int tx, int ty) {
        Player other = foe(p);
        if (other == null) return null;
        int ox = floor(other.x);
        int oy = floor(other.y);
        if (farAndBlocked(tx, ty, ox, oy)) return null;
        if (other.stun > 0f) return route(tx, ty, ox, oy);
        if (wouldHitFoe(p, tx, ty)) return null;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        boolean[][] seen = new boolean[Rules.ROWS][Rules.COLS];
        int[][] prevX = new int[Rules.ROWS][Rules.COLS];
        int[][] prevY = new int[Rules.ROWS][Rules.COLS];
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                prevX[y][x] = -1;
                prevY[y][x] = -1;
            }
        }
        queue.add(new int[]{tx, ty, 0});
        seen[ty][tx] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int x = cur[0];
            int y = cur[1];
            int dist = cur[2];
            for (int[] dir : DIRS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (!walkable(nx, ny) || seen[ny][nx] || hasBlast(nx, ny) || threatened(nx, ny)) continue;
                if (inPocket(nx, ny)) continue;
                int nd = dist + 1;
                if (wouldHitFoe(p, nx, ny)) {
                    if (escapeRoute(p, nx, ny, p.flame(), true) == null) continue;
                    prevX[ny][nx] = x;
                    prevY[ny][nx] = y;
                    int[] step = firstDirs(prevX, prevY, nx, ny, tx, ty);
                    if (step == null) return null;
                    return new int[]{step[0], step[1], nd};
                }
                if (foeCanHit(p, nx, ny)) continue;
                seen[ny][nx] = true;
                prevX[ny][nx] = x;
                prevY[ny][nx] = y;
                queue.add(new int[]{nx, ny, nd});
            }
        }
        return null;
    }

    /**
     * 走向被困玩家。只迈进离他更近的格子，所以不会在原地左右对调。
     * 空路被软墙挡住就走过去放炮拆开。已经踩在他身上才停下。
     * 没有通路而且离得远时先不追，免得对着软墙在原地左右转。
     */
    private Input huntStunned(Player p, int tx, int ty, int ox, int oy) {
        if (tx == ox && ty == oy) return Input.NONE;
        if (farAndBlocked(tx, ty, ox, oy)) return null;
        int far = Rules.COLS * Rules.ROWS;
        int[][] safe = huntDist(ox, oy, false);
        int here = safe[ty][tx];
        if (here < far) {
            int[] step = downStep(tx, ty, safe, here);
            if (step != null) return go(step);
        }
        int[][] through = huntDist(ox, oy, true);
        int hereSoft = through[ty][tx];
        if (hereSoft < far) {
            int[] step = downStep(tx, ty, through, hereSoft);
            if (step != null) return go(step);
            if (softCloser(tx, ty, through, hereSoft)) {
                if (!inPlantRange(p)) {
                    Input fix = hold(moveToward(p, tx, ty));
                    if (fix != null) return fix;
                }
                Input bomb = plant(p, tx, ty);
                if (bomb != null) return bomb;
            }
        }
        Input bomb = tryBombFoe(p, tx, ty);
        if (bomb != null) return bomb;
        if (Math.abs(tx - ox) + Math.abs(ty - oy) == 1
                && (hasBlast(ox, oy) || threatened(ox, oy) || inPocket(ox, oy))) {
            aiDx = 0;
            aiDy = 0;
            return Input.NONE;
        }
        if (here < far && closerCell(tx, ty, safe, here)) {
            aiDx = 0;
            aiDy = 0;
            return Input.NONE;
        }
        return null;
    }

    /** 从被困玩家那格往外标距离。软墙只在 throughSoft 时算通路，危险格一律不穿过。 */
    private int[][] huntDist(int gx, int gy, boolean throughSoft) {
        int far = Rules.COLS * Rules.ROWS;
        int[][] dist = new int[Rules.ROWS][Rules.COLS];
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) dist[y][x] = far;
        }
        if (!inBounds(gx, gy)) return dist;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        dist[gy][gx] = 0;
        queue.add(new int[]{gx, gy});
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int x = cur[0];
            int y = cur[1];
            int next = dist[y][x] + 1;
            for (int[] dir : DIRS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (!inBounds(nx, ny) || dist[ny][nx] <= next) continue;
                if (!huntOpen(nx, ny, throughSoft)) continue;
                dist[ny][nx] = next;
                queue.add(new int[]{nx, ny});
            }
        }
        return dist;
    }

    private boolean huntOpen(int x, int y, boolean throughSoft) {
        Tile tile = map[y][x];
        if (tile == Tile.HARD) return false;
        if (tile == Tile.SOFT) return throughSoft;
        if (tile != Tile.EMPTY) return false;
        return !hasBomb(x, y) && !hasBlast(x, y) && !threatened(x, y) && !inPocket(x, y);
    }

    /** 邻居里离目标更近的一格。距离相同就保持刚才的方向。 */
    private int[] downStep(int tx, int ty, int[][] dist, int here) {
        int best = here;
        for (int[] dir : DIRS) {
            int nx = tx + dir[0];
            int ny = ty + dir[1];
            if (!stepCell(nx, ny)) continue;
            if (dist[ny][nx] < best) best = dist[ny][nx];
        }
        if (best >= here) return null;
        if ((aiDx != 0 || aiDy != 0) && stepCell(tx + aiDx, ty + aiDy)
                && dist[ty + aiDy][tx + aiDx] == best) {
            return new int[]{aiDx, aiDy};
        }
        for (int[] dir : DIRS) {
            int nx = tx + dir[0];
            int ny = ty + dir[1];
            if (stepCell(nx, ny) && dist[ny][nx] == best) return new int[]{dir[0], dir[1]};
        }
        return null;
    }

    private boolean stepCell(int x, int y) {
        return walkable(x, y) && !hasBlast(x, y) && !threatened(x, y) && !inPocket(x, y);
    }

    private boolean softCloser(int tx, int ty, int[][] dist, int here) {
        for (int[] dir : DIRS) {
            int nx = tx + dir[0];
            int ny = ty + dir[1];
            if (!inBounds(nx, ny) || map[ny][nx] != Tile.SOFT) continue;
            if (dist[ny][nx] < here) return true;
        }
        return false;
    }

    private boolean closerCell(int tx, int ty, int[][] dist, int here) {
        for (int[] dir : DIRS) {
            int nx = tx + dir[0];
            int ny = ty + dir[1];
            if (inBounds(nx, ny) && dist[ny][nx] < here) return true;
        }
        return false;
    }

    private boolean inPlantRange(Player p) {
        int tx = floor(p.x);
        int ty = floor(p.y);
        float dx = p.x - (tx + 0.5f);
        float dy = p.y - (ty + 0.5f);
        return dx * dx + dy * dy <= 0.20f;
    }

    /** 放一颗糖泡并朝逃生格走。不要求身旁已经有软墙。 */
    private Input plant(Player p, int tx, int ty) {
        if (!canPlace(p, tx, ty)) return null;
        int[] escape = escapeRoute(p, tx, ty, p.flame(), false);
        if (escape == null) return null;
        fleeX = escape[2];
        fleeY = escape[3];
        aiDx = escape[0];
        aiDy = escape[1];
        return new Input(0, 0, true);
    }

    private Input go(int[] step) {
        aiDx = step[0];
        aiDy = step[1];
        return new Input(aiDx, aiDy, false);
    }

    /** 直线隔得远，而且空格走不到对方。 */
    private boolean farAndBlocked(int tx, int ty, int ox, int oy) {
        return Math.abs(tx - ox) + Math.abs(ty - oy) >= CHASE_FAR && !walkConnected(tx, ty, ox, oy);
    }

    private boolean walkConnected(int sx, int sy, int gx, int gy) {
        if (!inBounds(sx, sy) || !inBounds(gx, gy)) return false;
        if (sx == gx && sy == gy) return true;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        boolean[][] seen = new boolean[Rules.ROWS][Rules.COLS];
        queue.add(new int[]{sx, sy});
        seen[sy][sx] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            for (int[] dir : DIRS) {
                int nx = cur[0] + dir[0];
                int ny = cur[1] + dir[1];
                if (!inBounds(nx, ny) || seen[ny][nx] || !walkable(nx, ny)) continue;
                if (nx == gx && ny == gy) return true;
                seen[ny][nx] = true;
                queue.add(new int[]{nx, ny});
            }
        }
        return false;
    }

    /** 避开危险格走向目标。返回 dx, dy, 距离。 */
    private int[] route(int sx, int sy, int gx, int gy) {
        if (sx == gx && sy == gy) return new int[]{0, 0, 0};
        if (!walkable(gx, gy) || threatened(gx, gy) || hasBlast(gx, gy)) return null;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        boolean[][] seen = new boolean[Rules.ROWS][Rules.COLS];
        int[][] prevX = new int[Rules.ROWS][Rules.COLS];
        int[][] prevY = new int[Rules.ROWS][Rules.COLS];
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                prevX[y][x] = -1;
                prevY[y][x] = -1;
            }
        }
        queue.add(new int[]{sx, sy, 0});
        seen[sy][sx] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int x = cur[0];
            int y = cur[1];
            int dist = cur[2];
            if (x == gx && y == gy) {
                int[] step = firstDirs(prevX, prevY, x, y, sx, sy);
                if (step == null) return null;
                return new int[]{step[0], step[1], dist};
            }
            for (int[] dir : DIRS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (!walkable(nx, ny) || seen[ny][nx] || hasBlast(nx, ny) || threatened(nx, ny)) continue;
                if (inPocket(nx, ny)) continue;
                seen[ny][nx] = true;
                prevX[ny][nx] = x;
                prevY[ny][nx] = y;
                queue.add(new int[]{nx, ny, dist + 1});
            }
        }
        return null;
    }

    private int[] firstDirs(int[][] prevX, int[][] prevY, int x, int y, int sx, int sy) {
        while (prevX[y][x] != sx || prevY[y][x] != sy) {
            int px = prevX[y][x];
            int py = prevY[y][x];
            if (px < 0) return null;
            x = px;
            y = py;
        }
        return new int[]{x - sx, y - sy};
    }

    private Input followFlee(Player p, int tx, int ty) {
        int[] step = firstToward(tx, ty, fleeX, fleeY, true, 1f / p.speed());
        if (step == null) return null;
        Input toward = moveToward(p, tx + step[0], ty + step[1]);
        if (toward == null) return null;
        return hold(toward);
    }

    private boolean threatened(int x, int y) {
        return dangerAt(x, y) < 900f;
    }

    private Input commit(Player p, int[] dir) {
        if (dir == null || (dir[0] == 0 && dir[1] == 0) || !canStep(p, dir[0], dir[1])) {
            aiDx = 0;
            aiDy = 0;
            return Input.NONE;
        }
        aiDx = dir[0];
        aiDy = dir[1];
        return new Input(aiDx, aiDy, false);
    }

    private boolean nearCenter(Player p) {
        int tx = floor(p.x);
        int ty = floor(p.y);
        float dx = p.x - (tx + 0.5f);
        float dy = p.y - (ty + 0.5f);
        return dx * dx + dy * dy <= 0.0144f;
    }

    /** 出格方向要先贴着这一格的中线，身体才不会刮到墙角。 */
    private boolean linedUp(Player p, int dx, int dy) {
        if (dx == 0 && dy == 0) return false;
        float ox = p.x - (floor(p.x) + 0.5f);
        float oy = p.y - (floor(p.y) + 0.5f);
        float cross = dx != 0 ? Math.abs(oy) : Math.abs(ox);
        return cross <= 0.12f;
    }

    private Input hold(Input input) {
        if (input == null) return null;
        aiDx = input.dx;
        aiDy = input.dy;
        return input;
    }

    private Input moveToward(Player p, int gx, int gy) {
        float dx = (gx + 0.5f) - p.x;
        float dy = (gy + 0.5f) - p.y;
        // 一帧最多走这么远。误差比这还小就别反向，否则会在中线两边来回跨。
        float step = Math.max(0.08f, p.speed() * 0.05f);
        int hx = Math.abs(dx) < 0.02f ? 0 : (dx > 0f ? 1 : -1);
        int hy = Math.abs(dy) < 0.02f ? 0 : (dy > 0f ? 1 : -1);
        if (hx == -aiDx && aiDx != 0 && Math.abs(dx) <= step) hx = 0;
        if (hy == -aiDy && aiDy != 0 && Math.abs(dy) <= step) hy = 0;
        if (hx == aiDx && hx != 0 && canStep(p, hx, 0)) return new Input(hx, 0, false);
        if (hy == aiDy && hy != 0 && canStep(p, 0, hy)) return new Input(0, hy, false);
        if (Math.abs(dx) >= Math.abs(dy)) {
            if (hx != 0 && canStep(p, hx, 0)) return new Input(hx, 0, false);
            if (hy != 0 && canStep(p, 0, hy)) return new Input(0, hy, false);
        } else {
            if (hy != 0 && canStep(p, 0, hy)) return new Input(0, hy, false);
            if (hx != 0 && canStep(p, hx, 0)) return new Input(hx, 0, false);
        }
        return null;
    }

    private int[] pathStep(Player p, int tx, int ty) {
        int flame = p.flame();
        int[] chase = chaseStep(p, tx, ty);
        int[] bun = search(tx, ty, 0, p, flame);
        if (chase != null && (bun == null || chase[2] <= bun[2])) return chase;
        if (bun != null) return bun;
        int[] item = search(tx, ty, 1, p, flame);
        if (item != null && (chase == null || item[2] < chase[2])) return item;
        if (chase != null) return chase;
        if (countBombs(p.id) < p.bombMax()) return search(tx, ty, 2, p, flame);
        return null;
    }

    private int[] search(int sx, int sy, int mode, Player p, int flame) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int[][] prevX = new int[Rules.ROWS][Rules.COLS];
        int[][] prevY = new int[Rules.ROWS][Rules.COLS];
        boolean[][] seen = new boolean[Rules.ROWS][Rules.COLS];
        queue.add(new int[]{sx, sy, 0});
        seen[sy][sx] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int x = cur[0];
            int y = cur[1];
            int dist = cur[2];
            if ((x != sx || y != sy) && matches(x, y, mode, p, flame)) {
                while (prevX[y][x] != sx || prevY[y][x] != sy) {
                    int nx = prevX[y][x];
                    int ny = prevY[y][x];
                    x = nx;
                    y = ny;
                }
                return new int[]{x - sx, y - sy, dist};
            }
            for (int[] dir : DIRS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (!walkable(nx, ny) || seen[ny][nx] || threatened(nx, ny)) continue;
                seen[ny][nx] = true;
                prevX[ny][nx] = x;
                prevY[ny][nx] = y;
                queue.add(new int[]{nx, ny, dist + 1});
            }
        }
        return null;
    }

    private boolean matches(int x, int y, int mode, Player p, int flame) {
        Item item = itemAt(x, y);
        if (mode == 0) return item != null && item.kind == ItemKind.BUN;
        if (mode == 1) return item != null;
        if (item != null || !canFlee(p, x, y, flame)) return false;
        return besideSoft(x, y);
    }

    private boolean besideSoft(int tx, int ty) {
        for (int[] dir : DIRS) {
            int x = tx + dir[0];
            int y = ty + dir[1];
            if (inBounds(x, y) && map[y][x] == Tile.SOFT) return true;
        }
        return false;
    }

    private boolean canStep(Player p, int dx, int dy) {
        if (dx == 0 && dy == 0) return false;
        return !blocked(p.x + dx * 0.12f, p.y + dy * 0.12f, p);
    }

    private int[] openStep(Player p, int tx, int ty) {
        int[] best = new int[]{0, 0};
        float bestScore = -1f;
        for (int[] dir : DIRS) {
            int x = tx + dir[0];
            int y = ty + dir[1];
            if (!walkable(x, y) || threatened(x, y)) continue;
            float score = dangerAt(x, y);
            for (int[] next : DIRS) {
                int nx = x + next[0];
                int ny = y + next[1];
                if (nx == tx && ny == ty) continue;
                if (!walkable(nx, ny)) continue;
                score = Math.max(score, dangerAt(nx, ny));
            }
            if (score > bestScore) {
                bestScore = score;
                best = dir;
            }
        }
        return best;
    }

    private int[] nearestSafe(int sx, int sy, int bombX, int bombY, int flame, boolean throughDanger) {
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        boolean[][] seen = new boolean[Rules.ROWS][Rules.COLS];
        int[][] prevX = new int[Rules.ROWS][Rules.COLS];
        int[][] prevY = new int[Rules.ROWS][Rules.COLS];
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                prevX[y][x] = -1;
                prevY[y][x] = -1;
            }
        }
        queue.add(new int[]{sx, sy, 0});
        seen[sy][sx] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int x = cur[0];
            int y = cur[1];
            int dist = cur[2];
            if ((x != sx || y != sy) && cellSafe(x, y, bombX, bombY, flame)) {
                return new int[]{x, y, dist};
            }
            if (dist > Rules.COLS + Rules.ROWS) continue;
            for (int[] dir : DIRS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (!walkable(nx, ny) || seen[ny][nx] || hasBlast(nx, ny) || inPocket(nx, ny)) continue;
                if (!throughDanger && threatened(nx, ny)) continue;
                if (throughDanger && threatened(nx, ny) && dangerAt(nx, ny) + 0.05f < dangerAt(sx, sy)) continue;
                seen[ny][nx] = true;
                prevX[ny][nx] = x;
                prevY[ny][nx] = y;
                queue.add(new int[]{nx, ny, dist + 1});
            }
        }
        return null;
    }

    private int[] firstToward(int sx, int sy, int gx, int gy, boolean throughDanger, float stepTime) {
        if (sx == gx && sy == gy) return new int[]{0, 0};
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        boolean[][] seen = new boolean[Rules.ROWS][Rules.COLS];
        int[][] prevX = new int[Rules.ROWS][Rules.COLS];
        int[][] prevY = new int[Rules.ROWS][Rules.COLS];
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                prevX[y][x] = -1;
                prevY[y][x] = -1;
            }
        }
        queue.add(new int[]{sx, sy, 0});
        seen[sy][sx] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int x = cur[0];
            int y = cur[1];
            int dist = cur[2];
            if (x == gx && y == gy) {
                while (prevX[y][x] != sx || prevY[y][x] != sy) {
                    int px = prevX[y][x];
                    int py = prevY[y][x];
                    if (px < 0) return null;
                    x = px;
                    y = py;
                }
                return new int[]{x - sx, y - sy};
            }
            for (int[] dir : DIRS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                int nd = dist + 1;
                if (!walkable(nx, ny) || seen[ny][nx] || hasBlast(nx, ny) || inPocket(nx, ny)) continue;
                if (stepTime > 0f && (nd + 1) * stepTime + 0.45f >= dangerAt(nx, ny)) continue;
                if (!throughDanger && threatened(nx, ny)) continue;
                if (throughDanger && threatened(nx, ny) && dangerAt(nx, ny) + 0.05f < dangerAt(sx, sy)) continue;
                seen[ny][nx] = true;
                prevX[ny][nx] = x;
                prevY[ny][nx] = y;
                queue.add(new int[]{nx, ny, nd});
            }
        }
        return null;
    }

    /**
     * 假设在这里放一颗糖泡，找一条来得及走出去的路。
     * 经过的格子只要能在爆炸前离开即可，终点必须完全不会被炸到。
     * 返回第一步方向和终点，没有则 null。
     */
    private int[] escapeRoute(Player p, int sx, int sy, int flame, boolean shy) {
        boolean[][] sealed = new boolean[Rules.ROWS][Rules.COLS];
        markPocket(sealed, null, sx, sy, flame);
        float stepTime = 1f / Math.max(0.1f, p.speed());
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        boolean[][] seen = new boolean[Rules.ROWS][Rules.COLS];
        int[][] prevX = new int[Rules.ROWS][Rules.COLS];
        int[][] prevY = new int[Rules.ROWS][Rules.COLS];
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                prevX[y][x] = -1;
                prevY[y][x] = -1;
            }
        }
        queue.add(new int[]{sx, sy, 0});
        seen[sy][sx] = true;
        while (!queue.isEmpty()) {
            int[] cur = queue.removeFirst();
            int x = cur[0];
            int y = cur[1];
            int dist = cur[2];
            if (dist > 0 && canStay(x, y, sx, sy, flame) && (!shy || !foeCanHit(p, x, y))) {
                int gx = x;
                int gy = y;
                while (prevX[y][x] != sx || prevY[y][x] != sy) {
                    int px = prevX[y][x];
                    int py = prevY[y][x];
                    if (px < 0) return null;
                    x = px;
                    y = py;
                }
                return new int[]{x - sx, y - sy, gx, gy};
            }
            if (dist > Rules.COLS + Rules.ROWS) continue;
            for (int[] dir : DIRS) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                int nd = dist + 1;
                if (!walkable(nx, ny) || seen[ny][nx] || hasBlast(nx, ny)) continue;
                if (sealed[ny][nx] || inPocket(nx, ny)) continue;
                if ((nd + 1) * stepTime + 0.45f >= dangerIfPlaced(nx, ny, sx, sy, flame)) continue;
                seen[ny][nx] = true;
                prevX[ny][nx] = x;
                prevY[ny][nx] = y;
                queue.add(new int[]{nx, ny, nd});
            }
        }
        return null;
    }

    private boolean canStay(int x, int y, int bombX, int bombY, int flame) {
        if (hasBlast(x, y) || threatened(x, y)) return false;
        return !blastHits(bombX, bombY, flame, x, y);
    }

    private float dangerIfPlaced(int x, int y, int bombX, int bombY, int flame) {
        float danger = dangerAt(x, y);
        if (blastHits(bombX, bombY, flame, x, y)) danger = Math.min(danger, Rules.FUSE);
        return danger;
    }

    private boolean cellSafe(int x, int y, int bombX, int bombY, int flame) {
        if (threatened(x, y)) return false;
        return flame < 0 || !blastHits(bombX, bombY, flame, x, y);
    }

    private int[] safestNeighbor(int tx, int ty) {
        int[] best = new int[]{0, 0};
        float bestScore = -1f;
        for (int[] dir : DIRS) {
            int x = tx + dir[0];
            int y = ty + dir[1];
            if (!walkable(x, y) || inPocket(x, y) || inAlley(x, y)) continue;
            float score = dangerAt(x, y);
            if (score > bestScore) {
                bestScore = score;
                best = dir;
            }
        }
        return best;
    }

    private boolean canFlee(Player p, int x, int y, int flame) {
        return escapeRoute(p, x, y, flame, false) != null;
    }

    private float dangerAt(int tx, int ty) {
        if (hasBlast(tx, ty)) return 0f;
        float best = 999f;
        for (Bomb bomb : bombs) {
            if (!reaches(bomb, tx, ty)) continue;
            best = Math.min(best, chainFuse(bomb, new HashSet<Bomb>()));
        }
        return best;
    }

    private boolean blastHits(int bx, int by, int flame, int tx, int ty) {
        if (bx != tx && by != ty) return false;
        int dist = Math.abs(bx - tx) + Math.abs(by - ty);
        if (dist > flame) return false;
        if (dist == 0) return true;
        int sx = sgn(tx - bx);
        int sy = sgn(ty - by);
        int x = bx;
        int y = by;
        int leftSteps = dist;
        while (leftSteps > 0) {
            x += sx;
            y += sy;
            leftSteps--;
            if (!inBounds(x, y)) return false;
            if (map[y][x] == Tile.HARD) return false;
            if (map[y][x] == Tile.SOFT) return leftSteps == 0;
        }
        return true;
    }

    private float chainFuse(Bomb bomb, HashSet<Bomb> seen) {
        if (!seen.add(bomb)) return bomb.fuse;
        float fuse = bomb.fuse;
        for (Bomb other : bombs) {
            if (other == bomb) continue;
            if (reaches(other, bomb.x, bomb.y)) fuse = Math.min(fuse, chainFuse(other, seen));
        }
        return fuse;
    }

    private boolean reaches(Bomb bomb, int tx, int ty) {
        return blastHits(bomb.x, bomb.y, bomb.flame, tx, ty);
    }

    private void refreshPockets() {
        if (pocket == null) pocket = new boolean[Rules.ROWS][Rules.COLS];
        if (alley == null) alley = new boolean[Rules.ROWS][Rules.COLS];
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                pocket[y][x] = false;
                alley[y][x] = false;
            }
        }
        for (Bomb bomb : bombs) markPocket(pocket, alley, bomb.x, bomb.y, bomb.flame);
    }

    private boolean inPocket(int x, int y) {
        return pocket != null && inBounds(x, y) && pocket[y][x];
    }

    private boolean inAlley(int x, int y) {
        return alley != null && inBounds(x, y) && alley[y][x];
    }

    /**
     * 这颗糖泡若是死胡同的唯一出口，而且胡同里没有炸不到的格子，就把胡同标出来。
     * 另一头还连着外面时才算死胡同，直走廊的两边都不标。
     */
    private void markPocket(boolean[][] death, boolean[][] shortArm, int bx, int by, int flame) {
        int[][] comp = new int[Rules.ROWS][Rules.COLS];
        int[] touch = new int[5];
        int[] size = new int[5];
        boolean[] refuge = new boolean[5];
        int id = 0;
        int entrances = 0;
        for (int[] dir : DIRS) {
            int nx = bx + dir[0];
            int ny = by + dir[1];
            if (!emptyCell(nx, ny) || hasBomb(nx, ny)) continue;
            entrances++;
            if (comp[ny][nx] != 0) {
                touch[comp[ny][nx]]++;
                continue;
            }
            id++;
            touch[id] = 1;
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{nx, ny});
            comp[ny][nx] = id;
            while (!queue.isEmpty()) {
                int[] cur = queue.removeFirst();
                int x = cur[0];
                int y = cur[1];
                size[id]++;
                if (!blastHits(bx, by, flame, x, y) && !threatened(x, y) && !hasBlast(x, y)) {
                    refuge[id] = true;
                }
                for (int[] step : DIRS) {
                    int ex = x + step[0];
                    int ey = y + step[1];
                    if (ex == bx && ey == by) continue;
                    if (!emptyCell(ex, ey) || hasBomb(ex, ey) || comp[ey][ex] != 0) continue;
                    comp[ey][ex] = id;
                    queue.add(new int[]{ex, ey});
                }
            }
        }
        if (entrances <= 1) return;
        int biggest = 0;
        for (int c = 1; c <= id; c++) biggest = Math.max(biggest, size[c]);
        for (int c = 1; c <= id; c++) {
            if (touch[c] != 1) continue;
            boolean deathArm = !refuge[c];
            boolean smaller = size[c] < biggest;
            if (!deathArm && !smaller) continue;
            for (int y = 0; y < Rules.ROWS; y++) {
                for (int x = 0; x < Rules.COLS; x++) {
                    if (comp[y][x] != c) continue;
                    if (death != null && deathArm) death[y][x] = true;
                    if (shortArm != null && smaller) shortArm[y][x] = true;
                }
            }
        }
    }

    private boolean emptyCell(int x, int y) {
        return inBounds(x, y) && map[y][x] == Tile.EMPTY;
    }

    private boolean walkable(int x, int y) {
        if (!inBounds(x, y)) return false;
        if (map[y][x] != Tile.EMPTY) return false;
        return !hasBomb(x, y);
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < Rules.COLS && y >= 0 && y < Rules.ROWS;
    }

    private boolean hasBomb(int x, int y) {
        for (Bomb bomb : bombs) {
            if (bomb.x == x && bomb.y == y) return true;
        }
        return false;
    }

    private boolean hasBlast(int x, int y) {
        for (Blast blast : blasts) {
            if (blast.x == x && blast.y == y) return true;
        }
        return false;
    }

    private Item itemAt(int x, int y) {
        for (Item item : items) {
            if (item.x == x && item.y == y) return item;
        }
        return null;
    }

    private int countBombs(int owner) {
        int n = 0;
        for (Bomb bomb : bombs) {
            if (bomb.owner == owner) n++;
        }
        return n;
    }

    private int countKind(ItemKind kind) {
        int n = 0;
        for (Item item : items) {
            if (item.kind == kind) n++;
        }
        return n;
    }

    private static int sgn(int v) {
        return Integer.compare(v, 0);
    }

    private static int floor(float v) {
        return (int) Math.floor(v);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
