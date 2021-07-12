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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.InputBNSynapse;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;
import static network.aika.neuron.steps.activation.ActivationStep.LINKING;

/**
 *
* @author Lukas Molzberger
*/
public class TextModel extends Model {

    public static String REL_PREVIOUS_TOKEN_LABEL = " Rel Prev. Token";
    public static String REL_NEXT_TOKEN_LABEL = " Rel Next Token";
    public static String PREVIOUS_TOKEN_LABEL = "Prev. Token";
    public static String NEXT_TOKEN_LABEL = "Next Token";


    private NeuronProvider prevTokenInhib;
    private NeuronProvider nextTokenInhib;

    public TextModel() {
        super();
    }

    public TextModel(SuspensionCallback sc) {
        super(sc);
    }

    public void init() {
        if(prevTokenInhib == null)
            prevTokenInhib = initInhibitoryNeuron(PREVIOUS_TOKEN_LABEL);

        if(nextTokenInhib == null)
            nextTokenInhib = initInhibitoryNeuron(NEXT_TOKEN_LABEL);
    }

    private NeuronProvider initInhibitoryNeuron(String label) {
        InhibitoryNeuron n = getTemplates().INHIBITORY_TEMPLATE.instantiateTemplate(true);
        n.setInputNeuron(true);
        n.setLabel(label);
        NeuronProvider np = n.getProvider();
        np.save();
        return np;
    }

    @Override
    public void linkInputRelations(Activation fromAct, Direction dir) {
        TextReference ref = fromAct.getReference();
        TextReference lastRef = ref.getPrevious();
        if(lastRef == null) return;

        if (dir == OUTPUT) {
            if (fromAct.getNeuron().isInputNeuron() && prevTokenInhib.getId().equals(fromAct.getNeuron().getId()) && lastRef.nextTokenBAct != null) {
                Synapse s = getRelSynapse(lastRef.nextTokenBAct.getNeuron());
                addLink(s, fromAct, lastRef.nextTokenBAct);
            }
        } else if (dir == INPUT) {
            Neuron n = fromAct.getNeuron();
            if (n instanceof ExcitatoryNeuron) {
                Synapse s = getRelSynapse(n);

                if (s != null) {
                    if (isPrevTokenBinding(fromAct.getNeuron()) && lastRef.nextTokenIAct != null) {
                        addLink(s, lastRef.nextTokenIAct, fromAct);
                    }
                }
            }
        }
    }

    private static void addLink(Synapse s, Activation iAct, Activation oAct) {
        Link nl = oAct.addLink(s, iAct, false, null);

        LINKING.getNextSteps(nl);
    }

    private Synapse getRelSynapse(Neuron<?> n) {
        return n.getInputSynapses()
                .filter(s -> s instanceof InputBNSynapse)
                .map(s -> (InputBNSynapse) s)
                .findAny()
                .orElse(null);
    }

    private boolean isPrevTokenBinding(Neuron<?> n) {
        return n.getOutputSynapses()
                .anyMatch(s -> prevTokenInhib.getId().equals(s.getOutput().getId()));
    }

    public PatternNeuron lookupToken(String tokenLabel) {
        Neuron inProv = getNeuron(tokenLabel);
        if(inProv != null) {
            return (PatternNeuron) inProv;
        }

        PatternNeuron in = getTemplates().INPUT_PATTERN_TEMPLATE
                .instantiateTemplate(true);
        BindingNeuron inRelPT = getTemplates().INPUT_BINDING_TEMPLATE
                .instantiateTemplate(true);
        BindingNeuron inRelNT = getTemplates().INPUT_BINDING_TEMPLATE
                .instantiateTemplate(true);

        in.setTokenLabel(tokenLabel);
        in.setInputNeuron(true);
        in.setLabel(tokenLabel);
        in.setAllowTraining(false);
        putLabel(tokenLabel, in.getId());

        initRelationNeuron(tokenLabel + REL_PREVIOUS_TOKEN_LABEL, in, inRelPT, getNextTokenInhib(), false);
        initRelationNeuron(tokenLabel + REL_NEXT_TOKEN_LABEL, in, inRelNT, getPrevTokenInhib(), true);

        initInhibitorySynapse(inRelPT, getPrevTokenInhib());
        initInhibitorySynapse(inRelNT, getNextTokenInhib());

        in.getProvider().save();
        inRelPT.getProvider().save();
        inRelNT.getProvider().save();

        return in;
    }

    private void initRelationNeuron(String label, PatternNeuron in, BindingNeuron inRel, InhibitoryNeuron inhib, boolean b) {
        inRel.setInputNeuron(true);
        inRel.setLabel(label);

        initRelatedRecurrentInputSynapse(in, inRel);
        initRelatedInputSynapse(inRel, inhib, b);

        inRel.setBias(4.0);
        inRel.setAllowTraining(false);
    }

    private void initRelatedInputSynapse(BindingNeuron inRel, InhibitoryNeuron inhib, boolean recurrent) {
        Synapse s = getTemplates().RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE
                .instantiateTemplate(inhib, inRel);

        s.linkOutput();
        s.addWeight(10.0);
        s.setAllowTraining(false);
        inRel.addConjunctiveBias(-10.0, recurrent);
    }

    private void initRelatedRecurrentInputSynapse(PatternNeuron in, BindingNeuron inRel) {
        Synapse s = getTemplates().RELATED_RECURRENT_INPUT_TEMPLATE
                .instantiateTemplate(in, inRel);

        s.linkInput();
        s.linkOutput();
        s.setWeight(11.0);
        s.setAllowTraining(false);
        inRel.addConjunctiveBias(-11.0, false);
    }

    private void initInhibitorySynapse(BindingNeuron inRelPT, InhibitoryNeuron prevTokenInhib) {
        Synapse s = getTemplates().INHIBITORY_SYNAPSE_TEMPLATE
                .instantiateTemplate(inRelPT, prevTokenInhib);

        s.linkInput();
        s.addWeight(1.0);
        s.setAllowTraining(false);
    }

    public InhibitoryNeuron getPrevTokenInhib() {
        return (InhibitoryNeuron) prevTokenInhib.getNeuron();
    }

    public InhibitoryNeuron getNextTokenInhib() {
        return (InhibitoryNeuron) nextTokenInhib.getNeuron();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeLong(prevTokenInhib.getId());
        out.writeLong(nextTokenInhib.getId());
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        prevTokenInhib = lookupNeuron(in.readLong());
        nextTokenInhib = lookupNeuron(in.readLong());
    }
}
