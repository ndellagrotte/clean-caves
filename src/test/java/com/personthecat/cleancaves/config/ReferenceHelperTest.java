package com.personthecat.cleancaves.config;

import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(substitute(definitions, "$NUM")).isEqualTo("42");
    }

    @Test
    @DisplayName("leaves strings with no references untouched")
    void ignoresPlainStrings() {
        final JsonObject definitions = new JsonObject().add("NUM", 42);
        assertThat(ReferenceHelper.trySubstitute(definitions, "minecraft:stone").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("an escaped \\$ is not a reference")
    void respectsEscape() {
        final JsonObject definitions = new JsonObject().add("NUM", 42);
        final Optional<JsonValue> result = ReferenceHelper.trySubstitute(definitions, "\\$NUM");
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("substitutes function arguments in order")
    void substitutesFunctionArguments() {
        final JsonObject definitions = new JsonObject().add("ADD()", "[@1, @2]");
        assertThat(substitute(definitions, "$ADD(1, 2)").replace(" ", "")).isEqualTo("[1,2]");
    }

    @Test
    @DisplayName("an omitted optional argument falls back to its default")
    void usesArgumentDefault() {
        final JsonObject definitions = new JsonObject().add("VAL()", "@1?(7)");
        assertThat(substitute(definitions, "$VAL()")).isEqualTo("7");
        assertThat(substitute(definitions, "$VAL(3)")).isEqualTo("3");
    }

    @Test
    @DisplayName("readValue resolves a variable declared as a function")
    void readValueFallsBackToFunctionForm() {
        final JsonObject definitions = new JsonObject().add("SHAPE()", "round");
        assertThat(ReferenceHelper.readValue(definitions, "SHAPE").asString()).isEqualTo("round");
    }

    @Test
    @DisplayName("an undeclared variable is an error, not a silent empty value")
    void undeclaredVariableThrows() {
        final JsonObject definitions = new JsonObject();
        assertThatThrownBy(() -> ReferenceHelper.readValue(definitions, "NOPE")).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("arguments need no space after the comma")
    void readsArgumentsWithoutSpaces() {
        final JsonObject definitions = new JsonObject().add("ADD()", "[@1, @2]");
        assertThat(substitute(definitions, "$ADD(1,2)").replace(" ", "")).isEqualTo("[1,2]");
        assertThat(substitute(definitions, "$ADD(12,34)").replace(" ", "")).isEqualTo("[12,34]");
    }

    @Test
    @DisplayName("quoted arguments are read whole, with or without a space between them")
    void readsQuotedArguments() {
        final JsonObject definitions = new JsonObject().add("PAIR()", "[@1, @2]");
        assertThat(substitute(definitions, "$PAIR('12','34')").replace(" ", "")).isEqualTo("[12,34]");
        assertThat(substitute(definitions, "$PAIR('12', '34')").replace(" ", "")).isEqualTo("[12,34]");
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @DisplayName("variables that refer to each other are reported instead of expanding forever")
    void detectsCyclicalReferences() {
        final JsonObject definitions = new JsonObject().add("A", "$B").add("B", "$A");
        assertThatThrownBy(() -> ReferenceHelper.trySubstitute(definitions, "$A"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cyclical reference");
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @DisplayName("a variable that expands to more of itself is reported too")
    void detectsGrowingReferences() {
        final JsonObject definitions = new JsonObject().add("A", "$A x");
        assertThatThrownBy(() -> ReferenceHelper.trySubstitute(definitions, "$A"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cyclical reference");
    }

}
