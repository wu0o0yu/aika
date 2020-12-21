package network.aika.neuron;

import network.aika.Model;
import network.aika.Writable;
import network.aika.neuron.activation.Reference;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SampleSpace implements Writable {

    private double N = 0;
    private Integer lastPos;
    private Reference lastRef;

    public double getN() {
        return N;
    }

    public void setN(int N) {
        this.N = N;
    }

    public int getLastPos() {
        return lastPos;
    }

    public void update(Model m, Reference ref) {
        int n = 0;

        if(lastPos != null) {
            n = getAbsoluteBegin(m, ref) - lastPos;
        }
        assert n >= 0;

        N += 1 + n / ref.length();

        Integer newPos = getAbsoluteEnd(m, ref);
        assert lastPos == null || newPos > lastPos;

        lastPos = newPos;
        lastRef = ref;
    }

    public int getAbsoluteBegin(Model m, Reference ref) {
        return m.getN() + (ref != null ? ref.getBegin() : 0);
    }

    public int getAbsoluteEnd(Model m, Reference ref) {
        return m.getN() + (ref != null ? ref.getEnd() : 0);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(N);
        out.writeBoolean(lastPos != null);
        if(lastPos != null) {
            out.writeInt(lastPos);
        }
    }

    public static SampleSpace read(DataInput in, Model m) throws IOException {
        SampleSpace sampleSpace = new SampleSpace();
        sampleSpace.readFields(in, m);
        return sampleSpace;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        N = in.readDouble();
        if(in.readBoolean()) {
            lastPos = in.readInt();
        }
    }

    public String toString() {
        return "N:" + N + " lastPos:" + lastPos;
    }
}
