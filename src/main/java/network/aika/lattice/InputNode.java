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
import network.aika.Provider;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Range;
import network.aika.neuron.relation.InstanceRelation;
import network.aika.neuron.relation.RangeRelation;
import network.aika.neuron.relation.Relation;
import network.aika.neuron.activation.Activation;
import network.aika.training.PatternDiscovery;
import network.aika.lattice.AndNode.AndActivation;
import network.aika.lattice.InputNode.InputActivation;
import network.aika.lattice.AndNode.Refinement;
import network.aika.lattice.AndNode.RefValue;
import network.aika.lattice.AndNode.RelationsMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

import static network.aika.neuron.activation.Range.Relation.*;


/**
 * The {@code InputNode} class is the input layer for the boolean logic. The input-node has two sources of
 * activations. First, it might be underlying logic node of an {@code InputNeuron} in which case the input
 * activations come from the outside. The second option is that the activation come from the output of another neuron.
 *
 * @author Lukas Molzberger
 */
public class InputNode extends Node<InputNode, InputActivation> {

    public Neuron inputNeuron;

    public TreeMap<AndNode.Refinement, AndNode.RefValue> nonExactAndChildren;

    private long visitedDiscover;


    public static final Relation[] CANDIDATE_RELATIONS = new Relation[] {
        new RangeRelation(EQUALS),
        new RangeRelation(BEGIN_TO_END_EQUALS),
        new RangeRelation(END_TO_BEGIN_EQUALS),
        new RangeRelation(BEGIN_EQUALS),
        new RangeRelation(END_EQUALS),
        new RangeRelation(CONTAINS),
        new RangeRelation(CONTAINED_IN),
        new InstanceRelation(InstanceRelation.Type.CONTAINS),
        new InstanceRelation(InstanceRelation.Type.CONTAINED_IN),
        new InstanceRelation(InstanceRelation.Type.COMMON_ANCESTOR)
    };


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


    void addAndChild(AndNode.Refinement ref, AndNode.RefValue child) {
        super.addAndChild(ref, child);

        if(!ref.relations.isExact()) {
            if (nonExactAndChildren == null) {
                nonExactAndChildren = new TreeMap<>();
            }

            AndNode.RefValue n = nonExactAndChildren.put(ref, child);
            assert n == null;
        }
    }


    void removeAndChild(AndNode.Refinement ref) {
        super.removeAndChild(ref);

        if(!ref.relations.isExact()) {
            if (nonExactAndChildren != null) {
                nonExactAndChildren.remove(ref);

                if (nonExactAndChildren.isEmpty()) {
                    nonExactAndChildren = null;
                }
            }
        }
    }



    public RefValue extend(int threadId, Document doc, Refinement ref, boolean patterDiscovery) {
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
                TreeMap<AndNode.Refinement, AndNode.RefValue> children;
                if(andChildren.size() > 10) {
                    children = nonExactAndChildren;
                    applyExactRelations(act);
                } else {
                    children = andChildren;
                }

                if(children != null) {
                    children.forEach((ref, rv) -> {
                        InputNode in = ref.input.getIfNotSuspended();
                        if (in != null) {
                            addNextLevelActivations(in, ref, rv.child.get(act.doc), act);
                        }
                    });
                }
            }
        } finally {
            lock.releaseReadLock();
        }

        OrNode.processCandidate(this, act, false);
    }


    private void applyExactRelations(InputActivation act) {
        Activation iAct = act.input.input;

        for(Range.Relation rel: new Range.Relation[] {BEGIN_EQUALS, END_EQUALS, BEGIN_TO_END_EQUALS, END_TO_BEGIN_EQUALS}) {
            for(Activation linkedAct: RangeRelation.getActivationsByRangeEquals(act.doc, iAct.range, rel)) {
                Provider<InputNode> in = linkedAct.getINeuron().outputNode;
                for (Map.Entry<AndNode.Refinement, AndNode.RefValue> me : andChildren.subMap(
                        new Refinement(RelationsMap.MIN, in),
                        new Refinement(RelationsMap.MAX, in)).entrySet()) {
                    addNextLevelActivations(in.get(act.doc), me.getKey(), me.getValue().child.get(act.doc), act);
                }
            }
        }
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
        if(!act.input.input.isFinalActivation()) {
            return;
        }

        Document doc = act.doc;
        doc.getFinalActivations().forEach(secondNAct -> {
            InputActivation secondAct = secondNAct.outputToInputNode.output;
            if (act != secondAct && config.candidateCheck.check(act, secondAct)) {
                List<Relation> relations = getRelations(act.input.input, secondNAct);
                for(Relation r: relations) {
                    InputNode in = secondAct.node;

                    if (r != null) {
                        RelationsMap rm = new RelationsMap(new Relation[] {r});
                        Refinement ref = new Refinement(rm, in.provider);

                        AndNode nln = extend(doc.threadId, doc, ref, true).child.get();

                        if (nln != null) {
                            nln.isDiscovered = true;
                        }
                    }
                }
            }
        });
    }


    public static List<Relation> getRelations(Activation act1, Activation act2) {
        ArrayList<Relation> rels = new ArrayList<>();
        for(Relation rel: CANDIDATE_RELATIONS) {
            if(rel.test(act2, act1)) {
                rels.add(rel);
                break;
            }
        }

        return rels;
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


        public String toString() {
            return "I-ACT(" + input.input.getLabel() + " " + input.input.range + ")";
        }
    }


    public static class Link {
        public Activation input;
        public InputActivation output;

        public Link(Activation iAct, InputActivation oAct) {
            input = iAct;
            output = oAct;
        }
    }
}
