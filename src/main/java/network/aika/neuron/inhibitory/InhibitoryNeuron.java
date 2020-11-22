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

import network.aika.ActivationFunction;
import network.aika.Model;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.PatternPartSynapse;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;

import java.util.stream.Stream;

import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.SAME;

/**
 *
 * @author Lukas Molzberger
 */
public class InhibitoryNeuron extends Neuron<InhibitorySynapse> {

    public static byte type;

    protected InhibitoryNeuron() {
        super();
    }

    public InhibitoryNeuron(NeuronProvider p) {
        super(p);
    }

    public InhibitoryNeuron(Model model) {
        super(model);
    }

    @Override
    public Stream<Synapse> getTemplateSynapses() {
        return null;
    }

    @Override
    public void addDummyLinks(Activation act) {

    }

    @Override
    public void prepareInitialSynapseInduction(Activation iAct, Activation newAct) {
        newAct
                .getNeuron()
                .induceSynapse(
                        iAct,
                        newAct,
                        new Visitor(iAct, newAct, SAME, null, INPUT, false)
                );
    }

    @Override
    public void initOutgoingPPSynapse(PatternPartSynapse s, Visitor v) {
        s.setNegative(v.getSelfRef());
        s.setRecurrent(true);
    }

    @Override
    public InhibitorySynapse induceOutgoingInhibitorySynapse(InhibitoryNeuron outN) {
        return null;
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public void transition(Visitor v, Activation act, boolean create) {
        v.followLinks(act);
    }

    public boolean isInitialized() {
        return false;
    }

    @Override
    public void updateReference(Link nl) {
        nl.getOutput().propagateReference(nl.getInput().getReference());
    }
/*
    @Override
    public void induceNeuron(Activation activation) {

    }
*/
    /*
    public static Activation induce(Activation iAct) {
        if(!iAct.getConfig().checkInhibitoryNeuronInduction(iAct.getNeuron())) {
//            System.out.println("N  " + "dbg:" + (Neuron.debugId++) + " " + act.getNeuron().getDescriptionLabel() + "  " + Utils.round(s) + " below threshold");
            return null;
        }

        Activation act = iAct.getOutputLinks()
                .filter(l -> l.getSynapse().inductionRequired(InhibitoryNeuron.class))
                .map(l -> l.getOutput())
                .findAny()
                .orElse(null);

        if (act == null) {
            Neuron n = new InhibitoryNeuron(iAct.getModel());
            act = n.initInducedNeuron(iAct);
        }

        return act;
    }

     */

    public Link induceSynapse(Activation iAct, Activation oAct, Visitor v) {
        InhibitorySynapse s = iAct.getNeuron().induceOutgoingInhibitorySynapse(this);

        if(s == null) {
            return null;
        }

        s.setWeight(1.0);

        return s.initInducedSynapse(iAct, oAct, v);
    }

    @Override
    public Fired incrementFired(Fired f) {
        return f;
    }

    public ActivationFunction getActivationFunction() {
        return ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT;
    }
}
