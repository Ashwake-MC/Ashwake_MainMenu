package com.ashwake.mainmenu;

import com.ashwake.mainmenu.client.branding.AshwakeWindowBranding;
import com.ashwake.mainmenu.client.screen.AshwakeSettingsScreen;
import com.ashwake.mainmenu.client.compat.BlurCompat;
import com.ashwake.mainmenu.config.AshwakeClientConfig;
import com.ashwake.mainmenu.internal.AshwakeMenuApiImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = AshwakeMainMenuMod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AshwakeMainMenuMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AshwakeMainMenuClient {
    public AshwakeMainMenuClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new AshwakeSettingsScreen(parent));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        BlurCompat.initialize();
        event.enqueueWork(AshwakeWindowBranding::requestIconReapply);
        AshwakeMenuApiImpl.getInstance().invalidateChangelogCache("session start");
        AshwakeMainMenuMod.LOGGER.info("Ashwake main menu client setup complete.");
    }

    @SubscribeEvent
    static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager ->
                AshwakeMenuApiImpl.getInstance().invalidateChangelogCache("resource reload"));
    }

    @SubscribeEvent
    static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == AshwakeClientConfig.SPEC) {
            AshwakeWindowBranding.requestIconReapply();
            AshwakeWindowBranding.onScreenChanged(Minecraft.getInstance());
            AshwakeMenuApiImpl.getInstance().invalidateChangelogCache("config reload");
        }
    }
}
