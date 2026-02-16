package ragamuffin.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class RagamuffinGame extends ApplicationAdapter {

    @Override
    public void create() {
        Gdx.app.log("Ragamuffin", "Welcome to the real world, kid.");
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void dispose() {
    }
}
