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
    ArrayList<SynapseActivation> queue = new ArrayList<>();

    public enum Direction {
        INPUT,
        OUTPUT
    }


    public Linker(Document doc) {
        this.doc = doc;
    }



    /**
     * Adds the incoming and outgoing links between neuron activations.
     *
     * @param act
     */
    public static void link(Activation act) {

        INeuron n = act.getINeuron();
        n.lock.acquireReadLock();
        n.provider.lock.acquireReadLock();
        act.doc.linker.linkInput(act);
        n.provider.lock.releaseReadLock();
        n.lock.releaseReadLock();

        long v = act.doc.visitedCounter++;
        Conflicts.linkConflicts(act, v, INPUT);
        Conflicts.linkConflicts(act, v, OUTPUT);
    }


    private void linkInput(Activation act) {
        for(OrNode.Link ol: act.inputs.values()) {
            for(int i = 0; i < ol.oe.synapseIds.length; i++) {
                int synId = ol.oe.synapseIds[i];
                Synapse s = act.node.neuron.getSynapseById(synId);
                Activation iAct = ol.input.getInputActivation(i);
                SynapseActivation sa = link(s, iAct, act);

                ol.input.node.
            }
        }
        process();
    }


    private SynapseActivation link(Synapse s, Activation iAct, Activation oAct) {
        SynapseActivation sa = new SynapseActivation(s, iAct, oAct);
        SynapseActivation esa = oAct.neuronInputs.get(sa);
        if(esa != null) {
            return esa;
        }

        sa.unmatchedRelations = new TreeMap<>(s.relations);
        iAct.addSynapseActivation(INPUT, sa);
        oAct.addSynapseActivation(OUTPUT, sa);

        addToQueue(sa);
        return sa;
    }


    public static void lateLinking(Document doc) {
        for(Activation act: doc.activationsByRangeBegin.values()) {
            for(SynapseActivation sa: act.neuronInputs) {
                doc.linker.addToQueue(sa);
            }
        }
        doc.linker.process();
    }


    public void addToQueue(SynapseActivation sa) {
        if(sa.unmatchedRelations.size() > 0) {
            queue.add(sa);
        }
    }


    public void process() {
        for(SynapseActivation linkedSA: queue) {
            for(Map.Entry<Synapse, Relation> me: linkedSA.unmatchedRelations.entrySet()) {
                Synapse s = me.getKey();
                Relation r = me.getValue();
                INeuron.ThreadState ts = s.input.get().getThreadState(doc.threadId, true);
                if(!r.isExact()) {
                    for(Activation iAct: ts.activations.values()) {
                        if(r.test(iAct, linkedSA.input)) {
                            SynapseActivation sa = link(s, iAct, linkedSA.output);
                            sa.unmatchedRelations.remove(linkedSA.synapse);
                            linkedSA.unmatchedRelations.remove(sa.synapse);
                        }
                    }
                } else {
                    for(Activation iAct: r.getLinkedActivationCandidates(linkedSA.input)) {
                        if(iAct.getNeuron() == s.input && r.test(iAct, linkedSA.input)) {
                            SynapseActivation sa = link(s, iAct, linkedSA.output);
                            sa.unmatchedRelations.remove(linkedSA.synapse);
                            linkedSA.unmatchedRelations.remove(sa.synapse);
                        }
                    }
                }
            }
        }
        queue.clear();
    }

}
