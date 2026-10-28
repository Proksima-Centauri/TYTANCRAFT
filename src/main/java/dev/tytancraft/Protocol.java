package dev.tytancraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

/** Length-prefixed binary protocol shared by the dedicated server and desktop client. */
final class Protocol {
    static final int VERSION = 1;
    static final int MAX_PACKET_BYTES = 32 * 1024 * 1024;
    static final int MAX_WORLD_BYTES = 16 * 1024 * 1024;
    static final int MAX_PLAYERS = 10;

    enum Type {
        HELLO(1), WELCOME(2), REJECT(3), PLAYER_POSITION(4), PLAYER_JOINED(5), PLAYER_LEFT(6),
        BLOCK_ACTION(7), BLOCK_CHANGED(8), WATER_CHANGED(9), ANIMAL_SNAPSHOT(10),
        ANIMAL_ATTACK(11), ITEM_DROP(12), ENVIRONMENT(13), SERVER_ERROR(14), PLAYER_SNAPSHOT(15),
        FISH_SPAWN(16), PLAYER_MOVED(17);

        final int id;
        Type(int id) { this.id = id; }

        static Type fromId(int id) throws IOException {
            for (Type type : values()) if (type.id == id) return type;
            throw new IOException("Nieznany typ komunikatu: " + id);
        }
    }

    record Frame(Type type, byte[] payload) {}

    @FunctionalInterface
    interface PayloadWriter { void write(DataOutputStream output) throws IOException; }

    private Protocol() {}

    static byte[] payload(PayloadWriter writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            writer.write(output);
        }
        return bytes.toByteArray();
    }

    static DataInputStream input(byte[] payload) { return new DataInputStream(new ByteArrayInputStream(payload)); }

    static void requireConsumed(DataInputStream input) throws IOException {
        if (input.read() != -1) throw new IOException("Komunikat zawiera nadmiarowe dane.");
    }

    static void writeFrame(DataOutputStream output, Type type, byte[] payload) throws IOException {
        if (payload == null || payload.length > MAX_PACKET_BYTES) throw new IOException("Komunikat jest za duży.");
        output.writeByte(type.id);
        output.writeInt(payload.length);
        output.write(payload);
        output.flush();
    }

    static Frame readFrame(DataInputStream input) throws IOException {
        int type;
        try {
            type = input.readUnsignedByte();
        } catch (EOFException endOfStream) {
            throw endOfStream;
        }
        int length = input.readInt();
        if (length < 0 || length > MAX_PACKET_BYTES) throw new IOException("Nieprawidłowa długość komunikatu.");
        byte[] payload = new byte[length];
        input.readFully(payload);
        return new Frame(Type.fromId(type), payload);
    }

    static byte[] hello(String nickname) throws IOException {
        validateNickname(nickname);
        return payload(output -> {
            output.writeInt(VERSION);
            output.writeUTF(nickname);
        });
    }

    static String readNickname(DataInputStream input) throws IOException {
        String nickname = input.readUTF();
        validateNickname(nickname);
        return nickname;
    }

    static void validateNickname(String nickname) throws IOException {
        if (nickname == null || nickname.isBlank() || nickname.length() > 16) {
            throw new IOException("Nieprawidłowy nick.");
        }
        for (int i = 0; i < nickname.length(); i++) {
            char c = nickname.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') throw new IOException("Nieprawidłowy nick.");
        }
    }
}
