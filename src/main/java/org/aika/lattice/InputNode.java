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
import org.aika.corpus.Document.DiscoveryConfig;
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
    public Map<SynapseKey, Synapse> synapses;

    public ReadWriteLock synapseLock = new ReadWriteLock();


    private long visitedDiscover;


    public InputNode() {
    }

    public InputNode(Model m, Key key) {
        super(m, 1);
        this.key = Synapse.lookupKey(key);

        endRequired = false;
        ridRequired = false;
        if (key != null) {
            endRequired = key.startRangeMapping == Mapping.NONE;
            ridRequired = key.relativeRid != null || key.absoluteRid != null;
        }
    }


    public static InputNode add(Model m, Key key, INeuron input) {
        Provider<InputNode> pin = (input != null ? input.outputNodes.get(key) : null);
        if (pin != null) {
            return pin.get();
        }
        InputNode in = new InputNode(m, key);

        if (input != null && in.inputNeuron == null) {
            in.inputNeuron = input.provider;
            input.outputNodes.put(key, in.provider);
            input.provider.setModified();
        }
        return in;
    }


    @Override
    protected NodeActivation<InputNode> createActivation(Document doc, NodeActivation.Key ak) {
        return new NodeActivation<>(doc.activationIdCounter++, doc, ak);
    }


    private NodeActivation.Key computeActivationKey(NodeActivation iAct) {
        NodeActivation.Key ak = iAct.key;
        if ((key.absoluteRid != null && key.absoluteRid != ak.rid) || ak.o.isConflicting(ak.o.doc.visitedCounter++))
            return null;

        return new NodeActivation.Key(
                this,
                new Range(key.startRangeMapping.getSignalPos(ak.r), key.endRangeMapping.getSignalPos(ak.r)),
                key.relativeRid != null ? ak.rid : null,
                ak.o
        );
    }


    public void addActivation(Document doc, NodeActivation inputAct) {
        NodeActivation.Key ak = computeActivationKey(inputAct);

        if (ak != null) {
            addActivationAndPropagate(doc, ak, Collections.singleton(inputAct));
        }
    }


    public void propagateAddedActivation(Document doc, NodeActivation act) {
        if (!key.isRecurrent) {
            apply(doc, act);
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
     * @param doc
     * @param act
     */
    @Override
    void apply(Document doc, NodeActivation act) {

        lock.acquireReadLock();
        if (andChildren != null) {
            for (Map.Entry<Refinement, Provider<AndNode>> me : andChildren.entrySet()) {
                Provider<InputNode> refInput = me.getKey().input;
                InputNode in = refInput.getIfNotSuspended();
                if (in != null) {
                    addNextLevelActivations(doc, in, me.getKey(), me.getValue(), act);
                }
            }
        }
        lock.releaseReadLock();

        OrNode.processCandidate(doc, this, act, false);
    }


    private static void addNextLevelActivations(Document doc, InputNode secondNode, Refinement ref, Provider<AndNode> pnlp, NodeActivation act) {
        ThreadState th = secondNode.getThreadState(doc.threadId, false);
        if (th == null || th.activations.isEmpty()) return;

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
                    InterprNode o = InterprNode.add(doc, true, ak.o, secondAct.key.o);
                    if (o != null) {
                        AndNode nlp = pnlp.get();
                        nlp.addActivation(doc,
                                new NodeActivation.Key(
                                        nlp,
                                        Range.mergeRange(
                                                Range.getOutputRange(ak.r, new boolean[]{firstNode.key.startRangeOutput, firstNode.key.endRangeOutput}),
                                                Range.getOutputRange(secondAct.key.r, new boolean[]{secondNode.key.startRangeOutput, secondNode.key.endRangeOutput})
                                        ),
                                        Utils.nullSafeMin(ak.rid, secondAct.key.rid),
                                        o
                                ),
                                AndNode.prepareInputActs(act, secondAct)
                        );
                    }
                }
        );
    }


    private static Operator computeStartRangeMatch(Key k1, Key k2) {
        if (k2.startRangeOutput) {
            return k1.startRangeMatch;
        } else if (k1.startRangeOutput) {
            return Operator.invert(k2.startRangeMatch);
        }
        return NONE;
    }


    private static Operator computeEndRangeMatch(Key k1, Key k2) {
        if (k2.endRangeOutput) {
            return k1.endRangeMatch;
        } else if (k1.endRangeOutput) {
            return Operator.invert(k2.endRangeMatch);
        }
        return NONE;
    }


    @Override
    public void discover(Document doc, NodeActivation<InputNode> act, DiscoveryConfig discoveryConfig) {
        long v = provider.m.visitedCounter.addAndGet(1);

        for (INeuron n : doc.finallyActivatedNeurons) {
            for (Activation secondNAct : n.getFinalActivations(doc)) {
                for (NodeActivation secondAct : secondNAct.outputs.values()) {
                    Refinement ref = new Refinement(secondAct.key.rid, act.key.rid, (Provider<InputNode>) secondAct.key.n.provider);
                    InputNode in = ref.input.get();
                    Operator srm = computeStartRangeMatch(key, in.key);
                    Operator erm = computeEndRangeMatch(key, in.key);

                    if (act != secondAct &&
                            this != in &&
                            in.visitedDiscover != v &&
                            !in.key.isRecurrent &&
                            !(key.startRangeOutput && in.key.startRangeOutput) &&
                            !(key.endRangeOutput && in.key.endRangeOutput) &&
                            srm.compare(secondAct.key.r.begin, act.key.r.begin) &&
                            erm.compare(secondAct.key.r.end, act.key.r.end)
                        ) {
                        in.visitedDiscover = v;
                        AndNode nln = AndNode.createNextLevelNode(doc.m, doc.threadId, this, ref, discoveryConfig);

                        if(nln != null) {
                            nln.isDiscovered = true;
                            doc.addedNodes.add(nln);
                        }
                    }
                }
            }
        }
    }


    @Override
    boolean contains(Refinement ref) {
        return this == ref.input.get() && Utils.compareInteger(key.relativeRid, ref.rid) == 0;
    }


    @Override
    public double computeSynapseWeightSum(Integer offset, INeuron n) {
        return n.bias + Math.abs(getSynapse(key.relativeRid == null ? null : offset, n.provider).w);
    }


    public Synapse getSynapse(Integer rid, Neuron outputNeuron) {
        synapseLock.acquireReadLock();
        Synapse s = synapses != null ? synapses.get(new SynapseKey(rid, outputNeuron)) : null;
        synapseLock.releaseReadLock();
        return s;
    }


    public void setSynapse(Synapse s) {
        synapseLock.acquireWriteLock();
        if (synapses == null) {
            synapses = new TreeMap<>();
        }
        synapses.put(new SynapseKey(s.key.relativeRid, s.output), s);
        synapseLock.releaseWriteLock();
    }


    public void removeSynapse(Synapse s) {
        synapseLock.acquireWriteLock();
        synapses.remove(new SynapseKey(s.key.relativeRid, s.output));
        synapseLock.releaseWriteLock();
    }


    @Override
    public void reactivate() {
        inputNeuron.lock.acquireReadLock();
        inputNeuron.inMemoryOutputSynapses.values().forEach(s -> {
            if (key.compareTo(s.key.createInputNodeKey()) == 0) {
                setSynapse(s);
            }
        });
        inputNeuron.lock.releaseReadLock();
    }


    @Override
    public void cleanup() {
    }


    @Override
    public void remove() {
        inputNeuron.get().outputNodes.remove(key);
        super.remove();
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("I");
        sb.append(key.isRecurrent ? "R" : "");

        sb.append(getRangeBrackets(key.startRangeOutput, key.startRangeMapping));

        if (inputNeuron != null) {
            sb.append(inputNeuron.id);
            if (inputNeuron.get().label != null) {
                sb.append(",");
                sb.append(inputNeuron.get().label);
            }
        }

        sb.append(getRangeBrackets(key.endRangeOutput, key.endRangeMapping));

        return sb.toString();
    }


    private String getRangeBrackets(boolean ro, Mapping rs) {
        if (rs == Mapping.NONE) return "|";
        else if (ro) return rs == Mapping.START ? "[" : "]";
        else if (!ro) return rs == Mapping.START ? "<" : ">";
        else return "|";
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(false);
        out.writeChar('I');
        super.write(out);
        key.write(out);

        out.writeBoolean(inputNeuron != null);
        if (inputNeuron != null) {
            out.writeInt(inputNeuron.id);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);
        key = Synapse.lookupKey(Key.read(in, m));

        if (in.readBoolean()) {
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
            if (r != 0) return r;
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
            if (rid != null) {
                out.writeInt(rid);
            }
            out.writeInt(n.id);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            if (in.readBoolean()) {
                rid = in.readInt();
            }
            n = m.lookupNeuron(in.readInt());
        }
    }
}
