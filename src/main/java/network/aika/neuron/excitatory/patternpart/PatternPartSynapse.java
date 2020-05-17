package network.aika.neuron.excitatory.patternpart;

import network.aika.neuron.NeuronProvider;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.Neuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;

public class PatternPartSynapse<I extends Neuron> extends ExcitatorySynapse<I, PatternPartNeuron> {

    public static byte type;

    private PatternScope patternScope;
    private boolean isRecurrent;
    private boolean isNegative;

    public PatternPartSynapse() {
    }

    public PatternPartSynapse(NeuronProvider input, NeuronProvider output) {
        super(input, output);
    }



    @Override
    public byte getType() {
        return type;
    }

    public static class Builder extends Synapse.Builder {
        public Synapse createSynapse(NeuronProvider outputNeuron) {
            return new PatternPartSynapse(inputNeuron, outputNeuron);
        }
    }
}
