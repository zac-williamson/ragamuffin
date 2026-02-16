package ragamuffin.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

/**
 * Handles keyboard and mouse input for the game.
 */
public class InputHandler implements InputProcessor {

    private boolean forward, backward, left, right;
    private boolean escapePressed;
    private float mouseDeltaX, mouseDeltaY;
    private int lastX = -1, lastY = -1;

    public void update() {
        // Update movement keys
        forward = Gdx.input.isKeyPressed(Input.Keys.W);
        backward = Gdx.input.isKeyPressed(Input.Keys.S);
        left = Gdx.input.isKeyPressed(Input.Keys.A);
        right = Gdx.input.isKeyPressed(Input.Keys.D);

        // Mouse delta for look
        if (Gdx.input.isCursorCatched()) {
            int currentX = Gdx.input.getX();
            int currentY = Gdx.input.getY();

            if (lastX == -1) {
                lastX = currentX;
                lastY = currentY;
            }

            mouseDeltaX = currentX - lastX;
            mouseDeltaY = currentY - lastY;

            lastX = currentX;
            lastY = currentY;
        } else {
            mouseDeltaX = 0;
            mouseDeltaY = 0;
            lastX = -1;
            lastY = -1;
        }
    }

    public boolean isForward() { return forward; }
    public boolean isBackward() { return backward; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isEscapePressed() { return escapePressed; }

    public float getMouseDeltaX() { return mouseDeltaX; }
    public float getMouseDeltaY() { return mouseDeltaY; }

    public void resetEscape() { escapePressed = false; }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            escapePressed = true;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
}
