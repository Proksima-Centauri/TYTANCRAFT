package dev.tytancraft;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Headless authoritative server. This class intentionally has no AWT/Swing dependencies. */
public final class GameServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 25570;
    private static final int MAX_ANIMALS = 128;
    private static final int MAX_ANIMALS_PER_KIND = 10;
    private static final int OUTGOING_QUEUE_SIZE = 64;
    private static final int TICK_RATE = 20;
    private static final float TICK_SECONDS = 1.0f / TICK_RATE;
    private static final long SAVE_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(5);

    private final long requestedSeed;
    private final int requestedPort;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicInteger nextConnectionId = new AtomicInteger(1);
    private final LinkedBlockingQueue<Inbound> inbound = new LinkedBlockingQueue<>(8192);
    private final ConcurrentHashMap<Integer, ClientSession> connections = new ConcurrentHashMap<>();
    private final LinkedHashMap<Integer, Player> players = new LinkedHashMap<>();
    private final ArrayList<ServerAnimal> animals = new ArrayList<>();
    private final int[] populationScratch = new int[Animal.Kind.values().length];
    private final Random animalRandom;
    private final Random weatherRandom;
    private volatile int boundPort;
    private volatile ServerSocket serverSocket;
    private Thread acceptThread;
    private Thread simulationThread;
    private World world;
    private long seed;
    private int spawnX, spawnZ;
    private int nextPlayerId = 1, nextAnimalId = 1;
    private boolean creativeMode;
    private int aggressionLevel = 25;
    private float daySpeed = 1.0f, dayTime = 0.5f;
    private int weather;
    private float weatherTimer = 35, cloudCoverage = 0.12f, targetCloudCoverage = 0.12f;
    private float rainIntensity, targetRainIntensity, cloudOffsetX, cloudOffsetY, rainPhase;
    private long ticks, lastSaveNanos;

    public GameServer(long seed, int port) {
        if (port < 0 || port > 65535) throw new IllegalArgumentException("Nieprawidłowy port serwera.");
        requestedSeed = seed;
        requestedPort = port;
        animalRandom = new Random(seed ^ 0x6a09e667f3bcc909L);
        weatherRandom = new Random(seed ^ 0xbb67ae8584caa73bL);
    }

    public static void main(String[] args) throws InterruptedException {
        long seed = args.length > 0 ? parseSeed(args[0]) : new Random().nextLong();
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        GameServer server = new GameServer(seed, port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "tytancraft-server-shutdown"));
        try {
            server.start();
            server.await();
        } catch (IOException failure) {
            System.err.println("Nie udało się uruchomić serwera: " + failure.getMessage());
            server.close();
        }
    }

    private static long parseSeed(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException signedOverflow) {
            return Long.parseUnsignedLong(value);
        }
    }

    public synchronized void start() throws IOException {
        if (!running.compareAndSet(false, true)) return;
        initializeWorld();
        world.trackWaterChanges(true);
        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(requestedPort), Protocol.MAX_PLAYERS + 8);
        serverSocket = socket;
        boundPort = socket.getLocalPort();
        lastSaveNanos = System.nanoTime();
        acceptThread = new Thread(this::acceptLoop, "tytancraft-server-accept");
        simulationThread = new Thread(this::simulationLoop, "tytancraft-server-simulation");
        acceptThread.setDaemon(true);
        simulationThread.setDaemon(true);
        acceptThread.start();
        simulationThread.start();
        System.out.println("TytanCraft: serwer nasłuchuje na porcie " + boundPort
                + " (seed " + Long.toUnsignedString(seed) + ", limit " + Protocol.MAX_PLAYERS + " graczy).");
    }

    public int port() { return boundPort; }

    public long seed() { return seed; }

    int connectionCount() { return connections.size(); }

    public void await() throws InterruptedException {
        Thread thread = simulationThread;
        if (thread != null) thread.join();
    }

    private void initializeWorld() throws IOException {
        seed = requestedSeed;
        WorldStorage.Data saved = null;
        for (WorldStorage.SaveInfo save : WorldStorage.list()) {
            if (save.seed == seed) {
                try {
                    saved = WorldStorage.load(save.path);
                } catch (IOException invalidSave) {
                    System.err.println("Pominięto uszkodzony zapis świata: " + invalidSave.getMessage());
                }
                break;
            }
        }
        if (saved == null) {
            world = new World(seed);
            int[] spawn = world.findDrySpawn();
            spawnX = spawn[0];
            spawnZ = spawn[1];
            createInitialAnimals();
            return;
        }

        seed = saved.seed;
        world = new World(saved.seed, saved.blocks, saved.waterFlow, saved.waterQueue);
        spawnX = saved.spawnX;
        spawnZ = saved.spawnZ;
        creativeMode = saved.creativeMode;
        aggressionLevel = saved.aggressionLevel;
        daySpeed = saved.daySpeed;
        dayTime = saved.dayTime;
        weather = saved.weather;
        weatherTimer = saved.weatherTimer;
        cloudCoverage = saved.cloudCoverage;
        targetCloudCoverage = saved.targetCloudCoverage;
        rainIntensity = saved.rainIntensity;
        targetRainIntensity = saved.targetRainIntensity;
        cloudOffsetX = saved.cloudOffsetX;
        cloudOffsetY = saved.cloudOffsetY;
        rainPhase = saved.rainPhase;
        for (WorldStorage.AnimalState state : saved.animals) {
            Animal animal = new Animal(state.kind, state.x, state.z, state.y, animalRandom);
            restoreAnimal(animal, state);
            animals.add(new ServerAnimal(nextAnimalId++, animal));
        }
    }

    private void createInitialAnimals() {
        Animal.Kind[] landKinds = {
                Animal.Kind.SHEEP, Animal.Kind.FOX, Animal.Kind.CHICKEN, Animal.Kind.BOAR,
                Animal.Kind.PIG, Animal.Kind.DEER, Animal.Kind.RABBIT
        };
        for (int i = 0; i < 33; i++) {
            Animal.Kind kind = i < 6 ? Animal.Kind.SHEEP : i < 10 ? Animal.Kind.FOX
                    : i < 16 ? Animal.Kind.CHICKEN : i < 21 ? Animal.Kind.BOAR
                    : landKinds[4 + (i - 21) % 3];
            for (int attempt = 0; attempt < 80; attempt++) {
                float angle = animalRandom.nextFloat() * (float) (Math.PI * 2);
                float distance = 8 + animalRandom.nextFloat() * 18;
                float x = spawnX + 0.5f + (float) Math.sin(angle) * distance;
                float z = spawnZ + 0.5f + (float) Math.cos(angle) * distance;
                if (!World.inBounds((int) x, 1, (int) z)) continue;
                int ground = world.heightAt((int) x, (int) z);
                if (ground <= World.WATER_LEVEL + 1) continue;
                if (crowded(x, z, false, 3.0f)) continue;
                animals.add(new ServerAnimal(nextAnimalId++, new Animal(kind, x, z, ground, animalRandom)));
                break;
            }
        }

        ArrayList<int[]> deepWater = new ArrayList<>();
        for (int z = Math.max(2, spawnZ - 48); z <= Math.min(World.DEPTH - 3, spawnZ + 48); z += 2) {
            for (int x = Math.max(2, spawnX - 48); x <= Math.min(World.WIDTH - 3, spawnX + 48); x += 2) {
                if (world.heightAt(x, z) <= World.WATER_LEVEL - 3) deepWater.add(new int[]{x, z});
            }
        }
        if (deepWater.isEmpty()) return;
        Animal.Kind[] fishKinds = {Animal.Kind.COD, Animal.Kind.SALMON, Animal.Kind.TROPICAL_FISH,
                Animal.Kind.PIKE, Animal.Kind.CARP};
        for (int i = 0; i < 25; i++) {
            int[] position = deepWater.get(animalRandom.nextInt(deepWater.size()));
            float x = position[0] + 0.5f + (animalRandom.nextFloat() - 0.5f) * 0.5f;
            float z = position[1] + 0.5f + (animalRandom.nextFloat() - 0.5f) * 0.5f;
            if (crowded(x, z, true, 1.7f)) continue;
            Animal.Kind kind = fishKinds[i % fishKinds.length];
            animals.add(new ServerAnimal(nextAnimalId++,
                    new Animal(kind, x, z, World.WATER_LEVEL - 0.9f, animalRandom)));
        }
    }

    private static void restoreAnimal(Animal animal, WorldStorage.AnimalState state) {
        animal.alive = state.alive;
        animal.health = state.health;
        animal.x = state.x; animal.y = state.y; animal.z = state.z;
        animal.yaw = state.yaw; animal.gait = state.gait;
        animal.updatePartAnimation();
        animal.verticalVelocity = state.verticalVelocity;
        animal.growth = state.growth;
        animal.setBreedingCooldown(state.breedingCooldown);
    }

    private boolean crowded(float x, float z, boolean fish, float radius) {
        float radiusSquared = radius * radius;
        for (ServerAnimal other : animals) {
            if (!other.animal.alive || other.animal.kind.isFish() != fish) continue;
            float dx = other.animal.x - x, dz = other.animal.z - z;
            if (dx * dx + dz * dz < radiusSquared) return true;
        }
        return false;
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(5000);
                if (connections.size() >= Protocol.MAX_PLAYERS) {
                    rejectFull(socket);
                    continue;
                }
                ClientSession session = new ClientSession(nextConnectionId.getAndIncrement(), socket);
                connections.put(session.connectionId, session);
                session.start();
            } catch (SocketException closedSocket) {
                if (running.get()) System.err.println("Akceptowanie połączeń zakończone: " + closedSocket.getMessage());
                return;
            } catch (IOException failure) {
                if (running.get()) System.err.println("Błąd gniazda serwera: " + failure.getMessage());
            }
        }
    }

    private void rejectFull(Socket socket) {
        try (Socket rejected = socket;
             DataOutputStream output = new DataOutputStream(new BufferedOutputStream(rejected.getOutputStream()))) {
            Protocol.writeFrame(output, Protocol.Type.REJECT,
                    Protocol.payload(payload -> payload.writeUTF("Serwer jest pełny")));
        } catch (IOException ignored) {
            // The refused connection is closed even if its peer has already gone away.
        }
    }

    private void simulationLoop() {
        long nextTick = System.nanoTime();
        while (running.get()) {
            processInbound();
            tick(TICK_SECONDS);
            ticks++;
            if (ticks % 1 == 0) broadcastPlayerChanges();
            if (ticks % 2 == 0) broadcastAnimals();
            if (ticks % TICK_RATE == 0) broadcastEnvironment();
            if (System.nanoTime() - lastSaveNanos >= SAVE_INTERVAL_NANOS) saveWorldQuietly();

            nextTick += TimeUnit.MILLISECONDS.toNanos(1000 / TICK_RATE);
            long sleepNanos = nextTick - System.nanoTime();
            if (sleepNanos > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(sleepNanos);
                } catch (InterruptedException interrupted) {
                    if (!running.get()) return;
                    Thread.currentThread().interrupt();
                    return;
                }
            } else if (sleepNanos < -TimeUnit.MILLISECONDS.toNanos(250)) {
                nextTick = System.nanoTime();
            }
        }
    }

    private void processInbound() {
        Inbound event;
        while ((event = inbound.poll()) != null) {
            try {
                if (event instanceof Hello hello) handleHello(hello);
                else if (event instanceof PacketInput packet) handlePacket(packet);
                else if (event instanceof Disconnected disconnected) handleDisconnect(disconnected.session);
            } catch (IOException | RuntimeException invalid) {
                ClientSession session = event.session();
                if (session != null) session.reject("Nieprawidłowe dane: " + safeMessage(invalid));
            }
        }
    }

    private void handleHello(Hello hello) throws IOException {
        ClientSession session = hello.session;
        if (!running.get() || session.closed.get() || players.size() >= Protocol.MAX_PLAYERS) {
            session.reject("Serwer jest pełny");
            return;
        }
        String nickname = uniqueNickname(hello.nickname);
        int id = nextPlayerId++;
        int ordinal = players.size();
        int xOffset = ordinal % 4;
        int zOffset = ordinal / 4;
        float x = spawnX + 0.5f + xOffset * 1.2f;
        float z = spawnZ + 0.5f + zOffset * 1.2f;
        x = Math.min(World.WIDTH - 2.0f, x);
        z = Math.min(World.DEPTH - 2.0f, z);
        Player player = new Player(id, session, nickname, x, world.heightAt((int) x, (int) z) + 1.0f, z);
        session.playerId = id;
        session.nickname = nickname;
        players.put(id, player);
        session.send(Protocol.Type.WELCOME, welcomePayload(player));
        byte[] joined = playerPayload(player);
        for (Player other : players.values()) if (other.id != player.id) other.session.send(Protocol.Type.PLAYER_JOINED, joined);
        System.out.println("Gracz dołączył: " + nickname + " (" + players.size() + "/" + Protocol.MAX_PLAYERS + ")");
    }

    private String uniqueNickname(String requested) {
        String base = requested;
        int suffix = 2;
        while (nicknameExists(base)) {
            String tail = "_" + suffix++;
            base = requested.substring(0, Math.min(requested.length(), 16 - tail.length())) + tail;
        }
        return base;
    }

    private boolean nicknameExists(String candidate) {
        for (Player player : players.values()) if (player.nickname.equalsIgnoreCase(candidate)) return true;
        return false;
    }

    private byte[] welcomePayload(Player joining) throws IOException {
        byte[] compressedWorld = WorldStateCodec.encode(world);
        return Protocol.payload(output -> {
            output.writeInt(Protocol.VERSION);
            output.writeInt(joining.id);
            output.writeUTF(joining.nickname);
            output.writeLong(seed);
            output.writeBoolean(creativeMode);
            output.writeInt(aggressionLevel);
            output.writeFloat(daySpeed);
            output.writeInt(spawnX);
            output.writeInt(spawnZ);
            output.writeFloat(joining.x);
            output.writeFloat(joining.y);
            output.writeFloat(joining.z);
            output.writeInt(compressedWorld.length);
            output.write(compressedWorld);
            writeEnvironment(output);
            int otherPlayers = players.size() - 1;
            output.writeInt(otherPlayers);
            for (Player player : players.values()) if (player.id != joining.id) writePlayer(output, player);
            writeAnimals(output);
        });
    }

    private byte[] playerPayload(Player player) throws IOException {
        return Protocol.payload(output -> writePlayer(output, player));
    }

    private static void writePlayer(DataOutputStream output, Player player) throws IOException {
        output.writeInt(player.id);
        output.writeUTF(player.nickname);
        output.writeFloat(player.x);
        output.writeFloat(player.y);
        output.writeFloat(player.z);
        output.writeFloat(player.yaw);
        output.writeFloat(player.pitch);
    }

    private void writeEnvironment(DataOutputStream output) throws IOException {
        output.writeFloat(dayTime);
        output.writeFloat(daySpeed);
        output.writeByte(weather);
        output.writeFloat(cloudCoverage);
        output.writeFloat(rainIntensity);
        output.writeFloat(rainPhase);
        output.writeFloat(cloudOffsetX);
        output.writeFloat(cloudOffsetY);
    }

    private void writeAnimals(DataOutputStream output) throws IOException {
        output.writeInt(animals.size());
        for (ServerAnimal entry : animals) {
            Animal animal = entry.animal;
            output.writeInt(entry.id);
            output.writeByte(animal.kind.ordinal());
            output.writeBoolean(animal.alive);
            output.writeInt(animal.health);
            output.writeFloat(animal.x);
            output.writeFloat(animal.y);
            output.writeFloat(animal.z);
            output.writeFloat(animal.yaw);
            output.writeFloat(animal.gait);
            output.writeFloat(animal.growth);
        }
    }

    private void handlePacket(PacketInput packet) throws IOException {
        ClientSession session = packet.session;
        Player player = players.get(session.playerId);
        if (player == null || session.closed.get()) return;
        try (DataInputStream input = Protocol.input(packet.frame.payload())) {
            switch (packet.frame.type()) {
                case PLAYER_POSITION -> handlePlayerPosition(player, input);
                case BLOCK_ACTION -> handleBlockAction(player, input);
                case ANIMAL_ATTACK -> handleAnimalAttack(player, input);
                case FISH_SPAWN -> handleFishSpawn(player, input);
                default -> throw new IOException("Komunikat niedozwolony od klienta.");
            }
            Protocol.requireConsumed(input);
        }
    }

    private void handlePlayerPosition(Player player, DataInputStream input) throws IOException {
        float x = input.readFloat(), y = input.readFloat(), z = input.readFloat();
        float yaw = input.readFloat(), pitch = input.readFloat();
        if (!finite(x, y, z, yaw, pitch) || x < 1 || x > World.WIDTH - 1 || z < 1 || z > World.DEPTH - 1
                || y < 0 || y > World.HEIGHT || Math.abs(yaw) > 10_000 || Math.abs(pitch) > 10_000) {
            throw new IOException("Pozycja gracza poza światem.");
        }
        player.x = x;
        player.y = y;
        player.z = z;
        player.yaw = yaw;
        player.pitch = Math.max(-1.48f, Math.min(1.48f, pitch));
    }

    private void handleBlockAction(Player player, DataInputStream input) throws IOException {
        int action = input.readUnsignedByte();
        int x = input.readInt(), y = input.readInt(), z = input.readInt();
        byte requestedType = input.readByte();
        int normalX = input.readByte(), normalY = input.readByte(), normalZ = input.readByte();
        if (action > 1 || !World.inBounds(x, y, z) || y == 0
                || !inReach(player, x + 0.5f, y + 0.5f, z + 0.5f)) return;
        if (action == 0) {
            byte oldType = world.get(x, y, z);
            if (oldType == World.AIR) return;
            world.set(x, y, z, World.AIR);
            world.flowWaterInto(x, y, z);
            broadcastBlockChange(x, y, z, oldType, World.AIR, player.id);
            if (oldType != World.WATER) {
                byte drop = World.isLadderBlock(oldType) ? World.LADDER : oldType;
                player.session.send(Protocol.Type.ITEM_DROP, Protocol.payload(output -> {
                    output.writeByte(drop);
                    output.writeInt(1);
                }));
            }
            return;
        }

        int normalMagnitude = Math.abs(normalX) + Math.abs(normalY) + Math.abs(normalZ);
        if (normalMagnitude != 1 || requestedType <= World.AIR || requestedType > World.LADDER_X_NEG
                || World.isFishSpawnEgg(requestedType)) return;
        if (world.get(x, y, z) != World.AIR && world.get(x, y, z) != World.WATER) return;
        int supportX = x - normalX, supportY = y - normalY, supportZ = z - normalZ;
        if (!World.inBounds(supportX, supportY, supportZ) || !World.isSolid(world.get(supportX, supportY, supportZ))) return;

        byte placedType = requestedType;
        if (requestedType == World.LADDER) {
            if (normalY != 0) return;
            placedType = World.ladderAgainstWall(normalX, normalZ);
        }
        if (overlapsPlayer(x, y, z)) return;
        byte replaced = world.get(x, y, z);
        world.set(x, y, z, placedType);
        broadcastBlockChange(x, y, z, replaced, placedType, player.id);
    }

    private void handleAnimalAttack(Player player, DataInputStream input) throws IOException {
        int animalId = input.readInt();
        ServerAnimal target = findAnimal(animalId);
        if (target == null || !target.animal.alive || player.attackCooldown > 0) return;
        float dx = target.animal.x - player.x, dy = target.animal.y - player.y, dz = target.animal.z - player.z;
        if (dx * dx + dy * dy + dz * dz > 5.5f * 5.5f) return;
        player.attackCooldown = 0.35f;
        if (target.animal.takeHit() && !creativeMode) {
            player.session.send(Protocol.Type.ITEM_DROP, Protocol.payload(output -> {
                output.writeByte(World.AIR);
                output.writeInt(target.animal.meatDrop);
            }));
            if (target.animal.kind == Animal.Kind.SHEEP) sendDrop(player, World.WOOL, 1);
            byte egg = target.animal.spawnEggDrop();
            if (egg != World.AIR) sendDrop(player, egg, 1);
        }
    }

    private void handleFishSpawn(Player player, DataInputStream input) throws IOException {
        int x = input.readInt(), y = input.readInt(), z = input.readInt();
        byte egg = input.readByte();
        if (!World.isFishSpawnEgg(egg) || !World.inBounds(x, y, z) || world.get(x, y, z) != World.WATER
                || !inReach(player, x + 0.5f, y + 0.5f, z + 0.5f)) return;
        Animal.Kind kind = switch (egg) {
            case World.COD_SPAWN_EGG -> Animal.Kind.COD;
            case World.SALMON_SPAWN_EGG -> Animal.Kind.SALMON;
            case World.TROPICAL_FISH_SPAWN_EGG -> Animal.Kind.TROPICAL_FISH;
            case World.PIKE_SPAWN_EGG -> Animal.Kind.PIKE;
            default -> Animal.Kind.CARP;
        };
        if (population(kind) >= MAX_ANIMALS_PER_KIND || animals.size() >= MAX_ANIMALS) return;
        int ground = world.heightAt(x, z);
        float swimY = Math.max(ground + 1.1f, World.WATER_LEVEL - 0.9f);
        addAnimal(new Animal(kind, x + 0.5f, z + 0.5f, swimY, animalRandom));
        if (!creativeMode) sendDrop(player, egg, -1);
    }

    private void sendDrop(Player player, byte type, int amount) throws IOException {
        player.session.send(Protocol.Type.ITEM_DROP, Protocol.payload(output -> {
            output.writeByte(type);
            output.writeInt(amount);
        }));
    }

    private boolean inReach(Player player, float x, float y, float z) {
        float dx = x - player.x, dy = y - (player.y + 1.4f), dz = z - player.z;
        return dx * dx + dy * dy + dz * dz <= 5.5f * 5.5f;
    }

    private boolean overlapsPlayer(int x, int y, int z) {
        for (Player player : players.values()) {
            boolean horizontal = x + 1 > player.x - 0.23f && x < player.x + 0.23f
                    && z + 1 > player.z - 0.23f && z < player.z + 0.23f;
            if (horizontal && y + 1 > player.y && y < player.y + 1.8f) return true;
        }
        return false;
    }

    private void handleDisconnect(ClientSession session) {
        connections.remove(session.connectionId, session);
        if (session.playerId < 0) return;
        Player player = players.remove(session.playerId);
        if (player == null) return;
        try {
            byte[] payload = Protocol.payload(output -> output.writeInt(player.id));
            broadcast(Protocol.Type.PLAYER_LEFT, payload, -1);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
        System.out.println("Gracz opuścił serwer: " + player.nickname);
    }

    private void tick(float dt) {
        for (Player player : players.values()) player.attackCooldown = Math.max(0, player.attackCooldown - dt);
        if (!creativeMode) {
            for (ServerAnimal entry : animals) {
                Animal animal = entry.animal;
                if (!animal.alive) continue;
                Player closest = closestPlayer(animal.x, animal.z);
                if (closest == null) animal.update(world, dt, animalRandom, 0, spawnX + 0.5f, spawnZ + 0.5f, false);
                else animal.update(world, dt, animalRandom, aggressionLevel, closest.x, closest.z, true);
            }
            breedAnimals();
        } else {
            for (ServerAnimal entry : animals) {
                if (entry.animal.alive) entry.animal.update(world, dt, animalRandom, 0, 0, 0, false);
            }
        }
        world.tickWaterFlow();
        List<World.BlockChange> water = world.drainWaterChanges();
        if (!water.isEmpty()) broadcastWaterChanges(water);
        advanceEnvironment(dt);
        if (animals.size() > MAX_ANIMALS) throw new IllegalStateException("Limit zwierząt został przekroczony.");
    }

    private Player closestPlayer(float x, float z) {
        Player closest = null;
        float closestDistance = Float.POSITIVE_INFINITY;
        for (Player player : players.values()) {
            float dx = player.x - x, dz = player.z - z;
            float distance = dx * dx + dz * dz;
            if (distance < closestDistance) { closestDistance = distance; closest = player; }
        }
        return closest;
    }

    private void breedAnimals() {
        Arrays.fill(populationScratch, 0);
        for (ServerAnimal entry : animals) if (entry.animal.alive) populationScratch[entry.animal.kind.ordinal()]++;
        for (int i = 0; i < animals.size(); i++) {
            Animal first = animals.get(i).animal;
            if (!first.canBreed() || populationScratch[first.kind.ordinal()] >= MAX_ANIMALS_PER_KIND) continue;
            for (int j = i + 1; j < animals.size(); j++) {
                Animal second = animals.get(j).animal;
                if (!second.canBreed() || second.kind != first.kind) continue;
                float dx = first.x - second.x, dz = first.z - second.z;
                float radius = first.kind.isFish() ? 2.8f : 2.2f;
                if (dx * dx + dz * dz > radius * radius || Math.abs(first.y - second.y) > 1.5f) continue;
                float babyX = (first.x + second.x) * 0.5f, babyZ = (first.z + second.z) * 0.5f;
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
                first.beginBreedingCooldown(45);
                second.beginBreedingCooldown(45);
                addAnimal(new Animal(first.kind, babyX, babyZ, babyY, animalRandom, true));
                return;
            }
        }
    }

    private int population(Animal.Kind kind) {
        int count = 0;
        for (ServerAnimal entry : animals) if (entry.animal.alive && entry.animal.kind == kind) count++;
        return count;
    }

    private void addAnimal(Animal animal) {
        animal.networkId = nextAnimalId++;
        for (int i = 0; i < animals.size(); i++) {
            if (!animals.get(i).animal.alive) {
                animals.set(i, new ServerAnimal(animal.networkId, animal));
                return;
            }
        }
        if (animals.size() < MAX_ANIMALS) animals.add(new ServerAnimal(animal.networkId, animal));
    }

    private ServerAnimal findAnimal(int id) {
        for (ServerAnimal animal : animals) if (animal.id == id) return animal;
        return null;
    }

    private void advanceEnvironment(float dt) {
        if (daySpeed > 0) dayTime = (dayTime + dt * daySpeed / 240.0f) % 1.0f;
        cloudOffsetX += dt * 0.0018f;
        cloudOffsetY += dt * 0.00055f;
        rainPhase += dt * (28 + daySpeed * 8);
        weatherTimer -= dt;
        if (weatherTimer <= 0) chooseWeather();
        float blend = Math.min(1, dt * 0.045f);
        cloudCoverage += (targetCloudCoverage - cloudCoverage) * blend;
        rainIntensity += (targetRainIntensity - rainIntensity) * blend;
    }

    private void chooseWeather() {
        float choice = weatherRandom.nextFloat();
        if (choice < 0.55f) {
            weather = 0;
            targetCloudCoverage = 0.10f + weatherRandom.nextFloat() * 0.14f;
            targetRainIntensity = 0;
        } else if (choice < 0.82f) {
            weather = 1;
            targetCloudCoverage = 0.52f + weatherRandom.nextFloat() * 0.18f;
            targetRainIntensity = 0;
        } else {
            weather = 2;
            targetCloudCoverage = 0.82f + weatherRandom.nextFloat() * 0.15f;
            targetRainIntensity = 0.72f + weatherRandom.nextFloat() * 0.28f;
        }
        weatherTimer = 35 + weatherRandom.nextFloat() * 75;
    }

    private void broadcastPlayerChanges() {
        for (Player player : players.values()) {
            if (Math.abs(player.x - player.lastBroadcastX) < 0.015f
                    && Math.abs(player.y - player.lastBroadcastY) < 0.015f
                    && Math.abs(player.z - player.lastBroadcastZ) < 0.015f
                    && Math.abs(player.yaw - player.lastBroadcastYaw) < 0.005f
                    && Math.abs(player.pitch - player.lastBroadcastPitch) < 0.005f) continue;
            try {
                byte[] payload = Protocol.payload(output -> writePlayer(output, player));
                broadcast(Protocol.Type.PLAYER_MOVED, payload, player.id);
                player.lastBroadcastX = player.x; player.lastBroadcastY = player.y; player.lastBroadcastZ = player.z;
                player.lastBroadcastYaw = player.yaw; player.lastBroadcastPitch = player.pitch;
            } catch (IOException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    private void broadcastAnimals() {
        if (players.isEmpty()) return;
        try {
            byte[] payload = Protocol.payload(this::writeAnimals);
            broadcast(Protocol.Type.ANIMAL_SNAPSHOT, payload, -1);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void broadcastEnvironment() {
        if (players.isEmpty()) return;
        try {
            byte[] payload = Protocol.payload(this::writeEnvironment);
            broadcast(Protocol.Type.ENVIRONMENT, payload, -1);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void broadcastBlockChange(int x, int y, int z, byte oldType, byte newType, int actorId) {
        try {
            byte[] payload = Protocol.payload(output -> {
                output.writeInt(x); output.writeInt(y); output.writeInt(z);
                output.writeByte(oldType); output.writeByte(newType); output.writeInt(actorId);
            });
            broadcast(Protocol.Type.BLOCK_CHANGED, payload, -1);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void broadcastWaterChanges(List<World.BlockChange> changes) {
        try {
            byte[] payload = Protocol.payload(output -> {
                output.writeInt(changes.size());
                for (World.BlockChange change : changes) {
                    output.writeInt(change.x()); output.writeInt(change.y()); output.writeInt(change.z());
                    output.writeByte(change.type());
                    output.writeShort(change.waterFlow());
                }
            });
            broadcast(Protocol.Type.WATER_CHANGED, payload, -1);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void broadcast(Protocol.Type type, byte[] payload, int exceptId) {
        for (Player player : players.values()) if (player.id != exceptId) player.session.send(type, payload);
    }

    private void saveWorldQuietly() {
        if (world == null) return;
        try {
            WorldStorage.Data data = new WorldStorage.Data();
            data.seed = seed;
            data.spawnX = spawnX; data.spawnZ = spawnZ;
            data.creativeMode = creativeMode; data.aggressionLevel = aggressionLevel;
            data.renderDistance = 4; data.playerHealth = 100; data.meatCount = 0;
            data.selectedBlock = 0; data.weather = weather; data.daySpeed = daySpeed;
            data.playerX = spawnX + 0.5f; data.playerY = world.heightAt(spawnX, spawnZ) + 1.0f;
            data.playerZ = spawnZ + 0.5f; data.yaw = 0; data.pitch = 0;
            data.hunger = 100; data.oxygen = 100; data.dayTime = dayTime;
            data.weatherTimer = weatherTimer; data.cloudCoverage = cloudCoverage;
            data.targetCloudCoverage = targetCloudCoverage; data.rainIntensity = rainIntensity;
            data.targetRainIntensity = targetRainIntensity; data.cloudOffsetX = cloudOffsetX;
            data.cloudOffsetY = cloudOffsetY; data.rainPhase = rainPhase;
            data.nickname = "Serwer"; data.inventory = new int[48]; data.hotbar = new byte[10];
            data.blocks = world.copyBlocks(); data.waterFlow = world.copyWaterFlow();
            data.waterQueue = world.copyPendingWaterFlow(); data.animals = new ArrayList<>(animals.size());
            for (ServerAnimal entry : animals) {
                Animal animal = entry.animal;
                WorldStorage.AnimalState state = new WorldStorage.AnimalState();
                state.kind = animal.kind; state.alive = animal.alive; state.health = animal.health;
                state.x = animal.x; state.y = animal.y; state.z = animal.z; state.yaw = animal.yaw;
                state.gait = animal.gait; state.verticalVelocity = animal.verticalVelocity;
                state.growth = animal.growth; state.breedingCooldown = animal.breedingCooldown();
                data.animals.add(state);
            }
            WorldStorage.save(data);
            lastSaveNanos = System.nanoTime();
        } catch (IOException failure) {
            System.err.println("Nie udało się zapisać świata serwera: " + failure.getMessage());
        }
    }

    private void rejectSocket(Socket socket, String reason) {
        try (Socket rejected = socket;
             DataOutputStream output = new DataOutputStream(new BufferedOutputStream(rejected.getOutputStream()))) {
            Protocol.writeFrame(output, Protocol.Type.REJECT,
                    Protocol.payload(payload -> payload.writeUTF(reason)));
        } catch (IOException ignored) {
            // The socket is closed even if the client cannot receive the reason.
        }
    }

    private boolean queue(Inbound event) {
        if (inbound.offer(event)) return true;
        if (event.session() != null) event.session().disconnect();
        return false;
    }

    private static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? "błąd protokołu" : message;
    }

    @Override public void close() {
        if (!stopped.compareAndSet(false, true)) return;
        running.set(false);
        ServerSocket listener = serverSocket;
        if (listener != null) try { listener.close(); } catch (IOException ignored) {}
        for (ClientSession session : connections.values()) session.shutdown();
        join(acceptThread);
        join(simulationThread);
        saveWorldQuietly();
    }

    private static void join(Thread thread) {
        if (thread == null || thread == Thread.currentThread()) return;
        try { thread.join(3000); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }

    private sealed interface Inbound permits Hello, PacketInput, Disconnected {
        ClientSession session();
    }
    private record Hello(ClientSession session, String nickname) implements Inbound {}
    private record PacketInput(ClientSession session, Protocol.Frame frame) implements Inbound {}
    private record Disconnected(ClientSession session) implements Inbound {}
    private record ServerAnimal(int id, Animal animal) {}

    private static final class Player {
        final int id;
        final ClientSession session;
        final String nickname;
        float x, y, z, yaw, pitch;
        float lastBroadcastX, lastBroadcastY, lastBroadcastZ, lastBroadcastYaw, lastBroadcastPitch;
        float attackCooldown;

        Player(int id, ClientSession session, String nickname, float x, float y, float z) {
            this.id = id; this.session = session; this.nickname = nickname;
            this.x = x; this.y = y; this.z = z;
            lastBroadcastX = x; lastBroadcastY = y; lastBroadcastZ = z;
        }
    }

    private final class ClientSession {
        final int connectionId;
        final Socket socket;
        final ArrayBlockingQueue<Protocol.Frame> outgoing = new ArrayBlockingQueue<>(OUTGOING_QUEUE_SIZE);
        final AtomicBoolean closed = new AtomicBoolean();
        final Object outputLock = new Object();
        volatile int playerId = -1;
        volatile String nickname = "";
        volatile boolean closeAfterFlush;
        Thread reader, writer;

        ClientSession(int connectionId, Socket socket) {
            this.connectionId = connectionId; this.socket = socket;
        }

        void start() {
            writer = new Thread(this::writeLoop, "tytancraft-client-writer-" + connectionId);
            reader = new Thread(this::readLoop, "tytancraft-client-reader-" + connectionId);
            writer.setDaemon(true); reader.setDaemon(true);
            writer.start(); reader.start();
        }

        boolean send(Protocol.Type type, byte[] payload) {
            if (closed.get()) return false;
            if (outgoing.offer(new Protocol.Frame(type, payload))) return true;
            disconnect();
            return false;
        }

        void reject(String reason) {
            if (closed.get()) return;
            try {
                byte[] payload = Protocol.payload(output -> output.writeUTF(reason));
                synchronized (outputLock) {
                    Protocol.writeFrame(new DataOutputStream(socket.getOutputStream()), Protocol.Type.REJECT, payload);
                }
                closeAfterFlush = true;
                disconnect();
            } catch (IOException failure) {
                disconnect();
            }
        }

        void readLoop() {
            try {
                DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                Protocol.Frame first = Protocol.readFrame(input);
                if (first.type() != Protocol.Type.HELLO) throw new IOException("Najpierw wymagany jest handshake.");
                String requested;
                try (DataInputStream hello = Protocol.input(first.payload())) {
                    int version = hello.readInt();
                    requested = Protocol.readNickname(hello);
                    Protocol.requireConsumed(hello);
                    if (version != Protocol.VERSION) throw new IOException("Niezgodna wersja protokołu.");
                }
                socket.setSoTimeout(0);
                if (!queue(new Hello(this, requested))) return;
                while (!closed.get()) {
                    Protocol.Frame frame = Protocol.readFrame(input);
                    if (!clientMessage(frame.type())) throw new IOException("Komunikat niedozwolony od klienta.");
                    if (!queue(new PacketInput(this, frame))) return;
                }
            } catch (IOException | RuntimeException failure) {
                if (!closed.get() && !closeAfterFlush) {
                    reject(safeMessage(failure));
                }
            } finally {
                if (!closeAfterFlush) disconnect();
            }
        }

        void writeLoop() {
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
                while (!closed.get()) {
                    Protocol.Frame frame = outgoing.poll(250, TimeUnit.MILLISECONDS);
                    if (frame != null) {
                        synchronized (outputLock) {
                            Protocol.writeFrame(output, frame.type(), frame.payload());
                        }
                    }
                    else if (closeAfterFlush) break;
                }
            } catch (IOException failure) {
                // Closing a socket wakes the writer; the disconnect event is handled on the simulation thread.
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                disconnect();
            }
        }

        void disconnect() {
            if (!closed.compareAndSet(false, true)) return;
            try { socket.close(); } catch (IOException ignored) {}
            if (reader != null && reader != Thread.currentThread()) reader.interrupt();
            if (writer != null && writer != Thread.currentThread()) writer.interrupt();
            if (running.get()) queue(new Disconnected(this));
        }

        void shutdown() {
            if (!closed.compareAndSet(false, true)) return;
            try { socket.close(); } catch (IOException ignored) {}
            if (reader != null) reader.interrupt();
            if (writer != null) writer.interrupt();
        }
    }

    private static boolean clientMessage(Protocol.Type type) {
        return type == Protocol.Type.PLAYER_POSITION || type == Protocol.Type.BLOCK_ACTION
                || type == Protocol.Type.ANIMAL_ATTACK || type == Protocol.Type.FISH_SPAWN;
    }
}
