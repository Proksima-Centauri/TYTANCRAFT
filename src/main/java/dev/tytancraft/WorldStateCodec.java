package dev.tytancraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Shared compressed world format for network snapshots and world saves. */
final class WorldStateCodec {
    private static final int MAGIC = 0x54594352;
    private static final int VERSION = 2;
    private static final int MAX_QUEUE_ENTRIES = World.WIDTH * World.HEIGHT * World.DEPTH;

    record Snapshot(long seed, byte[] blocks, short[] waterFlow, long[] waterQueue) {}

    private WorldStateCodec() {}

    static byte[] encode(World world) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(bytes))) {
            write(output, new Snapshot(world.seed(), world.copyBlocks(), world.copyWaterFlow(),
                    world.copyPendingWaterFlow()));
        }
        return bytes.toByteArray();
    }

    static World createWorld(Snapshot snapshot) {
        return new World(snapshot.seed(), snapshot.blocks(), snapshot.waterFlow(), snapshot.waterQueue());
    }

    static Snapshot decode(byte[] compressed) throws IOException {
        if (compressed == null || compressed.length == 0 || compressed.length > 32 * 1024 * 1024) {
            throw new IOException("Nieprawidłowy rozmiar migawki świata.");
        }
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(compressed)))) {
            Snapshot snapshot = read(input);
            if (input.read() != -1) throw new IOException("Nadmiarowe dane w migawce świata.");
            return snapshot;
        }
    }

    static void write(DataOutputStream output, Snapshot snapshot) throws IOException {
        validate(snapshot);
        output.writeInt(MAGIC);
        output.writeInt(VERSION);
        output.writeLong(snapshot.seed());
        output.writeInt(World.WIDTH);
        output.writeInt(World.HEIGHT);
        output.writeInt(World.DEPTH);
        output.write(snapshot.blocks());
        for (short flow : snapshot.waterFlow()) output.writeShort(flow);
        output.writeInt(snapshot.waterQueue().length);
        for (long entry : snapshot.waterQueue()) output.writeLong(entry);
    }

    static Snapshot read(DataInputStream input) throws IOException {
        if (input.readInt() != MAGIC) throw new IOException("To nie jest zapis świata TytanCraft.");
        int version = input.readInt();
        if (version < 1 || version > VERSION) throw new IOException("Nieobsługiwana wersja świata.");
        long seed = input.readLong();
        if (input.readInt() != World.WIDTH || input.readInt() != World.HEIGHT
                || input.readInt() != World.DEPTH) throw new IOException("Zapis ma inny rozmiar świata.");
        int volume = World.WIDTH * World.HEIGHT * World.DEPTH;
        byte[] blocks = new byte[volume];
        input.readFully(blocks);
        short[] waterFlow = new short[volume];
        for (int i = 0; i < waterFlow.length; i++) waterFlow[i] = input.readShort();
        long[] waterQueue;
        if (version >= 2) {
            int length = input.readInt();
            if (length < 0 || length > MAX_QUEUE_ENTRIES) throw new IOException("Nieprawidłowa kolejka wody.");
            waterQueue = new long[length];
            for (int i = 0; i < length; i++) waterQueue[i] = input.readLong();
        } else waterQueue = new long[0];
        Snapshot snapshot = new Snapshot(seed, blocks, waterFlow, waterQueue);
        validate(snapshot);
        return snapshot;
    }

    private static void validate(Snapshot snapshot) throws IOException {
        int volume = World.WIDTH * World.HEIGHT * World.DEPTH;
        if (snapshot.blocks() == null || snapshot.blocks().length != volume
                || snapshot.waterFlow() == null || snapshot.waterFlow().length != volume
                || snapshot.waterQueue() == null || snapshot.waterQueue().length > MAX_QUEUE_ENTRIES) {
            throw new IOException("Nieprawidłowe dane świata.");
        }
        for (long entry : snapshot.waterQueue()) {
            int cell = (int) entry, level = (int) (entry >>> 32);
            if (cell < 0 || cell >= volume || level < 0 || level > World.WIDTH + World.DEPTH) {
                throw new IOException("Nieprawidłowa kolejka wody.");
            }
        }
    }
}
