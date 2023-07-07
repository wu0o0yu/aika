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
import network.aika.elements.activations.ConjunctiveActivation;
import network.aika.elements.synapses.ConjunctiveSynapse;
import network.aika.fields.FieldOutput;

import static network.aika.fields.FieldLink.linkAndConnect;
import static network.aika.fields.Fields.*;


/**
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveLink<S extends ConjunctiveSynapse, IA extends Activation<?>, OA extends ConjunctiveActivation<?>> extends Link<S, IA, OA> {

    private FieldOutput weightUpdatePosCase;
    private FieldOutput weightUpdateNegCase;
    private FieldOutput biasUpdateNegCase;

    public ConjunctiveLink(S s, IA input, OA output) {
        super(s, input, output);
    }

    @Override
    protected void initWeightInput() {
        super.initWeightInput();

        if(synapse.isOptional())
            linkAndConnect(getSynapse().getSynapseBias(), getOutput().getNet());
    }

    @Override
    public void connectWeightUpdate() {
        weightUpdatePosCase = mul(
                this,
                "weight update (pos case)",
                getInputValue(),
                getOutput().getUpdateValue(),
                synapse.getWeight()
        );

        weightUpdateNegCase = scale(
                this,
                "weight update (neg case)",
                -1.0,
                mul(
                        this,
                        "weight update (neg case)",
                        getNegInputIsFired(),
                        getOutput().getNegUpdateValue()
                ),
                synapse.getWeight()
        );

        biasUpdateNegCase = mul(
                this,
                "bias update (neg case)",
                getNegInputIsFired(),
                getOutput().getNegUpdateValue(),
                getSynapse().getSynapseBias()
        );
    }

    @Override
    public void disconnect() {
        super.disconnect();

        if(weightUpdatePosCase != null)
            weightUpdatePosCase.disconnectAndUnlinkOutputs(false);
        if(weightUpdateNegCase != null)
            weightUpdateNegCase.disconnectAndUnlinkOutputs(false);
        if(biasUpdateNegCase != null)
            biasUpdateNegCase.disconnectAndUnlinkOutputs(false);
    }

    public FieldOutput getWeightUpdatePosCase() {
        return weightUpdatePosCase;
    }

    public FieldOutput getWeightUpdateNegCase() {
        return weightUpdateNegCase;
    }

    public FieldOutput getBiasUpdateNegCase() {
        return biasUpdateNegCase;
    }
}
