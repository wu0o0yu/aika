package network.aika.neuron.relation;


import network.aika.Model;
import network.aika.Writable;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;
import static network.aika.neuron.relation.PositionRelation.Equals;
import static network.aika.neuron.relation.PositionRelation.LessThan;
import static network.aika.neuron.relation.PositionRelation.GreaterThan;


public abstract class Relation implements Comparable<Relation>, Writable {


    public static Map<Integer, RelationFactory> relationRegistry = new TreeMap<>();

    public static Relation EQUALS = new MultiRelation(new Equals(BEGIN, BEGIN), new Equals(END, END));
    public static Relation BEGIN_EQUALS = new Equals(BEGIN, BEGIN);
    public static Relation END_EQUALS = new Equals(END, END);
    public static Relation BEGIN_TO_END_EQUALS = new Equals(BEGIN, END);
    public static Relation END_TO_BEGIN_EQUALS = new Equals(END, BEGIN);
    public static Relation CONTAINS = new MultiRelation(new LessThan(BEGIN, BEGIN, true), new GreaterThan(END, END, true));
    public static Relation CONTAINED_IN = new MultiRelation(new GreaterThan(BEGIN, BEGIN, true), new LessThan(END, END, true));
    public static Relation OVERLAPS = new MultiRelation(new LessThan(BEGIN, END, false), new GreaterThan(END, BEGIN, false));
    public static Relation BEFORE = new LessThan(END, BEGIN, true);
    public static Relation AFTER = new GreaterThan(BEGIN, END, true);

    public static Relation NONE = new None();


    public static Comparator<Relation> COMPARATOR = (r1, r2) -> {
        int r = Integer.compare(r1.getId(), r2.getId());
        if(r != 0) return r;
        return r1.compareTo(r2);
    };


    @Override
    public int compareTo(Relation rel) {
        return Integer.compare(getId(), rel.getId());
    }



    public static void registerRelation(int relationId, RelationFactory rf) {
        relationRegistry.put(relationId, rf);
    }

    public interface RelationFactory {
        Relation create();
    }

    public abstract int getId();

    public abstract boolean test(Activation act, Activation linkedAct);

    public abstract Relation invert();

    public abstract void mapRange(Map<Integer, Position> slots, Activation act);

    public abstract void linksOutputs(Set<Integer> outputs);


    public void write(DataOutput out) throws IOException {
        out.writeInt(getId());
    }


    public static Relation read(DataInput in, Model m) throws IOException {
        RelationFactory rf = relationRegistry.get(in.readInt());
        Relation rel = rf.create();
        rel.readFields(in, m);
        return rel;
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
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

            Relation rel = getRelation();
            if(from != to) {
                addRelation(fromRel, to, from, n, rel);
                addRelation(toRel, from, to, n, rel.invert());
            } else {
                MultiRelation mr = new MultiRelation(Arrays.asList(rel, rel.invert()));
                addRelation(fromRel, to, from, n, mr);
            }
        }

        @Override
        public void registerSynapseIds(Neuron n) {
            n.registerSynapseId(from);
            n.registerSynapseId(to);
        }
    }


    public static class None extends Relation {
        public static int ID = 100;

        static {
            registerRelation(ID, () -> Relation.NONE);
        }

        @Override
        public int getId() {
            return ID;
        }

        @Override
        public boolean test(Activation act, Activation linkedAct) {
            return true;
        }

        @Override
        public Relation invert() {
            return this;
        }

        @Override
        public void mapRange(Map<Integer, Position> slots, Activation act) {
        }

        @Override
        public void linksOutputs(Set<Integer> outputs) {
        }

        @Override
        public boolean isExact() {
            return false;
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
            INeuron.ThreadState th = n.getThreadState(linkedAct.doc.threadId, false);

            if(th == null || th.isEmpty()) {
                return Stream.empty();
            }
            return th.getActivations();
        }

        @Override
        public void registerRequiredSlots(Neuron input) {
        }
    }
}
