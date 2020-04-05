package network.aika.neuron.excitatory.pattern;

import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PatternSynapse extends ExcitatorySynapse<PatternPartNeuron, PatternNeuron> {

    public static byte type;

    private boolean propagate;

    public PatternSynapse() {
    }

    public PatternSynapse(Neuron input, Neuron output) {
        super(input, output);
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

    public boolean isPropagate() {
        return propagate;
    }

    public void setPropagate(boolean propagate) {
        this.propagate = propagate;

        if(propagate) {
            input.get().addPropagateTarget(output);
        } else {
            input.get().removePropagateTarget(output);
        }
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
            PatternSynapse s = (PatternSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected SynapseFactory getSynapseFactory() {
            return (input, output) -> new PatternSynapse(input, output);
        }
    }
}
