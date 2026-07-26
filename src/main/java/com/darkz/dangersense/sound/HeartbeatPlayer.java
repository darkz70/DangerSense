package com.darkz.dangersense.sound;

import com.darkz.dangersense.DangerSenseMod;
import com.darkz.dangersense.danger.DangerWarning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Plays the heartbeat sound at a rate that scales with danger intensity:
 * higher intensity = shorter interval between beats (down to a configurable
 * floor). Never plays more than once per interval, so this cannot spam
 * even if called every tick.
 */
public final class HeartbeatPlayer {

    private int ticksUntilNextBeat = 0;

    public void tick(Minecraft client, LocalPlayer player, DangerWarning warning, int baseIntervalTicks, double volumeMultiplier) {
        if (!warning.isActive()) {
            ticksUntilNextBeat = 0;
            return;
        }

        if (ticksUntilNextBeat > 0) {
            ticksUntilNextBeat--;
            return;
        }

        // Intensity 0 -> baseInterval, intensity 1 -> baseInterval / 3 (min 4 ticks).
        int interval = Math.max(4, (int) (baseIntervalTicks / (1.0f + warning.intensity() * 2.0f)));
        ticksUntilNextBeat = interval;

        float volume = (float) Math.max(0.0, Math.min(1.0, volumeMultiplier)) * (0.5f + warning.intensity() * 0.5f);
        if (volume <= 0.001f) return;

        // Client-side-only playback anchored to the player, so it is
        // positional for other purposes but always audible to the local user.
        // ⚠ MOJMAP: Entity#level() is my best-confidence guess for Yarn's
        // getWorld() — verify against a build log.
        player.level().playSound(
                player,
                player.blockPosition(),
                DangerSenseMod.HEARTBEAT_SOUND,
                SoundSource.MASTER,
                volume,
                1.0f
        );
    }

    public void reset() {
        ticksUntilNextBeat = 0;
    }
}
