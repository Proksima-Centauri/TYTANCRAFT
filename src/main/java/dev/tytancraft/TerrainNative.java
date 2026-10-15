package dev.tytancraft;

final class TerrainNative {
    private static final boolean AVAILABLE;

    static {
        AVAILABLE = loadLibrary();
    }

    @SuppressWarnings("restricted")
    private static boolean loadLibrary() {
        boolean loaded;
        try {
            System.loadLibrary("tytancraft_world");
            loaded = true;
        } catch (UnsatisfiedLinkError ignored) {
            loaded = false;
        }
        return loaded;
    }

    private TerrainNative() {}

    static boolean isAvailable() {
        return AVAILABLE;
    }

    static native byte[] generateHeights(int width, int depth, long seed);
}
