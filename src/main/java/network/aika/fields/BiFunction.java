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

/**
 * @author Lukas Molzberger
 */
public class BiFunction extends AbstractBiFunction {

    private DoubleBinaryOperator function;

    public BiFunction(String label, DoubleFieldOutput in1, boolean register1, DoubleFieldOutput in2, boolean register2, DoubleBinaryOperator f) {
        super(label, in1, register1, in2, register2);
        this.function = f;
    }

    @Override
    public double getCurrentValue() {
        return function.applyAsDouble(in1.getCurrentValue(), in2.getCurrentValue());
    }

    @Override
    public double getNewValue() {
        switch (currentArgument) {
            case 1:
                return function.applyAsDouble(in1.getNewValue(), in2.getCurrentValue());
            case 2:
                return function.applyAsDouble(in1.getCurrentValue(), in2.getNewValue());
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public double getUpdate() {
        return getNewValue() - getCurrentValue();
    }
}
