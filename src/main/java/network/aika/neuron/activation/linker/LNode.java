package network.aika.neuron.activation.linker;

import network.aika.neuron.activation.Activation;

public class LNode {

    String label;

    LLink[] links;
    PatternType patternType;

    Byte neuronType;

    public LNode(PatternType patternType, Byte neuronType, String label) {
        this.patternType = patternType;
        this.neuronType = neuronType;
        this.label = label;
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
