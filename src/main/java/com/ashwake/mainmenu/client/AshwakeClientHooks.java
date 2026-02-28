package com.ashwake.mainmenu.client;

import com.ashwake.mainmenu.AshwakeMainMenuMod;
import com.ashwake.mainmenu.client.branding.AshwakeWindowBranding;
import com.ashwake.mainmenu.client.compat.BlurCompat;
import com.ashwake.mainmenu.client.overlay.AshwakeLoadingOverlay;
import com.ashwake.mainmenu.client.screen.AshwakeCreateWorldScreen;
import com.ashwake.mainmenu.client.screen.AshwakeMainMenuScreen;
import com.ashwake.mainmenu.client.screen.AshwakeMultiplayerScreen;
import com.ashwake.mainmenu.client.screen.AshwakeSelectWorldScreen;
import com.ashwake.mainmenu.client.screen.AshwakeScreenBase;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import com.ashwake.mainmenu.internal.AshwakeMenuApiImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = AshwakeMainMenuMod.MOD_ID, value = Dist.CLIENT)
public final class AshwakeClientHooks {
    private AshwakeClientHooks() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen incoming = event.getNewScreen();
        if (incoming == null) {
            return;
        }

        if (incoming instanceof TitleScreen && AshwakeClientConfig.enableMenuReplacement()) {
            event.setNewScreen(new AshwakeMainMenuScreen());
            BlurCompat.onAshwakeScreenOpen();
            AshwakeWindowBranding.onScreenChanged(Minecraft.getInstance());
            return;
        }

        if (incoming instanceof CreateWorldScreen createWorldScreen) {
            event.setNewScreen(new AshwakeCreateWorldScreen(createWorldScreen));
            BlurCompat.onAshwakeScreenOpen();
            AshwakeWindowBranding.onScreenChanged(Minecraft.getInstance());
            return;
        }

        if (incoming instanceof AshwakeScreenBase
                || incoming instanceof AshwakeMultiplayerScreen
                || incoming instanceof AshwakeSelectWorldScreen) {
            BlurCompat.onAshwakeScreenOpen();
        }

        AshwakeWindowBranding.onScreenChanged(Minecraft.getInstance());
        AshwakeMenuApiImpl.getInstance().updateMenuState(incoming.getClass().getName());
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof AshwakeScreenBase
                || event.getScreen() instanceof AshwakeMultiplayerScreen
                || event.getScreen() instanceof AshwakeSelectWorldScreen) {
            BlurCompat.onAshwakeScreenClose();
        }
        AshwakeMenuApiImpl.getInstance().updateMenuState("");
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        AshwakeWindowBranding.onClientTick(minecraft);

        Overlay overlay = minecraft.getOverlay();
        if (!(overlay instanceof LoadingOverlay) || overlay instanceof AshwakeLoadingOverlay) {
            return;
        }

        Overlay wrapped = AshwakeLoadingOverlay.wrapVanilla(overlay);
        if (wrapped != overlay) {
            minecraft.setOverlay(wrapped);
        }
    }
}
