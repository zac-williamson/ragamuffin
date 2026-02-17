package ragamuffin.teavm;

import com.github.xpenatan.gdx.backends.teavm.TeaApplication;
import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration;
import ragamuffin.core.RagamuffinGame;

public class TeaVMLauncher {

    public static void main(String[] args) {
        TeaApplicationConfiguration config = new TeaApplicationConfiguration("canvas");
        config.width = 0;  // Use all available browser width
        config.height = 0; // Use all available browser height
        config.antialiasing = true;
        new TeaApplication(new RagamuffinGame(), config);
    }
}
