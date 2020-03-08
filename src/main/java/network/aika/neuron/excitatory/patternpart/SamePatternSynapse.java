package network.aika.neuron.excitatory.patternpart;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.excitatory.pattern.PatternNeuron;

public class SamePatternSynapse extends PatternPartSynapse<PatternNeuron> {

    public static byte type;

    public SamePatternSynapse() {
    }

    public SamePatternSynapse(Neuron input, Neuron output) {
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
    public void collectLinkingCandidates(Link l, Linker.CollectResults c) {

    }

    @Override
    public boolean isPropagate() {
        return false;
    }

    public static class Builder extends PatternPartSynapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            SamePatternSynapse s = (SamePatternSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected SynapseFactory getSynapseFactory() {
            return (input, output) -> new SamePatternSynapse(input, output);
        }
    }
}
