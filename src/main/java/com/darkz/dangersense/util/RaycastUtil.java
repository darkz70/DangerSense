package com.darkz.dangersense.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/**
 * Cheap line-of-sight helper used by detectors that must "ignore targets
 * behind walls" (e.g. Creeper Alert). Uses vanilla's block raycast, which
 * is O(distance) and does not allocate beyond the single {@link ClipContext}.
 */
public final class RaycastUtil {

    private RaycastUtil() {
    }

    /**
     * Returns true if there is a clear block-collision-free line between the
     * two eye positions (entity origin is not eye height — callers should
     * pass eye positions explicitly to avoid surprises).
     */
    public static boolean hasLineOfSight(Level world, Entity from, Vec3 fromPos, Entity target, Vec3 toPos) {
        // ⚠ MOJMAP: ClipContext.Block / ClipContext.Fluid are my best-confidence
        // names for Yarn's ShapeType/FluidHandling enums — verify against a build log.
        ClipContext context = new ClipContext(
                fromPos,
                toPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                from
        );
        HitResult result = world.clip(context);
        return result.getType() == HitResult.Type.MISS;
    }
}
