package com.personthecat.cleancaves.io;

import com.personthecat.cleancaves.CaveInit;
import com.personthecat.cleancaves.Reference;
import com.personthecat.cleancaves.config.PresetExpander;
import com.personthecat.cleancaves.world.feature.StructureSpawner;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.personthecat.cleancaves.io.SafeFileIO.ensureDirExists;
import static com.personthecat.cleancaves.io.SafeFileIO.fileExists;
import static com.personthecat.cleancaves.io.SafeFileIO.getRequiredResource;
import static com.personthecat.cleancaves.io.SafeFileIO.mkdirs;
import static com.personthecat.cleancaves.util.CommonMethods.runEx;

/**
 * Seeds the config directory from the assets bundled in the jar.
 *
 * <p>The manifests below are hard-coded because the jar is not scanned: adding a new preset,
 * import, or structure means adding its name to the matching list here.
 */
public final class JarFiles {

    /** The path where all of this mod's assets are stored. */
    private static final String ASSETS_PATH = "assets/" + Reference.MOD_ID;

    /** The name of the tutorial file. */
    private static final String TUTORIAL_NAME = "TUTORIAL.cave";

    /** The name of the stripped tutorial file. */
    private static final String REFERENCE_NAME = "REFERENCE.cave";

    /** All of the <b>example</b> presets to be copied from the jar. */
    private static final List<String> EXAMPLES = List.of(
        "flooded_vanilla.cave", "large_caves.cave", "spirals.cave",
        "tunnels.cave", "caverns.cave", "stone_layers.cave", "stalactites.cave",
        "ravines.cave", "stone_clusters.cave", "large_stalactites.cave",
        "vanilla.cave", "underground_forest.cave", "euclids_tunnels.cave",
        "lower_caves.cave", "cluster_caverns.cave"
    );

    private static final List<String> DEFAULTS = List.of(
        "00_stone_layers_basaltic.cave", "00_stone_layers_cold.cave",
        "00_stone_layers_desert.cave", "00_stone_layers_frozen.cave",
        "00_stone_layers_generic.cave", "00_stone_layers_mesa.cave",
        "00_stone_layers_mudstones.cave", "00_stone_layers_ocean.cave",
        "00_stone_layers_stone_beach.cave", "00_stone_layers_volcanic.cave",
        "01_arid_cliffs.hjson", "01_overgrown_cliffs.hjson",
        "01_tropics_cliffs.hjson", "02_caverns.cave",
        "04_bands_dirt_on_arid_tuff.cave", "04_moor_crevice.hjson",
        "10_jungle_ravines.cave", "10_lush_caves.cave",
        "10_rocky_dirt_entrances.cave", "10_stone_varnish.cave"
    );

    /** A couple of structure NBTs to be copied from the jar. */
    private static final List<String> STRUCTURES = List.of(
        "brown_mushroom_lg", "brown_mushroom_sm", "bush1_sm",
        "bush2_sm", "bush3_sm", "bush4_med", "hanging_spawner",
        "hanging_spawner_skeleton", "hanging_spawner_zombie",
        "red_mushroom"
    );

    /** Any preset that specifically belongs in /imports. */
    private static final List<String> IMPORTS = List.of(
        "blocks.cave", "common_values.cave", "conditions.cave",
        "defaults.cave", "sand_cave_values.cave", "types.cave"
    );

    private JarFiles() {}

    /** Copies the example presets from the jar to the disk. */
    public static void copyFiles() {
        // Verify the folders' integrity before proceeding.
        ensureDirExists(CaveInit.exampleDir())
            .expect("Error: Unable to create the example preset directory");
        ensureDirExists(CaveInit.importDir())
            .expect("Error: Unable to create the import directory");

        copyExamples();
        copyDefaults();
        copyImports();
        copyStructures();
        copyTutorial();
    }

    /** Reads {@code defaults.cave} straight out of the jar, without touching the disk copy. */
    public static JsonObject getDefaults() {
        final String fromLocation = ASSETS_PATH + "/imports/" + PresetExpander.DEFAULTS;
        try (final InputStream is = getRequiredResource(fromLocation);
             final Reader rx = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return JsonValue.readHjson(rx).asObject();
        } catch (final IOException e) {
            throw runEx("Reading internal defaults.cave", e);
        }
    }

    /** Examples are checked individually, so deleting one brings it back on the next launch. */
    private static void copyExamples() {
        for (final String fileName : EXAMPLES) {
            copyIfAbsent(ASSETS_PATH + "/examples/" + fileName,
                CaveInit.exampleDir().resolve(fileName));
        }
    }

    /** Defaults seed the preset folder once; after that the user owns its contents. */
    private static void copyDefaults() {
        final Path presetDir = CaveInit.presetDir();
        if (!fileExists(presetDir, "Error: Unable to read from preset directory")) {
            mkdirs(presetDir).expect("Error: Unable to create preset directory");
            for (final String fileName : DEFAULTS) {
                copyFile(ASSETS_PATH + "/presets/" + fileName, presetDir.resolve(fileName));
            }
        }
    }

    private static void copyImports() {
        final Path importDir = CaveInit.importDir();
        ensureDirExists(importDir).expect("Error creating import directory");
        for (final String fileName : IMPORTS) {
            copyIfAbsent(ASSETS_PATH + "/imports/" + fileName, importDir.resolve(fileName));
        }
    }

    /** Copies the example structures from the jar to the disk. */
    private static void copyStructures() {
        final Path structureDir = StructureSpawner.dir();
        if (!fileExists(structureDir, "Error: Unable to read from structure directory")) {
            mkdirs(structureDir).expect("Error: Unable to create structure directory.");
            for (final String structure : STRUCTURES) {
                copyFile(ASSETS_PATH + "/structures/" + structure + ".nbt",
                    structureDir.resolve(structure + ".nbt"));
            }
        }
    }

    /**
     * Copies the tutorial files into this mod's root directory. These are refreshed on every
     * launch so that they always document the running version -- edit a copy, not these.
     */
    private static void copyTutorial() {
        copyFile(ASSETS_PATH + "/" + TUTORIAL_NAME, CaveInit.caveDir().resolve(TUTORIAL_NAME));
        // Todo: Generate this file.
        copyFile(ASSETS_PATH + "/" + REFERENCE_NAME, CaveInit.caveDir().resolve(REFERENCE_NAME));
    }

    private static void copyIfAbsent(String fromLocation, Path toLocation) {
        if (!fileExists(toLocation, "Security error on " + toLocation)) {
            copyFile(fromLocation, toLocation);
        }
    }

    /** Copies any file from the jar to the disk. */
    private static void copyFile(String fromLocation, Path toLocation) {
        try (final InputStream toCopy = getRequiredResource(fromLocation);
             final OutputStream output = Files.newOutputStream(toLocation)) {
            toCopy.transferTo(output);
        } catch (final IOException e) {
            throw runEx("Error copying " + fromLocation + ". Perhaps the jar is corrupt.", e);
        }
    }
}
