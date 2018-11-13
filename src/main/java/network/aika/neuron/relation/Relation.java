package network.aika.neuron.relation;


import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.Writable;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.range.Range;

import java.io.DataInput;
import java.io.IOException;
import java.util.*;

import static network.aika.neuron.Synapse.OUTPUT;


public abstract class Relation implements Comparable<Relation>, Writable {


    public static Comparator<Relation> COMPARATOR = (r1, r2) -> {
        int r = Integer.compare(r1.getRelationType(), r2.getRelationType());
        if(r != 0) return r;
        return r1.compareTo(r2);
    };


    public abstract int getRelationType();

    public abstract boolean test(Activation act, Activation linkedAct);

    public abstract Relation invert();

    public abstract Range mapRange(Activation act, Linker.Direction direction);

    public abstract boolean linksOutputBegin();

    public abstract boolean linksOutputEnd();


    public static Relation read(DataInput in, Model m) throws IOException {
        switch(in.readInt()) {
            case AncestorRelation.RELATION_TYPE:
                return AncestorRelation.read(in, m);
            case RangeRelation.RELATION_TYPE:
                return RangeRelation.read(in, m);
            case InputRelation.RELATION_TYPE:
                return InputRelation.read(in, m);
            default:
                return null;
        }
    }

    public abstract boolean isExact();


    public abstract Collection<Activation> getActivations(INeuron n, Activation linkedAct);


    public boolean follow(Activation rAct, Activation oAct, Map<Integer, Set<Relation>> relations) {
        return true;
    }


    public static void addRelation(Map<Integer, Set<Relation>> relMap, Integer synId, Integer targetSynId, Neuron n, Relation r) {
        if(targetSynId == OUTPUT) {
            Synapse s = n.getSynapseById(synId);
            if(s == null || s.isDisjunction) {
                return;
            }
        }

        Set<Relation> relSet = relMap.get(synId);
        if(relSet == null) {
            relSet = new TreeSet<>(COMPARATOR);
            relMap.put(synId, relSet);
        }
        relSet.add(r);
    }


    public static Map<Integer, Set<Relation>> getRelationsMap(int synapseId, Neuron n) {
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


    public static class Builder implements Neuron.Builder {
        private int from;
        private int to;
        private int fromInput = OUTPUT;
        private int toInput = OUTPUT;

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

        public Builder setFromInput(int synapseId) {
            assert synapseId >= -2;
            fromInput = synapseId;
            return this;
        }

        public Builder setToInput(int synapseId) {
            assert synapseId >= -2;
            toInput = synapseId;
            return this;
        }

        public Builder setAncestorRelation(AncestorRelation.Type type) {
            relation = new AncestorRelation(type);
            return this;
        }

        public Builder setRangeRelation(Range.Relation rr) {
            relation = new RangeRelation(rr);
            return this;
        }

        public Builder setRelation(Relation rel) {
            relation = rel;
            return this;
        }

        public Relation getRelation() {
            if(fromInput != OUTPUT || toInput != OUTPUT) {
                return new InputRelation(relation, fromInput, toInput);
            }
            return relation;
        }

        public void connect(Neuron n) {
            Map<Integer, Set<Relation>> fromRel = getRelationsMap(from, n);
            Map<Integer, Set<Relation>> toRel = getRelationsMap(to, n);

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
