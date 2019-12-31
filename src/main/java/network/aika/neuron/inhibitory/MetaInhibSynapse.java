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
package network.aika.neuron.inhibitory;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.TSynapse;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.input.InputNeuron;


/**
 *
 * @author Lukas Molzberger
 */
public class MetaInhibSynapse extends TSynapse<TNeuron, InhibitoryNeuron> {

    public static final String TYPE_STR = Model.register("SMI", MetaInhibSynapse.class);

    public MetaInhibSynapse() {
        super();
    }

    public MetaInhibSynapse(Neuron input, Neuron output, Integer id) {
        super(input, output, id, false, true);
    }

    @Override
    public String getType() {
        return TYPE_STR;
    }

    @Override
    public boolean storeOnInputSide() {
        return false;
    }

    @Override
    public boolean storeOnOutputSide() {
        return true;
    }


    public InhibitorySynapse transferMetaSynapse(Document doc, TNeuron<?> inputNeuron) {
        InhibitoryNeuron inhibNeuron = getOutput();
        InhibitorySynapse targetSynapse = create(doc, inputNeuron.getProvider(), inhibNeuron);

        targetSynapse.updateDelta(
                doc,
                getWeight()
        );

        System.out.println("  Transfer Template Synapse: IN:" +
                inputNeuron.getLabel() +
                " OUT:" + inhibNeuron.getLabel() +
                " M-SynId:" + getId() +
                " T-SynId:" + targetSynapse.getId() +
                " W:" + targetSynapse.getNewWeight()
        );

        transferMetaRelations(this, targetSynapse);

        return targetSynapse;
    }


    private void transferMetaRelations(MetaInhibSynapse metaInhibSynapse, InhibitorySynapse targetSynapse) {
        // TODO
    }


    public static InhibitorySynapse create(Document doc, Neuron inputNeuron, InhibitoryNeuron outputNeuron) {
        inputNeuron.get(doc);
        InhibitorySynapse synapse = new InhibitorySynapse(inputNeuron, outputNeuron.getProvider(), outputNeuron.getNewSynapseId());
        synapse.link();

        return synapse;
    }


    public static class Builder extends Synapse.Builder {
        protected SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new MetaInhibSynapse(input, output, id);
        }
    }
}
