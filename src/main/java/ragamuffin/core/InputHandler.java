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
    private boolean inventoryPressed;
    private boolean helpPressed;
    private boolean craftingPressed;
    private boolean punchPressed;
    private boolean placePressed;
    private boolean enterPressed;
    private int hotbarSlotPressed = -1; // 0-8 for slots 1-9, -1 for none
    private int craftingSlotPressed = -1; // For selecting recipes in crafting menu
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
    public boolean isInventoryPressed() { return inventoryPressed; }
    public boolean isHelpPressed() { return helpPressed; }
    public boolean isCraftingPressed() { return craftingPressed; }
    public boolean isPunchPressed() { return punchPressed; }
    public boolean isPlacePressed() { return placePressed; }
    public boolean isEnterPressed() { return enterPressed; }
    public int getHotbarSlotPressed() { return hotbarSlotPressed; }
    public int getCraftingSlotPressed() { return craftingSlotPressed; }

    public float getMouseDeltaX() { return mouseDeltaX; }
    public float getMouseDeltaY() { return mouseDeltaY; }

    public void resetEscape() { escapePressed = false; }
    public void resetInventory() { inventoryPressed = false; }
    public void resetHelp() { helpPressed = false; }
    public void resetCrafting() { craftingPressed = false; }
    public void resetPunch() { punchPressed = false; }
    public void resetPlace() { placePressed = false; }
    public void resetEnter() { enterPressed = false; }
    public void resetHotbarSlot() { hotbarSlotPressed = -1; }
    public void resetCraftingSlot() { craftingSlotPressed = -1; }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            escapePressed = true;
        } else if (keycode == Input.Keys.I) {
            inventoryPressed = true;
        } else if (keycode == Input.Keys.H) {
            helpPressed = true;
        } else if (keycode == Input.Keys.C) {
            craftingPressed = true;
        } else if (keycode == Input.Keys.ENTER) {
            enterPressed = true;
        } else if (keycode == Input.Keys.NUM_1) {
            hotbarSlotPressed = 0;
            craftingSlotPressed = 0;
        } else if (keycode == Input.Keys.NUM_2) {
            hotbarSlotPressed = 1;
            craftingSlotPressed = 1;
        } else if (keycode == Input.Keys.NUM_3) {
            hotbarSlotPressed = 2;
            craftingSlotPressed = 2;
        } else if (keycode == Input.Keys.NUM_4) {
            hotbarSlotPressed = 3;
            craftingSlotPressed = 3;
        } else if (keycode == Input.Keys.NUM_5) {
            hotbarSlotPressed = 4;
            craftingSlotPressed = 4;
        } else if (keycode == Input.Keys.NUM_6) {
            hotbarSlotPressed = 5;
            craftingSlotPressed = 5;
        } else if (keycode == Input.Keys.NUM_7) {
            hotbarSlotPressed = 6;
            craftingSlotPressed = 6;
        } else if (keycode == Input.Keys.NUM_8) {
            hotbarSlotPressed = 7;
            craftingSlotPressed = 7;
        } else if (keycode == Input.Keys.NUM_9) {
            hotbarSlotPressed = 8;
            craftingSlotPressed = 8;
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
        if (button == Input.Buttons.LEFT) {
            punchPressed = true;
        } else if (button == Input.Buttons.RIGHT) {
            placePressed = true;
        }
        return true;
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
