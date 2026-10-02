package rp.games.baozi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.ArrayList;
import java.util.List;

import rp.games.R;

/**
 * 和帮助页同一套浅色页面，中间嵌一块方形取景。
 */
public class ScanActivity extends CaptureActivity {
    private static final String PREF = "baozi_join";
    private static final String KEY_HOSTS = "hosts";
    private static final int HOST_HISTORY = 8;

    @Override
    protected DecoratedBarcodeView initializeContent() {
        setContentView(R.layout.activity_scan);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnJoin).setOnClickListener(v -> submitHost());
        AutoCompleteTextView input = findViewById(R.id.hostInput);
        List<String> history = loadHosts();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, history) {
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        results.values = history;
                        results.count = history.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        notifyDataSetChanged();
                    }
                };
            }
        };
        input.setAdapter(adapter);
        if (!history.isEmpty()) input.setText(history.get(0));
        input.setOnClickListener(v -> input.showDropDown());
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) input.showDropDown();
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitHost();
                return true;
            }
            return false;
        });
        fitFrame();
        DecoratedBarcodeView barcode = findViewById(R.id.zxing_barcode_scanner);
        float radius = 12f * getResources().getDisplayMetrics().density;
        barcode.setClipToOutline(true);
        barcode.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        return barcode;
    }

    private void submitHost() {
        AutoCompleteTextView input = findViewById(R.id.hostInput);
        String text = input.getText().toString().trim();
        if (RoomCode.parse(text) == null) {
            Toast.makeText(this, "请输入 host:port，地址可以是 IP 或域名", Toast.LENGTH_SHORT).show();
            return;
        }
        rememberHost(text);
        Intent data = new Intent();
        data.putExtra("SCAN_RESULT", text);
        setResult(RESULT_OK, data);
        finish();
    }

    private List<String> loadHosts() {
        String raw = prefs().getString(KEY_HOSTS, "");
        List<String> hosts = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return hosts;
        for (String line : raw.split("\n")) {
            String host = line.trim();
            if (!host.isEmpty() && !hosts.contains(host)) hosts.add(host);
        }
        return hosts;
    }

    private void rememberHost(String host) {
        List<String> hosts = loadHosts();
        hosts.remove(host);
        hosts.add(0, host);
        while (hosts.size() > HOST_HISTORY) hosts.remove(hosts.size() - 1);
        StringBuilder raw = new StringBuilder();
        for (int i = 0; i < hosts.size(); i++) {
            if (i > 0) raw.append('\n');
            raw.append(hosts.get(i));
        }
        prefs().edit().putString(KEY_HOSTS, raw.toString()).apply();
    }

    private SharedPreferences prefs() {
        return getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /**
     * 方框跟着中间空白变大，同时不超过一掌宽。
     */
    private void fitFrame() {
        View slot = findViewById(R.id.scanSlot);
        FrameLayout frame = findViewById(R.id.scanFrame);
        float density = getResources().getDisplayMetrics().density;
        slot.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int space = Math.min(right - left, bottom - top);
            int size = Math.min(space - Math.round(48 * density), Math.round(320 * density));
            if (size < Math.round(160 * density)) size = Math.round(160 * density);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) frame.getLayoutParams();
            if (lp.width == size && lp.height == size) return;
            lp.width = size;
            lp.height = size;
            frame.setLayoutParams(lp);
        });
    }
}
