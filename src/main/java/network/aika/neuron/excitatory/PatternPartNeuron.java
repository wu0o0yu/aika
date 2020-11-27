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
package network.aika.neuron.excitatory;

import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Direction.*;

/**
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron<PatternPartSynapse> {
    private static final Logger log = LoggerFactory.getLogger(PatternPartNeuron.class);

    public static byte type;

    public static PatternPartNeuron THIS_TEMPLATE = new PatternPartNeuron();

    public static PatternPartSynapse PRIMARY_INPUT_SYNAPSE_TEMPLATE = new PatternPartSynapse(PatternNeuron.THIS_TEMPLATE, THIS_TEMPLATE, null, false, false, true, false);
    public static PatternPartSynapse RELATED_INPUT_SYNAPSE_TEMPLATE = new PatternPartSynapse(PatternPartNeuron.THIS_TEMPLATE, THIS_TEMPLATE, null, false, false, true, false);
    public static PatternPartSynapse SAME_PATTERN_SYNAPSE_TEMPLATE = new PatternPartSynapse(PatternPartNeuron.THIS_TEMPLATE, THIS_TEMPLATE, null, false, false, false, true);
    public static PatternPartSynapse RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE = new PatternPartSynapse(PatternNeuron.THIS_TEMPLATE, THIS_TEMPLATE, null, false, true, false, true);
    public static PatternPartSynapse NEGATIVE_SYNAPSE_TEMPLATE = new PatternPartSynapse(InhibitoryNeuron.THIS_TEMPLATE, THIS_TEMPLATE, null, true, true, false, false);

    private PatternPartNeuron() {
        super();
    }

    public PatternPartNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternPartNeuron(Model model) {
        super(model);
    }

    @Override
    public Neuron<?> getTemplate() {
        return THIS_TEMPLATE;
    }

    @Override
    public Stream<PatternPartSynapse> getTemplateSynapses() {
        return Arrays.asList(
                PRIMARY_INPUT_SYNAPSE_TEMPLATE,
                RELATED_INPUT_SYNAPSE_TEMPLATE,
                SAME_PATTERN_SYNAPSE_TEMPLATE,
                RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE,
                NEGATIVE_SYNAPSE_TEMPLATE
        ).stream();
    }

/*
    public Link induceSynapse(Activation iAct, Activation oAct, Visitor v) {
        PatternPartSynapse s = new PatternPartSynapse(iAct.getNeuron(), this);
        iAct.getNeuron().initOutgoingPPSynapse(s, v);

        return s.initInducedSynapse(iAct, oAct, v);
    }
*/
    @Override
    public void transition(Visitor v, Activation act, boolean create) {
        if(v.samePattern) {
            if(v.downUpDir == OUTPUT) {
                return;
            }

            Visitor nv = v.prepareNextStep();
            nv.downUpDir = OUTPUT;
            nv.followLinks(act);
        } else {
            v.followLinks(act);
        }
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public void updateReference(Link nl) {
        nl.getOutput().propagateReference(nl.getInput().getReference());
    }
}
