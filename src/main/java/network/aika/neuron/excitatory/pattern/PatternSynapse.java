package network.aika.neuron.excitatory.pattern;

import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.PatternScope.SAME_PATTERN;

public class PatternSynapse extends ExcitatorySynapse<PatternPartNeuron, PatternNeuron> {

    public static byte type;

    public PatternSynapse() {
    }

    public PatternSynapse(Neuron input, Neuron output, boolean propagate) {
        super(input, output);
        this.propagate = propagate;
    }

    @Override
    public void init(PatternScope patternScope, Boolean isRecurrent, Boolean isNegative, boolean propagate) {
        this.propagate = propagate;
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public boolean isRecurrent() {
        return false;
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PatternScope getPatternScope() {
        return SAME_PATTERN;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(propagate);

        super.write(out);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        propagate = in.readBoolean();

        super.readFields(in, m);
    }

    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            return new PatternSynapse(inputNeuron, outputNeuron, propagate);
        }
    }
}
