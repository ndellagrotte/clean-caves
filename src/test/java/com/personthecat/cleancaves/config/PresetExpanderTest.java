package com.personthecat.cleancaves.config;

import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The import and variable system is what makes presets composable, and it runs before anything is
 * type-checked -- a mistake here silently produces a preset that generates nothing.
 */
class PresetExpanderTest {

    private static final Path PRESET = Paths.get("preset.cave");
    private static final Path IMPORT = Paths.get("shared.cave");
    /** {@code defaults.cave} is bound to {@code $VANILLA} implicitly and must always be present. */
    private static final Path DEFAULTS = Paths.get("defaults.cave");

    private static JsonObject expand(JsonObject preset, Map<Path, JsonObject> definitions) {
        definitions.putIfAbsent(DEFAULTS, new JsonObject());
        final Map<Path, JsonObject> presets = new LinkedHashMap<>();
        presets.put(PRESET, preset);
        PresetExpander.expandAll(presets, definitions);
        return presets.get(PRESET);
    }

    private static Map<Path, JsonObject> imports(JsonObject definition) {
        final Map<Path, JsonObject> definitions = new LinkedHashMap<>();
        definitions.put(IMPORT, definition);
        return definitions;
    }

    @Test
    @DisplayName("expands a variable declared in the same preset")
    void expandsLocalVariable() {
        final JsonObject preset = new JsonObject()
                .add("HEIGHT", 40)
                .add("variables", new JsonObject().add("HEIGHT", 40))
                .add("count", "$HEIGHT");
        final JsonObject result = expand(preset, new LinkedHashMap<>());
        assertThat(result.get("count").asInt()).isEqualTo(40);
    }

    @Test
    @DisplayName("imports pull variables in from another file")
    void expandsImportedVariable() {
        final JsonObject preset = new JsonObject().add("imports", "shared.cave").add("count", "$SHARED");
        final JsonObject result = expand(preset, imports(new JsonObject().add("SHARED", 12)));
        assertThat(result.get("count").asInt()).isEqualTo(12);
    }

    @Test
    @DisplayName("`as` renames the imported variable")
    void supportsImportRename() {
        final JsonObject preset = new JsonObject()
                .add("imports", "shared.cave::SHARED as LOCAL")
                .add("count", "$LOCAL");
        final JsonObject result = expand(preset, imports(new JsonObject().add("SHARED", 5)));
        assertThat(result.get("count").asInt()).isEqualTo(5);
    }

    @Test
    @DisplayName("arithmetic in a value is evaluated after variables are substituted")
    void evaluatesArithmetic() {
        final JsonObject preset = new JsonObject()
                .add("BASE", 10)
                .add("variables", new JsonObject().add("BASE", 10))
                .add("count", "$BASE * 3");
        final JsonObject result = expand(preset, new LinkedHashMap<>());
        assertThat(result.get("count").asInt()).isEqualTo(30);
    }

    @Test
    @DisplayName("imports and variables are stripped once expansion finishes")
    void deletesScaffolding() {
        final JsonObject preset = new JsonObject().add("imports", "shared.cave").add("count", "$SHARED");
        final JsonObject result = expand(preset, imports(new JsonObject().add("SHARED", 1)));
        assertThat(result.get("imports")).isNull();
        assertThat(result.get("variables")).isNull();
    }

    @Test
    @DisplayName("`*key` overrides that key on every descendant that already has it")
    void appliesRecursiveOverride() {
        final JsonObject tunnels = new JsonObject().add("count", 1);
        final JsonObject ravines = new JsonObject().add("count", 2);
        final JsonObject preset = new JsonObject().add("*count", 99).add("tunnels", tunnels).add("ravines", ravines);

        final JsonObject result = expand(preset, new LinkedHashMap<>());

        assertThat(result.get("tunnels").asObject().get("count").asInt()).isEqualTo(99);
        assertThat(result.get("ravines").asObject().get("count").asInt()).isEqualTo(99);
        // The directive itself must not survive into the parsed preset.
        assertThat(result.names().contains("*count")).isFalse();
    }

    @Test
    @DisplayName("an import's `variables` block stays private to that import")
    void importVariablesArePrivate() {
        // The import can use NESTED to build its own exported value...
        final JsonObject definition = new JsonObject()
                .add("variables", new JsonObject().add("NESTED", 8))
                .add("EXPORTED", "$NESTED");
        final JsonObject preset = new JsonObject().add("imports", "shared.cave").add("count", "$EXPORTED");
        final JsonValue count = expand(preset, imports(definition)).get("count");
        assertThat(count.asInt()).isEqualTo(8);

        // ...but the importer cannot reach NESTED itself.
        final JsonObject leaky = new JsonObject().add("imports", "shared.cave").add("count", "$NESTED");
        assertThatThrownBy(() -> expand(leaky,
                imports(new JsonObject().add("variables",
                        new JsonObject().add("NESTED", 8))))).isInstanceOf(RuntimeException.class);
    }

}
