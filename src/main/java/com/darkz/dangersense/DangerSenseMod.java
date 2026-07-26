package com.darkz.dangersense;

import com.darkz.dangersense.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. Danger Sense is a client-only mod (it only warns the
 * local player), but the sound event has to be registered on the common
 * side so it exists in the registry before assets are resolved.
 */
public final class DangerSenseMod implements ModInitializer {

    public static final String MOD_ID = "dangersense";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Custom heartbeat sound event, played client-side by {@code sound.HeartbeatPlayer}.
     *
     * ⚠ MOJMAP UNCERTAINTY: {@code SoundEvent.createVariableRangeEvent(ResourceLocation)}
     * is my best-confidence guess for the Yarn {@code SoundEvent.of(Identifier)}
     * equivalent — verify against a real build log. Same for the
     * {@code new ResourceLocation(namespace, path)} constructor: it's public
     * in 1.20.1–1.21.x historically, but some later 1.21 releases push you
     * toward {@code ResourceLocation.fromNamespaceAndPath(...)} instead —
     * if the constructor is gone/deprecated-removed on a given version,
     * swap it there.
     */
    public static final SoundEvent HEARTBEAT_SOUND =
            Registry.register(
                    BuiltInRegistries.SOUND_EVENT,
                    new ResourceLocation(MOD_ID, "heartbeat"),
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "heartbeat"))
            );

    @Override
    public void onInitialize() {
        // Config is loaded here (not in the client initializer) so that it is
        // available as early as possible and can, in theory, be reused by
        // server-side logic later without restructuring.
        ConfigManager.load();
        LOGGER.info("[Danger Sense] Инициализация завершена, конфигурация загружена.");

        // NOTE: FigureStoneLib integration point.
        // If FigureStoneLib exposes a shared config/JSON utility (e.g.
        // FigureStoneLib.json().load(...) or a generic file-watcher), replace
        // ConfigManager's raw Gson I/O with it here to avoid duplicating
        // file-handling code across your mod ecosystem.
    }
}
