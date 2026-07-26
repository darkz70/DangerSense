package com.darkz.dangersense.detector;

import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.danger.DangerLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Warns when remaining air runs low while submerged.
 */
public final class DrowningDetector implements DangerDetector {

    private DangerLevel level = DangerLevel.NONE;
    private float intensity = 0.0f;

    @Override
    public String getId() {
        return "drowning";
    }

    @Override
    public boolean isEnabled(DangerSenseConfig config) {
        return config.drowningAlertEnabled;
    }

    @Override
    public boolean shouldWarn(LocalPlayer player, ClientLevel world, DangerSenseConfig config) {
        // Mojmap: getAirSupply() is the equivalent of Yarn's getAir() — high confidence.
        int air = player.getAirSupply();
        int threshold = config.airThreshold;

        if (air > threshold) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        float ratio = 1.0f - Math.max(0f, air / (float) Math.max(1, threshold));
        level = air <= 0 ? DangerLevel.CRITICAL
                : ratio > 0.6f ? DangerLevel.HIGH
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
