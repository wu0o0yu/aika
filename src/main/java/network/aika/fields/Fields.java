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

import network.aika.FieldObject;

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

    public static Addition add(FieldObject ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Addition add = new Addition(ref, label);
        link(in1, 0, add);
        link(in2, 1, add);

        return add;
    }

    public static Addition add(FieldObject ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Addition add = add(ref, label, in1, in2);
        linkAll(add, out);
        return add;
    }

    public static Subtraction sub(FieldObject ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Subtraction sub = new Subtraction(ref, label);
        link(in1, 0, sub);
        link(in2, 1, sub);

        return sub;
    }

    public static Subtraction sub(FieldObject ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Subtraction sub = sub(ref, label, in1, in2);
        linkAll(sub, out);
        return sub;
    }

    public static MixFunction mix(FieldObject ref, String label, FieldOutput x, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        MixFunction mix = new MixFunction(ref, label);
        link(x, 0, mix);
        link(in1, 1, mix);
        link(in2, 2, mix);

        return mix;
    }

    public static MixFunction mix(FieldObject ref, String label, FieldOutput x, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        MixFunction mix = mix(ref, label, x, in1, in2);
        linkAll(mix, out);
        return mix;
    }

    public static Multiplication mul(FieldObject ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Multiplication mul = new Multiplication(ref, label);
        link(in1, 0, mul);
        link(in2, 1, mul);

        return mul;
    }

    public static Multiplication mul(FieldObject ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Multiplication mul = mul(ref, label, in1, in2);
        linkAll(mul, out);
        return mul;
    }

    public static Division div(FieldObject ref, String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        Division div = new Division(ref, label);
        link(in1, 0, div);
        link(in2, 1, div);

        return div;
    }

    public static Division div(FieldObject ref, String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Division div = div(ref, label, in1, in2);
        linkAll(div, out);
        return div;
    }

    public static FieldFunction func(FieldObject ref, String label, FieldOutput in, DoubleFunction<Double> f) {
        if(in == null)
            return null;

        FieldFunction func = new FieldFunction(ref, label, f);
        link(in, 0, func);
        return func;
    }

    public static FieldFunction func(FieldObject ref, String label, FieldOutput in, DoubleFunction<Double> f, FieldInput... out) {
        if(in == null)
            return null;

        FieldFunction func = func(ref, label, in, f);
        linkAll(func, out);
        return func;
    }

    public static BiFunction func(FieldObject ref, String label, FieldOutput in1, FieldOutput in2, DoubleBinaryOperator f) {
        if(in1 == null || in2 == null)
            return null;

        BiFunction func = new BiFunction(ref, label, f);
        link(in1, 0, func);
        link(in2, 1, func);

        return func;
    }

    public static BiFunction func(FieldObject ref, String label, FieldOutput in1, FieldOutput in2, DoubleBinaryOperator f, FieldInput... out) {
        BiFunction func = func(ref, label, in1, in2, f);
        linkAll(func, out);
        return func;
    }

    public static ThresholdOperator threshold(FieldObject ref, String label, double threshold, ThresholdOperator.Type type, FieldOutput in) {
        if(in == null)
            return null;

        ThresholdOperator op = new ThresholdOperator(ref, label, threshold, type);
        link(in, 0, op);
        return op;
    }

    public static ThresholdOperator threshold(FieldObject ref, String label, double threshold, ThresholdOperator.Type type, boolean isFinal, FieldOutput in) {
        if(in == null)
            return null;

        ThresholdOperator op = new ThresholdOperator(ref, label, threshold, type, isFinal);
        link(in, 0, op);
        return op;
    }
/*
    public static ThresholdOperator threshold(FieldObject ref, String label, double threshold, ThresholdOperator.Type type, boolean isFinal, FieldOutput in, FieldInput... out) {
        ThresholdOperator op = threshold(ref, label, threshold, type, isFinal, in, out);
        linkAll(op, out);
        return op;
    }
*/
    public static InvertFunction invert(FieldObject ref, String label, FieldOutput in) {
        if(in == null)
            return null;

        InvertFunction f = new InvertFunction(ref, label);
        link(in, 0, f);
        return f;
    }

    public static ScaleFunction scale(FieldObject ref, String label, double scale, FieldOutput in) {
        if(in == null)
            return null;

        ScaleFunction f = new ScaleFunction(ref, label, scale);
        link(in, 0, f);
        return f;
    }

    public static ScaleFunction scale(FieldObject ref, String label, double scale, FieldOutput in, FieldInput... out) {
        ScaleFunction f = scale(ref, label, scale, in);
        linkAll(f, out);
        return f;
    }
}
