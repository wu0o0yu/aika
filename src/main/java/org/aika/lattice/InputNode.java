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
package org.aika.lattice;


import org.aika.Activation;
import org.aika.Iteration;
import org.aika.Model;
import org.aika.Utils;
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.aika.neuron.Synapse.Key;
import org.aika.neuron.Synapse.RangeSignal;
import org.aika.neuron.Synapse.RangeVisibility;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author Lukas Molzberger
 */
public class InputNode extends Node {

    public Key key;
    public Neuron inputNeuron;

    // Key: Output Neuron
    public Map<SynapseKey, Synapse> synapses;

    private long visitedTrain = -1;

    public InputNode() {}

    public InputNode(Iteration t, Key key) {
        super(t, 1);
        this.key = key;

        Model m = t.m;
        if(m != null) {
            m.stat.nodes++;
            m.stat.nodesPerLevel[level]++;
        }

        endRequired = false;
        ridRequired = false;
        if(key != null) {
            rangeVisibility = new RangeVisibility[] {key.startVisibility, key.endVisibility};
            matchRange = new boolean[] {key.startSignal != RangeSignal.NONE && key.startVisibility == RangeVisibility.MATCH_INPUT, key.endSignal != RangeSignal.NONE && key.endVisibility == RangeVisibility.MATCH_INPUT};
            endRequired = key.startSignal == Synapse.RangeSignal.NONE;
            ridRequired = key.relativeRid != null || key.absoluteRid != null;
        }
    }


    public static InputNode add(Iteration t, Key key, Neuron input) {
        InputNode in = (input != null ? input.outputNodes.get(key) : null);
        if(in != null) {
            return in;
        }
        in = new InputNode(t, key);

        if(input != null) {
            in.inputNeuron = input;
            in.passive = input.node != null && input.node.passive;
            input.outputNodes.put(key, in);
        }
        return in;
    }


    @Override
    protected void changeNumberOfNeuronRefs(Iteration t, long v, int d) {
        ThreadState th = getThreadState(t);
        if(th.visitedNeuronRefsChange == v) return;
        th.visitedNeuronRefsChange = v;
        numberOfNeuronRefs += d;
    }


    @Override
    public void initActivation(Iteration t, Activation act) {
        if(neuron instanceof InputNeuron) {
            t.inputNeuronActivations.add(act);

            if(getThreadState(t).activations.isEmpty()) {
                t.activatedNeurons.add(neuron);
                t.activatedInputNeurons.add(neuron);
            }
        } else if(!isBlocked) {
            t.inputNodeActivations.add(act);
        }
    }


    @Override
    public void deleteActivation(Iteration t, Activation act) {
        if(neuron instanceof InputNeuron) {
            t.inputNeuronActivations.remove(act);

            if(getThreadState(t).activations.isEmpty()) {
                t.activatedNeurons.remove(neuron);
                t.activatedInputNeurons.remove(neuron);
            }
        } else if(!isBlocked) {
            t.inputNodeActivations.remove(act);
        }
    }


    private Activation.Key computeActivationKey(Activation iAct) {
        Activation.Key ak = iAct.key;
        if((key.absoluteRid != null && key.absoluteRid != ak.rid) || ak.o.isConflicting(Option.visitedCounter++)) return null;

        return new Activation.Key(
                this,
                new Range(key.startSignal.getSignalPos(ak.r, Integer.MIN_VALUE), key.endSignal.getSignalPos(ak.r, Integer.MAX_VALUE)),
                key.relativeRid != null ? ak.rid : null,
                ak.o
        );
    }


    @Override
    public void computeNullHyp(Model m) {
        nullHypFreq = frequency;
    }


    @Override
    public boolean isExpandable(boolean checkFrequency) {
        return true;
    }


    @Override
    protected boolean hasSupport(Activation act) {
        for(Activation iAct: act.inputs.values()) {
            if(!iAct.isRemoved && iAct.upperBound > 0.0) return true;
        }

        return false;
    }

    static int dbc = 0;

    protected Range preProcessAddedActivation(Iteration t, Activation.Key ak, Collection<Activation> inputActs) {
        dbc++;
        if(neuron == null && (key.startSignal == Synapse.RangeSignal.NONE || key.endSignal == Synapse.RangeSignal.NONE)) {
            boolean dir = key.startSignal == Synapse.RangeSignal.NONE;
            int pos = ak.r.getBegin(dir);

         // TODO: efficient implementation
/*            for(Activation act: Activation.select(t, this, ak.rid, new Range(pos, pos), Range.Relation.OVERLAPS, ak.o, Option.Relation.CONTAINS)) {
                addActivationInternal(t, new Activation.Key(this, new Range(act.key.r.getBegin(dir), pos).invert(dir), act.key.rid, act.key.o), act.inputs.values(), false);
                act.removedId = Activation.removedIdCounter++;
                act.isRemoved = true;
                removeActivationInternal(t, act, act.inputs.values());
            }
*/
            Activation cAct = Activation.getNextSignal(this, t, pos, ak.rid, ak.o, dir, dir);
            return new Range(ak.r.getBegin(dir), cAct != null ? cAct.key.r.getBegin(dir) : (dir ? Integer.MIN_VALUE : Integer.MAX_VALUE)).invert(dir);
        }
        return ak.r;
    }


    protected void postProcessRemovedActivation(Iteration t, Activation act, Collection<Activation> inputActs) {
        Activation.Key ak = act.key;
        if(neuron == null && (key.startSignal == Synapse.RangeSignal.NONE || key.endSignal == Synapse.RangeSignal.NONE)) {
            boolean dir = key.startSignal == Synapse.RangeSignal.NONE;
            Activation.select(t, this, ak.rid, new Range(ak.r.getBegin(dir), dir ? Integer.MAX_VALUE : Integer.MIN_VALUE).invert(!dir), dir ? Range.Relation.BEGINS_WITH : Range.Relation.ENDS_WITH, ak.o, Option.Relation.CONTAINS).forEach(cAct -> {
                Activation.Key cak = cAct.key;
                processAddedActivation(t, new Activation.Key(cak.n, new Range(dir ? Integer.MIN_VALUE : cak.r.begin, dir ? cak.r.end : Integer.MAX_VALUE), cak.rid, cak.o), cAct.inputs.values());
                cAct.removedId = Activation.removedIdCounter++;
                cAct.isRemoved = true;
                removeActivationInternal(t, cAct, cAct.inputs.values());
            });
        }
    }


    public void addActivation(Iteration t, Activation inputAct) {
        Activation.Key ak = computeActivationKey(inputAct);

        if(ak != null) {
            addActivationAndPropagate(t, ak, Collections.singleton(inputAct));
        }
    }


    public void removeActivation(Iteration t, Activation inputAct) {
        for(Activation act: inputAct.outputs.values()) {
            if(act.key.n == this) {
                removeActivationAndPropagate(t, act, Collections.singleton(inputAct));
            }
        }
    }


    public void propagateAddedActivation(Iteration t, Activation act, Option removedConflict) {
        if(neuron instanceof InputNeuron) {
            if(removedConflict == null) {
                neuron.propagateAddedActivation(t, act);
            }
        } else if(!key.isNeg && !key.isRecurrent) {
            apply(t, act, removedConflict);
        }
    }


    public void propagateRemovedActivation(Iteration t, Activation act) {
        if(neuron instanceof InputNeuron) {
            neuron.propagateRemovedActivation(t, act);
        } else if(!key.isNeg && !key.isRecurrent) {
            removeFromNextLevel(t, act);
        }
    }


    @Override
    public boolean isAllowedOption(Iteration t, Option n, Activation act, long v) {
        return false;
    }


    @Override
    protected Collection<Refinement> collectNodeAndRefinements(Refinement newRef) {
        List<Refinement> result = new ArrayList<>(2);
        result.add(new Refinement(key.relativeRid, newRef.rid, this));
        result.add(newRef);
        return result;
    }

    /**
     *
     * @param t
     * @param act
     * @param removedConflict This parameter contains a removed conflict if it is not null. In this case only expand activations that contain this removed conflict.
     */
    @Override
    public void apply(Iteration t, Activation act, Option removedConflict) {
        // Check if the activation has been deleted in the meantime.
        if(act.isRemoved || passive) {
            return;
        }

        lock.acquireReadLock();
        if(andChildren != null) {
            for (Map.Entry<Refinement, AndNode> me : andChildren.entrySet()) {
                addNextLevelActivations(t, me.getKey().input, me.getKey(), me.getValue(), act, removedConflict);
            }
        }
        lock.releaseReadLock();

        if(removedConflict == null) {
            OrNode.processCandidate(t, this, act, false);
        }
    }


    private static void addNextLevelActivations(Iteration t, InputNode secondNode, Refinement ref, AndNode nlp, Activation act, Option removedConflict) {
        Activation.Key ak = act.key;
        Integer secondRid = Utils.nullSafeAdd(ak.rid, false, ref.rid, false);

        Activation.select(t, secondNode, secondRid, ak.r, new Range.BeginEndMatcher(ak.n.matchRange, secondNode.matchRange), null, null)
                .forEach(secondAct -> {
            if(!secondAct.isRemoved) {
                Option o = Option.add(t.doc, true, ak.o, secondAct.key.o);
                if (o != null && (removedConflict == null || o.contains(removedConflict, false))) {
                    nlp.addActivation(t,
                            new Activation.Key(
                                    nlp,
                                    Range.applyVisibility(ak.r, ak.n.rangeVisibility, secondAct.key.r, secondAct.key.n.rangeVisibility),
                                    Utils.nullSafeMin(ak.rid, secondAct.key.rid),
                                    o
                            ),
                            AndNode.prepareInputActs(act, secondAct)
                    );
                }
            }
        });
    }


    @Override
    public void discover(Iteration t, Activation act) {
        long v = Node.visitedCounter++;

        for(Activation secondAct: t.inputNodeActivations) {
            Refinement ref = new Refinement(secondAct.key.rid, act.key.rid, (InputNode) secondAct.key.n);
            Range.BeginEndMatcher mr = new Range.BeginEndMatcher(matchRange, ref.input.matchRange);
            Integer ridDelta = Utils.nullSafeSub(act.key.rid, false, secondAct.key.rid, false);

            if(act != secondAct &&
                    this != ref.input &&
                    ref.input.visitedTrain != v &&
                    !ref.input.key.isNeg &&
                    !ref.input.key.isRecurrent &&
                    (mr.match(act.key.r, secondAct.key.r) || (ridDelta != null && ridDelta < AndNode.MAX_RID_RANGE))) {
                ref.input.visitedTrain = v;
                AndNode.createNextLevelNode(t, this, ref, true);
            }
        }
    }


    @Override
    public double computeSynapseWeightSum(Integer offset, Neuron n) {
        return n.bias + Math.abs(getSynapse(new SynapseKey(key.relativeRid == null ? null : offset, n)).w);
    }


    public Synapse getSynapse(SynapseKey sk) {
        lock.acquireReadLock();
        Synapse s = synapses != null ? synapses.get(sk) : null;
        lock.releaseReadLock();
        return s;
    }


    public void setSynapse(Iteration t, SynapseKey sk, Synapse s) {
        lock.acquireWriteLock(t.threadId);
        if(synapses == null) {
            synapses = new TreeMap<>();
        }
        synapses.put(sk, s);
        lock.releaseWriteLock();
    }


    @Override
    public void cleanup(Iteration t) {
    }


    @Override
    public void remove(Iteration t) {
        inputNeuron.outputNodes.remove(key);
        super.remove(t);
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key.isNeg ? "N" : "P");
        sb.append(key.isRecurrent ? "R" : "");
        if(key.startSignal == RangeSignal.NONE) {
            sb.append("|");
        } else if(key.startSignal == RangeSignal.START) {
            if(key.startVisibility == RangeVisibility.MAX_OUTPUT) {
                sb.append("<");
            } else {
                sb.append("[");
            }
        } else if(key.startSignal == RangeSignal.END) {
            if(key.startVisibility == RangeVisibility.MAX_OUTPUT) {
                sb.append(">");
            } else {
                sb.append("]");
            }
        }
        if(inputNeuron != null) {
            sb.append(inputNeuron.id);
            if(inputNeuron.label != null) {
                sb.append(",");
                sb.append(inputNeuron.label);
            }
        }
        if(key.endSignal == RangeSignal.NONE) {
            sb.append("|");
        } else if(key.endSignal == RangeSignal.START) {
            if(key.endVisibility == RangeVisibility.MAX_OUTPUT) {
                sb.append("<");
            } else {
                sb.append("[");
            }
        } else if(key.endSignal == RangeSignal.END) {
            if(key.endVisibility == RangeVisibility.MAX_OUTPUT) {
                sb.append(">");
            } else {
                sb.append("]");
            }
        }
        return sb.toString();
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF("I");
        super.write(out);
        key.write(out);
    }


    @Override
    public void readFields(DataInput in, Iteration t) throws IOException {
        super.readFields(in, t);
        key = Key.read(in, t);
    }



    public static class SynapseKey implements Comparable<SynapseKey> {
        final Integer rid;
        final Neuron n;

        public SynapseKey(Integer rid, Neuron n) {
            this.rid = rid;
            this.n = n;
        }


        @Override
        public int compareTo(SynapseKey sk) {
            int r = Utils.compareInteger(rid, sk.rid);
            if(r != 0) return r;
            return n.compareTo(sk.n);
        }
    }
}
