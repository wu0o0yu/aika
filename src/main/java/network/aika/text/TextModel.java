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
package network.aika.text;

import network.aika.Model;
import network.aika.callbacks.SuspensionCallback;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.conjunctive.*;
import network.aika.neuron.disjunctive.CategoryNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.utils.TestUtils.createNeuron;

/**
 *
* @author Lukas Molzberger
*/
public class TextModel extends Model {

    private LatentRelationNeuron relationNeuron;

    public TextModel() {
        super();
    }

    public TextModel(SuspensionCallback sc) {
        super(sc);
    }

    public void init() {
        relationNeuron = initRelationNeuron("Rel. -1", -1);
    }

    private LatentRelationNeuron initRelationNeuron(String label, int distance) {
        LatentRelationNeuron n = getTemplates().LATENT_RELATION_TEMPLATE.instantiateTemplate(true);
        n.setLabel(label);

        n.setRangeBegin(distance);
        n.setRangeEnd(distance);

        n.getBias().receiveUpdate(-4.0);
        n.setAllowTraining(false);
        n.updateSumOfLowerWeights();

        n.getProvider().save();

        return n;
    }

    public PatternNeuron lookupToken(String tokenLabel) {
        Neuron inProv = getNeuron(tokenLabel);
        if(inProv != null) {
            return (PatternNeuron) inProv;
        }

        PatternNeuron in = getTemplates().PATTERN_TEMPLATE.instantiateTemplate(true);

        in.setTokenLabel(tokenLabel);
        in.setNetworkInput(true);
        in.setLabel(tokenLabel);
        in.setAllowTraining(false);
        putLabel(tokenLabel, in.getId());

        in.getProvider().save();

        return in;
    }

    public LatentRelationNeuron getPreviousTokenRelation() {
        return relationNeuron;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);
    }
}
