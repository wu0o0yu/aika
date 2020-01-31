package network.aika.neuron.pattern;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.excitatory.ExcitatorySynapse;

public class PositiveRecurrentSynapse extends ExcitatorySynapse<PatternNeuron, PatternPartNeuron> {

    public static byte type;

    public PositiveRecurrentSynapse(Neuron input, Neuron output, boolean propagate, int lastCount) {
        super(input, output, propagate, lastCount);
    }

    @Override
    public byte getType() {
        return type;
    }

    @Override
    public boolean isRecurrent() {
        return true;
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            PositiveRecurrentSynapse s = (PositiveRecurrentSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output) -> new PositiveRecurrentSynapse(input, output, propagate, output.getModel().charCounter);
        }
    }

}
