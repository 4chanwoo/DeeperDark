package com.chanwoo.deeperdark.client;

import com.chanwoo.deeperdark.core.DeeperDarkMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Client settings, saved to config/deeper_dark.json. */
@Environment(EnvType.CLIENT)
public final class DeeperDarkConfig {
    public static final String LODESTONE_MOD_ID = "lodestone";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(DeeperDarkMod.MOD_ID + ".json");

    private static Data data = new Data();

    /** Fields are what gets written to the file. */
    private static final class Data {
        boolean lodestoneParticles = false;
    }

    public static boolean isLodestoneInstalled() {
        return FabricLoader.getInstance().isModLoaded(LODESTONE_MOD_ID);
    }

    /** Whether to draw Deeper Dark's particles with LodestoneLib instead of vanilla particles. */
    public static boolean useLodestoneParticles() {
        return data.lodestoneParticles && isLodestoneInstalled();
    }

    /** Whether the vanilla-style scream ring glows, which it always does when LodestoneLib can draw the glow. */
    public static boolean useGlowingVanillaParticles() {
        return isLodestoneInstalled() && !useLodestoneParticles();
    }

    public static boolean getLodestoneParticlesSetting() {
        return data.lodestoneParticles;
    }

    public static void setLodestoneParticles(boolean enabled) {
        data.lodestoneParticles = enabled;
        save();
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(PATH)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) {
                data = loaded;
            }
        } catch (IOException | JsonParseException e) {
            DeeperDarkMod.LOGGER.warn("Could not read {}, using defaults", PATH, e);
        }
    }

    private static void save() {
        try (Writer writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            DeeperDarkMod.LOGGER.warn("Could not save {}", PATH, e);
        }
    }

    private DeeperDarkConfig() {
    }
}
