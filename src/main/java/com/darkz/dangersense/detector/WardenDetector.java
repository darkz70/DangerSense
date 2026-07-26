package com.darkz.dangersense.detector;

import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.danger.DangerLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Warns about nearby Wardens. Escalates to a strong warning the moment the
 * Warden is actively targeting the player (its most lethal state).
 */
public final class WardenDetector implements DangerDetector {

    private DangerLevel level = DangerLevel.NONE;
    private float intensity = 0.0f;

    @Override
    public String getId() {
        return "warden";
    }

    @Override
    public boolean isEnabled(DangerSenseConfig config) {
        return config.wardenAlertEnabled;
    }

    @Override
    public boolean shouldWarn(LocalPlayer player, ClientLevel world, DangerSenseConfig config) {
        double range = config.wardenDetectionDistance;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<Warden> wardens = world.getEntitiesOfClass(Warden.class, searchBox, Warden::isAlive);

        if (wardens.isEmpty()) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        double closestDistance = Double.MAX_VALUE;
        boolean targetingPlayer = false;

        for (Warden warden : wardens) {
            double distance = Math.sqrt(warden.distanceToSqr(player));
            if (distance > range) continue;
            if (distance < closestDistance) closestDistance = distance;

            if (warden.getTarget() == player) {
                targetingPlayer = true;
            }
        }

        if (closestDistance == Double.MAX_VALUE) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        float proximity = (float) (1.0 - Math.min(1.0, closestDistance / range));

        level = targetingPlayer ? DangerLevel.CRITICAL
                : proximity > 0.5f ? DangerLevel.HIGH
                : DangerLevel.MEDIUM;
        intensity = Math.min(1.0f, level.baseIntensity() + proximity * 0.2f);
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
