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

import static network.aika.neuron.INeuron.Type.INHIBITORY;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;
import static network.aika.neuron.relation.PositionRelation.Equals;
import static network.aika.neuron.relation.PositionRelation.LessThan;
import static network.aika.neuron.relation.PositionRelation.GreaterThan;


public abstract class Relation implements Comparable<Relation>, Writable {

    public static Map<Integer, RelationFactory> relationRegistry = new TreeMap<>();

    public static Relation EQUALS = new MultiRelation(
            new Equals(BEGIN, BEGIN),
            new Equals(END, END, false, false)
    );
    public static Relation BEGIN_EQUALS = new Equals(BEGIN, BEGIN);
    public static Relation END_EQUALS = new Equals(END, END);
    public static Relation BEGIN_TO_END_EQUALS = new Equals(BEGIN, END);
    public static Relation END_TO_BEGIN_EQUALS = new Equals(END, BEGIN);
    public static Relation CONTAINS = new MultiRelation(
            new LessThan(BEGIN, BEGIN, true),
            new GreaterThan(END, END, true, false, false, Integer.MAX_VALUE)
    );
    public static Relation CONTAINED_IN = new MultiRelation(
            new GreaterThan(BEGIN, BEGIN, true),
            new LessThan(END, END, true, false, false, Integer.MAX_VALUE)
    );
    public static Relation OVERLAPS = new MultiRelation(
            new LessThan(BEGIN, END, false),
            new GreaterThan(END, BEGIN, false, false, false, Integer.MAX_VALUE)
    );
    public static Relation BEFORE = new LessThan(END, BEGIN, true);
    public static Relation AFTER = new GreaterThan(BEGIN, END, true);

    public static Relation ANY = new Any();

    protected boolean optional;
    protected boolean follow = true;


    public boolean isOptional() {
        return optional;
    }

    public boolean isFollow() {
        return follow;
    }

    @Override
    public int compareTo(Relation rel) {
        return Integer.compare(getType(), rel.getType());
    }


    public static void registerRelation(int relationType, RelationFactory rf) {
        relationRegistry.put(relationType, rf);
    }

    public interface RelationFactory {
        Relation create();
    }

    public abstract int getType();

    public abstract boolean test(Activation act, Activation linkedAct);

    public abstract Relation invert();

    public abstract void mapSlots(Map<Integer, Position> slots, Activation act);

    public abstract void linksOutputs(Set<Integer> outputs);

    public abstract Relation setOptionalAndFollow(boolean optional, boolean follow);


    public Relation() {
    }


    public Relation(boolean optional, boolean follow) {
        this.optional = optional;
        this.follow = follow;
    }


    public void write(DataOutput out) throws IOException {
        out.writeInt(getType());
        out.writeBoolean(optional);
        out.writeBoolean(follow);
    }


    public static Relation read(DataInput in, Model m) throws IOException {
        RelationFactory rf = relationRegistry.get(in.readInt());
        Relation rel = rf.create();
        rel.readFields(in, m);
        return rel;
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        optional = in.readBoolean();
        follow = in.readBoolean();
    }


    public abstract boolean isExact();


    public abstract Stream<Activation> getActivations(INeuron n, Activation linkedAct);


    public boolean isConvertible() {
        return !optional && follow;
    }


    public static void link(Neuron n, Relation rel, int from, int to) {
        addRelation(to, from, n, rel);
        addRelation(from, to, n, rel.invert());
    }


    public static void addRelation(Integer synId, Integer targetSynId, Neuron n, Relation rel) {
        addRelation(getRelationsMap(targetSynId, n), synId, targetSynId, n, rel);
    }


    public static void addRelation(Map<Integer, Relation> relMap, Integer synId, Integer targetSynId, Neuron n, Relation r) {
        if(targetSynId == OUTPUT) {
            Synapse s = n.getSynapseById(synId);
            if(s == null || (r.isExact() && n.getType() == INHIBITORY && !s.isInactive())) {
                return;
            }
        }

        Relation existingRel = relMap.get(synId);
        if (existingRel == null) {
            relMap.put(synId, r);
        } else if(existingRel instanceof MultiRelation) {
            MultiRelation mr = (MultiRelation) existingRel;
            mr.addRelation(r);
        } else if(existingRel.compareTo(r) != 0) {
            MultiRelation mr = new MultiRelation();
            mr.addRelation(existingRel);
            mr.addRelation(r);
            relMap.put(synId, mr);
        }
    }


    public static void removeRelation(Integer synId, Integer targetSynId, Neuron n, Relation r) {
        removeRelation(getRelationsMap(targetSynId, n), synId, r);
    }


    public static void removeRelation(Map<Integer, Relation> relMap, Integer synId, Relation r) {
        Relation existingRel = relMap.get(synId);
        if(existingRel == null) {
            return;
        } else if(existingRel.compareTo(r) == 0) {
            relMap.remove(synId);
        } else if(existingRel instanceof MultiRelation) {
            MultiRelation mr = (MultiRelation) existingRel;
            mr.removeRelation(r);

            if(mr.size() == 1) {
                relMap.put(synId, mr.getRelations().first());
            }
        }
    }


    public static Map<Integer, Relation> getRelationsMap(int synapseId, Neuron n) {
        if(synapseId == OUTPUT) {
            return n.get().getOutputRelations();
        } else {
            Synapse s = n.getSynapseById(synapseId);
            return s.getRelations();
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
            link(n, getRelation(), from, to);
        }

        @Override
        public void registerSynapseIds(Neuron n) {
            n.registerSynapseId(from);
            n.registerSynapseId(to);
        }
    }


    public static class Any extends Relation {
        public static int ID = 100;

        static {
            registerRelation(ID, () -> Relation.ANY);
        }

        public Any() {
        }

        public Any(boolean optional, boolean follow) {
            super(optional, follow);
        }

        @Override
        public int getType() {
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
        public void mapSlots(Map<Integer, Position> slots, Activation act) {
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
            return n.getActivations(linkedAct.getDocument());
        }

        @Override
        public boolean isConvertible() {
            return true;
        }

        @Override
        public Relation setOptionalAndFollow(boolean optional, boolean follow) {
            return new Any(optional, follow);
        }

        @Override
        public void registerRequiredSlots(Neuron input) {
        }
    }
}
