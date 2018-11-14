package network.aika.neuron.relation;


import network.aika.Model;
import network.aika.Writable;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.range.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.range.Position.Operator;
import static network.aika.neuron.Synapse.OUTPUT;


public abstract class Relation implements Comparable<Relation>, Writable {

    public static Relation EQUALS = createRangeRelation(Operator.EQUALS, Operator.EQUALS);
    public static Relation BEGIN_EQUALS = createRangeRelation(Operator.EQUALS, Operator.NONE);
    public static Relation END_EQUALS = createRangeRelation(Operator.NONE, Position.Operator.EQUALS);
    public static Relation BEGIN_TO_END_EQUALS = createRangeRelation(Position.Operator.NONE, Operator.EQUALS, Operator.NONE , Operator.NONE);
    public static Relation END_TO_BEGIN_EQUALS = createRangeRelation(Operator.NONE, Operator.NONE, Operator.NONE , Operator.EQUALS);
    public static Relation CONTAINS = createRangeRelation(Operator.LESS_THAN_EQUAL, Operator.GREATER_THAN_EQUAL);
    public static Relation CONTAINED_IN = createRangeRelation(Operator.GREATER_THAN_EQUAL, Operator.LESS_THAN_EQUAL);
    public static Relation OVERLAPS = createRangeRelation(Operator.NONE, Operator.LESS_THAN, Operator.NONE, Operator.GREATER_THAN);
    public static Relation NONE = createRangeRelation(Operator.NONE, Operator.NONE);
    public static Relation BETWEEN = createRangeRelation(Operator.GREATER_THAN, Operator.LESS_THAN);
    public static Relation BEFORE = createRangeRelation(Operator.NONE, Operator.NONE, Operator.NONE , Operator.LESS_THAN_EQUAL);
    public static Relation AFTER = createRangeRelation(Operator.NONE, Operator.NONE, Operator.NONE , Operator.GREATER_THAN_EQUAL);



    public static Comparator<Relation> COMPARATOR = (r1, r2) -> {
        int r = Integer.compare(r1.getRelationType(), r2.getRelationType());
        if(r != 0) return r;
        return r1.compareTo(r2);
    };


    public static Relation createRangeRelation(Operator beginToBegin, Operator beginToEnd, Operator endToEnd, Operator endToBegin) {
        List<Relation> rels = new ArrayList<>();

        if(beginToBegin != Operator.NONE) {
            rels.add(new PositionRelation(Activation.BEGIN, Activation.BEGIN, beginToBegin));
        }
        if(beginToEnd != Operator.NONE) {
            rels.add(new PositionRelation(Activation.BEGIN, Activation.END, beginToEnd));
        }
        if(endToEnd != Operator.NONE) {
            rels.add(new PositionRelation(Activation.END, Activation.END, endToEnd));
        }
        if(endToBegin != Operator.NONE) {
            rels.add(new PositionRelation(Activation.END, Activation.BEGIN, endToBegin));
        }

        if(rels.size() == 1) {
            return rels.get(0);
        } else {
            return new MultiRelation(rels);
        }
    }


    public static Relation createRangeRelation(Operator beginToBegin, Operator endToEnd) {
        return createRangeRelation(beginToBegin, Operator.NONE, endToEnd, Operator.NONE);
    }


    public abstract int getRelationType();

    public abstract boolean test(Activation act, Activation linkedAct);

    public abstract Relation invert();

    public abstract void mapRange(Map<Integer, Position> slots, Activation act);

    public abstract void linksOutputs(Set<Integer> outputs);


    public void write(DataOutput out) throws IOException {
        out.writeInt(getRelationType());
    }


    public static Relation read(DataInput in, Model m) throws IOException {
        switch (in.readInt()) {
            case AncestorRelation.RELATION_TYPE:
                return AncestorRelation.read(in, m);
            case PositionRelation.RELATION_TYPE:
                return PositionRelation.read(in, m);
            case MultiRelation.RELATION_TYPE:
                return MultiRelation.read(in, m);
            default:
                return null;
        }
    }

    public abstract boolean isExact();


    public abstract Stream<Activation> getActivations(INeuron n, Activation linkedAct);


    public boolean follow(Activation rAct, Activation oAct, Map<Integer, Relation> relations) {
        return true;
    }


    public static void addRelation(Map<Integer, Relation> relMap, Integer synId, Integer targetSynId, Neuron n, Relation r) {
        if(targetSynId == OUTPUT) {
            Synapse s = n.getSynapseById(synId);
            if(s == null || s.isDisjunction) {
                return;
            }
        }

        relMap.put(synId, r);
    }


    public static Map<Integer, Relation> getRelationsMap(int synapseId, Neuron n) {
        if(synapseId == OUTPUT) {
            INeuron in = n.get();
            if (in.outputRelations == null) {
                in.outputRelations = new TreeMap<>();
            }
            return in.outputRelations;
        } else {
            Synapse s = n.getSynapseById(synapseId);
            return s.relations;
        }
    }

    public abstract void registerRequiredSlots(Neuron input);


    public static class Builder implements Neuron.Builder {
        private int from;
        private int to;

        private Relation relation;


        /**
         * This parameter allows to specify whether the relations connected to thy synapse refer to the activation of the
         * input neuron (that's the default) or if the should refer to one of the input activations of the input neuron.
         * In this case the synapseId of the input neuron needs to be specified which leads to the desired input activation.
         *
         * @param synapseId
         * @return
         */



        public Builder setFrom(int synapseId) {
            assert synapseId >= -1;
            from = synapseId;
            return this;
        }

        public Builder setTo(int synapseId) {
            assert synapseId >= -1;
            to = synapseId;
            return this;
        }

        public Builder setAncestorRelation(AncestorRelation.Type type) {
            this.relation = new AncestorRelation(type);
            return this;
        }

        public Builder setPositionRelation(int fromSlot, int toSlot, Operator operator) {
            this.relation = new PositionRelation(fromSlot, toSlot, operator);
            return this;
        }

        public Builder setRelation(Relation rel) {
            this.relation = rel;
            return this;
        }


        public Relation getRelation() {
            return relation;
        }

        public void connect(Neuron n) {
            Map<Integer, Relation> fromRel = getRelationsMap(from, n);
            Map<Integer, Relation> toRel = getRelationsMap(to, n);

            Relation r = getRelation();
            addRelation(fromRel, to, from, n, r);
            addRelation(toRel, from, to, n, r.invert());
        }

        @Override
        public void registerSynapseIds(Neuron n) {
            n.registerSynapseId(from);
            n.registerSynapseId(to);
        }
    }
}
