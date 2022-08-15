/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.fields;

import network.aika.neuron.activation.Element;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;

/**
 * @author Lukas Molzberger
 */
public class Fields {

    public static boolean isTrue(FieldOutput f) {
        return f != null && f.isInitialized() && f.getCurrentValue() > 0.5;
    }

    public static void reconnect(FieldLink fl, Field newInput) {
        fl.getInput().removeOutput(fl, true);
        newInput.addOutput(fl, true);
    }

    public static FieldLink connect(FieldOutput in, FieldInput out) {
        return connect(in, out.getNextArg(), out);
    }

    public static FieldLink connect(FieldOutput in, FieldInput out, boolean propagateInitialValue) {
        return connect(in, out.getNextArg(), out, propagateInitialValue);
    }

    public static FieldLink connect(FieldOutput in, int arg, FieldInput out) {
        return connect(in, arg, out, true);
    }

    public static FieldLink connect(FieldOutput in, int arg, FieldInput out, boolean propagateInitialValue) {
        FieldLink fl = new FieldLink(in, arg, out);
        out.addInput(fl);
        in.addOutput(fl, propagateInitialValue);
        return fl;
    }

    private static void connectAll(FieldOutput in, FieldInput... out) {
        assert in != null;

        for(FieldInput o : out) {
            if(o != null) {
                connect(in, 0, o);
            }
        }
    }

    public static void disconnect(FieldOutput in, FieldInput out) {
        disconnect(in, 0, out);
    }

    public static void disconnect(FieldOutput in, int arg, FieldInput out) {
        FieldLink l = new FieldLink(in, arg, out);
        out.removeInput(l);
        in.removeOutput(l, false);
    }

    public static Addition add(Element ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Addition add = new Addition(ref, label);
        connect(in1, 1, add);
        connect(in2, 2, add);
        return add;
    }

    public static Addition add(Element ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Addition add = add(ref, label, in1, in2);
        connectAll(add, out);
        return add;
    }

    public static Multiplication mul(Element ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Multiplication mul = new Multiplication(ref, label);
        connect(in1, 1, mul);
        connect(in2, 2, mul);
        return mul;
    }

    public static Multiplication mul(Element ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Multiplication mul = mul(ref, label, in1, in2);
        connectAll(mul, out);
        return mul;
    }

    public static Division div(Element ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Division div = new Division(ref, label);
        connect(in1, 1, div);
        connect(in2, 2, div);
        return div;
    }

    public static Division div(Element ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Division div = div(ref, label, in1, in2);
        connectAll(div, out);
        return div;
    }


    public static FieldFunction pow(Element ref, FieldOutput a, double b) {
        return func(
                ref,
                "pow(a, b)",
                a,
                x -> Math.pow(x, b)
        );
    }

    public static FieldFunction func(Element ref, String label, FieldOutput in, DoubleFunction<Double> f) {
        if(in == null)
            return null;

        FieldFunction func = new FieldFunction(ref, label, f);
        connect(in, func);
        return func;
    }

    public static FieldFunction func(Element ref, String label, FieldOutput in, DoubleFunction<Double> f, FieldInput... out) {
        if(in == null)
            return null;

        FieldFunction func = func(ref, label, in, f);
        connectAll(func, out);
        return func;
    }

    public static BiFunction func(Element ref, String label, FieldOutput in1, FieldOutput in2, DoubleBinaryOperator f) {
        if(in1 == null || in2 == null)
            return null;

        BiFunction func = new BiFunction(ref, label, f);
        connect(in1, 1, func);
        connect(in2, 2, func);
        return func;
    }

    public static BiFunction func(Element ref, String label, FieldOutput in1, FieldOutput in2, DoubleBinaryOperator f, FieldInput... out) {
        BiFunction func = func(ref,label, in1, in2, f);
        connectAll(func, out);
        return func;
    }

    public static ThresholdOperator threshold(Element ref, String label, double threshold, ThresholdOperator.Type type, FieldOutput in) {
        if(in == null)
            return null;

        ThresholdOperator op = new ThresholdOperator(ref, label, threshold, type);
        connect(in, op);
        return op;
    }

    public static ThresholdOperator threshold(Element ref, String label, double threshold, ThresholdOperator.Type type, FieldOutput in, FieldInput... out) {
        ThresholdOperator op = threshold(ref, label, threshold, type, in, out);
        connectAll(op, out);
        return op;
    }

    public static InvertFunction invert(String label, FieldOutput in) {
        if(in == null)
            return null;

        InvertFunction f = new InvertFunction(label);
        connect(in, f);
        return f;
    }

    public static ScaleFunction scale(Element ref, String label, double scale, FieldOutput in) {
        if(in == null)
            return null;

        ScaleFunction f = new ScaleFunction(ref, label, scale);
        connect(in, f);
        return f;
    }

    public static ScaleFunction scale(Element ref, String label, double scale, FieldOutput in, FieldInput... out) {
        ScaleFunction f = scale(ref, label, scale, in);
        connectAll(f, out);
        return f;
    }
}
