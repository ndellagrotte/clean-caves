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
        final Map<Path, JsonObject> jsons = loadJsons(dir, ConfigFile.ignoreInvalidPresets);
        final Map<Path, JsonObject> definitions = loadJsons(imports, false);
        // Detect a few common syntax errors.
        jsons.entrySet().removeIf(e -> !passesSyntaxCheck(e.getKey(), e.getValue()));
        definitions.forEach(SyntaxHelper::check);
        // Merges at the root must come before the values that override them.
        jsons.values().forEach(PresetReader::enforceValueOrder);
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

    /**
     * Moves {@code imports}, {@code variables} and every root-level {@code $MERGE} above the
     * regular values, keeping the relative order within each group. A merge copies its keys in at
     * its own position, replacing whatever is already there, so a preset that writes
     * {@code tunnels: {...}} above {@code $VANILLA: ALL} would otherwise lose its own tunnels.
     * This only reorders the object in memory; the file on disk is left as the user wrote it.
     */
    static void enforceValueOrder(JsonObject json) {
        final JsonObject top = new JsonObject();
        final JsonObject bottom = new JsonObject();
        for (final String key : List.of(PresetExpander.IMPORTS, PresetExpander.VARIABLES)) {
            if (json.has(key)) {
                top.add(key, json.get(key));
            }
        }
        for (final JsonObject.Member member : json) {
            final String name = member.getName();
            if (name.equals(PresetExpander.IMPORTS) || name.equals(PresetExpander.VARIABLES)) {
                continue;
            }
            (name.startsWith("$") ? top : bottom).add(name, member.getValue());
        }
        json.clear();
        json.addAll(top);
        json.addAll(bottom);
    }

    /**
     * Loads every JSON file in a directory, recursively.
     *
     * @param skipInvalid Whether a file that cannot be parsed is logged and left out, instead of
     *                    stopping the load.
     */
    private static Map<Path, JsonObject> loadJsons(Path dir, boolean skipInvalid) {
        final Map<Path, JsonObject> jsons = new HashMap<>();
        loadInto(jsons, dir, skipInvalid);
        return jsons;
    }

    private static void loadInto(Map<Path, JsonObject> jsons, Path dir, boolean skipInvalid) {
        for (final Path file : listFiles(dir).orElse(List.of())) {
            if (Files.isDirectory(file)) {
                loadInto(jsons, file, skipInvalid);
            } else if (CaveInit.validExtension(file)) {
                log.info("Parsing preset file: {}", file.getFileName());
                try {
                    jsons.put(file, loadJson(file).asObject());
                } catch (final RuntimeException e) {
                    if (!skipInvalid) {
                        throw e;
                    }
                    log.error("Skipping {}: {}", file.getFileName(), describe(e));
                }
            }
        }
    }

    /** Runs the syntax check on a preset, which is skipped rather than fatal when the user allows it. */
    private static boolean passesSyntaxCheck(Path file, JsonObject json) {
        try {
            SyntaxHelper.check(file, json);
            return true;
        } catch (final RuntimeException e) {
            if (!ConfigFile.ignoreInvalidPresets) {
                throw e;
            }
            log.error("Skipping {}: {}", file.getFileName(), describe(e));
            return false;
        }
    }

    /** The message of an error together with that of its cause, which is where a parser's detail sits. */
    private static String describe(Throwable e) {
        final Throwable cause = e.getCause();
        return cause == null || cause.getMessage() == null
                ? String.valueOf(e.getMessage())
                : e.getMessage() + " (" + cause.getMessage() + ")";
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
        final Map<String, Path> sources = new HashMap<>();
        // Presets are known by file name alone, so two files can claim the same name. Going in path
        // order keeps the survivor the same from one launch to the next.
        jsons.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            final String name = noExtension(e.getKey());
            final Path previous = sources.put(name, e.getKey());
            if (previous != null) {
                log.warn("{} and {} are both named \"{}\". Only {} will be loaded.", previous, e.getKey(), name, e.getKey());
            }
            extractRecursive(extracted, name, e.getValue());
        });
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

    /**
     * Parses the contents of the given file into a generic JsonValue. Bytes that are not valid
     * UTF-8 decode to a replacement character instead of failing, so a preset saved in a legacy
     * encoding still loads as long as the offending characters sit in comments or strings.
     */
    private static JsonValue loadJson(Path file) {
        final String text;
        try {
            text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw runEx(f("Unable to load preset file {}: {}", file.getFileName(), e), e);
        }
        try {
            return extension(file).equals("json") ? JsonValue.readJSON(text) : JsonValue.readHjson(text);
        } catch (final ParseException e) {
            throw runEx(f("Error reading {}", file.getFileName()), e);
        }
    }

}
