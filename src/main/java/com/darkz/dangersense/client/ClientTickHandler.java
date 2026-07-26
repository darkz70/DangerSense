package com.darkz.dangersense.client;

import com.darkz.dangersense.config.ConfigManager;
import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.danger.DangerWarning;
import com.darkz.dangersense.danger.WarningManager;
import com.darkz.dangersense.render.VignetteOverlay;
import com.darkz.dangersense.sound.HeartbeatPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Glue class run once per client tick. Kept deliberately thin: all actual
 * detection lives in {@link WarningManager}, all drawing in
 * {@link VignetteOverlay}, all audio in {@link HeartbeatPlayer}.
 */
public final class ClientTickHandler {

    private final WarningManager warningManager = new WarningManager();
    private final VignetteOverlay vignetteOverlay = new VignetteOverlay();
    private final HeartbeatPlayer heartbeatPlayer = new HeartbeatPlayer();

    /** Hot-reload check is cheap but still only done a few times a second, not every tick. */
    private int hotReloadCounter = 0;

    public void onEndTick(Minecraft client) {
        LocalPlayer player = client.player;
        ClientLevel world = client.level; // Mojmap field name is "level", not "world"
        if (player == null || world == null) {
            return;
        }

        if (++hotReloadCounter >= 20) { // ~once per second at 20 TPS
            hotReloadCounter = 0;
            ConfigManager.checkHotReload();
        }

        DangerSenseConfig config = ConfigManager.get();
        DangerWarning warning = warningManager.tick(player, world);

        // screenIntensity only scales the visual vignette; the heartbeat
        // volume is scaled separately by heartbeatVolume inside HeartbeatPlayer.
        float visualIntensity = (float) Math.max(0.0, Math.min(1.0, warning.intensity() * config.screenIntensity));
        vignetteOverlay.setWarning(new DangerWarning(warning.sourceId(), warning.level(), visualIntensity));

        heartbeatPlayer.tick(client, player, warning, config.heartbeatInterval, config.heartbeatVolume);
    }

    public VignetteOverlay getVignetteOverlay() {
        return vignetteOverlay;
    }
}
