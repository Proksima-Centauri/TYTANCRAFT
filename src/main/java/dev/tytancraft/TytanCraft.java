package dev.tytancraft;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Shape;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
    private static final int MAX_ANIMALS = 128;
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
            World.PIKE_SPAWN_EGG, World.CARP_SPAWN_EGG
    };
    private static final String[] BLOCK_NAMES = {
            "Trawa", "Ziemia", "Kamień", "Piasek", "Drewno", "Liście", "Węgiel", "Miedź",
            "Żelazo", "Złoto", "Diament", "Szmaragd", "Rubin", "Szafir", "Śnieg",
            "Mak", "Jaskier", "Chaber", "Drewno brzozy", "Liście brzozy", "Drewno świerku",
            "Igły świerku", "Drewno akacji", "Liście akacji", "Drewno wiśni", "Liście wiśni",
            "Deski", "Pochodnia", "Blok szmaragdu", "Mały muchomor", "Średni muchomor",
            "Duży muchomor", "Mały grzyb jadalny", "Średni grzyb jadalny", "Duży grzyb jadalny",
            "Ikra dorsza", "Ikra łososia", "Ikra tropikalna", "Ikra szczupaka", "Ikra karpia"
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
    private static final int CLOUD_SIZE = 128;
    private static final float[] CLOUD_MAP = createCloudMap();

    private enum Weather { CLEAR, CLOUDY, RAIN }

    private final World world;
    private final int spawnX, spawnZ;
    private Animal[] animals;
    private final Animal[] visibleAnimals = new Animal[MAX_ANIMALS];
    private final Random animalRandom;
    private final Random weatherRandom;
    private final Hit traceScratch = new Hit();
    private final Hit underwaterTraceScratch = new Hit();
    private final BufferedImage scene = new BufferedImage(VIEW_WIDTH, VIEW_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) scene.getRaster().getDataBuffer()).getData();
    private final boolean[] keys = new boolean[512];
    private final float[] columnX = new float[VIEW_WIDTH];
    private final float[] columnY = new float[VIEW_HEIGHT];
    private final int[] inventory = new int[48];
    private final byte[] hotbar = {
            GRASS_BLOCK, DIRT_BLOCK, STONE_BLOCK, SAND_BLOCK, WOOD_BLOCK,
            World.COAL_ORE, World.IRON_ORE, World.COPPER_ORE, World.SNOW, World.RED_FLOWER
    };
    private final long seed;
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
    private boolean craftingTab;
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
    private java.awt.Point windowCenter;
    private java.awt.Cursor hiddenCursor;

    private TytanCraft(long seed) {
        this.seed = seed;
        world = new World(seed);
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
    }

    public static void main(String[] args) {
        long seed = args.length == 0 ? new Random().nextLong() : parseSeed(args[0]);
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("TytanCraft — Dzika Dolina");
            TytanCraft game = new TytanCraft(seed);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
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

    private static long parseSeed(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException signedOverflow) {
            return Long.parseUnsignedLong(text);
        }
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
    }

    private void initializeViewRays() {
        float focalLength = (VIEW_HEIGHT * 0.5f) / (float) Math.tan(FOV * 0.5f);
        for (int x = 0; x < VIEW_WIDTH; x++) columnX[x] = (x + 0.5f - VIEW_WIDTH * 0.5f) / focalLength;
        for (int y = 0; y < VIEW_HEIGHT; y++) columnY[y] = (VIEW_HEIGHT * 0.5f - y - 0.5f) / focalLength;
    }

    private void start() {
        createBufferStrategy(2);
        long previous = System.nanoTime();
        double accumulator = 0;
        final double step = 1.0 / 60.0;
        while (running && isDisplayable()) {
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
        if (inLobby || inventoryOpen || paused || !captured) return;
        for (Animal animal : animals) {
            if (animal.alive) animal.update(world, dt, animalRandom, aggressionLevel, playerX, playerZ, !creativeMode);
        }
        advanceEnvironment(dt);
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
        float forward = (pressed(KeyEvent.VK_W) ? 1 : 0) - (pressed(KeyEvent.VK_S) ? 1 : 0);
        float strafe = (pressed(KeyEvent.VK_D) ? 1 : 0) - (pressed(KeyEvent.VK_A) ? 1 : 0);
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

        if (swimming) {
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

    private static float sampleCloudMap(float x, float y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        float tx = x - x0, ty = y - y0;
        x0 &= CLOUD_SIZE - 1;
        y0 &= CLOUD_SIZE - 1;
        int nearestX = (x0 + (tx >= 0.5f ? 1 : 0)) & (CLOUD_SIZE - 1);
        int nearestY = (y0 + (ty >= 0.5f ? 1 : 0)) & (CLOUD_SIZE - 1);
        return CLOUD_MAP[nearestY * CLOUD_SIZE + nearestX];
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
        if (creativeMode) return;
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
        for (int sampleX = -1; sampleX <= 1; sampleX++) {
            for (int sampleZ = -1; sampleZ <= 1; sampleZ++) {
                float blockX = x + sampleX * 0.23f, blockZ = z + sampleZ * 0.23f;
                if (solidAt(blockX, feetY + 0.15f, blockZ)
                        || solidAt(blockX, feetY + 0.90f, blockZ)
                        || solidAt(blockX, feetY + 1.65f, blockZ)
                        || solidAt(blockX, feetY + 1.78f, blockZ)) return false;
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
        return type != World.AIR && type != World.WATER && !World.isFlower(type) && !World.isMushroom(type);
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

        for (int py = 0, index = 0; py < VIEW_HEIGHT; py++) {
            float sy = columnY[py];
            for (int px = 0; px < VIEW_WIDTH; px++, index++) {
                float sx = columnX[px];
                float dx = fx + rx * sx + ux * sy;
                float dy = fy + uy * sy;
                float dz = fz + rz * sx + uz * sy;
                float inverseLength = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx *= inverseLength; dy *= inverseLength; dz *= inverseLength;
                Hit hit = trace(eyeX, eyeY, eyeZ, dx, dy, dz, maxDistance, false, traceScratch);
                AnimalHit animalHit = traceAnimals(eyeX, eyeY, eyeZ, dx, dy, dz, maxDistance, visibleAnimalCount);
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
                    Hit underwaterHit = trace(eyeX, eyeY, eyeZ, dx, dy, dz, maxDistance, true, underwaterTraceScratch);
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
        }
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
                    Hit plant = tracePlant(ox, oy, oz, dx, dy, dz, maxDistance, distance, x, y, z, type, FLOWER_PARTS);
                    if (plant != null) return result.set(plant);
                } else if (World.isMushroom(type)) {
                    Hit mushroom = tracePlant(ox, oy, oz, dx, dy, dz, maxDistance, distance,
                            x, y, z, type, mushroomParts(type));
                    if (mushroom != null) return result.set(mushroom);
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

    private Hit tracePlant(float ox, float oy, float oz, float dx, float dy, float dz,
                           float maxDistance, float cellEntry, int cellX, int cellY, int cellZ,
                           byte plantType, float[][] plantParts) {
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
        if (nearest >= maxDistance) return null;
        return new Hit(cellX, cellY, cellZ, plantType, visibleType,
                normalX, normalY, normalZ, nearest,
                ox + dx * nearest, oy + dy * nearest, oz + dz * nearest);
    }

    private static float[][] mushroomParts(byte type) {
        if (type == World.RED_MUSHROOM_SMALL || type == World.BROWN_MUSHROOM_SMALL) return SMALL_MUSHROOM_PARTS;
        if (type == World.RED_MUSHROOM_MEDIUM || type == World.BROWN_MUSHROOM_MEDIUM) return MEDIUM_MUSHROOM_PARTS;
        return LARGE_MUSHROOM_PARTS;
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
            if (count < visibleAnimals.length) visibleAnimals[count++] = animal;
        }
        return count;
    }

    private AnimalHit traceAnimals(float ox, float oy, float oz, float dx, float dy, float dz,
                                   float maxDistance, int visibleAnimalCount) {
        AnimalHit closest = null;
        float closestDistance = maxDistance;
        for (int animalIndex = 0; animalIndex < visibleAnimalCount; animalIndex++) {
            Animal animal = visibleAnimals[animalIndex];
            float toX = animal.x - ox, toY = animal.y + 0.9f - oy, toZ = animal.z - oz;
            float along = toX * dx + toY * dy + toZ * dz;
            float perpendicularSquared = toX * toX + toY * toY + toZ * toZ - along * along;
            if (perpendicularSquared > 4.41f || along + 2.1f < 0 || along - 2.1f > closestDistance) continue;

            float cos = (float) Math.cos(animal.yaw), sin = (float) Math.sin(animal.yaw);
            float localOx = -cos * toX + sin * toZ;
            float localOz = -sin * toX - cos * toZ;
            float localDx = cos * dx - sin * dz;
            float localDz = sin * dx + cos * dz;
            float localOy = oy - animal.y, localDy = dy;
            float modelScale = animal.kind.isFish() ? 1.35f : 1.0f;
            for (Animal.Part part : animal.parts) {
                float legSwing = part.gaitOffset == 0 ? 0 : (float) Math.sin(animal.gait + part.gaitOffset) * 0.10f * modelScale;
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
                closest = new AnimalHit(animal, part.color, distance,
                        worldNx, ny, worldNz, ox + dx * distance, oy + dy * distance, oz + dz * distance);
            }
        }
        return closest;
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
        float baseR, baseG, baseB;
        if (type == World.GRASS) {
            if (hit.ny == 0 && hit.py - hit.y > 0.84f) { baseR = 85; baseG = 133; baseB = 63; }
            else if (hit.ny == 1) { baseR = 87; baseG = 157; baseB = 69; }
            else { baseR = 107; baseG = 91; baseB = 61; }
        } else if (type == World.DIRT) { baseR = 125; baseG = 91; baseB = 61; }
        else if (type == World.STONE) { baseR = 120; baseG = 126; baseB = 126; }
        else if (type == World.SAND) { baseR = 205; baseG = 190; baseB = 139; }
        else if (type == World.WOOD) { baseR = 126; baseG = 85; baseB = 48; }
        else if (type == World.LEAVES) { baseR = 55; baseG = 120; baseB = 65; }
        else if (type == World.COAL_ORE) { baseR = 62; baseG = 67; baseB = 73; }
        else if (type == World.COPPER_ORE) { baseR = 190; baseG = 112; baseB = 70; }
        else if (type == World.IRON_ORE) { baseR = 175; baseG = 132; baseB = 108; }
        else if (type == World.GOLD_ORE) { baseR = 226; baseG = 186; baseB = 65; }
        else if (type == World.DIAMOND_ORE) { baseR = 81; baseG = 211; baseB = 225; }
        else if (type == World.EMERALD_ORE) { baseR = 47; baseG = 192; baseB = 111; }
        else if (type == World.RUBY_ORE) { baseR = 208; baseG = 57; baseB = 79; }
        else if (type == World.SAPPHIRE_ORE) { baseR = 71; baseG = 116; baseB = 229; }
        else if (type == World.SNOW) { baseR = 223; baseG = 236; baseB = 241; }
        else if (type == World.RED_FLOWER) { baseR = 215; baseG = 65; baseB = 83; }
        else if (type == World.YELLOW_FLOWER) { baseR = 244; baseG = 205; baseB = 68; }
        else if (type == World.BLUE_FLOWER) { baseR = 87; baseG = 120; baseB = 226; }
        else if (World.isMushroom(type)) {
            boolean redCap = type <= World.RED_MUSHROOM_LARGE;
            if (hit.visualType == World.SNOW) { baseR = 231; baseG = 224; baseB = 207; }
            else if (redCap) {
                baseR = texture > 0.82f ? 243 : 190;
                baseG = texture > 0.82f ? 227 : 56;
                baseB = texture > 0.82f ? 198 : 54;
            } else {
                baseR = texture > 0.86f ? 224 : 151;
                baseG = texture > 0.86f ? 195 : 103;
                baseB = texture > 0.86f ? 151 : 62;
            }
        }
        else if (type == World.BIRCH_WOOD) { baseR = 224; baseG = 215; baseB = 179; }
        else if (type == World.BIRCH_LEAVES) { baseR = 116; baseG = 168; baseB = 80; }
        else if (type == World.PINE_WOOD) { baseR = 96; baseG = 69; baseB = 44; }
        else if (type == World.PINE_LEAVES) { baseR = 37; baseG = 89; baseB = 56; }
        else if (type == World.ACACIA_WOOD) { baseR = 166; baseG = 105; baseB = 57; }
        else if (type == World.ACACIA_LEAVES) { baseR = 126; baseG = 142; baseB = 62; }
        else if (type == World.CHERRY_WOOD) { baseR = 129; baseG = 77; baseB = 73; }
        else if (type == World.CHERRY_LEAVES) { baseR = 224; baseG = 135; baseB = 173; }
        else if (type == World.PLANKS) { baseR = 175; baseG = 125; baseB = 75; }
        else if (type == World.TORCH) { baseR = 245; baseG = 166; baseB = 57; }
        else if (type == World.EMERALD_BLOCK) { baseR = 32; baseG = 154; baseB = 93; }
        else { baseR = 48; baseG = 122; baseB = 143; }

        float diffuse = Math.max(0, hit.nx * sunX + hit.ny * sunY + hit.nz * sunZ);
        float normalLight = 0.16f + sunlight * 0.35f + diffuse * (0.43f * sunlight);
        if (hit.ny == -1) normalLight *= 0.62f;
        float heightAmbient = Math.max(0.78f, Math.min(1.0f, 0.78f + hit.y * 0.004f));
        float lighting = normalLight * heightAmbient * (0.92f + texture * 0.15f);
        lighting *= 1.0f - rainIntensity * 0.16f;
        if (hit.y < world.heightAt(hit.x, hit.z) - 3) lighting *= 0.34f;
        if (type != World.WATER && inSunShadow(hit)) lighting *= 0.68f;
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

    private void drawFrame() {
        BufferStrategy strategy = getBufferStrategy();
        if (strategy == null) return;
        do {
            do {
                Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(scene, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
                if (!inLobby && !paused) drawRain(g);
                drawHud(g);
                g.dispose();
            } while (strategy.contentsRestored());
            strategy.show();
            Toolkit.getDefaultToolkit().sync();
        } while (strategy.contentsLost());
    }

    private void drawHud(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = WINDOW_WIDTH / 2, cy = WINDOW_HEIGHT / 2;
        if (inLobby) {
            drawLobby(g, cx, cy);
            return;
        }
        if (inventoryOpen) {
            drawInventoryDialog(g, cx, cy);
            return;
        }
        drawEnvironmentBadge(g, cx);
        g.setColor(new Color(255, 255, 255, 210));
        g.setStroke(new BasicStroke(2.2f));
        g.drawLine(cx - 10, cy, cx - 3, cy); g.drawLine(cx + 3, cy, cx + 10, cy);
        g.drawLine(cx, cy - 10, cx, cy - 3); g.drawLine(cx, cy + 3, cx, cy + 10);
        if (!creativeMode) drawSurvivalIndicators(g);
        if (aimedAnimal != null) {
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(7, 17, 20, 170));
            int labelWidth = g.getFontMetrics().stringWidth(aimedAnimal) + 20;
            g.fillRoundRect(cx - labelWidth / 2, cy + 18, labelWidth, 23, 10, 10);
            g.setColor(new Color(245, 244, 221));
            g.drawString(aimedAnimal, cx - g.getFontMetrics().stringWidth(aimedAnimal) / 2, cy + 34);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(8, 19, 27, 145));
        g.fillRoundRect(23, 23, 340, 73, 18, 18);
        g.setColor(Color.WHITE);
        g.drawString("TYTANCRAFT", 40, 51);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(224, 238, 235));
        g.drawString(creativeMode ? "Kreatywny  •  " + fps + " FPS"
                : "Przetrwanie  •  HP " + playerHealth + "  •  " + fps + " FPS", 40, 77);

        int slotSize = 56, gap = 4, total = hotbar.length * slotSize + (hotbar.length - 1) * gap;
        int startX = (WINDOW_WIDTH - total) / 2, y = WINDOW_HEIGHT - 95;
        for (int i = 0; i < hotbar.length; i++) {
            int x = startX + i * (slotSize + gap);
            g.setColor(new Color(7, 16, 22, 168));
            g.fillRoundRect(x, y, slotSize, slotSize, 12, 12);
            g.setColor(i == selectedBlock ? new Color(247, 226, 156) : new Color(255, 255, 255, 120));
            g.setStroke(new BasicStroke(i == selectedBlock ? 3 : 1));
            g.drawRoundRect(x, y, slotSize, slotSize, 12, 12);
            drawBlockIcon(g, x + 11, y + 15, hotbar[i]);
            g.setColor(new Color(245, 247, 242));
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            if (i < 9) g.drawString(Integer.toString(i + 1), x + 3, y + 14);
            else g.drawString("0", x + 3, y + 14);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        byte selectedType = hotbar[selectedBlock];
        String selectedName = blockName(selectedType)
                + (creativeMode ? "" : " ×" + inventory[selectedType & 0xff]);
        g.setColor(new Color(10, 20, 24, 165));
        int labelWidth = g.getFontMetrics().stringWidth(selectedName) + 26;
        g.fillRoundRect(cx - labelWidth / 2, y - 31, labelWidth, 23, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(selectedName, cx - g.getFontMetrics().stringWidth(selectedName) / 2, y - 14);

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(255, 255, 255, 224));
        if (isInWater()) {
            g.drawString("PŁYWANIE   SPACJA — góra   CTRL — dół   W+SPACJA — wyjdź", 26, WINDOW_HEIGHT - 57);
        }
        g.drawString(String.format("X %d   Y %d   Z %d", (int) playerX, (int) playerY, (int) playerZ), 26, WINDOW_HEIGHT - 32);
        if (!captured || paused) drawStartOverlay(g, cx, cy);
    }

    private void drawHotbarDialogUnused(Graphics2D g, int panelX, int panelY) {
        int cardY = panelY + 112, cardHeight = 455;
        int leftX = panelX + 22, leftWidth = 300;
        int rightX = panelX + 337, rightWidth = 612;
        drawLobbyCard(g, leftX, cardY, leftWidth, cardHeight, "10 MIEJSC HOTBARA");
        drawLobbyCard(g, rightX, cardY, rightWidth, cardHeight, "DOSTĘPNE BLOKI");

        for (int slot = 0; slot < hotbar.length; slot++) {
            int col = slot % 2, row = slot / 2;
            int x = leftX + 12 + col * 137, y = cardY + 46 + row * 73;
            g.setColor(new Color(8, 19, 22, 215));
            g.fillRoundRect(x, y, 130, 64, 10, 10);
            g.setColor(slot == customizeSlot ? new Color(238, 215, 143) : new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(slot == customizeSlot ? 2 : 1));
            g.drawRoundRect(x, y, 130, 64, 10, 10);
            drawBlockIcon(g, x + 3, y + 18, hotbar[slot]);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(231, 237, 225));
            g.drawString(slot == 9 ? "0" : Integer.toString(slot + 1), x + 5, y + 13);
            String label = shorten(blockName(hotbar[slot]), 12);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString(label, x + 39, y + 29);
            if (!creativeMode) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.setColor(new Color(176, 199, 182));
                g.drawString("×" + inventory[hotbar[slot] & 0xff], x + 39, y + 46);
            }
        }

        int gridX = rightX + 8, gridY = cardY + 43, tileWidth = 82, tileHeight = 64, xStep = 84, yStep = 69;
        for (int i = 0; i < BLOCK_TYPES.length; i++) {
            int col = i % 7, row = i / 7;
            int x = gridX + col * xStep, y = gridY + row * yStep;
            boolean selected = BLOCK_TYPES[i] == hotbar[customizeSlot];
            g.setColor(new Color(8, 19, 22, 215));
            g.fillRoundRect(x, y, tileWidth, tileHeight, 9, 9);
            g.setColor(selected ? new Color(183, 214, 153) : new Color(255, 255, 255, 55));
            g.setStroke(new BasicStroke(selected ? 1.7f : 1));
            g.drawRoundRect(x, y, tileWidth, tileHeight, 9, 9);
            drawBlockIcon(g, x + 2, y + 18, BLOCK_TYPES[i]);
            g.setFont(new Font("SansSerif", Font.BOLD, 9));
            g.setColor(new Color(235, 239, 228));
            g.drawString(shorten(blockName(BLOCK_TYPES[i]), 11), x + 36, y + 28);
            if (!creativeMode) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.setColor(new Color(176, 199, 182));
                g.drawString("×" + inventory[BLOCK_TYPES[i] & 0xff], x + 36, y + 46);
            }
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(204, 222, 208));
        g.drawString("Wybierz miejsce hotbara, potem kliknij blok, który chcesz tam przypisać.",
                panelX + 205, panelY + 602);
    }

    private void drawCraftingDialogUnused(Graphics2D g, int panelX, int panelY) {
        String[] names = {"Deski", "Pochodnie", "Blok szmaragdu"};
        String[] ingredients = {"1× dowolne drewno", "2× deski + 1× węgiel", "9× szmaragd"};
        byte[] outputs = {World.PLANKS, World.TORCH, World.EMERALD_BLOCK};
        int[] outputCounts = {4, 4, 1};
        int startX = panelX + 24, cardY = panelY + 123, cardWidth = 296, cardHeight = 430;
        for (int recipe = 0; recipe < names.length; recipe++) {
            int x = startX + recipe * 310;
            g.setColor(new Color(22, 39, 42, 232));
            g.fillRoundRect(x, cardY, cardWidth, cardHeight, 18, 18);
            g.setColor(recipe == selectedRecipe ? new Color(190, 219, 157) : new Color(255, 255, 255, 48));
            g.setStroke(new BasicStroke(recipe == selectedRecipe ? 2 : 1));
            g.drawRoundRect(x, cardY, cardWidth, cardHeight, 18, 18);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(new Color(238, 243, 231));
            g.drawString(names[recipe], x + 20, cardY + 53);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(185, 205, 190));
            g.drawString("Składniki:", x + 20, cardY + 101);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(228, 232, 213));
            g.drawString(ingredients[recipe], x + 20, cardY + 127);
            g.setColor(new Color(255, 255, 255, 36));
            g.drawLine(x + 20, cardY + 152, x + cardWidth - 20, cardY + 152);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(185, 205, 190));
            g.drawString("Wynik:", x + 20, cardY + 183);
            drawBlockIcon(g, x + 36, cardY + 211, outputs[recipe]);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(236, 241, 226));
            g.drawString(blockName(outputs[recipe]) + " ×" + outputCounts[recipe], x + 81, cardY + 245);

            boolean available = creativeMode || canCraftRecipe(recipe);
            int buttonY = cardY + 340;
            g.setColor(available ? new Color(105, 159, 83) : new Color(75, 82, 77));
            g.fillRoundRect(x + 20, buttonY, cardWidth - 40, 50, 13, 13);
            g.setColor(available ? new Color(194, 232, 167) : new Color(141, 149, 141));
            g.drawRoundRect(x + 20, buttonY, cardWidth - 40, 50, 13, 13);
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.setColor(new Color(245, 247, 238));
            String button = available ? "WYTWÓRZ" : "BRAK SKŁADNIKÓW";
            g.drawString(button, x + (cardWidth - g.getFontMetrics().stringWidth(button)) / 2, buttonY + 31);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(207, 225, 208));
        g.drawString(craftMessage.isEmpty() ? "Wybierz recepturę i naciśnij WYTWÓRZ." : craftMessage,
                panelX + 26, panelY + 603);
    }

    private void drawInventoryDialog(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(4, 10, 15, 175));
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        int x = WINDOW_WIDTH / 2 - 485, y = WINDOW_HEIGHT / 2 - 315;
        g.setColor(new Color(13, 25, 31, 246));
        g.fillRoundRect(x, y, INVENTORY_WIDTH, INVENTORY_HEIGHT, 24, 24);
        g.setColor(new Color(204, 225, 208, 190));
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(x, y, INVENTORY_WIDTH, INVENTORY_HEIGHT, 24, 24);
        g.setFont(new Font("SansSerif", Font.BOLD, 25));
        g.setColor(new Color(190, 226, 172));
        String title = "PLECAK I RZEMIOSŁO";
        g.drawString(title, cx - g.getFontMetrics().stringWidth(title) / 2, y + 39);

        int tabY = y + 55;
        drawLobbyModeButton(g, x + 25, tabY, 175, 36, "HOTBAR", !craftingTab);
        drawLobbyModeButton(g, x + 212, tabY, 175, 36, "CRAFTING", craftingTab);
        g.setColor(new Color(163, 186, 169));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("ESC / E — zamknij", x + 700, tabY + 23);
        g.setColor(new Color(131, 59, 63));
        g.fillRoundRect(x + INVENTORY_WIDTH - 51, y + 15, 34, 32, 9, 9);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("×", x + INVENTORY_WIDTH - 41, y + 38);

        if (craftingTab) drawCraftingDialog(g, x, y);
        else drawHotbarDialog(g, x, y);
    }

    private void drawHotbarDialog(Graphics2D g, int panelX, int panelY) {
        int cardY = panelY + 112, cardHeight = 455;
        int leftX = panelX + 22, leftWidth = 300;
        int rightX = panelX + 337, rightWidth = 612;
        drawLobbyCard(g, leftX, cardY, leftWidth, cardHeight, "10 MIEJSC HOTBARA");
        drawLobbyCard(g, rightX, cardY, rightWidth, cardHeight, "DOSTĘPNE BLOKI");

        for (int slot = 0; slot < hotbar.length; slot++) {
            int col = slot % 2, row = slot / 2;
            int x = leftX + 12 + col * 137, y = cardY + 46 + row * 73;
            g.setColor(new Color(8, 19, 22, 215));
            g.fillRoundRect(x, y, 130, 64, 10, 10);
            g.setColor(slot == customizeSlot ? new Color(238, 215, 143) : new Color(255, 255, 255, 65));
            g.setStroke(new BasicStroke(slot == customizeSlot ? 2 : 1));
            g.drawRoundRect(x, y, 130, 64, 10, 10);
            drawBlockIcon(g, x + 3, y + 18, hotbar[slot]);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.setColor(new Color(231, 237, 225));
            g.drawString(slot == 9 ? "0" : Integer.toString(slot + 1), x + 5, y + 13);
            String label = shorten(blockName(hotbar[slot]), 12);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString(label, x + 39, y + 29);
            if (!creativeMode) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.setColor(new Color(176, 199, 182));
                g.drawString("×" + inventory[hotbar[slot] & 0xff], x + 39, y + 46);
            }
        }

        int gridX = rightX + 8, gridY = cardY + 43, tileWidth = 96, tileHeight = 64, xStep = 100, yStep = 69;
        for (int i = 0; i < BLOCK_TYPES.length; i++) {
            int col = i % 6, row = i / 6;
            int x = gridX + col * xStep, y = gridY + row * yStep;
            boolean selected = BLOCK_TYPES[i] == hotbar[customizeSlot];
            g.setColor(new Color(8, 19, 22, 215));
            g.fillRoundRect(x, y, tileWidth, tileHeight, 9, 9);
            g.setColor(selected ? new Color(183, 214, 153) : new Color(255, 255, 255, 55));
            g.setStroke(new BasicStroke(selected ? 1.7f : 1));
            g.drawRoundRect(x, y, tileWidth, tileHeight, 9, 9);
            drawBlockIcon(g, x, y + 18, BLOCK_TYPES[i]);
            g.setFont(new Font("SansSerif", Font.BOLD, 8));
            g.setColor(new Color(235, 239, 228));
            g.drawString(shorten(blockName(BLOCK_TYPES[i]), 9), x + 34, y + 27);
            if (!creativeMode) {
                g.setFont(new Font("SansSerif", Font.PLAIN, 8));
                g.setColor(new Color(176, 199, 182));
                g.drawString("×" + inventory[BLOCK_TYPES[i] & 0xff], x + 34, y + 45);
            }
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(204, 222, 208));
        g.drawString("Najpierw wybierz miejsce hotbara, potem kliknij blok, który chcesz tam przypisać.",
                panelX + 205, panelY + 602);
    }

    private void drawCraftingDialog(Graphics2D g, int panelX, int panelY) {
        String[] names = {"Deski", "Pochodnie", "Blok szmaragdu"};
        String[] ingredients = {"1× dowolne drewno", "2× deski + 1× węgiel", "9× szmaragd"};
        byte[] outputs = {World.PLANKS, World.TORCH, World.EMERALD_BLOCK};
        int[] outputCounts = {4, 4, 1};
        int startX = panelX + 24, cardY = panelY + 123, cardWidth = 296, cardHeight = 430;
        for (int recipe = 0; recipe < names.length; recipe++) {
            int x = startX + recipe * 310;
            drawLobbyCard(g, x, cardY, cardWidth, cardHeight, "RECEPTURA " + (recipe + 1));
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(new Color(238, 243, 231));
            g.drawString(names[recipe], x + 20, cardY + 69);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(185, 205, 190));
            g.drawString("Składniki:", x + 20, cardY + 108);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(228, 232, 213));
            g.drawString(ingredients[recipe], x + 20, cardY + 133);
            g.setColor(new Color(255, 255, 255, 36));
            g.drawLine(x + 20, cardY + 157, x + cardWidth - 20, cardY + 157);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(185, 205, 190));
            g.drawString("Wynik:", x + 20, cardY + 188);
            drawBlockIcon(g, x + 30, cardY + 215, outputs[recipe]);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(236, 241, 226));
            g.drawString(blockName(outputs[recipe]) + " ×" + outputCounts[recipe], x + 75, cardY + 249);

            boolean canCraft = creativeMode || canCraftRecipe(recipe);
            int buttonY = cardY + 340;
            g.setColor(canCraft ? new Color(105, 159, 83) : new Color(75, 82, 77));
            g.fillRoundRect(x + 20, buttonY, cardWidth - 40, 50, 13, 13);
            g.setColor(canCraft ? new Color(194, 232, 167) : new Color(141, 149, 141));
            g.drawRoundRect(x + 20, buttonY, cardWidth - 40, 50, 13, 13);
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.setColor(new Color(245, 247, 238));
            String button = canCraft ? "WYTWÓRZ" : "BRAK SKŁADNIKÓW";
            g.drawString(button, x + (cardWidth - g.getFontMetrics().stringWidth(button)) / 2, buttonY + 31);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(207, 225, 208));
        g.drawString(craftMessage.isEmpty() ? "Wybierz recepturę i naciśnij WYTWÓRZ." : craftMessage,
                panelX + 26, panelY + 603);
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
        int x = INVENTORY_PANEL_X, y = INVENTORY_PANEL_Y;
        if (inside(mouseX, mouseY, x + INVENTORY_WIDTH - 51, y + 15, 34, 32)) {
            closeInventory();
            return;
        }
        int tabY = y + 55;
        if (inside(mouseX, mouseY, x + 25, tabY, 175, 36)) { craftingTab = false; return; }
        if (inside(mouseX, mouseY, x + 212, tabY, 175, 36)) { craftingTab = true; return; }
        if (craftingTab) {
            int startX = x + 24, cardY = y + 123, cardWidth = 296, buttonY = cardY + 340;
            for (int recipe = 0; recipe < 3; recipe++) {
                int cardX = startX + recipe * 310;
                if (inside(mouseX, mouseY, cardX + 20, buttonY, cardWidth - 40, 50)) {
                    selectedRecipe = recipe;
                    if (creativeMode || canCraftRecipe(recipe)) craftRecipe(recipe);
                    else craftMessage = "Brakuje składników do tej receptury.";
                    return;
                }
                if (inside(mouseX, mouseY, cardX, cardY, cardWidth, 325)) {
                    selectedRecipe = recipe;
                    return;
                }
            }
            return;
        }

        int cardY = y + 112;
        int hotbarX = x + 22 + 12, hotbarY = cardY + 46;
        for (int slot = 0; slot < hotbar.length; slot++) {
            int col = slot % 2, row = slot / 2;
            int tileX = hotbarX + col * 137, tileY = hotbarY + row * 73;
            if (inside(mouseX, mouseY, tileX, tileY, 130, 64)) {
                customizeSlot = slot;
                return;
            }
        }
        int catalogX = x + 337 + 8, catalogY = cardY + 43;
        for (int i = 0; i < BLOCK_TYPES.length; i++) {
            int tileX = catalogX + (i % 7) * 84, tileY = catalogY + (i / 7) * 69;
            if (inside(mouseX, mouseY, tileX, tileY, 82, 64)) {
                hotbar[customizeSlot] = BLOCK_TYPES[i];
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
        g.setColor(new Color(8, 19, 27, 150));
        g.fillRoundRect(x, y, 264, 34, 14, 14);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(new Color(240, 244, 231));
        g.drawString(label, centerX - g.getFontMetrics().stringWidth(label) / 2, y + 22);
    }

    private void drawRain(Graphics2D g) {
        if (rainIntensity < 0.08f) return;
        int x = (int) Math.floor(playerX), z = (int) Math.floor(playerZ);
        if (playerY < world.canopyHeightAt(x, z) - 1.0f) return;
        Graphics2D rain = (Graphics2D) g.create();
        rain.scale((double) WINDOW_WIDTH / VIEW_WIDTH, (double) WINDOW_HEIGHT / VIEW_HEIGHT);
        rain.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        rain.setColor(new Color(174, 216, 236, (int) (145 * rainIntensity)));
        int driftX = (int) (rainPhase * 0.35f);
        int driftY = (int) (rainPhase * 1.1f);
        int drops = (int) (150 * rainIntensity);
        for (int i = 0; i < drops; i++) {
            int dropX = Math.floorMod(i * 47 + driftX, VIEW_WIDTH);
            int dropY = Math.floorMod(i * 83 + driftY, VIEW_HEIGHT);
            rain.fillRect(dropX, dropY, 1, 3 + (i & 1));
        }
        rain.dispose();
    }

    private void drawSurvivalIndicators(Graphics2D g) {
        boolean underwater = isHeadUnderwater();
        int panelX = WINDOW_WIDTH - 330, panelY = 16;
        g.setColor(new Color(8, 19, 27, 168));
        g.fillRoundRect(panelX, panelY, 310, underwater ? 99 : 69, 14, 14);
        int startX = panelX + 9;
        for (int i = 0; i < 10; i++) {
            drawHeartIcon(g, startX + i * 21, panelY + 8, playerHealth >= (i + 1) * 10);
            drawMeatIcon(g, startX + i * 21, panelY + 32, hunger >= (i + 1) * 10);
            if (underwater) drawBubbleIcon(g, startX + i * 21, panelY + 69, oxygen >= (i + 1) * 10);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(250, 245, 233));
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
        g.setColor(filled ? new Color(221, 58, 72) : new Color(89, 59, 66));
        g.fill(heart);
        g.setColor(new Color(255, 225, 222, 180));
        g.draw(heart);
    }

    private void drawMeatIcon(Graphics2D g, int x, int y, boolean filled) {
        g.setColor(filled ? new Color(171, 73, 58) : new Color(88, 69, 65));
        g.fillOval(x + 5, y + 3, 13, 11);
        g.setColor(filled ? new Color(239, 173, 143) : new Color(155, 147, 139));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x + 4, y + 12, x + 1, y + 15);
        g.fillOval(x, y + 12, 5, 5);
        g.fillOval(x + 3, y + 15, 5, 5);
        if (filled) {
            g.setColor(new Color(248, 205, 164));
            g.fillOval(x + 9, y + 5, 4, 3);
        }
    }

    private void drawBubbleIcon(Graphics2D g, int x, int y, boolean filled) {
        g.setColor(filled ? new Color(103, 210, 236) : new Color(78, 111, 126));
        g.fillOval(x + 2, y + 2, 15, 15);
        g.setColor(filled ? new Color(220, 250, 255) : new Color(155, 181, 189));
        g.setStroke(new BasicStroke(1.4f));
        g.drawOval(x + 2, y + 2, 15, 15);
        if (filled) g.fillOval(x + 6, y + 5, 4, 4);
    }

    private void drawLobby(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(5, 13, 17, 142));
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        int x = cx - 340, y = cy - 350, width = 680, height = 700;
        g.setColor(new Color(12, 25, 30, 238));
        g.fillRoundRect(x, y, width, height, 28, 28);
        g.setColor(new Color(205, 230, 213, 185));
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(x, y, width, height, 28, 28);

        g.setColor(new Color(174, 219, 154));
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        String title = "TYTANCRAFT";
        g.drawString(title, cx - g.getFontMetrics().stringWidth(title) / 2, y + 59);
        g.setColor(new Color(222, 235, 224));
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        String subtitle = "LOBBY  /  PRZYGOTUJ SIĘ DO WYPRAWY";
        g.drawString(subtitle, cx - g.getFontMetrics().stringWidth(subtitle) / 2, y + 87);
        g.setColor(new Color(255, 255, 255, 42));
        g.drawLine(x + 30, y + 108, x + width - 30, y + 108);

        int cardY = y + 132, cardHeight = 395, cardWidth = 296;
        int leftX = x + 31, rightX = x + width - 31 - cardWidth;
        drawLobbyCard(g, leftX, cardY, cardWidth, cardHeight, "GRACZ");
        drawLobbyCard(g, rightX, cardY, cardWidth, cardHeight, "ŚWIAT");

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(182, 202, 190));
        g.drawString("NAZWA GRACZA", leftX + 18, cardY + 57);
        int nameX = leftX + 17, nameY = cardY + 69, nameWidth = cardWidth - 34;
        g.setColor(nicknameFocused ? new Color(42, 63, 48) : new Color(22, 39, 42));
        g.fillRoundRect(nameX, nameY, nameWidth, 51, 12, 12);
        g.setColor(nicknameFocused ? new Color(173, 220, 151) : new Color(107, 133, 119));
        g.setStroke(new BasicStroke(nicknameFocused ? 2 : 1));
        g.drawRoundRect(nameX, nameY, nameWidth, 51, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        String name = nickname.isEmpty() ? "Wpisz nick..." : nickname;
        g.drawString(name, nameX + 15, nameY + 32);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(172, 194, 180));
        g.drawString("LOKALNY GRACZ", leftX + 19, cardY + 158);
        g.setColor(new Color(133, 205, 117));
        g.fillOval(leftX + 19, cardY + 177, 10, 10);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.setColor(new Color(237, 243, 233));
        g.drawString(nickname.isEmpty() ? "Gracz" : nickname, leftX + 38, cardY + 188);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(153, 191, 153));
        g.drawString("GOTOWY", leftX + cardWidth - 82, cardY + 188);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(182, 202, 190));
        g.drawString("TRYB GRY", rightX + 18, cardY + 55);
        int modeY = cardY + 65, modeButtonHeight = 34;
        drawLobbyModeButton(g, rightX + 17, modeY, 122, modeButtonHeight, "PRZETRWANIE", !creativeMode);
        drawLobbyModeButton(g, rightX + 151, modeY, 128, modeButtonHeight, "KREATYWNY", creativeMode);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(172, 194, 180));
        g.drawString(creativeMode ? "Lot • nieograniczone bloki" : "Zbieraj bloki • dbaj o zdrowie", rightX + 18, cardY + 119);
        g.setColor(new Color(255, 255, 255, 42));
        g.drawLine(rightX + 18, cardY + 132, rightX + cardWidth - 18, cardY + 132);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(182, 202, 190));
        g.drawString("ZIARNO ŚWIATA", rightX + 18, cardY + 152);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(218, 232, 205));
        g.drawString(Long.toUnsignedString(seed), rightX + 18, cardY + 174);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(creativeMode ? new Color(127, 143, 134) : new Color(182, 202, 190));
        g.drawString("AGRESYWNOŚĆ ZWIERZĄT", rightX + 18, cardY + 198);
        String aggressionValue = creativeMode ? "—" : aggressionLevel + "%";
        g.drawString(aggressionValue, rightX + cardWidth - 18 - g.getFontMetrics().stringWidth(aggressionValue), cardY + 198);
        int sliderX = rightX + 20, sliderY = cardY + 211, sliderWidth = cardWidth - 40;
        g.setColor(new Color(255, 255, 255, 48));
        g.fillRoundRect(sliderX, sliderY, sliderWidth, 6, 6, 6);
        int knobX = sliderX + sliderWidth * aggressionLevel / 100;
        g.setColor(creativeMode ? new Color(105, 119, 111) : new Color(146, 202, 121));
        g.fillRoundRect(sliderX, sliderY, knobX - sliderX, 6, 6, 6);
        g.fillOval(knobX - 8, sliderY - 5, 16, 16);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(164, 184, 171));
        g.drawString(creativeMode ? "Dostępne w trybie przetrwania"
                : aggressionLevel < 35 ? "Spokojne" : aggressionLevel < 70 ? "Płochliwe" : "Atakujące",
                rightX + 18, cardY + 235);

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(182, 202, 190));
        g.drawString("SZYBKOŚĆ PÓR DNIA", rightX + 18, cardY + 258);
        String speedLabel = String.format("%.1fx", daySpeed);
        g.drawString(speedLabel, rightX + cardWidth - 18 - g.getFontMetrics().stringWidth(speedLabel), cardY + 258);
        int timeSliderX = rightX + 20, timeSliderY = cardY + 273, timeSliderWidth = cardWidth - 40;
        g.setColor(new Color(255, 255, 255, 48));
        g.fillRoundRect(timeSliderX, timeSliderY, timeSliderWidth, 6, 6, 6);
        int timeKnobX = timeSliderX + Math.round(timeSliderWidth * daySpeed / 8.0f);
        g.setColor(new Color(116, 190, 218));
        g.fillRoundRect(timeSliderX, timeSliderY, timeKnobX - timeSliderX, 6, 6, 6);
        g.fillOval(timeKnobX - 8, timeSliderY - 5, 16, 16);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(164, 184, 171));
        g.drawString("0x — zatrzymany", rightX + 18, cardY + 299);
        String maxSpeed = "8x";
        g.drawString(maxSpeed, rightX + cardWidth - 18 - g.getFontMetrics().stringWidth(maxSpeed), cardY + 299);

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(new Color(182, 202, 190));
        g.drawString("ZASIĘG RENDEROWANIA", rightX + 18, cardY + 326);
        String distanceLabel = renderDistance + "x";
        g.drawString(distanceLabel, rightX + cardWidth - 18 - g.getFontMetrics().stringWidth(distanceLabel), cardY + 326);
        int distanceSliderY = cardY + 341;
        g.setColor(new Color(255, 255, 255, 48));
        g.fillRoundRect(timeSliderX, distanceSliderY, timeSliderWidth, 6, 6, 6);
        int distanceKnobX = timeSliderX + (renderDistance - 2) * timeSliderWidth / 6;
        g.setColor(new Color(203, 174, 119));
        g.fillRoundRect(timeSliderX, distanceSliderY, distanceKnobX - timeSliderX, 6, 6, 6);
        g.fillOval(distanceKnobX - 8, distanceSliderY - 5, 16, 16);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(new Color(164, 184, 171));
        g.drawString("2–8x widoku  •  dalej obraz znika we mgle", rightX + 18, cardY + 368);

        int buttonX = cx - 165, buttonY = y + 565, buttonWidth = 330, buttonHeight = 63;
        g.setColor(new Color(105, 159, 83));
        g.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 17, 17);
        g.setColor(new Color(194, 232, 167));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 17, 17);
        g.setColor(new Color(16, 31, 22));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        String startLabel = "WEJDŹ DO ŚWIATA";
        g.drawString(startLabel, cx - g.getFontMetrics().stringWidth(startLabel) / 2, buttonY + 39);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(173, 193, 179));
        String hint = "Kliknij pole, aby zmienić nick  •  ENTER rozpoczyna grę";
        g.drawString(hint, cx - g.getFontMetrics().stringWidth(hint) / 2, y + height - 20);
    }

    private void drawLobbyCard(Graphics2D g, int x, int y, int width, int height, String title) {
        g.setColor(new Color(22, 39, 42, 232));
        g.fillRoundRect(x, y, width, height, 18, 18);
        g.setColor(new Color(255, 255, 255, 48));
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(x, y, width, height, 18, 18);
        g.setColor(new Color(177, 215, 166));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(title, x + 18, y + 29);
    }

    private void drawLobbyModeButton(Graphics2D g, int x, int y, int width, int height,
                                     String label, boolean selected) {
        g.setColor(selected ? new Color(91, 139, 76) : new Color(31, 49, 48));
        g.fillRoundRect(x, y, width, height, 10, 10);
        g.setColor(selected ? new Color(191, 225, 163) : new Color(255, 255, 255, 54));
        g.setStroke(new BasicStroke(selected ? 1.5f : 1));
        g.drawRoundRect(x, y, width, height, 10, 10);
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.setColor(selected ? Color.WHITE : new Color(190, 207, 194));
        g.drawString(label, x + (width - g.getFontMetrics().stringWidth(label)) / 2,
                y + (height + g.getFontMetrics().getAscent()) / 2 - 2);
    }

    private void setAggressionFromMouse(int mouseX) {
        int sliderX = WINDOW_WIDTH / 2 + 33;
        int sliderWidth = 256;
        aggressionLevel = Math.max(0, Math.min(100, (mouseX - sliderX) * 100 / sliderWidth));
    }

    private void setDaySpeedFromMouse(int mouseX) {
        int sliderX = WINDOW_WIDTH / 2 + 33;
        int sliderWidth = 256;
        float value = Math.max(0, Math.min(8, (mouseX - sliderX) * 8.0f / sliderWidth));
        daySpeed = Math.round(value * 10.0f) / 10.0f;
    }

    private void setRenderDistanceFromMouse(int mouseX) {
        int sliderX = WINDOW_WIDTH / 2 + 33;
        int sliderWidth = 256;
        float proportion = Math.max(0, Math.min(1, (mouseX - sliderX) / (float) sliderWidth));
        renderDistance = 2 + Math.round(proportion * 6);
    }

    private void enterWorld() {
        if (nickname.isEmpty()) nickname = "Gracz";
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
        nicknameFocused = false;
        inLobby = false;
        paused = false;
        captureMouse();
    }

    private void drawBlockIcon(Graphics2D g, int x, int y, byte type) {
        if (World.isFishSpawnEgg(type)) {
            drawFishEggIcon(g, x, y, type);
            return;
        }
        if (World.isFlower(type)) {
            drawFlowerIcon(g, x, y, type);
            return;
        }
        if (World.isMushroom(type)) {
            drawMushroomIcon(g, x, y, type);
            return;
        }

        Color material = blockColor(type);
        Color top = material.brighter();
        Color front = material;
        Color side = material.darker();
        if (type == World.GRASS) {
            front = new Color(111, 91, 61);
            side = new Color(83, 68, 49);
        } else if (type == World.LEAVES || type == World.BIRCH_LEAVES || type == World.PINE_LEAVES
                || type == World.ACACIA_LEAVES || type == World.CHERRY_LEAVES) {
            top = material.brighter();
            side = new Color(Math.max(15, material.getRed() - 27), Math.max(20, material.getGreen() - 27),
                    Math.max(18, material.getBlue() - 27));
        }

        Polygon topFace = new Polygon(new int[]{x + 2, x + 10, x + 34, x + 26},
                new int[]{y + 6, y, y + 6, y + 12}, 4);
        Polygon frontFace = new Polygon(new int[]{x + 2, x + 26, x + 26, x + 2},
                new int[]{y + 6, y + 12, y + 32, y + 26}, 4);
        Polygon sideFace = new Polygon(new int[]{x + 26, x + 34, x + 34, x + 26},
                new int[]{y + 12, y + 6, y + 26, y + 32}, 4);
        g.setColor(top); g.fillPolygon(topFace);
        g.setColor(front); g.fillPolygon(frontFace);
        g.setColor(side); g.fillPolygon(sideFace);

        if (type == World.GRASS) {
            g.setColor(new Color(111, 164, 69));
            g.fillPolygon(new int[]{x + 2, x + 26, x + 26, x + 2},
                    new int[]{y + 6, y + 12, y + 16, y + 10}, 4);
            g.setColor(new Color(84, 137, 57));
            g.fillPolygon(new int[]{x + 26, x + 34, x + 34, x + 26},
                    new int[]{y + 12, y + 6, y + 10, y + 16}, 4);
        }
        drawBlockFaceDetails(g, frontFace, x, y, type, front);

        if (isLog(type)) {
            Shape oldClip = g.getClip();
            g.clip(topFace);
            g.setColor(material.darker());
            g.drawOval(x + 12, y + 2, 9, 7);
            g.setColor(material.brighter());
            g.fillOval(x + 15, y + 4, 3, 2);
            g.setClip(oldClip);
        }
        g.setColor(new Color(17, 24, 25, 190));
        g.drawPolygon(topFace); g.drawPolygon(frontFace); g.drawPolygon(sideFace);
    }

    private void drawBlockFaceDetails(Graphics2D g, Polygon face, int x, int y, byte type, Color base) {
        Shape oldClip = g.getClip();
        g.clip(face);
        if (isLog(type)) {
            g.setStroke(new BasicStroke(1));
            for (int stripe = 0; stripe < 5; stripe++) {
                int stripeX = x + 5 + stripe * 4;
                g.setColor((stripe & 1) == 0 ? base.darker() : base.brighter());
                g.drawLine(stripeX, y + 10, stripeX, y + 29);
            }
        } else {
            int specks = isLeafBlock(type) ? 12 : isOre(type) ? 7 : 5;
            for (int i = 0; i < specks; i++) {
                int px = x + 4 + Math.floorMod(i * 11 + (type & 0xff) * 3, 20);
                int py = y + 10 + Math.floorMod(i * 7 + (type & 0xff) * 5, 16);
                if (isOre(type) && i % 2 == 0) g.setColor(base.brighter());
                else if (isLeafBlock(type)) g.setColor(i % 3 == 0 ? base.darker() : base.brighter());
                else g.setColor(i % 2 == 0 ? base.darker() : base.brighter());
                g.fillRect(px, py, isLeafBlock(type) ? 2 : 1, isLeafBlock(type) ? 2 : 1);
            }
            if (type == World.PLANKS) {
                g.setColor(base.darker());
                for (int line = 0; line < 3; line++) g.drawLine(x + 4, y + 14 + line * 6, x + 24, y + 14 + line * 6);
            }
        }
        g.setClip(oldClip);
    }

    private void drawFlowerIcon(Graphics2D g, int x, int y, byte type) {
        Color petals = blockColor(type);
        g.setColor(new Color(57, 113, 55));
        g.fillRect(x + 15, y + 13, 3, 18);
        g.fillRect(x + 9, y + 22, 7, 3);
        g.fillRect(x + 18, y + 26, 6, 3);
        g.setColor(petals.darker());
        g.fillRect(x + 10, y + 4, 8, 7);
        g.fillRect(x + 17, y + 4, 8, 7);
        g.fillRect(x + 13, y, 9, 7);
        g.fillRect(x + 13, y + 9, 9, 6);
        g.setColor(petals.brighter());
        g.fillRect(x + 13, y + 4, 10, 7);
        g.setColor(new Color(246, 211, 72));
        g.fillRect(x + 17, y + 6, 4, 4);
    }

    private void drawMushroomIcon(Graphics2D g, int x, int y, byte type) {
        boolean red = type <= World.RED_MUSHROOM_LARGE;
        boolean small = type == World.RED_MUSHROOM_SMALL || type == World.BROWN_MUSHROOM_SMALL;
        boolean large = type == World.RED_MUSHROOM_LARGE || type == World.BROWN_MUSHROOM_LARGE;
        Color cap = red ? new Color(184, 54, 54) : new Color(139, 91, 55);
        Color capLight = cap.brighter(), stem = new Color(231, 222, 197);
        int stemX = large ? x + 13 : x + 15;
        int stemY = small ? y + 13 : large ? y + 15 : y + 14;
        int stemWidth = large ? 8 : 5;
        int stemHeight = small ? 15 : large ? 13 : 14;
        g.setColor(new Color(177, 164, 139));
        g.fillRect(stemX + 1, stemY, stemWidth, stemHeight);
        g.setColor(stem);
        g.fillRect(stemX, stemY, stemWidth, stemHeight - 1);

        if (small) {
            g.setColor(cap.darker()); g.fillRect(x + 10, y + 7, 20, 7);
            g.setColor(cap); g.fillRect(x + 8, y + 5, 24, 8);
            g.setColor(capLight); g.fillRect(x + 13, y + 3, 14, 4);
        } else if (large) {
            g.setColor(cap.darker()); g.fillRect(x + 4, y + 10, 31, 7);
            g.setColor(cap); g.fillRect(x + 2, y + 7, 34, 10);
            g.setColor(capLight); g.fillRect(x + 8, y + 4, 24, 6);
            g.fillRect(x + 14, y + 2, 12, 4);
        } else {
            g.setColor(cap.darker()); g.fillRect(x + 7, y + 9, 26, 7);
            g.setColor(cap); g.fillRect(x + 4, y + 6, 32, 10);
            g.setColor(capLight); g.fillRect(x + 10, y + 3, 20, 6);
        }
        g.setColor(red ? new Color(244, 224, 190) : new Color(208, 174, 127));
        g.fillRect(x + 11, y + 8, 3, 2);
        g.fillRect(x + 25, y + 11, 3, 2);
        if (large) g.fillRect(x + 18, y + 5, 3, 2);
    }

    private void drawFishEggIcon(Graphics2D g, int x, int y, byte type) {
        Color shell = type == World.COD_SPAWN_EGG ? new Color(112, 161, 174)
                : type == World.SALMON_SPAWN_EGG ? new Color(221, 126, 100)
                : type == World.TROPICAL_FISH_SPAWN_EGG ? new Color(70, 190, 174)
                : type == World.PIKE_SPAWN_EGG ? new Color(128, 157, 76) : new Color(193, 160, 82);
        int[] eggX = {x + 17, x + 25, x + 28, x + 25, x + 20, x + 12, x + 8, x + 9};
        int[] eggY = {y + 2, y + 7, y + 15, y + 22, y + 28, y + 26, y + 18, y + 9};
        g.setColor(shell.darker());
        g.fillPolygon(eggX, eggY, eggX.length);
        g.setColor(shell);
        g.fillPolygon(new int[]{x + 16, x + 22, x + 24, x + 21, x + 17, x + 11, x + 11, x + 12},
                new int[]{y + 3, y + 8, y + 15, y + 21, y + 25, y + 23, y + 17, y + 9}, 8);
        g.setColor(shell.brighter());
        g.fillRect(x + 14, y + 7, 4, 3);
        g.fillRect(x + 12, y + 13, 3, 3);
    }

    private Color blockColor(byte type) {
        if (type == World.GRASS) return new Color(96, 161, 68);
        if (type == World.DIRT) return new Color(145, 101, 65);
        if (type == World.STONE) return new Color(145, 153, 153);
        if (type == World.SAND) return new Color(223, 203, 145);
        if (type == World.WOOD) return new Color(142, 93, 50);
        if (type == World.LEAVES) return new Color(71, 139, 76);
        if (type == World.COAL_ORE) return new Color(62, 67, 73);
        if (type == World.COPPER_ORE) return new Color(190, 112, 70);
        if (type == World.IRON_ORE) return new Color(175, 132, 108);
        if (type == World.GOLD_ORE) return new Color(226, 186, 65);
        if (type == World.DIAMOND_ORE) return new Color(81, 211, 225);
        if (type == World.EMERALD_ORE) return new Color(47, 192, 111);
        if (type == World.RUBY_ORE) return new Color(208, 57, 79);
        if (type == World.SAPPHIRE_ORE) return new Color(71, 116, 229);
        if (type == World.SNOW) return new Color(223, 236, 241);
        if (type == World.RED_FLOWER) return new Color(215, 65, 83);
        if (type == World.YELLOW_FLOWER) return new Color(244, 205, 68);
        if (type == World.BLUE_FLOWER) return new Color(87, 120, 226);
        if (type == World.BIRCH_WOOD) return new Color(224, 215, 179);
        if (type == World.BIRCH_LEAVES) return new Color(116, 168, 80);
        if (type == World.PINE_WOOD) return new Color(96, 69, 44);
        if (type == World.PINE_LEAVES) return new Color(37, 89, 56);
        if (type == World.ACACIA_WOOD) return new Color(166, 105, 57);
        if (type == World.ACACIA_LEAVES) return new Color(126, 142, 62);
        if (type == World.CHERRY_WOOD) return new Color(129, 77, 73);
        if (type == World.CHERRY_LEAVES) return new Color(224, 135, 173);
        if (type == World.PLANKS) return new Color(175, 125, 75);
        if (type == World.TORCH) return new Color(245, 166, 57);
        return new Color(32, 154, 93);
    }

    private static boolean isLog(byte type) {
        return type == World.WOOD || type == World.BIRCH_WOOD || type == World.PINE_WOOD
                || type == World.ACACIA_WOOD || type == World.CHERRY_WOOD;
    }

    private static boolean isLeafBlock(byte type) {
        return type == World.LEAVES || type == World.BIRCH_LEAVES || type == World.PINE_LEAVES
                || type == World.ACACIA_LEAVES || type == World.CHERRY_LEAVES;
    }

    private static boolean isOre(byte type) {
        return type >= World.COAL_ORE && type <= World.SAPPHIRE_ORE;
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
        g.setFont(new Font("SansSerif", Font.BOLD, 34));
        String title = "TYTANCRAFT";
        g.drawString(title, cx - g.getFontMetrics().stringWidth(title) / 2, y + 56);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
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
            if (aimedAnimalEntity.takeHit() && !creativeMode) meatCount = Math.min(99, meatCount + aimedAnimalEntity.meatDrop);
            return;
        }
        if (!hasHit || hitY < 0 || hitX < 0 || hitZ < 0 || hitX >= World.WIDTH || hitZ >= World.DEPTH) return;
        if (aimedBlockDistance > INTERACTION_REACH || blockActionCooldown > 0) return;
        if (place) {
            int x = hitX + hitNormalX, y = hitY + hitNormalY, z = hitZ + hitNormalZ;
            if (!World.inBounds(x, y, z)) return;
            byte selectedType = hotbar[selectedBlock];
            int itemIndex = selectedType & 0xff;
            byte targetBlock = world.get(x, y, z);
            boolean replaceable = targetBlock == World.AIR || targetBlock == World.WATER;
            boolean overlapsPlayer = x + 1 > playerX - 0.23f && x < playerX + 0.23f
                    && z + 1 > playerZ - 0.23f && z < playerZ + 0.23f
                    && y + 1 > playerY && y < playerY + 1.8f;
            if (replaceable && !overlapsPlayer
                    && (creativeMode || inventory[itemIndex] > 0)) {
                world.set(x, y, z, selectedType);
                if (!creativeMode) inventory[itemIndex]--;
                blockActionCooldown = 0.20f;
            }
        } else if (hitY > 0) {
            byte minedBlock = world.get(hitX, hitY, hitZ);
            world.set(hitX, hitY, hitZ, World.AIR);
            world.flowWaterInto(hitX, hitY, hitZ);
            if (!creativeMode && minedBlock != World.AIR && minedBlock != World.WATER) {
                int itemIndex = minedBlock & 0xff;
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
        if (key == KeyEvent.VK_R && !wasPressed) eatFood();
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
        if (inventoryOpen) {
            if (e.getButton() == MouseEvent.BUTTON1) handleInventoryClick(e.getX(), e.getY());
            return;
        }
        if (inLobby) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                int left = WINDOW_WIDTH / 2 - 292;
                int top = WINDOW_HEIGHT / 2 - 149;
                int startX = WINDOW_WIDTH / 2 - 165;
                int startY = WINDOW_HEIGHT / 2 + 215;
                int rightX = WINDOW_WIDTH / 2 + 13;
                int cardY = WINDOW_HEIGHT / 2 - 218;
                int modeY = cardY + 65;
                if (inside(e.getX(), e.getY(), rightX + 17, modeY, 122, 34)) {
                    creativeMode = false;
                    nicknameFocused = false;
                    return;
                }
                if (inside(e.getX(), e.getY(), rightX + 151, modeY, 128, 34)) {
                    creativeMode = true;
                    nicknameFocused = false;
                    aggressionDragging = false;
                    return;
                }
                int sliderX = rightX + 20, sliderY = cardY + 211, sliderWidth = 256;
                if (!creativeMode && inside(e.getX(), e.getY(), sliderX - 8, sliderY - 9, sliderWidth + 16, 24)) {
                    aggressionDragging = true;
                    setAggressionFromMouse(e.getX());
                    return;
                }
                int timeSliderY = cardY + 273;
                if (inside(e.getX(), e.getY(), sliderX - 8, timeSliderY - 9, sliderWidth + 16, 24)) {
                    timeSpeedDragging = true;
                    setDaySpeedFromMouse(e.getX());
                    return;
                }
                int distanceSliderY = cardY + 341;
                if (inside(e.getX(), e.getY(), sliderX - 8, distanceSliderY - 9, sliderWidth + 16, 24)) {
                    renderDistanceDragging = true;
                    setRenderDistanceFromMouse(e.getX());
                    return;
                }
                if (e.getX() >= left && e.getX() <= left + 262
                        && e.getY() >= top && e.getY() <= top + 51) {
                    nicknameFocused = true;
                } else if (e.getX() >= startX && e.getX() <= startX + 330
                        && e.getY() >= startY && e.getY() <= startY + 63) {
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
            setAggressionFromMouse(e.getX());
            return;
        }
        if (inLobby && timeSpeedDragging) {
            setDaySpeedFromMouse(e.getX());
            return;
        }
        if (inLobby && renderDistanceDragging) {
            setRenderDistanceFromMouse(e.getX());
            return;
        }
        mouseMoved(e);
    }
    @Override public void mouseWheelMoved(MouseWheelEvent e) {
        if (inventoryOpen) return;
        selectedBlock = Math.floorMod(selectedBlock + e.getWheelRotation(), hotbar.length);
    }

    private String blockName(byte type) {
        for (int i = 0; i < BLOCK_TYPES.length; i++) if (BLOCK_TYPES[i] == type) return BLOCK_NAMES[i];
        return "Blok";
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
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
        final Animal animal;
        final Animal.Kind kind;
        final int color;
        final float distance, nx, ny, nz, x, y, z;

        AnimalHit(Animal animal, int color, float distance, float nx, float ny, float nz,
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
