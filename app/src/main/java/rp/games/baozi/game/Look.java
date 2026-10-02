package rp.games.baozi.game;

import rp.games.baozi.Profile;

/**
 * 玩家昵称和皮肤。套数跟 Profile.SKIN_RES 一样长，编号从 0 起。
 */
public final class Look {
    public static final int SKINS = Profile.SKIN_RES.length;
    public static final int NAME_LIMIT = 6;

    /** 没选过皮肤时，0 号用第 3 套，1 号用第 6 套。 */
    public static int roleSkin(final int slot) {
        return slot == 0 ? 2 : 5;
    }

    public static String roleName(final int slot) {
        return slot == 0 ? "一二" : "布布";
    }

    public static String shown(final String custom, final int slot) {
        if (custom != null && !custom.isEmpty()) return custom;
        return roleName(slot);
    }

    public static int clampSkin(final int skin, final int fallback) {
        if (skin < 0 || skin >= SKINS) return fallback;
        return skin;
    }

    public static String clean(final String raw) {
        if (raw == null) return "";
        final String trimmed = raw.trim();
        final StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length() && out.length() < NAME_LIMIT; i++) {
            final char c = trimmed.charAt(i);
            if (c < ' ' || c == 0x7f) continue;
            out.append(c);
        }
        return out.toString();
    }
}
