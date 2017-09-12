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


import org.aika.*;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.lattice.AndNode.Refinement;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;
import org.aika.neuron.Synapse.Key;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.aika.corpus.Range.Operator.*;


/**
 * The {@code InputNode} class is the input layer for the boolean logic. The input-node has two sources of
 * activations. First, it might be underlying logic node of an {@code InputNeuron} in which case the input
 * activations come from the outside. The second option is that the activation come from the output of another neuron.
 *
 * @author Lukas Molzberger
 */
public class InputNode extends Node<InputNode, NodeActivation<InputNode>> {

    public Key key;
    public Neuron inputNeuron;

    // Key: Output Neuron
    Map<SynapseKey, Synapse> synapses;

    private long visitedTrain = -1;

    public InputNode() {}

    public InputNode(Model m, Key key) {
        super(m, 1);
        this.key = Synapse.lookupKey(key);

        if(m != null) {
            m.stat.nodes++;
            m.stat.nodesPerLevel[level]++;
        }

        endRequired = false;
        ridRequired = false;
        if(key != null) {
            endRequired = key.startRangeMapping == Mapping.NONE;
            ridRequired = key.relativeRid != null || key.absoluteRid != null;
        }
    }


    public static InputNode add(Model m, Key key, INeuron input) {
        Provider<InputNode> pin = (input != null ? input.outputNodes.get(key) : null);
        if(pin != null) {
            return pin.get();
        }
        InputNode in = new InputNode(m, key);

        if(input != null && in.inputNeuron == null) {
            in.inputNeuron = input.provider;
            input.outputNodes.put(key, in.provider);
            input.provider.setModified();
        }
        return in;
    }


    @Override
    void changeNumberOfNeuronRefs(int threadId, long v, int d) {
        ThreadState th = getThreadState(threadId, true);
        if(th.visitedNeuronRefsChange == v) return;
        th.visitedNeuronRefsChange = v;
        numberOfNeuronRefs += d;
    }


    @Override
    protected NodeActivation<InputNode> createActivation(Document doc, NodeActivation.Key ak, boolean isTrainingAct) {
        NodeActivation<InputNode> act = new NodeActivation<>(doc.activationIdCounter++, ak);
        act.isTrainingAct = isTrainingAct;
        return act;
    }


    @Override
    public void deleteActivation(Document doc, NodeActivation act) {
    }


    private NodeActivation.Key computeActivationKey(NodeActivation iAct) {
        NodeActivation.Key ak = iAct.key;
        if((key.absoluteRid != null && key.absoluteRid != ak.rid) || ak.o.isConflicting(ak.o.doc.visitedCounter++)) return null;

        return new NodeActivation.Key(
                this,
                new Range(key.startRangeMapping.getSignalPos(ak.r), key.endRangeMapping.getSignalPos(ak.r)),
                key.relativeRid != null ? ak.rid : null,
                ak.o
        );
    }

    @Override
    public void computeNullHyp(Model m) {
        nullHypFreq = frequency;
    }


    @Override
    boolean isExpandable(boolean checkFrequency) {
        return true;
    }


    @Override
    boolean hasSupport(NodeActivation<InputNode> act) {
        for(NodeActivation iAct: act.inputs.values()) {
            Activation iNAct = (Activation) iAct;
            if(!iAct.isRemoved && iNAct.upperBound > 0.0) return true;
        }

        return false;
    }


    @Override
    NodeActivation<InputNode> processAddedActivation(Document doc, NodeActivation.Key<InputNode> ak, Collection<NodeActivation> inputActs, boolean isTrainingAct) {
        Range r = ak.r;
        if(key.startRangeMapping == Mapping.NONE || key.endRangeMapping == Mapping.NONE) {
            boolean dir = key.startRangeMapping == Mapping.NONE;
            int pos = ak.r.getBegin(dir);

            List<NodeActivation> tmp = NodeActivation.select(
                    doc,
                    this,
                    ak.rid,
                    new Range(pos, pos),
                    LESS_THAN,
                    GREATER_THAN,
                    ak.o,
                    InterprNode.Relation.CONTAINS
            ).collect(Collectors.toList());

            for(NodeActivation act: tmp) {
                super.processAddedActivation(doc, new NodeActivation.Key(this, new Range(act.key.r.getBegin(dir), pos).invert(dir), act.key.rid, act.key.o), act.inputs.values(), false);
                act.removedId = NodeActivation.removedIdCounter++;
                act.isRemoved = true;
                super.processRemovedActivation(doc, act, act.inputs.values());
            }

            NodeActivation cAct = NodeActivation.getNextSignal(this, doc, pos, ak.rid, ak.o, dir, dir);
            r = new Range(ak.r.getBegin(dir), cAct != null ? cAct.key.r.getBegin(dir) : (dir ? Integer.MIN_VALUE : Integer.MAX_VALUE)).invert(dir);
        }
        return super.processAddedActivation(doc, new NodeActivation.Key(this, r, ak.rid, ak.o), inputActs, isTrainingAct);
    }


    void processRemovedActivation(Document doc, NodeActivation<InputNode> act, Collection<NodeActivation> inputActs) {
        super.processRemovedActivation(doc, act, inputActs);

        if(act.isRemoved) {
            NodeActivation.Key ak = act.key;
            if (key.startRangeMapping == Mapping.NONE || key.endRangeMapping == Mapping.NONE) {
                boolean dir = key.startRangeMapping == Mapping.NONE;
                List<NodeActivation> tmp = NodeActivation.select(
                        doc,
                        this,
                        ak.rid,
                        new Range(ak.r.getBegin(dir), dir ? Integer.MAX_VALUE : Integer.MIN_VALUE).invert(!dir),
                        dir ? Operator.EQUALS : NONE,
                        dir ? NONE : Operator.EQUALS,
                        ak.o,
                        InterprNode.Relation.CONTAINS
                ).collect(Collectors.toList());

                for (NodeActivation cAct : tmp) {
                    NodeActivation.Key cak = cAct.key;
                    processAddedActivation(doc, new NodeActivation.Key(cak.n, new Range(dir ? Integer.MIN_VALUE : cak.r.begin, dir ? cak.r.end : Integer.MAX_VALUE), cak.rid, cak.o), cAct.inputs.values(), false);
                    if (!cAct.isRemoved) {
                        cAct.removedId = NodeActivation.removedIdCounter++;
                        cAct.isRemoved = true;
                        super.processRemovedActivation(doc, cAct, cAct.inputs.values());
                    }
                }
            }
        }
    }


    public void addActivation(Document doc, NodeActivation inputAct) {
        NodeActivation.Key ak = computeActivationKey(inputAct);

        if(ak != null) {
            addActivationAndPropagate(doc, ak, Collections.singleton(inputAct));
        }
    }


    public void removeActivation(Document doc, NodeActivation<?> inputAct) {
        for(NodeActivation act: inputAct.outputs.values()) {
            if(act.key.n == this) {
                removeActivationAndPropagate(doc, act, Collections.singleton(inputAct));
            }
        }
    }


    public void propagateAddedActivation(Document doc, NodeActivation act, InterprNode removedConflict) {
        if(!key.isNeg && !key.isRecurrent) {
            apply(doc, act, removedConflict);
        }
    }


    public void propagateRemovedActivation(Document doc, NodeActivation act) {
        if(!key.isNeg && !key.isRecurrent) {
            removeFromNextLevel(doc, act);
        }
    }


    @Override
    public boolean isAllowedOption(int threadId, InterprNode n, NodeActivation act, long v) {
        return false;
    }


    @Override
    Collection<Refinement> collectNodeAndRefinements(Refinement newRef) {
        List<Refinement> result = new ArrayList<>(2);
        result.add(new Refinement(key.relativeRid, newRef.rid, provider));
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
    void apply(Document doc, NodeActivation act, InterprNode removedConflict) {
        // Check if the activation has been deleted in the meantime.
        if(act.isRemoved) {
            return;
        }

        lock.acquireReadLock();
        if(andChildren != null) {
            for (Map.Entry<Refinement, Provider<AndNode>> me : andChildren.entrySet()) {
                Provider<InputNode> refInput = me.getKey().input;
                if(!refInput.isSuspended()) {
                    addNextLevelActivations(doc, refInput.get(), me.getKey(), me.getValue(), act, removedConflict);
                }
            }
        }
        lock.releaseReadLock();

        if(removedConflict == null) {
            OrNode.processCandidate(doc, this, act, false);
        }
    }


    private static void addNextLevelActivations(Document doc, InputNode secondNode, Refinement ref, Provider<AndNode> pnlp, NodeActivation act, InterprNode removedConflict) {
        ThreadState th = secondNode.getThreadState(doc.threadId, false);
        if(th == null || th.activations.isEmpty()) return;

        NodeActivation.Key ak = act.key;
        InputNode firstNode = ((InputNode) ak.n);
        Integer secondRid = Utils.nullSafeAdd(ak.rid, false, ref.rid, false);

        Stream<NodeActivation<InputNode>> s = NodeActivation.select(
                th,
                secondNode,
                secondRid,
                ak.r,
                computeStartRangeMatch(firstNode.key, secondNode.key),
                computeEndRangeMatch(firstNode.key, secondNode.key),
                null,
                null
        );

        s.forEach(secondAct -> {
            if(!secondAct.isRemoved) {
                InterprNode o = InterprNode.add(doc, true, ak.o, secondAct.key.o);
                if (o != null && (removedConflict == null || o.contains(removedConflict, false))) {
                    AndNode nlp = pnlp.get();
                    nlp.addActivation(doc,
                            new NodeActivation.Key(
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


    private static Operator computeStartRangeMatch(Key k1, Key k2) {
        if(k1.startRangeMatch == FIRST || k1.startRangeMatch == LAST) return k1.startRangeMatch;
        if(k2.startRangeMatch == FIRST || k2.startRangeMatch == LAST) return Operator.invert(k2.startRangeMatch);

        if(k2.startRangeOutput) {
            return k1.startRangeMatch;
        } else if(k1.startRangeOutput) {
            return Operator.invert(k2.startRangeMatch);
        }
        return NONE;
    }


    private static Operator computeEndRangeMatch(Key k1, Key k2) {
        if(k1.endRangeMatch == FIRST || k1.endRangeMatch == LAST) return k1.endRangeMatch;
        if(k2.endRangeMatch == FIRST || k2.endRangeMatch == LAST) return Operator.invert(k2.endRangeMatch);

        if(k2.endRangeOutput) {
            return k1.endRangeMatch;
        } else if(k1.endRangeOutput) {
            return Operator.invert(k2.endRangeMatch);
        }
        return NONE;
    }


    @Override
    public void discover(Document doc, NodeActivation<InputNode> act) {
        long v = Node.visitedCounter++;

        for(INeuron n: doc.finallyActivatedNeurons) {
            for(Activation secondNAct: n.getFinalActivations(doc)) {
                for (NodeActivation secondAct : secondNAct.outputs.values()) {
                    Refinement ref = new Refinement(secondAct.key.rid, act.key.rid, (Provider<InputNode>) secondAct.key.n.provider);
                    InputNode in = ref.input.get();
                    Operator srm = computeStartRangeMatch(key, in.key);
                    Operator erm = computeEndRangeMatch(key, in.key);
                    Integer ridDelta = Utils.nullSafeSub(act.key.rid, false, secondAct.key.rid, false);

                    if (act != secondAct &&
                            this != in &&
                            in.visitedTrain != v &&
                            !in.key.isNeg &&
                            !in.key.isRecurrent &&
                            ((srm.compare(act.key.r.begin, act.key.r.end, secondAct.key.r.begin, secondAct.key.r.end) && erm.compare(act.key.r.end, act.key.r.begin, secondAct.key.r.end, secondAct.key.r.begin)) ||
                                    (ridDelta != null && ridDelta < AndNode.MAX_RID_RANGE))) {
                        in.visitedTrain = v;
                        AndNode.createNextLevelNode(doc.m, doc.threadId, this, ref, true);
                    }
                }
            }
        }
    }


    @Override
    public double computeSynapseWeightSum(Integer offset, INeuron n) {
        return n.bias + Math.abs(getSynapse(key.relativeRid == null ? null : offset, n.provider).w);
    }


    public Synapse getSynapse(Integer rid, Neuron outputNeuron) {
        lock.acquireReadLock();
        Synapse s = synapses != null ? synapses.get(new SynapseKey(rid, outputNeuron)) : null;
        lock.releaseReadLock();
        return s;
    }


    public void setSynapse(int threadId, Synapse s) {
        lock.acquireWriteLock(threadId);
        if(synapses == null) {
            synapses = new TreeMap<>();
        }
        synapses.put(new SynapseKey(s.key.relativeRid, s.output), s);
        lock.releaseWriteLock();
    }


    public boolean containsSynapse(Neuron outputNeuron) {
        for(Synapse s: synapses.values()) {
            if(s.output.compareTo(outputNeuron) == 0) {
                return true;
            }
        }

        return false;
    }


    public void removeSynapse(int threadId, Synapse s) {
        lock.acquireWriteLock(threadId);
        synapses.remove(new SynapseKey(s.key.relativeRid, s.output));
        lock.releaseWriteLock();
    }


    @Override
    public void reactivate() {
        inputNeuron.inMemoryOutputSynapses.values().forEach(s -> {
            if(key.compareTo(s.key.createInputNodeKey()) == 0) {
                setSynapse(provider.m.defaultThreadId, s);
            }
        });
    }


    @Override
    public void cleanup(Model m, int threadId) {
    }


    @Override
    void remove(Model m, int threadId) {
        inputNeuron.get().outputNodes.remove(key);
        super.remove(m, threadId);
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(key.isNeg ? "N" : "P");
        sb.append(key.isRecurrent ? "R" : "");

        sb.append(getRangeBrackets(key.startRangeOutput, key.startRangeMapping));

        if(inputNeuron != null) {
            sb.append(inputNeuron.id);
            if(inputNeuron.get().label != null) {
                sb.append(",");
                sb.append(inputNeuron.get().label);
            }
        }

        sb.append(getRangeBrackets(key.endRangeOutput, key.endRangeMapping));

        return sb.toString();
    }


    private String getRangeBrackets(boolean ro, Mapping rs) {
        if(rs == Mapping.NONE) return "|";
        else if(ro) return rs == Mapping.START ? "[" : "]";
        else if(!ro) return rs == Mapping.START ? "<" : ">";
        else return "|";
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(false);
        out.writeUTF("I");
        super.write(out);
        key.write(out);

        out.writeBoolean(inputNeuron != null);
        if(inputNeuron != null) {
            out.writeInt(inputNeuron.id);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);
        key = Synapse.lookupKey(Key.read(in, m));

        if(in.readBoolean()) {
            inputNeuron = m.lookupNeuron(in.readInt());
        }
    }


    private static class SynapseKey implements Writable, Comparable<SynapseKey> {
        Integer rid;
        Neuron n;

        private SynapseKey() {
        }


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


        public static SynapseKey read(DataInput in, Model m) throws IOException {
            SynapseKey sk = new SynapseKey();
            sk.readFields(in, m);
            return sk;
        }


        @Override
        public void write(DataOutput out) throws IOException {
            out.writeBoolean(rid != null);
            if(rid != null) {
                out.writeInt(rid);
            }
            out.writeInt(n.id);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            if(in.readBoolean()) {
                rid = in.readInt();
            }
            n = m.lookupNeuron(in.readInt());
        }
    }
}
