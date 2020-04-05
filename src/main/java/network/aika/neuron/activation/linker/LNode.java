package network.aika.neuron.activation.linker;

import network.aika.neuron.activation.Activation;

public class LNode {

    LLink[] links;
    PatternType patternType;

    enum PatternType {
        CURRENT,
        INPUT,
        RELATED
    }


    public LNode(PatternType patternType, LLink... links) {
        this.links = links;
        this.patternType = patternType;
    }

    public void follow(Activation act, LLink from, long v, Linker.CollectResults c) {
        if(act.visited == v) return;
        act.visited = v;

        for(int i = 0; i < links.length; i++) {
            LLink ln = links[i];
            if(ln == from) {
                continue;
            }

            ln.follow(act, this, v, c);
        }
    }
}
