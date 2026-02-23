package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Pixel-art font renderer.
 *
 * Each character is defined as a 5-wide × 7-tall bitmap (stored as 7 rows, each a 5-bit
 * integer where bit 4 is the leftmost pixel).  Characters are rendered as filled squares
 * using a ShapeRenderer so no texture assets are required, matching the rest of the
 * game's vertex-colour rendering approach.
 *
 * Usage (ShapeRenderer must NOT already be active):
 * <pre>
 *   PixelFont.drawString(shapeRenderer, "Hello", x, y, pixelSize, color);
 * </pre>
 */
public final class PixelFont {

    /** Pixel columns per glyph (not including trailing gap). */
    public static final int GLYPH_W = 5;
    /** Pixel rows per glyph. */
    public static final int GLYPH_H = 7;
    /** Gap columns between glyphs. */
    private static final int GLYPH_GAP = 1;

    private PixelFont() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Draw a string in pixel-art style.
     *
     * @param sr        ShapeRenderer (must NOT be active — this method begins/ends it)
     * @param text      the string to draw
     * @param x         left edge of first character in screen pixels
     * @param y         bottom edge of the character row in screen pixels
     * @param pixelSize size of each "pixel" square in screen pixels
     * @param color     glyph colour
     */
    public static void drawString(ShapeRenderer sr, String text,
                                  float x, float y, float pixelSize, Color color) {
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(color);
        float cx = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            drawGlyph(sr, c, cx, y, pixelSize);
            cx += (GLYPH_W + GLYPH_GAP) * pixelSize;
        }
        sr.end();
    }

    /**
     * Returns the rendered width (in screen pixels) of a string at the given pixel size.
     */
    public static float stringWidth(String text, float pixelSize) {
        if (text.isEmpty()) return 0f;
        return (text.length() * (GLYPH_W + GLYPH_GAP) - GLYPH_GAP) * pixelSize;
    }

    /**
     * Returns the rendered height (in screen pixels) of a single glyph row.
     */
    public static float glyphHeight(float pixelSize) {
        return GLYPH_H * pixelSize;
    }

    // -----------------------------------------------------------------------
    // Internal glyph drawing
    // -----------------------------------------------------------------------

    private static void drawGlyph(ShapeRenderer sr, char c, float x, float y, float ps) {
        int[] rows = getGlyph(c);
        for (int row = 0; row < GLYPH_H; row++) {
            int bits = rows[row];
            // Row 0 is the top of the glyph; screen Y increases upward in LibGDX
            float gy = y + (GLYPH_H - 1 - row) * ps;
            for (int col = 0; col < GLYPH_W; col++) {
                if ((bits & (1 << (GLYPH_W - 1 - col))) != 0) {
                    sr.rect(x + col * ps, gy, ps, ps);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Glyph bitmaps  (5 wide × 7 tall, row 0 = top)
    // Each integer is a 5-bit row: bit4=leftmost, bit0=rightmost
    // -----------------------------------------------------------------------

    private static int[] getGlyph(char c) {
        switch (Character.toUpperCase(c)) {
            // Letters A-Z
            case 'A': return new int[]{0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001};
            case 'B': return new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110};
            case 'C': return new int[]{0b01111, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b01111};
            case 'D': return new int[]{0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110};
            case 'E': return new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111};
            case 'F': return new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000};
            case 'G': return new int[]{0b01111, 0b10000, 0b10000, 0b10011, 0b10001, 0b10001, 0b01111};
            case 'H': return new int[]{0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001};
            case 'I': return new int[]{0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b11111};
            case 'J': return new int[]{0b11111, 0b00010, 0b00010, 0b00010, 0b00010, 0b10010, 0b01100};
            case 'K': return new int[]{0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001};
            case 'L': return new int[]{0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111};
            case 'M': return new int[]{0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001};
            case 'N': return new int[]{0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001};
            case 'O': return new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110};
            case 'P': return new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000};
            case 'Q': return new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101};
            case 'R': return new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001};
            case 'S': return new int[]{0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110};
            case 'T': return new int[]{0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100};
            case 'U': return new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110};
            case 'V': return new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100};
            case 'W': return new int[]{0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001};
            case 'X': return new int[]{0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001};
            case 'Y': return new int[]{0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100};
            case 'Z': return new int[]{0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111};

            // Lowercase — same bitmaps as uppercase (pixel font is all-caps style)
            case 'a': return new int[]{0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001};
            case 'b': return new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110};
            case 'c': return new int[]{0b01111, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b01111};
            case 'd': return new int[]{0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110};
            case 'e': return new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111};
            case 'f': return new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000};
            case 'g': return new int[]{0b01111, 0b10000, 0b10000, 0b10011, 0b10001, 0b10001, 0b01111};
            case 'h': return new int[]{0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001};
            case 'i': return new int[]{0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b11111};
            case 'j': return new int[]{0b11111, 0b00010, 0b00010, 0b00010, 0b00010, 0b10010, 0b01100};
            case 'k': return new int[]{0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001};
            case 'l': return new int[]{0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111};
            case 'm': return new int[]{0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001};
            case 'n': return new int[]{0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001};
            case 'o': return new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110};
            case 'p': return new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000};
            case 'q': return new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101};
            case 'r': return new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001};
            case 's': return new int[]{0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110};
            case 't': return new int[]{0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100};
            case 'u': return new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110};
            case 'v': return new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100};
            case 'w': return new int[]{0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001};
            case 'x': return new int[]{0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001};
            case 'y': return new int[]{0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100};
            case 'z': return new int[]{0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111};

            // Digits
            case '0': return new int[]{0b01110, 0b10011, 0b10101, 0b10101, 0b11001, 0b10001, 0b01110};
            case '1': return new int[]{0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110};
            case '2': return new int[]{0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111};
            case '3': return new int[]{0b01110, 0b10001, 0b00001, 0b00110, 0b00001, 0b10001, 0b01110};
            case '4': return new int[]{0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010};
            case '5': return new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b00001, 0b00001, 0b11110};
            case '6': return new int[]{0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110};
            case '7': return new int[]{0b11111, 0b00001, 0b00010, 0b00100, 0b00100, 0b00100, 0b00100};
            case '8': return new int[]{0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110};
            case '9': return new int[]{0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100};

            // Punctuation
            case '.': return new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00100};
            case ',': return new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00100, 0b01000};
            case '!': return new int[]{0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00000, 0b00100};
            case '?': return new int[]{0b01110, 0b10001, 0b00001, 0b00110, 0b00100, 0b00000, 0b00100};
            case '\'': return new int[]{0b00100, 0b00100, 0b01000, 0b00000, 0b00000, 0b00000, 0b00000};
            case '\u2019': // right single quotation mark (')
                          return new int[]{0b00100, 0b00100, 0b01000, 0b00000, 0b00000, 0b00000, 0b00000};
            case '-': return new int[]{0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000};
            case '&': return new int[]{0b01100, 0b10010, 0b10100, 0b01000, 0b10100, 0b10010, 0b01101};
            case '+': return new int[]{0b00000, 0b00100, 0b00100, 0b11111, 0b00100, 0b00100, 0b00000};
            case '/': return new int[]{0b00001, 0b00010, 0b00100, 0b00100, 0b01000, 0b10000, 0b00000};
            case '#': return new int[]{0b01010, 0b01010, 0b11111, 0b01010, 0b11111, 0b01010, 0b01010};
            case ' ': return new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00000};

            // Fallback: small square for unknown characters
            default:  return new int[]{0b00000, 0b01110, 0b01010, 0b01010, 0b01110, 0b00000, 0b00000};
        }
    }
}
