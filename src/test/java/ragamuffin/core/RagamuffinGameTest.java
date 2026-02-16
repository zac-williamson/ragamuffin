package ragamuffin.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RagamuffinGameTest {

    @Test
    void gameClassCanBeInstantiated() {
        RagamuffinGame game = new RagamuffinGame();
        assertNotNull(game);
    }
}
