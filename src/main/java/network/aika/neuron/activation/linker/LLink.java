package network.aika.neuron.activation.linker;


import network.aika.neuron.PatternScope;
import network.aika.neuron.activation.Activation;

public abstract class LLink {
    public String label;

    public LNode input;
    public LNode output;

    protected PatternScope patternScope;

    public LLink(LNode input, LNode output, PatternScope patternScope, String label) {
        this.input = input;
        this.output = output;
        this.patternScope = patternScope;
        this.label = label;
    }

    public abstract void follow(Activation act, LNode from, long v, Linker.CollectResults c);
}
