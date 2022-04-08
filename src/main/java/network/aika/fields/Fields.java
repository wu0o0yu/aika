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

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;

/**
 * @author Lukas Molzberger
 */
public class Fields {

    public static Addition add(String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        return new Addition(label, in1, in2);
    }

    public static Addition add(String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Addition func = add(label, in1, in2);
        func.addReceivers(out);
        return func;
    }

    public static Multiplication mul(String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        return new Multiplication(label, in1, in2);
    }

    public static Multiplication mul(String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Multiplication func = mul(label, in1, in2);
        func.addReceivers(out);
        return func;
    }

    public static Division div(String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        return new Division(label, in1, in2);
    }

    public static Division div(String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        Division func = div(label, in1, in2);
        func.addReceivers(out);
        return func;
    }

    public static FieldFunction func(String label, FieldOutput in, DoubleFunction<Double> f) {
        if(in == null)
            return null;

        return new FieldFunction(label, in, f);
    }

    public static FieldFunction func(String label, FieldOutput in, DoubleFunction<Double> f, FieldInput... out) {
        if(in == null)
            return null;

        FieldFunction func = func(label, in, f);
        func.addReceivers(out);
        return func;
    }

    public static BiFunction func(String label, FieldOutput in1, FieldOutput in2, DoubleBinaryOperator f, FieldInput... out) {
        if(in1 == null || in2 == null)
            return null;

        BiFunction func = new BiFunction(label, in1, in2, f);
        func.addReceivers(out);
        return func;
    }

    public static FieldConnect connect(String label, FieldOutput in) {
        if(in == null)
            return null;

        return new FieldConnect(label, in);
    }

    public static FieldConnect connect(String label, FieldOutput in, FieldInput... out) {
        FieldConnect fieldConnect = connect(label, in);
        fieldConnect.addReceivers(out);
        return fieldConnect;
    }

    public static ThresholdOperator threshold(String label, double threshold, FieldOutput in) {
        if(in == null)
            return null;

        return new ThresholdOperator(label, threshold, in);
    }

    public static ThresholdOperator threshold(String label, double threshold, FieldOutput in, FieldInput... out) {
        ThresholdOperator threshOp = threshold(label, threshold, in, out);
        threshOp.addReceivers(out);
        return threshOp;
    }

    public static InvertFunction invert(String label, FieldOutput in) {
        if(in == null)
            return null;

        return new InvertFunction(label, in);
    }

    public static ScaleFunction scale(String label, double scale, FieldOutput in) {
        if(in == null)
            return null;

        return new ScaleFunction(label, scale, in);
    }

    public static ScaleFunction scale(String label, double scale, FieldOutput in, FieldInput... out) {
        ScaleFunction scaleFunc = scale(label, scale, in);
        scaleFunc.addReceivers(out);
        return scaleFunc;
    }
}
