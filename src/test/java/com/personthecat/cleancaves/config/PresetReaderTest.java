package com.personthecat.cleancaves.config;

import org.hjson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers what happens to a preset between the file and the expander: decoding, and the reordering
 * that lets a root-level merge sit anywhere in the file.
 */
class PresetReaderTest {

    private static final Path PRESET = Paths.get("preset.cave");
    private static final Path DEFAULTS = Paths.get("defaults.cave");

    @Test
    @DisplayName("imports, variables and root merges move above regular values, in their original order")
    void reordersRootValues() {
        final JsonObject preset = new JsonObject()
                .add("tunnels", new JsonObject())
                .add("$B", "ALL")
                .add("variables", new JsonObject())
                .add("enabled", true)
                .add("$A", "ALL")
                .add("imports", "shared.cave");

        PresetReader.enforceValueOrder(preset);

        assertThat(preset.names()).containsExactly("imports", "variables", "$B", "$A", "tunnels", "enabled");
    }

    @Test
    @DisplayName("a value written above `$VANILLA: ALL` still overrides the merged default")
    void ownValueWinsOverLaterMerge() {
        final JsonObject preset = new JsonObject()
                .add("tunnels", new JsonObject().add("count", 3))
                .add("$VANILLA", "ALL");
        final Map<Path, JsonObject> presets = new LinkedHashMap<>();
        presets.put(PRESET, preset);
        final Map<Path, JsonObject> definitions = new LinkedHashMap<>();
        // The whole of defaults.cave is what `$VANILLA` refers to.
        definitions.put(DEFAULTS,
                new JsonObject()
                        .add("tunnels", new JsonObject().add("count", 1))
                        .add("ravines", new JsonObject().add("count", 2)));

        PresetReader.enforceValueOrder(preset);
        PresetExpander.expandAll(presets, definitions);

        assertThat(preset.get("tunnels").asObject().get("count").asInt()).isEqualTo(3);
        assertThat(preset.get("ravines").asObject().get("count").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("a preset holding bytes that are not UTF-8 still loads")
    void toleratesLegacyEncoding(@TempDir Path dir) throws IOException {
        final Path file = dir.resolve("legacy.cave");
        // "café" in Latin-1: the 0xE9 byte is not a valid UTF-8 sequence.
        Files.write(file, "# café\nenabled: false\n".getBytes(StandardCharsets.ISO_8859_1));

        final Optional<JsonObject> json = PresetReader.getPresetJson(file);

        assertThat(json).isPresent();
        assertThat(json.get().get("enabled").asBoolean()).isFalse();
    }

}
