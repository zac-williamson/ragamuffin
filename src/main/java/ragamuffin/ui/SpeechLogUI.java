package ragamuffin.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayDeque;
import java.util.Deque;
import ragamuffin.entity.NPC;
import ragamuffin.entity.NPCType;

/**
 * On-screen speech log displaying recent NPC dialogue.
 *
 * Entries fade out over time and the log shows at most MAX_ENTRIES lines.
 * Positioned in the bottom-right corner of the screen.
 */
public class SpeechLogUI {

    public static final int MAX_ENTRIES = 6;
    public static final float ENTRY_DURATION = 8.0f; // seconds before fade begins
    public static final float FADE_DURATION = 2.0f;  // seconds to fade out

    private static final float LOG_MARGIN_RIGHT = 20f;
    private static final float LOG_MARGIN_BOTTOM = 60f; // above the hotbar
    private static final float LINE_HEIGHT = 22f;
    private static final float PADDING_X = 8f;
    private static final float PADDING_Y = 4f;
    private static final float MAX_ENTRY_WIDTH = 400f;

    /** A single log entry: NPC type label + speech text with a remaining lifetime. */
    public static class Entry {
        public final String label;
        public final String text;
        private float timeRemaining;

        Entry(String label, String text) {
            this.label = label;
            this.text = text;
            this.timeRemaining = ENTRY_DURATION + FADE_DURATION;
        }

        /** 0.0 (invisible) → 1.0 (fully opaque). */
        public float getAlpha() {
            if (timeRemaining <= 0) return 0f;
            if (timeRemaining >= FADE_DURATION) return 1f;
            return timeRemaining / FADE_DURATION;
        }

        public boolean isExpired() {
            return timeRemaining <= 0;
        }

        public void update(float delta) {
            timeRemaining -= delta;
        }
    }

    // Most-recent entry at the front (head) of the deque
    private final Deque<Entry> entries = new ArrayDeque<>();

    // Track what each NPC was last saying to detect new speech
    private final java.util.Map<NPC, String> lastSpeech = new java.util.WeakHashMap<>();

    /**
     * Call once per frame to advance entry timers and detect new NPC speech.
     *
     * @param npcs  collection of all active NPCs
     * @param delta frame delta time in seconds
     */
    public void update(Iterable<NPC> npcs, float delta) {
        // Advance existing timers and remove expired entries
        entries.removeIf(e -> {
            e.update(delta);
            return e.isExpired();
        });

        // Detect new speech from any NPC
        for (NPC npc : npcs) {
            if (!npc.isSpeaking()) {
                lastSpeech.remove(npc);
                continue;
            }
            String current = npc.getSpeechText();
            String last = lastSpeech.get(npc);
            if (!current.equals(last)) {
                lastSpeech.put(npc, current);
                addEntry(npc.getType(), current);
            }
        }
    }

    /**
     * Manually add an entry (e.g. for testing or non-NPC speech).
     */
    public void addEntry(NPCType type, String text) {
        String label = formatLabel(type);
        entries.addFirst(new Entry(label, text));
        // Trim to max size
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    /**
     * Render the speech log on-screen.
     *
     * The log grows upward from the bottom-right corner.
     * ShapeRenderer must NOT be active when this is called.
     */
    public void render(SpriteBatch spriteBatch, ShapeRenderer shapeRenderer,
                       BitmapFont font, int screenWidth, int screenHeight) {
        if (entries.isEmpty()) return;

        // Snapshot to array so we can iterate oldest → newest (bottom to top)
        Entry[] snapshot = entries.toArray(new Entry[0]);

        float rowY = LOG_MARGIN_BOTTOM;

        // Draw bottom-most (oldest visible) entry first so newer ones sit above
        for (int i = snapshot.length - 1; i >= 0; i--) {
            Entry e = snapshot[i];
            float alpha = e.getAlpha();
            if (alpha <= 0f) continue;

            String line = e.label + ": " + e.text;
            float textWidth = Math.min(line.length() * 7f, MAX_ENTRY_WIDTH);
            float bgW = textWidth + PADDING_X * 2;
            float bgH = LINE_HEIGHT;
            float bgX = screenWidth - LOG_MARGIN_RIGHT - bgW;
            float bgY = rowY;

            // Draw semi-transparent background
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, 0.6f * alpha);
            shapeRenderer.rect(bgX, bgY, bgW, bgH);
            shapeRenderer.end();

            // Draw text
            spriteBatch.begin();
            Color labelColor = getLabelColor(e.label, alpha);
            font.setColor(labelColor);
            float textX = bgX + PADDING_X;
            float textY = bgY + bgH - PADDING_Y;
            font.draw(spriteBatch, line, textX, textY);
            font.setColor(Color.WHITE);
            spriteBatch.end();

            rowY += bgH + 2f;
        }
    }

    /** Returns the number of currently active (non-expired) entries. */
    public int getEntryCount() {
        return entries.size();
    }

    /** Returns a snapshot of current entries for inspection. */
    public java.util.List<Entry> getEntries() {
        return java.util.List.copyOf(entries);
    }

    private static String formatLabel(NPCType type) {
        switch (type) {
            case PUBLIC:          return "Passer-by";
            case YOUTH_GANG:      return "Youth";
            case POLICE:          return "Police";
            case SHOPKEEPER:      return "Shopkeeper";
            case POSTMAN:         return "Postman";
            case JOGGER:          return "Jogger";
            case DRUNK:           return "Drunk";
            case BUSKER:          return "Busker";
            case DELIVERY_DRIVER: return "Delivery";
            case PENSIONER:       return "Pensioner";
            case SCHOOL_KID:      return "School kid";
            case DOG:             return "Dog";
            case COUNCIL_BUILDER: return "Worker";
            case COUNCIL_MEMBER:  return "Official";
            default:              return "NPC";
        }
    }

    private static Color getLabelColor(String label, float alpha) {
        if (label.startsWith("Police")) {
            return new Color(0.4f, 0.8f, 1.0f, alpha);
        }
        if (label.startsWith("Youth")) {
            return new Color(1.0f, 0.4f, 0.4f, alpha);
        }
        return new Color(1.0f, 1.0f, 0.8f, alpha);
    }
}
