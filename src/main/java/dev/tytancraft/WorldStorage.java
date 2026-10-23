package dev.tytancraft;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class WorldStorage {
    private static final int MAGIC = 0x54594352;
    private static final int VERSION = 2;
    private static final int MAX_ANIMALS = 128;
    private static final Path SAVE_DIRECTORY = Path.of(
            System.getProperty("user.home"), ".tytancraft", "worlds");

    private WorldStorage() {}

    static final class SaveInfo {
        final Path path;
        final long seed;
        final FileTime modified;

        SaveInfo(Path path, long seed, FileTime modified) {
            this.path = path;
            this.seed = seed;
            this.modified = modified;
        }
    }

    static final class AnimalState {
        Animal.Kind kind;
        boolean alive;
        int health;
        float x, y, z, yaw, gait, verticalVelocity, growth, breedingCooldown;
    }

    static final class Data {
        long seed;
        int spawnX, spawnZ;
        boolean creativeMode;
        int aggressionLevel, renderDistance, playerHealth, meatCount, selectedBlock, weather;
        float daySpeed, playerX, playerY, playerZ, yaw, pitch, hunger, oxygen, dayTime;
        float weatherTimer, cloudCoverage, targetCloudCoverage, rainIntensity, targetRainIntensity;
        float cloudOffsetX, cloudOffsetY, rainPhase;
        String nickname;
        int[] inventory;
        byte[] hotbar;
        byte[] blocks;
        short[] waterFlow;
        long[] waterQueue;
        List<AnimalState> animals;
    }

    static List<SaveInfo> list() throws IOException {
        if (!Files.isDirectory(SAVE_DIRECTORY)) return List.of();
        List<SaveInfo> saves = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(SAVE_DIRECTORY, "*.tcw")) {
            for (Path path : files) {
                String fileName = path.getFileName().toString();
                String seedText = fileName.substring(0, fileName.length() - 4);
                try {
                    long seed = Long.parseUnsignedLong(seedText);
                    saves.add(new SaveInfo(path, seed, Files.getLastModifiedTime(path)));
                } catch (NumberFormatException ignored) {
                    // Ignore unrelated or malformed files in the save directory.
                }
            }
        }
        saves.sort(Comparator.comparing((SaveInfo save) -> save.modified).reversed());
        return List.copyOf(saves);
    }

    static void save(Data data) throws IOException {
        validate(data);
        Files.createDirectories(SAVE_DIRECTORY);
        Path destination = SAVE_DIRECTORY.resolve(Long.toUnsignedString(data.seed) + ".tcw");
        Path temporary = SAVE_DIRECTORY.resolve(Long.toUnsignedString(data.seed) + ".tmp");
        try {
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                    new GZIPOutputStream(Files.newOutputStream(temporary))))) {
                write(output, data);
            }
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

    static Data load(Path path) throws IOException {
        Data data;
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(Files.newInputStream(path))))) {
            data = read(input);
        }
        validate(data);
        return data;
    }

    private static void write(DataOutputStream output, Data data) throws IOException {
        output.writeInt(MAGIC);
        output.writeInt(VERSION);
        output.writeLong(data.seed);
        output.writeInt(World.WIDTH);
        output.writeInt(World.HEIGHT);
        output.writeInt(World.DEPTH);
        output.write(data.blocks);
        for (short flow : data.waterFlow) output.writeShort(flow);
        output.writeInt(data.waterQueue.length);
        for (long entry : data.waterQueue) output.writeLong(entry);

        output.writeInt(data.spawnX);
        output.writeInt(data.spawnZ);
        output.writeBoolean(data.creativeMode);
        output.writeInt(data.aggressionLevel);
        output.writeInt(data.renderDistance);
        output.writeInt(data.playerHealth);
        output.writeInt(data.meatCount);
        output.writeInt(data.selectedBlock);
        output.writeInt(data.weather);
        output.writeFloat(data.daySpeed);
        output.writeFloat(data.playerX);
        output.writeFloat(data.playerY);
        output.writeFloat(data.playerZ);
        output.writeFloat(data.yaw);
        output.writeFloat(data.pitch);
        output.writeFloat(data.hunger);
        output.writeFloat(data.oxygen);
        output.writeFloat(data.dayTime);
        output.writeFloat(data.weatherTimer);
        output.writeFloat(data.cloudCoverage);
        output.writeFloat(data.targetCloudCoverage);
        output.writeFloat(data.rainIntensity);
        output.writeFloat(data.targetRainIntensity);
        output.writeFloat(data.cloudOffsetX);
        output.writeFloat(data.cloudOffsetY);
        output.writeFloat(data.rainPhase);
        output.writeUTF(data.nickname);
        output.writeInt(data.inventory.length);
        for (int item : data.inventory) output.writeInt(item);
        output.writeInt(data.hotbar.length);
        output.write(data.hotbar);
        output.writeInt(data.animals.size());
        for (AnimalState animal : data.animals) {
            output.writeInt(animal.kind.ordinal());
            output.writeBoolean(animal.alive);
            output.writeInt(animal.health);
            output.writeFloat(animal.x);
            output.writeFloat(animal.y);
            output.writeFloat(animal.z);
            output.writeFloat(animal.yaw);
            output.writeFloat(animal.gait);
            output.writeFloat(animal.verticalVelocity);
            output.writeFloat(animal.growth);
            output.writeFloat(animal.breedingCooldown);
        }
    }

    private static Data read(DataInputStream input) throws IOException {
        if (input.readInt() != MAGIC) throw new IOException("To nie jest zapis TytanCraft.");
        int version = input.readInt();
        if (version < 1 || version > VERSION) throw new IOException("Nieobsługiwana wersja zapisu.");
        Data data = new Data();
        data.seed = input.readLong();
        if (input.readInt() != World.WIDTH || input.readInt() != World.HEIGHT
                || input.readInt() != World.DEPTH) throw new IOException("Zapis ma inny rozmiar świata.");
        data.blocks = new byte[World.WIDTH * World.HEIGHT * World.DEPTH];
        input.readFully(data.blocks);
        data.waterFlow = new short[data.blocks.length];
        for (int i = 0; i < data.waterFlow.length; i++) data.waterFlow[i] = input.readShort();
        if (version >= 2) {
            int queuedWater = input.readInt();
            if (queuedWater < 0 || queuedWater > data.blocks.length) throw new IOException("Nieprawidłowa kolejka wody.");
            data.waterQueue = new long[queuedWater];
            for (int i = 0; i < queuedWater; i++) data.waterQueue[i] = input.readLong();
        } else data.waterQueue = new long[0];

        data.spawnX = input.readInt();
        data.spawnZ = input.readInt();
        data.creativeMode = input.readBoolean();
        data.aggressionLevel = input.readInt();
        data.renderDistance = input.readInt();
        data.playerHealth = input.readInt();
        data.meatCount = input.readInt();
        data.selectedBlock = input.readInt();
        data.weather = input.readInt();
        data.daySpeed = input.readFloat();
        data.playerX = input.readFloat();
        data.playerY = input.readFloat();
        data.playerZ = input.readFloat();
        data.yaw = input.readFloat();
        data.pitch = input.readFloat();
        data.hunger = input.readFloat();
        data.oxygen = input.readFloat();
        data.dayTime = input.readFloat();
        data.weatherTimer = input.readFloat();
        data.cloudCoverage = input.readFloat();
        data.targetCloudCoverage = input.readFloat();
        data.rainIntensity = input.readFloat();
        data.targetRainIntensity = input.readFloat();
        data.cloudOffsetX = input.readFloat();
        data.cloudOffsetY = input.readFloat();
        data.rainPhase = input.readFloat();
        data.nickname = input.readUTF();
        int inventorySize = input.readInt();
        if (inventorySize != 48) throw new IOException("Nieprawidłowy ekwipunek w zapisie.");
        data.inventory = new int[inventorySize];
        for (int i = 0; i < inventorySize; i++) data.inventory[i] = input.readInt();
        int hotbarSize = input.readInt();
        if (hotbarSize != 10) throw new IOException("Nieprawidłowy pasek szybkiego dostępu w zapisie.");
        data.hotbar = new byte[hotbarSize];
        input.readFully(data.hotbar);
        int animalCount = input.readInt();
        if (animalCount < 0 || animalCount > MAX_ANIMALS) throw new IOException("Nieprawidłowa liczba zwierząt.");
        data.animals = new ArrayList<>(animalCount);
        Animal.Kind[] kinds = Animal.Kind.values();
        for (int i = 0; i < animalCount; i++) {
            int kind = input.readInt();
            if (kind < 0 || kind >= kinds.length) throw new IOException("Nieznany gatunek zwierzęcia w zapisie.");
            AnimalState animal = new AnimalState();
            animal.kind = kinds[kind];
            animal.alive = input.readBoolean();
            animal.health = input.readInt();
            animal.x = input.readFloat();
            animal.y = input.readFloat();
            animal.z = input.readFloat();
            animal.yaw = input.readFloat();
            animal.gait = input.readFloat();
            animal.verticalVelocity = input.readFloat();
            animal.growth = input.readFloat();
            animal.breedingCooldown = input.readFloat();
            data.animals.add(animal);
        }
        return data;
    }

    private static void validate(Data data) throws IOException {
        if (data.blocks == null || data.blocks.length != World.WIDTH * World.HEIGHT * World.DEPTH
                || data.waterFlow == null || data.waterFlow.length != data.blocks.length
                || data.waterQueue == null || data.waterQueue.length > data.blocks.length) {
            throw new IOException("Nieprawidłowe dane bloków świata.");
        }
        for (long entry : data.waterQueue) {
            int cell = (int) entry, level = (int) (entry >>> 32);
            if (cell < 0 || cell >= data.blocks.length || level < 0 || level > World.WIDTH + World.DEPTH) {
                throw new IOException("Nieprawidłowa kolejka wody.");
            }
        }
        if (data.inventory == null || data.inventory.length != 48 || data.hotbar == null || data.hotbar.length != 10
                || data.animals == null || data.animals.size() > MAX_ANIMALS) {
            throw new IOException("Nieprawidłowe dane gracza lub zwierząt.");
        }
        if (data.nickname == null || data.nickname.length() > 16 || data.weather < 0 || data.weather > 2
                || data.spawnX < 0 || data.spawnX >= World.WIDTH || data.spawnZ < 0 || data.spawnZ >= World.DEPTH
                || data.selectedBlock < 0 || data.selectedBlock >= data.hotbar.length) {
            throw new IOException("Nieprawidłowe ustawienia zapisanej gry.");
        }
        for (int count : data.inventory) if (count < 0 || count > 999) throw new IOException("Nieprawidłowy ekwipunek.");
        for (byte type : data.hotbar) {
            if ((type & 0xff) >= data.inventory.length) throw new IOException("Nieznany blok na pasku szybkiego dostępu.");
        }
        if (!finite(data.daySpeed, data.playerX, data.playerY, data.playerZ, data.yaw, data.pitch,
                data.hunger, data.oxygen, data.dayTime, data.weatherTimer, data.cloudCoverage,
                data.targetCloudCoverage, data.rainIntensity, data.targetRainIntensity,
                data.cloudOffsetX, data.cloudOffsetY, data.rainPhase)) {
            throw new IOException("Zapis zawiera nieprawidłowe wartości liczbowe.");
        }
        for (AnimalState animal : data.animals) {
            if (animal.kind == null || !finite(animal.x, animal.y, animal.z, animal.yaw, animal.gait,
                    animal.verticalVelocity, animal.growth, animal.breedingCooldown)) {
                throw new IOException("Nieprawidłowe dane zwierzęcia.");
            }
        }
    }

    private static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }
}
