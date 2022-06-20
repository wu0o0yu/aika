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

    public static String TOKEN_LABEL = "Token Category";

    private CategoryNeuron tokenCategory;
    private LatentRelationNeuron relationNeuron;

    public TextModel() {
        super();
    }

    public TextModel(SuspensionCallback sc) {
        super(sc);
    }

    public void init() {
        tokenCategory = getTemplates().CATEGORY_TEMPLATE.instantiateTemplate(true);
        tokenCategory.setNetworkInput(true);
        tokenCategory.setLabel(TOKEN_LABEL);

        relationNeuron = createNeuron(getTemplates().LATENT_RELATION_TEMPLATE, "Rel. -1");
        relationNeuron.setRangeBegin(-1);
        relationNeuron.setRangeEnd(-1);
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

        initCategorySynapse(in, getTokenCategory());

        in.getProvider().save();

        return in;
    }
/*
    private void initRelationNeuron(String label, BindingNeuron inRel) {
        inRel.setNetworkInput(true);
        inRel.setLabel(label);
        inRel.getBias().receiveUpdate(4.0);
        inRel.setAllowTraining(false);
        inRel.updateSumOfLowerWeights();
    }

    private PrimaryInputSynapse initRelatedInputSynapse(CategoryNeuron relTokenCat, BindingNeuron relBN) {
        PrimaryInputSynapse s = (PrimaryInputSynapse) getTemplates().PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE
                .instantiateTemplate(null, relTokenCat, relBN);

        double w = 10.0;

        s.linkOutput();
        s.getWeight().set(w);
        s.setAllowTraining(false);

        relBN.getBias().receiveUpdate(-w);
        return s;
    }

    private ReversePatternSynapse initReversePatternSynapse(CategoryNeuron in, BindingNeuron inRel) {
        ReversePatternSynapse s = (ReversePatternSynapse) getTemplates().REVERSE_PATTERN_SYNAPSE_FROM_CATEGORY_TEMPLATE
                .instantiateTemplate(null, in, inRel);

        double w = 11.0;

        s.linkInput();
        s.linkOutput();
        s.getWeight().set(w);
        s.setAllowTraining(false);
        inRel.getBias().receiveUpdate(-w);

        return s;
    }
*/
    private void initCategorySynapse(PatternNeuron tokenNeuron, CategoryNeuron tokenCat) {
        Synapse s = getTemplates().CATEGORY_SYNAPSE_TEMPLATE
                .instantiateTemplate(null, tokenNeuron, tokenCat);

        s.linkInput();
        s.getWeight().set(2.0);
        s.setAllowTraining(false);
    }

    public CategoryNeuron getTokenCategory() {
        return tokenCategory;
    }

    public BindingNeuron getPreviousTokenRelation() {
        return relationNeuron;
    }
/*
    public BindingNeuron getNextTokenRelationBindingNeuron() {
        return (BindingNeuron) relNextToken.getNeuron();
    }

    public PrimaryInputSynapse<CategoryNeuron, CategoryActivation> getRelPTPrimaryInputSyn() {
        return relPTPrimaryInputSyn;
    }

    public ReversePatternSynapse<CategoryNeuron, CategoryActivation> getRelPTRevPatternSyn() {
        return relPTRevPatternSyn;
    }

    public PrimaryInputSynapse<CategoryNeuron, CategoryActivation> getRelNTPrimaryInputSyn() {
        return relNTPrimaryInputSyn;
    }

    public ReversePatternSynapse<CategoryNeuron, CategoryActivation> getRelNTRevPatternSyn() {
        return relNTRevPatternSyn;
    }
*/
    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeLong(tokenCategory.getId());
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        tokenCategory = (CategoryNeuron) lookupNeuron(in.readLong()).getNeuron();
    }
}
