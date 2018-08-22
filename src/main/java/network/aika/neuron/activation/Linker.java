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

import static network.aika.neuron.Synapse.Builder.VARIABLE;

/**
 * The {@code Linker} class is responsible for for the linkage of neuron activations. These links mirror the synapses between
 * the neurons of these activations.
 *
 * @author Lukas Molzberger
 */
public class Linker {

    Document doc;
    ArrayDeque<Link> queue = new ArrayDeque<>();

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
            for (Map.Entry<Integer, Relation> me : n.outputRelations.entrySet()) {
                Synapse s = act.node.neuron.getSynapseById(me.getKey());
                link(act, act, s, me.getValue());
            }
        }
    }


    public void lateLinking() {
        int oldSize;
        do {
            oldSize = doc.getNumberOfActivations();
            for (Activation act : doc.getActivations(false)) {
                linkOutputRelations(act);

                for (Link l : act.inputLinks.values()) {
                    if(!l.passive) {
                        addToQueue(l);
                    }
                }
            }
            doc.linker.process();
        } while(oldSize != doc.getNumberOfActivations());
    }


    public void process() {
        while(!queue.isEmpty()) {
            Link l = queue.pollFirst();
            for(Map.Entry<Integer, Relation> me: l.synapse.relations.entrySet()) {
                int relId = me.getKey();
                if(relId >= 0) {
                    Synapse s = l.output.getNeuron().getSynapseById(relId);
                    if(s != null) {
                        Relation r = me.getValue();
                        link(l.input, l.output, s, r);
                    }
                }
            }
            doc.propagate();
        }
    }


    private void link(Activation rAct, Activation oAct, Synapse s, Relation r) {
        if(!r.isExact()) {
            INeuron.ThreadState ts = s.input.get().getThreadState(doc.threadId, true);
            for(Activation iAct: ts.getActivations()) {
                if(r.test(rAct, iAct)) {
                    link(s, iAct, oAct);
                }
            }
        } else {
            for(Activation iAct: r.invert().getActivations(s.input.get(rAct.doc), rAct)) {
                link(s, iAct, oAct);
            }
        }
    }


    protected void link(Synapse s, Activation iAct, Activation oAct) {
        if(s.key.rangeInput == Synapse.Builder.OUTPUT) {
            Integer outputBegin = s.key.rangeOutput.begin.map(iAct.range);
            Integer outputEnd = s.key.rangeOutput.end.map(iAct.range);

            if(outputBegin != null && outputBegin.intValue() != oAct.range.begin.intValue() || outputEnd != null && outputEnd.intValue() != oAct.range.end.intValue()) {
                return;
            }
        } else {
            boolean match = false;
            for(Link l: iAct.inputLinks.values()) {
                if(!l.passive && l.synapse.id == s.key.rangeInput || s.key.rangeInput == VARIABLE) {
                    if(l.input.range.begin == oAct.range.begin.intValue() && l.input.range.end == oAct.range.end.intValue()) {
                        match = true;
                        break;
                    }
                }
            }
            if(!match) {
                return;
            }
        }

        Link nl = new Link(s, iAct, oAct, false);
        Link el = oAct.inputLinks.get(nl);
        if(el != null) {
            return;
        }

        if(s.key.identity && s.key.isRecurrent) {
            el = oAct.getLinkBySynapseId(s.id);
            if(el != null && el.input != iAct) {
                splitActivation(el, nl);
                nl.passive = true;
            }
        }

        nl.link();
        if(!nl.passive) {
            addToQueue(nl);
        }
    }


    private void splitActivation(Link el, Link nl) {
        Activation splitAct = new Activation(doc.activationIdCounter++, doc, nl.output.range, nl.output.node);
        nl.output.node.processActivation(splitAct);
        doc.ubQueue.add(splitAct);

        System.out.println("iAct:" + nl.input.id + " oAct:" + nl.output.id + " splitAct:" + splitAct.id);

        for(Link il: nl.output.inputLinks.values()) {
            new Link(il.synapse, il.input, splitAct, il.synapse.id == nl.synapse.id || il.passive).link();
        }
        new Link(nl.synapse, nl.input, splitAct, false).link();

        for(Link ol: nl.output.outputLinks.values()) {
            Link nol = new Link(ol.synapse, splitAct, ol.output, ol.passive);
            nol.link();

            if(!ol.synapse.isNegative() && checkLoop(nl.input, ol.output)) {
                ol.passive = true;
            }
            if(!ol.synapse.isNegative() && checkLoop(el.input, ol.output)) {
                nol.passive = true;
            }
        }
    }


    private boolean checkLoop(Activation iAct, Activation oAct) {
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
