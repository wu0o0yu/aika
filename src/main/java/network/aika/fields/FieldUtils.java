package network.aika.fields;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;

public class FieldUtils {

    public static FieldMultiplication mul(String label, FieldOutput in1, FieldOutput in2) {
        return new FieldMultiplication(label, in1, true, in2, true);
    }

    public static FieldMultiplication mul(String label, FieldOutput in1, FieldOutput in2, FieldInput out) {
        return new FieldMultiplication(label, in1, true, in2, true, out);
    }

    public static FieldMultiplication mulUnregistered(String label, FieldOutput in1, FieldOutput in2) {
        return new FieldMultiplication(label, in1, false, in2, false);
    }

    public static FieldMultiplication mulUnregistered(String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        return new FieldMultiplication(label, in1, false, in2, false, out);
    }

    public static FieldDivision div(String label, FieldOutput in1, FieldOutput in2) {
        return new FieldDivision(label, in1, true, in2, true);
    }

    public static FieldDivision div(String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        return new FieldDivision(label, in1, true, in2, true, out);
    }

    public static FieldFunction func(String label, FieldOutput in, DoubleFunction<Double> f) {
        return new FieldFunction(label, in, f);
    }

    public static FieldFunction func(String label, FieldOutput in, DoubleFunction<Double> f, FieldInput... out) {
        return new FieldFunction(label, in, f, out);
    }

    public static BiFunction func(String label, FieldOutput in1, FieldOutput in2, DoubleBinaryOperator f) {
        return new BiFunction(label, in1, true, in2, true, f);
    }

    public static BiFunction func(String label, FieldOutput in1, FieldOutput in2, DoubleBinaryOperator f, FieldInput... out) {
        return new BiFunction(label, in1, true, in2, true, f);
    }
}
