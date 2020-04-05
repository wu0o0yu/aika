package network.aika.neuron.activation.linker;

import network.aika.neuron.activation.Activation;

public class LNode {

    LLink[] links;
    PatternType patternType;

    Byte neuronType;

    public LNode(PatternType patternType, Byte neuronType) {
        this.patternType = patternType;
        this.neuronType = neuronType;
    }

    public void setLinks(LLink... links) {
        this.links = links;
    }

    public void follow(Activation act, LLink from, long v, Linker.CollectResults c) {
        if(act.visited == v) return;
        act.visited = v;

        if(neuronType != null && neuronType != act.getINeuron().getType()) {
            return;
        }

        for(int i = 0; i < links.length; i++) {
            LLink ln = links[i];
            if(ln == from) {
                continue;
            }

            ln.follow(act, this, v, c);
        }
    }
}
