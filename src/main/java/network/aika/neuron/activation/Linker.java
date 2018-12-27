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
import network.aika.neuron.relation.Relation;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation.Link;

import java.util.*;

import static network.aika.neuron.Synapse.OUTPUT;

/**
 * The {@code Linker} class is responsible for for the linkage of neuron activations. These links mirror the synapses between
 * the neurons of these activations.
 *
 * @author Lukas Molzberger
 */
public class Linker {

    protected Document doc;
    ArrayDeque<Link> queue = new ArrayDeque<>();


    public enum Direction {
        INPUT,
        OUTPUT
    }


    public Linker(Document doc) {
        this.doc = doc;
    }


    public Activation computeInputActivation(Synapse s, Activation iAct) {
        return iAct;
    }


    /**
     * Adds the incoming links between neuron activations.
     *
     * @param act
     */
    public void link(Activation act, OrNode.Link ol) {
        linkOrNodeRelations(act, ol);
        process();
    }


    private void linkOrNodeRelations(Activation act, OrNode.Link ol) {
        for (int i = 0; i < ol.oe.synapseIds.length; i++) {
            int synId = ol.oe.synapseIds[i];
            Synapse s = act.node.neuron.getSynapseById(synId);
            Activation iAct = ol.input.getInputActivation(i);
            link(s, iAct, act);
        }
    }


    private void linkOutputRelations(Activation act) {
        INeuron n = act.getINeuron();
        if(n.outputRelations != null) {
            linkRelated(act, act, n.outputRelations);
        }
    }


    public void linkInput(Activation act) {
        for(Synapse s: act.getNeuron().inMemoryInputSynapses.values()) {
            for(Map.Entry<Integer, Relation> me: s.relations.entrySet()) {
                Relation rel = me.getValue();
                if(me.getKey() == OUTPUT) {
                    rel.getActivations(s.input.get(act.doc), act)
                            .forEach(iAct -> link(s, iAct, act));
                }
            }
        }
    }


    public void lateLinking() {
        int oldSize;
        do {
            oldSize = doc.getNumberOfActivations();
            Activation act = null;
            while((act = doc.getNextActivation(act)) != null) {
                linkOutputRelations(act);

                act.getInputLinks(false, false)
                        .forEach(l -> addToQueue(l));
            }
            doc.linker.process();
            doc.propagate();
        } while(oldSize != doc.getNumberOfActivations());


        postLateLinking();
    }


    public void process() {
        while(!queue.isEmpty()) {
            Link l = queue.pollFirst();
            linkRelated(l.input, l.output, l.synapse.relations);
        }
    }


    private void linkRelated(Activation rAct, Activation oAct, Map<Integer, Relation> relations) {
        for(Map.Entry<Integer, Relation> me: relations.entrySet()) {
            Relation rel = me.getValue();
            Integer relId = me.getKey();
            if(relId != OUTPUT) {
                Synapse s = oAct.getNeuron().getSynapseById(relId);
                if (s != null) {
                    rel.invert().getActivations(s.input.get(rAct.doc), rAct)
                            .forEach(iAct -> link(s, iAct, oAct));
                }
            }
        }
    }


    protected void link(Synapse s, Activation iAct, Activation oAct) {
        iAct = computeInputActivation(s, iAct);

        if(iAct == null || iAct.blocked || !checkRelations(s, iAct, oAct)) {
            return;
        }

        Link nl = new Link(s, iAct, oAct, false);
        if(oAct.getInputLink(nl) != null) {
            return;
        }

        if(s.identity) {
            Link el = oAct.getLinkBySynapseId(s.id);
            if(el != null && el.input != iAct) {
                nl.passive = true;
            }
        }

        nl.link();

        if(!nl.passive) {
            addToQueue(nl);
        }
    }


    private boolean checkRelations(Synapse s, Activation iAct, Activation oAct) {
        for(Map.Entry<Integer, Relation> me: s.relations.entrySet()) {
            Integer relSynId = me.getKey();
            Relation rel = me.getValue();
            if(relSynId == Synapse.OUTPUT) {
                if (!rel.test(iAct, oAct)) {
                    return false;
                }
            } else {
                Synapse relSyn = oAct.getNeuron().getSynapseById(relSynId);
                if(relSyn!= null && oAct.getInputLinksBySynapse(false, relSyn)
                        .anyMatch(l -> !rel.test(iAct, l.input))) {
                    return false;
                }
            }
        }

        return true;
    }


    protected void postLateLinking() {
        doc.getActivations(false)
                .stream()
                .flatMap(act -> act.getInputLinks(false, false))
                .filter(l -> l.synapse.isRecurrent && !l.synapse.isNegative())
                .forEach(l -> {
                    if(!l.passive && !checkLoop(l.input, l.output)) {
                        l.passive = true;
                    }
                });
    }


    protected boolean checkLoop(Activation iAct, Activation oAct) {
        long v = doc.visitedCounter++;

        oAct.markedPredecessor = v;
        return iAct.checkSelfReferencing(false, 0, v);
    }


    private void addToQueue(Link l) {
        if(l == null) {
            return;
        }
        if(!l.synapse.isNegative()) {
            queue.add(l);
        }
        doc.ubQueue.add(l);
    }
}
