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

    /** The name of the folder containing cat presets. */
    private static final String CAT_FOLDER_NAME = "cat";

    /** The name of the tutorial file. */
    private static final String TUTORIAL_NAME = "TUTORIAL.cave";

    /** The name of the stripped tutorial file. */
    private static final String REFERENCE_NAME = "REFERENCE.cave";

    /** All of the <b>example</b> presets to be copied from the jar. */
    private static final List<String> EXAMPLES = List.of(
        "flooded_vanilla", "large_caves", "spirals",
        "tunnels", "caverns", "stone_layers", "stalactites",
        "ravines", "stone_clusters", "large_stalactites",
        "vanilla", "underground_forest", "euclids_tunnels",
        "lower_caves", "cluster_caverns"
    );

    private static final List<String> CAT_IMPORTS = List.of(
        "common", "crack", "desert", "generic",
        "jungle", "lava_aquifers", "maze",
        "mountain", "mushroom", "snow", "water"
    );

    private static final List<String> DEFAULTS = List.of(
        "cat", "cat_lite", "geodes", "ore_veins"
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
        "blocks", "conditions", "defaults", "types"
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
        copyCatImports();
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
            copyIfAbsent(ASSETS_PATH + "/presets/" + fileName + ".cave",
                CaveInit.exampleDir().resolve(fileName + ".cave"));
        }
    }

    /** Defaults seed the preset folder once; after that the user owns its contents. */
    private static void copyDefaults() {
        final Path presetDir = CaveInit.presetDir();
        if (!fileExists(presetDir, "Error: Unable to read from preset directory")) {
            mkdirs(presetDir).expect("Error: Unable to create preset directory");
            for (final String name : DEFAULTS) {
                copyFile(ASSETS_PATH + "/presets/" + name + ".cave", presetDir.resolve(name + ".cave"));
            }
        }
    }

    private static void copyImports() {
        final Path importDir = CaveInit.importDir();
        ensureDirExists(importDir).expect("Error creating import directory");
        for (final String name : IMPORTS) {
            copyIfAbsent(ASSETS_PATH + "/imports/" + name + ".cave", importDir.resolve(name + ".cave"));
        }
    }

    private static void copyCatImports() {
        final Path catDir = CaveInit.importDir().resolve(CAT_FOLDER_NAME);
        ensureDirExists(catDir).expect("Error creating cat directory");
        for (final String name : CAT_IMPORTS) {
            copyIfAbsent(ASSETS_PATH + "/imports/cat/" + name + ".cave", catDir.resolve(name + ".cave"));
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
