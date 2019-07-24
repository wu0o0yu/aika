package network.aika.lattice.refinement;

import network.aika.Model;
import network.aika.Provider;
import network.aika.Writable;
import network.aika.lattice.AndNode;
import network.aika.lattice.Node;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


public class RefValue implements Writable {
    public Integer[] offsets;  // input offsets -> output offsets
    public Integer[] reverseOffsets;  // output offsets -> input offsets
    public int refOffset;
    public Provider<? extends Node> parent;
    public Provider<AndNode> child;


    private RefValue() {}


    public RefValue(Integer[] offsets, int refOffset, Provider<? extends Node> parent) {
        this.offsets = offsets;
        reverseOffsets = new Integer[offsets.length + 1];
        for(int i = 0; i < offsets.length; i++) {
            reverseOffsets[offsets[i]] = i;
        }

        this.refOffset = refOffset;
        this.parent = parent;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(offsets.length);
        for(int i = 0; i < offsets.length; i++) {
            Integer ofs = offsets[i];
            out.writeBoolean(ofs != null);
            out.writeInt(ofs);
        }
        out.writeInt(refOffset);
        out.writeInt(parent.getId());
        out.writeInt(child.getId());
    }


    public static RefValue read(DataInput in, Model m)  throws IOException {
        RefValue rv = new RefValue();
        rv.readFields(in, m);
        return rv;
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        int l = in.readInt();
        offsets = new Integer[l];
        reverseOffsets = new Integer[l + 1];
        for(int i = 0; i < l; i++) {
            if(in.readBoolean()) {
                Integer ofs = in.readInt();
                offsets[i] = ofs;
                reverseOffsets[ofs] = i;
            }
        }
        refOffset = in.readInt();
        parent = m.lookupNodeProvider(in.readInt());
        child = m.lookupNodeProvider(in.readInt());
    }
}
