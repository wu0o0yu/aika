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
package org.aika.corpus;


import org.aika.Activation;
import org.aika.Activation.Key;
import org.aika.Iteration;
import org.aika.lattice.Node;

import java.util.*;


/**
 * The <code>Option</code> class represents a node within the options lattice. Such a node consists of a set of
 * primitive options that are emitted by each <code>Neuron</code>. There may be conflicts between options in the lattice.
 *
 * @author Lukas Molzberger
 */
public class Option implements Comparable<Option> {

    public static final Option MIN = new Option(null, -1, 0, 0);
    public static final Option MAX = new Option(null, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);


    public final int primId;
    public int minPrim = -1;
    public int maxPrim = -1;

    public final int id;
    public int length;

    public Map<Activation, Option> orOptions;
    public Set<Option> refByOrOption;
    public Option largestCommonSubset;
    public Set<Option> linkedByLCS;

    private long visitedLinkRelations;
    private long visitedContains;
    private long visitedCollect;
    private long visitedExpandActivations;
    private long visitedRemoveActivations;
    public long visitedMarkCovered;
    public long markedCovered = -1;
    public long markedExcluded = -1;
    public long markedSelected;
    private long visitedIsConflicting;
    public long visitedCollectAllConflicting = -1;
    private long visitedStoreFinalWeight = -1;
    public long visitedExpandRefinementRecursiveStep = -1;
    public long visitedCollectConflicts = -1;
    private long visitedComputeLargestCommonSubset = -1;
    private long visitedComputeLength = -1;
    public long visitedCheckExcluded = -1;
    public long markedConflict = -1;
    private long visitedComputeParents = -1;
    private long visitedNumberInnerInputs = -1;

    private int numberInnerInputs = 0;

    private int largestCommonSubsetCount = 0;


    public static long visitedCounter = 1;

    public final Document doc;

    public boolean isRemoved;
    public int removedId;
    public static int removedIdCounter = 1;

    public Option[] parents;
    public Option[] children;

    public int isConflict = -1;
    public Conflicts conflicts = new Conflicts();

    public NavigableMap<Key, Activation> activations;
    public NavigableSet<Activation> neuronActivations;


    public int refCount = 0;


    public enum Relation {
        EQUALS,
        CONTAINS,
        CONTAINED_IN;

        public boolean compare(Option a, Option b) {
            switch (this) {
                case EQUALS:
                    return a == b;
                case CONTAINS:
                    return a.contains(b, false);
                case CONTAINED_IN:
                    return b.contains(a, false);
                default:
                    return false;
            }
        }
    }

    public Option(Document doc, int primId, int id, int length) {
        this(doc, primId, id);
        this.length = length;
    }


    public Option(Document doc, int primId, int id) {
        this.doc = doc;
        this.primId = primId;
        this.id = id;
        parents = new Option[0];
        children = new Option[0];
    }


    public void computeLargestCommonSubset() {
        int s = orOptions.size();
        long vMin = Option.visitedCounter++;
        List<Option> results = new ArrayList<>();
        for (Option on : orOptions.values()) {
            on.computeLargestCommonSubsetRecursiveStep(results, Option.visitedCounter++, vMin, s, 0);
        }
        setLCS(results.isEmpty() ? null : Option.add(doc, true, results));
    }


    public void computeLargestCommonSubsetIncremental(Option no) {
        if (orOptions.size() == 0) {
            setLCS(no);
            return;
        }
        long vMin = Option.visitedCounter++;
        List<Option> results = new ArrayList<>();
        largestCommonSubset.computeLargestCommonSubsetRecursiveStep(results, Option.visitedCounter++, vMin, 2, 0);
        no.computeLargestCommonSubsetRecursiveStep(results, Option.visitedCounter++, vMin, 2, 0);
        setLCS(Option.add(doc, true, results));
    }


    private void setLCS(Option lcs) {
        if (largestCommonSubset != null) {
            largestCommonSubset.linkedByLCS.remove(this);
        }
        largestCommonSubset = lcs;
        if (largestCommonSubset != null) {
            if(largestCommonSubset.linkedByLCS == null) {
                largestCommonSubset.linkedByLCS = new TreeSet<>();
            }
            largestCommonSubset.linkedByLCS.add(this);
        }
    }


    private void computeLargestCommonSubsetRecursiveStep(List<Option> results, long v, long vMin, int s, int depth) {
        if (visitedComputeLargestCommonSubset == v) return;
        if (visitedComputeLargestCommonSubset <= vMin) largestCommonSubsetCount = 0;
        visitedComputeLargestCommonSubset = v;
        largestCommonSubsetCount++;

        if(depth > 10) return;

        if (largestCommonSubsetCount == s) {
            results.add(this);
            return;
        }

        for (Option pn : parents) {
            pn.computeLargestCommonSubsetRecursiveStep(results, v, vMin, s, depth + 1);
        }

        if (largestCommonSubset != null) {
            largestCommonSubset.computeLargestCommonSubsetRecursiveStep(results, v, vMin, s, depth + 1);
        }
    }


    public void addOrOption(Activation inputAct, Option n) {
        if (orOptions == null) {
            orOptions = new TreeMap<>();
        }
        computeLargestCommonSubsetIncremental(n);
        orOptions.put(inputAct, n);
        if (n.refByOrOption == null) {
            n.refByOrOption = new TreeSet<>();
        }
        n.refByOrOption.add(this);
    }


    public void removeOrOption(Activation inputAct, Option n) {
        orOptions.remove(inputAct);
        n.refByOrOption.remove(this);
        computeLargestCommonSubset();
    }


    public void countRef() {
        if (isBottom()) return;
        refCount++;
    }


    public void releaseRef() {
        if (isBottom()) return;
        assert refCount > 0;
        refCount--;
        if (refCount == 0) {
            remove();
        }
    }


    void expandActivationsRecursiveStep(Iteration t, Option conflict, long v) {
        if (v == visitedExpandActivations) return;
        visitedExpandActivations = v;

        for (Activation act : getActivations()) {
            act.key.n.propagateAddedActivation(t, act, conflict);
        }

        for (Option p : parents) {
            p.expandActivationsRecursiveStep(t, conflict, v);
        }
    }


    void removeActivationsRecursiveStep(Iteration t, Option conflict, long v) {
        if (v == visitedRemoveActivations) return;
        visitedRemoveActivations = v;

        for (Activation act : getActivations()) {
            if (act.key.o.contains(conflict, false)) {
                Node.removeActivationAndPropagate(t, act, act.inputs.values());
            }
        }

        if (children != null) {
            for (Option c : children) {
                if (!c.isRemoved) {
                    c.removeActivationsRecursiveStep(t, conflict, v);
                }
            }
        }
    }


    public Collection<Activation> getActivations() {
        return activations != null ? activations.values() : Collections.emptySet();
    }


    public Collection<Activation> getNeuronActivations() {
        return neuronActivations != null ? neuronActivations : Collections.emptySet();
    }


    public static Option add(Document doc, boolean nonConflicting, Option... input) {
        ArrayList<Option> in = new ArrayList<>();
        for (Option n : input) {
            if (n != null && !n.isBottom()) in.add(n);
        }
        return add(doc, nonConflicting, in);
    }


    public static Option add(Document doc, boolean nonConflicting, List<Option> inputs) {
        if (inputs.size() == 0) return doc.bottom;

        for (Iterator<Option> it = inputs.iterator(); it.hasNext(); ) {
            if (it.next().isBottom()) {
                it.remove();
            }
        }

        if (inputs.size() == 1 || (inputs.size() == 2 && inputs.get(0) == inputs.get(1))) {
            Option n = inputs.get(0);
            if (nonConflicting && n.isConflicting(visitedCounter++)) return null;
            n.countRef();
            return n;
        }

        ArrayList<Option> parents = new ArrayList<>();
        ArrayList<Option> children = new ArrayList<>();
        computeRelations(parents, children, inputs);

        if (parents.size() == 1) {
            Option n = parents.get(0);
            if (nonConflicting && n.isConflicting(visitedCounter++)) return null;
            n.countRef();
            return n;
        }

        if (nonConflicting) {
            for (Option p : parents) {
                if (p.isConflicting(visitedCounter++)) {
                    return null;
                }
            }
        }

        Option n = new Option(doc, -1, doc.optionIdCounter++);

        n.linkRelations(parents, children, visitedCounter++);

        n.length = n.computeLength(Option.visitedCounter++);

        n.minPrim = Integer.MAX_VALUE;
        n.maxPrim = Integer.MIN_VALUE;
        for(Option in: inputs) {
            n.minPrim = Math.min(n.minPrim, in.minPrim);
            n.maxPrim = Math.max(n.maxPrim, in.maxPrim);
        }

        n.countRef();

        return n;
    }


    private static Comparator<Option> LENGTH_COMP = new Comparator<Option>() {
        @Override
        public int compare(Option n1, Option n2) {
            return Integer.compare(n2.length, n1.length);
        }
    };


    public static void computeRelations(List<Option> parentsResults, List<Option> childrenResults, List<Option> inputs) {
        if (inputs.isEmpty()) return;
        long v = Option.visitedCounter++;
        int i = 0;
        int s = inputs.size();

        Collections.sort(inputs, LENGTH_COMP);

        if (s == 2 && inputs.get(1).primId >= 0 && inputs.get(1).children.length == 0) {
            parentsResults.addAll(inputs);
            return;
        }

        for(int pass = 0; pass <= 1; pass++) {
            for (Option n : inputs) {
                n.computeParents(parentsResults, v, pass);
            }
            v = visitedCounter++;
        }

        if(parentsResults.size() == 1) return;
        assert parentsResults.size() != 0;

        for (Option n : inputs) {
            n.computeChildren(childrenResults, visitedCounter++, v, inputs.size(), 0);
        }

        inputs.get(0).computeChildren(childrenResults, visitedCounter++, v, inputs.size(), 1);
    }


    private void computeParents(List<Option> parentResults, long v, int pass) {
        if (visitedComputeParents == v || length == 0) return;
        visitedComputeParents = v;

        for (Option pn: parents) {
            pn.computeParents(parentResults, v, pass);
        }

        boolean flag = true;
        for(Option cn: children) {
            if(pass == 0) {
                if (cn.visitedNumberInnerInputs != v) {
                    cn.numberInnerInputs = 0;
                    cn.visitedNumberInnerInputs = v;
                }
                cn.numberInnerInputs++;
            }

            if(cn.numberInnerInputs == cn.parents.length) {
                cn.computeParents(parentResults, v, pass);
                flag = false;
            }
        }

        if(flag && pass == 1) {
            parentResults.add(this);
        }
    }


    private long visitedComputeChildren = -1;
    private int numberOfInputsComputeChildren = 0;


    private void computeChildren(List<Option> childrenResults, long v, long nv, int s, int pass) {
        if (visitedComputeChildren == v) return;

        if (pass == 0) {
            if (visitedComputeChildren <= nv) {
                numberOfInputsComputeChildren = 0;
            }
            numberOfInputsComputeChildren++;
        }

        visitedComputeChildren = v;

        if(pass == 1 && numberOfInputsComputeChildren == s) {
            boolean covered = false;
            for(Option pn: parents) {
                if(pn.numberOfInputsComputeChildren == s) {
                    covered = true;
                    break;
                }
            }

            if(!covered) {
                childrenResults.add(this);
            }
        } else {
            for (Option cn : children) {
                cn.computeChildren(childrenResults, v, nv, s, pass);
            }
        }
    }


    private int computeLength(long v) {
        if (visitedComputeLength == v) return 0;
        visitedComputeLength = v;

        if (primId >= 0) return 1;

        int result = 0;
        for (Option p : parents) {
            result += p.computeLength(v);
        }
        return result;
    }


    private void linkRelations(List<Option> pSet, List<Option> cSet, long v) {
        for (Option p : pSet) {
            addLink(p, this);
        }
        for (Option c : cSet) {
            c.visitedLinkRelations = v;
            addLink(this, c);
        }

        for (Option p : pSet) {
            ArrayList<Option> tmp = new ArrayList<>();
            for (Option c : p.children) {
                if (c.visitedLinkRelations == v) {
                    tmp.add(c);
                }
            }

            for (Option c : tmp) {
                removeLink(p, c);
            }
        }
    }


    public static void addLink(Option a, Option b) {
        a.children = addToArray(a.children, b);
        b.parents = addToArray(b.parents, a);
    }


    public static void removeLink(Option a, Option b) {
        a.children = removeToArray(a.children, b);
        b.parents = removeToArray(b.parents, a);
    }


    private static Option[] addToArray(Option[] in, Option n) {
        Option[] r = Arrays.copyOf(in, in.length + 1);
        r[in.length] = n;
        return r;
    }


    private static Option[] removeToArray(Option[] in, Option n) {
        Option[] r = new Option[in.length - 1];
        int i = 0;
        for(Option x: in) {
            if(x != n) {
                r[i++] = x;
            }
        }

        return r;
    }


    public static Option addPrimitive(Document doc) {
        assert doc != null;

        Option n = new Option(doc, doc.bottom.children.length, doc.optionIdCounter++, 1);

        n.minPrim = n.primId;
        n.maxPrim = n.primId;

        n.countRef();

        addLink(doc.bottom, n);
        return n;
    }


    private void remove() {
        assert !isRemoved;
        isRemoved = true;
        removedId = removedIdCounter++;

        for (Option p : parents) {
            p.children = removeToArray(p.children, this);
        }
        for (Option c : children) {
            c.parents = removeToArray(c.parents, this);
        }
        for (Option p : parents) {
            for (Option c : children) {
                if (!c.isLinked(p, visitedCounter++)) {
                    addLink(p, c);
                }
            }
        }

        parents = null;
        children = null;
        conflicts = null;
    }


    public boolean isBottom() {
        return length == 0;
    }


    public boolean contains(boolean dir, Option n, boolean followLCS) {
        boolean r;
        if (!dir) {
            r = contains(n, followLCS);
        } else {
            r = n.contains(this, followLCS);
        }
        return r;
    }


    public boolean contains(Option n, boolean followLCS) {
        return contains(n, followLCS, visitedCounter++);
    }


    private boolean contains(Option n, boolean followLCS, long v) {
        visitedContains = v;

        if(this == n || n.isBottom()) {
            return true;
        }

        if(!followLCS && length <= n.length) return false;

        for(Option p: parents) {
            if(n.maxPrim >= p.minPrim && n.minPrim <= p.maxPrim &&
                    (p.visitedContains != v && p.contains(n, followLCS, v))) {
                return true;
            }
        }

        if(followLCS && largestCommonSubset != null) {
            if(largestCommonSubset.contains(n, followLCS, v)) return true;
        }

        return false;
    }


    private boolean isLinked(Option n, long v) {
        assert visitedContains <= v;
        assert !isRemoved;
        assert !n.isRemoved;

        if(this == n) {
            return true;
        }

        visitedContains = v;
        if(length < n.length) return false;

        for(Option p: parents) {
            if(p.visitedContains != v && p.isLinked(n, v)) return true;
        }
        return false;
    }


    private void collectPrimitiveNodes(Set<Option> results, long v) {
        if(v == visitedCollect) return;
        visitedCollect = v;

        if(primId >= 0) {
            results.add(this);
        } else {
            for(Option n: parents) {
                n.collectPrimitiveNodes(results, v);
            }
        }
    }


    public void collectConflicts(Set<Option> conflicts, long v) {
        if(visitedCollectConflicts == v) return;
        visitedCollectConflicts = v;

        for(Option n: children) {
            if(isConflict >= 0) {
                conflicts.add(n);
            }
            n.collectConflicts(conflicts, v);
        }
    }


    public void markSelected(long v) {
        if(markedSelected == v) return;
        markedSelected = v;

        for(Option p: parents) {
            p.markSelected(v);
        }
    }


    public boolean isConflicting(long v) {
        if (isConflict >= 0) {
            return true;
        } else if(conflictsAllowed()) {
            if(visitedIsConflicting == v) return false;
            visitedIsConflicting = v;

            for(Option p : parents) {
                if(p.isConflicting(v)) {
                    return true;
                }
            }
        }
        return false;
    }


    public boolean conflictsAllowed() {
        return activations == null || activations.isEmpty();
    }


    public void storeFinalWeight(long v) {
        if(visitedStoreFinalWeight == v) return;
        visitedStoreFinalWeight = v;

        for(Activation act: getNeuronActivations()) {
            act.finalState = act.rounds.getLast();
        }

        for(Option cn: children) {
            cn.storeFinalWeight(v);
        }
    }


    public String toString() {
        return toString(false);
    }


    private String toString(boolean level) {
        SortedSet<Option> ids = new TreeSet<>();
        collectPrimitiveNodes(ids, visitedCounter++);

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        boolean f1 = true;
        for(Option n: ids) {
            if(!f1) sb.append(",");
            f1 = false;
            sb.append(n.primId);
            if(!level && n.orOptions != null) {
                sb.append("[");
                boolean f2 = true;
                for(Option on: n.orOptions.values()) {
                    if(!f2) sb.append(",");
                    f2 = false;
                    sb.append(on.toString(true));
                }
                sb.append("]");
            }
        }

        sb.append(")");
        return sb.toString();
    }


    @Override
    public int compareTo(Option n) {
        int r = Integer.compare(length, n.length);
        if(r != 0) return r;
        return Integer.compare(id, n.id);
    }


    public static int compare(Option oa, Option ob) {
        if(oa == ob) return 0;
        if(oa == null && ob != null) return -1;
        if(oa != null && ob == null) return 1;
        return oa.compareTo(ob);
    }
}
