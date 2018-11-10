package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class RelationsSet implements Writable {

    public static final Comparator<RelationsSet> COMPARATOR = (rsa, rsb) -> {
        return 0;
    };

    public Set<Relation> relations = new TreeSet<>();


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(relations.size());
        for(Relation r: relations) {
            r.write(out);
        }
    }


    public static RelationsSet read(DataInput in, Model m) throws IOException {
        RelationsSet rs = new RelationsSet();
        rs.readFields(in, m);
        return rs;
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        int l = in.readInt();
        for(int i = 0; i < l; i++) {
            relations.add(Relation.read(in, m));
        }
    }

    public boolean isExact() {
        return false;
    }

    public RelationsSet invert() {
        return null;
    }
}
