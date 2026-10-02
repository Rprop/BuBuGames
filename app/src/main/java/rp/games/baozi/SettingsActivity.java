package rp.games.baozi;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import rp.games.R;
import rp.games.baozi.game.Look;

public class SettingsActivity extends Activity {
    private EditText nameInput;
    private final FrameLayout[] cells = new FrameLayout[Look.SKINS];
    private int picked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        nameInput = findViewById(R.id.nameInput);
        nameInput.setText(Profile.name(this));
        picked = Profile.savedSkin(this);
        if (picked < 0) picked = Look.roleSkin(0);
        fillSkins();
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void fillSkins() {
        GridLayout grid = findViewById(R.id.skinGrid);
        int gap = dp(6);
        for (int i = 0; i < Look.SKINS; i++) {
            FrameLayout cell = new FrameLayout(this);
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int pad = dp(8);
            image.setPadding(pad, pad, pad, pad);
            Bitmap face = faceOf(Profile.SKIN_RES[i]);
            if (face != null) image.setImageBitmap(face);
            cell.addView(image, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER));
            int index = i;
            cell.setOnClickListener(v -> {
                picked = index;
                paintCells();
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 4),
                    GridLayout.spec(i % 4, GridLayout.FILL, 1f));
            lp.width = 0;
            lp.height = dp(96);
            lp.setMargins(gap, gap, gap, gap);
            grid.addView(cell, lp);
            cells[i] = cell;
        }
        paintCells();
    }

    private void paintCells() {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == null) continue;
            cells[i].setBackgroundResource(i == picked ? R.drawable.skin_on : R.drawable.skin_off);
        }
    }

    private void save() {
        String raw = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (raw.length() > Look.NAME_LIMIT) {
            Toast.makeText(this, R.string.settings_name_bad, Toast.LENGTH_SHORT).show();
            return;
        }
        Profile.save(this, raw, picked);
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
    }

    private Bitmap faceOf(int resId) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), resId, bounds);
        int edge = Math.max(bounds.outWidth / 4, bounds.outHeight / 4);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample(edge, dp(72));
        Bitmap sheet = BitmapFactory.decodeResource(getResources(), resId, opts);
        if (sheet == null) return null;
        int fw = sheet.getWidth() / 4;
        int fh = sheet.getHeight() / 4;
        if (fw <= 0 || fh <= 0) return sheet;
        return Bitmap.createBitmap(sheet, 0, 0, fw, fh);
    }

    private static int sample(int edge, int target) {
        int sample = 1;
        while (edge / (sample * 2) >= target && edge / (sample * 2) > 0) sample *= 2;
        return sample;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
