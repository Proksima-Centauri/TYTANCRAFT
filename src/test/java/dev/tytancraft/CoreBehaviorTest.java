package dev.tytancraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class CoreBehaviorTest {
    private CoreBehaviorTest() {}

    public static void main(String[] args) throws Exception {
        String previousHome = System.getProperty("user.home");
        Path temporaryHome = Files.createTempDirectory("tytancraft-test-");
        System.setProperty("user.home", temporaryHome.toString());
        try {
            World first = testSeededWorlds();
            testBlockRegistry();
            testIncrementalWaterAndSaveRoundTrip();
            testLadderClimbing();
            testFoodAndQueuedCraft(first);
            System.out.println("TytanCraft core tests passed.");
        } finally {
            deleteTree(temporaryHome);
            if (previousHome == null) System.clearProperty("user.home");
            else System.setProperty("user.home", previousHome);
        }
    }

    private static World testSeededWorlds() {
        World first = new World(123L);
        World sameSeed = new World(123L);
        World otherSeed = new World(124L);
        check(Arrays.equals(first.copyBlocks(), sameSeed.copyBlocks()), "same seed must recreate the same world");

        int minHeight = World.HEIGHT, maxHeight = 0, differentHeights = 0;
        int grass = 0, snow = 0, sand = 0;
        for (int z = 0; z < World.DEPTH; z++) {
            for (int x = 0; x < World.WIDTH; x++) {
                int height = first.heightAt(x, z);
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
                if (height != otherSeed.heightAt(x, z)) differentHeights++;
                byte surface = first.get(x, height, z);
                if (surface == World.GRASS) grass++;
                if (surface == World.SNOW) snow++;
                if (surface == World.SAND) sand++;
            }
        }
        check(differentHeights > World.WIDTH * World.DEPTH / 2, "different seeds should create distinct terrain");
        check(minHeight <= 5 && maxHeight >= 50, "terrain should contain lowlands and high peaks");
        check(grass > 0 && snow > 0 && sand > 0, "world should contain varied surface biomes");
        return first;
    }

    private static void testBlockRegistry() {
        check("Wełna".equals(BlockRegistry.info(World.WOOL).name()), "block names should come from the registry");
        check(BlockRegistry.has(World.DIAMOND_ORE, BlockRegistry.ORE), "ores should expose their registry flag");
        check(!World.isSolid(World.RED_FLOWER) && World.isSolid(World.TORCH),
                "registry solidity flags should preserve plant and block collision");
        check(World.isLadderBlock(World.LADDER) && !World.isSolid(World.LADDER)
                        && World.ladderAgainstWall(1, 0) == World.LADDER_X_NEG,
                "ladder variants should be climbable and mount against wall faces");
        check("grass_block".equals(BlockRenderAssets.slugFor(World.GRASS))
                        && "white_wool".equals(BlockRenderAssets.slugFor(World.WOOL))
                        && "ladder".equals(BlockRenderAssets.slugFor(World.LADDER)),
                "vanilla blocks should map to BlockRender item icons");
        check("redstone_ore".equals(BlockRenderAssets.slugFor(World.RUBY_ORE))
                        && "pufferfish_spawn_egg".equals(BlockRenderAssets.slugFor(World.PIKE_SPAWN_EGG)),
                "custom blocks and fish eggs should use their closest available BlockRender icons");
    }

    private static void testIncrementalWaterAndSaveRoundTrip() throws Exception {
        int volume = World.WIDTH * World.HEIGHT * World.DEPTH;
        World world = new World(5L, new byte[volume], new short[volume], new long[0]);
        world.set(10, 10, 10, World.TORCH);
        check(world.torchLightAt(10, 10, 10) == 15 && world.torchLightAt(11, 10, 10) == 13,
                "torch should illuminate nearby blocks");
        check(world.torchLightAt(18, 10, 10) == 0, "torch light should have a finite radius");
        world.set(10, 10, 10, World.AIR);
        check(world.torchLightAt(11, 10, 10) == 0, "removing a torch should remove its light");
        world.set(10, 10, 10, World.WATER);
        world.flowWaterInto(11, 10, 10);
        check(world.get(11, 10, 10) == World.AIR, "water should be scheduled rather than spread synchronously");
        world.tickWaterFlow();
        check(world.get(11, 10, 10) == World.WATER, "water should spread during a simulation tick");
        check(world.copyPendingWaterFlow().length > 0, "large water flow should continue across later ticks");
        int floodedCells = 0;
        for (byte block : world.copyBlocks()) if (block == World.WATER) floodedCells++;
        check(floodedCells < volume / 100, "one water-flow tick should process only a small bounded region");

        WorldStorage.Data data = new WorldStorage.Data();
        data.seed = 5L;
        data.nickname = "Test";
        data.blocks = world.copyBlocks();
        data.waterFlow = world.copyWaterFlow();
        data.waterQueue = world.copyPendingWaterFlow();
        data.inventory = new int[48];
        data.inventory[World.WOOL] = 7;
        data.hotbar = new byte[10];
        data.playerX = 14.25f;
        data.playerY = 16.0f;
        data.playerZ = 23.5f;
        data.animals = List.of();
        WorldStorage.save(data);
        WorldStorage.SaveInfo save = WorldStorage.list().get(0);
        WorldStorage.Data loaded = WorldStorage.load(save.path);
        World restored = new World(loaded.seed, loaded.blocks, loaded.waterFlow, loaded.waterQueue);
        check(restored.get(11, 10, 10) == World.WATER, "saved blocks should be restored");
        check(loaded.inventory[World.WOOL] == 7, "saved inventory should be restored");
        check(loaded.playerX == 14.25f && loaded.playerY == 16.0f && loaded.playerZ == 23.5f,
                "saved player position should be restored");
        check(Arrays.equals(world.copyPendingWaterFlow(), restored.copyPendingWaterFlow()),
                "pending water propagation should survive save/load");
    }

    private static void testLadderClimbing() throws Exception {
        int volume = World.WIDTH * World.HEIGHT * World.DEPTH;
        World world = new World(88L, new byte[volume], new short[volume], new long[0]);
        world.set(95, 1, 96, World.STONE);
        world.set(96, 1, 96, World.LADDER_X_NEG);
        var constructor = TytanCraft.class.getDeclaredConstructor(long.class, World.class);
        constructor.setAccessible(true);
        TytanCraft game = constructor.newInstance(88L, world);
        field("inLobby").setBoolean(game, false);
        field("captured").setBoolean(game, true);
        field("playerX").setFloat(game, 96.5f);
        field("playerY").setFloat(game, 1.0f);
        field("playerZ").setFloat(game, 96.5f);
        boolean[] keys = (boolean[]) field("keys").get(game);
        keys[KeyEvent.VK_W] = true;
        invoke(game, "update", new Class<?>[]{float.class}, 0.1f);
        check(field("playerY").getFloat(game) > 1.2f,
                "holding W next to a ladder should climb upward instead of falling");
        keys[KeyEvent.VK_W] = false;
        keys[KeyEvent.VK_S] = true;
        float highPosition = field("playerY").getFloat(game);
        invoke(game, "update", new Class<?>[]{float.class}, 0.1f);
        check(field("playerY").getFloat(game) < highPosition,
                "holding S next to a ladder should climb downward");
    }

    private static void testFoodAndQueuedCraft(World world) throws Exception {
        var constructor = TytanCraft.class.getDeclaredConstructor(long.class, World.class);
        constructor.setAccessible(true);
        TytanCraft game = constructor.newInstance(123L, world);
        game.setSize(1920, 1080);
        int virtualCenterX = (int) invoke(game, "toVirtualX", new Class<?>[]{int.class}, 960);
        int virtualCenterY = (int) invoke(game, "toVirtualY", new Class<?>[]{int.class}, 540);
        check(virtualCenterX == 640 && virtualCenterY == 360,
                "fullscreen mouse input should scale into virtual UI coordinates");
        game.setSize(1280, 720);
        BufferedImage lobbyFrame = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D lobbyGraphics = lobbyFrame.createGraphics();
        invoke(game, "drawLobby", new Class<?>[]{Graphics2D.class, int.class, int.class},
                lobbyGraphics, 640, 360);
        lobbyGraphics.dispose();
        Field inventoryField = field("inventory");
        int[] inventory = (int[]) inventoryField.get(game);
        Field hunger = field("hunger"), health = field("playerHealth");
        inventory[World.RED_MUSHROOM_SMALL] = 1;
        hunger.setFloat(game, 100);
        health.setInt(game, 100);
        invoke(game, "eatFood");
        check(inventory[World.RED_MUSHROOM_SMALL] == 1 && health.getInt(game) == 100,
                "poisonous mushrooms should not be eaten at full hunger");

        hunger.setFloat(game, 70);
        field("inLobby").setBoolean(game, false);
        invoke(game, "keyPressed", new Class<?>[]{KeyEvent.class},
                new KeyEvent(game, KeyEvent.KEY_PRESSED, 1L, 0, KeyEvent.VK_R, 'r'));
        check(inventory[World.RED_MUSHROOM_SMALL] == 1 && health.getInt(game) == 100,
                "food key should queue consumption for the simulation thread");
        invoke(game, "update", new Class<?>[]{float.class}, 0.0f);
        check(inventory[World.RED_MUSHROOM_SMALL] == 0 && health.getInt(game) == 94,
                "poison should apply during update when the player is hungry");

        Field inventoryTab = field("inventoryTab");
        inventoryTab.setInt(game, 3);
        inventory[World.WOOD] = 1;
        invoke(game, "handleInventoryClick", new Class<?>[]{int.class, int.class}, 663, 276);
        check(inventory[World.WOOD] == 1 && inventory[World.PLANKS] == 0,
                "crafting input should wait for the game update thread");
        invoke(game, "update", new Class<?>[]{float.class}, 0.0f);
        check(inventory[World.WOOD] == 0 && inventory[World.PLANKS] == 4,
                "queued crafting should be applied during game update");
        BufferedImage craftingFrame = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D craftingGraphics = craftingFrame.createGraphics();
        invoke(game, "drawCraftingDialog", new Class<?>[]{Graphics2D.class, int.class, int.class},
                craftingGraphics, 155, 45);
        craftingGraphics.dispose();
        for (int tab = 0; tab < 3; tab++) {
            inventoryTab.setInt(game, tab);
            BufferedImage inventoryFrame = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
            Graphics2D inventoryGraphics = inventoryFrame.createGraphics();
            invoke(game, "drawInventoryDialog", new Class<?>[]{Graphics2D.class, int.class, int.class},
                    inventoryGraphics, 640, 360);
            inventoryGraphics.dispose();
        }
        invoke(game, "render");
        Method cloudSample = TytanCraft.class.getDeclaredMethod("sampleCloudMap", float.class, float.class);
        cloudSample.setAccessible(true);
        float beforeWrap = (float) cloudSample.invoke(null, 127.999f, 38.25f);
        float afterWrap = (float) cloudSample.invoke(null, 128.001f, 38.25f);
        check(Math.abs(beforeWrap - afterWrap) < 0.01f, "cloud map should blend continuously across its tile edge");

        Field rainIntensity = field("rainIntensity"), cloudCoverage = field("cloudCoverage");
        Field playerY = field("playerY");
        rainIntensity.setFloat(game, 1.0f);
        playerY.setFloat(game, World.HEIGHT - 2.0f);
        Method drawRain = TytanCraft.class.getDeclaredMethod("drawRain", Graphics2D.class);
        drawRain.setAccessible(true);
        BufferedImage rainImage = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rainImage.createGraphics();
        cloudCoverage.setFloat(game, 0.05f);
        drawRain.invoke(game, graphics);
        check(countColoredPixels(rainImage) == 0, "clear skies should produce no rain streaks");
        cloudCoverage.setFloat(game, 0.95f);
        drawRain.invoke(game, graphics);
        check(countColoredPixels(rainImage) > 0, "rain should appear when cloud cover is present");
        graphics.dispose();

        Field spawnX = field("spawnX"), spawnZ = field("spawnZ"), worldField = field("world");
        World gameWorld = (World) worldField.get(game);
        int sx = spawnX.getInt(game), sz = spawnZ.getInt(game);
        playerY.setFloat(game, gameWorld.heightAt(sx, sz) + 1.0f);
        Field splashTimer = field("rainSplashTimer");
        splashTimer.setFloat(game, 0);
        Method updateSplashes = TytanCraft.class.getDeclaredMethod("updateRainSplashes", float.class);
        updateSplashes.setAccessible(true);
        cloudCoverage.setFloat(game, 0.05f);
        for (int i = 0; i < 10; i++) updateSplashes.invoke(game, 0.1f);
        check(((List<?>) field("rainSplashes").get(game)).isEmpty(),
                "clear skies should not produce surface impacts");
        cloudCoverage.setFloat(game, 0.99f);
        splashTimer.setFloat(game, 0);
        for (int i = 0; i < 40; i++) updateSplashes.invoke(game, 0.05f);
        check(!((List<?>) field("rainSplashes").get(game)).isEmpty(),
                "rain should generate animated surface impacts under cloud cover");
        BufferedImage splashFrame = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D splashGraphics = splashFrame.createGraphics();
        Method drawSplashes = TytanCraft.class.getDeclaredMethod("drawRainSplashes", Graphics2D.class);
        drawSplashes.setAccessible(true);
        drawSplashes.invoke(game, splashGraphics);
        check(countColoredPixels(splashFrame) > 0, "surface impacts should render animated splash marks");
        splashGraphics.dispose();
    }

    private static int countColoredPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) if ((image.getRGB(x, y) & 0x00ffffff) != 0) count++;
        }
        return count;
    }

    private static Field field(String name) throws Exception {
        Field field = TytanCraft.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object invoke(Object target, String name) throws Exception {
        return invoke(target, name, new Class<?>[0]);
    }

    private static Object invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = TytanCraft.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
