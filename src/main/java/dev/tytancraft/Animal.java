package dev.tytancraft;

import java.util.Random;

final class Animal {
    enum Kind {
        SHEEP("Owca", 1.0f),
        PIG("Świnia", 0.85f),
        BOAR("Dzik", 1.25f),
        DEER("Jeleń", 1.6f),
        RABBIT("Królik", 1.35f),
        FOX("Lis", 1.55f),
        CHICKEN("Kura", 0.7f),
        COD("Dorsz", 1.2f),
        SALMON("Łosoś", 1.55f),
        TROPICAL_FISH("Ryba tropikalna", 1.0f),
        PIKE("Szczupak", 1.7f),
        CARP("Karp", 1.0f);

        final String label;
        final float speed;

        Kind(String label, float speed) {
            this.label = label;
            this.speed = speed;
        }

        boolean isFish() {
            return this == COD || this == SALMON || this == TROPICAL_FISH || this == PIKE || this == CARP;
        }
    }

    static final class Part {
        final float minX, minY, minZ, maxX, maxY, maxZ;
        final int color;
        final float gaitOffset;
        final boolean animatedJaw;

        Part(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
            this(minX, minY, minZ, maxX, maxY, maxZ, color, 0, false);
        }

        Part(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color, float gaitOffset) {
            this(minX, minY, minZ, maxX, maxY, maxZ, color, gaitOffset, false);
        }

        Part(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
             int color, boolean animatedJaw) {
            this(minX, minY, minZ, maxX, maxY, maxZ, color, 0, animatedJaw);
        }

        private Part(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                     int color, float gaitOffset, boolean animatedJaw) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.color = color;
            this.gaitOffset = gaitOffset;
            this.animatedJaw = animatedJaw;
        }
    }

    final Kind kind;
    final Part[] parts;
    final int meatDrop;
    int health;
    boolean alive = true;
    float x, y, z, yaw, gait, verticalVelocity;
    private float targetX, targetZ, pause, mouthPhase;

    Animal(Kind kind, float x, float z, float groundHeight, Random random) {
        this.kind = kind;
        this.x = x;
        this.z = z;
        y = kind.isFish() ? groundHeight : groundHeight + 1.0f;
        health = kind == Kind.BOAR ? 4 : kind == Kind.DEER || kind == Kind.FOX ? 3
                : kind.isFish() || kind == Kind.RABBIT || kind == Kind.CHICKEN ? 1 : 2;
        meatDrop = kind == Kind.BOAR ? 4 : kind == Kind.DEER ? 3
                : kind == Kind.FOX || kind == Kind.SHEEP || kind == Kind.PIG ? 2 : 1;
        yaw = random.nextFloat() * (float) (Math.PI * 2);
        mouthPhase = random.nextFloat() * (float) (Math.PI * 2);
        targetX = x;
        targetZ = z;
        pause = random.nextFloat() * 2.0f;
        parts = createParts(kind);
    }

    float mouthOpening() {
        if (kind != Kind.CARP) return 0;
        return 0.02f + 0.14f * (0.5f + 0.5f * (float) Math.sin(gait * 1.1f + mouthPhase));
    }

    boolean takeHit() {
        if (!alive) return false;
        health--;
        if (health <= 0) alive = false;
        return !alive;
    }

    void update(World world, float dt, Random random, int aggression,
                float playerX, float playerZ, boolean survival) {
        if (kind.isFish()) {
            gait += dt * kind.speed * 8.0f;
            updateFish(world, dt, random);
            return;
        }
        updateVerticalPosition(world, dt);
        if (survival && aggression >= 70) {
            float dxToPlayer = playerX - x, dzToPlayer = playerZ - z;
            if (dxToPlayer * dxToPlayer + dzToPlayer * dzToPlayer < 18 * 18) {
                targetX = playerX;
                targetZ = playerZ;
                pause = 0;
            }
        } else if (survival && aggression >= 35) {
            float dxFromPlayer = x - playerX, dzFromPlayer = z - playerZ;
            float distanceSquared = dxFromPlayer * dxFromPlayer + dzFromPlayer * dzFromPlayer;
            if (distanceSquared < 9 * 9 && distanceSquared > 0.01f) {
                float inverseDistance = 1.0f / (float) Math.sqrt(distanceSquared);
                targetX = Math.max(2.5f, Math.min(World.WIDTH - 2.5f, x + dxFromPlayer * inverseDistance * 8));
                targetZ = Math.max(2.5f, Math.min(World.DEPTH - 2.5f, z + dzFromPlayer * inverseDistance * 8));
                pause = 0;
            }
        }
        if (pause > 0) {
            pause -= dt;
            return;
        }
        gait += dt * kind.speed * 8.0f;

        float dx = targetX - x, dz = targetZ - z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.45f) {
            chooseDestination(world, random);
            return;
        }

        float desiredYaw = (float) Math.atan2(dx, dz);
        float difference = wrapAngle(desiredYaw - yaw);
        yaw += Math.max(-2.2f * dt, Math.min(2.2f * dt, difference));
        float step = Math.min(distance, kind.speed * dt);
        float nextX = x + (float) Math.sin(yaw) * step;
        float nextZ = z + (float) Math.cos(yaw) * step;
        if (World.isSolid(world.get((int) nextX, (int) Math.floor(y + 0.25f), (int) nextZ))
                || World.isSolid(world.get((int) nextX, (int) Math.floor(y + 1.10f), (int) nextZ))) {
            chooseDestination(world, random);
            return;
        }
        int nextHeight = world.heightAt((int) nextX, (int) nextZ);
        int currentHeight = world.heightAt((int) x, (int) z);
        if (nextHeight <= World.WATER_LEVEL + 1 || Math.abs(nextHeight - currentHeight) > 1) {
            chooseDestination(world, random);
            return;
        }
        x = nextX;
        z = nextZ;
        if (nextHeight > currentHeight) {
            y += nextHeight - currentHeight;
            verticalVelocity = 0;
        }
    }

    private void updateVerticalPosition(World world, float dt) {
        verticalVelocity -= 20.0f * dt;
        float nextY = y + verticalVelocity * dt;
        if (verticalVelocity <= 0) {
            int below = (int) Math.floor(nextY - 0.08f);
            byte support = world.get((int) Math.floor(x), below, (int) Math.floor(z));
            if (World.isSolid(support)) {
                y = below + 1.05f;
                verticalVelocity = 0;
                return;
            }
        }
        y = Math.max(1.0f, nextY);
    }

    private void updateFish(World world, float dt, Random random) {
        if (pause > 0) {
            pause -= dt;
            y = World.WATER_LEVEL - 0.9f + (float) Math.sin(gait * 0.45f) * 0.16f;
            return;
        }
        float dx = targetX - x, dz = targetZ - z;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.45f) {
            chooseDestination(world, random);
            return;
        }
        float desiredYaw = (float) Math.atan2(dx, dz);
        float difference = wrapAngle(desiredYaw - yaw);
        yaw += Math.max(-2.8f * dt, Math.min(2.8f * dt, difference));
        float step = Math.min(distance, kind.speed * dt);
        float nextX = x + (float) Math.sin(yaw) * step;
        float nextZ = z + (float) Math.cos(yaw) * step;
        if (world.heightAt((int) nextX, (int) nextZ) > World.WATER_LEVEL - 3) {
            chooseDestination(world, random);
            return;
        }
        x = nextX;
        z = nextZ;
        y = World.WATER_LEVEL - 0.9f + (float) Math.sin(gait * 0.45f) * 0.16f;
    }

    private void chooseDestination(World world, Random random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float distance = 3.0f + random.nextFloat() * 8.0f;
            float candidateX = Math.max(2.5f, Math.min(World.WIDTH - 2.5f, x + (float) Math.sin(angle) * distance));
            float candidateZ = Math.max(2.5f, Math.min(World.DEPTH - 2.5f, z + (float) Math.cos(angle) * distance));
            int candidateHeight = world.heightAt((int) candidateX, (int) candidateZ);
            if (kind.isFish() ? candidateHeight <= World.WATER_LEVEL - 3 : candidateHeight > World.WATER_LEVEL + 1) {
                targetX = candidateX;
                targetZ = candidateZ;
                pause = 0.5f + random.nextFloat() * 2.5f;
                return;
            }
        }
        pause = 1.0f;
    }

    private static float wrapAngle(float angle) {
        while (angle > Math.PI) angle -= (float) (Math.PI * 2);
        while (angle < -Math.PI) angle += (float) (Math.PI * 2);
        return angle;
    }

    private static Part[] createParts(Kind kind) {
        if (kind == Kind.SHEEP) {
            return new Part[]{
                    new Part(-0.36f, 0.42f, -0.45f, 0.36f, 1.02f, 0.38f, 0xe6e1d4),
                    new Part(-0.21f, 0.58f, 0.34f, 0.21f, 0.96f, 0.69f, 0x927b68),
                    new Part(-0.14f, 0.57f, 0.66f, 0.14f, 0.74f, 0.82f, 0xb09a84),
                    new Part(-0.29f, 0.73f, 0.40f, -0.20f, 0.83f, 0.53f, 0xc6b6a3),
                    new Part(0.20f, 0.73f, 0.40f, 0.29f, 0.83f, 0.53f, 0xc6b6a3),
                    new Part(-0.24f, 0.0f, -0.34f, -0.12f, 0.49f, -0.20f, 0x756252, -0.7f),
                    new Part(0.12f, 0.0f, -0.34f, 0.24f, 0.49f, -0.20f, 0x756252, 0.7f),
                    new Part(-0.24f, 0.0f, 0.17f, -0.12f, 0.49f, 0.31f, 0x756252, 0.7f),
                    new Part(0.12f, 0.0f, 0.17f, 0.24f, 0.49f, 0.31f, 0x756252, -0.7f),
                    new Part(-0.13f, 0.81f, 0.685f, -0.075f, 0.865f, 0.71f, 0x302b28),
                    new Part(0.075f, 0.81f, 0.685f, 0.13f, 0.865f, 0.71f, 0x302b28)
            };
        }
        if (kind == Kind.PIG) {
            return new Part[]{
                    new Part(-0.36f, 0.36f, -0.44f, 0.36f, 0.92f, 0.38f, 0xd88986),
                    new Part(-0.23f, 0.57f, 0.34f, 0.23f, 0.96f, 0.66f, 0xe3a09a),
                    new Part(-0.17f, 0.55f, 0.64f, 0.17f, 0.76f, 0.82f, 0xc97872),
                    new Part(-0.27f, 0.9f, 0.42f, -0.12f, 1.04f, 0.53f, 0xd88986),
                    new Part(0.12f, 0.9f, 0.42f, 0.27f, 1.04f, 0.53f, 0xd88986),
                    new Part(-0.26f, 0.0f, -0.33f, -0.12f, 0.43f, -0.19f, 0xb96f69, -0.7f),
                    new Part(0.12f, 0.0f, -0.33f, 0.26f, 0.43f, -0.19f, 0xb96f69, 0.7f),
                    new Part(-0.26f, 0.0f, 0.16f, -0.12f, 0.43f, 0.30f, 0xb96f69, 0.7f),
                    new Part(0.12f, 0.0f, 0.16f, 0.26f, 0.43f, 0.30f, 0xb96f69, -0.7f),
                    new Part(-0.14f, 0.79f, 0.655f, -0.085f, 0.85f, 0.68f, 0x322b29),
                    new Part(0.085f, 0.79f, 0.655f, 0.14f, 0.85f, 0.68f, 0x322b29),
                    new Part(-0.075f, 0.60f, 0.817f, -0.02f, 0.69f, 0.83f, 0x633d3c),
                    new Part(0.02f, 0.60f, 0.817f, 0.075f, 0.69f, 0.83f, 0x633d3c)
            };
        }
        if (kind == Kind.RABBIT) {
            return new Part[]{
                    new Part(-0.20f, 0.17f, -0.30f, 0.20f, 0.55f, 0.24f, 0xb99a72),
                    new Part(-0.15f, 0.38f, 0.17f, 0.15f, 0.69f, 0.47f, 0xc5a982),
                    new Part(-0.11f, 0.57f, 0.27f, -0.035f, 1.12f, 0.35f, 0xb56565),
                    new Part(0.035f, 0.57f, 0.27f, 0.11f, 1.12f, 0.35f, 0xb56565),
                    new Part(-0.09f, 0.67f, 0.28f, -0.055f, 1.06f, 0.34f, 0xe4b9ae),
                    new Part(0.055f, 0.67f, 0.28f, 0.09f, 1.06f, 0.34f, 0xe4b9ae),
                    new Part(-0.12f, 0.55f, 0.45f, -0.07f, 0.61f, 0.48f, 0x302923),
                    new Part(0.07f, 0.55f, 0.45f, 0.12f, 0.61f, 0.48f, 0x302923),
                    new Part(-0.24f, 0.0f, -0.23f, -0.10f, 0.22f, -0.08f, 0x8d7358, -0.7f),
                    new Part(0.10f, 0.0f, -0.23f, 0.24f, 0.22f, -0.08f, 0x8d7358, 0.7f),
                    new Part(-0.21f, 0.0f, 0.08f, -0.10f, 0.22f, 0.22f, 0x8d7358, 0.7f),
                    new Part(0.10f, 0.0f, 0.08f, 0.21f, 0.22f, 0.22f, 0x8d7358, -0.7f),
                    new Part(-0.09f, 0.23f, -0.37f, 0.09f, 0.43f, -0.27f, 0xe8e0d0)
            };
        }
        if (kind == Kind.FOX) {
            return new Part[]{
                    new Part(-0.28f, 0.34f, -0.42f, 0.28f, 0.79f, 0.34f, 0xc66d37),
                    new Part(-0.20f, 0.58f, 0.27f, 0.20f, 0.94f, 0.58f, 0xd77b3d),
                    new Part(-0.12f, 0.52f, 0.55f, 0.12f, 0.70f, 0.72f, 0xe6d6b7),
                    new Part(-0.10f, 0.87f, 0.33f, -0.025f, 1.05f, 0.43f, 0xb95731),
                    new Part(0.025f, 0.87f, 0.33f, 0.10f, 1.05f, 0.43f, 0xb95731),
                    new Part(-0.20f, 0.72f, 0.56f, -0.14f, 0.78f, 0.59f, 0x211e1c),
                    new Part(0.14f, 0.72f, 0.56f, 0.20f, 0.78f, 0.59f, 0x211e1c),
                    new Part(-0.20f, 0.0f, -0.31f, -0.10f, 0.38f, -0.18f, 0x51443a, -0.7f),
                    new Part(0.10f, 0.0f, -0.31f, 0.20f, 0.38f, -0.18f, 0x51443a, 0.7f),
                    new Part(-0.20f, 0.0f, 0.13f, -0.10f, 0.38f, 0.28f, 0x51443a, 0.7f),
                    new Part(0.10f, 0.0f, 0.13f, 0.20f, 0.38f, 0.28f, 0x51443a, -0.7f),
                    new Part(-0.14f, 0.30f, -0.78f, 0.14f, 0.54f, -0.38f, 0xc66d37),
                    new Part(-0.12f, 0.31f, -0.80f, 0.12f, 0.40f, -0.68f, 0xe6d6b7)
            };
        }
        if (kind == Kind.CHICKEN) {
            return new Part[]{
                    new Part(-0.25f, 0.22f, -0.32f, 0.25f, 0.68f, 0.30f, 0xe8e5d9),
                    new Part(-0.14f, 0.55f, 0.25f, 0.14f, 0.87f, 0.49f, 0xf0ede1),
                    new Part(-0.10f, 0.52f, 0.47f, 0.10f, 0.64f, 0.65f, 0xdf9a3f),
                    new Part(-0.07f, 0.85f, 0.30f, 0.07f, 1.00f, 0.40f, 0xc94d42),
                    new Part(-0.36f, 0.31f, -0.18f, -0.23f, 0.55f, 0.20f, 0xd4d0c6),
                    new Part(0.23f, 0.31f, -0.18f, 0.36f, 0.55f, 0.20f, 0xd4d0c6),
                    new Part(-0.12f, 0.73f, 0.475f, -0.065f, 0.79f, 0.50f, 0x25221f),
                    new Part(0.065f, 0.73f, 0.475f, 0.12f, 0.79f, 0.50f, 0x25221f),
                    new Part(-0.12f, 0.0f, -0.10f, -0.04f, 0.26f, 0.04f, 0xd59c3e, -0.7f),
                    new Part(0.04f, 0.0f, -0.10f, 0.12f, 0.26f, 0.04f, 0xd59c3e, 0.7f)
            };
        }
        if (kind == Kind.BOAR) {
            return new Part[]{
                    new Part(-0.36f, 0.30f, -0.45f, 0.36f, 0.92f, 0.38f, 0x58463b),
                    new Part(-0.25f, 0.53f, 0.30f, 0.25f, 1.00f, 0.65f, 0x705545),
                    new Part(-0.17f, 0.48f, 0.61f, 0.17f, 0.73f, 0.83f, 0xa18870),
                    new Part(-0.14f, 0.40f, 0.72f, -0.08f, 0.54f, 0.79f, 0xe4d7ba),
                    new Part(0.08f, 0.40f, 0.72f, 0.14f, 0.54f, 0.79f, 0xe4d7ba),
                    new Part(-0.27f, 0.91f, 0.42f, -0.12f, 1.08f, 0.54f, 0x594336),
                    new Part(0.12f, 0.91f, 0.42f, 0.27f, 1.08f, 0.54f, 0x594336),
                    new Part(-0.18f, 0.81f, -0.16f, 0.18f, 1.04f, 0.30f, 0x44352e),
                    new Part(-0.25f, 0.72f, 0.64f, -0.19f, 0.78f, 0.68f, 0x211c19),
                    new Part(0.19f, 0.72f, 0.64f, 0.25f, 0.78f, 0.68f, 0x211c19),
                    new Part(-0.26f, 0.0f, -0.34f, -0.13f, 0.40f, -0.20f, 0x473830, -0.7f),
                    new Part(0.13f, 0.0f, -0.34f, 0.26f, 0.40f, -0.20f, 0x473830, 0.7f),
                    new Part(-0.26f, 0.0f, 0.16f, -0.13f, 0.40f, 0.30f, 0x473830, 0.7f),
                    new Part(0.13f, 0.0f, 0.16f, 0.26f, 0.40f, 0.30f, 0x473830, -0.7f)
            };
        }
        if (kind.isFish()) return createFishParts(kind);
        return new Part[]{
                new Part(-0.30f, 0.62f, -0.49f, 0.30f, 1.16f, 0.34f, 0xa9794b),
                new Part(-0.15f, 0.91f, 0.25f, 0.15f, 1.42f, 0.49f, 0x9c7046),
                new Part(-0.20f, 1.17f, 0.39f, 0.20f, 1.48f, 0.70f, 0xb98754),
                new Part(-0.14f, 1.14f, 0.67f, 0.14f, 1.29f, 0.80f, 0x594639),
                new Part(-0.27f, 1.37f, 0.45f, -0.18f, 1.48f, 0.57f, 0xa97c4d),
                new Part(0.18f, 1.37f, 0.45f, 0.27f, 1.48f, 0.57f, 0xa97c4d),
                new Part(-0.22f, 1.43f, 0.45f, -0.17f, 1.78f, 0.50f, 0x765632),
                new Part(-0.39f, 1.68f, 0.45f, -0.17f, 1.75f, 0.50f, 0x765632),
                new Part(-0.39f, 1.59f, 0.45f, -0.36f, 1.75f, 0.50f, 0x765632),
                new Part(0.17f, 1.43f, 0.45f, 0.22f, 1.78f, 0.50f, 0x765632),
                new Part(0.17f, 1.68f, 0.45f, 0.39f, 1.75f, 0.50f, 0x765632),
                new Part(0.36f, 1.59f, 0.45f, 0.39f, 1.75f, 0.50f, 0x765632),
                new Part(-0.22f, 0.0f, -0.38f, -0.12f, 0.72f, -0.23f, 0x76583d, -0.7f),
                new Part(0.12f, 0.0f, -0.38f, 0.22f, 0.72f, -0.23f, 0x76583d, 0.7f),
                new Part(-0.22f, 0.0f, 0.14f, -0.12f, 0.72f, 0.28f, 0x76583d, 0.7f),
                new Part(0.12f, 0.0f, 0.14f, 0.22f, 0.72f, 0.28f, 0x76583d, -0.7f),
                new Part(-0.11f, 1.28f, 0.685f, -0.06f, 1.34f, 0.71f, 0x332b25),
                new Part(0.06f, 1.28f, 0.685f, 0.11f, 1.34f, 0.71f, 0x332b25)
        };
    }

    private static Part[] createFishParts(Kind kind) {
        if (kind == Kind.PIKE) {
            return new Part[]{
                    new Part(-0.16f, 0.27f, -0.62f, 0.16f, 0.67f, 0.42f, 0x708845),
                    new Part(-0.13f, 0.39f, 0.36f, 0.13f, 0.61f, 0.68f, 0x85964e),
                    new Part(-0.09f, 0.42f, 0.64f, 0.09f, 0.53f, 0.86f, 0x9da45a),
                    new Part(-0.09f, 0.23f, 0.61f, 0.09f, 0.34f, 0.83f, 0x596d39),
                    new Part(-0.05f, 0.34f, 0.82f, 0.05f, 0.41f, 0.84f, 0x30271e),
                    new Part(-0.18f, 0.28f, -0.68f, 0.18f, 0.66f, -0.56f, 0x536b3b),
                    new Part(-0.045f, 0.32f, -0.85f, 0.045f, 0.62f, -0.67f, 0x536b3b),
                    new Part(-0.06f, 0.63f, -0.20f, 0.06f, 0.79f, 0.18f, 0x536b3b),
                    new Part(-0.27f, 0.34f, -0.02f, -0.14f, 0.52f, 0.18f, 0x536b3b, -0.3f),
                    new Part(0.14f, 0.34f, -0.02f, 0.27f, 0.52f, 0.18f, 0x536b3b, 0.3f),
                    new Part(-0.10f, 0.52f, 0.68f, -0.05f, 0.58f, 0.71f, 0x17201a),
                    new Part(0.05f, 0.52f, 0.68f, 0.10f, 0.58f, 0.71f, 0x17201a)
            };
        }
        if (kind == Kind.CARP) {
            return new Part[]{
                    new Part(-0.25f, 0.20f, -0.44f, 0.25f, 0.76f, 0.36f, 0xb4974e),
                    new Part(-0.18f, 0.39f, 0.30f, 0.18f, 0.68f, 0.55f, 0xc0a85a),
                    new Part(-0.13f, 0.44f, 0.52f, 0.13f, 0.57f, 0.68f, 0x967b39),
                    new Part(-0.10f, 0.26f, 0.52f, 0.10f, 0.38f, 0.66f, 0x806831, true),
                    new Part(-0.05f, 0.36f, 0.65f, 0.05f, 0.42f, 0.67f, 0x30271e),
                    new Part(-0.23f, 0.26f, -0.52f, 0.23f, 0.72f, -0.40f, 0x9c733a),
                    new Part(-0.07f, 0.73f, -0.16f, 0.07f, 0.91f, 0.19f, 0x9c733a),
                    new Part(-0.36f, 0.33f, -0.01f, -0.21f, 0.55f, 0.20f, 0x9c733a, -0.25f),
                    new Part(0.21f, 0.33f, -0.01f, 0.36f, 0.55f, 0.20f, 0x9c733a, 0.25f),
                    new Part(-0.14f, 0.57f, 0.58f, -0.09f, 0.62f, 0.70f, 0x352e24),
                    new Part(0.09f, 0.57f, 0.58f, 0.14f, 0.62f, 0.70f, 0x352e24)
            };
        }
        int body = kind == Kind.SALMON ? 0xe58360 : kind == Kind.TROPICAL_FISH ? 0x36b4b3 : 0x91aeb6;
        int fin = kind == Kind.SALMON ? 0xb74c42 : kind == Kind.TROPICAL_FISH ? 0xf0ca54 : 0xd0a344;
        int belly = kind == Kind.SALMON ? 0xf0b493 : kind == Kind.TROPICAL_FISH ? 0xe9df92 : 0xd4d7c9;
        Part[] basic = {
                new Part(-0.18f, 0.27f, -0.43f, 0.18f, 0.68f, 0.38f, body),
                new Part(-0.14f, 0.34f, 0.34f, 0.14f, 0.62f, 0.57f, body),
                new Part(-0.20f, 0.30f, -0.52f, 0.20f, 0.64f, -0.40f, fin),
                new Part(-0.035f, 0.34f, -0.75f, 0.035f, 0.62f, -0.48f, fin),
                new Part(-0.055f, 0.64f, -0.20f, 0.055f, 0.84f, 0.18f, fin),
                new Part(-0.30f, 0.35f, -0.04f, -0.16f, 0.56f, 0.18f, fin, -0.3f),
                new Part(0.16f, 0.35f, -0.04f, 0.30f, 0.56f, 0.18f, fin, 0.3f),
                new Part(-0.15f, 0.28f, -0.31f, 0.15f, 0.36f, 0.40f, belly),
                new Part(-0.15f, 0.52f, 0.54f, -0.095f, 0.59f, 0.58f, 0x22292a),
                new Part(0.095f, 0.52f, 0.54f, 0.15f, 0.59f, 0.58f, 0x22292a)
        };
        Part[] mouth = appendPart(basic, new Part(-0.12f, 0.42f, 0.52f, 0.12f, 0.53f, 0.69f, body));
        mouth = appendPart(mouth, new Part(-0.10f, 0.25f, 0.53f, 0.10f, 0.36f, 0.68f, fin));
        mouth = appendPart(mouth, new Part(-0.055f, 0.36f, 0.67f, 0.055f, 0.42f, 0.69f, 0x292522));
        if (kind == Kind.SALMON) {
            return appendPart(mouth, new Part(-0.17f, 0.43f, -0.15f, 0.17f, 0.48f, 0.23f, 0xf3bd79));
        }
        if (kind == Kind.TROPICAL_FISH) {
            return appendPart(mouth, new Part(-0.19f, 0.42f, -0.20f, 0.19f, 0.49f, 0.12f, 0xf5d65b));
        }
        return mouth;
    }

    private static Part[] appendPart(Part[] parts, Part extra) {
        Part[] result = java.util.Arrays.copyOf(parts, parts.length + 1);
        result[parts.length] = extra;
        return result;
    }
}
