package com.personthecat.cleancaves.config;

import org.hjson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** An import expression names a file loosely, so which file it lands on has to be predictable. */
class ImportHelperTest {

    private static final Path IMPORTS = Paths.get("config", "cleancaves", "imports");

    private static Map<Path, JsonObject> definitions(String... files) {
        final Map<Path, JsonObject> definitions = new LinkedHashMap<>();
        for (final String file : files) {
            definitions.put(IMPORTS.resolve(file), new JsonObject().add("source", file));
        }
        return definitions;
    }

    private static String resolve(Map<Path, JsonObject> definitions, String expression) {
        return ImportHelper.findDefinition(definitions, expression)
                .map(json -> json.get("source").asString())
                .orElse(null);
    }

    @Test
    @DisplayName("a file whose name merely ends with the expression is not a match")
    void matchesWholeSegmentsOnly() {
        final Map<Path, JsonObject> definitions = definitions("all_types.cave", "types.cave");
        assertThat(resolve(definitions, "types.cave")).isEqualTo("types.cave");
        assertThat(resolve(definitions("all_types.cave"), "types.cave")).isNull();
    }

    @Test
    @DisplayName("the file directly in the imports folder wins over one in a subfolder")
    void prefersShallowestMatch() {
        final Map<Path, JsonObject> definitions = definitions("a/common.cave", "common.cave", "b/common.cave");
        assertThat(resolve(definitions, "common.cave")).isEqualTo("common.cave");
    }

    @Test
    @DisplayName("an expression can name the subfolder to pick a deeper file")
    void supportsSubfolderExpressions() {
        final Map<Path, JsonObject> definitions = definitions("a/common.cave", "common.cave", "b/common.cave");
        assertThat(resolve(definitions, "b/common.cave")).isEqualTo("b/common.cave");
    }

}
