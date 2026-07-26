package com.darkz.dangersense.detector;

import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.danger.DangerLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Warns when the player is walking/falling toward lava. Checks the block(s)
 * directly ahead along the current movement vector plus the block below,
 * rather than scanning an area, keeping this detector effectively O(1).
 */
public final class LavaDetector implements DangerDetector {

    private DangerLevel level = DangerLevel.NONE;
    private float intensity = 0.0f;

    @Override
    public String getId() {
        return "lava";
    }

    @Override
    public boolean isEnabled(DangerSenseConfig config) {
        return config.lavaAlertEnabled;
    }

    @Override
    public boolean shouldWarn(LocalPlayer player, ClientLevel world, DangerSenseConfig config) {
        Vec3 velocity = player.getDeltaMovement();
        Vec3 pos = player.position();

        // Already touching lava is handled by vanilla damage/fire — here we
        // only care about the "about to step/fall into it" prediction.
        if (player.isInLava()) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        boolean movingHorizontally = (velocity.x * velocity.x + velocity.z * velocity.z) > 0.0025;
        Vec3 aheadPos = movingHorizontally
                ? pos.add(velocity.x * 4.0, 0, velocity.z * 4.0)
                : pos;

        BlockPos aheadFeet = BlockPos.containing(aheadPos);
        BlockPos below = BlockPos.containing(pos).below();

        boolean lavaAhead = world.getFluidState(aheadFeet).is(FluidTags.LAVA)
                || world.getFluidState(aheadFeet.below()).is(FluidTags.LAVA);
        boolean lavaBelow = world.getFluidState(below).is(FluidTags.LAVA);

        if (!lavaAhead && !lavaBelow) {
            level = DangerLevel.NONE;
            intensity = 0.0f;
            return false;
        }

        // Falling toward lava below is the most urgent case.
        level = lavaBelow && velocity.y < -0.1 ? DangerLevel.CRITICAL
                : lavaAhead ? DangerLevel.HIGH
                : DangerLevel.MEDIUM;
        intensity = level.baseIntensity();
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
