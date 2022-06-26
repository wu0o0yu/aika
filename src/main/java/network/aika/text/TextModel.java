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

//    public static String TOKEN_LABEL = "Token Category";

//    private CategoryNeuron tokenCategory;
    private LatentRelationNeuron relationNeuron;

    public TextModel() {
        super();
    }

    public TextModel(SuspensionCallback sc) {
        super(sc);
    }

    public void init() {
/*        tokenCategory = getTemplates().CATEGORY_TEMPLATE.instantiateTemplate(true);
        tokenCategory.setNetworkInput(true);
        tokenCategory.setLabel(TOKEN_LABEL);
*/
        relationNeuron = initRelationNeuron("Rel. -1", -1);
    }

    private LatentRelationNeuron initRelationNeuron(String label, int distance) {
        LatentRelationNeuron n = getTemplates().LATENT_RELATION_TEMPLATE.instantiateTemplate(true);
        n.setLabel(label);

        n.setRangeBegin(distance);
        n.setRangeEnd(distance);

//        Synapse pis = initPrimaryInputSynapse(tokenCategory, n);
//        Synapse rps = initReversePatternSynapse(tokenCategory, n);

        // n.setNetworkInput(true);
        n.getBias().receiveUpdate(-4.0);
        n.setAllowTraining(false);
        n.updateSumOfLowerWeights();

//        assert !pis.isPropagate();
//        assert !rps.isPropagate();

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

//        initCategorySynapse(in, getTokenCategory());

        in.getProvider().save();

        return in;
    }
/*
    private PrimaryInputSynapse initPrimaryInputSynapse(CategoryNeuron relTokenCat, BindingNeuron relBN) {
        PrimaryInputSynapse s = (PrimaryInputSynapse) getTemplates().PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE
                .instantiateTemplate(null, relTokenCat, relBN);

        double w = 1.0;

        s.linkOutput();
        s.getWeight().set(w);
        s.setAllowTraining(false);

        relBN.getBias().receiveUpdate(-w);
        return s;
    }

    private ReversePatternSynapse initReversePatternSynapse(CategoryNeuron in, BindingNeuron inRel) {
        ReversePatternSynapse s = (ReversePatternSynapse) getTemplates().REVERSE_PATTERN_SYNAPSE_FROM_CATEGORY_TEMPLATE
                .instantiateTemplate(null, in, inRel);

        double w = 2.0;

        s.linkInput();
        s.linkOutput();
        s.getWeight().set(w);
        s.setAllowTraining(false);
        inRel.getBias().receiveUpdate(-w);

        return s;
    }

    private void initCategorySynapse(PatternNeuron tokenNeuron, CategoryNeuron tokenCat) {
        Synapse s = getTemplates().CATEGORY_SYNAPSE_TEMPLATE
                .instantiateTemplate(null, tokenNeuron, tokenCat);

        s.linkInput();
        s.getWeight().set(1.0);
        s.setAllowTraining(false);
    }

    public CategoryNeuron getTokenCategory() {
        return tokenCategory;
    }
*/
    public LatentRelationNeuron getPreviousTokenRelation() {
        return relationNeuron;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

//        out.writeLong(tokenCategory.getId());
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

//        tokenCategory = (CategoryNeuron) lookupNeuron(in.readLong()).getNeuron();
    }
}
