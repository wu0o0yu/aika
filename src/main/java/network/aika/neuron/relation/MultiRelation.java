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
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
public class MultiRelation extends Relation {
    public static final int ID = 1;

    private SortedMap<Relation, Relation> relations;

    private MultiRelation inverted;

    static {
        registerRelation(ID, () -> new MultiRelation());
    }


    private MultiRelation(MultiRelation im) {
        inverted = im;
        relations = new TreeMap<>();
    }


    public MultiRelation() {
        inverted = new MultiRelation(this);
        relations = new TreeMap<>();
    }


    public MultiRelation(Relation... rels) {
        this();

        for(Relation r: rels) {
            relations.put(r, r);
            Relation ir = r.invert();
            inverted.relations.put(ir, ir);
        }
    }


    public MultiRelation(SortedMap<Relation, Relation> rels) {
        relations = rels;
        inverted = new MultiRelation(this);

        for(Relation r: rels.values()) {
            Relation ir = r.invert();
            inverted.relations.put(ir, ir);
        }
    }

    public SortedMap<Relation, Relation> getRelations() {
        return relations;
    }

    public Collection<Relation> getLeafRelations() {
        return relations.values();
    }

    public int size() {
        return relations.size();
    }


    public Relation getRelation(Relation key) {
        return relations.get(key);
    }


    public void addRelation(Relation r) {
        relations.put(r, r);
        inverted.relations.put(r, r);
    }


    public void removeRelation(Relation r) {
        relations.remove(r);
    }


    @Override
    public int getType() {
        return ID;
    }


    @Override
    public boolean test(Activation act, Activation linkedAct, boolean allowUndefined) {
        for (Relation rel : relations.values()) {
            if (!rel.test(act, linkedAct, allowUndefined)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public MultiRelation invert() {
        return inverted;
    }


    @Override
    public void mapSlots(Map<Integer, Position> slots, Activation act) {
        for(Relation rel: relations.values()) {
            rel.mapSlots(slots, act);
        }
    }


    @Override
    public boolean isExact() {
        for(Relation rel: relations.values()) {
            if(rel.isExact()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
        if(relations.isEmpty()) {
            return n.getActivations(linkedAct.getDocument());
        } else {
            return relations.firstKey()
                    .getActivations(n, linkedAct)
                    .filter(act -> {
                        for (Relation rel : relations.values()) {
                            if (!rel.test(act, linkedAct, false)) {
                                return false;
                            }
                        }
                        return true;
                    });
        }
    }


    @Override
    public int compareTo(Relation rel) {
        int r = super.compareTo(rel);
        if(r != 0) return r;

        MultiRelation mr = (MultiRelation) rel;
        r = Integer.compare(relations.size(), mr.relations.size());
        if(r != 0) return r;

        Iterator<Relation> ita = relations.values().iterator();
        Iterator<Relation> itb = mr.relations.values().iterator();

        while(ita.hasNext() || itb.hasNext()) {
            Relation a = ita.next();
            Relation b = itb.next();
            r = a.compareTo(b);
            if(r != 0) return r;
        }
        return 0;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(relations.size());
        for(Relation rel: relations.values()) {
            rel.write(out);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);
        int l = in.readInt();
        for(int i = 0; i < l; i++) {
            Relation r = Relation.read(in, m);
            relations.put(r, r);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MULTI(");
        boolean first = true;
        for(Relation rel: relations.values()) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(rel.toString());
        }
        sb.append(")");
        return sb.toString();
    }

}
