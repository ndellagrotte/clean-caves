package com.personthecat.cleancaves.config;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hjson lets quotes be omitted, which makes a stray comma, semicolon, or comment part of the
 * string instead of a syntax error. These checks turn that into a message the user can act on.
 */
class SyntaxHelperTest {

    private static final Path FILE = Paths.get("example.cave");

    @Test
    @DisplayName("a trailing comma inside an unquoted string is reported")
    void rejectsTrailingComma() {
        final JsonObject json = new JsonObject().add("state", "minecraft:stone,");
        assertThatThrownBy(() -> SyntaxHelper.check(FILE, json)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("a trailing semicolon is reported")
    void rejectsSemicolon() {
        final JsonObject json = new JsonObject().add("state", "minecraft:stone;");
        assertThatThrownBy(() -> SyntaxHelper.check(FILE, json)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("an embedded comment marker is reported")
    void rejectsEmbeddedComment() {
        assertThatThrownBy(() -> SyntaxHelper.check(FILE,
                new JsonObject().add("state", "stone # comment"))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> SyntaxHelper.check(FILE,
                new JsonObject().add("state", "stone // comment"))).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("nested objects and arrays are checked too")
    void checksNestedValues() {
        final JsonObject nested = new JsonObject().add("decorators",
                new JsonArray().add(new JsonObject().add("state", "stone,")));
        assertThatThrownBy(() -> SyntaxHelper.check(FILE, nested)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("well-formed presets pass")
    void acceptsValidPreset() {
        final JsonObject json = new JsonObject()
                .add("enabled", true)
                .add("state", "minecraft:stone")
                .add("height", new JsonArray().add(0).add(50))
                .add("tunnels", new JsonArray().add(new JsonObject().add("count", 15)));
        assertThatCode(() -> SyntaxHelper.check(FILE, json)).doesNotThrowAnyException();
    }

}
