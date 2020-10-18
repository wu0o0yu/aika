package network.aika.neuron;

import network.aika.Model;
import network.aika.Writable;
import network.aika.neuron.activation.Reference;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Instances implements Writable {

    protected double N = 1;
    protected int lastPos = Integer.MIN_VALUE;

    public double getN() {
        if(!isInitialized()) throw new NotInitializedException();

        return N;
    }

    public void setN(int N) {
        this.N = N;
    }

    public boolean isInitialized() {
        return lastPos != Integer.MIN_VALUE;
    }

    public int getLastPos() {
        return lastPos;
    }

    public void setLastPos(int lastPos) {
        this.lastPos = lastPos;
    }

    public void update(Model m, Reference ref) {
        if(isInitialized()) {
            N += (m.getN() - lastPos) / ref.length();
        }
        lastPos = m.getN();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(N);
        out.writeInt(lastPos);
    }

    public static Instances read(DataInput in, Model m) throws IOException {
        Instances instances = new Instances();
        instances.readFields(in, m);
        return instances;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        N = in.readDouble();
        lastPos = in.readInt();
    }

    public static class NotInitializedException extends RuntimeException {
        public NotInitializedException() {
            super("Not initialized!");
        }
    }
}
