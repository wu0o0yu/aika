package network.aika.neuron.excitatory.patternpart;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.excitatory.pattern.PatternNeuron;

public class PrimarySynapse extends PatternPartSynapse<PatternNeuron> {

    public static byte type;

    public PrimarySynapse() {
    }

    public PrimarySynapse(Neuron input, Neuron output) {
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

    @Override
    public boolean isPropagate() {
        return true;
    }

    public static class Builder extends PatternPartSynapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            PrimarySynapse s = (PrimarySynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output) -> new PrimarySynapse(input, output);
        }
    }
}
