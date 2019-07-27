package network.aika.neuron.activation;

import network.aika.neuron.Synapse;

import java.util.Comparator;

import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;


public class Link {
    private final Synapse synapse;
    private final Activation input;
    private final Activation output;

    public static Comparator<Link> INPUT_COMP = (l1, l2) -> {
        int r = Synapse.INPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
        if (r != 0) return r;
        return Integer.compare(l1.input.getId(), l2.input.getId());
    };

    public static Comparator<Link> OUTPUT_COMP = (l1, l2) -> {
        int r = Synapse.OUTPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
        if (r != 0) return r;
        return Integer.compare(l1.output.getId(), l2.output.getId());
    };


    public Link(Synapse s, Activation input, Activation output) {
        this.synapse = s;
        this.input = input;
        this.output = output;
    }


    public Synapse getSynapse() {
        return synapse;
    }


    public Activation getInput() {
        return input;
    }


    public Activation getOutput() {
        return output;
    }


    public boolean isRecurrent() {
        return synapse.isRecurrent();
    }


    public boolean isIdentity() {
        return synapse.isIdentity();
    }


    public boolean isNegative(Synapse.State s) {
        return synapse.isNegative(s);
    }


    public boolean isInactive() {
        return synapse.isInactive();
    }


    public void link() {
        input.addLink(INPUT, this);
        output.addLink(OUTPUT, this);
    }


    public String toString() {
        return synapse + ": " + input + " --> " + output;
    }
}


