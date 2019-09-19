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
package network.aika.neuron.activation.link;

import network.aika.Document;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.relation.Relation;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.RelationEndpoint;

import java.util.*;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.Synapse.State.CURRENT;
import static network.aika.neuron.activation.link.Direction.INPUT;

/**
 * The {@code Linker} class is responsible for for the linkage of neuron activations. These links mirror the synapses between
 * the neurons of these activations.
 *
 * @author Lukas Molzberger
 */
public class Linker {

    private Document doc;
    private ArrayDeque<Link> queue = new ArrayDeque<>();


    public Linker(Document doc) {
        this.doc = doc;
    }


    public Activation computeInputActivation(Synapse s, Activation iAct) {
        return iAct;
    }


    private void linkOutputRelations(Activation act) {
        linkRelated(act, act, act.getINeuron());
    }


    public void linkInput(Activation act) {
        Document doc = act.getDocument();

        Neuron n = act.getNeuron();
        if(n.getType() == INeuron.Type.EXCITATORY) {
            for (Synapse s : n.getActiveInputSynapses()) {
                for (Relation.Key rk : s.getOutputRelations()) {
                    rk.getRelation().getActivations(s.getInput().get(doc), act, rk.getDirection())
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

                act.getInputLinks()
                        .forEach(l -> addToQueue(l));
            }
            doc.getLinker().process();
            doc.propagate();
        } while(oldSize != doc.getNumberOfActivations());
    }


    public void process() {
        while(!queue.isEmpty()) {
            Link l = queue.pollFirst();
            linkRelated(l.getInput(), l.getOutput(), l.getSynapse());
        }
    }


    private void linkRelated(Activation rAct, Activation oAct, RelationEndpoint re) {
        Document doc = rAct.getDocument();
        for(Relation.Key rk: re.getRelations()) {
            if(rk.getRelatedId() != OUTPUT) {
                Synapse s = oAct.getSynapseById(rk.getRelatedId());
                if (s != null) {
                    rk.getRelation().getActivations(s.getInput().get(doc), rAct, rk.getInvertedDirection())
                            .forEach(iAct -> link(s, iAct, oAct));
                }
            }
        }
    }


    public void link(Synapse s, Activation iAct, Activation oAct) {
        iAct = computeInputActivation(s, iAct);

        if(iAct == null || iAct.blocked || !checkRelations(s, iAct, oAct)) {
            return;
        }

        Link nl = new Link(s, iAct, oAct);
        if(oAct.getInputLink(nl) != null) {
            return;
        }

        if(s.isIdentity()) {
            Link el = oAct.getLinkBySynapseId(s.getId());
            if(el != null && el.getInput() != iAct) {
                return;
            }
        }

        nl.link();

        addToQueue(nl);
    }


    private boolean checkRelations(Synapse s, Activation iAct, Activation oAct) {
        for(Relation.Key rk: s.getRelations()) {
            if(rk.getRelatedId() == Synapse.OUTPUT) {
                if (!rk.getRelation().test(iAct, oAct, true, rk.getDirection())) {
                    return false;
                }
            } else {
                Synapse relSyn = oAct.getSynapseById(rk.getRelatedId());
                if(relSyn!= null && oAct.getLinksBySynapse(INPUT, relSyn)
                        .anyMatch(l -> !rk.getRelation().test(iAct, l.getInput(), false, rk.getDirection()))) {
                    return false;
                }
            }
        }

        return true;
    }


    private void addToQueue(Link l) {
        if(l == null) {
            return;
        }
        if(!l.getSynapse().isNegative(CURRENT)) {
            queue.add(l);
        }
        doc.getUpperBoundQueue().add(l);
    }
}
