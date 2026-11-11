package com.personthecat.cleancaves.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Presets may write arithmetic in place of any numeric field; this is what evaluates it. */
class CalculatorTest {

    @ParameterizedTest
    @CsvSource({
                    "'1 + 1', 2", "'2 * 3', 6", "'10 - 4', 6", "'9 / 3', 3", "'2 ^ 8', 256", "'1 + 2 * 3', 7", // precedence, not left-to-right
                    "'(1 + 2) * 3', 9", "'2 * (3 + 4) - 5', 9", "'-5 + 8', 3", // leading sign
                    "'0.5 * 4', 2", "'2(3)', 6", // implicit multiplication before a paren
                    "'((2))', 2"
            })
    @DisplayName("evaluates arithmetic with the expected precedence")
    void evaluates(String expression, double expected) {
        assertThat(Calculator.evaluate(expression)).isCloseTo(expected, within(1e-9));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1 + 1", "2*3", "(4 + 5)", "0.5", "12", "-3 + 1" })
    @DisplayName("recognises strings that are arithmetic")
    void isExpression(String expression) {
        assertThat(Calculator.isExpression(expression)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "minecraft:stone", "ALL", "$VANILLA", "a + b" })
    @DisplayName("leaves non-arithmetic strings alone")
    void isNotExpression(String value) {
        assertThat(Calculator.isExpression(value)).isFalse();
    }

    @Test
    @DisplayName("reports malformed expressions instead of returning nonsense")
    void rejectsMalformed() {
        assertThatThrownBy(() -> Calculator.evaluate("1 +")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> Calculator.evaluate("* 2")).isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @CsvSource({ "'.5', 0.5", "'2 * .5', 1", "'.25 + .75', 1", "'5.', 5" })
    @DisplayName("a number may start or end with its decimal point")
    void evaluatesBareDecimalPoints(String expression, double expected) {
        assertThat(Calculator.isExpression(expression)).isTrue();
        assertThat(Calculator.evaluate(expression)).isCloseTo(expected, within(1e-9));
    }

    @ParameterizedTest
    @CsvSource({ "'-(2 + 3)', -5", "'2 * -(3 + 1)', -8", "'-(-(4))', 4", "'10 - (2 + 3)', 5", "'-(2) + 7', 5" })
    @DisplayName("a sign in front of a group negates the whole group")
    void negatesGroups(String expression, double expected) {
        assertThat(Calculator.evaluate(expression)).isCloseTo(expected, within(1e-9));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   ", "1.0.0", "1..5", ".", "-", "()", "+ -" })
    @DisplayName("text made of arithmetic characters but holding no valid number is left alone")
    void isNotExpressionWithoutANumber(String value) {
        assertThat(Calculator.isExpression(value)).isFalse();
    }

    @Test
    @DisplayName("a number with two decimal points is malformed")
    void rejectsDoubleDecimal() {
        assertThatThrownBy(() -> Calculator.evaluate("1.5.2 + 1")).isInstanceOf(RuntimeException.class);
    }

}
