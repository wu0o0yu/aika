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
            input.setModified();
        }
        return in;
    }


    @Override
    protected NodeActivation<InputNode> createActivation(Document doc, NodeActivation.Key ak) {
        return new NodeActivation<>(doc.activationIdCounter++, doc, ak);
    }


    private NodeActivation.Key computeActivationKey(NodeActivation iAct) {
        NodeActivation.Key ak = iAct.key;
        if ((key.absoluteRid != null && key.absoluteRid != ak.rid) || ak.interpretation.isConflicting(ak.interpretation.doc.visitedCounter++))
            return null;

        return new NodeActivation.Key(
                this,
                key.rangeOutput.map(ak.range),
                key.relativeRid != null ? ak.rid : null,
                ak.interpretation
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
            andChildren.forEach((ref, cn) -> {
                InputNode in = ref.input.getIfNotSuspended();
                if (in != null) {
                    addNextLevelActivations(doc, in, ref, cn, act);
                }
            });
        }
        lock.releaseReadLock();

        OrNode.processCandidate(doc, this, act, false);
    }


    private static void addNextLevelActivations(Document doc, InputNode secondNode, Refinement ref, Provider<AndNode> pnlp, NodeActivation act) {
        INeuron.ThreadState th = secondNode.inputNeuron.get().getThreadState(doc.threadId, false);
        if (th == null || th.activations.isEmpty()) return;

        Activation iAct = (Activation) act.inputs.firstEntry().getValue();
        AndNode nlp = pnlp.get(doc);
        if(nlp.combinatorialExpensive) return;

        Activation.Key ak = act.key;
        Activation.Key iak = iAct.key;
        InputNode firstNode = ((InputNode) ak.node);
        Integer secondRid = Utils.nullSafeAdd(ak.rid, false, ref.rid, false);

        Stream<Activation> s = Activation.select(
                th,
                secondNode.inputNeuron.get(),
                secondRid,
                iak.range,
                Range.Relation.createQuery(firstNode.key.rangeMatch, secondNode.key.rangeOutput, firstNode.key.rangeOutput, secondNode.key.rangeMatch),
                null,
                null
        );

        s.forEach(secondAct -> {
                    InterprNode o = InterprNode.add(doc, true, ak.interpretation, secondAct.key.interpretation);
                    if (o != null) {
                        Node.addActivationAndPropagate(doc,
                                new NodeActivation.Key(
                                        nlp,
                                        Range.mergeRange(
                                                firstNode.key.rangeOutput.map(iak.range),
                                                secondNode.key.rangeOutput.map(secondAct.key.range)
                                        ),
                                        Utils.nullSafeMin(ak.rid, secondAct.key.rid),
                                        o
                                ),
                                AndNode.prepareInputActs(act, secondNode.getInputNodeActivation(secondAct))
                        );
                    }
                }
        );
    }



    private NodeActivation getInputNodeActivation(Activation act) {
        for(NodeActivation inAct: act.outputs.values()) {
            if(inAct.key.node == this) return inAct;
        }
        return null;
    }



    @Override
    public void discover(Document doc, NodeActivation<InputNode> act, DiscoveryConfig discoveryConfig) {
        long v = provider.model.visitedCounter.addAndGet(1);

        for (INeuron n : doc.finallyActivatedNeurons) {
            for (Activation secondNAct : n.getFinalActivations(doc)) {
                for (NodeActivation secondAct : secondNAct.outputs.values()) {
                    Refinement ref = new Refinement(secondAct.key.rid, act.key.rid, (Provider<InputNode>) secondAct.key.node.provider);
                    InputNode in = ref.input.get(doc);
                    Range.Relation rm = Range.Relation.createQuery(key.rangeMatch, in.key.rangeOutput, key.rangeOutput, in.key.rangeMatch);

                    if (act != secondAct &&
                            this != in &&
                            in.visitedDiscover != v &&
                            !in.key.isRecurrent &&
                            rm.compare(secondAct.key.range, act.key.range)
                        ) {
                        in.visitedDiscover = v;
                        AndNode nln = AndNode.createNextLevelNode(doc.model, doc.threadId, this, ref, discoveryConfig);

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
        return n.biasSum + Math.abs(getSynapse(key.relativeRid == null ? null : offset, n.provider).weight);
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
        if(synapses != null) {
            synapseLock.acquireWriteLock();
            synapses.remove(new SynapseKey(s.key.relativeRid, s.output));
            synapseLock.releaseWriteLock();
        }
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

        sb.append(getRangeBrackets(key.rangeOutput.begin));

        if (inputNeuron != null) {
            sb.append(inputNeuron.id);
            if (inputNeuron.get().label != null) {
                sb.append(",");
                sb.append(inputNeuron.get().label);
            }
        }

        sb.append(getRangeBrackets(key.rangeOutput.end));

        return sb.toString();
    }


    private String getRangeBrackets(Mapping rs) {
        if (rs == Mapping.NONE) return "|";
        return rs == Mapping.BEGIN ? "[" : "]";
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
        Neuron neuron;

        private SynapseKey() {
        }


        public SynapseKey(Integer rid, Neuron neuron) {
            this.rid = rid;
            this.neuron = neuron;
        }


        @Override
        public int compareTo(SynapseKey sk) {
            int r = Utils.compareInteger(rid, sk.rid);
            if (r != 0) return r;
            return neuron.compareTo(sk.neuron);
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
            out.writeInt(neuron.id);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            if (in.readBoolean()) {
                rid = in.readInt();
            }
            neuron = m.lookupNeuron(in.readInt());
        }
    }
}
