package com.ashwake.mainmenu.client.branding;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import com.mojang.blaze3d.platform.Window;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class AshwakeWindowBranding {
    private static final String PACK_NAME = "Ashwake";
    private static final String TITLE_SEPARATOR = " | ";
    private static final long TITLE_REFRESH_INTERVAL_MS = 800L;
    private static final ResourceLocation DEFAULT_ICON_16 = ResourceLocation.fromNamespaceAndPath(
            AshwakeMainMenuMod.MOD_ID,
            "icons/icon_16x16.png");
    private static final ResourceLocation DEFAULT_ICON_32 = ResourceLocation.fromNamespaceAndPath(
            AshwakeMainMenuMod.MOD_ID,
            "icons/icon_32x32.png");

    @Nullable
    private static final Method CREATE_TITLE_METHOD = resolveCreateTitleMethod();

    private static boolean iconApplied;
    private static boolean titleFailureLogged;
    private static boolean iconFailureLogged;
    private static long lastTitleAppliedAt;

    private AshwakeWindowBranding() {
    }

    public static void onClientTick(@Nullable Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTitleAppliedAt >= TITLE_REFRESH_INTERVAL_MS) {
            if (AshwakeClientConfig.windowTitleEnabled()) {
                applyWindowTitle(minecraft);
            } else {
                restoreVanillaTitle(minecraft);
            }
            lastTitleAppliedAt = now;
        }

        if (!iconApplied) {
            applyWindowIcon(minecraft);
        }
    }

    public static void onScreenChanged(@Nullable Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        if (AshwakeClientConfig.windowTitleEnabled()) {
            applyWindowTitle(minecraft);
        } else {
            restoreVanillaTitle(minecraft);
        }
    }

    public static void requestIconReapply() {
        iconApplied = false;
    }

    private static void applyWindowTitle(Minecraft minecraft) {
        if (!AshwakeClientConfig.windowTitleEnabled()) {
            return;
        }

        try {
            Window window = minecraft.getWindow();
            if (window == null) {
                return;
            }

            String baseTitle = vanillaTitle(minecraft);
            String brandedTitle = buildBrandedTitle(baseTitle);
            window.setTitle(brandedTitle);
        } catch (Throwable throwable) {
            if (!titleFailureLogged) {
                titleFailureLogged = true;
                AshwakeMainMenuMod.LOGGER.warn("Failed to apply Ashwake window title branding.", throwable);
            }
        }
    }

    private static String buildBrandedTitle(String vanillaTitle) {
        String packLabel = PACK_NAME;
        if (AshwakeClientConfig.showPackVersionInWindowTitle()) {
            String version = packVersion();
            if (!version.isBlank()) {
                packLabel = PACK_NAME + " v" + version;
            }
        }

        return switch (AshwakeClientConfig.windowTitleFormat()) {
            case ASHWAKE_FIXED -> {
                String mcVersion = SharedConstants.getCurrentVersion().getName();
                yield packLabel + TITLE_SEPARATOR + "Minecraft " + mcVersion;
            }
            case ASHWAKE_PREFIX -> packLabel + TITLE_SEPARATOR + vanillaTitle;
        };
    }

    private static String vanillaTitle(Minecraft minecraft) {
        if (CREATE_TITLE_METHOD != null) {
            try {
                Object value = CREATE_TITLE_METHOD.invoke(minecraft);
                if (value instanceof String stringValue && !stringValue.isBlank()) {
                    return stringValue;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        String mcVersion = SharedConstants.getCurrentVersion().getName();
        return "Minecraft " + mcVersion;
    }

    private static String packVersion() {
        return ModList.get()
                .getModContainerById(AshwakeMainMenuMod.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("")
                .trim();
    }

    private static void restoreVanillaTitle(Minecraft minecraft) {
        try {
            minecraft.updateTitle();
        } catch (Throwable throwable) {
            if (!titleFailureLogged) {
                titleFailureLogged = true;
                AshwakeMainMenuMod.LOGGER.warn("Failed restoring vanilla window title.", throwable);
            }
        }
    }

    private static void applyWindowIcon(Minecraft minecraft) {
        iconApplied = true;
        if (!AshwakeClientConfig.windowIconEnabled()) {
            return;
        }

        long handle = minecraft.getWindow().getWindow();
        if (handle == 0L) {
            iconApplied = false;
            return;
        }

        ResourceLocation icon16Path = parseResourceLocation(AshwakeClientConfig.windowIconPath16(), DEFAULT_ICON_16);
        ResourceLocation icon32Path = parseResourceLocation(AshwakeClientConfig.windowIconPath32(), DEFAULT_ICON_32);

        LoadedIcon icon16 = null;
        LoadedIcon icon32 = null;
        try {
            icon16 = loadIcon(minecraft.getResourceManager(), icon16Path);
            icon32 = loadIcon(minecraft.getResourceManager(), icon32Path);
            if (icon16 == null && icon32 == null) {
                return;
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                int count = (icon16 != null ? 1 : 0) + (icon32 != null ? 1 : 0);
                GLFWImage.Buffer images = GLFWImage.malloc(count, stack);
                int index = 0;
                if (icon16 != null) {
                    images.position(index++);
                    images.width(icon16.width);
                    images.height(icon16.height);
                    images.pixels(icon16.pixels);
                }
                if (icon32 != null) {
                    images.position(index);
                    images.width(icon32.width);
                    images.height(icon32.height);
                    images.pixels(icon32.pixels);
                }
                images.position(0);
                GLFW.glfwSetWindowIcon(handle, images);
            }
        } catch (Throwable throwable) {
            if (!iconFailureLogged) {
                iconFailureLogged = true;
                AshwakeMainMenuMod.LOGGER.warn("Failed to apply Ashwake window icon branding.", throwable);
            }
        } finally {
            closeQuietly(icon16);
            closeQuietly(icon32);
        }
    }

    @Nullable
    private static LoadedIcon loadIcon(ResourceManager resourceManager, ResourceLocation location) {
        Optional<Resource> resource = resourceManager.getResource(location);
        if (resource.isEmpty()) {
            return null;
        }

        try (InputStream inputStream = resource.get().open()) {
            byte[] pngBytes = inputStream.readAllBytes();
            ByteBuffer source = MemoryUtil.memAlloc(pngBytes.length);
            source.put(pngBytes);
            source.flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(source, width, height, channels, 4);
                if (pixels == null) {
                    throw new IOException("STB decode failed for " + location + ": " + STBImage.stbi_failure_reason());
                }
                return new LoadedIcon(width.get(0), height.get(0), pixels);
            } finally {
                MemoryUtil.memFree(source);
            }
        } catch (IOException exception) {
            if (!iconFailureLogged) {
                iconFailureLogged = true;
                AshwakeMainMenuMod.LOGGER.warn("Failed reading window icon resource {}.", location, exception);
            }
            return null;
        }
    }

    private static void closeQuietly(@Nullable LoadedIcon icon) {
        if (icon != null) {
            icon.close();
        }
    }

    private static ResourceLocation parseResourceLocation(String configured, ResourceLocation fallback) {
        if (configured == null || configured.isBlank()) {
            return fallback;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(configured.trim());
        return parsed != null ? parsed : fallback;
    }

    @Nullable
    private static Method resolveCreateTitleMethod() {
        try {
            Method method = Minecraft.class.getDeclaredMethod("createTitle");
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static final class LoadedIcon implements AutoCloseable {
        private final int width;
        private final int height;
        private final ByteBuffer pixels;

        private LoadedIcon(int width, int height, ByteBuffer pixels) {
            this.width = width;
            this.height = height;
            this.pixels = pixels;
        }

        @Override
        public void close() {
            STBImage.stbi_image_free(this.pixels);
        }
    }
}
