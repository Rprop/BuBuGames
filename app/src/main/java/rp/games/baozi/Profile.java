package rp.games.baozi;

import android.content.Context;
import android.content.SharedPreferences;

import rp.games.R;
import rp.games.baozi.game.Look;

/**
 * 这台手机上的昵称和皮肤。没保存过皮肤时返回 -1，对局里按槽位用默认套。
 */
public final class Profile {
    public static final int[] SKIN_RES = {
            R.drawable.player_00,
            R.drawable.player_01,
            R.drawable.player_02,
            R.drawable.player_03,
            R.drawable.player_04,
            R.drawable.player_05,
            R.drawable.player_06,
            R.drawable.player_07,
            R.drawable.player_08,
            R.drawable.player_09,
            R.drawable.player_10
    };

    private static final String PREF = "baozi_profile";
    private static final String KEY_NAME = "name";
    private static final String KEY_SKIN = "skin";

    public static String name(final Context context) {
        return Look.clean(prefs(context).getString(KEY_NAME, ""));
    }

    /**
     * 保存过才是 0 到 7，否则是 -1。
     */
    public static int savedSkin(final Context context) {
        final SharedPreferences prefs = prefs(context);
        if (!prefs.contains(KEY_SKIN)) return -1;
        return Look.clampSkin(prefs.getInt(KEY_SKIN, -1), -1);
    }

    public static int skinForSlot(final Context context, final int slot) {
        return Look.clampSkin(savedSkin(context), Look.roleSkin(slot));
    }

    public static void save(final Context context, final String name, final int skin) {
        prefs(context).edit()
                .putString(KEY_NAME, Look.clean(name))
                .putInt(KEY_SKIN, Look.clampSkin(skin, Look.roleSkin(0)))
                .apply();
    }

    private static SharedPreferences prefs(final Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
