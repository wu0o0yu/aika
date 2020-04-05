package network.aika.neuron.activation.linker;


import network.aika.neuron.activation.Activation;

public abstract class LLink {
    public LNode input;
    public LNode output;

    public LLink(LNode input, LNode output) {
        this.input = input;
        this.output = output;
    }

    public abstract void follow(Activation act, LNode from, long v, Linker.CollectResults c);
}
