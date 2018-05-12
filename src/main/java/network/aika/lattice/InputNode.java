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
package network.aika.lattice;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.relation.Relation;
import network.aika.neuron.activation.Activation;
import network.aika.training.PatternDiscovery;
import network.aika.lattice.AndNode.AndActivation;
import network.aika.lattice.InputNode.InputActivation;
import network.aika.neuron.activation.Selector;
import network.aika.lattice.AndNode.Refinement;
import network.aika.lattice.AndNode.RefValue;
import network.aika.lattice.AndNode.RelationsMap;

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
public class InputNode extends Node<InputNode, InputActivation> {

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


    public void addActivation(Activation inputAct) {
        if(inputAct.repropagateV != null && inputAct.repropagateV != markedCreated) return;

        InputActivation act = new InputActivation(inputAct.doc.activationIdCounter++, inputAct, this);

        addActivation(act);
    }


    public void propagate(InputActivation act) {
        apply(act);
    }


    public void reprocessInputs(Document doc) {
        inputNeuron.get(doc).getActivations(doc, false).forEach(act -> {
            act.repropagateV = markedCreated;
            if(act.upperBound > 0.0) {
                act.getINeuron().propagate(act);
            }
        });
    }


    public RefValue extend(int threadId, Document doc, Refinement ref) {
        if(ref.relations.size() == 0) return null;

        Relation rel = ref.relations.get(0);
        if(rel == null) {
            return null;
        }

        RefValue rv = getAndChild(ref);
        if(rv != null) {
            return rv;
        }

        SortedMap<Refinement, RefValue> nlParents = new TreeMap<>();

        Refinement mirrorRef = new Refinement(new AndNode.RelationsMap(new Relation[]{rel.invert()}), provider);
        nlParents.put(mirrorRef, new RefValue(new Integer[] {1}, 0, ref.input));

        rv = new RefValue(new Integer[] {0}, 1, provider);
        nlParents.put(ref, rv);

        return AndNode.createAndNode(provider.model, doc, nlParents, level + 1) ? rv : null;
    }


    /**
     * @param act
     */
    @Override
    void apply(InputActivation act) {
        try {
            lock.acquireReadLock();
            if (andChildren != null) {
                andChildren.forEach((ref, rv) -> {
                    InputNode in = ref.input.getIfNotSuspended();
                    if (in != null) {
                        addNextLevelActivations(in, ref, rv.child.get(act.doc), act);
                    }
                });
            }
        } finally {
            lock.releaseReadLock();
        }

        OrNode.processCandidate(this, act, false);
    }


    private static void addNextLevelActivations(InputNode secondNode, Refinement ref, AndNode nln, InputActivation act) {
        Document doc = act.doc;
        INeuron.ThreadState th = secondNode.inputNeuron.get().getThreadState(doc.threadId, false);
        if (th == null || th.activations.isEmpty()) return;

        Activation iAct = act.input.input;

        if(act.repropagateV != null && act.repropagateV != nln.markedCreated) return;

        ref.relations.get(0).getActivations(secondNode.inputNeuron.get(doc), iAct).forEach(secondIAct -> {
                    InputActivation secondAct = secondIAct.outputToInputNode.output;
                    if(secondAct != null) {
                    //    if (!Conflicts.isConflicting(iAct, secondIAct)) {
                            AndActivation oAct = new AndActivation(doc.activationIdCounter++, doc, nln);
                            for(Map.Entry<Refinement, RefValue> me: nln.parents.entrySet()) {
                                boolean match = me.getKey().compareTo(ref) == 0;
                                oAct.link(me.getKey(), me.getValue(), match ? secondAct : act, match ? act : secondAct);
                            }
                            nln.addActivation(oAct);
                        }
                   // }
                }
        );
    }


    @Override
    public void discover(InputActivation act, PatternDiscovery.Config config) {
        long v = provider.model.visitedCounter.addAndGet(1);

        Document doc = act.doc;
        doc.getFinalActivations().forEach(secondNAct -> {
            InputActivation secondAct = secondNAct.outputToInputNode.output;
            if (act != secondAct) {
                List<Relation> relations = config.refinementFactory.create(act, 0, secondAct, 0);
                for(Relation r: relations) {
                    InputNode in = secondAct.node;

                    if (in.visitedDiscover != v && r != null) {
                        in.visitedDiscover = v;

                        RelationsMap rm = new RelationsMap(new Relation[] {r});
                        Refinement ref = new Refinement(rm, in.provider);

                        AndNode nln = extend(doc.threadId, doc, ref).child.get();

                        if (nln != null) {
                            nln.isDiscovered = true;
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


    public static class InputActivation extends NodeActivation<InputNode> {

        public Link input;

        public InputActivation(int id, Activation iAct, InputNode node) {
            super(id, iAct.doc, node);
            input = new Link(iAct, this);
            iAct.outputToInputNode = input;
        }

        public Activation getInputActivation(int i) {
            assert i == 0;
            return input.input;
        }
    }


    public static class Link {
        Activation input;
        InputActivation output;

        public Link(Activation iAct, InputActivation oAct) {
            input = iAct;
            output = oAct;
        }
    }
}
