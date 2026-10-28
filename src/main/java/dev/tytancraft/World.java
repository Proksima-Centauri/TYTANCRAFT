package dev.tytancraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

final class World {
    record BlockChange(int x, int y, int z, byte type, short waterFlow) {}
    static final int WIDTH = 192;
    static final int HEIGHT = 64;
    static final int DEPTH = 192;
    static final int WATER_LEVEL = 10;
    private static final int MAX_WATER_FLOW = WIDTH + DEPTH;

    static final byte AIR = 0;
    static final byte GRASS = 1;
    static final byte DIRT = 2;
    static final byte STONE = 3;
    static final byte SAND = 4;
    static final byte WOOD = 5;
    static final byte LEAVES = 6;
    static final byte WATER = 7;
    static final byte COPPER_ORE = 8;
    static final byte IRON_ORE = 9;
    static final byte GOLD_ORE = 10;
    static final byte COAL_ORE = 11;
    static final byte DIAMOND_ORE = 12;
    static final byte EMERALD_ORE = 13;
    static final byte RUBY_ORE = 14;
    static final byte SAPPHIRE_ORE = 15;
    static final byte SNOW = 16;
    static final byte RED_FLOWER = 17;
    static final byte YELLOW_FLOWER = 18;
    static final byte BLUE_FLOWER = 19;
    static final byte BIRCH_WOOD = 20;
    static final byte BIRCH_LEAVES = 21;
    static final byte PINE_WOOD = 22;
    static final byte PINE_LEAVES = 23;
    static final byte ACACIA_WOOD = 24;
    static final byte ACACIA_LEAVES = 25;
    static final byte CHERRY_WOOD = 26;
    static final byte CHERRY_LEAVES = 27;
    static final byte PLANKS = 28;
    static final byte TORCH = 29;
    static final byte EMERALD_BLOCK = 30;
    static final byte RED_MUSHROOM_SMALL = 31;
    static final byte RED_MUSHROOM_MEDIUM = 32;
    static final byte RED_MUSHROOM_LARGE = 33;
    static final byte BROWN_MUSHROOM_SMALL = 34;
    static final byte BROWN_MUSHROOM_MEDIUM = 35;
    static final byte BROWN_MUSHROOM_LARGE = 36;
    static final byte COD_SPAWN_EGG = 37;
    static final byte SALMON_SPAWN_EGG = 38;
    static final byte TROPICAL_FISH_SPAWN_EGG = 39;
    static final byte PIKE_SPAWN_EGG = 40;
    static final byte CARP_SPAWN_EGG = 41;
    static final byte WOOL = 42;
    static final byte LADDER = 43;
    static final byte LADDER_Z_NEG = 44;
    static final byte LADDER_X_POS = 45;
    static final byte LADDER_X_NEG = 46;
    private final byte[] blocks = new byte[WIDTH * HEIGHT * DEPTH];
    private final short[] waterFlow = new short[WIDTH * HEIGHT * DEPTH];
    private final short[] waterVisits = new short[WIDTH * HEIGHT * DEPTH];
    private final byte[] torchLight = new byte[WIDTH * HEIGHT * DEPTH];
    private final int[] heights = new int[WIDTH * DEPTH];
    private final int[] canopyHeights = new int[WIDTH * DEPTH];
    private final WaterQueue waterQueue = new WaterQueue();
    private final ArrayList<BlockChange> waterChanges = new ArrayList<>();
    private boolean trackWaterChanges;
    private short waterVisitGeneration = 1;
    private final long seed;

    World(long seed) {
        this.seed = seed;
        generateBaseTerrain();
        carveCaves();
        recalculateAllHeights();
        generateOres();
        growTrees();
        recalculateAllCanopyHeights();
        growFlowers();
        growMushrooms();
    }

    World(long seed, byte[] savedBlocks, short[] savedWaterFlow) {
        this(seed, savedBlocks, savedWaterFlow, new long[0]);
    }

    World(long seed, byte[] savedBlocks, short[] savedWaterFlow, long[] savedWaterQueue) {
        if (savedBlocks.length != blocks.length || savedWaterFlow.length != waterFlow.length) {
            throw new IllegalArgumentException("Nieprawidłowy rozmiar zapisanego świata");
        }
        this.seed = seed;
        System.arraycopy(savedBlocks, 0, blocks, 0, blocks.length);
        System.arraycopy(savedWaterFlow, 0, waterFlow, 0, waterFlow.length);
        recalculateAllHeights();
        recalculateAllCanopyHeights();
        rebuildTorchLighting();
        for (long entry : savedWaterQueue) {
            int cell = (int) entry;
            int level = (int) (entry >>> 32);
            if (cell < 0 || cell >= blocks.length || level < 0 || level > MAX_WATER_FLOW
                    || blocks[cell] != WATER) continue;
            waterQueue.add(entry);
        }
    }

    long seed() { return seed; }

    byte[] copyBlocks() { return blocks.clone(); }

    short[] copyWaterFlow() { return waterFlow.clone(); }

    long[] copyPendingWaterFlow() { return waterQueue.toArray(); }

    void trackWaterChanges(boolean enabled) {
        trackWaterChanges = enabled;
        waterChanges.clear();
    }

    List<BlockChange> drainWaterChanges() {
        if (waterChanges.isEmpty()) return List.of();
        List<BlockChange> changes = List.copyOf(waterChanges);
        waterChanges.clear();
        return changes;
    }

    int torchLightAt(int x, int y, int z) {
        if (!inBounds(x, y, z)) return 0;
        return torchLight[index(x, y, z)] & 0xff;
    }

    int[] findDrySpawn() {
        int centerX = WIDTH / 2, centerZ = DEPTH / 2;
        for (int radius = 0; radius <= 64; radius += 4) {
            int samples = Math.max(1, radius * 2);
            for (int sample = 0; sample < samples; sample++) {
                float angle = (float) (Math.PI * 2 * sample / samples);
                int x = centerX + Math.round((float) Math.sin(angle) * radius);
                int z = centerZ + Math.round((float) Math.cos(angle) * radius);
                int height = heightAt(x, z);
                if (height > WATER_LEVEL + 1 && get(x, height + 1, z) == AIR
                        && get(x, height + 2, z) == AIR) return new int[]{x, z};
            }
        }
        return new int[]{centerX, centerZ};
    }

    int heightAt(int x, int z) {
        if (!insideWorld(x, z)) return WATER_LEVEL;
        return heights[z * WIDTH + x];
    }

    int canopyHeightAt(int x, int z) {
        if (!insideWorld(x, z)) return WATER_LEVEL;
        return canopyHeights[z * WIDTH + x];
    }

    static boolean inBounds(int x, int y, int z) {
        return insideWorld(x, z) && y >= 0 && y < HEIGHT;
    }

    byte get(int x, int y, int z) {
        if (!insideWorld(x, z) || y < 0 || y >= HEIGHT) return AIR;
        return blocks[index(x, y, z)];
    }

    void set(int x, int y, int z, byte type) {
        if (!insideWorld(x, z) || y < 0 || y >= HEIGHT) return;
        int blockIndex = index(x, y, z);
        if (blocks[blockIndex] == type) return;
        byte oldType = blocks[blockIndex];
        blocks[blockIndex] = type;
        if (type != WATER) waterFlow[blockIndex] = 0;
        if (y >= heights[z * WIDTH + x] - 1) recalculateHeight(x, z);
        if (y >= canopyHeights[z * WIDTH + x] - 1) recalculateCanopyHeight(x, z);
        if (oldType == TORCH && type != TORCH) rebuildTorchLighting();
        else if (type == TORCH) spreadTorchLightFrom(blockIndex);
    }

    void applyWaterChange(int x, int y, int z, short flowLevel) {
        if (!insideWorld(x, z) || y < 0 || y >= HEIGHT || flowLevel < 0
                || (flowLevel & 0xffff) > MAX_WATER_FLOW) return;
        int cell = index(x, y, z);
        blocks[cell] = WATER;
        waterFlow[cell] = flowLevel;
    }

    void flowWaterInto(int x, int y, int z) {
        if (!insideWorld(x, z) || y < 0 || y >= HEIGHT || get(x, y, z) != AIR) return;
        if (get(x - 1, y, z) != WATER && get(x + 1, y, z) != WATER
                && get(x, y - 1, z) != WATER && get(x, y + 1, z) != WATER
                && get(x, y, z - 1) != WATER && get(x, y, z + 1) != WATER) return;
        if (++waterVisitGeneration == 0) {
            Arrays.fill(waterVisits, (short) 0);
            waterVisitGeneration = 1;
        }
        addWaterSource(x - 1, y, z);
        addWaterSource(x + 1, y, z);
        addWaterSource(x, y - 1, z);
        addWaterSource(x, y + 1, z);
        addWaterSource(x, y, z - 1);
        addWaterSource(x, y, z + 1);
    }

    void tickWaterFlow() {
        for (int processed = 0; processed < 192 && !waterQueue.isEmpty(); processed++) {
            long entry = waterQueue.remove();
            int cell = (int) entry;
            if (waterVisits[cell] == waterVisitGeneration || blocks[cell] != WATER) continue;
            waterVisits[cell] = waterVisitGeneration;
            int level = (int) (entry >>> 32);
            int y = cell / (WIDTH * DEPTH);
            int remainder = cell - y * WIDTH * DEPTH;
            int z = remainder / WIDTH;
            int x = remainder - z * WIDTH;
            if (y > 1) spreadWater(x, y - 1, z, level);
            if (level >= MAX_WATER_FLOW) continue;
            int nextLevel = level + 1;
            spreadWater(x - 1, y, z, nextLevel);
            spreadWater(x + 1, y, z, nextLevel);
            spreadWater(x, y, z - 1, nextLevel);
            spreadWater(x, y, z + 1, nextLevel);
        }
    }

    static boolean isFlower(byte type) { return BlockRegistry.has(type, BlockRegistry.FLOWER); }
    static boolean isMushroom(byte type) { return BlockRegistry.has(type, BlockRegistry.MUSHROOM); }
    static boolean isFishSpawnEgg(byte type) { return BlockRegistry.has(type, BlockRegistry.FISH_EGG); }
    static boolean isLadderBlock(byte type) { return BlockRegistry.has(type, BlockRegistry.CLIMBABLE); }

    static byte ladderAgainstWall(int normalX, int normalZ) {
        if (normalX > 0) return LADDER_X_NEG;
        if (normalX < 0) return LADDER_X_POS;
        if (normalZ > 0) return LADDER_Z_NEG;
        return LADDER;
    }

    static boolean isTreeBlock(byte type) {
        return BlockRegistry.has(type, BlockRegistry.TREE);
    }

    static boolean isWood(byte type) {
        return BlockRegistry.has(type, BlockRegistry.WOOD);
    }

    static boolean isSolid(byte type) {
        return BlockRegistry.has(type, BlockRegistry.SOLID);
    }

    private void generateBaseTerrain() {
        for (int z = 0; z < DEPTH; z++) {
            for (int x = 0; x < WIDTH; x++) {
                int height = terrainHeight(x, z);
                heights[z * WIDTH + x] = height;
                byte surface = surfaceBlock(x, z, height);
                for (int y = 0; y <= height; y++) {
                    byte type;
                    if (y == 0) type = STONE;
                    else if (y == height) type = surface;
                    else if (y >= height - 3) type = surface == SAND ? SAND : DIRT;
                    else type = STONE;
                    blocks[index(x, y, z)] = type;
                }
                if (height < WATER_LEVEL) {
                    for (int y = height + 1; y <= WATER_LEVEL; y++) blocks[index(x, y, z)] = WATER;
                }
            }
        }
    }

    private void carveCaves() {
        for (int z = 0; z < DEPTH; z++) {
            for (int x = 0; x < WIDTH; x++) {
                int surface = heights[z * WIDTH + x];
                for (int y = 4; y < surface - 3; y++) {
                    int cell = index(x, y, z);
                    if (blocks[cell] != STONE) continue;
                    float broad = noise3(x * 0.045f, y * 0.075f, z * 0.045f, seed ^ 0x13198a2e03707344L);
                    float detail = noise3(x * 0.12f, y * 0.17f, z * 0.12f, seed ^ 0xa4093822299f31d0L);
                    if (broad > 0.66f && detail > 0.43f) blocks[cell] = AIR;
                }
            }
        }

        Random random = new Random(seed ^ 0x1f83d9abfb41bd6bL);
        int entranceCount = Math.max(18, WIDTH * DEPTH / 1800);
        for (int entrance = 0; entrance < entranceCount; entrance++) {
            int x = 8 + random.nextInt(WIDTH - 16);
            int z = 8 + random.nextInt(DEPTH - 16);
            int surface = heights[z * WIDTH + x];
            if (surface < WATER_LEVEL + 6) continue;
            carveTunnel(random, x, z, surface - 0.9f, random.nextFloat() * (float) (Math.PI * 2),
                    18 + random.nextInt(16), true);
        }
        int tunnelCount = Math.max(80, WIDTH * DEPTH / 220);
        for (int cave = 0; cave < tunnelCount; cave++) {
            int x = 4 + random.nextInt(WIDTH - 8);
            int z = 4 + random.nextInt(DEPTH - 8);
            int surface = heights[z * WIDTH + x];
            if (surface < WATER_LEVEL + 7) continue;
            int y = 4 + random.nextInt(Math.max(1, surface - 8));
            carveTunnel(random, x, z, y, random.nextFloat() * (float) (Math.PI * 2),
                    15 + random.nextInt(28), false);
        }
    }

    private void carveTunnel(Random random, float x, float z, float y, float angle, int length, boolean entrance) {
        for (int step = 0; step < length; step++) {
            float radius = 1.25f + random.nextFloat() * 0.55f;
            int centerX = Math.round(x), centerY = Math.round(y), centerZ = Math.round(z);
            int horizontalRadius = (int) Math.ceil(radius), verticalRadius = (int) Math.ceil(radius * 0.78f);
            for (int by = centerY - verticalRadius; by <= centerY + verticalRadius; by++) {
                if (by < 2 || by >= HEIGHT - 2) continue;
                for (int bz = centerZ - horizontalRadius; bz <= centerZ + horizontalRadius; bz++) {
                    for (int bx = centerX - horizontalRadius; bx <= centerX + horizontalRadius; bx++) {
                        if (!insideWorld(bx, bz)) continue;
                        float dx = (bx - x) / radius, dy = (by - y) / (radius * 0.78f), dz = (bz - z) / radius;
                        if (dx * dx + dy * dy + dz * dz > 1) continue;
                        int ceiling = heights[bz * WIDTH + bx] - 2;
                        if (entrance && step < 3) ceiling = heights[bz * WIDTH + bx];
                        int cell = index(bx, by, bz);
                        if (by <= ceiling && blocks[cell] != WATER) blocks[cell] = AIR;
                    }
                }
            }
            x += (float) Math.sin(angle) * 0.85f;
            z += (float) Math.cos(angle) * 0.85f;
            if (entrance && step < 4) y -= 0.52f;
            else y += (random.nextFloat() - 0.58f) * 0.36f;
            angle += (random.nextFloat() - 0.5f) * 0.36f;
            if (x < 5 || z < 5 || x >= WIDTH - 5 || z >= DEPTH - 5) break;
        }
    }

    private void generateOres() {
        Random random = new Random(seed ^ 0x5be0cd19137e2179L);
        float areaScale = WIDTH * DEPTH / (112.0f * 112.0f);
        generateVeins(random, COAL_ORE, Math.round(185 * areaScale), 3, 34, 5, 12);
        generateVeins(random, COPPER_ORE, Math.round(125 * areaScale), 5, 28, 4, 9);
        generateVeins(random, IRON_ORE, Math.round(115 * areaScale), 4, 27, 4, 9);
        generateVeins(random, GOLD_ORE, Math.round(54 * areaScale), 3, 17, 3, 7);
        generateVeins(random, DIAMOND_ORE, Math.round(20 * areaScale), 3, 10, 2, 5);
        generateVeins(random, EMERALD_ORE, Math.round(16 * areaScale), 5, 19, 2, 4);
        generateVeins(random, RUBY_ORE, Math.round(19 * areaScale), 3, 13, 2, 5);
        generateVeins(random, SAPPHIRE_ORE, Math.round(19 * areaScale), 3, 15, 2, 5);
    }

    private void generateVeins(Random random, byte ore, int count, int minY, int maxY, int minSize, int maxSize) {
        for (int vein = 0; vein < count; vein++) {
            int x = 2 + random.nextInt(WIDTH - 4), z = 2 + random.nextInt(DEPTH - 4);
            int y = minY + random.nextInt(maxY - minY + 1);
            int size = minSize + random.nextInt(maxSize - minSize + 1);
            for (int part = 0; part < size; part++) {
                if (y > 2 && y < heights[z * WIDTH + x] - 3 && get(x, y, z) == STONE) {
                    blocks[index(x, y, z)] = ore;
                }
                x = Math.max(2, Math.min(WIDTH - 3, x + random.nextInt(3) - 1));
                z = Math.max(2, Math.min(DEPTH - 3, z + random.nextInt(3) - 1));
                y = Math.max(minY, Math.min(maxY, y + random.nextInt(3) - 1));
            }
        }
    }

    private void growTrees() {
        Random random = new Random(seed ^ 0x4f1bbcdcL);
        int attempts = WIDTH * DEPTH * 5 / 256;
        for (int tree = 0; tree < attempts; tree++) {
            int x = 3 + random.nextInt(WIDTH - 6), z = 3 + random.nextInt(DEPTH - 6);
            int y = heights[z * WIDTH + x];
            byte ground = get(x, y, z);
            if ((ground != GRASS && ground != SNOW) || get(x, y + 1, z) != AIR) continue;
            float temperature = noise2(x * 0.035, z * 0.035, seed ^ 0x082efa98ec4e6c89L);
            float humidity = noise2(x * 0.043, z * 0.043, seed ^ 0x452821e638d01377L);
            float density;
            int species;
            if (ground == SNOW) {
                species = 2;
                density = 0.54f;
            } else if (ground == GRASS && temperature > 0.63f && humidity < 0.42f) {
                species = 3;
                density = 0.42f;
            } else if (temperature < 0.24f) {
                species = 2;
                density = 0.54f;
            } else {
                float kind = random.nextFloat();
                species = kind < 0.43f ? 0 : kind < 0.73f ? 1 : kind < 0.91f ? 4 : 2;
                density = 0.60f;
            }
            if (random.nextFloat() > density) continue;
            growTree(x, y, z, species, random);
        }
    }

    private void growTree(int x, int y, int z, int species, Random random) {
        if (species == 2) {
            int height = 6 + random.nextInt(4);
            for (int dy = 1; dy <= height; dy++) setDirect(x, y + dy, z, PINE_WOOD);
            int crown = y + height;
            for (int layer = 0; layer < height - 1; layer++) {
                int layerY = crown - layer;
                int radius = Math.min(4, 1 + layer / 2);
                for (int dz = -radius; dz <= radius; dz++) for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dz) <= radius + 1 && get(x + dx, layerY, z + dz) == AIR) {
                        setDirect(x + dx, layerY, z + dz, PINE_LEAVES);
                    }
                }
            }
            return;
        }

        if (species == 3) {
            int height = 4 + random.nextInt(3);
            for (int dy = 1; dy <= height; dy++) setDirect(x, y + dy, z, ACACIA_WOOD);
            boolean alongX = random.nextBoolean();
            int direction = random.nextBoolean() ? 1 : -1;
            int crownY = y + height;
            int crownX = x + (alongX ? direction * 2 : 0);
            int crownZ = z + (alongX ? 0 : direction * 2);
            for (int offset = 1; offset <= 2; offset++) {
                int branchX = x + (alongX ? direction * offset : 0);
                int branchZ = z + (alongX ? 0 : direction * offset);
                setDirect(branchX, crownY - offset / 2, branchZ, ACACIA_WOOD);
            }
            growLeafCrown(crownX, crownY, crownZ, 3, 1, ACACIA_LEAVES, true);
            growLeafCrown(x - (alongX ? direction * 2 : 0), crownY, z - (alongX ? 0 : direction * 2),
                    2, 1, ACACIA_LEAVES, true);
            return;
        }

        byte log = species == 1 ? BIRCH_WOOD : species == 4 ? CHERRY_WOOD : WOOD;
        byte leaves = species == 1 ? BIRCH_LEAVES : species == 4 ? CHERRY_LEAVES : LEAVES;
        int trunk = species == 1 ? 5 + random.nextInt(3) : 4 + random.nextInt(2);
        for (int dy = 1; dy <= trunk; dy++) setDirect(x, y + dy, z, log);
        int radius = species == 4 ? 3 : species == 1 ? 2 : 2;
        growLeafCrown(x, y + trunk, z, radius, species == 4 ? 2 : 2, leaves, false);
    }

    private void growLeafCrown(int x, int y, int z, int radius, int verticalRadius, byte leafType, boolean flat) {
        for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
            int layerRadius = flat ? radius - (Math.abs(dy) == verticalRadius ? 1 : 0)
                    : Math.max(1, radius - Math.max(0, Math.abs(dy) - 1));
            for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                    boolean inside = flat ? Math.abs(dx) <= layerRadius && Math.abs(dz) <= layerRadius
                            : Math.abs(dx) + Math.abs(dz) <= layerRadius + 1;
                    if (inside && get(x + dx, y + dy, z + dz) == AIR) {
                        setDirect(x + dx, y + dy, z + dz, leafType);
                    }
                }
            }
        }
    }

    private void growFlowers() {
        Random random = new Random(seed ^ 0x3c6ef372fe94f82bL);
        int placed = 0;
        for (int attempt = 0; attempt < 512 && placed < 5; attempt++) {
            int x = 1 + random.nextInt(WIDTH - 2), z = 1 + random.nextInt(DEPTH - 2);
            int y = heights[z * WIDTH + x];
            if (get(x, y, z) == GRASS && get(x, y + 1, z) == AIR) {
                setDirect(x, y + 1, z, switch (random.nextInt(3)) {
                    case 0 -> RED_FLOWER;
                    case 1 -> YELLOW_FLOWER;
                    default -> BLUE_FLOWER;
                });
                placed++;
            }
        }
    }

    private void growMushrooms() {
        Random random = new Random(seed ^ 0x1f6a2c3d4e5b7081L);
        int attempts = WIDTH * DEPTH / 150;
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = 1 + random.nextInt(WIDTH - 2), z = 1 + random.nextInt(DEPTH - 2);
            int y = heights[z * WIDTH + x];
            if (get(x, y, z) != GRASS || y + 1 >= HEIGHT || get(x, y + 1, z) != AIR) continue;
            boolean red = random.nextBoolean();
            float sizeRoll = random.nextFloat();
            byte type = sizeRoll < 0.58f
                    ? (red ? RED_MUSHROOM_SMALL : BROWN_MUSHROOM_SMALL)
                    : sizeRoll < 0.91f
                    ? (red ? RED_MUSHROOM_MEDIUM : BROWN_MUSHROOM_MEDIUM)
                    : (red ? RED_MUSHROOM_LARGE : BROWN_MUSHROOM_LARGE);
            setDirect(x, y + 1, z, type);
        }
    }

    private byte surfaceBlock(int x, int z, int height) {
        if (height <= WATER_LEVEL + 1) return SAND;
        float temperature = noise2(x * 0.035, z * 0.035, seed ^ 0x082efa98ec4e6c89L);
        float humidity = noise2(x * 0.043, z * 0.043, seed ^ 0x452821e638d01377L);
        if (temperature < 0.24f) return SNOW;
        if (temperature > 0.72f && humidity < 0.25f) return SAND;
        return GRASS;
    }

    private int terrainHeight(int x, int z) {
        float continents = fbm(x * 0.012, z * 0.012, seed ^ 0x243f6a8885a308d3L);
        float hills = fbm(x * 0.034, z * 0.034, seed ^ 0x13198a2e03707344L);
        float detail = noise2(x * 0.105, z * 0.105, seed ^ 0xa4093822299f31d0L);
        float ridgeNoise = noise2(x * 0.021, z * 0.021, seed ^ 0x082efa98ec4e6c89L);
        float ridges = 1.0f - Math.abs(ridgeNoise * 2.0f - 1.0f);
        float height = 17.0f + (continents - 0.5f) * 66.0f + (hills - 0.5f) * 44.0f
                + (detail - 0.5f) * 16.0f + Math.max(0, ridges - 0.70f) * 34.0f;
        return Math.max(4, Math.min(HEIGHT - 8, Math.round(height)));
    }

    private float fbm(double x, double z, long salt) {
        return (noise2(x, z, salt) * 0.57f
                + noise2(x * 2.0, z * 2.0, salt ^ 0x9e3779b97f4a7c15L) * 0.29f
                + noise2(x * 4.0, z * 4.0, salt ^ 0xc2b2ae3d27d4eb4fL) * 0.14f);
    }

    private float noise2(double x, double z, long salt) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        float tx = fade((float) (x - x0)), tz = fade((float) (z - z0));
        float a = hashNoise(x0, z0, salt), b = hashNoise(x0 + 1, z0, salt);
        float c = hashNoise(x0, z0 + 1, salt), d = hashNoise(x0 + 1, z0 + 1, salt);
        float top = a + (b - a) * tx, bottom = c + (d - c) * tx;
        return top + (bottom - top) * tz;
    }

    private float noise3(double x, double y, double z, long salt) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
        float tx = fade((float) (x - x0)), ty = fade((float) (y - y0)), tz = fade((float) (z - z0));
        float c000 = hashNoise3(x0, y0, z0, salt), c100 = hashNoise3(x0 + 1, y0, z0, salt);
        float c010 = hashNoise3(x0, y0 + 1, z0, salt), c110 = hashNoise3(x0 + 1, y0 + 1, z0, salt);
        float c001 = hashNoise3(x0, y0, z0 + 1, salt), c101 = hashNoise3(x0 + 1, y0, z0 + 1, salt);
        float c011 = hashNoise3(x0, y0 + 1, z0 + 1, salt), c111 = hashNoise3(x0 + 1, y0 + 1, z0 + 1, salt);
        float z0a = lerp(lerp(c000, c100, tx), lerp(c010, c110, tx), ty);
        float z1a = lerp(lerp(c001, c101, tx), lerp(c011, c111, tx), ty);
        return lerp(z0a, z1a, tz);
    }

    private float hashNoise(int x, int z, long salt) {
        long value = mix64(salt ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL));
        return (value >>> 40) / 16777215.0f;
    }

    private float hashNoise3(int x, int y, int z, long salt) {
        long value = mix64(salt ^ (x * 0x9E3779B97F4A7C15L)
                ^ (y * 0xD6E8FEB86659FD93L) ^ (z * 0xC2B2AE3D27D4EB4FL));
        return (value >>> 40) / 16777215.0f;
    }

    private long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private void recalculateAllHeights() {
        for (int z = 0; z < DEPTH; z++) for (int x = 0; x < WIDTH; x++) recalculateHeight(x, z);
    }

    private void recalculateAllCanopyHeights() {
        for (int z = 0; z < DEPTH; z++) for (int x = 0; x < WIDTH; x++) recalculateCanopyHeight(x, z);
    }

    private void recalculateHeight(int x, int z) {
        for (int y = HEIGHT - 1; y >= 0; y--) {
            byte type = blocks[index(x, y, z)];
            if (type != AIR && type != WATER && !isFlower(type) && !isMushroom(type)
                    && !isTreeBlock(type) && !isLadderBlock(type)) {
                heights[z * WIDTH + x] = y;
                return;
            }
        }
        heights[z * WIDTH + x] = 0;
    }

    private void recalculateCanopyHeight(int x, int z) {
        for (int y = HEIGHT - 1; y >= 0; y--) {
            byte type = blocks[index(x, y, z)];
            if (type != AIR && type != WATER && !isFlower(type) && !isMushroom(type) && !isLadderBlock(type)) {
                canopyHeights[z * WIDTH + x] = y;
                return;
            }
        }
        canopyHeights[z * WIDTH + x] = 0;
    }

    private void setDirect(int x, int y, int z, byte type) {
        if (insideWorld(x, z) && y >= 0 && y < HEIGHT) blocks[index(x, y, z)] = type;
    }

    private void addWaterSource(int x, int y, int z) {
        if (get(x, y, z) != WATER) return;
        int cell = index(x, y, z);
        if (waterVisits[cell] == waterVisitGeneration) return;
        int level = waterFlow[cell] & 0xffff;
        if (level >= MAX_WATER_FLOW) level = 0;
        waterQueue.add(packWaterCell(cell, level));
    }

    private void spreadWater(int x, int y, int z, int level) {
        if (level > MAX_WATER_FLOW || !insideWorld(x, z) || y < 0 || y >= HEIGHT || get(x, y, z) != AIR) return;
        int cell = index(x, y, z);
        blocks[cell] = WATER;
        waterFlow[cell] = (short) level;
        waterQueue.add(packWaterCell(cell, level));
        if (trackWaterChanges) waterChanges.add(new BlockChange(x, y, z, WATER, (short) level));
    }

    private void spreadTorchLightFrom(int source) {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        torchLight[source] = 15;
        queue.add(source);
        propagateTorchLight(queue);
    }

    private void rebuildTorchLighting() {
        Arrays.fill(torchLight, (byte) 0);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int cell = 0; cell < blocks.length; cell++) {
            if (blocks[cell] != TORCH) continue;
            torchLight[cell] = 15;
            queue.addLast(cell);
        }
        propagateTorchLight(queue);
    }

    private void propagateTorchLight(ArrayDeque<Integer> queue) {
        while (!queue.isEmpty()) {
            int cell = queue.removeFirst();
            int light = (torchLight[cell] & 0xff) - 2;
            if (light <= 0) continue;
            int y = cell / (WIDTH * DEPTH);
            int remainder = cell - y * WIDTH * DEPTH;
            int z = remainder / WIDTH;
            int x = remainder - z * WIDTH;
            spreadTorchLight(x - 1, y, z, light, queue);
            spreadTorchLight(x + 1, y, z, light, queue);
            spreadTorchLight(x, y - 1, z, light, queue);
            spreadTorchLight(x, y + 1, z, light, queue);
            spreadTorchLight(x, y, z - 1, light, queue);
            spreadTorchLight(x, y, z + 1, light, queue);
        }
    }

    private void spreadTorchLight(int x, int y, int z, int light, ArrayDeque<Integer> queue) {
        if (!inBounds(x, y, z)) return;
        int cell = index(x, y, z);
        if (light <= (torchLight[cell] & 0xff)) return;
        torchLight[cell] = (byte) light;
        if (blocks[cell] == TORCH || !isSolid(blocks[cell])) queue.addLast(cell);
    }

    private static long packWaterCell(int cell, int level) { return ((long) level << 32) | (cell & 0xffffffffL); }

    private int index(int x, int y, int z) { return (y * DEPTH + z) * WIDTH + x; }
    private static boolean insideWorld(int x, int z) { return x >= 0 && z >= 0 && x < WIDTH && z < DEPTH; }
    private static float fade(float value) { return value * value * (3 - 2 * value); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static final class WaterQueue {
        private long[] entries = new long[1024];
        private int first, size;

        boolean isEmpty() { return size == 0; }

        void add(long entry) {
            if (size == entries.length) grow();
            entries[(first + size) % entries.length] = entry;
            size++;
        }

        long remove() {
            long entry = entries[first];
            first = (first + 1) % entries.length;
            size--;
            return entry;
        }

        long[] toArray() {
            long[] snapshot = new long[size];
            for (int i = 0; i < size; i++) snapshot[i] = entries[(first + i) % entries.length];
            return snapshot;
        }

        private void grow() {
            long[] larger = new long[entries.length * 2];
            for (int i = 0; i < size; i++) larger[i] = entries[(first + i) % entries.length];
            entries = larger;
            first = 0;
        }
    }
}
