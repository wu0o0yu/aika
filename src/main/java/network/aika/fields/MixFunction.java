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
public class MixFunction extends Field implements FieldInput, FieldOutput {

    public MixFunction(Element ref, String label) {
        super(ref, label);
    }

    @Override
    public Element getReference() {
        return null;
    }

    public void receiveUpdate(FieldLink fl, double u) {
        propagateUpdate(
                computeUpdate(fl, u)
        );
    }

    protected double computeUpdate(FieldLink fl, double u) {
        int arg = fl.getArgument();
        if(arg == 0) {
            double a = getInputByArg(1).getCurrentValue();
            double b = getInputByArg(2).getCurrentValue();

            return (-u * a + u * b) / 2;
        } else {
            double x = getInputByArg(0).getCurrentValue();

            if(arg == 1)
                x = (1 - x);

            return (x * u) / 2;
        }
    }
}
