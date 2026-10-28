package dev.tytancraft;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public final class TytanCraft extends Canvas implements KeyListener, MouseListener,
        MouseMotionListener, MouseWheelListener, FocusListener {
    private static final int VIEW_WIDTH = 320;
    private static final int VIEW_HEIGHT = 180;
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final int INVENTORY_WIDTH = 970;
    private static final int INVENTORY_HEIGHT = 630;
    private static final int INVENTORY_PANEL_X = (WINDOW_WIDTH - INVENTORY_WIDTH) / 2;
    private static final int INVENTORY_PANEL_Y = (WINDOW_HEIGHT - INVENTORY_HEIGHT) / 2;
    private static final int CRAFT_GRID_X = 80, CRAFT_GRID_Y = 145, CRAFT_SLOT_SIZE = 62, CRAFT_SLOT_GAP = 4;
    private static final int CRAFT_OUTPUT_X = 475, CRAFT_OUTPUT_SIZE = 72;
    private static final int CRAFT_RECIPE_X = 620, CRAFT_RECIPE_Y = 140, CRAFT_RECIPE_STEP = 98;
    private static final int CRAFT_RECIPE_WIDTH = 90, CRAFT_RECIPE_HEIGHT = 72;
    private static final int CRAFT_INVENTORY_TOP = 365, CRAFT_HOTBAR_TOP = 567;
    private static final int CRAFT_INVENTORY_SLOT_SIZE = 62, CRAFT_INVENTORY_GAP = 4;
    private static final byte[] CRAFT_OUTPUTS = {World.PLANKS, World.TORCH, World.EMERALD_BLOCK};
    private static final int[] CRAFT_OUTPUT_COUNTS = {4, 4, 1};
    private static final String[] CRAFT_RECIPE_NAMES = {"Deski", "Pochodnie", "Blok szmaragdu"};
    private static final int MAX_ANIMALS = 128;
    private static final int MAX_ANIMALS_PER_KIND = 10;
    private static final int LOBBY_CARD_WIDTH = 296;
    private static final int LOBBY_CARD_HEIGHT = 395;
    private static final int LOBBY_NICKNAME_X = 17, LOBBY_NICKNAME_Y = 69;
    private static final int LOBBY_NICKNAME_WIDTH = 262, LOBBY_NICKNAME_HEIGHT = 51;
    private static final int LOBBY_MODE_Y = 65, LOBBY_MODE_HEIGHT = 34;
    private static final int LOBBY_SURVIVAL_X = 17, LOBBY_SURVIVAL_WIDTH = 122;
    private static final int LOBBY_CREATIVE_X = 151, LOBBY_CREATIVE_WIDTH = 128;
    private static final int LOBBY_SLIDER_X = 33, LOBBY_SLIDER_WIDTH = 256;
    private static final int LOBBY_AGGRESSION_Y = 211, LOBBY_DAY_SPEED_Y = 273, LOBBY_RENDER_DISTANCE_Y = 341;
    private static final int LOBBY_SAVED_LIST_Y = 229, LOBBY_SAVED_ROW_STEP = 31, LOBBY_SAVED_ROW_HEIGHT = 28;
    private static final int LOBBY_START_BUTTON_WIDTH = 330, LOBBY_START_BUTTON_HEIGHT = 63;
    private static final int MAX_RAIN_SPLASHES = 64;
    private static final BasicStroke RAIN_SPLASH_STROKE = new BasicStroke(1.2f);
    private static final float FOV = (float) Math.toRadians(76);
    private static final int RENDER_DISTANCE_UNIT = 16;
    private static final float INTERACTION_REACH = 5.5f;
    private static final byte GRASS_BLOCK = World.GRASS;
    private static final byte DIRT_BLOCK = World.DIRT;
    private static final byte STONE_BLOCK = World.STONE;
    private static final byte SAND_BLOCK = World.SAND;
    private static final byte WOOD_BLOCK = World.WOOD;
    private static final byte LEAF_BLOCK = World.LEAVES;
    private static final byte[] BLOCK_TYPES = {
            GRASS_BLOCK, DIRT_BLOCK, STONE_BLOCK, SAND_BLOCK, WOOD_BLOCK, LEAF_BLOCK,
            World.COAL_ORE, World.COPPER_ORE, World.IRON_ORE, World.GOLD_ORE,
            World.DIAMOND_ORE, World.EMERALD_ORE, World.RUBY_ORE, World.SAPPHIRE_ORE, World.SNOW,
            World.RED_FLOWER, World.YELLOW_FLOWER, World.BLUE_FLOWER,
            World.BIRCH_WOOD, World.BIRCH_LEAVES, World.PINE_WOOD, World.PINE_LEAVES,
            World.ACACIA_WOOD, World.ACACIA_LEAVES, World.CHERRY_WOOD, World.CHERRY_LEAVES,
            World.PLANKS, World.TORCH, World.EMERALD_BLOCK,
            World.RED_MUSHROOM_SMALL, World.RED_MUSHROOM_MEDIUM, World.RED_MUSHROOM_LARGE,
            World.BROWN_MUSHROOM_SMALL, World.BROWN_MUSHROOM_MEDIUM, World.BROWN_MUSHROOM_LARGE,
            World.COD_SPAWN_EGG, World.SALMON_SPAWN_EGG, World.TROPICAL_FISH_SPAWN_EGG,
            World.PIKE_SPAWN_EGG, World.CARP_SPAWN_EGG, World.WOOL, World.LADDER
    };
    private static final Font[][] UI_FONTS = createUiFonts();
    private static final Font MONOSPACED_BOLD_14 = new Font(Font.MONOSPACED, Font.BOLD, 14);
    private static final Font MONOSPACED_BOLD_34 = new Font(Font.MONOSPACED, Font.BOLD, 34);
    private static final Font MONOSPACED_PLAIN_12 = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private static final int[] COLOR_CACHE_KEYS = new int[512];
    private static final Color[] COLOR_CACHE = new Color[COLOR_CACHE_KEYS.length];
    private static final byte[] BLOCKS_TAB = {
            World.GRASS, World.DIRT, World.STONE, World.SAND, World.WOOD,
            World.COAL_ORE, World.COPPER_ORE, World.IRON_ORE, World.GOLD_ORE,
            World.DIAMOND_ORE, World.EMERALD_ORE, World.RUBY_ORE, World.SAPPHIRE_ORE,
            World.PLANKS, World.TORCH, World.EMERALD_BLOCK,
            World.BIRCH_WOOD, World.PINE_WOOD, World.ACACIA_WOOD, World.CHERRY_WOOD, World.LADDER
    };
    private static final byte[] NATURE_TAB = {
            World.SNOW, World.WOOL, World.LEAVES, World.BIRCH_LEAVES, World.PINE_LEAVES,
            World.ACACIA_LEAVES, World.CHERRY_LEAVES,
            World.RED_FLOWER, World.YELLOW_FLOWER, World.BLUE_FLOWER,
            World.RED_MUSHROOM_SMALL, World.RED_MUSHROOM_MEDIUM, World.RED_MUSHROOM_LARGE,
            World.BROWN_MUSHROOM_SMALL, World.BROWN_MUSHROOM_MEDIUM, World.BROWN_MUSHROOM_LARGE
    };
    private static final byte[] SPAWN_TAB = {
            World.COD_SPAWN_EGG, World.SALMON_SPAWN_EGG, World.TROPICAL_FISH_SPAWN_EGG,
            World.PIKE_SPAWN_EGG, World.CARP_SPAWN_EGG
    };
    private static final float[][] FLOWER_PARTS = {
            {0.44f, 0.00f, 0.44f, 0.56f, 0.69f, 0.56f, World.LEAVES},
            {0.24f, 0.31f, 0.44f, 0.76f, 0.39f, 0.56f, World.LEAVES},
            {0.44f, 0.31f, 0.24f, 0.56f, 0.39f, 0.76f, World.LEAVES},
            {0.39f, 0.63f, 0.39f, 0.61f, 0.83f, 0.61f, 0},
            {0.27f, 0.67f, 0.39f, 0.43f, 0.82f, 0.61f, 0},
            {0.57f, 0.67f, 0.39f, 0.73f, 0.82f, 0.61f, 0},
            {0.39f, 0.67f, 0.27f, 0.61f, 0.82f, 0.43f, 0},
            {0.39f, 0.67f, 0.57f, 0.61f, 0.82f, 0.73f, 0}
    };
    private static final float[][] SMALL_MUSHROOM_PARTS = {
            {0.43f, 0.00f, 0.43f, 0.57f, 0.24f, 0.57f, World.SNOW},
            {0.20f, 0.20f, 0.20f, 0.80f, 0.43f, 0.80f, 0}
    };
    private static final float[][] MEDIUM_MUSHROOM_PARTS = {
            {0.40f, 0.00f, 0.40f, 0.60f, 0.39f, 0.60f, World.SNOW},
            {0.14f, 0.36f, 0.14f, 0.86f, 0.68f, 0.86f, 0}
    };
    private static final float[][] LARGE_MUSHROOM_PARTS = {
            {0.37f, 0.00f, 0.37f, 0.63f, 0.52f, 0.63f, World.SNOW},
            {0.06f, 0.48f, 0.06f, 0.94f, 0.94f, 0.94f, 0}
    };
    private static final float[][] LADDER_Z_POS_PARTS = createLadderParts(false, true);
    private static final float[][] LADDER_Z_NEG_PARTS = createLadderParts(false, false);
    private static final float[][] LADDER_X_POS_PARTS = createLadderParts(true, true);
    private static final float[][] LADDER_X_NEG_PARTS = createLadderParts(true, false);
    private static final int CLOUD_SIZE = 128;
    private static final float[] CLOUD_MAP = createCloudMap();

    private enum Weather { CLEAR, CLOUDY, RAIN }

    private World world;
    private int spawnX, spawnZ;
    private Animal[] animals;
    private final Animal[] visibleAnimals = new Animal[MAX_ANIMALS];
    private final float[] visibleAnimalCos = new float[MAX_ANIMALS];
    private final float[] visibleAnimalSin = new float[MAX_ANIMALS];
    private final int[] animalPopulationScratch = new int[Animal.Kind.values().length];
    private Random animalRandom;
    private Random weatherRandom;
    private final Hit[] traceScratch = newHitScratch(VIEW_HEIGHT);
    private final Hit[] underwaterTraceScratch = newHitScratch(VIEW_HEIGHT);
    private final AnimalHit[] animalHitScratch = newAnimalHitScratch(VIEW_HEIGHT);
    private final BufferedImage scene = new BufferedImage(VIEW_WIDTH, VIEW_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) scene.getRaster().getDataBuffer()).getData();
    private final boolean[] keys = new boolean[512];
    private final ConcurrentLinkedQueue<Runnable> pendingActions = new ConcurrentLinkedQueue<>();
    private final float[] columnX = new float[VIEW_WIDTH];
    private final float[] columnY = new float[VIEW_HEIGHT];
    private final int[] inventory = new int[48];
    private final byte[] hotbar = {
            GRASS_BLOCK, DIRT_BLOCK, STONE_BLOCK, SAND_BLOCK, WOOD_BLOCK,
            World.COAL_ORE, World.IRON_ORE, World.COPPER_ORE, World.SNOW, World.RED_FLOWER
    };
    private long seed;
    private float playerX, playerY, playerZ, verticalVelocity;
    private volatile float yaw = 0.15f, pitch = -0.05f;
    private volatile int selectedBlock;
    private int hitX = -1, hitY, hitZ, hitNormalX, hitNormalY, hitNormalZ;
    private boolean hasHit;
    private float aimedBlockDistance = Float.POSITIVE_INFINITY;
    private String aimedAnimal;
    private Animal aimedAnimalEntity;
    private float aimedAnimalDistance;
    private boolean hasWaterHit;
    private int waterHitX, waterHitY, waterHitZ;
    private float waterHitDistance;
    private volatile boolean captured, paused, running = true;
    private volatile boolean inLobby = true;
    private volatile boolean inventoryOpen;
    private volatile boolean saveRequested;
    private volatile String saveMessage = "";
    private volatile float saveMessageTimer;
    private volatile List<WorldStorage.SaveInfo> savedWorlds = List.of();
    private int savedWorldOffset;
    private boolean loadedSave;
    private int inventoryTab;
    private int customizeSlot;
    private int selectedRecipe;
    private String craftMessage = "";
    private volatile boolean creativeMode;
    private volatile int aggressionLevel = 25;
    private volatile float daySpeed = 1.0f;
    private volatile int renderDistance = 4;
    private volatile boolean leftPressed, rightPressed;
    private volatile String nickname = "Gracz";
    private volatile boolean nicknameFocused;
    private boolean aggressionDragging;
    private boolean timeSpeedDragging;
    private boolean renderDistanceDragging;
    private int playerHealth = 100;
    private float hunger = 100, oxygen = 100;
    private int meatCount;
    private float damageCooldown, attackCooldown, blockActionCooldown;
    private final ArrayList<RainSplash> rainSplashes = new ArrayList<>(MAX_RAIN_SPLASHES);
    private float rainSplashTimer;
    private float dayTime = 0.5f, daylight = 1.0f, sunlight = 1.0f;
    private float sunX, sunY = 1, sunZ = 0.25f;
    private Weather weather = Weather.CLEAR;
    private float weatherTimer = 35, cloudCoverage = 0.12f, targetCloudCoverage = 0.12f;
    private float rainIntensity, targetRainIntensity, cloudOffsetX, cloudOffsetY, rainPhase;
    private long fpsStarted = System.nanoTime();
    private int frames, fps;
    private long lastNativeMouseEvent;
    private Robot robot;
    private Thread gameThread;
    private JFrame window;
    private GraphicsDevice graphicsDevice;
    private boolean fullScreen;
    private java.awt.Point windowCenter;
    private java.awt.Cursor hiddenCursor;

    private TytanCraft(long seed) {
        this(seed, new World(seed));
    }

    private TytanCraft(long seed, World world) {
        this.seed = seed;
        this.world = world;
        int[] spawn = world.findDrySpawn();
        spawnX = spawn[0];
        spawnZ = spawn[1];
        animalRandom = new Random(seed ^ 0x6a09e667f3bcc909L);
        weatherRandom = new Random(seed ^ 0xbb67ae8584caa73bL);
        animals = createAnimals(seed);
        playerX = spawnX + 0.5f;
        playerZ = spawnZ + 0.5f;
        playerY = world.heightAt(spawnX, spawnZ) + 1.0f;
        updateSunPosition();
        setPreferredSize(new java.awt.Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setIgnoreRepaint(true);
        addKeyListener(this);
        addFocusListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        setFocusable(true);
        initializeViewRays();
        refreshSavedWorlds();
        BlockRenderAssets.prefetch(BLOCK_TYPES);
    }

    public static void main(String[] args) {
        long seed = args.length == 0 ? new Random().nextLong() : parseSeed(args[0]);
        World world = new World(seed);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("TytanCraft — Dzika Dolina");
            BufferedImage icon = loadApplicationIcon();
            if (icon != null) {
                frame.setIconImage(icon);
                try {
                    if (Taskbar.isTaskbarSupported()) {
                        Taskbar taskbar = Taskbar.getTaskbar();
                        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) taskbar.setIconImage(icon);
                    }
                } catch (UnsupportedOperationException ignored) {
                    // Some desktop environments expose Taskbar without supporting custom icons.
                }
            }
            TytanCraft game = new TytanCraft(seed, world);
            game.window = frame;
            game.graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
                    game.releaseFullScreen();
                    game.stopGame();
                    frame.dispose();
                }
            });
            frame.setResizable(false);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
            game.gameThread = new Thread(game::start, "tytancraft-game");
            game.gameThread.setDaemon(true);
            game.gameThread.start();
        });
    }

    private static BufferedImage loadApplicationIcon() {
        try (var input = TytanCraft.class.getResourceAsStream("/ICON.png")) {
            return input == null ? null : ImageIO.read(input);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static long parseSeed(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException signedOverflow) {
            return Long.parseUnsignedLong(text);
        }
    }

    private void toggleFullScreen() {
        if (window == null || graphicsDevice == null) return;
        synchronized (this) {
            boolean entering = !fullScreen;
            boolean recapture = captured;
            if (recapture) {
                captured = false;
                setCursor(java.awt.Cursor.getDefaultCursor());
            }
            try {
                if (fullScreen) graphicsDevice.setFullScreenWindow(null);
                window.dispose();
                window.setUndecorated(entering);
                if (entering) window.setBounds(graphicsDevice.getDefaultConfiguration().getBounds());
                else {
                    window.pack();
                    window.setLocationRelativeTo(null);
                }
                window.setVisible(true);
                if (entering) graphicsDevice.setFullScreenWindow(window);
                fullScreen = entering;
                recreateBufferStrategy();
                if (recapture) captureMouse();
                else requestFocusInWindow();
            } catch (RuntimeException failure) {
                try {
                    graphicsDevice.setFullScreenWindow(null);
                    window.dispose();
                    window.setUndecorated(false);
                    window.pack();
                    window.setLocationRelativeTo(null);
                    window.setVisible(true);
                    fullScreen = false;
                    recreateBufferStrategy();
                    if (recapture) captureMouse();
                    else requestFocusInWindow();
                } catch (RuntimeException ignored) {
                    fullScreen = false;
                }
                saveMessage = "Nie można przełączyć trybu pełnoekranowego";
                saveMessageTimer = 4.0f;
            }
        }
    }

    private void releaseFullScreen() {
        if (!fullScreen || graphicsDevice == null) return;
        graphicsDevice.setFullScreenWindow(null);
        fullScreen = false;
    }

    private void stopGame() {
        running = false;
        if (gameThread != null && gameThread != Thread.currentThread()) {
            try {
                gameThread.join(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        releaseFullScreen();
        if (!inLobby) saveWorldNow();
    }

    private void initializeViewRays() {
        float focalLength = (VIEW_HEIGHT * 0.5f) / (float) Math.tan(FOV * 0.5f);
        for (int x = 0; x < VIEW_WIDTH; x++) columnX[x] = (x + 0.5f - VIEW_WIDTH * 0.5f) / focalLength;
        for (int y = 0; y < VIEW_HEIGHT; y++) columnY[y] = (VIEW_HEIGHT * 0.5f - y - 0.5f) / focalLength;
    }

    private void start() {
        synchronized (this) {
            if (getBufferStrategy() == null) createBufferStrategy(2);
        }
        long previous = System.nanoTime();
        double accumulator = 0;
        final double step = 1.0 / 60.0;
        while (running) {
            if (!isDisplayable()) {
                previous = System.nanoTime();
                accumulator = 0;
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }
            long now = System.nanoTime();
            accumulator += Math.min(0.1, (now - previous) / 1_000_000_000.0);
            previous = now;
            while (accumulator >= step) {
                update((float) step);
                accumulator -= step;
            }
            render();
            drawFrame();
            countFrame();
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void update(float dt) {
        Runnable action;
        while ((action = pendingActions.poll()) != null) action.run();
        if (saveRequested) {
            saveRequested = false;
            saveWorldNow();
        }
        if (saveMessageTimer > 0) saveMessageTimer = Math.max(0, saveMessageTimer - dt);
        if (inLobby || inventoryOpen || paused || !captured) return;
        for (Animal animal : animals) {
            if (animal.alive) animal.update(world, dt, animalRandom, aggressionLevel, playerX, playerZ, !creativeMode);
        }
        breedAnimals();
        world.tickWaterFlow();
        advanceEnvironment(dt);
        updateRainSplashes(dt);
        attackCooldown = Math.max(0, attackCooldown - dt);
        blockActionCooldown = Math.max(0, blockActionCooldown - dt);
        if (creativeMode) {
            updateCreative(dt);
            if (leftPressed) mineOrPlace(false);
            if (rightPressed) mineOrPlace(true);
            return;
        }
        damageCooldown = Math.max(0, damageCooldown - dt);
        boolean swimming = isInWater();
        boolean climbing = !swimming && isOnLadder();
        float forward = (pressed(KeyEvent.VK_W) ? 1 : 0) - (pressed(KeyEvent.VK_S) ? 1 : 0);
        float strafe = (pressed(KeyEvent.VK_D) ? 1 : 0) - (pressed(KeyEvent.VK_A) ? 1 : 0);
        if (climbing) forward = 0;
        float length = (float) Math.sqrt(forward * forward + strafe * strafe);
        if (length > 0) {
            forward /= length;
            strafe /= length;
            float speed = swimming
                    ? (pressed(KeyEvent.VK_SHIFT) ? 4.0f : 2.55f)
                    : (pressed(KeyEvent.VK_SHIFT) ? 7.8f : 4.6f);
            float dx = ((float) Math.sin(yaw) * forward + (float) Math.cos(yaw) * strafe) * speed * dt;
            float dz = ((float) Math.cos(yaw) * forward - (float) Math.sin(yaw) * strafe) * speed * dt;
            boolean movingForward = forward > 0.05f;
            tryMoveX(dx, !swimming && movingForward, swimming && movingForward && pressed(KeyEvent.VK_SPACE));
            tryMoveZ(dz, !swimming && movingForward, swimming && movingForward && pressed(KeyEvent.VK_SPACE));
        }

        climbing = !swimming && isOnLadder();
        if (climbing) {
            if (pressed(KeyEvent.VK_W) || pressed(KeyEvent.VK_SPACE)) verticalVelocity = 3.0f;
            else if (pressed(KeyEvent.VK_S) || pressed(KeyEvent.VK_CONTROL)) verticalVelocity = -3.0f;
            else verticalVelocity = 0;
        } else if (swimming) {
            if (pressed(KeyEvent.VK_SPACE)) verticalVelocity = Math.min(3.6f, verticalVelocity + 13.0f * dt);
            else if (pressed(KeyEvent.VK_CONTROL)) verticalVelocity = Math.max(-3.0f, verticalVelocity - 13.0f * dt);
            else {
                float waterlineFeet = World.WATER_LEVEL + 1.0f - 1.62f;
                verticalVelocity += (waterlineFeet - playerY) * 2.6f * dt;
                verticalVelocity *= 0.90f;
            }
            verticalVelocity = Math.max(-3.6f, Math.min(3.6f, verticalVelocity));
        } else {
            if (pressed(KeyEvent.VK_SPACE) && isOnGround()) {
                verticalVelocity = 8.0f;
                synchronized (keys) { keys[KeyEvent.VK_SPACE] = false; }
            }
            verticalVelocity -= 21.0f * dt;
        }
        float newY = playerY + verticalVelocity * dt;
        if (verticalVelocity <= 0 && solidAt(playerX, newY - 0.05f, playerZ)) {
            playerY = (float) Math.floor(newY - 0.05f) + 1.05f;
            verticalVelocity = 0;
        } else if (verticalVelocity > 0 && (solidAt(playerX, newY + 1.75f, playerZ))) {
            verticalVelocity = 0;
        } else {
            playerY = Math.max(1.0f, newY);
        }
        updateSurvivalVitals(dt);
        updateAnimalDamage();
        if (leftPressed) mineOrPlace(false);
        if (rightPressed) mineOrPlace(true);
    }

    private void advanceEnvironment(float dt) {
        float speed = daySpeed;
        if (speed > 0) {
            dayTime = (dayTime + dt * speed / 240.0f) % 1.0f;
        }
        weatherTimer -= dt;
        cloudOffsetX += dt * 0.0018f;
        cloudOffsetY += dt * 0.00055f;
        rainPhase += dt * (28.0f + speed * 8.0f);
        if (weatherTimer <= 0) chooseWeather();
        float blend = Math.min(1.0f, dt * 0.045f);
        cloudCoverage += (targetCloudCoverage - cloudCoverage) * blend;
        rainIntensity += (targetRainIntensity - rainIntensity) * blend;
        updateSunPosition();
    }

    private void chooseWeather() {
        float choice = weatherRandom.nextFloat();
        if (choice < 0.55f) {
            weather = Weather.CLEAR;
            targetCloudCoverage = 0.10f + weatherRandom.nextFloat() * 0.14f;
            targetRainIntensity = 0;
        } else if (choice < 0.82f) {
            weather = Weather.CLOUDY;
            targetCloudCoverage = 0.52f + weatherRandom.nextFloat() * 0.18f;
            targetRainIntensity = 0;
        } else {
            weather = Weather.RAIN;
            targetCloudCoverage = 0.82f + weatherRandom.nextFloat() * 0.15f;
            targetRainIntensity = 0.72f + weatherRandom.nextFloat() * 0.28f;
        }
        weatherTimer = 35.0f + weatherRandom.nextFloat() * 75.0f;
    }

    private void updateSunPosition() {
        float angle = dayTime * (float) (Math.PI * 2) - (float) (Math.PI / 2);
        sunX = (float) Math.cos(angle) * 0.92f;
        sunY = (float) Math.sin(angle);
        sunZ = 0.24f;
        float inverseLength = 1.0f / (float) Math.sqrt(sunX * sunX + sunY * sunY + sunZ * sunZ);
        sunX *= inverseLength;
        sunY *= inverseLength;
        sunZ *= inverseLength;
        float dayBlend = smoothStep(clamp01((sunY + 0.20f) / 0.68f));
        daylight = dayBlend;
        sunlight = daylight * (1.0f - cloudCoverage * 0.35f - rainIntensity * 0.20f);
    }

    private float cloudOpacity(float directionY, float directionX, float directionZ) {
        if (directionY < 0.045f || cloudCoverage < 0.08f) return 0;
        float height = Math.max(0.07f, directionY);
        float u = (directionX / height * 0.20f + cloudOffsetX) * CLOUD_SIZE;
        float v = (directionZ / height * 0.20f + cloudOffsetY) * CLOUD_SIZE;
        float noise = sampleCloudMap(u, v);
        float threshold = 0.94f - cloudCoverage * 0.72f;
        float edge = noise - threshold;
        if (edge <= 0) return 0;
        return edge < 0.09f ? 0.72f : 1.0f;
    }

    private void updateRainSplashes(float dt) {
        for (int i = rainSplashes.size() - 1; i >= 0; i--) {
            RainSplash splash = rainSplashes.get(i);
            splash.age += dt;
            if (splash.age >= (splash.onWater ? 0.48f : 0.34f)) rainSplashes.remove(i);
        }
        if (rainIntensity < 0.08f) return;
        rainSplashTimer -= dt * rainIntensity;
        if (rainSplashTimer <= 0) {
            rainSplashTimer = 0.07f + weatherRandom.nextFloat() * 0.11f;
            spawnRainSplash();
        }
    }

    private void spawnRainSplash() {
        float angle = yaw + (weatherRandom.nextFloat() - 0.5f) * 1.35f;
        float distance = 4.0f + weatherRandom.nextFloat() * 21.0f;
        int x = (int) Math.floor(playerX + 0.5f + (float) Math.sin(angle) * distance);
        int z = (int) Math.floor(playerZ + 0.5f + (float) Math.cos(angle) * distance);
        if (x < 1 || z < 1 || x >= World.WIDTH - 1 || z >= World.DEPTH - 1) return;

        boolean onWater = world.get(x, World.WATER_LEVEL, z) == World.WATER;
        int surfaceY = onWater ? World.WATER_LEVEL : world.canopyHeightAt(x, z);
        if (!onWater && !World.isSolid(world.get(x, surfaceY, z))) return;
        if (!cloudAbovePosition(x + 0.5f, z + 0.5f)) return;

        if (rainSplashes.size() == MAX_RAIN_SPLASHES) rainSplashes.remove(0);
        rainSplashes.add(new RainSplash(x + 0.5f, surfaceY + 1.0f, z + 0.5f,
                onWater, weatherRandom.nextInt()));
    }

    private boolean cloudAbovePosition(float x, float z) {
        float dx = x - playerX, dz = z - playerZ, dy = 42.0f;
        float inverseLength = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return cloudOpacity(dy * inverseLength, dx * inverseLength, dz * inverseLength) > 0;
    }

    private static float sampleCloudMap(float x, float y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        float tx = x - x0, ty = y - y0;
        int wrappedX = x0 & (CLOUD_SIZE - 1), wrappedY = y0 & (CLOUD_SIZE - 1);
        int nextX = (wrappedX + 1) & (CLOUD_SIZE - 1), nextY = (wrappedY + 1) & (CLOUD_SIZE - 1);
        float top = lerp(CLOUD_MAP[wrappedY * CLOUD_SIZE + wrappedX],
                CLOUD_MAP[wrappedY * CLOUD_SIZE + nextX], tx);
        float bottom = lerp(CLOUD_MAP[nextY * CLOUD_SIZE + wrappedX],
                CLOUD_MAP[nextY * CLOUD_SIZE + nextX], tx);
        return lerp(top, bottom, ty);
    }

    private static float[] createCloudMap() {
        float[] map = new float[CLOUD_SIZE * CLOUD_SIZE];
        for (int y = 0; y < CLOUD_SIZE; y++) {
            for (int x = 0; x < CLOUD_SIZE; x++) {
                float broad = valueNoise(x * 0.018f, y * 0.038f, 0x6a09e667f3bcc909L);
                float detail = valueNoise(x * 0.055f, y * 0.105f, 0xbb67ae8584caa73bL);
                float bands = 0.5f + 0.5f * (float) Math.sin(y * 0.105f
                        + valueNoise(x * 0.028f, y * 0.025f, 0x3c6ef372fe94f82bL) * 3.2f);
                float cloud = broad * 0.52f + detail * 0.20f + bands * 0.28f;
                map[y * CLOUD_SIZE + x] = Math.round(cloud * 12.0f) / 12.0f;
            }
        }
        return map;
    }

    private static float valueNoise(float x, float y, long seed) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        float tx = smoothStep(x - x0), ty = smoothStep(y - y0);
        float top = noiseHash(x0, y0, seed) * (1 - tx) + noiseHash(x0 + 1, y0, seed) * tx;
        float bottom = noiseHash(x0, y0 + 1, seed) * (1 - tx) + noiseHash(x0 + 1, y0 + 1, seed) * tx;
        return top * (1 - ty) + bottom * ty;
    }

    private static float noiseHash(int x, int y, long seed) {
        long value = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL);
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        return (value >>> 40) / 16777215.0f;
    }

    private static float smoothStep(float value) { return value * value * (3 - 2 * value); }
    private static float clamp01(float value) { return Math.max(0, Math.min(1, value)); }
    private static float lerp(float from, float to, float amount) { return from + (to - from) * amount; }

    private static int lobbyCardY(int centerY) { return centerY - 218; }
    private static int lobbyLeftCardX(int centerX) { return centerX - 309; }
    private static int lobbyRightCardX(int centerX) { return centerX + 13; }
    private static int lobbyStartButtonX(int centerX) { return centerX - 165; }
    private static int lobbyStartButtonY(int centerY) { return centerY + 215; }

    private static Font[][] createUiFonts() {
        Font[][] fonts = new Font[2][40];
        for (int size = 1; size < fonts[0].length; size++) {
            fonts[0][size] = new Font("SansSerif", Font.PLAIN, size);
            fonts[1][size] = new Font("SansSerif", Font.BOLD, size);
        }
        return fonts;
    }

    private static Font uiFont(int style, int size) { return UI_FONTS[style == Font.BOLD ? 1 : 0][size]; }

    private static Color cachedColor(int red, int green, int blue) {
        return cachedColor(red, green, blue, 255);
    }

    private static Color cachedColor(int red, int green, int blue, int alpha) {
        int key = (alpha << 24) | (red << 16) | (green << 8) | blue;
        int start = (key * 0x9e3779b9) >>> 23;
        for (int probe = 0; probe < COLOR_CACHE.length; probe++) {
            int index = (start + probe) & (COLOR_CACHE.length - 1);
            if (COLOR_CACHE_KEYS[index] == key) return COLOR_CACHE[index];
            if (COLOR_CACHE_KEYS[index] == 0) {
                COLOR_CACHE_KEYS[index] = key;
                return COLOR_CACHE[index] = new Color(red, green, blue, alpha);
            }
        }
        return new Color(red, green, blue, alpha);
    }

    private void updateCreative(float dt) {
        float forward = (pressed(KeyEvent.VK_W) ? 1 : 0) - (pressed(KeyEvent.VK_S) ? 1 : 0);
        float strafe = (pressed(KeyEvent.VK_D) ? 1 : 0) - (pressed(KeyEvent.VK_A) ? 1 : 0);
        float length = (float) Math.sqrt(forward * forward + strafe * strafe);
        if (length > 0) {
            forward /= length;
            strafe /= length;
            float speed = pressed(KeyEvent.VK_SHIFT) ? 18.0f : 10.0f;
            playerX += ((float) Math.sin(yaw) * forward + (float) Math.cos(yaw) * strafe) * speed * dt;
            playerZ += ((float) Math.cos(yaw) * forward - (float) Math.sin(yaw) * strafe) * speed * dt;
        }
        float vertical = (pressed(KeyEvent.VK_SPACE) ? 1 : 0) - (pressed(KeyEvent.VK_CONTROL) ? 1 : 0);
        playerY += vertical * (pressed(KeyEvent.VK_SHIFT) ? 18.0f : 10.0f) * dt;
        playerX = Math.max(1.5f, Math.min(World.WIDTH - 1.5f, playerX));
        playerZ = Math.max(1.5f, Math.min(World.DEPTH - 1.5f, playerZ));
        playerY = Math.max(1.5f, Math.min(World.HEIGHT - 2.0f, playerY));
    }

    private void breedAnimals() {
        int[] populations = animalPopulationScratch;
        Arrays.fill(populations, 0);
        for (Animal animal : animals) if (animal.alive) populations[animal.kind.ordinal()]++;
        for (int i = 0; i < animals.length; i++) {
            Animal first = animals[i];
            if (!first.canBreed() || populations[first.kind.ordinal()] >= MAX_ANIMALS_PER_KIND) continue;
            for (int j = i + 1; j < animals.length; j++) {
                Animal second = animals[j];
                if (!second.canBreed() || first.kind != second.kind) continue;
                float dx = first.x - second.x, dz = first.z - second.z;
                float radius = first.kind.isFish() ? 2.8f : 2.2f;
                if (dx * dx + dz * dz > radius * radius || Math.abs(first.y - second.y) > 1.5f) continue;

                float babyX = (first.x + second.x) * 0.5f;
                float babyZ = (first.z + second.z) * 0.5f;
                int ground = world.heightAt((int) babyX, (int) babyZ);
                float babyY;
                if (first.kind.isFish()) {
                    if (ground > World.WATER_LEVEL - 3) continue;
                    babyY = Math.max(ground + 1.1f, World.WATER_LEVEL - 0.9f);
                } else {
                    if (ground <= World.WATER_LEVEL + 1 || world.get((int) babyX, ground + 1, (int) babyZ) != World.AIR
                            || world.get((int) babyX, ground + 2, (int) babyZ) != World.AIR) continue;
                    babyY = ground;
                }

                Animal baby = new Animal(first.kind, babyX, babyZ, babyY, animalRandom, true);
                int freeSlot = -1;
                for (int slot = 0; slot < animals.length; slot++) {
                    if (!animals[slot].alive) { freeSlot = slot; break; }
                }
                if (freeSlot >= 0) animals[freeSlot] = baby;
                else {
                    if (animals.length >= MAX_ANIMALS) return;
                    animals = Arrays.copyOf(animals, animals.length + 1);
                    animals[animals.length - 1] = baby;
                }
                first.beginBreedingCooldown(45.0f);
                second.beginBreedingCooldown(45.0f);
                return;
            }
        }
    }

    private void updateSurvivalVitals(float dt) {
        hunger = Math.max(0, hunger - 0.12f * dt);
        if (isHeadUnderwater()) oxygen = Math.max(0, oxygen - 14.0f * dt);
        else oxygen = Math.min(100, oxygen + 28.0f * dt);
        if (damageCooldown > 0) return;
        if (oxygen <= 0) takePlayerDamage(6, 0.9f);
        else if (hunger <= 0) takePlayerDamage(2, 1.8f);
    }

    private void updateAnimalDamage() {
        if (aggressionLevel < 70 || damageCooldown > 0) return;
        for (Animal animal : animals) {
            if (!animal.alive || animal.kind.isFish()) continue;
            float dx = animal.x - playerX, dz = animal.z - playerZ;
            if (dx * dx + dz * dz < 1.7f * 1.7f && Math.abs(animal.y - playerY) < 2.2f) {
                takePlayerDamage(2 + aggressionLevel / 20, 0.9f);
                return;
            }
        }
    }

    private void takePlayerDamage(int amount, float cooldown) {
        playerHealth = Math.max(0, playerHealth - amount);
        damageCooldown = cooldown;
        if (playerHealth == 0) respawnPlayer();
    }

    private void eatFood() {
        if (creativeMode || hunger >= 100) return;
        if (meatCount > 0 && hunger < 100) {
            meatCount--;
            restoreHunger(35);
            return;
        }
        byte[] edibleMushrooms = {World.BROWN_MUSHROOM_LARGE, World.BROWN_MUSHROOM_MEDIUM, World.BROWN_MUSHROOM_SMALL};
        for (byte mushroom : edibleMushrooms) {
            if (hunger >= 100) break;
            if (inventory[mushroom] <= 0) continue;
            inventory[mushroom]--;
            int foodValue = mushroom == World.BROWN_MUSHROOM_LARGE ? 28
                    : mushroom == World.BROWN_MUSHROOM_MEDIUM ? 19 : 11;
            restoreHunger(foodValue);
            return;
        }
        byte[] poisonousMushrooms = {World.RED_MUSHROOM_LARGE, World.RED_MUSHROOM_MEDIUM, World.RED_MUSHROOM_SMALL};
        for (byte mushroom : poisonousMushrooms) {
            if (inventory[mushroom] <= 0) continue;
            inventory[mushroom]--;
            int poison = mushroom == World.RED_MUSHROOM_LARGE ? 14
                    : mushroom == World.RED_MUSHROOM_MEDIUM ? 9 : 6;
            takePlayerDamage(poison, 0.5f);
            return;
        }
    }

    private void restoreHunger(int amount) {
        hunger = Math.min(100, hunger + amount);
        if (hunger > 50) playerHealth = Math.min(100, playerHealth + 4);
    }

    private int mushroomCount() {
        int count = 0;
        for (int type = World.RED_MUSHROOM_SMALL; type <= World.BROWN_MUSHROOM_LARGE; type++) {
            count += inventory[type];
        }
        return count;
    }

    private boolean isHeadUnderwater() {
        return world.get((int) Math.floor(playerX), (int) Math.floor(playerY + 1.62f),
                (int) Math.floor(playerZ)) == World.WATER;
    }

    private boolean isOnLadder() {
        int minX = (int) Math.floor(playerX - 0.34f), maxX = (int) Math.floor(playerX + 0.34f);
        int minY = (int) Math.floor(playerY + 0.1f), maxY = (int) Math.floor(playerY + 1.7f);
        int minZ = (int) Math.floor(playerZ - 0.34f), maxZ = (int) Math.floor(playerZ + 0.34f);
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    if (World.isLadderBlock(world.get(x, y, z))) return true;
                }
            }
        }
        return false;
    }

    private void respawnPlayer() {
        playerX = spawnX + 0.5f;
        playerZ = spawnZ + 0.5f;
        playerY = world.heightAt(spawnX, spawnZ) + 1.0f;
        verticalVelocity = 0;
        playerHealth = 100;
        hunger = 75;
        oxygen = 100;
    }

    private boolean isInWater() {
        int x = (int) Math.floor(playerX), z = (int) Math.floor(playerZ);
        return world.get(x, (int) Math.floor(playerY + 0.08f), z) == World.WATER
                || world.get(x, (int) Math.floor(playerY + 0.75f), z) == World.WATER
                || world.get(x, (int) Math.floor(playerY + 1.45f), z) == World.WATER;
    }

    private void tryMoveX(float dx, boolean allowStep, boolean allowSwimExit) {
        float next = playerX + dx;
        if (canOccupy(next, playerY, playerZ)) playerX = next;
        else if (allowStep) tryAutoStep(next, playerZ);
        else if (allowSwimExit) tryExitWater(next, playerZ);
    }

    private void tryMoveZ(float dz, boolean allowStep, boolean allowSwimExit) {
        float next = playerZ + dz;
        if (canOccupy(playerX, playerY, next)) playerZ = next;
        else if (allowStep) tryAutoStep(playerX, next);
        else if (allowSwimExit) tryExitWater(playerX, next);
    }

    private boolean canOccupy(float x, float feetY, float z) {
        int minX = (int) Math.floor(x - 0.23f), maxX = (int) Math.floor(x + 0.23f - 0.001f);
        int minY = (int) Math.floor(feetY + 0.15f), maxY = (int) Math.floor(feetY + 1.78f);
        int minZ = (int) Math.floor(z - 0.23f), maxZ = (int) Math.floor(z + 0.23f - 0.001f);
        for (int blockY = minY; blockY <= maxY; blockY++) {
            for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                for (int blockX = minX; blockX <= maxX; blockX++) {
                    if (solidAt(blockX + 0.5f, blockY + 0.5f, blockZ + 0.5f)) return false;
                }
            }
        }
        if (!creativeMode) {
            for (Animal animal : animals) {
                if (!animal.alive || animal.kind.isFish()) continue;
                float radius = animal.collisionRadius() + 0.23f;
                if (Math.abs(x - animal.x) < radius && Math.abs(z - animal.z) < radius
                        && feetY < animal.y + animal.collisionHeight() && feetY + 1.8f > animal.y) return false;
            }
        }
        return true;
    }

    private void tryAutoStep(float x, float z) {
        if (!isOnGround() || verticalVelocity > 0.1f) return;
        float stepY = (float) Math.floor(playerY - 0.08f) + 2.0f;
        if (stepY - playerY > 1.2f || stepY <= playerY) return;
        boolean hasFooting = solidAt(x - 0.23f, stepY - 0.08f, z)
                || solidAt(x + 0.23f, stepY - 0.08f, z)
                || solidAt(x, stepY - 0.08f, z - 0.23f)
                || solidAt(x, stepY - 0.08f, z + 0.23f);
        if (!hasFooting || !canOccupy(x, stepY, z)) return;
        playerX = x;
        playerZ = z;
        playerY = stepY;
        verticalVelocity = 0;
    }

    private void tryExitWater(float x, float z) {
        int surface = world.heightAt((int) x, (int) z);
        if (surface < World.WATER_LEVEL) return;
        float landingY = surface + 1.05f;
        if (landingY <= playerY || landingY - playerY > 5.2f) return;
        if (solidAt(x, landingY - 0.08f, z) && canOccupy(x, landingY, z)) {
            playerX = x;
            playerZ = z;
            playerY = landingY;
            verticalVelocity = 0;
        }
    }

    private boolean isOnGround() {
        return solidAt(playerX, playerY - 0.08f, playerZ);
    }

    private boolean solidAt(float x, float y, float z) {
        int blockX = (int) Math.floor(x), blockZ = (int) Math.floor(z);
        if (blockX < 1 || blockZ < 1 || blockX >= World.WIDTH - 1 || blockZ >= World.DEPTH - 1) return true;
        byte type = world.get(blockX, (int) Math.floor(y), blockZ);
        return World.isSolid(type);
    }

    private void render() {
        float eyeX = playerX, eyeY = playerY + 1.62f, eyeZ = playerZ;
        float maxDistance = renderDistance * RENDER_DISTANCE_UNIT;
        float cosYaw = (float) Math.cos(yaw), sinYaw = (float) Math.sin(yaw);
        float cosPitch = (float) Math.cos(pitch), sinPitch = (float) Math.sin(pitch);
        float fx = sinYaw * cosPitch, fy = sinPitch, fz = cosYaw * cosPitch;
        float rx = cosYaw, rz = -sinYaw;
        float ux = -sinYaw * sinPitch, uy = cosPitch, uz = -cosYaw * sinPitch;
        int visibleAnimalCount = findVisibleAnimals(eyeX, eyeY, eyeZ, fx, fy, fz);
        hasHit = false;
        aimedAnimal = null;
        aimedAnimalEntity = null;
        aimedBlockDistance = Float.POSITIVE_INFINITY;
        hasWaterHit = false;
        waterHitDistance = Float.POSITIVE_INFINITY;
        int centerIndex = (VIEW_HEIGHT / 2) * VIEW_WIDTH + VIEW_WIDTH / 2;

        IntStream.range(0, VIEW_HEIGHT).parallel().forEach(py -> {
            float sy = columnY[py];
            int index = py * VIEW_WIDTH;
            Hit rowTraceScratch = traceScratch[py];
            Hit rowUnderwaterScratch = underwaterTraceScratch[py];
            AnimalHit rowAnimalScratch = animalHitScratch[py];
            for (int px = 0; px < VIEW_WIDTH; px++, index++) {
                float sx = columnX[px];
                float dx = fx + rx * sx + ux * sy;
                float dy = fy + uy * sy;
                float dz = fz + rz * sx + uz * sy;
                float inverseLength = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx *= inverseLength; dy *= inverseLength; dz *= inverseLength;
                Hit hit = trace(eyeX, eyeY, eyeZ, dx, dy, dz, maxDistance, false, rowTraceScratch);
                AnimalHit animalHit = traceAnimals(eyeX, eyeY, eyeZ, dx, dy, dz, maxDistance,
                        visibleAnimalCount, rowAnimalScratch);
                if (hit.type == World.WATER) {
                    if (index == centerIndex) {
                        hasWaterHit = true;
                        waterHitX = hit.x; waterHitY = hit.y; waterHitZ = hit.z;
                        waterHitDistance = hit.distance;
                    }
                    if (animalHit != null && !animalHit.kind.isFish() && animalHit.distance < hit.distance) {
                        pixels[index] = shadeAnimal(animalHit);
                        if (index == centerIndex) {
                            aimedAnimal = animalHit.kind.label;
                            aimedAnimalEntity = animalHit.animal;
                            aimedAnimalDistance = animalHit.distance;
                        }
                        continue;
                    }
                    Hit underwaterHit = trace(eyeX, eyeY, eyeZ, dx, dy, dz, maxDistance, true, rowUnderwaterScratch);
                    float backgroundDistance = underwaterHit.type == World.AIR ? 25.0f : underwaterHit.distance;
                    float waterDepth = Math.max(0.0f, backgroundDistance - hit.distance);
                    int background;
                    if (animalHit != null && animalHit.kind.isFish()
                            && animalHit.distance > hit.distance && animalHit.distance < backgroundDistance) {
                        background = shadeAnimal(animalHit);
                        if (index == centerIndex) {
                            aimedAnimal = animalHit.kind.label;
                            aimedAnimalEntity = animalHit.animal;
                            aimedAnimalDistance = animalHit.distance;
                        }
                    } else if (underwaterHit.type == World.AIR) {
                        background = skyColor(dy, dx, dz);
                    } else {
                        background = shadeBlock(underwaterHit, eyeX, eyeY, eyeZ);
                        if (index == centerIndex) {
                            hasHit = true;
                            hitX = underwaterHit.x; hitY = underwaterHit.y; hitZ = underwaterHit.z;
                            hitNormalX = underwaterHit.nx; hitNormalY = underwaterHit.ny; hitNormalZ = underwaterHit.nz;
                            aimedBlockDistance = underwaterHit.distance;
                        }
                    }
                    pixels[index] = filterWater(background, hit, waterDepth);
                    continue;
                }
                if (animalHit != null && animalHit.distance < hit.distance) {
                    pixels[index] = shadeAnimal(animalHit);
                    if (index == centerIndex) {
                        aimedAnimal = animalHit.kind.label;
                        aimedAnimalEntity = animalHit.animal;
                        aimedAnimalDistance = animalHit.distance;
                    }
                    continue;
                }
                if (hit.type == World.AIR) {
                    pixels[index] = skyColor(dy, dx, dz);
                    continue;
                }
                if (index == centerIndex) {
                    hasHit = true;
                    hitX = hit.x; hitY = hit.y; hitZ = hit.z;
                    hitNormalX = hit.nx; hitNormalY = hit.ny; hitNormalZ = hit.nz;
                    aimedBlockDistance = hit.distance;
                }
                pixels[index] = shadeBlock(hit, eyeX, eyeY, eyeZ);
            }
        });
    }

    private Hit trace(float ox, float oy, float oz, float dx, float dy, float dz,
                      float maxDistance, boolean ignoreWater, Hit result) {
        int x = (int) Math.floor(ox), y = (int) Math.floor(oy), z = (int) Math.floor(oz);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        float deltaX = dx == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dx);
        float deltaY = dy == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dy);
        float deltaZ = dz == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dz);
        float nextX = dx > 0 ? x + 1 - ox : ox - x;
        float nextY = dy > 0 ? y + 1 - oy : oy - y;
        float nextZ = dz > 0 ? z + 1 - oz : oz - z;
        float maxX = dx == 0 ? Float.POSITIVE_INFINITY : nextX * deltaX;
        float maxY = dy == 0 ? Float.POSITIVE_INFINITY : nextY * deltaY;
        float maxZ = dz == 0 ? Float.POSITIVE_INFINITY : nextZ * deltaZ;
        float distance = 0;
        int nx = 0, ny = 0, nz = 0;
        while (distance < maxDistance) {
            byte type = world.get(x, y, z);
            if (type != World.AIR && !(ignoreWater && type == World.WATER)) {
                if (World.isFlower(type)) {
                    if (tracePlant(ox, oy, oz, dx, dy, dz, maxDistance, distance,
                            x, y, z, type, FLOWER_PARTS, result)) return result;
                } else if (World.isMushroom(type)) {
                    if (tracePlant(ox, oy, oz, dx, dy, dz, maxDistance, distance,
                            x, y, z, type, mushroomParts(type), result)) return result;
                } else if (World.isLadderBlock(type)) {
                    if (tracePlant(ox, oy, oz, dx, dy, dz, maxDistance, distance,
                            x, y, z, type, ladderParts(type), result)) return result;
                } else {
                    return result.set(x, y, z, type, type, nx, ny, nz, distance,
                            ox + dx * distance, oy + dy * distance, oz + dz * distance);
                }
            }
            if (maxX < maxY && maxX < maxZ) {
                distance = maxX; maxX += deltaX; x += stepX; nx = -stepX; ny = 0; nz = 0;
            } else if (maxY < maxZ) {
                distance = maxY; maxY += deltaY; y += stepY; nx = 0; ny = -stepY; nz = 0;
            } else {
                distance = maxZ; maxZ += deltaZ; z += stepZ; nx = 0; ny = 0; nz = -stepZ;
            }
            if (y < 0 || y >= World.HEIGHT || x < 0 || z < 0 || x >= World.WIDTH || z >= World.DEPTH) break;
        }
        return Hit.MISS;
    }

    private boolean tracePlant(float ox, float oy, float oz, float dx, float dy, float dz,
                               float maxDistance, float cellEntry, int cellX, int cellY, int cellZ,
                               byte plantType, float[][] plantParts, Hit result) {
        float localOx = ox - cellX, localOy = oy - cellY, localOz = oz - cellZ;
        float nearest = maxDistance;
        int normalX = 0, normalY = 0, normalZ = 0;
        byte visibleType = plantType;
        for (float[] part : plantParts) {
            byte partType = part[6] == 0 ? plantType : (byte) part[6];
            float distance = intersectBox(localOx, localOy, localOz, dx, dy, dz,
                    part[0], part[1], part[2], part[3], part[4], part[5], nearest);
            if (distance < cellEntry - 0.0001f || distance >= nearest) continue;
            nearest = distance;
            visibleType = partType;
            float px = localOx + dx * distance, py = localOy + dy * distance, pz = localOz + dz * distance;
            float face = Math.abs(px - part[0]);
            float candidate = Math.abs(px - part[3]);
            int nx = candidate < face ? 1 : -1, ny = 0, nz = 0;
            if (candidate < face) face = candidate;
            candidate = Math.abs(py - part[1]);
            if (candidate < face) { face = candidate; nx = 0; ny = -1; nz = 0; }
            candidate = Math.abs(py - part[4]);
            if (candidate < face) { face = candidate; nx = 0; ny = 1; nz = 0; }
            candidate = Math.abs(pz - part[2]);
            if (candidate < face) { face = candidate; nx = 0; ny = 0; nz = -1; }
            candidate = Math.abs(pz - part[5]);
            if (candidate < face) { nx = 0; ny = 0; nz = 1; }
            normalX = nx; normalY = ny; normalZ = nz;
        }
        if (nearest >= maxDistance) return false;
        result.set(cellX, cellY, cellZ, plantType, visibleType, normalX, normalY, normalZ, nearest,
                ox + dx * nearest, oy + dy * nearest, oz + dz * nearest);
        return true;
    }

    private static float[][] mushroomParts(byte type) {
        if (type == World.RED_MUSHROOM_SMALL || type == World.BROWN_MUSHROOM_SMALL) return SMALL_MUSHROOM_PARTS;
        if (type == World.RED_MUSHROOM_MEDIUM || type == World.BROWN_MUSHROOM_MEDIUM) return MEDIUM_MUSHROOM_PARTS;
        return LARGE_MUSHROOM_PARTS;
    }

    private static float[][] ladderParts(byte type) {
        return switch (type) {
            case World.LADDER_Z_NEG -> LADDER_Z_NEG_PARTS;
            case World.LADDER_X_POS -> LADDER_X_POS_PARTS;
            case World.LADDER_X_NEG -> LADDER_X_NEG_PARTS;
            default -> LADDER_Z_POS_PARTS;
        };
    }

    private static float[][] createLadderParts(boolean alongX, boolean highSide) {
        float wallMin = highSide ? 0.88f : 0.04f, wallMax = highSide ? 0.96f : 0.12f;
        float[][] parts = new float[7][7];
        if (alongX) {
            parts[0] = new float[]{wallMin, 0.0f, 0.12f, wallMax, 1.0f, 0.22f, 0};
            parts[1] = new float[]{wallMin, 0.0f, 0.78f, wallMax, 1.0f, 0.88f, 0};
            for (int rung = 0; rung < 5; rung++) {
                float bottom = 0.07f + rung * 0.21f;
                parts[rung + 2] = new float[]{wallMin, bottom, 0.18f, wallMax, bottom + 0.075f, 0.82f, 0};
            }
        } else {
            parts[0] = new float[]{0.12f, 0.0f, wallMin, 0.22f, 1.0f, wallMax, 0};
            parts[1] = new float[]{0.78f, 0.0f, wallMin, 0.88f, 1.0f, wallMax, 0};
            for (int rung = 0; rung < 5; rung++) {
                float bottom = 0.07f + rung * 0.21f;
                parts[rung + 2] = new float[]{0.18f, bottom, wallMin, 0.82f, bottom + 0.075f, wallMax, 0};
            }
        }
        return parts;
    }

    private int findVisibleAnimals(float ox, float oy, float oz, float fx, float fy, float fz) {
        int count = 0;
        float maxDistance = renderDistance * RENDER_DISTANCE_UNIT + 2.1f;
        for (Animal animal : animals) {
            if (!animal.alive) continue;
            float toX = animal.x - ox, toY = animal.y + 0.9f - oy, toZ = animal.z - oz;
            float distanceSquared = toX * toX + toY * toY + toZ * toZ;
            if (distanceSquared > maxDistance * maxDistance) continue;
            float forwardDistance = toX * fx + toY * fy + toZ * fz;
            if (forwardDistance + 2.1f < 0
                    || (forwardDistance > 0 && forwardDistance * forwardDistance < distanceSquared * 0.04f)) continue;
            if (count < visibleAnimals.length) {
                visibleAnimals[count] = animal;
                visibleAnimalCos[count] = (float) Math.cos(animal.yaw);
                visibleAnimalSin[count] = (float) Math.sin(animal.yaw);
                count++;
            }
        }
        return count;
    }

    private AnimalHit traceAnimals(float ox, float oy, float oz, float dx, float dy, float dz,
                                   float maxDistance, int visibleAnimalCount, AnimalHit result) {
        boolean found = false;
        float closestDistance = maxDistance;
        for (int animalIndex = 0; animalIndex < visibleAnimalCount; animalIndex++) {
            Animal animal = visibleAnimals[animalIndex];
            float toX = animal.x - ox, toY = animal.y + 0.9f - oy, toZ = animal.z - oz;
            float along = toX * dx + toY * dy + toZ * dz;
            float perpendicularSquared = toX * toX + toY * toY + toZ * toZ - along * along;
            if (perpendicularSquared > 4.41f || along + 2.1f < 0 || along - 2.1f > closestDistance) continue;

            float cos = visibleAnimalCos[animalIndex], sin = visibleAnimalSin[animalIndex];
            float localOx = -cos * toX + sin * toZ;
            float localOz = -sin * toX - cos * toZ;
            float localDx = cos * dx - sin * dz;
            float localDz = sin * dx + cos * dz;
            float localOy = oy - animal.y, localDy = dy;
            float modelScale = (animal.kind.isFish() ? 1.35f : 1.0f) * animal.growth;
            for (Animal.Part part : animal.parts) {
                float legSwing = part.gaitSwing * modelScale;
                float jawDrop = part.animatedJaw ? animal.mouthOpening() * modelScale : 0;
                float minX = part.minX * modelScale, maxX = part.maxX * modelScale;
                float minY = part.minY * modelScale - jawDrop, maxY = part.maxY * modelScale - jawDrop;
                float minZ = part.minZ * modelScale + legSwing, maxZ = part.maxZ * modelScale + legSwing;
                float distance = intersectBox(localOx, localOy, localOz, localDx, localDy, localDz,
                        minX, minY, minZ, maxX, maxY, maxZ, closestDistance);
                if (distance >= closestDistance) continue;

                float px = localOx + localDx * distance;
                float py = localOy + localDy * distance;
                float pz = localOz + localDz * distance;
                float nx = 0, ny = 0, nz = 0;
                float faceDistance = Math.abs(px - minX);
                float nearestFace = Math.abs(px - maxX);
                if (nearestFace < faceDistance) { faceDistance = nearestFace; nx = 1; }
                else nx = -1;
                nearestFace = Math.abs(py - minY);
                if (nearestFace < faceDistance) { faceDistance = nearestFace; nx = 0; ny = -1; nz = 0; }
                nearestFace = Math.abs(py - maxY);
                if (nearestFace < faceDistance) { faceDistance = nearestFace; nx = 0; ny = 1; nz = 0; }
                nearestFace = Math.abs(pz - minZ);
                if (nearestFace < faceDistance) { faceDistance = nearestFace; nx = 0; ny = 0; nz = -1; }
                nearestFace = Math.abs(pz - maxZ);
                if (nearestFace < faceDistance) { nx = 0; ny = 0; nz = 1; }

                float worldNx = cos * nx + sin * nz;
                float worldNz = -sin * nx + cos * nz;
                closestDistance = distance;
                result.set(animal, part.color, distance,
                        worldNx, ny, worldNz, ox + dx * distance, oy + dy * distance, oz + dz * distance);
                found = true;
            }
        }
        return found ? result : null;
    }

    private static float intersectBox(float ox, float oy, float oz, float dx, float dy, float dz,
                                      float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                      float maxDistance) {
        float near = 0, far = maxDistance;
        if (Math.abs(dx) < 1e-7f) { if (ox < minX || ox > maxX) return Float.POSITIVE_INFINITY; }
        else {
            float first = (minX - ox) / dx, second = (maxX - ox) / dx;
            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
        }
        if (Math.abs(dy) < 1e-7f) { if (oy < minY || oy > maxY) return Float.POSITIVE_INFINITY; }
        else {
            float first = (minY - oy) / dy, second = (maxY - oy) / dy;
            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
        }
        if (Math.abs(dz) < 1e-7f) { if (oz < minZ || oz > maxZ) return Float.POSITIVE_INFINITY; }
        else {
            float first = (minZ - oz) / dz, second = (maxZ - oz) / dz;
            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
        }
        return far >= near && far > 0 ? (near > 0 ? near : far) : Float.POSITIVE_INFINITY;
    }

    private int shadeAnimal(AnimalHit hit) {
        float diffuse = Math.max(0, hit.nx * sunX + hit.ny * sunY + hit.nz * sunZ);
        float lighting = 0.16f + sunlight * 0.35f + diffuse * (0.35f * sunlight);
        int red = (hit.color >> 16) & 255, green = (hit.color >> 8) & 255, blue = hit.color & 255;
        float fog = Math.max(0, Math.min(0.89f, (hit.distance - renderDistance * 4.0f) / (renderDistance * 16.0f)));
        return rgb(mix(red * lighting, 190, fog), mix(green * lighting, 219, fog), mix(blue * lighting, 221, fog));
    }

    private Animal[] createAnimals(long worldSeed) {
        Random random = new Random(worldSeed ^ 0x510e527fade682d1L);
        List<Animal> herd = new ArrayList<>();
        int centerX = spawnX, centerZ = spawnZ;
        for (int i = 0; i < 33; i++) {
            Animal.Kind kind = i < 6 ? Animal.Kind.SHEEP
                    : i < 10 ? Animal.Kind.FOX
                    : i < 16 ? Animal.Kind.CHICKEN
                    : i < 21 ? Animal.Kind.BOAR
                    : switch ((i - 21) % 3) {
                        case 0 -> Animal.Kind.PIG;
                        case 1 -> Animal.Kind.DEER;
                        default -> Animal.Kind.RABBIT;
                    };
            for (int attempt = 0; attempt < 80; attempt++) {
                float angle = random.nextFloat() * (float) (Math.PI * 2);
                float distance = 8.0f + random.nextFloat() * 18.0f;
                float x = centerX + 0.5f + (float) Math.sin(angle) * distance;
                float z = centerZ + 0.5f + (float) Math.cos(angle) * distance;
                int terrainHeight = world.heightAt((int) x, (int) z);
                boolean crowded = false;
                for (Animal other : herd) {
                    if (other != null && Math.hypot(other.x - x, other.z - z) < 3.0) {
                        crowded = true;
                        break;
                    }
                }
                if (terrainHeight > World.WATER_LEVEL + 1 && !crowded) {
                    herd.add(new Animal(kind, x, z, terrainHeight, random));
                    break;
                }
            }
        }

        List<int[]> deepWater = new ArrayList<>();
        for (int z = centerZ - 48; z <= centerZ + 48; z += 2) {
            for (int x = centerX - 48; x <= centerX + 48; x += 2) {
                if (world.heightAt(x, z) <= World.WATER_LEVEL - 3) deepWater.add(new int[]{x, z});
            }
        }
        if (deepWater.isEmpty()) return herd.toArray(Animal[]::new);

        int[] nearestLake = deepWater.get(0);
        for (int[] spot : deepWater) {
            if (Math.hypot(spot[0] - centerX, spot[1] - centerZ)
                    < Math.hypot(nearestLake[0] - centerX, nearestLake[1] - centerZ)) nearestLake = spot;
        }
        List<int[]> nearbyLake = new ArrayList<>();
        for (int[] spot : deepWater) {
            if (Math.hypot(spot[0] - nearestLake[0], spot[1] - nearestLake[1]) <= 9.0) nearbyLake.add(spot);
        }

        Animal.Kind[] fishKinds = {Animal.Kind.COD, Animal.Kind.SALMON, Animal.Kind.TROPICAL_FISH,
                Animal.Kind.PIKE, Animal.Kind.CARP};
        for (int i = 0; i < 25; i++) {
            Animal.Kind kind = fishKinds[i % fishKinds.length];
            for (int attempt = 0; attempt < 120; attempt++) {
                List<int[]> pool = i < 6 && !nearbyLake.isEmpty() ? nearbyLake : deepWater;
                int[] spot = pool.get(random.nextInt(pool.size()));
                float x = spot[0] + 0.5f + (random.nextFloat() - 0.5f) * 0.5f;
                float z = spot[1] + 0.5f + (random.nextFloat() - 0.5f) * 0.5f;
                boolean crowded = false;
                for (Animal other : herd) {
                    if (other.kind.isFish() && Math.hypot(other.x - x, other.z - z) < 1.7) {
                        crowded = true;
                        break;
                    }
                }
                if (!crowded) {
                    herd.add(new Animal(kind, x, z, World.WATER_LEVEL - 0.9f, random));
                    break;
                }
            }
        }
        return herd.toArray(Animal[]::new);
    }

    private int skyColor(float directionY, float directionX, float directionZ) {
        float t = Math.max(0, Math.min(1, (directionY + 0.28f) / 1.05f));
        float smooth = t * t * (3 - 2 * t);
        int dayR = mix(207, 72, smooth), dayG = mix(231, 141, smooth), dayB = mix(237, 207, smooth);
        int nightR = mix(31, 7, smooth), nightG = mix(43, 13, smooth), nightB = mix(78, 34, smooth);
        int r = mix(nightR, dayR, daylight), g = mix(nightG, dayG, daylight), b = mix(nightB, dayB, daylight);
        float twilight = clamp01((1.0f - Math.abs(sunY) * 3.8f) * (1.0f - cloudCoverage * 0.45f));
        r = mix(r, 238, twilight * 0.46f);
        g = mix(g, 133, twilight * 0.31f);
        b = mix(b, 97, twilight * 0.18f);

        float clouds = cloudOpacity(directionY, directionX, directionZ);
        float cloudBlend = clouds * (0.91f + rainIntensity * 0.09f);
        float cloudTone = clouds == 0 ? 0 : (sampleCloudMap(
                (directionX / Math.max(0.07f, directionY) * 0.20f + cloudOffsetX) * CLOUD_SIZE,
                (directionZ / Math.max(0.07f, directionY) * 0.20f + cloudOffsetY) * CLOUD_SIZE) - 0.5f) * 34.0f;
        int cloudR = mix(43, 152 + cloudTone, daylight);
        int cloudG = mix(52, 169 + cloudTone, daylight);
        int cloudB = mix(75, 190 + cloudTone, daylight);
        r = mix(r, cloudR, cloudBlend);
        g = mix(g, cloudG, cloudBlend);
        b = mix(b, cloudB, cloudBlend);

        float sunDot = directionX * sunX + directionY * sunY + directionZ * sunZ;
        float visibleSun = sunY > -0.08f ? 1.0f - clouds * 0.92f : 0.0f;
        float glow = clamp01((sunDot - 0.90f) * 4.5f) * daylight * visibleSun;
        float rayAzimuth = (float) Math.atan2(directionX, directionZ);
        float sunAzimuth = (float) Math.atan2(sunX, sunZ);
        float azimuthDelta = (float) Math.atan2(Math.sin(rayAzimuth - sunAzimuth), Math.cos(rayAzimuth - sunAzimuth));
        float rayElevation = (float) Math.asin(Math.max(-1, Math.min(1, directionY)));
        float sunElevation = (float) Math.asin(Math.max(-1, Math.min(1, sunY)));
        float squareSun = Math.abs(azimuthDelta) < 0.043f && Math.abs(rayElevation - sunElevation) < 0.043f
                ? visibleSun * daylight : 0;
        r = mix(r, 255, glow * 0.25f + squareSun * 0.98f);
        g = mix(g, 218, glow * 0.18f + squareSun * 0.88f);
        b = mix(b, 55, glow * 0.07f + squareSun * 0.82f);

        float moonDot = -(directionX * sunX + directionY * sunY + directionZ * sunZ);
        float moon = clamp01((moonDot - 0.996f) * 180) * (1.0f - daylight) * (1.0f - clouds);
        r = mix(r, 211, moon);
        g = mix(g, 224, moon);
        b = mix(b, 255, moon);
        float stars = starIntensity(directionY, directionX, directionZ, clouds);
        return rgb(mix(r, 226, stars), mix(g, 237, stars), mix(b, 255, stars));
    }

    private float starIntensity(float directionY, float directionX, float directionZ, float clouds) {
        if (daylight > 0.18f || directionY < 0.025f || clouds > 0.72f) return 0;
        float azimuth = (float) Math.atan2(directionX, directionZ);
        float elevation = (float) Math.asin(Math.max(-1, Math.min(1, directionY)));
        float gridX = azimuth * 160.0f, gridY = elevation * 160.0f;
        int cellX = (int) Math.floor(gridX), cellY = (int) Math.floor(gridY);
        float starSeed = noiseHash(cellX, cellY, 0x3c6ef372fe94f82bL);
        if (starSeed < 0.9988f) return 0;
        float localX = gridX - cellX - noiseHash(cellX, cellY, 0xa54ff53a5f1d36f1L);
        float localY = gridY - cellY - noiseHash(cellX, cellY, 0x510e527fade682d1L);
        float distance = (float) Math.sqrt(localX * localX + localY * localY);
        float core = clamp01((0.38f - distance) * 5.0f);
        float twinkle = 0.72f + (float) Math.sin(rainPhase * 0.035f + starSeed * 6.28f) * 0.28f;
        return core * twinkle * (1.0f - clouds * 0.92f) * (1.0f - daylight * 5.5f);
    }

    private int shadeBlock(Hit hit, float eyeX, float eyeY, float eyeZ) {
        byte type = hit.visualType;
        float texture = textureNoise(hit);
        int baseColor = BlockRegistry.info(type).shadedColor();
        if (type == World.GRASS) {
            if (hit.ny == 0 && hit.py - hit.y > 0.84f) baseColor = 0x55853f;
            else if (hit.ny == 1) baseColor = 0x579d45;
            else baseColor = 0x6b5b3d;
        } else if (World.isMushroom(hit.type) && type != World.SNOW) {
            boolean redCap = hit.type <= World.RED_MUSHROOM_LARGE;
            if (redCap) baseColor = texture > 0.82f ? 0xf3e3c6 : 0xbe3836;
            else baseColor = texture > 0.86f ? 0xe0c397 : 0x97673e;
        }
        float baseR = (baseColor >>> 16) & 0xff;
        float baseG = (baseColor >>> 8) & 0xff;
        float baseB = baseColor & 0xff;

        float diffuse = Math.max(0, hit.nx * sunX + hit.ny * sunY + hit.nz * sunZ);
        float normalLight = 0.16f + sunlight * 0.35f + diffuse * (0.43f * sunlight);
        if (hit.ny == -1) normalLight *= 0.62f;
        float heightAmbient = Math.max(0.78f, Math.min(1.0f, 0.78f + hit.y * 0.004f));
        float lighting = normalLight * heightAmbient * (0.92f + texture * 0.15f);
        lighting *= 1.0f - rainIntensity * 0.16f;
        if (hit.y < world.heightAt(hit.x, hit.z) - 3) lighting *= 0.34f;
        if (type != World.WATER && inSunShadow(hit)) lighting *= 0.68f;
        int torchLight = world.torchLightAt(hit.x, hit.y, hit.z);
        if (torchLight > 0) lighting = Math.max(lighting, 0.12f + torchLight / 15.0f * 0.88f);
        if (type == World.WATER) lighting *= 1.16f;
        float distance = hit.distance;
        float fog = Math.max(0, Math.min(0.89f, (distance - renderDistance * 4.0f) / (renderDistance * 16.0f)));
        float fogR = mix(31, 190, daylight), fogG = mix(43, 219, daylight), fogB = mix(73, 221, daylight);
        return rgb(mix(baseR * lighting, fogR, fog), mix(baseG * lighting, fogG, fog), mix(baseB * lighting, fogB, fog));
    }

    private int filterWater(int background, Hit entry, float depth) {
        float ripple = (float) Math.sin(entry.px * 2.1f + entry.pz * 1.7f) * 0.035f;
        float opacity = Math.min(0.62f, 0.20f + depth * 0.035f);
        float waterRed = 24 + ripple * 90;
        float waterGreen = 125 + ripple * 150;
        float waterBlue = 157 + ripple * 120;
        int red = (background >> 16) & 255;
        int green = (background >> 8) & 255;
        int blue = background & 255;
        return rgb(mix(red, waterRed, opacity), mix(green, waterGreen, opacity), mix(blue, waterBlue, opacity));
    }

    private boolean inSunShadow(Hit hit) {
        if (sunlight < 0.12f || sunY <= 0) return false;
        for (float distance = 1.5f; distance < 6.0f; distance += 2.2f) {
            int x = (int) Math.floor(hit.px + sunX * distance);
            int y = (int) Math.floor(hit.py + sunY * distance);
            int z = (int) Math.floor(hit.pz + sunZ * distance);
            byte block = world.get(x, y, z);
            if (World.isSolid(block)) return true;
        }
        return false;
    }

    private float textureNoise(Hit hit) {
        int iu = (int) Math.floor(hit.px * 7.0f), iv = (int) Math.floor(hit.py * 7.0f), iw = (int) Math.floor(hit.pz * 7.0f);
        long value = (iu * 0x9E3779B97F4A7C15L) ^ (iv * 0xC2B2AE3D27D4EB4FL) ^ (iw * 0x165667B19E3779F9L);
        value ^= value >>> 30; value *= 0xBF58476D1CE4E5B9L; value ^= value >>> 27;
        return ((value >>> 40) & 0xffff) / 65535.0f;
    }

    private synchronized void drawFrame() {
        if (!isDisplayable() || getWidth() <= 0 || getHeight() <= 0) return;
        BufferStrategy strategy = getBufferStrategy();
        if (strategy == null) {
            try {
                recreateBufferStrategy();
                strategy = getBufferStrategy();
            } catch (IllegalStateException unavailable) {
                return;
            }
            if (strategy == null) return;
        }
        int canvasWidth = getWidth(), canvasHeight = getHeight();
        do {
            do {
                Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(scene, 0, 0, canvasWidth, canvasHeight, null);
                g.scale(canvasWidth / (double) WINDOW_WIDTH, canvasHeight / (double) WINDOW_HEIGHT);
                if (!inLobby && !paused) {
                    drawRain(g);
                    drawRainSplashes(g);
                }
                drawHud(g);
                g.dispose();
            } while (strategy.contentsRestored());
            strategy.show();
            Toolkit.getDefaultToolkit().sync();
        } while (strategy.contentsLost());
    }

    private void recreateBufferStrategy() {
        BufferStrategy previous = getBufferStrategy();
        if (previous != null) previous.dispose();
        if (isDisplayable()) createBufferStrategy(2);
    }

    private void drawHud(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = WINDOW_WIDTH / 2, cy = WINDOW_HEIGHT / 2;
        if (inLobby) {
            drawLobby(g, cx, cy);
            drawSaveMessage(g);
            return;
        }
        if (inventoryOpen) {
            drawInventoryDialog(g, cx, cy);
            drawSaveMessage(g);
            return;
        }
        drawEnvironmentBadge(g, cx);
        g.setColor(cachedColor(255, 255, 255, 210));
        g.setStroke(new BasicStroke(2.2f));
        g.drawLine(cx - 10, cy, cx - 3, cy); g.drawLine(cx + 3, cy, cx + 10, cy);
        g.drawLine(cx, cy - 10, cx, cy - 3); g.drawLine(cx, cy + 3, cx, cy + 10);
        if (!creativeMode) drawSurvivalIndicators(g);
        if (aimedAnimal != null) {
            g.setFont(uiFont(Font.BOLD, 14));
            g.setColor(cachedColor(7, 17, 20, 170));
            int labelWidth = g.getFontMetrics().stringWidth(aimedAnimal) + 20;
            g.fillRoundRect(cx - labelWidth / 2, cy + 18, labelWidth, 23, 10, 10);
            g.setColor(cachedColor(245, 244, 221));
            g.drawString(aimedAnimal, cx - g.getFontMetrics().stringWidth(aimedAnimal) / 2, cy + 34);
        }

        g.setFont(uiFont(Font.BOLD, 17));
        g.setColor(cachedColor(8, 19, 27, 145));
        g.fillRoundRect(23, 23, 340, 73, 18, 18);
        g.setColor(Color.WHITE);
        g.drawString("TYTANCRAFT", 40, 51);
        g.setFont(uiFont(Font.PLAIN, 14));
        g.setColor(cachedColor(224, 238, 235));
        g.drawString(creativeMode ? "Kreatywny  •  " + fps + " FPS"
                : "Przetrwanie  •  HP " + playerHealth + "  •  " + fps + " FPS", 40, 77);

        int slotSize = 56, gap = 4, total = hotbar.length * slotSize + (hotbar.length - 1) * gap;
        int startX = (WINDOW_WIDTH - total) / 2, y = WINDOW_HEIGHT - 95;
        for (int i = 0; i < hotbar.length; i++) {
            int x = startX + i * (slotSize + gap);
            g.setColor(cachedColor(7, 16, 22, 168));
            g.fillRoundRect(x, y, slotSize, slotSize, 12, 12);
            g.setColor(i == selectedBlock ? cachedColor(247, 226, 156) : cachedColor(255, 255, 255, 120));
            g.setStroke(new BasicStroke(i == selectedBlock ? 3 : 1));
            g.drawRoundRect(x, y, slotSize, slotSize, 12, 12);
            drawBlockIcon(g, x + 11, y + 15, hotbar[i]);
            g.setColor(cachedColor(245, 247, 242));
            g.setFont(uiFont(Font.BOLD, 12));
            if (i < 9) g.drawString(Integer.toString(i + 1), x + 3, y + 14);
            else g.drawString("0", x + 3, y + 14);
        }
        g.setFont(uiFont(Font.PLAIN, 14));
        byte selectedType = hotbar[selectedBlock];
        String selectedName = blockName(selectedType)
                + (creativeMode ? "" : " ×" + inventory[selectedType & 0xff]);
        g.setColor(cachedColor(10, 20, 24, 165));
        int labelWidth = g.getFontMetrics().stringWidth(selectedName) + 26;
        g.fillRoundRect(cx - labelWidth / 2, y - 31, labelWidth, 23, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(selectedName, cx - g.getFontMetrics().stringWidth(selectedName) / 2, y - 14);

        g.setFont(uiFont(Font.PLAIN, 14));
        g.setColor(cachedColor(255, 255, 255, 224));
        if (isInWater()) {
            g.drawString("PŁYWANIE   SPACJA — góra   CTRL — dół   W+SPACJA — wyjdź", 26, WINDOW_HEIGHT - 57);
        }
        g.drawString(String.format("X %d   Y %d   Z %d", (int) playerX, (int) playerY, (int) playerZ), 26, WINDOW_HEIGHT - 32);
        if (!captured || paused) drawStartOverlay(g, cx, cy);
        drawSaveMessage(g);
    }

    private void drawSaveMessage(Graphics2D g) {
        if (saveMessageTimer <= 0 || saveMessage.isEmpty()) return;
        g.setFont(uiFont(Font.BOLD, 14));
        int width = g.getFontMetrics().stringWidth(saveMessage) + 28;
        int x = (WINDOW_WIDTH - width) / 2;
        int top = inLobby ? 539 : 24;
        g.setColor(cachedColor(8, 20, 20, 220));
        g.fillRoundRect(x, top, width, 34, 10, 10);
        g.setColor(saveMessage.startsWith("Nie") ? cachedColor(255, 176, 153) : cachedColor(218, 241, 201));
        g.drawString(saveMessage, x + 14, top + 22);
    }

    private void drawInventoryDialog(Graphics2D g, int cx, int cy) {
        g.setColor(cachedColor(4, 10, 15, 175));
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        int x = WINDOW_WIDTH / 2 - 485, y = WINDOW_HEIGHT / 2 - 315;
        g.setColor(cachedColor(198, 198, 198));
        g.fillRect(x, y, INVENTORY_WIDTH, INVENTORY_HEIGHT);
        g.setColor(cachedColor(58, 58, 58));
        g.drawRect(x, y, INVENTORY_WIDTH - 1, INVENTORY_HEIGHT - 1);
        g.setColor(Color.WHITE);
        g.drawLine(x + 1, y + 1, x + INVENTORY_WIDTH - 2, y + 1);
        g.drawLine(x + 1, y + 1, x + 1, y + INVENTORY_HEIGHT - 2);
        g.setColor(cachedColor(105, 105, 105));
        g.drawLine(x + 2, y + INVENTORY_HEIGHT - 2, x + INVENTORY_WIDTH - 2, y + INVENTORY_HEIGHT - 2);
        g.drawLine(x + INVENTORY_WIDTH - 2, y + 2, x + INVENTORY_WIDTH - 2, y + INVENTORY_HEIGHT - 2);
        g.setFont(MONOSPACED_BOLD_14);
        g.setColor(cachedColor(46, 46, 46));
        String title = inventoryTab == 3 ? "Crafting" : "Inventory";
        g.drawString(title, cx - g.getFontMetrics().stringWidth(title) / 2, y + 39);

        int tabY = y + 55;
        String[] tabs = {"BLOKI", "NATURA", "SPAWN", "CRAFTING"};
        for (int i = 0; i < tabs.length; i++) {
            drawClassicTabButton(g, x + 24 + i * 208, tabY, 200, 36,
                    tabs[i], inventoryTab == i);
        }
        g.setColor(cachedColor(60, 60, 60));
        g.setFont(uiFont(Font.PLAIN, 12));
        g.drawString("ESC / E — zamknij", x + 850, tabY + 23);
        g.setColor(cachedColor(142, 142, 142));
        g.fillRect(x + INVENTORY_WIDTH - 51, y + 15, 34, 32);
        g.setColor(Color.WHITE);
        g.drawRect(x + INVENTORY_WIDTH - 51, y + 15, 33, 31);
        g.setColor(cachedColor(46, 46, 46));
        g.setFont(uiFont(Font.BOLD, 20));
        g.drawString("×", x + INVENTORY_WIDTH - 41, y + 38);

        if (inventoryTab == 3) drawCraftingDialog(g, x, y);
        else drawHotbarDialog(g, x, y, inventoryItems(inventoryTab), tabs[inventoryTab]);
    }

    private void drawClassicTabButton(Graphics2D g, int x, int y, int width, int height,
                                      String label, boolean selected) {
        g.setColor(selected ? cachedColor(188, 188, 188) : cachedColor(158, 158, 158));
        g.fillRect(x, y, width, height);
        g.setColor(Color.WHITE);
        g.drawLine(x, y, x + width - 1, y);
        g.drawLine(x, y, x, y + height - 1);
        g.setColor(cachedColor(65, 65, 65));
        g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
        g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
        g.setColor(cachedColor(45, 45, 45));
        g.setFont(uiFont(Font.BOLD, 10));
        g.drawString(label, x + (width - g.getFontMetrics().stringWidth(label)) / 2,
                y + (height + g.getFontMetrics().getAscent()) / 2 - 2);
    }

    private byte[] inventoryItems(int tab) {
        return switch (tab) {
            case 0 -> BLOCKS_TAB;
            case 1 -> NATURE_TAB;
            default -> SPAWN_TAB;
        };
    }

    private void drawHotbarDialog(Graphics2D g, int panelX, int panelY, byte[] items, String category) {
        g.setColor(cachedColor(52, 52, 52));
        g.setFont(MONOSPACED_BOLD_14);
        g.setFont(MONOSPACED_BOLD_14);
        g.setColor(cachedColor(52, 52, 52));
        g.drawString("Available: " + category, panelX + 30, panelY + 124);
        int slotSize = CRAFT_INVENTORY_SLOT_SIZE, gap = CRAFT_INVENTORY_GAP;
        int gridWidth = 9 * slotSize + 8 * gap;
        int gridX = panelX + (INVENTORY_WIDTH - gridWidth) / 2, gridY = panelY + 140;
        for (int i = 0; i < items.length; i++) {
            int col = i % 9, row = i / 9;
            int x = gridX + col * (slotSize + gap), y = gridY + row * (slotSize + gap);
            boolean selected = items[i] == hotbar[customizeSlot];
            drawClassicSlot(g, x, y, slotSize, items[i], creativeMode ? 0 : inventory[items[i] & 0xff], selected);
        }
        g.setFont(MONOSPACED_BOLD_14);
        g.setColor(cachedColor(52, 52, 52));
        g.drawString("Hotbar", panelX + 30, panelY + 530);
        int hotbarWidth = hotbar.length * CRAFT_INVENTORY_SLOT_SIZE + (hotbar.length - 1) * CRAFT_INVENTORY_GAP;
        int hotbarX = panelX + (INVENTORY_WIDTH - hotbarWidth) / 2;
        int hotbarY = panelY + 542;
        for (int slot = 0; slot < hotbar.length; slot++) {
            int x = hotbarX + slot * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
            drawClassicSlot(g, x, hotbarY, CRAFT_INVENTORY_SLOT_SIZE, hotbar[slot],
                    creativeMode ? 0 : inventory[hotbar[slot] & 0xff], slot == customizeSlot);
            g.setFont(uiFont(Font.BOLD, 10));
            g.setColor(cachedColor(45, 45, 45));
            g.drawString(slot == 9 ? "0" : Integer.toString(slot + 1), x + 3, hotbarY + 11);
        }
        g.setFont(uiFont(Font.PLAIN, 12));
        g.setColor(cachedColor(52, 52, 52));
        String selectedItem = blockName(hotbar[customizeSlot]);
        g.drawString("Wybierz slot hotbara, potem przedmiot. Wybrano: " + selectedItem,
                panelX + 30, panelY + 625);
    }

    private void drawCraftingDialog(Graphics2D g, int panelX, int panelY) {
        g.setColor(cachedColor(48, 48, 48));
        g.setFont(MONOSPACED_BOLD_14);
        g.drawString("Crafting", panelX + 32, panelY + 130);
        g.drawString("Recipes", panelX + CRAFT_RECIPE_X, panelY + 130);

        for (int recipe = 0; recipe < CRAFT_OUTPUTS.length; recipe++) {
            int x = panelX + CRAFT_RECIPE_X + recipe * CRAFT_RECIPE_STEP;
            int y = panelY + CRAFT_RECIPE_Y;
            drawClassicButton(g, x, y, CRAFT_RECIPE_WIDTH, CRAFT_RECIPE_HEIGHT, selectedRecipe == recipe);
            drawBlockIcon(g, x + (CRAFT_RECIPE_WIDTH - 40) / 2, y + 4, CRAFT_OUTPUTS[recipe]);
            g.setColor(cachedColor(35, 35, 35));
            g.setFont(uiFont(Font.BOLD, 10));
            String label = shorten(CRAFT_RECIPE_NAMES[recipe], 12);
            g.drawString(label, x + (CRAFT_RECIPE_WIDTH - g.getFontMetrics().stringWidth(label)) / 2, y + 66);
        }

        int gridX = panelX + CRAFT_GRID_X, gridY = panelY + CRAFT_GRID_Y;
        for (int slot = 0; slot < 9; slot++) {
            int x = gridX + (slot % 3) * (CRAFT_SLOT_SIZE + CRAFT_SLOT_GAP);
            int y = gridY + (slot / 3) * (CRAFT_SLOT_SIZE + CRAFT_SLOT_GAP);
            drawClassicSlot(g, x, y, CRAFT_SLOT_SIZE, craftingIngredient(selectedRecipe, slot), -1, false);
        }

        int arrowY = gridY + (3 * CRAFT_SLOT_SIZE + 2 * CRAFT_SLOT_GAP) / 2;
        g.setColor(cachedColor(132, 132, 132));
        g.fillRect(panelX + 305, arrowY - 4, 92, 8);
        g.fillPolygon(new int[]{panelX + 397, panelX + 421, panelX + 397},
                new int[]{arrowY - 14, arrowY, arrowY + 14}, 3);

        int outputX = panelX + CRAFT_OUTPUT_X;
        int outputY = gridY + (3 * CRAFT_SLOT_SIZE + 2 * CRAFT_SLOT_GAP - CRAFT_OUTPUT_SIZE) / 2;
        drawClassicSlot(g, outputX, outputY, CRAFT_OUTPUT_SIZE, CRAFT_OUTPUTS[selectedRecipe],
                CRAFT_OUTPUT_COUNTS[selectedRecipe], creativeMode || canCraftRecipe(selectedRecipe));

        g.setFont(MONOSPACED_BOLD_14);
        g.setColor(cachedColor(48, 48, 48));
        g.drawString("Inventory", panelX + 32, panelY + 350);
        byte[] items = craftingInventoryItems();
        int inventoryGridWidth = 9 * CRAFT_INVENTORY_SLOT_SIZE + 8 * CRAFT_INVENTORY_GAP;
        int inventoryX = panelX + (INVENTORY_WIDTH - inventoryGridWidth) / 2;
        int inventoryY = panelY + CRAFT_INVENTORY_TOP;
        for (int slot = 0; slot < 27; slot++) {
            int x = inventoryX + (slot % 9) * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
            int y = inventoryY + (slot / 9) * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
            byte item = slot < items.length ? items[slot] : World.AIR;
            drawClassicSlot(g, x, y, CRAFT_INVENTORY_SLOT_SIZE, item,
                    item == World.AIR ? 0 : inventory[item & 0xff], false);
        }

        int hotbarWidth = hotbar.length * CRAFT_INVENTORY_SLOT_SIZE + (hotbar.length - 1) * CRAFT_INVENTORY_GAP;
        int hotbarX = panelX + (INVENTORY_WIDTH - hotbarWidth) / 2;
        int hotbarY = panelY + CRAFT_HOTBAR_TOP;
        for (int slot = 0; slot < hotbar.length; slot++) {
            int x = hotbarX + slot * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
            drawClassicSlot(g, x, hotbarY, CRAFT_INVENTORY_SLOT_SIZE, hotbar[slot],
                    creativeMode ? 0 : inventory[hotbar[slot] & 0xff], slot == customizeSlot);
        }
        g.setFont(uiFont(Font.PLAIN, 12));
        g.setColor(cachedColor(55, 55, 55));
        String hint = craftMessage.isEmpty() ? "Kliknij recepturę, potem wynik, aby wytworzyć przedmiot." : craftMessage;
        g.drawString(hint, panelX + 300, panelY + 350);
    }

    private byte[] craftingInventoryItems() {
        byte[] items = new byte[27];
        int count = 0;
        for (byte type : BLOCK_TYPES) {
            if ((creativeMode || inventory[type & 0xff] > 0) && count < items.length) items[count++] = type;
        }
        return Arrays.copyOf(items, count);
    }

    private static byte craftingIngredient(int recipe, int slot) {
        if (recipe == 0) return slot == 4 ? World.WOOD : World.AIR;
        if (recipe == 1) return slot == 1 || slot == 4 ? World.PLANKS : slot == 7 ? World.COAL_ORE : World.AIR;
        return World.EMERALD_ORE;
    }

    private void drawClassicSlot(Graphics2D g, int x, int y, int size, byte item, int count, boolean selected) {
        g.setColor(cachedColor(55, 55, 55));
        g.fillRect(x, y, size, size);
        g.setColor(Color.WHITE);
        g.drawLine(x + 1, y + 1, x + size - 2, y + 1);
        g.drawLine(x + 1, y + 1, x + 1, y + size - 2);
        g.setColor(cachedColor(83, 83, 83));
        g.drawLine(x + 1, y + size - 2, x + size - 2, y + size - 2);
        g.drawLine(x + size - 2, y + 1, x + size - 2, y + size - 2);
        g.setColor(cachedColor(139, 139, 139));
        g.fillRect(x + 3, y + 3, size - 6, size - 6);
        if (selected) {
            g.setColor(cachedColor(255, 230, 92));
            g.drawRect(x + 2, y + 2, size - 5, size - 5);
        }
        if (item == World.AIR) return;
        drawBlockIcon(g, x + (size - 40) / 2, y + (size - 36) / 2, item);
        if (count > 1 || creativeMode && count == 0) {
            String amount = count > 1 ? Integer.toString(count) : "∞";
            g.setFont(uiFont(Font.BOLD, 11));
            int textX = x + size - 5 - g.getFontMetrics().stringWidth(amount);
            g.setColor(cachedColor(40, 40, 40));
            g.drawString(amount, textX + 1, y + size - 5 + 1);
            g.setColor(Color.WHITE);
            g.drawString(amount, textX, y + size - 6);
        }
    }

    private void drawClassicButton(Graphics2D g, int x, int y, int width, int height, boolean selected) {
        g.setColor(selected ? cachedColor(179, 179, 179) : cachedColor(166, 166, 166));
        g.fillRect(x, y, width, height);
        g.setColor(selected ? cachedColor(80, 80, 80) : Color.WHITE);
        g.drawLine(x, y, x + width - 1, y);
        g.drawLine(x, y, x, y + height - 1);
        g.setColor(selected ? Color.WHITE : cachedColor(72, 72, 72));
        g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
        g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
    }

    private static String shorten(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
    }

    private boolean canCraftRecipe(int recipe) {
        if (recipe == 0) return hasWoodLog();
        if (recipe == 1) return inventory[World.PLANKS] >= 2 && inventory[World.COAL_ORE] >= 1;
        return inventory[World.EMERALD_ORE] >= 9;
    }

    private boolean hasWoodLog() {
        return inventory[World.WOOD] + inventory[World.BIRCH_WOOD] + inventory[World.PINE_WOOD]
                + inventory[World.ACACIA_WOOD] + inventory[World.CHERRY_WOOD] > 0;
    }

    private void craftRecipe(int recipe) {
        int output;
        byte result;
        if (recipe == 0) {
            result = World.PLANKS;
            output = 4;
            if (!creativeMode) {
                byte[] woods = {World.WOOD, World.BIRCH_WOOD, World.PINE_WOOD, World.ACACIA_WOOD, World.CHERRY_WOOD};
                for (byte wood : woods) if (inventory[wood] > 0) { inventory[wood]--; break; }
            }
        } else if (recipe == 1) {
            if (!creativeMode) { inventory[World.PLANKS] -= 2; inventory[World.COAL_ORE]--; }
            result = World.TORCH;
            output = 4;
        } else {
            if (!creativeMode) inventory[World.EMERALD_ORE] -= 9;
            result = World.EMERALD_BLOCK;
            output = 1;
        }
        inventory[result] = Math.min(999, inventory[result] + output);
        craftMessage = "Wytworzono: " + blockName(result) + " ×" + output;
    }

    private void handleInventoryClick(int mouseX, int mouseY) {
        pendingActions.add(() -> processInventoryClick(mouseX, mouseY));
    }

    private void processInventoryClick(int mouseX, int mouseY) {
        int x = INVENTORY_PANEL_X, y = INVENTORY_PANEL_Y;
        if (inside(mouseX, mouseY, x + INVENTORY_WIDTH - 51, y + 15, 34, 32)) {
            closeInventory();
            return;
        }
        int tabY = y + 55;
        for (int tab = 0; tab < 4; tab++) {
            if (inside(mouseX, mouseY, x + 24 + tab * 208, tabY, 200, 36)) {
                inventoryTab = tab;
                return;
            }
        }
        if (inventoryTab == 3) {
            for (int recipe = 0; recipe < CRAFT_OUTPUTS.length; recipe++) {
                int recipeX = x + CRAFT_RECIPE_X + recipe * CRAFT_RECIPE_STEP;
                int recipeY = y + CRAFT_RECIPE_Y;
                if (inside(mouseX, mouseY, recipeX, recipeY, CRAFT_RECIPE_WIDTH, CRAFT_RECIPE_HEIGHT)) {
                    selectedRecipe = recipe;
                    craftMessage = "";
                    return;
                }
            }

            int outputX = x + CRAFT_OUTPUT_X;
            int outputY = y + CRAFT_GRID_Y
                    + (3 * CRAFT_SLOT_SIZE + 2 * CRAFT_SLOT_GAP - CRAFT_OUTPUT_SIZE) / 2;
            if (inside(mouseX, mouseY, outputX, outputY, CRAFT_OUTPUT_SIZE, CRAFT_OUTPUT_SIZE)) {
                if (creativeMode || canCraftRecipe(selectedRecipe)) craftRecipe(selectedRecipe);
                else craftMessage = "Brakuje składników do tej receptury.";
                return;
            }

            int hotbarWidth = hotbar.length * CRAFT_INVENTORY_SLOT_SIZE
                    + (hotbar.length - 1) * CRAFT_INVENTORY_GAP;
            int hotbarX = x + (INVENTORY_WIDTH - hotbarWidth) / 2;
            int hotbarY = y + CRAFT_HOTBAR_TOP;
            for (int slot = 0; slot < hotbar.length; slot++) {
                int tileX = hotbarX + slot * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
                if (inside(mouseX, mouseY, tileX, hotbarY,
                        CRAFT_INVENTORY_SLOT_SIZE, CRAFT_INVENTORY_SLOT_SIZE)) {
                    customizeSlot = slot;
                    selectedBlock = slot;
                    return;
                }
            }

            byte[] items = craftingInventoryItems();
            int inventoryGridWidth = 9 * CRAFT_INVENTORY_SLOT_SIZE + 8 * CRAFT_INVENTORY_GAP;
            int inventoryX = x + (INVENTORY_WIDTH - inventoryGridWidth) / 2;
            int inventoryY = y + CRAFT_INVENTORY_TOP;
            for (int slot = 0; slot < items.length; slot++) {
                int tileX = inventoryX + (slot % 9) * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
                int tileY = inventoryY + (slot / 9) * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
                if (inside(mouseX, mouseY, tileX, tileY,
                        CRAFT_INVENTORY_SLOT_SIZE, CRAFT_INVENTORY_SLOT_SIZE)) {
                    hotbar[customizeSlot] = items[slot];
                    selectedBlock = customizeSlot;
                    return;
                }
            }
            return;
        }

        int hotbarWidth = hotbar.length * CRAFT_INVENTORY_SLOT_SIZE + (hotbar.length - 1) * CRAFT_INVENTORY_GAP;
        int hotbarX = x + (INVENTORY_WIDTH - hotbarWidth) / 2, hotbarY = y + 542;
        for (int slot = 0; slot < hotbar.length; slot++) {
            int tileX = hotbarX + slot * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
            if (inside(mouseX, mouseY, tileX, hotbarY,
                    CRAFT_INVENTORY_SLOT_SIZE, CRAFT_INVENTORY_SLOT_SIZE)) {
                customizeSlot = slot;
                selectedBlock = slot;
                return;
            }
        }
        int gridWidth = 9 * CRAFT_INVENTORY_SLOT_SIZE + 8 * CRAFT_INVENTORY_GAP;
        int catalogX = x + (INVENTORY_WIDTH - gridWidth) / 2, catalogY = y + 140;
        byte[] items = inventoryItems(inventoryTab);
        for (int i = 0; i < items.length; i++) {
            int tileX = catalogX + (i % 9) * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
            int tileY = catalogY + (i / 9) * (CRAFT_INVENTORY_SLOT_SIZE + CRAFT_INVENTORY_GAP);
            if (inside(mouseX, mouseY, tileX, tileY,
                    CRAFT_INVENTORY_SLOT_SIZE, CRAFT_INVENTORY_SLOT_SIZE)) {
                hotbar[customizeSlot] = items[i];
                selectedBlock = customizeSlot;
                return;
            }
        }
    }

    private void closeInventory() {
        inventoryOpen = false;
        captureMouse();
    }

    private void drawEnvironmentBadge(Graphics2D g, int centerX) {
        int minutes = (int) (dayTime * 1440) % 1440;
        String time = String.format("%02d:%02d", minutes / 60, minutes % 60);
        String conditions = rainIntensity > 0.35f ? "Deszcz"
                : cloudCoverage > 0.42f ? "Pochmurno" : daylight < 0.12f ? "Noc" : "Słonecznie";
        String label = time + "  •  " + conditions + "  •  " + String.format("%.1fx", daySpeed);
        int x = centerX - 132, y = 22;
        g.setColor(cachedColor(8, 19, 27, 150));
        g.fillRoundRect(x, y, 264, 34, 14, 14);
        g.setFont(uiFont(Font.BOLD, 14));
        g.setColor(cachedColor(240, 244, 231));
        g.drawString(label, centerX - g.getFontMetrics().stringWidth(label) / 2, y + 22);
    }

    private void drawRain(Graphics2D g) {
        if (rainIntensity < 0.08f) return;
        int x = (int) Math.floor(playerX), z = (int) Math.floor(playerZ);
        if (playerY < world.canopyHeightAt(x, z) - 1.0f) return;
        Graphics2D rain = (Graphics2D) g.create();
        rain.scale((double) WINDOW_WIDTH / VIEW_WIDTH, (double) WINDOW_HEIGHT / VIEW_HEIGHT);
        rain.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        rain.setColor(cachedColor(174, 216, 236, (int) (145 * rainIntensity)));
        int driftX = (int) (rainPhase * 0.35f);
        int driftY = (int) (rainPhase * 1.1f);
        int drops = (int) (150 * rainIntensity);
        float cosYaw = (float) Math.cos(yaw), sinYaw = (float) Math.sin(yaw);
        float cosPitch = (float) Math.cos(pitch), sinPitch = (float) Math.sin(pitch);
        float fx = sinYaw * cosPitch, fy = sinPitch, fz = cosYaw * cosPitch;
        float rx = cosYaw, rz = -sinYaw;
        float ux = -sinYaw * sinPitch, uy = cosPitch, uz = -cosYaw * sinPitch;
        int drawn = 0;
        for (int attempt = 0; attempt < drops * 8 && drawn < drops; attempt++) {
            int sourceX = Math.min(VIEW_WIDTH - 1, (int) (noiseHash(attempt, 17,
                    seed ^ 0x6a09e667f3bcc909L) * VIEW_WIDTH));
            int sourceY = Math.min(VIEW_HEIGHT - 1, (int) (noiseHash(attempt, 43,
                    seed ^ 0xbb67ae8584caa73bL) * VIEW_HEIGHT));
            if (cloudOpacityAtScreen(sourceX, sourceY, fx, fy, fz, rx, rz, ux, uy, uz) <= 0) continue;
            int dropX = Math.floorMod(sourceX + driftX, VIEW_WIDTH);
            int dropY = Math.floorMod(sourceY + driftY, VIEW_HEIGHT);
            int length = 3 + (int) (noiseHash(attempt, 71, seed ^ 0x3c6ef372fe94f82bL) * 4);
            rain.fillRect(dropX, dropY, 1, length);
            drawn++;
        }
        rain.dispose();
    }

    private float cloudOpacityAtScreen(int px, int py, float fx, float fy, float fz,
                                       float rx, float rz, float ux, float uy, float uz) {
        float sx = columnX[px], sy = columnY[py];
        float dx = fx + rx * sx + ux * sy;
        float dy = fy + uy * sy;
        float dz = fz + rz * sx + uz * sy;
        float inverseLength = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return cloudOpacity(dy * inverseLength, dx * inverseLength, dz * inverseLength);
    }

    private void drawRainSplashes(Graphics2D g) {
        if (rainSplashes.isEmpty()) return;
        float eyeX = playerX, eyeY = playerY + 1.62f, eyeZ = playerZ;
        float cosYaw = (float) Math.cos(yaw), sinYaw = (float) Math.sin(yaw);
        float cosPitch = (float) Math.cos(pitch), sinPitch = (float) Math.sin(pitch);
        float fx = sinYaw * cosPitch, fy = sinPitch, fz = cosYaw * cosPitch;
        float rx = cosYaw, rz = -sinYaw;
        float ux = -sinYaw * sinPitch, uy = cosPitch, uz = -cosYaw * sinPitch;
        float focalLength = (VIEW_HEIGHT * 0.5f) / (float) Math.tan(FOV * 0.5f);

        for (RainSplash splash : rainSplashes) {
            float dx = splash.x - eyeX, dy = splash.y - eyeY, dz = splash.z - eyeZ;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float depth = dx * fx + dy * fy + dz * fz;
            if (depth <= 0.1f) continue;
            float screenRight = (dx * rx + dz * rz) / depth;
            float screenUp = (dx * ux + dy * uy + dz * uz) / depth;
            float pixelX = VIEW_WIDTH * 0.5f + screenRight * focalLength;
            float pixelY = VIEW_HEIGHT * 0.5f - screenUp * focalLength;
            if (pixelX < 0 || pixelY < 0 || pixelX >= VIEW_WIDTH || pixelY >= VIEW_HEIGHT) continue;

            float inverseLength = 1.0f / distance;
            Hit surface = trace(eyeX, eyeY, eyeZ, dx * inverseLength, dy * inverseLength, dz * inverseLength,
                    distance + 0.7f, false, traceScratch[0]);
            if (surface.type == World.AIR) continue;

            int x = Math.round(pixelX * WINDOW_WIDTH / VIEW_WIDTH);
            int y = Math.round(pixelY * WINDOW_HEIGHT / VIEW_HEIGHT);
            boolean waterImpact = surface.type == World.WATER;
            float lifetime = waterImpact ? 0.48f : 0.34f;
            float progress = Math.min(1.0f, splash.age / lifetime);
            int alpha = Math.max(0, Math.round((waterImpact ? 175 : 205) * (1.0f - progress)));
            g.setColor(waterImpact ? cachedColor(164, 224, 239, alpha) : cachedColor(205, 230, 238, alpha));
            g.setStroke(RAIN_SPLASH_STROKE);
            if (waterImpact) {
                int radius = 2 + Math.round(progress * 7);
                g.drawOval(x - radius, y - Math.max(1, radius / 3), radius * 2, Math.max(2, radius / 2));
                if (progress < 0.35f) g.drawArc(x - radius - 2, y - radius, radius * 2 + 4, radius * 2, 205, 125);
            } else {
                for (int particle = 0; particle < 3; particle++) {
                    float angle = ((splash.randomBits >>> (particle * 8)) & 0xff) * ((float) (Math.PI * 2) / 256.0f);
                    int endX = x + Math.round((float) Math.sin(angle) * (2.0f + progress * 6.0f));
                    int endY = y - Math.round(1.0f + progress * 5.0f + Math.abs((float) Math.cos(angle)) * 3.0f);
                    g.drawLine(x, y, endX, endY);
                }
            }
        }
    }

    private void drawSurvivalIndicators(Graphics2D g) {
        boolean underwater = isHeadUnderwater();
        int panelX = WINDOW_WIDTH - 330, panelY = 16;
        g.setColor(cachedColor(8, 19, 27, 168));
        g.fillRoundRect(panelX, panelY, 310, underwater ? 99 : 69, 14, 14);
        int startX = panelX + 9;
        for (int i = 0; i < 10; i++) {
            drawHeartIcon(g, startX + i * 21, panelY + 8, playerHealth >= (i + 1) * 10);
            drawMeatIcon(g, startX + i * 21, panelY + 32, hunger >= (i + 1) * 10);
            if (underwater) drawBubbleIcon(g, startX + i * 21, panelY + 69, oxygen >= (i + 1) * 10);
        }
        g.setFont(uiFont(Font.BOLD, 11));
        g.setColor(cachedColor(250, 245, 233));
        g.drawString("M×" + meatCount, panelX + 226, panelY + 42);
        g.drawString("G×" + mushroomCount(), panelX + 226, panelY + 59);
    }

    private void drawHeartIcon(Graphics2D g, int x, int y, boolean filled) {
        Path2D heart = new Path2D.Float();
        heart.moveTo(x + 9, y + 17);
        heart.curveTo(x + 7, y + 15, x, y + 10, x, y + 6);
        heart.curveTo(x, y + 1, x + 6, y, x + 9, y + 5);
        heart.curveTo(x + 12, y, x + 18, y + 1, x + 18, y + 6);
        heart.curveTo(x + 18, y + 10, x + 11, y + 15, x + 9, y + 17);
        heart.closePath();
        g.setColor(filled ? cachedColor(221, 58, 72) : cachedColor(89, 59, 66));
        g.fill(heart);
        g.setColor(cachedColor(255, 225, 222, 180));
        g.draw(heart);
    }

    private void drawMeatIcon(Graphics2D g, int x, int y, boolean filled) {
        g.setColor(filled ? cachedColor(171, 73, 58) : cachedColor(88, 69, 65));
        g.fillOval(x + 5, y + 3, 13, 11);
        g.setColor(filled ? cachedColor(239, 173, 143) : cachedColor(155, 147, 139));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x + 4, y + 12, x + 1, y + 15);
        g.fillOval(x, y + 12, 5, 5);
        g.fillOval(x + 3, y + 15, 5, 5);
        if (filled) {
            g.setColor(cachedColor(248, 205, 164));
            g.fillOval(x + 9, y + 5, 4, 3);
        }
    }

    private void drawBubbleIcon(Graphics2D g, int x, int y, boolean filled) {
        g.setColor(filled ? cachedColor(103, 210, 236) : cachedColor(78, 111, 126));
        g.fillOval(x + 2, y + 2, 15, 15);
        g.setColor(filled ? cachedColor(220, 250, 255) : cachedColor(155, 181, 189));
        g.setStroke(new BasicStroke(1.4f));
        g.drawOval(x + 2, y + 2, 15, 15);
        if (filled) g.fillOval(x + 6, y + 5, 4, 4);
    }

    private void drawLobby(Graphics2D g, int cx, int cy) {
        g.setColor(cachedColor(5, 13, 17, 142));
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        int x = cx - 340, y = cy - 350, width = 680, height = 700;
        drawClassicPanel(g, x, y, width, height, 198);

        g.setColor(cachedColor(45, 45, 45));
        g.setFont(MONOSPACED_BOLD_34);
        String title = "TYTANCRAFT";
        g.drawString(title, cx - g.getFontMetrics().stringWidth(title) / 2, y + 59);
        g.setColor(cachedColor(67, 67, 67));
        g.setFont(MONOSPACED_PLAIN_12);
        String subtitle = "LOBBY  /  PRZYGOTUJ SIĘ DO WYPRAWY";
        g.drawString(subtitle, cx - g.getFontMetrics().stringWidth(subtitle) / 2, y + 87);
        g.setColor(cachedColor(128, 128, 128));
        g.drawLine(x + 30, y + 108, x + width - 30, y + 108);

        int cardY = lobbyCardY(cy), cardHeight = LOBBY_CARD_HEIGHT, cardWidth = LOBBY_CARD_WIDTH;
        int leftX = lobbyLeftCardX(cx), rightX = lobbyRightCardX(cx);
        drawLobbyCard(g, leftX, cardY, cardWidth, cardHeight, "GRACZ");
        drawLobbyCard(g, rightX, cardY, cardWidth, cardHeight, "ŚWIAT");

        g.setFont(uiFont(Font.BOLD, 12));
        g.setColor(cachedColor(50, 50, 50));
        g.drawString("NAZWA GRACZA", leftX + 18, cardY + 57);
        int nameX = leftX + LOBBY_NICKNAME_X, nameY = cardY + LOBBY_NICKNAME_Y;
        drawClassicPanel(g, nameX, nameY, LOBBY_NICKNAME_WIDTH, LOBBY_NICKNAME_HEIGHT,
                nicknameFocused ? 224 : 194);
        g.setColor(cachedColor(35, 35, 35));
        g.setFont(MONOSPACED_BOLD_14);
        String name = nickname.isEmpty() ? "Wpisz nick..." : nickname;
        g.drawString(name, nameX + 15, nameY + 32);
        g.setFont(MONOSPACED_PLAIN_12);
        g.setColor(cachedColor(65, 65, 65));
        g.drawString("LOKALNY GRACZ", leftX + 19, cardY + 158);
        g.setColor(cachedColor(88, 88, 88));
        g.fillOval(leftX + 19, cardY + 177, 10, 10);
        g.setFont(uiFont(Font.BOLD, 15));
        g.setColor(cachedColor(43, 43, 43));
        g.drawString(nickname.isEmpty() ? "Gracz" : nickname, leftX + 38, cardY + 188);
        g.setFont(MONOSPACED_PLAIN_12);
        g.setColor(cachedColor(72, 72, 72));
        g.drawString("GOTOWY", leftX + cardWidth - 82, cardY + 188);

        g.setFont(uiFont(Font.BOLD, 10));
        g.setColor(cachedColor(57, 57, 57));
        g.drawString("ZAPISANE ŚWIATY · F5 W GRZE", leftX + 18, cardY + 218);
        int visibleSaves = Math.min(5, Math.max(0, savedWorlds.size() - savedWorldOffset));
        if (visibleSaves == 0) {
            g.setFont(uiFont(Font.PLAIN, 12));
            g.setColor(cachedColor(83, 83, 83));
            g.drawString("Nowy świat zapisze się po zamknięciu gry", leftX + 18, cardY + 246);
        }
        for (int row = 0; row < visibleSaves; row++) {
            WorldStorage.SaveInfo save = savedWorlds.get(savedWorldOffset + row);
            int buttonY = cardY + LOBBY_SAVED_LIST_Y + row * LOBBY_SAVED_ROW_STEP;
            boolean selected = loadedSave && save.seed == seed;
            drawClassicButton(g, leftX + 15, buttonY, cardWidth - 30, LOBBY_SAVED_ROW_HEIGHT, selected);
            g.setFont(uiFont(Font.BOLD, 11));
            g.setColor(cachedColor(42, 42, 42));
            String label = "Świat " + Long.toUnsignedString(save.seed);
            g.drawString(label, leftX + 27, buttonY + 18);
            g.setFont(uiFont(Font.PLAIN, 9));
            g.setColor(cachedColor(65, 65, 65));
            String hint = selected ? "WCZYTANO" : "KLIKNIJ, ABY WCZYTAĆ";
            g.drawString(hint, leftX + cardWidth - 25 - g.getFontMetrics().stringWidth(hint), buttonY + 18);
        }
        if (savedWorlds.size() > 5) {
            g.setFont(uiFont(Font.PLAIN, 9));
            g.setColor(cachedColor(83, 83, 83));
            g.drawString("Kółko myszy: starsze / nowsze zapisy", leftX + 18, cardY + 388);
        }

        g.setFont(uiFont(Font.BOLD, 12));
        g.setColor(cachedColor(52, 52, 52));
        g.drawString("TRYB GRY", rightX + 18, cardY + 55);
        int modeY = cardY + LOBBY_MODE_Y;
        drawClassicTabButton(g, rightX + LOBBY_SURVIVAL_X, modeY, LOBBY_SURVIVAL_WIDTH,
                LOBBY_MODE_HEIGHT, "PRZETRWANIE", !creativeMode);
        drawClassicTabButton(g, rightX + LOBBY_CREATIVE_X, modeY, LOBBY_CREATIVE_WIDTH,
                LOBBY_MODE_HEIGHT, "KREATYWNY", creativeMode);
        g.setFont(uiFont(Font.PLAIN, 12));
        g.setColor(cachedColor(67, 67, 67));
        g.drawString(creativeMode ? "Lot • nieograniczone bloki" : "Zbieraj bloki • dbaj o zdrowie", rightX + 18, cardY + 119);
        g.setColor(cachedColor(135, 135, 135));
        g.drawLine(rightX + 18, cardY + 132, rightX + cardWidth - 18, cardY + 132);
        g.setFont(uiFont(Font.PLAIN, 11));
        g.setColor(cachedColor(65, 65, 65));
        g.drawString("ZIARNO ŚWIATA", rightX + 18, cardY + 152);
        g.setFont(MONOSPACED_BOLD_14);
        g.setColor(cachedColor(42, 42, 42));
        g.drawString(Long.toUnsignedString(seed), rightX + 18, cardY + 174);
        g.setFont(uiFont(Font.BOLD, 11));
        g.setColor(cachedColor(55, 55, 55));
        g.drawString("AGRESYWNOŚĆ ZWIERZĄT", rightX + 18, cardY + 198);
        String aggressionValue = creativeMode ? "—" : aggressionLevel + "%";
        g.drawString(aggressionValue, rightX + cardWidth - 18 - g.getFontMetrics().stringWidth(aggressionValue), cardY + 198);
        int sliderX = cx + LOBBY_SLIDER_X, sliderY = cardY + LOBBY_AGGRESSION_Y;
        int sliderWidth = LOBBY_SLIDER_WIDTH;
        drawClassicSliderTrack(g, sliderX, sliderY, sliderWidth);
        int knobX = sliderX + sliderWidth * aggressionLevel / 100;
        g.setColor(cachedColor(83, 83, 83));
        g.fillRect(sliderX, sliderY + 1, knobX - sliderX, 4);
        drawClassicButton(g, knobX - 8, sliderY - 5, 16, 16, false);
        g.setFont(uiFont(Font.PLAIN, 10));
        g.setColor(cachedColor(70, 70, 70));
        g.drawString(creativeMode ? "Dostępne w trybie przetrwania"
                : aggressionLevel < 35 ? "Spokojne" : aggressionLevel < 70 ? "Płochliwe" : "Atakujące",
                rightX + 18, cardY + 235);

        g.setFont(uiFont(Font.BOLD, 11));
        g.setColor(cachedColor(55, 55, 55));
        g.drawString("SZYBKOŚĆ PÓR DNIA", rightX + 18, cardY + 258);
        String speedLabel = String.format("%.1fx", daySpeed);
        g.drawString(speedLabel, rightX + cardWidth - 18 - g.getFontMetrics().stringWidth(speedLabel), cardY + 258);
        int timeSliderX = cx + LOBBY_SLIDER_X, timeSliderY = cardY + LOBBY_DAY_SPEED_Y;
        int timeSliderWidth = LOBBY_SLIDER_WIDTH;
        drawClassicSliderTrack(g, timeSliderX, timeSliderY, timeSliderWidth);
        int timeKnobX = timeSliderX + Math.round(timeSliderWidth * daySpeed / 8.0f);
        g.setColor(cachedColor(83, 83, 83));
        g.fillRect(timeSliderX, timeSliderY + 1, timeKnobX - timeSliderX, 4);
        drawClassicButton(g, timeKnobX - 8, timeSliderY - 5, 16, 16, false);
        g.setFont(uiFont(Font.PLAIN, 10));
        g.setColor(cachedColor(70, 70, 70));
        g.drawString("0x — zatrzymany", rightX + 18, cardY + 299);
        String maxSpeed = "8x";
        g.drawString(maxSpeed, rightX + cardWidth - 18 - g.getFontMetrics().stringWidth(maxSpeed), cardY + 299);

        g.setFont(uiFont(Font.BOLD, 11));
        g.setColor(cachedColor(55, 55, 55));
        g.drawString("ZASIĘG RENDEROWANIA", rightX + 18, cardY + 326);
        String distanceLabel = renderDistance + "x";
        g.drawString(distanceLabel, rightX + cardWidth - 18 - g.getFontMetrics().stringWidth(distanceLabel), cardY + 326);
        int distanceSliderY = cardY + LOBBY_RENDER_DISTANCE_Y;
        drawClassicSliderTrack(g, timeSliderX, distanceSliderY, timeSliderWidth);
        int distanceKnobX = timeSliderX + (renderDistance - 2) * timeSliderWidth / 6;
        g.setColor(cachedColor(83, 83, 83));
        g.fillRect(timeSliderX, distanceSliderY + 1, distanceKnobX - timeSliderX, 4);
        drawClassicButton(g, distanceKnobX - 8, distanceSliderY - 5, 16, 16, false);
        g.setFont(uiFont(Font.PLAIN, 10));
        g.setColor(cachedColor(70, 70, 70));
        g.drawString("2–8x widoku  •  dalej obraz znika we mgle", rightX + 18, cardY + 368);

        int buttonX = lobbyStartButtonX(cx), buttonY = lobbyStartButtonY(cy);
        int buttonWidth = LOBBY_START_BUTTON_WIDTH, buttonHeight = LOBBY_START_BUTTON_HEIGHT;
        drawClassicButton(g, buttonX, buttonY, buttonWidth, buttonHeight, false);
        g.setColor(cachedColor(40, 40, 40));
        g.setFont(MONOSPACED_BOLD_14);
        String startLabel = "WEJDŹ DO ŚWIATA";
        g.drawString(startLabel, cx - g.getFontMetrics().stringWidth(startLabel) / 2, buttonY + 39);
        g.setFont(MONOSPACED_PLAIN_12);
        g.setColor(cachedColor(62, 62, 62));
        String hint = "Kliknij pole, aby zmienić nick  •  ENTER rozpoczyna grę";
        g.drawString(hint, cx - g.getFontMetrics().stringWidth(hint) / 2, y + height - 20);
    }

    private void drawLobbyCard(Graphics2D g, int x, int y, int width, int height, String title) {
        drawClassicPanel(g, x, y, width, height, 178);
        g.setColor(cachedColor(47, 47, 47));
        g.setFont(MONOSPACED_BOLD_14);
        g.drawString(title, x + 18, y + 29);
    }

    private void drawClassicPanel(Graphics2D g, int x, int y, int width, int height, int fill) {
        g.setColor(cachedColor(fill, fill, fill));
        g.fillRect(x, y, width, height);
        g.setColor(Color.WHITE);
        g.drawLine(x, y, x + width - 1, y);
        g.drawLine(x, y, x, y + height - 1);
        g.setColor(cachedColor(58, 58, 58));
        g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
        g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
        g.setColor(cachedColor(88, 88, 88));
        g.drawRect(x + 1, y + 1, width - 3, height - 3);
    }

    private void drawClassicSliderTrack(Graphics2D g, int x, int y, int width) {
        g.setColor(cachedColor(75, 75, 75));
        g.fillRect(x, y, width, 7);
        g.setColor(cachedColor(225, 225, 225));
        g.drawLine(x + 1, y + 1, x + width - 2, y + 1);
        g.setColor(cachedColor(130, 130, 130));
        g.drawLine(x + 1, y + 5, x + width - 2, y + 5);
    }

    private void setAggressionFromMouse(int mouseX) {
        int sliderX = WINDOW_WIDTH / 2 + LOBBY_SLIDER_X;
        int sliderWidth = LOBBY_SLIDER_WIDTH;
        aggressionLevel = Math.max(0, Math.min(100, (mouseX - sliderX) * 100 / sliderWidth));
    }

    private void setDaySpeedFromMouse(int mouseX) {
        int sliderX = WINDOW_WIDTH / 2 + LOBBY_SLIDER_X;
        int sliderWidth = LOBBY_SLIDER_WIDTH;
        float value = Math.max(0, Math.min(8, (mouseX - sliderX) * 8.0f / sliderWidth));
        daySpeed = Math.round(value * 10.0f) / 10.0f;
    }

    private void setRenderDistanceFromMouse(int mouseX) {
        int sliderX = WINDOW_WIDTH / 2 + LOBBY_SLIDER_X;
        int sliderWidth = LOBBY_SLIDER_WIDTH;
        float proportion = Math.max(0, Math.min(1, (mouseX - sliderX) / (float) sliderWidth));
        renderDistance = 2 + Math.round(proportion * 6);
    }

    private void enterWorld() {
        if (nickname.isEmpty()) nickname = "Gracz";
        if (!loadedSave) {
            Arrays.fill(inventory, 0);
            if (!creativeMode) {
                inventory[World.GRASS] = 32;
                inventory[World.DIRT] = 32;
                inventory[World.STONE] = 24;
                inventory[World.SAND] = 16;
                inventory[World.WOOD] = 16;
                inventory[World.LEAVES] = 16;
                inventory[World.COD_SPAWN_EGG] = 2;
                inventory[World.SALMON_SPAWN_EGG] = 2;
                inventory[World.TROPICAL_FISH_SPAWN_EGG] = 2;
                inventory[World.PIKE_SPAWN_EGG] = 2;
                inventory[World.CARP_SPAWN_EGG] = 2;
            }
        }
        nicknameFocused = false;
        inLobby = false;
        paused = false;
        captureMouse();
    }

    private void refreshSavedWorlds() {
        try {
            savedWorlds = WorldStorage.list();
        } catch (java.io.IOException error) {
            savedWorlds = List.of();
            saveMessage = "Nie można odczytać listy zapisów";
        }
    }

    private void loadSavedWorld(WorldStorage.SaveInfo save) {
        try {
            WorldStorage.Data data = WorldStorage.load(save.path);
            World loadedWorld = new World(data.seed, data.blocks, data.waterFlow, data.waterQueue);
            Animal[] loadedAnimals = new Animal[data.animals.size()];
            Random loadRandom = new Random(data.seed ^ 0x6a09e667f3bcc909L);
            for (int i = 0; i < loadedAnimals.length; i++) {
                WorldStorage.AnimalState state = data.animals.get(i);
                Animal animal = new Animal(state.kind, state.x, state.z, state.y, loadRandom);
                animal.alive = state.alive;
                animal.health = state.health;
                animal.x = state.x;
                animal.y = state.y;
                animal.z = state.z;
                animal.yaw = state.yaw;
                animal.gait = state.gait;
                animal.updatePartAnimation();
                animal.verticalVelocity = state.verticalVelocity;
                animal.growth = state.growth;
                animal.setBreedingCooldown(state.breedingCooldown);
                loadedAnimals[i] = animal;
            }

            seed = data.seed;
            world = loadedWorld;
            spawnX = data.spawnX;
            spawnZ = data.spawnZ;
            creativeMode = data.creativeMode;
            aggressionLevel = data.aggressionLevel;
            renderDistance = data.renderDistance;
            playerHealth = data.playerHealth;
            meatCount = data.meatCount;
            selectedBlock = data.selectedBlock;
            weather = Weather.values()[data.weather];
            daySpeed = data.daySpeed;
            playerX = data.playerX;
            playerY = data.playerY;
            playerZ = data.playerZ;
            yaw = data.yaw;
            pitch = data.pitch;
            hunger = data.hunger;
            oxygen = data.oxygen;
            dayTime = data.dayTime;
            weatherTimer = data.weatherTimer;
            cloudCoverage = data.cloudCoverage;
            targetCloudCoverage = data.targetCloudCoverage;
            rainIntensity = data.rainIntensity;
            targetRainIntensity = data.targetRainIntensity;
            cloudOffsetX = data.cloudOffsetX;
            cloudOffsetY = data.cloudOffsetY;
            rainPhase = data.rainPhase;
            nickname = data.nickname;
            System.arraycopy(data.inventory, 0, inventory, 0, inventory.length);
            System.arraycopy(data.hotbar, 0, hotbar, 0, hotbar.length);
            animals = loadedAnimals;
            animalRandom = new Random(seed ^ 0x6a09e667f3bcc909L);
            weatherRandom = new Random(seed ^ 0xbb67ae8584caa73bL);
            loadedSave = true;
            updateSunPosition();
            saveMessage = "Wczytano świat " + Long.toUnsignedString(seed);
            saveMessageTimer = 4.0f;
        } catch (java.io.IOException | IllegalArgumentException error) {
            saveMessage = "Nie udało się wczytać świata: " + error.getMessage();
            saveMessageTimer = 6.0f;
        }
    }

    private void saveWorldNow() {
        try {
            WorldStorage.Data data = new WorldStorage.Data();
            data.seed = seed;
            data.spawnX = spawnX;
            data.spawnZ = spawnZ;
            data.creativeMode = creativeMode;
            data.aggressionLevel = aggressionLevel;
            data.renderDistance = renderDistance;
            data.playerHealth = playerHealth;
            data.meatCount = meatCount;
            data.selectedBlock = selectedBlock;
            data.weather = weather.ordinal();
            data.daySpeed = daySpeed;
            data.playerX = playerX;
            data.playerY = playerY;
            data.playerZ = playerZ;
            data.yaw = yaw;
            data.pitch = pitch;
            data.hunger = hunger;
            data.oxygen = oxygen;
            data.dayTime = dayTime;
            data.weatherTimer = weatherTimer;
            data.cloudCoverage = cloudCoverage;
            data.targetCloudCoverage = targetCloudCoverage;
            data.rainIntensity = rainIntensity;
            data.targetRainIntensity = targetRainIntensity;
            data.cloudOffsetX = cloudOffsetX;
            data.cloudOffsetY = cloudOffsetY;
            data.rainPhase = rainPhase;
            data.nickname = nickname;
            data.inventory = inventory.clone();
            data.hotbar = hotbar.clone();
            data.blocks = world.copyBlocks();
            data.waterFlow = world.copyWaterFlow();
            data.waterQueue = world.copyPendingWaterFlow();
            data.animals = new ArrayList<>(animals.length);
            for (Animal animal : animals) {
                WorldStorage.AnimalState state = new WorldStorage.AnimalState();
                state.kind = animal.kind;
                state.alive = animal.alive;
                state.health = animal.health;
                state.x = animal.x;
                state.y = animal.y;
                state.z = animal.z;
                state.yaw = animal.yaw;
                state.gait = animal.gait;
                state.verticalVelocity = animal.verticalVelocity;
                state.growth = animal.growth;
                state.breedingCooldown = animal.breedingCooldown();
                data.animals.add(state);
            }
            WorldStorage.save(data);
            saveMessage = "Zapisano świat " + Long.toUnsignedString(seed);
            saveMessageTimer = 4.0f;
            refreshSavedWorlds();
        } catch (java.io.IOException error) {
            saveMessage = "Nie udało się zapisać świata: " + error.getMessage();
            saveMessageTimer = 6.0f;
            System.err.println(saveMessage);
        }
    }

    private void drawBlockIcon(Graphics2D g, int x, int y, byte type) {
        BufferedImage blockRenderIcon = BlockRenderAssets.image(type);
        if (blockRenderIcon != null) g.drawImage(blockRenderIcon, x, y - 2, 40, 40, null);
    }

    private void drawStartOverlay(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(5, 12, 17, 105));
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        int width = 515, height = paused ? 210 : 270;
        int x = cx - width / 2, y = cy - height / 2;
        g.setColor(new Color(14, 26, 31, 220));
        g.fillRoundRect(x, y, width, height, 26, 26);
        g.setColor(new Color(210, 232, 217, 220));
        g.setStroke(new BasicStroke(1.3f)); g.drawRoundRect(x, y, width, height, 26, 26);
        g.setColor(new Color(175, 219, 155));
        g.setFont(uiFont(Font.BOLD, 34));
        String title = "TYTANCRAFT";
        g.drawString(title, cx - g.getFontMetrics().stringWidth(title) / 2, y + 56);
        g.setFont(uiFont(Font.PLAIN, 16));
        g.setColor(new Color(228, 238, 231));
        if (paused) {
            g.drawString("Gra wstrzymana", cx - 57, y + 91);
            g.drawString("Kliknij, aby wrócić do doliny", cx - 114, y + 128);
        } else {
            g.drawString("Zbuduj coś na skraju dzikiej doliny.", cx - 143, y + 91);
        g.drawString("WASD — ruch     SPACJA — skok     SHIFT — sprint", cx - 205, y + 137);
        g.drawString("LPM — kop / atak     PPM — buduj     R — zjedz mięso", cx - 216, y + 168);
            g.setColor(new Color(175, 219, 155));
            g.drawString("Kliknij, aby zacząć", cx - 77, y + 224);
        }
    }

    private void captureMouse() {
        if (captured || paused) return;
        captured = true;
        try {
            if (robot == null) robot = new Robot();
            if (hiddenCursor == null) {
                BufferedImage blank = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                hiddenCursor = getToolkit().createCustomCursor(blank, new java.awt.Point(0, 0), "hidden");
            }
            setCursor(hiddenCursor);
            java.awt.Point location = getLocationOnScreen();
            windowCenter = new java.awt.Point(location.x + getWidth() / 2, location.y + getHeight() / 2);
            lastNativeMouseEvent = System.nanoTime();
            robot.mouseMove(windowCenter.x, windowCenter.y);
        } catch (Exception ignored) {
            captured = false;
            setCursor(java.awt.Cursor.getDefaultCursor());
        }
    }

    private void releaseMouse(boolean pause) {
        captured = false;
        paused = pause;
        leftPressed = rightPressed = false;
        setCursor(java.awt.Cursor.getDefaultCursor());
    }

    private void mineOrPlace(boolean place) {
        if (place && World.isFishSpawnEgg(hotbar[selectedBlock])) {
            spawnFishFromEgg(hotbar[selectedBlock]);
            return;
        }
        if (!place && aimedAnimalEntity != null) {
            if (attackCooldown > 0 || aimedAnimalDistance > 5.5f || !aimedAnimalEntity.alive) return;
            attackCooldown = 0.35f;
            if (aimedAnimalEntity.takeHit() && !creativeMode) {
                meatCount = Math.min(99, meatCount + aimedAnimalEntity.meatDrop);
                if (aimedAnimalEntity.kind == Animal.Kind.SHEEP) {
                    inventory[World.WOOL] = Math.min(999, inventory[World.WOOL] + 1);
                }
                byte spawnEgg = aimedAnimalEntity.spawnEggDrop();
                if (spawnEgg != World.AIR) inventory[spawnEgg] = Math.min(999, inventory[spawnEgg] + 1);
            }
            return;
        }
        if (!hasHit || hitY < 0 || hitX < 0 || hitZ < 0 || hitX >= World.WIDTH || hitZ >= World.DEPTH) return;
        if (aimedBlockDistance > INTERACTION_REACH || blockActionCooldown > 0) return;
        if (place) {
            int x = hitX + hitNormalX, y = hitY + hitNormalY, z = hitZ + hitNormalZ;
            if (!World.inBounds(x, y, z)) return;
            byte selectedType = hotbar[selectedBlock];
            byte placedType = selectedType;
            if (selectedType == World.LADDER) {
                if (hitNormalY != 0 || !World.isSolid(world.get(hitX, hitY, hitZ))) return;
                placedType = World.ladderAgainstWall(hitNormalX, hitNormalZ);
            }
            int itemIndex = selectedType & 0xff;
            byte targetBlock = world.get(x, y, z);
            boolean replaceable = targetBlock == World.AIR || targetBlock == World.WATER;
            boolean overlapsPlayer = x + 1 > playerX - 0.23f && x < playerX + 0.23f
                    && z + 1 > playerZ - 0.23f && z < playerZ + 0.23f
                    && y + 1 > playerY && y < playerY + 1.8f;
            if (replaceable && !overlapsPlayer
                    && (creativeMode || inventory[itemIndex] > 0)) {
                world.set(x, y, z, placedType);
                if (!creativeMode) inventory[itemIndex]--;
                blockActionCooldown = 0.20f;
            }
        } else if (hitY > 0) {
            byte minedBlock = world.get(hitX, hitY, hitZ);
            world.set(hitX, hitY, hitZ, World.AIR);
            world.flowWaterInto(hitX, hitY, hitZ);
            if (!creativeMode && minedBlock != World.AIR && minedBlock != World.WATER) {
                int itemIndex = World.isLadderBlock(minedBlock) ? World.LADDER : minedBlock & 0xff;
                inventory[itemIndex] = Math.min(999, inventory[itemIndex] + 1);
            }
            blockActionCooldown = 0.20f;
        }
    }

    private void spawnFishFromEgg(byte eggType) {
        if (blockActionCooldown > 0 || !hasWaterHit || waterHitDistance > INTERACTION_REACH
                || world.get(waterHitX, waterHitY, waterHitZ) != World.WATER) return;
        int reusableSlot = -1;
        for (int i = 0; i < animals.length; i++) {
            if (!animals[i].alive) { reusableSlot = i; break; }
        }
        if (reusableSlot < 0 && animals.length >= MAX_ANIMALS) return;
        if (!creativeMode && inventory[eggType & 0xff] <= 0) return;

        Animal.Kind kind = switch (eggType) {
            case World.COD_SPAWN_EGG -> Animal.Kind.COD;
            case World.SALMON_SPAWN_EGG -> Animal.Kind.SALMON;
            case World.TROPICAL_FISH_SPAWN_EGG -> Animal.Kind.TROPICAL_FISH;
            case World.PIKE_SPAWN_EGG -> Animal.Kind.PIKE;
            default -> Animal.Kind.CARP;
        };
        int kindPopulation = 0;
        for (Animal animal : animals) if (animal.alive && animal.kind == kind) kindPopulation++;
        if (kindPopulation >= MAX_ANIMALS_PER_KIND) return;
        int ground = world.heightAt(waterHitX, waterHitZ);
        float swimHeight = Math.max(ground + 1.1f, World.WATER_LEVEL - 0.9f);
        Animal fish = new Animal(kind, waterHitX + 0.5f, waterHitZ + 0.5f, swimHeight, animalRandom);
        if (reusableSlot >= 0) animals[reusableSlot] = fish;
        else {
            animals = Arrays.copyOf(animals, animals.length + 1);
            animals[animals.length - 1] = fish;
        }
        if (!creativeMode) inventory[eggType & 0xff]--;
        blockActionCooldown = 0.20f;
    }

    private boolean pressed(int key) {
        synchronized (keys) { return key >= 0 && key < keys.length && keys[key]; }
    }

    private void countFrame() {
        frames++;
        long now = System.nanoTime();
        if (now - fpsStarted >= 1_000_000_000L) { fps = frames; frames = 0; fpsStarted = now; }
    }

    private static int rgb(float r, float g, float b) {
        return 0xff000000 | (clamp(Math.round(r)) << 16) | (clamp(Math.round(g)) << 8) | clamp(Math.round(b));
    }
    private static int clamp(int value) { return Math.max(0, Math.min(255, value)); }
    private static int mix(float a, float b, float t) { return clamp(Math.round(a + (b - a) * t)); }

    @Override public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_F11) {
            toggleFullScreen();
            return;
        }
        if (!inLobby && key == KeyEvent.VK_F5) {
            saveRequested = true;
            saveMessage = "Zapisywanie świata…";
            saveMessageTimer = 4.0f;
            return;
        }
        if (inLobby) {
            if (key == KeyEvent.VK_BACK_SPACE && nicknameFocused && !nickname.isEmpty()) {
                nickname = nickname.substring(0, nickname.length() - 1);
            } else if (key == KeyEvent.VK_ENTER) {
                enterWorld();
            } else if (key == KeyEvent.VK_ESCAPE) {
                nicknameFocused = false;
            }
            return;
        }
        if (inventoryOpen) {
            boolean wasPressed = pressed(key);
            synchronized (keys) { if (key >= 0 && key < keys.length) keys[key] = true; }
            if ((key == KeyEvent.VK_E || key == KeyEvent.VK_ESCAPE) && !wasPressed) {
                inventoryOpen = false;
                captureMouse();
            }
            return;
        }
        boolean wasPressed = pressed(key);
        synchronized (keys) { if (key >= 0 && key < keys.length) keys[key] = true; }
        if (key == KeyEvent.VK_E && !wasPressed) {
            inventoryOpen = true;
            releaseMouse(false);
            return;
        }
        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_9) selectedBlock = key - KeyEvent.VK_1;
        if (key == KeyEvent.VK_0) selectedBlock = 9;
        if (key == KeyEvent.VK_R && !wasPressed) pendingActions.add(this::eatFood);
        if (key == KeyEvent.VK_ESCAPE) releaseMouse(!paused);
    }
    @Override public void focusLost(FocusEvent e) {
        synchronized (keys) { Arrays.fill(keys, false); }
        if (captured) releaseMouse(true);
        else leftPressed = rightPressed = false;
    }
    @Override public void focusGained(FocusEvent e) {}
    @Override public void keyReleased(KeyEvent e) { int key = e.getKeyCode(); synchronized (keys) { if (key >= 0 && key < keys.length) keys[key] = false; } }
    @Override public void keyTyped(KeyEvent e) {
        char character = e.getKeyChar();
        if (inLobby && nicknameFocused && nickname.length() < 16
                && (Character.isLetterOrDigit(character) || character == '_' || character == '-')) {
            nickname += character;
        }
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {
        int mouseX = toVirtualX(e.getX()), mouseY = toVirtualY(e.getY());
        if (inventoryOpen) {
            if (e.getButton() == MouseEvent.BUTTON1) handleInventoryClick(mouseX, mouseY);
            return;
        }
        if (inLobby) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                int centerX = WINDOW_WIDTH / 2, centerY = WINDOW_HEIGHT / 2;
                int left = lobbyLeftCardX(centerX) + LOBBY_NICKNAME_X;
                int top = lobbyCardY(centerY) + LOBBY_NICKNAME_Y;
                int startX = lobbyStartButtonX(centerX);
                int startY = lobbyStartButtonY(centerY);
                int rightX = lobbyRightCardX(centerX);
                int cardY = lobbyCardY(centerY);
                int modeY = cardY + LOBBY_MODE_Y;
                int saveListX = lobbyLeftCardX(centerX);
                int saveListTop = cardY + LOBBY_SAVED_LIST_Y;
                int visibleSaves = Math.min(5, Math.max(0, savedWorlds.size() - savedWorldOffset));
                if (inside(mouseX, mouseY, saveListX + 15, saveListTop,
                        LOBBY_CARD_WIDTH - 30, visibleSaves * LOBBY_SAVED_ROW_STEP - 3)) {
                    int row = (mouseY - saveListTop) / LOBBY_SAVED_ROW_STEP;
                    if (row < visibleSaves) {
                        WorldStorage.SaveInfo save = savedWorlds.get(savedWorldOffset + row);
                        saveMessage = "Wczytywanie świata…";
                        saveMessageTimer = 6.0f;
                        pendingActions.add(() -> loadSavedWorld(save));
                    }
                    return;
                }
                if (inside(mouseX, mouseY, rightX + LOBBY_SURVIVAL_X, modeY,
                        LOBBY_SURVIVAL_WIDTH, LOBBY_MODE_HEIGHT)) {
                    creativeMode = false;
                    nicknameFocused = false;
                    return;
                }
                if (inside(mouseX, mouseY, rightX + LOBBY_CREATIVE_X, modeY,
                        LOBBY_CREATIVE_WIDTH, LOBBY_MODE_HEIGHT)) {
                    creativeMode = true;
                    nicknameFocused = false;
                    aggressionDragging = false;
                    return;
                }
                int sliderX = centerX + LOBBY_SLIDER_X;
                int sliderY = cardY + LOBBY_AGGRESSION_Y, sliderWidth = LOBBY_SLIDER_WIDTH;
                if (!creativeMode && inside(mouseX, mouseY, sliderX - 8, sliderY - 9, sliderWidth + 16, 24)) {
                    aggressionDragging = true;
                    setAggressionFromMouse(mouseX);
                    return;
                }
                int timeSliderY = cardY + LOBBY_DAY_SPEED_Y;
                if (inside(mouseX, mouseY, sliderX - 8, timeSliderY - 9, sliderWidth + 16, 24)) {
                    timeSpeedDragging = true;
                    setDaySpeedFromMouse(mouseX);
                    return;
                }
                int distanceSliderY = cardY + LOBBY_RENDER_DISTANCE_Y;
                if (inside(mouseX, mouseY, sliderX - 8, distanceSliderY - 9, sliderWidth + 16, 24)) {
                    renderDistanceDragging = true;
                    setRenderDistanceFromMouse(mouseX);
                    return;
                }
                if (inside(mouseX, mouseY, left, top, LOBBY_NICKNAME_WIDTH, LOBBY_NICKNAME_HEIGHT)) {
                    nicknameFocused = true;
                } else if (inside(mouseX, mouseY, startX, startY,
                        LOBBY_START_BUTTON_WIDTH, LOBBY_START_BUTTON_HEIGHT)) {
                    enterWorld();
                } else {
                    nicknameFocused = false;
                }
            }
            return;
        }
        if (!captured) { paused = false; captureMouse(); return; }
        if (e.getButton() == MouseEvent.BUTTON1) leftPressed = true;
        if (e.getButton() == MouseEvent.BUTTON3) rightPressed = true;
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftPressed = false;
            aggressionDragging = false;
            timeSpeedDragging = false;
            renderDistanceDragging = false;
        }
        if (e.getButton() == MouseEvent.BUTTON3) rightPressed = false;
    }
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {
        if (!captured || robot == null || windowCenter == null) return;
        long now = System.nanoTime();
        if (now - lastNativeMouseEvent < 2_000_000L) return;
        java.awt.Point location = e.getLocationOnScreen();
        int dx = location.x - windowCenter.x, dy = location.y - windowCenter.y;
        if (dx != 0 || dy != 0) {
            yaw += dx * 0.0027f;
            pitch = Math.max(-1.48f, Math.min(1.48f, pitch - dy * 0.0027f));
            robot.mouseMove(windowCenter.x, windowCenter.y);
            lastNativeMouseEvent = now;
        }
    }
    @Override public void mouseDragged(MouseEvent e) {
        if (inventoryOpen) return;
        if (inLobby && aggressionDragging) {
            setAggressionFromMouse(toVirtualX(e.getX()));
            return;
        }
        if (inLobby && timeSpeedDragging) {
            setDaySpeedFromMouse(toVirtualX(e.getX()));
            return;
        }
        if (inLobby && renderDistanceDragging) {
            setRenderDistanceFromMouse(toVirtualX(e.getX()));
            return;
        }
        mouseMoved(e);
    }
    @Override public void mouseWheelMoved(MouseWheelEvent e) {
        if (inventoryOpen) return;
        if (inLobby) {
            int listX = lobbyLeftCardX(WINDOW_WIDTH / 2);
            int listY = lobbyCardY(WINDOW_HEIGHT / 2) + 229;
            if (savedWorlds.size() > 5 && inside(toVirtualX(e.getX()), toVirtualY(e.getY()), listX, listY,
                    LOBBY_CARD_WIDTH, 5 * LOBBY_SAVED_ROW_STEP + 10)) {
                savedWorldOffset = Math.max(0, Math.min(savedWorlds.size() - 5,
                        savedWorldOffset + e.getWheelRotation()));
            }
            return;
        }
        selectedBlock = Math.floorMod(selectedBlock + e.getWheelRotation(), hotbar.length);
    }

    private String blockName(byte type) {
        return BlockRegistry.info(type).name();
    }

    private int toVirtualX(int x) { return Math.round(x * WINDOW_WIDTH / (float) Math.max(1, getWidth())); }

    private int toVirtualY(int y) { return Math.round(y * WINDOW_HEIGHT / (float) Math.max(1, getHeight())); }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private static Hit[] newHitScratch(int count) {
        Hit[] hits = new Hit[count];
        for (int i = 0; i < count; i++) hits[i] = new Hit();
        return hits;
    }

    private static AnimalHit[] newAnimalHitScratch(int count) {
        AnimalHit[] hits = new AnimalHit[count];
        for (int i = 0; i < count; i++) hits[i] = new AnimalHit();
        return hits;
    }

    private static final class RainSplash {
        final float x, y, z;
        final boolean onWater;
        final int randomBits;
        float age;

        RainSplash(float x, float y, float z, boolean onWater, int randomBits) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.onWater = onWater;
            this.randomBits = randomBits;
        }
    }

    private static final class Hit {
        static final Hit MISS = new Hit(0, 0, 0, World.AIR, 0, 0, 0, Float.POSITIVE_INFINITY, 0, 0, 0);
        int x, y, z, nx, ny, nz;
        byte type, visualType;
        float distance, px, py, pz;
        Hit() {}
        Hit(int x, int y, int z, byte type, int nx, int ny, int nz, float distance, float px, float py, float pz) {
            set(x, y, z, type, type, nx, ny, nz, distance, px, py, pz);
        }
        Hit(int x, int y, int z, byte type, byte visualType, int nx, int ny, int nz,
            float distance, float px, float py, float pz) {
            set(x, y, z, type, visualType, nx, ny, nz, distance, px, py, pz);
        }
        Hit set(Hit source) {
            return set(source.x, source.y, source.z, source.type, source.visualType,
                    source.nx, source.ny, source.nz, source.distance, source.px, source.py, source.pz);
        }
        Hit set(int x, int y, int z, byte type, byte visualType, int nx, int ny, int nz,
                float distance, float px, float py, float pz) {
            this.x = x; this.y = y; this.z = z; this.type = type; this.visualType = visualType;
            this.nx = nx; this.ny = ny; this.nz = nz; this.distance = distance;
            this.px = px; this.py = py; this.pz = pz;
            return this;
        }
    }

    private static final class AnimalHit {
        Animal animal;
        Animal.Kind kind;
        int color;
        float distance, nx, ny, nz, x, y, z;

        void set(Animal animal, int color, float distance, float nx, float ny, float nz,
                 float x, float y, float z) {
            this.animal = animal;
            this.kind = animal.kind;
            this.color = color;
            this.distance = distance;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
