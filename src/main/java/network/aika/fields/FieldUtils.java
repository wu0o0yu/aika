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

import java.util.function.BooleanSupplier;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;

/**
 * @author Lukas Molzberger
 */
public class FieldUtils {

    public static FieldMultiplication mul(String label, DoubleFieldOutput in1, DoubleFieldOutput in2) {
        return new FieldMultiplication(label, in1, true, in2, true);
    }

    public static FieldMultiplication mul(String label, DoubleFieldOutput in1, DoubleFieldOutput in2, DoubleFieldInput out) {
        FieldMultiplication func = new FieldMultiplication(label, in1, true, in2, true);
        func.registerOutputs(out);
        return func;
    }

    public static FieldMultiplication mulUnregistered(String label, DoubleFieldOutput in1, DoubleFieldOutput in2) {
        return new FieldMultiplication(label, in1, false, in2, false);
    }

    public static FieldMultiplication mulUnregistered(String label, DoubleFieldOutput in1, DoubleFieldOutput in2, DoubleFieldInput... out) {
        FieldMultiplication func = new FieldMultiplication(label, in1, false, in2, false);
        func.registerOutputs(out);
        return func;
    }

    public static FieldDivision div(String label, DoubleFieldOutput in1, DoubleFieldOutput in2) {
        return new FieldDivision(label, in1, true, in2, true);
    }

    public static FieldDivision div(String label, DoubleFieldOutput in1, DoubleFieldOutput in2, DoubleFieldInput... out) {
        FieldDivision func = new FieldDivision(label, in1, true, in2, true);
        func.registerOutputs(out);
        return func;
    }

    public static FieldFunction func(String label, DoubleFieldOutput in, DoubleFunction<Double> f) {
        return new FieldFunction(label, in, f);
    }

    public static FieldFunction func(String label, DoubleFieldOutput in, DoubleFunction<Double> f, DoubleFieldInput... out) {
        return new FieldFunction(label, in, f, out);
    }

    public static BiFunction func(String label, DoubleFieldOutput in1, DoubleFieldOutput in2, DoubleBinaryOperator f) {
        return new BiFunction(label, in1, true, in2, true, f);
    }

    public static BiFunction func(String label, DoubleFieldOutput in1, DoubleFieldOutput in2, DoubleBinaryOperator f, DoubleFieldInput... out) {
        BiFunction func = new BiFunction(label, in1, true, in2, true, f);
        func.registerOutputs(out);
        return func;
    }

    public static SwitchField switchField(String label, DoubleFieldInterface in1, DoubleFieldInterface in2, BooleanSupplier test) {
        return new SwitchField(label, in1, false, in2, false, test);
    }

    public static ThresholdOperator threshold(String label, double threshold, DoubleFieldOutput in, BooleanFieldInput... out) {
        return new ThresholdOperator(label, threshold, in, out);
    }

}
