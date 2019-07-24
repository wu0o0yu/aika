package network.aika.lattice.refinement;

import network.aika.Model;
import network.aika.Writable;
import network.aika.neuron.relation.Relation;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;



public class RelationsMap implements Comparable<RelationsMap>, Writable {

    public static final RelationsMap MIN = new RelationsMap();
    public static final RelationsMap MAX = new RelationsMap();

    public Relation[] relations;


    public RelationsMap() {}


    public RelationsMap(Relation[] relations) {
        this.relations = relations;
    }


    public void write(DataOutput out) throws IOException {
        out.writeInt(relations.length);
        for(int i = 0; i < relations.length; i++) {
            Relation rel = relations[i];
            out.writeBoolean(rel != null);
            if(rel != null) {
                rel.write(out);
            }
        }
    }


    public void readFields(DataInput in, Model m) throws IOException {
        int l = in.readInt();
        relations = new Relation[l];
        for(int i = 0; i < l; i++) {
            if(in.readBoolean()) {
                relations[i] = Relation.read(in, m);
            }
        }
    }


    public static RelationsMap read(DataInput in, Model m) throws IOException {
        RelationsMap k = new RelationsMap();
        k.readFields(in, m);
        return k;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < relations.length; i++) {
            Relation rel = relations[i];
            if(rel != null) {
                sb.append(i + ":" + rel + ", ");
            }
        }
        return sb.toString();
    }


    @Override
    public int compareTo(RelationsMap rm) {
        if (this == MIN) return -1;
        if (rm == MIN) return 1;
        if (this == MAX) return 1;
        if (rm == MAX) return -1;

        int r = Integer.compare(relations.length, rm.relations.length);
        if(r != 0) return r;

        for(int i = 0; i < relations.length; i++) {
            Relation ra = relations[i];
            Relation rb = rm.relations[i];

            if(ra == null && rb == null) continue;
            if(ra == null && rb != null) return -1;
            if(ra != null && rb == null) return 1;

            r = ra.compareTo(rb);
            if(r != 0) return r;
        }
        return 0;
    }


    public int length() {
        return relations.length;
    }


    public Relation get(int i) {
        return relations[i];
    }


    public int size() {
        if(relations.length == 0) return 0;
        int count = 0;
        for(int i = 0; i < relations.length; i++) {
            if(relations[i] != null) count++;
        }
        return count;
    }


    public boolean isExact() {
        for(Relation rel: relations) {
            if(!rel.isExact()) {
                return false;
            }
        }
        return true;
    }
}

