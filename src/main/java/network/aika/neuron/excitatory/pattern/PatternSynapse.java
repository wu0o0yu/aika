package network.aika.neuron.excitatory.pattern;

import network.aika.neuron.NeuronProvider;
import network.aika.neuron.Synapse;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;

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

    public static class Builder extends Synapse.Builder {

        public Synapse createSynapse(NeuronProvider outputNeuron) {
            return new PatternSynapse(inputNeuron, outputNeuron);
        }
    }
}
