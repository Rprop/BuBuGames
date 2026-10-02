package rp.games.baozi;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import rp.games.R;

public class ForwardActivity extends Activity {
    private EditText serverInput;
    private EditText userInput;
    private EditText passInput;
    private EditText portInput;
    private TextView statusText;
    private TextView btnToggle;
    private final Tunnel.Listener listener = () -> runOnUiThread(this::render);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forward);
        serverInput = findViewById(R.id.serverInput);
        userInput = findViewById(R.id.userInput);
        passInput = findViewById(R.id.passInput);
        portInput = findViewById(R.id.portInput);
        statusText = findViewById(R.id.statusText);
        btnToggle = findViewById(R.id.btnToggle);
        serverInput.setText(Tunnel.server(this));
        userInput.setText(Tunnel.user(this));
        passInput.setText(Tunnel.pass(this));
        int savedPort = Tunnel.port(this);
        if (savedPort > 0) portInput.setText(String.valueOf(savedPort));
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            if (readForm() != null) Toast.makeText(this, R.string.forward_saved, Toast.LENGTH_SHORT).show();
        });
        btnToggle.setOnClickListener(v -> {
            if (Tunnel.isOn()) {
                Tunnel.stop();
                return;
            }
            if (!Tunnel.arm64()) {
                Toast.makeText(this, R.string.forward_abi, Toast.LENGTH_SHORT).show();
                return;
            }
            Form form = readForm();
            if (form == null) return;
            Tunnel.start(this, form.server, form.user, form.pass, form.port);
        });
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        Tunnel.setListener(listener);
        render();
    }

    @Override
    protected void onDestroy() {
        Tunnel.setListener(null);
        super.onDestroy();
    }

    private void render() {
        if (isFinishing()) return;
        boolean on = Tunnel.isOn();
        btnToggle.setText(on ? R.string.forward_stop : R.string.forward_start);
        statusText.setText(Tunnel.statusText());
    }

    private Form readForm() {
        String server = text(serverInput);
        String user = text(userInput);
        String pass = text(passInput);
        String portText = text(portInput);
        if (server.isEmpty() || user.isEmpty() || pass.isEmpty() || portText.isEmpty()) {
            Toast.makeText(this, R.string.forward_need, Toast.LENGTH_SHORT).show();
            return null;
        }
        if (!hostOk(server)) {
            Toast.makeText(this, R.string.forward_host_bad, Toast.LENGTH_SHORT).show();
            return null;
        }
        int port = portOf(portText);
        if (port < 0) {
            Toast.makeText(this, R.string.forward_port_bad, Toast.LENGTH_SHORT).show();
            return null;
        }
        Tunnel.save(this, server, user, pass, port);
        return new Form(server, user, pass, port);
    }

    private static String text(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static boolean hostOk(String host) {
        if (host.length() > 253) return false;
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (c <= ' ' || c == '/' || c == '\\') return false;
        }
        return true;
    }

    private static int portOf(String raw) {
        if (raw.length() > 5) return -1;
        int n = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < '0' || c > '9') return -1;
            n = n * 10 + (c - '0');
        }
        if (n < 1 || n > 65535) return -1;
        return n;
    }

    private static final class Form {
        final String server;
        final String user;
        final String pass;
        final int port;

        Form(String server, String user, String pass, int port) {
            this.server = server;
            this.user = user;
            this.pass = pass;
            this.port = port;
        }
    }
}
