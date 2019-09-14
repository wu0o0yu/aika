/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Relation implements Writable {

    public static final Relation MIN = new Any();
    public static final Relation MAX = new Any();


    public static Map<Integer, RelationFactory> relationRegistry = new TreeMap<>();

    public static Relation[] EQUALS = new Relation[] {
            new Equals(BEGIN, BEGIN),
            new Equals(END, END)
    };
    public static Relation BEGIN_EQUALS = new Equals(BEGIN, BEGIN);
    public static Relation END_EQUALS = new Equals(END, END);
    public static Relation BEGIN_TO_END_EQUALS = new Equals(BEGIN, END);
    public static Relation END_TO_BEGIN_EQUALS = new Equals(END, BEGIN);
    public static Relation[] CONTAINS = new Relation[] {
            new LessThan(BEGIN, BEGIN, true),
            new GreaterThan(END, END, true, Integer.MAX_VALUE)
    };
    public static Relation[] CONTAINED_IN = new Relation[]{
            new GreaterThan(BEGIN, BEGIN, true),
            new LessThan(END, END, true, Integer.MAX_VALUE)
    };
    public static Relation[] OVERLAPS = new Relation[] {
            new LessThan(BEGIN, END, false),
            new GreaterThan(END, BEGIN, false, Integer.MAX_VALUE)
    };
    public static Relation BEFORE = new LessThan(END, BEGIN, true);
    public static Relation AFTER = new GreaterThan(BEGIN, END, true);

    public static Relation ANY = new Any();


    public int compareTo(Relation rel, Direction dir) {
        return Integer.compare(getType(), rel.getType());
    }


    public static void registerRelation(int relationType, RelationFactory rf) {
        relationRegistry.put(relationType, rf);
    }

    public interface RelationFactory {
        Relation create();
    }

    public abstract int getType();

    public abstract boolean test(Activation act, Activation linkedAct, boolean allowUndefined, Direction dir);

    public abstract void mapSlots(Map<Integer, Position> slots, Activation act, Direction dir);


    public Relation() {
    }

    public void setFromSynapseId(int fromSynapseId) {
    }

    public void setToSynapseId(int toSynapseId) {
    }


    public void write(DataOutput out) throws IOException {
        out.writeInt(getType());
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


    public abstract Stream<Activation> getActivations(INeuron n, Activation linkedAct, Direction dir);


    public void link(Neuron n, int from, int to) {
        link(
                getRelationsMap(from, n),
                getRelationsMap(to, n),
                from,
                to,
                n
        );
    }


    public void link(RelationEndpoint fromEndpoint, RelationEndpoint toEndpoint, Integer fromSynId, Integer toSynId, Neuron n) {
        MultiRelation mr;
        if(toSynId != OUTPUT) {
            mr = toRelMap.get(fromSynId);
        } else {
            mr = fromRelMap.get(toSynId);
            if(mr != null) {
                mr = mr.invert();
            }
        }

        if (mr == null) {
            boolean ior = isInhibitoryOutputRelation(n);

            mr = new MultiRelation();
            if(!ior || fromSynId != OUTPUT) {
                fromRelMap.put(toSynId, mr);
            }
            if(!ior || toSynId != OUTPUT) {
                toRelMap.put(fromSynId, mr.invert());
            }
        }

        mr.addRelation(this);
    }


    private boolean isInhibitoryOutputRelation(Neuron n) {
        return isExact() && n.getType() == INHIBITORY;
    }


    public static void removeRelation(Integer synId, Integer targetSynId, Neuron n, Relation r) {
        removeRelation(getRelationsMap(targetSynId, n), synId, r);
    }


    public static void removeRelation(Map<Integer, MultiRelation> relMap, Integer synId, Relation r) {
        Relation existingRel = relMap.get(synId);
        if(existingRel == null) {
            return;
        } else if(existingRel instanceof MultiRelation) {
            MultiRelation mr = (MultiRelation) existingRel;
            mr.removeRelation(r);
        }
    }


    public static Map<Integer, MultiRelation> getRelationsMap(int synapseId, Neuron n) {
        if(synapseId == OUTPUT) {
            return n.get().getOutputRelations();
        } else {
            Synapse s = n.getSynapseById(synapseId);
            return s.getRelations();
        }
    }


    public static class Builder implements Neuron.Builder {
        protected int from;
        protected int to;

        protected Relation[] relation;


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


        public Builder setRelation(Relation... rel) {
            this.relation = rel;
            return this;
        }


        public Relation[] getRelation() {
            return relation;
        }

        public void connect(Neuron n) {
            getRelation().link(n, from, to);
        }

        @Override
        public void registerSynapseIds(Neuron n) {
            n.registerSynapseId(from);
            n.registerSynapseId(to);

            relation.setFromSynapseId(from);
            relation.setToSynapseId(to);
        }
    }




    public static class Key implements Comparable<Key> {
        private Integer relId;
        private Relation rel;
        private Direction dir;

        public Key(Integer relId, Relation rel, Direction dir) {
            this.relId = relId;
            this.rel = rel;
            this.dir = dir;
        }


        public Integer getRelatedId() {
            return relId;
        }

        public Relation getRelation() {
            return rel;
        }

        public Direction getDirection() {
            return dir;
        }

        @Override
        public int compareTo(Key k) {
            int r = Integer.compare(relId, k.relId);
            if(r != 0) return r;

            return 0;
        }

        public Direction getInvertedDirection() {
            return null;
        }
    }


    public static class Any extends Relation {
        public static int ID = 100;

        static {
            registerRelation(ID, () -> Relation.ANY);
        }

        public Any() {
        }

        @Override
        public int getType() {
            return ID;
        }

        @Override
        public boolean test(Activation act, Activation linkedAct, boolean allowUndefined, Direction dir) {
            return true;
        }

        @Override
        public void mapSlots(Map<Integer, Position> slots, Activation act, Direction dir) {
        }

        @Override
        public boolean isExact() {
            return false;
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Activation linkedAct, Direction dir) {
            return n.getActivations(linkedAct.getDocument());
        }
    }
}
