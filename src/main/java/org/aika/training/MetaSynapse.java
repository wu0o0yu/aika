package org.aika.training;


import org.aika.Model;
import org.aika.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MetaSynapse implements Writable {

    public double metaWeight;
    public double metaBias;
    public boolean metaRelativeRid;


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(metaWeight);
        out.writeDouble(metaBias);
        out.writeBoolean(metaRelativeRid);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        metaWeight = in.readDouble();
        metaBias = in.readDouble();
        metaRelativeRid = in.readBoolean();
    }
}
