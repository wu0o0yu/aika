package org.aika.neuron;


import org.aika.neuron.activation.Activation;
import org.aika.neuron.activation.Range;

import static org.aika.neuron.Synapse.INPUT_SYNAPSE_COMP;

public abstract class Relation implements Comparable<Relation> {

    public Synapse linkedSynapse;


    public abstract boolean test(Activation act, Activation linkedAct);


    public static class InstanceRelation extends Relation {
        public Type type;


        public enum Type  {
            COMMON_ANCESTOR,
            CONTAINS,
            CONTAINED_IN
        }

        public InstanceRelation(Type type, Synapse s) {
            this.type = type;
            linkedSynapse = s;
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


        private static boolean contains(Activation actA, Activation actB, long v) {
            if(actA.visited == v) return false;
            actA.visited = v;

            if(actA == actB) return true;

            for(Activation.SynapseActivation sa: actA.neuronInputs) {
                if(!sa.synapse.key.isRecurrent) {
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
                if(!sa.synapse.key.isRecurrent) {
                    markAncestors(sa.input, v);
                }
            }
        }


        private static boolean hasCommonAncestor(Activation act, long v1, long v2) {
            if(act.visited == v2) return false;
            act.visited = v2;

            if(act.markedAncestor == v1) return true;

            for(Activation.SynapseActivation sa: act.neuronInputs) {
                if(!sa.synapse.key.isRecurrent) {
                    if(hasCommonAncestor(sa.input, v1, v2)) return true;
                }
            }
            return false;
        }


        @Override
        public int compareTo(Relation rel) {
            if(rel instanceof RangeRelation) return 1;
            InstanceRelation ir = (InstanceRelation) rel;

            int r = type.compareTo(ir.type);
            if(r != 0) return r;

            return INPUT_SYNAPSE_COMP.compare(linkedSynapse, rel.linkedSynapse);
        }
    }


    public static class RangeRelation extends Relation {
        Range.Relation relation;

        public RangeRelation(Range.Relation relation, Synapse s) {
            this.relation = relation;
            linkedSynapse = s;
        }


        @Override
        public boolean test(Activation act, Activation linkedAct) {
            return relation.compare(act.key.range, linkedAct.key.range);
        }


        @Override
        public int compareTo(Relation rel) {
            if(rel instanceof InstanceRelation) return -1;
            RangeRelation rr = (RangeRelation) rel;

            int r = relation.compareTo(rr.relation);
            if(r != 0) return r;

            return INPUT_SYNAPSE_COMP.compare(linkedSynapse, rel.linkedSynapse);
        }
    }

}
