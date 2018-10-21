package network.aika.neuron.relation;


import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.Writable;
import network.aika.neuron.range.Range;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;


public abstract class Relation implements Comparable<Relation>, Writable {


    public abstract boolean test(Activation act, Activation linkedAct);

    public abstract Relation invert();


    public static Relation read(DataInput in, Model m) throws IOException {
        if(in.readBoolean()) {
            return AncestorRelation.read(in, m);
        } else {
            return RangeRelation.read(in, m);
        }
    }

    public abstract boolean isExact();


    public abstract Collection<Activation> getActivations(INeuron n, Activation linkedAct);


    public static class Key implements Comparable<Key>, Writable {
        public int group;
        public int synapseId;

        private Key() {
        }

        public Key(int group, int synapseId) {
            this.group = group;
            this.synapseId = synapseId;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeInt(group);
            out.writeInt(synapseId);
        }

        public static Key read(DataInput in, Model m) throws IOException {
            Key rk = new Key();
            rk.readFields(in, m);
            return rk;
        }

        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            group = in.readInt();
            synapseId = in.readInt();
        }

        @Override
        public int compareTo(Key rk) {
            int r = Integer.compare(group, rk.group);
            if(r != 0) return r;
            return Integer.compare(synapseId, rk.synapseId);
        }
    }


    public static class Builder implements Neuron.Builder {
        public static final int DEFAULT_GROUP = -1;

        public int from;
        public int to;

        public int fromGroup = DEFAULT_GROUP;
        public int toGroup = DEFAULT_GROUP;

        public Relation relation;


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

        public Builder setFromGroup(int group) {
            fromGroup = group;
            return this;
        }

        public Builder setToGroup(int group) {
            toGroup = group;
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

        public void connect(Neuron n) {
            Map<Key, Relation> fromRel = getRelationsMap(from, n);
            Map<Key, Relation> toRel = getRelationsMap(to, n);

            fromRel.put(new Key(toGroup, to), relation);
            toRel.put(new Key(fromGroup, from), relation.invert());
        }

        private static Map<Key, Relation> getRelationsMap(int synapseId, Neuron n) {
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

        @Override
        public void registerSynapseIds(Neuron n) {
            n.registerSynapseId(from);
            n.registerSynapseId(to);
        }
    }
}
