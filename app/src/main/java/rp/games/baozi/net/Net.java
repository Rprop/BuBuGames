package rp.games.baozi.net;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

public final class Net {
    public static String lanIpv4() {
        try {
            final ArrayList<Inet4Address> all = new ArrayList<>();
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return "没有局域网 IP";
            while (interfaces.hasMoreElements()) {
                final NetworkInterface net = interfaces.nextElement();
                final Enumeration<java.net.InetAddress> addresses = net.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    final java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        all.add((Inet4Address) addr);
                    }
                }
            }
            Inet4Address lan = null;
            for (final Inet4Address addr : all) {
                if (addr.isSiteLocalAddress()) {
                    lan = addr;
                    break;
                }
            }
            if (lan == null && !all.isEmpty()) lan = all.get(0);
            if (lan == null || lan.getHostAddress() == null) return "没有局域网 IP";
            return lan.getHostAddress();
        } catch (Exception ignored) {
            return "没有局域网 IP";
        }
    }
}
