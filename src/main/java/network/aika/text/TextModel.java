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
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.conjunctive.*;
import network.aika.neuron.disjunctive.CategoryNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
* @author Lukas Molzberger
*/
public class TextModel extends Model {

    public static String REL_PREVIOUS_TOKEN_LABEL = " Rel Prev. Token";
    public static String REL_NEXT_TOKEN_LABEL = " Rel Next Token";
    public static String TOKEN_LABEL = "Token Category";


    private NeuronProvider tokenCategory;

    public TextModel() {
        super();
    }

    public TextModel(SuspensionCallback sc) {
        super(sc);
    }

    public void init() {
        if(tokenCategory == null)
            tokenCategory = initCategoryNeuron(TOKEN_LABEL);
    }

    private NeuronProvider initCategoryNeuron(String label) {
        CategoryNeuron n = getTemplates().CATEGORY_TEMPLATE.instantiateTemplate(true);
        n.setInputNeuron(true);
        n.setLabel(label);
        NeuronProvider np = n.getProvider();
        np.save();
        return np;
    }

    public PatternNeuron lookupToken(String tokenLabel) {
        Neuron inProv = getNeuron(tokenLabel);
        if(inProv != null) {
            return (PatternNeuron) inProv;
        }

        PatternNeuron in = getTemplates().INPUT_PATTERN_TEMPLATE.instantiateTemplate(true);
        BindingNeuron inRelPT = getTemplates().INPUT_BINDING_TEMPLATE.instantiateTemplate(true);
        BindingNeuron inRelNT = getTemplates().INPUT_BINDING_TEMPLATE.instantiateTemplate(true);

        in.setTokenLabel(tokenLabel);
        in.setInputNeuron(true);
        in.setLabel(tokenLabel);
        in.setAllowTraining(false);
        putLabel(tokenLabel, in.getId());

        initCategorySynapse(in, getTokenCategory());

        initRelationNeuron(tokenLabel + REL_PREVIOUS_TOKEN_LABEL, in, getTokenCategory(), inRelPT);
        initRelationNeuron(tokenLabel + REL_NEXT_TOKEN_LABEL, in, getTokenCategory(), inRelNT);

        in.getProvider().save();
        inRelPT.getProvider().save();
        inRelNT.getProvider().save();

        return in;
    }

    private void initRelationNeuron(String label, PatternNeuron in, CategoryNeuron relTokenCat, BindingNeuron inRel) {
        inRel.setInputNeuron(true);
        inRel.setLabel(label);

        initFeedbackSamePatternSynapse(in, inRel);
        initRelatedInputSynapse(relTokenCat, inRel);

        inRel.getBias().addAndTriggerUpdate(4.0);
        inRel.getFinalBias().addAndTriggerUpdate(4.0);
        inRel.setAllowTraining(false);
        inRel.updateSynapseInputConnections();
    }

    private void initRelatedInputSynapse(CategoryNeuron relTokenCat, BindingNeuron relBN) {
        Synapse s = getTemplates().PRIMARY_INPUT_SYNAPSE_FROM_CATEGORY_TEMPLATE
                .instantiateTemplate(relTokenCat, relBN);

        double w = 10.0;

        s.linkOutput();
        s.getWeight().setAndTriggerUpdate(w);
        s.setAllowTraining(false);

        relBN.getBias().add(-w);
        relBN.getFinalBias().add(-w);
    }

    private void initFeedbackSamePatternSynapse(PatternNeuron in, BindingNeuron inRel) {
        Synapse s = getTemplates().POSITIVE_FEEDBACK_SYNAPSE_TEMPLATE
                .instantiateTemplate(in, inRel);

        double w = 11.0;

        s.linkInput();
        s.linkOutput();
        s.getWeight().setAndTriggerUpdate(w);
        s.setAllowTraining(false);
        inRel.getFinalBias().add(-w);
    }

    private void initCategorySynapse(PatternNeuron tokenNeuron, CategoryNeuron tokenCat) {
        Synapse s = getTemplates().CATEGORY_SYNAPSE_TEMPLATE
                .instantiateTemplate(tokenNeuron, tokenCat);

        s.linkInput();
        s.getWeight().setAndTriggerUpdate(2.0);
        s.setAllowTraining(false);
    }

    public CategoryNeuron getTokenCategory() {
        return (CategoryNeuron) tokenCategory.getNeuron();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeLong(tokenCategory.getId());
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        tokenCategory = lookupNeuron(in.readLong());
    }
}
