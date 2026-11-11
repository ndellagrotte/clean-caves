package com.personthecat.cleancaves.util;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Pattern;

import static com.personthecat.cleancaves.util.CommonMethods.runExF;

/** An expression evaluator used for generating numeric field data in presets. */
public final class Calculator {

    /** Determines whether a string contains numbers and operations. */
    private static final Pattern IS_EXP = Pattern.compile("[\\d.]*\\s*([-+/*()^]\\s*[\\d.]*\\s*)*");

    /** A run of digits and dots that is not one number: two decimal points, or a point on its own. */
    private static final Pattern MALFORMED_NUMBER = Pattern.compile("\\.\\d*\\.|(?<!\\d)\\.(?!\\d)");

    /** Sits beneath an opening parenthesis on the operator stack when that group is to be negated. */
    private static final char NEGATE = '~';

    private Calculator() { }

    /**
     * Whether a string is arithmetic that {@link #evaluate} can take. Text that merely consists of
     * the right characters is not: an empty string has nothing to evaluate, and {@code 1.0.0} is a
     * version, not a number.
     */
    public static boolean isExpression(String exp) {
        return IS_EXP.matcher(exp).matches()
                && exp.chars().anyMatch(Character::isDigit)
                && !MALFORMED_NUMBER.matcher(exp).find();
    }

    public static double evaluate(String exp) {
        try {
            return evaluateInternal(exp);
        } catch (EmptyStackException | IllegalArgumentException e) {
            throw runExF("Invalid expression: {}", exp);
        }
    }

    private static double evaluateInternal(String exp) {
        final char[] tokens = exp.toCharArray();
        final Stack<Double> values = new Stack<>();
        final Stack<Character> ops = new Stack<>();
        boolean lastIsNumber = false;
        int sign = 1;

        for (int i = 0; i < tokens.length; i++) {
            final char token = tokens[i];
            if (token == ' ') {
                continue;
            }
            boolean number = isNumeric(token);

            if (number) {
                final StringBuilder buffer = new StringBuilder();

                boolean decimal = false;
                while (i < tokens.length && isNumeric(tokens[i])) {
                    final char c = tokens[i++];
                    if (c == '.') {
                        if (decimal) {
                            throw new IllegalArgumentException("Invalid decimal");
                        }
                        decimal = true;
                    }
                    buffer.append(c);
                }
                values.push(sign * Double.parseDouble(buffer.toString()));
                sign = 1;
                i--;
            } else if (token == '(') {
                if (lastIsNumber) {
                    ops.push('*');
                }
                // A pending sign applies to the whole group, not to the first number inside it.
                if (sign < 0) {
                    ops.push(NEGATE);
                    sign = 1;
                }
                ops.push(token);
            } else if (token == ')') {
                while (ops.peek() != '(') {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.pop();
                if (!ops.isEmpty() && ops.peek() == NEGATE) {
                    ops.pop();
                    values.push(-values.pop());
                }
                // A numeric value was extracted
                number = true;
            } else if (isOperator(token)) {
                if (!lastIsNumber) {
                    if (token == '-') {
                        sign = -sign;
                        continue;
                    } else if (token == '+') {
                        continue;
                    }
                }
                while (!ops.isEmpty() && hasPrecedence(token, ops.peek())) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(token);
            }
            lastIsNumber = number;
        }
        while (!ops.isEmpty()) {
            values.push(applyOp(ops.pop(), values.pop(), values.pop()));
        }
        return values.pop();
    }

    private static boolean isNumeric(char c) {
        return c == '.' || (c >= '0' && c <= '9');
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private static boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')') {
            return false;
        }
        if (op1 == '^') {
            return false;
        }
        return (op1 != '*' && op1 != '/') || (op2 != '+' && op2 != '-');
    }

    private static double applyOp(char op, double b, double a) {
        switch (op) {
            case '+' -> {
                return a + b;
            }
            case '-' -> {
                return a - b;
            }
            case '^' -> {
                return Math.pow(a, b);
            }
            case '*' -> {
                return a * b;
            }
            case '/' -> {
                if (b == 0.0) {
                    throw new UnsupportedOperationException("Divide by zero");
                }
                return a / b;
            }
            default -> throw new UnsupportedOperationException("operator: " + op);
        }
    }

}
