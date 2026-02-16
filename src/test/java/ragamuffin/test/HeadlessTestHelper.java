package ragamuffin.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.ApplicationAdapter;

/**
 * Helper for initializing LibGDX headless backend in tests.
 * Use this in @BeforeAll methods to ensure Gdx is available.
 */
public class HeadlessTestHelper {

    private static HeadlessApplication application;
    private static boolean initialized = false;

    /**
     * Initialize the headless backend. Safe to call multiple times.
     */
    public static void initHeadless() {
        if (!initialized) {
            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
            application = new HeadlessApplication(new ApplicationAdapter() {}, config);
            initialized = true;

            // Give it a moment to initialize
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Check if headless mode is initialized.
     */
    public static boolean isInitialized() {
        return initialized && Gdx.app != null;
    }
}
