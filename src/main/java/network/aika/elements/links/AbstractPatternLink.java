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
package network.aika.elements.links;

import network.aika.elements.activations.Activation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.synapses.AbstractPatternSynapse;

import static network.aika.fields.Fields.mul;

/**
 * @author Lukas Molzberger
 */
public abstract class AbstractPatternLink<S extends AbstractPatternSynapse, IA extends Activation<?>> extends ConjunctiveLink<S, IA, PatternActivation> {

    public AbstractPatternLink(S s, IA input, PatternActivation output) {
        super(s, input, output);
    }

    @Override
    protected void connectGradientFields() {
        initForwardsGradient();
        initBackwardsGradient();
        super.connectGradientFields();
    }

    protected void initBackwardsGradient() {
        if(output.getOutputGradient() == null)
            return;

        backwardsGradient = mul(
                this,
                "output.forwardsGradient * s.weight",
                output.getOutputGradient(),
                synapse.getWeight(),
                input.getBackwardsGradientIn()
        );
    }
}
