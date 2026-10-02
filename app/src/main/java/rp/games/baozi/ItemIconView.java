package rp.games.baozi;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import rp.games.baozi.game.ItemKind;

/** 帮助页里的道具小图。android:tag 填 BUN、SPEED、BOMB、RANGE 或 FORK。 */
public class ItemIconView extends View {
    private final ItemGlyph glyph = new ItemGlyph();
    private ItemKind kind = ItemKind.BUN;

    public ItemIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        glyph.bind(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Object tag = getTag();
        if (tag != null) kind = ItemKind.valueOf(String.valueOf(tag));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = Math.min(getWidth(), getHeight()) * 0.38f;
        glyph.draw(canvas, kind, getWidth() / 2f, getHeight() / 2f, r);
        if (kind == ItemKind.BUN || kind == ItemKind.SPEED || kind == ItemKind.BOMB || kind == ItemKind.RANGE) {
            postInvalidateOnAnimation();
        }
    }
}
