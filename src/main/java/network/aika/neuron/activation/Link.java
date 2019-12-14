package network.aika.neuron.activation;

import network.aika.neuron.Synapse;
import network.aika.neuron.TSynapse;

import static network.aika.neuron.activation.Direction.INPUT;
import static network.aika.neuron.activation.Direction.OUTPUT;

public class Link {

    final Synapse synapse;
    TSynapse targetSynapse;

    final Activation input;
    final Activation output;


    public Link(Synapse s, Activation input, Activation output) {
        this.synapse = s;
        this.targetSynapse = null;
        this.input = input;
        this.output = output;
    }


    public Link(Synapse s, TSynapse targetSynapse, Activation input, Activation output) {
        this.synapse = s;
        this.targetSynapse = targetSynapse;
        this.input = input;
        this.output = output;
    }


    public Synapse getSynapse() {
        return synapse;
    }


    public TSynapse getTargetSynapse() {
        return targetSynapse;
    }

    public void setTargetSynapse(TSynapse targetSynapse) {
        this.targetSynapse = targetSynapse;
    }


    public Activation getInput() {
        return input;
    }


    public Activation getOutput() {
        return output;
    }


    public boolean isNegative(Synapse.State s) {
        return synapse.isNegative(s);
    }


    public boolean isInactive() {
        return synapse.isInactive();
    }


    public boolean isRecurrent() {
        return synapse.isRecurrent();
    }


    public void link() {
        input.outputLinks.put(this, this);
        output.inputLinks.put(this, this);
    }


    public String toString() {
        return synapse + ": " + input + " --> " + output;
    }

}
