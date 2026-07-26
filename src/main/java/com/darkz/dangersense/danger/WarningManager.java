package com.darkz.dangersense.danger;

import com.darkz.dangersense.config.ConfigManager;
import com.darkz.dangersense.config.DangerSenseConfig;
import com.darkz.dangersense.detector.CreeperDetector;
import com.darkz.dangersense.detector.DangerDetector;
import com.darkz.dangersense.detector.DrowningDetector;
import com.darkz.dangersense.detector.FallDetector;
import com.darkz.dangersense.detector.LavaDetector;
import com.darkz.dangersense.detector.LowHealthDetector;
import com.darkz.dangersense.detector.TntDetector;
import com.darkz.dangersense.detector.WardenDetector;
import com.darkz.dangersense.util.TickThrottle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.List;

/**
 * Owns the list of {@link DangerDetector}s, throttles how often they run,
 * and reduces their results to a single "strongest active warning" per
 * evaluation. This is the only class the client tick handler needs to talk to.
 */
public final class WarningManager {

    private final List<DangerDetector> detectors = List.of(
            new CreeperDetector(),
            new TntDetector(),
            new WardenDetector(),
            new LowHealthDetector(),
            new DrowningDetector(),
            new LavaDetector(),
            new FallDetector()
    );

    private final TickThrottle throttle = new TickThrottle();
    private DangerWarning lastWarning = DangerWarning.NONE;

    /**
     * Re-evaluates detectors if the throttle interval elapsed, otherwise
     * returns the previously computed warning unchanged (cheap no-op path).
     */
    public DangerWarning tick(LocalPlayer player, ClientLevel world) {
        DangerSenseConfig config = ConfigManager.get();

        if (!throttle.tick(config.scanIntervalTicks)) {
            return lastWarning;
        }

        DangerDetector strongest = null;
        DangerLevel strongestLevel = DangerLevel.NONE;
        float strongestIntensity = 0.0f;

        for (DangerDetector detector : detectors) {
            if (!detector.isEnabled(config)) continue;

            boolean warns = detector.shouldWarn(player, world, config);
            if (!warns) continue;

            if (detector.getIntensity() > strongestIntensity) {
                strongest = detector;
                strongestLevel = detector.getLevel();
                strongestIntensity = detector.getIntensity();
            }
        }

        lastWarning = strongest == null
                ? DangerWarning.NONE
                : new DangerWarning(strongest.getId(), strongestLevel, strongestIntensity);

        return lastWarning;
    }

    public DangerWarning getLastWarning() {
        return lastWarning;
    }
}
