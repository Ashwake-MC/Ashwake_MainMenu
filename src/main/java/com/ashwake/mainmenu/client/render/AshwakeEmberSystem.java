package com.ashwake.mainmenu.client.render;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class AshwakeEmberSystem {
    private static final int MAX_PARTICLES = 450;
    private static final ResourceLocation[] EMBER_TEXTURES = {
            AshwakeUiSkin.PARTICLE_EMBER_0,
            AshwakeUiSkin.PARTICLE_EMBER_1,
            AshwakeUiSkin.PARTICLE_EMBER_2
    };

    private final boolean[] active = new boolean[MAX_PARTICLES];
    private final float[] x = new float[MAX_PARTICLES];
    private final float[] y = new float[MAX_PARTICLES];
    private final float[] vx = new float[MAX_PARTICLES];
    private final float[] vy = new float[MAX_PARTICLES];
    private final float[] life = new float[MAX_PARTICLES];
    private final float[] maxLife = new float[MAX_PARTICLES];
    private final int[] variant = new int[MAX_PARTICLES];

    private final RandomSource random = RandomSource.create();

    private int nextIndex;
    private float spawnAccumulator;
    private long lastFrameNanos = Util.getNanos();

    public void reset() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            active[i] = false;
        }
        nextIndex = 0;
        spawnAccumulator = 0F;
        lastFrameNanos = Util.getNanos();
    }

    public void updateAndRender(
            GuiGraphics graphics,
            int width,
            int height,
            boolean enabled,
            int density,
            boolean reducedMotion) {
        long now = Util.getNanos();
        float dt = Mth.clamp((now - lastFrameNanos) / 1_000_000_000F, 0.0F, 0.1F);
        lastFrameNanos = now;

        if (!enabled || density <= 0) {
            return;
        }

        int targetCount = Mth.clamp((width * height) / 5200, 18, MAX_PARTICLES);
        targetCount = Mth.clamp((targetCount * density) / 100, 8, MAX_PARTICLES);
        float spawnRate = targetCount * (reducedMotion ? 0.45F : 0.85F);
        spawnAccumulator += spawnRate * dt;

        while (spawnAccumulator >= 1F) {
            spawnAccumulator -= 1F;
            spawnParticle(width, height, reducedMotion);
        }

        int aliveCount = 0;
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (!active[i]) {
                continue;
            }
            aliveCount++;
            life[i] += dt;
            if (life[i] >= maxLife[i]) {
                active[i] = false;
                continue;
            }

            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            vx[i] += (random.nextFloat() - 0.5F) * (reducedMotion ? 1.5F : 3.0F) * dt;

            if (x[i] < -4 || x[i] > width + 4 || y[i] < -6 || y[i] > height + 6) {
                active[i] = false;
                continue;
            }

            float lifeRatio = 1F - (life[i] / maxLife[i]);
            int alpha = Mth.clamp((int) (lifeRatio * 210F), 28, 210);
            int ix = Mth.floor(x[i]);
            int iy = Mth.floor(y[i]);
            if (!drawEmberTexture(graphics, ix, iy, lifeRatio, variant[i])) {
                int color = (alpha << 24) | (AshwakePalette.EMBER_ORANGE & 0x00FFFFFF);
                graphics.fill(ix, iy, ix + 2, iy + 2, color);
            }
        }

        // If all particles are gone, avoid stale timestamps causing burst spawns.
        if (aliveCount == 0) {
            lastFrameNanos = Util.getNanos();
        }
    }

    private void spawnParticle(int width, int height, boolean reducedMotion) {
        int index = nextIndex++;
        if (nextIndex >= MAX_PARTICLES) {
            nextIndex = 0;
        }

        active[index] = true;
        x[index] = random.nextFloat() * width;
        y[index] = height + random.nextFloat() * 12F;
        vx[index] = (random.nextFloat() - 0.5F) * (reducedMotion ? 8F : 20F);
        vy[index] = -(18F + random.nextFloat() * (reducedMotion ? 14F : 36F));
        life[index] = 0F;
        maxLife[index] = 1.5F + random.nextFloat() * (reducedMotion ? 1.0F : 2.0F);
        variant[index] = random.nextInt(EMBER_TEXTURES.length);
    }

    private static boolean drawEmberTexture(
            GuiGraphics guiGraphics,
            int x,
            int y,
            float lifeRatio,
            int variantIndex) {
        ResourceLocation texture = EMBER_TEXTURES[Math.floorMod(variantIndex, EMBER_TEXTURES.length)];
        if (!AshwakeUiSkin.hasTexture(texture)) {
            return false;
        }

        int size = lifeRatio > 0.66F ? 4 : (lifeRatio > 0.33F ? 3 : 2);
        int drawX = x - (size / 2);
        int drawY = y - (size / 2);
        guiGraphics.blit(texture, drawX, drawY, 0, 0, size, size, 16, 16);
        return true;
    }
}
