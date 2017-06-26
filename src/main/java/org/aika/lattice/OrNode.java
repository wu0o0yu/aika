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
import org.aika.corpus.Option;
import org.aika.corpus.Range;
import org.aika.lattice.AndNode.Refinement;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;


/**
 *
 * @author Lukas Molzberger
 */
public class OrNode extends Node {


    public TreeSet<Node> nullRidParents = new TreeSet<>();
    public TreeMap<Integer, TreeSet<Node>> parents = new TreeMap<>();


    public OrNode() {}


    public OrNode(Iteration t) {
        super(t, -1); // Or Node Activations always need to be processed first!
        Model m = t.m;

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
    public boolean isAllowedOption(Iteration t, Option n, Activation act, long v) {
        return false;
    }


    @Override
    public void initActivation(Iteration t, Activation act) {
        for(Synapse s: neuron.inputSynapses) {
            if(s.key.isNeg || s.key.isRecurrent) {
                for (Activation iAct : Activation.select(t, s.inputNode, Utils.nullSafeAdd(act.key.rid, false, s.key.relativeRid, false), act.key.r, Range.Relation.OVERLAPS, null, null)) {
                    iAct.outputs.put(act.key, act);
                }
            }
        }

        if(getThreadState(t).activations.isEmpty()) {
            t.activatedNeurons.add(neuron);
        }
    }


    @Override
    public void deleteActivation(Iteration t, Activation act) {
        if(getThreadState(t).activations.isEmpty()) {
            t.activatedNeurons.remove(neuron);
        }
    }


    public void updateActivation(Iteration t, Range inputR, Integer rid) {
        List<Activation> inputs = new ArrayList<>();
        Range r = null;

        for (Map.Entry<Integer, TreeSet<Node>> me : parents.entrySet()) {
            r = extractRange(t, inputR, rid, inputs, r, me.getKey(), me.getValue());
        }
        if (!nullRidParents.isEmpty()) {
            r = extractRange(t, inputR, rid, inputs, r, null, nullRidParents);
        }


        for(Activation oAct: Activation.select(t, this, rid, inputR, Range.Relation.OVERLAPS, null, null)) {
            for (Iterator<Activation> it = oAct.inputs.values().iterator(); it.hasNext(); ) {
                Activation iAct = it.next();

                if(r == null || Range.compare(r, oAct.key.r) != 0) {
                    oAct.isReplaced = true;
                    oAct.key.o.removeOrOption(iAct, iAct.key.o);
                    removeActivationAndPropagate(t, oAct, oAct.inputs.values());
                }
                if(iAct.isRemoved) {
                    oAct.key.o.removeOrOption(iAct, iAct.key.o);
                    it.remove();
                }
            }
        }

        if(inputs.isEmpty()) return;

        Option no = lookupOrOption(t, r, true);

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
                t,
                nak,
                inputs
        );
    }


    private Range extractRange(Iteration t, Range inputR, Integer rid, List<Activation> inputs, Range r, Integer pRidOffset, TreeSet<Node> parents) {
        // Optimization the number of parents can get very large, thus we need to avoid iterating over all of them.
        if(parents.size() > 10) {
            return extractRange(t, null, inputR, rid, inputs, r, pRidOffset, parents);
        } else {
            for(Node pn: parents) {
                r = extractRange(t, pn, inputR, rid, inputs, r, pRidOffset, parents);
            }
            return r;
        }
    }


    private Range extractRange(Iteration t, Node n, Range inputR, Integer rid, List<Activation> inputs, Range r, Integer pRidOffset, TreeSet<Node> parents) {
        for(Activation iAct: Activation.select(t, n, Utils.nullSafeAdd(rid, true, pRidOffset, false), inputR, Range.Relation.OVERLAPS, null, null)) {
            if(!iAct.isRemoved && parents.contains(iAct.key.n) && !checkSelfReferencing(t, iAct)) {
                inputs.add(iAct);
                r = r == null ? iAct.key.r : new Range(Math.min(r.begin, iAct.key.r.begin), Math.max(r.end, iAct.key.r.end));
            }
        }
        return r;
    }


    public void addActivation(Iteration t, Integer ridOffset, Activation inputAct) {
        if(checkSelfReferencing(t, inputAct)) return;

        Key ak = inputAct.key;
        updateActivation(t, ak.r, Utils.nullSafeSub(ak.rid, true, ridOffset, false));
    }


    public void removeActivation(Iteration t, Integer ridOffset, Activation inputAct) {
        if(checkSelfReferencing(t, inputAct)) return;

        Key ak = inputAct.key;
        updateActivation(t, ak.r, Utils.nullSafeSub(ak.rid, true, ridOffset, false));
    }


    private boolean checkSelfReferencing(Iteration t, Activation inputAct) {
        Option o = lookupOrOption(t, inputAct.key.r, false);
        if(o == null) return false;
        return inputAct.key.o.contains(o, true);
    }


    public void propagateAddedActivation(Iteration t, Activation act, Option removedConflict) {
        if(removedConflict == null) {
            neuron.propagateAddedActivation(t, act);
        }
    }


    public void propagateRemovedActivation(Iteration t, Activation act) {
        neuron.propagateRemovedActivation(t, act);
    }


    @Override
    public double computeSynapseWeightSum(Integer offset, Neuron n) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void cleanup(Iteration t) {

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
    public void apply(Iteration t, Activation act, Option conflict) {
        if(conflict == null) {
            OrNode.processCandidate(t, this, act, false);
        }
    }


    @Override
    public void discover(Iteration t, Activation act) {
    }


    public static void processCandidate(Iteration t, Node parentNode, Activation inputAct, boolean train) {
        Key ak = inputAct.key;
        parentNode.lock.acquireReadLock();
        if(parentNode.orChildren != null) {
            for (OrEntry oe : parentNode.orChildren) {
                if (!ak.o.isConflicting(Option.visitedCounter++)) {
                    ((OrNode) oe.node).addActivation(t, oe.ridOffset, inputAct);
                }
            }
        }
        parentNode.lock.releaseReadLock();
    }


    // TODO: RID
    public Option lookupOrOption(Iteration t, Range r, boolean create) {
        for(Activation act: Activation.select(t, this, null, r, Range.Relation.CONTAINS, null, null)) {
            return act.key.o;
        }
        for(Key ak: getThreadState(t).added.keySet()) {
            if(Range.compare(ak.r, r) == 0) {
                return ak.o;
            }
        }
        return create ? Option.addPrimitive(t.doc) : null;
    }


    @Override
    protected Set<Refinement> collectNodeAndRefinements(Refinement newRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void changeNumberOfNeuronRefs(Iteration t, long v, int d) {
        throw new UnsupportedOperationException();
    }


    public void addInput(Iteration t, Integer ridOffset, Node in) {
        in.changeNumberOfNeuronRefs(t, Node.visitedCounter++, 1);
        in.addOrChild(t, new OrEntry(ridOffset, this));
        lock.acquireWriteLock(t.threadId);
        TreeSet<Node> pn = ridOffset != null ? parents.get(ridOffset) : nullRidParents;
        if(pn == null) {
            pn = new TreeSet();
            parents.put(ridOffset, pn);
        }
        pn.add(in);
        lock.releaseWriteLock();
    }


    public void removeInput(Iteration t, Integer ridOffset, Node in) {
        in.changeNumberOfNeuronRefs(t, Node.visitedCounter++, -1);
        in.removeOrChild(t, new OrEntry(ridOffset, this));
        lock.acquireWriteLock(t.threadId);
        TreeSet<Node> pn = ridOffset != null ? parents.get(ridOffset) : nullRidParents;
        if(pn != null) {
            pn.remove(in);
            if(pn.isEmpty() && ridOffset != null) {
                parents.remove(ridOffset);
            }
        }
        lock.releaseWriteLock();
    }


    public void removeAllInputs(Iteration t) {
        lock.acquireWriteLock(t.threadId);
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            for(Node pn: me.getValue()) {
                pn.changeNumberOfNeuronRefs(t, Node.visitedCounter++, -1);
                pn.removeOrChild(t, new OrEntry(me.getKey(), this));
            }
        }
        for(Node pn: nullRidParents) {
            pn.changeNumberOfNeuronRefs(t, Node.visitedCounter++, -1);
            pn.removeOrChild(t, new OrEntry(null, this));
        }
        parents.clear();
        lock.releaseWriteLock();
    }


    public void remove(Iteration t) {
        super.remove(t);

        lock.acquireReadLock();
        for(Map.Entry<Integer, TreeSet<Node>> me: parents.entrySet()) {
            for(Node pn: me.getValue()) {
                pn.removeOrChild(t, new OrEntry(me.getKey(), this));
            }
        }
        for(Node pn: nullRidParents) {
            pn.removeOrChild(t, new OrEntry(null, this));
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
                sb.append(me.getKey());
                sb.append(":");
                sb.append(pn.logicToString());
                if (i > 10) {
                    sb.append(",...");
                    break;
                }

                i++;
            }
        }

        for(Node pn: nullRidParents) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("null");
            sb.append(":");
            sb.append(pn.logicToString());
            if (i > 10) {
                sb.append(",...");
                break;
            }

            i++;
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
        out.writeInt(nullRidParents.size());
        for(Node pn: nullRidParents) {
            out.writeInt(pn.id);
        }
    }


    @Override
    public void readFields(DataInput in, Iteration t) throws IOException {
        super.readFields(in, t);

        int s = in.readInt();
        for(int i = 0; i < s; i++) {
            TreeSet<Node> ridParents = new TreeSet<>();
            Integer ridOffset = in.readInt();
            parents.put(ridOffset, ridParents);

            int sa = in.readInt();
            for(int j = 0; j < sa; j++) {
                Node pn = t.m.initialNodes.get(in.readInt());
                pn.addOrChild(t, new OrEntry(ridOffset, this));
                ridParents.add(pn);
            }
        }

        int sa = in.readInt();
        for(int j = 0; j < sa; j++) {
            Node pn = t.m.initialNodes.get(in.readInt());
            pn.addOrChild(t, new OrEntry(null, this));
            nullRidParents.add(pn);
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
        public void readFields(DataInput in, Iteration t) throws IOException {
            if(in.readBoolean()) {
                ridOffset = in.readInt();
            }
            node = t.m.initialNodes.get(in.readInt());
        }


        public static OrEntry read(DataInput in, Iteration t) throws IOException {
            OrEntry n = new OrEntry();
            n.readFields(in, t);
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
