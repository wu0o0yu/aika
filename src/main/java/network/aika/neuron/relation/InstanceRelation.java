package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class InstanceRelation extends Relation {
    public Type type;

    public enum Type  {
        COMMON_ANCESTOR,
        CONTAINS,
        CONTAINED_IN
    }

    InstanceRelation() {}

    public InstanceRelation(Type type) {
        this.type = type;
    }


    @Override
    public Collection<Activation> getActivations(INeuron n, Activation linkedAct) {
        List<Activation> results = new ArrayList<>();
        switch(type) {
            case COMMON_ANCESTOR:
                collectCommonAncestor(results, n, linkedAct, linkedAct.doc.visitedCounter++);
            case CONTAINS:
                collectContains(results, n, linkedAct, linkedAct.doc.visitedCounter++);
            case CONTAINED_IN:
                collectContainedIn(results, n, linkedAct, linkedAct.doc.visitedCounter++);
        }
        return results;
    }


    private void collectCommonAncestor(Collection<Activation> results, INeuron n, Activation linkedAct, long v) {
        if(linkedAct.visited == v) return;

        collectContains(results, n, linkedAct, v);

        for(Activation.Link l: linkedAct.neuronInputs.values()) {
            if(!l.passive && l.synapse.key.identity) {
                collectCommonAncestor(results, n, l.input, v);
            }
        }
    }


    private void collectContains(Collection<Activation> results, INeuron n, Activation linkedAct, long v) {
        if(linkedAct.visited == v) return;
        linkedAct.visited = v;

        if(linkedAct.getINeuron() == n) {
            results.add(linkedAct);
        }

        for(Activation.Link l: linkedAct.neuronOutputs.values()) {
            if(!l.passive && l.synapse.key.identity) {
                collectContains(results, n, l.output, v);
            }
        }
    }


    private void collectContainedIn(Collection<Activation> results, INeuron n, Activation linkedAct, long v) {
        if(linkedAct.visited == v) return;
        linkedAct.visited = v;

        if(linkedAct.getINeuron() == n) {
            results.add(linkedAct);
        }

        for(Activation.Link l: linkedAct.neuronInputs.values()) {
            if(!l.passive && l.synapse.key.identity) {
                collectContainedIn(results, n, l.input, v);
            }
        }
    }



    @Override
    public boolean test(Activation act, Activation linkedAct) {
        switch(type) {
            case COMMON_ANCESTOR:
                return hasCommonAncestor(act, linkedAct);
            case CONTAINS:
                return contains(act, linkedAct, act.doc.visitedCounter++);
            case CONTAINED_IN:
                return contains(linkedAct, act, act.doc.visitedCounter++);
        }
        return true;
    }


    @Override
    public Relation invert() {
        switch(type) {
            case COMMON_ANCESTOR:
                return this;
            case CONTAINS:
                return new InstanceRelation(Type.CONTAINED_IN);
            case CONTAINED_IN:
                return new InstanceRelation(Type.CONTAINS);
        }
        return null;
    }


    private static boolean contains(Activation actA, Activation actB, long v) {
        if(actA.visited == v) return false;
        actA.visited = v;

        if(actA == actB) return true;

        for(Activation.Link l: actA.neuronInputs.values()) {
            if(!l.passive && l.synapse.key.identity) {
                if(contains(l.input, actB, v)) return true;
            }
        }
        return false;
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

        for(Activation.Link l: act.neuronInputs.values()) {
            if(!l.passive && l.synapse.key.identity) {
                markAncestors(l.input, v);
            }
        }
    }


    private static boolean hasCommonAncestor(Activation act, long v1, long v2) {
        if(act.visited == v2) return false;
        act.visited = v2;

        if(act.markedAncestor == v1) return true;

        for(Activation.Link l: act.neuronInputs.values()) {
            if(!l.passive && l.synapse.key.identity) {
                if(hasCommonAncestor(l.input, v1, v2)) return true;
            }
        }
        return false;
    }



    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(true);

        out.writeUTF(type.name());
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        type = Type.valueOf(in.readUTF());
    }


    public static InstanceRelation read(DataInput in, Model m) throws IOException {
        InstanceRelation ir = new InstanceRelation();
        ir.readFields(in, m);
        return ir;
    }

    @Override
    public boolean isExact() {
        return false;
    }


    @Override
    public int compareTo(Relation rel) {
        if(rel instanceof RangeRelation) return 1;
        InstanceRelation ir = (InstanceRelation) rel;

        return type.compareTo(ir.type);
    }
}
