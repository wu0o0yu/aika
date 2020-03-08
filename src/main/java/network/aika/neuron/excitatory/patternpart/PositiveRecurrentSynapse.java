package network.aika.neuron.excitatory.patternpart;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.excitatory.pattern.PatternNeuron;

public class PositiveRecurrentSynapse extends PatternPartSynapse<PatternNeuron> {

    public static byte type;

    public PositiveRecurrentSynapse(Neuron input, Neuron output) {
        super(input, output);
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

    @Override
    public void collectLinkingCandidates(Link l, Linker.CollectResults c) {

    }

    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            PositiveRecurrentSynapse s = (PositiveRecurrentSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output) -> new PositiveRecurrentSynapse(input, output);
        }
    }

}
