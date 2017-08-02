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
import org.aika.Model;
import org.aika.Utils;
import org.aika.corpus.Document;
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.neuron.InputNeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.aika.neuron.Synapse.Key;
import org.aika.neuron.Synapse.RangeSignal;
import org.aika.neuron.Synapse.RangeMatch;

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

    public InputNode(Document doc, Key key) {
        super(doc, 1);
        this.key = key;

        Model m = doc.m;
        if(m != null) {
            m.stat.nodes++;
            m.stat.nodesPerLevel[level]++;
        }

        endRequired = false;
        ridRequired = false;
        if(key != null) {
            endRequired = key.startSignal == Synapse.RangeSignal.NONE;
            ridRequired = key.relativeRid != null || key.absoluteRid != null;
        }
    }


    public static InputNode add(Document doc, Key key, Neuron input) {
        InputNode in = (input != null ? input.outputNodes.get(key) : null);
        if(in != null) {
            return in;
        }
        in = new InputNode(doc, key);

        if(input != null) {
            in.inputNeuron = input;
            in.passive = input.node != null && input.node.passive;
            input.outputNodes.put(key, in);
        }
        return in;
    }


    @Override
    protected void changeNumberOfNeuronRefs(Document doc, long v, int d) {
        ThreadState th = getThreadState(doc, true);
        if(th.visitedNeuronRefsChange == v) return;
        th.visitedNeuronRefsChange = v;
        numberOfNeuronRefs += d;
    }


    @Override
    public void initActivation(Document doc, Activation act) {
        if(neuron instanceof InputNeuron) {
            doc.inputNeuronActivations.add(act);

            ThreadState th = getThreadState(doc, false);
            if(th == null || th.activations.isEmpty()) {
                doc.activatedNeurons.add(neuron);
                doc.activatedInputNeurons.add(neuron);
            }
        } else if(!isBlocked) {
            doc.inputNodeActivations.add(act);
        }
    }


    @Override
    public void deleteActivation(Document doc, Activation act) {
        if(neuron instanceof InputNeuron) {
            doc.inputNeuronActivations.remove(act);

            ThreadState th = getThreadState(doc, false);
            if(th == null || th.activations.isEmpty()) {
                doc.activatedNeurons.remove(neuron);
                doc.activatedInputNeurons.remove(neuron);
            }
        } else if(!isBlocked) {
            doc.inputNodeActivations.remove(act);
        }
    }


    private Activation.Key computeActivationKey(Activation iAct) {
        Activation.Key ak = iAct.key;
        if((key.absoluteRid != null && key.absoluteRid != ak.rid) || ak.o.isConflicting(Option.visitedCounter++)) return null;

        return new Activation.Key(
                this,
                new Range(key.startSignal.getSignalPos(ak.r), key.endSignal.getSignalPos(ak.r)),
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


    protected Range preProcessAddedActivation(Document doc, Activation.Key ak, Collection<Activation> inputActs) {
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
            Activation cAct = Activation.getNextSignal(this, doc, pos, ak.rid, ak.o, dir, dir);
            return new Range(ak.r.getBegin(dir), cAct != null ? cAct.key.r.getBegin(dir) : (dir ? Integer.MIN_VALUE : Integer.MAX_VALUE)).invert(dir);
        }
        return ak.r;
    }


    protected void postProcessRemovedActivation(Document doc, Activation act, Collection<Activation> inputActs) {
        Activation.Key ak = act.key;
        if(neuron == null && (key.startSignal == Synapse.RangeSignal.NONE || key.endSignal == Synapse.RangeSignal.NONE)) {
            boolean dir = key.startSignal == Synapse.RangeSignal.NONE;
            Activation.select(doc, this, ak.rid, new Range(ak.r.getBegin(dir), dir ? Integer.MAX_VALUE : Integer.MIN_VALUE).invert(!dir), dir ? RangeMatch.EQUALS : RangeMatch.NONE, dir ? RangeMatch.NONE : RangeMatch.EQUALS, ak.o, Option.Relation.CONTAINS).forEach(cAct -> {
                Activation.Key cak = cAct.key;
                processAddedActivation(doc, new Activation.Key(cak.n, new Range(dir ? Integer.MIN_VALUE : cak.r.begin, dir ? cak.r.end : Integer.MAX_VALUE), cak.rid, cak.o), cAct.inputs.values());
                cAct.removedId = Activation.removedIdCounter++;
                cAct.isRemoved = true;
                removeActivationInternal(doc, cAct, cAct.inputs.values());
            });
        }
    }


    public void addActivation(Document doc, Activation inputAct) {
        Activation.Key ak = computeActivationKey(inputAct);

        if(ak != null) {
            addActivationAndPropagate(doc, ak, Collections.singleton(inputAct));
        }
    }


    public void removeActivation(Document doc, Activation inputAct) {
        for(Activation act: inputAct.outputs.values()) {
            if(act.key.n == this) {
                removeActivationAndPropagate(doc, act, Collections.singleton(inputAct));
            }
        }
    }


    public void propagateAddedActivation(Document doc, Activation act, Option removedConflict) {
        if(neuron instanceof InputNeuron) {
            if(removedConflict == null) {
                neuron.propagateAddedActivation(doc, act);
            }
        } else if(!key.isNeg && !key.isRecurrent) {
            apply(doc, act, removedConflict);
        }
    }


    public void propagateRemovedActivation(Document doc, Activation act) {
        if(neuron instanceof InputNeuron) {
            neuron.propagateRemovedActivation(doc, act);
        } else if(!key.isNeg && !key.isRecurrent) {
            removeFromNextLevel(doc, act);
        }
    }


    @Override
    public boolean isAllowedOption(Document doc, Option n, Activation act, long v) {
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
     * @param doc
     * @param act
     * @param removedConflict This parameter contains a removed conflict if it is not null. In this case only expand activations that contain this removed conflict.
     */
    @Override
    public void apply(Document doc, Activation act, Option removedConflict) {
        // Check if the activation has been deleted in the meantime.
        if(act.isRemoved || passive) {
            return;
        }

        lock.acquireReadLock();
        if(andChildren != null) {
            for (Map.Entry<Refinement, AndNode> me : andChildren.entrySet()) {
                addNextLevelActivations(doc, me.getKey().input, me.getKey(), me.getValue(), act, removedConflict);
            }
        }
        lock.releaseReadLock();

        if(removedConflict == null) {
            OrNode.processCandidate(doc, this, act, false);
        }
    }


    static int dbc = 0;
    private static void addNextLevelActivations(Document doc, InputNode secondNode, Refinement ref, AndNode nlp, Activation act, Option removedConflict) {
        Activation.Key ak = act.key;
        InputNode firstNode = ((InputNode) ak.n);
        Integer secondRid = Utils.nullSafeAdd(ak.rid, false, ref.rid, false);

        dbc++;
        System.out.println();
        Activation.select(
                doc,
                secondNode,
                secondRid,
                ak.r,
                computeStartRangeMatch(firstNode.key, secondNode.key),
                computeEndRangeMatch(firstNode.key, secondNode.key),
                null,
                null
        ).forEach(secondAct -> {
            if(!secondAct.isRemoved) {
                Option o = Option.add(doc, true, ak.o, secondAct.key.o);
                if (o != null && (removedConflict == null || o.contains(removedConflict, false))) {
                    nlp.addActivation(doc,
                            new Activation.Key(
                                    nlp,
                                    Range.mergeRange(
                                            Range.getOutputRange(ak.r, new boolean[]{ firstNode.key.startRangeOutput, firstNode.key.endRangeOutput}),
                                            Range.getOutputRange(secondAct.key.r, new boolean[]{ secondNode.key.startRangeOutput, secondNode.key.endRangeOutput})
                                    ),
                                    Utils.nullSafeMin(ak.rid, secondAct.key.rid),
                                    o
                            ),
                            AndNode.prepareInputActs(act, secondAct)
                    );
                }
            }
        });
    }


    private static RangeMatch computeStartRangeMatch(Key k1, Key k2) {
        if(k1.startRangeOutput && k2.startRangeOutput) {
            return RangeMatch.EQUALS;
        } else if(k1.startRangeOutput) {
            return RangeMatch.invert(k2.startRangeMatch);
        } else if(k2.startRangeOutput) {
            return k1.startRangeMatch;
        }
        return RangeMatch.NONE;
    }


    private static RangeMatch computeEndRangeMatch(Key k1, Key k2) {
        if(k1.endRangeOutput && k2.endRangeOutput) {
            return RangeMatch.EQUALS;
        } else if(k1.endRangeOutput) {
            return RangeMatch.invert(k2.endRangeMatch);
        } else if(k2.endRangeOutput) {
            return k1.endRangeMatch;
        }
        return RangeMatch.NONE;
    }


    @Override
    public void discover(Document doc, Activation act) {
        long v = Node.visitedCounter++;

        for(Activation secondAct: doc.inputNodeActivations) {
            Refinement ref = new Refinement(secondAct.key.rid, act.key.rid, (InputNode) secondAct.key.n);
            RangeMatch srm = computeStartRangeMatch(key, ref.input.key);
            RangeMatch erm = computeEndRangeMatch(key, ref.input.key);
            Integer ridDelta = Utils.nullSafeSub(act.key.rid, false, secondAct.key.rid, false);

            if(     act != secondAct &&
                    this != ref.input &&
                    ref.input.visitedTrain != v &&
                    !ref.input.key.isNeg &&
                    !ref.input.key.isRecurrent &&
                    ((srm.compare(act.key.r.begin, secondAct.key.r.begin) && erm.compare(act.key.r.end, secondAct.key.r.end)) ||
                            (ridDelta != null && ridDelta < AndNode.MAX_RID_RANGE))) {
                ref.input.visitedTrain = v;
                AndNode.createNextLevelNode(doc, this, ref, true);
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


    public void setSynapse(Document doc, SynapseKey sk, Synapse s) {
        lock.acquireWriteLock(doc.threadId);
        if(synapses == null) {
            synapses = new TreeMap<>();
        }
        synapses.put(sk, s);
        lock.releaseWriteLock();
    }


    @Override
    public void cleanup(Document doc) {
    }


    @Override
    public void remove(Document doc) {
        inputNeuron.outputNodes.remove(key);
        super.remove(doc);
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key.isNeg ? "N" : "P");
        sb.append(key.isRecurrent ? "R" : "");

        sb.append(getRangeBrackets(key.startRangeOutput, key.startSignal));

        if(inputNeuron != null) {
            sb.append(inputNeuron.id);
            if(inputNeuron.label != null) {
                sb.append(",");
                sb.append(inputNeuron.label);
            }
        }

        sb.append(getRangeBrackets(key.endRangeOutput, key.endSignal));

        return sb.toString();
    }


    private String getRangeBrackets(boolean ro, RangeSignal rs) {
        if(rs == RangeSignal.NONE) return "|";
        else if(ro) return rs == RangeSignal.START ? "[" : "]";
        else if(!ro) return rs == RangeSignal.START ? "<" : ">";
        else return "|";
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF("I");
        super.write(out);
        key.write(out);
    }


    @Override
    public void readFields(DataInput in, Document doc) throws IOException {
        super.readFields(in, doc);
        key = Key.read(in, doc);
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
