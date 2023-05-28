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
package experiment;

import network.aika.direction.Direction;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.elements.neurons.NeuronProvider;
import network.aika.elements.neurons.PatternNeuron;
import network.aika.elements.synapses.PatternSynapse;
import network.aika.elements.synapses.RelationInputSynapse;
import network.aika.elements.synapses.Synapse;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;


/**
 * @author Lukas Molzberger
 */
public class LabelUtil {


    public static String generateLabel(PatternNeuron pn) {
        BindingNeuron[] bn = pn.getInputSynapsesByType(PatternSynapse.class)
                .map(Synapse::getInput)
                .toList()
                .toArray(new BindingNeuron[0]);

        for (int i = 0; i < bn.length - 1; i++) {
            for (int j = i + 1; j < bn.length; j++) {
                BindingNeuron a = bn[i];
                BindingNeuron b = bn[j];
                if ((a.getInputSynapse(b.getProvider()) != null && getDirection(b) == INPUT) ||
                        (b.getInputSynapse(a.getProvider()) != null && getDirection(a) == INPUT)) {
                    bn[i] = b;
                    bn[j] = a;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bn.length; i++) {
            PatternSynapse s = (PatternSynapse) bn[i].getOutputSynapse(pn.getProvider());
            if (-s.getSynapseBias().getCurrentValue() > pn.getBias().getCurrentValue()) {
                sb.append(bn[i].getLabel());
            }
        }
        return sb.toString();
    }

    private static Direction getDirection(BindingNeuron n) {
        RelationInputSynapse rs = n.getInputSynapseByType(RelationInputSynapse.class);

        if (rs == null)
            return null;

        return rs.getInput().getRangeBegin() > 0 ? INPUT : OUTPUT;
    }
}
