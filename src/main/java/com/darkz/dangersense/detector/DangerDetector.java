package com.darkz.dangersense.detector;

import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.danger.DangerLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Common contract every danger type implements.
 *
 * Kept intentionally small: one detector = one responsibility. Detectors
 * are stateless between calls except for cheap internal caches (e.g. the
 * last computed intensity), so they can be reused for the whole client
 * session and are safe to call from {@link com.darkz.dangersense.danger.WarningManager}
 * every {@code scanIntervalTicks} ticks.
 */
public interface DangerDetector {

    /** Unique, stable id used in config lookups and logging (e.g. "creeper"). */
    String getId();

    /** Whether this detector is turned on in the current config. */
    boolean isEnabled(DangerSenseConfig config);

    /**
     * Core detection call, as requested by the mod spec.
     * Implementations should be cheap: nearby-entity search only, no
     * world-wide scans, no per-tick allocations where avoidable.
     */
    boolean shouldWarn(LocalPlayer player, ClientLevel world, DangerSenseConfig config);

    /**
     * Danger level computed during the last {@link #shouldWarn} call.
     * Returns {@link DangerLevel#NONE} if {@code shouldWarn} last returned false.
     */
    DangerLevel getLevel();

    /**
     * Final intensity (0.0 - 1.0) computed during the last {@link #shouldWarn}
     * call, typically {@code level.baseIntensity()} scaled by proximity.
     */
    float getIntensity();
}
