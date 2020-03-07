package network.aika.neuron.excitatory.patternpart;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;

public abstract class PatternPartSynapse<I extends TNeuron> extends ExcitatorySynapse<I, PatternPartNeuron> {

    public static byte type;

    public PatternPartSynapse() {
    }

    public PatternPartSynapse(Neuron input, Neuron output) {
        super(input, output);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public boolean isPropagate() {
        return false;
    }

    @Override
    public boolean isRecurrent() {
        return false;
    }

    @Override
    public boolean isNegative() {
        return false;
    }
}
