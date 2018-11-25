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


public class MultiRelation extends Relation {
    public static final int ID = 1;

    private List<Relation> relations;


    static {
        registerRelation(ID, () -> new MultiRelation());
    }


    public MultiRelation() {
        relations = new ArrayList<>();
    }


    public MultiRelation(Relation... rels) {
        relations = Arrays.asList(rels);
    }


    public MultiRelation(List<Relation> rels) {
        relations = rels;
    }


    public List<Relation> getRelations() {
        return relations;
    }


    @Override
    public int getType() {
        return ID;
    }


    @Override
    public boolean test(Activation act, Activation linkedAct) {
        for(Relation rel: relations) {
            if(!rel.test(act, linkedAct)) {
                return false;
            }
        }

        return true;
    }


    @Override
    public Relation invert() {
        List<Relation> invRels = new ArrayList<>();
        for(Relation rel: relations) {
            invRels.add(rel.invert());
        }
        return new MultiRelation(invRels);
    }


    @Override
    public void mapSlots(Map<Integer, Position> slots, Activation act) {
        for(Relation rel: relations) {
            rel.mapSlots(slots, act);
        }
    }


    @Override
    public void linksOutputs(Set<Integer> results) {
        for(Relation rel: relations) {
            rel.linksOutputs(results);
        }
    }


    @Override
    public boolean isExact() {
        for(Relation rel: relations) {
            if(rel.isExact()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
        if(relations.isEmpty()) {
            INeuron.ThreadState th = n.getThreadState(linkedAct.doc.threadId, false);
            return th != null ? th.getActivations() : Stream.empty();
        } else {
            Relation firstRelation = relations.get(0);
            return firstRelation
                    .getActivations(n, linkedAct)
                    .filter(act -> {
                        for(Relation rel: relations) {
                            if(!rel.test(act, linkedAct)) {
                                return false;
                            }
                        }
                        return true;
                    });
        }
    }

    @Override
    public boolean convertible() {
        for(Relation rel: relations) {
            if(rel.convertible()) return true;
        }

        return false;
    }


    @Override
    public void registerRequiredSlots(Neuron input) {
        for(Relation rel: relations) {
            rel.registerRequiredSlots(input);
        }
    }


    @Override
    public int compareTo(Relation rel) {
        MultiRelation mr = (MultiRelation) rel;
        int r = Integer.compare(relations.size(), mr.relations.size());
        if(r != 0) return r;
        for(int i = 0; i < relations.size(); i++) {
            Relation a = relations.get(i);
            Relation b = mr.relations.get(i);
            r = a.compareTo(b);
            if(r != 0) return r;
        }
        return 0;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeInt(relations.size());
        for(Relation rel: relations) {
            rel.write(out);
        }
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        int l = in.readInt();
        for(int i = 0; i < l; i++) {
            relations.add(Relation.read(in, m));
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MR(");
        boolean first = true;
        for(Relation rel: relations) {
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
