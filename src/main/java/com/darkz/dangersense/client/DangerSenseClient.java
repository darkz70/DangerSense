package com.darkz.dangersense.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * Client-only entrypoint. Registered separately from {@code DangerSenseMod}
 * so a dedicated server never loads any of this rendering/sound code.
 */
public final class DangerSenseClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickHandler handler = new ClientTickHandler();

        ClientTickEvents.END_CLIENT_TICK.register(handler::onEndTick);
        HudRenderCallback.EVENT.register(handler.getVignetteOverlay());
    }
}
