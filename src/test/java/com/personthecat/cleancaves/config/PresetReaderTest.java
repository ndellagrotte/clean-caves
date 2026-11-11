package com.personthecat.cleancaves.config;

import org.hjson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.init.Bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("with ignoreInvalidPresets, a preset that does not parse is skipped and the rest still load")
    void skipsUnparsablePresetWhenAllowed(@TempDir Path dir) throws IOException {
        Bootstrap.register();
        final Path presets = Files.createDirectory(dir.resolve("presets"));
        final Path imports = Files.createDirectory(dir.resolve("imports"));
        Files.writeString(imports.resolve("defaults.cave"), "{}");
        Files.writeString(presets.resolve("good.cave"), "enabled: false");
        Files.writeString(presets.resolve("broken.cave"), "tunnels: [ {");

        final boolean before = ConfigFile.ignoreInvalidPresets;
        try {
            ConfigFile.ignoreInvalidPresets = true;
            assertThat(PresetReader.getPresets(presets, imports)).containsOnlyKeys("good");

            ConfigFile.ignoreInvalidPresets = false;
            assertThatThrownBy(() -> PresetReader.getPresets(presets, imports)).isInstanceOf(RuntimeException.class);
        } finally {
            ConfigFile.ignoreInvalidPresets = before;
        }
    }

}
