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
import org.aika.neuron.activation.Conflicts;
import org.aika.Document;
import org.aika.neuron.*;
import org.aika.neuron.activation.Activation;
import org.aika.neuron.activation.Selector;
import org.aika.training.PatternDiscovery.Config;
import org.aika.neuron.activation.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.lattice.AndNode.RefValue;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;


/**
 * The {@code InputNode} class is the input layer for the boolean logic. The input-node has two sources of
 * activations. First, it might be underlying logic node of an {@code InputNeuron} in which case the input
 * activations come from the outside. The second option is that the activation come from the output of another neuron.
 *
 * @author Lukas Molzberger
 */
public class InputNode extends Node<InputNode, NodeActivation<InputNode>> {

    public Neuron inputNeuron;


    private long visitedDiscover;


    public InputNode() {
    }

    public InputNode(Model m) {
        super(m, 1);
    }


    public static InputNode add(Model m, INeuron input) {
        if (input.outputNode != null) {
            return input.outputNode.get();
        }
        InputNode in = new InputNode(m);

        if (input != null && in.inputNeuron == null) {
            in.inputNeuron = input.provider;
            input.outputNode = in.provider;
            input.setModified();
        }
        return in;
    }


    @Override
    protected NodeActivation<InputNode> createActivation(Document doc) {
        return new NodeActivation<>(doc.activationIdCounter++, doc, this);
    }


    public void addActivation(Activation inputAct) {
        if(inputAct.repropagateV != null && inputAct.repropagateV != markedCreated) return;

        addActivation(inputAct.doc, Collections.singleton(inputAct));
    }


    public void propagate(NodeActivation act) {
        apply(act);
    }


    public void reprocessInputs(Document doc) {
        inputNeuron.get(doc).getActivations(doc).forEach(act -> {
            act.repropagateV = markedCreated;
            if(act.upperBound > 0.0) {
                act.getINeuron().propagate(act);
            }
        });
    }


    public RefValue extend(int threadId, Document doc, Refinement ref) {
        RefValue rv = getAndChild(ref);
        if(rv != null) {
            return rv;
        }

        SortedMap<Refinement, RefValue> parents = new TreeMap<>();

        Refinement mirrorRef = new Refinement(new Relation[]{ref.relations[0].invert()}, provider);
        parents.put(mirrorRef, new RefValue(new Integer[] {1}, 0, ref.input));

        rv = new RefValue(new Integer[] {0}, 1, provider);
        parents.put(ref, rv);

        return AndNode.createAndNode(provider.model, doc, parents, level + 1) ? rv : null;
    }


    /**
     * @param act
     */
    @Override
    void apply(NodeActivation act) {

        try {
            lock.acquireReadLock();
            if (andChildren != null) {
                andChildren.forEach((ref, rv) -> {
                    InputNode in = ref.input.getIfNotSuspended();
                    if (in != null) {
                        addNextLevelActivations(in, ref, rv.child, act);
                    }
                });
            }
        } finally {
            lock.releaseReadLock();
        }

        OrNode.processCandidate(this, act, false);
    }


    private static void addNextLevelActivations(InputNode secondNode, Refinement ref, Provider<AndNode> pnlp, NodeActivation act) {
        Document doc = act.doc;
        INeuron.ThreadState th = secondNode.inputNeuron.get().getThreadState(doc.threadId, false);
        if (th == null || th.activations.isEmpty()) return;

        Activation iAct = (Activation) act.inputs.firstEntry().getValue();
        AndNode nlp = pnlp.get(doc);

        if(act.repropagateV != null && act.repropagateV != nlp.markedCreated) return;

        Stream<Activation> s = Selector.select(
                th,
                secondNode.inputNeuron.get(doc),
                ref.relations[0],
                iAct
        );

        s.forEach(secondIAct -> {
                    NodeActivation secondAct = secondNode.getInputNodeActivation(secondIAct);
                    if(secondAct != null) {
                        if (!Conflicts.isConflicting(iAct, secondIAct)) {
                            nlp.addActivation(doc, AndNode.prepareInputActs(act, secondAct));
                        }
                    }
                }
        );
    }



    private NodeActivation getInputNodeActivation(Activation act) {
        for(NodeActivation inAct: act.outputs.values()) {
            if(inAct.node == this) return inAct;
        }
        return null;
    }


    @Override
    public void discover(NodeActivation<InputNode> act, Config config) {
        long v = provider.model.visitedCounter.addAndGet(1);

        Document doc = act.doc;
        doc.getFinalActivations().forEach(secondNAct -> {
            for (NodeActivation secondAct : secondNAct.outputs.values()) {
                if(act != secondAct) {
                    Refinement ref = config.refinementFactory.create(act, secondAct);
                    if (ref != null) {
                        InputNode in = ref.input.get(doc);

                        if (in.visitedDiscover != v) {
                            in.visitedDiscover = v;
                            AndNode nln = extend(doc.threadId, doc, ref).child.get();

                            if (nln != null) {
                                nln.isDiscovered = true;
                            }
                        }
                    }
                }
            }
        });
    }


    @Override
    public void cleanup() {
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("I");

        sb.append("[");

        if (inputNeuron != null) {
            sb.append(inputNeuron.id);
            if (inputNeuron.getLabel() != null) {
                sb.append(",");
                sb.append(inputNeuron.getLabel());
            }
        }

        sb.append("]");

        return sb.toString();
    }



    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(false);
        out.writeChar('I');
        super.write(out);

        out.writeBoolean(inputNeuron != null);
        if (inputNeuron != null) {
            out.writeInt(inputNeuron.id);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        if (in.readBoolean()) {
            inputNeuron = m.lookupNeuron(in.readInt());
        }
    }
}
