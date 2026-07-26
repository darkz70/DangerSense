package com.darkz.dangersense.detector;

import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.danger.DangerLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Warns when the player's health drops below the configured threshold.
 * Intensity scales with how far below the threshold the player is.
 */
public final class LowHealthDetector implements DangerDetector {

    private DangerLevel level = DangerLevel.NONE;
    private float intensity = 0.0f;

    @Override
    public String getId() {
        return "low_health";
    }

    @Override
    public boolean isEnabled(DangerSenseConfig config) {
        return config.lowHealthAlertEnabled;
    }

    @Override
    public boolean shouldWarn(LocalPlayer player, ClientLevel world, DangerSenseConfig config) {
        float health = player.getHealth();
        float threshold = (float) config.healthThreshold;

        if (health <= 0f || health >= threshold) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        float ratio = 1.0f - Math.max(0f, health / threshold); // 0 at threshold, 1 at 0 HP
        level = ratio > 0.7f ? DangerLevel.CRITICAL
                : ratio > 0.4f ? DangerLevel.HIGH
                : DangerLevel.MEDIUM;
        intensity = Math.min(1.0f, level.baseIntensity() + ratio * 0.3f);
        return true;
    }

    @Override
    public DangerLevel getLevel() {
        return level;
    }

    @Override
    public float getIntensity() {
        return intensity;
    }
}
