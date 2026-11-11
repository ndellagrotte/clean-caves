package com.personthecat.cleancaves;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.config.ConfigFile;
import com.personthecat.cleancaves.config.PresetReader;
import com.personthecat.cleancaves.util.HjsonTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Loader;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.personthecat.cleancaves.io.SafeFileIO.ensureDirExists;
import static com.personthecat.cleancaves.io.SafeFileIO.fileExists;
import static com.personthecat.cleancaves.util.CommonMethods.extension;

/** Owns this mod's on-disk layout and the entry point for (re)loading presets. */
public final class CaveInit {

    private static final Logger log = LogManager.getLogger(Reference.MOD_NAME);

    /** A message to display when the preset directory is somehow unavailable. */
    private static final String NO_ACCESS = "Currently unable to access preset directory.";

    /** A message to display when failing to create a directory. */
    private static final String CANT_CREATE = "Unable to create directory";

    /** A list of valid extensions to compare against presets. */
    private static final List<String> EXTENSIONS = List.of("hjson", "json", "cave");

    private CaveInit() { }

    /** {@code config/cleancaves} -- holds TUTORIAL.cave and REFERENCE.cave. */
    public static Path caveDir() {
        return Dirs.ROOT;
    }

    public static Path backupDir() {
        return Dirs.BACKUPS;
    }

    public static Path exampleDir() {
        return Dirs.EXAMPLES;
    }

    public static Path presetDir() {
        return Dirs.PRESETS;
    }

    public static Path importDir() {
        return Dirs.IMPORTS;
    }

    public static Path generatedDir() {
        return Dirs.GENERATED;
    }

    /** Initializes the supplied map with presets from the directory. */
    public static void initPresets(Map<String, CavePreset> presets) {
        // Verify the folders' integrity before proceeding.
        ensureDirExists(presetDir()).expect(CANT_CREATE);
        ensureDirExists(importDir()).expect(CANT_CREATE);
        // Clear this to allow presets to be reloaded.
        presets.clear();
        // Handle all files in the preset directory.
        presets.putAll(PresetReader.getPresets(presetDir(), importDir()));
        // If necessary, automatically write the expanded values.
        if (ConfigFile.autoGenerate) {
            ensureDirExists(generatedDir()).expect(CANT_CREATE);
            presets.forEach((name, preset) -> HjsonTools.writeJson(preset.raw(), generatedDir().resolve(name + ".cave"))
                    .throwIfPresent());
        }
        printLoadedPresets(presets);
    }

    /** Attempts to locate a preset using each of the possible extensions. */
    public static Optional<Path> locatePreset(String preset) {
        return locatePreset(presetDir(), preset);
    }

    public static Optional<Path> locatePreset(Path directory, String preset) {
        final Path presetFile = directory.resolve(preset);
        if (fileExists(presetFile, "Error checking file: " + presetFile)) {
            return Optional.of(presetFile);
        }
        for (final String ext : EXTENSIONS) {
            final Path withExtension = directory.resolve(preset + "." + ext);
            if (fileExists(withExtension, NO_ACCESS)) {
                return Optional.of(withExtension);
            }
        }
        return Optional.empty();
    }

    public static boolean validExtension(Path file) {
        return EXTENSIONS.contains(extension(file));
    }

    /** Prints which presets are currently loaded and whether they are enabled. */
    private static void printLoadedPresets(Map<String, CavePreset> presets) {
        presets.forEach((name, preset) -> log.info("{} is {}.", name, preset.enabled() ? "enabled" : "disabled"));
    }

    /**
     * Holder so that the config directory is resolved on first use rather than when this class is
     * loaded. {@code Loader.instance().getConfigDir()} is only meaningful once FML has started.
     */
    private static final class Dirs {

        static final Path ROOT = Loader.instance().getConfigDir().toPath().resolve(Reference.MOD_ID);
        static final Path BACKUPS = ROOT.resolve("backup");
        static final Path EXAMPLES = ROOT.resolve("examples");
        static final Path PRESETS = ROOT.resolve("presets");
        static final Path IMPORTS = ROOT.resolve("imports");
        static final Path GENERATED = ROOT.resolve("generated");

    }

}
