package network.aika.neuron.pattern;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.excitatory.ExcitatorySynapse;

public class PatternSynapse extends ExcitatorySynapse<PatternPartNeuron, PatternNeuron> {

    public static byte type;

    public PatternSynapse() {
    }

    public PatternSynapse(Neuron input, Neuron output, boolean propagate) {
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
            PatternSynapse s = (PatternSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected SynapseFactory getSynapseFactory() {
            return (input, output) -> new PatternSynapse(input, output, propagate);
        }
    }
}
