package network.aika.neuron.pattern;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;

public class PatternPartSynapse<I extends TNeuron> extends ExcitatorySynapse<I, PatternPartNeuron> {

    public static byte type;

    public PatternPartSynapse() {
    }

    public PatternPartSynapse(Neuron input, Neuron output, boolean propagate) {
        super(input, output, propagate);
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

    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            PatternPartSynapse s = (PatternPartSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output) -> new PatternPartSynapse(input, output, propagate);
        }
    }
}
