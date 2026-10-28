package dev.tytancraft;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class CoreBehaviorTest {
    private CoreBehaviorTest() {}

    public static void main(String[] args) throws Exception {
        String previousHome = System.getProperty("user.home");
        Path temporaryHome = Files.createTempDirectory("tytancraft-test-");
        System.setProperty("user.home", temporaryHome.toString());
        try {
            World first = testSeededWorlds();
            testBlockRegistry();
            testProtocolFrames();
            testDedicatedServer();
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

    private static void testProtocolFrames() throws Exception {
        byte[] hello = Protocol.hello("Gracz_1");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            Protocol.writeFrame(output, Protocol.Type.HELLO, hello);
        }
        Protocol.Frame frame = Protocol.readFrame(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        check(frame.type() == Protocol.Type.HELLO, "protocol type should survive serialization");
        try (DataInputStream input = Protocol.input(frame.payload())) {
            check(input.readInt() == Protocol.VERSION, "protocol version should survive serialization");
            check("Gracz_1".equals(Protocol.readNickname(input)), "protocol nickname should survive serialization");
            Protocol.requireConsumed(input);
        }
    }

    private static void testDedicatedServer() throws Exception {
        long seed = 987654321L;
        GameServer server = new GameServer(seed, 0);
        ArrayList<TestClient> clients = new ArrayList<>();
        Socket eleventh = null;
        NetworkClient transportClient = null;
        try {
            server.start();
            TestClient first = connectTestClient(server.port(), "Sieciowy1");
            TestClient second = connectTestClient(server.port(), "Sieciowy2");
            clients.add(first);
            clients.add(second);
            check(first.welcome.seed == seed, "server handshake should announce its seed");
            World generated = new World(seed);
            check(Arrays.equals(first.welcome.world.blocks(), generated.copyBlocks()),
                    "server and client seed should recreate identical blocks");

            int targetX = -1, targetY = -1, targetZ = first.welcome.spawnZ;
            for (int offset = 3; offset <= 5; offset++) {
                int x = first.welcome.spawnX + offset;
                if (x >= World.WIDTH - 2) continue;
                int ground = generated.heightAt(x, targetZ);
                if (generated.get(x, ground, targetZ) != World.AIR
                        && generated.get(x, ground + 1, targetZ) == World.AIR) {
                    targetX = x;
                    targetY = ground + 1;
                    break;
                }
            }
            check(targetX >= 0, "test should find a reachable empty cell above a solid block");
            int blockX = targetX, blockY = targetY, blockZ = targetZ;
            byte[] placeBlock = Protocol.payload(output -> {
                output.writeByte(1);
                output.writeInt(blockX); output.writeInt(blockY); output.writeInt(blockZ);
                output.writeByte(World.DIRT);
                output.writeByte(0); output.writeByte(1); output.writeByte(0);
            });
            Protocol.writeFrame(first.output, Protocol.Type.BLOCK_ACTION, placeBlock);
            Protocol.Frame changed = readUntil(second.input, Protocol.Type.BLOCK_CHANGED);
            try (DataInputStream input = Protocol.input(changed.payload())) {
                int x = input.readInt(), y = input.readInt(), z = input.readInt();
                input.readByte();
                byte type = input.readByte();
                int actor = input.readInt();
                Protocol.requireConsumed(input);
                check(x == blockX && y == blockY && z == blockZ && type == World.DIRT && actor == first.welcome.id,
                        "another client should receive the server-approved block placement");
            }

            DataOutputStream malformed = new DataOutputStream(first.socket.getOutputStream());
            malformed.writeByte(127);
            malformed.writeInt(0);
            malformed.flush();
            Protocol.Frame rejection = readUntil(first.input, Protocol.Type.REJECT);
            try (DataInputStream input = Protocol.input(rejection.payload())) {
                check(!input.readUTF().isBlank(), "malformed packets should receive a protocol rejection");
            }
            first.close();
            clients.remove(first);
            for (int retry = 0; retry < 50 && server.connectionCount() > 1; retry++) Thread.sleep(20);

            var gameConstructor = TytanCraft.class.getDeclaredConstructor(long.class, World.class);
            gameConstructor.setAccessible(true);
            TytanCraft gameClient = gameConstructor.newInstance(seed, new World(seed));
            fieldFor(gameClient, "serverAddress").set(gameClient, "127.0.0.1:" + server.port());
            Method connect = TytanCraft.class.getDeclaredMethod("beginNetworkJoin");
            connect.setAccessible(true);
            connect.invoke(gameClient);
            Method update = TytanCraft.class.getDeclaredMethod("update", float.class);
            update.setAccessible(true);
            long connectDeadline = System.nanoTime() + 8_000_000_000L;
            while (fieldFor(gameClient, "joiningServer").getBoolean(gameClient)
                    && System.nanoTime() < connectDeadline) {
                update.invoke(gameClient, 0.0f);
                Thread.sleep(20);
            }
            check(fieldFor(gameClient, "networkMode").getBoolean(gameClient)
                            && !fieldFor(gameClient, "inLobby").getBoolean(gameClient),
                    "lobby client should apply the server welcome on the game update thread");
            Method stop = TytanCraft.class.getDeclaredMethod("stopGame");
            stop.setAccessible(true);
            stop.invoke(gameClient);
            for (int retry = 0; retry < 50 && server.connectionCount() > 1; retry++) Thread.sleep(20);

            CountDownLatch connected = new CountDownLatch(1);
            AtomicReference<Protocol.Type> clientMessage = new AtomicReference<>();
            transportClient = new NetworkClient("127.0.0.1", server.port(), new NetworkClient.Listener() {
                @Override public void onFrame(Protocol.Frame frame) {
                    clientMessage.set(frame.type());
                    if (frame.type() == Protocol.Type.WELCOME) connected.countDown();
                }
                @Override public void onFailure(String message) { connected.countDown(); }
            });
            transportClient.start("Transport");
            check(connected.await(8, TimeUnit.SECONDS) && clientMessage.get() == Protocol.Type.WELCOME,
                    "production network client should complete the server handshake headlessly");
            for (int i = 0; i < 8; i++) clients.add(connectTestClient(server.port(), "Dodatkowy" + i));
            check(server.connectionCount() == Protocol.MAX_PLAYERS, "server should admit exactly ten connections");
            eleventh = new Socket();
            eleventh.connect(new InetSocketAddress("127.0.0.1", server.port()), 3000);
            eleventh.setSoTimeout(5000);
            Protocol.Frame full = Protocol.readFrame(new DataInputStream(eleventh.getInputStream()));
            check(full.type() == Protocol.Type.REJECT, "eleventh connection should be refused");
            try (DataInputStream input = Protocol.input(full.payload())) {
                check("Serwer jest pełny".equals(input.readUTF()), "full-server response should be localized");
            }
            transportClient.close();
            transportClient = null;
        } finally {
            if (eleventh != null) try { eleventh.close(); } catch (Exception ignored) {}
            if (transportClient != null) transportClient.close();
            for (TestClient client : clients) client.close();
            server.close();
        }
    }

    private static TestClient connectTestClient(int port, String nickname) throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 3000);
        socket.setSoTimeout(8000);
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        Protocol.writeFrame(output, Protocol.Type.HELLO, Protocol.hello(nickname));
        Protocol.Frame frame = Protocol.readFrame(input);
        if (frame.type() != Protocol.Type.WELCOME) {
            String reason;
            try (DataInputStream rejection = Protocol.input(frame.payload())) { reason = rejection.readUTF(); }
            socket.close();
            throw new AssertionError("server rejected test client: " + reason);
        }
        Welcome welcome = readWelcome(frame.payload());
        return new TestClient(socket, input, output, welcome);
    }

    private static Welcome readWelcome(byte[] payload) throws Exception {
        try (DataInputStream input = Protocol.input(payload)) {
            check(input.readInt() == Protocol.VERSION, "welcome should include protocol version");
            int id = input.readInt();
            String nickname = Protocol.readNickname(input);
            long seed = input.readLong();
            input.readBoolean();
            input.readInt();
            input.readFloat();
            int spawnX = input.readInt(), spawnZ = input.readInt();
            input.readFloat(); input.readFloat(); input.readFloat();
            int worldLength = input.readInt();
            check(worldLength > 0 && worldLength <= Protocol.MAX_WORLD_BYTES, "welcome world payload size should be bounded");
            byte[] worldBytes = new byte[worldLength];
            input.readFully(worldBytes);
            WorldStateCodec.Snapshot world = WorldStateCodec.decode(worldBytes);
            input.readFloat(); input.readFloat(); input.readByte();
            input.readFloat(); input.readFloat(); input.readFloat(); input.readFloat(); input.readFloat();
            int players = input.readInt();
            check(players >= 0 && players < Protocol.MAX_PLAYERS, "welcome player count should be bounded");
            for (int i = 0; i < players; i++) readPlayer(input);
            int animals = input.readInt();
            check(animals >= 0 && animals <= 128, "welcome animal count should be bounded");
            for (int i = 0; i < animals; i++) {
                input.readInt(); input.readByte(); input.readBoolean(); input.readInt();
                for (int f = 0; f < 6; f++) input.readFloat();
            }
            Protocol.requireConsumed(input);
            return new Welcome(id, nickname, seed, spawnX, spawnZ, world);
        }
    }

    private static void readPlayer(DataInputStream input) throws Exception {
        input.readInt(); input.readUTF();
        for (int i = 0; i < 5; i++) input.readFloat();
    }

    private static Protocol.Frame readUntil(DataInputStream input, Protocol.Type type) throws Exception {
        long deadline = System.nanoTime() + 8_000_000_000L;
        while (System.nanoTime() < deadline) {
            Protocol.Frame frame = Protocol.readFrame(input);
            if (frame.type() == type) return frame;
        }
        throw new AssertionError("timed out waiting for " + type);
    }

    private record Welcome(int id, String nickname, long seed, int spawnX, int spawnZ,
                           WorldStateCodec.Snapshot world) {}

    private record TestClient(Socket socket, DataInputStream input, DataOutputStream output, Welcome welcome)
            implements AutoCloseable {
        @Override public void close() throws IOException { socket.close(); }
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
        float playerX = field("playerX").getFloat(game), playerYValue = field("playerY").getFloat(game);
        float playerZ = field("playerZ").getFloat(game);
        Class<?> playerStateClass = Class.forName("dev.tytancraft.TytanCraft$RemotePlayerState");
        var playerStateConstructor = playerStateClass.getDeclaredConstructor(int.class, String.class,
                float.class, float.class, float.class, float.class, float.class);
        playerStateConstructor.setAccessible(true);
        Object remote = playerStateConstructor.newInstance(88, "Tulej", playerX, playerYValue,
                playerZ + 5.0f, 0.0f, 0.0f);
        invoke(game, "applyRemotePlayer", new Class<?>[]{playerStateClass}, remote);
        field("networkMode").setBoolean(game, true);
        field("networkPlayerId").setInt(game, 1);
        invoke(game, "render");
        BufferedImage playerFrame = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D playerGraphics = playerFrame.createGraphics();
        invoke(game, "drawHud", new Class<?>[]{Graphics2D.class}, playerGraphics);
        playerGraphics.dispose();
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

    private static Field fieldFor(Object target, String name) throws Exception {
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
