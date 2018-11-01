package network.aika.neuron.relation;


import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.Writable;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.range.Position;
import network.aika.neuron.range.Range;

import java.io.DataInput;
import java.io.IOException;
import java.util.*;


public abstract class Relation implements Comparable<Relation>, Writable {


    public static Comparator<Relation> COMPARATOR = (r1, r2) -> {
        int r = Integer.compare(r1.getRelationType(), r2.getRelationType());
        if(r != 0) return r;
        return r1.compareTo(r2);
    };


    public abstract int getRelationType();

    public abstract boolean test(Activation act, Activation linkedAct);

    public abstract Relation invert();

    public abstract Range mapRange(Activation act, Linker.Direction input);

    public abstract boolean linksOutputBegin();

    public abstract boolean linksOutputEnd();


    public static Relation read(DataInput in, Model m) throws IOException {
        if(in.readBoolean()) {
            return AncestorRelation.read(in, m);
        } else {
            return RangeRelation.read(in, m);
        }
    }

    public abstract boolean isExact();


    public abstract Collection<Activation> getActivations(INeuron n, Activation linkedAct);


    public boolean follow(Activation rAct, Activation oAct, Map<Integer, Set<Relation>> relations) {
        return true;
    }


    public static void addRelation(Map<Integer, Set<Relation>> relMap, Integer synId, Relation r) {
        Set<Relation> relSet = relMap.get(synId);
        if(relSet == null) {
            relSet = new TreeSet<>(COMPARATOR);
            relMap.put(synId, relSet);
        }
        relSet.add(r);
    }


    public static Map<Integer, Set<Relation>> getRelationsMap(int synapseId, Neuron n) {
        if(synapseId == Synapse.OUTPUT) {
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
            return relation;
        }

        public void connect(Neuron n) {
            Map<Integer, Set<Relation>> fromRel = getRelationsMap(from, n);
            Map<Integer, Set<Relation>> toRel = getRelationsMap(to, n);

            Relation r = getRelation();
            addRelation(fromRel, to, r);
            addRelation(toRel, from, r.invert());
        }

        @Override
        public void registerSynapseIds(Neuron n) {
            n.registerSynapseId(from);
            n.registerSynapseId(to);
        }
    }
}
