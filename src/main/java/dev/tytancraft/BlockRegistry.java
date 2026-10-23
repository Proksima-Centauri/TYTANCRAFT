package dev.tytancraft;

final class BlockRegistry {
    static final int SOLID = 1;
    static final int PLANT = 1 << 1;
    static final int FLOWER = 1 << 2;
    static final int MUSHROOM = 1 << 3;
    static final int FISH_EGG = 1 << 4;
    static final int TREE = 1 << 5;
    static final int WOOD = 1 << 6;
    static final int LEAF = 1 << 7;
    static final int ORE = 1 << 8;
    static final int CLIMBABLE = 1 << 9;

    record Info(String name, int iconColor, int shadedColor, int flags) {}

    private static final Info UNKNOWN = new Info("Blok", 0x209a5d, 0x307a8f, SOLID);
    private static final Info[] BLOCKS = new Info[256];

    static {
        register(World.AIR, "Powietrze", 0x000000, 0x000000, 0);
        register(World.GRASS, "Trawa", 0x60a144, 0x6ba75b, SOLID);
        register(World.DIRT, "Ziemia", 0x916541, 0x7d5b3d, SOLID);
        register(World.STONE, "Kamień", 0x919999, 0x787e7e, SOLID);
        register(World.SAND, "Piasek", 0xdfcb91, 0xcdbe8b, SOLID);
        register(World.WOOD, "Drewno", 0x8e5d32, 0x7e5530, SOLID | TREE | WOOD);
        register(World.LEAVES, "Liście", 0x478b4c, 0x377841, SOLID | TREE | LEAF);
        register(World.WATER, "Woda", 0x209a5d, 0x307a8f, 0);
        register(World.COPPER_ORE, "Miedź", 0xbe7046, 0xbe7046, SOLID | ORE);
        register(World.IRON_ORE, "Żelazo", 0xaf846c, 0xaf846c, SOLID | ORE);
        register(World.GOLD_ORE, "Złoto", 0xe2ba41, 0xe2ba41, SOLID | ORE);
        register(World.COAL_ORE, "Węgiel", 0x3e4349, 0x3e4349, SOLID | ORE);
        register(World.DIAMOND_ORE, "Diament", 0x51d3e1, 0x51d3e1, SOLID | ORE);
        register(World.EMERALD_ORE, "Szmaragd", 0x2fc06f, 0x2fc06f, SOLID | ORE);
        register(World.RUBY_ORE, "Rubin", 0xd0394f, 0xd0394f, SOLID | ORE);
        register(World.SAPPHIRE_ORE, "Szafir", 0x4774e5, 0x4774e5, SOLID | ORE);
        register(World.SNOW, "Śnieg", 0xdfecf1, 0xdfecf1, SOLID);
        register(World.RED_FLOWER, "Mak", 0xd74153, 0xd74153, PLANT | FLOWER);
        register(World.YELLOW_FLOWER, "Jaskier", 0xf4cd44, 0xf4cd44, PLANT | FLOWER);
        register(World.BLUE_FLOWER, "Chaber", 0x5778e2, 0x5778e2, PLANT | FLOWER);
        register(World.BIRCH_WOOD, "Drewno brzozy", 0xe0d7b3, 0xe0d7b3, SOLID | TREE | WOOD);
        register(World.BIRCH_LEAVES, "Liście brzozy", 0x74a850, 0x74a850, SOLID | TREE | LEAF);
        register(World.PINE_WOOD, "Drewno świerku", 0x60452c, 0x60452c, SOLID | TREE | WOOD);
        register(World.PINE_LEAVES, "Igły świerku", 0x255938, 0x255938, SOLID | TREE | LEAF);
        register(World.ACACIA_WOOD, "Drewno akacji", 0xa66939, 0xa66939, SOLID | TREE | WOOD);
        register(World.ACACIA_LEAVES, "Liście akacji", 0x7e8e3e, 0x7e8e3e, SOLID | TREE | LEAF);
        register(World.CHERRY_WOOD, "Drewno wiśni", 0x814d49, 0x814d49, SOLID | TREE | WOOD);
        register(World.CHERRY_LEAVES, "Liście wiśni", 0xe087ad, 0xe087ad, SOLID | TREE | LEAF);
        register(World.PLANKS, "Deski", 0xaf7d4b, 0xaf7d4b, SOLID);
        register(World.TORCH, "Pochodnia", 0xf5a639, 0xf5a639, SOLID);
        register(World.EMERALD_BLOCK, "Blok szmaragdu", 0x209a5d, 0x209a5d, SOLID);
        register(World.RED_MUSHROOM_SMALL, "Mały muchomor", 0xa37d70, 0xbe3836, PLANT | MUSHROOM);
        register(World.RED_MUSHROOM_MEDIUM, "Średni muchomor", 0xa37d70, 0xbe3836, PLANT | MUSHROOM);
        register(World.RED_MUSHROOM_LARGE, "Duży muchomor", 0xa37d70, 0xbe3836, PLANT | MUSHROOM);
        register(World.BROWN_MUSHROOM_SMALL, "Mały grzyb jadalny", 0xa37d70, 0x97673e, PLANT | MUSHROOM);
        register(World.BROWN_MUSHROOM_MEDIUM, "Średni grzyb jadalny", 0xa37d70, 0x97673e, PLANT | MUSHROOM);
        register(World.BROWN_MUSHROOM_LARGE, "Duży grzyb jadalny", 0xa37d70, 0x97673e, PLANT | MUSHROOM);
        register(World.COD_SPAWN_EGG, "Ikra dorsza", 0x70a1ae, 0x307a8f, SOLID | FISH_EGG);
        register(World.SALMON_SPAWN_EGG, "Ikra łososia", 0xdd7e64, 0x307a8f, SOLID | FISH_EGG);
        register(World.TROPICAL_FISH_SPAWN_EGG, "Ikra tropikalna", 0x46beae, 0x307a8f, SOLID | FISH_EGG);
        register(World.PIKE_SPAWN_EGG, "Ikra szczupaka", 0x809d4c, 0x307a8f, SOLID | FISH_EGG);
        register(World.CARP_SPAWN_EGG, "Ikra karpia", 0xc1a052, 0x307a8f, SOLID | FISH_EGG);
        register(World.WOOL, "Wełna", 0xdcd6c6, 0xdcd6c6, SOLID);
        register(World.LADDER, "Drabina", 0x8f6c3e, 0x8f6c3e, CLIMBABLE);
        register(World.LADDER_Z_NEG, "Drabina", 0x8f6c3e, 0x8f6c3e, CLIMBABLE);
        register(World.LADDER_X_POS, "Drabina", 0x8f6c3e, 0x8f6c3e, CLIMBABLE);
        register(World.LADDER_X_NEG, "Drabina", 0x8f6c3e, 0x8f6c3e, CLIMBABLE);
    }

    private BlockRegistry() {}

    static Info info(byte type) {
        Info info = BLOCKS[type & 0xff];
        return info == null ? UNKNOWN : info;
    }

    static boolean has(byte type, int flag) { return (info(type).flags() & flag) != 0; }

    private static void register(byte id, String name, int iconColor, int shadedColor, int flags) {
        BLOCKS[id & 0xff] = new Info(name, iconColor, shadedColor, flags);
    }
}
