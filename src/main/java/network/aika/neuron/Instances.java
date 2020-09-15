package network.aika.neuron;

import network.aika.Model;
import network.aika.Writable;
import network.aika.neuron.activation.Reference;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Instances implements Writable {

    protected int offset = Integer.MAX_VALUE;
    protected int currentPos = Integer.MIN_VALUE;

    protected int coveredFactorSum;
    protected int count;

    private double getCoveredFactor() {
        if(coveredFactorSum == 0 || count == 0) return 1.0;
        return coveredFactorSum / (double) count;
    }

    public double getN() {
        double n = (currentPos - offset) / getCoveredFactor();
        assert n >= 0;
        return n;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void update(Model m, Reference ref) {
        offset = Math.min(offset, m.getN() + ref.getBegin());
        currentPos = Math.max(currentPos, m.getN() + ref.getEnd());
        coveredFactorSum += ref.length();
        count++;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(offset);
        out.writeInt(currentPos);
        out.writeInt(count);
        out.writeInt(coveredFactorSum);
    }

    public static Instances read(DataInput in, Model m) throws IOException {
        Instances instances = new Instances();
        instances.readFields(in, m);
        return instances;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        offset = in.readInt();
        currentPos = in.readInt();
        count = in.readInt();
        coveredFactorSum = in.readInt();
    }
}
