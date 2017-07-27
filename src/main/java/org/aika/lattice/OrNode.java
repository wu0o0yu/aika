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
import org.aika.Activation.Key;
import org.aika.corpus.Document;
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 *
 * @author Lukas Molzberger
 */
public class OrNode extends Node {

    // Hack: Integer.MIN_VALUE represents the null key
    public TreeMap<Integer, TreeSet<Node>> parents = new TreeMap<>();


    public OrNode() {}


    public OrNode(Document doc) {
        super(doc, -1); // Or Node Activations always need to be processed first!
        Model m = doc.m;

        m.stat.nodes++;
        m.stat.orNodes++;

        endRequired = true;
        ridRequired = true;
    }


    @Override
    public void computeNullHyp(Model m) {

    }


    @Override
    public boolean isExpandable(boolean checkFrequency) {
        return false;
    }


    @Override
    public boolean isAllowedOption(Document doc, Option n, Activation act, long v) {
        return false;
    }


    @Override
    public void initActivation(Document doc, Activation act) {
/*        for(Synapse s: neuron.inputSynapses) {
            if(s.key.isNeg || s.key.isRecurrent) {
                Activation.select(doc, s.inputNode, Utils.nullSafeAdd(act.key.rid, false, s.key.relativeRid, false), act.key.r, Range.Relation.OVERLAPS, null, null)
                        .forEach(iAct -> {
                    iAct.outputs.put(act.key, act);
                });
            }
        }
*/
        if(getThreadState(doc).activations.isEmpty()) {
            doc.activatedNeurons.add(neuron);
        }
    }


    @Override
    public void deleteActivation(Document doc, Activation act) {
        if(getThreadState(doc).activations.isEmpty()) {
            doc.activatedNeurons.remove(neuron);
        }
    }


    public void updateActivation(Document doc, Range inputR, Integer rid) {
        List<Activation> inputs = new ArrayList<>();
        Range r = null;

        for (Map.Entry<Integer, TreeSet<Node>> me : parents.entrySet()) {
            r = extractRange(doc, inputR, rid, inputs, r, me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, me.getValue());
        }

        final Range fr = r;
        Activation.select(doc, this, rid, inputR, Range.Relation.OVERLAPS, null, null).forEach(oAct -> {
            for (Iterator<Activation> it = oAct.inputs.values().iterator(); it.hasNext(); ) {
                Activation iAct = it.next();

                if(fr == null || Range.compare(fr, oAct.key.r) != 0) {
                    oAct.isReplaced = true;
                    oAct.key.o.removeOrOption(iAct, iAct.key.o);
                    removeActivationAndPropagate(doc, oAct, oAct.inputs.values());
                }
                if(iAct.isRemoved) {
                    oAct.key.o.removeOrOption(iAct, iAct.key.o);
                    it.remove();
                }
            }
        });

        if(inputs.isEmpty()) return;

        Option no = lookupOrOption(doc, r, true);

        for(Activation iAct: inputs) {
            no.addOrOption(iAct, iAct.key.o);
        }

        Key nak = new Key(
                this,
                r,
                rid,
                no
        );

        addActivationAndPropagate(
                doc,
                nak,
                inputs
        );
    }


    private Range extractRange(Document doc, Range inputR, Integer rid, List<Activation> inputs, Range r, Integer pRidOffset, TreeSet<Node> parents) {
        // Optimization the number of parents can get very large, thus we need to avoid iterating over all of them.
        if(parents.size() > 10) {
            return extractRange(doc, null, inputR, rid, inputs, r, pRidOffset, parents);
        } else {
            for(Node pn: parents) {
                r = extractRange(doc, pn, inputR, rid, inputs, r, pRidOffset, parents);
            }
            return r;
        }
    }


    private Range extractRange(Document doc, Node n, Range inputR, Integer rid, List<Activation> inputs, Range r, Integer pRidOffset, TreeSet<Node> parents) {
        for(Activation iAct: Activation.select(doc, n, Utils.nullSafeAdd(rid, true, pRidOffset, false), inputR, Range.Relation.OVERLAPS, null, null)
                .collect(Collectors.toList())) {
            if(!iAct.isRemoved && parents.contains(iAct.key.n) && !checkSelfReferencing(doc, iAct)) {
                inputs.add(iAct);
                r = r == null ? iAct.key.r : new Range(Math.min(r.begin, iAct.key.r.begin), Math.max(r.end, iAct.key.r.end));
            }
        }
        return r;
    }


    public void addActivation(Document doc, Integer ridOffset, Activation inputAct) {
        if(checkSelfReferencing(doc, inputAct)) return;

        Key ak = inputAct.key;
        updateActivation(doc, ak.r, Utils.nullSafeSub(ak.rid, true, ridOffset, false));
    }


    public void removeActivation(Document doc, Integer ridOffset, Activation inputAct) {
        if(checkSelfReferencing(doc, inputAct)) return;

        Key ak = inputAct.key;
        updateActivation(doc, ak.r, Utils.nullSafeSub(ak.rid, true, ridOffset, false));
    }


    private boolean checkSelfReferencing(Document doc, Activation inputAct) {
        Option o = lookupOrOption(doc, inputAct.key.r, false);
        if(o == null) return false;
        return inputAct.key.o.contains(o, true);
    }


    public void propagateAddedActivation(Document doc, Activation act, Option removedConflict) {
        if(removedConflict == null) {
            neuron.propagateAddedActivation(doc, act);
        }
    }


    public void propagateRemovedActivation(Document doc, Activation act) {
        neuron.propagateRemovedActivation(doc, act);
    }


    @Override
    public double computeSynapseWeightSum(Integer offset, Neuron n) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void cleanup(Document doc) {

    }


    @Override
    protected boolean hasSupport(Activation act) {
        if(act.isReplaced) return false;

        for(Activation iAct: act.inputs.values()) {
            if(!iAct.isRemoved) return true;
        }

        return false;
    }


    @Override
    public void apply(Document doc, Activation act, Option conflict) {
        if(conflict == null) {
            OrNode.processCandidate(doc, this, act, false);
        }
    }


    @Override
    public void discover(Document doc, Activation act) {
    }


    public static void processCandidate(Document doc, Node parentNode, Activation inputAct, boolean train) {
        Key ak = inputAct.key;
        parentNode.lock.acquireReadLock();
        if(parentNode.orChildren != null) {
            for (OrEntry oe : parentNode.orChildren) {
                if (!ak.o.isConflicting(Option.visitedCounter++)) {
                    ((OrNode) oe.node).addActivation(doc, oe.ridOffset, inputAct);
                }
            }
        }
        parentNode.lock.releaseReadLock();
    }


    // TODO: RID
    public Option lookupOrOption(Document doc, Range r, boolean create) {
        Activation act = Activation.select(doc, this, null, r, Range.Relation.CONTAINS, null, null)
                .findFirst()
                .orElse(null);

        if(act != null) {
            return act.key.o;
        }
        for(Key ak: getThreadState(doc).added.keySet()) {
            if(Range.compare(ak.r, r) == 0) {
                return ak.o;
            }
        }
        return create ? Option.addPrimitive(doc) : null;
    }


    @Override
    protected Set<Refinement> collectNodeAndRefinements(Refinement newRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void changeNumberOfNeuronRefs(Document doc, long v, int d) {
        throw new UnsupportedOperationException();
    }


    public void addInput(Document doc, Integer ridOffset, Node in) {
        in.changeNumberOfNeuronRefs(doc, Node.visitedCounter++, 1);
        in.addOrChild(doc, new OrEntry(ridOffset, this));
        lock.acquireWriteLock(doc.threadId);
        Integer key = ridOffset != null ? ridOffset : Integer.MIN_VALUE;
        TreeSet<Node> pn = parents.get(key);
        if(pn == null) {
            pn = new TreeSet();
            parents.put(key, pn);
        }
        pn.add(in);
        lock.releaseWriteLock();
    }


    public void removeInput(Document doc, Integer ridOffset, Node in) {
        in.changeNumberOfNeuronRefs(doc, Node.visitedCounter++, -1);
        in.removeOrChild(doc, new OrEntry(ridOffset, this));
        lock.acquireWriteLock(doc.threadId);
        Integer key = ridOffset != null ? ridOffset : Integer.MIN_VALUE;
        TreeSet<Node> pn = parents.get(key);
        if(pn != null) {
            pn.remove(in);
            if(pn.isEmpty() && ridOffset != null) {
                parents.remove(key);
            }
        }
        lock.releaseWriteLock();
    }


    public void removeAllInputs(Document doc) {
        lock.acquireWriteLock(doc.threadId);
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            for(Node pn: me.getValue()) {
                pn.changeNumberOfNeuronRefs(doc, Node.visitedCounter++, -1);
                pn.removeOrChild(doc, new OrEntry(me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, this));
            }
        }
        parents.clear();
        lock.releaseWriteLock();
    }


    public void remove(Document doc) {
        super.remove(doc);

        lock.acquireReadLock();
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            for(Node pn: me.getValue()) {
                pn.removeOrChild(doc, new OrEntry(me.getKey() != Integer.MIN_VALUE ? me.getKey() : null, this));
            }
        }
        lock.releaseReadLock();
    }


    public String logicToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OR[");
        boolean first = true;
        int i = 0;
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            for (Node pn : me.getValue()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(me.getKey() != Integer.MIN_VALUE ? me.getKey() : "X");
                sb.append(":");
                sb.append(pn.logicToString());
                if (i > 10) {
                    sb.append(",...");
                    break;
                }

                i++;
            }
        }

        sb.append("]");
        return sb.toString();
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF("O");
        super.write(out);

        out.writeInt(parents.size());
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            out.writeInt(me.getKey());
            out.writeInt(me.getValue().size());
            for(Node pn: me.getValue()) {
                out.writeInt(pn.id);
            }
        }
    }


    @Override
    public void readFields(DataInput in, Document doc) throws IOException {
        super.readFields(in, doc);

        int s = in.readInt();
        for(int i = 0; i < s; i++) {
            TreeSet<Node> ridParents = new TreeSet<>();
            Integer ridOffset = in.readInt();
            parents.put(ridOffset, ridParents);

            int sa = in.readInt();
            for(int j = 0; j < sa; j++) {
                Node pn = doc.m.initialNodes.get(in.readInt());
                pn.addOrChild(doc, new OrEntry(ridOffset, this));
                ridParents.add(pn);
            }
        }
    }


    public static class OrEntry implements Comparable<OrEntry>, Writable {
        public Integer ridOffset;
        public Node node;


        private OrEntry() {}


        public OrEntry(Integer ridOffset, Node node) {
            this.ridOffset = ridOffset;
            this.node = node;
        }


        @Override
        public void write(DataOutput out) throws IOException {
            out.writeBoolean(ridOffset != null);
            if(ridOffset != null) {
                out.writeInt(ridOffset);
            }
            out.writeInt(node.id);
        }


        @Override
        public void readFields(DataInput in, Document doc) throws IOException {
            if(in.readBoolean()) {
                ridOffset = in.readInt();
            }
            node = doc.m.initialNodes.get(in.readInt());
        }


        public static OrEntry read(DataInput in, Document doc) throws IOException {
            OrEntry n = new OrEntry();
            n.readFields(in, doc);
            return n;
        }


        @Override
        public int compareTo(OrEntry on) {
            int r = Utils.compareInteger(ridOffset, on.ridOffset);
            if(r != 0) return r;
            return node.compareTo(on.node);
        }
    }

}
