package com.darkz.dangersense.detector;

import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.danger.DangerLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/**
 * Predicts fall damage by raycasting straight down from the player to the
 * first solid block and estimating vanilla fall-damage rules
 * (1 damage per block beyond 3, roughly), then compares it against the
 * configured threshold. Only triggers while airborne and falling.
 */
public final class FallDetector implements DangerDetector {

    private static final double SAFE_FALL_BLOCKS = 3.0;
    private static final double MAX_PREDICTION_DEPTH = 40.0;

    private DangerLevel level = DangerLevel.NONE;
    private float intensity = 0.0f;

    @Override
    public String getId() {
        return "fall";
    }

    @Override
    public boolean isEnabled(DangerSenseConfig config) {
        return config.fallAlertEnabled;
    }

    @Override
    public boolean shouldWarn(LocalPlayer player, ClientLevel world, DangerSenseConfig config) {
        if (player.onGround() || player.isFallFlying() || player.getDeltaMovement().y >= -0.05
                || player.isInWater() || player.isInLava()) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        Vec3 from = player.position();
        Vec3 to = from.subtract(0, MAX_PREDICTION_DEPTH, 0);

        ClipContext context = new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );
        HitResult result = world.clip(context);

        double fallDistance;
        if (result.getType() == HitResult.Type.MISS) {
            fallDistance = MAX_PREDICTION_DEPTH; // effectively "very far / void"
        } else {
            // ⚠ MOJMAP: getLocation() is my best-confidence guess for Yarn's
            // HitResult#getPos() — verify against a build log.
            fallDistance = from.y - result.getLocation().y;
        }

        double predictedDamage = Math.max(0, fallDistance - SAFE_FALL_BLOCKS);

        if (predictedDamage < config.fallDamageThreshold) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        double ratio = Math.min(1.0, predictedDamage / (config.fallDamageThreshold * 3.0));
        level = ratio > 0.8 ? DangerLevel.CRITICAL
                : ratio > 0.5 ? DangerLevel.HIGH
                : DangerLevel.MEDIUM;
        intensity = Math.min(1.0f, level.baseIntensity() + (float) ratio * 0.2f);
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
