package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.range.Range;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class AncestorRelation extends Relation {
    public static final int RELATION_TYPE = 1;


    public Type type;

    public enum Type  {
        COMMON_ANCESTOR,
        IS_DESCENDANT_OF,
        IS_ANCESTOR_OF
    }

    AncestorRelation() {}

    public AncestorRelation(Type type) {
        this.type = type;
    }

    @Override
    public Collection<Activation> getActivations(INeuron n, Activation linkedAct) {
        List<Activation> results = new ArrayList<>();
        switch(type) {
            case COMMON_ANCESTOR:
                collectCommonAncestor(results, n, linkedAct, linkedAct.doc.visitedCounter++);
            case IS_DESCENDANT_OF:
                collectContains(results, n, linkedAct, linkedAct.doc.visitedCounter++);
            case IS_ANCESTOR_OF:
                collectContainedIn(results, n, linkedAct, linkedAct.doc.visitedCounter++);
        }
        return results;
    }


    private void collectCommonAncestor(Collection<Activation> results, INeuron n, Activation linkedAct, long v) {
        if(linkedAct.visited == v) return;

        collectContains(results, n, linkedAct, v);

        linkedAct.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .forEach(l -> collectCommonAncestor(results, n, l.input, v));
    }


    private void collectContains(Collection<Activation> results, INeuron n, Activation linkedAct, long v) {
        if(linkedAct.visited == v) return;
        linkedAct.visited = v;

        if(linkedAct.getINeuron() == n) {
            results.add(linkedAct);
        }

        linkedAct.getOutputLinks(false)
                .filter(l ->l.synapse.identity)
                .forEach(l -> collectContains(results, n, l.output, v));
    }


    private void collectContainedIn(Collection<Activation> results, INeuron n, Activation linkedAct, long v) {
        if(linkedAct.visited == v) return;
        linkedAct.visited = v;

        if(linkedAct.getINeuron() == n) {
            results.add(linkedAct);
        }

        linkedAct.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .forEach(l -> collectContainedIn(results, n, l.input, v));
    }



    @Override
    public boolean test(Activation act, Activation linkedAct) {
        switch(type) {
            case COMMON_ANCESTOR:
                return hasCommonAncestor(act, linkedAct);
            case IS_DESCENDANT_OF:
                return contains(act, linkedAct, act.doc.visitedCounter++);
            case IS_ANCESTOR_OF:
                return contains(linkedAct, act, act.doc.visitedCounter++);
        }
        return true;
    }


    @Override
    public Relation invert() {
        switch(type) {
            case COMMON_ANCESTOR:
                return this;
            case IS_DESCENDANT_OF:
                return new AncestorRelation(Type.IS_ANCESTOR_OF);
            case IS_ANCESTOR_OF:
                return new AncestorRelation(Type.IS_DESCENDANT_OF);
        }
        return null;
    }


    @Override
    public Range mapRange(Activation act, Linker.Direction direction) {
        return null;
    }


    @Override
    public boolean linksOutputBegin() {
        return false;
    }

    @Override
    public boolean linksOutputEnd() {
        return false;
    }


    private static boolean contains(Activation actA, Activation actB, long v) {
        if(actA.visited == v) return false;
        actA.visited = v;

        if(actA == actB) return true;

        return actA.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .anyMatch(l -> contains(l.input, actB, v));
    }


    private static boolean hasCommonAncestor(Activation act, Activation linkedAct) {
        long v = act.doc.visitedCounter++;
        markAncestors(linkedAct, v);
        return hasCommonAncestor(act, v, act.doc.visitedCounter++);
    }


    private static void markAncestors(Activation act, long v) {
        if(act.visited == v) return;
        act.visited = v;

        act.markedAncestor = v;

        act.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .forEach(l -> markAncestors(l.input, v));
    }


    private static boolean hasCommonAncestor(Activation act, long v1, long v2) {
        if(act.visited == v2) return false;
        act.visited = v2;

        if(act.markedAncestor == v1) return true;

        return act.getInputLinks(false, false)
                .filter(l -> l.synapse.identity)
                .anyMatch(l -> hasCommonAncestor(l.input, v1, v2));
    }



    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(getRelationType());

        out.writeUTF(type.name());
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        type = Type.valueOf(in.readUTF());
    }


    public static AncestorRelation read(DataInput in, Model m) throws IOException {
        AncestorRelation ir = new AncestorRelation();
        ir.readFields(in, m);
        return ir;
    }

    @Override
    public boolean isExact() {
        return false;
    }


    @Override
    public int getRelationType() {
        return RELATION_TYPE;
    }

    @Override
    public int compareTo(Relation rel) {
        AncestorRelation ir = (AncestorRelation) rel;

        return type.compareTo(ir.type);
    }
}
