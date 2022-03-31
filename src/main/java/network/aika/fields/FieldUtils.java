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
public class FieldUtils {

    public static FieldMultiplication mul(String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        return new FieldMultiplication(label, in1, in2);
    }

    public static FieldMultiplication mul(String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        if(in1 == null || in2 == null)
            return null;

        FieldMultiplication func = new FieldMultiplication(label, in1, in2);
        func.addFieldsAsAdditiveReceivers(out);
        return func;
    }

    public static FieldDivision div(String label, FieldOutput in1, FieldOutput in2) {
        if(in1 == null || in2 == null)
            return null;

        return new FieldDivision(label, in1, in2);
    }

    public static FieldDivision div(String label, FieldOutput in1, FieldOutput in2, FieldInput... out) {
        if(in1 == null || in2 == null)
            return null;

        FieldDivision func = new FieldDivision(label, in1, in2);
        func.addFieldsAsAdditiveReceivers(out);
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

        return new FieldFunction(label, in, f, out);
    }

    public static BiFunction func(String label, FieldOutput in1, FieldOutput in2, DoubleBinaryOperator f, FieldInput... out) {
        if(in1 == null || in2 == null)
            return null;

        BiFunction func = new BiFunction(label, in1, in2, f);
        func.addFieldsAsAdditiveReceivers(out);
        return func;
    }

    public static FieldConnect connect(String label, FieldOutput in, FieldInput... out) {
        if(in == null)
            return null;

        return new FieldConnect(label, in, out);
    }

    public static ThresholdOperator threshold(String label, double threshold, FieldOutput in, FieldInput... out) {
        if(in == null)
            return null;

        return new ThresholdOperator(label, threshold, in, out);
    }
}
