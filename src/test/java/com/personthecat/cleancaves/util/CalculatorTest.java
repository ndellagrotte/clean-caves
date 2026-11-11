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

}
