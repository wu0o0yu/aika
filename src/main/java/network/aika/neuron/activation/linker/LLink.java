package network.aika.neuron.activation.linker;


import network.aika.neuron.activation.Link;

public class LLink {
    public LNode input;
    public LNode output;

    public LLink(LNode input, LNode output) {
        this.input = input;
        this.output = output;
    }

    protected LNode getTo(LNode from) {
        if(from == input) {
            return output;
        }
        if(from == output) {
            return input;
        }
        return null;
    }

    public void follow(Link l, LNode from, long v, Linker.CollectResults c) {

    }

    private boolean checkLink(Link l, LNode from) {

    }

}
