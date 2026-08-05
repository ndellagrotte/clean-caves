package com.personthecat.cleancaves.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Presets may write arithmetic in place of any numeric field; this is what evaluates it. */
class CalculatorTest {

    @ParameterizedTest
    @CsvSource({
        "'1 + 1', 2",
        "'2 * 3', 6",
        "'10 - 4', 6",
        "'9 / 3', 3",
        "'2 ^ 8', 256",
        "'1 + 2 * 3', 7",          // precedence, not left-to-right
        "'(1 + 2) * 3', 9",
        "'2 * (3 + 4) - 5', 9",
        "'-5 + 8', 3",             // leading sign
        "'0.5 * 4', 2",
        "'2(3)', 6",               // implicit multiplication before a paren
        "'((2))', 2"
    })
    @DisplayName("evaluates arithmetic with the expected precedence")
    void evaluates(String expression, double expected) {
        assertEquals(expected, Calculator.evaluate(expression), 1e-9);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 + 1", "2*3", "(4 + 5)", "0.5", "12", "-3 + 1"})
    @DisplayName("recognises strings that are arithmetic")
    void isExpression(String expression) {
        assertTrue(Calculator.isExpression(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"minecraft:stone", "ALL", "$VANILLA", "a + b"})
    @DisplayName("leaves non-arithmetic strings alone")
    void isNotExpression(String value) {
        assertFalse(Calculator.isExpression(value));
    }

    @Test
    @DisplayName("reports malformed expressions instead of returning nonsense")
    void rejectsMalformed() {
        assertThrows(RuntimeException.class, () -> Calculator.evaluate("1 +"));
        assertThrows(RuntimeException.class, () -> Calculator.evaluate("* 2"));
    }
}
