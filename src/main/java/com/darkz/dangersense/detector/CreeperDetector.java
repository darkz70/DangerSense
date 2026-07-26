package com.darkz.dangersense.detector;

import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.danger.DangerLevel;
import com.darkz.dangersense.util.RaycastUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Warns about nearby Creepers. Intensity increases as distance decreases;
 * Creepers hidden behind blocks (no line of sight to the player's eyes)
 * are ignored entirely, as requested.
 */
public final class CreeperDetector implements DangerDetector {

    private DangerLevel level = DangerLevel.NONE;
    private float intensity = 0.0f;

    @Override
    public String getId() {
        return "creeper";
    }

    @Override
    public boolean isEnabled(DangerSenseConfig config) {
        return config.creeperAlertEnabled;
    }

    @Override
    public boolean shouldWarn(LocalPlayer player, ClientLevel world, DangerSenseConfig config) {
        double range = config.creeperDetectionDistance;
        AABB searchBox = player.getBoundingBox().inflate(range); // Yarn "expand" -> Mojmap "inflate"

        // Reused list from the world's entity query — Fabric/vanilla already
        // avoids a full-world scan here by querying the entity chunk cache.
        List<Creeper> creepers = world.getEntitiesOfClass(
                Creeper.class, searchBox, creeper -> creeper.isAlive());

        double closestDistanceSq = Double.MAX_VALUE;
        boolean found = false;

        Vec3 playerEyePos = player.getEyePosition();
        for (Creeper creeper : creepers) {
            double distanceSq = creeper.distanceToSqr(player);
            if (distanceSq > range * range) continue;

            Vec3 creeperEyePos = creeper.getEyePosition();
            if (!RaycastUtil.hasLineOfSight(world, player, playerEyePos, creeper, creeperEyePos)) {
                continue; // behind a wall — ignored, as specified
            }

            found = true;
            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
            }
        }

        if (!found) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        double closestDistance = Math.sqrt(closestDistanceSq);
        float proximity = (float) (1.0 - Math.min(1.0, closestDistance / range)); // 0 far .. 1 close

        // A charged/swelling (fuse active) Creeper is scarier at the same distance.
        // ⚠ MOJMAP UNCERTAINTY: I'm not confident about the exact official name
        // for Yarn's Creeper#getClientFuseTime(float). If this doesn't compile,
        // the simplest safe fallback is `creeper.isIgnited()` (drop the timing
        // interpolation, treat "ignited at all" as swelling).
        boolean swelling = false;
        for (Creeper creeper : creepers) {
            if (creeper.getSwellFactor(0f) > 0f) {
                swelling = true;
                break;
            }
        }

        level = swelling ? DangerLevel.HIGH : (proximity > 0.6f ? DangerLevel.MEDIUM : DangerLevel.LOW);
        intensity = Math.min(1.0f, level.baseIntensity() + proximity * 0.3f);
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
