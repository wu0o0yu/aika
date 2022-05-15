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

    public BiFunction(String label, DoubleBinaryOperator f) {
        super(label);
        this.function = f;
    }

    @Override
    public double getCurrentValue() {
        return function.applyAsDouble(in1.getInput().getCurrentValue(), in2.getInput().getCurrentValue());
    }

    @Override
    protected double computeUpdate(int arg, double inputCV, double ownCV, double u) {
        if(isInitialized())
            return computeNewValue(arg, inputCV, u) - getCurrentValue(arg, inputCV);
        else
            return computeNewValue(arg, inputCV, u);
    }

    @Override
    protected double getCurrentValue(int arg, double inputCV) {
        switch (arg) {
            case 1:
                return function.applyAsDouble(inputCV, in2.getInput().getCurrentValue());
            case 2:
                return function.applyAsDouble(in1.getInput().getCurrentValue(), inputCV);
            default:
                throw new IllegalArgumentException();
        }
    }

    private double computeNewValue(int arg, double inputCV, double u) {
        switch (arg) {
            case 1:
                return function.applyAsDouble(u + inputCV, FieldOutput.getCurrentValue(in2));
            case 2:
                return function.applyAsDouble(FieldOutput.getCurrentValue(in1), u + inputCV);
            default:
                throw new IllegalArgumentException();
        }
    }
}
