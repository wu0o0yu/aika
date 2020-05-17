package network.aika.neuron.excitatory.pattern;

import network.aika.Model;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


public class PatternSynapse extends ExcitatorySynapse<PatternPartNeuron, PatternNeuron> {

    public static byte type;

    public PatternSynapse() {
    }

    public PatternSynapse(NeuronProvider input, NeuronProvider output) {
        super(input, output);
    }


    @Override
    public byte getType() {
        return type;
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

        public Synapse createSynapse(NeuronProvider outputNeuron) {
            return new PatternSynapse(inputNeuron, outputNeuron);
        }
    }
}
