package rp.games.baozi;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import rp.games.baozi.game.Rules;

import java.util.HashMap;
import java.util.Map;

public final class RoomCode {
    public static final class Endpoint {
        public final String host;
        public final int port;

        public Endpoint(final String host, final int port) {
            this.host = host;
            this.port = port;
        }
    }

    public static String encode(final String host) {
        return host + ":" + Rules.PORT;
    }

    /** 新码是 host:port。旧的 baozi://join 链接也能认。 */
    public static Endpoint parse(final String raw) {
        if (raw == null) return null;
        final String text = raw.trim();
        if (text.startsWith("baozi://join")) return parseLink(text);
        return parseHostPort(text);
    }

    public static Bitmap bitmap(final String payload, final int size) {
        try {
            final Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            final BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints);
            final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Endpoint parseLink(final String text) {
        final int queryAt = text.indexOf('?');
        if (queryAt < 0) return null;
        String host = null;
        String port = null;
        for (final String part : text.substring(queryAt + 1).split("&")) {
            final int eq = part.indexOf('=');
            if (eq <= 0) continue;
            final String key = part.substring(0, eq);
            final String value = part.substring(eq + 1).trim();
            if ("ip".equals(key) || "host".equals(key)) host = value;
            else if ("port".equals(key)) port = value;
        }
        final int n = port == null || port.isEmpty() ? Rules.PORT : portOf(port);
        if (n < 0 || !hostOk(host)) return null;
        return new Endpoint(unwrap(host), n);
    }

    private static Endpoint parseHostPort(final String text) {
        final int colon = text.lastIndexOf(':');
        if (colon <= 0 || colon == text.length() - 1) return null;
        final String host = text.substring(0, colon).trim();
        final int port = portOf(text.substring(colon + 1).trim());
        if (port < 0 || !hostOk(host)) return null;
        return new Endpoint(unwrap(host), port);
    }

    private static String unwrap(final String host) {
        if (host.startsWith("[") && host.endsWith("]") && host.length() > 2) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static boolean hostOk(final String host) {
        if (host == null) return false;
        final String bare = unwrap(host.trim());
        if (bare.isEmpty()) return false;
        if (host.startsWith("[")) return ipv6(bare);
        return ipv4(bare) || domain(bare);
    }

    private static boolean ipv4(final String raw) {
        final String[] parts = raw.split("\\.");
        if (parts.length != 4) return false;
        for (final String part : parts) {
            if (part.isEmpty() || part.length() > 3) return false;
            int n = 0;
            for (int i = 0; i < part.length(); i++) {
                final char c = part.charAt(i);
                if (c < '0' || c > '9') return false;
                n = n * 10 + (c - '0');
            }
            if (n > 255) return false;
        }
        return true;
    }

    private static boolean ipv6(final String raw) {
        if (raw.length() > 45) return false;
        int colons = 0;
        for (int i = 0; i < raw.length(); i++) {
            final char c = raw.charAt(i);
            if (c == ':') colons++;
            else if (c == '.') continue;
            else if (!hex(c)) return false;
        }
        return colons >= 2;
    }

    private static boolean domain(final String raw) {
        if (raw.length() > 253) return false;
        final String[] labels = raw.split("\\.");
        if (labels.length == 0) return false;
        for (final String label : labels) {
            if (label.isEmpty() || label.length() > 63) return false;
            if (label.charAt(0) == '-' || label.charAt(label.length() - 1) == '-') return false;
            for (int i = 0; i < label.length(); i++) {
                final char c = label.charAt(i);
                boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9') || c == '-';
                if (!ok) return false;
            }
        }
        return true;
    }

    private static int portOf(final String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > 5) return -1;
        int n = 0;
        for (int i = 0; i < raw.length(); i++) {
            final char c = raw.charAt(i);
            if (c < '0' || c > '9') return -1;
            n = n * 10 + (c - '0');
        }
        if (n < 1 || n > 65535) return -1;
        return n;
    }

    private static boolean hex(final char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
