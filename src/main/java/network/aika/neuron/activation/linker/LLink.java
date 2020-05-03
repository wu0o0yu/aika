package network.aika.neuron.activation.linker;


import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

public abstract class LLink<S extends Synapse> {
    public String label;

    public LNode input;
    public LNode output;

    protected PatternScope patternScope;
    protected Class<S> synapseClass;

    public LLink(LNode input, LNode output, PatternScope patternScope, Class<S> synapseClass, String label) {
        this.input = input;
        this.output = output;
        this.patternScope = patternScope;
        this.synapseClass = synapseClass;
        this.label = label;

        input.addLink(this);
        output.addLink(this);
    }

    public abstract void follow(Mode m, Activation act, LNode from, Activation startAct);

    public abstract String getTypeStr();

    public Activation getToActivation(Link l, LNode to) {
        if(to == output) {
            return l.getOutput();
        }
        if(to == input) {
            return l.getInput();
        }
        return null;
    }

    public String toString() {
        return getTypeStr() + " " + label + " " + (patternScope != null ? patternScope : "X") + " " + input.label  + " -> " + output.label;
    }
}
