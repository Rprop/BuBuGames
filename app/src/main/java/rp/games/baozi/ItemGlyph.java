package rp.games.baozi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;

import rp.games.R;
import rp.games.baozi.game.ItemKind;

/**
 * 场上和帮助页共用的道具画法。
 */
public final class ItemGlyph {
    private final Paint bmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF box = new RectF();
    private Bitmap[] baozi;
    private Bitmap[] shoes;
    private Bitmap[] bombs;
    private Bitmap[] potions;
    private Bitmap fork;

    public void bind(Context context) {
        if (context == null) return;
        if (baozi == null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            baozi = new Bitmap[]{
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.baozi_0, opts),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.baozi_1, opts),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.baozi_2, opts),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.baozi_3, opts)
            };
        }
        if (shoes == null) {
            shoes = new Bitmap[]{
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.shoe_0),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.shoe_1),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.shoe_2)
            };
        }
        if (bombs == null) {
            bombs = new Bitmap[]{
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.bomb_0),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.bomb_1),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.bomb_2)
            };
        }
        if (potions == null) {
            potions = new Bitmap[]{
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.potion_0),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.potion_1),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.potion_2)
            };
        }
        if (fork == null) {
            fork = BitmapFactory.decodeResource(context.getResources(), R.drawable.fork);
        }
    }

    public void draw(Canvas canvas, ItemKind kind, float cx, float cy, float r) {
        if (kind == ItemKind.BUN) drawBaozi(canvas, cx, cy, r);
        else if (kind == ItemKind.SPEED) drawShoe(canvas, cx, cy, r);
        else if (kind == ItemKind.BOMB) drawFrames(canvas, bombs, cx, cy, r);
        else if (kind == ItemKind.FORK) drawSprite(canvas, fork, cx, cy, r);
        else drawFrames(canvas, potions, cx, cy, r);
    }

    private void drawBaozi(Canvas canvas, float cx, float cy, float r) {
        if (baozi == null || baozi.length == 0) return;
        int frame = (int) ((SystemClock.uptimeMillis() / 300L) % baozi.length);
        if (frame < 0) frame = 0;
        Bitmap bitmap = baozi[frame];
        if (bitmap == null) return;
        float px = r * 1.5f / 37f;
        float w = bitmap.getWidth() * px;
        float h = bitmap.getHeight() * px;
        box.set(cx - w * 0.5f, cy - h * 0.5f, cx + w * 0.5f, cy + h * 0.5f);
        canvas.drawBitmap(bitmap, null, box, bmpPaint);
    }

    private void drawShoe(Canvas canvas, float cx, float cy, float r) {
        drawFrames(canvas, shoes, cx, cy, r);
    }

    private void drawFrames(Canvas canvas, Bitmap[] frames, float cx, float cy, float r) {
        if (frames == null || frames.length == 0) return;
        int frame = (int) ((SystemClock.uptimeMillis() / 300L) % frames.length);
        drawSprite(canvas, frames[frame], cx, cy, r);
    }

    private void drawSprite(Canvas canvas, Bitmap bitmap, float cx, float cy, float r) {
        if (bitmap == null) return;
        float h = r * 1.9f;
        float w = h * bitmap.getWidth() / (float) bitmap.getHeight();
        box.set(cx - w * 0.5f, cy - h * 0.5f, cx + w * 0.5f, cy + h * 0.5f);
        canvas.drawBitmap(bitmap, null, box, bmpPaint);
    }
}
