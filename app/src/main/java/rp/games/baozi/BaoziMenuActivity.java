package rp.games.baozi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import rp.games.R;

public class BaoziMenuActivity extends Activity {
    private static final int CAMERA_REQUEST = 41;
    private static final int[] MINUTES = {3, 5, 8, 10};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baozi);
        findViewById(R.id.btnHost).setOnClickListener(v -> chooseTime("host"));
        findViewById(R.id.btnJoin).setOnClickListener(v -> joinByScan());
        findViewById(R.id.btnLocal).setOnClickListener(v -> chooseTime("local"));
        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnForward).setOnClickListener(v ->
                startActivity(new Intent(this, ForwardActivity.class)));
        findViewById(R.id.btnHelp).setOnClickListener(v ->
                startActivity(new Intent(this, HelpActivity.class)));
    }

    private void joinByScan() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchScan();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        }
    }

    private void launchScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("对准房间二维码");
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.setCaptureActivity(ScanActivity.class);
        integrator.initiateScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CAMERA_REQUEST) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchScan();
        } else {
            Toast.makeText(this, "需要相机才能扫码加入", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (result.getContents() == null) return;
        RoomCode.Endpoint end = RoomCode.parse(result.getContents());
        if (end == null) {
            Toast.makeText(this, "这不是房间地址", Toast.LENGTH_SHORT).show();
            return;
        }
        startGame("join", end.host, end.port, 0);
    }

    private void chooseTime(final String mode) {
        final String[] labels = new String[MINUTES.length];
        for (int i = 0; i < MINUTES.length; i++) labels[i] = MINUTES[i] + " 分钟";
        new AlertDialog.Builder(this)
                .setTitle("一局多久")
                .setItems(labels, (dialog, which) -> startGame(mode, null, 0, MINUTES[which] * 60))
                .show();
    }

    private void startGame(final String mode, final String host, final int port, final int seconds) {
        final Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_MODE, mode);
        if (host != null) intent.putExtra(GameActivity.EXTRA_HOST, host);
        if (port > 0) intent.putExtra(GameActivity.EXTRA_PORT, port);
        if (seconds > 0) intent.putExtra(GameActivity.EXTRA_SECONDS, seconds);
        startActivity(intent);
    }
}
