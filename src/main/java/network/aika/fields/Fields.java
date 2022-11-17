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

import static network.aika.fields.FieldLink.*;

/**
 * @author Lukas Molzberger
 */
public class Fields {

    public static boolean isTrue(FieldOutput f) {
        return f != null && f.getCurrentValue() > 0.5;
    }

    public static Addition add(Element ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Addition add = new Addition(ref, label);
        FieldLink fl0 = link(in1, 0, add);
        FieldLink fl1 = link(in2, 1, add);

        fl0.connect();
        fl1.connect();

        return add;
    }

    public static Addition add(Element ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Addition add = add(ref, label, in1, in2);
        connectAll(add, out);
        return add;
    }

    public static Subtraction sub(Element ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Subtraction sub = new Subtraction(ref, label);
        FieldLink fl0 = link(in1, 0, sub);
        FieldLink fl1 = link(in2, 1, sub);

        fl0.connect();
        fl1.connect();

        return sub;
    }

    public static Subtraction sub(Element ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Subtraction sub = sub(ref, label, in1, in2);
        connectAll(sub, out);
        return sub;
    }

    public static MixFunction mix(Element ref, String label, FieldOutput x, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        MixFunction mix = new MixFunction(ref, label);
        FieldLink fl0 = link(x, 0, mix);
        FieldLink fl1 = link(in1, 1, mix);
        FieldLink fl2 = link(in2, 2, mix);

        fl0.connect();
        fl1.connect();
        fl2.connect();

        return mix;
    }

    public static MixFunction mix(Element ref, String label, FieldOutput x, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        MixFunction mix = mix(ref, label, x, in1, in2);
        connectAll(mix, out);
        return mix;
    }

    public static Multiplication mul(Element ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Multiplication mul = new Multiplication(ref, label);
        FieldLink fl0 = link(in1, 0, mul);
        FieldLink fl1 = link(in2, 1, mul);

        fl0.connect();
        fl1.connect();

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
        FieldLink fl0 = link(in1, 0, div);
        FieldLink fl1 = link(in2, 1, div);

        fl0.connect();
        fl1.connect();

        return div;
    }

    public static Division div(Element ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Division div = div(ref, label, in1, in2);
        connectAll(div, out);
        return div;
    }

    public static FieldFunction func(Element ref, String label, FieldOutput in, DoubleFunction<Double> f) {
        if(in == null)
            return null;

        FieldFunction func = new FieldFunction(ref, label, f);
        connect(in, 0, func);
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
        FieldLink fl0 = link(in1, 0, func);
        FieldLink fl1 = link(in2, 1, func);

        fl0.connect();
        fl1.connect();

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
        connect(in, 0, op);
        return op;
    }

    public static ThresholdOperator threshold(Element ref, String label, double threshold, ThresholdOperator.Type type, boolean isFinal, FieldOutput in) {
        if(in == null)
            return null;

        ThresholdOperator op = new ThresholdOperator(ref, label, threshold, type, isFinal);
        connect(in, 0, op);
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
        connect(in, 0, f);
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
