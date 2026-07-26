package com.darkz.dangersense.config;

import com.darkz.dangersense.DangerSenseMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Handles reading/writing {@code config/dangersense.json}.
 *
 * NOTE — FigureStoneLib integration point:
 * If FigureStoneLib already ships a generic "JsonConfig<T>" helper (load,
 * save, hot-reload watcher), this whole class can be replaced by a thin
 * wrapper around it. Kept self-contained here so the mod has zero hard
 * dependency on FigureStoneLib's exact API surface.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("dangersense.json");

    private static volatile DangerSenseConfig instance = new DangerSenseConfig();
    private static volatile long lastLoadedModifiedTime = -1L;

    private ConfigManager() {
    }

    public static DangerSenseConfig get() {
        return instance;
    }

    public static synchronized void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                    DangerSenseConfig loaded = GSON.fromJson(reader, DangerSenseConfig.class);
                    instance = loaded != null ? loaded : new DangerSenseConfig();
                }
                lastLoadedModifiedTime = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
            } else {
                instance = new DangerSenseConfig();
                save();
            }
        } catch (IOException | RuntimeException e) {
            DangerSenseMod.LOGGER.warn("[Danger Sense] Не удалось загрузить конфиг, используются значения по умолчанию.", e);
            instance = new DangerSenseConfig();
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(
                    CONFIG_PATH, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(instance, writer);
            }
            lastLoadedModifiedTime = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
        } catch (IOException e) {
            DangerSenseMod.LOGGER.warn("[Danger Sense] Не удалось сохранить конфиг.", e);
        }
    }

    /**
     * Cheap hot-reload check: only stats the file, and only when
     * {@code hotReloadEnabled} is true. Called at most once per second by
     * {@link com.darkz.dangersense.client.ClientTickHandler}, never every tick.
     */
    public static synchronized void checkHotReload() {
        if (!instance.hotReloadEnabled) return;
        try {
            if (!Files.exists(CONFIG_PATH)) return;
            long modified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
            if (modified != lastLoadedModifiedTime) {
                load();
                DangerSenseMod.LOGGER.info("[Danger Sense] Конфигурация перезагружена (hot reload).");
            }
        } catch (IOException ignored) {
            // Non-fatal: keep using the currently loaded config.
        }
    }
}
