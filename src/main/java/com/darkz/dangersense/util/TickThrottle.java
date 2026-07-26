package com.darkz.dangersense.util;

/**
 * Minimal tick counter that answers "should I run now?" without allocating.
 * Central to the performance requirement: detectors are re-evaluated every
 * {@code scanIntervalTicks} ticks instead of every tick.
 */
public final class TickThrottle {

    private int counter = 0;

    /**
     * Advances the internal counter and returns true exactly once every
     * {@code intervalTicks} calls (interval clamped to a minimum of 1).
     */
    public boolean tick(int intervalTicks) {
        int interval = Math.max(1, intervalTicks);
        counter++;
        if (counter >= interval) {
            counter = 0;
            return true;
        }
        return false;
    }

    public void reset() {
        counter = 0;
    }
}
