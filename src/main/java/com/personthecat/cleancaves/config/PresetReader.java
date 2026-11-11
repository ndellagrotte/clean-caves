package com.personthecat.cleancaves.config;

import com.personthecat.cleancaves.CaveInit;
import com.personthecat.cleancaves.Reference;
import com.personthecat.cleancaves.util.HjsonTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.personthecat.cleancaves.io.SafeFileIO.listFiles;
import static com.personthecat.cleancaves.util.CommonMethods.extension;
import static com.personthecat.cleancaves.util.CommonMethods.f;
import static com.personthecat.cleancaves.util.CommonMethods.noExtension;
import static com.personthecat.cleancaves.util.CommonMethods.runEx;
import static com.personthecat.cleancaves.util.CommonMethods.runExF;

/**
 * Drives everything that has to happen to raw JSON before a preset can become a concrete Java
 * object: parsing, syntax linting, expanding variables and imports, and separating inner presets
 * into their regular counterparts using overrides from parent presets.
 */
public final class PresetReader {

    private static final Logger log = LogManager.getLogger(Reference.MOD_NAME);

    /** The key where inner presets are stored recursively. */
    public static final String INNER_KEY = "inner";

    private PresetReader() { }

    /**
     * Reads a series of {@link CavePreset} objects from a directory.
     *
     * @param dir The directory to read presets from.
     * @param imports The directory holding shared variable definitions.
     * @return A map of filename -> preset.
     * @throws ParseException if a preset contains a syntax error.
     */
    public static Map<String, CavePreset> getPresets(Path dir, Path imports) {
        final Map<Path, JsonObject> jsons = loadJsons(dir);
        final Map<Path, JsonObject> definitions = loadJsons(imports);
        // Detect a few common syntax errors.
        jsons.forEach(SyntaxHelper::check);
        definitions.forEach(SyntaxHelper::check);
        // Expand all of the variable definitions and imports.
        PresetExpander.expandAll(jsons, definitions);
        // Mark all values as unused so we can track them.
        jsons.forEach((file, json) -> json.setAllAccessed(false));
        // Convert to a map of filename -> POJO.
        final Map<String, CavePreset> settings = toSettings(jsons);
        // Check each preset and inform the user of potential mistakes.
        settings.forEach((name, cfg) -> new PresetTester(cfg, name, ConfigFile.strictPresets).run());
        return settings;
    }

    /** Loads every JSON file in a directory, recursively. */
    private static Map<Path, JsonObject> loadJsons(Path dir) {
        final Map<Path, JsonObject> jsons = new HashMap<>();
        loadInto(jsons, dir);
        return jsons;
    }

    private static void loadInto(Map<Path, JsonObject> jsons, Path dir) {
        for (final Path file : listFiles(dir).orElse(List.of())) {
            if (Files.isDirectory(file)) {
                loadInto(jsons, file);
            } else if (CaveInit.validExtension(file)) {
                log.info("Parsing preset file: {}", file.getFileName());
                jsons.put(file, loadJson(file).asObject());
            }
        }
    }

    /** Converts a map of (file -> json) to (filename -> POJO). */
    private static Map<String, CavePreset> toSettings(Map<Path, JsonObject> jsons) {
        final Map<String, CavePreset> settings = new HashMap<>();
        extractInner(jsons).forEach((name, json) -> {
            try {
                settings.put(name, CavePreset.from(json));
            } catch (final RuntimeException e) {
                final String msg = f("Error reading {}: {}", name, e.getMessage());
                if (ConfigFile.ignoreInvalidPresets) {
                    log.error(msg);
                } else {
                    throw runEx(msg);
                }
            }
        });
        return settings;
    }

    private static Map<String, JsonObject> extractInner(Map<Path, JsonObject> jsons) {
        final Map<String, JsonObject> extracted = new HashMap<>();
        jsons.forEach((file, json) -> extractRecursive(extracted, noExtension(file), json));
        return extracted;
    }

    /**
     * An {@code inner} array produces one sibling preset per element -- {@code name[0]},
     * {@code name[1]}, ... -- each being the parent minus {@code inner}, overlaid with the child.
     */
    private static void extractRecursive(Map<String, JsonObject> data, String name, JsonObject parent) {
        final List<JsonObject> inner = HjsonTools.getRegularObjects(parent, INNER_KEY);
        for (int i = 0; i < inner.size(); i++) {
            final JsonObject child = inner.get(i);
            final JsonObject clone = new JsonObject().addAll(parent).remove(INNER_KEY);
            for (final JsonObject.Member member : child) {
                clone.set(member.getName(), member.getValue());
            }
            extractRecursive(data, f("{}[{}]", name, i), clone);
        }
        data.put(name, parent);
    }

    /** Returns a JsonObject from the input file, letting external callers handle absence. */
    public static Optional<JsonObject> getPresetJson(Path file) {
        try {
            return Optional.of(loadJson(file).asObject());
        } catch (final RuntimeException ignored) {
            return Optional.empty();
        }
    }

    /** Parses the contents of the given file into a generic JsonValue. */
    private static JsonValue loadJson(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return extension(file).equals("json") ? JsonObject.readJSON(reader) : JsonObject.readHjson(reader);
        } catch (final IOException ignored) {
            throw runExF("Unable to load preset file {}", file.getFileName());
        } catch (final ParseException e) {
            throw runEx(f("Error reading {}", file.getFileName()), e);
        }
    }

}
