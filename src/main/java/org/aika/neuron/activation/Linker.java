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
package org.aika.neuron.activation;

import org.aika.Utils;
import org.aika.Document;
import org.aika.neuron.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.stream.Stream;

import static org.aika.neuron.activation.Linker.Direction.INPUT;
import static org.aika.neuron.activation.Linker.Direction.OUTPUT;

/**
 * The {@code Linker} class is responsible for for the linkage of neuron activations. These links mirror the synapses between
 * the neurons of these activations.
 *
 * @author Lukas Molzberger
 */
public class Linker {

    public enum Direction {
        INPUT,
        OUTPUT
    }

    public enum SortGroup {
        RANGE_BEGIN(
                new Synapse.Key(true, false, null, Range.Relation.BEGIN_EQUALS),
                new Synapse.Key(false, true, null, Range.Relation.BEGIN_EQUALS)
        ),
        RANGE_END(
                new Synapse.Key(true, false, null, Range.Relation.END_EQUALS),
                new Synapse.Key(false, true, null, Range.Relation.END_EQUALS)
        ),
        RID_ZERO(
                new Synapse.Key(true, false, 0, Range.Relation.NONE),
                new Synapse.Key(false, true, 0, Range.Relation.NONE)
        ),
        OTHERS(
                new Synapse.Key(true, false, null, Range.Relation.NONE),
                new Synapse.Key(false, true, null, Range.Relation.NONE)
        );

        Synapse begin;
        Synapse end;

        SortGroup(Synapse.Key beginKey, Synapse.Key endKey) {
            this.begin = new Synapse(Neuron.MIN_NEURON, Neuron.MIN_NEURON, beginKey);
            this.end = new Synapse(Neuron.MAX_NEURON, Neuron.MAX_NEURON, endKey);
        }


        public static int compare(Synapse.Key ka, Synapse.Key kb) {
            return getSortGroup(ka).compareTo(getSortGroup(kb));
        }


        public static SortGroup getSortGroup(Synapse.Key k) {
            if(k.rangeMatch.beginToBegin == Range.Operator.EQUALS) return SortGroup.RANGE_BEGIN;
            else if(k.rangeMatch.endToEnd == Range.Operator.EQUALS) return SortGroup.RANGE_END;
            else if(k.relativeRid != null && k.relativeRid == 0) return SortGroup.RID_ZERO;
            else return SortGroup.OTHERS;
        }


        public boolean checkActivation(Activation.Key ak) {
            switch(this) {
                case RANGE_BEGIN:
                    return ak.range.begin != Integer.MIN_VALUE;
                case RANGE_END:
                    return ak.range.end != Integer.MAX_VALUE;
                case RID_ZERO:
                    return ak.rid != null;
                default:
                    return true;
            }
        }
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
        link(act, INPUT);
        link(act, OUTPUT);
        n.provider.lock.releaseReadLock();
        n.lock.releaseReadLock();

        long v = act.doc.visitedCounter++;
        Conflicts.linkConflicts(act, v, INPUT);
        Conflicts.linkConflicts(act, v, OUTPUT);
    }


    private static void link(Activation act, Direction dir) {
        Neuron n = act.getNeuron();
        NavigableMap<Synapse, Synapse> syns = (dir == INPUT ? n.inMemoryInputSynapses : n.inMemoryOutputSynapses);
        int[] sortGroupCounts = (dir == INPUT ? n.inputSortGroupCounts : n.outputSortGroupCounts);

        if(syns.isEmpty()) return;

        if (syns.size() < 10) {
            // No need for further optimizations.
            linkOthers(act, dir, syns.values());
            return;
        }

        for(SortGroup sg: new SortGroup[] {SortGroup.RANGE_BEGIN, SortGroup.RANGE_END, SortGroup.RID_ZERO}) {
            if(sortGroupCounts[sg.ordinal()] > 0) {
                link(act, dir, sg, syns);
            }
        }

        NavigableMap<Synapse, Synapse> remainingSyns = syns.subMap(SortGroup.OTHERS.begin, true, SortGroup.OTHERS.end, true);
        if(remainingSyns.isEmpty()) return;

        // Optimization in case the set of synapses is very large
        if (act.doc.activatedNeurons.size() * 20 > sortGroupCounts[SortGroup.OTHERS.ordinal()]) {
            linkOthers(act, dir, remainingSyns.values());
        } else {
            linkOthers(act, dir, getActiveSynapses(act.doc, dir, remainingSyns));
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
                ) && testRelationMatch(s, iAct, oAct)) {
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

            if(testRelationMatch(s, iAct, oAct)) {
                Activation.SynapseActivation sa = new Activation.SynapseActivation(s, iAct, oAct);
                iAct.addSynapseActivation(INPUT, sa);
                oAct.addSynapseActivation(OUTPUT, sa);
            }
        });
    }


    private static Integer computeTargetRID(Activation act, Direction dir, Synapse.Key sk) {
        switch (dir) {
            case INPUT:
                return sk.absoluteRid != null ? sk.absoluteRid : Utils.nullSafeAdd(act.key.rid, false, sk.relativeRid, false);
            case OUTPUT:
                return Utils.nullSafeSub(act.key.rid, false, sk.relativeRid, false);
        }
        return null;
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


    public static boolean testRelationMatch(Synapse s, Activation iAct, Activation oAct) {
        for(Synapse.Relation sr: s.relations) {
            Activation.SynapseActivation linkedSa = lookupSynapse(oAct, sr.linkedSynapse);
            if(!testRelationMatch(sr.type, iAct, linkedSa.input)) return false;
        }
        return true;
    }


    public static boolean testRelationMatch(Synapse.Relation.Type type, Activation act, Activation linkedAct) {
        switch(type) {
            case COMMON_ANCESTOR:
                return hasCommonAncestor(act, linkedAct);
            case CONTAINS:
                return contains(act, linkedAct, act.doc.visitedCounter++);
            case CONTAINED_IN:
                return contains(linkedAct, act, act.doc.visitedCounter++);
        }
        return true;
    }


    private static boolean contains(Activation actA, Activation actB, long v) {
        if(actA.visited == v) return false;
        actA.visited = v;

        if(actA == actB) return true;

        for(Activation.SynapseActivation sa: actA.neuronInputs) {
            if(!sa.synapse.key.isRecurrent) {
                if(contains(sa.input, actB, v)) return true;
            }
        }
        return false;
    }




    private static boolean hasCommonAncestor(Activation act, Activation linkedAct) {
        long v = act.doc.visitedCounter++;
        markAncestors(linkedAct, v);
        return hasCommonAncestor(act, v, act.doc.visitedCounter++);
    }


    private static void markAncestors(Activation act, long v) {
        if(act.visited == v) return;
        act.visited = v;

        act.markedAncestor = v;

        for(Activation.SynapseActivation sa: act.neuronInputs) {
            if(!sa.synapse.key.isRecurrent) {
                markAncestors(sa.input, v);
            }
        }
    }


    private static boolean hasCommonAncestor(Activation act, long v1, long v2) {
        if(act.visited == v2) return false;
        act.visited = v2;

        if(act.markedAncestor == v1) return true;

        for(Activation.SynapseActivation sa: act.neuronInputs) {
            if(!sa.synapse.key.isRecurrent) {
                if(hasCommonAncestor(sa.input, v1, v2)) return true;
            }
        }
        return false;
    }


    private static Activation.SynapseActivation lookupSynapse(Activation oAct, Synapse s) {
        for(Activation.SynapseActivation sa: oAct.neuronInputs) {
            if(sa.synapse == s) {
                return sa;
            }
        }
        return null;
    }
}
