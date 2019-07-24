package network.aika.lattice.refinement;

import network.aika.Model;
import network.aika.Provider;
import network.aika.Writable;
import network.aika.lattice.InputNode;
import network.aika.neuron.relation.Relation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


public class Refinement implements Comparable<Refinement>, Writable {

    public RelationsMap relations;
    public Provider<InputNode> input;

    private Refinement() {}


    public Refinement(RelationsMap relations, Provider<InputNode> input) {
        this.relations = relations;
        this.input = input;
    }

    public boolean isConvertible() {
        for(Relation rel: relations.relations) {
            if(rel != null) return true;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(relations);
        sb.append(input.get().logicToString());
        sb.append(")");
        return sb.toString();
    }


    public void write(DataOutput out) throws IOException {
        relations.write(out);
        out.writeInt(input.getId());
    }


    public void readFields(DataInput in, Model m) throws IOException {
        relations = RelationsMap.read(in, m);
        input = m.lookupNodeProvider(in.readInt());
    }


    public static Refinement read(DataInput in, Model m) throws IOException {
        Refinement k = new Refinement();
        k.readFields(in, m);
        return k;
    }


    @Override
    public int compareTo(Refinement ref) {
        int r = input.compareTo(ref.input);
        if(r != 0) return r;

        return relations.compareTo(ref.relations);
    }

    public boolean contains(Refinement ref, RefValue rv) {
        for(int i = 0; i < ref.relations.length(); i++) {
            Relation ra = ref.relations.get(i);
            Relation rb = relations.get(rv.offsets[i]);

            if((ra == null && rb != null) || (ra != null && rb == null)) return false;

            if(ra != null && rb != null && ra.compareTo(rb) != 0) {
                return false;
            }
        }

        return true;
    }
}
