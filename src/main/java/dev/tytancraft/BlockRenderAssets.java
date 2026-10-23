package dev.tytancraft;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.imageio.ImageIO;

final class BlockRenderAssets {
    private static final String[] SLUGS = createSlugs();
    private static final AtomicReferenceArray<BufferedImage> IMAGES = new AtomicReferenceArray<>(256);
    private static final AtomicIntegerArray STATES = new AtomicIntegerArray(256);
    private static final ExecutorService DOWNLOADS = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "blockrender-image-cache");
        thread.setDaemon(true);
        return thread;
    });

    private BlockRenderAssets() {}

    static String slugFor(byte blockType) { return SLUGS[blockType & 0xff]; }

    static void prefetch(byte[] blockTypes) {
        for (byte blockType : blockTypes) image(blockType);
    }

    static BufferedImage image(byte blockType) {
        if (Boolean.getBoolean("tytancraft.disableBlockRender")) return null;
        int index = blockType & 0xff;
        String slug = slugFor(blockType);
        if (slug == null) return null;
        BufferedImage cached = IMAGES.get(index);
        if (cached != null) return cached;
        if (STATES.compareAndSet(index, 0, 1)) DOWNLOADS.execute(() -> load(index, slug));
        return null;
    }

    private static void load(int index, String slug) {
        try {
            Path cache = cachePath(slug);
            BufferedImage image = Files.isRegularFile(cache) ? ImageIO.read(cache.toFile()) : null;
            if (image == null) {
                var url = URI.create("https://blockrender.dev/render/item/" + slug + ".png?size=64").toURL();
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "TytanCraft item icon cache");
                try (InputStream input = connection.getInputStream()) {
                    image = ImageIO.read(input);
                }
                if (image == null) throw new IOException("BlockRender returned an unreadable image");
                store(cache, slug, index, image);
            }
            IMAGES.set(index, image);
            STATES.set(index, 2);
        } catch (IOException | RuntimeException ignored) {
            STATES.set(index, -1);
        }
    }

    private static Path cachePath(String slug) {
        return Path.of(System.getProperty("user.home"), ".tytancraft", "blockrender", slug + ".png");
    }

    private static void store(Path destination, String slug, int index, BufferedImage image) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = destination.resolveSibling(slug + "-" + index + ".tmp");
        try {
            ImageIO.write(image, "png", temporary.toFile());
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String[] createSlugs() {
        String[] slugs = new String[256];
        set(slugs, World.GRASS, "grass_block");
        set(slugs, World.DIRT, "dirt");
        set(slugs, World.STONE, "stone");
        set(slugs, World.SAND, "sand");
        set(slugs, World.WOOD, "oak_log");
        set(slugs, World.LEAVES, "oak_leaves");
        set(slugs, World.COAL_ORE, "coal_ore");
        set(slugs, World.COPPER_ORE, "copper_ore");
        set(slugs, World.IRON_ORE, "iron_ore");
        set(slugs, World.GOLD_ORE, "gold_ore");
        set(slugs, World.DIAMOND_ORE, "diamond_ore");
        set(slugs, World.EMERALD_ORE, "emerald_ore");
        set(slugs, World.RUBY_ORE, "redstone_ore");
        set(slugs, World.SAPPHIRE_ORE, "lapis_ore");
        set(slugs, World.SNOW, "snow");
        set(slugs, World.RED_FLOWER, "poppy");
        set(slugs, World.YELLOW_FLOWER, "dandelion");
        set(slugs, World.BLUE_FLOWER, "cornflower");
        set(slugs, World.BIRCH_WOOD, "birch_log");
        set(slugs, World.BIRCH_LEAVES, "birch_leaves");
        set(slugs, World.PINE_WOOD, "spruce_log");
        set(slugs, World.PINE_LEAVES, "spruce_leaves");
        set(slugs, World.ACACIA_WOOD, "acacia_log");
        set(slugs, World.ACACIA_LEAVES, "acacia_leaves");
        set(slugs, World.CHERRY_WOOD, "cherry_log");
        set(slugs, World.CHERRY_LEAVES, "cherry_leaves");
        set(slugs, World.PLANKS, "oak_planks");
        set(slugs, World.TORCH, "torch");
        set(slugs, World.EMERALD_BLOCK, "emerald_block");
        set(slugs, World.RED_MUSHROOM_SMALL, "red_mushroom");
        set(slugs, World.RED_MUSHROOM_MEDIUM, "red_mushroom");
        set(slugs, World.RED_MUSHROOM_LARGE, "red_mushroom");
        set(slugs, World.BROWN_MUSHROOM_SMALL, "brown_mushroom");
        set(slugs, World.BROWN_MUSHROOM_MEDIUM, "brown_mushroom");
        set(slugs, World.BROWN_MUSHROOM_LARGE, "brown_mushroom");
        set(slugs, World.COD_SPAWN_EGG, "cod_spawn_egg");
        set(slugs, World.SALMON_SPAWN_EGG, "salmon_spawn_egg");
        set(slugs, World.TROPICAL_FISH_SPAWN_EGG, "tropical_fish_spawn_egg");
        set(slugs, World.PIKE_SPAWN_EGG, "pufferfish_spawn_egg");
        set(slugs, World.CARP_SPAWN_EGG, "cod_spawn_egg");
        set(slugs, World.WOOL, "white_wool");
        set(slugs, World.LADDER, "ladder");
        set(slugs, World.LADDER_Z_NEG, "ladder");
        set(slugs, World.LADDER_X_POS, "ladder");
        set(slugs, World.LADDER_X_NEG, "ladder");
        return slugs;
    }

    private static void set(String[] slugs, byte type, String slug) { slugs[type & 0xff] = slug; }
}
