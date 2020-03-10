package network.aika.neuron.excitatory.patternpart;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.excitatory.pattern.PatternNeuron;

public class SecondaryInputSynapse extends PatternPartSynapse<PatternNeuron> {

    public static byte type;

    public SecondaryInputSynapse() {
    }

    public SecondaryInputSynapse(Neuron input, Neuron output) {
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
    public void collectLinkingCandidates(Link downLink, Linker.CollectResults c) {
        downLink.getInput().inputLinks
                .values()
                .stream()
                .filter(l -> l.getSynapse() instanceof SecondaryInputSynapse)  // Gibt es da eine bessere Lösung?
                .flatMap(l -> l.getInput().inputLinks.values().stream())
                .filter(l -> l.getSynapse() instanceof SecondaryInputSynapse)
                .flatMap(l -> l.getInput().outputLinks.values().stream())
                .filter(l -> l.getSynapse() instanceof PrimaryInputSynapse)
                .forEach(l -> c.collect(l.getOutput()));
    }

    @Override
    public boolean isPropagate() {
        return false;
    }

    public static class Builder extends PatternPartSynapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            SecondaryInputSynapse s = (SecondaryInputSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected SynapseFactory getSynapseFactory() {
            return (input, output) -> new SecondaryInputSynapse(input, output);
        }
    }
}
