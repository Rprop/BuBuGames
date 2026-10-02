package rp.games;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import rp.games.baozi.BaoziMenuActivity;
import rp.games.chess.ChessActivity;
import rp.games.journey.JourneyActivity;
import rp.games.merge.MergeActivity;

/**
 * 小游戏入口。新游戏加一条 {@link #GAMES} 即可。
 */
public class MainActivity extends Activity {
    private static final GameEntry[] GAMES = {
            new GameEntry(R.string.game_baozi_title, R.string.game_baozi_detail, BaoziMenuActivity.class),
            new GameEntry(R.string.game_merge_title, R.string.game_merge_detail, MergeActivity.class),
            new GameEntry(R.string.game_journey_title, R.string.game_journey_detail, JourneyActivity.class),
            new GameEntry(R.string.game_chess_title, R.string.game_chess_detail, ChessActivity.class),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout list = findViewById(R.id.gameList);
        for (GameEntry entry : GAMES) {
            View card = getLayoutInflater().inflate(R.layout.item_game, list, false);
            ((TextView) card.findViewById(R.id.gameTitle)).setText(entry.title);
            ((TextView) card.findViewById(R.id.gameDetail)).setText(entry.detail);
            card.setOnClickListener(v -> startActivity(new Intent(this, entry.screen)));
            list.addView(card);
        }
    }

    private static final class GameEntry {
        final int title;
        final int detail;
        final Class<?> screen;

        GameEntry(int title, int detail, Class<?> screen) {
            this.title = title;
            this.detail = detail;
            this.screen = screen;
        }
    }
}
