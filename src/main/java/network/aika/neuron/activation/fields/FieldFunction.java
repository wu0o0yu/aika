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
package network.aika.neuron.activation.fields;

import java.util.function.Function;

/**
 * @author Lukas Molzberger
 */
public class FieldFunction implements FieldOutput {

    FieldOutput input;
    Function<Double, Double> function;

    public FieldFunction(FieldOutput in, Function<Double, Double> f) {
        this.input = in;
        this.function = f;
    }

    @Override
    public double getCurrentValue() {
        return function.apply(input.getCurrentValue());
    }

    @Override
    public double getNewValue() {
        return function.apply(input.getNewValue());
    }

    @Override
    public boolean updateAvailable() {
        return input.updateAvailable();
    }

    @Override
    public double getUpdate() {
        return getNewValue() - getCurrentValue();
    }

    @Override
    public void acknowledgePropagated() {
        input.acknowledgePropagated();
    }
}
