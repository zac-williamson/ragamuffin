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
    private boolean achievementsPressed;
    private boolean questLogPressed;
    private boolean punchPressed;
    private boolean punchHeld; // true while left mouse button is held down
    private boolean placePressed;
    private boolean enterPressed;
    private boolean upPressed;
    private boolean downPressed;
    private boolean interactPressed; // E key for interaction
    private boolean jumpPressed; // Spacebar for jumping
    private boolean sprintHeld; // Left Shift for sprinting
    private boolean dodgePressed; // Left Ctrl for dodge/roll
    private int hotbarSlotPressed = -1; // 0-8 for slots 1-9, -1 for none
    private int craftingSlotPressed = -1; // For selecting recipes in crafting menu
    private float mouseDeltaX, mouseDeltaY;
    private float scrollAmountY;

    // Mouse position and click tracking for UI interaction
    private int mouseX, mouseY;
    private boolean leftClickPressed;
    private boolean leftClickReleased;
    private boolean rightClickPressed;

    public void update() {
        // Update movement keys
        forward = Gdx.input.isKeyPressed(Input.Keys.W);
        backward = Gdx.input.isKeyPressed(Input.Keys.S);
        left = Gdx.input.isKeyPressed(Input.Keys.A);
        right = Gdx.input.isKeyPressed(Input.Keys.D);
        sprintHeld = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        // Mouse delta for look - use LibGDX's built-in delta tracking
        if (Gdx.input.isCursorCatched()) {
            mouseDeltaX = Gdx.input.getDeltaX();
            mouseDeltaY = Gdx.input.getDeltaY();
        } else {
            mouseDeltaX = 0;
            mouseDeltaY = 0;
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
    public boolean isAchievementsPressed() { return achievementsPressed; }
    public boolean isQuestLogPressed() { return questLogPressed; }
    public boolean isPunchPressed() { return punchPressed; }
    public boolean isPunchHeld() { return punchHeld; }
    public boolean isPlacePressed() { return placePressed; }
    public boolean isEnterPressed() { return enterPressed; }
    public boolean isUpPressed() { return upPressed; }
    public boolean isDownPressed() { return downPressed; }
    public boolean isInteractPressed() { return interactPressed; }
    public boolean isJumpPressed() { return jumpPressed; }
    public boolean isSprintHeld() { return sprintHeld; }
    public boolean isDodgePressed() { return dodgePressed; }
    public int getHotbarSlotPressed() { return hotbarSlotPressed; }
    public int getCraftingSlotPressed() { return craftingSlotPressed; }

    public float getMouseDeltaX() { return mouseDeltaX; }
    public float getMouseDeltaY() { return mouseDeltaY; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public boolean isLeftClickPressed() { return leftClickPressed; }
    public boolean isLeftClickReleased() { return leftClickReleased; }
    public boolean isRightClickPressed() { return rightClickPressed; }
    public float getScrollAmountY() { return scrollAmountY; }

    public void resetLeftClick() { leftClickPressed = false; }
    public void resetLeftClickReleased() { leftClickReleased = false; }
    public void resetRightClick() { rightClickPressed = false; }
    public void resetScroll() { scrollAmountY = 0; }

    public void resetEscape() { escapePressed = false; }
    public void resetInventory() { inventoryPressed = false; }
    public void resetHelp() { helpPressed = false; }
    public void resetCrafting() { craftingPressed = false; }
    public void resetAchievements() { achievementsPressed = false; }
    public void resetQuestLog() { questLogPressed = false; }
    public void resetPunch() { punchPressed = false; }
    public void resetPunchHeld() { punchHeld = false; }
    public void resetPlace() { placePressed = false; }
    public void resetEnter() { enterPressed = false; }
    public void resetUp() { upPressed = false; }
    public void resetDown() { downPressed = false; }
    public void resetInteract() { interactPressed = false; }
    public void resetJump() { jumpPressed = false; }
    public void resetDodge() { dodgePressed = false; }
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
        } else if (keycode == Input.Keys.TAB) {
            achievementsPressed = true;
        } else if (keycode == Input.Keys.Q) {
            questLogPressed = true;
        } else if (keycode == Input.Keys.E) {
            interactPressed = true;
        } else if (keycode == Input.Keys.SPACE) {
            jumpPressed = true;
        } else if (keycode == Input.Keys.CONTROL_LEFT) {
            dodgePressed = true;
        } else if (keycode == Input.Keys.ENTER) {
            enterPressed = true;
        } else if (keycode == Input.Keys.UP) {
            upPressed = true;
        } else if (keycode == Input.Keys.DOWN) {
            downPressed = true;
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
        mouseX = screenX;
        mouseY = screenY;
        if (button == Input.Buttons.LEFT) {
            if (Gdx.input.isCursorCatched()) {
                punchPressed = true;
                punchHeld = true;
            } else {
                leftClickPressed = true;
            }
        } else if (button == Input.Buttons.RIGHT) {
            if (Gdx.input.isCursorCatched()) {
                placePressed = true;
            } else {
                rightClickPressed = true;
            }
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        mouseX = screenX;
        mouseY = screenY;
        if (button == Input.Buttons.LEFT) {
            punchHeld = false;
            if (!Gdx.input.isCursorCatched()) {
                leftClickReleased = true;
            }
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mouseX = screenX;
        mouseY = screenY;
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        scrollAmountY += amountY;
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            punchHeld = false;
            if (!Gdx.input.isCursorCatched()) {
                leftClickReleased = true;
            }
        }
        return false;
    }
}
