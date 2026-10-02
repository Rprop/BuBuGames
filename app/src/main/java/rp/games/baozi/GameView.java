package rp.games.baozi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.io.InputStream;
import java.util.ArrayList;

import rp.games.R;
import rp.games.baozi.game.InputState;
import rp.games.baozi.game.ItemView;
import rp.games.baozi.game.Look;
import rp.games.baozi.game.Phase;
import rp.games.baozi.game.PlayerView;
import rp.games.baozi.game.Rules;
import rp.games.baozi.game.Snap;
import rp.games.baozi.game.SnapBuffer;

public class GameView extends View {
    public final InputState input = new InputState();
    public SnapBuffer buffer;
    public int mySlot;
    public volatile String notice = "";
    public Bitmap roomQr;
    public String roomLabel = "";

    private boolean runningDraw = true;
    private float tile = 1f;
    private float originX;
    private float originY;
    private float hudH;
    private float mapW;
    private float mapH;
    private float sx;
    private float sy;
    private float ex;
    private float ey;

    private final RectF joyOuter = new RectF();
    private final RectF bombOuter = new RectF();
    public Runnable onHelp;
    public Runnable onAgain;
    public Runnable onLeave;
    public Runnable onRetry;
    public volatile boolean canRetry;
    /**
     * 最近一次延迟毫秒。加入方是往返时间，房主是相邻两次发出时间之差再减去 1 秒。还没测到时为 Long.MIN_VALUE。
     */
    public volatile long pingMs = Long.MIN_VALUE;
    private final RectF helpBtn = new RectF();
    private final RectF againBtn = new RectF();
    private final RectF leaveBtn = new RectF();
    private final RectF retryBtn = new RectF();
    private boolean showAgain;
    private boolean showRetry;
    private final RectF cellR = new RectF();
    private final RectF tmp = new RectF();
    private int movePointer = -1;
    private int bombPointer = -1;
    private boolean bombDown;
    private float knobX;
    private float knobY;
    private float lastStun;
    private final float[] stunWas = new float[2];
    private final float[] stunX = new float[2];
    private final float[] stunY = new float[2];
    private final ArrayList<Burst> bursts = new ArrayList<>();
    private long burstClock;

    private Bitmap yierBmp;
    private Bitmap bubuBmp;
    private final HeroSheet[] heroes = new HeroSheet[Look.SKINS];
    private final HeroWalk[] walks = {new HeroWalk(), new HeroWalk()};
    private Bitmap[] placedBombs;
    private Bitmap[] standMarks;
    private Bitmap[] masks;
    private Bitmap hudBun;
    private Bitmap hudBomb;
    private Bitmap hudFlame;
    private Bitmap hudShoe;
    private Bitmap hudFork;
    private Bitmap[] wallBitmaps;
    private Bitmap[] softBitmaps;

    private final Paint floorA;
    private final Paint floorB;
    private final Paint hardFill;
    private final Paint hardLine;
    private final Paint softFill;
    private final Paint softLine;
    private final Paint mapShadow;
    private final Paint hudBg;
    private final Paint white;
    private final Paint ink;
    private final Paint joyFill;
    private final Paint joyStroke;
    private final Paint joyKnob;
    private final Paint bombFill;
    private final ItemGlyph items = new ItemGlyph();
    private final Paint cardFill;
    private final Paint cardStroke;
    private final Paint btnFill;
    private final Paint blastFill;
    private final Paint fold;
    private final Paint shadow;
    private final Paint yierBody;
    private final Paint yierEar;
    private final Paint yierBow;
    private final Paint yierBlush;
    private final Paint bubuBody;
    private final Paint bubuBelly;
    private final Paint bubuEar;
    private final Paint bmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint qrPaint = new Paint();
    private final Paint hudName;
    private final Paint hudSub;
    private final Paint timerPaint;
    private final Paint namePaint;
    private final Paint cardBody;
    private final Paint cardBig;
    private final Paint bombText;
    private final Paint hintPaint;
    private static final int TONE_WIN = 0;
    private static final int TONE_LOSE = 1;
    private static final int TONE_DRAW = 2;
    private final Paint resultTitle;
    private final Paint resultFlavor;
    private final Paint resultName;
    private final Paint resultScore;
    private final Paint resultBadge;
    private final Paint sparkFill;
    private final Paint badgeFill;
    private final Paint ribbonFill;
    private final Paint barTrack;
    private final Paint barFill;
    private final Paint dullPaint;
    private final Path barClip;
    private final int pingGreen;
    private final int pingYellow;
    private final int pingRed;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        floorA = fill(Color.parseColor("#F8E3C0"));
        floorB = fill(Color.parseColor("#F3D7A6"));
        hardFill = fill(Color.parseColor("#8D5E45"));
        hardLine = stroke(Color.parseColor("#6E4632"), 2f);
        softFill = fill(Color.parseColor("#E7B56A"));
        softLine = stroke(Color.parseColor("#C48A45"), 2f);
        mapShadow = fill(Color.parseColor("#33A56A3A"));
        hudBg = fill(Color.parseColor("#F6E2C4"));
        white = fill(Color.WHITE);
        ink = fill(Color.parseColor("#3A2A22"));
        joyFill = fill(Color.parseColor("#66FFFFFF"));
        joyStroke = stroke(Color.parseColor("#CCFFFFFF"), 3f);
        joyKnob = fill(Color.parseColor("#E6FFFFFF"));
        bombFill = fill(Color.parseColor("#FF8FAB"));
        cardFill = fill(Color.parseColor("#F2FFF8EE"));
        cardStroke = stroke(Color.parseColor("#E7B56A"), 3f);
        btnFill = fill(Color.parseColor("#F07A3A"));
        blastFill = fill(Color.parseColor("#FFB703"));
        fold = stroke(Color.parseColor("#C9A078"), 2f);
        shadow = fill(Color.parseColor("#33000000"));
        yierBody = fill(Color.parseColor("#FFFDFC"));
        yierEar = fill(Color.parseColor("#FFB7C8"));
        yierBow = fill(Color.parseColor("#F284A8"));
        yierBlush = fill(Color.parseColor("#FFC0D0"));
        bubuBody = fill(Color.parseColor("#C4844A"));
        bubuBelly = fill(Color.parseColor("#F6D7B5"));
        bubuEar = fill(Color.parseColor("#E7B07A"));
        hudName = text(15f, Color.parseColor("#C4537A"), Paint.Align.LEFT);
        hudSub = text(11f, Color.parseColor("#8A5A40"), Paint.Align.LEFT);
        timerPaint = text(20f, Color.parseColor("#6B3F2A"), Paint.Align.CENTER);
        namePaint = text(11f, Color.WHITE, Paint.Align.CENTER);
        cardBody = text(16f, Color.parseColor("#6B3F2A"), Paint.Align.CENTER);
        cardBig = text(64f, Color.parseColor("#F07A3A"), Paint.Align.CENTER);
        bombText = text(16f, Color.WHITE, Paint.Align.CENTER);
        hintPaint = text(12f, Color.parseColor("#2E9B4F"), Paint.Align.CENTER);
        pingGreen = Color.parseColor("#2E9B4F");
        pingYellow = Color.parseColor("#D4A017");
        pingRed = Color.parseColor("#D6453D");
        setClickable(true);
        setKeepScreenOn(true);
        yierBmp = openBmp("yier.png");
        bubuBmp = openBmp("bubu.png");
        items.bind(context);
        for (int i = 0; i < Profile.SKIN_RES.length && i < heroes.length; i++) {
            heroes[i] = loadHero(Profile.SKIN_RES[i]);
        }
        placedBombs = new Bitmap[]{
                BitmapFactory.decodeResource(getResources(), R.drawable.bom_0),
                BitmapFactory.decodeResource(getResources(), R.drawable.bom_1),
                BitmapFactory.decodeResource(getResources(), R.drawable.bom_2),
                BitmapFactory.decodeResource(getResources(), R.drawable.bom_3)
        };
        standMarks = new Bitmap[]{
                BitmapFactory.decodeResource(getResources(), R.drawable.stand_0),
                BitmapFactory.decodeResource(getResources(), R.drawable.stand_1),
                BitmapFactory.decodeResource(getResources(), R.drawable.stand_2),
                BitmapFactory.decodeResource(getResources(), R.drawable.stand_3),
                BitmapFactory.decodeResource(getResources(), R.drawable.stand_4),
                BitmapFactory.decodeResource(getResources(), R.drawable.stand_5)
        };
        hudBun = still(R.drawable.baozi);
        hudBomb = still(R.drawable.bomb);
        hudFlame = still(R.drawable.potion);
        hudShoe = still(R.drawable.shoe);
        hudFork = still(R.drawable.fork);
        masks = new Bitmap[]{
                still(R.drawable.mask_0),
                still(R.drawable.mask_1),
                still(R.drawable.mask_2),
                still(R.drawable.mask_3),
                still(R.drawable.mask_4)
        };
        wallBitmaps = new Bitmap[]{
                still(R.drawable.wall_001),
                still(R.drawable.wall_002),
                still(R.drawable.wall_003),
                still(R.drawable.wall_004),
                still(R.drawable.wall_005),
                still(R.drawable.wall_006),
                still(R.drawable.wall_007),
                still(R.drawable.wall_008),
                still(R.drawable.wall_009),
                still(R.drawable.wall_010),
                still(R.drawable.wall_011),
                still(R.drawable.wall_012),
                still(R.drawable.wall_013),
                still(R.drawable.wall_014),
                still(R.drawable.wall_015),
        };
        softBitmaps = new Bitmap[]{
                still(R.drawable.wall_soft_001),
                still(R.drawable.wall_soft_002),
                still(R.drawable.wall_soft_003),
                still(R.drawable.wall_soft_004),
                still(R.drawable.wall_soft_005),
                still(R.drawable.wall_soft_006),
                still(R.drawable.wall_soft_007),
                still(R.drawable.wall_soft_008),
                still(R.drawable.wall_soft_009),
                still(R.drawable.wall_soft_010)
        };
        timerPaint.setFakeBoldText(true);
        cardBig.setFakeBoldText(true);
        bombText.setFakeBoldText(true);
        resultTitle = text(22f, Color.WHITE, Paint.Align.CENTER);
        resultFlavor = text(13f, Color.parseColor("#8A5A40"), Paint.Align.CENTER);
        resultName = text(13f, Color.parseColor("#6B3F2A"), Paint.Align.CENTER);
        resultScore = text(16f, Color.parseColor("#6B3F2A"), Paint.Align.LEFT);
        resultBadge = text(11f, Color.WHITE, Paint.Align.CENTER);
        sparkFill = fill(Color.WHITE);
        badgeFill = fill(Color.parseColor("#E07A2F"));
        ribbonFill = fill(Color.parseColor("#E07A2F"));
        barTrack = fill(Color.parseColor("#E8D4BE"));
        barFill = fill(Color.parseColor("#E07A9A"));
        dullPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        barClip = new Path();
        final ColorMatrix gray = new ColorMatrix();
        gray.setSaturation(0.22f);
        dullPaint.setColorFilter(new ColorMatrixColorFilter(gray));
        resultTitle.setFakeBoldText(true);
        resultScore.setFakeBoldText(true);
        resultBadge.setFakeBoldText(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        runningDraw = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float s = getResources().getDisplayMetrics().density;
        float margin = 16f * s;
        float joy = Math.min(150f * s, h * 0.46f);
        joyOuter.set(margin, h - margin - joy, margin + joy, h - margin);
        float bomb = Math.min(112f * s, h * 0.34f);
        bombOuter.set(w - margin - bomb, h - margin - bomb, w - margin, h - margin);
        float helpW = 88f * s;
        float helpH = 40f * s;
        float helpTop = Math.min(joyOuter.top, bombOuter.top) - 8f * s - helpH;
        helpBtn.set((w - helpW) / 2f, helpTop, (w + helpW) / 2f, helpTop + helpH);
        if (movePointer == -1) {
            knobX = joyOuter.centerX();
            knobY = joyOuter.centerY();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Snap snap = buffer == null ? null : buffer.sample();
        showAgain = false;
        showRetry = false;
        againBtn.setEmpty();
        leaveBtn.setEmpty();
        retryBtn.setEmpty();
        if (snap == null) {
            canvas.drawColor(Color.parseColor("#FFE6C4"));
            if (notice != null && !notice.isEmpty()) {
                showRetry = canRetry && onRetry != null;
                drawCard(canvas, showRetry ? notice : notice + "\n按返回键离开", false);
                if (showRetry) drawChoices(canvas, false);
            }
            if (runningDraw) postInvalidateOnAnimation();
            return;
        }
        maybeVibrate(snap);
        layoutMap();
        drawWorld(canvas, snap);
        drawHud(canvas, snap);
        final boolean settled = snap.phase == Phase.OVER && (notice == null || notice.isEmpty());
        if (joyOuter.width() > 0f && !settled) drawControls(canvas, snap);
        drawOverlay(canvas, snap);
        if (runningDraw) postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int index = event.getActionIndex();
            int id = event.getPointerId(index);
            float x = event.getX(index);
            float y = event.getY(index);
            if (showRetry && retryBtn.contains(x, y)) {
                if (onRetry != null) onRetry.run();
            } else if ((showAgain || showRetry) && leaveBtn.contains(x, y)) {
                if (onLeave != null) onLeave.run();
            } else if (showAgain && againBtn.contains(x, y)) {
                if (onAgain != null) onAgain.run();
            } else if (showAgain || showRetry) {
                // 结算和重试时不带动摇杆
            } else if (bombPointer == -1 && bombOuter.contains(x, y)) {
                bombPointer = id;
                bombDown = true;
                input.tapBomb();
            } else if (movePointer == -1 && joyOuter.contains(x, y)) {
                movePointer = id;
                updateJoy(x, y);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            int index = event.findPointerIndex(movePointer);
            if (index >= 0) updateJoy(event.getX(index), event.getY(index));
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int id = event.getPointerId(event.getActionIndex());
            if (id == movePointer) releaseJoy();
            if (id == bombPointer) {
                bombPointer = -1;
                bombDown = false;
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            releaseJoy();
            bombPointer = -1;
            bombDown = false;
        }
        return true;
    }

    private void releaseJoy() {
        movePointer = -1;
        input.setDir(0, 0);
        knobX = joyOuter.centerX();
        knobY = joyOuter.centerY();
    }

    private void updateJoy(float x, float y) {
        float cx = joyOuter.centerX();
        float cy = joyOuter.centerY();
        float dx = x - cx;
        float dy = y - cy;
        float maxR = joyOuter.width() / 2f;
        float dist = (float) Math.hypot(dx, dy);
        if (dist < 0.001f) dist = 0.001f;
        if (dist > maxR) {
            dx *= maxR / dist;
            dy *= maxR / dist;
        }
        knobX = cx + dx;
        knobY = cy + dy;
        if (dist / maxR < 0.28f) {
            input.setDir(0, 0);
        } else if (Math.abs(dx) > Math.abs(dy)) {
            input.setDir(dx > 0f ? 1 : -1, 0);
        } else {
            input.setDir(0, dy > 0f ? 1 : -1);
        }
    }

    private void layoutMap() {
        hudH = dp(50f);
        float tileW = getWidth() / (float) Rules.COLS;
        float tileH = (getHeight() - hudH) / (float) Rules.ROWS;
        tile = Math.min(tileW, tileH);
        mapW = tile * Rules.COLS;
        mapH = tile * Rules.ROWS;
        float spanLeft = joyOuter.width() > 0f ? joyOuter.right : 0f;
        float spanRight = bombOuter.width() > 0f ? bombOuter.left : getWidth();
        originX = (spanLeft + spanRight - mapW) / 2f;
        if (originX < 0f) originX = 0f;
        float maxX = getWidth() - mapW;
        if (originX > maxX) originX = maxX;
        originY = hudH + (getHeight() - hudH - mapH) / 2f;
    }

    private void drawWorld(Canvas canvas, Snap snap) {
        canvas.drawColor(Color.parseColor("#FFE6C4"));
        tmp.set(originX + dp(3f), originY + dp(4f), originX + mapW + dp(3f), originY + mapH + dp(4f));
        canvas.drawRect(tmp, mapShadow);
        String tiles = snap.tiles;
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                cellRect(x, y);
                canvas.drawRect(cellR, (x + y) % 2 == 0 ? floorA : floorB);
            }
        }
        for (int y = 0; y < Rules.ROWS; y++) {
            for (int x = 0; x < Rules.COLS; x++) {
                char tileCh = tileAt(tiles, x, y);
                if (tileCh == '#') drawHard(canvas, x, y, wallSkinAt(snap.walls, x, y, 'A', 'Y'));
                else if (tileCh == 'o')
                    drawSoft(canvas, x, y, wallSkinAt(snap.walls, x, y, 'a', 'j'));
            }
        }
        for (ItemView item : snap.items) drawItem(canvas, item);
        float time = SystemClock.uptimeMillis() / 1000f;
        for (rp.games.baozi.game.BombView bomb : snap.bombs) {
            drawBomb(canvas, bomb.x, bomb.y, bomb.fuse);
        }
        for (PlayerView p : snap.players) {
            if (p.away > 0f) continue;
            drawActor(canvas, p, time);
        }
        for (rp.games.baozi.game.BlastView blast : snap.blasts) {
            drawBlast(canvas, blast.x, blast.y, blast.life);
        }
        for (PlayerView p : snap.players) {
            if (p.stun > 0f) {
                worldToScreen(p.x, p.y);
                drawTrap(canvas, sx, sy);
            }
        }
        tickBursts(snap);
        for (int i = 0; i < bursts.size(); i++) {
            Burst burst = bursts.get(i);
            drawBurst(canvas, burst.x, burst.y, burst.age);
        }
    }

    private void drawHard(Canvas canvas, int x, int y, int skin) {
        cellRect(x, y);
        if (wallBitmaps != null && skin >= 0 && skin < wallBitmaps.length && wallBitmaps[skin] != null) {
            canvas.drawBitmap(wallBitmaps[skin], null, cellR, bmpPaint);
            return;
        }
        float inset = tile * 0.04f;
        tmp.set(cellR.left + inset, cellR.top + inset, cellR.right - inset, cellR.bottom - inset);
        canvas.drawRoundRect(tmp, tile * 0.08f, tile * 0.08f, hardFill);
        hardLine.setStrokeWidth(dp(1.2f));
        float midY = (tmp.top + tmp.bottom) / 2f;
        canvas.drawLine(tmp.left, midY, tmp.right, midY, hardLine);
        canvas.drawLine((tmp.left + tmp.right) / 2f, tmp.top, (tmp.left + tmp.right) / 2f, midY, hardLine);
    }

    private void drawSoft(Canvas canvas, int x, int y, int skin) {
        cellRect(x, y);
        if (softBitmaps != null && skin >= 0 && skin < softBitmaps.length && softBitmaps[skin] != null) {
            canvas.drawBitmap(softBitmaps[skin], null, cellR, bmpPaint);
            return;
        }
        float inset = tile * 0.1f;
        tmp.set(cellR.left + inset, cellR.top + inset, cellR.right - inset, cellR.bottom - inset);
        canvas.drawRoundRect(tmp, tile * 0.12f, tile * 0.12f, softFill);
        softLine.setStrokeWidth(dp(1.4f));
        canvas.drawRoundRect(tmp, tile * 0.12f, tile * 0.12f, softLine);
        float cx = tmp.centerX();
        float cy = tmp.centerY();
        canvas.drawLine(tmp.left + dp(4f), cy, tmp.right - dp(4f), cy, softLine);
        canvas.drawLine(cx, tmp.top + dp(4f), cx, tmp.bottom - dp(4f), softLine);
    }

    private void drawItem(Canvas canvas, ItemView item) {
        float tx = item.x + 0.5f;
        float ty = item.y + 0.5f;
        float wx = tx;
        float wy = ty;
        float hop = 0f;
        if (item.fly > 0f && item.span > 0f) {
            float u = 1f - item.fly / item.span;
            if (u < 0f) u = 0f;
            if (u > 1f) u = 1f;
            float ease = 1f - (1f - u) * (1f - u);
            wx = item.fromX + (tx - item.fromX) * ease;
            wy = item.fromY + (ty - item.fromY) * ease;
            hop = (float) Math.sin(u * Math.PI) * tile * 0.72f;
        }
        worldToScreen(wx, wy);
        items.draw(canvas, item.kind, sx, sy - hop, tile * 0.42f);
    }

    private void drawBomb(Canvas canvas, int x, int y, float fuse) {
        if (placedBombs == null || placedBombs.length == 0) return;
        worldToScreen(x + 0.5f, y + 0.5f);
        float urgency = 1f - clamp(fuse / Rules.FUSE, 0f, 1f);
        int frame = (int) (urgency * placedBombs.length);
        if (frame >= placedBombs.length) frame = placedBombs.length - 1;
        if (frame < 0) frame = 0;
        Bitmap bitmap = placedBombs[frame];
        if (bitmap == null) return;
        float h = tile * 0.78f;
        float w = h * bitmap.getWidth() / (float) bitmap.getHeight();
        tmp.set(sx - w * 0.5f, sy - h * 0.5f, sx + w * 0.5f, sy + h * 0.5f);
        canvas.drawBitmap(bitmap, null, tmp, bmpPaint);
    }

    private void drawBlast(Canvas canvas, int x, int y, float life) {
        float k = clamp(life / Rules.BLAST_TIME, 0f, 1f);
        worldToScreen(x + 0.5f, y + 0.5f);
        float rad = tile * (0.42f + (1f - k) * 0.28f);
        blastFill.setColor(Color.parseColor("#FFB703"));
        blastFill.setAlpha(clampInt((int) (220 * k), 0, 255));
        canvas.drawCircle(sx, sy, rad, blastFill);
        white.setAlpha(clampInt((int) (180 * k), 0, 255));
        canvas.drawCircle(sx, sy, rad * 0.45f, white);
        white.setAlpha(255);
        blastFill.setAlpha(255);
    }

    private void drawActor(Canvas canvas, PlayerView p, float time) {
        worldToScreen(p.x, p.y);
        float cx = sx;
        float cy0 = sy;
        float r = tile * 0.34f;
        float bob = 0f;
        if (p.stun <= 0f) {
            float speed = p.id == 0 ? 8f : 3.2f;
            float amp = p.id == 0 ? 0.045f : 0.02f;
            bob = (float) Math.sin(time * speed + p.id) * tile * amp;
        }
        float cy = cy0 + bob;
        canvas.drawOval(cx - r * 0.7f, cy0 + r * 0.72f, cx + r * 0.7f, cy0 + r * 1.05f, shadow);
        int skin = Look.clampSkin(p.skin, Look.roleSkin(p.id));
        HeroSheet hero = heroes[skin];
        HeroWalk walk = p.id == 1 ? walks[1] : walks[0];
        Bitmap bmp = null;
        if (hero != null) bmp = heroFrame(hero, walk, p, time);
        else if (p.id == 0) bmp = yierBmp;
        else bmp = bubuBmp;
        if (bmp != null && hero != null) {
            drawHeroSheet(canvas, bmp, cx, cy, r);
        } else if (bmp != null) {
            tmp.set(cx - r * 1.2f, cy - r * 1.45f, cx + r * 1.2f, cy + r * 1.05f);
            canvas.drawBitmap(bmp, null, tmp, bmpPaint);
        } else if (p.id == 0) {
            drawYier(canvas, cx, cy, r, p.facing);
        } else {
            drawBubu(canvas, cx, cy, r, p.facing);
        }
        float nameY = cy - r * 1.55f;
        if (p.id == mySlot) {
            drawStand(canvas, cx, nameY - namePaint.ascent() + tile * 0.04f);
        } else {
            namePaint.setColor(p.id == 0 ? Color.parseColor("#C4537A") : Color.parseColor("#8A5324"));
            canvas.drawText(Look.shown(p.name, p.id), cx, nameY, namePaint);
        }
    }

    private void drawStand(Canvas canvas, float cx, float nameY) {
        if (standMarks == null || standMarks.length == 0) return;
        int frame = (int) ((SystemClock.uptimeMillis() / 300L) % standMarks.length);
        if (frame < 0) frame = 0;
        Bitmap bitmap = standMarks[frame];
        if (bitmap == null || bitmap.getWidth() <= 0) return;
        float px = tile * 0.10f / 20f;
        float w = bitmap.getWidth() * px;
        float h = bitmap.getHeight() * px;
        float bottom = nameY + namePaint.ascent() - tile * 0.04f;
        tmp.set(cx - w * 0.5f, bottom - h, cx + w * 0.5f, bottom);
        canvas.drawBitmap(bitmap, null, tmp, bmpPaint);
    }

    private HeroSheet loadHero(int resId) {
        Bitmap sheet = BitmapFactory.decodeResource(getResources(), resId);
        if (sheet == null) return null;
        int cols = 4;
        int rows = 4;
        int fw = sheet.getWidth() / cols;
        int fh = sheet.getHeight() / rows;
        if (fw <= 0 || fh <= 0) return null;
        HeroSheet hero = new HeroSheet();
        hero.frames = new Bitmap[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                hero.frames[row][col] = Bitmap.createBitmap(sheet, col * fw, row * fh, fw, fh);
            }
        }
        return hero;
    }

    private Bitmap heroFrame(HeroSheet hero, HeroWalk walk, PlayerView p, float time) {
        if (p.facing == 2) walk.row = 0;
        else if (p.facing == 3) walk.row = 1;
        else if (p.facing == 1) walk.row = 2;
        else walk.row = 3;
        if (!walk.seen) {
            walk.lastX = p.x;
            walk.lastY = p.y;
            walk.seen = true;
        }
        float dx = p.x - walk.lastX;
        float dy = p.y - walk.lastY;
        boolean holding = p.moveX != 0 || p.moveY != 0;
        boolean moving = p.stun <= 0f && (holding || dx * dx + dy * dy > 0.0004f);
        walk.lastX = p.x;
        walk.lastY = p.y;
        int frame = moving ? (int) (time * 8f) % hero.frames[0].length : 0;
        if (frame < 0) frame = 0;
        return hero.frames[walk.row][frame];
    }

    private void drawHeroSheet(Canvas canvas, Bitmap bmp, float cx, float cy, float r) {
        float size = tile * 1.25f;
        float top = cy + r * 0.9f - size * 0.99f;
        tmp.set(cx - size * 0.5f, top, cx + size * 0.5f, top + size);
        canvas.drawBitmap(bmp, null, tmp, bmpPaint);
    }

    private void drawYier(Canvas canvas, float cx, float cy, float r, int facing) {
        eyeShift(facing, r);
        canvas.drawCircle(cx - r * 0.55f, cy - r * 0.85f, r * 0.38f, yierBody);
        canvas.drawCircle(cx + r * 0.62f, cy - r * 0.9f, r * 0.34f, yierBody);
        canvas.drawCircle(cx - r * 0.55f, cy - r * 0.85f, r * 0.2f, yierEar);
        canvas.drawCircle(cx + r * 0.62f, cy - r * 0.9f, r * 0.16f, yierEar);
        canvas.drawCircle(cx + r * 0.95f, cy - r * 1.05f, r * 0.16f, yierBow);
        canvas.drawCircle(cx + r * 0.72f, cy - r * 1.12f, r * 0.12f, yierBow);
        canvas.drawCircle(cx, cy, r, yierBody);
        canvas.drawCircle(cx - r * 0.28f, cy - r * 0.22f, r * 0.16f, white);
        canvas.drawCircle(cx - r * 0.22f + ex, cy - r * 0.02f + ey, r * 0.08f, ink);
        canvas.drawCircle(cx + r * 0.22f + ex, cy - r * 0.02f + ey, r * 0.08f, ink);
        canvas.drawCircle(cx - r * 0.38f, cy + r * 0.16f, r * 0.1f, yierBlush);
        canvas.drawCircle(cx + r * 0.38f, cy + r * 0.16f, r * 0.1f, yierBlush);
        fold.setColor(Color.parseColor("#E36A8C"));
        fold.setStrokeWidth(dp(1.4f));
        tmp.set(cx - r * 0.16f, cy + r * 0.18f, cx + r * 0.16f, cy + r * 0.42f);
        canvas.drawArc(tmp, 20f, 140f, false, fold);
        fold.setColor(Color.parseColor("#C9A078"));
    }

    private void drawBubu(Canvas canvas, float cx, float cy, float r, int facing) {
        eyeShift(facing, r);
        canvas.drawCircle(cx - r * 0.5f, cy - r * 0.82f, r * 0.34f, bubuBody);
        canvas.drawCircle(cx + r * 0.5f, cy - r * 0.82f, r * 0.34f, bubuBody);
        canvas.drawCircle(cx - r * 0.5f, cy - r * 0.82f, r * 0.16f, bubuEar);
        canvas.drawCircle(cx + r * 0.5f, cy - r * 0.82f, r * 0.16f, bubuEar);
        canvas.drawCircle(cx, cy, r, bubuBody);
        canvas.drawOval(cx - r * 0.55f, cy - r * 0.05f, cx + r * 0.55f, cy + r * 0.72f, bubuBelly);
        ink.setColor(Color.parseColor("#3A2A22"));
        canvas.drawRoundRect(cx - r * 0.28f + ex, cy - r * 0.08f + ey, cx - r * 0.08f + ex, cy + r * 0.02f + ey, 3f, 3f, ink);
        canvas.drawRoundRect(cx + r * 0.08f + ex, cy - r * 0.08f + ey, cx + r * 0.28f + ex, cy + r * 0.02f + ey, 3f, 3f, ink);
        canvas.drawCircle(cx, cy + r * 0.16f, r * 0.06f, ink);
        fold.setColor(Color.parseColor("#8D5A34"));
        fold.setStrokeWidth(dp(1.4f));
        canvas.drawLine(cx - r * 0.12f, cy + r * 0.32f, cx + r * 0.12f, cy + r * 0.32f, fold);
        fold.setColor(Color.parseColor("#C9A078"));
        ink.setColor(Color.parseColor("#3A2A22"));
    }

    private void drawTrap(Canvas canvas, float cx, float cy) {
        if (masks == null || masks.length == 0) return;
        int frame = (int) ((SystemClock.uptimeMillis() / 500L) % masks.length);
        if (frame < 0) frame = 0;
        Bitmap bitmap = masks[frame];
        if (bitmap == null || bitmap.getHeight() <= 0) return;
        float body = tile * 0.34f;
        float size = tile * 1.25f;
        float bottom = cy + body * 0.9f + size * 0.01f;
        float px = size / 70f;
        float w = bitmap.getWidth() * px;
        float h = bitmap.getHeight() * px;
        tmp.set(cx - w * 0.5f, bottom - h, cx + w * 0.5f, bottom);
        canvas.drawBitmap(bitmap, null, tmp, bmpPaint);
    }

    private void tickBursts(Snap snap) {
        long now = SystemClock.uptimeMillis();
        float dt = burstClock == 0L ? 0f : (now - burstClock) / 1000f;
        burstClock = now;
        if (dt < 0f) dt = 0f;
        if (dt > 0.05f) dt = 0.05f;
        if (snap.phase == Phase.PLAY) {
            for (PlayerView p : snap.players) {
                int id = p.id;
                if (id < 0 || id > 1) continue;
                if (p.stun > 0f) {
                    stunX[id] = p.x;
                    stunY[id] = p.y;
                } else if (stunWas[id] > 0f) {
                    bursts.add(new Burst(stunX[id], stunY[id]));
                }
                stunWas[id] = p.stun;
            }
        } else {
            stunWas[0] = 0f;
            stunWas[1] = 0f;
        }
        for (int i = bursts.size() - 1; i >= 0; i--) {
            bursts.get(i).age += dt;
            if (bursts.get(i).age >= 0.42f) bursts.remove(i);
        }
    }

    private void drawBurst(Canvas canvas, float x, float y, float age) {
        float t = clamp(age / 0.42f, 0f, 1f);
        worldToScreen(x, y);
        float body = tile * 0.34f;
        float size = tile * 1.25f;
        float bottom = sy + body * 0.9f + size * 0.01f;
        float cy = bottom - size * 0.42f;
        float radius = tile * (0.28f + t * 1.25f);
        int alpha = clampInt((int) (255 * (1f - t)), 0, 255);
        blastFill.setColor(Color.parseColor("#FFE7A0"));
        blastFill.setAlpha(clampInt(alpha * 2 / 3, 0, 255));
        canvas.drawCircle(sx, cy, radius * 0.55f, blastFill);
        white.setAlpha(clampInt(alpha * 3 / 4, 0, 255));
        canvas.drawCircle(sx, cy, radius * 0.28f, white);
        fold.setColor(Color.parseColor("#FFF4C96A"));
        fold.setStrokeWidth(dp(2.4f) * (1f - t * 0.65f));
        fold.setAlpha(alpha);
        canvas.drawCircle(sx, cy, radius, fold);
        for (int i = 0; i < 8; i++) {
            double ang = Math.PI * 2.0 * i / 8.0 + 0.4;
            float c = (float) Math.cos(ang);
            float s = (float) Math.sin(ang);
            float inner = radius * 0.35f;
            float outer = radius * (0.85f + (i % 2) * 0.2f);
            canvas.drawLine(sx + c * inner, cy + s * inner, sx + c * outer, cy + s * outer, fold);
        }
        fold.setAlpha(255);
        fold.setColor(Color.parseColor("#C9A078"));
        white.setAlpha(255);
        blastFill.setAlpha(255);
        blastFill.setColor(Color.parseColor("#FFB703"));
    }

    private void drawHud(Canvas canvas, Snap snap) {
        canvas.drawRect(0f, 0f, getWidth(), hudH, hudBg);
        if (!snap.players.isEmpty()) drawSide(canvas, snap.players.get(0), true);
        if (snap.players.size() > 1) drawSide(canvas, snap.players.get(1), false);
        int sec = Math.max(0, (int) Math.ceil(snap.left));
        String clock = String.format("%d:%02d", sec / 60, sec % 60);
        float cx = getWidth() / 2f;
        float timerY = pingMs != Long.MIN_VALUE ? hudH * 0.46f : hudH * 0.64f;
        canvas.drawText(clock, cx, timerY, timerPaint);
        if (pingMs != Long.MIN_VALUE) {
            hintPaint.setColor(pingColor(pingMs));
            canvas.drawText(pingMs + " ms", cx, hudH * 0.86f, hintPaint);
        }
    }

    private int pingColor(long ms) {
        if (ms >= 300L) return pingRed;
        if (ms >= 100L) return pingYellow;
        return pingGreen;
    }

    private void drawSide(Canvas canvas, PlayerView p, boolean left) {
        String name = Look.shown(p.name, p.id) + (p.id == mySlot ? " · 你" : "");
        float x = left ? dp(14f) : getWidth() - dp(14f);
        hudName.setTextAlign(left ? Paint.Align.LEFT : Paint.Align.RIGHT);
        hudName.setColor(p.id == 0 ? Color.parseColor("#C4537A") : Color.parseColor("#8A5324"));
        canvas.drawText(name, x, dp(20f), hudName);
        int have = Math.max(0, p.bombMax - p.bombOut);
        float base = dp(40f);
        float gap = dp(8f);
        hudSub.setTextAlign(Paint.Align.LEFT);
        if (left) {
            x = drawStat(canvas, hudBun, Integer.toString(p.score), x, base, true) + gap;
            x = drawStat(canvas, hudBomb, have + "/" + p.bombMax, x, base, true) + gap;
            x = drawStat(canvas, hudFlame, Integer.toString(p.flame), x, base, true) + gap;
            x = drawStat(canvas, hudShoe, Integer.toString(p.speed + 1), x, base, true) + gap;
            drawStat(canvas, hudFork, Integer.toString(p.forks), x, base, true);
        } else {
            x = drawStat(canvas, hudFork, Integer.toString(p.forks), x, base, false) - gap;
            x = drawStat(canvas, hudShoe, Integer.toString(p.speed + 1), x, base, false) - gap;
            x = drawStat(canvas, hudFlame, Integer.toString(p.flame), x, base, false) - gap;
            x = drawStat(canvas, hudBomb, have + "/" + p.bombMax, x, base, false) - gap;
            drawStat(canvas, hudBun, Integer.toString(p.score), x, base, false);
        }
    }

    private float drawStat(Canvas canvas, Bitmap icon, String value, float edge, float baseline, boolean left) {
        float h = dp(14f);
        float w = h;
        if (icon != null && icon.getHeight() > 0)
            w = h * icon.getWidth() / (float) icon.getHeight();
        float gap = dp(2f);
        float textW = hudSub.measureText(value);
        float iconLeft = left ? edge : edge - textW - gap - w;
        float textLeft = iconLeft + w + gap;
        if (icon != null) {
            float top = baseline + hudSub.ascent();
            float textH = hudSub.descent() - hudSub.ascent();
            float iy = top + (textH - h) * 0.5f;
            tmp.set(iconLeft, iy, iconLeft + w, iy + h);
            canvas.drawBitmap(icon, null, tmp, bmpPaint);
        }
        canvas.drawText(value, textLeft, baseline, hudSub);
        return left ? textLeft + textW : iconLeft;
    }

    private void drawControls(Canvas canvas, Snap snap) {
        float cx = joyOuter.centerX();
        float cy = joyOuter.centerY();
        float r = joyOuter.width() / 2f;
        canvas.drawCircle(cx, cy, r, joyFill);
        joyStroke.setStrokeWidth(dp(2f));
        canvas.drawCircle(cx, cy, r, joyStroke);
        canvas.drawCircle(knobX, knobY, r * 0.38f, joyKnob);
        float bx = bombOuter.centerX();
        float by = bombOuter.centerY();
        float br = bombOuter.width() / 2f;
        bombFill.setColor(bombDown ? Color.parseColor("#E86A8A") : Color.parseColor("#FF8FAB"));
        canvas.drawCircle(bx, by, br, bombFill);
        canvas.drawText(canPry(snap) ? "挣脱" : "糖泡", bx, by + bombText.getTextSize() * 0.32f, bombText);
    }

    private boolean canPry(Snap snap) {
        if (snap == null || mySlot < 0) return false;
        for (PlayerView p : snap.players) {
            if (p.id == mySlot) return p.stun > 0f && p.forks > 0;
        }
        return false;
    }

    private void drawOverlay(Canvas canvas, Snap snap) {
        if (notice != null && !notice.isEmpty()) {
            showRetry = canRetry && onRetry != null;
            drawCard(canvas, showRetry ? notice : notice + "\n按返回键离开", false);
            if (showRetry) drawChoices(canvas, false);
            return;
        }
        if (snap.phase == Phase.WAIT) {
            if (roomQr != null) drawQrCard(canvas, snap.banner);
            else if (snap.banner != null && !snap.banner.isEmpty())
                drawCard(canvas, snap.banner, false);
        } else if (snap.phase == Phase.COUNT) {
            int n = Math.max(1, (int) Math.ceil(snap.countdown));
            drawCard(canvas, Integer.toString(n), true);
        } else if (snap.phase == Phase.OVER) {
            final boolean ended = snap.winner >= 0;
            if (ended) drawResult(canvas, snap);
            else drawCard(canvas, resultLine(snap), false);
            showAgain = ended && onAgain != null;
            if (showAgain) drawChoices(canvas, true);
        } else if (snap.phase == Phase.PLAY) {
            drawAway(canvas, snap);
        }
    }

    private void drawAway(Canvas canvas, Snap snap) {
        if (mySlot < 0 || mySlot >= snap.players.size()) return;
        float away = snap.players.get(mySlot).away;
        if (away <= 0f) return;
        int color = mapShadow.getColor();
        mapShadow.setColor(Color.argb(176, 74, 69, 63));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), mapShadow);
        mapShadow.setColor(color);
        int n = Math.max(1, (int) Math.ceil(away));
        drawCard(canvas, Integer.toString(n), true);
    }

    private String resultLine(Snap snap) {
        if (snap.winner < 0 && snap.banner != null && !snap.banner.isEmpty()) {
            return snap.banner + "\n按返回键离开";
        }
        int a = scoreOf(snap, 0);
        int b = scoreOf(snap, 1);
        String leftName = nameOf(snap, 0);
        String rightName = nameOf(snap, 1);
        String title;
        if (snap.winner == 0) title = leftName + "赢了";
        else if (snap.winner == 1) title = rightName + "赢了";
        else if (snap.winner == 2) title = "平局";
        else if (snap.banner != null && !snap.banner.isEmpty()) title = snap.banner;
        else title = "对局结束";
        return title + "\n" + leftName + " " + a + " 个  ·  " + rightName + " " + b + " 个";
    }

    private static int scoreOf(Snap snap, int id) {
        for (PlayerView p : snap.players) {
            if (p.id == id) return p.score;
        }
        return 0;
    }

    private static String nameOf(Snap snap, int id) {
        for (PlayerView p : snap.players) {
            if (p.id == id) return Look.shown(p.name, id);
        }
        return Look.roleName(id);
    }

    /**
     * 分出胜负后的结算。你赢、你输、平局各用一套底色和角色大小。
     */
    private void drawResult(final Canvas canvas, final Snap snap) {
        final int tone = resultTone(snap);
        final int savedShadow = mapShadow.getColor();
        mapShadow.setColor(resultScrim(tone));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), mapShadow);
        mapShadow.setColor(savedShadow);

        final float padX = dp(22f);
        final float padY = dp(12f);
        final float ribbonH = dp(34f);
        final float barH = dp(8f);
        final float titleBlock = ribbonH + dp(8f) + resultFlavor.getFontSpacing() + dp(8f);
        final float belowFace = dp(2f) + resultName.getFontSpacing() + resultScore.getFontSpacing() + dp(8f) + barH;
        final float buttonReserve = dp(46f) + dp(14f) + dp(16f);
        float faceSlot = dp(104f);
        float boxH = padY + titleBlock + faceSlot + belowFace + padY;
        final float room = getHeight() - hudH - dp(6f) - buttonReserve;
        if (boxH > room) {
            faceSlot = Math.max(dp(68f), faceSlot - (boxH - room));
            boxH = padY + titleBlock + faceSlot + belowFace + padY;
        }
        final float boxW = Math.min(getWidth() - dp(24f), dp(540f));
        final float maxTop = getHeight() - buttonReserve - boxH;
        final float minTop = hudH + dp(4f);
        final float top = maxTop < minTop ? Math.max(0f, maxTop) : minTop + (maxTop - minTop) * 0.5f;
        final float leftEdge = (getWidth() - boxW) / 2f;
        final float radius = dp(tone == TONE_LOSE ? 12f : tone == TONE_WIN ? 22f : 18f);

        final int savedFill = cardFill.getColor();
        final int savedStroke = cardStroke.getColor();
        final float savedWidth = cardStroke.getStrokeWidth();
        cardFill.setColor(resultFill(tone));
        cardStroke.setColor(resultEdge(tone));
        cardStroke.setStrokeWidth(dp(tone == TONE_LOSE ? 2f : 3f));
        tmp.set(leftEdge, top, leftEdge + boxW, top + boxH);
        cardBottom = top + boxH;
        canvas.drawRoundRect(tmp, radius, radius, cardFill);
        canvas.drawRoundRect(tmp, radius, radius, cardStroke);

        if (tone == TONE_LOSE) {
            canvas.save();
            canvas.clipRect(leftEdge, top, leftEdge + boxW, top + boxH);
            drawDrizzle(canvas, leftEdge, top, boxW, boxH);
            canvas.restore();
        }

        final String head = resultHead(snap, tone);
        final float ribbonW = Math.min(boxW - dp(40f), resultTitle.measureText(head) + dp(40f));
        final float ribbonLeft = (getWidth() - ribbonW) / 2f;
        final float ribbonTop = top + padY;
        ribbonFill.setColor(resultRibbon(tone));
        tmp.set(ribbonLeft, ribbonTop, ribbonLeft + ribbonW, ribbonTop + ribbonH);
        canvas.drawRoundRect(tmp, ribbonH / 2f, ribbonH / 2f, ribbonFill);
        final float titleBase = ribbonTop + ribbonH / 2f - (resultTitle.ascent() + resultTitle.descent()) / 2f;
        canvas.drawText(head, getWidth() / 2f, titleBase, resultTitle);

        resultFlavor.setColor(tone == TONE_LOSE ? Color.parseColor("#6D7E92") : Color.parseColor("#8A5A40"));
        final float flavorBase = ribbonTop + ribbonH + dp(8f) - resultFlavor.ascent();
        canvas.drawText(resultFlavor(snap, tone), getWidth() / 2f, flavorBase, resultFlavor);

        final float slotTop = flavorBase + resultFlavor.descent() + dp(8f);
        final float nameBase = slotTop + faceSlot + dp(2f) - resultName.ascent();
        final float scoreBase = nameBase + resultName.getFontSpacing();
        final float spread = Math.min(boxW * 0.28f, dp(150f));
        final PlayerView left = resultPlayer(snap, true);
        final PlayerView right = resultPlayer(snap, false);
        drawResultSide(canvas, snap, left, getWidth() / 2f - spread, slotTop, faceSlot, nameBase, scoreBase, tone);
        drawResultSide(canvas, snap, right, getWidth() / 2f + spread, slotTop, faceSlot, nameBase, scoreBase, tone);
        resultName.setFakeBoldText(false);

        final float barTop = scoreBase + resultScore.descent() + dp(8f);
        drawResultBar(canvas, leftEdge + padX, barTop, boxW - padX * 2f, barH,
                left == null ? 0 : left.score, right == null ? 0 : right.score, left, right);

        cardFill.setColor(savedFill);
        cardStroke.setColor(savedStroke);
        cardStroke.setStrokeWidth(savedWidth);
    }

    private void drawResultSide(final Canvas canvas, final Snap snap, final PlayerView p,
                                final float cx, final float slotTop, final float slot,
                                final float nameBase, final float scoreBase, final int tone) {
        if (p == null) return;
        final boolean crowned = snap.winner == p.id;
        final boolean dull = tone != TONE_DRAW && !crowned;
        final boolean bobbing = crowned && tone == TONE_WIN;
        final float size = resultFaceSize(crowned, tone) * Math.min(1f, slot / dp(104f));
        final float slack = Math.max(0f, slot - size);
        final float bob = bobbing ? (float) Math.sin(SystemClock.uptimeMillis() / 280.0) * dp(3.5f) : 0f;
        final float faceTop = slotTop + (crowned || tone == TONE_DRAW ? slack * 0.12f : slack * 0.62f) + bob;
        final float cy = faceTop + size * 0.55f;
        canvas.drawOval(cx - size * 0.26f, cy + size * 0.28f, cx + size * 0.26f, cy + size * 0.40f, shadow);
        if (bobbing) drawSparkles(canvas, cx, faceTop + size * 0.45f, size * 0.62f);
        final Bitmap face = resultFace(p);
        final Paint paint = dull ? dullPaint : bmpPaint;
        final int alpha = paint.getAlpha();
        paint.setAlpha(dull ? 210 : 255);
        if (face != null) {
            tmp.set(cx - size * 0.5f, faceTop, cx + size * 0.5f, faceTop + size);
            canvas.drawBitmap(face, null, tmp, paint);
        }
        paint.setAlpha(alpha);
        if (tone != TONE_DRAW)
            drawBadge(canvas, crowned, cx + size * 0.30f, faceTop + size * 0.16f);
        final String shown = Look.shown(p.name, p.id);
        final String name = p.id == mySlot ? shown + " · 你" : shown;
        resultName.setFakeBoldText(crowned && tone != TONE_DRAW);
        resultName.setColor(dull
                ? Color.parseColor("#8E8880")
                : (p.id == 0 ? Color.parseColor("#C4537A") : Color.parseColor("#8A5324")));
        canvas.drawText(name, cx, nameBase, resultName);
        if (tone == TONE_DRAW) resultScore.setColor(Color.parseColor("#6B3F2A"));
        else if (crowned) resultScore.setColor(Color.parseColor("#C4532A"));
        else resultScore.setColor(Color.parseColor("#8A7A70"));
        drawBunCount(canvas, cx, scoreBase, p.score, resultScore);
    }

    private void drawBadge(final Canvas canvas, final boolean crowned, final float x, final float y) {
        final float radius = dp(11f);
        badgeFill.setColor(crowned ? Color.parseColor("#E07A2F") : Color.parseColor("#8A97A8"));
        canvas.drawCircle(x, y, radius, badgeFill);
        final String label = crowned ? "胜" : "负";
        canvas.drawText(label, x, y - (resultBadge.ascent() + resultBadge.descent()) / 2f, resultBadge);
    }

    private void drawBunCount(final Canvas canvas, final float cx, final float baseline, final int score, final Paint paint) {
        final String text = score + " 个";
        final float textW = paint.measureText(text);
        final float icon = dp(15f);
        final float gap = dp(3f);
        final float lead = hudBun == null ? 0f : icon + gap;
        final float start = cx - (lead + textW) * 0.5f;
        if (hudBun != null) {
            final float textTop = baseline + paint.ascent();
            final float textH = paint.descent() - paint.ascent();
            final float iy = textTop + (textH - icon) * 0.5f;
            tmp.set(start, iy, start + icon, iy + icon);
            canvas.drawBitmap(hudBun, null, tmp, bmpPaint);
        }
        canvas.drawText(text, start + lead, baseline, paint);
    }

    private void drawResultBar(final Canvas canvas, final float left, final float top, final float width, final float height,
                               final int leftScore, final int rightScore,
                               final PlayerView leftPlayer, final PlayerView rightPlayer) {
        if (width <= 0f || height <= 0f) return;
        final float radius = height / 2f;
        tmp.set(left, top, left + width, top + height);
        barClip.rewind();
        barClip.addRoundRect(tmp, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(barClip);
        canvas.drawRect(tmp, barTrack);
        final int sum = leftScore + rightScore;
        final float share = sum <= 0 ? 0.5f : Math.max(0.06f, Math.min(0.94f, leftScore / (float) sum));
        final float split = left + width * share;
        tmp.set(left, top, split, top + height);
        barFill.setColor(sideColor(leftPlayer));
        canvas.drawRect(tmp, barFill);
        tmp.set(split, top, left + width, top + height);
        barFill.setColor(sideColor(rightPlayer));
        canvas.drawRect(tmp, barFill);
        canvas.restore();
    }

    private void drawSparkles(final Canvas canvas, final float cx, final float cy, final float reach) {
        final long time = SystemClock.uptimeMillis();
        for (int i = 0; i < 8; i++) {
            final double ang = time / 700.0 + i * (Math.PI * 2.0 / 8.0);
            final float rad = reach * (0.85f + (i % 3) * 0.08f);
            final float x = cx + (float) Math.cos(ang) * rad;
            final float y = cy + (float) Math.sin(ang) * rad * 0.62f;
            sparkFill.setColor(i % 2 == 0 ? Color.parseColor("#FFC857") : Color.parseColor("#FF8FAB"));
            sparkFill.setAlpha(170);
            canvas.drawCircle(x, y, dp(i % 2 == 0 ? 3.4f : 2.2f), sparkFill);
        }
        sparkFill.setAlpha(255);
    }

    private void drawDrizzle(final Canvas canvas, final float left, final float top, final float width, final float height) {
        final long time = SystemClock.uptimeMillis();
        for (int i = 0; i < 8; i++) {
            final float x = left + ((i * 0.17f + 0.08f) % 1f) * width;
            final float y = top + ((time / 1400f + i * 0.13f) % 1f) * height;
            sparkFill.setColor(Color.parseColor("#A9BDD0"));
            sparkFill.setAlpha(70 + (i % 3) * 25);
            canvas.drawCircle(x, y, dp(i % 2 == 0 ? 2.2f : 1.5f), sparkFill);
        }
        sparkFill.setAlpha(255);
    }

    private int resultTone(final Snap snap) {
        if (snap.winner == 2) return TONE_DRAW;
        if ((mySlot == 0 || mySlot == 1) && snap.winner != mySlot) return TONE_LOSE;
        return TONE_WIN;
    }

    private String resultHead(final Snap snap, final int tone) {
        if (tone == TONE_DRAW) return "平局";
        if (mySlot == 0 || mySlot == 1) return tone == TONE_WIN ? "你赢了" : "你输了";
        return nameOf(snap, snap.winner) + "赢了";
    }

    private String resultFlavor(final Snap snap, final int tone) {
        final int gap = Math.abs(scoreOf(snap, 0) - scoreOf(snap, 1));
        if (tone == TONE_DRAW) return "包子一样多";
        if (tone == TONE_LOSE) return "少了 " + gap + " 个包子";
        if (mySlot == 0 || mySlot == 1) return "多了 " + gap + " 个包子";
        return nameOf(snap, snap.winner) + "多了 " + gap + " 个";
    }

    private PlayerView resultPlayer(final Snap snap, final boolean leftSide) {
        final int id = (mySlot == 0 || mySlot == 1) ? (leftSide ? mySlot : 1 - mySlot) : (leftSide ? 0 : 1);
        for (final PlayerView p : snap.players) {
            if (p.id == id) return p;
        }
        return null;
    }

    private Bitmap resultFace(final PlayerView p) {
        final int skin = Look.clampSkin(p.skin, Look.roleSkin(p.id));
        if (skin >= 0 && skin < heroes.length) {
            final HeroSheet hero = heroes[skin];
            if (hero != null && hero.frames != null && hero.frames.length > 0 && hero.frames[0].length > 0) {
                return hero.frames[0][0];
            }
        }
        return p.id == 0 ? yierBmp : bubuBmp;
    }

    private float resultFaceSize(final boolean crowned, final int tone) {
        if (tone == TONE_DRAW) return dp(78f);
        if (tone == TONE_WIN) return dp(crowned ? 96f : 64f);
        return dp(crowned ? 76f : 58f);
    }

    private static int sideColor(final PlayerView p) {
        if (p == null) return Color.parseColor("#C4B4A4");
        return p.id == 0 ? Color.parseColor("#E07A9A") : Color.parseColor("#D0924A");
    }

    private static int resultScrim(final int tone) {
        if (tone == TONE_LOSE) return Color.argb(176, 28, 40, 58);
        if (tone == TONE_DRAW) return Color.argb(150, 70, 52, 36);
        return Color.argb(168, 92, 58, 24);
    }

    private static int resultFill(final int tone) {
        if (tone == TONE_LOSE) return Color.parseColor("#F4EEF2F8");
        if (tone == TONE_DRAW) return Color.parseColor("#F2FFF8EE");
        return Color.parseColor("#F7FFF4D8");
    }

    private static int resultEdge(final int tone) {
        if (tone == TONE_LOSE) return Color.parseColor("#8FA4BA");
        if (tone == TONE_DRAW) return Color.parseColor("#E7B56A");
        return Color.parseColor("#E2A322");
    }

    private static int resultRibbon(final int tone) {
        if (tone == TONE_LOSE) return Color.parseColor("#6E86A4");
        if (tone == TONE_DRAW) return Color.parseColor("#C47A4A");
        return Color.parseColor("#E07A2F");
    }

    private void drawChoices(Canvas canvas, boolean again) {
        float h = dp(46f);
        float w = dp(132f);
        float gap = dp(14f);
        float y = Math.min(cardBottom + dp(14f), getHeight() - dp(16f) - h);
        float cx = getWidth() / 2f;
        leaveBtn.set(cx + gap / 2f, y, cx + gap / 2f + w, y + h);
        if (again) {
            againBtn.set(cx - gap / 2f - w, y, cx - gap / 2f, y + h);
            drawChoice(canvas, againBtn, "再来一局", true);
        } else {
            retryBtn.set(cx - gap / 2f - w, y, cx - gap / 2f, y + h);
            drawChoice(canvas, retryBtn, "重试", true);
        }
        drawChoice(canvas, leaveBtn, "离开", false);
    }

    private void drawChoice(Canvas canvas, RectF rect, String label, boolean primary) {
        canvas.drawRoundRect(rect, dp(14f), dp(14f), primary ? btnFill : white);
        float stroke = cardStroke.getStrokeWidth();
        cardStroke.setStrokeWidth(dp(2f));
        canvas.drawRoundRect(rect, dp(14f), dp(14f), cardStroke);
        cardStroke.setStrokeWidth(stroke);
        canvas.drawText(label, rect.centerX(), rect.centerY() + (primary ? bombText : cardBody).getTextSize() * 0.35f, primary ? bombText : cardBody);
    }

    private void drawQrCard(Canvas canvas, String title) {
        String head = title == null || title.isEmpty() ? "等待扫码加入" : title;
        String foot = roomLabel == null ? "" : roomLabel;
        float lineH = cardBody.getFontSpacing();
        float qr = Math.min(dp(220f), Math.min(getWidth() * 0.34f, getHeight() * 0.42f));
        float padX = dp(28f);
        float padY = dp(16f);
        float textW = Math.max(cardBody.measureText(head), cardBody.measureText(foot));
        float boxW = Math.max(qr + padX * 2f, textW + padX * 2f);
        float boxH = padY * 2f + lineH * 2f + qr + dp(20f);
        float left = (getWidth() - boxW) / 2f;
        float top = Math.max(hudH + dp(6f), (getHeight() - boxH) / 2f);
        tmp.set(left, top, left + boxW, top + boxH);
        canvas.drawRoundRect(tmp, dp(18f), dp(18f), cardFill);
        cardStroke.setStrokeWidth(dp(2f));
        canvas.drawRoundRect(tmp, dp(18f), dp(18f), cardStroke);
        float baseline = top + padY - cardBody.ascent();
        canvas.drawText(head, getWidth() / 2f, baseline, cardBody);
        float qrLeft = (getWidth() - qr) / 2f;
        float qrTop = baseline + cardBody.descent() + dp(10f);
        tmp.set(qrLeft - dp(8f), qrTop - dp(8f), qrLeft + qr + dp(8f), qrTop + qr + dp(8f));
        canvas.drawRoundRect(tmp, dp(8f), dp(8f), white);
        tmp.set(qrLeft, qrTop, qrLeft + qr, qrTop + qr);
        canvas.drawBitmap(roomQr, null, tmp, qrPaint);
        baseline = qrTop + qr + dp(12f) - cardBody.ascent();
        canvas.drawText(foot, getWidth() / 2f, baseline, cardBody);
    }

    private float cardBottom;

    private void drawCard(Canvas canvas, String text, boolean big) {
        String[] lines = text.split("\n", -1);
        Paint paint = big ? cardBig : cardBody;
        float maxW = 0f;
        for (String line : lines) maxW = Math.max(maxW, paint.measureText(line));
        float lineH = paint.getFontSpacing();
        float padX = dp(big ? 36f : 26f);
        float padY = dp(18f);
        float boxW = maxW + padX * 2;
        float boxH = lineH * lines.length + padY * 2;
        float left = (getWidth() - boxW) / 2f;
        float top = (getHeight() - boxH) / 2f;
        tmp.set(left, top, left + boxW, top + boxH);
        cardBottom = top + boxH;
        canvas.drawRoundRect(tmp, dp(18f), dp(18f), cardFill);
        cardStroke.setStrokeWidth(dp(2f));
        canvas.drawRoundRect(tmp, dp(18f), dp(18f), cardStroke);
        float baseline = top + padY - paint.ascent();
        for (String line : lines) {
            canvas.drawText(line, getWidth() / 2f, baseline, paint);
            baseline += lineH;
        }
    }

    private void maybeVibrate(Snap snap) {
        if (mySlot < 0 || mySlot >= snap.players.size()) return;
        float stun = snap.players.get(mySlot).stun;
        if (lastStun <= 0f && stun > 0f) buzz();
        lastStun = stun;
    }

    @SuppressWarnings("deprecation")
    private void buzz() {
        Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vib == null || !vib.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26) {
            vib.vibrate(VibrationEffect.createOneShot(24, 80));
        } else {
            vib.vibrate(24);
        }
    }

    private void eyeShift(int facing, float r) {
        float d = r * 0.08f;
        if (facing == 1) {
            ex = d;
            ey = 0f;
        } else if (facing == 3) {
            ex = -d;
            ey = 0f;
        } else if (facing == 0) {
            ex = 0f;
            ey = -d * 0.6f;
        } else {
            ex = 0f;
            ey = d * 0.5f;
        }
    }

    private void cellRect(int x, int y) {
        float left = (int) (originX + x * tile);
        float top = (int) (originY + y * tile);
        float right = (int) (originX + (x + 1) * tile);
        float bottom = (int) (originY + (y + 1) * tile);
        cellR.set(left, top, right, bottom);
    }

    private void worldToScreen(float x, float y) {
        sx = originX + x * tile;
        sy = originY + y * tile;
    }

    private int wallSkinAt(String walls, int x, int y, char from, char to) {
        int index = y * Rules.COLS + x;
        if (walls == null || index < 0 || index >= walls.length()) return -1;
        char skin = walls.charAt(index);
        if (skin < from || skin > to) return -1;
        return skin - from;
    }

    private char tileAt(String tiles, int x, int y) {
        int index = y * Rules.COLS + x;
        if (tiles == null || index < 0 || index >= tiles.length()) return '#';
        return tiles.charAt(index);
    }

    private Bitmap still(int resId) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        return BitmapFactory.decodeResource(getResources(), resId, opts);
    }

    private Bitmap openBmp(String name) {
        InputStream in = null;
        try {
            in = getContext().getAssets().open(name);
            return BitmapFactory.decodeStream(in);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private Paint fill(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        return paint;
    }

    private Paint stroke(int color, float widthDp) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(widthDp * getResources().getDisplayMetrics().density);
        return paint;
    }

    private Paint text(float sizeSp, int color, Paint.Align align) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextAlign(align);
        paint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, sizeSp, getResources().getDisplayMetrics()));
        return paint;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static final class HeroSheet {
        private Bitmap[][] frames;
    }

    /**
     * 走路帧记在人身上。两个人可以选同一套皮肤。
     */
    private static final class HeroWalk {
        private int row;
        private float lastX;
        private float lastY;
        private boolean seen;
    }

    private static final class Burst {
        final float x;
        final float y;
        float age;

        Burst(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
