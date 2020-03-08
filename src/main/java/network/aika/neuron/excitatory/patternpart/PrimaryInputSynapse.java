package network.aika.neuron.excitatory.patternpart;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.excitatory.pattern.PatternNeuron;

public class PrimaryInputSynapse extends PatternPartSynapse<PatternNeuron> {

    public static byte type;

    public PrimaryInputSynapse() {
    }

    public PrimaryInputSynapse(Neuron input, Neuron output) {
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
        return true;
    }

    public static class Builder extends PatternPartSynapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            PrimaryInputSynapse s = (PrimaryInputSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output) -> new PrimaryInputSynapse(input, output);
        }
    }
}
