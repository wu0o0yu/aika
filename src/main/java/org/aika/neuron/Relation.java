package org.aika.neuron;


import org.aika.Model;
import org.aika.Writable;
import org.aika.neuron.activation.Activation;
import org.aika.neuron.activation.Range;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


public abstract class Relation implements Comparable<Relation>, Writable {


    public abstract boolean test(Activation act, Activation linkedAct);

    public abstract Relation invert();


    public static Relation read(DataInput in, Model m) throws IOException {
        if(in.readBoolean()) {
            return InstanceRelation.read(in, m);
        } else {
            return RangeRelation.read(in, m);
        }
    }


    public static class InstanceRelation extends Relation {
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

            for(Activation.SynapseActivation sa: actA.neuronInputs) {
                if(!sa.synapse.isRecurrent) {
                    if(contains(sa.input, actB, v)) return true;
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

            for(Activation.SynapseActivation sa: act.neuronInputs) {
                if(!sa.synapse.isRecurrent) {
                    markAncestors(sa.input, v);
                }
            }
        }


        private static boolean hasCommonAncestor(Activation act, long v1, long v2) {
            if(act.visited == v2) return false;
            act.visited = v2;

            if(act.markedAncestor == v1) return true;

            for(Activation.SynapseActivation sa: act.neuronInputs) {
                if(!sa.synapse.isRecurrent) {
                    if(hasCommonAncestor(sa.input, v1, v2)) return true;
                }
            }
            return false;
        }



        @Override
        public void write(DataOutput out) throws IOException {
            out.writeBoolean(true);
        }

        @Override
        public void readFields(DataInput in, Model m) throws IOException {

        }

        public static InstanceRelation read(DataInput in, Model m) throws IOException {
            InstanceRelation ir = new InstanceRelation();
            ir.readFields(in, m);
            return ir;
        }

        @Override
        public int compareTo(Relation rel) {
            if(rel instanceof RangeRelation) return 1;
            InstanceRelation ir = (InstanceRelation) rel;

            return type.compareTo(ir.type);
        }
    }


    public static class RangeRelation extends Relation {
        Range.Relation relation;

        RangeRelation() {}

        public RangeRelation(Range.Relation relation) {
            this.relation = relation;
        }


        @Override
        public boolean test(Activation act, Activation linkedAct) {
            return relation.compare(act.range, linkedAct.range);
        }

        @Override
        public Relation invert() {
            return new RangeRelation(relation.invert());
        }


        @Override
        public int compareTo(Relation rel) {
            if(rel instanceof InstanceRelation) return -1;
            RangeRelation rr = (RangeRelation) rel;

            return relation.compareTo(rr.relation);
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeBoolean(false);
            relation.write(out);
        }

        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            relation = Range.Relation.read(in, m);
        }

        public static RangeRelation read(DataInput in, Model m) throws IOException {
            RangeRelation rr = new RangeRelation();
            rr.readFields(in, m);
            return rr;
        }
    }
}
