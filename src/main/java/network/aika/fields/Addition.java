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

/**
 * @author Lukas Molzberger
 */
public class Addition extends AbstractBiFunction {

    public Addition(Element ref, String label) {
        super(ref, label);
    }

    @Override
    public double getCurrentValue() {
        return FieldOutput.getCurrentValue(in1) + FieldOutput.getCurrentValue(in2);
    }

    @Override
    protected double getCurrentValue(FieldLink fl, double inputCV) {
        switch (fl.getArgument()) {
            case 1:
                return inputCV + in2.getInput().getCurrentValue();
            case 2:
                return in1.getInput().getCurrentValue() + inputCV;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    protected double computeUpdate(FieldLink fl, double inputCV, double ownCV, double u) {
        return u;
    }
}
