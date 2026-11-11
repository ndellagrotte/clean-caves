package com.personthecat.cleancaves.config;

import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Parses the presets that ship inside the jar. Nothing here touches Minecraft, so a broken preset
 * is caught by {@code ./gradlew test} instead of at world load, where the only symptom is a crash
 * or a preset that silently generates nothing.
 */
class ShippedPresetsTest {

    private static final Path ASSETS = Paths.get("src", "main", "resources", "assets", "cleancaves");
    private static final Path PRESETS = ASSETS.resolve("presets");
    private static final Path EXAMPLES = ASSETS.resolve("examples");
    private static final Path IMPORTS = ASSETS.resolve("imports");

    /** Mirrors the pattern {@link ReferenceHelper} uses, so a leftover match is an unbound name. */
    private static final Pattern REFERENCE = Pattern.compile("(?<!\\\\)\\$(?!\\d)(\\w+)");

    /** The extensions {@code CaveInit} treats as presets. Duplicated to avoid loading Forge. */
    private static final List<String> EXTENSIONS = List.of(".hjson", ".json", ".cave");

    @BeforeAll
    static void assetsArePresent() {
        assertThat(Files.isDirectory(PRESETS))
                .as(PRESETS + " is missing. Run the tests from the project root.")
                .isTrue();
        assertThat(Files.isDirectory(EXAMPLES)).as(EXAMPLES + " is missing").isTrue();
        assertThat(Files.isDirectory(IMPORTS)).as(IMPORTS + " is missing").isTrue();
    }

    private static Map<Path, JsonObject> load(Path dir) {
        final Map<Path, JsonObject> jsons = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(dir)) {
            files.filter(Files::isRegularFile)
                    .filter(f -> EXTENSIONS.stream().anyMatch(f.getFileName().toString()::endsWith))
                    .sorted()
                    .forEach(f -> jsons.put(f, parse(f)));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return jsons;
    }

    private static JsonObject parse(Path file) {
        try {
            return JsonValue.readHjson(Files.readString(file, StandardCharsets.UTF_8)).asObject();
        } catch (final IOException e) {
            throw new UncheckedIOException("Reading " + file, e);
        } catch (final RuntimeException e) {
            throw new AssertionError("Could not parse " + file + ": " + e.getMessage(), e);
        }
    }

    /** Collects every string still holding a {@code $reference} after expansion. */
    private static void findReferences(String path, JsonValue value, List<String> into) {
        if (value.isObject()) {
            value.asObject().forEach(m -> findReferences(path + "." + m.getName(), m.getValue(), into));
        } else if (value.isArray()) {
            int i = 0;
            for (final JsonValue element : value.asArray()) {
                findReferences(path + "[" + i++ + "]", element, into);
            }
        } else if (value.isString() && REFERENCE.matcher(value.asString()).find()) {
            into.add(path + " = " + value.asString());
        }
    }

    @Test
    @DisplayName("every shipped preset parses as hjson and passes the syntax check")
    void shippedPresetsParse() {
        final Map<Path, JsonObject> jsons = new LinkedHashMap<>(load(PRESETS));
        jsons.putAll(load(EXAMPLES));
        jsons.putAll(load(IMPORTS));

        assertThat(jsons.size()).as("expected 20 presets, 15 examples and 6 imports").isEqualTo(41);
        jsons.forEach((file, json) -> assertThatCode(() -> SyntaxHelper.check(file, json))
                .as(file.toString())
                .doesNotThrowAnyException());
    }

    @Test
    @DisplayName("every shipped preset expands with no unresolved references")
    void shippedPresetsExpand() {
        final Map<Path, JsonObject> presets = new LinkedHashMap<>(load(PRESETS));
        presets.putAll(load(EXAMPLES));
        final Map<Path, JsonObject> definitions = load(IMPORTS);

        assertThatCode(() -> PresetExpander.expandAll(presets, definitions)).doesNotThrowAnyException();

        final List<String> unresolved = new ArrayList<>();
        presets.forEach((file, json) -> findReferences(file.getFileName().toString(), json, unresolved));
        assertThat(unresolved.isEmpty()).as("unresolved references: " + unresolved).isTrue();
    }

}
