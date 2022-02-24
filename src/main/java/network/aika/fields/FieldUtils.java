package network.aika.fields;

import java.util.function.Function;

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

    public static FieldDivision div(String label, FieldOutput in1, FieldOutput in2) {
        return new FieldDivision(label, in1, true, in2, true);
    }

    public static FieldDivision div(String label, FieldOutput in1, FieldOutput in2, FieldInput out) {
        return new FieldDivision(label, in1, true, in2, true, out);
    }

    public static FieldFunction func(String label, FieldOutput in, Function<Double, Double> f) {
        return new FieldFunction(label, in, f);
    }

    public static FieldFunction func(String label, FieldOutput in, Function<Double, Double> f, FieldInput out) {
        return new FieldFunction(label, in, f, out);
    }
}
