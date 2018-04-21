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
import network.aika.lattice.Node;
import network.aika.lattice.OrNode;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Relation;
import network.aika.neuron.Synapse;
import network.aika.neuron.*;
import network.aika.neuron.activation.Activation.SynapseActivation;

import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Linker.Direction.INPUT;
import static network.aika.neuron.activation.Linker.Direction.OUTPUT;

/**
 * The {@code Linker} class is responsible for for the linkage of neuron activations. These links mirror the synapses between
 * the neurons of these activations.
 *
 * @author Lukas Molzberger
 */
public class Linker {


    ArrayDeque<Activation.SynapseActivation> queue = new ArrayDeque<>();

    public enum Direction {
        INPUT,
        OUTPUT
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
        linkInput(act);
        linkRelated(act);
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

                link(s, iAct, act);
            }
        }
    }



    private void linkRelated(Activation act) {
        Document doc = act.doc;
        for(int da = 0; da < 2; da++) {
            int p = da == 0 ? act.range.begin : act.range.end;

            for(int db = 0; db < 2; db++) {
                SortedMap<Document.ActKey, Activation> acts = db == 0 ?
                        doc.activationsByRangeBegin.subMap(
                                new Document.ActKey(new Range(p, Integer.MIN_VALUE), Node.MIN_NODE),
                                new Document.ActKey(new Range(p, Integer.MAX_VALUE), Node.MAX_NODE)) :
                        doc.activationsByRangeEnd.subMap(
                                new Document.ActKey(new Range(Integer.MIN_VALUE, p), Node.MIN_NODE),
                                new Document.ActKey(new Range(Integer.MAX_VALUE, p), Node.MAX_NODE));

                for(Activation rAct: acts.values()) {
                    for(SynapseActivation sa: rAct.neuronOutputs) {
                        for(Map.Entry<Synapse, Relation> me: sa.synapse.relations.entrySet()) {
                            Synapse s = me.getKey();
                            Relation r = me.getValue();

                            if(s.input == act.node.neuron && r.test(rAct, act)) {
                                link(s, act, sa.output);
                            }
                        }
                    }
                }
            }
        }
    }



    private void link(Synapse s, Activation iAct, Activation oAct) {
        SynapseActivation sa = new SynapseActivation(s, iAct, oAct);
        if(!oAct.neuronInputs.contains(sa)) {
            iAct.addSynapseActivation(INPUT, sa);
            oAct.addSynapseActivation(OUTPUT, sa);

            queue.add(sa);
        }
    }


    private static void link(Activation act, Direction dir, SortGroup sortGroup, NavigableMap<Synapse, Synapse> syns) {
        assert sortGroup.checkActivation(act.key);

        Selector.select(sortGroup, act).forEach(rAct -> {
            Activation oAct = (dir == INPUT ? act : rAct);
            Activation iAct = (dir == INPUT ? rAct : act);

            for(Synapse s: syns.subMap(
                    new Synapse(iAct.getNeuron(), oAct.getNeuron(), sortGroup.begin.key), true,
                    new Synapse(iAct.getNeuron(), oAct.getNeuron(), sortGroup.end.key), true).values()) {
                Synapse.Key sk = s.key;
                if(rAct.filter(null,
                        computeTargetRID(act, dir, sk),
                        act.key.range,
                        dir == INPUT ? sk.rangeMatch.invert() : sk.rangeMatch
                ) && testRelation(s, iAct, oAct)) {
                    Activation.SynapseActivation sa = new Activation.SynapseActivation(s, iAct, oAct);
                    iAct.addSynapseActivation(INPUT, sa);
                    oAct.addSynapseActivation(OUTPUT, sa);
                }
            }
        });
    }


    private static void linkOthers(Activation act, Direction dir, Collection<Synapse> syns) {
        Document doc = act.doc;
        for (Synapse s : syns) {
            Neuron p = (dir == INPUT ? s.input : s.output);
            INeuron an = p.getIfNotSuspended();
            if (an != null) {
                INeuron.ThreadState th = an.getThreadState(doc.threadId, false);
                if (th == null || th.activations.isEmpty()) continue;

                linkActSyn(an, act, dir, s);
            }
        }
    }


    private static void linkActSyn(INeuron n, Activation act, final Direction dir, Synapse s) {
        Synapse.Key sk = s.key;

        Stream<Activation> tmp = Selector.select(
                act.doc,
                n,
                computeTargetRID(act, dir, sk),
                act.key.range,
                dir == INPUT ? sk.rangeMatch.invert() : sk.rangeMatch
        );

        tmp.forEach(rAct -> {
            Activation oAct = (dir == INPUT ? act : rAct);
            Activation iAct = (dir == INPUT ? rAct : act);

            if(testRelation(s, iAct, oAct)) {
                Activation.SynapseActivation sa = new Activation.SynapseActivation(s, iAct, oAct);
                iAct.addSynapseActivation(INPUT, sa);
                oAct.addSynapseActivation(OUTPUT, sa);
            }
        });
    }



    private static Collection<Synapse> getActiveSynapses(Document doc, Direction dir, NavigableMap<Synapse, Synapse> syns) {
        ArrayList<Synapse> results = new ArrayList<>();
        Synapse lk = new Synapse(null, null, SortGroup.OTHERS.begin.key);
        Synapse uk = new Synapse(null, null, SortGroup.OTHERS.end.key);

        for (INeuron n : doc.activatedNeurons) {
            if (dir == INPUT) {
                lk.input = n.provider;
                uk.input = n.provider;
            } else {
                lk.output = n.provider;
                uk.output = n.provider;
            }

            // Using addAll is not efficient here.
            for (Synapse s : syns.subMap(lk, true, uk, true).values()) {
                results.add(s);
            }
        }

        return results;
    }


    public static boolean testRelation(Synapse s, Activation iAct, Activation oAct) {
        for(Relation sr: s.relations.values()) {
            Activation.SynapseActivation linkedSA = lookupSynapse(oAct, sr.linkedSynapse);
            if(linkedSA == null) return true;
            if(!sr.test(iAct, linkedSA.input)) return false;
        }
        return true;
    }


    public static Activation.SynapseActivation lookupSynapse(Activation oAct, Synapse s) {
        for(Activation.SynapseActivation sa: oAct.neuronInputs) {
            if(sa.synapse == s) {
                return sa;
            }
        }
        return null;
    }
}
