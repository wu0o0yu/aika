package org.aika.training;


import org.aika.Model;
import org.aika.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NeuronStatistic implements Writable {

    public volatile int frequency;
    public volatile int nOffset;


    public NeuronStatistic(int nOffset) {
        this.nOffset = nOffset;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(frequency);
        out.writeInt(nOffset);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        frequency = in.readInt();
        nOffset = in.readInt();
    }
}
