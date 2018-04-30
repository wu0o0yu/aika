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
package network.aika.neuron.activation;

import network.aika.Document;
import network.aika.lattice.OrNode;
import network.aika.neuron.INeuron;
import network.aika.neuron.Relation;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation.SynapseActivation;

import java.util.*;

import static network.aika.neuron.activation.Linker.Direction.INPUT;
import static network.aika.neuron.activation.Linker.Direction.OUTPUT;

/**
 * The {@code Linker} class is responsible for for the linkage of neuron activations. These links mirror the synapses between
 * the neurons of these activations.
 *
 * @author Lukas Molzberger
 */
public class Linker {

    Document doc;
    ArrayDeque<SynapseActivation> queue = new ArrayDeque<>();

    public enum Direction {
        INPUT,
        OUTPUT
    }


    public Linker(Document doc) {
        this.doc = doc;
    }


    /**
     * Adds the incoming links between neuron activations.
     *
     * @param act
     */
    public void link(Activation act, OrNode.Link ol) {
        INeuron n = act.getINeuron();
        n.lock.acquireReadLock();
        n.provider.lock.acquireReadLock();
        for (int i = 0; i < ol.oe.synapseIds.length; i++) {
            int synId = ol.oe.synapseIds[i];
            Synapse s = act.node.neuron.getSynapseById(synId);
            Activation iAct = ol.input.getInputActivation(i);
            link(s, iAct, act);
        }
        process();
        n.provider.lock.releaseReadLock();
        n.lock.releaseReadLock();
    }


    private SynapseActivation link(Synapse s, Activation iAct, Activation oAct) {
        SynapseActivation sa = new SynapseActivation(s, iAct, oAct);
        SynapseActivation esa = oAct.neuronInputs.get(sa);
        if(esa != null) {
            return esa;
        }

        iAct.addSynapseActivation(INPUT, sa);
        oAct.addSynapseActivation(OUTPUT, sa);

        queue.add(sa);
        return sa;
    }


    public void lateLinking() {
        for(Activation act: doc.activationsByRangeBegin.values()) {
            for(SynapseActivation sa: act.neuronInputs.values()) {
                queue.add(sa);
            }
        }
        doc.linker.process();
    }


    public void process() {
        while(!queue.isEmpty()) {
            SynapseActivation linkedSA = queue.pollFirst();
            for(Map.Entry<Integer, Relation> me: linkedSA.synapse.relations.entrySet()) {
                Synapse s = linkedSA.output.getNeuron().getSynapseById(me.getKey());
                Relation r = me.getValue();
                INeuron.ThreadState ts = s.input.get().getThreadState(doc.threadId, true);
                if(!r.isExact()) {
                    for(Activation iAct: ts.activations.values()) {
                        if(r.test(linkedSA.input, iAct)) {
                            link(s, iAct, linkedSA.output);
                        }
                    }
                } else {
                    for(Activation iAct: r.getLinkedActivationCandidates(linkedSA.input)) {
                        if(iAct.getNeuron() == s.input && r.test(linkedSA.input, iAct)) {
                            link(s, iAct, linkedSA.output);
                        }
                    }
                }
            }
        }
    }
}
