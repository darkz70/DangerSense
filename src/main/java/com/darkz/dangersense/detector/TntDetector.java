package com.darkz.dangersense.detector;

import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.danger.DangerLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Warns about primed TNT nearby. The closer the fuse is to zero (explosion
 * imminent) and the closer the player is, the stronger the warning.
 */
public final class TntDetector implements DangerDetector {

    /** Vanilla default fuse length in ticks, used to normalize urgency. */
    private static final int DEFAULT_FUSE = 80;

    private DangerLevel level = DangerLevel.NONE;
    private float intensity = 0.0f;

    @Override
    public String getId() {
        return "tnt";
    }

    @Override
    public boolean isEnabled(DangerSenseConfig config) {
        return config.tntAlertEnabled;
    }

    @Override
    public boolean shouldWarn(LocalPlayer player, ClientLevel world, DangerSenseConfig config) {
        double range = config.tntDetectionDistance;
        AABB searchBox = player.getBoundingBox().inflate(range);
        List<PrimedTnt> tntEntities = world.getEntitiesOfClass(PrimedTnt.class, searchBox, PrimedTnt::isAlive);

        if (tntEntities.isEmpty()) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        float worstUrgency = 0.0f;
        double closestDistance = Double.MAX_VALUE;

        for (PrimedTnt tnt : tntEntities) {
            double distance = Math.sqrt(tnt.distanceToSqr(player));
            if (distance > range) continue;

            // ⚠ MOJMAP: getFuse() is my best-confidence guess (short, literal
            // name Mojang tends to keep) — verify against a build log.
            int fuse = tnt.getFuse();
            float urgency = 1.0f - Math.min(1.0f, fuse / (float) DEFAULT_FUSE); // closer to 0 fuse = closer to 1
            if (urgency > worstUrgency) worstUrgency = urgency;
            if (distance < closestDistance) closestDistance = distance;
        }

        if (closestDistance == Double.MAX_VALUE) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        float proximity = (float) (1.0 - Math.min(1.0, closestDistance / range));
        float combined = Math.max(worstUrgency, proximity * 0.6f);

        level = combined > 0.75f ? DangerLevel.CRITICAL
                : combined > 0.4f ? DangerLevel.HIGH
                : DangerLevel.MEDIUM;
        intensity = Math.min(1.0f, level.baseIntensity() + combined * 0.25f);
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
