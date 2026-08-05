package com.personthecat.cleancaves.config;

import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The {@code $VARIABLE} and {@code $FUNCTION(args)} syntax that preset files are built around. */
class ReferenceHelperTest {

    private static String substitute(JsonObject definitions, String expression) {
        return ReferenceHelper.trySubstitute(definitions, expression)
            .map(JsonValue::toString)
            .orElseThrow(() -> new AssertionError("Expected a substitution for: " + expression));
    }

    @Test
    @DisplayName("substitutes a plain $VARIABLE")
    void substitutesVariable() {
        final JsonObject definitions = new JsonObject().add("NUM", 42);
        assertEquals("42", substitute(definitions, "$NUM"));
    }

    @Test
    @DisplayName("leaves strings with no references untouched")
    void ignoresPlainStrings() {
        final JsonObject definitions = new JsonObject().add("NUM", 42);
        assertTrue(ReferenceHelper.trySubstitute(definitions, "minecraft:stone").isEmpty());
    }

    @Test
    @DisplayName("an escaped \\$ is not a reference")
    void respectsEscape() {
        final JsonObject definitions = new JsonObject().add("NUM", 42);
        final Optional<JsonValue> result = ReferenceHelper.trySubstitute(definitions, "\\$NUM");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("substitutes function arguments in order")
    void substitutesFunctionArguments() {
        final JsonObject definitions = new JsonObject().add("ADD()", "[@1, @2]");
        assertEquals("[1,2]", substitute(definitions, "$ADD(1, 2)").replace(" ", ""));
    }

    @Test
    @DisplayName("an omitted optional argument falls back to its default")
    void usesArgumentDefault() {
        final JsonObject definitions = new JsonObject().add("VAL()", "@1?(7)");
        assertEquals("7", substitute(definitions, "$VAL()"));
        assertEquals("3", substitute(definitions, "$VAL(3)"));
    }

    @Test
    @DisplayName("readValue resolves a variable declared as a function")
    void readValueFallsBackToFunctionForm() {
        final JsonObject definitions = new JsonObject().add("SHAPE()", "round");
        assertEquals("round", ReferenceHelper.readValue(definitions, "SHAPE").asString());
    }

    @Test
    @DisplayName("an undeclared variable is an error, not a silent empty value")
    void undeclaredVariableThrows() {
        final JsonObject definitions = new JsonObject();
        assertThrows(RuntimeException.class, () -> ReferenceHelper.readValue(definitions, "NOPE"));
    }
}
